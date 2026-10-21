package com.github.edg_thexu.better_experience.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Item stacks describe ammunition identities; the chain itself never stores ammunition. */
public record AmmoChainSettings(List<AmmoChainSettings.Entry> entries, boolean enabled) {
    public static final int MIN_SLOTS = 6;
    public static final int MAX_SLOTS = 64;
    public static final int MAX_SHOTS = 9999;
    private static final Codec<List<Entry>> ENTRIES_CODEC = Entry.CODEC.sizeLimitedListOf(MAX_SLOTS)
            .validate(entries -> entries.size() >= MIN_SLOTS ? DataResult.success(entries)
                    : DataResult.error(() -> "An ammunition chain needs at least " + MIN_SLOTS + " slots"));
    public static final Codec<AmmoChainSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ENTRIES_CODEC.fieldOf("entries").forGetter(AmmoChainSettings::entries),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(AmmoChainSettings::enabled)
    ).apply(instance, AmmoChainSettings::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AmmoChainSettings> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AmmoChainSettings decode(RegistryFriendlyByteBuf buffer) {
            int slots = buffer.readVarInt();
            if (slots < MIN_SLOTS || slots > MAX_SLOTS) throw new DecoderException("Invalid ammunition chain slot count: " + slots);
            var entries = new ArrayList<Entry>(slots);
            for (int i = 0; i < slots; i++) entries.add(Entry.STREAM_CODEC.decode(buffer));
            return new AmmoChainSettings(entries, buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, AmmoChainSettings value) {
            buffer.writeVarInt(value.entries.size());
            for (Entry entry : value.entries) Entry.STREAM_CODEC.encode(buffer, entry);
            buffer.writeBoolean(value.enabled);
        }
    };

    public AmmoChainSettings {
        entries = List.copyOf(entries);
        if (entries.size() < MIN_SLOTS || entries.size() > MAX_SLOTS) {
            throw new IllegalArgumentException("Invalid ammunition chain slot count");
        }
    }

    public static AmmoChainSettings defaults() {
        return new AmmoChainSettings(Collections.nCopies(MIN_SLOTS, Entry.EMPTY), true);
    }

    public AmmoChainSettings withEntry(int index, Entry entry) {
        var updated = new ArrayList<>(entries);
        updated.set(index, Objects.requireNonNull(entry));
        return new AmmoChainSettings(updated, enabled);
    }

    public AmmoChainSettings addSlot() {
        if (entries.size() == MAX_SLOTS) return this;
        var updated = new ArrayList<>(entries);
        updated.add(Entry.EMPTY);
        return new AmmoChainSettings(updated, enabled);
    }

    public AmmoChainSettings withEnabled(boolean enabled) {
        return enabled == this.enabled ? this : new AmmoChainSettings(entries, enabled);
    }

    public record Entry(ItemStack ammo, int shots) {
        public static final Entry EMPTY = new Entry(ItemStack.EMPTY, 1);
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("ammo", ItemStack.EMPTY).forGetter(Entry::ammo),
                Codec.intRange(1, MAX_SHOTS).fieldOf("shots").forGetter(Entry::shots)
        ).apply(instance, Entry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Entry decode(RegistryFriendlyByteBuf buffer) {
                ItemStack ammo = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                int shots = buffer.readVarInt();
                if (shots < 1 || shots > MAX_SHOTS) throw new DecoderException("Invalid ammunition chain shot count: " + shots);
                return new Entry(ammo, shots);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Entry value) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, value.ammo);
                buffer.writeVarInt(value.shots);
            }
        };

        public Entry {
            Objects.requireNonNull(ammo);
            if (shots < 1 || shots > MAX_SHOTS) throw new IllegalArgumentException("Invalid ammunition chain shot count");
            ammo = ammo.isEmpty() ? ItemStack.EMPTY : ammo.copyWithCount(1);
        }

        @Override
        public ItemStack ammo() { return ammo.copy(); }

        public boolean isEmpty() { return ammo.isEmpty(); }

        @Override
        public boolean equals(Object other) {
            return other instanceof Entry entry && shots == entry.shots
                    && ItemStack.isSameItemSameComponents(ammo, entry.ammo);
        }

        @Override
        public int hashCode() { return 31 * ItemStack.hashItemAndComponents(ammo) + shots; }
    }
}
