package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.module.lootpity.LootPity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LootTable.class)
public abstract class LootPityTableMixin {
    @WrapMethod(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;")
    private ObjectArrayList<ItemStack> betterExperience$pity(LootContext context, Operation<ObjectArrayList<ItemStack>> original) {
        LootPity.Attempt attempt = LootPity.begin((LootTable) (Object) this, context);
        if (attempt == null) return original.call(context);
        ObjectArrayList<ItemStack> result = null;
        try {
            result = original.call(context);
            return result;
        } finally {
            LootPity.end(attempt, result);
        }
    }
}
