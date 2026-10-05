package com.github.edg_thexu.better_experience.module.boomstaff;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Distance interval for a locked shape's geometric center, independent of its size. */
public record StaffAimRange(double min, double max) {
    public static StaffAimRange of(double blockDistance, double maxReach) {
        double lower = Double.isFinite(blockDistance) ? blockDistance : 1;
        return new StaffAimRange(Math.clamp(lower, 0, maxReach), maxReach);
    }

    public boolean contains(double distance) {
        return Double.isFinite(distance) && distance >= min && distance <= max;
    }

    /** Maximum reach only; the front block lower bound belongs to client aiming. */
    public boolean withinMaximumReach(Vec3 eye, Vec3 center, double tolerance) {
        double squaredDistance = eye.distanceToSqr(center);
        double reach = max + tolerance;
        return Double.isFinite(squaredDistance) && squaredDistance <= reach * reach + 1.0E-6;
    }

    public BlockPos point(Vec3 eye, Vec3 look, double distance) {
        return BlockPos.containing(position(eye, look, distance));
    }

    public Vec3 position(Vec3 eye, Vec3 look, double distance) {
        return eye.add(look.scale(distance));
    }
}
