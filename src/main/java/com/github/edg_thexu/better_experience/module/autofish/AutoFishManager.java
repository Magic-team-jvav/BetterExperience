package com.github.edg_thexu.better_experience.module.autofish;

import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.intergration.terra_curios.TCHelper;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import org.confluence.lib.util.EnchantmentUtil;
import org.confluence.mod.common.init.item.AccessoryItems;
import org.confluence.mod.common.item.fishing.BaitItem;
import org.confluence.terra_curio.util.TCUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class AutoFishManager {

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
        float base = 2 + EnchantmentUtil.getEnchantmentLevel(Enchantments.LUCK_OF_THE_SEA, pole); // 0 1 2 3
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
        float bounds = 1 + (bait == null? 0 : (ConfluenceHelper.isLoaded() && bait instanceof BaitItem ? (((BaitItem) bait).getBaitBonus()): 0));
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
