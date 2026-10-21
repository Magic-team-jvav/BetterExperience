package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.module.ammochain.AmmoChainManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.confluence.mod.common.item.bow.BaseTerraBowItem;
import org.confluence.mod.common.item.bow.DaedalusStormbow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BaseTerraBowItem.class, remap = false)
public abstract class AmmoChainStormbowReleaseMixin {
    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void betterExperience$stopUnusedStormbowDraw(ItemStack weapon, Level level, LivingEntity shooter,
                                                        int remainingTicks, CallbackInfo callback) {
        // Rain arrows fire during onUseTick; the inherited release path only consumes an unused arrow.
        if ((Object) this instanceof DaedalusStormbow && shooter instanceof Player player
                && AmmoChainManager.controls(player, weapon)) callback.cancel();
    }
}
