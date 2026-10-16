# MAIN-FLOW-01 只读核查报告

日期：2026-10-16。结论：在本卡覆盖的本模组＋Create 原生来源中，未发现可证实的必需原料缺失、自举死循环或乏燃料无终点。发现的是必须在整线中正确布置的物流、工序与启动约束。静态可达不等于新生存整体验收；本轮没有运行 Gradle、游戏、世界或单机测试，也没有修复代码。

## 边界与使用方法

- 已读 AGENTS.md、docs/project-governance.md（尤其5.1）、docs/STATUS.md、docs/project.md、docs/recipes.md及本批活动卡。
- 实际技能入口：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`。前者用于按实际版本追踪注册、数据配方、capability和服务端调用；后者用于区分静态检查、既有GameTest、真实运行与人工整体验收，不套用技能示例版本或运行通用全套命令。
- HEAD=`18d0096c6673fc47471867a1495f5222f2d25090`，STATUS记录生产来源=`850904df6220487f8903b00cc9936b8f68307d32`。工作区还有前批无关dirty，全部保留；针对工业流体源码、其配置/注册和全部data资源执行`git diff -w --numstat 850904df -- ...`无语义差异输出。未作Git写操作。
- `gradle.properties`实核MC1.21.1、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；`build.gradle`的`java.toolchain.languageVersion`为21，dependencies确实引用上述属性。
- Create本地依赖位于`C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280/`；实际读取`92471e8fc5ed4e3c2279001d77ebcec6fd38bcab/create-1.21.1-6.0.10-280-slim.jar`内配方/标签，及`23e1219501c0debfa0bb56c30ef8e0193341aae5/create-1.21.1-6.0.10-280-sources.jar`内原生实现。没有下载或安装。
- 下列`recipe/`、`tags/`路径默认相对`src/main/resources/data/create_nuclear_industry/`；Java路径相对`src/main/java/com/iksxh/create_nuclear_industry/`。没有用配方总数替代链路证明。

## 短链路表

| 环节 | 可追溯生存路径及数量 | 核查结果/关键入口 |
| --- | --- | --- |
| 三矿 | 主世界新生成区块：铅Y=-48～32、锡-16～112、铀-64～-16；次数6/12/2，铀矿脉size4。矿石掉粗矿，精准采集保矿石 | `worldgen/placed_feature/ore_*.json`、`configured_feature/ore_*.json`、`neoforge/biome_modifier/add_*_ore.json`、`loot_table/blocks/*_ore.json`；`OreGenerationFilter.shouldPlace`限制主世界，auto在存在外部同种矿方块时去重；本mod `data/c/tags/block/ores/*.json`及物品粗矿标签有真实成员。铅/铀为铁级工具、锡为石级，见`data/minecraft/tags/block/needs_*_tool.json` |
| 原生机械与钢材 | 铁锭粉碎→铁粉；煤/木炭粉碎→粉；4铁粉+1碳粉混合→5钢粉；熔炉→钢锭；压板、切杆/网/包壳管；锡铅熔炼→锭/粒 | `recipe/crushing/{iron,coal,charcoal}_dust.json`、`mixing/steel_dust_from_*.json`、`smelting/steel_ingot_from_dust.json`；钢不依赖本mod设备或核热。Create粉碎轮本身只用安山合金/木板/石，经原生动力合成取得 |
| 首批零件/设备 | 钢/铅/锡、石英/黏土、铜/金板、Create电子管/精密构件/坚固板→传感器/轴承→离心机、烧结炉、装配台、堆/炉/汽轮机、管阀泵 | `recipe/sequenced_assembly/*.json`、`heat_exchanger/reinforced_steel_plate.json`、`mechanical_crafting/*.json`及设备工作台配方。Create本地`mixing/brass_ingot.json`、`sequenced_assembly/{precision_mechanism,sturdy_sheet}.json`、`crafting/materials/electron_tube.json`提供外部来源：坚固板由黑曜石粉+500mB熔岩及两次压制；没有核电依赖 |
| 首批热源/动力/冷液 | 普通燃料喂烈焰人燃烧室提供HEATED；水轮等Create动力经传动/增速驱动加工及泵。青金石粉+红石粉+荧石粉+1000mB水→1000mB冷复合冷却剂（不要求热） | `recipe/mixing/compound_coolant.json`、`crushing/lapis_dust.json`；Create `BlazeBurnerBlockEntity.tryUpdateFuel`接受常规燃料，`HeatCondition.HEATED`接受KINDLED/FADING；`FuelSinteringBlockEntity.heated():47`接受同级燃烧室，故第一枚芯块不依赖核换热器；下界石英/荧石和捕获烈焰人是原生进度要求 |
| 铀浓缩/富集 | 1粗铀→1 Create粉碎铀→1浓缩料+1尾矿；8浓缩料+1000mB水→1000mB料浆；每1000mB料浆→1低浓铀粉+7贫化铀粉+1000mB水，work400 | 本地Create `data/create/recipe/crushing/{raw_uranium,raw_uranium_block,uranium_ore}.json`的条件是标签非空，本mod填入对应标签；`recipe/splashing/crushed_raw_uranium.json`、`mixing/uranium_slurry.json`、`centrifuging/uranium_slurry.json`；非概率主产物路径存在 |
| 芯块/组件 | 1低浓粉压成1生坯，HEATED烧结work400→1芯块；8芯块+4包壳管+2焊料锭+1钢格栅→1新组件，work25600 | `recipe/pressing/green_fuel_pellet.json`、`sintering/sintered_fuel_pellet.json`、`shielded_assembly/fresh_fuel_assembly.json`。按粗矿保底路径，每组件消耗64浓缩料，伴生64尾矿、56贫化粉，离心回水共8000mB；不计设备材料和概率矿石额外产量 |
| 反应堆/一回路 | 顶部换料口每列1组件；冷液受热→等体积热液→高压锅炉/换热器→等量冷液回堆 | `reactor/FuelRefuelingTransaction`和`blockentity/ReactorPortBlockEntity`为权威取放事务；冷热口2048mB/t，锅炉水/汽口4096mB/t（`config/BoilerConfig`）；热液/SC专管，冷液可原生Create铜管（`industrial/RestrictionPolicy.restricted`只列热液与SC） |
| 蒸汽/应力/回水 | 锅炉按新批实际温压产普通汽或SC；汽轮机只吃SC、等量排普通汽；独立换热器普通汽冷凝1:1成水→给水口 | `boiler/BoilerState:291-309`、`BoilerPortBlockEntity.selectedSteamKind():29`；`config/TurbineConfig:20-22`额定54/108/216mB/t；`config/HeatExchangerConfig:34-48`单台冷凝54mB/t、1000‰回收，蓝冰无限冷源；正上方冷源与管口见`heat/NuclearHeatExchangerBlockEntity.topColdSource():267` |
| 乏燃料终点 | 耗尽列停止裂变后取出→冷却乏燃料；1乏燃料+1玻璃固化介质+1屏蔽桶→封存桶→无动力干式架 | `reactor/FuelRefuelingTransaction.extract():210`耗尽即转换冷却乏燃料；`recipe/shielded_assembly/sealed_spent_fuel_cask.json`、`mixing/vitrification_medium.json`、`crafting/lead_shielding_cask.json`、`crafting/dry_storage_rack.json`；`storage/SpentFuelPayload.seal`保存完整乏燃料到CONTAINER，`DryStorageBlockEntity`接收有效桶；默认16单件槽，已存在终点，无需等待再处理/辐射功能 |

## 按影响排序的发现

### 已证实的代码/配方硬断点

本轮未发现。以下约束不能单独作为“缺功能”“已验设备失效”的结论。

### 现行物流约束与待讨论取舍

1. **离心副水与贫化粉必须同时外排。** `production/CentrifugeState:14-15,41-45,57-79`：水箱4000mB、物品槽64，开批和完批都检查全部输出。只排低浓粉、不排水时，4批后第五批等待，尚不足首组件8芯块；只排水、不排贫化粉时，第9批后63粉，第10批不能再放7粉。`CentrifugeBlockEntity:258-333`已有侧面排水、侧面双槽提取、上半顶面料浆入口，可用Create管道及带筛选的漏斗分别送水、低浓粉和贫化粉。贫化粉当前没有继续消耗配方，作为库存保存；尾矿可4合1压块（`recipe/compacting/uranium_tailings_brick.json`）。副产物长期存储是玩法/成本选择，不授权新增用途，也不阻断现有主线。
2. **连续制造与封装不要争用同一台有余料的装配台。** `production/ShieldedAssemblyState:37-41,64-78,95-102`：首料锁工序，所有输入与输出清空、进度为0才解锁；普通自动口只提取输出，不能自动撤回余料（`ShieldedAssemblyBlockEntity:247-265`）。同一台可以按批清空切换；若要两路持续并行，现有解法是独立制造台与封装台，各自供料取成品。不是新设备需求。装配台完整2×2×2、外侧有效面；顶部只输入、侧面输入与输出、底面无口（`ShieldedAssemblyLayout.exterior():36`、BE `itemPort():127`）；速度至少32RPM，4SU/RPM（BE `calculateStressApplied():45`及`waitReason`）。
3. **锅炉普通汽与汽轮机排汽的排路不能省略。** `boiler/BoilerState:59-60,291-309`：普通/SC共汽容量，已有普通汽不会随温压升级；`BoilerPortBlockEntity:29`默认选SC，选择是库存过滤。启动普通汽可另设普通汽口或按需切口进入独立冷凝路，勿把普通汽送汽轮机，也勿据“普通汽有库存而SC口没出汽”认定断链。是否需要两口取决于实际启动/运行库存，不增加强制结构规则。汽轮机只有约1tick周转库存（`turbine/TurbineState:91,160-197`），排汽堵塞将抑制进汽与动力，是守恒背压。额定回水按单台54mB/t计算，短/中/长满负荷至少1/2/4台冷凝器；锅炉普通汽旁路若同时运行，另计所需吞吐。蓝冰能免冷源补充，水源/冰雪会消耗；冷凝器必须独立于供锅炉核热的同一台换热器，不能让同机承担冲突模式（`NuclearHeatExchangerBlockEntity:197-233`）。
4. **启动泵应保留可独立供给的原生动力。** 工业泵16SU/RPM，256RPM为4096SU（`config/IndustrialFluidConfig:18-23`）；流量按RPM缩放（`IndustrialPumpBlockEntity.flowLimit():34`），不必一开始强求256RPM额定。生产线及首次冷/热循环若全依赖尚未产汽的汽轮机，将产生玩家布置的启动循环；现有水轮等原生动力可打破它。切入汽轮机动力时的转向、转速和过载需在整线现场看；锅炉汽口可主动出汽，不自动要求每段都再放工业泵。
5. **换料有安全门且必须使用专用交互。** 换料端口没有普通ItemHandler；已注册Create机械臂专用点（`CreateNuclearIndustry:122`、`reactor/FuelRefuelingArmInteractionPoint:85-124`）。取料要求当前裂变热≤1E-12，耗尽组件可以在其他列仍工作时取出；不要求缓存余热归零（`FuelRefuelingTransaction:210-266`）。人工持新料可快捷替换枯竭件，机械臂仍分开取出与装入。停堆可取回尚有耐久的新组件，因此自动取料目的地应筛冷却乏燃料；不能假定机械臂抽到的一定是乏燃料。现有安全门为既定规则，不是本批新缺陷。

### 已排除的误报

- 没有本mod三矿粉碎JSON≠没有来源：本地Create配方按`c:raw_materials/*`、`c:ores/*`和粗矿块标签读取，本mod提供真实成员；Create的压板/精密构件/电子管/坚固板也均有本地来源。
- 锡线、包壳管、钢网等只有石切配方≠只能手工：本地Create `SawBlockEntity.getRecipes`接受`RecipeType.STONECUTTING`且执行`StonecutterRecipe`结果；`CRecipes.allowStonecuttingOnSaw`默认true。钢板有包壳管/钢网等多结果，锯设置成品筛选即可持续加工；如果用户主动关闭原生该配置，才退回手工石切或需讨论配置。
- 核换热器不能作为首批核热来源≠芯块自举失败：普通燃料烈焰人HEATED已能烧结。冷却剂配方也不要求核热或超级加热。
- 封存桶必须含真实乏燃料载荷：JEI显示的普通成品展示栈本身不是实际封存流程；`ShieldedAssemblyRecipe.actualResult`调用`SpentFuelPayload.seal`，实际制造的桶有合法CONTAINER，可入干式架。

## 最小集中整线手测顺序（本轮未执行）

沿用已验单机、现有世界/合法生存材料，不重做各单机边界测试。此顺序是一次整合观察，不是新增资格门；等待PM安排和用户实际操作。

1. **首批与独立启动：** 从实际采集的铀粗矿走通粉碎/洗矿/料浆；常规燃料HEATED烧结，制造一枚组件，同时实际混合冷液。采用水轮等独立动力供生产/泵，确认未从创造库存取必需材料。记录实际Create配置、速度/应力及产物数量；首枚组件预算为64浓缩料、8个1000mB离心批次、8芯块、4管、2焊料、1格栅。无需重跑已验矿脉/工具/单机全套。
2. **持续物流：** 上半顶入浆，侧面排水，侧面筛分低浓粉/贫化粉；让至少8批构成首组件，继续跨过第10批仍能运行，确认排水和副产物没有堵住下一批。生坯顶入烧结炉、成品侧出；装配台侧出新组件并进入换料物流。制造线持续供料时，封装使用另一现有装配台；若只用一台则先清空再切批，明确这项操作限制。
3. **核热与回水：** 先接完整冷液→反应堆→专用热液管→锅炉→冷液回流；确认锅炉内换热器/再加热段成对有效、给水和普通/SC汽路接对。观察启动普通汽去向、SC推动一档汽轮机、普通排汽进入独立冷凝器回锅炉；用蓝冰冷源或明确补给水/冰雪，按实际流量配冷凝台数。查看冷液、锅炉水、普通/SC汽和汽轮机排汽库存趋势，运行到这些库存不再单向积满/耗空，期间副产物仍外排。启动独立动力保留到用户确认循环稳定；不要求本轮改自动控制玩法。
4. **终点接续：** 等实际组件耗尽，由玩家或机械臂从停止裂变列取出冷却乏燃料，流向封装台（原生机械臂取新料与乏燃料按物品筛选分流）。投入1固化介质和1屏蔽桶，实际成品移入干式架，护目镜占用数增加1，保留这一件产物身份/数量记录。若本次自然燃耗尚未耗尽，就把第4步明确记为未完成，不能用创造乏燃料或自动测试结果冒充本次生存终点。

本次交付到只读报告为止；不宣布新整体验收，不修改配方、倍率、世界、状态文档、生产代码或Git。
