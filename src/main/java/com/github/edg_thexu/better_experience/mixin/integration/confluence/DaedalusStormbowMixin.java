package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.module.ammochain.AmmoChainManager;
import com.github.edg_thexu.better_experience.module.ammochain.AmmoVolley;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.confluence.mod.api.IGeneration;
import org.confluence.mod.common.item.bow.DaedalusStormbow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Mixin(value = DaedalusStormbow.class, remap = false)
public abstract class DaedalusStormbowMixin {
    @Unique private static final ThreadLocal<AmmoVolley> BETTER_EXPERIENCE_VOLLEY = new ThreadLocal<>();

    @WrapMethod(method = "onUseTick")
    private void betterExperience$trackVolley(Level level, LivingEntity shooter, ItemStack weapon, int remainingTicks,
                                              Operation<Void> original) {
        if (level.isClientSide || remainingTicks % 4 != 0 || !(shooter instanceof Player player) || !AmmoChainManager.controls(player, weapon)) {
            original.call(level, shooter, weapon, remainingTicks);
            return;
        }
        if (AmmoChainManager.projectile(player, weapon, ItemStack.EMPTY).isEmpty()) return;
        AmmoVolley previous = BETTER_EXPERIENCE_VOLLEY.get();
        AmmoVolley volley = new AmmoVolley();
        ItemStack snapshot = weapon.copy();
        BETTER_EXPERIENCE_VOLLEY.set(volley);
        try {
            original.call(level, shooter, weapon, remainingTicks);
            if (volley.fired()) AmmoChainManager.shot(player, snapshot, volley.ammo());
        } finally {
            if (previous == null) BETTER_EXPERIENCE_VOLLEY.remove();
            else BETTER_EXPERIENCE_VOLLEY.set(previous);
        }
    }

    @WrapOperation(method = "onUseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getProjectile(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;", remap = true))
    private ItemStack betterExperience$captureAmmo(LivingEntity shooter, ItemStack weapon, Operation<ItemStack> original) {
        ItemStack ammo = original.call(shooter, weapon);
        AmmoVolley volley = BETTER_EXPERIENCE_VOLLEY.get();
        if (volley != null) volley.capture(ammo);
        return ammo;
    }

    @WrapOperation(method = "onUseTick", at = @At(value = "INVOKE", target = "Lorg/confluence/mod/api/IGeneration;genProjectile(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;FLjava/util/function/Supplier;)I"))
    private int betterExperience$trackGeneration(IGeneration generation, LivingEntity shooter, ItemStack weapon, float velocity,
                                                  Supplier<? extends Projectile> supplier, Operation<Integer> original) {
        AmmoVolley volley = BETTER_EXPERIENCE_VOLLEY.get();
        if (volley == null) return original.call(generation, shooter, weapon, velocity, supplier);
        List<Projectile> projectiles = new ArrayList<>();
        int generated = original.call(generation, shooter, weapon, velocity, (Supplier<Projectile>) () -> {
            Projectile projectile = supplier.get();
            if (projectile != null) projectiles.add(projectile);
            return projectile;
        });
        volley.record(projectiles);
        return generated;
    }

}
