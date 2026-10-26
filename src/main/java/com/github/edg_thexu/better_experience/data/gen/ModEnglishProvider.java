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
        add("better_experience.anti_rage.prompt.toggle", "Anti-Rage: %s");
        add("better_experience.anti_rage.prompt.help", "Death 3 enables keep inventory; deaths 4+ extend hit protection; death 10 grants temporary creative. Host or OP only. Independent of the softcore choice above.");
        add("better_experience.anti_rage.prompt.read_only", "Uses server settings. Only the host or an OP can change this.");
        add("better_experience.anti_rage.prompt.save_failed", "Could not save Anti-Rage. The setting was not changed.");
        add("better_experience.world_options.unbreakable_eyes", "Eye recovery: %s");
        add("better_experience.world_options.unbreakable_eyes_help", "Thrown Eyes of Ender always drop for recovery after their flight.");
        add("better_experience.world_options.fair_trades", "Fair trades: %s");
        add("better_experience.world_options.fair_trades_help", "Removes demand and reputation price increases while preserving discounts, stock and restocking. Prices may rise again after disabling.");
        add("better_experience.world_options.friendly_neutrals", "Friendly neutrals: %s");
        add("better_experience.world_options.friendly_neutrals_help", "Neutral creatures stop targeting or damaging players and forget player anger. Hostile monsters and owned pets retain their normal combat.");
        add("better_experience.world_options.protected_pets", "Protect pets: %s");
        add("better_experience.world_options.protected_pets_help", "Owned pets retain at least 1 health. At 1 health they stop attacking and enemies forget them. Healing above 1 health restores combat.");
        add("better_experience.world_options.reusable_templates", "Keep templates: %s");
        add("better_experience.world_options.reusable_templates_help", "Smithing upgrades and armor trims preserve the template. Base equipment and additional materials are still consumed.");
        add("better_experience.world_options.no_cursed_loot", "No cursed loot: %s");
        add("better_experience.world_options.no_cursed_loot_help", "Removes curse enchantments from generated loot, trade results and newly spawned mob equipment while preserving items and other enchantments. Existing inventories are unchanged.");
        add("better_experience.world_options.hungry_sprinting", "Hungry sprint: %s");
        add("better_experience.world_options.hungry_sprinting_help", "Low hunger no longer prevents or stops sprinting. Food exhaustion and starvation damage retain their normal rules.");
        add("better_experience.world_options.full_speed_bows", "Full-speed bows: %s");
        add("better_experience.world_options.full_speed_bows_help", "Keeps normal movement and sprinting while drawing vanilla or Confluence bows. Eating, blocking and other movement restrictions are unchanged.");
        add("better_experience.world_options.permanent_night_vision", "Night vision: %s");
        add("better_experience.world_options.permanent_night_vision_help", "Provides steady night-vision brightness without replacing real potion effects. Disabling restores normal lighting or existing potion vision.");
        add("better_experience.world_options.extra_golem_iron", "Double golem iron: %s");
        add("better_experience.world_options.extra_golem_iron_help", "Doubles the iron ingots actually dropped by iron golems without changing other drops or loot conditions.");
        add("better_experience.world_options.leather_recipe", "Craft leather: %s");
        add("better_experience.world_options.leather_recipe_help", "Craft 1 leather from 4 rotten flesh in any arrangement, including the 2×2 inventory grid.");
        add("better_experience.world_options.enchanted_apple_recipe", "Craft god apples: %s");
        add("better_experience.world_options.enchanted_apple_recipe_help", "Craft 1 enchanted golden apple using 8 gold blocks surrounding 1 apple.");
        add("better_experience.world_options.title", "World Convenience Settings");
        add("better_experience.world_options.join_world", "Join a world to change settings");
        add("better_experience.world_options.world_scope", "Current world · Server settings");
        add("better_experience.world_options.read_only", "Read only · Host or OP required");
        add("better_experience.world_options.apply", "Apply settings");
        add("better_experience.world_options.defaults", "Reset defaults");
        add("better_experience.world_options.free_enchanting", "Free enchant XP: %s");
        add("better_experience.world_options.free_enchanting_help", "Enchanting spends no experience. Level requirements, lapis consumption and enchantment seed updates remain active.");
        add("better_experience.world_options.free_anchor", "Free anchor fuel: %s");
        add("better_experience.world_options.free_anchor_help", "Charging preserves held glowstone; respawning preserves charges. At least one charge is still required.");
        add("better_experience.world_options.safe_sleep", "Sleep anywhere: %s");
        add("better_experience.world_options.safe_sleep_help", "Beds also allow sleep and set spawn in other dimensions without exploding. Reach, space, time and nearby monster checks still apply.");
        add("better_experience.world_options.bed_blast_immunity", "Bed blast immunity: %s");
        add("better_experience.world_options.bed_blast_immunity_help", "Bed explosions still break blocks, but do no direct explosion damage or knockback to players. Anchors and other explosions are unchanged. Cross-dimension sleep prevents bed explosions entirely.");
        add("better_experience.world_options.instant_smelting", "Instant smelting: %s");
        add("better_experience.world_options.instant_smelting_help", "Furnaces, blast furnaces and smokers finish one recipe per tick. Recipes, fuel, output limits and experience still apply.");
        add("better_experience.world_options.instant_brewing", "Instant brewing: %s");
        add("better_experience.world_options.instant_brewing_help", "Brewing stands finish one batch per tick. Ingredients, fuel and mod brewing events still apply.");
        add("better_experience.world_options.crops", "Crop growth speed: %s×");
        add("better_experience.world_options.saplings", "Sapling growth speed: %s×");
        add("better_experience.world_options.growth_help", "0 pauses natural growth; 1 is normal; 2 roughly halves average growth time. Maximum 20×. Light, soil and space rules still apply. Bonemeal, global random ticks and unloaded areas are unchanged.");
        add("better_experience.spawning.title", "Better Experience · Settings");
        add("better_experience.spawning.rate", "Spawn rate: %s×");
        add("better_experience.spawning.cap", "Spawn cap: %s×");
        add("better_experience.spawning.rate_help", "Natural spawn attempt frequency; 0 disables it. Preserves Confluence modifiers. Excludes spawner blocks and invasion events.");
        add("better_experience.spawning.cap_help", "Multiplies global and local natural spawn caps. Lowering the cap does not remove existing mobs.");
        add("better_experience.spawning.apply", "Apply multipliers");
        add("better_experience.spawning.reset", "Reset to 1×");
        add("better_experience.spawning.mobs", "Spawn environment & mobs");
        add("better_experience.spawning.other", "Other settings");
        add("better_experience.spawning.join_world", "Join a world to adjust spawning");
        add("better_experience.spawning.loading", "Reading server data…");
        add("better_experience.spawning.world_scope", "Current world · Natural spawning");
        add("better_experience.spawning.read_only", "Read only · Host or OP required");
        add("better_experience.spawning.filter_help", "Checked mobs may spawn naturally if terrain, light and other rules allow. Applies nearby in your current biome; resets on biome change. The closest player's filter wins in shared areas.");
        add("better_experience.spawning.search", "Search mobs or mods");
        add("better_experience.spawning.allow_all", "Allow all");
        add("better_experience.spawning.environment", "Y %s · Light %s · Time %s");
        add("better_experience.spawning.empty", "No matching natural spawns");
        add("better_experience.spawning.truncated", "Showing the first 2048 entries");
        add("better_experience.spawning.enabled", "Allow");
        add("better_experience.spawning.disabled", "Block");
        add("better_experience.spawning.entry_info", "Candidate weight %s · Group %s–%s");

        add("better_experience.welcome_message", "Better Experience has been enabled, press ESC to access the mod's configuration to enable features.");

        add("creativetab.better_experience.item", "Confluence | Better Experience");


        add("better_experience.chronopilot.current", "Time: %s");
        add("better_experience.chronopilot.hour", "Hour");
        add("better_experience.chronopilot.minute", "Minute");
        add("better_experience.chronopilot.apply", "Set time");
        add("better_experience.chronopilot.dawn", "Dawn");
        add("better_experience.chronopilot.noon", "Noon");
        add("better_experience.chronopilot.dusk", "Dusk");
        add("better_experience.chronopilot.midnight", "Midnight");
        add("better_experience.chronopilot.world_time", "World time · 24-hour clock");

        // items
        Consumer<DeferredHolder<Item, ? extends Item>> itemAction = item -> add(item.get(), toTitleCase(item.getId().getPath()));
        ModItems.ITEMS.getEntries().forEach(itemAction);
        ModItems.TOOLS.getEntries().forEach(itemAction);
//        ModBlocks.BLOCKS.getEntries().forEach(block-> add(block.get(), toTitleCase(block.getId().getPath())));

        add("better_experience.staff.choose_corners", "%s+LMB: select");
        add("better_experience.staff.first_corner", "A set | LMB: set B");
        add("better_experience.staff.locked", "Locked | LMB blast | Shift+RMB clear");
        add("better_experience.staff.no_mana", "Need %s mana");
        add("better_experience.staff.no_target", "Aim within reach");
        add("better_experience.staff.out_of_reach", "Out of reach");
        add("better_experience.staff.reselect", "Cancelled | %s+LMB: reselect");
        add("better_experience.staff.size", "%sx%sx%s | Mana %s | Scroll size | RMB lock");
        add("better_experience.staff.too_large", "Selection limit reached");

        add("better_experience.staff.enter_selection", "%s+LMB: select");
        add("key.better_experience.staff_select_modifier", "Staff selection modifier (with left-click)");
        add("key.categories.better_experience", "Better Experience");

        add("better_experience.locator.title", "Structure Locator");
        add("better_experience.void_staff.tooltip.pickup", "Shift+RMB container: lift");
        add("better_experience.void_staff.tooltip.place", "Shift+LMB: place | Scroll: distance");
        add("better_experience.void_staff.tooltip.wheel", "Hold wheel key: facing and block states");
        add("better_experience.void_staff.carrying", "Place the carried container first");
        add("better_experience.void_staff.picked_up", "Lifted | Shift+LMB: place");
        add("better_experience.void_staff.placed", "Container placed");
        add("better_experience.void_staff.failed", "Transfer failed; container retained");
        add("better_experience.void_staff.blocked", "Cannot place here");
        add("better_experience.void_staff.ready", "Ready to place");
        add("better_experience.void_staff.preview", "Scroll: distance | Wheel: states");
        add("better_experience.void_staff.wheel", "Void Staff");
        add("better_experience.void_staff.wheel_help", "Release: choose | Click: repeat | Center: surface");
        add("better_experience.void_staff.rotate_left", "Turn left");
        add("better_experience.void_staff.rotate_right", "Turn right");
        add("better_experience.void_staff.closer", "Closer");
        add("better_experience.void_staff.farther", "Farther");
        add("better_experience.void_staff.previous_property", "Previous property");
        add("better_experience.void_staff.next_property", "Next property");
        add("better_experience.void_staff.previous_value", "Previous value");
        add("better_experience.void_staff.next_value", "Next value");
        add("better_experience.ammo_chain.title", "Ammo Chain");
        add("better_experience.ammo_chain.slot", "Slot %s");
        add("better_experience.ammo_chain.slot_details", "Slot %s\n%s × %s");
        add("better_experience.ammo_chain.empty", "Empty");
        add("better_experience.ammo_chain.add_slot", "Add slot");
        add("better_experience.ammo_chain.clear", "Clear");
        add("better_experience.ammo_chain.shots", "Shots");
        add("better_experience.ammo_chain.search", "Search ammo");
        add("better_experience.ammo_chain.enabled", "Enabled");
        add("better_experience.ammo_chain.disabled", "Disabled");
        add("better_experience.ammo_chain.enabled_help", "Carry to cycle shots; missing ammo is skipped");
        add("better_experience.ammo_chain.cancel", "Cancel");
        add("better_experience.ammo_chain.save", "Save");
        add("better_experience.ammo_chain.no_ammo", "No matching ammo");
        add("better_experience.ammo_chain.tooltip.configure", "RMB: configure ammo and counts");
        add("better_experience.ammo_chain.tooltip.use", "Carry to use ammo from your inventory");
        add("better_experience.ammo_chain.tooltip.slots", "%s slots · Missing ammo is skipped");
        add("better_experience.locator.search", "Search structures / @mod");
        add("better_experience.locator.select", "Select target");
        add("better_experience.locator.locate", "Locate · %s gold");
        add("better_experience.locator.locate_free", "Locate · Free");
        add("better_experience.locator.cost_help", "%s gold on success; failure/creative free");
        add("better_experience.locator.insufficient_funds", "Not enough money; locating costs 1 gold");
        add("better_experience.locator.no_change_space", "No room for change; free a slot and retry");
        add("better_experience.locator.marked_paid", "Marked · -%s gold");
        add("better_experience.locator.target", "Target: %s");
        add("better_experience.locator.no_results", "No matching structures");
        add("better_experience.locator.marked", "%s locations already marked");
        add("better_experience.locator.unavailable", "Structure unavailable in this dimension");
        add("better_experience.locator.busy", "Locator is busy");
        add("better_experience.locator.searching", "Searching within %s blocks");
        add("better_experience.locator.not_found", "No unmarked structure within search range");
        add("better_experience.locator.limit", "Search limit; try from another position");
        add("better_experience.locator.error", "Search failed; check the server log");
        add("better_experience.locator.history_full", "Structure marker history is full");
        add("better_experience.locator.found", "Marked %s: %s, %s, %s");
        add("better_experience.locator.marked_success", "Marked | RMB: locate next");
        add("better_experience.locator.tooltip.use", "RMB: locate nearest unmarked structure");
        add("better_experience.locator.tooltip.settings", "Shift+RMB: choose structure");
        add("better_experience.locator.tooltip.history", "History persists per player and world");

        add("better_experience.fluid.title", "Fluid Staff");
        add("better_experience.fluid.settings", "Fluids");
        add("better_experience.fluid.search", "Search fluids");
        add("better_experience.fluid.no_results", "No matching fluids");
        add("better_experience.fluid.mode.absorb", "Absorb");
        add("better_experience.fluid.mode.place", "Place");
        add("better_experience.fluid.clear_selection", "Clear selection");
        add("better_experience.fluid.total", "Total %s/%s ml");
        add("better_experience.fluid.total_infinite", "Total ∞ ml");
        add("better_experience.fluid.unit", "1000 ml = 1 fluid block");
        add("better_experience.fluid.locked", "Locked | LMB use | Shift+RMB clear");
        add("better_experience.fluid.busy", "Fluid operation in progress");
        add("better_experience.fluid.absorbed", "Absorbed %s ml");
        add("better_experience.fluid.placed", "Placed %s ml");
        add("better_experience.fluid.tooltip.settings", "RMB: settings | Hold G: shapes");
        add("better_experience.fluid.tooltip.select", "Shift+LMB: select | Scroll: size");
        add("better_experience.fluid.tooltip.locked", "RMB: lock | Shift+RMB: clear");
        add("better_experience.fluid.tooltip.capacity", "Survival: 512 blocks total | Creative: infinite");
        add("better_experience.fluid.wheel_hold", "Release: choose | Center/Tab: fluids | Esc: cancel");

        add("better_experience.space.title", "Space Staff");
        add("better_experience.space.search", "Search blocks");
        add("better_experience.space.search_help", "Name / ID / @mod | RMB: clear");
        add("better_experience.space.no_results", "No matching blocks");
        add("better_experience.space.pick", "Pick");
        add("better_experience.space.properties", "States %s/%s");
        add("better_experience.space.apply", "Save");
        add("better_experience.space.locked", "Locked | LMB place | Shift+RMB clear");
        add("better_experience.space.size", "%sx%sx%s | Scroll size | RMB lock");
        add("better_experience.space.busy", "Placement in progress");
        add("better_experience.space.placed", "Placed %s blocks");
        add("better_experience.space.lacking", "Out of materials; placed %s blocks");
        add("better_experience.space.tooltip.settings", "RMB: settings | Hold G: shapes");
        add("better_experience.space.tooltip.select", "Shift+LMB corners | Scroll size");
        add("better_experience.space.tooltip.locked", "RMB lock | LMB place | Shift+RMB clear");
        add("better_experience.space.tooltip.materials", "Survival uses materials; creative is free");
        add("better_experience.space.shape.cube", "Solid cube");
        add("better_experience.space.shape.hollow_cube", "Hollow cube");
        add("better_experience.space.shape.sphere", "Sphere");
        add("better_experience.space.shape.hollow_sphere", "Hollow sphere");
        add("better_experience.space.shape.line", "Line");
        add("better_experience.space.shape.bezier", "Bezier curve");
        add("key.better_experience.space_settings", "Staff shape wheel");

        add("better_experience.space.control", "%s %s: position inside selection (0-100%%)");
        add("better_experience.space.property.axis", "Axis");
        add("better_experience.space.property.facing", "Facing");
        add("better_experience.space.property.horizontal_facing", "Horizontal facing");
        add("better_experience.space.property.half", "Half");
        add("better_experience.space.property.type", "Type");
        add("better_experience.space.property.shape", "Shape");
        add("better_experience.space.property.waterlogged", "Waterlogged");
        add("better_experience.space.property.layers", "Layers");
        add("better_experience.space.property.lit", "Lit");
        add("better_experience.space.property.powered", "Powered");
        add("better_experience.space.property.open", "Open");
        add("better_experience.space.property.rotation", "Rotation");
        add("better_experience.space.property.snowy", "Snowy");
        add("better_experience.space.property.persistent", "Persistent");
        add("better_experience.space.property.distance", "Distance");
        add("better_experience.space.value.true", "True");
        add("better_experience.space.value.false", "False");
        add("better_experience.space.value.top", "Top");
        add("better_experience.space.value.bottom", "Bottom");
        add("better_experience.space.value.double", "Double");
        add("better_experience.space.value.upper", "Upper");
        add("better_experience.space.value.lower", "Lower");
        add("better_experience.space.value.north", "North");
        add("better_experience.space.value.south", "South");
        add("better_experience.space.value.east", "East");
        add("better_experience.space.value.west", "West");
        add("better_experience.space.value.up", "Up");
        add("better_experience.space.value.down", "Down");
        add("better_experience.space.value.straight", "Straight");
        add("better_experience.space.value.inner_left", "Inner left");
        add("better_experience.space.value.inner_right", "Inner right");
        add("better_experience.space.value.outer_left", "Outer left");
        add("better_experience.space.value.outer_right", "Outer right");

        add("better_experience.space.sphere_center", "Center set | LMB: horizontal radius");
        add("better_experience.space.sphere_horizontal", "Horizontal radius set | LMB: vertical radius");
        add("better_experience.space.bezier_points", "%s/%s points | LMB: add | RMB: lock");
        add("better_experience.space.bezier_limit", "18 points max | RMB: lock");
        add("better_experience.space.bezier_help", "Shift+LMB: start | LMB: add | RMB: lock");

        add("better_experience.space.add_control", "Add");
        add("better_experience.space.remove_control", "Remove");
        add("better_experience.space.wheel", "Choose shape");
        add("better_experience.space.block_state", "Block state");
        add("better_experience.space.wheel_hold", "Release: choose | Center/Tab: block | Esc: cancel");
        add("better_experience.space.wheel_click", "Click: choose | Center/Tab: back | Esc: cancel");

        // tooltips
        add("better_experience.tooltip.magic_boom_staff.info", "Offhand tool sets mining power");
        add("better_experience.tooltip.star_boom_staff.info", "Carried picks/drills set mining power");
        add("better_experience.tooltip.magic_boom_staff.select", "Shift+LMB: select (default key)");
        add("better_experience.tooltip.magic_boom_staff.corners", "LMB corners | Scroll size | RMB lock");
        add("better_experience.tooltip.magic_boom_staff.locked", "Locked: LMB blast | Shift+RMB clear");
        add("better_experience.tooltip.magic_boom_staff.move", "Shift+Scroll: move | Shift+MMB: forward");
        add("better_experience.tooltip.magic_boom_staff.mana", "1 mana per 8 blocks, rounded up");
        add("better_experience.tooltip.potion_bag.info", "Stores potions and food");
        add("better_experience.tooltip.jei.fetch_ingredients", "Fetch ingredients from nearby chests");

        add("better_experience.tooltip.better_reforge.enable", "Enable Better Reforging");


        add("better_experience.gui.fast_storage", "Fast Storage");
        add("better_experience.gui.open_config", "Open Better Experience Settings");
        add("better_experience.gui.potion_screen.auto_collect.message", "Auto Collect Potions and Food");

        // Config
        add("better_experience.configuration.Player", "Player");
        add("better_experience.configuration.World", "World");
        add("better_experience.configuration.Entity", "Entity");
        add("better_experience.configuration.Item", "Item");
        add("better_experience.configuration.AntiRage", "Anti-Rage Mode");
        add("better_experience.configuration.anti_rage_mode", "Enable Anti-Rage Mode");
        add("better_experience.configuration.anti_rage_mode.tooltip", "Death 3 enables world-wide keep inventory and singleplayer cheats. After deaths 4-9, hits grant x extra seconds of invulnerability during the first 10n seconds after respawn (n is this cycle's death count). Death 10 grants 100 seconds of creative, then restores the original game mode and clears the count. Cheats and keep inventory remain enabled. Disabling clears the count and ends temporary creative. Uses server settings in multiplayer; never grants OP.");
        add("better_experience.configuration.anti_rage_extra_invulnerability_seconds", "Extra Post-Hit Invulnerability (seconds)");
        add("better_experience.configuration.anti_rage_extra_invulnerability_seconds.tooltip", "Add x seconds to normal post-hit invulnerability during the protection window. Default: 1 second; range: 0-60. Damage that bypasses invulnerability still follows its normal rules.");
        add("better_experience.anti_rage.assistance", "Anti-Rage: 3 consecutive deaths. Keep inventory enabled.");
        add("better_experience.anti_rage.cheats", "Anti-Rage: singleplayer cheats enabled.");
        add("better_experience.anti_rage.creative", "Anti-Rage: 10 consecutive deaths. Temporary creative mode for 100 seconds.");
        add("better_experience.anti_rage.reset", "Anti-Rage: temporary creative ended and the death count was reset.");
        add("better_experience.anti_rage.progress", "Anti-Rage: %s/10 deaths this cycle. Post-hit protection starts on the fourth respawn.");
        add("better_experience.anti_rage.protection", "Anti-Rage: %s deaths. Respawn protection window: %s seconds; extra post-hit invulnerability: %s seconds.");
        add("better_experience.configuration.locator_search_radius", "Structure Locator Search Radius (blocks)");
        add("better_experience.configuration.locator_search_radius.tooltip", "Maximum structure search radius in blocks. Default: 32768. Skips structure starts already marked by the current player. Larger radii may take longer to search. The server setting applies in multiplayer.");



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
        add("better_experience.configuration.no_natural_monsters_during_boss", "No Natural Monsters During Boss Fights");
        add("better_experience.configuration.no_natural_monsters_during_boss.tooltip", "Prevent natural hostile spawns within 192 blocks of a living boss. Existing mobs, boss minions, spawners and passive mobs are unaffected. The server setting applies in multiplayer.");
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
