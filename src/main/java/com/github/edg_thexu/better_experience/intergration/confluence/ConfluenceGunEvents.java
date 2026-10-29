package com.github.edg_thexu.better_experience.intergration.confluence;

import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import org.confluence.mod.api.event.GunEvent;

/** Registered only when Confluence is present, mirroring the ammo saver for guns. */
public class ConfluenceGunEvents {

    @SubscribeEvent
    public void keepLargeAmmo(GunEvent.ShrinkBullet event) {
        ItemStack ammo = event.getBulletStack();
        if (ammo != null && WorldOptions.keepsAmmo(event.getPlayer().level(), ammo)) event.setInfinity(true);
    }
}
