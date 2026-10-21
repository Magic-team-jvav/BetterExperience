package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.module.ammochain.AmmoChainManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.confluence.mod.common.item.bow.BaseTerraBowItem;
import org.confluence.mod.common.item.crossbow.BaseTerraRepeaterItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = {BaseTerraBowItem.class, BaseTerraRepeaterItem.class}, remap = false)
public class TerraProjectileWeaponMixin {
    @Inject(method = "shoot", at = @At("HEAD"))
    private void betterExperience$captureShot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon,
                                              List<ItemStack> projectileItems, float velocity, float inaccuracy, boolean isCrit,
                                              LivingEntity target, CallbackInfo ci,
                                              @Share("ammoChainWeapon") LocalRef<ItemStack> weaponSnapshot,
                                              @Share("ammoChainAmmo") LocalRef<ItemStack> ammoSnapshot) {
        if (shooter instanceof Player) {
            weaponSnapshot.set(weapon.copy());
            for (ItemStack ammo : projectileItems) {
                if (!ammo.isEmpty()) {
                    ammoSnapshot.set(ammo.copyWithCount(1));
                    break;
                }
            }
        }
    }

    @WrapOperation(method = "shoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true))
    private boolean betterExperience$recordProjectile(ServerLevel level, Entity projectile, Operation<Boolean> original,
                                                      @Share("ammoChainSpawned") LocalBooleanRef spawned) {
        boolean added = original.call(level, projectile);
        if (added) {
            spawned.set(true);
        }
        return added;
    }

    @Inject(method = "shoot", at = @At("RETURN"))
    private void betterExperience$commitShot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon,
                                             List<ItemStack> projectileItems, float velocity, float inaccuracy, boolean isCrit,
                                             LivingEntity target, CallbackInfo ci,
                                             @Share("ammoChainWeapon") LocalRef<ItemStack> weaponSnapshot,
                                             @Share("ammoChainAmmo") LocalRef<ItemStack> ammoSnapshot,
                                             @Share("ammoChainSpawned") LocalBooleanRef spawned) {
        if (spawned.get() && shooter instanceof Player player && ammoSnapshot.get() != null) {
            AmmoChainManager.shot(player, weaponSnapshot.get(), ammoSnapshot.get());
        }
    }
}
