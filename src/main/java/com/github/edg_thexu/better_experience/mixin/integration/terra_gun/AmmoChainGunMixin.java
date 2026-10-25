package com.github.edg_thexu.better_experience.mixin.integration.terra_gun;

import com.github.edg_thexu.better_experience.module.ammochain.AmmoChainManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.confluence.terra_guns.common.init.TGTags;
import org.confluence.terra_guns.common.item.gun.BaseGun;
import org.confluence.terra_guns.impl.BulletHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.confluence.terra_guns.common.combat.ShootingService", remap = false)
public abstract class AmmoChainGunMixin {
    @Inject(method = "tryShoot", at = @At("HEAD"), cancellable = true)
    private static void betterExperience$guardEmptyChain(ServerPlayer player, CallbackInfoReturnable<Boolean> callback) {
        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof BaseGun) || !AmmoChainManager.controls(player, weapon)) return;
        // The native lookup falls back to an empty bullet, so stop the firing path when the chain has nothing left.
        if (AmmoChainManager.gunProjectile(player, weapon,
                candidate -> candidate.is(TGTags.AMMO) && BulletHandler.isCompatible(player, candidate, weapon),
                ItemStack.EMPTY).isEmpty()) {
            callback.setReturnValue(false);
        }
    }

    @WrapOperation(method = "tryShoot", at = @At(value = "INVOKE",
            target = "Lorg/confluence/terra_guns/common/combat/GunFiringService;fire(Lnet/minecraft/server/level/ServerPlayer;Lorg/confluence/terra_guns/common/item/gun/BaseGun;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)I"))
    private static int betterExperience$advanceChain(ServerPlayer player, BaseGun gunItem, ItemStack weapon, ItemStack ammo,
                                                     Operation<Integer> original) {
        ItemStack prototype = ammo.isEmpty() ? ItemStack.EMPTY : ammo.copyWithCount(1);
        int fired = original.call(player, gunItem, weapon, ammo);
        if (fired > 0 && !prototype.isEmpty()) {
            AmmoChainManager.shot(player, weapon, prototype);
        }
        return fired;
    }
}
