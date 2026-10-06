package com.github.edg_thexu.better_experience.client.gui.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Vanilla item rendering gives the same animated textures and models as an ingredient browser. */
public final class BlockIconButton extends Button {
    public static final int SIZE = 18;
    private Block block;
    private ItemStack icon = ItemStack.EMPTY;
    private boolean selected;

    public BlockIconButton(int x, int y, OnPress onPress) {
        super(x, y, SIZE, SIZE, Component.empty(), onPress, DEFAULT_NARRATION);
    }

    public Block block() { return block; }

    public void setBlock(Block block, boolean selected) {
        if (this.block != block) {
            this.block = block;
            icon = block == null ? ItemStack.EMPTY : new ItemStack(block);
            setMessage(block == null ? Component.empty() : block.getName());
            setTooltip(block == null ? null : Tooltip.create(Component.empty().append(block.getName())
                    .append("\n").append(Component.literal(BuiltInRegistries.BLOCK.getKey(block).toString())
                            .withStyle(ChatFormatting.GRAY))));
        }
        this.selected = selected;
        active = block != null;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY();
        boolean highlighted = active && isHoveredOrFocused();
        graphics.fill(x, y, x + SIZE, y + SIZE, selected ? 0xD0406575 : highlighted ? 0xB0526070 : 0x70212B35);
        if (selected || highlighted) graphics.renderOutline(x, y, SIZE, SIZE, selected ? 0xFF78CCE6 : 0xFFB6C4D0);
        if (!icon.isEmpty()) graphics.renderItem(icon, x + 1, y + 1);
    }
}
