# DEVICE-PONDER-04-FRAMING 独立审查

**审查结果：** 有一项需要修正的构图缺陷；建议修正后仅复审对应差异。其余静态检查通过。用户的两页定向播放门仍未完成。

## 发现

### [P1] 小型与中型机的高亮框没有随独立区段的 Z 位移移动

`TurbinePonderScenes.java:221-225` 中，小型机的独立区段整体移动 `(0, +1, +1)`，中型机移动 `(-4, 0, +0.5)`；但 `framedTier` 在第223行仍以原始 `fz` 构造，只有正文锚点在第225行加了 `offsetZ`。Ponder 1.0.82 的 `WorldSectionElement` 平移只作用于独立区段的呈现变换，`showOutlineWithText(Selection, …)` 的选择框仍按传入的世界坐标绘制，因此小型框会落后模型1格，中型框会落后0.5格。大型 `offsetZ=0` 不受影响。

应让高亮选择也使用平移后的 Z 坐标（按项目坐标及 `Selection` 的整格边界规则处理半格中型位移），并确认文本目标和高亮框都指向移动后的设备。不要改变三档设备尺寸或玩法。该项会直接影响本任务要求复核的当前重点/设备框对应关系，修正前不建议交用户做最终视觉门。

## 核对结论

- Ponder `1.0.82+mc1.21.1` 的 `PonderScene.SceneTransform.apply` 对基准板采用 `-basePlateSize / 2 - basePlateOffsetX/Z` 平移；`configureBasePlate(1, 1, 14)` 因而以 `(8,8)` 为 X/Z 构图中心。这里的中心计算正确。
- 同一变换将 `yOffset` 加入世界 Y 平移项 `-1 + yOffset`，之后经过 GUI 翻转和场景缩放；`setSceneOffsetY(0.35F)` 是正向抬升0.35个场景方块，符号和单位符合预期。
- 三档设备平移后共享 `x=8` 中心，设备与底座的静态包围范围均在所选14格基础板范围内。大型尺寸没有被缩放或改变。此几何核对不能替代客户端对标题、正文、按钮安全区和完整轮廓的实际播放确认；按任务合同仍由用户定向复看。
- 区段生命周期没有发现15 tick间隔重叠：初始小型区段隐藏后等待15 tick，再隐藏基础板并等待15 tick；每档独立区段显示后等待15 tick、平移15 tick后再等待15 tick，正文持续95 tick后才隐藏，隐藏后再等待15 tick进入下一档。可复用这些静态时序结论。
- 通汽页中文为“超临界蒸汽从进汽口进入，蒸汽从排汽口排出。”；英文资源与 Java 场景 fallback 均使用 `exhaust port`，符合锁定文案。其余受影响键没有发现差异。
- 已阅读 `build/reports/extension/DEVICE-PONDER-04-FRAMING/assemble.log`，其中记录本批 `assemble` 的 `BUILD SUCCESSFUL`。依据治理精简验证规则，本审查不重复构建或测试。

## 范围与证据

审查仅覆盖本任务 Java 差异、两个语言键、指定任务计划和实现报告。仓库中的其他工作区改动不属于本审查写集。审查使用仓库要求的 `minecraft-modding`、`minecraft-testing` 技能，并直接检查 Gradle 锁定的 Ponder `1.0.82+mc1.21.1` jar 字节码；未运行客户端，因此不声明视觉验收通过。

## 整改复审闭环

**复审结论：** 原高亮错位已静态闭环；未发现新的区段或文本生命周期问题。原始发现保留如上作为整改记录。客户端视觉验收仍待用户播放确认。

- `showTier` 第224–229行按设备真实长方体范围及同一组 `dx=viewX-x`、`dy=liftY`、`dz=offsetZ` 计算 AABB：X/Y 各覆盖 `diameter` 格，Z 覆盖 `length` 格。小型 `dz=1`、中型 `dz=0.5` 均以 `double` 原精度应用；高亮框不再依赖整格 `Selection`，正文锚点取同一个 bounds 的中心。三档的框和正文目标因此使用相同平移结果。
- Ponder `1.0.82` 中 `showOutlineWithText` 与 `showText` 最终均创建一个 `TextWindowElement`；该元素的 `.text(text)` 经 `PonderScene.registerText` 按当前 `textIndex` 注册并递增。新增的 `chaseBoundingBoxOutline` 只安排 AABB 轮廓指令，不注册正文键。因此搭建页三档依次仍绑定 `text_10`（小型）、`text_11`（中型）、`text_12`（大型），顺序和 JSON 键无需变化。
- 新 AABB 轮廓和正文都设为75 tick，`noteWithBounds` 仍空等 `duration + 20`（95 tick）；随后区段隐藏并空等15 tick，与旧流程保持相同时序，没有新增重叠或空区段窗口。
- 已读取 `build/reports/extension/DEVICE-PONDER-04-FRAMING/assemble-review-fix.log`，记录整改后的增量 `assemble` 为 `BUILD SUCCESSFUL`。本次仅复审既有差异与证据，未重跑构建或测试，也未运行客户端。
