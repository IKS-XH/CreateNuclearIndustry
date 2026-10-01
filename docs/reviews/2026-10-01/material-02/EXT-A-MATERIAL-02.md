# EXT-A-MATERIAL-02 A 段执行报告

## 基线与范围

- 工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支 `codex/ore-acquisition`。
- 派发基线：`7d01b86d9ac7e07fe73700052af62a904cdb7a7c`；开工时 `git status --short --branch` 为干净。
- 角色：执行者，只交付 A 段未提交改动；不执行 Git 写操作、核心文档维护或任务验收。
- 锁定版本：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。

## 实际使用的技能

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：核对 NeoForge 注册形式与 1.21.1 单数资源目录；以本仓库锁定版本和现有 `DeferredRegister` 样式为准。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：设计运行于 Minecraft 服务端的 GameTest，区分配方管理器断言、真实炉子与 Create 机器 tick；复用仓库现有空模板与 GameTest 发现方式。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/test-driven-development/SKILL.md` 及 `writing-good-tests.md`：先写能因缺少正式物品/配方而失败的实际行为用例，再观察 RED、实施最小代码并验证 GREEN；不把 JSON 文本镜像当测试。
- 另用 `systematic-debugging/SKILL.md` 排查 GameTest 运行器异常，用 `verification-before-completion/SKILL.md` 核对最终日志、JUnit 计数与 JAR。

## 实现范围

- 已新增 `ExtensionBasicMaterialGameTests.java` 的九个测试，覆盖注册与真实加载配方、四条熔炼/高炉的实际方块实体 tick、输出堵塞后的恢复、铅锡真实 Create 压片机与置物台在无动力和恢复动力后的处理，以及标签替代模式。堵塞实际机器用例仅覆盖铅熔炉一例；等价外部标签仅覆盖铅侧，不扩大表述为所有机器及材料。
- 已加入四个物品、创造栏、四模型与双语名称、六个原生配方和汇总/细分标签；只改 A 段允许的生产文件。隔离标签包位于 `build/reports/extension/EXT-A-MATERIAL-02/foreign_material_pack/`，只追加 `minecraft:flint` 至 `c:raw_materials/lead` 与 `minecraft:clay_ball` 至 `c:ingots/lead`。该包仅安装到报告目录中的隔离普通服世界，未安装到用户存档或正式运行配置。
- 已从锁定 Minecraft 与 Create JAR 读取 cooking/pressing 原生 JSON，并从 Create 6.0.10-280 源码确认置物台的 item capability、压片行为和相邻电机动力路径。
- RED：项目经理放行后执行 `gradlew --init-script build/reports/extension/EXT-A-MATERIAL-02/material-test.init.gradle runGameTestServer --max-workers=1`。日志 `red-gametest.log` 汇总 125 个 GameTest、9 个新用例失败，其余无失败；缺物品注册、缺配方或真实炉子不加工是预期原因。汇总后卡于 `Saving worlds`，仅结束本次 GameTest PID 7616；Gradle 退出码 `1`。这不是正常自动退出。
- 首轮 GREEN：同命令，`green-gametest.log` 在第三批、无新断言失败行时由 `GameTestInfo.tickInternal` 的 fastutil 迭代异常使服务端崩溃；退出码 `1`，没有完整通过汇总，不能称 GameTest 全过。
- 首轮 `gradlew test build --rerun-tasks --max-workers=1`：`test-build.log`，退出码 `0`；此轮早于最后的测试源码改动，最终构建证据见下节。

## 最终自动验证

- 项目经理先执行 `materialRunDirectories prepareServerRun prepareGameTestServerRun`，`pm-directory-check.log` 退出 `0`；运行模型与两个任务的四个目录均指向报告内 `isolated-default` 或 `isolated-reload`。随后使用已修正 init 串行执行一次完整 `runGameTestServer`；`isolated-gametest.log` 于 2026-10-01 15:37:09 记录 `125 GAME TESTS COMPLETE`、`All 125 required tests passed :)`，包括本批九项新测试。之后保存阶段挂起，仅结束该轮 Java PID 28488，`isolated-gametest-exit-code.txt` 为 `1`。这证明断言通过，不证明 GameTest 进程正常退出。
- 同一个隔离 `runServer` 进程内，`isolated-reload/logs/latest.log` 的 `MATERIAL_TAG_RELOAD_RESULT` 于 15:38:57 为 `external=false matchedExpected=true`；启用隔离数据包并显式 `/reload` 后，15:40:03 为 `external=true matchedExpected=true`；停用数据包并再次显式 `/reload` 后，15:40:48 恢复 `external=false matchedExpected=true`。三次均核验标签成员及对应真实配方匹配。进程 PID 21588，15:41:03 输入 `stop`，15:41:04 所有维度保存完成，Gradle `runServer` 退出 `0`。命令为 `gradlew --init-script build/reports/extension/EXT-A-MATERIAL-02/material-test.init.gradle runServer --max-workers=1`，有效游戏目录为报告内 `isolated-reload`。
- 最终源码改动后的 `gradlew test build --rerun-tasks --max-workers=1`：`final-test-build.log` 与 `final-test-build-exit-code.txt`，退出 `0`，日志 `BUILD SUCCESSFUL`。52 个 JUnit XML 合计 **265** 项、失败 `0`、错误 `0`、跳过 `0`。最终 `build/libs/create_nuclear_industry-0.1.0.jar` 的 SHA-256 为 `BE7D16211E1E0600AAF0CE9E697159FFD1FB00E27807BBECDD0800197499F80A`；JAR 含 53 张本模组纹理 PNG、四种本批材料纹理和六个本批配方。最终 build 后未额外重跑 GameTest。

## 运行目录事故与隔离修正

- 上述 RED/GREEN 和 2026-10-01 15:28:40 启动的普通 `runServer` 都使用了候选树既有 `run/`。原报告 init 只覆盖 `JavaExec.workingDir`，但锁定 ModDevGradle 2.0.143 的 `RunGameTask.exec()` 在执行时从 `gameDirectory` 重设工作目录。因此此前“RED/GREEN 隔离运行”不成立，保留已写入的 `run/` 现场，不自行删除或恢复。
- 普通服精确启动命令：`gradlew --init-script build/reports/extension/EXT-A-MATERIAL-02/material-test.init.gradle runServer --max-workers=1`；15:28:56 Ready、15:29:11 运行一次外部标签 GameTest、15:29:50 输入 `stop`、15:29:51 保存完成，Gradle 退出码 `0`。本次未记录 OS PID，仅有工具会话 ID 98345。
- `run/world` 目录创建于 9/29，非新建；其中已有历史 GameTest 区块并新增本轮测试区块。普通服日志 15:28:51 的 `No existing world data` 与 `run/world/level.dat_old`（15:28:53）、`level.dat`（15:29:51）创建时间，说明普通服此次生成世界数据。`run/server.properties` 创建于 9/29、15:28:51 被自动重写；无修改前备份，不能称已恢复。项目经理将保留并处理现场。
- 已修正报告目录中的 init，直接配置 `neoForge.runs.gameTestServer.gameDirectory` 与 `server.gameDirectory` 并打印有效路径；项目经理检查模型与任务实际目录后才放行上述隔离复测。
- 项目经理已将受影响现场备份到 `build/reports/extension/EXT-A-MATERIAL-02/isolation-incident/`（19 MB、64 文件，含清单）；原 `run/world` 与配置仍在原位，执行者未删除、搬动或恢复。项目经理另将自动写入的根目录两个跟踪日志备份到 `automatic-root-logs/` 并恢复这两个跟踪日志；这不代表 `run/` 已恢复。

## 用户人工门与结论边界

用户仍需在客户端检查铅锡原料经真实熔炉和高炉取得对应锭、配方时长及经验表现；两种锭经真实 Create 压片机/置物台成为对应板，并观察无动力暂停与恢复；还需核对 JEI 配方与用途、四物品外观、保存退出并重进后的表现。未启动客户端，未将自动测试或静态资源检查当作人工结果；执行者不宣布验收、提交或合入。
