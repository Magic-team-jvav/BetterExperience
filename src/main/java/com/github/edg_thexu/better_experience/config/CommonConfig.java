package com.github.edg_thexu.better_experience.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CommonConfig {
    public static ModConfigSpec.IntValue LOCATOR_SEARCH_RADIUS;
    //************************* Item *************************
    /**
     * 是否开启药水无线续杯
     */
    public static ModConfigSpec.BooleanValue AUTO_POTION_OPEN;
    /**
     * 无限续杯数量
     */
    public static ModConfigSpec.IntValue AUTO_POTION_STACK_SIZE;
    /**
     * 无限续杯每刻扫描物品数量
     */
    public static ModConfigSpec.IntValue AUTO_POTION_SCAN_ITEM_PER_TICK;
    /**
     * 瞬间喝药
     */
    public static ModConfigSpec.BooleanValue INSTANTLY_DRINK;
    /**
     * 是否开启药水无线续杯
     */
    public static ModConfigSpec.BooleanValue INFINITE_AMMO;
    /**
     * 无限弹药数量
     */
    public static ModConfigSpec.IntValue INFINITE_AMMO_STACK_SIZE;
    /**
     * 召唤BOSS无需消耗召唤物品
     */
    public static ModConfigSpec.BooleanValue NO_CONSUME_SUMMONER;
    /**
     * 更好的重铸
     */
    public static ModConfigSpec.BooleanValue BETTER_REINFORCED_TOOL;
    /**
     * 修改物品最大堆叠
     */
    public static ModConfigSpec.BooleanValue MODIFY_MAX_STACK_SIZE;

    /**
     * 服务器禁用法爆魔杖
     */
    public static ModConfigSpec.BooleanValue FORBIDDEN_MAGIC_BOOM_STAFF;


    //************************** Player ****************************
    /**
     * 重生时填满生命值
     */
    public static ModConfigSpec.BooleanValue FILL_LIFE_ON_RESPAWN;
    /**
     * 额外的摔落免疫
     */
    public static ModConfigSpec.IntValue ADDITIONAL_FALL_DISTANCE;
    /**
     * 多重钓鱼
     */
    public static ModConfigSpec.BooleanValue MULTI_FISH;
    /**
     * 挖矿速度++
     */
    public static ModConfigSpec.DoubleValue BLOCK_BREAK_SPEED_MULTIPLIER;
    /**
     * 自动存钱
     */
    public static ModConfigSpec.BooleanValue AUTO_SAVE_MONEY;
    /**
     * jei从附近箱子取出材料
     */
    public static ModConfigSpec.BooleanValue QUICK_JEI_FETCH;

    //*************************** World *****************************
     /**
     * 宝石树种植无限制
     */
    public static ModConfigSpec.BooleanValue STONE_SAPLING_TREE_NO_STRICT;
    /**
     * 草药种植无限制
     */
    public static ModConfigSpec.BooleanValue HERB_GROWTH_NO_STRICT;
    /**
     * 骨粉催熟
     */
    public static ModConfigSpec.BooleanValue VALID_BONEMEAL_TARGET;

    //**************************** Entity ****************************
    /**
     * 禁止史莱姆死亡时生成熔岩
     */
    public static ModConfigSpec.BooleanValue SLIME_DIE_NO_LAVA;
    public static ModConfigSpec.BooleanValue NO_NATURAL_MONSTERS_DURING_BOSS;

    public static ModConfigSpec init() {
        final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        BUILDER.push("Item");
        LOCATOR_SEARCH_RADIUS = BUILDER.comment("Structure locator search radius in blocks; previously marked starts are excluded per player.")
                .defineInRange("locator_search_radius", 32768, 256, 262144);

        // 药水无线续杯
        AUTO_POTION_OPEN = BUILDER
                .comment("Apply potion automatically without consuming. Support Inventory, Safe, Piggy Bank and Ender Chest.")
                .define("auto_potion_open", true);
        AUTO_POTION_STACK_SIZE = BUILDER
                .comment("How many potion stack could apply without consuming automatically?")
                .defineInRange("auto_potion_stack_size", 10, 1, 9999);
        AUTO_POTION_SCAN_ITEM_PER_TICK = BUILDER
                .comment("How many items to scan per tick for infinite potion?")
                .defineInRange("auto_potion_scan_item_per_tick", 2, 1, 10);

        INSTANTLY_DRINK = BUILDER
                .comment("Instantly Drink")
                .define("instantly_drink", false);

        // 无限弹药
        INFINITE_AMMO = BUILDER
                .comment("Should the player have infinite ammo?")
                .define("infinite_ammo", false);
        INFINITE_AMMO_STACK_SIZE = BUILDER
                .comment("How many ammo stack could apply without consuming automatically?")
                .defineInRange("infinite_ammo_stack_size", 3996, 64, 9999);
        NO_CONSUME_SUMMONER = BUILDER
                .comment("Not consume summon item?")
                .define("no_consume_summoner", false);
        BETTER_REINFORCED_TOOL = BUILDER
                .comment("Better reinforced tool.")
                .define("better_reinforced_tool", true);
        MODIFY_MAX_STACK_SIZE = BUILDER
                .comment("Modify Max Stack Size")
                .define("modify_max_stack_size", false);
        FORBIDDEN_MAGIC_BOOM_STAFF = BUILDER
                .comment("Forbidden magic boom staff.")
                .define("forbidden_magic_boom_staff", false);

        BUILDER.pop();
        BUILDER.push("Player");

        FILL_LIFE_ON_RESPAWN = BUILDER
                .comment("Fill life on respawn.")
                .define("fill_life_on_respawn", false);
        ADDITIONAL_FALL_DISTANCE = BUILDER
                .comment("Additional fall distance for players.")
                .defineInRange("additional_fall_distance", 0, 0, 100);
        MULTI_FISH = BUILDER
                .comment("Multi fish.")
                .define("server_multi_fishing", true);
        BLOCK_BREAK_SPEED_MULTIPLIER = BUILDER
                .comment("Block Break Speed Multiplier")
                .defineInRange("block_break_speed_multiplier", 1, 0.1, 100.0);
        AUTO_SAVE_MONEY = BUILDER
                .comment("Auto Save Money.")
                .define("auto_save_money", true);
        QUICK_JEI_FETCH = BUILDER
                .comment("Quick JEI Fetch.")
                .define("quick_jei_fetch", true);
        BUILDER.pop();
        BUILDER.push("World");


        STONE_SAPLING_TREE_NO_STRICT = BUILDER
                .comment("Stone sapling tree grows without strict conditions.")
                .define("stone_sapling_tree_no_strict", false);
        HERB_GROWTH_NO_STRICT = BUILDER
                .comment("Herb growth without strict conditions.")
                .define("herb_growth_no_strict", false);
        VALID_BONEMEAL_TARGET = BUILDER
                .comment("Valid bonemeal target.")
                .define("valid_bonemeal_target", true);

        BUILDER.pop();
        BUILDER.push("Entity");

        SLIME_DIE_NO_LAVA = BUILDER
                .comment("Forbidden slime to generate lava?")
                .define("slime_die_no_lava", true);

        NO_NATURAL_MONSTERS_DURING_BOSS = BUILDER
                .comment("Prevent natural hostile mob spawns within 192 blocks of a living boss. "
                        + "Existing mobs, boss summons, spawners and passive mobs are unaffected. "
                        + "In multiplayer, the server's setting applies.")
                .define("no_natural_monsters_during_boss", false);

        BUILDER.pop();

        SPEC = BUILDER.build();
        return SPEC;
    }

    public static ModConfigSpec SPEC;
}
