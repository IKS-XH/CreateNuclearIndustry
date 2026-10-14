# 换料端口枯竭组件快捷替换实施计划

> 执行方式：主PM按已授权自动派发，由一位执行者完成 Task 1，一位独立审查者合并规格与质量审查。应用 subagent-driven-development；执行者不得执行 Git 写操作、修改核心文档或再派发子代理。

**Goal：** 手持新燃料组件右击成型换料端口，一次取出已枯竭组件并装入一件新燃料；仍有耐久的组件不能直接替换。

**Architecture：** 在现有 FuelRefuelingTransaction 和 ReactorPortBlockEntity 的读阶段、提交前校验、双状态回滚边界中增加替换方向；玩家入口提交成功后才转移手中物品并返还旧组件。原空槽装入、空手取出及机械臂取放保持。

**Tech Stack：** Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82。

**Spec：** 用户2026-10-12本次请求；现行[局部控制合同](../../../reactor-local-control-revision-design.md)的单槽与服务端事务规则。本计划下列需求为本次新增规则。

## 已确定需求与全局约束

- 只接受正式 `fresh_fuel_assembly` 且仍有可用耐久，沿用原装料入口可保留输入已有耐久及全部数据组件的规则；只消费一件，不额外要求全新零损伤。
- 目标必须已耗尽（剩余耐久为零），包括已转成 `cooled_spent_fuel_assembly` 的正式枯竭产物。剩余一单位耐久也不允许替换，即使控制棒已完全插入、列不放热。
- 遵循现有成型绑定、服务端校验和目标列停止裂变条件。不清除余热、列完整度、其他列、SCRAM、冷热库存或尺寸；没有相邻控制棒时，新组件仍按既有规则开始放热，不新增停机/启动模式。
- 替换必须一次成功或完全失败：禁止先公开取空再装入；失败不消费输入、不返还旧组件、不变更端口物品或权威快照。保留精确旧物品的全部数据；旧耗尽新燃料形态按照现有提取规则生成冷却乏燃料。
- 玩家返还：消费一件后交互手为空则把旧组件交回该手，否则放入背包，背包满则只掉落一件在玩家身边。创造模式沿用现有装料消耗语义，不额外复制新旧组件；主手、副手均按实际交互手执行。
- 空槽、错误物品、未绑定结构、空手取出、钢板维修仍保持现有行为；Create机械臂 `insert/extract/simulate` 不因此自动替换。无GUI，无通用物品管道入口。
- 不新增配置、配方、美术、思索页、旧版本迁移或无关重构。所有手写代码/Javadoc新增修改部分用中文。

## 工作树、技能与权限

- 唯一实现树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，分支 `codex/reactor-variable-size`，基线 `31554094f6b8c2ae3726d4c60fd83c0872eb80a7`。复用已携带存档的现有隔离树，不新建分支、不读写/复制用户世界；可变尺寸、ART08/09各自人工门保持。
- root已按 using-git-worktrees 核实 linked worktree；既有候选证据复用，不重复开工全量测试。
- 执行者实际阅读根AGENTS、本卡及 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/test-driven-development/SKILL.md`。技术栈以本卡锁定值为准。
- 只允许三个生产Java：`block/ReactorPortBlock.java`、`blockentity/ReactorPortBlockEntity.java`、`reactor/FuelRefuelingTransaction.java`；原 `reactor/FuelRefuelingTransactionTest.java`；新增 `gametest/RefuelingQuickSwapGameTests.java`（独立域模板 `src/main/resources/data/create_nuclear_industry_quick_refuel/structure/p0_probe_empty.nbt` 逐字复制正式原空模板，不改原模板或构建入口）；zh_cn/en_us仅本次换料消息键。
- Java路径以 `src/main/java/com/iksxh/create_nuclear_industry/` 为根，测试以 `src/test/java/com/iksxh/create_nuclear_industry/` 为根；语言在 `src/main/resources/assets/create_nuclear_industry/lang/`。
- 执行者报告仅 `docs/reviews/2026-10-12/refueling-quick-swap-01/IMPLEMENTATION.md` 与本计划 `.superpowers/sdd/refueling-quick-swap-01/task-1-report.md`，日志/冻结身份写 `build/reports/refueling-quick-swap-01/`。审查者仅同目录 `REVIEW.md`。其他docs、Git、世界、PNG/OBJ/SVG、共享控件、生命周期/热工修改禁止，必要写集外改动先报告PM。

## Review Focus

1. 枯竭与仅剩一单位耐久不能混淆，停止放热不等于枯竭。
2. 精确组件和快照投影不一致、失效绑定、当前裂变不得导致半提交。
3. 副手、多个输入、满背包返还不能吞掉/复制旧组件。
4. 替换保留列余热/损坏与其他状态，不能因为with操作抹除尺寸或SCRAM。
5. 原机械臂仍只能向空槽装入，失败模拟不能转移物品。

### Task 1：原子替换及玩家右击

**接口：** 纯事务新增 `replaceExhausted(FuelColumnState current, ItemStack incoming, double currentFissionHeatHu, ItemStack exactStoredFuel)`，返回原 Result 合同；新增替换成功/目标未枯竭状态并正确扩展 success。端口新增 `tryReplaceExhaustedFuel(ItemStack incoming)`，复用原提交前二次校验及 rollback。玩家仅在手持正式新组件且目标已有组件时走替换；不得改变机械臂的现有接口含义。

- [x] RED：以现有玩家右击入口写独立域真实场景，实际期待枯竭产物返回并装入新燃料；先在现有生产代码上运行，确认因仍拒绝已占用槽而失败，不以缺方法编译错误作为行为RED。
- [x] GREEN：添加纯事务、端口原子提交及玩家返回路由；新增本地化“已替换枯竭燃料组件”“燃料尚未耗尽，无法直接替换”（英文同步），不硬编码玩家提示。
- [x] 纯事务定向覆盖：枯竭成功且只消耗一件；剩余一耐久拒绝；错物品/耗尽输入/裂变中拒绝；旧组件自定义数据保持；列热/完整度保持且失败输入不变。机械臂原insert/extract断言保留。
- [x] 独立真实GameTest覆盖：主/副手实际入口；一件输入回手、多件输入入包/满包掉落守恒；未耗尽拒绝、空槽仍可装入与未成型拒绝；使用非5尺寸结构验证坐标及尺寸/余热/其他列不变。可合并有限场景，禁止复制大量完整夹具。
- [x] 最终必要检查：`./gradlew.bat test --tests '*FuelRefuelingTransactionTest'`，新增独立域 `./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_quick_refuel -PgameTestDirectory=build/gametest-refueling-quick-swap-01`，最后唯一增量 `assemble`。执行者独占Gradle，本批不完整build、不clean、不强制重跑、不跑无关GameTest。
- [x] 写报告：实际技能/变更、RED/GREEN原日志与退出码、JUnit与真实用例数、源及class/语言/JAR身份、待人工范围。已知保存退出停滞若出现，区分业务断言与命令退出，不把前者冒充全程成功；不重复已通过业务。
- [x] 独立审查：同一diff一次合并规格/质量，读原证据不重跑。整改交原执行者，仅受影响证据复验。
- [ ] PM内容核对后提交候选，手测前不合main；保留所有无关改动和历史制品。用户确认本功能后才能关闭它的人工门，不顺带关闭尺寸/美术。

## PM接续

2026-10-12：直接落实用户指定功能；返还路线与可用新燃料耐久沿用现有交互语义，是实现默认值，不新增玩法选择。本批不新开分支，不触碰已经携带的存档，ART09美术树无三个生产Java写集交叉。

2026-10-12执行核实：锁定NeoForge按模板namespace筛选；首次exit0但零测试不算RED/GREEN。允许新增上述单个独立域空模板逐字副本，原模板SHA保持，无构建/功能扩范围。

2026-10-12收尾发现实际边界：原版Inventory.add在创造模式满包会清零未接收输出。PM批准增加此真实RED并在原玩家返还写集整改，复验仅真实GameTest；原12/12事务与ART09证据复用。首次组合assemble降为中间证据并备份，按治理5.1实际故障触发条件允许修复后的必要增量assemble作为最终冻结；保留原日志，不用有吞物品疑点的制品交人工。

2026-10-12候选交付：功能提交d811f841c89331ac19564ab80d94a331706ecaf7；ART09净接入26f4ba25ff71085009bf799ded418e9b796c141d。最终12/12事务、1/1真实玩家有限场景、必要增量assemble退出0，独立审查无必改项，源/class/资源/JAR身份已核。PM已提交候选，最后用户手测/合main步骤仍未完成，各项人工门独立保持；证据和启动见本批CANDIDATE。
