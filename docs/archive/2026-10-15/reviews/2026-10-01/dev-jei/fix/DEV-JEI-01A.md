# DEV-JEI-01A 执行报告

日期：2026-10-01。执行者：dev_jei。仅改主工程 `build.gradle`，保留用户 `.vscode/launch.json`；无 Git 写操作，无源码、资源、存档或其他依赖版本变更。当前合同为 PM 修订后的 `docs/superpowers/plans/2026-10-01-dev-jei-01a.md`。

实际使用：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`（锁定 MC 1.21.1 / NeoForge 21.1.219 / Java 21 / MDG 2.0.143 并核对开发模组发现路径）、`minecraft-testing/SKILL.md`（真实启动与 GUI 体验区分）、`superpowers/systematic-debugging`（追踪 MDG → BootstrapLauncher → FML 的完整路径）、`verification-before-completion`。配置修正前已取得真实失败日志与前后发现对照，不添加模仿配置的仓库测试。

## 原交付为何失败

原 DEV-JEI-01 把 JEI 放进三个 AdditionalRuntimeClasspath，仅检查 legacy 清单。MDG 2.0.143 将该配置接入启动库 legacy 清单，而非客户端真实 JavaExec classpath。首次补入真实 classpath 后仍失败：BootstrapLauncher 已把 legacy 清单中的 JEI 当作启动模块加载；FML LaunchContext 将其路径列为 located，DiscoveryPipeline.addPath 随后去重跳过，JEI 没有注册为游戏模组。

原“legacy 中必须有 JEI”的验收条件错误，已经被本卡修订合同明确推翻。仅调用 DevEnvUtils 的最小 probe 不能覆盖 bootstrap 模块层和发现管线去重，不能冒充最终加载通过。完整诊断、锁定字节码与失败证据保存在 `DEV-JEI-01A-DIAGNOSIS.md` / 同名目录；其中保留诊断被真实启动进一步修正的过程。

## 最终变更

建立可解析、不可消费的独立 `developmentJei` 配置，固定 JEI 19.27.0.340，仅注入 runClient / runClientA / runClientB 的实际 JavaExec classpath。删除原三个 AdditionalRuntimeClasspath 的 JEI 声明，让所有 legacy 启动库清单都不含 JEI。未加入通用 runtimeOnly、implementation、API、jarJar、强制模组依赖或 JVM 扫描绕过开关。

主工程旧 `run/mods` JEI 19.27.0.336 由 PM 原样移入 `DEV-JEI-01A/legacy-mod-backup/`，避免重复模组；执行者未修改该备份。记录见 PM 的 `legacy-mod-backup.json`。

## 验证与实际结果

1. 修前候选实际三个 JavaExec classpath 的 JEI 数均为 0，原 legacy 数为 1；修后实际三个客户端各 1，所有 legacy 均 0；server/GameTest 实际 classpath 均 0。证据：诊断目录 `actual-task-classpaths.json` 与最终 `DEV-JEI-01A/actual-task-classpaths.json`、`final-verification.json`。
2. 主工程命令 `.\gradlew.bat -I build/reports/development/DEV-JEI-01A/read-classpath.init.gradle prepareClientRun prepareClientARun prepareClientBRun prepareServerRun prepareGameTestServerRun jar help dependencies --console=plain` 退出 0，17 tasks，8 executed、9 up-to-date。完整 `final-preparation-dependencies.log` 中 JEI 只属于 `developmentJei`；编译/JUnit/服务端/GameTest/data/发布/jarJar 不含 JEI。
3. 发布 JAR 无 JEI 类、内嵌 JAR、强制元数据依赖。本次 SHA256 `CF93E36E4B0F874F58F340F5E07E307B57BED0EDBD093587F137511E7F811E62`，条目和 TOML 已保存。
4. 第一次隔离真实启动（14:31，PID 32036）只补实际 classpath、保留错误 legacy，仍无 JEI；已保留 `client-smoke-first-failed-latest.log`、`client-smoke-first-failed-gradle.log`、进程命令行与关闭记录。该次正常退出 0 不计 JEI 加载通过。
5. 最终方案第二次真实 `.\gradlew.bat -I build/reports/development/DEV-JEI-01A/client-smoke.init.gradle runClient --console=plain`，在独立 `DEV-JEI-01A/client-smoke` 游戏目录启动，PID 25660；无用户世界、无旧 mods JAR。`client-smoke-second-passed-latest.log` 明确含 `Just Enough Items 19.27.0.340 (jei)`；14:35:50 JEI ConfigManager 调用成功，14:35:53 ResourceManager 包含 `mod/jei`，14:35:56 创建 `jei:textures/atlas/gui.png-atlas`；未发现 JEI 相关启动错误。原有 Ponder refmap、JetBrains 注解、冷却剂 blockstate 和 shader 提示仍存在，本次不改无关资源。
6. 仅对已记录 PID 25660 调用 CloseMainWindow，返回 true；14:36:09 日志 `Stopping!`，Gradle BUILD SUCCESSFUL，退出 0。没有通配终止 Java，没有关闭用户进程，没有创建或进入任何世界。进程命令行和关闭记录见 `client-second-process.json` / `client-second-close.json`。
7. 临时 init 脚本只用于本次验证。退出后无 init 运行 `.\gradlew.bat prepareClientRun --console=plain` 退出 0，正常开发目录的永久配置未改。`git diff --check -- build.gradle` 退出 0。用户 `.vscode/launch.json` SHA256 仍为 `65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`。

## 限制与下一步

以上真实运行已验证加载器注册 JEI 及主菜单资源加载；未进入世界测试背包侧栏、R/U 配方和 Create 配方操作，不将其记作 GUI 体验验收。PM 需审查本次独立构建变更并同步矿物候选，随后用户重新从候选 runClient 进世界确认。三矿、粗矿、冷却剂等既有人工验收项不因本任务改变。
