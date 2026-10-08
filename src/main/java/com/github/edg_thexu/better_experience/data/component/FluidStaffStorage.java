package com.github.edg_thexu.better_experience.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Immutable multi-fluid reservoir. Creative supply is a player privilege, never persisted on an item. */
public final class FluidStaffStorage {
    public static final int CAPACITY = 512_000;
    public static final int MAX_TYPES = 1024;
    public static final FluidStaffStorage EMPTY = new FluidStaffStorage(List.of());
    public static final Codec<FluidStaffStorage> CODEC = FluidStack.CODEC.sizeLimitedListOf(MAX_TYPES)
            .validate(entries -> valid(entries) ? DataResult.success(entries)
                    : DataResult.error(() -> "Invalid fluid staff reservoir"))
            .xmap(FluidStaffStorage::new, FluidStaffStorage::fluids);
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStaffStorage> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);
    private final List<FluidStack> fluids;
    private final int total;

    public FluidStaffStorage(List<FluidStack> entries) {
        if (!valid(entries)) throw new IllegalArgumentException("Invalid fluid staff reservoir");
        fluids = entries.stream().map(FluidStack::copy).toList();
        total = fluids.stream().mapToInt(FluidStack::getAmount).sum();
    }

    /** Internal entries are never exposed or mutated; updates can share unchanged fluid snapshots. */
    private FluidStaffStorage(List<FluidStack> entries, int total) {
        fluids = List.copyOf(entries);
        this.total = total;
    }

    private static boolean valid(List<FluidStack> entries) {
        if (entries.size() > MAX_TYPES) return false;
        long total = 0;
        var identities = new HashSet<FluidStaffSettings>();
        for (FluidStack fluid : entries) {
            if (fluid.isEmpty()) return false;
            total += fluid.getAmount();
            if (total > CAPACITY || !identities.add(new FluidStaffSettings(false, fluid))) return false;
        }
        return true;
    }

    public List<FluidStack> fluids() { return fluids.stream().map(FluidStack::copy).toList(); }
    public int total() { return total; }
    public int types() { return fluids.size(); }
    public FluidStack fluidAt(int index) {
        return index >= 0 && index < fluids.size() ? fluids.get(index).copy() : FluidStack.EMPTY;
    }

    public int amount(FluidStack fluid) {
        for (FluidStack entry : fluids) {
            if (FluidStack.isSameFluidSameComponents(entry, fluid)) return entry.getAmount();
        }
        return 0;
    }

    public int accepted(FluidStack fluid) {
        if (fluid.isEmpty() || (fluids.size() == MAX_TYPES && amount(fluid) == 0)) return 0;
        return Math.min(CAPACITY - total, fluid.getAmount());
    }

    public FluidStaffStorage add(FluidStack fluid) {
        int accepted = accepted(fluid);
        if (accepted == 0) return this;
        var updated = new ArrayList<>(fluids);
        for (int index = 0; index < fluids.size(); index++) {
            FluidStack entry = fluids.get(index);
            if (FluidStack.isSameFluidSameComponents(entry, fluid)) {
                updated.set(index, entry.copyWithAmount(entry.getAmount() + accepted));
                return new FluidStaffStorage(updated, total + accepted);
            }
        }
        updated.add(fluid.copyWithAmount(accepted));
        return new FluidStaffStorage(updated, total + accepted);
    }

    public FluidStaffStorage remove(FluidStack fluid) {
        if (fluid.isEmpty()) return this;
        for (int index = 0; index < fluids.size(); index++) {
            FluidStack entry = fluids.get(index);
            if (FluidStack.isSameFluidSameComponents(entry, fluid)) {
                int removed = Math.min(entry.getAmount(), fluid.getAmount());
                var updated = new ArrayList<>(fluids);
                if (removed == entry.getAmount()) updated.remove(index);
                else updated.set(index, entry.copyWithAmount(entry.getAmount() - removed));
                return updated.isEmpty() ? EMPTY : new FluidStaffStorage(updated, total - removed);
            }
        }
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof FluidStaffStorage storage) || fluids.size() != storage.fluids.size()) return false;
        for (int i = 0; i < fluids.size(); i++) {
            if (fluids.get(i).getAmount() != storage.fluids.get(i).getAmount()
                    || !FluidStack.isSameFluidSameComponents(fluids.get(i), storage.fluids.get(i))) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for (FluidStack fluid : fluids) hash = 31 * hash + 31 * FluidStack.hashFluidAndComponents(fluid) + fluid.getAmount();
        return hash;
    }
}
