package com.github.edg_thexu.better_experience.networks.s2c;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.client.voidstaff.VoidStaffClient;
import com.github.edg_thexu.better_experience.module.voidstaff.VoidCargo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record VoidCargoPacket(ResourceLocation dimension, Optional<VoidCargo> cargo) implements CustomPacketPayload {
    public static final Type<VoidCargoPacket> TYPE = new Type<>(Better_experience.space("void_cargo"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoidCargoPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, VoidCargoPacket::dimension,
            ByteBufCodecs.optional(ByteBufCodecs.fromCodecWithRegistries(VoidCargo.CODEC)), VoidCargoPacket::cargo, VoidCargoPacket::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(VoidCargoPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> VoidStaffClient.synchronize(packet.dimension, packet.cargo.orElse(null)));
    }
}
