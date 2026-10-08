package com.github.edg_thexu.better_experience.module.fluidstaff;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.neoforged.neoforge.fluids.FluidStack;

public final class FluidStaffRules {
    private FluidStaffRules() {}

    public static boolean canAbsorb(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        var fluid = state.getFluidState();
        return fluid.isSource() && (state.getBlock() instanceof BucketPickup
                || state.getBlock() == fluid.getFluidType().getBlockForFluidState(level, pos, fluid).getBlock());
    }

    public static boolean canPlace(Level level, BlockPos pos, FluidStack fluid, Player player) {
        if (fluid.isEmpty() || !fluid.getFluidType().canBePlacedInLevel(level, pos, fluid)) return false;
        var state = level.getBlockState(pos);
        var placed = fluid.getFluidType().getStateForPlacement(level, pos, fluid);
        if (state.getFluidState().isSource()) return false;
        if (state.getBlock() instanceof LiquidBlockContainer container) {
            return container.canPlaceLiquid(player, level, pos, state, placed.getType());
        }
        return !state.hasBlockEntity() && (state.isAir() || state.canBeReplaced())
                && (state.getFluidState().isEmpty() || state.getFluidState().getType().isSame(placed.getType()));
    }
}
