package com.github.edg_thexu.better_experience.module.spawning;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** One authoritative pair of multipliers per save, shared by every dimension. */
public final class SpawnSettings extends SavedData {
    public static final double MAX_MULTIPLIER = 10.0;
    private static final Factory<SpawnSettings> FACTORY = new Factory<>(SpawnSettings::new, SpawnSettings::load);
    private double rate = 1.0;
    private double cap = 1.0;

    public static SpawnSettings get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "better_experience_spawning");
    }

    public double rate() { return rate; }
    public double cap() { return cap; }

    public static boolean valid(double value) {
        return Double.isFinite(value) && value >= 0.0 && value <= MAX_MULTIPLIER;
    }

    public boolean update(double newRate, double newCap) {
        if (!valid(newRate) || !valid(newCap)) return false;
        if (rate != newRate || cap != newCap) {
            rate = newRate;
            cap = newCap;
            setDirty();
        }
        return true;
    }

    static SpawnSettings load(CompoundTag tag, HolderLookup.Provider provider) {
        var settings = new SpawnSettings();
        if (tag.contains("rate") && valid(tag.getDouble("rate"))) settings.rate = tag.getDouble("rate");
        if (tag.contains("cap") && valid(tag.getDouble("cap"))) settings.cap = tag.getDouble("cap");
        return settings;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putDouble("rate", rate);
        tag.putDouble("cap", cap);
        return tag;
    }
}
