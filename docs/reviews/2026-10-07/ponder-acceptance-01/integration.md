# DEVICE-PONDER-01/02 主目录打包交付报告

- 基线：`6acd310efd392aed7a27d0fb948c89a5f5feeaac`；工作目录 `E:/MyMC/NewMod/Create_NuclearIndustry`。
- 范围：只执行指定的一次主目录增量打包和制品核对；未改源码、资源、测试、工具或构建文件，未执行 Git 写操作。
- 锁定版本：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。版本来自 `gradle.properties`；本轮 Java 使用 `C:/Program Files/Java/jdk-21`。
- 技能：实际读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 与 `minecraft-testing/SKILL.md`。技能中的通用全量测试/客户端命令按任务卡和治理5.1不适用于本轮；本次只做 assemble 和交付指定的静态封包比对，没有运行 test、GameTest、clean、生成器或客户端。

## 本轮打包与制品

唯一命令：`./gradlew.bat assemble --console=plain`（Windows 使用 `.\gradlew.bat`），退出码 0；`BUILD SUCCESSFUL in 11s`，4 actionable tasks（2 executed、1 from cache、1 up-to-date）。完整控制台日志及退出码见 [`assemble.log`](../../../../build/reports/extension/DEVICE-PONDER-01-02-ACCEPTANCE/assemble.log) 和 [`assemble.exit`](../../../../build/reports/extension/DEVICE-PONDER-01-02-ACCEPTANCE/assemble.exit)。

JAR `build/libs/create_nuclear_industry-0.1.0.jar`：2,169,114 bytes；SHA-256 `2075B232E1B7E45BBE151498376337A9F382D9435916C6C233C11982DFA8C443`。

五个教学 NBT（离心机及反应堆 A/B/C/D）与 `en_us.json`、`zh_cn.json` 均已按资源路径与 JAR 项逐字节比较，7 项全部一致。JAR 中存在 `P1PonderPlugin.class`、`P1PonderScenes.class`、`CentrifugePonderScenes.class`。文件长度、比较结果及 class 路径详见 [`artifact-check.json`](../../../../build/reports/extension/DEVICE-PONDER-01-02-ACCEPTANCE/artifact-check.json)。

## 复用证据与人工边界

反应堆 02-R1 原合同证据复用自 `docs/reviews/2026-10-06/ponder-02/`：既有定向 `P1Ponder01ContractTest` 为 6 tests、0 failures/errors/skips，最终候选 assemble 退出码 0；PM 审查报告还记录四个模板及双语资源与当时最终候选 JAR 一致。上述是历史候选证据，本轮没有重跑六项合同，也没有将其描述为本轮测试结果。离心机 R1 提示时序、R2 双语/后备文案、R3 模板布局及各自候选制品证据复用自 `docs/reviews/2026-10-06/ponder-01/` 下的既有审查和执行报告；本轮只对 main 当前封包重新做了上述资源字节核对。

用户于 2026-10-07 明确确认“离心机、反应堆思索已经手动测试通过了”。这是本轮采用的用户播放证据；执行者没有启动客户端，也不声称做了新的视觉或播放复验。该确认不包含 REWORK-01 锅炉手测。锅炉重构仍待人工验收，未纳入本轮功能或制品范围。
