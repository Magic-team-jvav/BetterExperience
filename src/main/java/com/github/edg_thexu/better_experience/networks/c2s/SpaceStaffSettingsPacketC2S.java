package com.github.edg_thexu.better_experience.networks.c2s;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.item.SpaceStaff;
import com.github.edg_thexu.better_experience.module.spacestaff.SpacePlacementRules;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SpaceStaffSettingsPacketC2S(int slot, SpaceStaffSettings settings) implements CustomPacketPayload {
    public static final Type<SpaceStaffSettingsPacketC2S> TYPE = new Type<>(Better_experience.space("space_staff_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpaceStaffSettingsPacketC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpaceStaffSettingsPacketC2S::slot,
            SpaceStaffSettings.STREAM_CODEC, SpaceStaffSettingsPacketC2S::settings, SpaceStaffSettingsPacketC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SpaceStaffSettingsPacketC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (packet.slot < 0 || packet.slot > 8 || player.getInventory().selected != packet.slot
                    || !(player.getMainHandItem().getItem() instanceof SpaceStaff)
                    || !SpacePlacementRules.supported(packet.settings.state())) return;
            player.getMainHandItem().set(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get(), packet.settings);
            player.containerMenu.broadcastChanges();
        });
    }
}
