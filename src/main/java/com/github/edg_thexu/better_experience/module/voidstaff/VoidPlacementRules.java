package com.github.edg_thexu.better_experience.module.voidstaff;

import com.github.edg_thexu.better_experience.module.spacestaff.SpacePlacementRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class VoidPlacementRules {
    private VoidPlacementRules() {}

    public static List<Property<?>> properties(BlockState state) {
        return state.getProperties().stream().filter(property -> property != ChestBlock.TYPE)
                .sorted(Comparator.comparing(Property::getName)).toList();
    }

    public static boolean validState(BlockState original, BlockState requested) {
        if (original.getBlock() != requested.getBlock()) return false;
        return !(original.getBlock() instanceof ChestBlock)
                || original.getValue(ChestBlock.TYPE) == requested.getValue(ChestBlock.TYPE);
    }

    public static Rotation rotation(BlockState original, BlockState requested) {
        if (!original.hasProperty(ChestBlock.FACING) || !requested.hasProperty(ChestBlock.FACING)) return Rotation.NONE;
        Direction from = original.getValue(ChestBlock.FACING), to = requested.getValue(ChestBlock.FACING);
        for (Rotation rotation : Rotation.values()) if (rotation.rotate(from) == to) return rotation;
        return Rotation.NONE;
    }

    public static BlockState partState(VoidCargo.Part part, BlockState primary, BlockState desired) {
        var state = part.state().rotate(rotation(primary, desired));
        for (var property : properties(desired)) if (state.hasProperty(property)
                && !desired.getValue(property).equals(primary.getValue(property))) state = copyProperty(state, desired, property);
        return state;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState state, BlockState desired, Property<T> property) {
        return state.setValue(property, desired.getValue(property));
    }

    public static boolean withinReach(Player player, BlockPos pos) {
        double reach = player.blockInteractionRange();
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= reach * reach + 1.0E-6;
    }

    public static boolean canPlace(Level level, BlockPos pos, BlockState state) {
        return SpacePlacementRules.canPlace(level, pos, state);
    }

    /** Rounding to a block must not push its center beyond the player's spherical reach. */
    public static BlockPos rayTarget(Vec3 eye, Vec3 look, double distance, double reach) {
        double limit = Math.max(0, Math.min(distance, reach));
        for (double current = limit; current >= 0; current -= 0.1) {
            var pos = BlockPos.containing(eye.add(look.scale(current)));
            if (eye.distanceToSqr(Vec3.atCenterOf(pos)) <= reach * reach + 1.0E-6) return pos;
        }
        return BlockPos.containing(eye);
    }
}
