package com.github.edg_thexu.better_experience.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class StaffKeyMappings {
    public static final KeyMapping SELECT_MODIFIER = new KeyMapping(
            "key.better_experience.staff_select_modifier", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, "key.categories.better_experience");

    public static final KeyMapping SPACE_SETTINGS = new KeyMapping(
            "key.better_experience.space_settings", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.better_experience");

    private StaffKeyMappings() {}
}
