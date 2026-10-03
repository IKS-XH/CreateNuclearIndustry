# EXT-A-REACTOR-01C：控制棒组件改为工作台制造

**状态：已完成。2026-10-03用户确认合并手测全部通过，已合入main。** 见[最终验收](../../reviews/2026-10-03/reactor-01/ACCEPTANCE.md)。以下保留本卡原实施合同、候选边界与验证安排的历史语境，当前状态以验收记录为准。

## 冻结合同

保留原用料与单件产量：1黄铜片＋1中子吸收陶瓷＋1钢杆→1现有`control_rod`组件。工作台有序配方竖排，从上到下为黄铜片、陶瓷、钢杆，即`["B","N","S"]`；B=`c:plates/brass`，N=`create_nuclear_industry:neutron_absorbing_ceramic`，S=`create_nuclear_industry:steel_rod`。采用原生工作台平移匹配，不新增加工耗时。

新路径`crafting/reactor/control_rod.json`；删除原`sequenced_assembly/control_rod.json`。保留`incomplete_control_rod`注册与素材供旧存档兼容，无新加工用途、不自动退料。仪表端口21格配方、驱动器动力合成配方、其他材料及设备行为不变。此调整不改变结构成本账。

## 写集与验证

候选`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`，基线`fa6c7648df999913d370997f6aebc82a9d3631be`。执行者仅可修改：

- 上述新增工作台配方与删除旧序列配方。
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionReactorCraftingGameTests.java`：在现有工作台用例加入此配方的真实匹配与单件结果；旧序列缺失断言覆盖五条，移除控制棒序列仍存在的过时断言/注释及无用导入。沿用原6项测试，不新增框架。
- `build/reports/extension/EXT-A-REACTOR-01C.md`与同名证据目录，记录实际技能应用、命令/结果、制品哈希及人工边界。

必读AGENTS、治理5.1、本卡，实际使用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`与`minecraft-testing/SKILL.md`。锁定Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82。中文注释；无Git写、核心docs写、再派发或用户客户端/存档操作。保留既有`tools/art-assets/__pycache__/`与主工程用户启动配置。

仅运行一次现有隔离组及增量打包：`./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_reactor_crafting -PgameTestDirectory=build/gametest-reactor-crafting-01c assemble --console=plain`。核对制品新工作台配方存在、旧序列不存在；不重复JUnit、冷却剂组/全量，不clean/rerun-tasks。只有实际失败或意外影响共享机制才扩大验证。既有机械配方证据复用01B，用户工作台/JEI验证合并到原待验清单。

PM执行一次独立差异/证据复审、更新现行文档并归档，保存候选；本批不合入main运行内容，也不宣称原两批手测已通过。
