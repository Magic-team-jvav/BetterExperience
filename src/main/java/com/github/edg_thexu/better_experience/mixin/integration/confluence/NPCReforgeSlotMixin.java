package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.module.reforge.ReforgeAccessories;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** The goblin reforge slot only accepts Confluence-tagged accessories; let any Curios item in. */
@Pseudo
@Mixin(targets = "org.confluence.mod.common.menu.NPCReforgeMenu$1")
public abstract class NPCReforgeSlotMixin {
    @WrapOperation(method = "mayPlace", at = @At(value = "INVOKE",
            target = "Lorg/confluence/mod/util/PrefixUtils;couldReforge(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean betterExperience$allowAccessory(ItemStack stack, Operation<Boolean> original) {
        return original.call(stack) || ReforgeAccessories.isReforgeable(stack);
    }
}
