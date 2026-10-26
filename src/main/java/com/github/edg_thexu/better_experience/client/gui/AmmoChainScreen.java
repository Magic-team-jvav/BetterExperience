package com.github.edg_thexu.better_experience.client.gui;

import com.github.edg_thexu.better_experience.client.gui.widget.LiveSearchBox;
import com.github.edg_thexu.better_experience.data.component.AmmoChainSettings;
import com.github.edg_thexu.better_experience.item.AmmoChain;
import com.github.edg_thexu.better_experience.module.ammochain.AmmoChainManager;
import com.github.edg_thexu.better_experience.networks.c2s.AmmoChainSettingsPacketC2S;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The chamber contents are configuration samples; ammunition stays in the player's inventory. */
public final class AmmoChainScreen extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 220;
    private static final int CHAMBERS_PER_PAGE = 12;
    private final int slot;
    private ItemStack source = ItemStack.EMPTY;
    private AmmoChainSettings settings;
    private List<AmmoChoice> ammunition = List.of();
    private List<AmmoChoice> results = List.of();
    private final List<ChamberButton> chambers = new ArrayList<>();
    private final List<AmmoButton> ammoButtons = new ArrayList<>();
    private int selectedIndex;
    private int chamberPage;
    private int left;
    private int top;
    private int panelWidth;
    private int gridColumns;
    private int gridRows;
    private int cellSize;
    private int inventoryTicks;
    private String query = "";
    private boolean changingCount;
    private boolean compact;
    private EditBox count;
    private Button add;
    private Button enabled;
    private Button previous;
    private Button pages;
    private Button next;
    private Button save;

    private record AmmoChoice(ItemStack sample, int available, String name, String id) {}

    public AmmoChainScreen(int slot, AmmoChainSettings settings) {
        super(text("title"));
        this.slot = slot;
        this.settings = settings;
    }

    private static Component text(String key, Object... arguments) {
        return Component.translatable("better_experience.ammo_chain." + key, arguments);
    }

    @Override
    protected void init() {
        compact = width < PANEL_WIDTH;
        panelWidth = compact ? 320 : PANEL_WIDTH;
        gridColumns = compact ? 6 : 9;
        gridRows = compact ? 6 : 4;
        cellSize = compact ? 16 : 18;
        left = (width - panelWidth) / 2;
        top = Math.max(0, (height - PANEL_HEIGHT) / 2);
        if (source.isEmpty() && minecraft.player != null) source = minecraft.player.getInventory().getItem(slot);
        chambers.clear();
        ammoButtons.clear();
        for (int index = 0; index < CHAMBERS_PER_PAGE; index++) {
            int localIndex = index;
            chambers.add(addRenderableWidget(new ChamberButton(button -> selectChamber(chamberPage * CHAMBERS_PER_PAGE + localIndex))));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> changeChamberPage(-1))
                .bounds(left + 10, top + 175, 26, 18).build());
        pages = addRenderableWidget(Button.builder(Component.empty(), button -> changeChamberPage(1))
                .bounds(left + 40, top + 175, 112, 18).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> changeChamberPage(1))
                .bounds(left + 156, top + 175, 26, 18).build());
        add = addRenderableWidget(Button.builder(text("add_slot"), button -> addSlot())
                .bounds(left + 10, top + 198, 84, 18).build());
        enabled = addRenderableWidget(Button.builder(Component.empty(), button -> {
            settings = settings.withEnabled(!settings.enabled());
            updateControls();
        }).bounds(left + 98, top + 198, 84, 18).build());
        addRenderableWidget(Button.builder(text("clear"), button -> setAmmo(ItemStack.EMPTY))
                .bounds(left + (compact ? 266 : 322), top + 23, compact ? 44 : 48, 18).build());
        count = addRenderableWidget(new EditBox(font, left + (compact ? 232 : 263), top + 46,
                compact ? 38 : 54, 18, text("shots")));
        count.setMaxLength(4);
        count.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        count.setResponder(this::changeCount);
        addRenderableWidget(Button.builder(Component.literal("−"), button -> adjustCount(-1))
                .bounds(left + (compact ? 274 : 322), top + 46, compact ? 16 : 22, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> adjustCount(1))
                .bounds(left + (compact ? 294 : 348), top + 46, compact ? 16 : 22, 18).build());
        var search = addRenderableWidget(new LiveSearchBox(font, left + 199, top + (compact ? 69 : 87),
                compact ? 111 : 171, 18,
                text("search"), query, value -> {
                    query = value;
                    filterAmmunition();
                }));
        search.setHint(text("search"));
        search.setTooltip(Tooltip.create(Component.translatable("better_experience.space.search_help")));
        for (int row = 0; row < gridRows; row++) {
            for (int column = 0; column < gridColumns; column++) {
                int index = row * gridColumns + column;
                ammoButtons.add(addRenderableWidget(new AmmoButton(left + (compact ? 199 : 203) + column * cellSize,
                        top + (compact ? 91 : 111) + row * cellSize, cellSize,
                        button -> { if (index < results.size()) setAmmo(results.get(index).sample()); })));
            }
        }
        addRenderableWidget(Button.builder(text("cancel"), button -> onClose())
                .bounds(left + 199, top + 198, compact ? 54 : 83, 18).build());
        save = addRenderableWidget(Button.builder(text("save"), button -> save())
                .bounds(left + (compact ? 256 : 287), top + 198, compact ? 54 : 83, 18).build());
        refreshAmmunition();
        selectChamber(selectedIndex);
    }

    private int pageCount() {
        return (settings.entries().size() + CHAMBERS_PER_PAGE - 1) / CHAMBERS_PER_PAGE;
    }

    private void changeChamberPage(int delta) {
        chamberPage = Math.floorMod(chamberPage + delta, pageCount());
        selectChamber(chamberPage * CHAMBERS_PER_PAGE);
    }

    private void selectChamber(int index) {
        selectedIndex = Mth.clamp(index, 0, settings.entries().size() - 1);
        chamberPage = selectedIndex / CHAMBERS_PER_PAGE;
        changingCount = true;
        count.setValue(Integer.toString(settings.entries().get(selectedIndex).shots()));
        changingCount = false;
        updateControls();
    }

    private void addSlot() {
        if (settings.entries().size() >= AmmoChainSettings.MAX_SLOTS) return;
        settings = settings.addSlot();
        selectChamber(settings.entries().size() - 1);
    }

    private void setAmmo(ItemStack ammo) {
        var current = settings.entries().get(selectedIndex);
        settings = settings.withEntry(selectedIndex, new AmmoChainSettings.Entry(ammo, current.shots()));
        updateControls();
    }

    private int enteredCount() {
        String value = count.getValue();
        if (value.isEmpty()) return 0;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException exception) { return 0; }
    }

    private void changeCount(String value) {
        if (changingCount) return;
        int shots = enteredCount();
        if (shots > 0 && shots <= AmmoChainSettings.MAX_SHOTS) {
            var current = settings.entries().get(selectedIndex);
            settings = settings.withEntry(selectedIndex, new AmmoChainSettings.Entry(current.ammo(), shots));
        }
        updateControls();
    }

    private void adjustCount(int delta) {
        int shots = enteredCount();
        if (shots < 1) shots = settings.entries().get(selectedIndex).shots();
        count.setValue(Integer.toString(Mth.clamp(shots + delta * (hasShiftDown() ? 10 : 1), 1, AmmoChainSettings.MAX_SHOTS)));
    }

    private void updateControls() {
        int start = chamberPage * CHAMBERS_PER_PAGE;
        int visible = Math.min(CHAMBERS_PER_PAGE, settings.entries().size() - start);
        for (int localIndex = 0; localIndex < chambers.size(); localIndex++) {
            var button = chambers.get(localIndex);
            button.visible = localIndex < visible;
            if (!button.visible) continue;
            int index = start + localIndex;
            double angle = -Math.PI / 2 + 2 * Math.PI * localIndex / visible;
            button.setPosition(left + 96 + (int) Math.round(Math.cos(angle) * 58) - 13,
                    top + 100 + (int) Math.round(Math.sin(angle) * 58) - 13);
            button.entry = settings.entries().get(index);
            button.icon = button.entry.ammo();
            button.index = index;
            button.selected = index == selectedIndex;
            button.setMessage(text("slot", index + 1));
            button.setTooltip(Tooltip.create(text("slot_details", index + 1,
                    button.icon.isEmpty() ? text("empty") : button.icon.getHoverName(), button.entry.shots())));
        }
        pages.setMessage(Component.literal((chamberPage + 1) + "/" + pageCount() + " · " + settings.entries().size()));
        previous.active = next.active = pages.active = pageCount() > 1;
        add.active = settings.entries().size() < AmmoChainSettings.MAX_SLOTS;
        enabled.setMessage(text(settings.enabled() ? "enabled" : "disabled"));
        enabled.setTooltip(Tooltip.create(text("enabled_help")));
        save.active = enteredCount() > 0 && enteredCount() <= AmmoChainSettings.MAX_SHOTS;
        updateAmmoButtons();
    }

    private void refreshAmmunition() {
        if (minecraft.player == null) return;
        List<AmmoChoice> choices = new ArrayList<>();
        var inventory = new ArrayList<>(minecraft.player.getInventory().items);
        inventory.addAll(minecraft.player.getInventory().offhand);
        for (var stack : inventory) {
            if (!AmmoChainManager.isAmmo(stack)) continue;
            int existing = -1;
            for (int index = 0; index < choices.size(); index++) {
                if (ItemStack.isSameItemSameComponents(choices.get(index).sample(), stack)) {
                    existing = index;
                    break;
                }
            }
            if (existing >= 0) {
                var choice = choices.get(existing);
                choices.set(existing, new AmmoChoice(choice.sample(), choice.available() + stack.getCount(), choice.name(), choice.id()));
            } else {
                choices.add(new AmmoChoice(stack.copyWithCount(1), stack.getCount(), stack.getHoverName().getString().toLowerCase(Locale.ROOT),
                        BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
            }
        }
        ammunition = List.copyOf(choices);
        filterAmmunition();
    }

    private void filterAmmunition() {
        var search = SpaceBlockSearch.parse(query);
        results = ammunition.stream().filter(choice -> search.matches(choice.name(), choice.id())).toList();
        updateAmmoButtons();
    }

    private void updateAmmoButtons() {
        var selected = settings.entries().get(selectedIndex).ammo();
        for (int index = 0; index < ammoButtons.size(); index++) {
            var button = ammoButtons.get(index);
            button.choice = index < results.size() ? results.get(index) : null;
            button.active = button.choice != null;
            button.selected = button.choice != null && ItemStack.isSameItemSameComponents(button.choice.sample(), selected);
            button.setMessage(button.choice == null ? Component.empty() : button.choice.sample().getHoverName());
        }
    }

    private boolean valid() {
        if (minecraft.player == null || minecraft.level == null || slot < 0 || slot >= minecraft.player.getInventory().getContainerSize()) return false;
        var stack = minecraft.player.getInventory().getItem(slot);
        return stack.getItem() instanceof AmmoChain && (stack == source || ItemStack.matches(stack, source));
    }

    private void save() {
        if (valid() && save.active) PacketDistributor.sendToServer(new AmmoChainSettingsPacketC2S(slot, settings));
        onClose();
    }

    @Override
    public void tick() {
        if (!valid()) {
            onClose();
            return;
        }
        if (++inventoryTicks % 10 == 0) refreshAmmunition();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX >= left + 8 && mouseX < left + 186 && mouseY >= top + 24 && mouseY < top + 194 && vertical != 0) {
            changeChamberPage(vertical > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + panelWidth, top + PANEL_HEIGHT, 0xEF19242F);
        graphics.renderOutline(left, top, panelWidth, PANEL_HEIGHT, 0xFF617B87);
        graphics.fill(left + 189, top + 23, left + 190, top + 213, 0xFF4A6472);
        graphics.drawCenteredString(font, title, left + panelWidth / 2, top + 8, 0xFFFFFF);
        circle(graphics, left + 96, top + 100, 75, 0xFF52606A);
        circle(graphics, left + 96, top + 100, 72, 0xFF26343F);
        circle(graphics, left + 96, top + 100, 21, 0xFF6A7982);
        circle(graphics, left + 96, top + 100, 17, 0xFF1C2630);
        graphics.drawCenteredString(font, "↻", left + 96, top + 95, 0xFFAACBDA);
        graphics.drawString(font, text("slot", selectedIndex + 1), left + 199, top + 28, 0xA7E6FF);
        graphics.drawString(font, text("shots"), left + 199, top + 51, 0xFFFFFF);
        if (!compact) {
            var selected = settings.entries().get(selectedIndex).ammo();
            String name = selected.isEmpty() ? text("empty").getString() : selected.getHoverName().getString();
            graphics.drawString(font, font.plainSubstrByWidth(name, 171), left + 199, top + 73, 0xD5E2E8);
        }
        graphics.fill(left + 199, top + (compact ? 89 : 109), left + panelWidth - 10,
                top + (compact ? 189 : 185), 0x90303D48);
        for (var widget : renderables) widget.render(graphics, mouseX, mouseY, partialTick);
        if (results.isEmpty()) graphics.drawCenteredString(font, text("no_ammo"), left + (compact ? 254 : 284), top + 142, 0xA8BAC5);
        for (var button : ammoButtons) {
            if (button.choice != null && button.isMouseOver(mouseX, mouseY)) {
                graphics.renderTooltip(font, button.choice.sample(), mouseX, mouseY);
                break;
            }
        }
    }

    private static void circle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int halfWidth = (int) Math.sqrt(radius * radius - y * y);
            graphics.fill(centerX - halfWidth, centerY + y, centerX + halfWidth + 1, centerY + y + 1, color);
        }
    }

    private final class ChamberButton extends Button {
        private AmmoChainSettings.Entry entry;
        private ItemStack icon = ItemStack.EMPTY;
        private int index;
        private boolean selected;

        private ChamberButton(OnPress onPress) {
            super(0, 0, 26, 26, Component.empty(), onPress, DEFAULT_NARRATION);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            double dx = mouseX - getX() - 13;
            double dy = mouseY - getY() - 13;
            return visible && dx * dx + dy * dy <= 14 * 14;
        }

        @Override
        protected boolean clicked(double mouseX, double mouseY) {
            return active && isMouseOver(mouseX, mouseY);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            isHovered = isMouseOver(mouseX, mouseY);
            int x = getX(), y = getY();
            circle(graphics, x + 13, y + 13, 14, selected ? 0xFF7ED7F2 : isHoveredOrFocused() ? 0xFFB4C6CF : 0xFF607580);
            circle(graphics, x + 13, y + 13, 12, selected ? 0xFF30556A : 0xFF131E28);
            if (!icon.isEmpty()) graphics.renderItem(icon, x + 5, y + 3);
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 200);
            graphics.pose().scale(0.65F, 0.65F, 1);
            graphics.drawString(font, Integer.toString(index + 1), 0, 0, 0xFFFFFF, true);
            String shots = Integer.toString(entry.shots());
            graphics.drawString(font, shots, Math.max(0, 38 - font.width(shots)), 28, 0xFFCCE8F2, true);
            graphics.pose().popPose();
        }
    }

    private final class AmmoButton extends Button {
        private AmmoChoice choice;
        private boolean selected;

        private AmmoButton(int x, int y, int size, OnPress onPress) {
            super(x, y, size, size, Component.empty(), onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY();
            int size = getWidth();
            int iconOffset = (size - 16) / 2;
            graphics.fill(x, y, x + size, y + size, selected ? 0xFF406878 : active && isHoveredOrFocused() ? 0xFF536777 : 0x70303E49);
            if (selected || active && isHoveredOrFocused()) graphics.renderOutline(x, y, size, size, selected ? 0xFF7ED7F2 : 0xFFB4C6CF);
            if (choice != null) {
                graphics.renderItem(choice.sample(), x + iconOffset, y + iconOffset);
                graphics.renderItemDecorations(font, choice.sample(), x + iconOffset, y + iconOffset, Integer.toString(choice.available()));
            }
        }
    }
}
