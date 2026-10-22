package com.github.edg_thexu.better_experience.networks.c2s;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.module.voidstaff.VoidStaffManager;
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

public record VoidStaffStatePacket(int slot, UUID token, BlockState state) implements CustomPacketPayload {
    public static final Type<VoidStaffStatePacket> TYPE = new Type<>(Better_experience.space("void_staff_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoidStaffStatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VoidStaffStatePacket::slot, UUIDUtil.STREAM_CODEC, VoidStaffStatePacket::token,
            LibStreamCodecUtils.BLOCK_STATE, VoidStaffStatePacket::state, VoidStaffStatePacket::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(VoidStaffStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) VoidStaffManager.changeState(player, packet.slot, packet.token, packet.state);
        });
    }
}
