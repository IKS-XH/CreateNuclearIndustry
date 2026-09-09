# P1-PONDER-01 注册 Ponder 入口交付报告

## 1. 任务与基线

- 任务 ID：`P1-PONDER-01`
- 交付状态：`DONE_WITH_CONCERNS`，等待项目经理验收
- 功能基线：`8246bfa 完成P1破坏性重组成型`
- 需求基线与本次开工提交：`6c2dbd8 冻结P1思索入口与总回归任务卡`
- 基线版本：Minecraft `1.21.1`、Java `21`、NeoForge `21.1.219`、Create `6.0.10-280`、Ponder `1.0.82`、Flywheel `1.0.6`
- 开工时既有未提交改动：无
- Git：未创建提交，未执行任何 Git 写操作

## 2. 实际使用的技能

- `C:\Users\lenovo\.codex\skills\minecraft-modding\SKILL.md`：核对 NeoForge 1.21.x 客户端生命周期、注册表和 Create/Ponder 接入边界；未采用技能中的 26.x 或其他版本示例。
- `C:\Users\lenovo\.codex\skills\minecraft-testing\SKILL.md`：按 JUnit 5 静态契约、NeoForge GameTest 和 Java 21 版本边界设计并执行验证。
- `C:\Users\lenovo\.codex\plugins\cache\openai-curated-remote\superpowers\6.3.0\skills\test-driven-development\SKILL.md`：先添加测试并确认因 `DIRECT_ENTRY_IDS` 缺失而失败，再进行最小生产改动并复验通过。

## 3. 实现摘要

现有 `P1PonderPlugin` 已经是唯一 P1 插件、唯一实验反应堆故事线和客户端初始化接线。本卡只将原有同一列表从语义含混的 `COVERED_COMPONENT_IDS` 重命名为 `DIRECT_ENTRY_IDS`，注册调用仍使用该同一列表，继续使用 `CreateNuclearIndustry.MOD_ID` 与 `P1ContentIds` 作为身份来源；没有新增插件、故事线、场景步骤或结构资源。

新增 `P1Ponder01ContractTest`，不以源码搜索冒充客户端真实打开验收，仅检查静态注册合同、正式 ID 归属、资源存在性和客户端注册点形态。实际入口打开、镜头、文本和资源运行时错误留给 `P1-PONDER-04`。

## 4. 入口矩阵

### 4.1 直接入口：精确 11 项

| ID | 所有者/类别 | 故事板 |
| :--- | :--- | :--- |
| `reactor_casing` | 本模组方块 | `create_nuclear_industry:experimental_reactor` |
| `reactor_window` | 本模组方块 | 同上 |
| `reactor_instrument_port` | 本模组方块 | 同上 |
| `reactor_cold_port` | 本模组方块 | 同上 |
| `reactor_hot_port` | 本模组方块 | 同上 |
| `reactor_refueling_port` | 本模组方块 | 同上 |
| `reactor_fuel_rod` | 本模组方块 | 同上 |
| `control_rod_drive` | 本模组方块 | 同上 |
| `fresh_fuel_assembly` | 本模组物品 | 同上 |
| `cooled_spent_fuel_assembly` | 本模组物品 | 同上 |
| `compound_coolant_bucket` | 本模组物品 | 同上 |

测试同时断言列表大小为 11、与手工冻结集合相等且无重复；每一项均命中 `P1ContentIds.FORMAL_IDS` 的 `MOD` 所有权。

### 4.2 间接展示：不得独立注册

`control_rod`、`steel_plate`、`compound_coolant`、`hot_compound_coolant` 和外部 `create:goggles` 不在直接入口列表中，继续由反应堆、驱动器、端口或桶的故事线间接说明。

### 4.3 明确排除

测试断言以下对象不在直接入口集合中：旧样例 `experimental_reactor_casing`、未注册 `main_coolant_pump`、`pressure_pipe_tier_1`、`pressure_valve_tier_1`、`reactor_control_port`、锅炉/汽轮机示例、`dosimeter`、`shielded_hot_cell`、事故效果关键词以及 `coolant_purifier`、`contaminated_compound_coolant`、`reactor_interlock`、`scram_interlock`。P0 探针、全部其他 P2/P3/暂缓内容也未进入该列表；没有注册事故产物、火灾、爆炸、污染、净化器或独立联锁器假入口。

## 5. 现有预备实现审计

- `P1PonderPlugin` 在基线中已存在，且只有一处 `PonderIndex.addPlugin(new P1PonderPlugin())`。
- `CreateNuclearIndustry` 通过 `FMLClientSetupEvent` 监听器在客户端初始化阶段接入该插件；服务端没有新增 Ponder 状态入口。
- `registerScenes` 只把直接入口列表绑定到 `create_nuclear_industry:experimental_reactor`，没有第二套插件或故事线。
- `P1PonderScenes.experimentalReactorBasics` 与 `assets/create_nuclear_industry/ponder/experimental_reactor.nbt` 均保持不变；本卡没有把预备场景标记为 P1-PONDER-02 完成。

## 6. 修改范围

本次允许文件内修改：

1. `src/main/java/com/iksxh/create_nuclear_industry/ponder/P1PonderPlugin.java`
2. `src/test/java/com/iksxh/create_nuclear_industry/P1Ponder01ContractTest.java`
3. `build/reports/p1/P1-PONDER-01.md`

未修改 `P1PonderScenes`、Ponder NBT、语言、玩法生产注册 ID、配方、GameTest、Gradle、模拟器和核心文档。

验证过程中 NeoForge/JUnit 更新了 `logs/debug.log` 与 `logs/latest.log`；这两个文件不属于本任务修改，按项目经理要求保留并在越界审计中报告，未纳入实现意图。

## 7. 测试与命令结果

| 命令 | 精确结果 |
| :--- | :--- |
| `./gradlew.bat test --tests com.iksxh.create_nuclear_industry.P1Ponder01ContractTest --rerun-tasks` | `6` 项通过，`0` 失败、`0` 错误、`0` 跳过，`BUILD SUCCESSFUL` |
| `./gradlew.bat test --rerun-tasks` | `50` 个测试结果文件、`251` 项通过，`0` 失败、`0` 错误、`0` 跳过，`BUILD SUCCESSFUL` |
| `./gradlew.bat runGameTestServer --rerun-tasks --max-workers=1` | `106 GAME TESTS COMPLETE`；`All 106 required tests passed :)`。随后停在已知 `Saving worlds` 阶段，记录成功行后手动结束已完成进程；没有 GameTest 断言失败。 |
| `./gradlew.bat build --rerun-tasks` | `BUILD SUCCESSFUL` |
| 任务范围 `git diff --check` | 通过；仅有 Git 的 LF/CRLF 提示，无尾随空白错误 |
| 完整 `git diff --check` | 仅报告测试生成的 `logs/debug.log`、`logs/latest.log` 第 7 行尾随空白；未发现本任务允许文件的错误 |

全量测试出现的现有编译/运行警告（NeoForge 已弃用 API、Ponder refmap、缺少 JetBrains 注解类、Create 版本检查提示）不是本卡改动引入，也未修改框架规避。

## 8. 验收边界

- 本卡未进入客户端逐项人工打开 Ponder；任务卡明确将真实可打开性、文本本地化、镜头和资源加载错误留给 `P1-PONDER-04`。
- 本卡只验证静态入口合同和现有资源存在，不宣称已经完成客户端实际打开验收。

## 9. 兼容性与注释检查

- 存档/NBT：无改动。
- 注册 ID：无新增或重命名；仅重命名 Java 列表字段，不改变注册表路径。
- 网络/服务端：无改动；Ponder 仍是客户端初始化接线，教学层不拥有服务端状态。
- 场景/资源：无新增或修改。
- 注释：新增测试注释和 Javadoc 均为中文；生产文件仅修改 Java 标识符及其引用，既有中文说明仍与行为一致。

## 10. 已知缺陷与未实现项

- 完整 `git diff --check` 受测试生成日志的尾随空白影响；日志属于越界生成文件，未清理，需项目经理决定是否在整合前处理。
- 本卡不实现 P1-PONDER-02/03 的教学步骤，不实现 P1-PONDER-04 的客户端人工验收。
- Ponder 场景中的文本、镜头、真实入口可打开性和资源加载仍需后续客户端验收。

## 11. 建议下一任务

由项目经理验收本报告和允许文件差异后，再派发 `P1-PONDER-02`；该任务只扩写已确认反应堆基础教学步骤，并继续保持 Ponder 不读取或修改服务端权威状态。

## 12. 项目经理复验与归档补记

项目经理于 `2026-09-09` 审查允许写集、实现差异和报告证据，确认执行者没有修改核心文档或执行 Git 写操作。本卡最终状态为 **已完成**：11 项直接入口、间接展示项与明确排除项符合冻结合同，`P1PonderPlugin` 仍是唯一插件，`experimental_reactor` 仍是唯一故事板；本卡没有提前完成 `P1-PONDER-02/03/04`，也没有修改任何服务端权威状态。

项目经理独立复验结果：定向契约测试 6/6 通过；全量 50 个测试结果文件、251/251 项 JUnit 通过，失败、错误和跳过均为 0；GameTest 完整重跑显示 `106 GAME TESTS COMPLETE` 与 `All 106 required tests passed :)`；`build --rerun-tasks` 通过。GameTest 首次复验在第二批测试中发生 NeoForge/Minecraft 测试框架内部 `Object2LongOpenHashMap` 空引用，没有项目测试断言失败，但该次运行未完成；立即重跑 106/106 通过，并在成功后停于已知 `Saving worlds`，由项目经理终止。若该框架瞬态故障在 `P1-VERIFY-01` 再次出现，应继续与项目断言失败分开记录。

执行者报告中的“建议下一任务”不构成任务派发或项目经理决策。当前路线图建议下一张由用户手动派发已经冻结的 `P1-VERIFY-01`；`P1-PONDER-02/03/04` 仍按活动计划后续推进。
