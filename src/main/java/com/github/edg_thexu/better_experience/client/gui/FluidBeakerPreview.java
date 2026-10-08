package com.github.edg_thexu.better_experience.client.gui;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** Two-dimensional glass, fluid atlas texture and a gently animated horizontal surface. */
public final class FluidBeakerPreview {
    private FluidBeakerPreview() {}

    public static void render(GuiGraphics graphics, FluidStack fluid, int amount, int capacity, boolean infinite,
                              int x, int y, int width, int height, boolean waves) {
        int inset = width >= 40 ? 5 : 2;
        int left = x + inset, right = x + width - inset;
        int top = y + 4, bottom = y + height - 3;
        graphics.fill(left, top, right, bottom, 0x60202C3C);
        int filled = FluidBeakerLayout.fillHeight(bottom - top, amount, capacity, infinite);
        if (!fluid.isEmpty() && filled > 0) {
            var extension = IClientFluidTypeExtensions.of(fluid.getFluid());
            var texture = extension.getStillTexture(fluid);
            if (texture != null) {
                var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
                int tint = extension.getTintColor(fluid);
                float red = ((tint >> 16) & 255) / 255F, green = ((tint >> 8) & 255) / 255F, blue = (tint & 255) / 255F;
                float alpha = ((tint >>> 24) & 255) / 255F;
                double time = Util.getMillis() / 1000.0;
                int flatSurface = bottom - filled;
                int bodyTop = waves ? Math.min(bottom, flatSurface + 3) : flatSurface;
                graphics.enableScissor(left, bodyTop, right, bottom);
                for (int column = left; column < right; column += 16) {
                    for (int row = bottom - 16; row > bodyTop - 16; row -= 16) {
                        graphics.blit(column, row, 0, 16, 16, sprite, red, green, blue, alpha);
                    }
                }
                graphics.disableScissor();
                // Only the few pixels around the surface need separate wave clips.
                if (waves) {
                    for (int column = left; column < right; column += 2) {
                        int surface = FluidBeakerLayout.surface(column - left, top, bottom, filled, time, true);
                        int end = Math.min(right, column + 2);
                        graphics.enableScissor(column, surface, end, bodyTop);
                        int tileX = left + ((column - left) / 16) * 16;
                        int tileY = bottom - ((bottom - surface + 15) / 16) * 16;
                        graphics.blit(tileX, tileY, 0, 16, 16, sprite, red, green, blue, alpha);
                        if (tileY + 16 < bodyTop) graphics.blit(tileX, tileY + 16, 0, 16, 16, sprite, red, green, blue, alpha);
                        graphics.disableScissor();
                        graphics.fill(column, surface, end, surface + 1, 0xA0FFFFFF);
                    }
                } else graphics.fill(left, flatSurface, right, flatSurface + 1, 0xA0FFFFFF);
            }
        }
        graphics.fill(left - 1, top, left + 1, bottom + 1, 0xFFE0EDF3);
        graphics.fill(right - 1, top, right + 1, bottom + 1, 0xFFABC5D3);
        graphics.fill(left, bottom, right, bottom + 2, 0xFFBCD5E0);
        graphics.fill(left - 3, y + 2, right + 3, y + 4, 0xFFE0EDF3);
        graphics.fill(left + 2, top + 2, left + 3, bottom - 1, 0x50FFFFFF);
        if (width >= 40) {
            for (int tick = 1; tick < 5; tick++) {
                int markY = bottom - (bottom - top) * tick / 5;
                graphics.fill(right - (tick % 2 == 0 ? 10 : 6), markY, right - 2, markY + 1, 0xAAFFFFFF);
            }
        }
    }
}
