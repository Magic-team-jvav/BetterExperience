package com.github.edg_thexu.better_experience.module.voidstaff;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;

/** Full container data stays on the server; preview data contains only the renderer's update tag. */
public record VoidCargo(UUID token, List<Part> parts, BlockState state) {
    public static final Codec<VoidCargo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("token").forGetter(VoidCargo::token),
            Part.CODEC.sizeLimitedListOf(2).fieldOf("parts").forGetter(VoidCargo::parts),
            BlockState.CODEC.fieldOf("state").forGetter(VoidCargo::state)
    ).apply(instance, VoidCargo::new));

    public VoidCargo {
        parts = List.copyOf(parts);
        if (parts.isEmpty() || parts.size() > 2 || !parts.getFirst().offset.equals(BlockPos.ZERO)
                || parts.stream().anyMatch(part -> part.offset.distManhattan(BlockPos.ZERO) > 1)
                || parts.size() == 2 && parts.get(1).offset.equals(BlockPos.ZERO)
                || state.getBlock() != parts.getFirst().state.getBlock()) throw new IllegalArgumentException("Invalid void cargo");
    }

    public VoidCargo withState(BlockState state) { return new VoidCargo(token, parts, state); }

    public record Part(BlockPos offset, BlockState state, CompoundTag data, CompoundTag display) {
        public static final Codec<Part> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("offset").forGetter(Part::offset),
                BlockState.CODEC.fieldOf("state").forGetter(Part::state),
                CompoundTag.CODEC.fieldOf("data").forGetter(Part::data),
                CompoundTag.CODEC.fieldOf("display").forGetter(Part::display)
        ).apply(instance, Part::new));
        public Part {
            offset = offset.immutable();
            data = data.copy();
            display = display.copy();
        }
        @Override public CompoundTag data() { return data.copy(); }
        @Override public CompoundTag display() { return display.copy(); }
    }
}
