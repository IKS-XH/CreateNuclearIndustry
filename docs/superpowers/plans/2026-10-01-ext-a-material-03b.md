# EXT-A-MATERIAL-03B：回归中断定位、修复与钢材整合

**状态：执行中。** 用户在PM说明“先解决回归、合入主工程，再整理基础零件参数”后明确要求“开始吧”。材料03客户端完整清单已通过，名称已简化为钢；不重复整链手测。

**基线与角色：** 主工程`E:/MyMC/NewMod/Create_NuclearIndustry` / main `92c730e`；同级候选`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition` / `codex/ore-acquisition` / `508178c`干净，复用候选。主工程既有`.vscode/launch.json` SHA-256 `65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`必须保留。只有PM做文档、任务状态与Git写；执行者不再派发。因涉及运行器与真实服务端隔离，主执行者采用标准模型较高思考，简洁差异审查优先高速模型。

**必读：** AGENTS、治理、材料03/03A卡、[03A只读诊断](../../reviews/2026-10-01/material-03a/EXT-A-MATERIAL-03A-REGRESSION-AUDIT.md)及原始两次异常；实际应用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`，systematic-debugging与verification-before-completion。锁定MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6，不升级依赖。

## 目标与写集

定位实际可复现的GameTest中断，在保持原断言与测试语义的前提下修复测试调度，完成155项required回归和新鲜JUnit/build证据。不得跳过测试、屏蔽异常、删除断言、改变生产行为或为了通过而改时序容差。

第一阶段只允许写候选`build/reports/extension/EXT-A-MATERIAL-03B.md`及同名目录，包括隔离init、诊断夹具、临时日志/调试工具与新测试世界。可在下列目标方法内临时添加诊断日志（及必要logger导入/字段），不提前修复；所有临时手写注释为中文：

- `src/main/java/com/iksxh/create_nuclear_industry/gametest/P1ControlGameTests.java`：`redstoneOnControlRodDriveDoesNotTriggerScram`。
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/P1LoopGameTests.java`：`dynamicTelemetryPacketReachesClientWithinTenTicks`（2026-10-01 PM诊断范围补充，只允许临时日志及必要logger导入/字段）。锁定`GameTestHelper.onEachTick`源码确认，它从当前tick到timeout-1批量调用`setRunAtTickTime`；该测试在tick5回调内调用它，timeout120，因此会在当前map迭代中追加115个任务。这是最小夹具扩容异常的直接实际候选，先单项复现，不提前改时序。

证实重入风险或具体触发后向PM提交证据，PM记录放行才进入第二阶段；后续允许精确调整下列候选方法的调度（保留绝对执行tick、断言及原有状态）：

- 上述控制测试。
- `gametest/P1StructureGameTests.java`：`structureFormsOnLoadAndBuildsColumnCache`、`structureInvalidatesOnBreakRestoresOnPlacementAndPreservesState`。
- `gametest/P1Refuel02aGameTests.java`：`instrumentReplacementRetainsPortFuel`。
- `gametest/P1Refuel03GameTests.java`：`armPointRejectsInvalidTargetsAndRollsBack`。

上述均相对同一Java包。如需测试局部状态容器或中文说明，可在这四类中作必要最小修改；不得顺带格式化其他方法。其他源码、正式构建、依赖缓存、资源、默认`run/`、历史报告与核心文档均禁止写；需新增路径先报PM。不使用生产Mixin或运行器补丁掩盖问题。

## 分步验证

1. 备份本轮根跟踪日志；快照当前默认run/存档路径、大小、哈希、修改时刻。记录已有Java进程，禁止停止用户客户端或Gradle守护进程。
2. 复用03已有隔离模式，但所有新输出仅03B；必须同时设置NeoForge run模型与JavaExec实际gameDirectory并断言规范路径，不能只改workingDir。使用锁定源码确认的单项筛选方式或隔离普通服命令，不猜系统属性。
3. 首先单测控制棒红石候选。记录名称、嵌套调度入口与异常/成功；单次没复现不能证明风险不存在。必要时在报告目录构建最小调度复现或给实际异常附上测试名，所有试验一次只变一个因素。不无界重试整套。
   - 第一阶段已证：控制单项执行到tick7，未复现；fastutil 8.5.12最小夹具在24个在途任务插入第25个、容量32→64时重现同栈，1–23个初始任务各10万轮未重现。夹具证明机制，不单独证明历史故障归属。完成当前控制单项持久日志后，正常停服，转测上述遥测方法，记录调用`onEachTick`前后、当前tick、是否同栈异常或成功。原先五个少量嵌套方法暂不修复；范围以真实复现结果收敛。
4. 证据提交PM后，按放行范围消除回调迭代期间注册新任务，保留原执行tick与行为断言；重测受影响单项，再完成155项全量required及`test build --rerun-tasks --max-workers=1`。构建与游戏串行，不并行争用资源。
5. 如果断言全过后Saving worlds挂起，分别记录完整通过汇总与退出状态；有限等待后只能停止该次精确PID，不把非零退出写成正常成功。新异常或断言失败先定位，不升级依赖或放宽断言。
6. 最终报告含复现→整改→验证证据、原/现调度tick对照、精确代码差异、JUnit/GameTest计数、命令/退出/PID与默认run保持；全部264项assets/data与JAR、60图、三钢材中文名核对。正式新代码不残留临时诊断。保存精简证据包，不纳入世界、缓存或JAR。

## 第二阶段放行记录（2026-10-01）

PM已直接读取本轮遥测单项日志：仅运行`dynamicTelemetryPacketReachesClientWithinTenTicks`，tick5追加115项，tick6原外层回调异常再次执行并追加114项，随即出现与历史全量相同的`wrapped == null` / `MapIterator.nextEntry:711` / `GameTestInfo.tickInternal:130`。这是本轮单项的已证实触发，历史两次具体对象仍无日志可回溯。该普通服虽然崩溃，Gradle返回0，故不能据退出码判定GameTest通过。

放行的正式修复写集收敛为`P1LoopGameTests.dynamicTelemetryPacketReachesClientWithinTenTicks`及必要中文说明：将批量调度移出`runAtTickTimeMap`正在迭代的阶段，优先使用锁定框架的sequence阶段，保持第5tick初始化、同一模拟客户端/通道/结构/快照、十tick可见性上限及全部原断言。不得增加超时、跳过任何测试或修改遥测生产代码。执行者先核对sequence实际tick语义；有差异或所需范围扩大时报告PM。原五个少量嵌套方法本轮不改；控制测试及遥测方法的临时诊断最终移除。

先以隔离单项取得修复后实际成功证据（验证时可保留临时日志），随后移除诊断，再用最终源码执行155项全量和新鲜JUnit/build。独立审查重点核对初始化时机、十tick边界、异常传播和日志清除。全量若出现新故障按事实定位，不因先前单项通过豁免。

## PM审查与整合

第一阶段可并行一次高速只读调度补审：核对锁定GameTestHelper/GameTestInfo是否还有隐式写入runAtTickTimeMap的路径，以及现有测试嵌套调度清单有无漏项；不运行Gradle/游戏或改任何代码，唯一交付为候选`build/reports/extension/EXT-A-MATERIAL-03B-SCHEDULER-AUDIT.md`。这项补审只为缩小当前异常诊断，不替代实际复现与最终审查。

独立执行者只读审查必要测试改动，唯一报告为候选`build/reports/extension/EXT-A-MATERIAL-03B-REVIEW.md`；不运行游戏或改实现。PM审核证据后恢复根日志，提交候选；确认main可安全快进且启动配置保持后合入，主工程运行适用的最终构建/资源核对并归档。若发生源码冲突交执行者，PM不代改代码。

钢材批次收尾后才整理下一批最小基础零件方案，以首台设备依赖为依据；数量、加工时间和热级未经批准不实施。到该决策门保存方案并暂停，不转跑辅助任务。
