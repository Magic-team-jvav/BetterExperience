package com.github.edg_thexu.better_experience.intergration.terra_gun;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.confluence.terra_guns.common.init.TGTags;
import org.confluence.terra_guns.common.item.gun.BaseGun;
import org.confluence.terra_guns.impl.BulletHandler;

/** Loaded only after the optional TerraGuns mod has been detected. */
public final class AmmoChainGunSupport {
    private AmmoChainGunSupport() {}
    public static boolean isGun(ItemStack weapon) { return weapon.getItem() instanceof BaseGun; }
    public static boolean compatible(Player player, ItemStack ammo, ItemStack weapon) {
        return !ammo.isEmpty() && ammo.is(TGTags.AMMO) && BulletHandler.isCompatible(player, ammo, weapon);
    }
}
