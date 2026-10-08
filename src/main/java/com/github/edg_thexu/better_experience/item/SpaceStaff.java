package com.github.edg_thexu.better_experience.item;

import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.module.spacestaff.SpaceShape;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public final class SpaceStaff extends ShapeSelectionStaff {
    public SpaceStaff(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var settings = stack.get(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get());
        for (String line : List.of("settings", "select", "locked", "materials")) {
            if (line.equals("select") && settings != null && settings.shape() == SpaceShape.BEZIER) {
                tooltip.add(Component.translatable("better_experience.space.bezier_help"));
                continue;
            }
            tooltip.add(Component.translatable("better_experience.space.tooltip." + line));
        }
    }
}
