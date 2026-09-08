# P1-MAINT-01 交付报告：拦截危险状态拆除

**任务 ID：** `P1-MAINT-01`
**状态：** 已完成并归档；项目经理自动验收通过
**基准分支：** `main`
**基准提交：** `eb68850467f5d6fd5653607a3e98b086013fe137`
**技术基线：** Minecraft `1.21.1`、Java `21`、NeoForge `21.1.219`、Create `6.0.10-280`、Ponder `1.0.82`

## 实际使用的技能

- `minecraft-modding`：`C:/Users/lenovo/.codex/skills/minecraft-modding/SKILL.md`。用于核对 NeoForge 1.21.1 的 `BlockEvent.BreakEvent` 服务端生命周期、方块实体权威状态、事件总线和现有结构缓存接线。
- `minecraft-testing`：`C:/Users/lenovo/.codex/skills/minecraft-testing/SKILL.md`。用于设计 JUnit/GameTest 覆盖、required GameTest 运行方式和服务端边界验证。

## 实现摘要

`ReactorStructureLifecycle.onBlockBreak` 现在在原方块移除前，以固定结构尺寸推导出的每轴正负 4 方块范围收集所有可能拥有该槽位的仪表端口方块实体。候选端口只使用最近一次有效结构缓存确认槽位归属，允许一个位置影响多个有效结构，但每个权威仪表坐标每次回调最多处理一次。

仪表端口的 `tryCommitDangerousDisassembly` 只在逻辑服务端、方块状态仍与世界一致、缓存结构槽位确实属于八类固定组件，且满足危险状态真值表时执行：先将 `meltdownProgressTicks` 提升到当前配置倒计时上限、保持 `meltdownCountdownStarted == true`、写入 `meltdownEventPublished == true`，通过 `setSnapshot` 提交，再调用已有唯一入口发布 `DANGEROUS_DISASSEMBLY`。未执行正式 reactor tick，不消耗燃料或冷却剂，不改变列完整度、控制棒/SCRAM、缓存热量或端口物品。

原 `BreakEvent` 从不被取消；之后仍按既有生命周期安排结构重扫，实际方块移除由原事件链继续处理。

## 危险状态真值表

| 当前服务端权威条件 | `meltdownCountdownStarted` | `meltdownProgressTicks` | `meltdownEventPublished` | 结果 |
| --- | ---: | ---: | ---: | --- |
| 当前配置计算出的新生裂变热 `> 1.0E-12 HU/t` | `false` 或 `true` | `< countdownTicks` | `false` | 提交完成状态并发布一次 |
| 新生裂变热不超过 epsilon，但倒计时正在运行或暂停 | `true` | `< countdownTicks` | `false` | 提交完成状态并发布一次 |
| 只有缓存余热、冷/热库存，未达到前两行 | `false` | `0` | `false` | 不发布、不改写 |
| 已完成：进度已达上限，或事件去重标记已写入 | 任意 | `>= countdownTicks` 或已标记 | `true`/任意 | 不重复发布、不改写 |
| `BreakEvent` 已取消、客户端/非 `ServerLevel` | 任意 | 任意 | 任意 | 不发布、不改写 |
| 无有效缓存结构、槽位 ID 不匹配、普通方块或附近同 ID 方块 | 任意 | 任意 | 任意 | 不发布、不改写 |

“倒计时暂停”只依赖已有 `meltdownCountdownStarted` 状态口径，覆盖 SCRAM 暂停和有效冷却暂停；本任务不定义完全停机阈值，也不重置倒计时。

## 八类组件覆盖矩阵

| 组件 | 缓存槽位判定 | GameTest 覆盖 |
| --- | --- | --- |
| `reactor_casing` | 固定边界或空列帽 | `fissionRunningCoversAllEightComponents` |
| `reactor_window` | `windowPositions()` | `fissionRunningCoversAllEightComponents` |
| `reactor_instrument_port` | `ScanResult.ports(INSTRUMENT)` | 直接破坏仪表端口 |
| `reactor_cold_port` | `ScanResult.ports(COLD_COOLANT)` | `fissionRunningCoversAllEightComponents` |
| `reactor_hot_port` | `ScanResult.ports(HOT_COOLANT)` | `fissionRunningCoversAllEightComponents` |
| `reactor_refueling_port` | 燃料列帽 | `fissionRunningCoversAllEightComponents` |
| `reactor_fuel_rod` | 燃料列主体 | `fissionRunningCoversAllEightComponents` |
| `control_rod_drive` | 控制棒列帽 | `fissionRunningCoversAllEightComponents` |

## 提交与发布时序

```text
BreakEvent（方块仍存在）
  → 服务端/取消检查
  → 收集附近唯一仪表端口坐标
  → 最近一次有效结构缓存匹配八类组件和局部槽位
  → 只读计算当前配置下的新生裂变热与倒计时边界
  → 构造只改变融毁完成三元组的下一快照
  → 仪表端口 setSnapshot 提交权威状态
  → ReactorMeltdownEvents.publish(DANGEROUS_DISASSEMBLY)
  → 原 BreakEvent 链路移除方块
  → 既有服务端 tick 末端结构重扫
```

事件回调观察到的快照就是 `setSnapshot` 已提交的同一不可变实例；事件不会反向修改快照，也没有第二个发布器。

## 实际修改文件

- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorStructureLifecycle.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/P1Maint01GameTests.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/P1StructureGameTests.java`：同步既有“破坏后保留快照”回归断言，使其保留非融毁字段并接受本任务冻结的危险完成状态。
- `src/test/java/com/iksxh/create_nuclear_industry/reactor/ReactorMeltdownEventsTest.java`：直接覆盖危险拆除入口的真实 `ClientLevel` 短路。

未修改 `ReactorMeltdownEvent` 载荷合同、事件发布器、热工/燃耗/损伤公式、完全停机阈值、注册 ID、配置、流体 capability、Gradle、核心文档、活动计划、模拟器或 Git 历史。

## 自动验证

- `./gradlew.bat test --tests com.iksxh.create_nuclear_industry.reactor.ReactorMeltdownEventsTest`：通过，2 个测试；新增测试直接调用 `tryCommitDangerousDisassembly`，确认真实 `ClientLevel` 下返回 `false`、权威快照不变且没有 `DANGEROUS_DISASSEMBLY` 事件。
- `./gradlew.bat test`：通过，JUnit `229/229`，`failures=0`、`errors=0`。
- `./gradlew.bat runGameTestServer`：`97` 个 required GameTest 全部通过，其中新增 `P1Maint01GameTests` 的 4 个测试和更新后的结构回归均通过。NeoForge 在 `Saving worlds` 阶段未自行退出，按项目既有运行约定终止外层进程；required 测试本身为 `97/97`。
- `./gradlew.bat build`：通过。
- 项目经理恢复测试生成的根目录日志后，任务范围及全局 `git diff --check` 通过。

新增 GameTest 覆盖：

- 裂变运行时八类组件、包括直接破坏仪表端口；事件原因、原事件未取消、提交快照、方块/实体/燃料物品不变。
- 事件监听器在同步回调当下直接断言服务端 `level`、维度、结构原点、被破坏方块仍存在，以及事件快照已等于仪表权威快照。
- 已启动的运行中倒计时、SCRAM/有效冷却暂停口径、重复回调和不同组件回调的去重。
- 安全停机、只有缓存余热、已经完成、预先取消的 `BreakEvent`、普通方块、附近同 ID 但无有效结构归属。
- 相邻双堆只由真正拥有被破坏槽位的仪表端口发布，邻堆快照保持不变。
- 客户端危险拆除入口由 `ReactorMeltdownEventsTest.clientLevelCannotCommitDangerousDisassembly` 直接覆盖；测试使用真实 `ClientLevel` 类型，事务入口拒绝客户端，生命周期入口同时只接受 `ServerLevel`，事件总线无投递且快照不变。

## 零附加世界副作用

危险拆除提交只写仪表端口权威快照及其同步脏标记，并发布内部事件。它不执行正式模拟 tick，不写燃料端口、不改变冷/热库存、不修改方块/实体/掉落/声音/粒子/火焰/爆炸/污染/聊天/事故日志，也不取消原方块破坏。结构重扫仍在世界修改完成后的既有服务端 tick 阶段执行。

## P1-MAINT-02/03 接口说明

- `P1-MAINT-02` 仍负责定义完全停机条件；本任务只消费已有的 `meltdownCountdownStarted` 与配置倒计时上限，不把缓存余热、库存或未定义阈值解释成完全停机或危险拆除条件。
- `P1-MAINT-03` 仍负责破坏性重组成型/重置边界；本任务不移除多余方块、不清空快照、不重置控制棒或库存。
- 后续事故系统应继续复用 `ReactorMeltdownEvents.publish`，并仅通过 `ReactorMeltdownEvent.Reason.DANGEROUS_DISASSEMBLY` 区分本原因；本任务不实现任何事故效果。

## 未覆盖风险

- 本任务没有客户端人工验收，因为 P1 危险拆除仍只有服务端事件占位符，没有玩家可见事故效果。
- required GameTest 使用真实 NeoForge 服务端和合成 `BreakEvent` 覆盖状态矩阵；项目最终验收仍应由项目经理按既有流程复核真实玩家破坏链路和提交差异。
- 测试运行产生的 `logs/` 与 `run/` 运行产物不纳入任务交付，且未执行任何 Git 写操作。

## 项目经理验收

项目经理于 `2026-09-08` 完成代码、测试和报告审查。首轮退回补齐真实客户端事务短路测试，以及事件同步回调中的提交顺序与位置载荷断言；整改后独立运行 `test --rerun-tasks`，JUnit XML 汇总为 47 个测试文件、229 项测试、0 失败、0 错误、0 跳过；`runGameTestServer --rerun-tasks --max-workers=1` 明确报告 97/97 个 required GameTest 通过；完整 `build` 成功。GameTest 在全部成功后停于已知的 `Saving worlds` 退出阶段，仅终止已完成会话。本任务不要求客户端人工验收，正式验收并归档。
