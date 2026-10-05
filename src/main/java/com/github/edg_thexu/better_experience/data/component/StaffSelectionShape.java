package com.github.edg_thexu.better_experience.data.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffSelectionBounds;

/** Signed offset from pos1 to pos2; preserves shape without saving world coordinates. */
public record StaffSelectionShape(BlockPos offset) {
    public static final Codec<StaffSelectionShape> CODEC = BlockPos.CODEC.xmap(StaffSelectionShape::new, StaffSelectionShape::offset);
    public static final StreamCodec<ByteBuf, StaffSelectionShape> STREAM_CODEC = BlockPos.STREAM_CODEC.map(StaffSelectionShape::new, StaffSelectionShape::offset);

    public StaffSelectionShape {
        offset = offset.immutable();
    }

    public static StaffSelectionShape fromCorners(BlockPos pos1, BlockPos pos2) {
        return new StaffSelectionShape(pos2.subtract(pos1));
    }

    public StaffSelectionBounds at(BlockPos pos1) {
        return StaffSelectionBounds.of(pos1, pos1.offset(offset));
    }

    /** Snap the geometric center to the nearest block-aligned placement of this shape. */
    public StaffSelectionBounds centeredAt(Vec3 center) {
        int x = Math.abs(offset.getX());
        int y = Math.abs(offset.getY());
        int z = Math.abs(offset.getZ());
        BlockPos min = BlockPos.containing(center.add(-x / 2.0, -y / 2.0, -z / 2.0));
        return StaffSelectionBounds.of(min, min.offset(x, y, z));
    }

    public boolean fits(int maxSide) {
        return at(BlockPos.ZERO).fits(maxSide);
    }
}
