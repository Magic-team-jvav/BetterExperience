package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.module.worldoptions.ConvenienceRules;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class FriendlyMobMixin {
    @ModifyReturnValue(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"))
    private boolean betterExperience$peacefulTargets(boolean original, LivingEntity target) {
        LivingEntity self = (LivingEntity) (Object) this;
        return original && (self.level().isClientSide || !ConvenienceRules.blocksAttack(self, target));
    }
}
