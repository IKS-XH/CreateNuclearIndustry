# P1-CONTROL-05 验收报告：控制棒门控反馈超频燃料列

**验收日期：** 2026-09-01
**状态：** 已完成
**前置任务：** `P1-CONTROL-03`、`P1-CONTROL-04`、`P1-THERMAL-01`、`P1-SIMWEB-08`

## 1. 验收结论

已修正正式 Java 裂变计算在反馈超频分支中丢弃 `controlledIntensity` 的问题。现在控制棒先决定每列的基础裂变强度，反馈只能放大该受控强度，完全插入的相邻控制棒会同时归零该列新生热、燃耗和反馈信号。

用户已确认三行 `F-C-F` 场景的客户端人工验收通过，包含手动全插、仪表端口红石 SCRAM、`50%`/`0%` 连续恢复和部分卡死棒不完整停堆检查。

## 2. 实现范围

- `ReactorFissionCalculator` 的反馈迭代初值从固定 `1.0` 改为每列 `controlledIntensity`。
- 反馈信号继续只读取上一轮四向相邻有效燃料列的热强度；该热强度已经经过控制棒门控。
- 超频分支的最终热/燃耗强度改为：

  ```text
  controlledIntensity × (1 + (feedbackMultiplier - 1) × activation)
  ```

- `ControlRodScramService` 继续通过正式裂变计算投影 SCRAM 后热量；公式修正后，完整覆盖的反馈簇自然返回零热，未覆盖燃料和部分卡死棒仍返回 `SCRAM_INCOMPLETE`。
- 模拟器原有数值公式已经符合该合同，本任务未重写其计算；新增 `fcf` 预设、精确场景 JSON、断言和过时公式说明整改，并同步模块化页面与 `reactor-simulator-single.html`。
- 未修改注册 ID、资源、流体 capability、冷却/损伤/传播/融毁公式、配置字段、默认数值、快照 NBT 版本、核心文档或活动计划。

## 3. 精确布局和对照数值

固定结构为 `5×5×5`，堆芯有效坐标使用 `(x,z)`：

```text
z=0: F C F
z=1: F C F
z=2: F C F
```

燃料列：`(0,0)`、`(2,0)`、`(0,1)`、`(2,1)`、`(0,2)`、`(2,2)`。
控制棒列：`(1,0)`、`(1,1)`、`(1,2)`。
左右两侧各自形成四向反馈簇，中间控制棒列阻断左右燃料之间的直接邻接。

Java JUnit 与模拟器对同一布局的默认参数对照值如下；燃耗单位为燃料组件份额/tick，热量单位为 HU/t：

| 三根控制棒实际深度 | 全堆新生裂变热 | 全堆计划燃耗 | 边缘燃料列热 | 中间燃料列热 |
| ---: | ---: | ---: | ---: | ---: |
| `0%` | `62.89770874243574` | `0.0002911930960297951` | `9.90613578343568` | `11.636582804346512` |
| `50%` | `23.604377652134303` | `0.00010927952616728845` | `3.729357701356585` | `4.3434734233539825` |
| `100%` | `0` | `0` | `0` | `0` |

六列在 `0% > 50% > 100%` 下逐列热量和燃耗均严格单调；`100%` 时六列全部为零。

## 4. SCRAM 状态矩阵

| 条件 | 预期状态 | 实际合同 |
| --- | --- | --- |
| 所有有效燃料均被可动控制棒完全覆盖 | `SCRAM_ACTIVE`，裂变热为零 | 通过 |
| 存在未被控制棒覆盖的有效燃料 | `SCRAM_INCOMPLETE`，保留真实裂变 | 通过 |
| 控制棒部分卡死并保留有效裂变 | `SCRAM_INCOMPLETE`，保留真实裂变 | 通过 |
| 控制棒卡死在 `100%` 且全堆新生热为零 | `SCRAM_ACTIVE` | 通过 |
| 红石低电平释放 | `SCRAM_RELEASED`，恢复可动棒 SCRAM 前目标 | 通过 |

缓存余热和热冷却剂转化不参与 SCRAM 完整性判定。

## 5. 自动化验证

- `ReactorFissionCalculatorTest.adjacentFuelFeedbackUsesControlledPreviousRoundSignal`
- `ReactorFissionCalculatorTest.threeRowFcfLayoutIsStrictlyMonotonicAndFullyInsertedStopsAllFission`
- `ReactorFissionCalculatorTest.mixedFcfCoverageDoesNotCrossControlRodsOrEmptyCenter`
- `ReactorFissionCalculatorTest.centerEmptyEightFuelAnchorUsesFourWayFeedbackWithoutControlState`
- `ReactorScramFissionRegressionTest` 的可动棒、部分卡死棒和非相邻燃料 SCRAM 回归
- `P1ControlGameTests.fcfManualFullInsertionAndRedstoneScramStopFeedback`
- 模拟器 `simulation.test.js` 的精确 F-C-F 热量/燃耗/混合覆盖断言
- `scenarios/p1-control-05-fcf.json` 的完全插棒重放断言

项目经理于 2026-09-01 独立执行：

| 命令 | 结果 |
| --- | --- |
| `./gradlew.bat test --rerun-tasks --max-workers=1` | 195/195 JUnit 通过 |
| `./gradlew.bat runGameTestServer --rerun-tasks --max-workers=1` | 53/53 required GameTest 通过 |
| `tools/reactor-simulator/npm test` | 30/30 Node 测试通过 |

针对本任务修改的已跟踪文件执行 `git diff --check` 无空白错误。完整工作区检查仍会报告既有 `logs/debug.log` 和 `logs/latest.log` 的日志尾随空格；这些日志不属于本任务写集。

## 6. 客户端人工验收

**结果：通过（用户于 2026-09-01 确认）。**

用户确认按计划完成以下检查：

1. 在三行 `F-C-F` 六燃料结构中用滑块将三根控制棒完全插入；等待既有遥测同步窗口后，六个换料端口和仪表端口的新生裂变热归零，燃料耐久不再下降。
2. 由仪表端口红石高电平触发 SCRAM，确认完全覆盖时成功停堆。
3. 将三根控制棒调至 `50%`、`0%`，确认发热按 `0% > 50% > 100%` 连续恢复。
4. 制造部分卡死棒，确认 SCRAM 明确报告不完整，只保留真实未受抑制的裂变。

本报告只记录用户确认的验收结论，不伪造未由执行者保存的截图或客户端日志数值。

## 7. 注释、状态所有权和兼容性

- 本任务新增或修改的手写 Java/JavaScript 注释均为中文；公式注释说明了服务端/客户端边界及控制强度不绕过反馈的关键不变量。
- `ReactorSnapshot` 仍是服务端唯一持久化反应堆状态；控制棒驱动器显示缓存、仪表遥测和换料端口遥测均未成为第二份持久化状态。
- 既有存档无需迁移；没有新增 NBT 字段或改变协议字段。模拟器场景仅用于离线重放和验证。
