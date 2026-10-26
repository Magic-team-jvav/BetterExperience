package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.module.ammochain.AmmoChainManager;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceGunSupport;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.confluence.mod.util.ModGunUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ModGunUtils.class, remap = false)
public abstract class AmmoChainGunAmmoMixin {
    @ModifyReturnValue(method = "getAmmo", at = @At("RETURN"))
    private static ItemStack betterExperience$chooseAmmo(ItemStack original, Player player, ItemStack weapon) {
        if (!ConfluenceGunSupport.isGun(weapon)) return original;
        return AmmoChainManager.projectile(player, weapon, original);
    }
}
