package com.github.edg_thexu.better_experience.data.component;

import com.github.edg_thexu.better_experience.module.boomstaff.StaffSelectionBounds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Exact clicked points relative to the selection minimum; endpoints need not be box corners. */
public record SpaceBezierShape(BlockPos span, List<BlockPos> points) {
    public static final int MAX_POINTS = SpaceStaffSettings.MAX_CONTROLS + 2;
    private record Encoded(BlockPos span, List<BlockPos> points) {}
    private static final Codec<Encoded> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("span").forGetter(Encoded::span),
            BlockPos.CODEC.listOf(2, MAX_POINTS).fieldOf("points").forGetter(Encoded::points)
    ).apply(instance, Encoded::new));
    public static final Codec<SpaceBezierShape> CODEC = RAW_CODEC.comapFlatMap(raw -> {
        try { return DataResult.success(new SpaceBezierShape(raw.span(), raw.points())); }
        catch (IllegalArgumentException exception) { return DataResult.error(() -> exception.getMessage()); }
    }, shape -> new Encoded(shape.span(), shape.points()));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpaceBezierShape> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public SpaceBezierShape {
        span = span.immutable();
        points = points.stream().map(BlockPos::immutable).toList();
        if (span.getX() < 0 || span.getX() > 20 || span.getY() < 0 || span.getY() > 20
                || span.getZ() < 0 || span.getZ() > 20 || points.size() < 2 || points.size() > MAX_POINTS
                || points.stream().anyMatch(point -> point.getX() < 0 || point.getY() < 0 || point.getZ() < 0)) {
            throw new IllegalArgumentException("Invalid world curve size or points");
        }
        for (BlockPos point : points) {
            if (point.getX() > span.getX() || point.getY() > span.getY() || point.getZ() > span.getZ()) {
                throw new IllegalArgumentException("Curve point outside its selection");
            }
        }
    }

    public static SpaceBezierShape fromWorld(StaffSelectionBounds bounds, List<BlockPos> points) {
        return new SpaceBezierShape(bounds.max().subtract(bounds.min()),
                points.stream().map(point -> point.subtract(bounds.min())).toList());
    }

    public List<Vec3> polygon(StaffSelectionBounds bounds) {
        BlockPos extent = bounds.max().subtract(bounds.min());
        return points.stream().map(point -> Vec3.atCenterOf(bounds.min()).add(
                scaled(point.getX(), span.getX(), extent.getX()),
                scaled(point.getY(), span.getY(), extent.getY()),
                scaled(point.getZ(), span.getZ(), extent.getZ()))).toList();
    }

    private static double scaled(int coordinate, int original, int extent) {
        return original == 0 ? 0 : (double) coordinate * extent / original;
    }

    public List<BlockPos> controls() {
        return points.subList(1, points.size() - 1).stream().map(point -> new BlockPos(
                percent(point.getX(), span.getX()), percent(point.getY(), span.getY()), percent(point.getZ(), span.getZ()))).toList();
    }

    private static int percent(int coordinate, int extent) {
        return extent == 0 ? 0 : (int) Math.round(coordinate * 100.0 / extent);
    }

    public SpaceBezierShape withControls(List<BlockPos> controls) {
        List<BlockPos> edited = new ArrayList<>();
        edited.add(points.getFirst());
        for (BlockPos control : controls) edited.add(new BlockPos(
                (int) Math.round(span.getX() * control.getX() / 100.0),
                (int) Math.round(span.getY() * control.getY() / 100.0),
                (int) Math.round(span.getZ() * control.getZ() / 100.0)));
        edited.add(points.getLast());
        return new SpaceBezierShape(span, edited);
    }
}
