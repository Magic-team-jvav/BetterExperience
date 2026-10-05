# 代码审核与修复（2026-10-05）

扫描 Java 源码、注册入口与资源引用，重点复核法杖、药水袋、自动收纳、配置重载和网络处理。保留已有框选和 GPU 修改。以下为本次新增修复；审核不等于证明整个项目没有并发问题或循环依赖。

| 问题 | 修复 |
| --- | --- |
| 物品注册字段使用类名式命名，允许重新赋值 | ModItems 使用 static final 和大写下划线常量；注册字符串不变 |
| 法杖 range 构造参数没有使用 | 删除多余参数 |
| 同类不同组件物品被混合 | ModUtils 共用按组件比较的合并规则；防止自合并和超过堆叠上限 |
| 循环内 indexOf 查找空槽 | 索引扫描，支持跨空槽存放 |
| 箱子容量判断错误使用 Z 偏移 | StorageManager 统一合并规则；标记箱子脏，通过精妙存储 handler 插入；检查服务器侧和区块加载 |
| 药水袋部分收纳成功却未保存内容 | PlayerInventoryManager 根据实际数量变化保存 |
| 药水袋打开忽略空槽，压缩槽位 | PotionBagMenu 按原始槽位复制 |
| 关闭菜单可能写入另一只药水袋 | 绑定原物品和槽位；禁止取出或交换源物品 |
| 自动收纳开关丢失，可变组件破坏值语义 | ItemContainerComponent 改为不可变值，菜单保存当前开关 |
| 组件误实现类型接口，非法尺寸导致越界 | 移除 DataComponentType 实现，限制尺寸为 9–54 整行，读取返回独立物品副本 |
| 配置重载累积旧条目，服务端效果阈值为空，重复条目抛异常 | ForbiddenConfig 解码时构建阈值，重复项采用最低阈值；同步和重载替换全部快照 |
| 配置线程看到半更新集合，单例初始化不安全 | volatile 发布不可变快照，holder 单例 |
| 爆破队列删除了玩家而非任务 | 迭代器删除任务 |
| 跨维度后破坏错误世界，断线任务继续运行 | 捕获原服务器世界及副手工具；断线、移除、换维度取消，服务器停止清空 |
| 执行时不复核，可能重复产生掉落 | 复核可破坏性、容器、区块、权限和 NeoForge 事件；不产生原版掉落地移除后汇总一次 |
| 方块回调添加任务导致迭代冲突 | 待处理任务放入线程安全队列，下个 tick 并入 |
| 保护事件修改方块后仍用旧状态 | 事件后核对状态，改变则跳过 |
| 断线后同步包访问空玩家 | EnderChestItemsS2C 和 ClientBoundConfigPacket 空值保护 |
| 网络枚举 ID 直接索引数组 | 非法 ID 抛明确解码异常 |
| 附件重读累积、共享集合、非法物品 ID 崩溃 | EnderChestAttachment 清旧数据、防御复制、只读列表、安全解析 ID |
| 混合逻辑条件难读、多余类型判断和空事件 | 明确逻辑括号，移除冗余判断、导入及废弃注释代码 |

删除经源码和资源检查未接入的类：AbstractBufferManager、空 AutoFishMachineModel、AbstractJsonConfig、FrameBasedQueueProcessor、未注册的 RecipeHandlerFactoryProviderTypes。删除未使用的两个视线辅助方法。保留注解、Mixin 或注册表加载的代码。

## 验证范围

ProjectAuditTest 覆盖不同组件不可合并、部分堆叠、自合并、空槽位置、数据副本、不可变开关、跨槽堆叠、重复效果阈值和替换配置清除旧条目。独立测试显式建立空 NeoForge 加载清单，再初始化原版注册表。菜单交互、跨维度爆破和实际保护插件仍需游戏内验证。

最终检查日志：build/desktop-run/audit-recheck.log。
数据生成、法杖数学和 GPU 测试日志：build/desktop-run/audit-final.log。
最终验证：check、projectAuditTest、staffSelectionTest、processResources 通过（audit-recheck.exit = 0）；GPU 测试通过，runData 完成全部生成器。首轮完整任务因独立测试缺少 NeoForge 加载清单而失败，补齐测试环境后重跑通过。git diff --check 通过。最终两个修改文件的 class 编译时间晚于源文件修改时间。

## 远程法杖作者

远程：https://github.com/Magic-team-jvav/BetterExperience

公开 HTTPS 查询 HEAD 得到 1ff72d5475fa46d56a2335ac4d684a881a9062f0，与本地 origin/HEAD 一致；据此核验远程提交历史。

原始实现作者是 **the_xu**。提交日期 **2025-03-06**，标题 **法爆魔杖，3996**：
https://github.com/Magic-team-jvav/BetterExperience/commit/ee145f52ee11b072ac5b89362015a336c3252b1d

该提交新增法爆魔杖的 MagicBoomStaff 实现。星爆魔杖由 the_xu 在 2025-03-09 的 147d523 提交中新增注册，复用同一类并设置不同范围：
https://github.com/Magic-team-jvav/BetterExperience/commit/147d523
远程后续修改该文件的提交作者也均标为 the_xu。这里只确认 Git 作者归属，不推断现实身份，也不将本地未提交的框选和 GPU 修改归入原始提交。
