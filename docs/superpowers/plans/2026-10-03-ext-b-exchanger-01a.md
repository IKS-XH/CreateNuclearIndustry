# EXT-B-EXCHANGER-01A：首台核热换热器

**状态：需整改，尚未客户端验收。** 自动验证与一次合并独立复审的历史证据保留。用户于2026-10-04要求降低制造成本、缩短合成链，并改为顶部鳍片格栅＋底部基础方块，见[成本与外观修订](./2026-10-04-heat-exchanger-cost-model-revision.md)；新配方已指定为工作台3铜板＋1管束＋5钢板，下文旧制造与笼架造型不再作为最终验收目标。其他运行合同继续沿用2026-10-03[整套方案](./2026-10-03-nuclear-heat-exchanger-proposal.md)。PM负责文档/Git/审核，执行者负责实现。完整三模式、工作盆与专用蒸汽链仍后置。

## 目标与基线

交付可生存制造的单格核换热器，将现有热复合冷却剂1:1转回冷液，并为顶部真实Create储罐锅炉供热。不接转轴、不产SU、不新增GUI、过滤槽、热液桶、蒸汽或事故破坏。

- 候选：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，`codex/ore-acquisition`；运行基线`851a848c98ee19343974855f0e774543d5a2f1f0`，开工时接收本卡的纯文档提交。
- 主目录`E:/MyMC/NewMod/Create_NuclearIndustry`暂不合入本批运行实现。保留用户`.vscode/launch.json`；候选保留`tools/art-assets/__pycache__/`，不清理、不暂存。
- Minecraft1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6 / JEI19.27.0.340；禁止升级依赖或改构建配置。
- 材料、燃料、冷却剂和实验堆制造已人工验收。锅炉API静态依据为[01A报告](../../reviews/2026-10-03/heat-api-01a/EXT-B-API-01A.md)，真实运行由本卡补证，不能据此宣称完整EXT-B-API-01完成。

## 必读与技能

所有执行者读`AGENTS.md`、`docs/project-governance.md`第1/4/5.1节、本卡、确认方案与API报告。必须实际读取并应用：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`
- 资源执行者另读`C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`

新增/修改手写注释与Javadoc使用中文；非显然生命周期、单位、事务、状态不变量和客户端/服务端边界需解释。用户精简验证规则优先于通用技能中的重复验证；执行者一律无Git写权限、无核心文档修改权限，不得自行派发。

## 冻结行为

以确认方案第2～4节为完整合同，以下为实现注意项：

1. `nuclear_heat_exchanger`单格、占满方块；水平朝向只影响外观。顶面只供热，其余五面填热液、仅排冷液；两个4000mB罐。模拟能力请求纯读取，热→冷转换同时检查输入与输出容量，服务端原子提交。缓存handler在移除/卸载后不得继续改库存。
2. 独立有界配置：锅炉整数热等级默认18、每等级1HU/t、预热/余热40tick。能量密度读取既有`P1ServerConfig.coolantAbsorptionHuPerMb`，默认0.5HU/mB，不改变反应堆配置与公式。无效/零密度安全停机，不允许NaN/无限值或配置变更凭空增热。
3. 默认每tick转换36mB，返36mB；先实际支付1440mB形成720HU储备后开始供热。持续供热每tick扣18HU、补回实际转换热量。保留不足整mB的有限余数，不能舍入创造热量。
4. 热回调只读。没有合格负载不转换；已付储备仅按剩余额定tick散去。断料/回液堵塞最多使用已付40tick余热；少量补液、反复查询、拆放、保存恢复不得免费刷新储备/倒计时。部分预热、热储备、运行阶段和剩余窗口可精确保存并验证。
5. 负载是紧贴上方的Create锅炉底层，具有引擎或汽笛，体积及供水至少支持一级。不要依赖activeHeat造成启动循环。18完整等级受原生72储罐/180mB/t供水上限约束，小锅炉仍按额定流量，护目镜提示限制。只放储罐不耗液；不检查外接轴网是否有应力消费者。
6. 使用`BoilerHeater.REGISTRY`；活动18，停止`NO_HEAT=-1`，不返回被动0。不得暴露可供盆读取的`BlazeBurnerBlock.HEAT_LEVEL`或被动加热标签。加载先验证负载；热态改变、移除、卸载和恢复必须使锅炉重算，覆盖跨区块缓存。
7. 扳手/正常拆除仅掉一台，携带两罐及已付储备；重放不重置时间或复制库存。禁止活塞/构造移动复制机器。沿用现有机器原版生存采集/创造模式语义，破坏取消时不掉落。无独立右键界面。
8. 护目镜显示罐量、预热/供热/余热/缺液/堵塞/无负载/无效配置、流量(mB/t)、热等级及锅炉尺寸/供水限制。客户端只读同步，不结算热量。

## 注册与制造接口

- 材料类`content/HeatMaterialsContent.java`注册`STEEL_PIPE_BLANK`=`steel_pipe_blank`、`REINFORCED_STEEL_PLATE`=`reinforced_steel_plate`、`NUCLEAR_HEAT_EXCHANGE_BUNDLE`=`nuclear_heat_exchange_bundle`。中文：钢管坯、强化钢板、核换热管束。
- 设备类`content/HeatExchangeContent.java`注册`NUCLEAR_HEAT_EXCHANGER`方块、`NUCLEAR_HEAT_EXCHANGER_ITEM`及BE；设备Java集中`heat/`包，配置独立`config/HeatExchangerConfig.java`。
- 钢管坯通用标签`c:tubes/steel`（本批新增约定，非声称NeoForge内置），父`c:tubes`；强化板`c:plates/reinforced_steel`汇入`c:plates`但不进入`c:plates/steel`；管束专用`create_nuclear_industry:nuclear_heat_exchange_bundles`。普通钢/铜、坚固板、精密构件优先对应具体通用/Create标签，核查未发现既有耐压接头/工业传感器专用标签，因此本批新增`create_nuclear_industry:pressure_fittings`与`industrial_sensors`，各仅包含既有同名物品，不更改旧配方。
- 钢锭切石1→2管坯，沿Create锯原生切石回退；强化板工作台竖排坚固板/精密构件/钢板1→1；管束工作台`P P / CRC / P P`（4管坯+2铜片+1强化板）→1；整机21格` SSS / SCPCS / SHIHS / SCPCS / SSS `→1，含12钢板、4铜片、2耐压接头、2管束、1工业传感器，`accept_mirrored:false`。无序列装配、额外工时、副产物。

## 分工与精确写集

三者并行使用同一候选，严禁覆盖另一执行者文件。全局接线由设备执行者独占；语言文件由材料执行者独占，设备提供待合入键值清单。

### MATERIAL（高速代理）

- `src/main/java/com/iksxh/create_nuclear_industry/content/HeatMaterialsContent.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionHeatExchangerCraftingGameTests.java`
- `src/main/resources/data/create_nuclear_industry/recipe/heat_exchanger/`四配方；`data/c/tags/item/tubes.json`、`tubes/steel.json`、`plates/reinforced_steel.json`和既有`plates.json`仅新增引用；`data/create_nuclear_industry/tags/item/nuclear_heat_exchange_bundles.json`、`pressure_fittings.json`、`industrial_sensors.json`（PM按实际资源核查补充写集，不新增物品/玩法）。
- 三材料同名`assets/create_nuclear_industry/models/item/*.json`、`textures/item/*.png`；`assets/.../lang/zh_cn.json`及`en_us.json`仅新增本批键，保留其他内容。
- 独立源稿/生成脚本`tools/art-assets/heat-exchanger-materials/`；报告`build/reports/extension/EXT-B-EXCHANGER-01A-MATERIAL.md`及同名证据目录。禁止改旧生成器/清单。
- 定向配方GameTest放统一命名空间`create_nuclear_industry_heat_exchanger`；模板由设备执行者唯一负责，不单独启动Gradle。材料负责代表性切石/原生锯兼容、工作台与21格匹配和数量；不重复旧材料矩阵。

### DEVICE（复杂事务与Create集成）

- `src/main/java/com/iksxh/create_nuclear_industry/heat/`新包；`content/HeatExchangeContent.java`；`config/HeatExchangerConfig.java`。
- `CreateNuclearIndustry.java`、`content/ModCreativeTabs.java`仅增加本批注册/能力/锅炉/配置/创造页接线，包含调用`HeatMaterialsContent.register`和三个材料展示项。
- `src/test/java/com/iksxh/create_nuclear_industry/heat/`定向账本测试；`src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionHeatExchangerGameTests.java`；`src/main/resources/data/create_nuclear_industry_heat_exchanger/structure/`必要测试模板。
- `data/create_nuclear_industry/loot_table/blocks/nuclear_heat_exchanger.json`；现有`data/minecraft/tags/block/mineable/pickaxe.json`、`needs_iron_tool.json`仅增加设备。
- 独占Gradle/测试运行；报告`build/reports/extension/EXT-B-EXCHANGER-01A-DEVICE.md`及同名目录；语言新增键清单也存此处交材料执行者。
- 不修改P1反应堆热工/配置/事务，不改旧设备、依赖或模拟器。发现需要越界先报告PM。

### ART（高速代理）

- `assets/create_nuclear_industry/blockstates/nuclear_heat_exchanger.json`；`models/block/nuclear_heat_exchanger*.json`或`models/block/nuclear_heat_exchanger/`；`models/item/nuclear_heat_exchanger.json`；`textures/block/nuclear_heat_exchanger*.png`或`textures/block/nuclear_heat_exchanger/`（独立纹理目录，PM确认）。
- `tools/art-assets/heat-exchanger-device/`独立SVG、生成器及部件/枢轴说明；报告`build/reports/extension/EXT-B-EXCHANGER-01A-ART.md`及同名目录。
- 外壳与热芯分层可拆、预留动画；本轮静态完整模型，无动态渲染。模型限定0～16，避免共面重叠、透明漏缝、默认越界UV和过大手持显示。临时约定`facing=north/east/south/west`、`lit=false/true`，lit仅本机视觉不是Create热级；与设备执行者经PM协调。
- 不改Java、配方、语言、旧素材。生成预览供PM查看；检查资源引用及打包由合并检查复用。

## 验证与交付

不强制开工跑旧测试。账本关键用例：流体及热量守恒；40tick预热/残热边界；断供、堵塞、无负载；模拟纯度；NBT与物品拆放；无效配置。真实GameTest至少覆盖公开BoilerHeater查询只读、实际水输入/锅炉结构与引擎热量18及原生上限、恢复与移除/卸载无陈旧热。不能直接写`activeHeat`后当作供热集成证据。

设备执行者待三写集齐备后执行一次相关JUnit与GameTest、一次增量assemble（可同一命令）：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-21'
./gradlew.bat test --tests '*heat.*' runGameTestServer -PgameTestNamespace=create_nuclear_industry_heat_exchanger -PgameTestDirectory=build/gametest-heat-exchanger-01a assemble --console=plain
```

筛选以实际任务/类名可用为准，不能悄悄回退全量。PM发起唯一最终测试时机；实现期允许必要增量编译或确有疑点的局部重现。失败后只修复/复测受影响部分，报告真实失败与修复证据；已通过部分不机械重跑。已知退出停滞保留明确断言结果并仅清理本轮自有进程。

交付包含实际技能、文件列表、关键实现说明、命令与原始证据、风险和未验收客户端项。三报告齐后进行一次合并规格/质量独立复审，读证据不重跑同套测试。PM保存候选提交及合并人工清单：①制造/JEI/模型/护目镜；②反应堆热液→Create锅炉发电→冷液回流；③堵塞/断供/恢复/拆放重载。人工未过不记完成、不合入main运行代码、不派发后续盆/蒸汽功能。

所有执行者仅可只读Git，不暂存/提交/切分支/回退/清理；不得动运行客户端及用户存档。

## 2026-10-04交付记录

候选实现采用上述写集；7个不同账本JUnit分两次通过，最终10/10定向GameTest及增量assemble通过。首轮测试准备错误及独立复审发现的FULL但非ticking缓存热缺陷均已定位整改；失败与成功原始证据保留于[交付页](../../reviews/2026-10-03/exchanger-01a/README.md)。新增边界验证使用真实区块上的受控原生FullStatus门；不冒称自然票据迁移或磁盘重载已通过。测试运行生成的两份已跟踪根日志由PM备份后恢复，原用户配置及pycache保持。运行候选不合入main，按交付页三组清单等人工验收。
