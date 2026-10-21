package com.github.edg_thexu.better_experience.module.locator;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class LocatorNames {
    private LocatorNames() {}

    public static Component structure(ResourceLocation id) {
        String fallback = Arrays.stream(id.getPath().replace('/', '_').split("_"))
                .filter(part -> !part.isEmpty()).map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining(" "));
        return Component.translatableWithFallback("structure." + id.getNamespace() + "." + id.getPath().replace('/', '.'), fallback);
    }
}
