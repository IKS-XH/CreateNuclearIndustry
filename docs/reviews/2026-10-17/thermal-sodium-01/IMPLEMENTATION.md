# THERMAL-SODIUM-01 执行报告

2026-10-17；执行者交付，待PM组合窄审及人工观察。工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-thermal-sodium`，分支 `codex/thermal-sodium-surface`，基准 `7d8fdaabe0ea71032e970d18c393616db2290ee1`。

手写写集恰四项（前缀 `src/main/java/com/iksxh/create_nuclear_industry/` / `src/test/java/com/iksxh/create_nuclear_industry/`）：

- 生产 `boiler/client/BoilerSurfaceSnapshots.java`：确切final LevelSlice、declared非static final ClientLevel字段的ClassValue只读缓存，继续与Publication.world按对象身份比较。capture不查询方块/BE/区块/光照，不借全局世界。
- 生产 `turbine/client/TurbineCasingAppearanceModel.java`：同形状严格上下文验证。atlas继续用入参state/pos，连窗继续从传入切片读正负各一格；旧切片可消费自身局部状态，无owner扫描。
- 新增 `boiler/client/BoilerSodiumSurfaceTest.java`，4项。
- 新增 `turbine/client/TurbineSodiumAppearanceTest.java`，4项。

版本实核 `gradle.properties`、`build.gradle`、`settings.gradle`：Minecraft1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6。未改依赖、资源、协议、服务器或已验反应堆代码。

## 证据索引

- 根因：复核本树旧入口和原始Sodium调用链证据，见main `build/reports/reactor-sodium-surface-01/*.javap.txt` 及本树归档DIAGNOSIS。所有5个真实夹具JAR与前次已核夹具SHA一致，见 `fixture-identities.json`；sodium-mod.jar SHA `54695055628A28678B483E7CA5774CF0F298624E1EF223C381CD641C261BD1E7`。测试断言独立URLClassLoader实际加载，不用仿制LevelSlice。
- 首次RED：`01-red.log`/`.exit.txt`、`red-results-initial/`，7项、5失败、0 errors/skips、exit1。补齐最终CT与双尺寸atlas断言后，`01b-red-complete.log`/`.exit.txt`、`red-results/`：8项、6失败、0 errors/skips、exit1。失败均是未适配入口的预期行为断言，非夹具装载错误。
- 最终GREEN：`02-green.log`/`.exit.txt`、`green-results/`、`green-counts.json`：32项、0 failures/errors/skipped、exit0（新增8项均执行）。锅炉同/旧世界、Create原生CT及最终共享面、原生region/level、未知包装；汽轮机中/大型atlas、真实OBJ连窗12/21/30quad、旧世界局部数据、小型/缺素材/未知回退均覆盖。
- GREEN命令：`./gradlew.bat test --tests '*BoilerSodiumSurfaceTest' --tests '*TurbineSodiumAppearanceTest' --tests '*BoilerConnectedTexturesTest' --tests '*BoilerConnectedTextureBehaviourTest' --tests '*BoilerSurfaceSnapshotTest' --tests '*TurbineCasingAppearanceModelTest' --tests '*TurbineWindowConnectedModelTest' --tests '*ReactorSodiumSurfaceTest' --init-script build/reports/thermal-sodium-20261017/sodium-fixture.init.gradle`。init仅转发fixtureDir。旧反应堆3项同次复核。
- 一次增量assemble：`03-assemble.log`/`.exit.txt`，`./gradlew.bat assemble`，exit0。没有全build、clean、全GameTest或真实服；纯客户端入口未扩大服务器验证。编译日志保留既有EventBusSubscriber removal警告、deprecated API提示和Gradle10兼容弃用提示，未称无警告。
- 精确源码长度/SHA256：`source-bindings.json`；基线源码/旧JAR：`baseline-identities.json`。两个生产文件定向diff空白检查exit0；两个未跟踪新测试另行检查尾空格、EOF空行/换行均通过，见 `source-whitespace.json`、`tracked-source-whitespace.*`。

## 制品身份与精确字节门

- 开工前保存tracked JAR为 `baseline-7d8fdaa.jar`，3333619 bytes，SHA256 `A963B483260AB732B16B74FE0C2C625D8E58328E882BE675A0497C469B173CA3`。
- 原始assemble输出及报告内副本 `create_nuclear_industry-0.1.0_261017_thermal-sodium.jar`：3347467 bytes，SHA256 `BF46FE4E9E598442DEF6C5F69D5358C485D4351F719535EF70391BE40CBDD0BC`。副本仍是原始Gradle包，不冒称已通过精确资源字节门。
- `artifact-verification.json`：2276→2278载荷文件，1038项字节不变；全部class差异恰两处外类及内部类，共9项（新增2个ClassValue匿名类），无其他class差异，无Sodium/Fabric/新增JUnit夹具打包。
- 检查脚本因1231项资源字节差异按预期拒绝，保留原始失败；`resource-line-endings.json` 将其全部逐项证实为LF基线/CRLF检出的纯行尾差异（996 assets、231 data、4 metadata），归一CRLF后逐字符完全相同，无语义差异。未修改资源树、未重跑assemble；原始Gradle包保持原样。

- PM补充裁定允许沿用baseline全部资源字节、仅取本次assemble的9项class生成窄候选，程序为 `make-narrow-candidate.ps1`，没有修改工程构建脚本或重build。最终候选 `candidate/create_nuclear_industry-0.1.0_261017_thermal-sodium.jar`：3336385 bytes，SHA256 `B366EB49F8994BB0213B676F530AE395E03A58F53E59CBA23AB9A77B682D7606`。
- 来源链及最终逐条payload核验见 `narrow-candidate-verification.json`：2276→2278项，2269项baseline原载荷逐字相同；差异恰9项目标class（7项替换＋2项新匿名类），每项字节与原始assemble相同。所有其他class、资源、Manifest/metadata逐字相同，无第三方或新增测试夹具。这是明确的baseline＋已编译class窄候选，不冒称资源已由Gradle重新规范化。

## 正常生成输出与人工界限

JUnit正常写入根 `logs/debug.log`、`logs/latest.log`（tracked），保持其原始运行证据，由PM排除提交；不在手写写集。既有TurbineWindowConnectedModelTest按原机制生成 `build/reports/art/ART-TURBINE-07B-R2/consumer/window-mesh-glass-uv.json`，PM已确认该正常输出例外；未改既有测试或扩展美术检查。正常Gradle JAR输出亦由PM管理，不替换main正式包。

没有Git写操作，没有用户实例/世界写入，也未启动已复制世界；PM已有 `saved-world-sha256.json` 复制证据，本批未重复扫描。未改治理、STATUS或活动卡。

真实Sodium/Iris GPU/真人联机未验。人工应使用保留Sodium/Iris的候选客户端连原服，观察锅炉成型壳/窗、中大型汽轮机壳/轴向窗、拆坏补全、重连、区块离开返回及光影开关。这里只证实入口缺陷回归与候选构建，不认定现场唯一原因或宣布现场已修。

实际读取应用技能：`C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-resource-pack,minecraft-ci-release}/SKILL.md`（版本/模型线程边界、真实夹具JUnit、atlas/OBJ原材质回退、JAR载荷身份）；`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/{systematic-debugging,test-driven-development,verification-before-completion}/SKILL.md`（调用链复核、先有效RED、以实际退出/XML和字节证据报告）。治理5.1与卡限定优先，不套用技能的无关全套build/服务器命令。
