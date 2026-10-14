# DEVICE-PONDER-01 R1：正文提示时序整改报告

- 候选基线：`cd78932`；只调整 `CentrifugePonderScenes.java` 中正文间等待，并补充一条中文时间语义说明。
- 根因：锁定 Ponder 1.0.82 的 `TextInstruction` 继承 `FadeInOutInstruction`；`fadeTime=5`，构造器总寿命为传入 duration 加两次 fade，即 `duration + 10 tick`。`showOutlineWithText` 的 duration 因而不是正文完整退场时间。
- 验证源码：`ponder-neoforge-1.0.82+mc1.21.1-sources.jar` 中 `TextInstruction.java` 第 8、13 行，`FadeInOutInstruction.java` 第 7、9 行。

## 八段时间线审计

以首个正文开始为相对 tick 20；区间按 `[出现 tick, 退场 tick)` 计算，寿命为 `指定 duration + 10`。正文之间的等待累计该段之后、下段正文之前所有 `scene.idle`，包括镜头/物料阶段中已有的等待。

| 正文 | duration / 寿命 | 整改前出现—退场 | 整改前净间隔 | 整改后出现—退场 | 整改后净间隔 |
|---|---:|---:|---:|---:|---:|
| 1 用途 | 90 / 100 | 20—120 | — | 20—120 | — |
| 2 放置上下段 | 90 / 100 | 120—220 | 0 | 130—230 | 10 |
| 3 下段动力 | 90 / 100 | 228—328 | 8 | 240—340 | 10 |
| 4 顶部进浆 | 100 / 110 | 338—448 | 10 | 350—460 | 10 |
| 5 当前配比 | 105 / 115 | 448—563 | 0 | 470—585 | 10 |
| 6 输出与回水 | 150 / 160 | 538—698 | -25 | 595—755 | 10 |
| 7 停机排查 | 105 / 115 | 658—773 | -40 | 765—880 | 10 |
| 8 轴承维修 | 100 / 110 | 773—883 | 0 | 890—1000 | 10 |

整改前净间隔 `[-]` 表示前一段仍在显示：第 5→6 段重叠25 tick，第 6→7段重叠40 tick；其余有0、8或10 tick间隔。逐帧计算的最大同时可见正文数从2降为1；整改后七处正文间隙均为10 tick。计算从原始 `cd78932` 文件和当前文件提取八个 `showOutlineWithText` 时长及对应区间 `scene.idle`，按锁定寿命公式逐 tick 区间检查；未增加持久化测试框架。

## 修改与验证

仅修改正文间等待：首帧后100→110；第2→3帧区间的8→10；第4帧后110→120；第5帧后80→115；第6帧后120→170；第7帧后115→125。正文、duration、同段控件、物料动画、8帧内容、模板、语言和设备行为保持原样。首版第3→4帧原有10 tick净间隔保留。

只运行一次候选增量构建：`./gradlew.bat assemble --console=plain`，退出码 `0`；`BUILD SUCCESSFUL in 11s`，4 actionable tasks（2 executed、2 up-to-date）。实际日志：`build/reports/extension/DEVICE-PONDER-01/timing-r1/candidate-assemble.log`。

- JAR：`create_nuclear_industry-0.1.0.jar`
- 大小：2,153,062 bytes
- SHA-256：`c201d34ec50c15cae346955f3c84b2621ceb0955078278f0bdcd5fe2b704aeeb`

本次依照 `minecraft-modding`、`minecraft-testing`、`superpowers:systematic-debugging`、`superpowers:verification-before-completion` 技能和 Ponder 1.0.82 锁定源码处理。未重跑旧资源/设备JUnit、GameTest或其他行为测试；未启动客户端。尚待用户重新播放同一故事线确认配比与输出提示不再重叠，并检查视觉读时。
