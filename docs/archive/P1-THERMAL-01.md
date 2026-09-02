# P1-THERMAL-01 验收归档：整数 mB 热量余数整改

**验收日期：** 2026-09-01
**状态：** 已完成
**后续门禁：** `P1-CONTROL-05`

## 1. 验收结论

正式冷却剂库存继续使用整数 `mB`，默认吸热量继续为 `0.5 HU/mB`。不足一次整数 `1 mB` 转化的安全热量余数已与真实冷却短缺分离：安全余数继续守恒缓存，但不再降低燃料列或控制棒列完整度、不参与失效传播，也不计入融毁覆盖；冷库存、热端空间或吞吐真实不足时，未覆盖热负荷仍按既有阈值和速率造成损伤。

用户已完成客户端人工验收，确认冷却能力充足时完整度稳定，制造真实短缺后损伤恢复；孤立燃料列相邻控制棒完全插入后，新生裂变热和燃耗归零，缓存余热仍可继续转化热冷却剂。

## 2. 状态与兼容性

- `FuelColumnState.cachedHeatHu` 继续表示列内未移除热量总量。
- 新增 `quantizedHeatRemainderHu`，作为缓存热量中的安全量化子集，并保持 `0 ≤ quantizedHeatRemainderHu ≤ cachedHeatHu`。
- `ReactorSnapshotNbtCodec` 格式从 v2 升至 v3；旧档缺失字段时迁移为 `0.0 HU`，不会把历史普通缓存热量误标为安全余数。
- 余数仍由 `reactor_instrument_port` 的唯一权威快照持有；端口和客户端遥测没有第二份可写状态。
- 注册 ID、配置字段、流体 capability、`0.5 HU/mB`、单端口 `128 mB/t`、冷热缓冲各 `1000 mB`、损伤阈值和损伤速率均未改变。

## 3. 验证证据

- 执行者报告：`build/reports/p1/P1-THERMAL-01.md`。
- 项目经理独立执行 `./gradlew.bat test --rerun-tasks --max-workers=1`：192 项 JUnit，0 失败、0 错误、0 跳过。
- 项目经理独立执行 `./gradlew.bat runGameTestServer --rerun-tasks --max-workers=1`：52 个 required GameTest 全部通过。
- 项目经理独立执行反应堆 HTML 模拟器 `npm test`：28 项 Node 测试全部通过。
- 用户于 2026-09-01 完成客户端人工验收。

## 4. 后续独立缺陷

人工验收后另以三行 `F-C-F` 布局测试六个直接相邻燃料列，确认正式 Java 裂变计算在反馈超频分支中丢弃控制棒抑制，导致手动全插棒和红石 SCRAM 都不能停止新生裂变。该问题不属于热量量化、冷却或损伤整改，也不推翻本任务验收；由活动任务 `P1-CONTROL-05` 统一 Java 与 HTML 模拟器的控制—反馈公式。
