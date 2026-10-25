package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.module.spawning.SpawnContext;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.level.LocalMobCapCalculator$MobCounts")
public abstract class LocalSpawnCapMixin {
    @ModifyExpressionValue(method = "canSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/MobCategory;getMaxInstancesPerChunk()I"))
    private int betterExperience$cap(int original) {
        return SpawnContext.scaleCap(original);
    }
}
