package com.github.edg_thexu.better_experience.networks.s2c;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.client.locator.LocatorClient;
import com.github.edg_thexu.better_experience.module.locator.StructureMarker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record LocatorMarkersPacket(List<StructureMarker> markers) implements CustomPacketPayload {
    public static final int MAX_BATCH = 64;
    public static final Type<LocatorMarkersPacket> TYPE = new Type<>(Better_experience.space("locator_markers"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LocatorMarkersPacket> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(StructureMarker.CODEC.sizeLimitedListOf(MAX_BATCH).xmap(LocatorMarkersPacket::new, LocatorMarkersPacket::markers));
    public LocatorMarkersPacket { markers = List.copyOf(markers); }
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(LocatorMarkersPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> LocatorClient.receive(packet.markers));
    }
}
