package com.github.edg_thexu.better_experience.data.gen.loot;

import com.github.edg_thexu.better_experience.block.ChronopilotBlock;
import com.github.edg_thexu.better_experience.init.ModBlocks;
import com.google.common.collect.Iterables;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.confluence.lib.common.block.StateProperties.VerticalTwoPart;

import java.util.Collections;

public class ModBlockLootProvider extends BlockLootSubProvider {


    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags(),registries);
    }

    @Override
    protected void generate() {
        add(ModBlocks.CHRONOPILOT.get(), block -> createSinglePropConditionTable(block,
                ChronopilotBlock.PART,
                VerticalTwoPart.BASE));
        dropSelf(ModBlocks.AUTO_FISH_BLOCK.get());
        dropSelf(ModBlocks.AUTO_SELL_BLOCK.get());

    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Iterables.concat(
                getIterableFromRegister(ModBlocks.BLOCKS)

        );
    }

    private Iterable<Block> getIterableFromRegister(DeferredRegister<Block> register) {
        return register.getEntries().stream().map(holder -> (Block) holder.get()).filter(block -> map.containsKey(block.getLootTable())).toList();
    }
}
