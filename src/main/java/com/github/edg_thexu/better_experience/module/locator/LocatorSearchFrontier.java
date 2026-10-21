package com.github.edg_thexu.better_experience.module.locator;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/** Breadth expansion ordered by distance bounds; a result is emitted only after all closer regions were considered. */
public final class LocatorSearchFrontier {
    public record Candidate(ChunkPos chunk, StructurePlacement placement, BlockPos position, double distanceSquared) {}
    private record Region(Grid grid, int x, int z, double lowerBound) {}
    private final class Grid {
        final StructurePlacement placement;
        final RandomSpreadStructurePlacement spread;
        final int spacing;
        final int extent;
        final LongOpenHashSet visited = new LongOpenHashSet();

        Grid(StructurePlacement placement) {
            this.placement = placement;
            spread = placement.getClass() == RandomSpreadStructurePlacement.class ? (RandomSpreadStructurePlacement) placement : null;
            spacing = spread == null ? 1 : spread.spacing();
            extent = spread == null ? 0 : spacing - spread.separation() - 1;
        }

        void offer(int x, int z) {
            if (!visited.add(ChunkPos.asLong(x, z))) return;
            var min = placement.getLocatePos(new ChunkPos(x * spacing, z * spacing));
            var max = placement.getLocatePos(new ChunkPos(x * spacing + extent, z * spacing + extent));
            double bound = rectangleDistanceSquared(origin.getX(), origin.getZ(), min.getX(), min.getZ(), max.getX(), max.getZ());
            if (bound <= radiusSquared) regions.add(new Region(this, x, z, bound));
        }
    }

    private final BlockPos origin;
    private final double radiusSquared;
    private final ChunkGeneratorStructureState state;
    private final PriorityQueue<Region> regions = new PriorityQueue<>(Comparator.comparingDouble(Region::lowerBound));
    private final PriorityQueue<Candidate> candidates = new PriorityQueue<>(Comparator.comparingDouble(Candidate::distanceSquared)
            .thenComparingLong(candidate -> candidate.chunk.toLong()));
    private final Map<Long, Candidate> queued = new HashMap<>();
    private final LongOpenHashSet emitted = new LongOpenHashSet();

    public LocatorSearchFrontier(BlockPos origin, int radius, ChunkGeneratorStructureState state, List<StructurePlacement> placements) {
        this.origin = origin.immutable();
        radiusSquared = (double) radius * radius;
        this.state = state;
        for (StructurePlacement placement : placements) {
            if (placement.getClass() == ConcentricRingsStructurePlacement.class) {
                var rings = (ConcentricRingsStructurePlacement) placement;
                var positions = state.getRingPositionsFor(rings);
                if (positions != null) for (ChunkPos chunk : positions) {
                    if (placement.isStructureChunk(state, chunk.x, chunk.z)) offerCandidate(chunk, placement);
                }
            } else {
                var grid = new Grid(placement);
                var offset = placement.getLocatePos(new ChunkPos(0, 0));
                int x = Math.floorDiv(origin.getX() - offset.getX(), grid.spacing * 16);
                int z = Math.floorDiv(origin.getZ() - offset.getZ(), grid.spacing * 16);
                // Four starting cells cover the nearest rectangle even in a random-spread separation gap.
                grid.offer(x, z);
                grid.offer(x + 1, z);
                grid.offer(x, z + 1);
                grid.offer(x + 1, z + 1);
            }
        }
    }

    public static double rectangleDistanceSquared(double x, double z, double minX, double minZ, double maxX, double maxZ) {
        double dx = Math.max(Math.max(minX - x, 0), x - maxX);
        double dz = Math.max(Math.max(minZ - z, 0), z - maxZ);
        return dx * dx + dz * dz;
    }

    private void offerCandidate(ChunkPos chunk, StructurePlacement placement) {
        long key = chunk.toLong();
        if (emitted.contains(key)) return;
        var point = placement.getLocatePos(chunk);
        double dx = (double) point.getX() - origin.getX(), dz = (double) point.getZ() - origin.getZ();
        double distance = dx * dx + dz * dz;
        if (distance > radiusSquared) return;
        var previous = queued.get(key);
        if (previous != null && previous.distanceSquared <= distance) return;
        var candidate = new Candidate(chunk, placement, point, distance);
        queued.put(key, candidate);
        candidates.add(candidate);
    }

    /** Performs one bounded expansion or emits one candidate. Null means more expansion is needed. */
    public Candidate advance() {
        while (!candidates.isEmpty() && queued.get(candidates.peek().chunk.toLong()) != candidates.peek()) candidates.remove();
        if (!candidates.isEmpty() && (regions.isEmpty() || candidates.peek().distanceSquared <= regions.peek().lowerBound)) {
            var candidate = candidates.remove();
            queued.remove(candidate.chunk.toLong());
            emitted.add(candidate.chunk.toLong());
            return candidate;
        }
        if (regions.isEmpty()) return null;
        var region = regions.remove();
        Grid grid = region.grid;
        grid.offer(region.x - 1, region.z);
        grid.offer(region.x + 1, region.z);
        grid.offer(region.x, region.z - 1);
        grid.offer(region.x, region.z + 1);
        ChunkPos chunk = grid.spread != null
                ? grid.spread.getPotentialStructureChunk(state.getLevelSeed(), region.x * grid.spacing, region.z * grid.spacing)
                : new ChunkPos(region.x, region.z);
        if (grid.placement.isStructureChunk(state, chunk.x, chunk.z)) offerCandidate(chunk, grid.placement);
        return null;
    }

    public boolean hasWork() { return !regions.isEmpty() || !candidates.isEmpty(); }
}
