# EXT-A-MATERIAL-03B 执行报告

## 基线与边界

- 候选工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；开工 `HEAD 508178c`，工作树干净。执行者只做任务卡允许的临时诊断，不做 Git 写、核心文档或正式构建修改。
- 锁定环境：Minecraft 1.21.1、Java 21.0.7、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。
- 前轮两次完整 GameTest 均在 `defaultBatch:2`、`Object2LongOpenHashMap$MapIterator.nextEntry:711` 抛出 `wrapped == null` NPE；旧日志没有正在执行的具体测试名。这是本轮待定位症状，不视为本轮复现结果。

## 已完成的单项验证

- 启动前备份根跟踪日志到 `EXT-A-MATERIAL-03B/root-latest.log.before` 和 `root-debug.log.before`；默认 `run/` 的180文件路径、长度、SHA-256、UTC 修改时间记录于 `default-run-before.csv`。当时只有用户 Gradle daemon Java PID 21468。
- `material-03b.init.gradle` 同时指定 NeoForge `server`、`gameTestServer` run 模型和 JavaExec `gameDirectory`，并在执行前断言规范路径均位于03B报告目录。`material03bRunDirectories --max-workers=1` 退出0；详情 `directory-check.log`。普通服仅使用 `isolated-control-server`，未启动 `runGameTestServer` 或客户端。
- 原控制测试 `P1ControlGameTests.redstoneOnControlRodDriveDoesNotTriggerScram` 只增加临时 logger 标记，保留原 tick 5→7、断言和 `helper.succeed()`。第二次隔离普通服输入 `test run redstoneoncontrolroddrivedoesnottriggerscram`，服务器报告 `Running 1 tests`。`control-single-latest.log:59–64` 依次记录方法入口、tick5回调、内层 `runAfterDelay(2)` 返回、tick7回调进入和 `helper.succeed()` 之后的 SUCCESS。随后 `stop`，`runServer` 退出0；对应游戏 Java PID 980，包装 PID 6240，均已退出。此单项未复现原 NPE。
- 控制单项后默认 `run/` 仍180文件，和前清单逐项比较差异0；见 `default-run-after-control.csv`。只读进程复查仅剩原 Gradle daemon 21468，见 `java-process-after-control.json`。

## 锁定 fastutil 最小夹具

- `MapReentryProbe.java` 使用实际 fastutil 8.5.12 JAR及与 `GameTestInfo.tickInternal` 相同的 iterator `next`→到期回调→`iterator.remove` 顺序。默认容量32，先登记 N 个回调，其中一个到 tick5 再登记 tick7 回调；固定种子测试每个 N 的10万组键哈希。
- N=1至23未出现 NPE。N=24 第1组插入第25个回调，容量从32重排为64，随后于 `Object2LongOpenHashMap$MapIterator.nextEntry:711` 抛出与原日志相同的 `wrapped == null` NPE；完整参数、访问顺序与栈见 `map-reentry-probe.log`。这证实**迭代期间扩容**可以产生同栈异常；并未证明原 `defaultBatch:2` 的具体测试或控制方法触发扩容。控制方法源码只显式登记一个初始延迟回调，实际单项成功，不能据此改它的调度。

## 仍待定位

- PM于2026-10-01把 `P1LoopGameTests.dynamicTelemetryPacketReachesClientWithinTenTicks` 纳入第一阶段临时日志写集。锁定 `GameTestHelper.onEachTick:930–932` 从当前 tick 到 `getTimeoutTicks()-1` 逐项调用 `setRunAtTickTime`。该方法在 tick5 回调中调用，超时120，首次追加115项。
- 新隔离普通服只输入 `test run dynamictelemetrypacketreachesclientwithintenticks`。`telemetry-single-latest.log:56–65` 记录单项运行、tick5 `onEachTick` 前后；紧接同一外层回调在 tick6 **再次进入**，再追加114项。这说明迭代期新增任务已经扰乱原回调的一次性执行。`telemetry-single-latest.log:66–71` 随后由服务器抛出与前两次完整回归同栈的 `wrapped == null` NPE，未出现 SUCCESS 标记。完整服务器崩溃报告见 `telemetry-single-crash.txt`。这项真实 GameTest 的单项隔离复现明确锁定一个可触发者；旧 `defaultBatch:2` 日志无测试名，仍不能单靠旧日志断言两次历史崩溃必由它引起。
- 此次崩溃后 Gradle 仍打印 `BUILD SUCCESSFUL in 34s` 并以0退出；那只表示 Gradle 任务进程结束，**不能**作为 GameTest 成功证据。游戏 Java 进程随 crash 退出，未停止用户 Gradle daemon。默认 `run/` 仍180文件、逐项差异0，见 `default-run-after-telemetry.csv`。
- 这次单项复现后，PM 才在主工程03B任务卡中新增第二阶段精确放行：只修 `P1LoopGameTests.dynamicTelemetryPacketReachesClientWithinTenTicks` 的调度。原先五个少量嵌套方法没有本次单项崩溃归因，本轮没有修改。

## 第二阶段整改

- 唯一正式源码差异为 `P1LoopGameTests.dynamicTelemetryPacketReachesClientWithinTenTicks`：第0 tick 注册 `GameTestSequence.thenExecuteAfter(5, ...)`，原 `runAfterDelay(5, ...)` 的初始化体仍在第5 tick执行。锁定 `GameTestInfo.tickInternal:127–153` 先遍历延迟任务表，再运行 sequences；`GameTestSequence.thenExecuteAfter:38–46` 自建立时的 tick0 等待5 tick。因此 `onEachTick` 在第5 tick 的 sequence 阶段批量注册，不在活动 map iterator 内扩容。
- 原初始化对象与顺序未动：同一 `instrument`、模拟 `ServerPlayer`、跟踪块、`EmbeddedChannel`、快照、结构重扫和 `startTick`；原 `onEachTick` 可见性断言 `helper.getTick() - startTick <= 10L`、超过十 tick 的失败分支和 `helper.succeed()` 均保留。新 `catch (Exception)` 将初始化异常交给 `helper.testInfo.fail`，保持原 `GameTestInfo.tickInternal` 对延迟回调异常归入该测试失败的行为。新增两条中文说明解释迭代顺序和异常边界。
- 诊断 logger、字段、导入和所有 `MATERIAL_03B_*` 标记已从正式源码清除。控制测试已恢复原字节与 CRLF，`git diff` 无其内容差异；没有修改正式构建、依赖、生产代码、资源或另外五个候选方法。

## 修复后验证

- **隔离单项：** 在新 `isolated-telemetry-fixed` 普通服世界运行 `test run dynamictelemetrypacketreachesclientwithintenticks`。`telemetry-fixed-single-latest.log:97–101` 记录入口、第5 tick 初始化及 `onEachTick` 返回、第7 tick 原断言后 `helper.succeed()` 的 SUCCESS；没有重复第6 tick 初始化或 fastutil NPE。21:56:05 提交 `stop`，服务器停在 `Saving worlds`；PM 在有限等待后结束本轮精确 Java PID 1364，Gradle 报子进程 `-1`、退出1。该单项的断言成功与保存退出异常分别记录，不能把它写成正常退出0。
- **最终源码完整 GameTest：** `./gradlew.bat -I build/reports/extension/EXT-A-MATERIAL-03B/material-03b.init.gradle runGameTestServer --rerun-tasks --max-workers=1`，隔离目录模型与 JavaExec 断言通过。`full-gametest.log:318–319` 明确四批共 `155 GAME TESTS COMPLETE`、`All 155 required tests passed :)`，无断言失败。22:03:21 后停在 `Saving worlds`，观察超过60秒，只结束本轮 Java PID 23404（父进程为原 Gradle daemon 21468，启动时间22:02:52，命令含 `gameTestServerRunProgramArgs`），详见 `full-gametest-stop.txt`；Gradle 因子进程 `-1` 退出1。这里的 GameTest 断言全过与 Gradle 退出1不能混为一项成功结论。
- **新鲜 JUnit/build：** `./gradlew.bat test build --rerun-tasks --max-workers=1` 退出0，`test-build.log:90` 为 `BUILD SUCCESSFUL in 26s`；52份 XML 汇总265 tests、0 failures、0 errors、0 skipped，见 `junit-summary.json`。
- **最终制品：** `verify-artifact.ps1 -RepoRoot <仓库根路径> -JarPath <最终JAR路径>` 在本候选执行退出0。源码/JAR 各264项 assets/data 逐字节 SHA-256 相同、差异0，游戏 PNG 60张；语言资源中 `steel_dust`/`steel_ingot`/`steel_plate` 为“钢粉/钢锭/钢板”，见 `artifact-verification.json`。JAR SHA-256 为 `E48FA4129DC20E642AA62F8DEDD62809585EDE38C0F31CE8B00EBD959F89152B`。游戏纹理相对本次 HEAD 无 Git 内容差异。
- **目录与进程：** 最终默认 `run/` 仍180文件，与启动前路径、大小、SHA-256、UTC 修改时刻逐项差异0，见 `default-run-after-final.csv`。结束后只剩原 Java Gradle daemon PID 21468，见 `java-process-final.json`。根跟踪 `logs/latest.log`、`logs/debug.log` 因测试变更；开工原副本已保存，按任务卡交 PM 恢复，不由执行者做 Git 回退。源码限定路径的 `git diff --check` 退出0；根日志生成内容有行尾空格，不归本次源码差异。

## 交接边界

正式功能代码、测试断言、资源与人工已通过的材料03客户端清单未改变。历史两次 `defaultBatch:2` 的具体活跃对象没有原始测试名，不能事后把单项证据写成历史逐项追踪；本轮已在真实单项中重现同一异常并在同一方法的修复后消除，随后155项完整 required 全过。最终验收、根日志恢复、Git 提交和 main 整合由 PM 处理。

## 实际使用技能

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：核对仓库锁定 Minecraft/NeoForge/Java/Create 版本及服务端运行边界；没有套用技能的较新版本示例。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：使用真实单项 GameTest 和明确成功标记，区分测试断言完成、普通服退出与155项全量门。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/systematic-debugging/SKILL.md`：先读原栈、再做隔离单项与单变量调度表夹具，分别记录机制与归因。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/verification-before-completion/SKILL.md`：以当轮日志、退出码和默认目录清单支持以上限定结论；没有把历史通过替代本轮回归。
