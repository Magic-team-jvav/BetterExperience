package com.github.edg_thexu.better_experience.module.ammochain;

import com.github.edg_thexu.better_experience.data.component.AmmoChainCursor;
import com.github.edg_thexu.better_experience.data.component.AmmoChainSettings;

import java.util.Objects;
import java.util.Optional;
import java.util.function.IntPredicate;

public final class AmmoChainQueue {
    private AmmoChainQueue() {}

    /** Availability includes both the weapon's projectile predicate and the player's inventory. */
    public static Optional<AmmoChainCursor> choose(AmmoChainSettings settings, AmmoChainCursor cursor, IntPredicate available) {
        Objects.requireNonNull(settings);
        Objects.requireNonNull(cursor);
        Objects.requireNonNull(available);
        if (!settings.enabled()) return Optional.empty();
        int size = settings.entries().size();
        int first = cursor.index() % size;
        for (int offset = 0; offset < size; offset++) {
            int index = (first + offset) % size;
            if (!settings.entries().get(index).isEmpty() && available.test(index)) {
                return Optional.of(new AmmoChainCursor(index, offset == 0 && index == cursor.index() ? cursor.fired() : 0));
            }
        }
        return Optional.empty();
    }

    /** Call once for an actual shot, including shots whose ammunition was conserved. */
    public static AmmoChainCursor advance(AmmoChainSettings settings, AmmoChainCursor cursor) {
        int size = settings.entries().size();
        int index = cursor.index() % size;
        var entry = settings.entries().get(index);
        int fired = index == cursor.index() ? cursor.fired() + 1 : 1;
        if (entry.isEmpty() || fired >= entry.shots()) return new AmmoChainCursor((index + 1) % size, 0);
        return new AmmoChainCursor(index, fired);
    }
}
