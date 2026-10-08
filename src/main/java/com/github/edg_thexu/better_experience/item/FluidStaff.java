package com.github.edg_thexu.better_experience.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public final class FluidStaff extends ShapeSelectionStaff {
    public FluidStaff(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (String line : List.of("settings", "select", "locked", "capacity")) {
            tooltip.add(Component.translatable("better_experience.fluid.tooltip." + line));
        }
    }
}
