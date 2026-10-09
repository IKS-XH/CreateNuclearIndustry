# ART-REACTOR-03-L2 实施交付

2026-10-10，实施执行者。隔离树 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-display`，基线 `c5f29f02e30e2d1f7244c0619d0d53c35a9ca8b3`。交付未提交源码，等待 PM 独立审查；没有执行 Git 写操作、派发子 Agent 或修改治理文档。动画消费及 02R1 客户端视觉门未验收。

## 实现与边界

- 仪表在原正式提交/融毁事件顺序完成后冻结同龄 L2。四个提交前早退、正式异常、重扫、外部 `setSnapshot` 和生命周期撤销显示，保留原权威/遥测合同。正式异常仍重抛；显示工厂异常仅撤 L2，继续原遥测和返回行为。
- 内部 `try/finally` 结算作用域包含 hydrate 与正式 setSnapshot，中间包只携带上一份完整 L2；网络 write 不拼装状态，持久化不写 L2。容量来自传给同次 advance 的 `CoolantInput`；库存、控制深度/卡死和转换量来自同次 Result，列 HU/t 保留裂变阶段本次真实值。
- `FuelColumn.fuelUsable` 使用 `FuelColumnState.isEffectiveFuel()`：组件存在、未耗尽且列 integrity > 0。此可用布尔值与本次 HU/t 独立，耗尽 tick 可为 false 且 HU/t > 0。既有权威模型对从未装料列同时省略 snapshot/fission 条目，投影为完整几何、不可用燃料及 0 HU/t；单阶段缺席仍拒收，不重新计算裂变。
- 列由缓存 scan 与同一 L1 bounds/列帽 ID 生成。合法冷却空间只含 EMPTY/CONTROL_ROD 的 body；行程按 body 数量，非固定三格。类型/数量/坐标/非有限值/重复位置/完整几何校验与 64 边长、256 列、4096 body 预算仅保护协议。
- 成功零变化也生成新 sample；原遥测包已带新完整样本时不追加，否则每 5 成功 tick 心跳，恢复/失效立即同步。独立客户端 State 按本地收到新 sample 的时间使用 20 tick 租约；重复/旧样本不续租。L1 generation/geometryRevision 与 L2 revision/sample 水位独立。
- 当前 ClientLevel/原生 RenderChunkRegion 实例绑定 capture。未知包装、Ponder 与旧 world 为空；事件层以非加载查询记录真实 chunk 对象，成员/owner 卸载、替换和缩视距撤销；恢复要求新可靠样本。歧义 bounds 同时撤销两方。物理客户端订阅既有 Update，common BE 无客户端引用。
- PM 追加批准的 `findControlOwner(capPos)` 从同份 owners/rod 索引产生，匹配 findControlRod 同龄归属。无新同步字段或扫描。
- P1Blocks 仅燃料注册增加 noOcclusion；真实用例确认完整碰撞/选框与默认阻光 1。未修改 L1/CT、drive BE、模拟、结构、构建/配置、其他注册及美术素材。

精确 12 个实现/测试/模板路径及 SHA-256 见 `build/reports/art/ART-REACTOR-03-L2/source-sha256.json`。既有 PM 文档/HANDOFF/历史日志保留。

## 实际验证

所有原证据在 `build/reports/art/ART-REACTOR-03-L2/`。

| 检查 | 本次结果/证据 |
| --- | --- |
| 初始入口合同 red | `red.log`，1 断言失败、退出 1；这是源码入口缺失门，未冒充运行行为 red |
| 零燃料正式 Result 行为 red | `empty-result-red.log`，`emptyAuthoritativeMapsStillProjectAllFuelColumnsWithZeroPower` 真实断言失败、退出 1；修复后加入下述 green |
| 定向 JUnit + assemble | `final-junit-assemble.log`，退出 0；24/24：Descriptor 3、Projection 4、Lifecycle 5、Telemetry 5、StructureSummary 7；failure/error/skip 全 0，XML 与 `junit-summary.json` 已复制 |
| 唯一真实仪表首轮 | `gametest-first-failed.log` 保留 No value present：空燃料权威 map 导致显示工厂拒收；测试服随后停在 Saving worlds，按治理仅处理本轮 PID 23872，`first-server-stop.json`，不冒充退出 0 |
| 同一真实用例定向修复重跑 | `gametest-second.log`，1/1 required 通过，独立服正常退出、Gradle 0；实际包含初次不可用、成功完整包、零变化、5tick计数、内部中间包、外部改写/早退保留遥测、端口/外壳重建、新 generation、非持久显示及燃料形状/阻光 |
| 最后仅缩进整理后增量 assemble | `final-assemble.log`，退出 0；class/JAR 内容没有变化，不重复业务测试 |
| 编译公共 API | `javap-api.txt`，JDK21 javap `-public -s` 从实际 JAR读取，退出 0 |
| 源码窄差异/注释自审 | 一次自审覆盖五个审查点，限定源码 `git diff --check` 退出 0；未改既有规则和正式顺序 |

JUnit 命令：`./gradlew.bat test --tests '*ReactorRuntime*Test' --tests '*ReactorInstrumentTelemetryTest' --tests '*ReactorInstrumentStructureSummaryTest' assemble`。

真实服命令：`./gradlew.bat runGameTestServer -PgameTestNamespace=reactor_runtime_display_probe -PgameTestDirectory=build/gametest-runtime-display`。独立服务端实际加载同时核实 common BE 没有加载客户端实现。客户端 capture/区块实例规则由 State JUnit 与源代码窄核查覆盖；没有启动客户端，不称作客户端动画或透明排序通过。

JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，SHA-256 `a45c8047bf939c37e257f057f34ca39096d8d743cf6dbc603b4079957cb2d266`，见 `jar-sha256.json`。复用旧 L1 26/26 与 1/1 原交付证据；未重跑 CT、玩法/Ponder、旧存档或全量。

## 编译实物公共入口

完整 JVM 描述符与构造器实物以 `javap-api.txt` 为准，以下是消费者所需 Java 签名/单位。包前缀为 `com.iksxh.create_nuclear_industry.structure`；客户端项位于 `.client`。

| 类型/入口 | 实物签名/语义 |
| --- | --- |
| BE | `Optional<ReactorRuntimeDescriptor> runtimeDescriptor()`；读最新完整信封，必须检查 available |
| ReactorRuntimeSnapshots | `static ReactorRuntimeSnapshot capture(BlockAndTintGetter context)` |
| ReactorRuntimeSnapshot | `static ReactorRuntimeSnapshot empty()`；`Optional<ReactorRuntimeDescriptor> findOwner(BlockPos ownerPos)`；`Optional<ControlRodColumn> findControlRod(BlockPos capPos)`；`Optional<ReactorRuntimeDescriptor> findControlOwner(BlockPos capPos)` |
| descriptor 身份 | `String dimension()`；`BlockPos ownerPos()`；`UUID ownerGeneration()`；`long revision()`（L2）；`long geometryRevision()`（L1）；`long sample()`；`long serverGameTime()`；`boolean available()` |
| descriptor 几何/运行 | `BlockPos origin()/maxInclusive()`；`List<Column> columns()`；`Set<BlockPos> coolantSpace()`；`long coldCoolantMb()/hotCoolantMb()/coolantCapacityMb()`；`double convertedCoolantMbPerTick()` |
| Column | sealed interface：`BlockPos capPos()`；`List<BlockPos> bodyPositions()`；`String expectedCapBlockId()/expectedBodyBlockId()` |
| FuelColumn | record ctor `(BlockPos capPos, List<BlockPos> bodyPositions, boolean fuelUsable, double fissionHeatHuPerTick)`；额外访问器 `fuelUsable()/fissionHeatHuPerTick()`，功率 HU/t |
| ControlRodColumn | record ctor `(BlockPos capPos, List<BlockPos> bodyPositions, double actualDepth, double targetDepth, boolean jammed)`；访问器 `actualDepth()/targetDepth()/jammed()`，深度 [0,1] |
| EmptyColumn | record ctor `(BlockPos capPos, List<BlockPos> bodyPositions)` |
| descriptor 构造器 | `(String dimension, BlockPos ownerPos, UUID ownerGeneration, long revision, long geometryRevision, long sample, long serverGameTime, boolean available, BlockPos origin, BlockPos maxInclusive, long coldCoolantMb, long hotCoolantMb, long coolantCapacityMb, double convertedCoolantMbPerTick, List<Column> columns)` |
| 工厂/协议 | `project(ReactorSurfaceDescriptor, ScanResult, Result, CoolantInput, long revision, long sample, long serverGameTime)`；`CompoundTag encode()`；`static Optional<ReactorRuntimeDescriptor> decode(CompoundTag)`；消费端不自行补扫 |

## 技能与工作区说明

实际读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 与 `minecraft-testing/SKILL.md`，核实 MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6；使用当前实际注册/真实 BE/GameTest 模式，未照新版本示例改依赖。读取 superpowers 的 executing-plans、test-driven-development、verification-before-completion，失败后读取 systematic-debugging；治理写集/Git禁令/5.1/5.2优先于通用技能的提交、派发、重复全量与旧存档步骤。using-superpowers 的 SUBAGENT-STOP 适用本执行者。

既有 JUnit 配置自动写 root logs，首次运行把历史字节轮转到 gzip；本轮日志已先复制为 `junit-debug-final.log`、`junit-latest-final.log`。按 PM 明确允许，仅从已核实首行 `2026-10-09 11:00:48.755` 的对应原字节恢复两文件：原 debug 在捕获时 `logs/debug-5.log.gz`，原 latest 为 `logs/2026-10-09-2.log.gz`，专属证据已保留 `original-debug.log.gz`/`original-latest.log.gz`。两文件恢复前 SHA 均 `1227e2c2175f9b26dc8ed0f85fefb2c602ffc12c17bd561d134e47a6d4075040`，恢复后原字节 SHA 均 `1b3cf6b9ffbe10093c4c2a0ba33a3be74c03117c9bbed6b83b0e5518e97a6368`；详见 `history-log-restoration.json`。没有用 Git 版本替代 dirty 历史日志，没有删除轮转证据或其他日志。

下一步由 PM 做一次独立规格/质量审查、净 Git 整合和美术 API HANDOFF；本执行者到此停止实现，不派发美术消费，不改变任务状态。

## R1：错误 dimension 即时撤销（2026-10-10）

按独立 REVIEW.md 唯一 P2 与 PM 窄派发，仅改变 `structure/client/ReactorRuntimeSnapshots.java` 的接收校验顺序，以及既有 `ReactorRuntimeLifecycleTest.java` 的行为回归。当前会话中先核实 owner/token，再拒收错误 dimension 并将 active 置 false；保留原 generation/revision/sample 水位。旧 world、未知/旧 token 仍直接忽略，不撤销当前好样本。没有改同步字段、ABI、其他源码或玩法。

新回归对新样本 NBT 只改为合法 `minecraft:the_nether`，确认 decode 成功；覆盖当前错误包返回 false 且同份 snapshot 的 findOwner/findControlRod/findControlOwner 同时为空；重复原 sample 不能恢复或续租，新正确 sample 可恢复；旧 world/token 错维度投递不影响可靠样本。

- `R1/red.log` 与 `R1/red-lifecycle.xml`：新行为用例 1/1 真实断言失败，Gradle 退出 1，失效 owner 索引仍可见。生产代码修复在观察该 red 后进行。
- `R1/green-assemble.log` 与 `R1/green-lifecycle.xml`：`./gradlew.bat test --tests '*ReactorRuntimeLifecycleTest' assemble` 退出 0；生命周期 **6/6，failure/error/skip 均 0**。未重跑原其他 JUnit、真实服或客户端；原 24/24、1/1 和首轮失败证据保留为历史原批核验记录。
- `R1/ReactorRuntimeSnapshots.diff` 与 `R1/ReactorRuntimeLifecycleTest.diff` 是相对独立审查候选的精确两路径差异；对应 before.java 副本保留原字节。`R1/changed-sources.json` 确认仅这两条源码 SHA 改变，其余原写集 10/10 匹配原冻结。
- 最新冻结入口 **`R1/final-freeze-r1.json`**，完整 12 路径新清单 `R1/source-sha256-r1.json`。Snapshots SHA `f11c0262b57b45dd85d8fcc4ed7b9ed350ceae7076872339948affe2b3c71022`；LifecycleTest SHA `e830863d1154e79f1d9f1c8f34b35b0cbad6619e485f939459e4cf8f6a357d62`。
- R1 JAR SHA `b4c56788146d672d7130a1231d0bce64f54afc91baba53a408d5f92ceaff83df`，见 `R1/jar-sha256-r1.json`。当前 build/libs JAR为R1，另冻结副本于 `R1/create_nuclear_industry-0.1.0-R1.jar`。初版 source-sha256.json、jar-sha256.json、javap-api.txt、XML、日志和失败记录均未覆盖。
- JDK21 javap 从 R1 实际 JAR读取，`R1/javap-api-r1.txt` 退出 0；与初版全部公共 API文本逐字节相同，SHA均 `177de2fc074b2fe9ffde92c8af1134466f9fc3d7ff3a351b2f1acb93e68a013a`，见 `R1/api-comparison.json`。公共字段/构造器/入口表沿用上文。
- R1 root 自动日志先保留为 `R1/junit-debug.log`/`R1/junit-latest.log`。按原 PM 窄允许，从既已核实的 original-debug.log.gz/original-latest.log.gz 原字节恢复；本轮恢复前两文件 SHA `9acd7dd35c3ed676941042d8c25be8fd38b5691d3af1eaf056ed95c40785017d`，恢复后均为原 `1b3cf6b9ffbe10093c4c2a0ba33a3be74c03117c9bbed6b83b0e5518e97a6368`。详见 `R1/history-log-before.json` 与 `R1/history-log-restoration.json`，轮转证据保留。

实际补读 receiving-code-review 技能，核实审查控制流后实施，沿用此前 TDD/验证与 Minecraft 技能。只读 diff/SHA/API 窄自审通过，未执行 Git 写操作、派发或改变任务状态。等待同一独立审查者只复审 R1 差异及对应证据。
