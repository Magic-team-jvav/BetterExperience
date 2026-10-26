package com.github.edg_thexu.better_experience.networks.c2s;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.module.antirage.AntiRageSettings;
import com.github.edg_thexu.better_experience.module.spawning.SpawnController;
import com.github.edg_thexu.better_experience.networks.s2c.AntiRageSnapshotPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AntiRageSettingsPacket(boolean update, boolean enabled) implements CustomPacketPayload {
    public static final Type<AntiRageSettingsPacket> TYPE = new Type<>(Better_experience.space("anti_rage_settings"));
    public static final StreamCodec<ByteBuf, AntiRageSettingsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AntiRageSettingsPacket::update, ByteBufCodecs.BOOL, AntiRageSettingsPacket::enabled, AntiRageSettingsPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(AntiRageSettingsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (packet.update) AntiRageSettings.update(player, packet.enabled);
            PacketDistributor.sendToPlayer(player, new AntiRageSnapshotPacket(CommonConfig.ANTI_RAGE_MODE.get(), SpawnController.canEdit(player)));
        });
    }
}
