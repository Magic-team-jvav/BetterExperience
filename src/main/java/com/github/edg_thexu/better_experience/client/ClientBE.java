package com.github.edg_thexu.better_experience.client;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.config.ClientConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import com.github.edg_thexu.better_experience.client.gui.SpawnSettingsScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;


@Mod(value = Better_experience.MODID, dist = Dist.CLIENT)
public class ClientBE {

    public ClientBE(IEventBus modEventBus, ModContainer container) {

        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.init());
        container.registerExtensionPoint(IConfigScreenFactory.class, SpawnSettingsScreen::new);
    }
}
