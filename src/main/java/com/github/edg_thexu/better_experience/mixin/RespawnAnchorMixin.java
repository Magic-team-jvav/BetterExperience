package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RespawnAnchorBlock.class)
public abstract class RespawnAnchorMixin {
    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"))
    private void betterExperience$keepGlowstone(ItemStack stack, int count, LivingEntity player,
                                               Operation<Void> original, @Local(argsOnly = true) Level level) {
        // The client does not know the world's setting. Let the server consume and sync the item
        // instead of predicting a shrink that would not be corrected when the cost is disabled.
        if (!level.isClientSide && !WorldOptions.enabled(level, WorldOptions.Feature.FREE_ANCHOR)) original.call(stack, count, player);
    }
}
