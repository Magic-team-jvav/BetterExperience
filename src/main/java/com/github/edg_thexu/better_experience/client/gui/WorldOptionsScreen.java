package com.github.edg_thexu.better_experience.client.gui;

import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions.Feature;
import com.github.edg_thexu.better_experience.networks.c2s.WorldOptionsPacket;
import com.github.edg_thexu.better_experience.networks.s2c.WorldOptionsSnapshotPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class WorldOptionsScreen extends Screen {
    private final Screen parent;
    private final int request = SpawnSettingsScreen.nextRequest();
    private final Map<Feature, Button> toggles = new EnumMap<>(Feature.class);
    private WorldOptionsSnapshotPacket snapshot;
    private WorldOptions.Values draft = WorldOptions.Values.DEFAULT;
    private GrowthSlider crops;
    private GrowthSlider saplings;
    private Button apply;
    private Button reset;
    private boolean dirty;
    private int top;
    private int ticks;

    public WorldOptionsScreen(Screen parent) {
        super(text("title"));
        this.parent = parent;
    }

    private static Component text(String key, Object... arguments) {
        return Component.translatable("better_experience.world_options." + key, arguments);
    }

    public static void receive(WorldOptionsSnapshotPacket packet) {
        if (Minecraft.getInstance().screen instanceof WorldOptionsScreen screen && packet.request() == screen.request) {
            screen.accept(packet);
        }
    }

    private void accept(WorldOptionsSnapshotPacket packet) {
        if (!packet.values().valid()) return;
        snapshot = packet;
        if (!dirty) draft = packet.values();
        refresh();
    }

    @Override
    protected void init() {
        if (minecraft.level == null) {
            snapshot = null;
            draft = WorldOptions.Values.DEFAULT;
            dirty = false;
        }
        int panelWidth = Math.min(340, width - 24);
        int left = (width - panelWidth) / 2;
        int half = (panelWidth - 4) / 2;
        top = Math.max(28, (height - 200) / 2);
        toggles.clear();
        for (Feature feature : Feature.values()) {
            int index = feature.ordinal();
            Button button = addRenderableWidget(Button.builder(Component.empty(), ignored -> {
                draft = draft.with(feature, !draft.enabled(feature));
                changed();
            }).bounds(left + index % 2 * (half + 4), top + 24 + index / 2 * 24, half, 20)
                    .tooltip(Tooltip.create(text(feature.key() + "_help"))).build());
            toggles.put(feature, button);
        }
        crops = addRenderableWidget(new GrowthSlider(left, top + 98, panelWidth, false));
        saplings = addRenderableWidget(new GrowthSlider(left, top + 122, panelWidth, true));
        apply = addRenderableWidget(Button.builder(text("apply"), ignored -> send(true))
                .bounds(left, top + 146, half, 20).build());
        reset = addRenderableWidget(Button.builder(text("defaults"), ignored -> {
            draft = WorldOptions.Values.DEFAULT;
            changed();
        }).bounds(left + half + 4, top + 146, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), ignored -> onClose())
                .bounds(left, top + 170, panelWidth, 20).build());
        refresh();
        send(false);
    }

    private boolean editable() {
        return minecraft.level != null && snapshot != null && snapshot.editable();
    }

    private void changed() {
        dirty = snapshot == null || !draft.equals(snapshot.values());
        refresh();
    }

    private void refresh() {
        boolean editable = editable();
        toggles.forEach((feature, button) -> {
            button.setMessage(text(feature.key(), Component.translatable(draft.enabled(feature) ? "options.on" : "options.off")));
            button.active = editable;
        });
        crops.setMultiplier(draft.crops());
        saplings.setMultiplier(draft.saplings());
        crops.active = saplings.active = reset.active = editable;
        apply.active = editable && dirty;
    }

    private void send(boolean update) {
        if (minecraft.level == null || update && !editable()) return;
        PacketDistributor.sendToServer(new WorldOptionsPacket(request, update, draft));
        if (update) {
            dirty = false;
            apply.active = false;
        }
    }

    @Override
    public void tick() {
        if (minecraft.level == null) {
            snapshot = null;
            dirty = false;
            refresh();
        } else if (++ticks % 40 == 0) {
            send(false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, top - 18, 0xFFFFFF);
        Component status = minecraft.level == null ? text("join_world")
                : snapshot == null ? SpawnSettingsScreen.text("loading")
                : text(snapshot.editable() ? "world_scope" : "read_only");
        graphics.drawCenteredString(font, status, width / 2, top + 5, 0xA9BCCB);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        if (dirty) send(true);
        minecraft.setScreen(parent);
    }

    private final class GrowthSlider extends AbstractSliderButton {
        private final boolean sapling;

        GrowthSlider(int x, int y, int width, boolean sapling) {
            super(x, y, width, 20, Component.empty(), (sapling ? draft.saplings() : draft.crops()) / WorldOptions.MAX_GROWTH);
            this.sapling = sapling;
            setTooltip(Tooltip.create(text("growth_help")));
            updateMessage();
        }

        void setMultiplier(double multiplier) {
            value = multiplier / WorldOptions.MAX_GROWTH;
            updateMessage();
        }

        private double multiplier() {
            return Math.round(value * WorldOptions.MAX_GROWTH * 10.0) / 10.0;
        }

        @Override
        protected void updateMessage() {
            setMessage(text(sapling ? "saplings" : "crops", String.format(Locale.ROOT, "%.1f", multiplier())));
        }

        @Override
        protected void applyValue() {
            draft = sapling ? new WorldOptions.Values(draft.flags(), draft.crops(), multiplier())
                    : new WorldOptions.Values(draft.flags(), multiplier(), draft.saplings());
            changed();
        }
    }
}
