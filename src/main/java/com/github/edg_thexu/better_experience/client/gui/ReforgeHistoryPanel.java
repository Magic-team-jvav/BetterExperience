package com.github.edg_thexu.better_experience.client.gui;

import com.github.edg_thexu.better_experience.module.reforge.ReforgeHistoryManager;
import com.github.edg_thexu.better_experience.networks.c2s.ReforgeHistoryPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.confluence.lib.common.component.ModRarity;
import org.confluence.mod.common.menu.NPCReforgeMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * Queue of the prefixes the goblin just rolled, newest at the bottom with five rows in view.
 * The wheel scrolls through older entries, clicking one reforges into it for the current price.
 */
public final class ReforgeHistoryPanel {
    private static final int ROWS = 5;
    private static final int LINE = 11;
    private static final int WIDTH = 46;
    private static final int TEXT_WIDTH = WIDTH - 8;
    private static final int BOTTOM_ROW_Y = 73;
    private static final int MAX_ENTRIES = 128;
    private static final int FADE_IN_TICKS = 4;
    private static final int PUSH_TICKS = 8;
    private static final float SCROLL_EASING = 0.35F;

    private record Entry(String name, int color, int createdAt) {}

    private final NPCReforgeMenu menu;
    private final List<Entry> entries = new ArrayList<>();
    private ItemStack lastItem = ItemStack.EMPTY;
    private int clock;
    private int scrollTarget;
    private float scroll;
    private float previousScroll;
    private float renderScroll;
    private float renderSlide;
    private int pushAge = PUSH_TICKS;
    private int left, top, right, bottom, contentTop, contentBottom, bottomRowY;
    private boolean shown;
    private boolean reforgeRequested;

    public ReforgeHistoryPanel(NPCReforgeMenu menu) { this.menu = menu; }

    public void tick() {
        clock++;
        if (pushAge < PUSH_TICKS) pushAge++;
        ItemStack item = menu.getReforgeItem();
        if (item.isEmpty() || !ItemStack.isSameItem(lastItem, item)) {
            entries.clear();
            scrollTarget = 0;
        }
        lastItem = item.copy();
        boolean scrolledUp = scrollTarget > 0;
        for (var rolled : ReforgeHistoryManager.drainRolled()) {
            if (!ItemStack.isSameItem(rolled.stack(), item)) continue;
            if (entries.size() >= MAX_ENTRIES) entries.remove(0);
            if (scrolledUp) scrollTarget++;
            else pushAge = 0;
            entries.add(new Entry(rolled.name(), colorOf(rolled.stack()), clock));
        }
        int maxScroll = Math.max(0, entries.size() - ROWS);
        scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll);
        previousScroll = scroll;
        scroll = Mth.lerp(SCROLL_EASING, scroll, scrollTarget);
        if (Math.abs(scrollTarget - scroll) < 0.01F) scroll = scrollTarget;
        scroll = Mth.clamp(scroll, 0, maxScroll);
    }

    public boolean consumeReforgeRequest() {
        if (!reforgeRequested) return false;
        reforgeRequested = false;
        return true;
    }

    public void render(GuiGraphics graphics, int leftPos, int topPos, int mouseX, int mouseY, float partialTick) {
        shown = !entries.isEmpty();
        if (!shown) return;
        bottomRowY = topPos + BOTTOM_ROW_Y;
        contentBottom = bottomRowY + LINE;
        contentTop = bottomRowY - (ROWS - 1) * LINE;
        top = contentTop;
        bottom = contentBottom;
        right = leftPos + NPCReforgeMenu.REFORGE_SLOT_X - 2;
        left = right - WIDTH;
        renderScroll = Mth.lerp(partialTick, previousScroll, scroll);
        renderSlide = slideRows(partialTick);

        var font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        int hovered = mouseX >= left && mouseX < right ? indexAt(mouseY) : -1;
        for (int i = 0; i < entries.size(); i++) {
            int y = rowTop(i);
            Entry entry = entries.get(i);
            float alpha = edgeFade(y) * Math.min(1.0F, (clock - entry.createdAt() + 1) / (float) FADE_IN_TICKS);
            if (alpha <= 0.02F) continue;
            if (i == hovered) graphics.fill(left, y, right, y + LINE, (int) (0x30 * alpha) << 24 | 0xFFFFFF);
            String label = font.plainSubstrByWidth(Component.translatable("prefix.confluence." + entry.name()).getString(), TEXT_WIDTH);
            graphics.drawString(font, label, right - 4 - font.width(label), y + 1, (int) (alpha * 255) << 24 | entry.color(), true);
        }
        graphics.pose().popPose();
    }

    public boolean click(double mouseX, double mouseY, int button) {
        if (!shown || mouseX < left || mouseX >= right || mouseY < top || mouseY >= bottom) return false;
        if (button == 0) {
            int index = indexAt(mouseY);
            if (index >= 0) reforgeTo(entries.get(index).name());
        }
        return true;
    }

    public boolean scroll(double delta, double mouseX, double mouseY) {
        if (!shown || delta == 0) return false;
        if (mouseX < left || mouseX >= right || mouseY < top || mouseY >= bottom) return false;
        int maxScroll = Math.max(0, entries.size() - ROWS);
        scrollTarget = Mth.clamp(scrollTarget + (delta > 0 ? 1 : -1), 0, maxScroll);
        return true;
    }

    private void reforgeTo(String name) {
        if (menu.getReforgeItem().isEmpty() || menu.getCooldownTicks() > 0) return;
        reforgeRequested = true;
        PacketDistributor.sendToServer(new ReforgeHistoryPacket(menu.containerId, menu.getReforgeItem().copy(), name));
    }

    /** 0 outside the five visible rows, 1 inside; rows sliding in or out fade like the original popups. */
    private float edgeFade(int rowTop) {
        float above = Mth.clamp((contentTop - rowTop) / (float) LINE, 0, 1);
        float below = Mth.clamp((rowTop + LINE - contentBottom) / (float) LINE, 0, 1);
        return (1 - above) * (1 - below);
    }

    private float slideRows(float partialTick) {
        float progress = Math.min(1.0F, (pushAge + partialTick) / (float) PUSH_TICKS);
        return 1.0F - progress * progress * (3.0F - 2.0F * progress);
    }

    private int rowTop(int index) {
        float offset = (entries.size() - 1 - index) - renderScroll - renderSlide;
        return Math.round(bottomRowY - offset * LINE);
    }

    private int indexAt(double mouseY) {
        if (!shown) return -1;
        for (int i = entries.size() - 1; i >= 0; i--) {
            int y = rowTop(i);
            if (mouseY >= y && mouseY < y + LINE) {
                return edgeFade(y) >= 0.5F ? i : -1;
            }
        }
        return -1;
    }

    private static int colorOf(ItemStack stack) {
        ModRarity rarity = ModRarity.getRarity(stack);
        if (rarity != null) return rarity.color() & 0xFFFFFF;
        TextColor color = stack.getRarity().getStyleModifier().apply(Style.EMPTY).getColor();
        return color == null ? 0xFFFFFF : color.getValue() & 0xFFFFFF;
    }
}
