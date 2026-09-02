# P1-GOGGLE-INSTRUMENT-01：仪表端口静态结构摘要

## 任务结论

已完成服务端静态结构摘要。仪表端口从最近一次结构扫描缓存读取固定结构尺寸、列角色和物理端口位置，并从 `P1ServerConfig` 读取冷/热缓冲容量；普通摘要读取不会扫描世界、运行模拟或修改 `ReactorSnapshot`。

## 字段来源与语义

| 字段 | 来源 | 语义 |
| --- | --- | --- |
| `width` / `height` / `depth` | 有效 P1 结构缓存的固定结构合同 | `5 × 5 × 5` |
| `fuelColumnCount` | 缓存中类型为 `FUEL` 的列映射 | 有效燃料列数量 |
| `controlRodColumnCount` | 缓存中类型为 `CONTROL_ROD` 的列映射 | 有效控制棒列数量 |
| `coldPortCount` / `hotPortCount` | 缓存中的端口局部坐标 | 按物理位置去重后的合法冷/热端口数量 |
| `totalFluidCapacityMb` | `coldInventoryCapacityMb + hotInventoryCapacityMb` | 冷、热缓冲配置容量总和，单位 `mB`，不是当前存量 |

摘要模型为不可变 `ReactorInstrumentStructureSummary`。有效摘要必须包含结构尺寸、至少一个燃料列以及至少一个冷端和热端；未扫描或无效结构返回 `valid == false` 的不可用摘要，不会把一组零值伪装成有效结构。

## 状态与性能边界

- 结构成型、方块实体加载、组件变化和扳手重扫负责更新既有结构缓存。
- 摘要读取只访问缓存与当前服务器配置，重复读取不增加 `structureScanCount`。
- 配置容量变化不需要额外世界扫描即可反映。
- 摘要不包含燃料、控制棒、余热、当前流体存量或运行速率，也不写入反应堆 NBT。
- 本任务不实现客户端同步和护目镜排版；对应工作属于 `P1-GOGGLE-INSTRUMENT-02`。

## 修改范围

- `src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorInstrumentStructureSummary.java`
- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/P1StructureGameTests.java`
- `src/test/java/com/iksxh/create_nuclear_industry/structure/ReactorInstrumentStructureSummaryTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/P1GoggleInstrument01ContractTest.java`

执行者未修改核心文档、活动计划、注册 ID、NBT 编解码、网络协议、客户端渲染或动态热工逻辑，也未执行提交、回退、建分支或合并。

## 项目经理最终验收

项目经理于 `2026-08-30` 完成代码审查和独立复验：

- `158/158` 项 JUnit 通过，0 失败、0 错误、0 跳过；
- `38/38` 个 required GameTest 通过；
- `git diff --check` 在恢复测试生成日志后通过；
- 确认摘要只从结构缓存和服务端配置派生，不复制 `ReactorSnapshot`，重复读取不触发重扫；
- 确认未提前实现动态遥测或客户端显示，不新增 NBT/网络兼容负担。

本任务不需要客户端人工验收。`P1-GOGGLE-INSTRUMENT-02` 显示无效结构时必须使用本地化的“未成型/数据不可用”文本，不得直接把服务端内部英文 `failureReason` 展示给玩家。
