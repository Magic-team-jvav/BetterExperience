package com.github.edg_thexu.better_experience.client.gui;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.client.gui.widget.LiveSearchBox;
import com.github.edg_thexu.better_experience.networks.c2s.SpawnControlPacket;
import com.github.edg_thexu.better_experience.networks.s2c.SpawnSnapshotPacket;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.github.edg_thexu.better_experience.client.gui.SpawnSettingsScreen.text;

/** Only visible cards render models; previews never join or tick the client world. */
public final class SpawnMobScreen extends Screen {
    private static final int MARGIN = 10;
    private static final int GRID_TOP = 70;
    private static final int FOOTER_SPACE = 44;
    private static final int CARD_GAP = 4;
    private static final int MIN_CARD_WIDTH = 88;
    private static final int MIN_CARD_HEIGHT = 100;

    private final Screen parent;
    private final int request = SpawnSettingsScreen.nextRequest();
    private final Map<ResourceLocation, LivingEntity> previews = new HashMap<>();
    private final Set<ResourceLocation> failedPreviews = new HashSet<>();
    private final List<Button> cards = new ArrayList<>();
    private SpawnSnapshotPacket snapshot;
    private List<SpawnSnapshotPacket.Entry> results = List.of();
    private String query = "";
    private int page;
    private int columns;
    private int pageSize;
    private int left;
    private int cardWidth;
    private int cardHeight;
    private int ticks;
    private Button previous;
    private Button next;
    private Button reset;

    public SpawnMobScreen(Screen parent) {
        super(text("mobs"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int firstVisible = page * pageSize;
        int availableWidth = Math.max(1, width - MARGIN * 2);
        int availableHeight = Math.max(1, height - GRID_TOP - FOOTER_SPACE);
        columns = Math.max(1, (availableWidth + CARD_GAP) / (MIN_CARD_WIDTH + CARD_GAP));
        int rows = Math.max(1, (availableHeight + CARD_GAP) / (MIN_CARD_HEIGHT + CARD_GAP));
        pageSize = columns * rows;
        page = firstVisible / pageSize;
        cardWidth = (availableWidth - (columns - 1) * CARD_GAP) / columns;
        cardHeight = (availableHeight - (rows - 1) * CARD_GAP) / rows;
        int gridWidth = columns * (cardWidth + CARD_GAP) - CARD_GAP;
        left = (width - gridWidth) / 2;
        var search = addRenderableWidget(new LiveSearchBox(font, left, 46, gridWidth, 18,
                text("search"), query, value -> { query = value; page = 0; filter(); }));
        search.setHint(text("search"));
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(left, height - 26, 22, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(left + 72, height - 26, 22, 20).build());
        reset = addRenderableWidget(Button.builder(text("allow_all"), button -> send(SpawnControlPacket.RESET_FILTER, SpawnControlPacket.NONE, true))
                .bounds(left + 100, height - 26, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + gridWidth - 80, height - 26, 80, 20).build());
        filter();
        queryServer();
    }

    void accept(SpawnSnapshotPacket packet) {
        if (packet.request() != request) return;
        boolean changed = snapshot == null || snapshot.token() != packet.token() || !snapshot.entries().equals(packet.entries())
                || snapshot.editable() != packet.editable();
        if (snapshot != null && snapshot.token() != packet.token()) page = 0;
        snapshot = packet;
        if (changed) filter();
    }

    private void queryServer() {
        if (minecraft.level != null) PacketDistributor.sendToServer(new SpawnControlPacket(request, SpawnControlPacket.QUERY,
                0, 0, 0, SpawnControlPacket.NONE, false));
    }

    private void send(int action, ResourceLocation id, boolean enabled) {
        if (snapshot == null || !snapshot.editable()) return;
        PacketDistributor.sendToServer(new SpawnControlPacket(request, action, snapshot.token(), 0, 0, id, enabled));
    }

    private Component name(ResourceLocation id) {
        return BuiltInRegistries.ENTITY_TYPE.containsKey(id) ? BuiltInRegistries.ENTITY_TYPE.get(id).getDescription() : Component.literal(id.toString());
    }

    private void filter() {
        String needle = query.toLowerCase(Locale.ROOT);
        results = snapshot == null ? List.of() : snapshot.entries().stream()
                .filter(entry -> entry.spawn().id().toString().contains(needle) || name(entry.spawn().id()).getString().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparing(entry -> name(entry.spawn().id()).getString())).toList();
        page = Math.min(page, pages() - 1);
        rebuildCards();
    }

    private int pages() { return Math.max(1, (results.size() + pageSize - 1) / pageSize); }

    private void changePage(int offset) {
        page = Math.clamp(page + offset, 0, pages() - 1);
        rebuildCards();
    }

    private void rebuildCards() {
        for (Button card : cards) removeWidget(card);
        cards.clear();
        Set<ResourceLocation> visible = new HashSet<>();
        for (int index = page * pageSize; index < Math.min(results.size(), (page + 1) * pageSize); index++) {
            var entry = results.get(index);
            int slot = index % pageSize;
            int x = left + slot % columns * (cardWidth + CARD_GAP);
            int y = GRID_TOP + slot / columns * (cardHeight + CARD_GAP);
            var card = new MobCard(x, y, entry);
            card.active = snapshot.editable();
            cards.add(addRenderableWidget(card));
            visible.add(entry.spawn().id());
        }
        previews.keySet().retainAll(visible);
        previous.active = page > 0;
        next.active = page + 1 < pages();
        reset.active = snapshot != null && snapshot.editable();
    }

    @Override
    public void tick() {
        if (minecraft.level == null) { onClose(); return; }
        if (++ticks % 40 == 0) queryServer();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        if (snapshot != null) {
            String biomeKey = "biome." + snapshot.biome().getNamespace() + "." + snapshot.biome().getPath().replace('/', '.');
            Component biome = I18n.exists(biomeKey) ? Component.translatable(biomeKey) : Component.literal(snapshot.biome().toString());
            String label = font.plainSubstrByWidth(biome.getString(), width - 20);
            graphics.drawCenteredString(font, label, width / 2, 21, 0x7EDDCB);
            graphics.drawCenteredString(font, text("environment", snapshot.height(), snapshot.light(), snapshot.time() % 24000), width / 2, 33, 0xA9BCCB);
        }
        if (results.isEmpty()) graphics.drawCenteredString(font, text(snapshot == null ? "loading" : "empty"), width / 2, 105, 0xA9BCCB);
        graphics.drawCenteredString(font, (page + 1) + "/" + pages(), left + 47, height - 20, 0xFFFFFF);
        if (snapshot != null && snapshot.truncated()) graphics.drawCenteredString(font, text("truncated"), width / 2, height - 38, 0xFFAA55);
    }

    private LivingEntity preview(ResourceLocation id) {
        if (failedPreviews.contains(id) || minecraft.level == null) return null;
        if (previews.containsKey(id)) return previews.get(id);
        try {
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(id) && BuiltInRegistries.ENTITY_TYPE.get(id).create(minecraft.level) instanceof LivingEntity entity) {
                previews.put(id, entity);
                return entity;
            }
        } catch (RuntimeException exception) {
            Better_experience.LOGGER.warn("Cannot create spawn preview for {}", id, exception);
        }
        failedPreviews.add(id);
        return null;
    }

    private void renderModel(GuiGraphics graphics, ResourceLocation id, int x, int y, int width, int height) {
        int modelBottom = y + height - 30;
        LivingEntity entity = preview(id);
        if (entity == null) {
            graphics.drawCenteredString(font, "?", x + width / 2, (y + modelBottom - font.lineHeight) / 2, 0xA9BCCB);
            return;
        }
        graphics.flush();
        graphics.enableScissor(x + 2, y + 2, x + width - 2, modelBottom);
        var dispatcher = minecraft.getEntityRenderDispatcher();
        Quaternionf camera = new Quaternionf(dispatcher.cameraOrientation());
        PoseStack pose = new PoseStack();
        float scale = Math.min(Math.max(1, height - 45) / Math.max(0.5F, entity.getBbHeight()),
                Math.max(1, width - 23) / Math.max(0.5F, entity.getBbWidth()));
        pose.translate(x + width / 2.0F, modelBottom - 6, 50);
        pose.scale(scale, scale, -scale);
        pose.mulPose(new Quaternionf().rotateZ((float) Math.PI).rotateY(0.45F));
        try {
            Lighting.setupForEntityInInventory();
            dispatcher.overrideCameraOrientation(new Quaternionf().rotateY((float) Math.PI));
            dispatcher.setRenderShadow(false);
            RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0, 0, 0, 0, 1, pose, graphics.bufferSource(), 15728880));
            graphics.flush();
        } catch (RuntimeException exception) {
            failedPreviews.add(id);
            previews.remove(id);
            Better_experience.LOGGER.warn("Cannot render spawn preview for {}", id, exception);
        } finally {
            dispatcher.setRenderShadow(true);
            dispatcher.overrideCameraOrientation(camera);
            Lighting.setupFor3DItems();
            graphics.disableScissor();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        changePage(scrollY > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    private final class MobCard extends Button {
        private final SpawnSnapshotPacket.Entry entry;

        MobCard(int x, int y, SpawnSnapshotPacket.Entry entry) {
            super(x, y, cardWidth, cardHeight, name(entry.spawn().id()).copy().append(" ").append(text(entry.enabled() ? "enabled" : "disabled")),
                    button -> send(SpawnControlPacket.TOGGLE, entry.spawn().id(), !entry.enabled()), DEFAULT_NARRATION);
            this.entry = entry;
            var spawn = entry.spawn();
            setTooltip(Tooltip.create(name(spawn.id()).copy().append("\n" + spawn.id() + "\n")
                    .append(text("entry_info", spawn.weight(), spawn.minCount(), spawn.maxCount())).append("\n").append(text("filter_help"))));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY();
            graphics.fill(x, y, x + width, y + height, isHoveredOrFocused() ? 0xE5354E60 : 0xDA182735);
            graphics.renderOutline(x, y, width, height, entry.enabled() ? 0xFF6ABBA8 : 0xFF59636B);
            renderModel(graphics, entry.spawn().id(), x, y, width, height);
            String label = font.plainSubstrByWidth(name(entry.spawn().id()).getString(), width - 6);
            graphics.drawCenteredString(font, label, x + width / 2, y + height - 28, 0xFFFFFF);
            graphics.renderOutline(x + 9, y + height - 15, 10, 10, 0xFFBDCED6);
            if (entry.enabled()) graphics.fill(x + 11, y + height - 13, x + 17, y + height - 7, 0xFF7EDDCB);
            graphics.drawString(font, text(entry.enabled() ? "enabled" : "disabled"), x + 24, y + height - 14, active ? 0xCBD8DF : 0x8A9298, false);
        }
    }
}
