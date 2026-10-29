package com.github.edg_thexu.better_experience.item;

import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.module.locator.LocatorCatalog;
import com.github.edg_thexu.better_experience.module.locator.LocatorNames;
import com.github.edg_thexu.better_experience.module.locator.LocatorPayment;
import com.github.edg_thexu.better_experience.module.locator.StructureLocatorManager;
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

import java.util.List;

public final class StructureLocatorOrb extends Item {
    public StructureLocatorOrb(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer serverPlayer && !player.isSpectator()) {
            var target = stack.get(ModDataComponentTypes.LOCATOR_STRUCTURE.get());
            if (player.isShiftKeyDown() || target == null) LocatorCatalog.open(serverPlayer);
            else StructureLocatorManager.getInstance().start(serverPlayer, target);
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
        var target = stack.get(ModDataComponentTypes.LOCATOR_STRUCTURE.get());
        tooltip.add(Component.translatable("better_experience.locator.tooltip.use"));
        tooltip.add(Component.translatable("better_experience.locator.tooltip.settings"));
        tooltip.add(Component.translatable(ConfluenceHelper.isLoaded()
                ? "better_experience.locator.cost_help" : "better_experience.locator.locate_free", LocatorPayment.GOLD_COST));
        tooltip.add(Component.translatable("better_experience.locator.tooltip.history"));
        if (target != null) tooltip.add(Component.translatable("better_experience.locator.target", LocatorNames.structure(target)));
    }
}
