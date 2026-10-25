package com.github.edg_thexu.better_experience.mixin.accessor;

import net.minecraft.world.level.LevelSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelSettings.class)
public interface LevelSettingsAccess {
    @Mutable
    @Accessor("allowCommands")
    void betterExperience$setAllowCommands(boolean enabled);
}
