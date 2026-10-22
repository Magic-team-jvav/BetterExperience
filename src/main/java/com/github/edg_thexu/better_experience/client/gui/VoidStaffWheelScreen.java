package com.github.edg_thexu.better_experience.client.gui;

import com.github.edg_thexu.better_experience.client.StaffKeyMappings;
import com.github.edg_thexu.better_experience.client.voidstaff.VoidStaffClient;
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
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/** Uses the space staff's radial layout, with distance and block-state actions instead of shapes. */
public final class VoidStaffWheelScreen extends Screen {
    private static final String[] ACTIONS = {"rotate_left", "rotate_right", "closer", "farther", "previous_property", "next_property", "previous_value", "next_value"};
    private final UUID token;
    private final BlockStatePreview preview = new BlockStatePreview();
    private SpaceWheelLayout layout;
    private int selected = -1;
    private boolean clicked;

    public VoidStaffWheelScreen() {
        super(Component.translatable("better_experience.void_staff.wheel"));
        token = VoidStaffClient.cargo().token();
    }
    @Override protected void init() { layout = new SpaceWheelLayout(33, Math.min(94, (height - 46) / 2.0), ACTIONS.length); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void tick() {
        if (!VoidStaffClient.active() || !token.equals(VoidStaffClient.cargo().token())) onClose();
    }
    private void apply() {
        switch (selected) {
            case 0 -> VoidStaffClient.rotate(-1);
            case 1 -> VoidStaffClient.rotate(1);
            case 2 -> VoidStaffClient.moveDistance(-0.25);
            case 3 -> VoidStaffClient.moveDistance(0.25);
            case 4 -> VoidStaffClient.selectProperty(-1);
            case 5 -> VoidStaffClient.selectProperty(1);
            case 6 -> VoidStaffClient.cycleValue(-1);
            case 7 -> VoidStaffClient.cycleValue(1);
        }
    }
    @Override public void mouseMoved(double x, double y) {
        selected = layout.sectorAt(x - width / 2.0, y - height / 2.0);
        super.mouseMoved(x, y);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { onClose(); return true; }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(x, y, button);
        clicked = true;
        selected = layout.sectorAt(x - width / 2.0, y - height / 2.0);
        if (Math.hypot(x - width / 2.0, y - height / 2.0) < layout.innerRadius()) VoidStaffClient.surface();
        else apply();
        return true;
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (vertical != 0) VoidStaffClient.cycleValue(vertical > 0 ? 1 : -1);
        return true;
    }
    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        if (StaffKeyMappings.SPACE_SETTINGS.matches(key, scan)) {
            if (!clicked) apply();
            onClose(); return true;
        }
        return super.keyReleased(key, scan, modifiers);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (StaffKeyMappings.SPACE_SETTINGS.matchesMouse(button)) {
            if (!clicked) apply();
            onClose(); return true;
        }
        return super.mouseReleased(x, y, button);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (VoidStaffClient.cargo() == null) return;
        graphics.fill(0, 0, width, height, 0x98202832);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xFFFFFF);
        graphics.flush();
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.disableCull(); RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = graphics.pose().last().pose();
        for (int index = 0; index < ACTIONS.length; index++) {
            int color = selected == index ? 0xEE635395 : 0xDC252339;
            double start = layout.centerAngle(index) - Math.PI / ACTIONS.length + 0.015;
            double end = layout.centerAngle(index) + Math.PI / ACTIONS.length - 0.015;
            for (int step = 0; step < 12; step++) {
                double a = start + (end - start) * step / 12;
                double b = start + (end - start) * (step + 1) / 12;
                vertex(buffer, matrix, layout.innerRadius(), a, color); vertex(buffer, matrix, layout.outerRadius(), a, color);
                vertex(buffer, matrix, layout.outerRadius(), b, color); vertex(buffer, matrix, layout.innerRadius(), a, color);
                vertex(buffer, matrix, layout.outerRadius(), b, color); vertex(buffer, matrix, layout.innerRadius(), b, color);
            }
        }
        try { BufferUploader.drawWithShader(buffer.buildOrThrow()); }
        finally { RenderSystem.disableBlend(); RenderSystem.enableCull(); RenderSystem.enableDepthTest(); }
        double radius = (layout.innerRadius() + layout.outerRadius()) / 2;
        for (int i = 0; i < ACTIONS.length; i++) {
            int x = width / 2 + (int) (Math.cos(layout.centerAngle(i)) * radius);
            int y = height / 2 + (int) (Math.sin(layout.centerAngle(i)) * radius);
            var lines = font.split(Component.translatable("better_experience.void_staff." + ACTIONS[i]), 50);
            for (int j = 0; j < lines.size(); j++) graphics.drawCenteredString(font, lines.get(j), x, y - lines.size() * 5 + j * 10, 0xFFFFFF);
        }
        preview.render(graphics, VoidStaffClient.cargo().state(), width / 2 - 16, height / 2 - 25, 32);
        graphics.drawCenteredString(font, font.plainSubstrByWidth(VoidStaffClient.propertyLabel(), 96), width / 2, height / 2 + 11, 0xDBBEFF);
        graphics.drawCenteredString(font, font.plainSubstrByWidth(Component.translatable("better_experience.void_staff.wheel_help").getString(), width - 12),
                width / 2, height - 14, 0xDDD4EE);
    }
    private void vertex(BufferBuilder buffer, Matrix4f matrix, double radius, double angle, int color) {
        buffer.addVertex(matrix, width / 2.0F + (float) (Math.cos(angle) * radius), height / 2.0F + (float) (Math.sin(angle) * radius), 0).setColor(color);
    }
}
