package com.github.edg_thexu.better_experience.networks.c2s;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.menu.ChronopilotMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ChronopilotTimePacket(int containerId, int ticks) implements CustomPacketPayload {
    public static final Type<ChronopilotTimePacket> TYPE = new Type<>(Better_experience.space("chronopilot_time"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChronopilotTimePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ChronopilotTimePacket::containerId,
            ByteBufCodecs.VAR_INT, ChronopilotTimePacket::ticks, ChronopilotTimePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChronopilotTimePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof ChronopilotMenu menu
                    && menu.containerId == packet.containerId) {
                menu.setTime(player, packet.ticks);
            }
        });
    }
}
