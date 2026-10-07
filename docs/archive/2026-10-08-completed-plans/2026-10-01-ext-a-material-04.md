# EXT-A-MATERIAL-04 / EXT-ART-06：锡条与传感器实施计划

> 执行方式：PM使用subagent-driven-development分派功能与独立美术任务，执行者使用executing-plans按本卡实施。仓库治理优先：执行者不派发、不做Git写、不改核心文档；代码和冲突修复不能交回PM代写。

**目标：** 通过原版/Create设备制作锡条、工业传感器和辐射传感器，提供完整中英文资源、原生序列装配进度与SVG素材。
**架构：** 复用`BasicMaterialContent`注册和Create原生`SequencedAssemblyItem`/配方/机器；不新增机器、协议、自定义配方类型或生产Mixin。
**技术栈：** MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6，JEI19.27.0.340；不升级依赖。
**需求依据：** [已确认方案](2026-10-01-mainline-material-04-proposal.md)，用户2026-10-01答复“采用这组推荐参数”。
**状态：已完成。** 用户于2026-10-01确认本批手动测试全部通过，人工门已解除。候选`2f8e08e`与主线文档在干净候选整合为`65ed661`，主工程已快进到该提交；主工程收尾验证及PM验收见[最终记录](../../reviews/2026-10-01/material-04/ACCEPTANCE.md)。

## 全局合同与工作区

- 主工程与同级候选代码开工基线为`8c4526c`，批准文档提交后实际派发基线为`9295e48`。复用`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition` / `codex/ore-acquisition`；派发时其工作树干净，暂无游戏进程。主工程仅既有`.vscode/launch.json`改动，SHA-256 `65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`，禁止纳入本批或覆盖。既有Java21468为Gradle守护进程，不能按此号码盲停任何进程。
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

- [x] 核对锁定Create序列示例、组件序列化、机械手/压片/锯的真实入口；报告技术实现选择。复制03B隔离模式到本批报告目录，**同时**设置NeoForge run模型和JavaExec的`gameDirectory`并断言路径，先备份根跟踪日志、记录Java进程和默认run每文件哈希/时刻。
- [x] 先编写有业务断言的新GameTest并取得RED：缺少物品/配方或错误合同导致失败，不把编译/网络失败当RED；使用项目现有注解与模板，不在延迟任务map的回调内批量注册`onEachTick`。真实机器场景复用已有Create动力夹具模式，必要的测试辅助逻辑只放本批两文件。
- [x] 按合同注册五身份、创造页3成品、5模型/语言、3配方与2标签。用原生序列半成品和组件，不新增硬编码加工逻辑。
- [x] 测试至少覆盖：切石菜单实际取料/产2、机械锯真实加工及配置边界；两种完整真实机械手+压片装配的每步扣料、进度和最终数量；错序/错物品拒绝、欠料补料、断电恢复、输出堵塞恢复；半成品ItemStack序列化往返；配方/注册/中英文/标签加载契约。具体分组可合并，不能只测试RecipeManager就声明真实产线通过。
- [x] 外部标签使用隔离测试数据包进行实际启用→reload→停用→reload；至少验证锡锭或锡条等价输入的false→true→false及一种真实加工结果。临时包不得进入默认客户端或正式资源。
- [x] 素材完成后，用最终源码依次运行`gradlew.bat test build --rerun-tasks --max-workers=1`和隔离`runGameTestServer --rerun-tasks --max-workers=1`。记录实际JUnit和required总数；历史基线为265/155，不当作本轮结果。通过后Saving worlds若无进展，最多观察60秒，核对创建时间/命令后只终止本轮精确PID，保留汇总与退出码。出现新断言失败要定位，不能盲目重跑或删测试。
- [x] 核对全部assets/data和最终JAR逐字节一致；预计新增5模型、5PNG、3配方、2标签后279项/65PNG，若实际路径有差异说明原因。默认run前后无变化，停止本轮隔离进程，根跟踪日志交PM恢复。报告命令/退出码、红绿、真实步骤、重载、源与制品哈希、剩余人工项；精简ZIP不含世界、缓存、JAR。

## 任务2：EXT-ART-06 五项SVG素材

**执行者：** 高速模型。与任务1并行，文件写集独立；不运行Gradle或游戏，不改Java、配方、模型、语言。

**精确写集：** `tools/art-assets/sources/item/`下上述5个ID的SVG；`palette.json`、`manifest.json`、`pipeline.py`仅扩充这5项固定白名单/计数/必要说明；`generated/`、`preview.png`、`preview.html`、`previews/`为现有导出器生成结果；`src/main/resources/assets/create_nuclear_industry/textures/item/`下仅上述5个PNG；报告`build/reports/extension/EXT-ART-06.md`及同名目录。若验证脚本存在额外硬编码，报告后PM再扩写集。`tools/art-assets/README.md`由PM同步，不由执行者改。

- [x] 开工快照60张游戏PNG；读现有SVG/色板/预览，沿用方正轮廓、有限色板和既有工业像素风。锡条应看得出金属条；两成品有明确可区分的工业/辐射识别；半成品表现装配状态，与成品同族。
- [x] 五张16×16严格整数rect SVG，RGBA透明外圈、alpha仅0/255；不得使用生图模型、外部位图或SVG栅格嵌图。加入原固定白名单，不扩宽任意路径写入能力；历史51项基线及8冷却剂哈希不动。
- [x] 使用已有捆绑Python/Pillow执行导出/安装/验证，记录重复一致和坏输入失败不写；旧60图逐字节保持，最终65游戏PNG、另1青金石粉工具候选。`verify.py`会短暂修改输入做负例，仅在本任务独占艺术文件时运行。
- [x] 实际查看五图放大预览，提供易审阅的独立PNG对照、原尺寸和明暗底，报告路径及验证结果；PNG审查不能代替游戏视觉通过。通知PM和功能执行者素材稳定可打包。

## PM审查、版本与人工门

PM保存调度记录于本卡；允许报告目录用作本计划独立ledger/brief/review载体，遵守仓库路径合同，不因技能默认脚本增加其他写集。角色/Git禁令、自动派发授权及人工暂停规则优先于技能示例。

### 调度记录（2026-10-01）

| 任务 | 执行者与模型 | 实际状态 |
| :--- | :--- | :--- |
| EXT-A-MATERIAL-04 | `/root/material04_impl`，`gpt-6-sol` / high | 已交付：265项JUnit/build退出0，167项required断言全过；真实设备/恢复/重载、279资源与65PNG、默认run180文件未变化均有证据 |
| EXT-ART-06 | `/root/art06_impl`，`gpt-6-luna` / high | 已交付稳定素材及离线PASS报告，65游戏图、旧60图保留；用户随后确认5项外观全部通过 |
| EXT-ART-06审查 | `/root/art06_review`，`gpt-6-luna` / high | 已通过；预览任务标识和一处不符实情的注释2项Minor已修复并定点复审关闭，65PNG修前后不变 |
| EXT-A-MATERIAL-04审查 | `/root/material04_review`，`gpt-6-sol` / high | 规格、质量及最终运行证据补审通过，无开放问题；不替代人工门 |
| 整批终审 | `/root/material04_final_review`，`gpt-6-astra` / high | 已通过，无开放Critical/Important/Minor；核对功能/素材接口、制品与验收范围，不重复游戏运行 |
| 合入后验证 EXT-A-MATERIAL-04-ACCEPTANCE | `/root/material04_acceptance_verify`，`gpt-6-luna` / high | 主工程新跑265项JUnit/build通过、167项required断言全过；279资源/65PNG/10名称PASS，默认run1408文件未变化；保存停滞与非零退出单列 |

模型选择依据：素材沿用现有严格管线，制作、审查与合入后验证优先高速模型；功能涉及原生序列状态、真实多设备恢复及数据包重载，采用标准模型；最终跨功能/素材/制品的一次整批审查采用高级模型。候选GameTest在167项通过后保存停滞，超过60秒核对当轮PID32544后停止，子进程-1、Gradle退出1，不能记为正常退出0；隔离普通服两轮均正常stop退出0。客户端随后获用户完整确认，主工程合入及再次验证另列于本卡末尾。PM保留各轮报告与精简证据；[交接](../../reviews/2026-10-01/material-04/README.md)及[已通过人工清单](../../reviews/2026-10-01/material-04/CLIENT-CHECKLIST.md)为当前入口。

**美术验证写集补充：** 执行者核实`tools/art-assets/verify.py`仍硬编码上一批61清单/60游戏图。PM允许本批仅在该文件更新任务标识、66/65计数、5项新增集合及相应中文说明，保留旧60图、51基线、8冷却剂和坏输入不写断言；不能删除或弱化原校验。该技术调整不改变玩法或美术范围。

| 预审关系 | 接口/共享风险 | 安排 |
| :--- | :--- | :--- |
| 功能↔美术 | 同5个ID，模型引用PNG；写集不重叠 | 素材独立制作，最终GREEN打包等待素材稳定 |
| 功能自身 | 注册、序列步骤、夹具、实际扣料契约一致 | 原生半成品，真实机器及失败恢复覆盖；单执行者持有Gradle/游戏 |
| 美术自身 | 清单、固定白名单、5源稿、导出65图一致 | 保留60旧图、51基线、8冷却剂；禁止用新基线掩盖变化 |
| 审查↔交付 | 只读证据与最终差异 | 所有审查不运行游戏；报告各用独立路径 |

功能与素材各交付后安排独立规格/质量审查，报告仅`build/reports/extension/EXT-A-MATERIAL-04-REVIEW.md`、`EXT-ART-06-REVIEW.md`；最后整批审查为`EXT-A-MATERIAL-04-FINAL-REVIEW.md`。不要求审查者重复相同验证。实质问题回交原执行者，精确整改、覆盖测试、限定复审；PM不代改代码。

候选阶段执行顺序为自动门通过后PM归档报告、恢复根日志、提交候选并生成客户端清单；人工前不合入main、不验收整批、不派发下一批。用户随后在同级候选完成切石/机械锯、两序列和准确数量、JEI/名称/5外观、半成品保存重进及断电/堵塞恢复检查并明确确认全部通过，PM据此完成下述整合与收尾。

## 2026-10-01 人工通过与主工程收尾

用户在收到本批完整客户端清单后答复“手动测试都通过了”，覆盖清单中加工与耗料、中断恢复、两类半成品保存重进、JEI/创造页、5项外观及双语名称。该确认解除本批人工门，不扩展为后续设备或任意第三方兼容验收。

PM先在干净的同级候选合并主线文档，再将主工程快进至`65ed661`；合并后的源码、工具与构建配置同已测候选`2f8e08e`一致，主工程原`.vscode/launch.json`哈希不变。下列收尾验证已经完成，PM按用户完整人工确认、既有独立审查及新验证证据验收本批。

**收尾执行任务 `EXT-A-MATERIAL-04-ACCEPTANCE`：** 高速执行者只负责合入后验证；先读本卡、治理及modding/testing/ci-release技能。目录固定主工程，唯一允许写报告`build/reports/extension/EXT-A-MATERIAL-04-ACCEPTANCE.md`及同名目录中的隔离init、日志、清单和精简证据；Gradle正常构建输出除外。不改源码、测试、正式资源、构建脚本、docs、默认run或旧报告，不做Git写、不再派发。

- 先快照主工程默认run、根跟踪日志和Java进程；复制既有04双层隔离init到新报告目录，只将隔离根改为本收尾目录，同时验证run模型和JavaExec规范路径。
- 串行运行主工程`test build --rerun-tasks --max-workers=1`与隔离`runGameTestServer --rerun-tasks --max-workers=1`，核对本轮265/167实际结果，不用历史数冒充。新失败先保留证据报PM，不自行改代码或盲目重跑。
- 汇总通过后保存无进展最多观察60秒，核对本轮精确PID、创建时间及命令后只停止本轮游戏，区分断言与退出码。保留原守护进程及任何用户客户端。
- 用现成`docs/reviews/2026-10-01/material-04/verify-artifact.ps1`核对本轮主工程JAR的279资源/65PNG/10名称，记录实际JAR哈希；默认run及用户调试配置保持不变，根日志备份交PM恢复。
- 报告与小ZIP包含命令、XML、GameTest、隔离路径和PID、资源与默认run对比，无世界/缓存/JAR。PM复核归档及最终状态，无功能改动不重复既有代码审查。

**收尾结果：** 主工程52套件265项JUnit全部通过，`test build --rerun-tasks --max-workers=1`退出0；隔离GameTest的167项required断言全部通过。汇总后世界保存停滞，执行者观察日志55秒无增加并核对本轮PID35516后结束该游戏进程，子进程-1、Gradle退出1，保留为已知退出限制。279项资源与JAR逐字节一致、65PNG及10项双语名称PASS，PM独立运行现成制品工具复核通过；默认run的1408文件前后路径、长度、UTC时间和SHA256一致，用户launch配置不变。本轮没有修改功能代码、测试或配方。完整证据与主工程JAR哈希见[最终验收](../../reviews/2026-10-01/material-04/ACCEPTANCE.md)。后续耐火材料、重型轴承和设备合同尚需新参数确认，不因本批验收自动实现。
