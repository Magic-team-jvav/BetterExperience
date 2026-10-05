package com.github.edg_thexu.better_experience.networks.s2c;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.init.ModAttachments;
import com.github.edg_thexu.better_experience.module.autopotion.PlayerInventoryManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientBoundConfigPacket(int message) implements CustomPacketPayload {

    public static final StreamCodec<ByteBuf, ClientBoundConfigPacket> STREAM_CODEC = ByteBufCodecs.INT.map(
            ClientBoundConfigPacket::new, ClientBoundConfigPacket::message
    ).cast();

    public static final Type<ClientBoundConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Better_experience.MODID, "client_bound_config_packet_s2c"));

    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;

    }

    public static void handle(ClientBoundConfigPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            if(packet.message == 0) {
                PlayerInventoryManager.getInstance().serverOpenAutoPotion = false;
            }else if(packet.message == 1) {
                PlayerInventoryManager.getInstance().serverOpenAutoPotion = true;
            } else if(packet.message == 2) { // 更好重铸开启
                player.getData(ModAttachments.TEMP_DATA).setBetterReforge(true);
            } else if(packet.message == 3) { // 更好重铸关闭
                player.getData(ModAttachments.TEMP_DATA).setBetterReforge(false);
            }

        });
    }

    public static void sync(ServerPlayer player){
        PacketDistributor.sendToPlayer(player, new ClientBoundConfigPacket(CommonConfig.AUTO_POTION_OPEN.get()? 1 : 0));
    }

    public static void syncAll(){
        PacketDistributor.sendToAllPlayers(new ClientBoundConfigPacket(CommonConfig.AUTO_POTION_OPEN.get()? 1 : 0));
    }
}
