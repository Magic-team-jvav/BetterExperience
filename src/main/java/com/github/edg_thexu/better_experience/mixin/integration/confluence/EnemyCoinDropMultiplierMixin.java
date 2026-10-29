package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.Level;
import org.confluence.mod.util.ModUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Scales the coins Confluence spawns for hostile mob deaths by the world option multiplier. */
@Mixin(value = ModUtils.class, remap = false)
public abstract class EnemyCoinDropMultiplierMixin {

    @WrapOperation(method = "enemyDropMoney", at = @At(value = "INVOKE",
            target = "Lorg/confluence/mod/util/ModUtils;dropMoney(IDDDLnet/minecraft/world/level/Level;)V"))
    private static void betterExperience$scaleCoinDrop(int amount, double x, double y, double z, Level level,
                                                       Operation<Void> original) {
        double multiplier = WorldOptions.coinDrop(level);
        if (multiplier == 1.0 || amount <= 0) {
            original.call(amount, x, y, z, level);
            return;
        }
        // Random carry keeps fractional multipliers unbiased instead of always rounding down.
        int scaled = (int) Math.floor(amount * multiplier + level.random.nextFloat());
        if (scaled > 0) original.call(scaled, x, y, z, level);
    }
}
