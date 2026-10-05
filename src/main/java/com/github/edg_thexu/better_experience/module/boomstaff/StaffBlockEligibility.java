package com.github.edg_thexu.better_experience.module.boomstaff;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Shared block eligibility for the destructive queue and its client preview. */
public final class StaffBlockEligibility {
    private StaffBlockEligibility() {}

    public enum Result { SKIP, DROP, NO_DROP }

    public static Result evaluate(Level level, BlockPos pos, BlockState state, ItemStack tool) {
        if (state.isAir() || state.is(BlockTags.FEATURES_CANNOT_REPLACE)
                || state.getDestroySpeed(level, pos) < 0) return Result.SKIP;
        boolean drop = true;
        if (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) {
            if (!state.is(Blocks.SNOW_BLOCK)) return Result.SKIP;
            drop = false;
        }
        if (level.getBlockEntity(pos) instanceof Container container && !container.isEmpty()) return Result.SKIP;
        return drop ? Result.DROP : Result.NO_DROP;
    }
}
