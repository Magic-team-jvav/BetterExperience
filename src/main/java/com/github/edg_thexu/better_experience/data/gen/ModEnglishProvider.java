package com.github.edg_thexu.better_experience.data.gen;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.init.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class ModEnglishProvider extends LanguageProvider {
    public ModEnglishProvider(PackOutput output) {
        super(output, Better_experience.MODID, "en_us");
    }

    private static String toTitleCase(String raw) {
        return Arrays.stream(raw.split("_"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    @Override
    protected void addTranslations() {

        add("better_experience.welcome_message", "Better Experience has been enabled, press ESC to access the mod's configuration to enable features.");

        add("creativetab.better_experience.item", "Confluence | Better Experience");


        // items
        Consumer<DeferredHolder<Item, ? extends Item>> itemAction = item -> add(item.get(), toTitleCase(item.getId().getPath()));
        ModItems.ITEMS.getEntries().forEach(itemAction);
        ModItems.TOOLS.getEntries().forEach(itemAction);
//        ModBlocks.BLOCKS.getEntries().forEach(block-> add(block.get(), toTitleCase(block.getId().getPath())));

        add("better_experience.staff.choose_corners", "Shift+LMB: select (default)");
        add("better_experience.staff.first_corner", "A set | LMB: set B");
        add("better_experience.staff.locked", "Locked | LMB blast | RMB cancel");
        add("better_experience.staff.no_mana", "Need %s mana");
        add("better_experience.staff.no_target", "Aim within reach");
        add("better_experience.staff.out_of_reach", "Out of reach");
        add("better_experience.staff.reselect", "Cancelled | Activate to reselect");
        add("better_experience.staff.size", "%sx%sx%s | Mana %s | Scroll size | RMB lock");
        add("better_experience.staff.too_large", "Selection limit reached");

        add("better_experience.staff.enter_selection", "%s+LMB: select");
        add("key.better_experience.staff_select_modifier", "Staff selection modifier (with left-click)");
        add("key.categories.better_experience", "Better Experience");

        // tooltips
        add("better_experience.tooltip.magic_boom_staff.info", "Offhand tool sets mining power");
        add("better_experience.tooltip.magic_boom_staff.select", "Shift+LMB: select (default key)");
        add("better_experience.tooltip.magic_boom_staff.corners", "LMB corners | Scroll size | RMB lock");
        add("better_experience.tooltip.magic_boom_staff.locked", "Locked: LMB blast | RMB cancel");
        add("better_experience.tooltip.magic_boom_staff.move", "Shift+Scroll: move | Shift+MMB: forward");
        add("better_experience.tooltip.magic_boom_staff.mana", "1 mana per 8 blocks, rounded up");
        add("better_experience.tooltip.potion_bag.info", "Stores potions and food");
        add("better_experience.tooltip.jei.fetch_ingredients", "Fetch ingredients from nearby chests");

        add("better_experience.tooltip.better_reforge.enable", "Enable Better Reforging");


        add("better_experience.gui.fast_storage", "Fast Storage");
        add("better_experience.gui.potion_screen.auto_collect.message", "Auto Collect Potions and Food");

        // Config
        add("better_experience.configuration.Player", "Player");
        add("better_experience.configuration.World", "World");
        add("better_experience.configuration.Entity", "Entity");
        add("better_experience.configuration.Item", "Item");



        add("better_experience.configuration.show_outlines", "Magic Boom Staff Show Outlines");
        add("better_experience.configuration.auto_potion_open", "Open Infinite-Potion");
        add("better_experience.configuration.auto_potion_stack_size", "Infinite-Potion Stack Size");
        add("better_experience.configuration.auto_potion_scan_item_per_tick", "Infinite-Potion Scan Item Per Tick");

        add("better_experience.configuration.instantly_drink", "Instantly Drink");
        add("better_experience.configuration.infinite_ammo", "Infinite Ammo");
        add("better_experience.configuration.infinite_ammo_stack_size", "Infinite Ammo Stack Size");
        add("better_experience.configuration.modify_max_stack_size", "Modify Item Max Stack Size (Restart the game to make it effect)");
        add("better_experience.configuration.no_consume_summoner", "No Consume Summon Item");
        add("better_experience.configuration.slime_die_no_lava", "Slime Dies Without Lava");
        add("better_experience.configuration.additional_fall_distance", "Additional Fall Distance");
        add("better_experience.configuration.stone_sapling_tree_no_strict", "Stone Sapling Tree No Strict");
        add("better_experience.configuration.fill_life_on_respawn", "Fill Life on Respawn");
        add("better_experience.configuration.herb_growth_no_strict", "Herb Growth No Strict");
        add("better_experience.configuration.better_reinforced_tool", "Better Reinforced");
        add("better_experience.configuration.client_multi_fishing", "Client Multi-Fishing Using Hammers");
        add("better_experience.configuration.server_multi_fishing", "Server Multi-Fishing Using Hammers");
        add("better_experience.configuration.valid_bonemeal_target", "Bonemeal Can Be Used On Stone Saplings");
        add("better_experience.configuration.block_break_speed_multiplier", "Block Break Speed Multiplier");
        add("better_experience.configuration.forbidden_magic_boom_staff", "Forbidden Magic Boom Staff");
        add("better_experience.configuration.auto_save_money", "Auto Save Money To Piggy Bank");
        add("better_experience.configuration.quick_jei_fetch", "Quick JEI Fetch Ingredients From Nearby Chests");



        // info
        add("better_experience.autofish.info.lack", "Auto Fish Machine Lacks connection: ");
        add("better_experience.info.forbidden_magic_boom_staff", "You can't use magic boom staff in this world, please open function in ESC-MOD-better_experience-config-Item");
        add("better_experience.info.jei.not_enough_ingredients", "You have not enough ingredients from nearby chests");


        // container
        add("block.better_experience.autofish_machine", "Auto Fish Machine");


    }
}
