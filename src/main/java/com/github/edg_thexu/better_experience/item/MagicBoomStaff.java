package com.github.edg_thexu.better_experience.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class MagicBoomStaff extends Item {

    public final int maxRange;
    public MagicBoomStaff(Properties properties, int maxRange) {
        super(properties);
        this.maxRange = maxRange;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {

        // Mouse selection is handled on the client; use never dispatches destruction.
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        for (String line : List.of("info", "select", "corners", "locked", "move", "mana")) {
            tooltipComponents.add(Component.translatable("better_experience.tooltip.magic_boom_staff." + line));
        }

    }

}
