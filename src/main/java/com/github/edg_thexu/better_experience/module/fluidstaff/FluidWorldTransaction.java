package com.github.edg_thexu.better_experience.module.fluidstaff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** World changes are committed only after protection hooks and resource revalidation. */
final class FluidWorldTransaction {
    private FluidWorldTransaction() {}

    static boolean apply(ServerPlayer player, BlockPos pos, boolean placing,
                         BooleanSupplier change, BooleanSupplier canCommit, Runnable consume) {
        var level = player.serverLevel();
        if (level.captureBlockSnapshots || level.restoringBlockSnapshots) return false;
        int start = level.capturedBlockSnapshots.size();
        List<BlockSnapshot> snapshots = new ArrayList<>();
        boolean accepted = false;
        try {
            level.captureBlockSnapshots = true;
            boolean changed;
            try {
                changed = change.getAsBoolean();
            } finally {
                level.captureBlockSnapshots = false;
                var captured = level.capturedBlockSnapshots.subList(start, level.capturedBlockSnapshots.size());
                snapshots.addAll(captured);
                captured.clear();
            }
            if (!changed || snapshots.isEmpty()) return false;
            if (placing) {
                boolean canceled = snapshots.size() > 1 ? EventHooks.onMultiBlockPlace(player, snapshots, Direction.UP)
                        : EventHooks.onBlockPlace(player, snapshots.getFirst(), Direction.UP);
                if (canceled) return false;
            }
            if (!level.mayInteract(player, pos) || !canCommit.getAsBoolean()) return false;
            consume.run();
            accepted = true;
        } finally {
            if (!accepted) {
                boolean previous = level.restoringBlockSnapshots;
                level.restoringBlockSnapshots = true;
                try {
                    for (BlockSnapshot snapshot : snapshots.reversed()) snapshot.restore(Block.UPDATE_CLIENTS);
                } finally {
                    level.restoringBlockSnapshots = previous;
                }
            }
        }
        for (BlockSnapshot snapshot : snapshots) {
            var state = level.getBlockState(snapshot.getPos());
            state.onPlace(level, snapshot.getPos(), snapshot.getState(), false);
            level.markAndNotifyBlock(snapshot.getPos(), level.getChunkAt(snapshot.getPos()), snapshot.getState(),
                    state, snapshot.getFlags(), 512);
        }
        return true;
    }
}
