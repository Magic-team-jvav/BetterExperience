package com.github.edg_thexu.better_experience.module.boomstaff;

import net.minecraft.core.BlockPos;

/** Six face normals and twelve edge diagonals; no three-axis corner directions. */
public record StaffSelectionDirection(int x, int y, int z) {
    public BlockPos move(BlockPos pos, int steps) {
        return pos.offset(x * steps, y * steps, z * steps);
    }

    public static StaffSelectionDirection fromRotation(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double vx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double vy = -Math.sin(pitchRad);
        double vz = Math.cos(yawRad) * Math.cos(pitchRad);
        StaffSelectionDirection best = new StaffSelectionDirection(0, 0, 1);
        double bestDot = -Double.MAX_VALUE;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    int axes = Math.abs(x) + Math.abs(y) + Math.abs(z);
                    if (axes == 0 || axes == 3) continue;
                    double dot = (vx * x + vy * y + vz * z) / Math.sqrt(axes);
                    if (dot > bestDot) {
                        bestDot = dot;
                        best = new StaffSelectionDirection(x, y, z);
                    }
                }
            }
        }
        return best;
    }
}
