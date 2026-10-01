# EXT-A-MATERIAL-04 / EXT-ART-06：锡条与传感器实施计划

> 执行方式：PM使用subagent-driven-development分派功能与独立美术任务，执行者使用executing-plans按本卡实施。仓库治理优先：执行者不派发、不做Git写、不改核心文档；代码和冲突修复不能交回PM代写。

**目标：** 通过原版/Create设备制作锡条、工业传感器和辐射传感器，提供完整中英文资源、原生序列装配进度与SVG素材。
**架构：** 复用`BasicMaterialContent`注册和Create原生`SequencedAssemblyItem`/配方/机器；不新增机器、协议、自定义配方类型或生产Mixin。
**技术栈：** MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6，JEI19.27.0.340；不升级依赖。
**需求依据：** [已确认方案](./2026-10-01-mainline-material-04-proposal.md)，用户2026-10-01答复“采用这组推荐参数”。
**状态：执行中；** 前置钢材及03B已验收，尚无本批运行或人工通过证据。

## 全局合同与工作区

- 主工程与同级候选开工均为`8c4526c`。复用`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition` / `codex/ore-acquisition`；其工作树干净，暂无游戏进程。主工程仅既有`.vscode/launch.json`改动，SHA-256 `65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`，禁止纳入本批或覆盖。既有Java21468为Gradle守护进程，不能按此号码盲停任何进程。
- 必读AGENTS、治理、扩展准备计划、本卡和参数方案。实际应用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`；资源/素材另读`minecraft-resource-pack/SKILL.md`。PM使用writing-plans/subagent-driven-development/dispatching-parallel-agents和既有工作树核对流程；Git与制品管理应用minecraft-ci-release。样例必须按锁定版本核对。
- 1锡锭→2锡条；工业传感器：1铁板为基底，依次机械手放1锡条、1红石粉、1电子管，再压片→1成品；辐射传感器：1铅板为基底，依次放1工业传感器、1电子管，再压片→1成品。两序列均1轮、100%成功、无热、无副产物、原生速度；不得增加拆解配方或赋予检测/主动使用功能。
- 金属输入用`c:ingots/tin`、`c:plates/iron`、`c:plates/lead`；锡条用`c:wires/tin`并加入父`c:wires`。红石和电子管分别为`minecraft:redstone`、`create:electron_tube`；工业传感器使用本模组确切身份。标签采用追加语义，不覆盖其他提供者。
- 五个物品ID：`tin_wire`、`industrial_sensor`、`radiation_sensor`、`incomplete_industrial_sensor`、`incomplete_radiation_sensor`。前3者为普通可堆叠成品、加入模组创造页；后2者复用Create原生半成品类（锁定源码将堆叠设为1并显示进度条），不加入模组创造页，不自定义NBT或额外进度协议。
- 显示名：锡条/Tin Wire、工业传感器/Industrial Sensor、辐射传感器/Radiation Sensor、工业传感器半成品/Incomplete Industrial Sensor、辐射传感器半成品/Incomplete Radiation Sensor。配方和模型使用MC1.21.1现有单数数据目录。
- 切石配方为唯一锡条生产配方；机械锯是否接收遵从Create原生`allowStonecuttingOnSaw`，不覆盖全局配置，不再增加相同输入的竞争切割配方。执行者核对锁定原生序列JSON和数据组件，不凭其他版本示例编写。
- 全部手写代码注释/Javadoc中文；保留现有冷却剂8图、全部60张旧游戏PNG、钢材及旧配方/测试。未决玩法或需扩大写集先报PM，不擅自放宽断言、改生产机器行为或转跑后续任务。

## 审查重点

1. 初始铁/铅板和各轮半成品不能混淆；错序或错误输入不得推进进度或吞掉投入。
2. 一轮4步/3步必须准确耗料并只产1件；半成品原生组件和进度条保留，不能靠直接造终态充当真实机器通过。
3. 欠料、断动力、输出受阻时材料须保留在输入、设备或半成品中；恢复后不能丢失或重复产出，不强加Create没有的整线原子事务。
4. 切石机与默认允许切石配方的机械锯真实产量为2；关闭配置的边界与标签重载按原生行为验证。
5. 五个模型/语言/纹理都能打包；SVG导出不改旧60图，客户端JEI、外观和存档重进单独验收。

## 任务1：EXT-A-MATERIAL-04 功能与自动验证

**执行者：** 标准模型，负责多文件原生序列集成与真实机器测试；仅此执行者可运行本批Gradle/游戏。美术执行者不运行Gradle，最终资源打包须等素材交付后再做。

**精确写集（相对候选根）：**

- `src/main/java/com/iksxh/create_nuclear_industry/content/BasicMaterialContent.java`、`content/ModCreativeTabs.java`（后者同包）。
- 新建`src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionSensorProcessingGameTests.java`；如需拆分夹具，仅可新建同包`SensorProcessingGameTestFixtures.java`。不改任何旧GameTest。
- `src/main/resources/assets/create_nuclear_industry/lang/{zh_cn,en_us}.json`仅加5项；`models/item/`下仅上述5个ID的JSON。PNG由任务2负责。
- `src/main/resources/data/create_nuclear_industry/recipe/stonecutting/tin_wire.json`和`recipe/sequenced_assembly/{industrial_sensor,radiation_sensor}.json`。
- `src/main/resources/data/c/tags/item/wires.json`和`wires/tin.json`。
- 报告`build/reports/extension/EXT-A-MATERIAL-04.md`及同名目录中的隔离init、临时诊断/重载夹具、运行输出与精简证据。禁止正式构建、其他源码/资源、默认run、旧报告和docs写入。

**步骤与完成定义：**

- [ ] 核对锁定Create序列示例、组件序列化、机械手/压片/锯的真实入口；报告技术实现选择。复制03B隔离模式到本批报告目录，**同时**设置NeoForge run模型和JavaExec的`gameDirectory`并断言路径，先备份根跟踪日志、记录Java进程和默认run每文件哈希/时刻。
- [ ] 先编写有业务断言的新GameTest并取得RED：缺少物品/配方或错误合同导致失败，不把编译/网络失败当RED；使用项目现有注解与模板，不在延迟任务map的回调内批量注册`onEachTick`。真实机器场景复用已有Create动力夹具模式，必要的测试辅助逻辑只放本批两文件。
- [ ] 按合同注册五身份、创造页3成品、5模型/语言、3配方与2标签。用原生序列半成品和组件，不新增硬编码加工逻辑。
- [ ] 测试至少覆盖：切石菜单实际取料/产2、机械锯真实加工及配置边界；两种完整真实机械手+压片装配的每步扣料、进度和最终数量；错序/错物品拒绝、欠料补料、断电恢复、输出堵塞恢复；半成品ItemStack序列化往返；配方/注册/中英文/标签加载契约。具体分组可合并，不能只测试RecipeManager就声明真实产线通过。
- [ ] 外部标签使用隔离测试数据包进行实际启用→reload→停用→reload；至少验证锡锭或锡条等价输入的false→true→false及一种真实加工结果。临时包不得进入默认客户端或正式资源。
- [ ] 素材完成后，用最终源码依次运行`gradlew.bat test build --rerun-tasks --max-workers=1`和隔离`runGameTestServer --rerun-tasks --max-workers=1`。记录实际JUnit和required总数；历史基线为265/155，不当作本轮结果。通过后Saving worlds若无进展，最多观察60秒，核对创建时间/命令后只终止本轮精确PID，保留汇总与退出码。出现新断言失败要定位，不能盲目重跑或删测试。
- [ ] 核对全部assets/data和最终JAR逐字节一致；预计新增5模型、5PNG、3配方、2标签后279项/65PNG，若实际路径有差异说明原因。默认run前后无变化，停止本轮隔离进程，根跟踪日志交PM恢复。报告命令/退出码、红绿、真实步骤、重载、源与制品哈希、剩余人工项；精简ZIP不含世界、缓存、JAR。

## 任务2：EXT-ART-06 五项SVG素材

**执行者：** 高速模型。与任务1并行，文件写集独立；不运行Gradle或游戏，不改Java、配方、模型、语言。

**精确写集：** `tools/art-assets/sources/item/`下上述5个ID的SVG；`palette.json`、`manifest.json`、`pipeline.py`仅扩充这5项固定白名单/计数/必要说明；`generated/`、`preview.png`、`preview.html`、`previews/`为现有导出器生成结果；`src/main/resources/assets/create_nuclear_industry/textures/item/`下仅上述5个PNG；报告`build/reports/extension/EXT-ART-06.md`及同名目录。若验证脚本存在额外硬编码，报告后PM再扩写集。`tools/art-assets/README.md`由PM同步，不由执行者改。

- [ ] 开工快照60张游戏PNG；读现有SVG/色板/预览，沿用方正轮廓、有限色板和既有工业像素风。锡条应看得出金属条；两成品有明确可区分的工业/辐射识别；半成品表现装配状态，与成品同族。
- [ ] 五张16×16严格整数rect SVG，RGBA透明外圈、alpha仅0/255；不得使用生图模型、外部位图或SVG栅格嵌图。加入原固定白名单，不扩宽任意路径写入能力；历史51项基线及8冷却剂哈希不动。
- [ ] 使用已有捆绑Python/Pillow执行导出/安装/验证，记录重复一致和坏输入失败不写；旧60图逐字节保持，最终65游戏PNG、另1青金石粉工具候选。`verify.py`会短暂修改输入做负例，仅在本任务独占艺术文件时运行。
- [ ] 实际查看五图放大预览，提供易审阅的独立PNG对照、原尺寸和明暗底，报告路径及验证结果；PNG审查不能代替游戏视觉通过。通知PM和功能执行者素材稳定可打包。

## PM审查、版本与人工门

PM保存调度记录于本卡；允许报告目录用作本计划独立ledger/brief/review载体，遵守仓库路径合同，不因技能默认脚本增加其他写集。角色/Git禁令、自动派发授权及人工暂停规则优先于技能示例。

| 预审关系 | 接口/共享风险 | 安排 |
| :--- | :--- | :--- |
| 功能↔美术 | 同5个ID，模型引用PNG；写集不重叠 | 素材独立制作，最终GREEN打包等待素材稳定 |
| 功能自身 | 注册、序列步骤、夹具、实际扣料契约一致 | 原生半成品，真实机器及失败恢复覆盖；单执行者持有Gradle/游戏 |
| 美术自身 | 清单、固定白名单、5源稿、导出65图一致 | 保留60旧图、51基线、8冷却剂；禁止用新基线掩盖变化 |
| 审查↔交付 | 只读证据与最终差异 | 所有审查不运行游戏；报告各用独立路径 |

功能与素材各交付后安排独立规格/质量审查，报告仅`build/reports/extension/EXT-A-MATERIAL-04-REVIEW.md`、`EXT-ART-06-REVIEW.md`；最后整批审查为`EXT-A-MATERIAL-04-FINAL-REVIEW.md`。不要求审查者重复相同验证。实质问题回交原执行者，精确整改、覆盖测试、限定复审；PM不代改代码。

自动门通过后PM归档报告、恢复根日志、提交候选并生成客户端清单。**人工前不合入main、不验收整批、不派发下一批。** 用户在同级候选运行`gradlew.bat runClient`，检查切石/机械锯、两序列和准确数量、JEI/名称/5外观、半成品保存重进及断电/堵塞恢复。main仍保持已验收钢材版本，待本批人工结果后整合；此处沿用既有工作区验收规则，不重复请求派发许可。
