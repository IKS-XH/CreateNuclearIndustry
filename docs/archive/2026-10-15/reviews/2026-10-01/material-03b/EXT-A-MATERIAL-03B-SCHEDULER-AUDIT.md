# EXT-A-MATERIAL-03B：GameTest 调度静态补审

日期：2026-10-01。工作区：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，锁定环境 Minecraft 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280。依照 03B 卡只做静态核对；未运行 Gradle/游戏，未改代码、既有审计、构建、Git或主执行者文件。实际沿用已读取的 `minecraft-modding`、`minecraft-testing` 与 `systematic-debugging` 技能。

## 调度表写入入口

锁定源码为 `C:/Users/IKSXH/.gradle/caches/neoformruntime/intermediate_results/sourcesAndCompiledWithNeoForge_4a83a73d95fdbeba9a47b7d6bbaabb6346ecca49_output.jar`：

- `GameTestInfo.java:127–138` 在 `startTest()` 返回后取得 `runAtTickTimeMap` iterator；对到期条目先运行 Runnable（133），再 `iterator.remove()`（138）。`GameTestInfo.java:169–170` 是被调度器使用的唯一 map 写入点 `setRunAtTickTime(...): map.put(...)`。
- `GameTestHelper.java:804–810` 中 `runAtTickTime` 和 `runAfterDelay` 会写入上述 map。
- 除这两个显式 API，helper 还有以下会隐式写同一 map 的入口：`pulseRedstone`（324–327，间接调用 `runAfterDelay`）；`assertAtTickTimeContainerContains/Empty`（722–728，调用 `runAtTickTime`）；`failIfEver`（858–861）与 `onEachTick`（930–933）逐 tick 调用 `setRunAtTickTime`。若在 `GameTestInfo` 的 map 回调内调用这些入口，也会落入相同的“迭代期间写 map”风险窗口。
- 该源码中没有发现 `setBlock`、结构准备/加载或测试初始入口会隐式写调度 map。初始测试函数在 iterator 创建前执行；`GameTestInfo.tickInternal` 先完整遍历 map，再于 151–152 行处理 `GameTestSequence`。因此 `succeedWhen/succeedIf/succeedOnTickWhen/failIf/startSequence` 本身走 sequence 队列，不在此 iterator 窗口写 map；其谓词若调用 map 写 API，运行时点仍需看它是否由 sequence 阶段调用。

上述只界定静态写入路径与风险窗口；不能单凭它们断言造成历史 `wrapped == null` 异常。实际触发和因果仍由 03B 隔离复现来确认。

## 仓库 GameTest 嵌套调度补充清单

完整扫查 `src/main/java/com/iksxh/create_nuclear_industry/gametest` 内 GameTest 中直接调度 API 及上述 helper 入口。最突出的批量注册候选是：

- `P1LoopGameTests.dynamicTelemetryPacketReachesClientWithinTenTicks`：`@GameTest(timeoutTicks = 120)`，在第247行的 `runAfterDelay(5, ...)` 回调里于第271行调用 `helper.onEachTick`。该回调在测试 tick 5 执行；锁定 helper 的 `onEachTick` 用 `LongStream.range(getTick(), timeoutTicks)`，因此调用时会为 tick 5 到119逐项写入同一个 `runAtTickTimeMap`，共115次 `setRunAtTickTime`。这比只增加一个待执行条目的嵌套 `runAfterDelay` 更可能触发 map 扩容，是当前静态审计中优先级最高的批量增长候选；尚无该方法在 `defaultBatch:2` 造成原 NPE 的因果复现。

另外发现以下少量直接嵌套，均只是静态候选：

- `P1CoolantCapacityGameTests.realLayoutsDeriveThreeAndNineThousandSharedCapacity`：`runAfterDelay(5)` 回调内在第51行继续安排一次，后续回调第57行再安排一次，构成三层时序链。
- `P1CoolantGameTests.formalCoolantCapabilitiesConvertDrainAndReloadConservatively`：第44行延时回调中，第80行继续 `runAfterDelay(1)`。
- `P1StructureGameTests.wrenchRequestsRescanAfterDirectComponentMutation`：外层延时回调中第180行安排后续延时断言。

03A 已列的少量候选是 `P1ControlGameTests.redstoneOnControlRodDriveDoesNotTriggerScram`、`P1StructureGameTests.structureFormsOnLoadAndBuildsColumnCache`、`structureInvalidatesOnBreakRestoresOnPlacementAndPreservesState`、`P1Refuel02aGameTests.instrumentReplacementRetainsPortFuel`、`P1Refuel03GameTests.armPointRejectsInvalidTargetsAndRollsBack`。控制棒文件正由主执行者补运行时日志，本轮未读取其在途改动。

不能仅由静态类名顺序把这些方法分配到历史 `defaultBatch:2`：锁定 NeoForge `GameTestHooks.java:46,51–58` 将扫描到的测试方法放入 `HashSet` 后直接迭代注册；`GameTestRegistry.register(Method)` 追加方法，而按方法名排序只出现在 `register(Class)` 路径；`GameTestBatchFactory.java:17–33` 再按传入顺序每50项切批。历史日志只记 batch 名和测试数，没有当前运行的 test ID。由于全局方法集合遍历顺序没有稳定保证，现有静态/日志证据不能证明 `dynamicTelemetryPacketReachesClientWithinTenTicks` 或上述任一方法属于触发异常的第二批。

扫描中的假阳性已排除：`ExtensionOreGameTests.allNineInputsRunThroughPoweredCrushingWheels` 与 `ExtensionSteelProcessingGameTests.realCrushingWheelsProcessThreeInputs` 的循环内多次调用都在初始测试函数同步阶段注册，不是在某个调度 Runnable 中重入。`pulseRedstone`、`assertAtTickTimeContainerContains/Empty`、`failIfEver` 在本仓库测试中未见调用点；`onEachTick` 的嵌套调用点是上述 P1Loop 测试。没有找到另外的自定义测试辅助函数暗中调用这些 map 写 API。

## 对主执行者的直接线索

单项控制棒方法尚未在 tick 7 复现异常，不能因此排除 map 写入风险。优先核验遥测方法在 tick 5 的115项注册；其余候选是冷却剂容量三层延时链、冷却剂测试的嵌套延时链、结构重扫延时断言及03A原列方法。若遥测单项通过，也不能据此推断它属于或不属于历史第二批。本审计没有运行复现，不对原异常归因；批次内的具体 test ID 需要运行时日志补证。
