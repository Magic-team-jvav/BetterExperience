package com.github.edg_thexu.better_experience.networks.s2c;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.attachment.EnderChestAttachment;
import com.github.edg_thexu.better_experience.init.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EnderChestItemsS2C(EnderChestAttachment attachment, TypeIndex typeIndex) implements CustomPacketPayload {
    public enum TypeIndex {
        ENDER,
        PIG,
        SAFE;

        public static TypeIndex fromId(int id) {
            if (id < 0 || id >= values().length) throw new io.netty.handler.codec.DecoderException("Invalid chest type: " + id);
            return values()[id];
        }
    }

    public static final StreamCodec<ByteBuf, EnderChestItemsS2C> STREAM_CODEC = StreamCodec.composite(
            EnderChestAttachment.STREAM_CODEC, EnderChestItemsS2C::attachment,
            ByteBufCodecs.INT.map(TypeIndex::fromId, Enum::ordinal), EnderChestItemsS2C::typeIndex,
            EnderChestItemsS2C::new
    );

    public static final Type<EnderChestItemsS2C> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Better_experience.MODID, "ender_chest_items_packet_s2c"));

    public Type<? extends CustomPacketPayload> type() {
        return TYPE;

    }

    public static void handle(EnderChestItemsS2C packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            if(packet.typeIndex == TypeIndex.ENDER) {
                player.getData(ModAttachments.ENDER_CHEST).refresh(packet.attachment);
            }else if(packet.typeIndex == TypeIndex.PIG){
                player.getData(ModAttachments.PIG_CHEST).refresh(packet.attachment);
            }else if(packet.typeIndex == TypeIndex.SAFE){
                player.getData(ModAttachments.SAFE_CHEST).refresh(packet.attachment);
            }
        });
    }
}
