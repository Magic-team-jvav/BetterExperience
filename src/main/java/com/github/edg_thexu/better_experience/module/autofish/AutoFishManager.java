package com.github.edg_thexu.better_experience.module.autofish;

import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.intergration.terra_curios.TCHelper;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import org.confluence.lib.util.LibEnchantmentUtils;
import org.confluence.mod.common.init.item.AccessoryItems;
import org.confluence.mod.common.item.fishing.BaitItem;
import org.confluence.terra_curio.util.TCUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class AutoFishManager {
    public static List<ItemStack> catchItems(net.minecraft.server.level.ServerPlayer player,
            net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos machine,
            net.minecraft.world.phys.Vec3 target, ItemStack pole, ItemStack bait, ItemStack accessory) {
        var previousHook = player.fishing;
        var playerData = org.confluence.mod.mixed.IPlayer.of(player);
        ItemStack previousBait = playerData.confluence$getCurrentBait();
        net.minecraft.world.entity.projectile.FishingHook hook = null;
        try {
            float power = computeFishingPower(null, pole, bait.isEmpty() ? null : bait.getItem(), accessory);
            hook = pole.getItem() instanceof org.confluence.mod.common.item.fishing.AbstractFishingPole fishingPole
                    ? fishingPole.getHook(pole, player, level, (int) power, 5)
                    : new net.minecraft.world.entity.projectile.FishingHook(player, level, (int) power, 5);
            var simulation = (com.github.edg_thexu.better_experience.mixed.IFishingHook) hook;
            simulation.betterExperience$setSimulation(true);
            simulation.betterExperience$setPos(net.minecraft.world.phys.Vec3.atCenterOf(machine));
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
        float base = 2 + (pole == null || pole.isEmpty() ? 0 : LibEnchantmentUtils.getEnchantmentLevel(Enchantments.LUCK_OF_THE_SEA, pole));
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

}
