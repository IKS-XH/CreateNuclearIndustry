# 高压锅炉思索实施计划（三情景定稿）

> **For agentic workers:** 使用 `superpowers:subagent-driven-development`；本卡由项目经理派发执行者实现。执行者禁止 Git 写操作和核心文档修改。用户已确认四情景并要求开始，不重复请求计划批准。

**Goal:** 为现行分区温压高压锅炉提供三个可独立播放的教学情景，帮助玩家搭建、接管和调压。用户已确认前三幕播放效果，删除重复的“停机与排查”幕。
**Architecture:** 独立 `BoilerPonderScenes`由现有Ponder插件挂接九种已注册锅炉部件，定稿只保留三份完整NBT模板及生成入口。仅操作Ponder客户端临时世界，复用原有资源、方块和交互，不改生产机制。
**Tech Stack:** Minecraft 1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；版本和依赖不变。
**Spec:** 用户于2026-10-08批准四情景方案并明确“好，开始吧”；现行玩法以 [锅炉重构](./2026-10-07-ext-b-boiler-rework-01.md)、[底层热口](./2026-10-07-boiler-hot-inlet-layer-fix.md)、[双汽库存01F](./2026-10-07-boiler-dual-steam-inventory-01f.md)及其实际实现为准。

## 任务合同

### R3：移除重复教学并收尾（2026-10-08，当前合同）

用户明确“删掉停机与排查这一页吧，该说的前面几页都说了，其他的都挺好”。前三幕搭建、接通运行、蒸汽输出调压按本次用户反馈记录播放通过；第四幕不再属于交付范围，不重复要求播放。此条优先于下方初版四幕及R2待播放描述，后者保留历史含义。

- **实现：** 在同级候选移除第四故事板绑定、场景方法及仅其使用的常量/import、生成器分支、`high_pressure_boiler_shutdown.nbt`和对应中英文Ponder键；更新现有合同测试的故事板列表/数量/分支及相关中文注释。前三幕、R2时序和其他设备教学不得改写，不把第四幕内容重新塞入前三幕。
- **写集：** `BoilerPonderScenes.java`、`P1PonderPlugin.java`、`BoilerPonderContractTest.java`、`tools/ponder/boiler_scenes.py`；删除第四幕NBT；两语言仅删除第四幕键；执行者追加本批 `implementation.md`，审查者追加 `review.md`。PM负责其余文档、状态与Git。
- **验证：** 三份剩余NBT字节不变，核对九入口只有三个故事板、没有第四幕引用和翻译；只跑原 `BoilerPonderContractTest` 四项一次，不新增镜像测试或重新生成未变的三模板。候选不重复assemble，最终合入main后由执行者做一次增量assemble和三模板/双语/场景class的必要封包核对；不得全量、clean、GameTest或启动用户客户端。
- **技能：** 实际应用minecraft-modding、minecraft-testing及receiving-code-review，版本仍MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10/Ponder1.0.82。执行者禁止Git写、核心文档修改和子派发。
- **收尾：** 独立审查仅看R3删减差异和已有证据；PM无冲突整合本台已验收三幕及R1/R2修正至main，同步入口/验收记录。前三幕人工证据来自用户，不冒充代理实际播放；删除一幕不另设人工门。不自动接下一台教学或其他主线。

### R2：首次进入思索崩溃整改（2026-10-08）

用户报告“对着锅炉方块按w游戏崩了”。当前候选崩溃报告为 `run/crash-reports/crash-2026-10-08_03.06.39-client.txt`：`PonderUI.tick` → `hideSection` → `WorldSectionElementImpl.erase`，基础区段的 `section` 仍为null。锁定Ponder1.0.82源码确认，`showSection`淡入15tick后才合并到基础区段；搭建幕开场紧随`showBasePlate`调用`hideSection(whole)`，尚未完成基础区段初始化。这是本台教学缺陷，不涉及锅炉生产逻辑。

- **派发范围：** 执行者在原候选内只修正 `BoilerPonderScenes.java` 的区域显隐与初始化顺序，检查同文件其余显隐操作是否符合锁定Ponder生命周期。模板默认不可见，不以捕获空指针、改第三方库或修改正式世界规避错误。
- **画面约束：** 仍逐层搭建；注意模板锅炉底部位于Y=0，与基础板选择相交。避免在基础板淡入未完成时擦除区段，也避免同一底层重复淡入导致重叠。
- **最小验证：** 允许在 `BoilerPonderContractTest.java` 新增针对显隐初始化/时序的回归，必须能在本次故障版本失败并在修正后通过，不能仅检查方法名/资源存在。优先直接验证实际指令或锁定API约束；如普通JUnit无法驱动客户端，清楚记录替代检查的范围，不把静态检查称为实际播放。
- **写集：** 上述两份代码文件；执行者追加 `docs/reviews/2026-10-08/boiler-ponder-03/implementation.md`，审查者追加同目录 `review.md`；新证据写 `build/reports/extension/DEVICE-PONDER-03-BOILER-R2/`。其余原写集本次不默认修改。
- **执行要求：** 必读本卡、AGENTS、治理5.1/5.2及 minecraft-modding、minecraft-testing、systematic-debugging、test-driven-development 技能。执行者禁止Git写、核心文档修改及子派发。先建立失败证据，再修复，一次定向测试及一次增量assemble即可，不全量、clean、旧档测试或启动用户客户端。
- **审核与交付：** PM核对源码/锁定Ponder实现及定向证据，由原审查者仅复核本次崩溃整改；修正进入同级候选后，用户重新按W并继续原四幕清单。未通过播放不合入main，不接其他任务。

- **任务ID：** DEVICE-PONDER-03-BOILER。
- **状态：** 已完成；前三幕获用户播放认可，R3删页`064fae9`定稿与R2修正已整合main`58a965e`，一次定向检查及唯一main增量打包通过。[验收记录](../../reviews/2026-10-08/boiler-ponder-03/ACCEPTANCE.md)关闭本批，原失败与历史证据保留，不重复人工门、不自动接下一项。
- **维护者：** 用户任命的现任项目经理；实现和合并规格/质量审查分别由执行者承担。
- **前置：** 锅炉REWORK-01～01F集中手测与main合入已完成；离心机、反应堆教学已独立通过，不重复验收。
- **执行根：** `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支 `codex/ore-acquisition`，基准 `b18c41c`；main当前 `b04a37c`。两根源码、思索工具及项目文档相同，使用已有同级候选，不创建新工作树。
- **既有改动：** 候选 `logs/debug.log`、`logs/latest.log` 和三个工具 `__pycache__` 保留，不暂存、不删除。不改用户世界及运行配置。
- **人工门：** 前三幕按本次用户反馈关闭，第四幕撤销；R3删减不另建播放门。完成收尾后不自动派其他设备或主线。

## 必读与技能

阅读 `AGENTS.md`、`docs/project-governance.md` 第4、5.1、5.2节、本卡及上述三份现行锅炉合同；对照 `BoilerStructure`、`BoilerState`、`BoilerControls`、`BoilerControllerBlockEntity`、`BoilerPortBlockEntity` 与 `BoilerConfig` 的实际规则。

必须实际读取并应用：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：现有客户端生命周期、注册归属与当前版本 API。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：受影响入口/模板合同与一台播放门，按治理5.1精简，不照搬技能中的全量示例。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/subagent-driven-development/SKILL.md` 的实现流程；执行者不派子代理，审查与Git由PM统一安排。

沿用现有 `CentrifugePonderScenes`、`P1PonderScenes`、`tools/ponder/reactor_scenes.py`、`centrifuge_scene.py` 的资源格式与客户端临时演示边界。交付报告列出实际应用点。

## 允许写集

仅允许以下功能文件及本批报告：

- 新建 `src/main/java/com/iksxh/create_nuclear_industry/ponder/BoilerPonderScenes.java`。
- 修改 `src/main/java/com/iksxh/create_nuclear_industry/ponder/P1PonderPlugin.java`：仅新增锅炉入口与四故事板及相关中文注释，保留现有离心机和反应堆绑定。
- 新建 `tools/ponder/boiler_scenes.py`。
- 新建 `src/main/resources/assets/create_nuclear_industry/ponder/high_pressure_boiler_build.nbt`、`high_pressure_boiler_operation.nbt`、`high_pressure_boiler_steam.nbt`、`high_pressure_boiler_shutdown.nbt`。
- 修改 `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json` 与 `en_us.json`：仅增加本批四情景的 `ponder` 条目，不重排整文件、不改其他翻译。
- 新建 `src/test/java/com/iksxh/create_nuclear_industry/BoilerPonderContractTest.java`，只检查本批资源与入口合同；不扩大原有测试范围。
- 新建 `docs/reviews/2026-10-08/boiler-ponder-03/implementation.md`、`review.md`；执行者仅写自己的报告，不改本卡状态或其他核心文档。
- 原始证据 `build/reports/extension/DEVICE-PONDER-03-BOILER/`。

禁止修改锅炉/汽轮机/换热器运行代码、配置、配方、方块模型、美术、现有教学场景/模板/工具、世界存档、构建脚本、依赖和许可证。不研究旧存档兼容。出现确需越界的具体障碍，保存证据并交PM处理。

## 全局约束

1. 中文与英文玩家文案聚焦锅炉操作和特有规则，短句逐条讲解；删去“画面仅作示意”“创造马达只是示意”“管道本身不会泵送”等开发说明或原版基础知识。Java英文fallback与en_us一致。
2. 同时最多一段正文。Ponder正文实际寿命为参数时长+10tick，相邻正文另留至少10tick净间隔；关键步骤建立可回放关键帧，避免长段文字挡住设备。
3. 默认镜头同时看清主要端口与管路，使用露面、切层、颜色和箭头辅助，不将冷热口藏在后方；水、冷热液和两种汽各有清晰、互不穿壳的去向。
4. 泵、轴、齿轮方向与速度可演示，但须构成自洽外接动力，不能让管路/动力漂浮或穿过炉体。成型展示使用合法完整结构；剖视隐藏原有壳层，不把违法缺块结构描述为完整成型。
5. 临时填罐、窗口液位、压力/状态演示只限客户端教学；不调用服务端热工/库存交易、不修改正式世界。技术说明只放中文注释与报告。
6. 所有新增或修改手写代码与非显然演示算法写准确中文注释；无GUI，不新增设备、机制、事故或辐射演示。

## 初版四情景与玩家知识（第四幕已由R3撤销）

四个story ID / Java入口分别为：

| Story ID | 方法 | 中文标题 |
| :--- | :--- | :--- |
| high_pressure_boiler_build | highPressureBoilerBuild | 高压锅炉：搭建与分区 |
| high_pressure_boiler_operation | highPressureBoilerOperation | 高压锅炉：接通并运行 |
| high_pressure_boiler_steam | highPressureBoilerSteam | 高压锅炉：蒸汽输出与调压 |
| high_pressure_boiler_shutdown | highPressureBoilerShutdown | 高压锅炉：停机与排查 |

### 1. 搭建与分区

逐步展示底框、底部换热器、隔层及上下炉腔、外壳/观察窗、控制器和各类端口、安全阀。

- 长宽高可在配置范围内分别调整，默认各5～11格；示例采用清晰的合法小锅炉，不暗示固定5³。
- 边框由外壳组成，唯一例外是底边非角点的热液入口；底面非边框格放换热器，不能演示底外另加一台。
- 内部完整隔层由再加热段/外壳构成，下方水区、上方汽区，至少各留一层；有效热力回路数量取换热器与再加热段数量中较小值。
- 热液入口与底部换热器同层、允许底边非角点；冷液出口与隔层同层非棱边；给水口/控制器在水区侧面，蒸汽口在汽区侧面，水平接口均朝外；顶部安全阀上方留空。
- 通过多口实物展示说明可布置多个给水、汽口和冷热口，合法层与非棱边限制仍适用。

### 2. 接通并运行

展示可见侧给水、底层热液输入、隔层冷液回收、外接储罐与完整泵动力，剖视窗口/炉腔随填水与供热变化。

- 先给水并接通冷热液循环，再持续供热；堵住冷液回路会影响继续收热。
- 水区和汽区容积分别决定缓存量；相同供热/进汽条件下，水区越大升温越慢，汽区越大升压越慢；不写固定预热秒数。
- 升温汽化和再加热是先后相连的过程，状态可由护目镜观察；不演示未实现的辅助热源。

### 3. 蒸汽输出与调压

可见侧至少两个汽口接各自独立管路和储罐；展示在端口使用Create原生汽种选项、在控制器设置压力下限的交互。

- 新产汽按实际温压归入蒸汽或超临界蒸汽；两种库存分别保存，共用汽区总容量和炉压，已有汽不因调压自动换种。
- 每个汽口只取所选库存，不降级转换；两种汽分别接管，避免不同流体堵住同一管路。
- 控制器设置0～100的“出汽压力下限”；它限制正常出汽，不是目标炉压，也不是安全阀阈值。
- 已有热量合格的超临界蒸汽可排至公共下限；不同库存不会互相占满整个容量之外的额外空间。

### 4. 停机与排查

分开演示给控制器红石、停止新收热但余热仍可产汽，随后演示缺水、出汽堵塞及解除故障，安全阀在高压时泄放且顶部须畅通。

- 停机后保留给水、冷液回收和出汽通路处理余热，不暗示红石立即清零温度/压力。
- 缺水检查给水口，收热停止检查热液供应及冷液回收，积汽检查出口汽种/去向和压力下限。
- 安全阀排汽会损失工质；不演示事故、爆炸、辐射或新故障机制，不给温压控件编造目标压力自动调节。

## 入口与资源契约

九个锅炉部件均挂接R3保留的前三故事板，保持顺序：`high_pressure_boiler_casing`、`high_pressure_boiler_window`、`high_pressure_boiler_water_port`、`high_pressure_boiler_steam_port`、`high_pressure_boiler_hot_coolant_port`、`high_pressure_boiler_cold_coolant_port`、`boiler_safety_valve`、`boiler_heat_exchange_section`、`high_pressure_boiler_controller`。不纳入未实现烈焰加热口，不提前接换热器独立教学。现有11个反应堆入口、四故事板及离心机绑定保持原样。

生成器定稿可重复生成三模板并检查NBT尺寸、唯一位置、palette/state索引与合法部件层位/外向方向，以及管路不占炉腔。报告记录每幕布局、文字顺序与等待时序；测试检查绑定到存在模板、双语key齐全及结构布局，不写逐句镜像或全量负例。

## Review Focus

- 冷热液在镜头后侧、汽种共管、泵朝向反向、管线穿壳：读实际模板布局与镜头选择，人工逐幕播放确认。
- 错误沿用旧固定锅炉/全炉自动换汽文案：对照01F、Structure与Controller实际行为。
- 正文相互遮挡或长字幕遮住控制器：静态时序核对，人工正常播放及关键帧回放。
- 模板非法方块属性、palette缺条目、旧故事板回归：生成器校验、定向合同测试与实际播放。
- 默认参数写成永远固定/红石立即停热：文案区别配置默认和现行机制，不补新玩法。

## 实施与精简验证

- [x] 先核对现行API、合法结构、双汽账本和原生控件；记录四幕可见侧布局。
- [x] 实现独立场景、四模板生成工具、双语文案及九入口绑定，保留现有教学。
- [x] 生成并校验四份NBT，核对字幕寿命与关键帧，建立一个必要资源/绑定合同测试。
- [x] 唯一构建执行者运行定向 `test --tests '*BoilerPonderContractTest' --tests '*P1Ponder01ContractTest'` 和最终一次增量 `assemble --console=plain`，保存原始日志、退出码与JAR。若新变化/失败，只补对应检查；不跑GameTest、clean或热端全量。
- [x] 一轮独立合并规格/质量审查，读差异、模板和已有证据，不重复运行测试；必要整改后仅复查实际差异。
- [x] PM核对写集、证据和打包资源，保存中文候选提交及交付说明。
- [x] 用户认可前三情景播放，要求删除重复第四幕；前三幕按总体反馈关闭播放门，未另附逐项操作日志，不冒充代理播放。
- [x] R3删减、定向核对、main整合及唯一增量打包完成；PM读取实际日志/制品并记录最终版本。

## 交付

报告包括实际技能应用、文件清单、四幕默认镜头/接口布局、文字起止时序、临时客户端状态边界、必要验证命令/退出码/结果、JAR及证据位置、仍需人工验证的视觉范围。区分静态/编译通过与用户播放通过。

前三幕已通过播放；当前按R3删减后合入main，不因纯删页重复人工门。候选和用户世界保留，不自动开始下一台。

## 初版/R1候选交付（2026-10-08，历史证据）

功能提交 `c113447`，见[交付入口](../../reviews/2026-10-08/boiler-ponder-03/README.md)、[实施及R1](../../reviews/2026-10-08/boiler-ponder-03/implementation.md)、[独立审查及复核](../../reviews/2026-10-08/boiler-ponder-03/review.md)、[唯一播放清单](../../reviews/2026-10-08/boiler-ponder-03/PLAYBACK.md)。初版9项定向检查通过；审查发现剖视返回值、故障时序、调压演示、实际NBT占位验证及启动顺序五项问题，已在同批修复，R1仅新跑锅炉3项，未变P1六项复用。最后0～100文案补回仅静态核对和增量打包，不重跑JUnit。原失败与制品保留，未启动客户端。

最终JAR为 `build/reports/extension/DEVICE-PONDER-03-BOILER-R1/create_nuclear_industry-0.1.0-R1-final.jar`，2,283,254字节，SHA-256 `A5EA3E8B371FB542CB171B4017392AF58981A2B37CA43347E198AF2FBD33FD3A`。PM读取最终构建退出0/5秒、R1 XML三项零失败，以及10/10封包资源/编译class逐字节一致证据。生产代码、现有教学、模板基线及运行配置未改；本台等待用户四幕集中播放确认，不接下一台或其他主线。
