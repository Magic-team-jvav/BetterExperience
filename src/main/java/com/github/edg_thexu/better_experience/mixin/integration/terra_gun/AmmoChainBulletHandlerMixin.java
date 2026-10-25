package com.github.edg_thexu.better_experience.mixin.integration.terra_gun;

import com.github.edg_thexu.better_experience.module.ammochain.AmmoChainManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.confluence.terra_guns.common.init.TGTags;
import org.confluence.terra_guns.common.item.gun.BaseGun;
import org.confluence.terra_guns.impl.BulletHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.confluence.terra_guns.impl.BulletHandler", remap = false)
public abstract class AmmoChainBulletHandlerMixin {
    @Inject(method = {
            "getAmmo(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            "getAmmoForShot(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"},
            at = @At("RETURN"), cancellable = true)
    private static void betterExperience$selectChainAmmo(Player player, ItemStack gun, CallbackInfoReturnable<ItemStack> callback) {
        if (!(gun.getItem() instanceof BaseGun)) return;
        // Run after the native lookup so extra-inventory and ammo-selection events still execute.
        var ammo = AmmoChainManager.gunProjectile(player, gun,
                candidate -> candidate.is(TGTags.AMMO) && BulletHandler.isCompatible(player, candidate, gun), callback.getReturnValue());
        callback.setReturnValue(ammo);
    }
}
