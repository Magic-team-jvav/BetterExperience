package com.github.edg_thexu.better_experience.mixin.client;

import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightTexture.class)
public abstract class NightVisionLightMixin {
    @WrapOperation(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean betterExperience$nightVision(LocalPlayer player, Holder<MobEffect> effect, Operation<Boolean> original) {
        return effect == MobEffects.NIGHT_VISION && WorldOptions.clientEnabled(WorldOptions.Feature.PERMANENT_NIGHT_VISION)
                || original.call(player, effect);
    }
}
