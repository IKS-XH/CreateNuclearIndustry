# EXT-A-MATERIAL-03A：完整 GameTest 回归异常只读审计

审计日期：2026-10-01。工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支基线按派发为 `feb2cb3`。本次只读检查，唯一写入本报告和同名目录；未运行 Gradle/游戏，未改实现、测试、构建脚本、正式文档或 Git。当前工作树另有 `en_us.json`、`zh_cn.json` 语言文件改动，属名称修改任务；本审计未触碰。

## 结论

锁定源码直接证实：`GameTestInfo.tickInternal()` 遍历该测试自己的 `runAtTickTimeMap` 时执行到期回调，之后才由同一 iterator 删除刚执行的条目；回调中的 `GameTestHelper.runAfterDelay()` 会向同一张 map `put` 新条目。仓库也存在多个旧测试在延迟回调内再次调用 `runAfterDelay`。这种回调重入与 fastutil 8.5.12 的迭代器内部状态及本次 `wrapped == null` NPE 构成**一致的候选机制**；静态证据尚未证明该写入导致两次崩溃，具体触发与因果仍待隔离复现。

**已定位到调度器的结构性冲突机制；尚未从现存日志定位到 `defaultBatch:2` 中具体执行嵌套调度的测试方法。** 错误栈不含 Runnable 调用方，日志也没有正在 tick 的 GameTest 名称。故不能把某个旧方法写成已证实的唯一根因，也不能据此豁免 155 项全量回归。

## 证据链

1. `build/reports/extension/EXT-A-MATERIAL-03/green-gametest.log:275–284` 与 `green-retry-gametest.log:275–283` 均在启动 `defaultBatch:2`（50 tests）后，下一秒内以相同栈崩溃：fastutil 8.5.12 `Object2LongOpenHashMap$MapIterator.nextEntry` → `EntryIterator.next` → Minecraft `GameTestInfo.tickInternal:130`。两次均无全量完成记录，Gradle 退出码为 1。失败发生在测试运行器的 tick 路径，日志没有列出触发 NPE 的 GameTest 名称。
2. 当前缓存的锁定 NeoForge/Minecraft 映射源码 `C:/Users/IKSXH/.gradle/caches/neoformruntime/intermediate_results/sourcesAndCompiledWithNeoForge_4a83a73d95fdbeba9a47b7d6bbaabb6346ecca49_output.jar` 中，`net/minecraft/gametest/framework/GameTestInfo.java:120–140` 显示 `tickInternal` 先取得 map iterator（127），执行 `entry.getKey().run()`（133），最后执行 `objectiterator.remove()`（138）。`GameTestInfo.java:169–170` 的 `setRunAtTickTime` 对同一 map 调用 `put`。这意味着回调运行期间若再安排延时任务，写入会发生在活动 iterator 的遍历中。
3. 同一锁定源码中，`GameTestHelper.java:808–809` 的 `runAfterDelay` 通过 `runAtTickTime` 调用 `testInfo.setRunAtTickTime`，确实写入上述 map；并不是一个独立安全队列。
4. 与运行时 fastutil 完全匹配的 8.5.12 sources JAR 中，`Object2LongOpenHashMap.java:687–690` 说明 `wrapped` 是惰性创建；`708–714` 在 iterator 游标小于零时无条件调用 `wrapped.get(...)`；`765–768` 仅在删除时检测到槽位 wrap 才创建 `wrapped`。NPE 表明 iterator 到达了 wrapped-entry 路径，但它没有 wrapped 列表。活跃迭代期间通过回调插入并随后 `iterator.remove()`，可能使 fastutil 迭代器依赖的槽位/游标关系不再满足原先假设；异常在下一次 `next()` 抛出，与候选机制相符。该静态对应关系尚未隔离复现，不能据此认定这是本次两次 NPE 的原因。现有栈显示异常发生于业务回调之后的 map 迭代路径，不是业务断言失败或物品状态异常的证据。
5. 新增 `ExtensionSteelProcessingGameTests` 的所有调度在测试入口同步注册；延时 lambda 与其辅助调用未在回调内再注册 GameTest 延时任务。新类没有此类重入证据。不过它与旧测试共享同一个框架，所以其14项普通服务器逐条通过并不能排除旧回调触发的全量运行器故障。

## 旧测试可疑点（候选，不等于批次归因）

- 首个最小候选：`src/main/java/com/iksxh/create_nuclear_industry/gametest/P1ControlGameTests.java:287–298`，方法 `redstoneOnControlRodDriveDoesNotTriggerScram` 在 tick 5 的回调内再次 `runAfterDelay(2, ...)`。这是单个且直接的 map 重入路径，适合作为第一项隔离复现。
- 另有独立路径：`P1StructureGameTests.structureFormsOnLoadAndBuildsColumnCache`（约 58–91 行）存在两层嵌套延时安排；`structureInvalidatesOnBreakRestoresOnPlacementAndPreservesState`（约 101–146 行）也在两个延时回调中继续调度。
- `P1Refuel02aGameTests` 与 `P1Refuel03GameTests` 各有一个延时回调继续安排延时任务（分别约 143–151、135–150 行）。

方法注册会在各测试类内按方法名排序（锁定 `GameTestRegistry.java:29–33`），但当前崩溃材料没有注册序列/活动测试 ID，`defaultBatch:2` 的名称本身不能证明上述任一候选属于当时触发回调的那个 `GameTestInfo`。报错栈在 map iterator 上，也没有 lambda 或测试方法帧。因此审计不把候选等同于根因归属。

## 最小后续验证

不重跑整批。由 PM 在隔离的 GameTest 环境安排一次**只含单一测试方法**的诊断运行，首选 `P1ControlGameTests.redstoneOnControlRodDriveDoesNotTriggerScram`：记录测试名、进入 tick 5 回调、内层调度返回以及是否出现相同 NPE。若该方法单测稳定复现，即确认旧测试回调是可重现触发条件；随后只需对结构/装料两个家族中其余嵌套回调做同样的单项筛查，判断是否还有多处合同问题。若单测不复现，则不能推翻并发修改机制，下一步应在**仅诊断运行器/临时测试夹具**中给每个测试回调标记 `testName` 并记录该 map 的迭代/写入事件，复现 `defaultBatch:2` 后锁定确切写入者，再决定是否需要精确整改。

目前没有证据要求修改生产实现或配方。若 PM 需要将上述旧测试改为非重入调度，精确候选范围是 `P1ControlGameTests.redstoneOnControlRodDriveDoesNotTriggerScram` 及 `P1StructureGameTests` 中列出的两个方法；由 PM 另派具体写集和验收，不在本只读任务内改动。

## 实际使用技能与版本

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：按仓库 NeoForge/Minecraft 实际版本核对 API 与运行时源码，未套用技能中较新版本示例。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：区分 GameTest 框架 tick 故障与业务断言，按真实世界测试边界评价现有证据。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/systematic-debugging/SKILL.md`：从完整异常栈回溯到调度表读写点，区分已证实机制与未证实触发方法。
- 基线仍为 Minecraft 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280；本次未更改技术栈。
