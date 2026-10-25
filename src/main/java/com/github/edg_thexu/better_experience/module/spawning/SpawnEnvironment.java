package com.github.edg_thexu.better_experience.module.spawning;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.event.EventHooks;

import java.util.Map;
import java.util.TreeMap;

/** Candidate table, not a promise that light, collision and placement checks will succeed. */
public final class SpawnEnvironment {
    public static final int MAX_ENTRIES = 2048;
    private static final ThreadLocal<Boolean> DETECTING = ThreadLocal.withInitial(() -> false);

    private SpawnEnvironment() {}

    public record Entry(ResourceLocation id, int weight, int minCount, int maxCount) {}

    public static boolean detecting() { return DETECTING.get(); }

    public static Map<ResourceLocation, Entry> detect(ServerPlayer player) {
        var level = player.serverLevel();
        var biome = level.getBiome(player.blockPosition());
        Map<ResourceLocation, Entry> entries = new TreeMap<>();
        boolean previous = DETECTING.get();
        DETECTING.set(true);
        try {
            for (MobCategory category : MobCategory.values()) {
                if (category == MobCategory.MISC) continue;
                // Always include the complete biome table, even when standing on an unsuitable block.
                for (var data : biome.value().getMobSettings().getMobs(category).unwrap()) add(entries, data);
                for (int dx = -16; dx <= 16; dx += 16) {
                    for (int dz = -16; dz <= 16; dz += 16) {
                        for (int dy = -16; dy <= 16; dy += 16) {
                            BlockPos pos = player.blockPosition().offset(dx, dy, dz);
                            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)
                                    || !level.getBiome(pos).equals(biome)) continue;
                            var generator = level.getChunkSource().getGenerator();
                            var list = generator.getMobsAt(biome, level.structureManager(), category, pos);
                            if (NaturalSpawner.isInNetherFortressBounds(pos, level, category, level.structureManager())) {
                                var fortress = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                                        .get(net.minecraft.world.level.levelgen.structure.BuiltinStructures.FORTRESS);
                                var override = fortress == null ? null : fortress.spawnOverrides().get(category);
                                if (override != null) list = override.spawns();
                            }
                            for (var data : EventHooks.getPotentialSpawns(level, category, pos, list).unwrap()) add(entries, data);
                        }
                    }
                }
            }
        } finally {
            if (previous) DETECTING.set(true);
            else DETECTING.remove();
        }
        return entries;
    }

    private static void add(Map<ResourceLocation, Entry> entries, MobSpawnSettings.SpawnerData data) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(data.type);
        entries.merge(id, new Entry(id, data.getWeight().asInt(), data.minCount, data.maxCount),
                (a, b) -> new Entry(id, Math.max(a.weight, b.weight), Math.min(a.minCount, b.minCount), Math.max(a.maxCount, b.maxCount)));
    }
}
