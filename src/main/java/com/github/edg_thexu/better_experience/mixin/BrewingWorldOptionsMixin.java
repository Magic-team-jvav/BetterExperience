package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingWorldOptionsMixin {
    @Shadow private int brewTime;

    @WrapMethod(method = "serverTick")
    private static void betterExperience$finishBrew(Level level, BlockPos pos, BlockState state,
                                                   BrewingStandBlockEntity stand, Operation<Void> original) {
        // The first pass validates changed ingredients and charges fuel before a brew may finish.
        original.call(level, pos, state, stand);
        var access = (BrewingWorldOptionsMixin) (Object) stand;
        if (access.brewTime > 0 && WorldOptions.enabled(level, WorldOptions.Feature.INSTANT_BREWING)) {
            access.brewTime = 1;
            original.call(level, pos, level.getBlockState(pos), stand);
        }
    }
}
