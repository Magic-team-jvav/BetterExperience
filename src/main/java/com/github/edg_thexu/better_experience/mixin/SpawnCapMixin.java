package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.module.spawning.SpawnContext;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Both vanilla global and per-player caps must be scaled, otherwise the local cap wins. */
@Mixin(NaturalSpawner.SpawnState.class)
public abstract class SpawnCapMixin {
    @ModifyExpressionValue(method = "canSpawnForCategory", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/MobCategory;getMaxInstancesPerChunk()I"))
    private int betterExperience$cap(int original) {
        return SpawnContext.scaleCap(original);
    }
}
