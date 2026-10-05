package com.github.edg_thexu.better_experience.client.event;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.client.StaffKeyMappings;
import com.github.edg_thexu.better_experience.client.buffer.StaffHighlightShader;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import java.io.IOException;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import com.github.edg_thexu.better_experience.client.gui.container.AutoFishScreen;
import com.github.edg_thexu.better_experience.client.gui.container.PotionBagScreen;
import com.github.edg_thexu.better_experience.client.renderer.AutoFishBlockRenderer;
import com.github.edg_thexu.better_experience.client.renderer.AutoSellBlockRenderer;
import com.github.edg_thexu.better_experience.init.ModBlocks;
import com.github.edg_thexu.better_experience.init.ModMenus;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Better_experience.MODID,value = Dist.CLIENT)
public class ClientModEvent {

    @SubscribeEvent
    public static void registerStaffShader(RegisterShadersEvent event) throws IOException {
        StaffHighlightShader.register(event);
    }

    @SubscribeEvent
    public static void registerStaffKeys(RegisterKeyMappingsEvent event) {
        event.register(StaffKeyMappings.SELECT_MODIFIER);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.AUTO_FISH_MENU.get(), AutoFishScreen::new);
        event.register(ModMenus.POTION_BAG_MENU.get(), PotionBagScreen::new);

    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.AUTO_FISH_BLOCK_ENTITY.get(), c -> new AutoFishBlockRenderer());
        if(ConfluenceHelper.isLoaded()) {
            event.registerBlockEntityRenderer(ModBlocks.AUTO_SELL_BLOCK_ENTITY.get(), c -> new AutoSellBlockRenderer());

        }

    }
}
