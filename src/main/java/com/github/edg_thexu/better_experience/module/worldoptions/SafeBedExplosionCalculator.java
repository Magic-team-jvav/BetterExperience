package com.github.edg_thexu.better_experience.module.worldoptions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Only beds use this calculator; TNT, anchors, terrain damage and other entities keep their rules. */
public final class SafeBedExplosionCalculator extends ExplosionDamageCalculator {
    private final ExplosionDamageCalculator delegate;

    public SafeBedExplosionCalculator(@Nullable ExplosionDamageCalculator delegate) {
        this.delegate = delegate == null ? new ExplosionDamageCalculator() : delegate;
    }

    @Override
    public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
        return !(entity instanceof Player) && delegate.shouldDamageEntity(explosion, entity);
    }

    @Override
    public float getKnockbackMultiplier(Entity entity) {
        return entity instanceof Player ? 0.0F : delegate.getKnockbackMultiplier(entity);
    }

    @Override
    public float getEntityDamageAmount(Explosion explosion, Entity entity) {
        return delegate.getEntityDamageAmount(explosion, entity);
    }

    @Override
    public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, FluidState fluid) {
        return delegate.getBlockExplosionResistance(explosion, level, pos, state, fluid);
    }

    @Override
    public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
        return delegate.shouldBlockExplode(explosion, level, pos, state, power);
    }
}
