package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.module.ammochain.AmmoChainManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.confluence.mod.common.event.game.entity.LivingEntityEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LivingEntityEvents.class, remap = false)
public class LivingProjectileSelectionMixin {
    @WrapOperation(method = "livingGetProjectile", at = @At(value = "INVOKE", target = "Lorg/confluence/mod/common/attachment/ExtraInventory;getProjectile(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack betterExperience$selectProjectile(ItemStack fallback, ItemStack weapon, LivingEntity shooter,
                                                              Operation<ItemStack> original) {
        ItemStack projectile = original.call(fallback, weapon, shooter);
        return shooter instanceof Player player ? AmmoChainManager.projectile(player, weapon, projectile) : projectile;
    }
}
