package com.github.edg_thexu.better_experience.client.gui.widget;

import com.github.edg_thexu.better_experience.client.gui.FluidBeakerPreview;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;

public final class FluidIconButton extends Button {
    public static final int WIDTH = 22;
    public static final int HEIGHT = 30;
    private FluidStack fluid = FluidStack.EMPTY;
    private boolean selected;

    public FluidIconButton(int x, int y, OnPress onPress) {
        super(x, y, WIDTH, HEIGHT, Component.empty(), onPress, DEFAULT_NARRATION);
    }

    public FluidStack fluid() { return fluid.copy(); }

    public void setFluid(FluidStack fluid, boolean selected, int amount, boolean infinite) {
        this.fluid = fluid.copy();
        this.selected = selected;
        active = !fluid.isEmpty();
        setMessage(active ? fluid.getHoverName() : Component.empty());
        setTooltip(active ? Tooltip.create(Component.empty().append(fluid.getHoverName()).append("\n")
                .append(BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString()).append("\n")
                .append(infinite ? "∞ ml" : amount + " ml")) : null);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY();
        boolean hovered = active && isHoveredOrFocused();
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, selected ? 0xD0406575 : hovered ? 0xB0526070 : 0x70212B35);
        if (selected || hovered) graphics.renderOutline(x, y, WIDTH, HEIGHT, selected ? 0xFF78CCE6 : 0xFFB6C4D0);
        if (active) FluidBeakerPreview.render(graphics, fluid, 1, 1, false, x + 2, y + 2, WIDTH - 4, HEIGHT - 4, false);
    }
}
