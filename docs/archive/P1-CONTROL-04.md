# P1-CONTROL-04 验收归档：控制棒滑块拖动抖动整改

**验收日期：** 2026-08-31
**状态：** 已完成
**后续任务：** `P1-COOL-04`

## 1. 验收结论

控制棒驱动器的 Create 浮动滑块已改为拖动期间完全使用客户端本地预览，异步网络响应不再反射修改 `ValueSettingsScreen`、行为缓存或物理鼠标位置。用户已完成客户端人工验收，确认整改后的拖动手感通过。

最终深度仍由 Create `ValueSettingsPacket → setValueSettings → commitFromCreate` 进入服务端权威校验；客户端只在最终 `COMMIT`/`CANCEL` 响应满足会话策略时更新驱动器展示缓存，没有把反应堆权威状态迁移到客户端。

## 2. 实现边界

- 删除 `ControlRodSliderScreenSync` 及其对 `initialSettings`、`lastHovered`、`setCursor` 的反射访问。
- 客户端不再针对每个悬停值发送 `PREVIEW`；服务端兼容协议仍保留，但预览不得写入 `ReactorSnapshot`。
- 自定义会话同时匹配驱动器位置和 `dragId`；同一驱动器旧会话的迟到最终响应不能覆盖新会话。
- `START`/`PREVIEW` 响应不更新滑块展示；最终提交成功或携带权威深度的最终拒绝才允许更新最终展示。
- 面板关闭、客户端退出和目标切换会清理客户端临时会话；服务端提交、取消和玩家退出会清理服务端会话。
- `P1-GOGGLE-INSTRUMENT-04` 的控制棒列完整度护目镜显示保持不变。

## 3. 验证证据

- 执行者报告：`build/reports/p1/P1-CONTROL-04.md`。
- 项目经理独立执行 `./gradlew.bat test --rerun-tasks`：181 项 JUnit，0 失败、0 错误、0 跳过。
- 项目经理独立执行 `./gradlew.bat runGameTestServer --rerun-tasks`：最终 47 个 required GameTest 全部通过。
- GameTest 前两次在任何模组断言执行前触发 NeoForge 1.21.1 `GameTestInfo`/fastutil 迭代器瞬态崩溃；第三次相同命令完整通过，因此该现象记录为测试框架稳定性风险，不计为功能用例失败。
- 用户完成客户端人工验收，确认拖动抖动整改通过。

## 4. 保留风险

- `ControlRodSliderClientEvents` 使用的 NeoForge `EventBusSubscriber.Bus.GAME` 在 21.1.219 中仍可工作，但编译器已将该写法标记为待移除；升级 NeoForge 时需要迁移事件注册方式。
- 服务端保留的 `PREVIEW` 阶段仅用于协议兼容与回归测试；当前客户端默认不发送逐值预览。

本归档只记录完成证据，不替代活动计划和核心设计文档。
