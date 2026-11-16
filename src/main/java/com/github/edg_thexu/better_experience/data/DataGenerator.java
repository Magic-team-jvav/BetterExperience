package com.github.edg_thexu.better_experience.data;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.data.gen.ModChineseProvider;
import com.github.edg_thexu.better_experience.data.gen.ModEnglishProvider;
import com.github.edg_thexu.better_experience.data.gen.ModItemModelProvider;
import com.github.edg_thexu.better_experience.data.gen.loot.ModLootTableProvider;
import com.github.edg_thexu.better_experience.data.gen.recipe.ModRecipeProvider;
import com.github.edg_thexu.better_experience.data.gen.tag.ModBlockTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;


@EventBusSubscriber(modid = Better_experience.MODID)
public class DataGenerator {
	@SubscribeEvent
	public static void gatherData(GatherDataEvent event) {
		net.minecraft.data.DataGenerator generator = event.getGenerator();
		PackOutput output = generator.getPackOutput();
		ExistingFileHelper helper = event.getExistingFileHelper();

		CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
		boolean server = event.includeServer();
		generator.addProvider(server, new ModBlockTagsProvider(output, lookup, helper));
		generator.addProvider(server, new ModRecipeProvider(output, lookup));
		generator.addProvider(server, ModLootTableProvider.getProvider(output, lookup));

		boolean client = event.includeClient();
		generator.addProvider(client, new ModChineseProvider(output));
		generator.addProvider(client, new ModEnglishProvider(output));
		generator.addProvider(client, new ModItemModelProvider(output, helper));

	}
}