# EXT-A-MATERIAL-05 / EXT-ART-07：石英粉、耐火砖与重型轴承实施计划

> PM使用writing-plans和subagent-driven-development编排任务；功能和美术精确写集独立，按dispatching-parallel-agents并行。执行者使用executing-plans按本卡实施，禁止派发、Git写和核心文档修改。仓库治理及用户已授权的自动派发优先于技能通用流程。

**目标：** 从原版/Create及已验收钢材加工出石英粉、耐火砖和重型轴承，保留原生热级与序列进度，补齐4项SVG素材及双语资源。
**架构：** 使用`BasicMaterialContent`、现有创造页、Create原生磨石/粉碎回退/加热搅拌/序列装配；不新增设备、Mixin、自定义配方类型或NBT协议。
**技术栈：** MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6 / JEI19.27.0.340；许可证及依赖不变。
**规格：** [材料05方案](./2026-10-02-mainline-material-05-proposal.md)，用户2026-10-02答复“确认”；[正式配方](../../recipes.md)第5.4、6节。
**状态：待派发。** 本卡记录实施与审查进度；人工通过前不合入main、不推进后续设备。

## 全局合同与工作区

- 开工代码基线`3d3fbec`。复用同级`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition` / `codex/ore-acquisition`，派发前由PM同步本卡并记录实际HEAD；其工作树干净。主工程原`.vscode/launch.json`保持SHA256 `65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`，禁止覆盖或夹带。
- 必读AGENTS、治理、扩展准备计划、本卡和方案；实际读取`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`，美术另读`minecraft-resource-pack/SKILL.md`。版本/Git与制品核对使用`minecraft-ci-release/SKILL.md`。按锁定版本及现有GameTest注解/单数数据目录实施，所有手写注释/Javadoc中文。
- 1份`c:gems/quartz`磨石制1`quartz_dust`，`processing_time=100`、无副产物。只添加磨石配方，粉碎轮使用原生回退；保留原矿石/闪长岩等Create配方。产物加入具体`c:dusts/quartz`及父`c:dusts`，标签追加。
- 1`minecraft:bricks`+1`minecraft:clay_ball`+1份`c:dusts/quartz`，工作盆普通加热动力搅拌成4`refractory_brick`；`processing_time=100`、`heat_requirement=heated`，无副产物。砖块不是红砖物品，黏土球不是黏土块；输出为材料物品，加入本模组公共`refractory_bricks`标签。不加生坯、熔炉路线或建筑方块。
- 无热/阴燃不足，普通燃烧和超级加热满足最低热级，不新增超热奖励或转速/应力覆写；100不是固定5秒。断热、缺料、输出受阻可暂停/重试，但物料不能消失或复制，不强加Create原生没有的精确进度恢复。
- 1`create:sturdy_sheet`为基底，机械手依次加入1份`c:ingots/steel`、1`create:precision_mechanism`，最后压片成1`heavy_bearing`；1轮、100%成功、无热/无副产物，原生速度。不得改精密构件原有概率或添加逆向拆解。
- 四身份`quartz_dust`、`refractory_brick`、`heavy_bearing`、`incomplete_heavy_bearing`；前三普通可堆叠并加入创造页，最后用`SequencedAssemblyItem`原生单件和进度组件、不入创造页。名称依次石英粉/Quartz Dust、耐火砖/Refractory Brick、重型轴承/Heavy Bearing、重型轴承半成品/Incomplete Heavy Bearing。
- 保留65张旧游戏PNG、历史51项基线和8冷却剂原图。四项新素材用现有严格整数rect SVG及有限色板，16×16、RGBA外圈透明、alpha仅0/255；不使用生图、外部位图或嵌图。新素材只描述材料外观，不赋予设备能力。
- 开工核对实际Java进程并保留用户客户端及既有daemon；当前见21468、464为daemon，24304为VS Code服务，号码只作线索，须核对创建时间和命令。仅功能执行者运行Gradle/游戏；所有server/GameTest使用本批报告下双层隔离路径，禁止默认run写入。

## 审查重点

1. 磨石与粉碎轮实际都产1粉；原矿石可按Create原生配方产石英，不能把“不能直接制粉”误写成完全禁止原生矿石加工。
2. 耐火砖热级、三原料和4件产量准确；红砖物品、黏土块、未制粉石英均不能冒充指定输入。
3. 断热/断动力、缺料和输出受阻保持物料可追踪；恢复后无重复或丢失，输入可能在原生设备或中间态内。
4. 轴承3步正确扣料、保留原生进度，错序/错误物品不推进；保存重进由用户另验。
5. 外部同标签成员实际reload生效/撤销；资源/JAR和旧图保留分别验证，不能仅凭JSON存在宣布整线通过。

## 任务1：EXT-A-MATERIAL-05 功能与验证

**执行者：** 标准模型处理原生热级、真实多设备恢复和标签重载；独占Gradle/游戏。接口：消费任务2四PNG，产出四注册/模型/语言、三配方和粉末/耐火材料标签。

**精确写集（相对候选根）：**

- `src/main/java/com/iksxh/create_nuclear_industry/content/BasicMaterialContent.java`、`content/ModCreativeTabs.java`。
- 新建`src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionRefractoryBearingGameTests.java`；如需拆分夹具仅可新建同包`RefractoryBearingGameTestFixtures.java`。不改旧测试或生产机器行为。
- `src/main/resources/assets/create_nuclear_industry/lang/{zh_cn,en_us}.json`仅加4名称，`models/item/{quartz_dust,refractory_brick,heavy_bearing,incomplete_heavy_bearing}.json`。
- `src/main/resources/data/create_nuclear_industry/recipe/milling/quartz_dust.json`、`recipe/mixing/refractory_brick.json`、`recipe/sequenced_assembly/heavy_bearing.json`及`tags/item/refractory_bricks.json`。
- `src/main/resources/data/c/tags/item/dusts/quartz.json`及已有`dusts.json`仅追加石英粉。
- 报告`build/reports/extension/EXT-A-MATERIAL-05.md`及同名目录下隔离init、临时标签/诊断夹具、日志、快照、校验工具和精简证据。常规build输出除外；不改正式构建、docs、默认run、旧报告或PNG。

- [ ] 读锁定Create源码与现有`ExtensionSteelProcessingGameTests`/`ExtensionSensorProcessingGameTests`夹具。复制04隔离init到本批新目录，修改报告根并同时断言run模型和JavaExec `gameDirectory`；记录Java、默认run文件路径/长度/UTC时间/哈希和根跟踪日志前副本。
- [ ] 为四身份、三工序、准确数量和热级建立新业务断言，获得因缺失内容/错误合同而失败的RED；编译/网络失败不算RED。临时诊断可放报告目录，不改旧测试，不在任务map遍历回调内批量注册`onEachTick`。
- [ ] 按全局合同添加注册、资源、三配方及标签；只用原生机器和进度机制。素材稳定前可以编译/做定点检查，最终制品验证必须等任务2。
- [ ] 实测磨石与粉碎轮各1:1产粉及对应配方选择；检查块/原矿不匹配本批直接制粉配方，保留原Create矿石加工。
- [ ] 实测工作盆准确消耗三原料、产4砖；无热/阴燃拒绝、普通燃烧/超热可完成；错误形态/缺料拒绝，断热和输出堵塞恢复不丢不重。至少一次真实加工而非直接调用配方结果。
- [ ] 实测轴承完整机械手+压片3步，逐步扣料、半成品进度、只产1件；错序/错物品、欠料和断动力恢复。覆盖原生ItemStack组件序列化往返，但不替代人工存档重进。
- [ ] 隔离临时数据包加入外部等价石英粉，执行停用→启用reload→停用reload的false→true→false；启用阶段至少真实加热搅拌成功。错误粉末拒绝，原具体/父标签成员保持。不将隔离包装入正式资源或默认客户端。
- [ ] 素材交付后依次新跑`gradlew.bat test build --rerun-tasks --max-workers=1`与隔离`runGameTestServer --rerun-tasks --max-workers=1`，原始XML/日志和退出码齐备；实际数量另算，不用历史265/167冒充。汇总全过后若停在Saving worlds，观察无进展最多60秒，核对本轮PID/创建时间/命令后仅停止该游戏，区分断言通过、子进程-1与Gradle1。
- [ ] 核对全部源assets/data与最终JAR逐字节一致、四项双语名称、69PNG（预计292资源，若实际不同解释）；默认run前后无差异、无自有游戏残留，根日志前后备份交PM恢复。报告和精简ZIP包含实际命令、红绿、机器/重载、XML、PID及哈希，不含世界/缓存/JAR。完成后停止执行。

## 任务2：EXT-ART-07 四项SVG素材

**执行者：** 高速模型；不运行Gradle或游戏、不写Java/配方/模型/语言。接口：四ID与任务1完全一致，输出同名游戏PNG并通知PM素材稳定。

**精确写集：** `tools/art-assets/sources/item/{quartz_dust,refractory_brick,heavy_bearing,incomplete_heavy_bearing}.svg`；`palette.json`、`manifest.json`、`pipeline.py`、`verify.py`仅扩展四项固定白名单/计数/本批验证及中文说明；`generated/`、`preview.png`、`preview.html`、`previews/`为现有导出器输出；游戏`textures/item/`下仅四ID PNG。报告`build/reports/extension/EXT-ART-07.md`及同名目录。工具README由PM维护。

- [ ] 开工快照65张旧游戏PNG并核对既有风格。石英粉浅色晶粒、耐火砖方正砖形、轴承呈坚固金属结构，半成品明显未完成且与成品同族；只使用全局SVG合同。
- [ ] 增加四源稿/色板/清单和必要硬编码计数，最终70清单/69游戏PNG/66SVG；不扩大任意路径写入能力，不改51历史基线或8冷却剂保护断言。保留青金石粉工具候选game=null。
- [ ] 用现成捆绑Python/Pillow导出/安装/验证，核对重复字节一致、65旧图不变、坏输入失败不写。`verify.py`负例短暂改输入，仅在本执行者独占美术文件时运行；记录实际命令及结果，不删减旧负例。
- [ ] 提供四图原尺寸和明暗底放大PNG对照并实际查看，报告确认不缺图、可区分；保存开工哈希及验证证据，通知素材稳定供任务1打包。离线预览不等于游戏视觉验收。

## PM审查、版本及人工门

本卡兼作独立ledger；任务报告目录保存brief/差异包/审查，不增加技能默认额外目录。执行者均不做Git写或派发；PM不改实现代码。玩法缺口先停相关范围报PM，已批准内容不重复设决策门。

| 调度 | 状态与证据 |
| :--- | :--- |
| 开工预审 | 两任务只共享四ID，写集不交叉；最终功能打包等待美术稳定。热级与缺料/阻塞要求以原生保料可恢复为准，不增加精确进度或整线原子性要求 |
| 功能/美术 | 待派发；实际agent及HEAD由PM记录 |
| 独立审查 | 各交付后分别核对规格与质量，不重跑同代码已通过的测试；报告`build/reports/extension/EXT-A-MATERIAL-05-REVIEW.md`、`EXT-ART-07-REVIEW.md` |
| 整批终审 | 分项通过后核对接口、最终打包和证据边界；报告`build/reports/extension/EXT-A-MATERIAL-05-FINAL-REVIEW.md` |
| 人工门 | 候选自动检查及审查通过后，PM归档并提供完整客户端清单；此时停止自动推进 |

素材及常规审查优先高速模型；功能涉及新加热盆真实生命周期和多设备恢复，采用标准模型提高可靠性；整批跨任务审查按实际复杂度选择。发现整改交原执行者，限定修复及复审；不由PM代写。

PM审核后只提交本批功能/素材与报告，保留用户launch和存档。人工清单覆盖真实制粉/热级/耗料、轴承装配和恢复、半成品保存重进、JEI/名称/4外观；用户通过后才整合main。后续设备合同未确认，不能顺带实现。
