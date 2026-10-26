package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingMenu.class)
public abstract class SmithingTemplateMixin {
    @Shadow @Final private Level level;

    @Inject(method = "shrinkStackInSlot", at = @At("HEAD"), cancellable = true)
    private void betterExperience$keepTemplate(int slot, CallbackInfo callback) {
        if (slot == SmithingMenu.TEMPLATE_SLOT && WorldOptions.enabled(level, WorldOptions.Feature.REUSABLE_TEMPLATES)) callback.cancel();
    }
}
