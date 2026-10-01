# EXT-A-MATERIAL-03B 只读代码审查

## 范围与结论

- 基线：候选工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，基线 `508178c`。
- 审查范围：`P1LoopGameTests.dynamicTelemetryPacketReachesClientWithinTenTicks` 的调度与异常处理；未检查或修改其他实现。
- 结论：当前调度与异常处理没有发现阻断项。复查时两个诊断类中已无 `MATERIAL_03B`、`LogUtils` 或 `Logger` 标记，目标方法最终差异也已无临时日志；后续全量验证结束时仍需确认源码保持此状态。

## 代码与框架语义

锁定的 NeoForge/Minecraft 源码位于 `C:/Users/IKSXH/.gradle/caches/neoformruntime/intermediate_results/sourcesAndCompiledWithNeoForge_4a83a73d95fdbeba9a47b7d6bbaabb6346ecca49_output.jar`。其中 `GameTestInfo.tickInternal` 先遍历 `runAtTickTimeMap`，随后调用各 `GameTestSequence.tickAndContinue`；`GameTestSequence` 创建时以当前测试 tick 初始化 `lastTick`，`thenExecuteAfter(5, ...)` 在 `parent.getTick() >= lastTick + 5` 时执行。因此新回调相对原调用时点仍为第 5 tick，但发生在本 tick 到期任务 map 的迭代之后。

`GameTestHelper.onEachTick` 会把当前 tick 至 timeout 前的回调逐项写入 `runAtTickTimeMap`。将这次批量注册从 map 回调移到 sequence 阶段，避开了同一活动 iterator；注册在迭代之后发生。保留 `onEachTick` 本身及 120 tick 测试超时，没有扩大测试超时。

比较 `508178c` 中的原方法与当前差异，以下行为断言和夹具均保留：同一模拟 `ServerPlayer`、`EmbeddedChannel`、instrument chunk tracking、快照值、`rescanInstrumentPortNow` 刷新、发送队列清理、数据包识别，以及数据包可见时 `helper.getTick() - startTick <= 10L` 的十 tick 上限。超时分支仍在经过十 tick上限后失败。初始化仍通过原第 5 tick 延迟启动。

新增 `catch (Exception exception) { helper.testInfo.fail(exception); }` 会把 sequence 回调中的普通异常（包括现有 `require` 抛出的断言异常）登记为该 GameTest 失败。锁定的 `GameTestInfo.tickInternal` 对到期 map 回调也捕获 `Exception` 并以 `fail(exception)` 记录，故不会把初始化异常吞掉或改成成功。此审查未对 `Error` 声明额外行为；框架原到期回调路径同样只捕获 `Exception`。

## 运行证据与待复核项

本审查未运行 Gradle 或游戏。独立读取 `build/reports/extension/EXT-A-MATERIAL-03B/telemetry-single-latest.log` 后确认：旧实现的单项运行在 tick 5 注册后，外层回调于 tick 6 再次进入并再次注册，随后出现与报告的 `Object2LongOpenHashMap$MapIterator.nextEntry:711` / `GameTestInfo.tickInternal:130` 一致的 `wrapped == null` 异常；没有 SUCCESS 标记。独立读取 `build/reports/extension/EXT-A-MATERIAL-03B/telemetry-fixed-single-latest.log` 后确认：修复版于 tick 5 初始化、注册逐 tick检查后未重入外层初始化，tick 7 在 `helper.succeed()` 后记录 SUCCESS；该日志没有出现 fastutil 异常。它支持隔离单项的成功结论，不代表 155 项全量回归。

修复服日志在 tick 7 成功后进入 `Stopping the server`、`Saving worlds`，没有正常退出记录。`build/reports/extension/EXT-A-MATERIAL-03B/fixed-server-pm-stop-process.json` 记录服务端 PID 1364、父 Gradle PID 21468；父执行者说明该 PID 已精确停止。因此不能把 Gradle/服务端进程退出码描述为正常通过。

最终源码复核确认 `P1LoopGameTests.java` 的唯一差异仅在指定方法，两个诊断测试类均无临时 logger、导入或标记。隔离的完整回归日志在 22:03:21 明确输出 `All 155 required tests passed`，且无 `wrapped == null` / `MapIterator.nextEntry`。随后服务端停在 `Saving worlds`；`full-gametest-stop.txt` 记录仅停止 PID 23404，`full-gametest.log` 记录 Java 子进程退出值 `-1`。因此，155 项断言通过有日志支持，但这次 GameTest 进程没有正常退出。

最终 `test-build.log` 显示 `test`、`assemble`、`check`、`build` 完成并以 `BUILD SUCCESSFUL in 26s` 结束。只读解析 `build/test-results/test/TEST-*.xml` 的 52 个套件共 265 项，failures、errors、skipped 均为 0。候选当前另有 `logs/latest.log` 和 `logs/debug.log` 差异；按 PM 指示，这两份根日志由 PM 恢复基线，不属于本审查范围。PM 尚需完成制品核对与候选整合。本报告结论只针对此方法的代码审查，不代替项目经理对 EXT-A-MATERIAL-03B 的最终验收。

## 实际使用的技能

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：核对所审源码对应的 MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 技术基线；不采用与仓库版本不符的示例。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：按真实 NeoForge GameTest 的调度、断言和测试完成证据边界审查；未将代码审查替代单项或全量运行。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/requesting-code-review/SKILL.md`：将审查聚焦在任务合同、当前差异和明确验收点；本角色是父执行任务中指定的只读审查者，未额外派发代理。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/verification-before-completion/SKILL.md`：区分源码语义分析、主执行者报告的单项运行和未完成的最终回归；不作测试通过或任务完成声明。
