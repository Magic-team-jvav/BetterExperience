package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.module.lootpity.PityLootPools;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Consumer;

@Mixin(LootPool.class)
public abstract class LootPityPoolMixin {
    @WrapMethod(method = "addRandomItems")
    private void betterExperience$pity(Consumer<ItemStack> output, LootContext context, Operation<Void> original) {
        if (!PityLootPools.generate((LootPool) (Object) this, output, context)) original.call(output, context);
    }
}
