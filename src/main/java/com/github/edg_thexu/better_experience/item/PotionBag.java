package com.github.edg_thexu.better_experience.item;

import com.github.edg_thexu.better_experience.menu.PotionBagMenu;
import com.github.edg_thexu.better_experience.module.potionbag.PotionBagStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

public class PotionBag extends Item {
    public PotionBag(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bag = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, opener) -> new PotionBagMenu(id, inventory, slot),
                    bag.getHoverName()), buffer -> buffer.writeVarInt(slot));
        }
        return InteractionResultHolder.sidedSuccess(bag, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        var contents = PotionBagStorage.read(stack);
        lines.add(Component.translatable("better_experience.tooltip.potion_bag.info"));
        lines.add(Component.translatable("better_experience.potion_bag.capacity", contents.entries().size(), contents.capacity()));
    }
}
