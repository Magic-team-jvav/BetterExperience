package com.github.edg_thexu.better_experience.module.lootpity;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.confluence.lib.util.LibUtils;
import org.confluence.mod.common.loot.DifficultyChanceLootItemCondition;

/** Loaded only when evaluating Confluence's registered difficulty condition. */
final class ConfluencePityChance {
    private ConfluencePityChance() {}

    static double probability(LootItemCondition condition, LootContext context) {
        DifficultyChanceLootItemCondition chance = (DifficultyChanceLootItemCondition) condition;
        return LibUtils.isAtLeastExpert(context.getLevel()) ? chance.expertChance() : chance.normalChance();
    }
}
