package com.github.edg_thexu.better_experience.module.spacestaff;

import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** At most one bounded job per player; world mutation and inventory consumption stay on the server thread. */
public final class SpacePlacementManager {
    private static final SpacePlacementManager INSTANCE = new SpacePlacementManager();
    private static final int BLOCKS_PER_TICK = 256;
    private final Map<UUID, Job> jobs = new HashMap<>();

    private static final class Job {
        final ServerPlayer player;
        final ServerLevel level;
        final ArrayDeque<BlockPos> positions;
        final SpaceStaffSettings settings;
        final int materialCost;
        int placed;

        Job(ServerPlayer player, List<BlockPos> positions, SpaceStaffSettings settings) {
            this.player = player;
            this.level = player.serverLevel();
            this.positions = new ArrayDeque<>(positions);
            this.settings = settings;
            this.materialCost = SpacePlacementRules.materialCost(settings.state());
        }
    }

    private SpacePlacementManager() {}
    public static SpacePlacementManager getInstance() { return INSTANCE; }

    public boolean enqueue(ServerPlayer player, List<BlockPos> positions, SpaceStaffSettings settings) {
        if (jobs.containsKey(player.getUUID())) {
            player.displayClientMessage(Component.translatable("better_experience.space.busy"), true);
            return false;
        }
        jobs.put(player.getUUID(), new Job(player, positions, settings));
        return true;
    }

    public void clear() { jobs.clear(); }

    public void tick() {
        for (Job job : List.copyOf(jobs.values())) {
            ServerPlayer player = job.player;
            if (!active(job)) {
                jobs.remove(player.getUUID(), job);
                continue;
            }
            boolean lacking = false;
            for (int i = 0; i < BLOCKS_PER_TICK && !job.positions.isEmpty() && active(job); i++) {
                BlockPos pos = job.positions.removeFirst();
                var state = job.settings.state();
                if (!SpacePlacementRules.canPlace(job.level, pos, state) || !job.level.mayInteract(player, pos)) continue;
                if (!player.isCreative() && materialCount(player, job.settings, job.materialCost) < job.materialCost) {
                    lacking = true;
                    break;
                }
                if (place(job, pos)) {
                    job.placed++;
                }
            }
            if (lacking || job.positions.isEmpty()) {
                player.displayClientMessage(Component.translatable(lacking ? "better_experience.space.lacking"
                        : "better_experience.space.placed", job.placed), true);
                jobs.remove(player.getUUID(), job);
            }
        }
    }

    private static boolean active(Job job) {
        return INSTANCE.jobs.get(job.player.getUUID()) == job && !job.player.hasDisconnected()
                && !job.player.isRemoved() && job.player.serverLevel() == job.level
                && job.player.mayBuild() && !job.player.isSpectator();
    }

    private static int materialCount(ServerPlayer player, SpaceStaffSettings settings, int needed) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (SpacePlacementRules.isMaterial(stack, settings.state())) count += Math.min(needed, stack.getCount());
            if (count >= needed) break;
        }
        ItemStack offhand = player.getOffhandItem();
        if (SpacePlacementRules.isMaterial(offhand, settings.state())) count += Math.min(needed, offhand.getCount());
        return count;
    }

    private static void consume(ServerPlayer player, SpaceStaffSettings settings, int count) {
        for (ItemStack stack : player.getInventory().items) {
            if (!SpacePlacementRules.isMaterial(stack, settings.state())) continue;
            int take = Math.min(count, stack.getCount());
            stack.shrink(take);
            count -= take;
            if (count == 0) break;
        }
        if (count > 0) player.getOffhandItem().shrink(count);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static boolean place(Job job, BlockPos pos) {
        ServerLevel level = job.level;
        // Capture suppresses premature neighbor notifications and lets placement protections veto the change.
        if (level.captureBlockSnapshots || level.restoringBlockSnapshots) return false;
        List<BlockSnapshot> snapshots = new ArrayList<>();
        int snapshotStart = level.capturedBlockSnapshots.size();
        boolean accepted = false;
        try {
            level.captureBlockSnapshots = true;
            boolean changed;
            try {
                changed = level.setBlock(pos, job.settings.state(), Block.UPDATE_ALL);
            } finally {
                level.captureBlockSnapshots = false;
                var captured = level.capturedBlockSnapshots.subList(snapshotStart, level.capturedBlockSnapshots.size());
                snapshots.addAll(captured);
                captured.clear();
            }
            if (!changed || snapshots.isEmpty()) return false;
            boolean canceled = snapshots.size() > 1
                    ? EventHooks.onMultiBlockPlace(job.player, snapshots, Direction.UP)
                    : EventHooks.onBlockPlace(job.player, snapshots.getFirst(), Direction.UP);
            if (canceled || !active(job) || !level.mayInteract(job.player, pos)
                    || level.getBlockState(pos) != job.settings.state()
                    || !job.settings.state().canSurvive(level, pos)
                    || (!job.player.isCreative() && materialCount(job.player, job.settings, job.materialCost) < job.materialCost)) {
                return false;
            }
            accepted = true;
        } finally {
            if (!accepted) {
                boolean previousRestoring = level.restoringBlockSnapshots;
                level.restoringBlockSnapshots = true;
                try {
                    // A replacement callback may mutate other blocks; veto must restore all of them.
                    for (BlockSnapshot snapshot : snapshots.reversed()) {
                        snapshot.restore(Block.UPDATE_CLIENTS);
                    }
                } finally {
                    level.restoringBlockSnapshots = previousRestoring;
                }
            }
        }
        if (!job.player.isCreative()) consume(job.player, job.settings, job.materialCost);
        for (BlockSnapshot snapshot : snapshots) {
            var state = level.getBlockState(snapshot.getPos());
            state.onPlace(level, snapshot.getPos(), snapshot.getState(), false);
            level.markAndNotifyBlock(snapshot.getPos(), level.getChunkAt(snapshot.getPos()), snapshot.getState(),
                    state, snapshot.getFlags(), 512);
        }
        return true;
    }
}
