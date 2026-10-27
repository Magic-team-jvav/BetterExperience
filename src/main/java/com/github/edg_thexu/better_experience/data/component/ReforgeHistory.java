package com.github.edg_thexu.better_experience.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;

/** Stable prefix names map to the number of times this particular item rolled them. */
public record ReforgeHistory(Map<String, Integer> prefixes) {
    public static final int MAX_PREFIXES = 256;
    public static final ReforgeHistory EMPTY = new ReforgeHistory(Map.of());
    public static final Codec<ReforgeHistory> CODEC = Codec.unboundedMap(Codec.STRING, Codec.intRange(1, Integer.MAX_VALUE))
            .validate(map -> map.size() <= MAX_PREFIXES ? DataResult.success(map)
                    : DataResult.error(() -> "Too many reforge history entries"))
            .xmap(ReforgeHistory::new, ReforgeHistory::prefixes);
    public static final StreamCodec<RegistryFriendlyByteBuf, ReforgeHistory> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public ReforgeHistory {
        prefixes = Map.copyOf(prefixes);
        if (prefixes.size() > MAX_PREFIXES || prefixes.values().stream().anyMatch(value -> value <= 0))
            throw new IllegalArgumentException("Invalid reforge history");
    }

    public ReforgeHistory record(String name) {
        if (name == null || name.equals("unknown") || name.isBlank()) return this;
        if (!prefixes.containsKey(name) && prefixes.size() >= MAX_PREFIXES) return this;
        var updated = new HashMap<>(prefixes);
        updated.compute(name, (key, count) -> count == null ? 1 : count == Integer.MAX_VALUE ? count : count + 1);
        return new ReforgeHistory(updated);
    }
}
