# P1-REFUEL-01：燃料组件换料事务交付报告

## 任务结论

技术验收通过，正式归档待项目经理确认。本任务在 `reactor_refueling_port` 的单列绑定上实现燃料组件放入、取出、耐久度保持、燃尽后冷却乏燃料产出和事务拒绝规则。

- 每个燃料列最多持有一个燃料组件。
- 燃料组件的耐久度由正式 `fresh_fuel_assembly` `ItemStack` 表示，并在放入、燃烧、取出之间保持一致。
- 燃料耗尽后不建立热乏燃料中间态；停止放热后取出时直接转换为 `cooled_spent_fuel_assembly`。
- 换料端口不保存第二份库存或模拟状态，事务成功后才将结果提交回仪表端口拥有的唯一 `ReactorSnapshot`。
- 当前放热量由服务端权威快照和配置计算，调用方不能传入客户端或端口自持的放热量绕过运行中拒绝。

## 换料事务状态图

以下状态图描述 `reactor_refueling_port` 的放入/取出事务。每条拒绝分支都返回失败结果，原输入 `ItemStack`、燃料列和反应堆快照保持不变；成功分支才提交新的列状态。

```text
                              ┌──────────────────────────────┐
                              │ 端口绑定是否有效？            │
                              └──────────────┬───────────────┘
                                             │否
                                             ▼
                              ┌──────────────────────────────┐
                              │ INVALID_PORT                  │
                              │ 拒绝；状态不变                │
                              └──────────────────────────────┘
                                             │是
                                             ▼
                    ┌────────────────────────────────────────────┐
                    │ 选择事务：放入 insert / 取出 extract        │
                    └───────────────┬────────────────────────────┘
                                    │
              ┌─────────────────────┴─────────────────────┐
              │                                           │
              ▼                                           ▼
      ┌──────────────────┐                         ┌──────────────────┐
      │ 放入 insert       │                         │ 取出 extract      │
      └────────┬─────────┘                         └────────┬─────────┘
               │                                           │
               ▼                                           ▼
      ┌──────────────────┐                         ┌──────────────────┐
      │ 当前是否停止放热？│                         │ 当前是否停止放热？│
      └───┬──────────┬───┘                         └───┬──────────┬───┘
          │否        │是                                │否        │是
          ▼          ▼                                  ▼          ▼
   ┌────────────┐  ┌──────────────────────┐      ┌────────────┐  ┌──────────────────┐
   │ COLUMN_    │  │ 列是否为空？          │      │ COLUMN_    │  │ 列是否有燃料？    │
   │ ACTIVE     │  └───┬──────────────┬───┘      │ ACTIVE     │  └───┬─────────┬────┘
   │ 拒绝不变    │      │否            │是         │ 拒绝不变    │      │否       │是
   └────────────┘      ▼              ▼           └────────────┘      ▼         ▼
                ┌──────────────┐  ┌──────────────────────────┐   ┌──────────┐  ┌──────────────┐
                │ COLUMN_      │  │ 输入是一个有效新燃料？   │   │ COLUMN_  │  │ 燃料是否耗尽？│
                │ OCCUPIED     │  └────┬─────────┬───────────┘   │ EMPTY    │  └───┬──────┬───┘
                │ 拒绝不变      │       │否        │是            │ 拒绝不变  │      │否     │是
                └──────────────┘       ▼          ▼             └──────────┘      ▼       ▼
                               ┌────────────┐  ┌────────────────────────┐  ┌──────────────┐ ┌──────────────────┐
                               │ EMPTY_      │  │ INSERTED                │  │ REMOVED      │ │ REMOVED          │
                               │ INPUT /     │  │ 空列 + 有效新燃料 +     │  │ 取出可用燃料 │ │ 取出冷却乏燃料   │
                               │ WRONG_ITEM / │  │ 停止放热；装入并消耗一个 │  │ 保留原耐久度 │ │ 已耗尽燃料不进入 │
                               │ EXHAUSTED_  │  │ 物品，返回剩余输入      │  └──────────────┘ │ 热乏燃料中间态   │
                               │ INPUT       │  └────────────────────────┘                   └──────────────────┘
                               │ 拒绝不变     │
                               └──────────────┘
```

事务规则摘要：

| 前置状态 | 事务 | 结果 | 状态变化 |
|---|---|---|---|
| 空列、有效新燃料、停止放热 | 放入 | `INSERTED` | 装入一个燃料组件，输入只消耗一个物品，列变为已装填状态 |
| 已装填可用燃料、停止放热 | 取出 | `REMOVED` | 取出保留当前耐久度的 `fresh_fuel_assembly`，列变为空 |
| 已耗尽燃料、停止放热 | 取出 | `REMOVED` | 取出一个 `cooled_spent_fuel_assembly`，列变为空 |
| 正在放热 | 放入或取出 | `COLUMN_ACTIVE` | 拒绝，列、输入和快照不变 |
| 放入时列已占用 | 放入 | `COLUMN_OCCUPIED` | 拒绝，列、输入和快照不变 |
| 放入物品为空 | 放入 | `EMPTY_INPUT` | 拒绝，列、输入和快照不变 |
| 放入物品不是正式新燃料 | 放入 | `WRONG_FUEL_ITEM` | 拒绝，列、输入和快照不变 |
| 放入已耗尽新燃料或冷却乏燃料 | 放入 | `EXHAUSTED_FUEL_INPUT` | 拒绝，列、输入和快照不变 |
| 取出时列为空 | 取出 | `COLUMN_EMPTY` | 拒绝，列、输入和快照不变 |
| 端口错位、结构失效或无有效绑定 | 放入或取出 | `INVALID_PORT` | 拒绝，不创建或修改任何列状态 |

## 状态与所有权约束

1. `FuelRefuelingTransaction` 是无副作用的事务计算入口：先复制输入并计算结果，不直接修改调用者传入的 `ItemStack`；只有端口确认成功后才提交快照。
2. 放入事务从正式燃料物品读取 `damage/maxDamage`，燃料列保存对应的 `FuelAssemblyState`；取出事务再按相同耐久度重建正式燃料物品，因此燃料组件往返不会重置燃耗。
3. 燃烧逻辑继续在燃料列状态中推进耐久度。燃料达到最大损伤后保留为已耗尽列状态，待停止放热并执行取出事务时一次性生成冷却乏燃料，避免产生无人拥有的中间输出。
4. 每次事务只访问绑定的 `CoreColumnPosition`，不会扫描或修改其他燃料列；多列燃料、耐久度和产出相互独立。
5. 当前放热判断使用服务端仪表端口根据唯一快照计算出的该列裂变发热量；缓存热量不会被换料端口自行改写，也不能作为调用方输入覆盖权威计算。

## 实际修改文件

- `src/main/java/com/iksxh/create_nuclear_industry/reactor/FuelAssemblyItemCodec.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/FuelRefuelingTransaction.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/FuelColumnState.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorSnapshot.java`
- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/P1RefuelGameTests.java`
- `src/test/java/com/iksxh/create_nuclear_industry/reactor/FuelRefuelingTransactionTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/P1BlockEntityRegistrationContractTest.java`
- `build/reports/p1/P1-REFUEL-01.md`

未修改核心文档、活动计划、注册表资源和 Git 历史；工作区中与本任务无关的既有 Ponder、资源、日志和崩溃报告改动均未处理。

## 自动验证

| 命令 | 结果 |
|---|---|
| `./gradlew.bat test --rerun-tasks` | 151/151 JUnit 通过 |
| `./gradlew.bat runGameTestServer` | 32/32 required GameTest 通过 |
| `./gradlew.bat build` | 完整构建通过 |
| `git diff --check`（任务代码范围） | 通过；工作区既有日志文件的尾随空格提示未处理 |

新增或重点覆盖的 GameTest：

- `emptyColumnLoadsOneFreshAssembly`
- `runningColumnRejectsUntilFissionStops`
- `exhaustedFuelProducesCooledSpentFuelAndKeepsOtherColumns`

JUnit 额外覆盖空输入、错误燃料、已耗尽输入、运行中拒绝、耐久度往返、完整性与缓存热量保持，以及多列独立性。运行中拒绝测试同时确认服务端权威发热判断和失败时无状态变更。

## 兼容影响

- 未新增注册 ID、网络协议、数据组件或持久化字段。
- `ReactorSnapshotNbtCodec` 的现有格式版本保持为 2；燃料耐久度与 `FuelBurnRemainder` 使用既有快照兼容路径。
- 未引入热乏燃料物品或热乏燃料中间状态。
- 换料事务不改变控制棒、冷却剂、损伤、传播或融毁状态机的既有接口。

## 明确留给后续任务的范围

- 玩家右键交互尚未实现，包括玩家手持物品交互、取出物品接收和交互提示；留给后续 `P1-REFUEL-02`。
- Create 机械臂自动换料尚未实现，包括输入/输出库存接口、机械臂过滤和自动事务接线；留给后续 `P1-REFUEL-03`。
- 本任务只提供可复用的服务端单列事务，不提前实现 GUI、通用物流或机械臂动画。

## 交付状态

技术验收项目全部通过，报告已补交。未提交任何文件，未改变暂存区或既有工作区改动；正式归档由项目经理按本报告确认。

## 项目经理最终验收

项目经理于 `2026-08-28` 完成代码审查和隔离复验：完整构建成功，151 项 JUnit 与 32 个 required GameTest 全部通过。补交报告包含换料事务状态图、修改清单、兼容边界和后续留项，本任务正式验收并归档。
