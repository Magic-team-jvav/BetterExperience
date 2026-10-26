package com.github.edg_thexu.better_experience.client;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = Better_experience.MODID, value = Dist.CLIENT)
public final class WorldOptionsClient {
    @SubscribeEvent
    public static void disconnected(ClientPlayerNetworkEvent.LoggingOut event) {
        WorldOptions.acceptClientValues(WorldOptions.Values.DEFAULT);
    }
}
