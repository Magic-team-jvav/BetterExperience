package com.github.edg_thexu.better_experience.client.gui;

import com.github.edg_thexu.better_experience.module.spawning.SpawnSettings;
import com.github.edg_thexu.better_experience.networks.c2s.SpawnControlPacket;
import com.github.edg_thexu.better_experience.networks.s2c.SpawnSnapshotPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/** Entry point for both the pause shortcut and the mod-list configuration button. */
public final class SpawnSettingsScreen extends Screen {
    private static final AtomicInteger REQUESTS = new AtomicInteger();
    private final int request = nextRequest();
    private final ModContainer container;
    private final Screen parent;
    private SpawnSnapshotPacket snapshot;
    private MultiplierSlider rate;
    private MultiplierSlider cap;
    private Button apply;
    private Button reset;
    private boolean dirty;
    private int ticks;
    private int top;

    public SpawnSettingsScreen(ModContainer container, Screen parent) {
        super(text("title"));
        this.container = container;
        this.parent = parent;
    }

    static int nextRequest() { return REQUESTS.incrementAndGet(); }

    static Component text(String key, Object... arguments) {
        return Component.translatable("better_experience.spawning." + key, arguments);
    }

    public static void receive(SpawnSnapshotPacket packet) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof SpawnSettingsScreen settings && packet.request() == settings.request) settings.accept(packet);
        else if (screen instanceof SpawnMobScreen mobs) mobs.accept(packet);
    }

    private void accept(SpawnSnapshotPacket packet) {
        snapshot = packet;
        if (!dirty) {
            rate.setMultiplier(packet.rate());
            cap.setMultiplier(packet.cap());
        }
        rate.active = cap.active = reset.active = packet.editable();
        apply.active = packet.editable() && dirty;
    }

    @Override
    protected void init() {
        if (minecraft.level == null) { snapshot = null; dirty = false; }
        int panelWidth = Math.min(300, width - 24);
        int left = (width - panelWidth) / 2;
        top = Math.max(28, (height - 190) / 2);
        double oldRate = rate == null ? 1.0 : rate.multiplier();
        double oldCap = cap == null ? 1.0 : cap.multiplier();
        rate = addRenderableWidget(new MultiplierSlider(left, top + 24, panelWidth, "rate", oldRate));
        cap = addRenderableWidget(new MultiplierSlider(left, top + 50, panelWidth, "cap", oldCap));
        rate.setTooltip(Tooltip.create(text("rate_help")));
        cap.setTooltip(Tooltip.create(text("cap_help")));
        apply = addRenderableWidget(Button.builder(text("apply"), button -> {
            dirty = false;
            sendSettings();
        }).bounds(left, top + 76, panelWidth / 2 - 2, 20).build());
        reset = addRenderableWidget(Button.builder(text("reset"), button -> {
            rate.setMultiplier(1);
            cap.setMultiplier(1);
            dirty = false;
            sendSettings();
        }).bounds(left + panelWidth / 2 + 2, top + 76, panelWidth / 2 - 2, 20).build());
        Button mobs = addRenderableWidget(Button.builder(text("mobs"), button -> minecraft.setScreen(new SpawnMobScreen(this)))
                .bounds(left, top + 102, panelWidth, 20).build());
        mobs.setTooltip(Tooltip.create(text("filter_help")));
        addRenderableWidget(Button.builder(text("other"), button -> minecraft.setScreen(new ConfigurationScreen(container, this)))
                .bounds(left, top + 128, panelWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left, top + 154, panelWidth, 20).build());
        rate.active = cap.active = apply.active = reset.active = false;
        mobs.active = minecraft.level != null;
        if (snapshot != null) accept(snapshot);
        query();
    }

    private void query() {
        if (minecraft.level != null) PacketDistributor.sendToServer(new SpawnControlPacket(request, SpawnControlPacket.QUERY,
                0, 0, 0, SpawnControlPacket.NONE, false));
    }

    private void sendSettings() {
        if (snapshot == null || !snapshot.editable()) return;
        apply.active = false;
        PacketDistributor.sendToServer(new SpawnControlPacket(request, SpawnControlPacket.SETTINGS, snapshot.token(),
                rate.multiplier(), cap.multiplier(), SpawnControlPacket.NONE, false));
    }

    @Override
    public void tick() {
        if (minecraft.level == null) {
            snapshot = null;
            rate.active = cap.active = apply.active = reset.active = false;
            return;
        }
        if (++ticks % 40 == 0) query();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, top - 18, 0xFFFFFF);
        Component status = minecraft.level == null ? text("join_world") : snapshot == null ? text("loading")
                : text(snapshot.editable() ? "world_scope" : "read_only");
        graphics.drawCenteredString(font, status, width / 2, top + 5, 0xA9BCCB);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        if (dirty) sendSettings();
        minecraft.setScreen(parent);
    }

    private final class MultiplierSlider extends AbstractSliderButton {
        private final String label;

        MultiplierSlider(int x, int y, int width, String label, double multiplier) {
            super(x, y, width, 20, Component.empty(), multiplier / SpawnSettings.MAX_MULTIPLIER);
            this.label = label;
            updateMessage();
        }

        double multiplier() { return Math.round(value * SpawnSettings.MAX_MULTIPLIER * 10.0) / 10.0; }

        void setMultiplier(double multiplier) {
            value = multiplier / SpawnSettings.MAX_MULTIPLIER;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(text(label, String.format(Locale.ROOT, "%.1f", multiplier())));
        }

        @Override
        protected void applyValue() {
            dirty = true;
            if (apply != null) apply.active = true;
        }
    }
}
