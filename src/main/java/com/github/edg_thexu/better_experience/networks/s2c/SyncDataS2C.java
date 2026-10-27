package com.github.edg_thexu.better_experience.networks.s2c;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.module.autopotion.ForbiddenConfig;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public record SyncDataS2C(int dataId, Object data) implements CustomPacketPayload {
    private static final Map<Integer, Handler<Object>> handlers = new HashMap<>();

    public static final int FORBIDDEN_CONFIG = register(ForbiddenConfig.CODEC.codec(), ForbiddenConfig::handleServer);

    public static final Type<SyncDataS2C> TYPE = new Type<>(Better_experience.space("sync_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDataS2C> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SyncDataS2C decode(RegistryFriendlyByteBuf buffer) {
            int dataId = buffer.readVarInt();
            return new SyncDataS2C(dataId, buffer.readJsonWithCodec(handlers.get(dataId).codec));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, SyncDataS2C value) {
            buffer.writeVarInt(value.dataId);
            buffer.writeJsonWithCodec(handlers.get(value.dataId).codec, value.data);
        }
    };

    @SuppressWarnings("unchecked")
    private static <T> int register(Codec<T> codec, Consumer<T> consumer) {
        int id = handlers.size();
        handlers.put(id, (Handler<Object>) new Handler<>(codec, consumer));
        return id;
    }

    @Override
    public @NotNull Type<SyncDataS2C> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> handlers.get(dataId).consumer.accept(data)).exceptionally(e -> null);
    }

    public static <T> void syncAll(int dataId, T value) {
        PacketDistributor.sendToAllPlayers(new SyncDataS2C(dataId, value));
    }

    public static <T> void sync(ServerPlayer player, int dataId, T value) {
        PacketDistributor.sendToPlayer(player, new SyncDataS2C(dataId, value));
    }

    public static void syncForbiddenConfig(ServerPlayer player) {
        sync(player, FORBIDDEN_CONFIG, ForbiddenConfig.getInstance());
    }

    public static void syncForbiddenConfig() {
        syncAll(FORBIDDEN_CONFIG, ForbiddenConfig.getInstance());
    }

    public record Handler<T>(Codec<T> codec, Consumer<T> consumer) {}
}
