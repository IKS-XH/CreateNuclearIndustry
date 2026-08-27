# P1-LOOP-02：反应堆端口绑定验收报告

**归档状态：** 已完成并通过项目经理验收
**验收日期：** 2026-08-27
**原始报告：** `build/reports/p1/P1-LOOP-02.md`

## 任务结论

已完成反应堆冷端、热端和换料端口的服务端结构绑定。端口方块实体只保存运行时绑定缓存，不创建第二份反应堆模拟或库存；权威状态仍由对应结构的 `reactor_instrument_port` 持有。

## 端口绑定表

| 端口 | 绑定目标 | 绑定条件 | 失效行为 |
| :--- | :--- | :--- | :--- |
| `reactor_cold_port` | 仪表端口拥有的共享冷却剂账本 | 端口位于合法结构侧面槽位且结构扫描有效 | 未绑定时不提供正式冷却剂 capability |
| `reactor_hot_port` | 同一仪表端口拥有的共享热冷却剂账本 | 端口位于合法结构侧面槽位且结构扫描有效 | 未绑定时不提供正式冷却剂 capability |
| `reactor_refueling_port` | 其正下方燃料列的 `CoreColumnPosition` | 端口位于燃料列顶部，且该列经结构扫描确认为 `FUEL` | 结构失效、错位或列角色不匹配时不提供列绑定 |

冷端和热端使用 `column = null` 表示全堆账本；换料端口使用非空列坐标表示单列绑定。多个冷/热端口的 capability 都指向同一个仪表端口快照，但每个物理端口继续独立使用 `perPortFlowMbPerTick` 配额，不增加全堆流量上限，也不复制冷/热缓冲。

## 绑定和重扫行为

1. 结构扫描成功后，仪表端口先按上一次缓存清理旧绑定，再依据最新的侧面端口列表和九列映射重建绑定；每个端口绑定建立或清除时都调用 `level.invalidateCapabilities(portPos)`。
2. 只有实际世界中存在与扫描结果一致的端口方块实体时才建立绑定，不以坐标列表伪造 capability。
3. 结构无效、端口错位、重复仪表端口或端口拆除触发重扫后，仪表端口清除全部已知绑定；剩余端口不能继续访问该反应堆账本。
4. 同一结构重复重扫采用清理后重建，绑定数量保持不变，不会重复计数。
5. 端口方块实体和绑定缓存不写入 NBT。区块或服务器重载后由仪表端口、端口方块实体的服务端 `onLoad` 安排结构重扫，重建合法绑定。
6. 流体 capability 只接受绑定后的冷/热端口；换料端口不暴露流体 capability，后续换料事务仍必须使用其绑定列。
7. 从世界 capability 查询取得的旧流体处理器会再次校验端口绑定；结构失效后即使外部仍持有旧对象，也不能继续修改共享库存。

NeoForge 1.21.1 的 block capability 规则要求 capability 出现、变化或消失时主动调用 `level.invalidateCapabilities(pos)`。本任务在端口 `bindTo` 的新绑定/绑定变化分支和 `clearBinding` 的清除分支执行该通知；这覆盖了结构成型、结构失效、结构修复重扫以及绑定目标变化，避免 `BlockCapabilityCache` 持有过期的空值或旧处理器。

## 失败行为

- 侧面端口不在合法槽位、顶部端口不在燃料列顶面、主体/顶盖不匹配或结构中存在重复仪表端口时，结构扫描返回无效诊断，所有旧端口绑定被清除。
- 端口方块实体缺失时，即使结构契约坐标包含该端口，也不建立运行时绑定；因此不会凭空增加冷却能力或生成单列观察入口。
- capability 通过绑定缓存读取仪表端口，不再对每次 capability 请求执行邻域全量搜索。
- 端口绑定不会改变裂变、燃耗、控制棒、损伤、传播或融毁状态；本任务不实现换料事务。

## 实际修改文件

- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorCoolantFluidHandler.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/P1Loop02GameTests.java`

执行者未修改核心文档、活动计划和 Git 历史。归档、状态更新与提交由项目经理完成。

## 自动验证

| 命令 | 结果 |
| :--- | :--- |
| `./gradlew.bat build --rerun-tasks --max-workers=1` | 通过；145/145 JUnit，0 失败、0 错误、0 跳过 |
| `./gradlew.bat runGameTestServer --rerun-tasks --max-workers=1` | 29/29 required GameTest 通过 |
| `git diff --check` | 任务原子写集通过 |

新增 GameTest：

- `portsBindToSharedLedgerAndUniqueFuelColumns`
- `portBindingsClearOnInvalidationAndReturnAfterRescan`
- `misplacedOrDuplicatePortsAreRejectedWithoutBinding`
- `capabilityCacheFollowsBindingLifecycle`

测试覆盖多端口共享账本与单端口限流、燃料列唯一绑定、重复重扫幂等、端口拆除后的绑定清理、端口恢复重绑、错位端口拒绝、重复仪表端口拒绝，以及真实 `BlockCapabilityCache` 的“未成型为空 → 成型出现 → 结构失效消失 → 旧处理器不可写入 → 修复重扫重新出现”生命周期。

## 中文注释与兼容性

本任务新增或触及的手写注释和 Javadoc 使用简体中文；标识符、注册 ID 和 NeoForge/Create API 名称保持原文。

本任务没有新增或修改持久化字段、注册 ID、网络协议或流体身份；绑定缓存是非持久化运行时数据。capability 失效通知只影响运行时缓存刷新，不改变快照存档格式。现有快照 NBT 版本 2 和 `FuelBurnRemainder` 兼容策略不变。

换料放入、取出、燃尽产出、玩家事务和动力机械臂事务属于后续 `P1-REFUEL-01` 至 `P1-REFUEL-03`，未在本任务提前实现。
