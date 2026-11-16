package com.github.edg_thexu.better_experience.networks.s2c;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.module.autopotion.ForbiddenConfig;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncDataS2C(ForbiddenConfig config) implements CustomPacketPayload {
	// Preserve the discriminator used by previously released clients.
	private static final int FORBIDDEN_CONFIG = 0;
	public static final Type<SyncDataS2C> TYPE = new Type<>(Better_experience.space("sync_data"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncDataS2C> STREAM_CODEC =
			new StreamCodec<>() {
				@Override
				public SyncDataS2C decode(RegistryFriendlyByteBuf buffer) {
					int dataId = buffer.readVarInt();
					if (dataId != FORBIDDEN_CONFIG) {
						throw new DecoderException("Unknown potion config sync ID: " + dataId);
					}
					return new SyncDataS2C(buffer.readJsonWithCodec(ForbiddenConfig.CODEC.codec()));
				}

				@Override
				public void encode(RegistryFriendlyByteBuf buffer, SyncDataS2C packet) {
					buffer.writeVarInt(FORBIDDEN_CONFIG);
					buffer.writeJsonWithCodec(ForbiddenConfig.CODEC.codec(), packet.config());
				}
			};

	@Override
	public Type<SyncDataS2C> type() {
		return TYPE;
	}

	public void handle(IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.connection().isConnected()) {
				ForbiddenConfig.applyClient(config);
			}
		});
	}

	public static void syncForbiddenConfig(ServerPlayer player) {
		PacketDistributor.sendToPlayer(player, new SyncDataS2C(ForbiddenConfig.serverSnapshot()));
	}

	public static void syncForbiddenConfig() {
		PacketDistributor.sendToAllPlayers(new SyncDataS2C(ForbiddenConfig.serverSnapshot()));
	}
}
