package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceFishingSupport;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.confluence.mod.util.PlayerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PlayerUtils.class, remap = false)
public abstract class MachineFishingPowerMixin {
    @WrapOperation(method = "getFishingPower", at = @At(value = "INVOKE",
            target = "Lorg/confluence/mod/common/item/fishing/IBait;getFirstBait(Lnet/minecraft/world/entity/player/Inventory;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack betterExperience$machineBait(Inventory inventory, Operation<ItemStack> original,
                                                          @Local(argsOnly = true) ServerPlayer player) {
        var machine = ConfluenceFishingSupport.current(player);
        return machine == null ? original.call(inventory) : machine.bait();
    }

    @WrapOperation(method = "getFishingPower", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;getAttribute(Lnet/minecraft/core/Holder;)Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;"))
    private static AttributeInstance betterExperience$machineAccessory(ServerPlayer player, Holder<Attribute> type,
                                                                       Operation<AttributeInstance> original) {
        return ConfluenceFishingSupport.withMachineAccessory(player, type, original.call(player, type));
    }
}
