package com.github.edg_thexu.better_experience.module.worldoptions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class GrowthController {
    private GrowthController() {}

    public static void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, Operation<Void> original) {
        var block = state.getBlock();
        boolean sapling = block instanceof SaplingBlock || block instanceof BambooSaplingBlock || state.is(BlockTags.SAPLINGS);
        boolean crop = block instanceof CropBlock || block instanceof StemBlock || block instanceof CocoaBlock
                || block instanceof SweetBerryBushBlock || block instanceof NetherWartBlock || block instanceof SugarCaneBlock
                || block instanceof CactusBlock || state.is(BlockTags.CROPS);
        if (!sapling && !crop) {
            original.call(state, level, pos, random);
            return;
        }
        var options = WorldOptions.get(level.getServer()).values();
        double multiplier = sapling ? options.saplings() : options.crops();
        if (multiplier == 1.0) {
            original.call(state, level, pos, random);
            return;
        }
        int attempts = (int) multiplier;
        double remainder = multiplier - attempts;
        if (remainder > 0.0 && random.nextDouble() < remainder) attempts++;
        for (int i = 0; i < attempts; i++) {
            // Growth can replace the block (tree, attached stem) or change its age after every attempt.
            BlockState current = level.getBlockState(pos);
            if (!current.is(block) || !current.isRandomlyTicking()) break;
            original.call(current, level, pos, random);
        }
    }
}
