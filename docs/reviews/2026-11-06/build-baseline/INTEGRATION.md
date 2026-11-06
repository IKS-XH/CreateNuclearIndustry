# 正常build出口修复与main验证

2026-11-06。用户要求先解决主分支构建测试失败，按[本批卡](../../../superpowers/plans/2026-11-06-build-baseline-repair.md)完成。main起点`1b8899d5`，计划`b8905473`，修复源`25abcf437e09861e57985c979b228ace285e5b00`；PM审核后将main快进至该准确源，再由同一执行者验证主目录正常build。没有新玩法、生产代码/配置/资源语义或依赖变更。

## 两个根因、两个测试文件

1. `HeatExchangerBoilerBridgeTest`已有缓存初始化没有包括`condensationSettings()`的8项配置。真实pauseHeat→topologyChanged→HeatExchangerLine.find走到冷凝配置读取时，未加载配置异常阻断了原来的两条断言。仅补齐缓存列表；逐项保存并在AfterAll还原原值（包括null），不在生产中增加默认配置兜底。
2. `ReactorTickReference`没有同步已验`850904df`的正尾热分配规则。scenario0/tick4的控制/裂变/维护/coolant完全一致，thermal首次不等：列(0,1)实际余热`1.4490234156865794E-13`，旧参考按1e-12逐列过滤，把应分配的安全余数记为0；生产为`1.4490234156865493E-13`。按历史批准规则将参考的该处筛选改成`remaining>0`，保留全堆阈值、独立裂变oracle及完整严格相等断言。

只有上述两测试文件10行新增/3行删除，未改断言容差、样本、失败处理或跳过条件。原700布局/1400裂变、600完整tick严格对照继续有效；没有修改已验生产算法。[原始执行交付](EXECUTION.md)、[一次组合窄审PASS](REVIEW.md)保留源提交门时点，其中后续build待办已由下方结果完成。

## 实际验证

| 检查 | 结果 |
| --- | --- |
| 未改两类复现 | 15项/3失败/0跳过，exit1，原XML和输入保留 |
| 修复后4类定向 | 33项全通过、0跳过，exit0；含原微尾热/无冷却/149列回归及1400/600严格比较 |
| 准确源候选正常build | 44.052秒/exit0；jar与test实际执行，其余7项UP-TO-DATE；153类871项=860通过+11既有Sodium夹具跳过，0失败/错误 |
| 主目录正常build | 50.592秒/exit0；processResources/jar/test实际执行，两项编译FROM-CACHE，其余4项UP-TO-DATE；同153类871项=860通过+11既有跳过，0失败/错误 |

两次均为`.\gradlew.bat build --console=plain`，未clean、rerun或排除test。原11项跳过源于未显式提供`cni.sodium.fixtureDir`的既有@BeforeAll假设，完整名称和理由在[候选数据](BUILD.json)与[main数据](MAIN-BUILD.json)中，不计为业务通过。生产与夹具反射缓存恢复风险由一次独立源审覆盖，审查者未重复测试。纯测试修正且生产class不变，无需新增客户端手测门；原独立游戏/多人观察仍由STATUS保留。

## 正式JAR

- 从准确源25abcf43在main正常生成`build/libs/create_nuclear_industry-0.1.0.jar`：**3781913 bytes**，SHA256 **`0B912E12FA128F485432A3E1FB71937D2A88EA3844A3F7F07FB9E9B79095D932`**。
- 候选重新打包仍与已验E0AB包完全一致：3782088 bytes、2621载荷原字节不变，完整原包保存在候选报告目录；没有以候选覆盖主目录结果。
- main包2621载荷无增删，全部825class与原包及main编译输出原字节一致；21个既有开发GameTest class继续被原打包规则排除。
- main相对E0AB仅311项文本物理换行不同：78 JSON、5 MCMETA、223 OBJ、2 MTL、3许可证文本。严格UTF-8解码并仅将CRLF统一LF后逐字符全等；每项新载荷绑定当前真实来源文件字节。其余2310载荷不变，没有套用历史303项豁免，未改gitattributes或规范化源码。[完整差异CSV](payload-deltas.csv)、[main完整核验](MAIN-BUILD.json)。
- 候选与main各2426项已记录源码/测试/Gradle输入构建前后不变。main最初清单未包括额外3许可打包源，已如实保留这一证据边界，不倒填预构建SHA；这3项依HEAD、前后Git无改动状态和当前包字节绑定记录。正文与许可含义不变。

主目录原JAR工作副本缺失，已由正常构建重新生成。PM独立读回构建日志、差异与包SHA，继续按用户既有要求跟踪本包Git；提交包含对应已提交源码的正式制品，不宣称发布或推送。

## 证据与保护

本目录保存必要短报告和机器清单：[候选build](BUILD.md)、[main build](MAIN-BUILD.md)。原始复现/诊断/33项XML、独立首分歧工具、846个class前后清单和候选完整build证据在`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/build-baseline-20261106/`；主目录原始日志、153 XML、输入/用户文件清单及核验工具在`E:/MyMC/NewMod/Create_NuclearIndustry/build/reports/build-baseline-20261106-main/`。报告内相对原始证据路径按这两个根解析，不当作docs下存在的脚本。

main无关3D dirty/untracked保留，package.json/serve.mjs前后SHA分别为`1C0D0AE2818A8BF62398AFCB3F33013788CFC083C42AFA5092BCA0910A4FB990`/`D7402108AE99217AD555C85969E0CF3DD04EA218A78DBC710D5B82D43155E63D`。同一候选目录切分支保留原位世界，未运行客户端或读写/搬运世界。只读原证据、许可与来源分支保留；没有自动接续整线观察、音效、压力管网、旧存档兼容、推送或发布。
