package com.github.edg_thexu.better_experience.module.antirage;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.module.spawning.SpawnController;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class AntiRageSettings {
    private AntiRageSettings() {}

    public static boolean update(ServerPlayer player, boolean enabled) {
        if (!SpawnController.canEdit(player)) return false;
        boolean previous = CommonConfig.ANTI_RAGE_MODE.get();
        if (previous == enabled) return true;
        CommonConfig.ANTI_RAGE_MODE.set(enabled);
        try {
            CommonConfig.ANTI_RAGE_MODE.save();
            return true;
        } catch (RuntimeException exception) {
            CommonConfig.ANTI_RAGE_MODE.set(previous);
            Better_experience.LOGGER.error("Cannot save anti-rage setting", exception);
            player.sendSystemMessage(Component.translatable("better_experience.anti_rage.prompt.save_failed"));
            return false;
        }
    }
}
