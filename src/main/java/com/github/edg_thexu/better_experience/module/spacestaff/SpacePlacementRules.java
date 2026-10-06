package com.github.edg_thexu.better_experience.module.spacestaff;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Shared preview/server rules. Multi-block items require their normal placement interaction. */
public final class SpacePlacementRules {
    private SpacePlacementRules() {}

    public static boolean supported(BlockState state) {
        Block block = state.getBlock();
        return !state.isAir() && block.asItem() instanceof BlockItem
                && !(block instanceof DoorBlock) && !(block instanceof BedBlock)
                && !(block instanceof DoublePlantBlock);
    }

    public static int materialCost(BlockState state) {
        if (state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) return 2;
        if (state.getBlock() instanceof SnowLayerBlock) return state.getValue(SnowLayerBlock.LAYERS);
        if (state.getBlock() instanceof CandleBlock) return state.getValue(CandleBlock.CANDLES);
        if (state.getBlock() instanceof SeaPickleBlock) return state.getValue(SeaPickleBlock.PICKLES);
        if (state.getBlock() instanceof TurtleEggBlock) return state.getValue(TurtleEggBlock.EGGS);
        return 1;
    }

    public static boolean canPlace(Level level, BlockPos pos, BlockState state) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.hasChunkAt(pos) && level.getBlockState(pos).canBeReplaced()
                && level.getBlockEntity(pos) == null && state.canSurvive(level, pos)
                && level.isUnobstructed(state, pos, net.minecraft.world.phys.shapes.CollisionContext.empty());
    }

    public static boolean isMaterial(ItemStack stack, BlockState state) {
        return !stack.isEmpty() && stack.is(state.getBlock().asItem())
                && !stack.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA)
                && !stack.has(net.minecraft.core.component.DataComponents.CONTAINER);
    }
}
