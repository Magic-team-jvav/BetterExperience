package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.module.ammochain.AmmoChainManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Pseudo
@Mixin(targets = "org.confluence.mod.common.item.bow.BaseTerraBowItem", remap = false)
public abstract class AmmoChainBowMixin {
    private static final String SHOOT = "shoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Ljava/util/List;FFZZFLnet/minecraft/world/entity/LivingEntity;)V";

    @Inject(method = SHOOT, at = @At("HEAD"))
    private void betterExperience$captureBowShot(CallbackInfo callback,
                                                  @Local(argsOnly = true, ordinal = 0) LivingEntity shooter,
                                                  @Local(argsOnly = true) ItemStack weapon,
                                                  @Local(argsOnly = true) List<ItemStack> projectiles,
                                                  @Share("weapon") LocalRef<ItemStack> savedWeapon,
                                                  @Share("ammo") LocalRef<ItemStack> savedAmmo) {
        if (!(shooter instanceof Player player) || !AmmoChainManager.controls(player, weapon)) return;
        for (ItemStack ammo : projectiles) {
            if (!ammo.isEmpty()) {
                savedWeapon.set(weapon.copy());
                savedAmmo.set(ammo.copyWithCount(1));
                break;
            }
        }
    }

    @WrapOperation(method = SHOOT, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true))
    private boolean betterExperience$markBowProjectile(ServerLevel level, Entity projectile, Operation<Boolean> original,
                                                        @Share("spawned") LocalBooleanRef spawned) {
        boolean added = original.call(level, projectile);
        if (added) spawned.set(true);
        return added;
    }

    @Inject(method = SHOOT, at = @At("RETURN"))
    private void betterExperience$advanceBowShot(CallbackInfo callback,
                                                  @Local(argsOnly = true, ordinal = 0) LivingEntity shooter,
                                                  @Share("weapon") LocalRef<ItemStack> savedWeapon,
                                                  @Share("ammo") LocalRef<ItemStack> savedAmmo,
                                                  @Share("spawned") LocalBooleanRef spawned) {
        if (spawned.get() && savedAmmo.get() != null && shooter instanceof Player player) {
            AmmoChainManager.shot(player, savedWeapon.get(), savedAmmo.get());
        }
    }
}
