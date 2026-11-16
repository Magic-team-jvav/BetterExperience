package com.github.edg_thexu.better_experience.intergration.terra_curios;

import net.neoforged.fml.ModList;

public class TCHelper {
    public static boolean isLoaded() {
        return ModList.get().isLoaded("terra_curio");
    }
}
