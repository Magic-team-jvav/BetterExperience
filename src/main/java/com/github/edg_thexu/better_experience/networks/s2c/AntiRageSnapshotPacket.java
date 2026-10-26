package com.github.edg_thexu.better_experience.networks.s2c;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.client.gui.AntiRagePrompt;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AntiRageSnapshotPacket(boolean enabled, boolean editable) implements CustomPacketPayload {
    public static final Type<AntiRageSnapshotPacket> TYPE = new Type<>(Better_experience.space("anti_rage_snapshot"));
    public static final StreamCodec<ByteBuf, AntiRageSnapshotPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AntiRageSnapshotPacket::enabled, ByteBufCodecs.BOOL, AntiRageSnapshotPacket::editable, AntiRageSnapshotPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(AntiRageSnapshotPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> AntiRagePrompt.receive(packet));
    }
}
