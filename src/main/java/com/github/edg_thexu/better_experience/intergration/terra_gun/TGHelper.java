package com.github.edg_thexu.better_experience.intergration.terra_gun;

import net.neoforged.fml.ModList;

public class TGHelper {
    public static boolean isLoaded() {
        return ModList.get().isLoaded("terra_guns");
    }
}
