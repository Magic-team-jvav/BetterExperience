package com.github.edg_thexu.better_experience.client.gui;

/** Shared drawing and hit-test geometry; zero radians on screen points to the right. */
public record SpaceWheelLayout(double innerRadius, double outerRadius, int sectors) {
    public double centerAngle(int index) {
        return -Math.PI / 2 + index * Math.TAU / sectors;
    }

    public int sectorAt(double dx, double dy) {
        double radius = Math.hypot(dx, dy);
        if (!Double.isFinite(radius) || radius <= innerRadius || radius > outerRadius) return -1;
        double angle = Math.atan2(dy, dx) + Math.PI / 2 + Math.PI / sectors;
        return Math.floorMod((int) Math.floor(angle / (Math.TAU / sectors)), sectors);
    }
}
