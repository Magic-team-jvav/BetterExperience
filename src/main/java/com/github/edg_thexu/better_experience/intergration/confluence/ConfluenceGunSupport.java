package com.github.edg_thexu.better_experience.intergration.confluence;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.confluence.mod.common.init.ModTags;
import org.confluence.mod.common.item.gun.BaseGun;
import org.confluence.mod.util.ModGunUtils;

public final class ConfluenceGunSupport {
    private ConfluenceGunSupport() {}

    public static boolean isGun(ItemStack weapon) { return weapon.getItem() instanceof BaseGun; }

    public static boolean compatible(Player player, ItemStack ammo, ItemStack weapon) {
        return !ammo.isEmpty() && ammo.is(ModTags.Items.AMMO) && ModGunUtils.isCompatible(player, ammo, weapon);
    }
}
