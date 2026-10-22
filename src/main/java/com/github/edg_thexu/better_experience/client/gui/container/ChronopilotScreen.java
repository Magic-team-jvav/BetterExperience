package com.github.edg_thexu.better_experience.client.gui.container;

import com.github.edg_thexu.better_experience.menu.ChronopilotMenu;
import com.github.edg_thexu.better_experience.networks.c2s.ChronopilotTimePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class ChronopilotScreen extends AbstractContainerScreen<ChronopilotMenu> {
    private EditBox hour;
    private EditBox minute;

    public ChronopilotScreen(ChronopilotMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 220;
        imageHeight = 172;
    }

    @Override
    protected void init() {
        super.init();
        String oldHour = hour == null ? null : hour.getValue();
        String oldMinute = minute == null ? null : minute.getValue();
        hour = timeField(leftPos + 56, Component.translatable("better_experience.chronopilot.hour"), 23);
        minute = timeField(leftPos + 124, Component.translatable("better_experience.chronopilot.minute"), 59);
        String[] current = ChronopilotMenu.formatTime(menu.time()).split(":");
        hour.setValue(oldHour == null ? current[0] : oldHour);
        minute.setValue(oldMinute == null ? current[1] : oldMinute);
        addRenderableWidget(Button.builder(Component.translatable("better_experience.chronopilot.apply"), button -> apply())
                .bounds(leftPos + 56, topPos + 98, 108, 20).build());
        int[] times = {0, 6000, 12000, 18000};
        String[] names = {"dawn", "noon", "dusk", "midnight"};
        for (int i = 0; i < times.length; i++) {
            int ticks = times[i];
            addRenderableWidget(Button.builder(Component.translatable("better_experience.chronopilot." + names[i]),
                    button -> sendTime(ticks)).bounds(leftPos + 8 + i * 51, topPos + 126, 49, 20).build());
        }
        setInitialFocus(hour);
    }

    private EditBox timeField(int x, Component label, int maximum) {
        EditBox field = new EditBox(font, x, topPos + 66, 40, 20, label);
        field.setMaxLength(2);
        field.setFilter(value -> value.isEmpty() || value.length() <= 2 && value.chars().allMatch(c -> c >= '0' && c <= '9')
                && Integer.parseInt(value) <= maximum);
        return addRenderableWidget(field);
    }

    private void apply() {
        if (hour.getValue().isEmpty() || minute.getValue().isEmpty()) return;
        sendTime(ChronopilotMenu.ticksForTime(Integer.parseInt(hour.getValue()), Integer.parseInt(minute.getValue())));
    }

    private void sendTime(int ticks) {
        PacketDistributor.sendToServer(new ChronopilotTimePacket(menu.containerId, ticks));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF0202838);
        graphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFFE0BE64);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 10, 0xFFE0BE64);
        graphics.drawCenteredString(font, Component.translatable("better_experience.chronopilot.current",
                ChronopilotMenu.formatTime(menu.time())), imageWidth / 2, 32, 0xFFFFFFFF);
        graphics.drawString(font, hour.getMessage(), 56, 53, 0xFFBFC9DA, false);
        graphics.drawString(font, minute.getMessage(), 124, 53, 0xFFBFC9DA, false);
        graphics.drawCenteredString(font, ":", imageWidth / 2, 72, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("better_experience.chronopilot.world_time"),
                imageWidth / 2, 155, 0xFFBFC9DA);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256) return super.keyPressed(key, scanCode, modifiers);
        // Text entry must not trigger the inventory-close binding.
        if (hour.isFocused() || minute.isFocused()) {
            if (key == 257 || key == 335) {
                apply();
                return true;
            }
            if (getFocused() != null && getFocused().keyPressed(key, scanCode, modifiers)) return true;
            return key != 258 || super.keyPressed(key, scanCode, modifiers);
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
