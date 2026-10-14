# EXT-A-MATERIAL-02A A 段执行报告

## 基线与合同

- 候选工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支 `codex/ore-acquisition`，开工 HEAD `0d38c99fe8d3e041c6148057fc4f62827b9e45b4`。开工时仅两张板材及其工具预览等 9 项独立美术未提交改动；本执行者不触碰。
- 按主工程 `docs/superpowers/plans/2026-10-01-material-02a-feedback.md` 执行。Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。保留既有粗矿直熔及压片；新矿石/深层矿 200/100 ticks、0.7 XP，粉碎料 200/100 ticks、0.1 XP，洗矿固定九粒，九粒与一锭双向守恒。
- 已核对只读原生审计 `EXT-A-MATERIAL-02A-NATIVE-AUDIT.md` 及其锁定 Create JSON 摘录；原生铅锡粉碎继续复用，第三方兼容输出不是本模组成品。另只读核查锁定 Create 源码：水洗风扇识别水源、置物台的 `TransportedItemStackHandlerBehaviour`、默认风扇处理时间 150 ticks，以及锌九粒压合与一锭拆九粒的 JSON 形态。

## 技能与实施

- 实际读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`（注册、原生数据与锁定版本）、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`（真实机器 GameTest）、`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/test-driven-development/SKILL.md` 及 `writing-good-tests.md`（先写能因缺注册/配方失败的行为用例）。技能中较新版本或其他加载器示例未移植到本工程。
- 先新增 `ExtensionNativeMetalProcessingGameTests.java` 的 16 项：12 个普通/深层矿石与粉碎料在铅锡熔炉/高炉中的真实 tick 测试，检查时间、经验声明、固定一锭与错误形态拒绝；两项真实 Create 风扇、水源和置物台水洗，覆盖无动力保料及恢复动力后的九粒产出；运行时配方管理器中的水洗结果和通用粒标签；九粒↔一锭的匹配、错金属/欠料拒绝与守恒。测试直接使用已加载配方和机器，不以检查 JSON 文本代替加工。
- RED 确认后才增加 `lead_nugget`/`tin_nugget` 普通物品和创造栏、两粒模型/双语名称、`c:nuggets` 汇总及细分标签；新增 8 条矿石/粉碎料 cooking、2 条 Create 水洗及 4 条九粒↔锭原生合成配方。没有修改旧粗矿直熔、压板、粉碎、铀或 P1 逻辑；两粒纹理由独立素材执行者提供，本执行者未修改其素材/工具。
- 报告目录中的 `material-02a-test.init.gradle` 直接配置 `gameTestServer`、`server` 的 run model `gameDirectory`，并在两个 JavaExec 启动前断言模型、任务属性与预期路径相等，且不落入默认 `run/`。RED 与 GREEN 使用各自独立的报告内世界。

## TDD 与运行证据

- 启动前只读记录默认 `run/` 176 个文件、`run/saves` 68 个文件的 SHA-256 清单，见 `before-run-sha256.json` 和 `before-run-saves-sha256.json`。执行 `gradlew --init-script build/reports/extension/EXT-A-MATERIAL-02A/material-02a-test.init.gradle material02aRunDirectories prepareServerRun prepareGameTestServerRun --max-workers=1`，`directory-prepare.log` 退出 `0`；四项模型/任务实际目录均位于本报告目录下。GREEN 的 `'-Dcni.material02a.green=true'` 参数另经 `green-directory-prepare.log` 核对到 `isolated-gametest-green`。曾有一次未加引号的 PowerShell `-D` 解析成任务名，仅目录诊断失败、未启动游戏；其输出被下一次成功诊断日志覆盖，未当作通过证据。
- RED 命令：`gradlew --init-script build/reports/extension/EXT-A-MATERIAL-02A/material-02a-test.init.gradle runGameTestServer --max-workers=1`，实际游戏目录 `isolated-gametest`，Java PID `30448` 及完整命令见 `red-java-process.json`。`red-gametest.log` 发现 141 项、16 个新增测试都出现预期失败：12 项找不到新 cooking 配方，4 项缺对应粒注册。之后发生既有 `GameTestInfo.tickInternal` / fastutil 迭代器异常，未形成完整汇总；`red-gametest-exit-code.txt` 为 `1`。这轮 RED 只证明新测试对缺失行为敏感，不证明原有 125 项完成。
- GREEN 命令：`gradlew '-Dcni.material02a.green=true' --init-script build/reports/extension/EXT-A-MATERIAL-02A/material-02a-test.init.gradle runGameTestServer --max-workers=1`，实际游戏目录 `isolated-gametest-green`，Java PID `32348` 及完整命令见 `green-java-process.json`。`green-gametest.log` 于 2026-10-01 16:31:19 明确记录 `141 GAME TESTS COMPLETE` 与 `All 141 required tests passed :)`，没有断言失败。随后停在 `Saving worlds`；有限等待后核对命令并只结束 PID `32348`，时间/原因见 `green-java-stop.json`，Gradle 退出 `1`。因此只主张 **141/141 required GameTest 断言通过**，不称运行器正常退出，也未为退出码额外重跑。
- 最终 `gradlew test build --rerun-tasks --max-workers=1` 的 `final-test-build.log` / `final-test-build-exit-code.txt` 为退出 `0`、`BUILD SUCCESSFUL`；52 个 JUnit XML 合计 265 项、失败 `0`、错误 `0`、跳过 `0`，见 `final-junit-count.json`。最终 `build/libs/create_nuclear_industry-0.1.0.jar` SHA-256 为 `A4382526AD6AF43A7A82A80992C2F33681D950F5DA1142AFC792108A66FD3A5A`。`artifact-verification.json` 记录 JAR 含 55 张本模组 PNG、14 条本批新配方；174 个源 assets/recipe/nugget tag 文件与 JAR 对应条目逐字节一致，缺失或不一致 `0`。

## 隔离与交付边界

`run-sha256-comparison.json` 与 `run-saves-sha256-comparison.json` 记录默认 `run/` 176 文件、`run/saves` 68 文件前后 SHA-256 均无增删改；旧目录事故现场未清理。Gradle 自动修改了候选根目录跟踪 `logs/debug.log` 与 `logs/latest.log`，执行者没有 Git 写权限，不自行回退；已通知项目经理备份和恢复。正式构建脚本、旧报告、用户存档、核心文档均未修改。

尚未启动客户端或联调第三方金属模组。用户仍需在客户端复测普通/深层矿石和粉碎料的铅锡熔炉与高炉产出、Create 水洗九粒与动力恢复、九粒↔锭合成、JEI 配方及用途、六物品外观和保存重进；人工结果由项目经理收集。执行者不宣布验收、提交或合入。
