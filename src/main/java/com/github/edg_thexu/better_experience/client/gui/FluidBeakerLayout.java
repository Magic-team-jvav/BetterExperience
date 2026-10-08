package com.github.edg_thexu.better_experience.client.gui;

/** Pure layout math also used to verify empty/full surfaces and bounded wave motion. */
public final class FluidBeakerLayout {
    private FluidBeakerLayout() {}

    public static int fillHeight(int height, int amount, int capacity, boolean infinite) {
        if (height <= 0 || capacity <= 0 || (!infinite && amount <= 0)) return 0;
        double fraction = infinite ? 1 : Math.min(1, (double) amount / capacity);
        return Math.max(1, (int) Math.round(height * fraction));
    }

    public static int surface(int x, int top, int bottom, int fillHeight, double time, boolean waves) {
        if (fillHeight <= 0) return bottom;
        double wave = waves ? Math.sin(x * 0.22 + time * 2.5) * 1.5 + Math.sin(x * 0.09 - time * 1.6) : 0;
        return Math.clamp(bottom - fillHeight + (int) Math.round(wave), top, bottom - 1);
    }
}
