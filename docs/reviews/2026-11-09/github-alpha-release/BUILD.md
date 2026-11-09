# GitHub Alpha主树唯一正常构建交付

- 主树/main准确源 `ea7ed52f2bba93594a711cce4cbf32c8cd720984`，前后HEAD不变。仅执行一次 `./gradlew.bat build --console=plain`；无clean/rerun/skip、未跑候选build。2026-11-09 12:19:45.084～12:20:43.924 +08:00，墙钟58.835秒、exit0，Gradle `BUILD SUCCESSFUL in 58s`；9项actionable，3执行/6 UP-TO-DATE，实际执行processResources、jar、test。
- 本次JUnit实际153类/871项：860通过、0失败、0错误、11原有Sodium夹具门跳过。11个类/方法与旧0B9报告逐一完全一致，条件仍为三个类原@BeforeAll未提供 `cni.sodium.fixtureDir` 的assumeTrue；未改跳过规则，不称871全通过。原XML继续保留 `build/test-results/test/`，短名单见 `skipped-tests.json`。
- 构建前后2440个输入SHA完全保持，构建前后均核对已提交源无diff；两份用户3D工具SHA和原有dirty逐行保持，完成后仅另增正式JAR的M。前后清单见 `before.json`/`after.json`，未修改3D工具、世界、源码、核心文档或Git，未启动游戏。

## 制品差异与身份

旧包在运行前完整冻结至本目录 `original-0B912E12-create_nuclear_industry-0.1.0.jar`：3781913 bytes，SHA256 `0B912E12FA128F485432A3E1FB71937D2A88EA3844A3F7F07FB9E9B79095D932`。

新包 `build/libs/create_nuclear_industry-0.1.0.jar`：3781898 bytes，SHA256 `D2B5390C368779CA881A391711F7F1B4158A666F407B5FF94F860506EA6175F3`。

两包均2621载荷、无增删，唯一修改 `META-INF/neoforge.mods.toml`；剔除description正文后全文严格一致，正文准确为活动卡英文。全部825个class及其他2620载荷原字节一致，无换行豁免。逐项双侧SHA见 `payload-comparison.csv`，摘要见 `ARTIFACT.json`；不是依赖文字检查来推定载荷相同。

当前0.1.0、MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6保持。包内依赖范围仍MC `[1.21.1,1.22)`、NeoForge `[21.1.219,)`、Create `[6.0.10,6.1.0)`；全为required/BOTH。MIT、LICENSE、NOTICE及Create许可证与旧包字节一致且逐项等于main来源；Logo及其保留权利、第三方声明保持。既有开发GameTest排除规则源码未变，23项glob/2项前缀均无包内泄漏。

## 边界与证据

实读并应用本机 `C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-ci-release,minecraft-resource-pack}/SKILL.md`：核锁定平台、按发布门唯一正常build、区分JUnit/游戏/视觉、严核资源与许可载荷身份；未升级或新增发布系统。纯描述变更与所有生产/素材载荷不变支持复用原已验游戏/教学，未新增客户端/全GameTest/旧存档兼容验证，未补记STATUS独立观察通过。

完整本轮命令/输出与时间：`build.log`、`build-run.json`。只读核验工具 `capture-inputs.ps1`、`verify-artifact.ps1` 已实际执行；最终摘要额外与旧0B9报告比对了11个skip名称。JAR工作副本待PM审查/提交/发布，执行者未进行Git写或GitHub操作。
