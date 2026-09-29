# EXT-A-START-01：三矿最小开工批次与接续决策包

日期：2026-09-29。执行者静态准备交付，等待 PM 审核。**本报告不批准参数、不派发实现、不改变任务状态，也不是游戏验证或生存闭环验收。**

## 1. 结论与本轮最早门

推荐下一实现批仅交付：**铅/锡/铀普通及深层矿石、粗矿/粗矿块、自然生成与正确挖掘、通用标签、粗矿块压缩/解压，以及真实 Create 既有粉碎路线的接入验证**。出口到三种 `create:crushed_raw_*` 和可储存粗矿即止；不混入洗矿、粒熔锭、富集或空设备壳。矿脉参数、工具掉落及原生粉碎副产物的取舍是本批最早的用户门，集中在第 6 节三组决定。

已有 D-01、D-02/02a、D-03a/b/c/d 不重问。D-03e 的制粉 1:1、两道 `processing_time=100` 和冷却剂 `heated` 仍是候选；不把它作为三矿批次的前置，也不追认批准。P1 交接已满足，不沿用历史审计的旧 P1 人工阻塞；`EXT-A-MATERIAL-01A` 自身验收仍未通过，不能视作已合并。依据：准备计划第 1、2 节（12–16、74–106 行）。

## 2. 基线、实际使用技能与证据

- 工作目录 `E:/MyMC/NewMod/Create_NuclearIndustry`；开工 HEAD `966945808860e7b0ee1d40a5241805f78ef7ca2d`。启动卡正文的代码基线 `8af153f` 是代码历史锚点；实际执行基线为 PM 指定的 `9669458`。
- 开工只有 `.vscode/launch.json` 跟踪修改，本执行者保留。未访问另一美术工作树，未执行任何 Git 写操作、派发、Gradle、客户端、服务端、GameTest、配置改动或依赖安装。
- 完整读取 `AGENTS.md`、`docs/project-governance.md`、2026-09-29 启动卡，以及活动准备计划、成本草案和下述相关策划材料。所有候选仅落本报告。
- 实际读取 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：核对注册与素材的区别、原生配方复用、1.21.1 单数 `recipe`/`loot_table`/`tags/item` 路径。
- 实际读取 `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：将静态资源、实际注册/挖掘/选方、自然生成采样和客户端体验分开；不把 JSON 或 JAR 证据当游戏通过。
- 实际读取 `C:/Users/IKSXH/.codex/skills/minecraft-world-generation/SKILL.md`：使用 configured/placed feature、石质替换标签、biome modifier、数据包关闭与新世界验证；不套用 1.21.11 数据结构。
- 版本由 `gradle.properties:6–14,18`、`build.gradle:13,80–85` 核对：Minecraft 1.21.1、Java 工具链 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6、All Rights Reserved。

### 2.1 复用已验收审计，不重做全链

`docs/archive/EXT-A-DEPS-01.md:3–5` 记录 2026-09-24 的静态审计验收。直接以只读 ZIP 流读取 `docs/handoffs/2026-09-28/previous-task-evidence.zip!EXT-A-DEPS-01.md`，原始条目 SHA-256 实算为 `2dcad9f26ee11b734cd11add22ebe02a914cd77f6ff791c3687e205b94a2fc05`，与归档一致。旧报告的章节/行号均指 ZIP 条目本身，不是当前文档行号。

复用重点：旧报告 73–117 行的节点表；171–175 行的三矿已有粉碎/第三方洗涤限制；184–195 行 F01–F12；199–203 行非核启动分析。旧 F01 碳粉设备分工、F08 洗水语义以及 F04 最低热级已被 D-03d/b 更新；未解决的是实际接入、数量和设备行为，不能原样再次问用户旧问题。也读取 ZIP 中 `EXT-A-PARAM-01.md` 的静态复核结论；它没有批准 D-03e。

### 2.2 本次新增定点证据

证据目录为 `build/reports/extension/EXT-A-START-01/`。

| 文件 | 内容与准确定位 |
|---|---|
| `minecraft-1.21.1-selected.json` | 从本机 `minecraft_1.21.1_client.jar` 只读提取，键即原 JAR 条目名；`version.json` 确认 1.21.1、Java 21；选中铜/铁/钻石 configured/placed feature、铁/铜掉落及工具标签 |
| `create-selected-crushing.json` | 锁定 Create slim JAR 的九个 `data/create/recipe/crushing/{lead,tin,uranium}_ore.json`、`raw_{lead,tin,uranium}.json`、`raw_{lead,tin,uranium}_block.json`；保留条件、标签、数量、概率、时长 |
| `javap-ProcessingOutput.txt:133–168` | JDK 21 `javap -c -p` 检查锁定 slim JAR 的 `ProcessingOutput.rollOutput`：概率输出逐个数量抽取，不能把 9 个经验颗粒写成整组一次 75% |
| `artifact-hashes.txt` | Minecraft 客户端 JAR、Create slim JAR、交接 ZIP 的绝对路径和 SHA-256 |

Minecraft JAR：`C:/Users/IKSXH/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar`，SHA-256 `499f6897d1837516680f3114072d8106e11c9adcd933fe5cf051b551089b0c99`。

Create JAR：`C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280/92471e8fc5ed4e3c2279001d77ebcec6fd38bcab/create-1.21.1-6.0.10-280-slim.jar`，SHA-256 `f0652bee27460f2d26a748f537cb5d687981441378d3a526d17ffaab5a1072bb`，与旧审计一致。

## 3. 最小差集：已确认、缺实现、缺参数

| 范围 | 已有合同/实际内容 | 尚缺内容；本批边界 |
|---|---|---|
| 三矿身份 | `content-catalog.md:53–61,133–140` 已定六矿石、三粗矿、三粗矿块身份；Create 粉碎粗矿复用，不注册铀金属形态 | 当前本模组未注册这些身份，只有规划/素材；需注册、模型、标签、掉落、生成。工具等级、具体掉落数量/附魔效果未冻结 |
| 生成定位 | `project.md:102–113`：铅中低层中等矿脉低于铁、锡中上层小至中型略低于铜、铀深层小型稀有；生成数字可配置、数据包可关闭、检测等价矿物默认避免重复并允许整合包覆盖 | 高度端点、形状、次数、size、空气暴露和精确群系范围未冻结。定性定位不能当作已有数字 |
| 掉落合同 | `content-catalog.md:140,229` 要求掉落表、镐挖掘及正确工具等级；粗矿身份已定 | 没有已批准的“铅铁镐、锡石镐、铀铁镐”或 Fortune 数字。本报告第 4 节全部为建议，不能把普通原版习惯写成旧合同 |
| 粉碎接入 | 原 JAR 九条非空标签条件配方实际存在；三种粉碎粗矿身份已有 | 加标签会激活配方。与 `recipes.md:160,170,181` 副产物表达差异须决定；不再添加同输入粉碎配方。洗涤不是无第三方即可自动闭合 |
| 基础材料 | 钢板现有注册 `ModItems.java:41–44`；通用标签原则及粉碎轮制碳粉已批准 | 粗矿→锭→板，碳粉→钢锭→现有钢板均缺生产实现和数量。铅/锡粒熔锭存在数量/工序歧义，不能默认为 1 粒→1 锭 |
| 三台设备 | `recipes.md:329–331` 已给材料关系；`content-catalog.md:166–168` 已给职责，烧结最低普通热已定 | 三机未注册/无运行；制造数量、状态/库存/接口及故障恢复待合同。不能先做无用途设备壳并称首台设备完成 |

本次源码定点复核与旧审计一致：`ModItems.java:24–48` 只有 P1 共用物品，`ModBlocks.java:28–38` 只有旧外壳；入口 `CreateNuclearIndustry.java:39–49` 注册这些内容和 P1/P0。`ModCreativeTabs.java:29–43` 不含三矿；`rg --files src/main/resources/data` 仅发现 P0 两条配方、P1 掉落/结构及钢板标签，没有三矿 worldgen/biome modifier。旧图像不能证明注册或生存来源。

## 4. 三矿第一轮参数建议（全部未批准）

### 4.1 直接来自 Minecraft 1.21.1 的比较基准

下列是依赖事实，不是对本项目新值的批准。条目均见 `minecraft-1.21.1-selected.json`：

| 原版条目 | placed feature | configured feature |
|---|---|---|
| 普通铜 `ore_copper` | 16 次；Y=-16…112，trapezoid，未设 plateau（中心约 48） | `ore_copper_small` size=10，空气暴露丢弃=0 |
| 大铜 `ore_copper_large` | 16 次；相同高度分布 | size=20，空气暴露丢弃=0；不能把普通铜当作所有群系唯一基准 |
| 中层铁 `ore_iron_middle` | 10 次；Y=-24…56，trapezoid（中心约 16） | `ore_iron` size=9，空气暴露丢弃=0 |
| 铁另外两批 | `ore_iron_small` 10 次、从世界底部至72的 uniform；`ore_iron_upper` 90 次、Y80…384 的 trapezoid | small size=4，upper 复用 size=9；铁有多批，不能只比较10次就宣布铅实际少于铁 |
| 小钻石 `ore_diamond` | 7 次，`above_bottom=-80…80` 的 trapezoid；不得误读成绝对 Y=-80…80 | size=4，空气暴露丢弃=0.5；仅借小型规模/暴露机制，不把铀燃料价值等同钻石 |

`count` 是放置尝试次数；`size` 是矿脉算法参数，**两者乘积不是区块产量，也不是保证命中块数**。分布会被有效石质、世界高度、洞穴/液体/替换条件裁剪；不同批与矿脉可能重叠。下面是首轮采样的起点，尚未证明定性稀有度或玩家采矿耗时满足目标。

### 4.2 推荐生成表

所有本项目数字、分布及具体范围均为**候选**：

| 矿 | 目标维度/群系 | Y 分布（绝对高度） | 每区块尝试 | size | 空气暴露丢弃概率 | 推荐理由 |
|---|---|---:|---:|---:|---:|---|
| 铅 | 默认世界的主世界群系，首轮不设专属群系奖励 | -48…32，三角形（`trapezoid`、plateau=0），中心-8 | 6 | 7 | 0 | 比中层铁批次更少的尝试/规模，允许洞穴发现；兼顾屏蔽大量使用的定位 |
| 锡 | 同上 | -16…112，三角形，中心48 | 12 | 8 | 0 | 与普通铜同高度便于顺路寻找；比普通铜少的次数/规模，避免专门远征 |
| 铀 | 同上 | -64…-16，三角形，中心-40 | 2 | 4 | 0.25 | 深层、小型、较少尝试；保留部分洞穴可见性，不直接采用钻石0.5暴露抑制 |

替换目标沿原版结构：`minecraft:stone_ore_replaceables` → 对应普通矿石，`minecraft:deepslate_ore_replaceables` → 对应深层矿石。每矿只添加一次 placed feature，避免石质版本各算一次矿脉而翻倍。

实现建议用本模组可覆盖的目标群系标签，初始成员引用 `#minecraft:is_overworld`。**群系标签不是严格维度过滤器**：自定义维度复用主世界群系时也可能命中；若批准文字要求“仅 minecraft:overworld”，下一卡须显式加入并验证维度限制，不能宣称单个 biome modifier 已保证。默认三维度的下界/末地不在该群系集合内。

可配置、默认去重、整合包强制开关沿已有合同实施，不是新玩法提问。建议数字和形状以可覆盖数据资源表达；默认去重按“同矿石标签中存在本模组以外的有效矿石成员”判断，不把本模组自身成员或单个第三方锭当成等价世界矿物；该检测口径是**实现建议，须 PM 冻结并验证标签加载生命周期**，不能仅用普通无条件 `neoforge:add_features` 就声称完成去重。关闭仅影响新生成区块，不删除旧矿，不承诺热重载能重新生成旧区块。

数据包同路径覆盖 biome modifier 为 `{"type":"neoforge:none"}` 是 NeoForge 1.21.1 文档支持的关闭方式；该机制可作关闭入口，但不能替代本项目其他配置与去重要求。[NeoForge 1.21.1 Biome Modifiers](https://docs.neoforged.net/docs/1.21.1/worldgen/biomemodifier/)

### 4.3 推荐工具、掉落与压缩（全部候选）

| 对象 | 最低正确工具 | 普通挖掘 | 精准采集 | 时运 |
|---|---|---|---|---|
| 锡普通/深层矿 | 石镐及更高有效等级 | 1 `raw_tin` | 对应原矿石1个 | 粗矿使用原版铁矿同类 `minecraft:ore_drops` |
| 铅普通/深层矿 | 铁镐及更高有效等级 | 1 `raw_lead` | 对应原矿石1个 | 同上 |
| 铀普通/深层矿 | 铁镐及更高有效等级 | 1 `raw_uranium` | 对应原矿石1个 | 同上；不额外削减核材料时运 |
| 三种粗矿块 | 对应矿种同等级镐 | 原粗矿块1个 | 相同 | 不增产 |

建议未达工具等级/错误工具不掉物；精准分支先于时运，二者不叠加；矿石自身不掉经验，避免另添即时经验奖励（Create 经验颗粒另计）。爆炸掉落采用原版衰减语义。原版铁矿的精准/粗铁/Fortune/爆炸分支见 JAR `data/minecraft/loot_table/blocks/iron_ore.json`；这里只复用策略，不谎称铅锡铀已有该合同。

建议三矿均为 9 粗矿 ↔ 1 粗矿块、可逆工作台压缩/解压；该 **9:1 数量也是待批准候选**。输入按具体粗矿/储存块标签，输出固定本模组标准形态，防止重复循环增殖。普通三矿均基础掉1份，使差异主要由矿脉稀有度承担；锡不复制铜的多份基础掉落，避免配方尚未冻结时意外放大供给。

## 5. Create 原生粉碎事实与最小接续方案

### 5.1 九条已有粉碎配方的实际数据

下表对铅、锡、铀三种矿**完全相同**，产物分别是同矿的 `create:crushed_raw_lead/tin/uranium`。每个输入对应具体标签非空条件，无第三方 `mod_loaded` 条件。所有条目 `processing_time=400`，这是既有参数，不是400秒或固定20秒。

| 输入1份 | 确定产物 | 概率产物 | 现状与差异 |
|---|---|---|---|
| `c:ores/<矿>`（普通/深层） | 粉碎粗矿1 | 额外粉碎粗矿1，75%；经验颗粒1，75% | 无副石料。铅锡文档写“经验颗粒或副石料”，铀文档仅写“副石料” |
| `c:raw_materials/<矿>` | 粉碎粗矿1 | 经验颗粒1，75% | 没有额外75%粗矿输出；不可与原矿石路线混写 |
| `c:storage_blocks/raw_<矿>` | 粉碎粗矿9 | 至多经验颗粒9，每个75% | `count:9,chance:0.75` 逐项抽取，不是75%得到整组9颗；无额外粉碎粗矿 |

推荐**保留九条锁定 Create 原配方，不新增竞争配方**，并请用户选择接受其既有副产物语义，由 PM 更新确实受影响的材料关系。此建议不等于已批准铀矿经验颗粒替代副石料。参考产物数是配方声明/概率，未测实际运行样本，不承诺每次固定得到经验或额外粗矿；时运取得粗矿和精准取得矿石后加工的经济差异应在首轮体验记录。

原生第三方条件洗涤/熔炼并不输出本模组铅锡粒/铀精矿；不能因标签触发粉碎就宣称铀链闭合。沿用旧审计 ZIP 报告 172–174 行结论；本轮不重做全部第三方配方审计。

### 5.2 后续基础加工分批，不在本轮要求全量数值

| 建议接续段 | 可控交付与依赖 | 最早真实门；不能声称什么 |
|---|---|---|
| 铅锡粗矿直熔与压板 | 只做 `raw_lead/tin` → `lead_ingot/tin_ingot` → `lead_plate/tin_plate`，输入具体标签；不引入粒和洗矿 | 粗矿熔炼数量/时间/经验及压板数量先冻结。可从“每粗矿1锭、每锭1板”候选起讨论，但本报告未批准。真实熔炉/压片与标签替代后才有来源证据 |
| 碳粉与钢板 | 原版煤/木炭 → 已批准粉碎轮制两粉；铁+碳粉加热搅拌→钢锭→已有 `steel_plate` | 产率、铁碳配比、热级/时长未定；D-03d 设备分工不重问。只做到钢板，不同时塞密封环/钢网/包壳三条竞争压片。`P1DataContractTest.java:125–149` 旧禁生产断言须定向调整，保留冷热互转/污染禁令 |
| 首机上游零件 | 锡条→工业传感器→辐射传感器；钢锭+Create坚固板/精密构件→重型轴承；石英粉→耐火砖 | 零件数量、序列步骤与半成品、耐火砖多输入工序先明确；仅提供零件不引入辐射功能。复用原生黑曜石粉/坚固板路径，不做同输入第二配方 |
| 专用首机 | 材料可达后，优先把“制造首台富集离心机 + 一批料浆处理并完整保留低浓缩粉/贫化粉/工艺水”作为单独运行切片 | 料浆数量与容器、稳定转速/进度、应力、输出容量、断电恢复先定；不能只做离心机配方和壳。烧结与屏蔽装配各自后续成卡 |

**粒→锭歧义：** `recipes.md:162,172` 的“铅/锡粒或粗矿→熔炼→锭”不能转成单输入 `1粒→1锭`，也不能擅自改成已经批准 `9粒→1锭`。推荐当前直熔小批完全排除粒；到洗矿批时由 PM 明确粒换锭数量及工序，连同洗涤主副产物做守恒闭环。若建议9粒合成1锭，须承认这是对该行熔炼表达的变更，先经用户决定。不要用隐藏回收/压缩循环掩盖问题。

### 5.3 首台设备的自依赖结论

复用旧审计 103–105、199–203 行，并与当前 `recipes.md:329–331` 复核：

- 离心机制造是铜外壳、重型轴承、精密构件、工业传感器；轴承/传感器先于首机，不要求离心产物。坚固板由 Create 黑曜石粉路线取得，不要求核热。
- 烧结炉制造是耐火砖、钢板、烈焰人燃烧室、工业传感器；不要求烧结芯块。D-03b 已允许普通热，但制造时消耗燃烧室不等于机器运行已能读到热源；接入位置、热级判定、断热进度仍待合同。
- 屏蔽装配台制造是铅板、钢板、机械手、辐射传感器；不必先产燃料棒。首发自动化与 P3 `shielded_manipulator` 边界（`content-catalog.md:168,174`）未闭合，允许手动转运不能替代该接口。

因此“未发现材料图必然自循环”可以成立，“首台设备已可启动”不能成立。`reactor_fuel_rod` 制造来源仍是后续完整生存建堆缺口（旧报告113行、F05；当前 recipes 第12节），不用组件/新燃料棒擅自填空。当前不输出整线铁铅锡总数、首组件分钟数、正常运行耗材率或完整闭环结论。

## 6. 提交用户的三组决定（仅建议，等待 PM 组织）

1. **三矿生成首轮档。** 推荐第4.2表：锡12次/size8/Y-16…112，铅6次/size7/Y-48…32，铀2次/size4/Y-64…-16；各为三角形，空气暴露0/0/0.25。替代方向是保留其他候选、将铀调为3次且暴露0，以降低寻找首批燃料的挖矿压力。玩家差别主要是深层探索时间；两档都未实测，不能保证矿量比例。数据可配置/可关/默认去重沿已有规则，不再作为玩法投票。
2. **工具与获取产物。** 推荐锡石镐、铅铀铁镐；普通1粗矿、精准1原矿、Fortune沿铁矿规则；粗矿块9:1可逆、块不受时运。替代方向是铅也允许石镐，其他不变，降低前期顺路采铅门槛；不建议首轮禁铀时运或用铜式多份基础掉落。差别是首次采铅工具门槛，不能把所选数字写成原有合同。
3. **已有 Create 粉碎副产物。** 推荐整体沿用第5.1的九条原配方，接受无副石料及铀经验颗粒，由 PM 同步文档。替代方向是坚持现有副石料关系，另定石料种类/概率并明确覆盖原配方身份、测试重载稳定；不并存竞争配方。推荐项开工面更小、玩家继承原生加工收益，替代项增加待定副产物及覆盖验证。

这三组只批准本批三矿到粉碎粗矿的入口。D-03e、铅锡粒换锭、制钢、传感器和专用设备参数留对应批次，不要求用户一次批准整棵科技树。任一必需选择未明确，保存本报告后停在参数门；不是全项目需求重新审查。

## 7. 建议下一实现卡的精确范围（尚未派发）

建议标题：“三矿可获取入口及既有 Create 粉碎接入”；正式任务 ID 由 PM 分配。本节路径是供 PM 冻结写集的建议，不授予本执行者写权限。第一实现卡只接到粉碎粗矿/可储存粗矿，不做下游洗涤、锭/粒/燃料/设备。

### 7.1 允许范围候选

以下花括号集合为**有限展开清单**，不得解释为任意同目录文件：

- 新 Java：`src/main/java/com/iksxh/create_nuclear_industry/content/OreContent.java`（六矿、三粗矿、三粗矿块注册）；`worldgen/ModOreWorldgen.java`、`worldgen/OreBiomeModifier.java`、`worldgen/OreDimensionFilter.java`（各均相对同一 Java 包根，分别接线、去重/开关、严格主世界过滤）。具体 NeoForge 标签加载与生成配置实现应在卡冻结时说明；如证明不需要某类可删减候选写集，不能另改 P1 核心。
- 修改入口 `src/main/java/com/iksxh/create_nuclear_industry/CreateNuclearIndustry.java`、`content/ModCreativeTabs.java`（后一文件同 Java 包根）；只加本批注册与展示。
- 资源根 `src/main/resources/assets/create_nuclear_industry/`：`blockstates/{lead_ore,deepslate_lead_ore,tin_ore,deepslate_tin_ore,uranium_ore,deepslate_uranium_ore,raw_lead_block,raw_tin_block,raw_uranium_block}.json`；同九名称 `models/block/*.json`、`models/item/*.json`；`models/item/{raw_lead,raw_tin,raw_uranium}.json`；`lang/zh_cn.json`、`lang/en_us.json`。纹理仅限同九名称 `textures/block/*.png` 和三粗矿 `textures/item/*.png`，复用/修正现有素材，不能提前覆盖另一美术候选；像素风格与客户端检查单独留证。
- 数据根 `src/main/resources/data/create_nuclear_industry/`：上述九方块 `loot_table/blocks/*.json`；`worldgen/configured_feature/ore_{lead,tin,uranium}.json`、`worldgen/placed_feature/ore_{lead,tin,uranium}.json`；`neoforge/biome_modifier/add_{lead,tin,uranium}_ore.json`；`tags/worldgen/biome/ore_generation.json`；`recipe/raw_{lead,tin,uranium}_block.json` 与 `recipe/raw_{lead,tin,uranium}_from_block.json`。生成资源数字是可覆盖参数，modifier支持关闭与默认去重；不能只添原生 add_features 遗漏去重。
- 通用标签根 `src/main/resources/data/c/tags/`：`block/ores/{lead,tin,uranium}.json`、`block/ores.json`；`block/storage_blocks/raw_{lead,tin,uranium}.json`、`block/storage_blocks.json`；`item/ores/{lead,tin,uranium}.json`、`item/ores.json`；`item/raw_materials/{lead,tin,uranium}.json`、`item/raw_materials.json`；`item/storage_blocks/raw_{lead,tin,uranium}.json`、`item/storage_blocks.json`；`item/crushed_raw_materials/{lead,tin,uranium}.json`、`item/crushed_raw_materials.json`。父标签明确引用子标签，全部追加。
- 挖掘标签：`src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json`、`needs_stone_tool.json`、`needs_iron_tool.json`；不改变 P1 现有成员。
- 测试：`src/test/java/com/iksxh/create_nuclear_industry/ExtensionOreDataContractTest.java`；`src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionOreGameTests.java`。复用已存在测试结构，若确需新结构只列 `src/main/resources/data/create_nuclear_industry/structure/extension_ore_empty.nbt`。隔离标签替代/关闭数据包与统计证据限定 `build/reports/extension/<PM分配任务ID>/`，其报告为同名 `.md`。

推荐保留 Create 九条原配方时，**不允许写 `data/create/recipe/crushing/`，也不新增本模组三矿 crushing 配方**。如用户选择替代副产物路线，PM 需先冻结覆盖配方的另版精确写集，本清单不能自动扩大。

禁止范围：已有 P1 状态/配置/维修/燃料合同、生产下游、热端/模拟器、核心 docs、构建脚本、`.vscode/launch.json`、另一美术工作树、所有 Git 写操作。这里只建议通过数据包配置矿脉；若 PM 认定已有“可配置”合同还要求 TOML/UI，须在派发前补明确配置入口及精确文件，而不能实现者自行借用 P1ServerConfig。

### 7.2 下一卡验证与停止标准（本轮均未执行）

- 静态：批准表逐项对照、引用闭合、工具与标签成员/父引用、无竞争粉碎、压缩解压守恒、数据包关闭入口。以项目1.21.1路径为准，不复制技能中的旧 plural 路径。
- 自动游戏证据：实际加载注册、正确/错误工具、普通/精准/Fortune边界、粗矿块掉落；原矿/粗矿/块通过真实粉碎轮得到目标产物；重载后选方一致，无第三方仍可到该批出口。模拟标签成员只是受控接口测试，不是真第三方联调。
- 生成证据：在新的测试世界/新生成区块验证三矿、两种替换材质、高度、关闭入口、默认去重与整合包覆盖；若严格主世界，则测试复用主世界群系的测试维度不能生成。固定记录种子、配置、区块范围和有效区块数，多种子按高度统计实际矿块与矿脉样本，不能用 count×size 替代结果。未来采样方案可用3种子各256区块作为起点（方法建议，非已批准产量阈值）。
- 人工：用户在合法工具/生存获取条件下分别发现并挖三矿，检查普通/深层辨识、合理采集压力、Create加工；记录实际发现/获得数量与时间。三种矿出现或自动测试通过不等于平衡体验通过。
- 完成定义限“本批资源入口可达且本批人工门通过”。不宣称洗矿、钢材、首机或全生存生产线完成。生成/兼容实现如果越出卡内技术边界，应报告 PM 补卡，不能绕过默认去重或关闭要求。

## 8. 实际命令、检查结果与未验证项

本轮实际使用 PowerShell 只读命令：`Get-Content`（含逐行编号）、`rg -n`、`rg --files`、`git status --short`、`git rev-parse HEAD`；用 `.NET System.IO.Compression.ZipFile.OpenRead` 读取交接 ZIP 和两 JAR，`StreamReader`/`ConvertFrom-Json` 解析所选条目，`Get-FileHash -Algorithm SHA256` 核对来源并仅写本报告证据目录。原报告条目通过 `SHA256.ComputeHash(entry.Open())` 校验，未解压覆盖旧证据。

单次新增反汇编命令（`<CreateJAR>` 为第2.2节完整路径）：

```powershell
& 'C:/Program Files/Java/jdk-21/bin/javap.exe' -c -p -classpath <CreateJAR> com.simibubi.create.content.processing.recipe.ProcessingOutput
```

输出保存 `EXT-A-START-01/javap-ProcessingOutput.txt`。`Get-Command javap` 未定位到 PATH 命令后，以 `rg --files` 定点找到 JDK 21 的绝对入口；没有修改 PATH 或安装工具。另只读打开 NeoForge 官方1.21.1 biome modifier 文档核对关闭入口。

静态结果：原审计报告哈希匹配；当前锁定 Create 哈希与旧报告匹配；Minecraft JAR 自报1.21.1；提取的九条粉碎配方为同样的按输入分组数值，未发现本模组三矿实现。候选生成值可用当前数据模型表达，这不是自然分布/注册生命周期/运行匹配通过。

未验证：所有新候选数值、去重标签加载时机、生成配置/维度过滤实现、真实世界产量、客户端材质、玩家采矿耗时、真实粉碎运行、第三方兼容、下游材料及机器事务。没有运行 Gradle/JUnit/GameTest/客户端/服务端。未修改任何运行行为、存档、网络、注册、配置或核心文档。本执行者交付后停止，用户门与下一卡由 PM 处理。

交付末检：`git diff --check` 无差异格式错误（Git 另提示既有文件 LF/CRLF 转换，不是本任务写入）；HEAD 仍为 `9669458`。共享主工作区期间观察到 `docs/README.md`、准备计划、01A 卡、启动卡的并行文档修改，均非本执行者写入，未覆盖；连同用户 `.vscode/launch.json` 原改动保留，详见 `EXT-A-START-01/final-static-check.txt`。两份新增 JSON 证据回读成功，九条 Create 配方均 `processing_time=400`，这是静态证据一致性检查。实际写集仅本报告与其同名目录的五个证据文件。
