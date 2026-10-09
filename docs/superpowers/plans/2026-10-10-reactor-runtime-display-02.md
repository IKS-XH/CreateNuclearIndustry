# 反应堆只读运行显示接口L2实施计划

> **执行者：** 使用superpowers:subagent-driven-development。PM派发一个实现执行者、一次合并规格与质量的独立审查；执行者不派发子Agent、不写Git或核心文档。已确认需求及必要显示前置依既有自动授权推进。

**任务ID / 状态：** ART-REACTOR-03-L2 / 自动与独立审查门通过，唯一P2经R1窄复审闭合；净源码提交`ff3bacb`、main `cb123c9`、美术前置同步`4cb0d7f`。实际编译API、精确消费写集及证据见[HANDOFF](../../reviews/2026-10-10/reactor-runtime-display-02/HANDOFF.md)。动画接入与视觉门仍由美术卡独立完成，不启动07或其他主线。

**目标：** 为美术运行动画提供真实结算数据及可靠生命周期，不修改反应堆玩法。

**架构：** 仪表BE成功提交后冻结完整运行信封，经既有BE包/生命周期事件传送；新独立客户端只读索引按当前世界与租约捕获不可变快照。燃料注册仅做noOcclusion显示适配；动画消费者由美术负责人另卡实现。

**技术栈：** MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。

**规格：** [L2合同](../../art/ART-REACTOR-03-INTERFACE.md)；美术树`docs/art/ART-REACTOR-03-DESIGN.md`已由PM实际阅读，必要共享部分由此合同冻结。[L1实物交付](../../reviews/2026-10-09/reactor-surface-display-01/HANDOFF.md)保持。

## 全局边界与隔离

- 复用`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-display`，开工HEAD `c5f29f02e30e2d1f7244c0619d0d53c35a9ca8b3`、分支`codex/reactor-surface-display`。PM已核实git-dir为主工程`.git/worktrees/Create_NuclearIndustry-reactor-display`，common-dir为主工程`.git`，非子模块；开工相关仪表/控制/结构/反应堆/注册与依赖Git内容和main `670ddd3`一致，只净同步任务路径，不整体合并旧树。
- 原有L1未提交PM文档、HANDOFF及logs保留。当前AGENTS/本合同/本卡由PM同步；源实现先只在显示树，经R1审查后已按上方净提交进入main及美术树。不得启动客户端、改写用户世界或清理旧证据。
- 实际读取AGENTS、治理5.1/5.2、合同，以及`C:/Users/IKSXH/.codex/skills/minecraft-{modding,testing}/SKILL.md`、适用TDD/实施/验证技能；按锁定本地API，不升级依赖。中文注释覆盖单位、权威/客户端边界及非显然提交/失效分支。
- 5成功tick心跳、20客户端tick租约为本次显示技术策略；不写配置或影响模拟。协议预算64边长/256列/4096body仅用于防坏包；实际扫描尺寸不变。

## 审查重点

1. 成功但`tickReactor()`返回false仍可用；四种早退和外部setSnapshot撤销显示，不清旧权威/遥测。
2. 中间sendData不能混合新snapshot与旧产热；解码坏包不能续旧显示，重复样本不能续租。
3. 卡死实际≠目标、耗尽tick可用状态≠本次HU/t，以及容量为0均忠实投影。
4. owner与成员区块卸载两种顺序、chunk实例替换、世界切换/旧包均安全撤销；未知/Ponder上下文为空。
5. 新服务端BE不加载客户端类；燃料noOcclusion不改碰撞/选框/结构。

## 精确写集

- 修改`src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`：只增加冻结信封及显示同步/成功失效/生命周期。
- 修改`src/main/java/com/iksxh/create_nuclear_industry/content/P1Blocks.java`：只燃料noOcclusion与相关中文说明。
- 新建`src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorRuntimeDescriptor.java`、`ReactorRuntimeDescriptorFactory.java`。
- 新建同包`client/ReactorRuntimeSnapshot.java`、`ReactorRuntimeSnapshots.java`、`ReactorRuntimeClientEvents.java`。
- 新建`src/test/java/com/iksxh/create_nuclear_industry/structure/ReactorRuntimeDescriptorTest.java`、`ReactorRuntimeProjectionTest.java`、`ReactorRuntimeLifecycleTest.java`。
- 新建`src/main/java/com/iksxh/create_nuclear_industry/gametest/ReactorRuntimeDisplayGameTests.java`及该独立namespace所需的专属empty NBT（如需，使用`data/reactor_runtime_display_probe/structure/empty.nbt`，不改既有模板）。
- 报告只写`docs/reviews/2026-10-10/reactor-runtime-display-02/IMPLEMENTATION.md`；审查者只写同目录`REVIEW.md`；原始日志/XML/API/冻结SHA于`build/reports/art/ART-REACTOR-03-L2/`。

ControlRodDriveBE、L1类/测试/AT、CT、反应堆模拟、其他注册、模型资源、构建/配置、所有其他全局文档只读。若缺必要测试适配或实际签名不符，先报告具体缺口，由PM定范围；不为方便加正式setter。

## 单一实现任务与检查

**输出入口：** 以合同的capture/findOwner/findControlRod/findControlOwner/runtimeDescriptor为准；findControlOwner与同cap的列必须来自同一完整信封，歧义为空，避免客户端插值跨owner/代次延续。完整信封公开字段在报告和javap实物表登记，后续消费者不能猜字段。美术预检提出此最小补充，PM已批准，未增加同步字段/扫描或写集。

- [ ] 写少量行为red：真实Result一致投影，实际/目标卡死、耗尽tick、EMPTY+CONTROL空间/零容量、不可变样本；坏NBT类型/超限/重复/非有限拒收；生命周期旧包/同sample不续20tick租约。保留初始失败，基础设施异常不当作red。
- [ ] 实现信封/工厂、BE成功提交与内部结算作用域、独立客户端会话/索引/心跳。无需在write时计算或保存运行信封到持久NBT；初次加载等待成功样本。按合同保留正式提交顺序，明确失败和成功零变化。
- [ ] 燃料注册窄noOcclusion，真实服务端用例核对碰撞/选框仍完整。
- [ ] 定向JUnit加受影响`ReactorInstrumentTelemetryTest`、`ReactorInstrumentStructureSummaryTest`及增量assemble：`./gradlew.bat test --tests '*ReactorRuntime*Test' --tests '*ReactorInstrumentTelemetryTest' --tests '*ReactorInstrumentStructureSummaryTest' assemble`，退出0；核对XML实际数及0failure/error/skip。
- [ ] 一个真实仪表GameTest覆盖初次不可用、成功完整包、零变化仍有效、早退/外部快照改写只撤L2且保留旧遥测、恢复、拆坏/重建和新owner代次、非持久显示及燃料遮挡属性。使用既有`-PgameTestNamespace=reactor_runtime_display_probe -PgameTestDirectory=build/gametest-runtime-display`筛选入口；独立服加载同时验证commonBE不依赖客户端类。只运行本用例一次，新的断言失败才定向重跑。已知Saving worlds停滞按治理5.1处理自有进程并区分assertion通过和进程退出，不无限重跑。
- [ ] 冻结源路径/SHA、JAR/SHA、实际javap、命令退出/XML及GameTest证据；自审职责/中文注释/写集，提交未提交代码与报告。
- [ ] PM核对，一次独立规格+质量审查读已存在证据，不重跑Gradle或客户端。未改L1的26/26及1/1原证据复用，不重测CT、已通过玩法/Ponder、旧存档或全量。
- [ ] PM完成净Git整合及源/公共API文档实际同步美术树，编写HANDOFF注明精确可消费API/客户端写集/尚待视觉；消费者只在实际交付后开始。L2自动通过不意味着动画/02R1视觉通过。

自审：规格字段由投影任务覆盖，5个审查重点分别由投影/生命周期JUnit与唯一真实仪表用例承接；一个实现任务共享BE与信封，不并行编辑。没有新增玩法取舍或未批准的兼容范围。07装配台教学仍准备未派发，本次不切换工程主线。
