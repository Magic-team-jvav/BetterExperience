package com.github.edg_thexu.better_experience.mixin.accessor;

import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(LootPoolEntryContainer.class)
public interface PityLootEntryAccess {
    @Accessor("conditions") List<LootItemCondition> betterExperience$conditions();
}
