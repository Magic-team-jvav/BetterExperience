package com.github.edg_thexu.better_experience.networks.c2s;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.module.spawning.SpawnController;
import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import com.github.edg_thexu.better_experience.networks.s2c.WorldOptionsSnapshotPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WorldOptionsPacket(int request, boolean update, WorldOptions.Values values) implements CustomPacketPayload {
    public static final Type<WorldOptionsPacket> TYPE = new Type<>(Better_experience.space("world_options"));
    public static final StreamCodec<FriendlyByteBuf, WorldOptionsPacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> {
        buf.writeVarInt(packet.request);
        buf.writeBoolean(packet.update);
        WorldOptions.Values.STREAM_CODEC.encode(buf, packet.values);
    }, buf -> new WorldOptionsPacket(buf.readVarInt(), buf.readBoolean(), WorldOptions.Values.STREAM_CODEC.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(WorldOptionsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) handle(player, packet);
        });
    }

    public static void handle(ServerPlayer player, WorldOptionsPacket packet) {
        var options = WorldOptions.get(player.server);
        boolean editable = SpawnController.canEdit(player);
        if (packet.update && editable && !options.values().equals(packet.values) && options.update(packet.values)) {
            PacketDistributor.sendToAllPlayers(new WorldOptionsSnapshotPacket(0, false, options.values()));
            // Invalidate already displayed crafting results when either optional recipe is disabled.
            for (ServerPlayer viewer : player.server.getPlayerList().getPlayers()) {
                var menu = viewer.containerMenu;
                if (menu instanceof net.minecraft.world.inventory.CraftingMenu || menu instanceof net.minecraft.world.inventory.InventoryMenu) {
                    menu.slotsChanged(viewer.getInventory());
                    menu.broadcastChanges();
                }
            }
        }
        PacketDistributor.sendToPlayer(player, new WorldOptionsSnapshotPacket(packet.request, editable, options.values()));
    }
}
