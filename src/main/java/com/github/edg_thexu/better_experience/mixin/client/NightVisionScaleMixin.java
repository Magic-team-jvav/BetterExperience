package com.github.edg_thexu.better_experience.mixin.client;

import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class NightVisionScaleMixin {
    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
    private static void betterExperience$steadyNightVision(LivingEntity living, float partialTick, CallbackInfoReturnable<Float> callback) {
        if (WorldOptions.clientEnabled(WorldOptions.Feature.PERMANENT_NIGHT_VISION)) callback.setReturnValue(1.0F);
    }
}
