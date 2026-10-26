package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.module.worldoptions.ConvenienceRules;
import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LootTable.class)
public abstract class GeneratedLootMixin {
    @ModifyReturnValue(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;", at = @At("RETURN"))
    private ObjectArrayList<ItemStack> betterExperience$uncursedLoot(ObjectArrayList<ItemStack> items, LootContext context) {
        if (WorldOptions.enabled(context.getLevel(), WorldOptions.Feature.NO_CURSED_LOOT)) {
            for (int i = 0; i < items.size(); i++) {
                ItemStack copy = items.get(i).copy();
                ConvenienceRules.stripCurses(copy);
                items.set(i, copy);
            }
        }
        return items;
    }
}
