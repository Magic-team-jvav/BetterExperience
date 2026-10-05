package com.github.edg_thexu.better_experience.module.boomstaff;

import net.minecraft.world.phys.Vec3;

/** Keeps the center in the sight interval and follows the camera at either distance limit. */
public final class StaffAimCursor {
    private double distance = Double.NaN;
    private double lastValidDistance = Double.NaN;
    private Vec3 lastValidPoint;

    public void reset() {
        distance = Double.NaN;
        lastValidDistance = Double.NaN;
        lastValidPoint = null;
    }

    public Vec3 update(StaffAimRange range, Vec3 eye, Vec3 look, double initialDistance) {
        if (Double.isNaN(distance)) distance = initialDistance;
        // A frozen world coordinate can leave reach when the player walks away.
        // Restore a valid sight distance instead of sending an unreachable blast center.
        if (!range.contains(distance) && lastValidPoint != null
                && !range.withinMaximumReach(eye, lastValidPoint, 0)) {
            distance = Math.clamp(lastValidDistance, range.min(), range.max());
        }
        if (range.contains(distance)) {
            lastValidDistance = distance;
            lastValidPoint = range.position(eye, look, distance);
        }
        return lastValidPoint;
    }

    public void move(StaffAimRange range, double delta) {
        // Rejected input must not accumulate an invisible overshoot beyond the interval.
        double base = Double.isNaN(lastValidDistance) ? distance : lastValidDistance;
        // A new block under the crosshair can move the interval past the old valid distance.
        // In that case, successive inputs toward the interval must still make progress.
        if (!range.contains(base) && ((distance < range.min() && delta > 0)
                || (distance > range.max() && delta < 0))) base = distance;
        if (!Double.isNaN(base)) {
            double next = base + delta;
            // Reject scrolling past either limit without invalidating the accepted distance.
            // The center must keep following camera movement at both limits.
            distance = range.contains(base) && !range.contains(next) ? base : next;
        }
    }
}
