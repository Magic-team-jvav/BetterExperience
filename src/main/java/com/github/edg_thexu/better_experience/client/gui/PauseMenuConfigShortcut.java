package com.github.edg_thexu.better_experience.client.gui;

import com.github.edg_thexu.better_experience.Better_experience;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = Better_experience.MODID, value = Dist.CLIENT)
public final class PauseMenuConfigShortcut {
    private static final int GAP = 4;
    private static final String TITLE = "better_experience.gui.open_config";

    private PauseMenuConfigShortcut() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen pause) || !pause.showsPauseMenu()) return;

        Button modsButton = null;
        for (var listener : event.getListenersList()) {
            if (listener instanceof Button button
                    && button.getMessage().getContents() instanceof TranslatableContents text
                    && text.getKey().equals("fml.menu.mods")) {
                modsButton = button;
                break;
            }
        }
        if (modsButton == null) return;

        var container = ModList.get().getModContainerById(Better_experience.MODID).orElse(null);
        if (container == null) return;

        // Keep the original row width, with a square shortcut at its right edge.
        int size = modsButton.getHeight();
        int originalWidth = modsButton.getWidth();
        if (originalWidth <= size + GAP) return;
        int x = modsButton.getX() + originalWidth - size;
        modsButton.setWidth(originalWidth - size - GAP);
        event.addListener(Button.builder(Component.literal("BE"), button ->
                        Minecraft.getInstance().setScreen(new SpawnSettingsScreen(container, pause)))
                .bounds(x, modsButton.getY(), size, size)
                .tooltip(Tooltip.create(Component.translatable(TITLE)))
                .createNarration(supplier -> Component.translatable(TITLE))
                .build());
    }
}
