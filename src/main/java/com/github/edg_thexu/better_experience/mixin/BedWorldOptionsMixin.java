package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.module.worldoptions.SafeBedExplosionCalculator;
import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BedBlock.class)
public abstract class BedWorldOptionsMixin {
    @ModifyReturnValue(method = "canSetSpawn", at = @At("RETURN"))
    private static boolean betterExperience$safeSleep(boolean original, Level level) {
        return original || WorldOptions.enabled(level, WorldOptions.Feature.SAFE_SLEEP);
    }

    @WrapOperation(method = "useWithoutItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;Lnet/minecraft/world/phys/Vec3;FZLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"))
    private Explosion betterExperience$protectPlayers(Level level, Entity source, DamageSource damageSource,
                                                       ExplosionDamageCalculator calculator, Vec3 pos, float radius, boolean fire,
                                                       Level.ExplosionInteraction interaction, Operation<Explosion> original) {
        if (WorldOptions.enabled(level, WorldOptions.Feature.BED_BLAST_IMMUNITY)) calculator = new SafeBedExplosionCalculator(calculator);
        return original.call(level, source, damageSource, calculator, pos, radius, fire, interaction);
    }
}
