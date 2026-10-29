package com.github.edg_thexu.better_experience.client.gui;

import com.github.edg_thexu.better_experience.client.gui.widget.LiveSearchBox;
import com.github.edg_thexu.better_experience.item.StructureLocatorOrb;
import com.github.edg_thexu.better_experience.module.locator.LocatorCatalog;
import com.github.edg_thexu.better_experience.module.locator.LocatorNames;
import com.github.edg_thexu.better_experience.module.locator.LocatorPayment;
import com.github.edg_thexu.better_experience.networks.c2s.LocatorTargetPacketC2S;
import com.github.edg_thexu.better_experience.networks.s2c.LocatorCatalogPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StructureLocatorScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 220;
    private static final int PAGE_SIZE = 6;
    private record Entry(LocatorCatalog.Entry data, String name, String modName, String searchable) {}
    private final int slot;
    private final List<Entry> entries;
    private final List<Button> rows = new ArrayList<>();
    private List<Entry> results = List.of();
    private ResourceLocation selected;
    private String query = "";
    private int page;
    private int left;
    private int top;
    private Button pages;
    private Button previous;
    private Button next;
    private Button save;
    private Button search;

    public StructureLocatorScreen(LocatorCatalogPacket packet) {
        super(Component.translatable("better_experience.locator.title"));
        slot = packet.slot();
        selected = packet.selected().orElse(null);
        entries = packet.entries().stream().map(data -> {
            String name = LocatorNames.structure(data.id()).getString();
            String modName = ModList.get().getModContainerById(data.id().getNamespace())
                    .map(container -> container.getModInfo().getDisplayName()).orElse(data.id().getNamespace());
            return new Entry(data, name, modName, (name + " " + modName).toLowerCase(Locale.ROOT));
        }).toList();
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = Math.max(0, (height - PANEL_HEIGHT) / 2);
        rows.clear();
        var box = addRenderableWidget(new LiveSearchBox(font, left + 10, top + 25, 280, 18,
                Component.translatable("better_experience.locator.search"), query, text -> {
                    query = text;
                    page = 0;
                    filter();
                }));
        box.setHint(Component.translatable("better_experience.locator.search"));
        box.setTooltip(Tooltip.create(Component.translatable("better_experience.space.search_help")));
        for (int row = 0; row < PAGE_SIZE; row++) {
            final int index = row;
            rows.add(addRenderableWidget(Button.builder(Component.empty(), button -> {
                selected = results.get(page * PAGE_SIZE + index).data.id();
                updatePage();
            }).bounds(left + 10, top + 48 + row * 20, 280, 18).build()));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(left + 10, top + 172, 28, 18).build());
        pages = addRenderableWidget(Button.builder(Component.empty(), button -> changePage(1))
                .bounds(left + 42, top + 172, 216, 18).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(left + 262, top + 172, 28, 18).build());
        save = addRenderableWidget(Button.builder(Component.translatable("better_experience.locator.select"), button -> choose(false))
                .bounds(left + 10, top + 196, 136, 18).build());
        boolean free = minecraft.player != null && LocatorPayment.isFree(minecraft.player);
        var price = Component.translatable(free ? "better_experience.locator.locate_free" : "better_experience.locator.locate", LocatorPayment.GOLD_COST);
        search = addRenderableWidget(Button.builder(price, button -> choose(true))
                .bounds(left + 154, top + 196, 136, 18).build());
        search.setTooltip(Tooltip.create(Component.translatable("better_experience.locator.cost_help", LocatorPayment.GOLD_COST)));
        filter();
    }

    private void filter() {
        var search = SpaceBlockSearch.parse(query);
        results = entries.stream().filter(entry -> search.matches(entry.searchable, entry.data.id().toString())).toList();
        updatePage();
    }

    private int pageCount() { return Math.max(1, (results.size() + PAGE_SIZE - 1) / PAGE_SIZE); }
    private void changePage(int delta) { page = Math.floorMod(page + delta, pageCount()); updatePage(); }

    private void updatePage() {
        page = Math.min(page, pageCount() - 1);
        for (int row = 0; row < rows.size(); row++) {
            var button = rows.get(row);
            int index = page * PAGE_SIZE + row;
            button.visible = index < results.size();
            if (!button.visible) continue;
            var entry = results.get(index);
            String text = (entry.data.id().equals(selected) ? "✓ " : "") + entry.name + " · " + entry.modName;
            button.setMessage(Component.literal(font.plainSubstrByWidth(text, 266)));
            button.setTooltip(Tooltip.create(Component.empty().append(LocatorNames.structure(entry.data.id()))
                    .append("\n" + entry.data.id() + "\n")
                    .append(Component.translatable(entry.data.available() ? "better_experience.locator.marked"
                            : "better_experience.locator.unavailable", entry.data.marked()))));
        }
        pages.setMessage(Component.literal((page + 1) + "/" + pageCount() + " · " + results.size()));
        previous.active = next.active = pages.active = pageCount() > 1;
        save.active = selected != null;
        search.active = selected != null && entries.stream().anyMatch(entry -> entry.data.id().equals(selected) && entry.data.available());
    }

    private boolean valid() {
        return minecraft.player != null && minecraft.level != null && minecraft.player.getInventory().selected == slot
                && minecraft.player.getMainHandItem().getItem() instanceof StructureLocatorOrb;
    }

    private void choose(boolean search) {
        if (selected != null && valid()) PacketDistributor.sendToServer(new LocatorTargetPacketC2S(slot, selected, search));
        onClose();
    }

    @Override
    public void tick() { if (!valid()) onClose(); }
    @Override
    public boolean isPauseScreen() { return false; }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX >= left && mouseX < left + PANEL_WIDTH && mouseY >= top + 48 && mouseY < top + 190 && vertical != 0) {
            changePage(vertical > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xEB19242F);
        graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF567585);
        graphics.drawCenteredString(font, title, left + PANEL_WIDTH / 2, top + 8, 0xFFFFFF);
        for (var widget : renderables) widget.render(graphics, mouseX, mouseY, partialTick);
        if (results.isEmpty()) graphics.drawCenteredString(font, Component.translatable("better_experience.locator.no_results"),
                left + PANEL_WIDTH / 2, top + 98, 0xA7E6FF);
    }
}
