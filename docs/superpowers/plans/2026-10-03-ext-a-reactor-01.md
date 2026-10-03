# EXT-A-REACTOR-01：固定实验堆的生存制造

**状态：已完成。2026-10-03用户确认合并手测全部通过，已合入main。** 见[最终验收](../../reviews/2026-10-03/reactor-01/ACCEPTANCE.md)。以下保留本卡原实施合同、候选边界与验证安排的历史语境，当前状态以验收记录为准。

**Goal：** 使用已有材料和Create工序制造固定实验堆的全部正式组成块，补齐最小前置材料。

**Architecture：** 新增独立`ReactorCraftingContent`注册普通材料、一个普通混凝土块和Create原生序列半成品；16条原生配方得到现有P1身份。反应堆运行、状态、燃料、GUI、管网接口均不变。管道沿用当前支持的Create原生管道/泵。

**Tech Stack：** Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；不升级、不改许可证。

**Spec：** 本卡第1节是此次委托确定的完整配方合同；延续`docs/recipes.md`第5.4、6、12节材料关系。只补生存制造，不实现换热器、蒸汽机组、专用耐压管、封存、辐射效果或动画。

## 1. 推荐值与玩家流程

用户随后明确三种端口/驱动器使用动力合成，具体紧凑布局见01A卡；其余外壳、窗口、冷热端和燃料柱仍使用工作台。控制棒组件按01C改工作台竖排合成，至此本批原五条序列路线全部停用；搅拌`processing_time=100`，不是固定5秒。

| 配方ID末段 / 输出 | 输入与数量 | 工序与参数 |
| :--- | :--- | :--- |
| `stonecutting/steel_rod` →2钢杆 | 1 `c:ingots/steel` | 切石；机械锯沿Create原生设置 |
| `pressing/seal_ring` →2密封环 | 1 `c:plates/steel` | 原生压片 |
| `deploying/pressure_fitting` →1耐压接头 | 1 `c:plates/steel`基底＋1锡合金焊料 | 原生机械手加工 |
| `mixing/industrial_ceramic` →2工业陶瓷 | 1 `c:gems/quartz`＋1黏土球 | 最低普通加热，100 |
| `mixing/neutron_absorbing_ceramic` →1中子吸收陶瓷 | 1工业陶瓷＋1 `c:nuggets/lead`＋1 `c:dusts/redstone` | 最低普通加热，100 |
| `mixing/shielded_glass` →1铅屏蔽玻璃材料 | 1原版无色玻璃块＋1 `c:ingots/lead` | 普通加热搅拌100，无副产物；材料物品 |
| `mixing/shielding_concrete` →1屏蔽混凝土方块 | 1任意颜色已硬化原版混凝土＋1 `c:nuggets/lead` | 无热，100；混凝土粉末不接受；屏蔽尾矿替代路线后置 |
| `crafting/reactor/reactor_casing` →4正式反应堆外壳 | 2钢板＋2铅板＋1屏蔽混凝土 | 3×3 ` S /LCL/ S `；S钢、L铅、C混凝土 |
| `crafting/reactor/reactor_window` →1反应堆观察窗 | 1正式外壳＋1铅屏蔽玻璃 | 工作台无序 |
| `crafting/reactor/reactor_cold_port` →1冷端 | 1正式外壳＋1耐压接头＋1密封环＋1蓝色染料 | 工作台无序；颜色区分冷热配方 |
| `crafting/reactor/reactor_hot_port` →1热端 | 同上，蓝色染料换红色染料 | 工作台无序 |
| `mechanical_crafting/reactor_instrument_port` →1仪表端口 | 10钢板＋6金板＋2精密构件＋1外壳＋1工业传感器＋1电子管 | 21格动力合成，5×5去四角；按[01B卡](./2026-10-03-ext-a-reactor-01b.md) |
| `mechanical_crafting/reactor_refueling_port` →1换料端口 | 1外壳＋1完整`create:deployer`＋1工业传感器＋1密封环 | 2×2动力合成 |
| `crafting/reactor/control_rod` →1现有控制棒组件 | 1钢杆＋1中子吸收陶瓷＋1 `c:plates/brass` | 工作台竖排：上黄铜片、中陶瓷、下钢杆；不可放置材料 |
| `mechanical_crafting/control_rod_drive` →1控制棒驱动器 | 1控制棒组件＋1机械活塞＋1活塞杆＋1精密构件 | 2×2动力合成 |
| `crafting/reactor/reactor_fuel_rod` →1燃料柱结构块 | 2钢板＋1钢格架 | 工作台竖列`S/G/S`；无燃料、无耐久，实际燃料仍装入顶部换料端口 |

通用钢、铅等输入读取已有细分标签。新钢杆加入`c:rods/steel`及父`c:rods`，材料语义不明确的陶瓷等保留本模组身份。混凝土使用`create_nuclear_industry:concrete_blocks`明确枚举16色原版硬化混凝土，不污染通用标签。蓝/红染料使用锁定版本已有`c:dyes/blue`、`c:dyes/red`（实施者先核对存在）。不增加拆解配方，不从状态设备/燃料组件回收材料。

屏蔽混凝土为普通静态建筑块，硬度3、爆炸抗性6、石材音效、镐采掘且至少铁镐、掉自身；名称不承诺尚未实现的辐射防护。其余新增材料堆叠64。原5个`incomplete_*`保留身份兼容；01C后原5种路线均停用，半成品仅作旧存档兼容。不加创造页、不自建进度或库存。

## 2. 初版范围、资源与验证记录（后续差异按01A/01B/01C卡）

- 使用已有8个P1方块、`control_rod`及其素材，禁止给旧`experimental_reactor_casing`样例添加配方或改变其身份。实际新燃料仍来自已验收屏蔽装配台；本批结构块不能包含或生成燃料组件。
- 新素材由SVG确定性导出16×16，沿已认可蓝灰钢、黄铜和陶瓷风格；共6种新材料、1张混凝土方块纹理和5种半成品。保留现有114张游戏PNG（含01B青金石粉），不修改旧共用manifest/pipeline。
- 16配方由独立审查逐份核对输入、数量、热级、身份与JAR打包；自动加载和加工验证集中于真实无热/有热搅拌、最长序列代表、五个工作台配方匹配，以及全套P1产物身份/纯结构合法性。不新增逐字镜像JSON的JUnit，复用已有原生压片/切石/机械锯/序列进度/管网证据，不复制全机器矩阵。PM收尾按治理5.1将原“16条全部实际匹配”的过宽描述细化为本分层范围；其余配方的实际客户端操作仍待手测，不宣称已获完整自动覆盖。
- 唯一执行者运行Gradle，沿隔离命名空间`create_nuclear_industry_reactor_crafting`与目录`build/gametest-reactor-crafting`。运行相关`P1DataContractTest`5项及新增GameTest组，最后一次增量assemble。若旧生存禁令误拦本卡精确路径的钢板输入，允许只对相应路径放行`steel_plate`，不放开冷热状态/乏燃料/污染路线，报告实际变更。禁止全量、clean、rerun-tasks或运行01B旧测试。
- 测试产物能构建既有合法固定堆、控制棒空列不被结构材料堵塞、无免费燃料；不点火、不改世界或自动操作用户客户端。真实新画面、JEI和搭建仍等用户人工验收，不用自动证据冒充。

### 首台成本样本

PM核对实际Ponder场景：基础模板加中心驱动器后，场景还增加第二对冷热端口，因此完整样本为81外壳、3窗、1仪表、2冷端、2热端、8换料端口、24燃料柱结构块和1驱动器，共122个可放置方块（98外层＋24内层）。该八列样本用于复核已存在的教学结构，不建议直接满燃料点火；配方验收仅空堆成型。

升级窗和端口还要消耗16外壳，故总需97外壳：制造25批产100，余3。25块屏蔽混凝土需25块任意硬化混凝土＋25铅粒；3铅屏蔽玻璃需3原版玻璃＋3铅锭。以下成本已按01A每次1柱及01B仪表21格修订。

| 本模组直接材料账（结构本体） | 总量与批次余量 |
| :--- | :--- |
| 钢板 | 118：外壳50、密封环6、耐压接头4、24燃料柱48、仪表端口10；另需24钢格架，其上游额外消耗48钢板 |
| 钢锭（杆材） | 1→2钢杆，用1余1；合并格架后的钢材总需求167钢锭当量，Create零件内含成本另计 |
| 铅板 / 铅锭 | 50板制外壳；另3锭制铅玻璃 |
| 铅粒 | 26：混凝土25、吸收陶瓷1；三铅锭拆27粒余1 |
| 焊料 | 使用4；按已有3锡＋1铅生产一批4件，无余量 |
| 工业陶瓷 | 1石英＋1黏土球产2，用1制吸收陶瓷余1；另消耗1红石粉 |
| 密封环 / 耐压接头 | 12 / 4；环需6批（各产2），接头4批 |
| 工业传感器 / 电子管 | 9 / 1（额外管）；传感器自身上游沿已验收配方另计 |
| Create成品部件 | 8完整机械手`create:deployer`＋1机械活塞＋1活塞杆＋3精密构件＋1黄铜板＋6金板 |
| 其他 | 2蓝色染料、2红色染料、3玻璃、25硬化混凝土 |

此表不把8个燃料组件、首次冷却剂、Create工厂与外部管路算免费，它们不属于结构本体账。若后续选择填满该布局默认3000mB堆内容量，01B需三粉各3＋3000mB水；回路持液另计，不将满罐设为启动门槛。新配方没有自建转速或固定秒数，吞吐由原生设备与物流决定。

## 3. 初版精确写集与分工（01A不沿用此写集）

候选：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`，实现基线`5353152`。既有01B不动，主工程用户`.vscode/launch.json`不动。PM独占核心docs和Git；执行者不得Git写、派发或验收。

`tools/art-assets/README.md`的本批生成器说明由PM按AGENTS职责维护，不是美术执行者写集。测试改动的根目录跟踪日志由PM保存后恢复，不纳入实现提交。

实现执行者：

1. 新`src/main/java/com/iksxh/create_nuclear_industry/content/ReactorCraftingContent.java`；仅在`CreateNuclearIndustry.java`增加注册调用/必要import，在`content/ModCreativeTabs.java`追加7成品；不重构旧注册或接口。
2. 双语言仅增加12个新身份键（6材料＋1方块＋5半成品）。新半成品ID：`incomplete_shielded_glass`、`incomplete_reactor_instrument_port`、`incomplete_reactor_refueling_port`、`incomplete_control_rod`、`incomplete_control_rod_drive`。
3. 16条配方文件路径如第1节，根`src/main/resources/data/create_nuclear_industry/recipe/`。`data/c/tags/item/rods/steel.json`、父`rods.json`；`data/create_nuclear_industry/tags/item/concrete_blocks.json`；仅追加`data/minecraft/tags/block/mineable/pickaxe.json`和`needs_iron_tool.json`的混凝土项；混凝土loot_table。
4. 新`gametest/ExtensionReactorCraftingGameTests.java`与隔离空模板`data/create_nuclear_industry_reactor_crafting/structure/p0_probe_empty.nbt`；`src/test/java/.../P1DataContractTest.java`仅在上述受限条件必要时修改。
5. 报告`build/reports/extension/EXT-A-REACTOR-01.md`与同名证据目录；不得改美术执行者资源、构建/依赖、运行算法、其他测试或用户存档。

**交付中单点授权：** 美术执行者冻结后，PM发现专用生成器把当时114张旧PNG数量写成永久运行前置。由实现执行者仅接手`tools/art-assets/reactor_01_assets.py`，改为按精确12相对路径保护写集、动态核对其它PNG哈希，取消固定数量门；不变更任何SVG/PNG设计。114仍保留作本轮基线证据，原美术执行者不再并发写入。新增重复资源存在性JUnit撤回，资源链由已有静态与JAR检查覆盖，原5项P1测试不删除。

美术执行者独占：

1. `tools/art-assets/reactor_01_assets.py`和`sources/reactor-01/`下本批12个SVG；允许复用现有严格SVG渲染模块，禁止改共用清单/导出器。对应`generated/item/`下11 PNG、`generated/block/shielding_concrete.png`。
2. 游戏`assets/create_nuclear_industry/textures/item/`下上述6材料＋5半成品PNG及同名`models/item/*.json`；`textures/block/shielding_concrete.png`、`models/block/shielding_concrete.json`、`models/item/shielding_concrete.json`、`blockstates/shielding_concrete.json`。普通cube_all和继承方块item，现有8种反应堆模型不动。
3. 报告`build/reports/extension/EXT-A-REACTOR-01-ART.md`和证据目录。只做本批资源校验与明暗底预览、旧PNG未改核对；不运行Gradle/客户端，不写语言/数据/核心docs。

## 4. 初版执行步骤与验收记录

必读AGENTS、治理5.1、本卡；实际应用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`与`minecraft-testing/SKILL.md`，美术另读`minecraft-resource-pack/SKILL.md`。手写代码注释/Javadoc中文。PM使用方案梳理、任务计划和子代理执行技能；用户本次委托覆盖通用技能的重复设计审批，治理5.1覆盖多层重复审查/全量验证，执行者Git禁令覆盖通用提交步骤。

- [x] 只读审计现有身份、结构样本和工序依赖，PM核对本卡与既有合同；`reactor_crafting_probe`报告位于`build/reports/extension/EXT-A-REACTOR-01-PROBE.md`。P1成品身份复用、7材料缺口、燃料结构分离及原生管道范围已核实。
- [x] 高速模型并行实施纯配方/注册及SVG，资源文件不交叉。
- [x] 美术交付后实现执行者独占Gradle完成定向组和增量打包；7项GameTest、原5项P1合同测试通过。首次模板命名错误修正后仅重跑受影响GameTest和assemble，失败报告保留。
- [x] 一次独立审查合并规格/质量/资源，未发现实际配方或运行逻辑缺陷；PM处理必要整改、保存候选和成本表。
- [x] 交付与01B合并的人工清单，保留待验状态后结束本批；不再自动推进热端。

人工验收分组：①01B原三项合并为原料/冷却剂生产；②新材料图标与代表工序、现行工作台配方及三种动力合成配方；③按Ponder搭固定5×5×5实验堆，核对新配方产物能成型，控制棒/端口行为沿旧规则。仅测试新增范围，不重跑旧全功能矩阵。
