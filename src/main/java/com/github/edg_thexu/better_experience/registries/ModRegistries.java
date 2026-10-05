package com.github.edg_thexu.better_experience.registries;

//import com.github.edg_thexu.better_experience.intergration.jei.JeiHelper;

import com.github.edg_thexu.better_experience.intergration.jei.JeiHelper;
import com.github.edg_thexu.better_experience.intergration.jei.JeiRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NewRegistryEvent;

public class ModRegistries {

    public static void newRegistry(NewRegistryEvent event) {
        if(JeiHelper.isLoaded()) {
            event.register(JeiRegistries.RecipeHandlerProviders.REGISTRY);
            event.register(JeiRegistries.RecipeHandlerFactoryProviders.REGISTRY);
            event.register(JeiRegistries.ItemMatcherProviders.REGISTRY);
        }
    }

    public static void register(IEventBus bus) {
//        if(JeiHelper.isLoaded()) {
//            RecipeHandlerProviderTypes.TYPES.register(bus);
//            ItemMatcherTypes.TYPES.register(bus);
//        }
    }

}
