package com.github.edg_thexu.better_experience.intergration.curios;

import net.neoforged.fml.ModList;

public class CuriosHelper {
    public static boolean isLoaded() {
        return ModList.get().isLoaded("curios");
    }
}
