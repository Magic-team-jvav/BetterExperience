package com.github.edg_thexu.better_experience.module.voidstaff;

import com.github.edg_thexu.better_experience.Better_experience;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player transport survives death, disconnect, dimension changes and orderly server restarts. */
public final class VoidCargoStorage extends SavedData {
    private static final Factory<VoidCargoStorage> FACTORY = new Factory<>(VoidCargoStorage::new, VoidCargoStorage::load);
    private final Map<UUID, VoidCargo> cargo = new HashMap<>();

    public static VoidCargoStorage get(ServerPlayer player) {
        return player.server.overworld().getDataStorage().computeIfAbsent(FACTORY, "better_experience_void_cargo");
    }
    public VoidCargo cargo(UUID player) { return cargo.get(player); }
    public void put(UUID player, VoidCargo value) { cargo.put(player, value); setDirty(); }
    public void remove(UUID player) { cargo.remove(player); setDirty(); }

    static VoidCargoStorage load(CompoundTag tag, HolderLookup.Provider provider) {
        var storage = new VoidCargoStorage();
        var entries = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.getCompound(i);
            if (!entry.hasUUID("player")) continue;
            VoidCargo.CODEC.parse(NbtOps.INSTANCE, entry.getCompound("cargo")).resultOrPartial(error ->
                    Better_experience.LOGGER.error("Cannot load void cargo: {}", error))
                    .ifPresent(value -> storage.cargo.put(entry.getUUID("player"), value));
        }
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        var entries = new ListTag();
        cargo.forEach((player, value) -> {
            var entry = new CompoundTag();
            entry.putUUID("player", player);
            entry.put("cargo", VoidCargo.CODEC.encodeStart(NbtOps.INSTANCE, value).getOrThrow());
            entries.add(entry);
        });
        tag.put("players", entries);
        return tag;
    }
}
