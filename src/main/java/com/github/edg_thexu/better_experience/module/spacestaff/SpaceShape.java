package com.github.edg_thexu.better_experience.module.spacestaff;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum SpaceShape {
    CUBE, HOLLOW_CUBE, SPHERE, HOLLOW_SPHERE, LINE, BEZIER;

    public static final Codec<SpaceShape> CODEC = Codec.STRING.comapFlatMap(name -> {
        try {
            return DataResult.success(valueOf(name.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown space shape: " + name);
        }
    }, shape -> shape.name().toLowerCase(Locale.ROOT));

    public String translationKey() {
        return "better_experience.space.shape." + name().toLowerCase(Locale.ROOT);
    }
}
