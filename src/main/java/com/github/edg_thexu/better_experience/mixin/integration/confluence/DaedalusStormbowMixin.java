package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.module.ammochain.AmmoChainManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import org.confluence.mod.common.item.bow.DaedalusStormbow;
import org.confluence.terraentity.api.entity.IGeneration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(value = DaedalusStormbow.class, remap = false)
public class DaedalusStormbowMixin {
    @Unique
    private static final ThreadLocal<Consumer<ItemStack>> BETTER_EXPERIENCE_AMMO_CAPTURE = new ThreadLocal<>();

    @WrapOperation(method = "lambda$onUseTick$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getProjectile(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;", remap = true))
    private ItemStack betterExperience$captureAmmo(LivingEntity shooter, ItemStack weapon, Operation<ItemStack> original) {
        ItemStack ammo = original.call(shooter, weapon);
        Consumer<ItemStack> capture = BETTER_EXPERIENCE_AMMO_CAPTURE.get();
        if (capture != null && !ammo.isEmpty()) {
            capture.accept(ammo);
        }
        return ammo;
    }

    @WrapOperation(method = "onUseTick", at = @At(value = "INVOKE", target = "Lorg/confluence/terraentity/api/entity/IGeneration;genProjectile(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;FLjava/util/function/Supplier;)V"))
    private void betterExperience$trackGeneration(IGeneration generation, LivingEntity shooter, ItemStack weapon, float velocity,
                                                  Supplier<? extends Projectile> supplier, Operation<Void> original) {
        if (!(shooter instanceof Player player) || shooter.level().isClientSide()) {
            original.call(generation, shooter, weapon, velocity, supplier);
            return;
        }
        if (AmmoChainManager.controls(player, weapon)
                && AmmoChainManager.projectile(player, weapon, ItemStack.EMPTY).isEmpty()) return;
        ItemStack weaponSnapshot = weapon.copy();
        List<ItemStack> ammoSnapshots = new ArrayList<>(1);
        List<Projectile> projectiles = new ArrayList<>(1);
        Consumer<ItemStack> previousCapture = BETTER_EXPERIENCE_AMMO_CAPTURE.get();
        BETTER_EXPERIENCE_AMMO_CAPTURE.set(ammo -> ammoSnapshots.add(ammo.copyWithCount(1)));
        try {
            Supplier<Projectile> trackedSupplier = () -> {
                Projectile projectile = supplier.get();
                if (projectile != null) {
                    projectiles.add(projectile);
                }
                return projectile;
            };
            original.call(generation, shooter, weapon, velocity, trackedSupplier);
            if (!ammoSnapshots.isEmpty() && projectiles.stream().anyMatch(Projectile::isAddedToLevel)) {
                AmmoChainManager.shot(player, weaponSnapshot, ammoSnapshots.getFirst());
            }
        } finally {
            if (previousCapture == null) {
                BETTER_EXPERIENCE_AMMO_CAPTURE.remove();
            } else {
                BETTER_EXPERIENCE_AMMO_CAPTURE.set(previousCapture);
            }
        }
    }
}
