# 反应堆成型表面显示接口实施计划

> **For agentic workers:** 用户任命的PM依`superpowers:subagent-driven-development`派发一个实现任务及一轮独立审查；执行者只交未提交改动。仓库权限、精简验证与首发前存档边界优先于技能中的提交/全量测试步骤。

**任务ID / 状态：** ART-REACTOR-02-L1 / 待实际派发；本卡与美术02A独立执行，不改变换热器五幕待播放门。
**Goal:** 交付美术CT接入需要的服务端权威表面描述与模型线程安全只读快照，成型可连接、失效及卸载及时撤销。
**Architecture:** 从现有扫描缓存投影表面，搭载仪表现有BE更新；客户端按世界会话发布不可变成员索引并刷新受影响区段。不增设外壳BE、不另造结构扫描、不改反应堆运行规则。
**Tech Stack:** Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，锁定依赖不变。
**Spec:** [接口合同](../../art/ART-REACTOR-02-INTERFACE.md)及美术工作树[已确认设计](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/ART-REACTOR-02-DESIGN.md)。

## 授权、隔离与前置

- PM已直接读取美术负责人对话`01a11c6b-f440-77c1-9275-5b2dca5cfa0f`：用户在turn `01a11cba-0c75-73a2-95c8-db1eaf07f650`要求成型连接纹理并考虑可变尺寸，在turn `01a11e70-db53-7bb3-a524-a04490e9d941`回答“好”确认设计与接口协调。接入所必需的显示状态归逻辑侧，按既有美术并行授权及PM自动派发规则实施，不是收到其他Agent消息即自行扩展授权。
- 换热器五幕`f4aa143`仍待用户播放，既有功能及教学人工门保持；本任务是已批准美术的独立前置，无需换热器通过才能实施。显示接口自动门与最终CT美术接入/用户视觉门分别记录，不宣布美术已完成。
- 现行反应堆固定5×5×5；仅投影适配层使用现有`ReactorStructureDefinition.SIZE`。不实现可变尺寸玩法、配置、热工、流体或燃料改造。
- 工作目录`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-display`，分支`codex/reactor-surface-display`，由PM从当前main及本任务文档创建；不写美术树或换热器候选。用户指定同级目录，原生创建工具无目录参数，故PM按明确目录偏好使用Git工作树。
- 必读根AGENTS、治理1.2/5.1/5.2、本卡、接口合同；实际使用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`，核对实际API。报告记录使用、HEAD、既有脏文件；所有手写注释/Javadoc中文。
- 执行者不得Git写、派发子Agent或改治理/核心文档。PM不写Java、测试、构建或模拟器代码；具体技术缺口先报告PM确定最小扩展写集，不重复询问已批准需求。

## 一个完整任务：表面描述、同步与客户端索引

Java路径以`src/main/java/com/iksxh/create_nuclear_industry/`为根，允许写集如下：

| 文件 | 唯一职责 |
| :--- | :--- |
| `blockentity/ReactorInstrumentPortBlockEntity.java` | 新增展示描述生成、BE更新读写、加载/失效通知；原摘要、热工/燃料/端口绑定/护目镜行为保持 |
| `structure/ReactorSurfaceDescriptor.java` | 不可变描述、严格受限NBT编解码、身份/边界/成员校验；未知/错误返回不可用 |
| `structure/ReactorSurfaceDescriptorFactory.java` | 已有有效扫描结果投影实际表面；不重新决定成型、不强制加载区块 |
| `structure/ReactorSurfaceSyncEvents.java` | 不引用客户端类的展示通知桥，保持专用服务端隔离 |
| `structure/client/ReactorSurfaceSnapshot.java` | 不可变查询数据、结构身份和外面归属，无Level/BE/NBT引用 |
| `structure/client/ReactorSurfaceSnapshots.java` | 游戏线程更新状态与原子快照发布，过期/revision去重、歧义归属降级 |
| `structure/client/ReactorSurfaceClientEvents.java` | 独立客户端事件接入，世界/owner/成员区块生命周期及发布后的有限重建 |

允许新增测试：`src/test/java/com/iksxh/create_nuclear_industry/structure/ReactorSurfaceDescriptorTest.java`、`ReactorSurfaceSnapshotTest.java`、`ReactorSurfaceLifecycleTest.java`；允许新增`gametest/ReactorSurfaceDisplayGameTests.java`及如确有需要的专属空模板`src/main/resources/data/reactor_surface_display_probe/structure/surface_probe_empty.nbt`。不修改既有测试、构建/注册/共享入口、其他资源。报告仅`docs/reviews/2026-10-09/reactor-surface-display-01/IMPLEMENTATION.md`；原始证据在`build/reports/art/ART-REACTOR-02-L1/`。

**产生的接口：** 包`com.iksxh.create_nuclear_industry.structure.client`；`ReactorSurfaceSnapshots.capture()`及模型消费必用的`capture(BlockAndTintGetter)`返回不可变`ReactorSurfaceSnapshot`；`ReactorSurfaceSnapshot.findSurface(BlockPos, Direction)`返回`Optional<ReactorSurfaceSnapshot.Member>`。上下文入口必须证明当前真实世界会话，不依赖仅匹配block/坐标；未知/Ponder/旧世界任务降级空快照。Member供美术读取dimension、ownerPos、ownerGeneration、revision、origin、maxInclusive及expectedBlockId。具体字段类型和公共签名在报告中列编译后实物，不能仅交设计文档。所有坐标防御复制，集合不可变，空快照不使用null。

**关键不变量：** 同owner实例的revision只因valid/bounds/members改变递增；同坐标重放的owner有新generation；重复热工遥测不重复大范围重建。外表面成员包括当前合法壳体/窗口/端口/顶部功能件而排除内部棒体；棱角可对应多个真正朝外的面。数据能表达独立长宽高，客户端不硬编码5。无可靠当前owner或任一必需成员区块未加载时整台索引撤销，可靠加载证据重齐后恢复；不同owner的重叠归属不能择一猜测。

描述只随服务端有效扫描缓存变化生成；服务端包有明确防御大小上限及几何/重复/owner校验，上限属于传输安全预算，不能据此改合法尺寸。客户端只能读，不新增C2S更改成型。发布新快照后重建旧/new bounds并集及外扩一格的相关区段；重复描述不重建，未加载处不强制加载。

模型工作线程的一次构建只捕获一次快照。必须核对当前Create模型构建上下文与客户端世界会话的关联，不能让新世界相同坐标索引用于旧世界尚未结束的构建。美术02B预检已明确这一问题，并可从覆写的`gatherModelData`提供`BlockAndTintGetter`；本卡新增带上下文入口，不把尚未查证的ModelDataManager/RenderChunkRegion示例当成可行结论。先审计锁定API，若当前写集仍不能可靠关联，报告具体缺口与最小扩展；不自创扫描/外壳BE/mixin或修改CT消费者。BE更新早于`onLoad`、拆除owner、成员块卸载/重载、世界切换和乱序旧revision都须处理。收到未知/坏数据降级单块，不能无限刷新。

## 实施与必要证据

- [ ] 审计当前BE更新、NeoForge区块/世界事件及模型线程API，指出可行恢复来源；先写失败用例再最小实现，记录真实失败，禁止只查源码字符串代替行为测试。
- [ ] 单元用例覆盖NBT往返及非法/巨大/重复成员；5×5×5和假设6×5×8、9×7×5各面/棱角几何；相邻两owner/歧义、不可变历史快照；revision/新generation/旧数据拒绝；owner/成员块卸载后撤销及可靠重载恢复；世界切换后旧会话不能复活。假设尺寸不进入游戏成型扫描。
- [ ] 最小实现描述投影和BE传输，保持原热工及护目镜合同；完成客户端事件、发布/刷新顺序与加载先后处理。服务器不能因公共BE新增字段加载Minecraft客户端类。
- [ ] 新增一个或两个真实仪表GameTest，使用隔离namespace `reactor_surface_display_probe`；直接搭现行合法结构，验证更新包实际描述、相同扫描revision稳定、拆坏失效/恢复及owner重放身份变化。模板优先复用现有空模板；若跨namespace限制则复制专属空模板。不得使用玩家存档或新增Probe注册设备。
- [ ] 一轮定向JUnit与增量assemble：`./gradlew.bat test --tests '*ReactorSurface*Test' --tests '*ReactorInstrumentStructureSummaryTest' --tests '*ReactorStructureLifecycleContractTest' --tests '*ReactorInstrumentGoggleDisplayTest' assemble`。保存日志、退出码和XML，明确测试数，不能把零用例算通过。
- [ ] 一次定向服务器：`./gradlew.bat runGameTestServer -PgameTestNamespace=reactor_surface_display_probe -PgameTestDirectory=run/reactor_surface_display_probe`。实际执行数和断言通过须记录；专用服务端启动同时验证客户端隔离。GameTest不证明客户端贴图或线程生命周期已经人工通过。
- [ ] 实现者交报告及未提交差异：API签名、schema/安全预算、revision/会话生命周期、实际源API依据、验证命令/日志/XML/JAR哈希、余留客户端验证范围。如已知Saving worlds退出停滞，保留断言日志与本轮进程身份，不机械重跑。
- [ ] PM确认冻结差异后派独立审查者一次检查规格与质量，不重复运行测试；只整改具体阻断。无全量build/旧存档验证/各设备玩法复测。
- [ ] PM仅在自动与审查门通过后记录可消费API，管理净代码提交/集成并通过共享HANDOFF交美术；最终CT接入与用户视觉门另行进行。

## 审查重点与预检

1. 客户端类型加载只发生在Dist.CLIENT；GameTest专用服能启动。
2. 不可变对象真实隔离可变NBT/坐标/集合；工作线程不读远处BE或可变Map；世界会话上下文有实物关联证据。
3. 结构失效、owner移除、成员区块卸载及时撤销；恢复不依赖拆放仪表、每帧扫描或隐式强制加载。
4. 发布在有限区段重建前；重复遥测去重；非法包/重叠归属降级而非越界连接。
5. 现有结构/热工/端口绑定/护目镜合同和未提交资产无越界变化。

自检：单实现者维护互相依赖的描述、BE和缓存；美术仅在资产目录工作，预留三个CT消费类不与此卡交叠。未发现授权/写集互斥；服务器投影和客户端消费分界明确。核心尺寸仅适配层读取，不新建玩法参数。此卡兼作任务状态台账，报告仅记录实施事实，状态由PM维护。
