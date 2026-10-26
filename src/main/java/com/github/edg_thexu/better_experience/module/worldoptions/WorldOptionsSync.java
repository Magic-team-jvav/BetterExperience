package com.github.edg_thexu.better_experience.module.worldoptions;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.networks.s2c.WorldOptionsSnapshotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Better_experience.MODID)
public final class WorldOptionsSync {
    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) { sync(event.getEntity()); }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) { sync(event.getEntity()); }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) { sync(event.getEntity()); }

    private static void sync(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new WorldOptionsSnapshotPacket(0, false, WorldOptions.get(serverPlayer.server).values()));
        }
    }
}
