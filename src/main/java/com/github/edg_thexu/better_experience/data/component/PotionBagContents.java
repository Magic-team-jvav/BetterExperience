package com.github.edg_thexu.better_experience.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable vector snapshot. Quantities are separate from vanilla stack limits. */
public record PotionBagContents(List<PotionBagContents.Entry> entries, int capacity, boolean autoCollect) {
    public static final int INITIAL_CAPACITY = 18;
    public static final int MAX_TYPES = 4096;
    public static final PotionBagContents EMPTY = new PotionBagContents(List.of(), INITIAL_CAPACITY, true);
    public static final Codec<PotionBagContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.sizeLimitedListOf(MAX_TYPES).fieldOf("entries").forGetter(PotionBagContents::entries),
            Codec.intRange(INITIAL_CAPACITY, MAX_TYPES).fieldOf("capacity").forGetter(PotionBagContents::capacity),
            Codec.BOOL.optionalFieldOf("auto_collect", true).forGetter(PotionBagContents::autoCollect)
    ).apply(instance, PotionBagContents::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, PotionBagContents> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public PotionBagContents {
        if (capacity < INITIAL_CAPACITY || capacity > MAX_TYPES || entries.size() > capacity) {
            throw new IllegalArgumentException("Invalid potion bag capacity");
        }
        var unique = new ArrayList<Entry>(capacity);
        for (Entry entry : entries) {
            int existing = find(unique, entry.item);
            if (existing < 0) unique.add(entry);
            else {
                Entry previous = unique.get(existing);
                unique.set(existing, new Entry(previous.item, Math.addExact(previous.count, entry.count), previous.enabled));
            }
        }
        entries = Collections.unmodifiableList(unique);
    }

    private static int find(List<Entry> entries, ItemStack item) {
        for (int i = 0; i < entries.size(); i++) {
            if (ItemStack.isSameItemSameComponents(entries.get(i).item, item)) return i;
        }
        return -1;
    }

    public int indexOf(ItemStack item) { return find(entries, item); }

    /** Inserts as many as fit; the caller retains ownership of its input stack. */
    public PotionBagContents insert(ItemStack item, int requested) {
        if (item.isEmpty() || requested <= 0) return this;
        int index = indexOf(item);
        if (index < 0 && entries.size() == MAX_TYPES) return this;
        int previous = index < 0 ? 0 : entries.get(index).count;
        int accepted = Math.min(requested, Integer.MAX_VALUE - previous);
        if (accepted == 0) return this;
        var updated = new ArrayList<>(entries);
        Entry replacement = new Entry(item, previous + accepted, index < 0 || entries.get(index).enabled);
        if (index < 0) updated.add(replacement);
        else updated.set(index, replacement);
        int grown = capacity;
        while (updated.size() > grown) grown = Math.min(MAX_TYPES, grown * 2);
        return new PotionBagContents(updated, grown, autoCollect);
    }

    public PotionBagContents remove(int index, int amount) {
        if (index < 0 || index >= entries.size() || amount <= 0) return this;
        var updated = new ArrayList<>(entries);
        Entry entry = updated.get(index);
        if (amount >= entry.count) updated.remove(index);
        else updated.set(index, new Entry(entry.item, entry.count - amount, entry.enabled));
        return new PotionBagContents(updated, capacity, autoCollect);
    }

    public PotionBagContents withAutoCollect(boolean enabled) {
        return new PotionBagContents(entries, capacity, enabled);
    }

    public PotionBagContents withEffectEnabled(int index, boolean enabled) {
        if (index < 0 || index >= entries.size() || entries.get(index).enabled == enabled) return this;
        var updated = new ArrayList<>(entries);
        Entry entry = updated.get(index);
        updated.set(index, new Entry(entry.item, entry.count, enabled));
        return new PotionBagContents(updated, capacity, autoCollect);
    }

    public record Entry(ItemStack item, int count, boolean enabled) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("item").forGetter(Entry::item),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("count").forGetter(Entry::count),
                Codec.BOOL.fieldOf("enabled").forGetter(Entry::enabled)
        ).apply(instance, Entry::new));

        public Entry(ItemStack item, int count) {
            this(item, count, true);
        }

        public Entry {
            Objects.requireNonNull(item);
            if (item.isEmpty() || count <= 0) throw new IllegalArgumentException("Empty potion bag entry");
            item = item.copyWithCount(1);
        }

        @Override public ItemStack item() { return item.copy(); }
        @Override public boolean equals(Object other) {
            return other instanceof Entry entry && count == entry.count && enabled == entry.enabled
                    && ItemStack.isSameItemSameComponents(item, entry.item);
        }
        @Override public int hashCode() { return Objects.hash(ItemStack.hashItemAndComponents(item), count, enabled); }
    }
}
