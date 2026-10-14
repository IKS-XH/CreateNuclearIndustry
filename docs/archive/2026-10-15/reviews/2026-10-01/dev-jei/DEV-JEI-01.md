# DEV-JEI-01 执行报告

- 日期：2026-10-01；执行者 dev_jei；主工程基线 af8ccf8。
- 仅修改 `build.gradle`、`gradle.properties`；未执行 Git 写操作、未启动/关闭游戏、未改存档或源码资源。`.vscode/launch.json` SHA256 保持 `65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`。
- 已读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`（核对平台、版本、开发依赖边界）、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`（配置验证与真实客户端体验分别记录）。另按 verification-before-completion 核对本轮命令结果。

## 变更与版本依据

新增限定 `mezz.jei` 组的官方 Maven 仓库，将固定 `jei_version=19.27.0.340` 分别加入 `clientAdditionalRuntimeClasspath`、`clientAAdditionalRuntimeClasspath`、`clientBAdditionalRuntimeClasspath`。不加入 JEI API、通用 runtimeOnly、implementation、jarJar 或模组元数据。

Minecraft 1.21.1 / Java 21 / NeoForge 21.1.219 / ModDevGradle 2.0.143 / Create 6.0.10-280 保持不变。

官方来源：
- https://github.com/mezz/JustEnoughItems/wiki/Getting-Started-%5Bminecraft-1.21-and-1.21.1%5D
- https://github.com/neoforged/ModDevGradle#external-dependencies-runs
- https://maven.blamejared.com/mezz/jei/jei-1.21.1-neoforge/19.27.0.340/jei-1.21.1-neoforge-19.27.0.340.jar

最新 19.57.0.450 实际 JAR 要求 NeoForge `[21.1.238,)`，不满足本项目 21.1.219，因此没有采用；原始元数据保存在 `DEV-JEI-01/jei-neoforge.mods.toml`。也检查了 19.21.0.247，最终固定较新的 19.27.0.340。

19.27.0.340 的实际 mod ID 为 `jei`，版本为 `19.27.0.340`，NeoForge 下限 `[21.0.118-beta,)` 直接包含本项目版本。其 Minecraft 原始范围为 `[1.21, 1.21.1)`，不直接包含 1.21.1；但本项目锁定的 FML loader 4.0.42 内建 `VersionSupportMatrix` 在 MC 1.21.1 时接受 MC 1.21 模组，故由该兼容机制满足加载版本检查。没有修改 JEI 制品或增加忽略依赖开关。兼容映射的实际字节码证据保存为 `fml-version-support-matrix.txt`，来自 `C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/net.neoforged.fancymodloader/loader/4.0.42/76e15f98bf676fb44b36c5e776f52a55581ec43a/loader-4.0.42.jar`。

MDG API 实际以锁定 2.0.143 本地 JAR 的 `javap -c -p net.neoforged.moddevgradle.dsl.RunModel` 核对，存在 `getAdditionalRuntimeClasspathConfiguration()`；证据 `mdg-run-model.txt`。在线尝试无 v 前缀的 2.0.143 源码 URL 返回 404，未以该失败请求作 API 依据。

## 本轮验证

1. `.\gradlew.bat prepareClientRun prepareClientARun prepareClientBRun prepareServerRun prepareGameTestServerRun jar --console=plain`：退出码 0，BUILD SUCCESSFUL；15 tasks，9 executed、6 up-to-date。服务器任务仅生成启动参数，无进程启动。日志 `prepare-and-jar.log`。
2. `.\gradlew.bat dependencies --console=plain`：退出码 0，无 FAILED。完整 `dependencies.log` 中 JEI 只出现于三个客户端各自的 AdditionalRuntimeClasspath / LegacyClasspath 共六个配置；编译、JUnit runtime、服务端、GameTest、data、apiElements、runtimeElements、jarJar 均没有 JEI。
3. PowerShell 检查生成的五份 LegacyClasspath：client/clientA/clientB 各 1 份同版本 JEI；server/gameTestServer 各 0 份。每个客户端的 VM 参数引用对应 LegacyClasspath。启动参数与 classpath 已复制到证据目录。
4. 实际 Gradle 缓存 JEI 与官方直下载 JAR 的 SHA256 一致：`8AAF547432F1B4958239B036356B910692FE40F858C3073D996F56BBF7C99826`。
5. 检查 `build/libs/create_nuclear_industry-0.1.0.jar`：无 `mezz/jei/` 类、无 JEI 嵌套 JAR、无 JEI 元数据依赖；本次 SHA256 `CF93E36E4B0F874F58F340F5E07E307B57BED0EDBD093587F137511E7F811E62`。条目及 TOML 保存在 `project-jar-entries.txt`、`project-neoforge.mods.toml`。
6. 上述依赖配置集合、数量、哈希和发布内容断言脚本退出码 0，摘要 `verification.json`。`git diff --check` 退出码 0，仅现有行尾提示；没有添加配置镜像测试或运行全量 JUnit/GameTest。

## 人工与后续

尚未实测 JEI 游戏内启动、物品列表和配方界面；启动参数准备通过不表示客户端体验已通过。用户需重启当前客户端以加载新依赖。项目经理后续独立审查并将两个构建文件的独立提交同步至矿物候选，再准备该候选启动参数。本任务不改变三矿/美术/冷却剂的既有人工验收状态，也未合入待验收玩法。
