# P1-REFUEL-02：玩家换料交互交付报告

## 任务结论

已完成玩家通过顶部 `reactor_refueling_port` 进行局部换料的服务端交互，成功换料和失败拒绝路径均已闭环。玩家能力不按权限、身份或所有权区分；所有玩家都通过同一方块交互入口和同一套 `FuelRefuelingTransaction` 校验。

- 手持有效新燃料组件右键空列：装入一个组件，并只消耗手中一个物品。
- 空手右键已装填且目标列停止裂变放热的换料端口：取出组件，保留当前耐久度。
- 已耗尽燃料按 P1-REFUEL-01 事务规则直接取出冷却乏燃料。
- 目标列仍有裂变发热、端口未绑定、物品错误、空列取出或列已占用时：动作栏给出本地化提示，失败不消耗物品、不改变列状态和反应堆快照。
- 目标列的停止条件是该列服务端当前裂变发热为零；余热缓存本身不阻止局部换料，且不会被交互清除。

## 玩家交互验收步骤和结果

以下步骤对应真实 NeoForge 服务端 `BlockState.useItemOn` 交互入口，由 `P1RefuelPlayerGameTests` 自动执行；提示文本同时提供中文和英文语言键。

| 步骤 | 前置状态与玩家动作 | 预期结果 | 验收结果 |
|---|---|---|---|
| 1 | 合法结构已成型；目标列由相邻全插入控制棒隔离；玩家主手持有 2 个耐久度为 `12345` 的新燃料组件，右键顶部端口 | 装入 1 个组件，手中余 1 个，目标列保存耐久度 `12345` | 通过 |
| 2 | 延续步骤 1；玩家清空主手并再次右键同一端口 | 取回 1 个新燃料组件，耐久度仍为 `12345`，目标列变为空 | 通过 |
| 3 | 目标列存在有效燃料且当前裂变发热大于零；玩家空手右键 | 拒绝，动作栏提示正在放热；手和完整 `ReactorSnapshot` 不变 | 通过 |
| 4 | 相邻控制棒完全插入，目标列裂变发热为零，但列仍保存 `9.0 HU` 缓存余热；玩家空手右键 | 允许取出燃料，保留耐久度；`9.0 HU` 余热缓存不被换料清除 | 通过 |
| 5 | 目标列由 SCRAM 保持全插入且裂变发热为零；玩家空手右键 | 允许该列局部取料；整堆 SCRAM 请求仍保留 | 通过 |
| 6 | 空列中玩家手持 3 个铁锭右键 | 拒绝错误物品；铁锭数量和反应堆快照不变 | 通过 |

## 服务端交互边界

1. 换料端口方块只对 `reactor_refueling_port` 状态处理玩家换料；冷端口和热端口继续走默认方块交互路径。
2. 客户端只返回交互预测结果，不读取或写入反应堆状态；服务端重新验证实际方块、绑定记录、目标列和当前裂变发热。
3. 放入成功后将事务返回的 `remainingInput` 写回交互手；失败时不写回、不调用 `shrink`，因此错误物品、运行中状态和无效端口均不会消耗玩家物品。
4. 取出成功后将事务输出直接写入空的交互手；服务端先完成列状态事务，再把输出放入玩家手中。空手交互天然提供了一个不会丢失输出的接收位置。
5. 所有拒绝结果使用可消费的交互返回值，避免错误物品继续进入原版 `Item.useOn`；这不等于消耗物品，只是截断当前方块交互链。
6. 事务仍然只替换绑定的 `CoreColumnPosition`，不会重置其他列、冷却剂、控制棒、SCRAM、损伤、热量或融毁进度。

## 本次修改文件

- `src/main/java/com/iksxh/create_nuclear_industry/block/ReactorPortBlock.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/FuelRefuelingTransaction.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/P1RefuelPlayerGameTests.java`
- `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`
- `src/main/resources/assets/create_nuclear_industry/lang/en_us.json`
- `build/reports/p1/P1-REFUEL-02.md`

未修改核心文档、活动计划、注册 ID、网络协议、NBT 编解码和 Git 历史；工作区中与本任务无关的既有文档、Ponder、资源、日志和崩溃报告改动均未处理。

## 自动验证

| 命令 | 结果 |
|---|---|
| `./gradlew.bat test --rerun-tasks --no-daemon` | 151/151 JUnit 通过 |
| `./gradlew.bat runGameTestServer --no-daemon` | 37/37 required GameTest 通过 |
| `./gradlew.bat build --no-daemon` | 完整构建通过 |
| 任务范围 `git diff --check` 与新增文件空白检查 | 通过；工作区既有日志文件提示未处理 |

新增玩家交互 GameTest：

- `playerRightClickLoadsAndExtractsFuel`
- `playerRightClickRejectsRunningColumnWithoutMutation`
- `playerRightClickAllowsResidualHeatAfterFissionStops`
- `playerRightClickWorksDuringScram`
- `playerRightClickRejectsWrongItemWithoutMutation`

## 兼容影响

- 未新增物品、方块、数据组件、NBT 字段或网络 payload；复用 P1-REFUEL-01 的物品注册和快照格式 2。
- 新增的本地化消息只用于动作栏反馈，不改变注册 ID 或存档格式。
- 客户端预测和服务端提交均复用已有单列事务；没有新增端口库存，也没有引入玩家私有反应堆状态。

## 未实现项与后续任务

- Create 动力机械臂自动换料尚未实现：包括机械臂 API 适配、合法输入/输出访问、过滤和中途失败回滚，留给 `P1-REFUEL-03`。
- 本任务不实现 GUI、菜单、普通漏斗/通用管道访问或跨列换料。
- 本次自动验收覆盖服务端真实玩家交互入口；客户端多人/视觉人工验收仍可由项目经理按步骤 1～6 复验。

## 交付状态

玩家换料交互的技术验收项全部通过，报告已补交。未提交任何文件，未改变暂存区或任务范围外既有工作区改动；正式归档由项目经理确认。

## 项目经理最终验收

项目经理已完成代码审查和隔离复验：完整构建成功，151 项 JUnit 与 37 个 required GameTest 全部通过。用户于 `2026-08-30` 确认客户端人工验收完成，真实鼠标交互、动作栏反馈和玩家侧物品变化通过。本任务正式验收并归档。
