package com.github.edg_thexu.better_experience.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class MagicBoomStaff extends SelectionStaff {
    private final boolean inventoryTools;

    public MagicBoomStaff(Properties properties, int maxRange) {
        this(properties, maxRange, false);
    }

    public MagicBoomStaff(Properties properties, int maxRange, boolean inventoryTools) {
        super(properties, maxRange);
        this.inventoryTools = inventoryTools;
    }

    public boolean usesInventoryTools() {
        return inventoryTools;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {

        // Mouse selection is handled on the client; use never dispatches destruction.
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        for (String line : List.of("info", "select", "corners", "locked", "move", "mana")) {
            String key = inventoryTools && line.equals("info")
                    ? "better_experience.tooltip.star_boom_staff.info"
                    : "better_experience.tooltip.magic_boom_staff." + line;
            tooltipComponents.add(Component.translatable(key));
        }

    }

}
