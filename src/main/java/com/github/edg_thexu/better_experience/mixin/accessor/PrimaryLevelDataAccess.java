package com.github.edg_thexu.better_experience.mixin.accessor;

import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PrimaryLevelData.class)
public interface PrimaryLevelDataAccess {
    @Accessor("settings")
    LevelSettings betterExperience$getSettings();
}
