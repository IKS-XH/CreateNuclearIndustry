# EXCHANGER-03：换热器给燃料烧结炉持续供热

**状态：** 用户2026-10-09确认工作盆02R1手测通过，并指定立即补齐烧结炉加热。本批实现及独立复核通过，功能候选`094a925`，增量打包成功、隔离GameTest3/3通过；账本未改复用02R1 21/21。本批停在[烧结炉手测](../../reviews/2026-10-09/exchanger-sintering-03/CANDIDATE.md)，未合main，不等待汽轮机取景复看，也不自动接其他主线或教学。

**目标与合同：** 现有燃料烧结炉直接放在核换热器顶部，即可替代下方烈焰人燃烧室。复用刚验收的工作盆持续热源：`basinHeatLevelEquivalent × huPerLevel HU/t`，默认2HU/t、4mB热液/t，产生等量冷液。沿用原配置键并说明同时作用于这两种顶部加工设备，不新增另一组数值。空炉、缺料、出料满照常耗热；不探测烧结工作状态，不按批次再扣费。缺热液或冷液满在换热器下一tick停热，恢复供回液后恢复。原生燃烧室普通/超级热仍可用；烧结1件400有效tick、断热保留工时、物流和配方均不变。

**架构：** 复用唯一持续供热账本及既有直列冷热事务，只扩展顶部负载识别与烧结炉只读热级查询。账本内部既有盆命名可以保留，不做无关重命名。服务端真实支付后才供热；重复热查询、护目镜和客户端视图不扣费。客户端使用同步视图展示，不从未同步的客户端账本推算运行状态。

**前置与分支：** 02R1自动21/21、真实GameTest7/7及用户手测已通过。本批实施于`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`、`codex/ore-acquisition`，功能基线`ab91356`，当前含取景候选`a94f2ea`与文档`e2300df`；派发时以实际HEAD记录。保留`logs/debug.log`、`logs/latest.log`及三个`tools/**/__pycache__/`。不修改美术工作树、用户世界或汽轮机教学。

## 写集与约束

执行者须读根AGENTS、治理5.1/5.2、本卡、[已验收02R1](./2026-10-08-ext-b-exchanger-02r1-continuous.md)；实际应用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`和`minecraft-testing/SKILL.md`。核对Minecraft1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6，不升级。

功能写集（Java根为`src/main/java/com/iksxh/create_nuclear_industry/`）：

- `heat/HeatExchangerBasinBridge.java`：扩展负载识别和热级查询，仍保留原工作盆调用和搅拌器唤醒；必要时可新建`heat/HeatExchangerProcessHeatBridge.java`供两设备共用，但不复制结算或储热。
- `heat/NuclearHeatExchangerBlockEntity.java`：顶部烧结炉优先走持续负载，与Create锅炉/内置高压锅炉/冷凝互斥；护目镜正确显示烧结炉负载与缺液/回液受阻。可按实际负载选择语言键，不能给烧结炉显示“工作盆加热中”。
- `production/FuelSinteringBlockEntity.java`：在保留原生燃烧室判断的同时读取本热源，保持既有热级门槛、LIT同步及工时语义。不得给换热器伪造烈焰人HEAT_LEVEL属性。
- `config/HeatExchangerConfig.java`：仅更新该共用配置键的中文说明，不新增费用、改默认数值或扩大配置合同。
- `gametest/ExtensionHeatExchangerSinteringGameTests.java`（新）：隔离命名空间`create_nuclear_industry_ext_b_sintering`；允许将已验收空盆模板原样复制到`src/main/resources/data/create_nuclear_industry_ext_b_sintering/structure/basin_empty.nbt`，仅作为本批隔离模板，不改变内容。必要时可修改`gametest/ExtensionHeatExchangerBasinGameTests.java`增加一项负载替换回归，不批量改旧测试。
- `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`、`en_us.json`：仅本热源新增/修改键。无模型、纹理、渲染器改动，不触碰美术负责人写集。
- 唯一实施报告`docs/reviews/2026-10-09/exchanger-sintering-03/IMPLEMENTATION.md`，原始证据`build/reports/extension/EXT-B-EXCHANGER-03/`。其他核心文档由PM维护。

PM另负责将已验收工作盆净功能合入main，排除未验收汽轮机教学。执行者如接到明确的集成指令，可仅在主工程两份语言文件加入`ab91356`中工作盆的八个键；这属于已批准差异集成，不得带入汽轮机文本或本批新烧结功能。具体指令与实际差异写入报告。执行者无任何Git写权限；代码/测试/资源中文注释遵守AGENTS。

## 关键不变量与精简验证

- 每源每tick只结算一次，热冷mB守恒；不同读取/ticker先后不会双扣、免费续热或让真实烧结永久停顿。
- 下方热源失效、顶炉移除、冷凝/直列冲突或内置锅炉归属时不供炉热；原锅炉40tick余热不得借给持续负载。换盆/炉不同时向两者发布热级。
- 工作盆路径保留现有行为。顶炉只读热查询不改库存、不强制加载区块；正常保存加载后须重新实际付款，不发放持久化免费热。
- 一次增量`./gradlew.bat assemble`；真实隔离`./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_ext_b_sintering -PgameTestDirectory=build/gametest-ext-b-sintering-03`，少量用例覆盖实际400tick产物、缺液/冷满暂停并保留进度后恢复、空炉消耗、反向ticker先后及盆/炉替换。同一用例可覆盖多个边界，原生燃烧室可用原有代表证据或本批一项代表，不展开矩阵。
- 账本不变时复用02R1定向JUnit；公共账本实质改动先报告写集需要，再补受影响JUnit。禁止全量、clean、强制重跑、旧存档测试；只因真实失败/修正复跑受影响项。构建/GameTest由一个执行者持有，保留每次日志，不覆盖失败证据。

实现冻结后单轮规格与质量复核，审查者读原始证据不重复运行测试。PM提交同级候选、更新制品和短客户端清单，停在烧结供热手测：确认热液烧结、断热/冷堵后进度保留并恢复，以及护目镜提示。不得自动接换热器教学或其他主线。

## 实际版本核查修订

执行者通过NeoForge21.1.219实际`GameTestRegistry`确认，`templateNamespace`同时决定结构寻址及测试命名空间过滤，无法用本批独立过滤直接引用另一命名空间模板。首轮模板ID拼接错误、第二轮无测试执行均不算通过，日志保留。PM选择原样复制109字节已验收空模板到本批命名空间，避免重跑7项已验收盆测试；这是验证入口修订，不是玩法决策或新测试框架。
