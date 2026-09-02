# P1-GOGGLE-INSTRUMENT-03 动态遥测验收归档

日期：2026-08-31

## 验收结论

项目经理复核服务端正式 tick 派生、独立遥测编解码、方块实体更新包节流、结构失效与重载边界后，确认本任务完成。动态遥测只读来源于 `ReactorServerTick.Result` 与结算后 `ReactorSnapshot`，不会反向驱动模拟，不写入持久化反应堆 NBT，也不会触发额外结构扫描。

项目经理独立执行：

- `./gradlew.bat test --rerun-tasks`：169/169 JUnit 通过，0 失败、0 错误、0 跳过。
- `./gradlew.bat runGameTestServer --rerun-tasks`：45/45 required GameTest 通过。
- `git diff --check`：清理测试生成日志后通过。

本任务只提供服务端权威动态遥测和客户端只读同步，不添加护目镜动态文本，因此不要求客户端人工验收。显示与人工验收属于 `P1-GOGGLE-INSTRUMENT-04`。

## 字段来源与语义

| 字段 | 来源 | 单位与有效性 |
| --- | --- | --- |
| `coldCoolantMb` / `hotCoolantMb` | `ReactorServerTick.Result.snapshot()` 的 tick 后快照 | `mB`；只在成功正式 tick 后有效 |
| `FuelColumnTelemetry.position` | tick 后 `ReactorSnapshot.fuelColumns()` 的现有列坐标 | P1 内部零基坐标；对外列表按先 `z` 后 `x` 排序 |
| `fuelColumnIntegrity` | tick 后燃料列状态 | `[0, 1]`；无单位 |
| `generatedFissionHeatHuPerTick` | `ReactorServerTick.Result.fission().columns()` | `HU/t`；仅本 tick 新生裂变热，不含 `cachedHeatHu` 余热 |
| `ControlRodColumnTelemetry.position` | tick 后 `ReactorSnapshot.controlRodColumns()` | P1 内部零基坐标；对外列表按先 `z` 后 `x` 排序 |
| `controlRodColumnIntegrity` | tick 后控制棒列状态 | `[0, 1]`；无单位 |
| `totalGeneratedFissionHeatHuPerTick` | 所有燃料列 `generatedFissionHeatHuPerTick` 之和 | `HU/t`；模型构造器校验全堆求和不变量 |
| `convertedCoolantMbPerTick` | `ReactorCoolantLedger.Settlement.convertedCoolantMb()` | `mB/t`；实际冷态到热态转化量，不使用理论端口能力或请求量 |

## 有效性与状态所有权

- 遥测由 `ReactorInstrumentTelemetry.from(ReactorServerTick.Result)` 在正式 tick 完成后派生。
- `ReactorSnapshot`、燃料组件和冷却剂库存仍是唯一权威运行状态；遥测不反向驱动模拟。
- 瞬时速率不写入持久化 NBT；`ReactorSnapshotNbtCodec.FORMAT_VERSION` 未修改。
- 区块加载或结构重扫后，在下一次成功正式 tick 前遥测为不可用。
- 结构失效立即清空服务端和客户端可用遥测，修复重扫后不会恢复旧值，必须等待新的正式 tick。
- 不可用遥测不携带库存、列或速率明细，避免客户端显示陈旧或伪造零值。

## 同步策略

- 遥测通过 `InstrumentTelemetry` 方块实体更新数据同步，客户端只保存 `clientTelemetry` 副本。
- 客户端不运行模拟器、不扫描世界、不轮询服务端。
- 服务端只在遥测发生变化且达到节流条件时发送更新；首次有效遥测立即发送，随后最多每 5 个游戏 tick 同步一次，满足 10 tick 可见性上限。
- 结构变化触发立即更新，使失效状态优先于旧遥测到达客户端。
- 独立遥测格式版本为 `1`，解码缺字段、重复坐标、坐标冲突或非法数值时降级为不可用。

## 自动化覆盖

- 空库存与非空库存分别断言，实际转化量来自正式冷却结算。
- 多燃料列、多控制棒列、先 `z` 后 `x` 稳定排序及 tick 后完整度。
- 控制棒深度变化对应的单列与全堆新生裂变热变化，全堆值等于各列求和且不重复计入余热。
- 结构失效立即清空，修复重扫后等待下一次正式 tick 才恢复。
- 通过服务端 NBT 加载路径验证重载后首个成功 tick 前不可用。
- 通过真实 `ClientboundBlockEntityDataPacket`、区块观察者和嵌入式连接验证同步延迟不超过 10 tick。
- 重复读取服务端和客户端遥测不增加结构扫描次数，也不修改权威快照。

## 后续边界

静态摘要后的动态排版、单位格式、本地化文本和客户端人工验收由 `P1-GOGGLE-INSTRUMENT-04` 完成；真实 Create 储罐—机械泵—管道的非零库存与实际转化显示验收继续留给 `P1-COOL-04`。
