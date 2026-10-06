package com.github.edg_thexu.better_experience.module.boomstaff;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.confluence.mod.common.init.ModTags;
import org.confluence.mod.common.init.ModTiers;

import java.util.List;
import java.util.Map;

/** Exercises real tool components and Confluence tiers against representative block tags. */
public final class StaffMiningToolsTest {
    public static void main(String[] args) {
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Map<TagKey<Block>, List<Holder<Block>>> tags = Map.of(
                BlockTags.MINEABLE_WITH_PICKAXE, List.of(Blocks.STONE.builtInRegistryHolder(),
                        Blocks.DIAMOND_ORE.builtInRegistryHolder(), Blocks.OBSIDIAN.builtInRegistryHolder()),
                BlockTags.INCORRECT_FOR_WOODEN_TOOL, List.of(Blocks.DIAMOND_ORE.builtInRegistryHolder(), Blocks.OBSIDIAN.builtInRegistryHolder()),
                BlockTags.INCORRECT_FOR_IRON_TOOL, List.of(Blocks.OBSIDIAN.builtInRegistryHolder()),
                ModTags.Blocks.NEEDS_7_LEVEL, List.of(Blocks.OBSIDIAN.builtInRegistryHolder()));
        BuiltInRegistries.BLOCK.bindTags(tags);

        var stone = Blocks.STONE.defaultBlockState();
        var ore = Blocks.DIAMOND_ORE.defaultBlockState();
        var titaniumTier = Blocks.OBSIDIAN.defaultBlockState();
        var wood = new ItemStack(Items.WOODEN_PICKAXE);
        var iron = new ItemStack(Items.IRON_PICKAXE);
        var woodenTools = StaffMiningTools.fromInventory(List.of(wood), ItemStack.EMPTY, false);
        require(woodenTools.forBlock(stone).correct(), "a backpack wooden pickaxe mines stone with empty offhand");
        require(!woodenTools.forBlock(ore).correct(), "wooden pickaxe cannot bypass ore tier requirements");
        require(woodenTools.forBlock(stone) == woodenTools.forBlock(stone), "repeated states reuse one tool decision");
        var mixed = StaffMiningTools.fromInventory(List.of(wood, iron), wood, false);
        require(mixed.forBlock(ore).stack().is(Items.IRON_PICKAXE), "each block searches past weaker tools");
        require(mixed.forBlock(stone).stack().is(Items.WOODEN_PICKAXE), "eligible offhand tool takes priority");
        wood.setCount(0);
        iron.setDamageValue(50);
        require(mixed.forBlock(stone).stack().getCount() == 1 && mixed.forBlock(ore).stack().getDamageValue() == 0,
                "inventory mutations cannot change queued tool snapshots");
        var empty = StaffMiningTools.fromInventory(List.of(new ItemStack(Items.DIAMOND_AXE)), ItemStack.EMPTY, false);
        require(!empty.forBlock(stone).correct(), "axes never confer pickaxe mining permission");
        require(empty.forBlock(Blocks.DIRT.defaultBlockState()).correct(), "hand-breakable blocks need no pickaxe");
        require(!empty.forBlock(titaniumTier).correct(), "empty inventory cannot mine high tier blocks");

        // These tools use precisely the tiers used by Confluence's drill registrations.
        ((MappedRegistry<Item>) BuiltInRegistries.ITEM).unfreeze();
        var palladiumItem = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath("staff_test", "palladium_drill"),
                new PickaxeItem(ModTiers.PALLADIUM, new Item.Properties()));
        var mythrilItem = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath("staff_test", "mythril_drill"),
                new PickaxeItem(ModTiers.MYTHRIL, new Item.Properties()));
        BuiltInRegistries.ITEM.freeze();
        var palladium = StaffMiningTools.fromInventory(List.of(new ItemStack(palladiumItem)), ItemStack.EMPTY, true);
        var mythril = StaffMiningTools.fromInventory(List.of(new ItemStack(mythrilItem)), ItemStack.EMPTY, true);
        require(palladium.forBlock(stone).correct(), "powered drill is recognized as a pickaxe even without pickaxe tag");
        require(!palladium.forBlock(titaniumTier).correct(), "palladium follows actual Confluence tier restrictions");
        require(mythril.forBlock(titaniumTier).correct(), "mythril unlocks the titanium tier");
        var powered = StaffMiningTools.fromInventory(List.of(new ItemStack(palladiumItem), new ItemStack(mythrilItem)), ItemStack.EMPTY, true);
        require(powered.forBlock(titaniumTier).stack().is(mythrilItem), "loot uses the eligible drill, not the first weak drill");

        // A third-party drill can opt into the common drill tag without extending PickaxeItem.
        var taggedDrill = new ItemStack(Items.STICK);
        taggedDrill.set(DataComponents.TOOL, new Tool(List.of(Tool.Rule.minesAndDrops(
                List.of(Blocks.STONE), 4)), 1, 1));
        var drillTag = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("c", "tools/drill"));
        BuiltInRegistries.ITEM.bindTags(Map.of(drillTag, List.of(Items.STICK.builtInRegistryHolder())));
        require(StaffMiningTools.fromInventory(List.of(taggedDrill), ItemStack.EMPTY, false).forBlock(stone).correct(),
                "third-party drill tag and tool component are supported");
        System.out.println("Staff inventory mining checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
