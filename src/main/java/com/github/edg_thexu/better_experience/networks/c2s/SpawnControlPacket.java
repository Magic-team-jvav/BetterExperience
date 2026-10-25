package com.github.edg_thexu.better_experience.networks.c2s;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.module.spawning.SpawnController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SpawnControlPacket(int request, int action, long token, double rate, double cap,
                                 ResourceLocation entity, boolean enabled) implements CustomPacketPayload {
    public static final int QUERY = 0, SETTINGS = 1, TOGGLE = 2, RESET_FILTER = 3;
    public static final ResourceLocation NONE = Better_experience.space("none");
    public static final Type<SpawnControlPacket> TYPE = new Type<>(Better_experience.space("spawn_control"));
    public static final StreamCodec<FriendlyByteBuf, SpawnControlPacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> {
        buf.writeVarInt(packet.request);
        buf.writeVarInt(packet.action);
        buf.writeLong(packet.token);
        buf.writeDouble(packet.rate);
        buf.writeDouble(packet.cap);
        buf.writeResourceLocation(packet.entity);
        buf.writeBoolean(packet.enabled);
    }, buf -> new SpawnControlPacket(buf.readVarInt(), buf.readVarInt(), buf.readLong(), buf.readDouble(),
            buf.readDouble(), buf.readResourceLocation(), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SpawnControlPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) SpawnController.handle(player, packet);
        });
    }
}
