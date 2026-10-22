package com.github.edg_thexu.better_experience.networks.c2s;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.module.voidstaff.VoidStaffManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.confluence.lib.util.LibStreamCodecUtils;

import java.util.UUID;

public record VoidStaffPlacePacket(int slot, UUID token, BlockPos pos, BlockState state) implements CustomPacketPayload {
    public static final Type<VoidStaffPlacePacket> TYPE = new Type<>(Better_experience.space("void_staff_place"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoidStaffPlacePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VoidStaffPlacePacket::slot, UUIDUtil.STREAM_CODEC, VoidStaffPlacePacket::token,
            BlockPos.STREAM_CODEC, VoidStaffPlacePacket::pos, LibStreamCodecUtils.BLOCK_STATE,
            VoidStaffPlacePacket::state, VoidStaffPlacePacket::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(VoidStaffPlacePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) VoidStaffManager.place(player, packet.slot, packet.token, packet.pos, packet.state);
        });
    }
}
