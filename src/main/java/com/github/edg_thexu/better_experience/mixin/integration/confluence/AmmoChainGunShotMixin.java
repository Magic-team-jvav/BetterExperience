package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.module.ammochain.AmmoChainManager;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceGunSupport;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.confluence.mod.common.item.gun.BaseGun;
import org.confluence.mod.common.item.gun.ShootingService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ShootingService.class, remap = false)
public abstract class AmmoChainGunShotMixin {
    @Inject(method = "tryShoot", at = @At("HEAD"), cancellable = true)
    private static void betterExperience$stopEmptyChain(ServerPlayer player, CallbackInfoReturnable<Boolean> callback) {
        ItemStack weapon = player.getMainHandItem();
        if (ConfluenceGunSupport.isGun(weapon) && AmmoChainManager.controls(player, weapon)
                && AmmoChainManager.projectile(player, weapon, ItemStack.EMPTY).isEmpty()) callback.setReturnValue(false);
    }

    @WrapOperation(method = "tryShoot", at = @At(value = "INVOKE",
            target = "Lorg/confluence/mod/common/item/gun/GunFiringService;fire(Lnet/minecraft/server/level/ServerPlayer;Lorg/confluence/mod/common/item/gun/BaseGun;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)I"))
    private static int betterExperience$advanceChain(ServerPlayer player, BaseGun gun, ItemStack weapon, ItemStack ammo, Operation<Integer> original) {
        ItemStack sample = ammo.copyWithCount(1);
        int fired = original.call(player, gun, weapon, ammo);
        if (fired > 0 && !sample.isEmpty()) AmmoChainManager.shot(player, weapon, sample);
        return fired;
    }
}
