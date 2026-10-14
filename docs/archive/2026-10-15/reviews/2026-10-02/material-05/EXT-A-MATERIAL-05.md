# EXT-A-MATERIAL-05 执行者交付报告

**角色与范围：** 本报告仅记录任务1执行结果，验收、任务状态与 Git 操作由项目经理处理。候选目录 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；开工实际 `HEAD=53f4ebdd7b2f3bd580d96d85fb39b18e02fbb98d`，当时干净。没有执行 Git 写操作，也没有改正式构建、默认 `run`、旧测试、旧报告或任何游戏 PNG。并行的 `docs/` 与美术写集由项目经理和美术执行者分别维护。

## 合同与实现

- 按已确认方案加入 `quartz_dust`、`refractory_brick`、`heavy_bearing` 三个可堆叠成品以及 `incomplete_heavy_bearing` 原生单件序列物品；前三个进入模组创造页。
- 新增磨石配方：`c:gems/quartz` 一份、100加工参数，产一份石英粉；粉碎轮沿锁定 Create 的原生磨石回退。原生 `create:crushing/nether_quartz_ore` 保留。
- 新增加热搅拌：`minecraft:bricks`、`minecraft:clay_ball`、`c:dusts/quartz` 各一份，`heated`、100加工参数，产四件耐火砖。石英粉加入 `c:dusts/quartz` 与父 `c:dusts`，耐火砖加入本模组 `refractory_bricks` 标签。
- 新增一轮轴承序列：坚固板底板、机械手钢锭、机械手精密构件、压片，100%一件成品。半成品沿用 Create 的进度组件。四项双语名称与模型已加；四张 PNG 由 `EXT-ART-07` 美术任务交付。
- 未修改生产机器、NBT、网络、原版/Create 旧配方或精密构件的概率路线。

## 先失败后通过

- RED 命令：`./gradlew.bat -I build/reports/extension/EXT-A-MATERIAL-05/material-05.init.gradle material05RunDirectories runGameTestServer --rerun-tasks --max-workers=1 --console=plain`。`red-gametest.log` 中隔离模型和 `JavaExec.gameDirectory` 同时指向本批报告目录；168项汇总，新增 `loadedmaterial05contract` 因“缺少物品注册: create_nuclear_industry:quartz_dust”失败，是业务缺失而非编译/联网错误。
- 代码与数据加入后，`compile-1.log` 的 `compileJava processResources --rerun-tasks` 退出0。探针1因测试夹具误用 `LIT_BLAZE_BURNER`（无热级/燃烧室实体）与磨石电机方向错误失败；探针2只余断热时未移除带燃料的旧燃烧室实体。修正夹具后，`green-probe-3.log` 汇总179/179 GameTests通过；之后又补入真实错误钢粉拒绝、耐火砖公共标签和原生石英矿路线断言，整改前 `final-gametest.log` 再次179/179通过。
- 独立功能审查发现整改前 `mixerHeatAndPowerInterruptionPreserveMaterials` 在真实加工启动前就设动力为0，未证明“处理中断”；上述179通过**不作为这一要求的证据**。项目经理限定整改只改新测试及报告，未改生产注册、配方或美术。整改后以 `fix1-gametest.log` 的真实进度断言与180项汇总为准。
- `red-stop.txt`、探针和最终两轮 `*-stop.txt` 保留 GameTest 汇总后停在 `Saving worlds`、超过60秒无进展时核对并停止的本轮游戏 PID；子进程 `-1`、Gradle退出1与业务断言通过分别报告。

## 隔离真实标签重载

- 仅在 `EXT-A-MATERIAL-05/isolated-server/world/datapacks/external-quartz-pack` 创建临时数据包，向 `c:dusts/quartz` 追加 `minecraft:flint`；无正式资源或默认客户端污染。隔离普通服 `runServer --max-workers=1 --console=plain` 通过 `material-05.init.gradle` 路由，正常 `stop` 且 Gradle 退出0。
- `datapack disable "file/external-quartz-pack"` → `reload` → `test run externalquartzdustfollowsactualreload`：00:28:00，false。
- `datapack enable "file/external-quartz-pack"` → `reload` → 同项测试：00:28:17，true；00:28:28 真实加热搅拌消耗等价粉末并产四件耐火砖。
- 再次 disable → reload → 同项测试：00:28:43，false。`datapack list` 显示外部包仅 available。原始日志 `reload-false-true-false-latest.log`、关键行 `reload-evidence.txt`，服务端命令操作见本节。

## 环境与保护

- 开工 `java-before.json` 记录 Java 进程的 PID、创建时间及命令，既有 Gradle daemon / VS Code 服务未停止。GameTest 停滞时仅停止本轮隔离游戏 PID；隔离普通服正常退出。
- `default-run-before.csv` 按相对路径记录默认 `run` 各文件长度、UTC时间和 SHA-256。根日志开工前与整改后副本均保存在本报告目录；整改前结束时的后快照另以 `pre-fix1-` 前缀保留。
- 使用 `minecraft-modding` 核对锁定 NeoForge/Create 的注册、配方类型和版本；`minecraft-testing` 及 `superpowers:test-driven-development` 用于先RED再真实 GameTest；`minecraft-resource-pack` 核对1.21.1模型/语言资源路径；`minecraft-ci-release` 用于最终 JAR/资源核查；`superpowers:executing-plans` 按任务卡步骤执行。技能示例没有覆盖本项目版本或 Git 禁令。实际项目版本 MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6。

## 整改前全量与整改后定点复验

- 美术整改与定点复审通过、项目经理放行后，新跑 `./gradlew.bat test build --rerun-tasks --max-workers=1 --console=plain`：整改前 `final-test-build.log` 成功、退出0。整改后按项目经理限定命令跑 `./gradlew.bat build --rerun-tasks --max-workers=1 --console=plain`：`fix1-build.log` 成功、退出0，其中 `:test` 实际执行。整改后52份原始 XML 保存在 `junit-xml/`，合计 **265 tests / 0 failures / 0 errors / 0 skipped**（`fix1-junit-summary.txt`）；没有将整改前XML冒充最新结果。编译只见现存 API 弃用3条警告。
- 整改前隔离全量 `final-gametest.log` 为179/179通过，但其旧中断场景存在上述覆盖缺口。整改后重跑同一隔离命令，`fix1-gametest.log` 汇总 **180/180 required GameTests passed**。本批新增13条场景均有 `MATERIAL_05_TEST_RESULT`；后补的真实错误钢粉/缺粉拒绝与修正恢复、耐火砖公共标签及 Create原生石英矿路线也再次通过。隔离目录模型与 `JavaExec.gameDirectory` 均经启动前断言。
- 锁定 Create 的搅拌机在服务端已 `running`、`runningTicks == 20` 且 `processingTicks` 从16实际降到15时，两个用例才分别拆除燃烧室和把电机速度降为0。`MATERIAL_05_INTERRUPT_RESULT` 两条日志均记录 `progressBefore=16 progressAt=15 inputCount=3 outputCount=0`；中断后允许原生机器保留三料或完成四件，均以唯一批次守恒断言核对，不要求精确进度冻结。恢复热源/动力后，两场景各仅得到四件，日志场景 `mixer-started-heat-interruption-recovery` 与 `mixer-started-power-interruption-recovery` 通过。机器状态只读，未手设 `running` 或进度。
- 整改后 GameTest 在180项汇总后又停于 `Saving worlds`，00:48:50至00:50:03无进展超过60秒。仅停止经创建时间及完整命令核对的本轮隔离 GameTest PID **37540**（`fix1-gametest-stop.txt`）。因此子进程退出 **-1**、Gradle退出 **1**（`fix1-gametest-exit.txt`）；业务断言通过不等于游戏干净退出。既有daemon、VS Code服务和用户客户端均未停止。
- `verify-artifact.ps1` 与项目经理对整改后JAR的独立复核 `pm-fix1-artifact-verification.json` 均PASS：**292源资源=292 JAR资源，逐文件 SHA-256 相同；69游戏PNG，65旧图逐字节匹配开工快照，4新图匹配交接哈希；4项中英文共8名称正确**。整改后最终JAR SHA-256：`8D5BB2559370A47CB5DD9AE087DB20826E5BCB3C9FA54727DB725B54D37DB6E4`。整改前 `artifact-verification.json`、`pm-artifact-verification.json` 对应旧JAR哈希，保留为历史记录。源资源快照 `resource-hashes.csv` 为292行且最终未漂移；正式资源不含临时外部标签包。
- 整改后再次运行 `verify-default-run.ps1`，`default-run-{before,after}.csv` 中默认 `run` **182项**路径、长度、UTC时间、SHA-256全同。`java-before.json` 与最终 `java-after.json` 均仅有原进程 PID **21468、24304、464**，本批自有游戏进程为0。根目录 `latest.log` 与 `debug.log` 前后副本及整改前快照均已保存于05报告目录，运行后哈希与开工副本不同；交由项目经理按其职责判断恢复，不由执行者擅改。

## 交付边界

本任务完成的是代码、数据、真实机器与隔离服务端验证；客户端视觉、JEI、半成品保存重进仍属计划中的用户人工门。`evidence-small.zip` 包含本报告、init与验证脚本、RED/探针/整改前后原始日志、三态重载原始日志与临时包复制、最新原始JUnit XML、PID记录及哈希快照；不含游戏世界、缓存或JAR。ZIP的实际文件数和SHA-256另见 `evidence-index.txt`。
