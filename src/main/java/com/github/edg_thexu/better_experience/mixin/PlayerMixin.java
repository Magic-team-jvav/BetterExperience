package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.mixed.IPlayer;
import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.LinkedList;
import java.util.List;

@Mixin(Player.class)
public class PlayerMixin implements IPlayer {

    @Unique
    BlockEntity betterExperience$interactBlockEntity;

    @Unique
    List<FishingHook> betterExperience$fishingHookList = new LinkedList<>();

    @Unique
    int betterExperience$hammerUsingTicks;

    @WrapOperation(method = "onEnchantmentPerformed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;giveExperienceLevels(I)V"))
    private void betterExperience$enchantWithoutExperience(Player player, int levels, Operation<Void> original) {
        if (!WorldOptions.enabled(player.level(), WorldOptions.Feature.FREE_ENCHANTING)) original.call(player, levels);
    }

    @Override
    public BlockEntity betterExperience$getInteractBlockEntity() {
        return betterExperience$interactBlockEntity;
    }

    @Override
    public void betterExperience$setInteractBlockEntity(BlockEntity blockEntity) {
        betterExperience$interactBlockEntity = blockEntity;
    }

    @Override
    public int betterExperience$getHammerUsingTicks() {
        return betterExperience$hammerUsingTicks;
    }

    @Override
    public void betterExperience$setHammerUsingTicks(int ticks) {
        betterExperience$hammerUsingTicks = ticks;
    }

    @Override
    public List<FishingHook> betterExperience$getFishingHookList() {
        return betterExperience$fishingHookList;
    }

    @Override
    public void betterExperience$setFishingHookList(List<FishingHook> fishingHookList) {
        betterExperience$fishingHookList = fishingHookList;
    }
}
