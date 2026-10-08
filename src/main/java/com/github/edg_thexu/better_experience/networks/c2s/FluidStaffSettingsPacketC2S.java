package com.github.edg_thexu.better_experience.networks.c2s;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.data.component.FluidStaffSettings;
import com.github.edg_thexu.better_experience.data.component.FluidStaffStorage;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.item.FluidStaff;
import com.github.edg_thexu.better_experience.module.fluidstaff.FluidCatalog;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FluidStaffSettingsPacketC2S(int slot, FluidStaffSettings settings) implements CustomPacketPayload {
    public static final Type<FluidStaffSettingsPacketC2S> TYPE = new Type<>(Better_experience.space("fluid_staff_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStaffSettingsPacketC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FluidStaffSettingsPacketC2S::slot,
            FluidStaffSettings.STREAM_CODEC, FluidStaffSettingsPacketC2S::settings, FluidStaffSettingsPacketC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(FluidStaffSettingsPacketC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            var stack = player.getMainHandItem();
            if (player.isSpectator() || packet.slot < 0 || packet.slot > 8 || player.getInventory().selected != packet.slot
                    || !(stack.getItem() instanceof FluidStaff)) return;
            var fluid = packet.settings.fluid();
            var storage = stack.getOrDefault(ModDataComponentTypes.FLUID_STAFF_STORAGE.get(), FluidStaffStorage.EMPTY);
            // Arbitrary component patches cannot fabricate a fluid variant that was never stored.
            var previous = stack.getOrDefault(ModDataComponentTypes.FLUID_STAFF_SETTINGS.get(), FluidStaffSettings.defaults());
            if (!FluidCatalog.isSource(fluid.getFluid()) || (!fluid.isComponentsPatchEmpty() && storage.amount(fluid) == 0
                    && !net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(fluid, previous.fluid()))) return;
            stack.set(ModDataComponentTypes.FLUID_STAFF_SETTINGS.get(), packet.settings);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        });
    }
}
