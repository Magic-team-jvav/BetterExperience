package com.github.edg_thexu.better_experience.module.lootpity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Server-owned progress. A collected item keeps its ordinary chance until the round completes. */
public final class LootPityState implements INBTSerializable<CompoundTag> {
    private static final int MAX_SOURCES = 2048;
    private static final int MAX_ITEMS = 256;
    private final Map<String, Map<String, Progress>> sources = new HashMap<>();

    private static final class Progress {
        private double accumulated;
        private boolean collected;
    }

    public double chance(String source, String item, double base) {
        if (!Double.isFinite(base) || base <= 0) return 0;
        Map<String, Progress> items = sources.get(source);
        Progress progress = items == null ? null : items.get(item);
        return progress == null || progress.collected ? base : Math.min(1_000_000, progress.accumulated + base);
    }

    public void finish(String source, Map<String, Double> scores, Set<String> dropped) {
        Set<String> eligible = scores.keySet();
        if (eligible.isEmpty()) return;
        Map<String, Progress> items = sources.get(source);
        if (items == null) {
            if (sources.size() >= MAX_SOURCES) return;
            items = new HashMap<>();
            sources.put(source, items);
        }
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            if (!items.containsKey(entry.getKey()) && items.size() >= MAX_ITEMS) continue;
            Progress progress = items.computeIfAbsent(entry.getKey(), key -> new Progress());
            if (!progress.collected) progress.accumulated = entry.getValue();
        }
        for (String item : dropped) {
            Progress progress = items.get(item);
            if (progress != null) {
                progress.collected = true;
                progress.accumulated = 0;
            }
        }
        // Temporarily ineligible drops neither block this round nor lose their saved progress.
        Map<String, Progress> current = items;
        if (eligible.stream().allMatch(item -> current.containsKey(item) && current.get(item).collected)) {
            eligible.forEach(items::remove);
            if (items.isEmpty()) sources.remove(source);
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag result = new CompoundTag();
        ListTag entries = new ListTag();
        sources.forEach((source, items) -> {
            CompoundTag group = new CompoundTag();
            group.putString("Source", source);
            ListTag values = new ListTag();
            items.forEach((item, progress) -> {
                CompoundTag value = new CompoundTag();
                value.putString("Item", item);
                value.putDouble("Chance", progress.accumulated);
                value.putBoolean("Collected", progress.collected);
                values.add(value);
            });
            group.put("Items", values);
            entries.add(group);
        });
        result.put("Sources", entries);
        return result;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        sources.clear();
        ListTag groups = tag.getList("Sources", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(groups.size(), MAX_SOURCES); i++) {
            CompoundTag group = groups.getCompound(i);
            String source = group.getString("Source");
            if (source.isEmpty() || source.length() > 512) continue;
            Map<String, Progress> items = new HashMap<>();
            ListTag values = group.getList("Items", Tag.TAG_COMPOUND);
            for (int j = 0; j < Math.min(values.size(), MAX_ITEMS); j++) {
                CompoundTag value = values.getCompound(j);
                String item = value.getString("Item");
                double chance = value.getDouble("Chance");
                if (item.isEmpty() || item.length() > 256 || !Double.isFinite(chance)) continue;
                Progress progress = new Progress();
                progress.accumulated = Math.clamp(chance, 0, 1_000_000);
                progress.collected = value.getBoolean("Collected");
                items.put(item, progress);
            }
            if (!items.isEmpty()) sources.put(source, items);
        }
    }
}
