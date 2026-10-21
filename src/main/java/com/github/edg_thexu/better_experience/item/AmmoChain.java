package com.github.edg_thexu.better_experience.item;

import com.github.edg_thexu.better_experience.data.component.AmmoChainSettings;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.networks.s2c.AmmoChainOpenPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class AmmoChain extends Item {
    public AmmoChain(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer && !player.isSpectator()) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            PacketDistributor.sendToPlayer(serverPlayer, new AmmoChainOpenPacket(slot,
                    stack.getOrDefault(ModDataComponentTypes.AMMO_CHAIN_SETTINGS.get(), AmmoChainSettings.defaults())));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return context.getPlayer() == null ? InteractionResult.PASS
                : use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var settings = stack.getOrDefault(ModDataComponentTypes.AMMO_CHAIN_SETTINGS.get(), AmmoChainSettings.defaults());
        tooltip.add(Component.translatable("better_experience.ammo_chain.tooltip.configure"));
        tooltip.add(Component.translatable("better_experience.ammo_chain.tooltip.use"));
        tooltip.add(Component.translatable("better_experience.ammo_chain.tooltip.slots", settings.entries().size()));
        tooltip.add(Component.translatable("better_experience.ammo_chain." + (settings.enabled() ? "enabled" : "disabled")));
    }
}
