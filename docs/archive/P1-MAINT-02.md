# P1-MAINT-02 交付报告：实现完全停机条件

**任务 ID：** `P1-MAINT-02`
**状态：** 已完成并归档；项目经理自动验收通过
**基准分支：** `main`
**实现代码基线：** `05feb0858f3dd786922f8dc657564e91c851c30b`
**任务卡派发提交：** `31a4045`
**技术基线：** Minecraft `1.21.1`、Java `21`、NeoForge `21.1.219`、Create `6.0.10-280`、Ponder `1.0.82`

## 实际使用的技能

- `minecraft-modding`：`C:/Users/lenovo/.codex/skills/minecraft-modding/SKILL.md`。用于核对当前 NeoForge 1.21.1、加载器无关 reactor 状态边界、现有裂变计算器和服务端权威快照约束。
- `minecraft-testing`：`C:/Users/lenovo/.codex/skills/minecraft-testing/SKILL.md`。用于设计 JUnit 5 纯逻辑覆盖、required GameTest 运行方式和全量构建验收。

## 实现结果

新增 `ReactorFullShutdownAssessment.assess` 作为加载器无关的纯判定入口，并返回不可变 `ReactorFullShutdownResult`。入口只读取传入的同一份 `ReactorSnapshot` 与 `ReactorSimulationParameters`，对合法输入只调用一次 `ReactorFissionCalculator.calculate`；不调用 `ReactorServerTick.advance`，不提交快照，不写方块实体，不修改世界，也不消耗燃料或冷却剂。

完全停机只在以下四项同时满足时成立：

| 条件 | 结果字段 | 通过规则 |
| --- | --- | --- |
| 新生裂变热 | `generatedHeatSafe` | `generatedHeatHu <= 1.0E-12` |
| 计划燃耗 | `plannedFuelBurnSafe` | `plannedFuelBurnUnits <= 1.0E-12` |
| 燃料列活动余热 | `activeResidualHeatSafe` | 所有燃料列活动余热确定性求和后 `<= 1.0E-12 HU` |
| 融毁倒计时 | `meltdownCountdownInactive` | `meltdownCountdownStarted == false` |

最终字段 `fullyStopped` 是四项条件与 `inputValid` 的合取。任一条件不满足均拒绝；因此单独 SCRAM、完全插入控制棒、暂停倒计时或停止新生热都不能替代其他条件。

## 余热口径

每个燃料列的安全量化余数和活动余热按以下公式计算：

```text
safeRemainderHu = max(0, min(cachedHeatHu, quantizedHeatRemainderHu))
activeResidualHeatHu = max(0, cachedHeatHu - safeRemainderHu)
```

全堆燃料列从 `ReactorSnapshot` 的确定性有序映射中遍历并求和，结果字段分别为 `safeQuantizedHeatRemainderHu` 和 `activeResidualHeatHu`。完全由整数 `mB` 冷却剂量化产生的安全余数可以大于 epsilon 并保留，不会制造永远无法通过的停机状态；同列仅部分量化时，剩余活动余热仍会拒绝停机。epsilon 使用 `ReactorFullShutdownAssessment.SAFETY_EPSILON` 的 `1.0E-12`。

`ControlRodColumnState.cachedHeatHu` 明确排除在本次活动余热统计之外。这是现有模型边界：热传播 tick 已将传播到控制棒列的热结算为控制棒完整度损失，之后该缓存不再充当热源、参与冷却或继续传播。本任务没有改写传播或冷却公式。

以下存量不单独参与完全停机判定：冷/热冷却剂库存、换料端口物品、燃料是否耗尽、`fuelBurnRemainder`、控制棒目标/实际深度、列完整度、卡死状态和 SCRAM 请求。它们不是当前生产热或燃耗；`meltdownEventPublished` 也不单独放行，已发布状态仍因 `meltdownCountdownStarted == true` 拒绝。

## 结果字段

`ReactorFullShutdownResult` 暴露：

- `fullyStopped`：四项条件全部满足且输入有效时为 `true`。
- `inputValid`：输入和一次裂变计算均有效；`null`、无效数值或运行时计算异常失败关闭。
- `generatedHeatSafe`、`plannedFuelBurnSafe`、`activeResidualHeatSafe`、`meltdownCountdownInactive`：四项独立诊断字段。
- `generatedHeatHu`、`plannedFuelBurnUnits`：生产裂变计算器返回的新生热和计划燃耗。
- `activeResidualHeatHu`、`safeQuantizedHeatRemainderHu`：燃料列余热统计结果。

失败关闭结果所有条件和最终布尔值均为 `false`，数值字段为零。结果类型为不可变 record，且构造器校验最终字段与分条件的一致性。

## 改动文件

- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorFullShutdownAssessment.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorFullShutdownResult.java`
- `src/test/java/com/iksxh/create_nuclear_industry/reactor/ReactorFullShutdownAssessmentTest.java`
- `build/reports/p1/P1-MAINT-02.md`

未修改方块实体、结构扫描/生命周期、事件发布器、GameTest 生产入口、快照/NBT 格式、配置、热工/燃耗/损伤/传播/融毁公式、注册/资源、Gradle、模拟器、核心文档、活动计划或 Git 历史；没有新增 GameTest。

## JUnit 覆盖

新增 `ReactorFullShutdownAssessmentTest` 共 13 项：

1. `emptyHeapMeetsAllFourConditions`
2. `nonZeroGeneratedHeatRejectsEvenWhenPlannedBurnIsWithinEpsilon`
3. `nonZeroPlannedFuelBurnRejectsEvenWhenGeneratedHeatIsWithinEpsilon`
4. `activeCachedHeatRejects`
5. `safeQuantizedRemainderDoesNotBlockShutdown`
6. `partialSafeQuantizedRemainderLeavesActiveHeat`
7. `newlyStartedRunningScramPausedAndCoolingPausedCountdownsReject`
8. `completedAndPublishedCountdownRejects`
9. `fuelInventoriesRemaindersDamageDepthJammedAndScramDoNotFalseReject`
10. `controlRodHistoricalCachedHeatDoesNotBlockShutdown`
11. `exactEpsilonActiveHeatPassesAndAboveEpsilonRejects`
12. `inputSnapshotIsUnchangedAndRepeatedAssessmentIsEqual`
13. `nullInputsFailClosed`

覆盖了空堆、合法独立热/燃耗参数、仅活动余热、仅安全量化余数、部分量化余热、倒计时刚建立/运行/SCRAM 暂停/冷却暂停/完成/已发布、燃料与库存存量、燃耗小数、控制棒深度/损伤/卡死、SCRAM、控制棒历史缓存热、epsilon 边界、重复求值和输入快照不变。

## 自动验证

- `./gradlew.bat test --tests com.iksxh.create_nuclear_industry.reactor.ReactorFullShutdownAssessmentTest --rerun-tasks`：通过。
- `./gradlew.bat test --rerun-tasks`：通过，JUnit `242/242`，`failures=0`、`errors=0`；其中新增测试为 `13/13`。
- `./gradlew.bat runGameTestServer`：服务端日志报告 `97/97 required tests passed`。全部测试通过后 NeoForge 停在已知的 `Saving worlds` 退出阶段，已结束完成的外层会话；required GameTest 结论不受影响。
- `./gradlew.bat build`：通过。
- 任务范围 `git diff --check`：通过，任务代码/测试/报告范围无空白错误。

Gradle 和 GameTest 运行生成或更新了根目录 `logs/debug.log`、`logs/latest.log`；它们不是本任务交付文件，未被清理、回退或纳入报告修改范围。执行者未执行任何 Git 写操作。

## 无副作用证明

判定入口没有世界、方块实体、事件总线或流体 capability 依赖；输入快照为不可变 record，测试确认求值前后快照相等，重复求值结果相等。除一次纯 `ReactorFissionCalculator.calculate` 外，只读取燃料列和倒计时字段，不推进服务端 tick，不消耗资源，不改变控制棒、SCRAM、完整度、燃料组件、冷/热库存、缓存热量或 `meltdownEventPublished`。

## P1-MAINT-03 接入说明

后续停机破坏性重组成型可在服务端持有的当前权威快照上调用：

```java
ReactorFullShutdownResult result = ReactorFullShutdownAssessment.assess(snapshot, parameters);
```

`P1-MAINT-03` 应使用 `result.fullyStopped()` 作为唯一完全停机放行结果，并可使用四项分条件和四个数值字段生成拒绝诊断；本任务不接入破坏事件、不移除结构、不清空 NBT，也不执行重组成型。`P1-MAINT-03` 仍需自行提交清空燃料、冷/热冷却剂、控制棒深度、列缓存热量、完整度和融毁进度的新快照，并将新成型控制棒设为完全插入。

## 未覆盖风险

本任务没有世界接线和玩家可见行为，因此未进行客户端人工验收；真实拆除与重组成型由 `P1-MAINT-03` 及最终 P1 验收覆盖。数值无效状态由现有 `ReactorSnapshot` 与 `ReactorSimulationParameters` 构造器拒绝，本入口对 `null`、计算异常和结果数值异常统一失败关闭。

## 项目经理验收

项目经理于 `2026-09-08` 完成纯判定、结果模型、13 项新增 JUnit 和交付报告审查。独立运行 `test --rerun-tasks`，JUnit XML 汇总为 48 个测试文件、242 项测试、0 失败、0 错误、0 跳过；`runGameTestServer --rerun-tasks --max-workers=1` 明确报告 97/97 个 required GameTest 通过；完整 `build` 成功。GameTest 在全部成功后停于已知的 `Saving worlds` 退出阶段，仅终止已完成会话。测试生成日志已恢复，`git diff --check` 通过。本任务不要求客户端人工验收，正式验收并归档。
