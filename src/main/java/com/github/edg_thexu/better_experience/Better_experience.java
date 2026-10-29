package com.github.edg_thexu.better_experience;

import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceGunEvents;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.intergration.terra_gun.TerraGunEvents;
import com.github.edg_thexu.better_experience.init.*;
import com.github.edg_thexu.better_experience.intergration.terra_gun.TGHelper;
import com.github.edg_thexu.better_experience.registries.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Mod(Better_experience.MODID)
public class Better_experience {
    public static final String MODID = "better_experience";

    public static final Logger LOGGER = LoggerFactory.getLogger("Better Experience");

    public static ResourceLocation space(String location){
        return ResourceLocation.fromNamespaceAndPath(Better_experience.MODID, location);
    }

    public static ResourceLocation space(String modid, String location){
        return ResourceLocation.fromNamespaceAndPath(modid, location);
    }

    public Better_experience(IEventBus modEventBus, ModContainer modContainer) {

        modEventBus.addListener(ModRegistries::newRegistry);
        ModRegistries.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.init());
        ModItems.register(modEventBus);

        ModTabs.TABS.register(modEventBus);
        ModAttachments.TYPES.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModMenus.TYPES.register(modEventBus);
        ModDataComponentTypes.TYPES.register(modEventBus);


        if(TGHelper.isLoaded()){
//            TerraGunEvents.init();
            NeoForge.EVENT_BUS.register(new TerraGunEvents());
        }
        if (ConfluenceHelper.isLoaded()) {
            NeoForge.EVENT_BUS.register(new ConfluenceGunEvents());
        }
    }

}
