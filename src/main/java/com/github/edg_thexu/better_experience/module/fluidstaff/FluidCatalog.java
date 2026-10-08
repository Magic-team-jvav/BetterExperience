package com.github.edg_thexu.better_experience.module.fluidstaff;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public final class FluidCatalog {
    private FluidCatalog() {}

    public static List<FluidStack> fluids() {
        return BuiltInRegistries.FLUID.stream().filter(fluid -> fluid != Fluids.EMPTY)
                .map(fluid -> fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid).distinct()
                .map(fluid -> new FluidStack(fluid, 1)).toList();
    }

    public static boolean isSource(Fluid fluid) {
        return fluid != Fluids.EMPTY && (!(fluid instanceof FlowingFluid flowing) || flowing.getSource() == fluid);
    }
}
