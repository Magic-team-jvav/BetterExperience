package com.github.edg_thexu.better_experience.module.autofish;

import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.intergration.terra_curios.TCHelper;
import com.github.edg_thexu.better_experience.mixed.IFishingHook;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;
import org.confluence.mod.common.init.item.AccessoryItems;
import org.confluence.terra_curio.util.TCUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class AutoFishManager {
    public static List<ItemStack> catchItems(ServerPlayer player, ServerLevel level, BlockPos machine,
            Vec3 target, ItemStack pole, ItemStack bait, ItemStack accessory) {
        return ConfluenceHelper.isLoaded()
                ? catchWithConfluence(player, level, machine, target, pole, bait, accessory)
                : catchVanilla(player, level, machine, target, pole);
    }

    /** Vanilla simulation used when Confluence is absent: same hook, no Confluence bait or pole API. */
    private static List<ItemStack> catchVanilla(ServerPlayer player, ServerLevel level, BlockPos machine,
            Vec3 target, ItemStack pole) {
        var previousHook = player.fishing;
        FishingHook hook = null;
        try {
            float power = computeFishingPower(null, pole, null, null);
            hook = new FishingHook(player, level, (int) power, 5);
            var simulation = (IFishingHook) hook;
            simulation.betterExperience$setSimulation(true);
            simulation.betterExperience$setPos(Vec3.atCenterOf(machine));
            hook.setPos(target);
            hook.nibble = 10;
            player.fishing = hook;
            hook.retrieve(pole);
            return simulation.betterExperience$getItems().stream().map(ItemStack::copy).toList();
        } finally {
            if (hook != null) hook.discard();
            player.fishing = previousHook;
        }
    }

    private static List<ItemStack> catchWithConfluence(ServerPlayer player, ServerLevel level, BlockPos machine,
            Vec3 target, ItemStack pole, ItemStack bait, ItemStack accessory) {
        var previousHook = player.fishing;
        var playerData = org.confluence.mod.mixed.IPlayer.of(player);
        ItemStack previousBait = playerData.confluence$getCurrentBait();
        FishingHook hook = null;
        try {
            float power = computeFishingPower(null, pole, bait.isEmpty() ? null : bait.getItem(), accessory);
            hook = pole.getItem() instanceof org.confluence.mod.common.item.fishing.AbstractFishingPole fishingPole
                    ? fishingPole.getHook(pole, player, level, (int) power, 5)
                    : new FishingHook(player, level, (int) power, 5);
            var simulation = (IFishingHook) hook;
            simulation.betterExperience$setSimulation(true);
            simulation.betterExperience$setPos(Vec3.atCenterOf(machine));
            hook.setPos(target);
            hook.nibble = 10;
            player.fishing = hook;
            playerData.confluence$setCurrentBait(bait.copyWithCount(1));
            if (pole.is(org.confluence.mod.common.init.item.FishingPoleItems.HOTLINE_FISHING_HOOK)
                    || bait.is(org.confluence.mod.common.init.ModTags.Items.LAVA_PROOF_BAIT)
                    || TCUtils.hasType(player, AccessoryItems.LAVAPROOF$FISHING$HOOK)) {
                org.confluence.mod.mixed.IFishingHook.of(hook).confluence$setIsLavaHook();
            }
            hook.retrieve(pole);
            return simulation.betterExperience$getItems().stream().map(ItemStack::copy).toList();
        } finally {
            if (hook != null) hook.discard();
            player.fishing = previousHook;
            playerData.confluence$setCurrentBait(previousBait);
        }
    }

    /**
     * 计算渔力
     * @param player 玩家
     * @param pole 钓竿
     * @param bait 鱼饵
     * @param curios 饰品
     * @return 渔力
     */
    public static float computeFishingPower(@Nullable Player player, @Nullable ItemStack pole, @Nullable Item bait, @Nullable ItemStack curios){

        // 人物属性
        float base = 2 + (pole == null || pole.isEmpty() ? 0 : luckOfTheSea(pole));
        if (player != null && TCHelper.isLoaded()) {
            base += TCUtils.getValue(player, AccessoryItems.FISHING$POWER);
        }

        // 鱼竿加成
        float multiplier = 0;
        if (pole != null) {
            List<AttributeModifier> mul = new ArrayList<>();
            for(var modify : pole.getAttributeModifiers().modifiers()){
                if(modify.attribute() == Attributes.LUCK){
                    if(modify.modifier().operation() == AttributeModifier.Operation.ADD_VALUE)
                        base += (float) modify.modifier().amount();
                    else if(modify.modifier().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                        mul.add(modify.modifier());
                }
            }
            for(var modify : mul){
                multiplier += (float) (modify.amount());
            }
        }


        // 鱼饵加成
        float bounds = 1 + (bait == null? 0 : (ConfluenceHelper.isLoaded() && bait instanceof org.confluence.mod.common.item.fishing.IBait value ? value.getBaitBonus() : 0));
        // 饰品加成
        float curiosBonus = 0;
        if(curios != null && !curios.isEmpty() && TCHelper.isLoaded()) {
            var component = TCUtils.getAccessoriesComponent(curios);
            if( component != null && component.contains(AccessoryItems.FISHING$POWER)) {
                curiosBonus = component.get(AccessoryItems.FISHING$POWER).get();
            }
        }
        return (base + curiosBonus * 0.1f) * (bounds + multiplier);
    }

    private static int luckOfTheSea(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (entry.getKey().is(Enchantments.LUCK_OF_THE_SEA)) return entry.getValue();
        }
        return 0;
    }

}
