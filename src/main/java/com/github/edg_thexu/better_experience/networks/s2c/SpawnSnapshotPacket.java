package com.github.edg_thexu.better_experience.networks.s2c;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.client.gui.SpawnSettingsScreen;
import com.github.edg_thexu.better_experience.module.spawning.SpawnEnvironment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record SpawnSnapshotPacket(int request, long token, boolean editable, double rate, double cap,
                                  ResourceLocation biome, int height, int light, long time,
                                  boolean truncated, List<Entry> entries) implements CustomPacketPayload {
    public record Entry(SpawnEnvironment.Entry spawn, boolean enabled) {}
    public static final Type<SpawnSnapshotPacket> TYPE = new Type<>(Better_experience.space("spawn_snapshot"));
    public static final StreamCodec<FriendlyByteBuf, SpawnSnapshotPacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> {
        buf.writeVarInt(packet.request);
        buf.writeLong(packet.token);
        buf.writeBoolean(packet.editable);
        buf.writeDouble(packet.rate);
        buf.writeDouble(packet.cap);
        buf.writeResourceLocation(packet.biome);
        buf.writeInt(packet.height);
        buf.writeVarInt(packet.light);
        buf.writeLong(packet.time);
        buf.writeBoolean(packet.truncated);
        buf.writeCollection(packet.entries, (out, entry) -> {
            var spawn = entry.spawn;
            out.writeResourceLocation(spawn.id());
            out.writeVarInt(spawn.weight());
            out.writeVarInt(spawn.minCount());
            out.writeVarInt(spawn.maxCount());
            out.writeBoolean(entry.enabled);
        });
    }, buf -> new SpawnSnapshotPacket(buf.readVarInt(), buf.readLong(), buf.readBoolean(), buf.readDouble(),
            buf.readDouble(), buf.readResourceLocation(), buf.readInt(), buf.readVarInt(), buf.readLong(), buf.readBoolean(),
            buf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, SpawnEnvironment.MAX_ENTRIES), in ->
                    new Entry(new SpawnEnvironment.Entry(in.readResourceLocation(), in.readVarInt(), in.readVarInt(), in.readVarInt()), in.readBoolean()))));

    public SpawnSnapshotPacket { entries = List.copyOf(entries); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SpawnSnapshotPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> SpawnSettingsScreen.receive(packet));
    }
}
