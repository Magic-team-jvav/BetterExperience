package com.github.edg_thexu.better_experience.client.gui;

import com.github.edg_thexu.better_experience.client.StaffSelectionHandler;
import com.github.edg_thexu.better_experience.client.gui.widget.FluidIconButton;
import com.github.edg_thexu.better_experience.client.gui.widget.LiveSearchBox;
import com.github.edg_thexu.better_experience.data.component.FluidStaffSettings;
import com.github.edg_thexu.better_experience.data.component.FluidStaffStorage;
import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.item.FluidStaff;
import com.github.edg_thexu.better_experience.module.fluidstaff.FluidCatalog;
import com.github.edg_thexu.better_experience.networks.c2s.FluidStaffSettingsPacketC2S;
import com.github.edg_thexu.better_experience.networks.c2s.SpaceStaffSettingsPacketC2S;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Fits a 320x240 GUI (480p at 2x) without hiding the capacity or selected fluid. */
public final class FluidStaffScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 216;
    private static final int COLUMNS = 6;
    private static final int PAGE_SIZE = COLUMNS * 3;
    private final int slot;
    private FluidStaffSettings settings;
    private FluidStaffStorage lastStorage;
    private List<FluidStack> catalog = List.of();
    private List<FluidStack> results = List.of();
    private final List<FluidIconButton> icons = new ArrayList<>();
    private String query = "";
    private int page;
    private int left;
    private int top;
    private Button pages;
    private Button previous;
    private Button next;
    private Button mode;
    private Button shape;
    private boolean creative;

    public FluidStaffScreen(int slot, ItemStack stack) {
        super(Component.translatable("better_experience.fluid.title"));
        this.slot = slot;
        settings = stack.getOrDefault(ModDataComponentTypes.FLUID_STAFF_SETTINGS.get(), FluidStaffSettings.defaults());
    }

    private ItemStack staff() { return minecraft.player.getMainHandItem(); }
    private FluidStaffStorage storage() {
        return staff().getOrDefault(ModDataComponentTypes.FLUID_STAFF_STORAGE.get(), FluidStaffStorage.EMPTY);
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = Math.max(0, (height - PANEL_HEIGHT) / 2);
        creative = minecraft.player.isCreative();
        icons.clear();
        mode = addRenderableWidget(Button.builder(modeLabel(), button -> {
            settings = new FluidStaffSettings(!settings.placing(), settings.fluid());
            save();
            button.setMessage(modeLabel());
        }).bounds(left + 10, top + 22, 134, 20).build());
        var geometry = staff().getOrDefault(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get(), SpaceStaffSettings.defaults());
        shape = addRenderableWidget(Button.builder(Component.translatable(geometry.shape().translationKey()), button -> {
            var current = staff().getOrDefault(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get(), SpaceStaffSettings.defaults());
            minecraft.setScreen(new SpaceShapeWheelScreen(slot, staff(), current.shape(), current.state(), this, false, chosen -> {
                var latest = staff().getOrDefault(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get(), SpaceStaffSettings.defaults()).withShape(chosen);
                staff().set(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get(), latest);
                PacketDistributor.sendToServer(new SpaceStaffSettingsPacketC2S(slot, latest));
            }));
        }).bounds(left + 156, top + 22, 134, 20).build());
        EditBox search = addRenderableWidget(new LiveSearchBox(font, left + 10, top + 48, 134, 18,
                Component.translatable("better_experience.fluid.search"), query, text -> {
                    query = text;
                    page = 0;
                    filter();
                }));
        search.setHint(Component.translatable("better_experience.fluid.search"));
        for (int cell = 0; cell < PAGE_SIZE; cell++) {
            icons.add(addRenderableWidget(new FluidIconButton(left + 10 + cell % COLUMNS * FluidIconButton.WIDTH,
                    top + 70 + cell / COLUMNS * FluidIconButton.HEIGHT, button -> {
                        settings = new FluidStaffSettings(settings.placing(), ((FluidIconButton) button).fluid());
                        save();
                        updatePage();
                    })));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(left + 10, top + 164, 24, 18).build());
        pages = addRenderableWidget(Button.builder(Component.empty(), button -> changePage(1))
                .bounds(left + 38, top + 164, 78, 18).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(left + 120, top + 164, 24, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("better_experience.fluid.clear_selection"), button ->
                StaffSelectionHandler.clearSelection()).bounds(left + 10, top + 188, 134, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + 156, top + 188, 134, 20).build());
        refreshCatalog();
    }

    private Component modeLabel() {
        return Component.translatable(settings.placing() ? "better_experience.fluid.mode.place" : "better_experience.fluid.mode.absorb");
    }

    private void save() {
        staff().set(ModDataComponentTypes.FLUID_STAFF_SETTINGS.get(), settings);
        PacketDistributor.sendToServer(new FluidStaffSettingsPacketC2S(slot, settings));
    }

    private void refreshCatalog() {
        lastStorage = storage();
        var entries = new ArrayList<>(lastStorage.fluids());
        for (FluidStack fluid : FluidCatalog.fluids()) {
            if (entries.stream().noneMatch(entry -> FluidStack.isSameFluidSameComponents(entry, fluid))) entries.add(fluid);
        }
        entries.sort(Comparator.comparing(fluid -> BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString()));
        catalog = List.copyOf(entries);
        filter();
    }

    private void filter() {
        var search = SpaceBlockSearch.parse(query);
        results = catalog.stream().filter(fluid -> search.matches(fluid.getHoverName().getString().toLowerCase(Locale.ROOT),
                BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString())).toList();
        updatePage();
    }

    private int pageCount() { return Math.max(1, (results.size() + PAGE_SIZE - 1) / PAGE_SIZE); }
    private void changePage(int delta) { page = Math.floorMod(page + delta, pageCount()); updatePage(); }

    private void updatePage() {
        int count = pageCount();
        page = Math.min(page, count - 1);
        for (int cell = 0; cell < icons.size(); cell++) {
            int index = page * PAGE_SIZE + cell;
            var fluid = index < results.size() ? results.get(index) : FluidStack.EMPTY;
            icons.get(cell).setFluid(fluid, FluidStack.isSameFluidSameComponents(fluid, settings.fluid()),
                    lastStorage.amount(fluid), creative);
        }
        pages.setMessage(Component.literal((page + 1) + "/" + count));
        previous.active = next.active = pages.active = count > 1;
    }

    @Override
    public void tick() {
        // Component synchronization may replace the stack while this non-pausing screen is open.
        if (minecraft.player == null || minecraft.level == null || minecraft.player.getInventory().selected != slot
                || !(staff().getItem() instanceof FluidStaff)) {
            minecraft.setScreen(null);
            return;
        }
        boolean changed = creative != minecraft.player.isCreative();
        creative = minecraft.player.isCreative();
        if (changed || !storage().equals(lastStorage)) refreshCatalog();
        var current = staff().getOrDefault(ModDataComponentTypes.FLUID_STAFF_SETTINGS.get(), FluidStaffSettings.defaults());
        if (!current.equals(settings)) { settings = current; mode.setMessage(modeLabel()); updatePage(); }
        shape.setMessage(Component.translatable(staff().getOrDefault(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get(),
                SpaceStaffSettings.defaults()).shape().translationKey()));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX >= left + 10 && mouseX < left + 144 && mouseY >= top + 70 && mouseY < top + 182 && vertical != 0) {
            changePage(vertical > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xEB19242F);
        graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF567585);
        graphics.drawCenteredString(font, title, left + PANEL_WIDTH / 2, top + 7, 0xFFFFFF);
        var fluid = settings.fluid();
        var storage = storage();
        FluidBeakerPreview.render(graphics, fluid, storage.amount(fluid), FluidStaffStorage.CAPACITY, creative,
                left + 180, top + 48, 86, 105, true);
        graphics.drawCenteredString(font, font.plainSubstrByWidth(fluid.getHoverName().getString(), 134), left + 223, top + 155, 0xD9EDF7);
        graphics.drawCenteredString(font, creative ? "∞ ml" : storage.amount(fluid) + " ml", left + 223, top + 166, 0xA7E6FF);
        graphics.drawCenteredString(font, creative ? Component.translatable("better_experience.fluid.total_infinite")
                : Component.translatable("better_experience.fluid.total", storage.total(), FluidStaffStorage.CAPACITY),
                left + 223, top + 177, 0xAEBECB);
        if (results.isEmpty()) graphics.drawCenteredString(font, Component.translatable("better_experience.fluid.no_results"),
                left + 77, top + 106, 0xAEBECB);
        for (var widget : renderables) widget.render(graphics, mouseX, mouseY, partialTick);
        if (mouseX >= left + 180 && mouseX < left + 266 && mouseY >= top + 48 && mouseY < top + 154) {
            graphics.renderTooltip(font, List.of(fluid.getHoverName(), Component.translatable("better_experience.fluid.unit")),
                    java.util.Optional.empty(), mouseX, mouseY);
        }
    }
}
