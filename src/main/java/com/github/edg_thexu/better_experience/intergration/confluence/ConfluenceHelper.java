package com.github.edg_thexu.better_experience.intergration.confluence;

import net.neoforged.fml.ModList;

public class ConfluenceHelper {
    public static boolean isLoaded() {
        return ModList.get().isLoaded("confluence");
    }
}
