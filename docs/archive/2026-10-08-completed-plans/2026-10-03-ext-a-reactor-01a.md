# EXT-A-REACTOR-01A：实验堆制造配方调整

**状态：已完成。2026-10-03用户确认合并手测全部通过，已合入main。** 见[最终验收](../../reviews/2026-10-03/reactor-01/ACCEPTANCE.md)。以下保留本卡原实施合同、候选边界与验证安排的历史语境，当前状态以验收记录为准。

**后续取代：** 仪表端口三格配方按用户“与离心机同级”的要求提升为[01B的21格方案](2026-10-03-ext-a-reactor-01b.md)。本卡保留01A当时合同和验证范围，其它修改继续有效。

## 1. 01A当时冻结行为

| 新配方路径（`recipe/`下） | 输入与输出 | 参数/布局 |
| :--- | :--- | :--- |
| `mixing/shielded_glass.json` | 1 `c:ingots/lead`＋1原版无色玻璃块→1现有铅屏蔽玻璃材料 | 普通加热`heated`，`processing_time=100`，无副产物 |
| `crafting/reactor/reactor_fuel_rod.json` | 原2钢板＋1钢格架→1现有燃料柱结构块 | 原竖列`S/G/S`不变，仅产量3改1；不含实际燃料 |
| `mechanical_crafting/reactor_instrument_port.json` | 1外壳＋1工业传感器＋1电子管→1仪表端口 | 横排`CST`，依次外壳/传感器/电子管，3台动力合成器 |
| `mechanical_crafting/reactor_refueling_port.json` | 1完整`create:deployer`＋1工业传感器＋1外壳＋1密封环→1换料端口 | 2×2 `DS/CR`，D机械手/S传感器/C外壳/R密封环 |
| `mechanical_crafting/control_rod_drive.json` | 1机械活塞＋1活塞杆＋1控制棒组件＋1精密构件→1驱动器 | 2×2 `PE/CM`，P活塞/E活塞杆/C控制棒/M精密构件 |

三条机械配方均使用原生`create:mechanical_crafting`、`accept_mirrored=false`，只在动力合成器生效，原用料各1不变，无额外钢板或放大成本；该明确指令取代这三种方块原来的序列装配。控制棒组件本身仍为原序列装配。产物身份、旧设备状态、NBT、运行、GUI、纹理均不变。

删除对应四条旧`sequenced_assembly/`配方：`shielded_glass`、`reactor_instrument_port`、`reactor_refueling_port`、`control_rod_drive`。保留其已有半成品注册、名称和素材以免旧候选存档丢失身份，但旧半成品不再获得加工路线；不自动回收、转换或退款。新制造总数仍16条，不留下JEI双路线或JAR旧配方残留。

## 2. 写集与执行

工作树`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`，实现基线`c83ca0a4c8c72340b1ed58e2c39b8645087b0654`。已有未跟踪`tools/art-assets/__pycache__/`不动；主工程用户`.vscode/launch.json`不动。

执行者唯一允许修改：

1. `src/main/resources/data/create_nuclear_industry/recipe/`下第1节五条现行配方及四条被替代配方，共9个精确文件。
2. `src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionReactorCraftingGameTests.java`：更新被替代的假设，保留未受影响的断言；不得把旧路线仍通过当成新路线覆盖。
3. `build/reports/extension/EXT-A-REACTOR-01A.md`及同名证据目录。报告包含变更、验证命令/实际结果、原始日志、JAR哈希、旧配方移除检查和人工边界。

执行者必读AGENTS、治理5.1、本卡，实际使用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`与`minecraft-testing/SKILL.md`。实际技术栈Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82；不升级。手写注释中文。执行者不得修改Git、核心docs、构建、注册、素材、运行逻辑、用户存档，不再派发子任务。PM独占文档、审查及Git。

## 3. 精简验证与手测

- 一次现有隔离组：`./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_reactor_crafting -PgameTestDirectory=build/gametest-reactor-crafting-01a assemble --console=plain`。不`clean`、不`rerun-tasks`，不跑全量或旧01B组，无关JUnit复用。
- 更新工作台产量断言为1；加载态检查三个机械配方的尺寸、按序配料、不可镜像及产物各1，普通工作台输入不匹配它们；四旧配方不可加载。
- 新铅玻璃代表实际Mixer验证：无热不消耗，普通加热后精确消耗1铅锭和1玻璃并产1件；错用铅板不应匹配。同一普通加热Mixer代表用例由工业陶瓷改测铅玻璃，陶瓷配方未变，其原实机证据复用，现行数据断言保留；不重复同机制实机样例。
- 三种动力合成使用加载后的原生配方对象检查尺寸、配料与结果，正向专用输入匹配、错位拒绝、实际设备连接/消耗/出料由客户端手测覆盖，不将配料断言称为完整匹配或真实机器加工。执行者报告新物理探针涉及Create的`ConnectedInput`/`GroupedItems`链，尚未形成可用布局；PM按用户精简测试要求和治理5.1停止扩建探针。PM随后核实`GroupedItems`公开构造/merge/read可用于正向matcher测试，但当时定向组已完成，不为扩大这次验证范围重跑；不是技术不可行，也不伪造自动实机证据。
- 静态及最终JAR检查五条现行配方、四条旧配方不存在；同一资源检查复用，不生成镜像JSON单元测试。只有实际失败才重跑对应部分。
- 一次独立复审读取差异和已有证据，不再执行测试。手测仍并入原清单：加热铅玻璃、工作台燃料柱1件、三个机械配方/JEI与旧路线消失。当前用户没有宣称原两批完整手测通过。

## 4. PM同步

同步`docs/recipes.md`、原01任务卡当前配方/成本、内容索引、路线图、文档入口及合并手测清单；原自动报告保持历史语境。Ponder结构本体仍用24柱块：钢板由76直接增为108，钢格架由8增为24；连同格架上游48板和杆1锭，共157钢锭当量（不含Create零件）。铅玻璃用3铅锭代替3铅板，外壳用50铅板不变，铅锭总量当量不变。

完成候选审查后保存进度，等待用户人工验收；本卡不继续热端或其它新任务。
