# EXT-A-MATERIAL-04-ACCEPTANCE 合入后验证报告

- 任务：`EXT-A-MATERIAL-04-ACCEPTANCE`（执行者交付；最终验收状态由项目经理记录）
- 主工程：`E:\MyMC\NewMod\Create_NuclearIndustry`，分支 `main`，验证基准 `65ed6611be6aad4417648d8db217795ddac80de7`
- 技术基线：Minecraft 1.21.1、Java 21（`C:\Program Files\Java\jdk-21`，运行时 21.0.7）、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6
- 角色：收尾验证执行者。仅在本报告目录写入验收报告、隔离 init、日志、清单与精简 ZIP；常规 Gradle `build/` 输出按任务约定生成。未改源码、测试、正式资源、构建脚本、`docs/`、默认 `run/` 或既有报告；未执行 Git 写操作。
- 技能：实际读取并应用 `C:\Users\IKSXH\.codex\skills\minecraft-modding\SKILL.md`（核对运行配置和版本基线）、`C:\Users\IKSXH\.codex\skills\minecraft-testing\SKILL.md`（JUnit XML 与 NeoForge GameTest 结果分开判断）、`C:\Users\IKSXH\.codex\skills\minecraft-ci-release\SKILL.md`（制品/发布示例不覆盖仓库锁定版本与执行者权限）。技能示例版本未用于升级。

## 命令与结果

| 命令 | 退出结果 | 实际结果 |
|---|---:|---|
| `gradlew.bat -I <报告目录> material04RunDirectories --max-workers=1` | 0 | NeoForge `server`、`gameTestServer` 模型和对应 JavaExec `gameDirectory` 均指向各自隔离目录。 |
| `gradlew.bat test build --rerun-tasks --max-workers=1` | 0 | 本轮 JUnit XML 共 52 个套件文件、265 tests、0 failures、0 errors、0 skipped；原始 XML 已附。 |
| `gradlew.bat -I <报告目录> runGameTestServer --rerun-tasks --max-workers=1` | 1 | GameTest 日志总结 `167 GAME TESTS COMPLETE`，并打印 `All 167 required tests passed :)`。随后停在 `Saving worlds`；无日志进展观察55秒后，按任务要求只终止本轮游戏 PID 35516。子进程因此以 -1 结束，Gradle 报非零退出。该非零退出对应停滞后的受控停止，不是断言失败；没有盲目重跑。 |
| `docs/reviews/2026-10-01/material-04/verify-artifact.ps1 -ProjectRoot <主工程>` | PASS | 279 个资源源文件/JAR 条目逐项相同、65 PNG、10 个中英文名称均核对通过；JAR SHA256 如下。该 PowerShell 脚本返回 PASS 对象而未设置 `$LASTEXITCODE`，报告将成功完成且无异常记为状态 0。 |

GameTest 使用的 init 是已测脚本的逐行复制版，只把 `reportRoot` 改为 `build/reports/extension/EXT-A-MATERIAL-04-ACCEPTANCE`。详细模型/任务路径在 `directory-check.log` 和 `gametest.log`。隔离目录为：

- `...\isolated-server`
- `...\isolated-gametest`

本轮 GameTest PID 35516 创建于 `2026-10-01 23:47:28`；记录到的命令为 JDK 21 的 `java.exe ... net.neoforged.devlaunch.Main @...\build\moddev\gameTestServerRunProgramArgs.txt`。停止前重新核验了 PID、创建时间和 GameTest 启动参数；停止后未发现匹配该启动参数的残留进程。旧 Gradle daemon PID 21468（创建于 `2026-10-01 14:09:19`）一直保留，未停止。启动前没有匹配 Minecraft 客户端入口的 `java.exe/javaw.exe`。

隔离服务器启动日志有一次读取尚不存在的隔离目录 `server.properties` 的 `NoSuchFileException`；之后服务器正常启动并完成167项测试。保留完整日志供复核。

## 保护项与制品

- 主工程默认 `run/` 前后递归比较：1,408 个文件；路径、长度、UTC修改时间和 SHA256 全部一致。`run/` 目录本身修改时间也一致。
- `.vscode/launch.json` SHA256 前后均为 `65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`。
- 主工程 JAR：`build/libs/create_nuclear_industry-0.1.0.jar`；SHA256：`C50F67E2D6DB81B9F2AB60D2021847EBA2B3A4F4D7BA62EBDC1F73774EE54E24`。
- 根目录 `logs/debug.log` 和 `logs/latest.log` 已在运行前后复制留证。两份日志前 SHA256 均为 `B094FA55E95CBD30DD0B2B58647AB547EEFB257D63DD22410F754B16AC19E56D`，运行后均为 `84A96175971A707F66C76751EA31E776E628C1A06A7C8027735FB5E976DCB158`。已交项目经理核对和恢复，本执行者未改写根日志。
- 第一次默认 run 预检曾因 PowerShell 将 JSON UTC 日期自动反序列化后以本地格式显示而误报1,408项变更。抽查确认长度/SHA256相同；之后按 UTC 时间值归一后，GameTest 前及最终前后清单均为 `IDENTICAL`、0 changed、0 deleted。此次只是预检格式问题，不代表默认 run 有文件变化。
- 用户已在任务卡记录本批完整客户端手测清单通过；本次执行只重做约定的自动回归，不扩展人工验收范围。

精简证据包 `evidence-small.zip` 包含本报告、命令与退出证据、原始 JUnit XML、GameTest 日志、隔离路径和 PID 证据、默认 run 清单、根日志前后备份及制品校验结果；不含 world、缓存或 JAR。
