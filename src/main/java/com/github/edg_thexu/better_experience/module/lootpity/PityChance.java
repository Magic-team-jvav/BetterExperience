package com.github.edg_thexu.better_experience.module.lootpity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;

public final class PityChance {
    private PityChance() {}

    public static boolean supported(LootItemCondition condition) {
        return condition instanceof LootItemRandomChanceCondition
                || condition instanceof LootItemRandomChanceWithEnchantedBonusCondition
                || BuiltInRegistries.LOOT_CONDITION_TYPE.getKey(condition.getType()).toString().equals("confluence:difficulty_chance");
    }

    public static double probability(LootItemCondition condition, LootContext context) {
        double value;
        if (condition instanceof LootItemRandomChanceCondition chance) {
            value = chance.chance().getFloat(context);
        } else if (condition instanceof LootItemRandomChanceWithEnchantedBonusCondition chance) {
            int level = context.getParamOrNull(LootContextParams.ATTACKING_ENTITY) instanceof LivingEntity killer
                    ? EnchantmentHelper.getEnchantmentLevel(chance.enchantment(), killer) : 0;
            value = level > 0 ? chance.enchantedChance().calculate(level) : chance.unenchantedChance();
        } else {
            value = ConfluencePityChance.probability(condition, context);
        }
        return Double.isFinite(value) ? Math.clamp(value, 0, 1) : 0;
    }
}
