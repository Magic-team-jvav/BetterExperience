package com.github.edg_thexu.better_experience.module.locator;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Stored in the server's overworld data: separate for each player, dimension and registered structure. */
public final class LocatorHistory extends SavedData {
    public static final int MAX_MARKERS_PER_PLAYER = 65_536;
    private static final Factory<LocatorHistory> FACTORY = new Factory<>(LocatorHistory::new, LocatorHistory::load);
    private record Target(ResourceLocation dimension, ResourceLocation structure) {}
    private final Map<UUID, Map<Target, Long2ObjectLinkedOpenHashMap<StructureMarker>>> players = new HashMap<>();

    public static LocatorHistory get(ServerPlayer player) {
        return player.server.overworld().getDataStorage().computeIfAbsent(FACTORY, "better_experience_locator");
    }

    public boolean contains(UUID player, ResourceLocation dimension, ResourceLocation structure, long chunk) {
        var targets = players.get(player);
        var markers = targets == null ? null : targets.get(new Target(dimension, structure));
        return markers != null && markers.containsKey(chunk);
    }

    public List<StructureMarker> markers(UUID player) {
        var targets = players.get(player);
        if (targets == null) return List.of();
        var markers = new ArrayList<StructureMarker>();
        targets.values().forEach(values -> markers.addAll(values.values()));
        return List.copyOf(markers);
    }

    public boolean add(UUID player, StructureMarker marker) {
        var targets = players.computeIfAbsent(player, unused -> new HashMap<>());
        var target = new Target(marker.dimension(), marker.structure());
        var markers = targets.computeIfAbsent(target, unused -> new Long2ObjectLinkedOpenHashMap<>());
        if (markers.containsKey(marker.startChunk()) || count(player) >= MAX_MARKERS_PER_PLAYER) return false;
        markers.put(marker.startChunk(), marker);
        setDirty();
        return true;
    }

    public int count(UUID player) {
        var targets = players.get(player);
        return targets == null ? 0 : targets.values().stream().mapToInt(Map::size).sum();
    }

    static LocatorHistory load(CompoundTag tag, HolderLookup.Provider provider) {
        var history = new LocatorHistory();
        var players = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            var player = players.getCompound(i);
            if (!player.hasUUID("player")) continue;
            UUID id = player.getUUID("player");
            var markers = player.getList("markers", Tag.TAG_COMPOUND);
            for (int j = 0; j < Math.min(markers.size(), MAX_MARKERS_PER_PLAYER); j++) {
                StructureMarker.CODEC.parse(NbtOps.INSTANCE, markers.get(j)).result().ifPresent(marker -> history.add(id, marker));
            }
        }
        history.setDirty(false);
        return history;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        var entries = new ListTag();
        for (UUID id : players.keySet()) {
            var player = new CompoundTag();
            player.putUUID("player", id);
            var markers = new ListTag();
            for (StructureMarker marker : markers(id)) markers.add(StructureMarker.CODEC.encodeStart(NbtOps.INSTANCE, marker).getOrThrow());
            player.put("markers", markers);
            entries.add(player);
        }
        tag.put("players", entries);
        return tag;
    }
}
