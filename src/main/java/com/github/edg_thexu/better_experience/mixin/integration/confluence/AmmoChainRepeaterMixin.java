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
import org.confluence.mod.common.component.RepeaterContents;
import org.confluence.mod.common.item.crossbow.BaseTerraRepeaterItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Keep the repeater's shooting scheduler while sourcing each shot from the active chain. */
@Mixin(value = BaseTerraRepeaterItem.class, remap = false)
public abstract class AmmoChainRepeaterMixin extends CrossbowItem {

    protected AmmoChainRepeaterMixin(Properties properties) { super(properties); }

    /** 自动连射：射击入口被拆成 onLeftClick 与 shootOnLeftClick，
     *  旧版把 RepeaterContents.isEmpty 判断写在 onLeftClick（含 lambda$onLeftClick$1）里，新版移到了 shootOnLeftClick。
     *  同时列出三个位置，任意一个命中即可在旧/新版本生效。 */
    @WrapOperation(method = {"onLeftClick", "lambda$onLeftClick$1", "shootOnLeftClick"}, at = @At(value = "INVOKE",
            target = "Lorg/confluence/mod/common/component/RepeaterContents;isEmpty()Z"))
    private boolean betterExperience$chainNotEmpty(RepeaterContents contents, Operation<Boolean> original,
            @Local(argsOnly = true) Player player, @Local(argsOnly = true) ItemStack weapon) {
        if (!original.call(contents)) return false;
        return AmmoChainManager.projectile(player, weapon, ItemStack.EMPTY).isEmpty();
    }

    @WrapOperation(method = "shootingPerform", at = @At(value = "INVOKE",
            target = "Lorg/confluence/mod/common/item/crossbow/BaseTerraRepeaterItem;extractFromContents(Lnet/minecraft/world/item/ItemStack;IZ)Ljava/util/List;"))
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
