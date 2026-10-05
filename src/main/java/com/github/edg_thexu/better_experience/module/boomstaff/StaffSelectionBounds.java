package com.github.edg_thexu.better_experience.module.boomstaff;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Inclusive block coordinates, shared by selection previews and server validation. */
public record StaffSelectionBounds(BlockPos min, BlockPos max) {
    public static StaffSelectionBounds of(BlockPos a, BlockPos b) {
        return new StaffSelectionBounds(
                new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ())),
                new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())));
    }

    public boolean fits(int maxSide) {
        return span(0) <= maxSide && span(1) <= maxSide && span(2) <= maxSide;
    }

    public long volume() {
        return span(0) * span(1) * span(2);
    }

    public Vec3 center() {
        return new Vec3(((double) min.getX() + max.getX() + 1) / 2,
                ((double) min.getY() + max.getY() + 1) / 2,
                ((double) min.getZ() + max.getZ() + 1) / 2);
    }

    public boolean withinReach(Vec3 eye, double reach) {
        // The farthest corner can mix coordinates from both original points.
        double dx = Math.max(Math.abs(min.getX() + .5 - eye.x), Math.abs(max.getX() + .5 - eye.x));
        double dy = Math.max(Math.abs(min.getY() + .5 - eye.y), Math.abs(max.getY() + .5 - eye.y));
        double dz = Math.max(Math.abs(min.getZ() + .5 - eye.z), Math.abs(max.getZ() + .5 - eye.z));
        return dx * dx + dy * dy + dz * dz <= reach * reach;
    }

    private long span(int axis) {
        return (long) coordinate(max, axis) - coordinate(min, axis) + 1;
    }

    private static int coordinate(BlockPos pos, int axis) {
        return axis == 0 ? pos.getX() : axis == 1 ? pos.getY() : pos.getZ();
    }

    /** One mana per eight selected blocks, rounded up, including air. */
    public int manaCost() {
        return (int) Math.max(1, (volume() + 7) / 8);
    }

    /** Returns null when an adjustment would cross a boundary or exceed the side limit. */
    public StaffSelectionBounds adjust(StaffSelectionDirection direction, int delta, int maxSide) {
        int[] low = {min.getX(), min.getY(), min.getZ()};
        int[] high = {max.getX(), max.getY(), max.getZ()};
        int[] steps = {direction.x(), direction.y(), direction.z()};
        for (int axis = 0; axis < 3; axis++) {
            if (steps[axis] > 0) high[axis] += delta;
            else if (steps[axis] < 0) low[axis] -= delta;
            if (high[axis] < low[axis] || (long) high[axis] - low[axis] + 1 > maxSide) return null;
        }
        return new StaffSelectionBounds(new BlockPos(low[0], low[1], low[2]),
                new BlockPos(high[0], high[1], high[2]));
    }
}
