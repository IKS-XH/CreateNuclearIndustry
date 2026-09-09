# P1-MAINT-03：停机破坏性重组成型复验报告

## 执行边界

- 执行基线：`f7d99c7`。
- 本次为首轮验收意见的增量整改，没有重新实现任务。
- 已阅读根目录 `AGENTS.md`、活动计划完整 P1-MAINT-03 任务卡，以及第 913 行起的四项首轮验收意见和第 920 行复验门。
- 实际使用并应用：`minecraft-modding`、`minecraft-testing`。
- 技术基线保持为 Minecraft `1.21.1`、Java `21`、NeoForge `21.1.219`、Create `6.0.10-280`、Ponder `1.0.82`；未按技能示例升级技术栈。
- 未执行任何 Git 写操作；未修改活动计划、治理文档、Gradle、公式、NBT 版本、注册 ID、流体 capability、Ponder 或模拟器。

## 四项首轮意见整改证据

### 1. 端口权威投影先于停机分类

`prepareDisassemblyPlan` 现在先按当前有效结构映射枚举并校验全部换料端口，捕获每个端口的实体、列坐标和完整 `ItemStack`，再用 `FuelAssemblyItemCodec.simulationState` 创建无副作用燃料投影。危险判定、完全停机判定、计划提交前复验均使用同一投影；端口缺失、绑定变化、物品变化或非法物品均拒绝。

新增 required GameTest `staleSnapshotUsesCurrentFuelPortProjection` 使用真实 `NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent)`：仪表快照故意为空/陈旧，但源换料端口保留可用燃料并让相邻控制棒未插入。断言事件放行、融毁占位提交、精确燃料投影恢复且端口物品不变，覆盖了旧实现会误判为完全停机清空的回归。

### 2. 可观察事务回滚、维护例外和迁移信封

- `resetRollbackRestoresMigrationAndTelemetry` 注入旧 `LegacyFuelMigration` 信封，先完成受控清空提交，再篡改一个已清空端口制造提交后校验失败；断言校验失败后精确恢复权威快照、端口物品及其数据组件、提交前遥测和迁移信封。
- 同一测试随后执行成功真实 BreakEvent，断言完全清空成功且旧迁移信封不再出现，覆盖“成功重置不能复活旧信封”。
- `instrumentMaintenancePreservesMultipleFuelPorts` 使用两个燃料列和两个带自定义名称/耐久的端口物品，验证完全停机仪表维护例外的真实 BreakEvent 放行，以及仪表实际移除—补回—结构重扫后多个端口物品身份、耐久和组件均保持不变。
- 危险、完全清空和仪表维护计划均进入统一提交/后置校验/回滚路径；回滚恢复端口、快照、迁移映射和遥测副本，不发布额外融毁事件。

### 3. 真实移除—重扫—补回的精确控制棒断言

`realBreakRemoveRescanAndReformStartsEmpty` 在真实移除外壳、结构失效、补回外壳并正式重扫后，同时断言：

- 重扫所得控制棒列数量等于结构扫描控制棒列数量；
- 两边坐标集合完全相等；
- 每一列均为 `ControlRodColumnState.fullyInserted()`。

完全停机夹具保留一个不影响源燃料四向邻接的控制棒列 `actualDepth != 1`，并将原错误说明改为准确中文：源燃料四个相邻控制棒完全插入，所以正式裂变产热和计划燃耗为零；非零量化余热仅用于验证清空字段完整性。

### 4. 危险兼容入口返回值与去重语义

`tryCommitDangerousDisassembly` 现在只有在本次调用实际完成危险状态提交并成功发布事件时返回 `true`。首次调用返回 `true`；重复调用、已达到倒计时上限但未发布、已发布状态均返回 `false`，且不重复发布。提交失败、后置校验失败或发布失败均调用同一回滚路径。

新增 `dangerousCompatibilityEntryReportsOnlyFirstPublish` 覆盖首次、重复、已完成未发布和已发布四种返回值，并断言事件计数始终只有一次。既有真实 BreakEvent 多组件和倒计时 GameTest 继续覆盖生命周期入口及事件去重。

## 复验门（活动计划第 920 行）

| 命令 | 结果 |
| --- | --- |
| `./gradlew.bat test --rerun-tasks` | 通过；49 个测试文件，245 项 JUnit，失败 0，错误 0，跳过 0 |
| `./gradlew.bat runGameTestServer --rerun-tasks --max-workers=1` | 通过；`All 106 required tests passed :)` |
| `./gradlew.bat build --rerun-tasks` | 通过 |
| `git diff --check` | 通过 |

保留并复验了七类非仪表组件统一清空、双所有者全有或全无预检、真实移除—重扫—补回闭环和既有危险八组件覆盖。GameTest 新增行为断言没有以生产源码字符串搜索代替。

## 允许写集与副作用核对

本次交付仅涉及任务允许的生产代码、对应 required GameTest 夹具/行为测试、定向 JUnit 契约测试、既有语言资源和本报告：

- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorDisassemblyPlan.java`
- `src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorStructureLifecycle.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/P1Maint03GameTests.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/P1Maint01GameTests.java`（端口权威夹具对齐）
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/P1StructureGameTests.java`（危险夹具端口对齐）
- `src/test/java/com/iksxh/create_nuclear_industry/P1Maint03ContractTest.java`
- `src/main/resources/assets/create_nuclear_industry/lang/en_us.json`
- `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`

Gradle/GameTest 运行期间生成的根目录日志已按 `f7d99c7` 对象内容恢复；`logs/debug.log` 和 `logs/latest.log` 当前内容哈希均为 `1e07e338476873623f22376ca172cce02faa83f2`，与基线一致。没有执行 `git add`、`commit`、`reset`、`checkout`、`restore`、建分支、合并、变基、stash、标签、推送或其他 Git 写操作。

## 项目经理复验与归档补记

项目经理于 `2026-09-09` 按活动任务卡第 920 行复验门审查并通过本次整改。下表补齐归档时的最终决策口径：

| 当前状态 | BreakEvent | 状态写入 | 危险事件 |
| --- | --- | --- | --- |
| 端口、绑定、世界身份、配置或判定无效 | 取消 | 无 | 无 |
| 新生热大于 epsilon，或融毁倒计时已启动 | 放行 | 首次时提交完成态；不清空 | 按既有入口最多一次 |
| 无危险，但计划燃耗或活动余热未达停机条件 | 取消 | 无 | 无 |
| 四项完全停机，破坏本结构仪表端口 | 放行 | 只同步当前燃料投影 | 无；所有原位端口物品保留 |
| 四项完全停机，破坏其余七类组件之一 | 放行 | 原子清空 | 无 |

七类破坏性重置入口统一覆盖 `reactor_casing`、`reactor_window`、`reactor_cold_port`、`reactor_hot_port`、`reactor_refueling_port`、`reactor_fuel_rod` 和 `control_rod_drive`。重置清空全部换料端口物品及数据组件、燃料与控制棒列状态、冷热冷却剂、SCRAM、融毁状态、旧燃料迁移信封和运行时遥测；不额外生成掉落、库存返还或世界事故效果。

事务顺序固定为：收集所有有效所有者并只读预检 → 任一拒绝则整次取消 → 全部计划提交 → 全部后置校验 → 发布需要的危险占位事件 → 安排既有结构重扫。提交或校验失败时恢复本次计划捕获的精确快照、端口物品、迁移信封和遥测。项目经理独立结果为 49 个测试文件、245/245 项 JUnit、106/106 个 required GameTest 与完整构建通过；`git diff --check` 在恢复运行日志后通过。
