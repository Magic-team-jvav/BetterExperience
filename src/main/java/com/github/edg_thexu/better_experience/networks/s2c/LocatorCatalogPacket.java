package com.github.edg_thexu.better_experience.networks.s2c;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.client.locator.LocatorClient;
import com.github.edg_thexu.better_experience.module.locator.LocatorCatalog;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

/** Structures are not a vanilla client-synchronized registry; the server supplies this catalog explicitly. */
public record LocatorCatalogPacket(int slot, List<LocatorCatalog.Entry> entries, Optional<ResourceLocation> selected) implements CustomPacketPayload {
    public static final Type<LocatorCatalogPacket> TYPE = new Type<>(Better_experience.space("locator_catalog"));
    public static final StreamCodec<FriendlyByteBuf, LocatorCatalogPacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> {
        buffer.writeVarInt(packet.slot);
        buffer.writeCollection(packet.entries, (out, entry) -> {
            out.writeResourceLocation(entry.id()); out.writeBoolean(entry.available()); out.writeVarInt(entry.marked());
        });
        buffer.writeOptional(packet.selected, FriendlyByteBuf::writeResourceLocation);
    }, buffer -> new LocatorCatalogPacket(buffer.readVarInt(), buffer.readCollection(
            FriendlyByteBuf.limitValue(ArrayList::new, LocatorCatalog.MAX_ENTRIES), in ->
            new LocatorCatalog.Entry(in.readResourceLocation(), in.readBoolean(), in.readVarInt())),
            buffer.readOptional(FriendlyByteBuf::readResourceLocation)));

    public LocatorCatalogPacket { entries = List.copyOf(entries); }
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(LocatorCatalogPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> LocatorClient.open(packet));
    }
}
