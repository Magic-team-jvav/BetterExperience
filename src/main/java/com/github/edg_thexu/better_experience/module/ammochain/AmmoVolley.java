package com.github.edg_thexu.better_experience.module.ammochain;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** One consumed ammunition stack can produce several projectile-generation calls. */
public final class AmmoVolley {
    private ItemStack ammo = ItemStack.EMPTY;
    private boolean spawned;

    public void capture(ItemStack stack) { ammo = stack.copyWithCount(1); }
    public ItemStack ammo() { return ammo; }
    public boolean fired() { return spawned && !ammo.isEmpty(); }
    public void record(List<Projectile> projectiles) {
        spawned |= projectiles.stream().anyMatch(Projectile::isAddedToLevel);
    }
}
