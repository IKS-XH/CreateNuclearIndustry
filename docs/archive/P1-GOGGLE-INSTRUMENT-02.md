# P1-GOGGLE-INSTRUMENT-02：仪表端口静态护目镜显示

## 任务结论

已将 `reactor_instrument_port` 接入 Create 6.0.10 的 `IHaveGoggleInformation`。服务端最近一次结构扫描生成的静态摘要通过方块实体更新数据同步到客户端副本；护目镜回调只读取该副本，不扫描世界、不发起网络请求，也不写入反应堆状态。

## 显示内容

有效结构按固定顺序显示：

1. 结构尺寸；
2. 燃料列数量；
3. 控制棒列数量；
4. 冷端口数量；
5. 热端口数量；
6. 冷却剂总容量。

容量使用 `mB`，并明确表示配置容量而非当前库存。未成型、尚未同步或同步数据非法时，只显示本地化的“未成型或数据不可用”，不会显示一组零值，也不会泄露服务端内部英文失败原因。

## 状态与兼容边界

- 服务端结构缓存和配置仍是静态摘要的数据来源。
- 客户端只保存用于显示的不可写副本。
- 摘要只进入方块实体更新标签/更新包，不写入持久化反应堆 NBT。
- 本任务不显示冷/热当前库存、完整度、发热量或转化速率；动态遥测属于 `P1-GOGGLE-INSTRUMENT-03/04`。
- 未修改注册 ID、热工公式、流体能力和 Ponder 文本。

## 修改范围

- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorInstrumentStructureSummary.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/P1StructureGameTests.java`
- `src/main/resources/assets/create_nuclear_industry/lang/en_us.json`
- `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`
- `src/test/java/com/iksxh/create_nuclear_industry/P1GoggleInstrument02ContractTest.java`

执行者未修改核心文档、活动计划或 Git 历史。

## 项目经理最终验收

项目经理于 `2026-08-30` 完成代码审查和独立自动化复验：

- `162/162` 项 JUnit 通过，0 失败、0 错误、0 跳过；
- `39/39` 个 required GameTest 通过；
- `git diff --check` 在恢复测试生成日志后通过；
- 确认护目镜回调不扫描结构、不请求服务端、不修改 `ReactorSnapshot`；
- 确认字段顺序、单位、中英文语言键和无效状态降级符合任务合同。

用户随后完成客户端人工验收，确认佩戴/摘下护目镜、注视/移开仪表端口、结构破坏重扫和修复重扫后的显示均符合预期。本任务最终验收完成。
