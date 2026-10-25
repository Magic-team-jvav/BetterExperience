package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.module.spawning.SpawnContext;
import com.github.edg_thexu.better_experience.module.spawning.SpawnSettings;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(NaturalSpawner.class)
public abstract class SpawnRateMixin {
    @WrapMethod(method = "spawnForChunk")
    private static void betterExperience$spawn(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState state,
                                               boolean friendly, boolean hostile, boolean persistent,
                                               Operation<Void> original) {
        SpawnSettings settings = SpawnSettings.get(level.getServer());
        if (settings.rate() == 0.0 || settings.cap() == 0.0) return;
        Double previous = SpawnContext.enter(settings.cap());
        try {
            int attempts = SpawnContext.attempts(settings.rate(), level.random);
            for (int i = 0; i < attempts; i++) {
                // Re-enter the vanilla cap checks on every pass; Confluence retains its own modifiers.
                original.call(level, chunk, state, friendly, hostile, persistent);
            }
        } finally {
            SpawnContext.restore(previous);
        }
    }
}
