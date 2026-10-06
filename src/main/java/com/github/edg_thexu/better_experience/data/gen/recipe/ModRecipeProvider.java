package com.github.edg_thexu.better_experience.data.gen.recipe;


import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.init.ModBlocks;
import com.github.edg_thexu.better_experience.init.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import org.confluence.mod.common.init.item.MaterialItems;

import java.util.concurrent.CompletableFuture;



public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.AUTO_FISH_BLOCK)
                .pattern("aaa")
                .pattern("bcb")
                .pattern("ddd")
                .define('a',Items.GOLD_INGOT)
                .define('b',Items.REDSTONE)
                .define('c',Items.CHEST)
                .define('d',Items.IRON_INGOT)
                .unlockedBy("has_gold_ingot",has(Items.GOLD_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.AUTO_SELL_BLOCK)
                .pattern("aaa")
                .pattern("aba")
                .pattern("aaa")
                .define('a', MaterialItems.PLATINUM_INGOT)
                .define('b',Items.CHEST)
                .unlockedBy("has_platinum_ingot",has(MaterialItems.PLATINUM_INGOT))
                .save(recipeOutput, "confluence:auto_sell_block");

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SPACE_STAFF)
                .pattern("  a").pattern(" b ").pattern("b  ")
                .define('a', Items.ENDER_PEARL).define('b', Items.STICK)
                .unlockedBy("has_ender_pearl", has(Items.ENDER_PEARL))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.POTION_BAG)
                .pattern("aaa")
                .pattern("a a")
                .pattern("aaa")
                .define('a',Items.LEATHER)
                .unlockedBy("has_leather",has(Items.LEATHER))
                .save(recipeOutput);


    }

    protected static <T extends AbstractCookingRecipe> void cookRecipes(
            RecipeOutput recipeOutput, String cookingMethod, RecipeSerializer<T> cookingSerializer, AbstractCookingRecipe.Factory<T> recipeFactory, int cookingTime
    ) {
//        simpleCookingRecipe(recipeOutput, cookingMethod, cookingSerializer, recipeFactory, cookingTime, ModItems.BREAD_SWORD, ModItems.BREAD_SWORD_HOT, 0.35F);
//        simpleCookingRecipe(recipeOutput, cookingMethod, cookingSerializer, recipeFactory, cookingTime, ModItems.BREAD_SWORD_HOT, ModItems.BREAD_SWORD_VERY_HOT, 0.35F);
//

    }

    protected static <T extends AbstractCookingRecipe> void simpleCookingRecipe(
            RecipeOutput recipeOutput,
            String cookingMethod,
            RecipeSerializer<T> cookingSerializer,
            AbstractCookingRecipe.Factory<T> recipeFactory,
            int cookingTime,
            ItemLike material,
            ItemLike result,
            float experience
    ) {
        SimpleCookingRecipeBuilder.generic(Ingredient.of(material), RecipeCategory.FOOD, result, experience, cookingTime, cookingSerializer, recipeFactory)
                .unlockedBy(getHasName(material), has(material))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(Better_experience.MODID,getItemName(result) + "_from_" + cookingMethod));
    }
}
