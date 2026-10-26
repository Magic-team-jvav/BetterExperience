package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EyeOfEnder.class)
public abstract class EyeOfEnderMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/projectile/EyeOfEnder;surviveAfterDeath:Z"))
    private boolean betterExperience$recoverEye(boolean original) {
        return original || WorldOptions.enabled(((EyeOfEnder) (Object) this).level(), WorldOptions.Feature.UNBREAKABLE_EYES);
    }
}
