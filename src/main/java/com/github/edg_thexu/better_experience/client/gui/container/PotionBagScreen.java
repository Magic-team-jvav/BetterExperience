package com.github.edg_thexu.better_experience.client.gui.container;

import com.github.edg_thexu.better_experience.menu.PotionBagMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PotionBagScreen extends AbstractContainerScreen<PotionBagMenu> {
    private Button previous;
    private Button next;
    private Button collect;

    public PotionBagScreen(PotionBagMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 200;
        inventoryLabelY = 106;
    }

    @Override protected void init() {
        super.init();
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> action(0))
                .bounds(leftPos + 8, topPos + 86, 22, 18).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> action(1))
                .bounds(leftPos + 146, topPos + 86, 22, 18).build());
        collect = addRenderableWidget(Button.builder(Component.empty(), button -> action(2))
                .bounds(leftPos + 88, topPos + 5, 80, 18).build());
        containerTick();
    }

    private void action(int action) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
    }

    @Override protected void containerTick() {
        previous.active = menu.page() > 0;
        next.active = menu.page() + 1 < menu.pages();
        collect.setMessage(Component.translatable(menu.autoCollect() ? "better_experience.potion_bag.collect_on" : "better_experience.potion_bag.collect_off"));
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFCDC8BB);
        graphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF766958);
        for (var slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF55524D);
            graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xFF99958D);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 10, 0x333333, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x333333, false);
        graphics.drawCenteredString(font, Component.literal((menu.page() + 1) + "/" + menu.pages()), imageWidth / 2, 91, 0xFFFFFF);
        // Draw aggregate quantities over the one-item presentation stacks.
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        for (int i = 0; i < PotionBagMenu.PAGE_SIZE; i++) {
            int count = menu.quantity(i);
            if (count <= 0) continue;
            var slot = menu.getSlot(i);
            if (!menu.effectEnabled(i)) {
                graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xA0202020);
                graphics.renderOutline(slot.x - 1, slot.y - 1, 18, 18, 0xFF9C5555);
            }
            String label = count >= 1_000_000 ? count / 1_000_000 + "m" : count >= 10_000 ? count / 1_000 + "k" : Integer.toString(count);
            graphics.drawString(font, label, slot.x + 17 - font.width(label), slot.y + 9, 0xFFFFFF, true);
        }
        graphics.pose().popPose();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hoveredSlot != null && hoveredSlot.index < PotionBagMenu.PAGE_SIZE && hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            var lines = new java.util.ArrayList<>(getTooltipFromContainerItem(hoveredSlot.getItem()));
            lines.add(Component.translatable("better_experience.potion_bag.quantity", menu.quantity(hoveredSlot.index)));
            boolean enabled = menu.effectEnabled(hoveredSlot.index);
            lines.add(Component.translatable(enabled ? "better_experience.potion_bag.effect_on" : "better_experience.potion_bag.effect_off")
                    .withColor(enabled ? 0x77DD88 : 0xDD8888));
            lines.add(Component.translatable("better_experience.potion_bag.toggle_help").withColor(0xAAAAAA));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else renderTooltip(graphics, mouseX, mouseY);
    }
}
