package com.github.edg_thexu.better_experience.networks.s2c;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.client.ammochain.AmmoChainClient;
import com.github.edg_thexu.better_experience.data.component.AmmoChainSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AmmoChainOpenPacket(int slot, AmmoChainSettings settings) implements CustomPacketPayload {
    public static final Type<AmmoChainOpenPacket> TYPE = new Type<>(Better_experience.space("ammo_chain_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AmmoChainOpenPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AmmoChainOpenPacket::slot, AmmoChainSettings.STREAM_CODEC,
            AmmoChainOpenPacket::settings, AmmoChainOpenPacket::new);
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(AmmoChainOpenPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> AmmoChainClient.open(packet.slot, packet.settings));
    }
}
