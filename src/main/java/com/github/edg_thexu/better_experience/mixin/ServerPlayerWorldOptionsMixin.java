package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerWorldOptionsMixin {
    @WrapOperation(method = "lambda$startSleepInBed$13", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/dimension/DimensionType;natural()Z"))
    private boolean betterExperience$allowSleep(DimensionType type, Operation<Boolean> original) {
        return original.call(type) || WorldOptions.enabled(((ServerPlayer) (Object) this).level(), WorldOptions.Feature.SAFE_SLEEP);
    }

    @WrapOperation(method = "findRespawnAndUseSpawnBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean betterExperience$keepAnchorCharge(ServerLevel level, BlockPos pos, BlockState state, int flags,
                                                            Operation<Boolean> original) {
        if (state.getBlock() instanceof RespawnAnchorBlock && WorldOptions.enabled(level, WorldOptions.Feature.FREE_ANCHOR)) return true;
        return original.call(level, pos, state, flags);
    }
}
