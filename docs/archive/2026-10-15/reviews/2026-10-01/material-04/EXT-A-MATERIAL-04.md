# EXT-A-MATERIAL-04 执行者交付报告

## 范围与基线

- 任务：仅实施 2026-10-01 卡的任务 1；候选工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支 `codex/ore-acquisition`，派发时 HEAD `9295e48`，功能代码开工基线 `8c4526c`。未执行 Git 写操作、核心文档写入或人工验收。
- 实际技术栈：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。锁定 Create 源码和同版本 slim JAR 的 `SequencedAssemblyItem`、配方 JSON、`DeployerBlockEntity`、`DepotBehaviour`、`SawBlockEntity` 已核对。半成品使用原生单件物品与 `SEQUENCED_ASSEMBLY` 组件；锯只依 Create 的 `allowStonecuttingOnSaw` 读取原版切石配方。
- 实际读取应用的技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`（注册和原生配方）、`minecraft-testing/SKILL.md`（GameTest分层）、`minecraft-resource-pack/SKILL.md`（模型/资源路径与打包）、`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/executing-plans/SKILL.md`（按卡执行）、`test-driven-development/SKILL.md`（业务RED先于实现）、`systematic-debugging/SKILL.md`（外部包测试夹具超时归因）。仓库治理和精确写集优先于通用技能的提交/清理建议。

## 实施文件与行为

- `BasicMaterialContent.java` 注册三个可堆叠成品及两个 Create `SequencedAssemblyItem` 单件半成品；`ModCreativeTabs.java` 仅展示三成品。
- `ExtensionSensorProcessingGameTests.java` 新增注册、标签、语言、切石菜单、真实锯、两条真实机械手/压片序列、原生组件序列化、错序/缺料/断动力恢复、锯堵塞输出恢复及外部标签重载测试。未改旧测试。
- 新增五个物品模型及中英文各五个名称；新增加 `stonecutting/tin_wire.json`、两条 `sequenced_assembly` 配方以及 `c:wires` / `c:wires/tin` 追加标签。锡锭切石产2锡条；工业传感器铁板→锡条→红石→电子管→压片，辐射传感器铅板→工业传感器→电子管→压片；均一轮唯一1件结果。未加竞争切割配方、检测功能、拆解或自定义进度协议。
- 五项PNG由独立 EXT-ART-06 执行者提供，本执行者未修改其文件。

## RED、机器与重载证据

- 启动前将默认 `run/` 的180个文件逐一记录路径、大小、UTC mtime 与SHA-256；备份根 `logs/latest.log`、`logs/debug.log`，记录 Java PID/命令/创建时间。`material-04.init.gradle` 同时隔离 NeoForge `server` / `gameTestServer` run 模型与 JavaExec `gameDirectory`，规范路径断言见 `directory-check.log`，退出0。
- 先加五身份注册 GameTest。首次隔离 `runGameTestServer --rerun-tasks --max-workers=1` 汇总 **156 tests，1 required failed**，首项 `tin_wire` 未注册；这是业务RED，不表示五个身份各自都已失败。见 `red-gametest.log`。首次也在 `Saving worlds` 停滞，精确PID停止证据见 `red-stop.txt`。
- 最终真实设备 GameTest：原版切石菜单实际选择并取走2锡条、扣1锡锭；默认配置机械锯实际加工1锡锭为2锡条，置物台被圆石占据时锯库存保留2锡条，腾空后转交2锡条。两条传感器序列均在真实置物台、向下机械手、Create电机和压片机运行，每一步断言机械手原料从手中扣除、半成品的配方ID/step/progress与数量为1，最终各产1件。工业传感器的代表性恢复测试验证错序红石、缺料、第二步错用电子管及断动力均不推进，修正/补料/复电后精确完成；这不代表辐射路线的所有错误组合均独立测试。
- 外部临时包只在 `isolated-server/world/datapacks`。普通服实际 `datapack disable`→`reload`→单项测试 false；`datapack enable`→`reload`→单项测试 true，`minecraft:flint` 经真实机械手被消耗并产工业半成品step1；再次 disable→reload→单项测试 false。三段标记及时间见 `reload-false-true-false-latest.log` 与 `reload-commands.txt`；普通服 `stop` 退出0。第一次 true 探针因新测试默认100 tick超时、断言排在120 tick，没有机器结论；保留 `reload-attempt-1-latest.log`，仅将该测试超时调整为180，随后上述三段全部通过。
- 在同一隔离普通服的 Create 配置将 `allowStonecuttingOnSaw` 暂时设为false，单项真实锯测试证明没有锡条产生且1锡锭留于锯/输出置物台，见 `saw-config-disabled-latest.log`；普通服 `stop` 退出0。随后配置已恢复为true。未修改默认客户端配置。

## 最终自动门与制品

- `gradlew.bat test build --rerun-tasks --max-workers=1`：退出 **0**；本轮 `build/test-results/test` 的52份XML合计 **265 JUnit，0失败、0错误、0跳过**，见 `final-test-build.log` 与精简ZIP内原始XML。
- `gradlew.bat -I build/reports/extension/EXT-A-MATERIAL-04/material-04.init.gradle runGameTestServer --rerun-tasks --max-workers=1`：最终游戏汇总 **167/167 required GameTests通过**，见 `final-gametest.log`。汇总后停在 `Saving worlds` 超过60秒，核对本轮 Java 创建时间、父进程与 `gameTestServerRunProgramArgs.txt` 命令后只终止该精确PID，见 `final-gametest-stop.txt`；因此 Gradle 退出 **1**，不能声称服务端自然退出0。这与断言通过分别记录。
- 只读 `verify-artifact.ps1` 复核了源资源279项、JAR相应279项逐字节SHA-256一致，含65个游戏PNG，五项双语名称共10条正确。`artifact-verification.json` 和 `resource-hashes.csv` 留证；JAR SHA-256 `5D47E144FA0909C73B513D8B21BB88589696E4536F9C3F6EE98A567A699791AD`。ZIP不包含JAR。
- 默认 `run/` 前后均180文件，路径/大小/UTC mtime/SHA-256比较变化0项，见两份CSV及 `default-run-comparison.txt`。结束后无本候选工作树游戏Java进程残留。根跟踪日志因游戏运行发生变化，前后原样备份为 `root-latest.log.before/after` 与 `root-debug.log.before/after`；其Git恢复交PM办理，本执行者不做Git写操作。

## 人工门与余项

客户端 JEI、五图外观、切石/锯交互体验、两条装配可视流程、半成品存档重进以及手动断电/堵塞恢复仍需用户在候选客户端验收。自动测试不替代这些结果；本报告不宣告任务或整批验收通过，也不推进后续材料/设备。

精简证据ZIP：`build/reports/extension/EXT-A-MATERIAL-04/evidence-small.zip`，包含运行摘要、必要原始测试XML、PID和重载命令证据及只读制品脚本；无世界、缓存或JAR。
