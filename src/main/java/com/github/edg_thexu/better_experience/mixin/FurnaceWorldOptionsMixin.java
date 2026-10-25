package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceWorldOptionsMixin {
    @Shadow private int cookingProgress;
    @Shadow private int cookingTotalTime;

    @Inject(method = "serverTick", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;cookingProgress:I",
            opcode = Opcodes.GETFIELD, ordinal = 0))
    private static void betterExperience$finishRecipe(Level level, BlockPos pos, BlockState state,
                                                       AbstractFurnaceBlockEntity furnace, CallbackInfo callback) {
        if (WorldOptions.enabled(level, WorldOptions.Feature.INSTANT_SMELTING)) {
            var access = (FurnaceWorldOptionsMixin) (Object) furnace;
            access.cookingTotalTime = Math.max(1, access.cookingTotalTime);
            access.cookingProgress = access.cookingTotalTime - 1;
        }
    }
}
