# 材料04与素材06最终验收

**PM验收：2026-10-01完成，已合入main。** 用户对本批[完整客户端清单](./CLIENT-CHECKLIST.md)答复“手动测试都通过了”。该确认覆盖锡条两种加工入口、两种传感器逐步装配与准确耗料、错序/欠料/断动力/堵塞恢复、两类半成品保存重进、JEI和创造页、五项外观及双语名称。自动验证与用户人工确认分别保留，不将组件序列化测试当作玩家存档重进证据。

## 整合与使用

候选`2f8e08e`在干净的同级工作树与主线文档`d824f17`整合为`65ed6611be6aad4417648d8db217795ddac80de7`，主工程随后快进至该提交。整合后的Git文件树与已测候选完全一致，未改功能、测试、资源或构建配置；本次收尾仅更新验收文档并保存合入后证据。既有功能/素材分项审查及整批终审均无开放问题，见[候选阶段归档](./README.md)。

主目录可以直接启动本批内容：

```powershell
Set-Location 'E:\MyMC\NewMod\Create_NuclearIndustry'
.\gradlew.bat runClient
```

已启动的客户端需重启。两个工作区的存档各留原处，未迁移或覆盖。锡条、工业传感器、辐射传感器及原生序列半成品现已可用；传感器此阶段仍只作为制造材料。

## 主工程本轮验证

高速执行者`/root/material04_acceptance_verify`使用`gpt-6-luna / high`执行既有验证命令，独占本轮Gradle/游戏。PM核对原始日志、52份JUnit XML、隔离路径、进程和工作区保护证据，并独立运行现成制品核对工具。

| 检查 | 本轮结果 |
| :--- | :--- |
| JUnit与构建 | `test build --rerun-tasks --max-workers=1`退出0；52套件265项，失败/错误/跳过均0 |
| 隔离GameTest | `runGameTestServer --rerun-tasks --max-workers=1`汇总167项required断言全部通过 |
| GameTest退出限制 | 汇总后停在`Saving worlds`；观察日志55秒无增加后重新核对PID35516的创建时间和完整命令，仅终止本轮游戏；子进程-1、Gradle退出1，不是正常退出成功 |
| 制品 | 279项assets/data与JAR逐字节一致，65PNG、10项中英文名称PASS；PM独立复核同样通过 |
| 隔离与存档保护 | NeoForge run模型及JavaExec的gameDirectory均指向本轮报告下的隔离目录；主工程默认run前后1408个文件的路径、长度、UTC时间及SHA256完全一致 |
| 进程与日志 | 本轮游戏无残留；保留原daemon21468及后来启动的VS Code服务24304/daemon464。PM核对根日志前副本与HEAD一致，保存前后证据后恢复两份跟踪日志 |
| 用户调试配置 | `.vscode/launch.json`保持原SHA256，未纳入提交 |

已知GameTest保存停滞仍未修复，继续作为测试运行环境限制记录。本轮不重复外部标签三态重载和机械锯关闭配置测试，沿用源码未变的候选实测证据；不扩大为任意第三方模组组合的联调结果。

首次默认run预检因PowerShell将JSON日期转换成本地显示格式而误报差异；按UTC时间值归一后，GameTest前和最终比较均为IDENTICAL。没有修改默认run来消除差异。

- [合入后执行报告](./EXT-A-MATERIAL-04-ACCEPTANCE.md)。报告中的相对证据文件位于下列ZIP内；原始执行报告保留执行者语境，最终验收结论以本页为准。
- [主工程精简证据包](./main-acceptance-evidence.zip)：86项、264585字节，含52份JUnit XML、构建/GameTest完整输出、命令与退出码、隔离init、PID、默认run前后清单、根日志前后副本和制品结果；无世界、缓存或JAR。SHA256：`297BB619F69532CA4378B1817D8BD3C817016479EBFC9B4B773E2017DEACB870`。
- [主工程制品核对](./main-artifact-verification.json)、[现成只读工具](./verify-artifact.ps1)；复核时将`-ProjectRoot`指向主工程根目录。

本轮主工程JAR SHA256：`C50F67E2D6DB81B9F2AB60D2021847EBA2B3A4F4D7BA62EBDC1F73774EE54E24`。用户调试配置SHA256：`65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`。候选阶段JAR和默认run180文件快照继续留在原归档，不与本轮主工程结果混用。

PM按`minecraft-modding`、`minecraft-testing`、`minecraft-ci-release`及完成前验证/分支收尾流程核对任务范围和证据；执行者实际技能使用记录见报告。未升级锁定技术栈，未发布或推送；同级工作树保留。

本批人工和整合门解除。下一步是耐火材料、重型轴承及首台设备合同；未确认的数量、时间、热级和运行行为须另行决策，尚未派发实现。铅锡水洗副产物继续按用户要求暂缓。
