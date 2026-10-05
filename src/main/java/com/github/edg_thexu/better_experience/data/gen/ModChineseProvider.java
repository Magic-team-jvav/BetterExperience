package com.github.edg_thexu.better_experience.data.gen;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.init.ModBlocks;
import com.github.edg_thexu.better_experience.init.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;



public class ModChineseProvider extends LanguageProvider {
    public ModChineseProvider(PackOutput output) {
        super(output, Better_experience.MODID, "zh_cn");
    }

    @Override
    protected void addTranslations() {

        add("better_experience.welcome_message", "已启用 [汇流来世:更好的体验] , 按ESC并在mod中找到本模组配置启用功能。");

        add("creativetab.better_experience.item", "汇流来世 | 更好的体验");

        // items
        add(ModItems.MAGIC_BOOM_STAFF.get(), "法爆魔杖");
        add(ModItems.STAR_BOOM_STAFF.get(), "星爆魔杖");
        add(ModItems.POTION_BAG.get(), "药水袋");
        add(ModBlocks.AUTO_FISH_BLOCK.get(), "自动钓鱼机");
        add(ModBlocks.AUTO_SELL_BLOCK.get(), "自动贩卖机");
        add(ModBlocks.REFORGE_BLOCK.get(), "重铸机");

        add("better_experience.staff.choose_corners", "Shift+左键选区（默认）");
        add("better_experience.staff.first_corner", "已选A，左键选B");
        add("better_experience.staff.locked", "已锁定｜左键爆破｜右键取消");
        add("better_experience.staff.no_mana", "魔力不足：需要 %s");
        add("better_experience.staff.no_target", "请瞄准范围内方块");
        add("better_experience.staff.out_of_reach", "超出触及范围");
        add("better_experience.staff.reselect", "已取消，重新激活选区");
        add("better_experience.staff.size", "%s×%s×%s｜魔力 %s｜滚轮调整｜右键锁定");
        add("better_experience.staff.too_large", "达到选区上限");

        add("better_experience.staff.enter_selection", "%s+左键选区");
        add("key.better_experience.staff_select_modifier", "法杖选区修饰键（配合左键）");
        add("key.categories.better_experience", "更好的体验");

        // tooltips
        add("better_experience.tooltip.magic_boom_staff.info", "副手工具决定镐力");
        add("better_experience.tooltip.magic_boom_staff.select", "Shift+左键：选区（默认键）");
        add("better_experience.tooltip.magic_boom_staff.corners", "左键选两点，滚轮调整，右键锁定");
        add("better_experience.tooltip.magic_boom_staff.locked", "锁定后：左键爆破，右键取消");
        add("better_experience.tooltip.magic_boom_staff.move", "Shift+滚轮移动；Shift+中键前进");
        add("better_experience.tooltip.magic_boom_staff.mana", "每8格消耗1魔力，向上取整");
        add("better_experience.tooltip.potion_bag.info", "存放药水和食物");
        add("better_experience.tooltip.jei.fetch_ingredients", "从周围箱子取出材料");
        add("better_experience.tooltip.better_reforge.enable", "启用更好的重铸");

        add("better_experience.gui.fast_storage", "一键存储");
        add("better_experience.gui.potion_screen.auto_collect.message", "自动收集药水");

        // config
        add("better_experience.configuration.Player", "玩家");
        add("better_experience.configuration.World", "世界");
        add("better_experience.configuration.Entity", "生物");
        add("better_experience.configuration.Item", "物品");



        add("better_experience.configuration.show_outlines", "法爆魔杖显示边框");
        add("better_experience.configuration.auto_potion_open", "药水 无限续杯");
        add("better_experience.configuration.auto_potion_stack_size", "药水 无限续杯 需求量");
        add("better_experience.configuration.auto_potion_scan_item_per_tick", "药水 无限续杯 每秒扫描量");

        add("better_experience.configuration.instantly_drink", "瞬间喝药");
        add("better_experience.configuration.infinite_ammo", "无限弹药");
        add("better_experience.configuration.infinite_ammo_stack_size", "无限弹药 需求量");
        add("better_experience.configuration.modify_max_stack_size", "调整 物品 最大堆叠（重启游戏生效）");
        add("better_experience.configuration.no_consume_summoner", "不消耗BOSS召唤物");
        add("better_experience.configuration.slime_die_no_lava", "岩浆史莱姆死亡不生成岩浆");
        add("better_experience.configuration.additional_fall_distance", "额外的摔落免疫高度");
        add("better_experience.configuration.stone_sapling_tree_no_strict", "宝石树生长无限制");
        add("better_experience.configuration.fill_life_on_respawn", "重生时回满生命值");
        add("better_experience.configuration.herb_growth_no_strict", "草生长无限制");
        add("better_experience.configuration.better_reinforced_tool", "更好的重铸");
        add("better_experience.configuration.client_multi_fishing", "客户端 抡锤子钓鱼bug");
        add("better_experience.configuration.server_multi_fishing", "服务器 抡锤子钓鱼bug");
        add("better_experience.configuration.valid_bonemeal_target", "骨粉可以催熟宝石树");
        add("better_experience.configuration.block_break_speed_multiplier", "方块 破坏速度 倍率");
        add("better_experience.configuration.forbidden_magic_boom_staff", "禁用法爆魔杖");
        add("better_experience.configuration.auto_save_money", "自动把钱存到猪猪存钱罐");
        add("better_experience.configuration.quick_jei_fetch", "JEI快速从周围箱子取材料");


        // info
        add("better_experience.autofish.info.lack", "自动钓鱼机缺少方块连接：");
        add("better_experience.info.forbidden_magic_boom_staff", "你不能在这个世界使用法爆魔杖，请在ESC-MOD-better_experience-config-Item中打开功能");
        add("better_experience.info.jei.not_enough_ingredients", "附近的箱子没有足够的材料");

        // container
        add("block.better_experience.autofish_machine", "自动钓鱼机");
    }
}
