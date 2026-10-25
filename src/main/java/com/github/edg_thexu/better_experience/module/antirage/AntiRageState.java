package com.github.edg_thexu.better_experience.module.antirage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Persistent per-player state, copied to the replacement player on death. */
public final class AntiRageState implements INBTSerializable<CompoundTag> {
    private int deaths;
    private boolean awaitingRespawn;
    private long protectionUntil;
    private long postHitUntil;
    private long creativeUntil;
    private GameType originalMode;

    public int deaths() { return deaths; }
    public boolean awaitingRespawn() { return awaitingRespawn; }
    public boolean hasTemporaryCreative() { return originalMode != null; }
    public boolean protectedAt(long now) { return now < protectionUntil; }
    public boolean postHitProtectedAt(long now) { return protectedAt(now) && now < postHitUntil; }

    public void extendPostHitProtection(long now, int baseTicks, int extraTicks) {
        if (extraTicks > 0 && protectedAt(now)) {
            postHitUntil = Math.max(postHitUntil, now + Math.max(0, baseTicks) + extraTicks);
        }
    }
    public boolean creativeExpired(long now) { return hasTemporaryCreative() && now >= creativeUntil; }

    public boolean recordDeath() {
        if (awaitingRespawn || hasTemporaryCreative()) return false;
        deaths = Math.min(10, deaths + 1);
        awaitingRespawn = true;
        protectionUntil = 0;
        postHitUntil = 0;
        return true;
    }

    public void respawn(long now, GameType mode) {
        if (!awaitingRespawn) return;
        awaitingRespawn = false;
        if (deaths >= 10) {
            originalMode = mode;
            creativeUntil = now + 100L * 20;
            protectionUntil = 0;
        } else if (deaths > 3) {
            protectionUntil = now + 10L * deaths * 20;
        }
    }

    /** Returns the mode to restore, if this cycle granted temporary creative. */
    public GameType reset() {
        GameType result = originalMode;
        deaths = 0;
        awaitingRespawn = false;
        protectionUntil = 0;
        postHitUntil = 0;
        creativeUntil = 0;
        originalMode = null;
        return result;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        tag.putInt("Deaths", deaths);
        tag.putBoolean("AwaitingRespawn", awaitingRespawn);
        tag.putLong("ProtectionUntil", protectionUntil);
        tag.putLong("PostHitUntil", postHitUntil);
        tag.putLong("CreativeUntil", creativeUntil);
        if (originalMode != null) tag.putString("OriginalMode", originalMode.getName());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        deaths = Math.clamp(tag.getInt("Deaths"), 0, 10);
        awaitingRespawn = tag.getBoolean("AwaitingRespawn");
        protectionUntil = Math.max(0, tag.getLong("ProtectionUntil"));
        postHitUntil = Math.max(0, tag.getLong("PostHitUntil"));
        creativeUntil = Math.max(0, tag.getLong("CreativeUntil"));
        originalMode = tag.contains("OriginalMode") ? GameType.byName(tag.getString("OriginalMode"), null) : null;
    }
}
