package com.github.edg_thexu.better_experience.module.spawning;

import net.minecraft.util.RandomSource;

/** Only active during natural spawning; never changes enum state or event/spawner group sizes. */
public final class SpawnContext {
    private static final ThreadLocal<Double> CAP = new ThreadLocal<>();

    private SpawnContext() {}

    public static Double enter(double cap) {
        Double previous = CAP.get();
        CAP.set(cap);
        return previous;
    }

    public static void restore(Double previous) {
        if (previous == null) CAP.remove();
        else CAP.set(previous);
    }

    public static int scaleCap(int original) {
        Double multiplier = CAP.get();
        if (multiplier == null || multiplier == 1.0) return original;
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(original * multiplier));
    }

    public static int attempts(double multiplier, RandomSource random) {
        int whole = (int) multiplier;
        double fraction = multiplier - whole;
        return whole + (fraction > 0.0 && random.nextDouble() < fraction ? 1 : 0);
    }
}
