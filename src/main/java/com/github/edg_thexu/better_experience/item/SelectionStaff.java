package com.github.edg_thexu.better_experience.item;

import net.minecraft.world.item.Item;

/** Shared range contract for construction and destruction selection tools. */
public abstract class SelectionStaff extends Item {
    public final int maxRange;

    protected SelectionStaff(Properties properties, int maxRange) {
        super(properties);
        this.maxRange = maxRange;
    }
}
