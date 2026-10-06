package com.github.edg_thexu.better_experience.client.gui.hud;

import com.github.edg_thexu.better_experience.client.gui.BlockStatePreview;
import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.item.SpaceStaff;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.HumanoidArm;

public final class SpaceStaffHud {
    private static final BlockStatePreview PREVIEW = new BlockStatePreview();

    private SpaceStaffHud() {}

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null
                || minecraft.options.hideGui || minecraft.player.isSpectator()
                || !(minecraft.player.getMainHandItem().getItem() instanceof SpaceStaff)) {
            PREVIEW.clear();
            return;
        }
        var settings = minecraft.player.getMainHandItem().getOrDefault(
                ModDataComponentTypes.SPACE_STAFF_SETTINGS.get(), SpaceStaffSettings.defaults());
        int x = graphics.guiWidth() / 2 + 98;
        int size = Math.min(44, graphics.guiWidth() - x - 4);
        if (size < 24) return;
        // Left-handed players put their offhand slot on the right of the hotbar.
        boolean rightOffhand = minecraft.player.getMainArm() == HumanoidArm.LEFT && !minecraft.player.getOffhandItem().isEmpty();
        int y = graphics.guiHeight() - size - (rightOffhand ? 34 : 12);
        PREVIEW.render(graphics, settings.state(), x, y, size);
        graphics.drawCenteredString(minecraft.font, minecraft.font.plainSubstrByWidth(
                settings.state().getBlock().getName().getString(), size), x + size / 2, y + size + 2, 0xA7E6FF);
    }
}
