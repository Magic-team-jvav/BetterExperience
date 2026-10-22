package com.github.edg_thexu.better_experience.client;

import com.github.edg_thexu.better_experience.block.ChronopilotBlock;
import com.github.edg_thexu.better_experience.menu.ChronopilotMenu;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.confluence.terra_curio.client.handler.InformationHandler;

/** Adds a watch entry to the existing information HUD without changing accessory data. */
public final class ChronopilotInformation {
    public static final int RANGE = 16;

    private ChronopilotInformation() {}

    public static void addTime(LocalPlayer player) {
        if (InformationHandler.DISABLE[InformationHandler.WATCH] || !nearClock(player)) return;
        InformationHandler.getInformation().put(InformationHandler.WATCH,
                Component.translatable("better_experience.chronopilot.current", ChronopilotMenu.formatTime(player.level().getDayTime())));
    }

    private static boolean nearClock(LocalPlayer player) {
        ClientLevel level = player.clientLevel;
        BlockPos origin = player.blockPosition();
        // Inspect only block entities in already loaded chunks, never every block in the volume.
        for (int x = (origin.getX() - RANGE) >> 4; x <= (origin.getX() + RANGE) >> 4; x++) {
            for (int z = (origin.getZ() - RANGE) >> 4; z <= (origin.getZ() + RANGE) >> 4; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof ChronopilotBlock.Entity && !entity.isRemoved()
                            && player.distanceToSqr(entity.getBlockPos().getCenter().add(0, 0.5, 0)) <= RANGE * RANGE) return true;
                }
            }
        }
        return false;
    }
}
