package com.github.edg_thexu.better_experience.client.gui;

import com.github.edg_thexu.better_experience.client.StaffKeyMappings;
import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.item.SpaceStaff;
import com.github.edg_thexu.better_experience.module.spacestaff.SpaceShape;
import com.github.edg_thexu.better_experience.networks.c2s.SpaceStaffSettingsPacketC2S;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public final class SpaceShapeWheelScreen extends Screen {
    private static final SpaceShape[] SHAPES = SpaceShape.values();
    private final int slot;
    private final ItemStack source;
    private final SpaceShape current;
    private final BlockState state;
    private final Screen parent;
    private final Consumer<SpaceShape> onSelect;
    private final boolean confirmOnRelease;
    private final BlockStatePreview preview = new BlockStatePreview();
    private SpaceWheelLayout layout;
    private int selected = -1;

    public static SpaceShapeWheelScreen forHeldStaff(int slot, ItemStack stack) {
        var settings = stack.getOrDefault(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get(), SpaceStaffSettings.defaults());
        return new SpaceShapeWheelScreen(slot, stack, settings.shape(), settings.state(), null, true, shape -> {
            var latest = stack.getOrDefault(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get(), SpaceStaffSettings.defaults());
            var changed = latest.withShape(shape);
            stack.set(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get(), changed);
            PacketDistributor.sendToServer(new SpaceStaffSettingsPacketC2S(slot, changed));
        });
    }

    public SpaceShapeWheelScreen(int slot, ItemStack source, SpaceShape current, BlockState state,
                                Screen parent, boolean confirmOnRelease, Consumer<SpaceShape> onSelect) {
        super(Component.translatable("better_experience.space.wheel"));
        this.slot = slot;
        this.source = source;
        this.current = current;
        this.state = state;
        this.parent = parent;
        this.confirmOnRelease = confirmOnRelease;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        layout = new SpaceWheelLayout(34, Math.min(94, (height - 52) / 2.0), SHAPES.length);
    }

    @Override
    public void tick() {
        if (minecraft.player == null || minecraft.level == null
                || minecraft.player.getInventory().selected != slot
                || minecraft.player.getMainHandItem() != source || !(source.getItem() instanceof SpaceStaff)) {
            minecraft.setScreen(null);
        }
    }

    private void choose() {
        if (selected >= 0) onSelect.accept(SHAPES[selected]);
        onClose();
    }

    private void openEditor() {
        minecraft.setScreen(parent == null ? new SpaceStaffScreen(slot, source) : parent);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        selected = layout.sectorAt(mouseX - width / 2.0, mouseY - height / 2.0);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            double dx = mouseX - width / 2.0, dy = mouseY - height / 2.0;
            if (Math.hypot(dx, dy) <= layout.innerRadius()) openEditor();
            else {
                selected = layout.sectorAt(dx, dy);
                if (selected >= 0) choose();
            }
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { onClose(); return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (confirmOnRelease && StaffKeyMappings.SPACE_SETTINGS.matches(keyCode, scanCode)) {
            choose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (confirmOnRelease && StaffKeyMappings.SPACE_SETTINGS.matchesMouse(button)) {
            selected = layout.sectorAt(mouseX - width / 2.0, mouseY - height / 2.0);
            choose();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB) { openEditor(); return true; }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) {
            int initial = selected < 0 ? current.ordinal() : selected;
            selected = Math.floorMod(initial + (keyCode == GLFW.GLFW_KEY_LEFT ? -1 : 1), SHAPES.length);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { choose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x98202832);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = graphics.pose().last().pose();
        for (int index = 0; index < SHAPES.length; index++) {
            int color = selected == index ? 0xEE387A96 : current.ordinal() == index ? 0xDC315066 : 0xDC192A39;
            double begin = layout.centerAngle(index) - Math.PI / SHAPES.length + 0.018;
            double end = layout.centerAngle(index) + Math.PI / SHAPES.length - 0.018;
            for (int step = 0; step < 16; step++) {
                double a = begin + (end - begin) * step / 16;
                double b = begin + (end - begin) * (step + 1) / 16;
                vertex(buffer, matrix, layout.innerRadius(), a, color);
                vertex(buffer, matrix, layout.outerRadius(), a, color);
                vertex(buffer, matrix, layout.outerRadius(), b, color);
                vertex(buffer, matrix, layout.innerRadius(), a, color);
                vertex(buffer, matrix, layout.outerRadius(), b, color);
                vertex(buffer, matrix, layout.innerRadius(), b, color);
            }
        }
        try {
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
        }
        double labelRadius = (layout.innerRadius() + layout.outerRadius()) / 2;
        for (int index = 0; index < SHAPES.length; index++) {
            int x = width / 2 + (int) (Math.cos(layout.centerAngle(index)) * labelRadius);
            int y = height / 2 + (int) (Math.sin(layout.centerAngle(index)) * labelRadius);
            var lines = font.split(Component.translatable(SHAPES[index].translationKey()), 66);
            for (int line = 0; line < lines.size(); line++) {
                graphics.drawCenteredString(font, lines.get(line), x, y - lines.size() * 5 + line * 10,
                        current.ordinal() == index ? 0xA7E6FF : 0xFFFFFF);
            }
        }
        preview.render(graphics, state, width / 2 - 18, height / 2 - 27, 36);
        graphics.drawCenteredString(font, Component.translatable("better_experience.space.block_state"),
                width / 2, height / 2 + 15, 0xA7E6FF);
        String hint = Component.translatable(confirmOnRelease ? "better_experience.space.wheel_hold"
                : "better_experience.space.wheel_click").getString();
        graphics.drawCenteredString(font, font.plainSubstrByWidth(hint, width - 12), width / 2, height - 16, 0xC8D6E2);
    }

    private void vertex(BufferBuilder buffer, Matrix4f matrix, double radius, double angle, int color) {
        buffer.addVertex(matrix, width / 2F + (float) (radius * Math.cos(angle)),
                height / 2F + (float) (radius * Math.sin(angle)), 0).setColor(color);
    }
}
