package com.github.edg_thexu.better_experience.module.spacestaff;

import com.github.edg_thexu.better_experience.module.boomstaff.StaffSelectionBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Two independent radii; a horizontal circle and a vertical scale describe a spheroid. */
public record SpaceSphere(Vec3 center, double horizontalRadius, double verticalRadius) {
    private static final Direction[] DIRECTIONS = Direction.values();
    public static SpaceSphere fromBounds(StaffSelectionBounds bounds) {
        return new SpaceSphere(bounds.center(),
                Math.min(bounds.max().getX() - bounds.min().getX() + 1,
                        bounds.max().getZ() - bounds.min().getZ() + 1) / 2.0,
                (bounds.max().getY() - bounds.min().getY() + 1) / 2.0);
    }

    public static int horizontalBlocks(BlockPos center, BlockPos point) {
        return (int) Math.ceil(Math.hypot((double) point.getX() - center.getX(), (double) point.getZ() - center.getZ()));
    }

    public static int verticalBlocks(BlockPos center, BlockPos point) {
        return Math.abs(point.getY() - center.getY());
    }

    public static StaffSelectionBounds bounds(BlockPos center, int horizontal, int vertical) {
        if (horizontal < 0 || horizontal > 10 || vertical < 0 || vertical > 10)
            throw new IllegalArgumentException("Sphere radii must be between 0 and 10 blocks");
        return StaffSelectionBounds.of(center.offset(-horizontal, -vertical, -horizontal),
                center.offset(horizontal, vertical, horizontal));
    }

    public boolean contains(BlockPos pos) {
        Vec3 delta = Vec3.atCenterOf(pos).subtract(center);
        double horizontal = (delta.x * delta.x + delta.z * delta.z) / (horizontalRadius * horizontalRadius);
        double vertical = delta.y * delta.y / (verticalRadius * verticalRadius);
        return horizontal + vertical <= 1 + 1e-9;
    }

    public boolean isShell(BlockPos pos) {
        for (Direction direction : DIRECTIONS) {
            if (!contains(pos.relative(direction))) return true;
        }
        return false;
    }
}
