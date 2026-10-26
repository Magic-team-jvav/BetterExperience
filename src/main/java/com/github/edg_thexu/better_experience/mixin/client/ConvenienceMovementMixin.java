package com.github.edg_thexu.better_experience.mixin.client;

import com.github.edg_thexu.better_experience.module.worldoptions.ConvenienceRules;
import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class ConvenienceMovementMixin {
    @ModifyReturnValue(method = "hasEnoughFoodToStartSprinting", at = @At("RETURN"))
    private boolean betterExperience$sprintWhenHungry(boolean original) {
        return original || WorldOptions.clientEnabled(WorldOptions.Feature.HUNGRY_SPRINTING);
    }

    @ModifyExpressionValue(method = {"aiStep", "canStartSprinting"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean betterExperience$drawAtFullSpeed(boolean original) {
        return original && !ConvenienceRules.fullSpeedBow(((LocalPlayer) (Object) this).getUseItem());
    }
}
