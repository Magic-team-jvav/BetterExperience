package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.module.ammochain.AmmoChainManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.confluence.mod.common.init.ModDataComponentTypes;
import org.confluence.mod.common.item.crossbow.BaseTerraRepeaterItem;
import org.confluence.mod.util.RepeaterContentsComponentHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Keep the repeater's shooting scheduler while sourcing each shot from the active chain. */
@Mixin(value = BaseTerraRepeaterItem.class, remap = false)
public abstract class AmmoChainRepeaterMixin extends CrossbowItem {
    @Shadow @Final private int baseCapacity;

    protected AmmoChainRepeaterMixin(Properties properties) { super(properties); }

    @WrapOperation(method = {"onLeftClick", "lambda$onLeftClick$3"}, at = @At(value = "INVOKE",
            target = "Lorg/confluence/mod/common/item/crossbow/BaseTerraRepeaterItem;getHandler(Lnet/minecraft/world/item/ItemStack;)Lorg/confluence/mod/util/RepeaterContentsComponentHandler;"))
    private RepeaterContentsComponentHandler betterExperience$chainHandler(BaseTerraRepeaterItem repeater, ItemStack weapon,
            Operation<RepeaterContentsComponentHandler> original, @Local(argsOnly = true) Player player) {
        var handler = original.call(repeater, weapon);
        if (handler == null && AmmoChainManager.controls(player, weapon)) {
            return new RepeaterContentsComponentHandler(weapon, ModDataComponentTypes.REPEATER_CONTENTS.get(), baseCapacity);
        }
        return handler;
    }

    @WrapOperation(method = {"onLeftClick", "lambda$onLeftClick$3"}, at = @At(value = "INVOKE",
            target = "Lorg/confluence/mod/util/RepeaterContentsComponentHandler;isEmpty()Z"))
    private boolean betterExperience$chainEmpty(RepeaterContentsComponentHandler handler, Operation<Boolean> original,
            @Local(argsOnly = true) Player player, @Local(argsOnly = true) ItemStack weapon) {
        return AmmoChainManager.controls(player, weapon)
                ? AmmoChainManager.projectile(player, weapon, ItemStack.EMPTY).isEmpty() : original.call(handler);
    }

    @WrapOperation(method = "shootingPerform", at = @At(value = "INVOKE",
            target = "Lorg/confluence/mod/util/RepeaterContentsComponentHandler;extractItemList(Lnet/minecraft/world/item/ItemStack;IZ)Ljava/util/List;"))
    private List<ItemStack> betterExperience$drawChainAmmo(ItemStack weapon, int count, boolean simulate,
            Operation<List<ItemStack>> original, @Local(argsOnly = true, ordinal = 0) LivingEntity shooter) {
        if (!(shooter instanceof Player player) || !AmmoChainManager.controls(player, weapon)) {
            return original.call(weapon, count, simulate);
        }
        // Player.getProjectile applies Confluence's ammo conservation and other projectile hooks.
        ItemStack ammo = player.getProjectile(weapon);
        if (ammo.isEmpty()) return List.of();
        // The repeater already schedules multishot through its burst/concurrent counters.
        ItemStack projectile = useAmmo(weapon, simulate ? ammo.copy() : ammo, shooter, false);
        return projectile.isEmpty() ? List.of() : List.of(projectile);
    }

    @Inject(method = "shootingPerform", at = @At(value = "INVOKE",
            target = "Lorg/confluence/mod/common/item/crossbow/BaseTerraRepeaterItem;shoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Ljava/util/List;FFZLnet/minecraft/world/entity/LivingEntity;)V"),
            cancellable = true)
    private void betterExperience$stopEmptyChainShot(Level level, LivingEntity shooter, InteractionHand hand, ItemStack weapon,
            float velocity, float inaccuracy, LivingEntity target, boolean consume, CallbackInfoReturnable<Boolean> callback,
            @Local List<ItemStack> projectiles) {
        if (shooter instanceof Player player && projectiles.isEmpty() && AmmoChainManager.controls(player, weapon)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void betterExperience$skipChainReload(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) {
        ItemStack weapon = player.getItemInHand(hand);
        if (AmmoChainManager.controls(player, weapon)) callback.setReturnValue(InteractionResultHolder.consume(weapon));
    }

    @Inject(method = "tryLoadProjectiles", at = @At("HEAD"), cancellable = true)
    private void betterExperience$stopPendingChainReload(LivingEntity shooter, ItemStack weapon, CallbackInfoReturnable<Boolean> callback) {
        if (shooter instanceof Player player && AmmoChainManager.controls(player, weapon)) callback.setReturnValue(false);
    }
}
