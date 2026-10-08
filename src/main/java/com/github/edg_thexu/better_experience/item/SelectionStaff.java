package com.github.edg_thexu.better_experience.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Shared range contract for construction and destruction selection tools. */
public abstract class SelectionStaff extends Item {
    public final int maxRange;

    protected SelectionStaff(Properties properties, int maxRange) {
        super(properties);
        this.maxRange = maxRange;
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionResultHolder.success(player.getItemInHand(hand))
                : InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
