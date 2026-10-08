package com.github.edg_thexu.better_experience.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** The fluid identity includes components, so modded variants remain distinct. */
public record FluidStaffSettings(boolean placing, FluidStack fluid) {
    public static final Codec<FluidStaffSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("placing").forGetter(FluidStaffSettings::placing),
            FluidStack.fixedAmountCodec(1).fieldOf("fluid").forGetter(FluidStaffSettings::fluid)
    ).apply(instance, FluidStaffSettings::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStaffSettings> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public FluidStaffSettings {
        if (fluid.isEmpty()) throw new IllegalArgumentException("Select a nonempty fluid");
        fluid = fluid.copyWithAmount(1);
    }

    @Override
    public FluidStack fluid() { return fluid.copy(); }

    @Override
    public boolean equals(Object other) {
        return other instanceof FluidStaffSettings settings && placing == settings.placing
                && FluidStack.isSameFluidSameComponents(fluid, settings.fluid);
    }

    @Override
    public int hashCode() { return 31 * Boolean.hashCode(placing) + FluidStack.hashFluidAndComponents(fluid); }

    public static FluidStaffSettings defaults() { return new FluidStaffSettings(false, new FluidStack(Fluids.WATER, 1)); }
}
