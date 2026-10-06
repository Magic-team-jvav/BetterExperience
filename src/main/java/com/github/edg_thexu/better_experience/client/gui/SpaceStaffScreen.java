package com.github.edg_thexu.better_experience.client.gui;

import com.github.edg_thexu.better_experience.client.gui.widget.BlockIconButton;
import com.github.edg_thexu.better_experience.client.gui.widget.LiveSearchBox;
import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.item.SpaceStaff;
import com.github.edg_thexu.better_experience.module.spacestaff.SpacePlacementRules;
import com.github.edg_thexu.better_experience.module.spacestaff.SpaceShape;
import com.github.edg_thexu.better_experience.networks.c2s.SpaceStaffSettingsPacketC2S;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** A compact paged editor: inventory materials in survival, all supported block items in creative. */
public final class SpaceStaffScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 220;
    private static final int PAGE_SIZE = 5;
    private static final int GRID_COLUMNS = 7;
    private static final int GRID_ROWS = 5;
    private static final int BLOCK_PAGE_SIZE = GRID_COLUMNS * GRID_ROWS;
    private final int slot;
    private final ItemStack source;
    private final SpaceStaffSettings originalSettings;
    private SpaceShape shape;
    private BlockState state;
    private final List<BlockPos> controlPoints = new ArrayList<>();
    private int controlIndex;
    private String query = "";
    private int blockPage;
    private int propertyPage;
    private final List<BlockEntry> blocks = new ArrayList<>();
    private List<BlockEntry> results = List.of();
    private final List<BlockIconButton> blockButtons = new ArrayList<>();
    private Button previousBlocks;
    private Button blockPages;
    private Button nextBlocks;
    private final List<EditBox> controls = new ArrayList<>();
    private EditBox search;
    private Button apply;
    private int left;
    private int top;
    private boolean externalPreview;
    private boolean rebuildRequested;
    private final BlockStatePreview preview = new BlockStatePreview();

    private record BlockEntry(Block block, String name, String id) {
        private static BlockEntry of(Block block) {
            return new BlockEntry(block, block.getName().getString().toLowerCase(Locale.ROOT),
                    BuiltInRegistries.BLOCK.getKey(block).toString());
        }
    }

    public SpaceStaffScreen(int slot, ItemStack stack) {
        super(Component.translatable("better_experience.space.title"));
        this.slot = slot;
        this.source = stack;
        SpaceStaffSettings settings = stack.getOrDefault(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get(), SpaceStaffSettings.defaults());
        originalSettings = settings;
        shape = settings.shape();
        state = settings.state();
        controlPoints.addAll(settings.controls());
    }

    @Override
    protected void init() {
        externalPreview = width >= PANEL_WIDTH + 88;
        left = (width - PANEL_WIDTH - (externalPreview ? 72 : 0)) / 2;
        top = Math.max(0, (height - PANEL_HEIGHT) / 2);
        blocks.clear();
        BuiltInRegistries.BLOCK.stream().filter(block -> SpacePlacementRules.supported(block.defaultBlockState()))
                .filter(block -> minecraft.player.isCreative() || minecraft.player.getInventory().items.stream()
                        .anyMatch(stack -> SpacePlacementRules.isMaterial(stack, block.defaultBlockState()))
                        || SpacePlacementRules.isMaterial(minecraft.player.getOffhandItem(), block.defaultBlockState()))
                .sorted(Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString()))
                .map(BlockEntry::of).forEach(blocks::add);
        rebuild();
    }

    private void filterBlocks() {
        SpaceBlockSearch search = SpaceBlockSearch.parse(query);
        results = blocks.stream().filter(entry -> search.matches(entry.name(), entry.id())).toList();
        updateBlockPage();
    }

    private int blockPageCount() {
        return Math.max(1, (results.size() + BLOCK_PAGE_SIZE - 1) / BLOCK_PAGE_SIZE);
    }

    private void changeBlockPage(int delta) {
        blockPage = Math.floorMod(blockPage + delta, blockPageCount());
        updateBlockPage();
    }

    private void updateBlockPage() {
        int pages = blockPageCount();
        blockPage = Math.min(blockPage, pages - 1);
        for (int cell = 0; cell < blockButtons.size(); cell++) {
            int index = blockPage * BLOCK_PAGE_SIZE + cell;
            Block block = index < results.size() ? results.get(index).block() : null;
            blockButtons.get(cell).setBlock(block, state.getBlock() == block);
        }
        blockPages.setMessage(Component.literal((blockPage + 1) + "/" + pages));
        previousBlocks.active = nextBlocks.active = blockPages.active = pages > 1;
    }

    private void rememberControls() {
        if (controlPoints.isEmpty() || controls.size() != 3
                || controls.stream().anyMatch(box -> !validNumber(box.getValue()))) return;
        controlPoints.set(controlIndex, new BlockPos(number(0), number(1), number(2)));
    }

    private void selectControl(int index) {
        rememberControls();
        controls.clear();
        controlIndex = index;
        requestRebuild();
    }

    private void addControl() {
        rememberControls();
        controls.clear();
        if (controlPoints.size() >= SpaceStaffSettings.MAX_CONTROLS) return;
        int insertion = controlPoints.isEmpty() ? 0 : controlIndex + 1;
        BlockPos before = insertion == 0 ? BlockPos.ZERO : controlPoints.get(insertion - 1);
        BlockPos after = insertion < controlPoints.size() ? controlPoints.get(insertion) : new BlockPos(100, 100, 100);
        controlPoints.add(insertion, new BlockPos((before.getX() + after.getX()) / 2,
                (before.getY() + after.getY()) / 2, (before.getZ() + after.getZ()) / 2));
        controlIndex = insertion;
        requestRebuild();
    }

    private void removeControl() {
        rememberControls();
        controls.clear();
        if (controlPoints.isEmpty()) return;
        controlPoints.remove(controlIndex);
        controlIndex = Math.max(0, Math.min(controlIndex, controlPoints.size() - 1));
        requestRebuild();
    }

    private int number(int index) { return Integer.parseInt(controls.get(index).getValue()); }

    private static boolean validNumber(String text) {
        try { int value = Integer.parseInt(text); return value >= 0 && value <= 100; }
        catch (NumberFormatException exception) { return false; }
    }

    private void rebuild() {
        rebuildRequested = false;
        rememberControls();
        boolean searchFocused = search != null && search.isFocused();
        int searchCursor = search == null ? query.length() : search.getCursorPosition();
        setFocused(null);
        clearWidgets();
        controls.clear();
        blockButtons.clear();
        search = new LiveSearchBox(font, left + 8, top + 26, 128, 18,
                Component.translatable("better_experience.space.search"), query, text -> {
            query = text;
            blockPage = 0;
            filterBlocks();
        });
        search.setHint(Component.translatable("better_experience.space.search"));
        search.setTooltip(Tooltip.create(Component.translatable("better_experience.space.search_help")));
        addRenderableWidget(search);
        int shapeWidth = externalPreview ? 146 : 88;
        Button shapeButton = addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(
                Component.translatable(shape.translationKey()).getString(), shapeWidth - 8)), button -> {
            rememberControls();
            minecraft.setScreen(new SpaceShapeWheelScreen(slot, source, shape, state, this, false,
                    selected -> shape = selected));
        }).bounds(left + 146, top + 26, shapeWidth, 18).build());
        Component shapeTip = Component.translatable("better_experience.space.wheel");
        if (shape == SpaceShape.BEZIER) shapeTip = shapeTip.copy().append("\n")
                .append(Component.translatable("better_experience.space.bezier_help"));
        shapeButton.setTooltip(Tooltip.create(shapeTip));

        for (int cell = 0; cell < BLOCK_PAGE_SIZE; cell++) {
            BlockIconButton button = new BlockIconButton(left + 8 + cell % GRID_COLUMNS * BlockIconButton.SIZE,
                    top + 49 + cell / GRID_COLUMNS * BlockIconButton.SIZE, clicked -> {
                Block block = ((BlockIconButton) clicked).block();
                if (block != null && state.getBlock() != block) {
                    state = block.defaultBlockState();
                    propertyPage = 0;
                    requestRebuild();
                }
            });
            blockButtons.add(addRenderableWidget(button));
        }
        previousBlocks = addRenderableWidget(Button.builder(Component.literal("<"), button -> changeBlockPage(-1))
                .bounds(left + 8, top + 141, 22, 17).build());
        blockPages = addRenderableWidget(Button.builder(Component.empty(), button -> changeBlockPage(1))
                .bounds(left + 33, top + 141, 42, 17).build());
        nextBlocks = addRenderableWidget(Button.builder(Component.literal(">"), button -> changeBlockPage(1))
                .bounds(left + 78, top + 141, 20, 17).build());
        filterBlocks();
        addRenderableWidget(Button.builder(Component.translatable("better_experience.space.pick"), button -> {
            if (minecraft.hitResult instanceof BlockHitResult hit) {
                BlockState picked = minecraft.level.getBlockState(hit.getBlockPos());
                if (SpacePlacementRules.supported(picked)) { state = picked; propertyPage = 0; requestRebuild(); }
            }
        }).bounds(left + 101, top + 141, 35, 17).build());

        List<Property<?>> properties = new ArrayList<>(state.getProperties());
        properties.sort(Comparator.comparing(Property::getName));
        int propertyPages = Math.max(1, (properties.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        propertyPage = Math.min(propertyPage, propertyPages - 1);
        for (int row = 0; row < PAGE_SIZE; row++) {
            int index = propertyPage * PAGE_SIZE + row;
            if (index >= properties.size()) break;
            Property<?> property = properties.get(index);
            String label = propertyLabel(property);
            Button button = Button.builder(Component.literal(font.plainSubstrByWidth(label, 134)),
                    ignored -> { state = state.cycle(property); requestRebuild(); })
                    .bounds(left + 146, top + 49 + row * 18, 146, 17).build();
            button.setTooltip(Tooltip.create(Component.literal(label)));
            addRenderableWidget(button);
        }
        addRenderableWidget(Button.builder(Component.translatable("better_experience.space.properties", propertyPage + 1, propertyPages),
                button -> { propertyPage = (propertyPage + 1) % propertyPages; requestRebuild(); })
                .bounds(left + 146, top + 141, 146, 17).build());

        if (shape == SpaceShape.BEZIER) {
            boolean hasControl = !controlPoints.isEmpty();
            BlockPos selected = hasControl ? controlPoints.get(controlIndex) : BlockPos.ZERO;
            int[] values = {selected.getX(), selected.getY(), selected.getZ()};
            Button previous = addRenderableWidget(Button.builder(Component.literal("<"),
                    button -> selectControl(Math.floorMod(controlIndex - 1, controlPoints.size())))
                    .bounds(left + 8, top + 176, 20, 16).build());
            Button next = addRenderableWidget(Button.builder(Component.literal(">"),
                    button -> selectControl((controlIndex + 1) % controlPoints.size()))
                    .bounds(left + 31, top + 176, 20, 16).build());
            previous.active = next.active = controlPoints.size() > 1;
            for (int i = 0; i < 3; i++) {
                EditBox field = new EditBox(font, left + 79 + i * 36, top + 176, 32, 16,
                        Component.literal("P" + (controlIndex + 1) + " " + "XYZ".charAt(i) + " %"));
                field.setTooltip(Tooltip.create(Component.translatable("better_experience.space.control",
                        "P" + (controlIndex + 1), "XYZ".substring(i, i + 1))));
                field.setMaxLength(3);
                field.setFilter(text -> text.isEmpty() || text.matches("[0-9]{1,3}"));
                field.setValue(Integer.toString(values[i]));
                field.setEditable(hasControl);
                field.setResponder(text -> updateApply());
                controls.add(field);
                addRenderableWidget(field);
            }
            Button add = addRenderableWidget(Button.builder(Component.translatable("better_experience.space.add_control"),
                    button -> addControl()).bounds(left + 190, top + 176, 48, 16).build());
            Button remove = addRenderableWidget(Button.builder(Component.translatable("better_experience.space.remove_control"),
                    button -> removeControl()).bounds(left + 242, top + 176, 50, 16).build());
            add.active = controlPoints.size() < SpaceStaffSettings.MAX_CONTROLS;
            remove.active = hasControl;
        }
        apply = addRenderableWidget(Button.builder(Component.translatable("better_experience.space.apply"), button -> {
            rememberControls();
            SpaceStaffSettings settings = originalSettings.edited(shape, state, controlPoints);
            source.set(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get(), settings);
            PacketDistributor.sendToServer(new SpaceStaffSettingsPacketC2S(slot, settings));
            onClose();
        }).bounds(left + 8, top + 199, 284, 18).build());
        updateApply();
        if (searchFocused) {
            search.setCursorPosition(searchCursor);
            search.setHighlightPos(searchCursor);
            setFocused(search);
        }
    }

    private <T extends Comparable<T>> String propertyLabel(Property<T> property) {
        String name = property.getName();
        String value = property.getName(state.getValue(property));
        String nameKey = "better_experience.space.property." + name;
        String valueKey = "better_experience.space.value." + value;
        return (net.minecraft.client.resources.language.I18n.exists(nameKey) ? Component.translatable(nameKey).getString() : name)
                + ": " + (net.minecraft.client.resources.language.I18n.exists(valueKey) ? Component.translatable(valueKey).getString() : value);
    }

    private void updateApply() {
        if (apply != null) apply.active = controls.stream().allMatch(field -> validNumber(field.getValue()));
    }

    private void requestRebuild() { rebuildRequested = true; }

    private void rebuildIfRequested() {
        if (rebuildRequested) rebuild();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        // Parent event dispatch must finish assigning focus before replacing any children.
        rebuildIfRequested();
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        rebuildIfRequested();
        return handled;
    }

    @Override
    public void tick() {
        if (minecraft.player == null || minecraft.level == null
                || minecraft.player.getInventory().selected != slot
                || minecraft.player.getMainHandItem() != source
                || !(source.getItem() instanceof SpaceStaff)) {
            onClose();
            return;
        }
        rebuildIfRequested();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= left + 8 && mouseX < left + 136 && mouseY >= top + 49 && mouseY < top + 159 && scrollY != 0) {
            changeBlockPage(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0202630);
        graphics.drawCenteredString(font, title, left + (externalPreview ? PANEL_WIDTH / 2 : 120), top + 7, 0xFFFFFF);
        String summary = font.plainSubstrByWidth(state.getBlock().getName().getString(), 230);
        if (shape != SpaceShape.BEZIER) graphics.drawString(font, summary, left + 8, top + 163, 0xA7E6FF, false);
        if (shape == SpaceShape.BEZIER) {
            graphics.drawString(font, controlPoints.isEmpty() ? "-" : "P" + (controlIndex + 1),
                    left + 56, top + 180, 0xFFFFFF, false);
            graphics.drawString(font, controlPoints.size() + "/" + SpaceStaffSettings.MAX_CONTROLS,
                    left + 8, top + 164, 0xFFFFFF, false);
            for (int i = 0; i < 3; i++) graphics.drawString(font, "XYZ".substring(i, i + 1),
                    left + 79 + i * 36, top + 166, 0xFFFFFF, false);
        }
        // Screen.render draws the background again, blurring the panel and its labels.
        for (var widget : renderables) widget.render(graphics, mouseX, mouseY, partialTick);
        if (results.isEmpty()) {
            graphics.drawWordWrap(font, Component.translatable("better_experience.space.no_results"), left + 14, top + 84, 112, 0xA7E6FF);
        }
        int previewX = left + (externalPreview ? PANEL_WIDTH + 6 : 242);
        int previewY = top + (externalPreview ? 26 : 3);
        int previewSize = externalPreview ? 64 : 40;
        preview.render(graphics, state, previewX, previewY, previewSize);
        if (externalPreview) {
            graphics.drawCenteredString(font, Component.translatable("better_experience.space.block_state"),
                    previewX + previewSize / 2, previewY + previewSize + 5, 0xA7E6FF);
        }
        if (mouseX >= previewX && mouseX < previewX + previewSize
                && mouseY >= previewY && mouseY < previewY + previewSize) {
            graphics.renderTooltip(font, state.getBlock().getName(), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
