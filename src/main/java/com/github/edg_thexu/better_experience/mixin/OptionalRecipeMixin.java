package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.module.worldoptions.ConvenienceRules;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Only our two reserved recipe groups are gated; datapack recipes keep their own rules. */
@Mixin({ShapedRecipe.class, ShapelessRecipe.class})
public abstract class OptionalRecipeMixin {
    @ModifyReturnValue(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z", at = @At("RETURN"))
    private boolean betterExperience$enabledRecipe(boolean original, CraftingInput input, Level level) {
        return original && ConvenienceRules.recipeEnabled(((CraftingRecipe) (Object) this).getGroup(), level);
    }
}
