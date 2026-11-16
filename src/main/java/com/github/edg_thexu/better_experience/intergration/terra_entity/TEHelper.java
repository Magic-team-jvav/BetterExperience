package com.github.edg_thexu.better_experience.intergration.terra_entity;

import net.neoforged.fml.ModList;

public class TEHelper {
    public static boolean isLoaded() {
        return ModList.get().isLoaded("terra_entity");
    }
}
