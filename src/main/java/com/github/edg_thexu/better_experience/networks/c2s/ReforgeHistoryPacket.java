package com.github.edg_thexu.better_experience.networks.c2s;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.module.reforge.ReforgeHistoryManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ReforgeHistoryPacket(int containerId, ItemStack expected, String prefix) implements CustomPacketPayload {
    public static final Type<ReforgeHistoryPacket> TYPE = new Type<>(Better_experience.space("reforge_history"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReforgeHistoryPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ReforgeHistoryPacket::containerId,
            ItemStack.OPTIONAL_STREAM_CODEC, ReforgeHistoryPacket::expected,
            ByteBufCodecs.stringUtf8(128), ReforgeHistoryPacket::prefix, ReforgeHistoryPacket::new);
    @Override public Type<ReforgeHistoryPacket> type() { return TYPE; }
    public static void handle(ReforgeHistoryPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player)
                ReforgeHistoryManager.select(player, packet.containerId, packet.expected, packet.prefix);
        });
    }
}
