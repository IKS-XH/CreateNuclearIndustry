# 管理员04原状态续接候选

2026-10-14。状态：内部实现、原组合审查及同次差异/制品补核完成，候选源码和JAR已提交；等待用户客户端复看，尚未合main。需求与写集以[04窄修卡](../../../superpowers/plans/2026-10-14-reactor-administrator-resume-04.md)为准。

用户明确确认：正常保存重进保留管理员模式、目标、实际/目标棒位和调节进度；加载暂缓本机结算，完成当前安全核验后直接续接。人工停堆、故障、急停与真实运行中失联保持保护。

同一候选目录 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation`，原世界和配置原位；main管理员源码与正式26cb18d0…包保持。本次不推进PI，不追认03及01/01R人工通过。

## 候选身份

- 目录：`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation`，分支`codex/reactor-automation-basic`。
- 源码与正式候选JAR提交：`d2171968ec43d0e6afedeb7902d104aeaf7637b6`，精确12个功能来源加一个正式JAR；交付文档另行净提交。
- 包：`build/libs/create_nuclear_industry-0.1.0.jar`，3199564B，SHA256 `dc82b20a19f17e81db2fd4e9efa69ed2b861fd703deaca4553ac7dfcbd6a7b05`。已按用户要求纳入Git，当前文件与该提交blob `cd5121c3b864713b576e6104b0d771bb563369bd`相同。
- 唯一必要增量`assemble`：`24-final-assemble.log`，1秒exit0；仅jar执行，未重复完整build或测试。
- 冻结副本：`build/reports/reactor-administrator-04/25-final-candidate.jar`，与正式候选完全相同；原879c3d72…包及全部失败证据保留。
- 最终清单：`25-final-artifact-manifest.json`，SHA256 `17ee65b0a6c1f8074e92435f7ed8c74263d0fe15d432c59c6a854570c386458d`。23稳定源清单与最终12来源零漂移。

## 行为与证据

正常保存退出时记录运行意图和棒位，加载时冻结本机结算，重建当前身份和保护核验后直接续接。旧授权、租约和热费票据不会恢复；健康核热值班首笔实际热费足额支付，不再要求额外冷启动燃料。人工STOP、FAULT和SCRAM仍保持原停止分类。合法空燃料列不会误判；反馈窗口发生真实结构代际变化立即保护。

| 实际证据 | 采用范围 |
| --- | --- |
| 原14真实11/11及15最终region只读 | NBT/BE重建、两BE顺序、真实反相线/零备用零积热首付、停止分类与真实正常停服最终保存；按未改范围复用，没有重跑或合成新15项整套。 |
| 最终21定向 | Resume8＋Lifecycle2，共10/10，22秒exit0，零失败/错误/跳过。 |
| 最终22纠错真实代表 | 稀疏运行、稀疏STOP及坏证据拒绝、删除原空燃料列、反馈期增加冷端导致代际变化，四代表4/4，21秒自然exit0且三维度均保存。 |
| 原组合审查及同次差异复核 | 两项Important均关闭，无新增必改；同一审查者补核最终制品和测试排除，没有另起整轮审查。 |

PM独立读取最终源和真实ZIP：12来源、661正式class、1546非class资源及生成MANIFEST，共2208非目录文件，全部与编译/资源输出和清单一致。新增测试outer/嵌套class及两NBT无正式文件载荷泄漏；四个空ZIP目录只作占位，不计测试文件。相对879c包仅新增Resume三class、修改九class，原资源零变化、无文件删除。

原当前测试世界38文件和27配置逐项与开工基线相同，未启动或回写用户世界。main正式26cb18d0…包和其他dirty保持；功能没有合main，02 PI未派发。

## 客户端复看

按[04局部清单](CLIENT-CHECKLIST.md)在同一候选目录运行`.\gradlew.bat runClient`。先在本包中使健康机组运行一次，正常保存退出，再进入同一世界查看模式、目标和棒位续接；核热值班不额外投冷启动燃料。另复看多燃料列仅装一列、人工停机/故障/急停保持。

NBT/BE重建、实际正常停服最终保存、直接BE运行卸载回调是不同证据；真正ChunkManager卸载/重载、独立进程restart、用户原世界/GPU/键鼠复看仍未验，不用自动结果追认。原01/01R/03其余未验项目按[03集中清单](../reactor-administrator-03/CLIENT-CHECKLIST.md)承接。

完整分阶段依据见[执行报告](IMPLEMENTATION.md)和[原审查报告](REVIEW.md)。

原03功能源78acd5d／879c3d72…仅为本批开工基线；本次交付替代其正常保存重进规则，不关闭任何未经用户确认的人工门。
