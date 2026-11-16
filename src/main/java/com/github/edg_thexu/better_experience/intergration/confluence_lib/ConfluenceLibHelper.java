package com.github.edg_thexu.better_experience.intergration.confluence_lib;

import net.neoforged.fml.ModList;

public class ConfluenceLibHelper {
    public static boolean isLoaded() {
        return ModList.get().isLoaded("confluence_magic_lib");
    }
}
