package com.github.edg_thexu.better_experience.module.potionbag;

import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.module.autopotion.ForbiddenConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import org.confluence.mod.common.block.functional.EffectiveCandleBlock;
import org.confluence.mod.common.init.ModEffects;
import org.confluence.mod.common.init.block.FunctionalBlocks;
import org.confluence.mod.common.item.potion.AbstractPotionItem;
import org.confluence.mod.common.item.potion.EffectPotionItem;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.List;

/** Shared item rules; deliberately has no client or GUI dependencies. */
public final class PotionEffectRules {
    private PotionEffectRules() {}

    public static boolean isPotion(ItemStack stack) {
        return stack.getItem() instanceof PotionItem
                || ConfluenceHelper.isLoaded() && stack.getItem() instanceof AbstractPotionItem;
    }

    private static boolean allowed(MobEffectInstance instance, boolean includeHarmful) {
        Holder<MobEffect> effect = instance.getEffect();
        var config = ForbiddenConfig.getInstance();
        if (effect.value().isInstantenous() || config.isEffectForbidden(effect.value(), instance.getAmplifier())
                || effect.getKey() != null && config.isModForbidden(effect.getKey().location().getNamespace())) return false;
        return includeHarmful || effect.value().getCategory() != MobEffectCategory.HARMFUL
                || ConfluenceHelper.isLoaded() && (effect == ModEffects.WATER_CANDLE || effect == ModEffects.PEACE_CANDLE);
    }

    public static List<Pair<Holder<MobEffect>, Integer>> effects(ItemStack stack, boolean ignoreCount) {
        return effects(stack, ignoreCount, false);
    }

    public static List<Pair<Holder<MobEffect>, Integer>> potionEffects(ItemStack stack) {
        return isPotion(stack) ? effects(stack, true, true) : List.of();
    }

    private static List<Pair<Holder<MobEffect>, Integer>> effects(ItemStack stack, boolean ignoreCount, boolean includeHarmful) {
        var effects = new ArrayList<Pair<Holder<MobEffect>, Integer>>();
        if (stack.isEmpty() || ForbiddenConfig.getInstance().isItemForbidden(stack.getItem())) return effects;
        if (ConfluenceHelper.isLoaded()) {
            if (stack.getItem() instanceof EffectiveCandleBlock.BItem candle) {
                for (var data : candle.effectData) if (allowed(data.create(), includeHarmful)) effects.add(new Pair<>(data.effect(), data.amplifier()));
                return effects;
            }
            if (stack.getItem() instanceof BlockItem block && block.getBlock() == FunctionalBlocks.LIFE_CAMPFIRE.get()) {
                effects.add(new Pair<>(ModEffects.COZY_FIRE, 0));
                return effects;
            }
        }
        if (!ignoreCount && stack.getCount() < CommonConfig.AUTO_POTION_STACK_SIZE.get()) return effects;
        if (ConfluenceHelper.isLoaded() && stack.getItem() instanceof EffectPotionItem potion) {
            if (allowed(potion.data.create(), includeHarmful)) effects.add(new Pair<>(potion.data.effect(), potion.data.amplifier()));
            return effects;
        }
        var food = stack.get(DataComponents.FOOD);
        if (food != null) for (var possible : food.effects()) {
            var effect = possible.effect();
            if (allowed(effect, includeHarmful)) effects.add(new Pair<>(effect.getEffect(), effect.getAmplifier()));
        }
        if (stack.getItem() instanceof PotionItem) {
            var contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null) for (var effect : contents.getAllEffects()) {
                if (allowed(effect, includeHarmful)) effects.add(new Pair<>(effect.getEffect(), effect.getAmplifier()));
            }
        }
        return effects;
    }
}
