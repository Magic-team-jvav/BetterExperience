package com.github.edg_thexu.better_experience.module.worldoptions;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Server-owned options shared by every dimension of one save. */
public final class WorldOptions extends SavedData {
    public static final double MAX_GROWTH = 20.0;
    private static final Factory<WorldOptions> FACTORY = new Factory<>(WorldOptions::new, WorldOptions::load);
    private Values values = Values.DEFAULT;
    private static volatile Values clientValues = Values.DEFAULT;

    public enum Feature {
        FREE_ENCHANTING("free_enchanting"),
        FREE_ANCHOR("free_anchor"),
        SAFE_SLEEP("safe_sleep"),
        BED_BLAST_IMMUNITY("bed_blast_immunity"),
        INSTANT_SMELTING("instant_smelting"),
        INSTANT_BREWING("instant_brewing"),
        UNBREAKABLE_EYES("unbreakable_eyes"),
        FAIR_TRADES("fair_trades"),
        FRIENDLY_NEUTRALS("friendly_neutrals"),
        PROTECTED_PETS("protected_pets"),
        REUSABLE_TEMPLATES("reusable_templates"),
        NO_CURSED_LOOT("no_cursed_loot"),
        HUNGRY_SPRINTING("hungry_sprinting"),
        FULL_SPEED_BOWS("full_speed_bows"),
        PERMANENT_NIGHT_VISION("permanent_night_vision"),
        EXTRA_GOLEM_IRON("extra_golem_iron"),
        LEATHER_RECIPE("leather_recipe"),
        ENCHANTED_APPLE_RECIPE("enchanted_apple_recipe");

        private final String key;

        Feature(String key) { this.key = key; }

        public String key() { return key; }
    }

    public record Values(int flags, double crops, double saplings) {
        public static final Values DEFAULT = new Values(0, 1.0, 1.0);
        private static final int ALLOWED_FLAGS = (1 << Feature.values().length) - 1;
        public static final StreamCodec<FriendlyByteBuf, Values> STREAM_CODEC = StreamCodec.of((buf, value) -> {
            buf.writeVarInt(value.flags);
            buf.writeDouble(value.crops);
            buf.writeDouble(value.saplings);
        }, buf -> new Values(buf.readVarInt(), buf.readDouble(), buf.readDouble()));

        public boolean enabled(Feature feature) { return (flags & (1 << feature.ordinal())) != 0; }

        public Values with(Feature feature, boolean enabled) {
            int bit = 1 << feature.ordinal();
            return new Values(enabled ? flags | bit : flags & ~bit, crops, saplings);
        }

        public boolean valid() {
            return (flags & ~ALLOWED_FLAGS) == 0 && validGrowth(crops) && validGrowth(saplings);
        }
    }

    public static WorldOptions get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "better_experience_world_options");
    }

    public static boolean enabled(Level level, Feature feature) {
        if (level.isClientSide) return clientEnabled(feature);
        return level instanceof ServerLevel serverLevel && get(serverLevel.getServer()).values.enabled(feature);
    }

    public static boolean clientEnabled(Feature feature) { return clientValues.enabled(feature); }

    public static void acceptClientValues(Values values) {
        if (values.valid()) clientValues = values;
    }

    public Values values() { return values; }

    public static boolean validGrowth(double value) {
        return Double.isFinite(value) && value >= 0.0 && value <= MAX_GROWTH;
    }

    public boolean update(Values updated) {
        if (!updated.valid()) return false;
        if (!values.equals(updated)) {
            values = updated;
            setDirty();
        }
        return true;
    }

    static WorldOptions load(CompoundTag tag, HolderLookup.Provider provider) {
        var options = new WorldOptions();
        int flags = 0;
        for (Feature feature : Feature.values()) {
            if (tag.getBoolean(feature.key())) flags |= 1 << feature.ordinal();
        }
        double crops = tag.contains("crops") && validGrowth(tag.getDouble("crops")) ? tag.getDouble("crops") : 1.0;
        double saplings = tag.contains("saplings") && validGrowth(tag.getDouble("saplings")) ? tag.getDouble("saplings") : 1.0;
        options.values = new Values(flags, crops, saplings);
        return options;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        for (Feature feature : Feature.values()) tag.putBoolean(feature.key(), values.enabled(feature));
        tag.putDouble("crops", values.crops());
        tag.putDouble("saplings", values.saplings());
        return tag;
    }
}
