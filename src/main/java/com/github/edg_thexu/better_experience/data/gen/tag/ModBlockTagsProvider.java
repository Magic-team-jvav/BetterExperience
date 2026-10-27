package com.github.edg_thexu.better_experience.data.gen.tag;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.init.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;


public class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Better_experience.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.CHRONOPILOT.get());
        tag(BlockTags.NEEDS_STONE_TOOL).add(
                ModBlocks.AUTO_FISH_BLOCK.get(),
                ModBlocks.AUTO_SELL_BLOCK.get()
        );


        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                ModBlocks.AUTO_FISH_BLOCK.get(),
                ModBlocks.AUTO_SELL_BLOCK.get()
        );
    }

    @Override
    public @NotNull IntrinsicTagAppender<Block> tag(@NotNull TagKey<Block> tag) {
        return super.tag(tag);
    }
}
