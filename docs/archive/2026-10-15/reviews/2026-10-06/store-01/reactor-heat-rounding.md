# REACTOR-HEAT-ROUNDING-01 实现报告

候选：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；开工 HEAD：`22c43e31c4af2ed2a0063d2380b6614b625b3a66`。执行者交付未提交改动，未修改核心文档或任务状态，未执行 Git 写操作。

裂变计算先保留原公式和总热上限，再全堆执行一次 `Math.ceil`。按原列热比例分配，固定坐标顺序的最后一个正产热列承接尾差，零热列保持零。正式 tick 冷却使用权威新生热总量加原缓存；遥测使用同一权威新生热总量。原始诊断热、燃耗、控制与反馈参数不变，缓存热不再次取整。

改动文件：

- `src/main/java/com/iksxh/create_nuclear_industry/reactor/`：`ReactorFissionCalculator.java`、`ReactorServerTick.java`、`ReactorInstrumentTelemetry.java`、`ReactorFissionResult.java`（仅语义说明）。
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/P1Verify01GameTests.java`：保留原始损伤倍率与燃耗断言，实际产热按先限幅再取整验证。
- `src/test/java/com/iksxh/create_nuclear_industry/reactor/`：`ReactorFissionCalculatorTest.java`、`ReactorServerTickTest.java`、`ReactorScramFissionRegressionTest.java`、`ReactorControlRodTickTest.java`、`ReactorThermalCalculatorTest.java`、`P1Thermal01ContractTest.java`。
- 本报告。

实际 Gradle 命令共 3 次，均增量运行，无 `clean`、`--rerun-tasks`、全量 GameTest 或客户端启动：

```powershell
.\gradlew.bat test --tests 'com.iksxh.create_nuclear_industry.reactor.ReactorFissionCalculatorTest.fractionalTotalRoundsOnceAndPreservesColumnProportions' --console=plain
.\gradlew.bat test --tests 'com.iksxh.create_nuclear_industry.reactor.ReactorFissionCalculatorTest' --tests 'com.iksxh.create_nuclear_industry.reactor.ReactorServerTickTest' --tests 'com.iksxh.create_nuclear_industry.reactor.ReactorInstrumentTelemetryTest' --tests 'com.iksxh.create_nuclear_industry.reactor.ReactorScramFissionRegressionTest' --tests 'com.iksxh.create_nuclear_industry.reactor.ReactorControlRodTickTest' --tests 'com.iksxh.create_nuclear_industry.reactor.ReactorThermalCalculatorTest' --tests 'com.iksxh.create_nuclear_industry.reactor.P1Thermal01ContractTest' assemble --console=plain
.\gradlew.bat test --tests 'com.iksxh.create_nuclear_industry.reactor.ReactorControlRodTickTest' --tests 'com.iksxh.create_nuclear_industry.reactor.ReactorThermalCalculatorTest' assemble --console=plain
```

首个回归在原实现失败：1 项、1 失败，预期 `7.0`、实际 `6.75`。首轮相关验证 49 项、47 通过、2 失败，均为旧精确期望：`targetDepthBecomesActualDepthOnTheNextTickAndChangesAdjacentFission` 原 `2.25`→实际 `3`，`higherNetHeatLoadProducesSteeperIntegrityLoss` 原 `4.5`→实际 `5`。保留这两项的 `rawHeatHu` 原值断言并更新实际值后，仅复验对应两类：10 项全部通过，`assemble` 成功（18s，2 executed / 7 up-to-date）。其他五类 39 项复用同一功能实现的首轮通过证据。

| 测试类 | 最终有效证据 | 数量 |
| :--- | :--- | ---: |
| ReactorFissionCalculatorTest | 首轮通过 | 14 |
| ReactorServerTickTest | 首轮通过 | 6 |
| ReactorInstrumentTelemetryTest | 首轮通过 | 5 |
| ReactorScramFissionRegressionTest | 首轮通过 | 3 |
| P1Thermal01ContractTest | 首轮通过 | 11 |
| ReactorControlRodTickTest | 修正期望后通过 | 3 |
| ReactorThermalCalculatorTest | 修正期望后通过 | 7 |

最终有效覆盖 49 项，0 失败/错误/跳过；计入红测和定向复验的实际测试执行总数为 60。原始执行摘要保留在 `C:/Users/IKSXH/.gradle/daemon/9.0.0/daemon-31016.out.log`：157–180 行为红测，353–379 行为首轮失败摘要，470–471 行为最终构建成功；上述原文已保留到 `build/reports/extension/REACTOR-HEAT-ROUNDING-01/gradle-evidence.log`。Gradle 后一次定向运行自然替换 `build/test-results/test/` 与 `build/reports/tests/test/`，现有 XML/HTML 为最终两类 10 项结果；首轮各类计数来自当次 XML 实际读取。

关键覆盖：两个分数列 `3.3+3.45=6.75`→全堆 `7`（逐列取整会错误得到 `8`），比例和总量一致，零热列不分配；原始 `6` 经 `3.3` 上限后为 `4`；整数/空堆/全插棒/燃耗边界保留。FCF 半插棒连续 10 个正式 tick 保持原始 `23.604377652134303`→`24 HU`、`48 mB`，列热、实际冷却、冷/热库存变化和遥测一致，完整度稳定，无新增缓存或量化余数。全插棒的 `0.75 HU` 缓存仅转 `1 mB`、保留 `0.25 HU`，不被取整。

存量余数用例改用等量缓存热、零新生热构造，继续验证 `0.25+0.25` 跨 tick→`0.5 HU/1 mB`、无冷却 `0.49 HU` 真实损伤、部分冷却的安全余数与真实短缺隔离、多列分配、传播隔离和当前快照往返；既有历史格式断言保留，未新增兼容研究。功能代码没有修改流体事务或余数机制。

制品：`build/libs/create_nuclear_industry-0.1.0.jar`，SHA256：`B224EF40A6EFE02D994EF0451492A8E4A5331EE4809F7311A48011144971A971`。本项源文件 `git diff --check` 通过。编译仍提示既有 `P1Coolant05CoverageGameTests.java` unchecked 和 Gradle 10 弃用警告。

实际读取并应用 `minecraft-modding`、`minecraft-testing`、`test-driven-development`（含 writing-good-tests）与 `verification-before-completion`；按治理 5.1/5.2 精简验证、任务写集和中文注释合同执行。核对 MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82，未升级依赖。未清理既有日志/缓存/世界；JUnit 自然刷新其既有日志输出。并行出现的 PM 文档改动未触碰。

限制：GameTest 断言仅随 `compileJava`/`assemble` 编译，本项未启动测试服；真实设备与客户端检查仍并入 STORE-01 联合人工门，本报告不代表人工验收或任务状态关闭。
