package com.github.edg_thexu.better_experience.module.locator;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.item.StructureLocatorOrb;
import com.github.edg_thexu.better_experience.networks.s2c.LocatorMarkersPacket;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** All history/world access stays on the server thread; expensive chunk work uses the chunk system's async path. */
public final class StructureLocatorManager {
    private static final StructureLocatorManager INSTANCE = new StructureLocatorManager();
    private static final long TICK_BUDGET_NANOS = 3_000_000;
    private static final long SEARCH_TIMEOUT_NANOS = 120_000_000_000L;
    private static final int MAX_STEPS = 200_000;
    private static final int MAX_JOBS = 16;
    private final Map<UUID, Job> jobs = new LinkedHashMap<>();
    private final Map<UUID, Sync> synchronization = new LinkedHashMap<>();
    private record Sync(ServerPlayer player, ResourceLocation dimension, ArrayDeque<StructureMarker> markers) {}

    private static final class Job {
        final ServerPlayer player;
        final ServerLevel level;
        final ItemStack stack;
        final int slot;
        final ResourceLocation target;
        final Holder<Structure> structure;
        final LocatorHistory history;
        final LocatorSearchFrontier frontier;
        final int radius;
        final long started = System.nanoTime();
        long nextProgress = started + 2_000_000_000L;
        int steps;
        CompletableFuture<ChunkResult<ChunkAccess>> pending;
        LocatorSearchFrontier.Candidate candidate;

        Job(ServerPlayer player, ResourceLocation target, Holder<Structure> structure, LocatorHistory history) {
            this.player = player;
            level = player.serverLevel();
            stack = player.getMainHandItem();
            slot = player.getInventory().selected;
            this.target = target;
            this.structure = structure;
            this.history = history;
            var generation = level.getChunkSource().getGeneratorState();
            radius = CommonConfig.LOCATOR_SEARCH_RADIUS.get();
            frontier = new LocatorSearchFrontier(player.blockPosition(), radius,
                    generation, generation.getPlacementsForStructure(structure));
        }
    }

    private StructureLocatorManager() {}
    public static StructureLocatorManager getInstance() { return INSTANCE; }
    public void clear() { jobs.clear(); synchronization.clear(); }
    public void cancel(UUID player) { jobs.remove(player); }

    public void start(ServerPlayer player, ResourceLocation target) {
        if (player.isSpectator() || !(player.getMainHandItem().getItem() instanceof StructureLocatorOrb)
                || player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem())) return;
        if (jobs.containsKey(player.getUUID()) || jobs.size() >= MAX_JOBS) { message(player, "busy"); return; }
        var structure = player.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .getHolder(ResourceKey.create(Registries.STRUCTURE, target));
        if (structure.isEmpty() || !player.serverLevel().structureManager().shouldGenerateStructures()
                || player.serverLevel().getChunkSource().getGeneratorState().getPlacementsForStructure(structure.get()).isEmpty()) {
            message(player, "unavailable");
            return;
        }
        var history = LocatorHistory.get(player);
        if (history.count(player.getUUID()) >= LocatorHistory.MAX_MARKERS_PER_PLAYER) { message(player, "history_full"); return; }
        if (!LocatorPayment.canAfford(player)) { message(player, "insufficient_funds"); return; }
        Job job;
        try {
            job = new Job(player, target, structure.get(), history);
        } catch (RuntimeException exception) {
            Better_experience.LOGGER.error("Cannot initialize structure locator for {}", target, exception);
            message(player, "error");
            return;
        }
        jobs.put(player.getUUID(), job);
        player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), 20);
        player.displayClientMessage(Component.translatable("better_experience.locator.searching", job.radius), true);
    }

    public void synchronize(ServerPlayer player) {
        var dimension = player.level().dimension().location();
        var markers = LocatorHistory.get(player).markers(player.getUUID()).stream()
                .filter(marker -> marker.dimension().equals(dimension)).toList();
        synchronization.put(player.getUUID(), new Sync(player, dimension, new ArrayDeque<>(markers)));
    }

    public void tick() {
        for (var sync : List.copyOf(synchronization.values())) {
            var player = sync.player;
            if (!connected(player) || !player.level().dimension().location().equals(sync.dimension)) {
                synchronization.remove(player.getUUID(), sync);
                continue;
            }
            var batch = new ArrayList<StructureMarker>(LocatorMarkersPacket.MAX_BATCH);
            while (!sync.markers.isEmpty() && batch.size() < LocatorMarkersPacket.MAX_BATCH) batch.add(sync.markers.removeFirst());
            if (!batch.isEmpty()) PacketDistributor.sendToPlayer(player, new LocatorMarkersPacket(batch));
            if (sync.markers.isEmpty()) synchronization.remove(player.getUUID(), sync);
        }
        long deadline = System.nanoTime() + TICK_BUDGET_NANOS;
        var round = new ArrayDeque<>(jobs.values());
        while (!round.isEmpty() && System.nanoTime() < deadline) {
            var job = round.removeFirst();
            try {
                if (!active(job)) { cancel(job.player.getUUID()); continue; }
                if (System.nanoTime() - job.started > SEARCH_TIMEOUT_NANOS || job.steps >= MAX_STEPS) {
                    message(job.player, "limit"); cancel(job.player.getUUID()); continue;
                }
                if (System.nanoTime() >= job.nextProgress) {
                    job.nextProgress = System.nanoTime() + 2_000_000_000L;
                    job.player.displayClientMessage(Component.translatable("better_experience.locator.searching", job.radius), true);
                }
                if (step(job) && active(job) && job.pending == null) round.addLast(job);
            } catch (RuntimeException exception) {
                Better_experience.LOGGER.error("Structure locator failed for {} in {}", job.target, job.level.dimension().location(), exception);
                message(job.player, "error");
                cancel(job.player.getUUID());
            }
        }
        // Rotate the head so multiple long searches share the following tick fairly.
        if (jobs.size() > 1) { var first = jobs.entrySet().iterator().next(); var job = first.getValue(); jobs.remove(first.getKey()); jobs.put(job.player.getUUID(), job); }
    }

    private static boolean connected(ServerPlayer player) { return !player.hasDisconnected() && !player.isRemoved(); }
    private static boolean active(Job job) {
        return INSTANCE.jobs.get(job.player.getUUID()) == job && connected(job.player) && !job.player.isSpectator()
                && job.player.serverLevel() == job.level && job.player.getInventory().selected == job.slot
                && job.player.getMainHandItem() == job.stack
                && job.target.equals(job.stack.get(ModDataComponentTypes.LOCATOR_STRUCTURE.get()));
    }

    private boolean step(Job job) {
        if (job.pending != null) {
            if (!job.pending.isDone()) return true;
            var result = job.pending.getNow(null);
            job.pending = null;
            var chunk = result == null ? null : result.orElse(null);
            if (chunk == null) throw new IllegalStateException("Structure-start chunk could not be loaded");
            if (finishCandidate(job, chunk)) return false;
        }
        if (!job.frontier.hasWork()) { message(job.player, "not_found"); cancel(job.player.getUUID()); return false; }
        job.steps++;
        var candidate = job.frontier.advance();
        if (candidate == null || !job.level.getWorldBorder().isWithinBounds(candidate.position())
                || job.history.contains(job.player.getUUID(), job.level.dimension().location(), job.target, candidate.chunk().toLong())) return true;
        var presence = job.level.structureManager().checkStructurePresence(candidate.chunk(), job.structure.value(), candidate.placement(), false);
        if (presence == StructureCheckResult.START_NOT_PRESENT) return true;
        job.candidate = candidate;
        var loaded = job.level.getChunkSource().getChunkNow(candidate.chunk().x, candidate.chunk().z);
        if (loaded != null) return !finishCandidate(job, loaded);
        var chunkSource = job.level.getChunkSource();
        // getChunkFuture waits on the server thread; its off-thread branch schedules safely instead.
        job.pending = CompletableFuture.supplyAsync(() -> chunkSource.getChunkFuture(candidate.chunk().x, candidate.chunk().z,
                ChunkStatus.STRUCTURE_STARTS, true)).thenCompose(future -> future);
        return true;
    }

    private boolean finishCandidate(Job job, ChunkAccess chunk) {
        var start = chunk.getStartForStructure(job.structure.value());
        if (start == null || !start.isValid()) return false;
        var point = job.candidate.position().atY(start.getBoundingBox().getCenter().getY());
        var marker = new StructureMarker(job.level.dimension().location(), job.target, start.getChunkPos().toLong(), point);
        if (job.history.contains(job.player.getUUID(), marker.dimension(), marker.structure(), marker.startChunk())) return false;
        if (job.history.count(job.player.getUUID()) >= LocatorHistory.MAX_MARKERS_PER_PLAYER) {
            message(job.player, "history_full");
            cancel(job.player.getUUID());
            return true;
        }
        var payment = LocatorPayment.pay(job.player);
        if (payment != LocatorPayment.Result.PAID) {
            message(job.player, payment == LocatorPayment.Result.INSUFFICIENT_FUNDS ? "insufficient_funds" : "no_change_space");
            cancel(job.player.getUUID());
            return true;
        }
        // The debit and insertion run together on the server thread after duplicate/capacity checks.
        job.history.add(job.player.getUUID(), marker);
        job.stack.set(ModDataComponentTypes.LOCATOR_MARKER.get(), marker);
        job.player.getInventory().setChanged();
        job.player.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(job.player, new LocatorMarkersPacket(List.of(marker)));
        job.player.displayClientMessage(Component.translatable("better_experience.locator.found", LocatorNames.structure(job.target),
                point.getX(), point.getY(), point.getZ()), false);
        if (job.player.isCreative()) message(job.player, "marked_success");
        else job.player.displayClientMessage(Component.translatable("better_experience.locator.marked_paid", LocatorPayment.GOLD_COST), true);
        cancel(job.player.getUUID());
        return true;
    }

    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("better_experience.locator." + key), true);
    }
}
