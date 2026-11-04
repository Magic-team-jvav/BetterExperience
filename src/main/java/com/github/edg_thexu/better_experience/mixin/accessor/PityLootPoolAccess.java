package com.github.edg_thexu.better_experience.mixin.accessor;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(LootPool.class)
public interface PityLootPoolAccess {
    @Accessor("entries") List<LootPoolEntryContainer> betterExperience$entries();
    @Accessor("conditions") List<LootItemCondition> betterExperience$conditions();
    @Accessor("functions") List<LootItemFunction> betterExperience$functions();
}
