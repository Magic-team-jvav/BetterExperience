package com.github.edg_thexu.better_experience.networks.s2c;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.client.gui.WorldOptionsScreen;
import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WorldOptionsSnapshotPacket(int request, boolean editable, WorldOptions.Values values) implements CustomPacketPayload {
    public static final Type<WorldOptionsSnapshotPacket> TYPE = new Type<>(Better_experience.space("world_options_snapshot"));
    public static final StreamCodec<FriendlyByteBuf, WorldOptionsSnapshotPacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> {
        buf.writeVarInt(packet.request);
        buf.writeBoolean(packet.editable);
        WorldOptions.Values.STREAM_CODEC.encode(buf, packet.values);
    }, buf -> new WorldOptionsSnapshotPacket(buf.readVarInt(), buf.readBoolean(), WorldOptions.Values.STREAM_CODEC.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(WorldOptionsSnapshotPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            WorldOptions.acceptClientValues(packet.values());
            WorldOptionsScreen.receive(packet);
        });
    }
}
