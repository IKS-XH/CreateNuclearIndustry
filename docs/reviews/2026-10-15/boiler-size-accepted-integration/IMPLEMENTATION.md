# 已验锅炉尺寸合入：阶段1交付

2026-10-15，唯一集成执行者。main基线`83cd1b6d8284db3e1f91043b62b637f7c399933c`，原尺寸来源`30020e469ec8719560a33833081979c8f7225daf`。仅适配活动卡七项来源、主目录锅炉配置单键及允许报告；无Git写入、核心文档/其他树/世界改动或子代理。当前等待PM恢复测试自动更新的两日志、一次组合窄审及准确来源提交；未导出、assemble或替换正式JAR。

## 最小适配

BoilerConfig与BoilerState只把默认最大11及合法上界32改16，配置中文说明同步；Settings后来增加的两路流量、默认2048/4096、long容量/阀乘积预检、双汽库存及主动出汽全部保持。没有取旧候选整文件覆盖main。

JUnit接原四项尺寸/容量合同并适配当前Settings签名；原无效33改17，溢出用例在合法最大16下分别核水/汽/阀乘积。新GameTest和94B空模板与原30020e4逐字相同，原Extension仅三处边界文本替换，保11格偏心参考。build.gradle原延迟jar配置只加新测试类/模板两排除，其余全文保持。

七项准确交付SHA256如下；Java主根为`src/main/java/com/iksxh/create_nuclear_industry/`，JUnit根为`src/test/java/com/iksxh/create_nuclear_industry/`。

| 来源 | SHA256 |
| --- | --- |
| 主config/BoilerConfig.java | `3dd0c9002476d09d075b28e6af1b6f5d3e6b982fce58f6cfa6e7fb827f8e9b72` |
| 主boiler/BoilerState.java | `f3feb457b34ca0152ab39a40682f447085bfe8fd53b87bc34c548d6322c86c16` |
| JUnit boiler/BoilerStateTest.java | `25bac7a727666bd8471389754be2690fd994815ac09cfe8a4ea9100398dd3346` |
| 主gametest/BoilerSizeLimitGameTests.java | `b940eb4fcf7eb4fc53bb5284c36abde316ebdb7e52fea5d138e689067ff3c904` |
| 主gametest/ExtensionBoilerGameTests.java | `da3bc00bcaa16faef23d94259c9d4529c18f9d08c1ba79a771593b2e213aa71e` |
| src/main/resources/data/create_nuclear_industry_boiler_size/structure/boiler_size_empty.nbt | `a46fbca10b8c94cd0228bd493bf5f1d19787ec034047715db6ac40b9ab211941` |
| build.gradle | `71c78a18eb11cb8f94981da1453b702eb3d5fbe05e5ebf6ff5a51a14fdb48c87` |

## 实际定向与复用

唯一一次`./gradlew.bat test --tests '*BoilerStateTest' --tests '*BoilerDualSteamInventoryTest' --tests '*IndustrialPortRateTest' --no-daemon --console=plain`，仅另带本次E盘tmp JVM参数。Gradle显示47秒、实际48.493秒，exit0；19＋13＋1＝33/33，失败/错误/跳过0。新测试类与现行主线均已编译，未启动GameTest或客户端。日志有18项既有API弃用警告及Gradle弃用提示。

原29/29、最终真实3/3自然保存exit0、原窄审及用户本次锅炉尺寸/外观通过分别复用[原实现](../../2026-10-12/boiler-size-limit-01/IMPLEMENTATION.md)、[原审查](../../2026-10-12/boiler-size-limit-01/REVIEW.md)和[本批验收](ACCEPTANCE.md)；不扩为15C动画验收，不重跑原生产线或全build。

## 配置及保持范围

先完整备份主目录锅炉配置，再只改dimensionRange [5,11]→[5,16]。文件2366B，仅offset109的49→54，其他字节/键不动；前SHA`a69ecdb5f503a0eceb57382387b9b5622526b4fd27bd15910da50ade5db7ec85`，后SHA`d2f9ac5134fccb91db51a8e341ef25630745a53e4807f92a3915ad648d026608`。配置不进Git，未启动客户端，不补称本轮客户端已加载。

本批报告根为`build/reports/boiler-size-accepted-integration/`。前件ZIP与清单完整冻结105项原dirty、2108源码/构建输入、59世界/运行配置及原33da包。七项外源码、配置单键外全部世界/运行配置均无漂移，新增来源恰为新测试类与模板。正式JAR及冻结备份仍3304595B、SHA256 `33da90021f938d2349e9d88c4522df72bfe587a4f1ca388d3158d34d9e8e9e8b`。

JUnit自动更新logs/debug.log及latest.log，原字节完整留在dirty-before.zip，测试后两份3493B日志已各冻结test-generated-logs/，SHA均`9585132fc8ecf985117a3177c52fc6d833047093eb928f66c428c35a68cb0432`。原105 dirty中的另外103项全等；执行者未自行恢复日志，由PM恢复原件。严格保护差异保留stage1-verification-strict.json，不静默忽略；无生产或测试失败。

## 证据入口与技能

报告根的STAGE1.md为详单；stage1-paths.txt、stage1-adaptation.json及stage1-diffs/提供七项范围/前后SHA/净差异；test-attempt.json、test-result.json、stage1-tests.log及junit-stage1/提供实际退出与原始XML；boiler-config-before.toml、before-manifest.json和三个前件ZIP提供配置/保护身份。七项`git diff --check` exit0。

本轮读取并应用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、minecraft-testing/SKILL.md、minecraft-ci-release/SKILL.md及superpowers6.4.2 verification-before-completion；核实际MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，不升级栈。Python完整路径`C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe`；TEMP/TMP仅本次进程树用E盘并已恢复，不停其他服务/进程。

PM处理日志保持、组合审查并提交准确来源后，执行者才从该干净Git树完整导出/校验并做唯一必要assemble；此报告不把阶段1交付称为正式制品完成。
