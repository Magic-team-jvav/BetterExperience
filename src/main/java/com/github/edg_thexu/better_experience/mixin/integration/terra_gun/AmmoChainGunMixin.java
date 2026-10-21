package com.github.edg_thexu.better_experience.mixin.integration.terra_gun;

import com.github.edg_thexu.better_experience.module.ammochain.AmmoChainManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import org.confluence.terra_guns.common.init.TGTags;
import org.confluence.terra_guns.impl.BulletHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.function.Consumer;

@Pseudo
@Mixin(targets = "org.confluence.terra_guns.common.item.gun.BaseGun", remap = false)
public abstract class AmmoChainGunMixin {
    @Inject(method = "shoot(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true)
    private void betterExperience$rememberChainAmmo(ServerPlayer player, ItemStack ammo, ItemStack gun, CallbackInfo callback,
            @Share("ammoChainPrototype") LocalRef<ItemStack> prototype,
            @Share("ammoChainSpawned") LocalBooleanRef spawned) {
        prototype.set(ammo.copyWithCount(1));
        spawned.set(false);
        // The native server packet substitutes EMPTY_BULLET after an empty lookup, so guard the actual firing path too.
        if (AmmoChainManager.controls(player, gun) && AmmoChainManager.gunProjectile(player, gun,
                candidate -> candidate.is(TGTags.AMMO) && BulletHandler.isCompatible(player, candidate, gun), ammo).isEmpty()) {
            callback.cancel();
        }
    }

    @WrapOperation(method = "shoot(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;forEach(Ljava/util/function/Consumer;)V"))
    private void betterExperience$observeChainShot(ArrayList<Projectile> projectiles, Consumer<? super Projectile> consumer,
            Operation<Void> original, @Share("ammoChainSpawned") LocalBooleanRef spawned) {
        original.call(projectiles, (Consumer<Projectile>) projectile -> {
            boolean alreadyAdded = projectile.isAddedToLevel();
            consumer.accept(projectile);
            if (!alreadyAdded && projectile.isAddedToLevel()) spawned.set(true);
        });
    }

    @Inject(method = "shoot(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("RETURN"))
    private void betterExperience$advanceChain(ServerPlayer player, ItemStack ammo, ItemStack gun, CallbackInfo callback,
            @Share("ammoChainPrototype") LocalRef<ItemStack> prototype,
            @Share("ammoChainSpawned") LocalBooleanRef spawned) {
        if (spawned.get()) AmmoChainManager.shot(player, gun, prototype.get());
    }
}
