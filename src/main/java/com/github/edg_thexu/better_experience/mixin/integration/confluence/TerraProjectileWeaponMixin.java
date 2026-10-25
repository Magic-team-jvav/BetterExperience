package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.module.ammochain.AmmoChainManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.confluence.mod.common.item.bow.BaseTerraBowItem;
import org.confluence.mod.common.item.crossbow.BaseTerraRepeaterItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = {BaseTerraBowItem.class, BaseTerraRepeaterItem.class}, remap = false)
public class TerraProjectileWeaponMixin {
    /// The bow splits `shoot` into a public entry point and a private overload that spawns the entities,
    /// so the chain bookkeeping cannot rely on `@Share` locals and keeps the state on the item instead.
    private static final String SHOOT = "shoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Ljava/util/List;FFZLnet/minecraft/world/entity/LivingEntity;)V";
    private static final String SHOOT_WITH_SPREAD = "shoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Ljava/util/List;FFZZFLnet/minecraft/world/entity/LivingEntity;)V";

    @Unique private ItemStack betterExperience$ammoChainWeapon;
    @Unique private ItemStack betterExperience$ammoChainAmmo;
    @Unique private boolean betterExperience$ammoChainSpawned;

    @Inject(method = SHOOT, at = @At("HEAD"))
    private void betterExperience$captureShot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon,
                                              List<ItemStack> projectileItems, float velocity, float inaccuracy, boolean isCrit,
                                              LivingEntity target, CallbackInfo ci) {
        betterExperience$ammoChainWeapon = null;
        betterExperience$ammoChainAmmo = null;
        betterExperience$ammoChainSpawned = false;
        if (!(shooter instanceof Player)) return;
        betterExperience$ammoChainWeapon = weapon.copy();
        for (ItemStack ammo : projectileItems) {
            if (!ammo.isEmpty()) {
                betterExperience$ammoChainAmmo = ammo.copyWithCount(1);
                break;
            }
        }
    }

    @WrapOperation(method = {SHOOT, SHOOT_WITH_SPREAD}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true))
    private boolean betterExperience$recordProjectile(ServerLevel level, Entity projectile, Operation<Boolean> original) {
        boolean added = original.call(level, projectile);
        if (added) {
            betterExperience$ammoChainSpawned = true;
        }
        return added;
    }

    @Inject(method = SHOOT, at = @At("RETURN"))
    private void betterExperience$commitShot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon,
                                             List<ItemStack> projectileItems, float velocity, float inaccuracy, boolean isCrit,
                                             LivingEntity target, CallbackInfo ci) {
        if (betterExperience$ammoChainSpawned && shooter instanceof Player player && betterExperience$ammoChainAmmo != null) {
            AmmoChainManager.shot(player, betterExperience$ammoChainWeapon, betterExperience$ammoChainAmmo);
        }
        betterExperience$ammoChainWeapon = null;
        betterExperience$ammoChainAmmo = null;
        betterExperience$ammoChainSpawned = false;
    }
}
