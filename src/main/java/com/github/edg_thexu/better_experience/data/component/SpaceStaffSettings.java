package com.github.edg_thexu.better_experience.data.component;

import com.github.edg_thexu.better_experience.module.spacestaff.SpaceShape;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Ordered curve controls are percentages inside the saved selection. */
public record SpaceStaffSettings(SpaceShape shape, BlockState state, List<BlockPos> controls, Optional<SpaceBezierShape> worldCurve) {
    public static final int MAX_CONTROLS = 16;
    private static final BlockPos DEFAULT_A = new BlockPos(33, 0, 50);
    private static final BlockPos DEFAULT_B = new BlockPos(67, 100, 50);
    private static final Codec<BlockPos> CONTROL_CODEC = BlockPos.CODEC.validate(pos ->
            validControl(pos) ? DataResult.success(pos)
                    : DataResult.error(() -> "Curve controls must be between 0 and 100"));
    private static final Codec<List<BlockPos>> CONTROLS_CODEC = CONTROL_CODEC.sizeLimitedListOf(MAX_CONTROLS);

    public static final Codec<SpaceStaffSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SpaceShape.CODEC.fieldOf("shape").forGetter(SpaceStaffSettings::shape),
            BlockState.CODEC.fieldOf("state").forGetter(SpaceStaffSettings::state),
            CONTROLS_CODEC.optionalFieldOf("control_points").forGetter(settings -> Optional.of(settings.controls)),
            CONTROL_CODEC.optionalFieldOf("control_a").forGetter((SpaceStaffSettings settings) -> Optional.empty()),
            CONTROL_CODEC.optionalFieldOf("control_b").forGetter((SpaceStaffSettings settings) -> Optional.empty()),
            SpaceBezierShape.CODEC.optionalFieldOf("world_curve").forGetter(SpaceStaffSettings::worldCurve)
    ).apply(instance, (shape, state, points, a, b, curve) -> new SpaceStaffSettings(shape, state,
            points.orElseGet(() -> curve.map(SpaceBezierShape::controls)
                    .orElseGet(() -> List.of(a.orElse(DEFAULT_A), b.orElse(DEFAULT_B)))), curve)));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpaceStaffSettings> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public SpaceStaffSettings {
        Objects.requireNonNull(shape);
        Objects.requireNonNull(state);
        Objects.requireNonNull(worldCurve);
        controls = controls.stream().map(BlockPos::immutable).toList();
        if (controls.size() > MAX_CONTROLS || controls.stream().anyMatch(pos -> !validControl(pos))) {
            throw new IllegalArgumentException("Invalid curve controls");
        }
    }

    public SpaceStaffSettings(SpaceShape shape, BlockState state, List<BlockPos> controls) {
        this(shape, state, controls, Optional.empty());
    }

    public SpaceStaffSettings withCurve(SpaceBezierShape curve) {
        return new SpaceStaffSettings(shape, state, curve.controls(), Optional.of(curve));
    }

    public SpaceStaffSettings withShape(SpaceShape shape) {
        return new SpaceStaffSettings(shape, state, controls, worldCurve);
    }

    public SpaceStaffSettings edited(SpaceShape shape, BlockState state, List<BlockPos> controls) {
        return new SpaceStaffSettings(shape, state, controls, controls.equals(this.controls) ? worldCurve
                : worldCurve.map(curve -> curve.withControls(controls)));
    }

    public static boolean validControl(BlockPos pos) {
        return pos.getX() >= 0 && pos.getX() <= 100 && pos.getY() >= 0 && pos.getY() <= 100
                && pos.getZ() >= 0 && pos.getZ() <= 100;
    }

    public static SpaceStaffSettings defaults() {
        return new SpaceStaffSettings(SpaceShape.CUBE, Blocks.STONE.defaultBlockState(), List.of(DEFAULT_A, DEFAULT_B));
    }
}
