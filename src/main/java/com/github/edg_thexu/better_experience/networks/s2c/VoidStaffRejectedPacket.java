package com.github.edg_thexu.better_experience.networks.s2c;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.client.voidstaff.VoidStaffClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record VoidStaffRejectedPacket(UUID token, BlockPos pos) implements CustomPacketPayload {
    public static final Type<VoidStaffRejectedPacket> TYPE = new Type<>(Better_experience.space("void_staff_rejected"));
    public static final StreamCodec<FriendlyByteBuf, VoidStaffRejectedPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, VoidStaffRejectedPacket::token, BlockPos.STREAM_CODEC, VoidStaffRejectedPacket::pos, VoidStaffRejectedPacket::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(VoidStaffRejectedPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> VoidStaffClient.reject(packet.token, packet.pos));
    }
}
