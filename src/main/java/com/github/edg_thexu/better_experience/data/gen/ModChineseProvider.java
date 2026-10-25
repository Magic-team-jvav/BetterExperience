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
        add("better_experience.world_options.title", "世界便利设置");
        add("better_experience.world_options.join_world", "进入世界后可调整设置");
        add("better_experience.world_options.world_scope", "当前世界 · 服务端生效");
        add("better_experience.world_options.read_only", "只读 · 房主或 OP 可调整");
        add("better_experience.world_options.apply", "应用设置");
        add("better_experience.world_options.defaults", "恢复默认");
        add("better_experience.world_options.free_enchanting", "附魔免经验：%s");
        add("better_experience.world_options.free_enchanting_help", "附魔不扣经验，仍需达到等级要求并消耗青金石。保留附魔种子刷新。");
        add("better_experience.world_options.free_anchor", "重生锚免消耗：%s");
        add("better_experience.world_options.free_anchor_help", "充能不扣手中荧石，重生不扣充能次数；仍需至少一次充能。");
        add("better_experience.world_options.safe_sleep", "跨维度睡觉：%s");
        add("better_experience.world_options.safe_sleep_help", "床在其他维度也能睡觉、设置重生点且不爆炸。仍需满足距离、空间、时间及附近无怪物等条件。");
        add("better_experience.world_options.bed_blast_immunity", "床爆炸免伤：%s");
        add("better_experience.world_options.bed_blast_immunity_help", "保留床爆炸及方块破坏，玩家不受该次爆炸的直接伤害和击退。不影响重生锚和其他爆炸；开启跨维度睡觉后床不会爆炸。");
        add("better_experience.world_options.instant_smelting", "瞬间熔炼：%s");
        add("better_experience.world_options.instant_smelting_help", "熔炉、高炉、烟熏炉每刻完成一次熔炼。仍需配方、燃料和产物空间，保留经验产出。");
        add("better_experience.world_options.instant_brewing", "瞬间酿造：%s");
        add("better_experience.world_options.instant_brewing_help", "酿造台每刻完成一批药水。正常消耗原料和燃料，并保留模组酿造事件。");
        add("better_experience.world_options.crops", "农作物生长速度：%s×");
        add("better_experience.world_options.saplings", "树苗生长速度：%s×");
        add("better_experience.world_options.growth_help", "0 暂停自然生长，1 为原速，2 倍约为一半平均耗时，最高 20 倍。保留光照、土壤及空间要求，不改变骨粉、全局随机刻或未加载区域。");
        add("better_experience.spawning.title", "更好的体验 · 设置");
        add("better_experience.spawning.rate", "刷怪速率：%s×");
        add("better_experience.spawning.cap", "刷怪上限：%s×");
        add("better_experience.spawning.rate_help", "自然生成尝试频率，0 为关闭。保留汇流原有加成，不影响刷怪笼、入侵事件。");
        add("better_experience.spawning.cap_help", "自然刷怪的全局与玩家附近上限倍率。降低上限不会移除已有生物。");
        add("better_experience.spawning.apply", "应用倍率");
        add("better_experience.spawning.reset", "恢复 1 倍");
        add("better_experience.spawning.mobs", "刷怪环境与生物筛选");
        add("better_experience.spawning.other", "其他设置");
        add("better_experience.spawning.join_world", "进入世界后可调整刷怪");
        add("better_experience.spawning.loading", "正在读取服务端数据…");
        add("better_experience.spawning.world_scope", "当前世界 · 自然刷怪");
        add("better_experience.spawning.read_only", "只读 · 房主或 OP 可调整");
        add("better_experience.spawning.filter_help", "勾选允许自然生成，仍需满足地形、光照等条件。筛选限自身附近的当前群系，切换群系即清除；多人重叠以最近玩家为准。");
        add("better_experience.spawning.search", "搜索生物或模组");
        add("better_experience.spawning.allow_all", "全部允许");
        add("better_experience.spawning.environment", "高度 %s · 光照 %s · 时间 %s");
        add("better_experience.spawning.empty", "没有匹配的自然生成生物");
        add("better_experience.spawning.truncated", "生物过多，列表仅显示前 2048 项");
        add("better_experience.spawning.enabled", "允许");
        add("better_experience.spawning.disabled", "禁止");
        add("better_experience.spawning.entry_info", "候选权重 %s · 每群 %s–%s");

        add("better_experience.welcome_message", "已启用 [汇流来世:更好的体验] , 按ESC并在mod中找到本模组配置启用功能。");

        add("creativetab.better_experience.item", "汇流来世 | 更好的体验");

        add(ModBlocks.CHRONOPILOT.get(), "Chronopilot 时钟");
        add("better_experience.chronopilot.current", "时间 %s");
        add("better_experience.chronopilot.hour", "时");
        add("better_experience.chronopilot.minute", "分");
        add("better_experience.chronopilot.apply", "设置时间");
        add("better_experience.chronopilot.dawn", "清晨");
        add("better_experience.chronopilot.noon", "正午");
        add("better_experience.chronopilot.dusk", "黄昏");
        add("better_experience.chronopilot.midnight", "午夜");
        add("better_experience.chronopilot.world_time", "调整世界时间 · 24小时制");

        // items
        add(ModItems.MAGIC_BOOM_STAFF.get(), "法爆魔杖");
        add(ModItems.STAR_BOOM_STAFF.get(), "星爆魔杖");
        add(ModItems.POTION_BAG.get(), "药水袋");
        add(ModBlocks.AUTO_FISH_BLOCK.get(), "自动钓鱼机");
        add(ModBlocks.AUTO_SELL_BLOCK.get(), "自动贩卖机");
        add(ModBlocks.REFORGE_BLOCK.get(), "重铸机");

        add("better_experience.staff.choose_corners", "%s+左键选区");
        add("better_experience.staff.first_corner", "已选A，左键选B");
        add("better_experience.staff.locked", "已锁定｜左键爆破｜Shift+右键取消");
        add("better_experience.staff.no_mana", "魔力不足：需要 %s");
        add("better_experience.staff.no_target", "请瞄准范围内方块");
        add("better_experience.staff.out_of_reach", "超出触及范围");
        add("better_experience.staff.reselect", "已取消，%s+左键重新选区");
        add("better_experience.staff.size", "%s×%s×%s｜魔力 %s｜滚轮调整｜右键锁定");
        add("better_experience.staff.too_large", "达到选区上限");

        add("better_experience.staff.enter_selection", "%s+左键选区");
        add("key.better_experience.staff_select_modifier", "法杖选区修饰键（配合左键）");
        add("key.categories.better_experience", "更好的体验");

        add("better_experience.locator.title", "结构定位球");
        add("better_experience.void_staff.tooltip.pickup", "Shift+右键容器：搬起");
        add("better_experience.void_staff.tooltip.place", "Shift+左键放置 · 滚轮调距离");
        add("better_experience.void_staff.tooltip.wheel", "按住轮盘键：方向与方块状态");
        add("better_experience.void_staff.carrying", "已有搬运中的容器，请先放置");
        add("better_experience.void_staff.picked_up", "已搬起 · Shift+左键放置");
        add("better_experience.void_staff.placed", "容器已放置");
        add("better_experience.void_staff.failed", "无法搬运，原容器保持完整");
        add("better_experience.void_staff.blocked", "此处不可放置");
        add("better_experience.void_staff.ready", "可放置");
        add("better_experience.void_staff.preview", "滚轮距离 · 轮盘调状态");
        add("better_experience.void_staff.wheel", "虚空法杖");
        add("better_experience.void_staff.wheel_help", "松键选择 · 点击可连调 · 中心恢复贴面");
        add("better_experience.void_staff.rotate_left", "左转");
        add("better_experience.void_staff.rotate_right", "右转");
        add("better_experience.void_staff.closer", "靠近");
        add("better_experience.void_staff.farther", "远离");
        add("better_experience.void_staff.previous_property", "上一属性");
        add("better_experience.void_staff.next_property", "下一属性");
        add("better_experience.void_staff.previous_value", "上一取值");
        add("better_experience.void_staff.next_value", "下一取值");
        add("better_experience.ammo_chain.title", "弹药链");
        add("better_experience.ammo_chain.slot", "槽位 %s");
        add("better_experience.ammo_chain.slot_details", "槽位 %s\n%s × %s");
        add("better_experience.ammo_chain.empty", "空槽");
        add("better_experience.ammo_chain.add_slot", "添加槽位");
        add("better_experience.ammo_chain.clear", "清空");
        add("better_experience.ammo_chain.shots", "连射数量");
        add("better_experience.ammo_chain.search", "搜索弹药");
        add("better_experience.ammo_chain.enabled", "已启用");
        add("better_experience.ammo_chain.disabled", "已停用");
        add("better_experience.ammo_chain.enabled_help", "携带并启用时循环发射，缺弹自动跳过");
        add("better_experience.ammo_chain.cancel", "取消");
        add("better_experience.ammo_chain.save", "保存");
        add("better_experience.ammo_chain.no_ammo", "背包中无匹配弹药");
        add("better_experience.ammo_chain.tooltip.configure", "右键配置弹药与数量");
        add("better_experience.ammo_chain.tooltip.use", "背包携带生效，消耗背包弹药");
        add("better_experience.ammo_chain.tooltip.slots", "%s槽 · 缺弹自动跳过");
        add("better_experience.locator.search", "搜索结构 / @模组");
        add("better_experience.locator.select", "选择目标");
        add("better_experience.locator.locate", "定位 · %s金币");
        add("better_experience.locator.locate_free", "定位 · 免费");
        add("better_experience.locator.cost_help", "成功扣%s金币，失败免费；创造免费");
        add("better_experience.locator.insufficient_funds", "金币不足，定位需要1金币");
        add("better_experience.locator.no_change_space", "零钱空间不足，请空出一格再试");
        add("better_experience.locator.marked_paid", "已标记 · 消耗%s金币");
        add("better_experience.locator.target", "目标：%s");
        add("better_experience.locator.no_results", "无匹配结构");
        add("better_experience.locator.marked", "已标记 %s 处");
        add("better_experience.locator.unavailable", "此维度不生成该结构");
        add("better_experience.locator.busy", "正在定位，请稍候");
        add("better_experience.locator.searching", "正在搜索 %s 格内的未标记结构");
        add("better_experience.locator.not_found", "范围内没有未标记的目标结构");
        add("better_experience.locator.limit", "搜索达到限制，可换位置重试");
        add("better_experience.locator.error", "搜索失败，详情见服务器日志");
        add("better_experience.locator.history_full", "标记记录已满");
        add("better_experience.locator.found", "已标记 %s：%s, %s, %s");
        add("better_experience.locator.marked_success", "已标记｜右键定位下一处");
        add("better_experience.locator.tooltip.use", "右键定位最近的未标记结构");
        add("better_experience.locator.tooltip.settings", "Shift+右键：选择结构");
        add("better_experience.locator.tooltip.history", "记录跟随玩家和世界保存");

        add("better_experience.fluid.title", "液体法杖");
        add("better_experience.fluid.settings", "液体配置");
        add("better_experience.fluid.search", "搜索液体");
        add("better_experience.fluid.no_results", "无匹配液体");
        add("better_experience.fluid.mode.absorb", "吸收模式");
        add("better_experience.fluid.mode.place", "放置模式");
        add("better_experience.fluid.clear_selection", "清除选区");
        add("better_experience.fluid.total", "总量 %s/%s ml");
        add("better_experience.fluid.total_infinite", "总量 ∞ ml");
        add("better_experience.fluid.unit", "1000 ml = 1 格液体");
        add("better_experience.fluid.locked", "已锁定｜左键使用｜Shift+右键取消");
        add("better_experience.fluid.busy", "正在处理液体");
        add("better_experience.fluid.absorbed", "已吸收 %s ml");
        add("better_experience.fluid.placed", "已放置 %s ml");
        add("better_experience.fluid.tooltip.settings", "右键配置；按住 G 选择形状");
        add("better_experience.fluid.tooltip.select", "Shift+左键选区；滚轮调整");
        add("better_experience.fluid.tooltip.locked", "右键锁定；Shift+右键取消");
        add("better_experience.fluid.tooltip.capacity", "生存共存 512 格；创造无限");
        add("better_experience.fluid.wheel_hold", "松开确认｜中心/Tab：液体｜Esc：取消");

        add("better_experience.space.title", "空间法杖");
        add("better_experience.space.search", "搜索方块");
        add("better_experience.space.search_help", "名称 / ID / @模组｜右键清空");
        add("better_experience.space.no_results", "无匹配方块");
        add("better_experience.space.pick", "取样");
        add("better_experience.space.properties", "状态 %s/%s");
        add("better_experience.space.apply", "保存");
        add("better_experience.space.locked", "已锁定｜左键放置｜Shift+右键取消");
        add("better_experience.space.size", "%s×%s×%s｜滚轮调整｜右键锁定");
        add("better_experience.space.busy", "正在放置，请稍候");
        add("better_experience.space.placed", "已放置 %s 个方块");
        add("better_experience.space.lacking", "材料不足，已放置 %s 个方块");
        add("better_experience.space.tooltip.settings", "右键配置；按住 G 选择形状");
        add("better_experience.space.tooltip.select", "Shift+左键选两点，滚轮调整");
        add("better_experience.space.tooltip.locked", "右键锁定；左键放置；Shift+右键取消");
        add("better_experience.space.tooltip.materials", "生存消耗材料；创造不限材料");
        add("better_experience.space.shape.cube", "实心立方体");
        add("better_experience.space.shape.hollow_cube", "空心立方体");
        add("better_experience.space.shape.sphere", "球");
        add("better_experience.space.shape.hollow_sphere", "空心球");
        add("better_experience.space.shape.line", "直线");
        add("better_experience.space.shape.bezier", "贝塞尔曲线");
        add("key.better_experience.space_settings", "法杖形状轮盘");
        add(ModItems.SPACE_STAFF.get(), "空间法杖");
        add(ModItems.FLUID_STAFF.get(), "液体法杖");
        add(ModItems.STRUCTURE_LOCATOR_ORB.get(), "结构定位球");
        add(ModItems.VOID_STAFF.get(), "虚空法杖");
        add(ModItems.AMMO_CHAIN.get(), "弹药链");

        add("better_experience.space.control", "%s %s：选区内位置 0–100%%");
        add("better_experience.space.property.axis", "轴向");
        add("better_experience.space.property.facing", "朝向");
        add("better_experience.space.property.horizontal_facing", "水平朝向");
        add("better_experience.space.property.half", "上下位置");
        add("better_experience.space.property.type", "类型");
        add("better_experience.space.property.shape", "形状");
        add("better_experience.space.property.waterlogged", "含水");
        add("better_experience.space.property.layers", "层数");
        add("better_experience.space.property.lit", "点亮");
        add("better_experience.space.property.powered", "充能");
        add("better_experience.space.property.open", "开启");
        add("better_experience.space.property.rotation", "旋转");
        add("better_experience.space.property.snowy", "积雪");
        add("better_experience.space.property.persistent", "持久");
        add("better_experience.space.property.distance", "距离");
        add("better_experience.space.value.true", "是");
        add("better_experience.space.value.false", "否");
        add("better_experience.space.value.top", "上");
        add("better_experience.space.value.bottom", "下");
        add("better_experience.space.value.double", "双层");
        add("better_experience.space.value.upper", "上部");
        add("better_experience.space.value.lower", "下部");
        add("better_experience.space.value.north", "北");
        add("better_experience.space.value.south", "南");
        add("better_experience.space.value.east", "东");
        add("better_experience.space.value.west", "西");
        add("better_experience.space.value.up", "上");
        add("better_experience.space.value.down", "下");
        add("better_experience.space.value.straight", "直");
        add("better_experience.space.value.inner_left", "内左");
        add("better_experience.space.value.inner_right", "内右");
        add("better_experience.space.value.outer_left", "外左");
        add("better_experience.space.value.outer_right", "外右");

        add("better_experience.space.sphere_center", "中心已选，左键选水平半径");
        add("better_experience.space.sphere_horizontal", "水平半径已选，左键选垂直半径");
        add("better_experience.space.bezier_points", "%s/%s 点｜左键加点｜右键锁定");
        add("better_experience.space.bezier_limit", "最多 18 点｜右键锁定");
        add("better_experience.space.bezier_help", "Shift+左键开始；左键加点；右键锁定");

        add("better_experience.space.add_control", "加点");
        add("better_experience.space.remove_control", "删点");
        add("better_experience.space.wheel", "选择形状");
        add("better_experience.space.block_state", "方块状态");
        add("better_experience.space.wheel_hold", "松开确认｜中心/Tab：方块｜Esc：取消");
        add("better_experience.space.wheel_click", "点击选择｜中心/Tab：返回｜Esc：取消");

        // tooltips
        add("better_experience.tooltip.magic_boom_staff.info", "副手工具决定镐力");
        add("better_experience.tooltip.star_boom_staff.info", "随身镐与钻头决定镐力");
        add("better_experience.tooltip.magic_boom_staff.select", "Shift+左键：选区（默认键）");
        add("better_experience.tooltip.magic_boom_staff.corners", "左键选两点，滚轮调整，右键锁定");
        add("better_experience.tooltip.magic_boom_staff.locked", "锁定后：左键爆破，Shift+右键取消");
        add("better_experience.tooltip.magic_boom_staff.move", "Shift+滚轮移动；Shift+中键前进");
        add("better_experience.tooltip.magic_boom_staff.mana", "每8格消耗1魔力，向上取整");
        add("better_experience.tooltip.potion_bag.info", "存放药水和食物");
        add("better_experience.tooltip.jei.fetch_ingredients", "从周围箱子取出材料");
        add("better_experience.tooltip.better_reforge.enable", "启用更好的重铸");

        add("better_experience.gui.fast_storage", "一键存储");
        add("better_experience.gui.open_config", "打开更好的体验配置");
        add("better_experience.gui.potion_screen.auto_collect.message", "自动收集药水");

        // config
        add("better_experience.configuration.Player", "玩家");
        add("better_experience.configuration.World", "世界");
        add("better_experience.configuration.Entity", "生物");
        add("better_experience.configuration.Item", "物品");
        add("better_experience.configuration.AntiRage", "防急眼模式");
        add("better_experience.configuration.anti_rage_mode", "启用防急眼模式");
        add("better_experience.configuration.anti_rage_mode.tooltip", "第3次死亡开启全世界死亡不掉落，并在单人世界开启作弊。第4至9次死亡后，重生的10n秒内受击无敌帧额外延长x秒（n为本轮死亡次数）。第10次重生给予100秒创造模式，结束后恢复原游戏模式并清零计数；保留作弊和死亡不掉落。关闭模式会清零计数并结束临时创造。联机时以服务器配置为准，不自动授予OP。");
        add("better_experience.configuration.anti_rage_extra_invulnerability_seconds", "受击无敌帧额外延长（秒）");
        add("better_experience.configuration.anti_rage_extra_invulnerability_seconds.tooltip", "保护时段内，每次正常受击后的无敌时间额外延长x秒；默认1秒，可设置为0至60秒。不改变绕过无敌帧的伤害规则。");
        add("better_experience.anti_rage.assistance", "防急眼模式：连续死亡3次，已开启死亡不掉落。");
        add("better_experience.anti_rage.cheats", "防急眼模式：已为单人世界开启作弊。");
        add("better_experience.anti_rage.creative", "防急眼模式：连续死亡10次，进入100秒临时创造模式。");
        add("better_experience.anti_rage.reset", "防急眼模式：临时创造已结束，死亡计数已清零。");
        add("better_experience.anti_rage.progress", "防急眼模式：本轮死亡%s/10次，受击保护从第4次重生开始。");
        add("better_experience.anti_rage.protection", "防急眼模式：本轮死亡%s次，重生保护窗口%s秒；受击后无敌时间额外延长%s秒。");
        add("better_experience.configuration.locator_search_radius", "结构定位球搜索半径（方块）");
        add("better_experience.configuration.locator_search_radius.tooltip", "结构定位球搜索结构的最大半径，单位为方块，默认32768。搜索时会跳过当前玩家已标记的结构起点。半径越大，搜索可能耗时越长；联机时以服务器配置为准。");



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
        add("better_experience.configuration.no_natural_monsters_during_boss", "打BOSS时不生成小怪");
        add("better_experience.configuration.no_natural_monsters_during_boss.tooltip", "阻止存活BOSS周围192格内自然刷新的敌怪。保留已有怪物、BOSS召唤的随从、刷怪笼和友好生物；联机时以服务器配置为准。");
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
