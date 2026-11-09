# 首个GitHub公开Alpha：规格/质量组合审查

审查日期：2026-11-09。结论：**PASS（限定本卡公开Alpha准备范围，无具体阻断）**。这是独立审查报告，不是PM验收、人工体验通过或GitHub已公开发布的声明。

## 范围与方法

- 只读核对主树已提交来源 `ea7ed52f2bba93594a711cce4cbf32c8cd720984`、README、`docs/releases/0.1.0-alpha.md`、活动卡、已有构建日志/原始JUnit XML、前后快照和制品清单；未运行Gradle、GameTest、客户端、服务端或其他测试，未做Git写入或发布。
- 已读取AGENTS、治理5.1/6、STATUS和本次活动卡。实际读取本机 `C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-ci-release,minecraft-resource-pack}/SKILL.md`：用于核版本/加载器与元数据边界、分离JUnit和人工证据、核tag/预发布/制品身份，以及资源和许可载荷保持；未套用技能示例升级版本、新增CI或发布平台。
- 唯一写入为本 `REVIEW.md`。结论先直接基于下列原始证据和 `ARTIFACT.json`形成；`BUILD.md`落盘后已追加只读核对，构建、测试计数、包身份、载荷与保护叙述和原始证据一致，无新增矛盾或验证门。

## 规格核查

1. `git show ea7ed52f`只有 `gradle.properties:mod_description` 和 `src/main/resources/META-INF/neoforge.mods.toml:description` 两处变化，正文逐字等于卡中批准英文：

   `A Create addon for uranium processing, configurable reactors, heat and steam systems, rotational power generation, radiation protection and spent-fuel storage.`

   版本仍0.1.0；Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6与实际Gradle保持一致。依赖范围、MIT、Logo/第三方例外、构建排除和生产逻辑未改。
2. README英文已删除管理员应力闭环及辐射/事故未实现旧措辞，与STATUS已合功能一致；保留Alpha、长期整线/整合包继续验证的边界。中文安装入口、版本表及当前功能与本卡一致。
3. `docs/releases/0.1.0-alpha.md`覆盖客户端/服务端安装、MC/Java/NeoForge/Create基线、Create自身依赖、JEI可选、源码压缩包不能安装及JAR/SHA附件说明。中英内容明确长期整线、超大多人、全部光影/整合包未充分验证，不冒记其通过；管网压力、复杂再处理与电站级协调保持后置。
4. 拟用 `v0.1.0`、标题 `Create: Nuclear Industry 0.1.0 Alpha (NeoForge 1.21.1)` 与 `prerelease=true`符合活动卡。此处只审拟定配置；远端tag、Release字段、附件上传及公开后的下载回核由PM在实际发布时核实，当前未宣称存在。

## 构建、测试与制品证据

| 项目 | 实际证据/结果 |
| --- | --- |
| 构建入口 | `build-run.json`：唯一一次 `.\gradlew.bat build --console=plain`，2026-11-09T04:19:45.0843815Z至04:20:43.9241537Z，58.8345338秒，exit0；`build.log`完整结尾为 `BUILD SUCCESSFUL in 58s` |
| JUnit | 独立读取 `build/test-results/test/TEST-*.xml`：153类、871项，其中860通过、0失败、0错误、11跳过；不是871项全通过 |
| 原跳过 | `BoilerSodiumSurfaceTest` 4、`ReactorSodiumSurfaceTest` 3、`TurbineSodiumAppearanceTest` 4；原始XML与 `skipped-tests.json` 一致。三类现有 `@BeforeAll` 以 `cni.sodium.fixtureDir` 门控，测试源未改；与STATUS原11项跳过一致，不称本批Sodium实测 |
| 旧正式包 | `original-0B912E12-create_nuclear_industry-0.1.0.jar`，3781913 bytes；独立核SHA256 `0B912E12FA128F485432A3E1FB71937D2A88EA3844A3F7F07FB9E9B79095D932` |
| 本批正常构建包 | `build/libs/create_nuclear_industry-0.1.0.jar`，3781898 bytes；独立核SHA256 `D2B5390C368779CA881A391711F7F1B4158A666F407B5FF94F860506EA6175F3` |
| 载荷对比 | 读取 `payload-comparison.csv` 与 `ARTIFACT.json`：前后各2621载荷，无增删，仅 `META-INF/neoforge.mods.toml` 变化；其余2620及全部825 class原字节相同，无class变化 |
| 元数据和许可 | 直接读本批JAR的mods.toml，版本/依赖/MIT与源一致，description准确；清单确认TOML正文外保持，LICENSE、NOTICE.md、Create-LICENSE.md与原包及当前来源一致；现有排除探针无泄漏 |
| 来源保持 | `before.json` / `after.json` 均为main、准确ea7ed52f；2440输入哈希无变化。当前源码无dirty，构建新增受跟踪变更只有正式JAR |
| 3D无关dirty | 前后dirty列表除正式JAR外一致；`package.json` SHA256 `1C0D0AE2818A8BF62398AFCB3F33013788CFC083C42AFA5092BCA0910A4FB990`、`scripts/serve.mjs` SHA256 `D7402108AE99217AD555C85969E0CF3DD04EA218A78DBC710D5B82D43155E63D` 前后保持；未清理/覆盖用户工具、来源树或世界 |

原始证据均在本目录：`build.log`、`build-run.json`、`ARTIFACT.json`、`payload-comparison.csv`、`before.json`、`after.json`、`skipped-tests.json`；JUnit原始XML位于主树 `build/test-results/test/`。

## 证据边界

本批纯简介改动且生产class/素材载荷未变，按治理5.1复用已验设备、25幕思索及当前保存/加载来源，不新增重复人工门；未将自动构建、原始清单或本报告替代客户端视觉、实际操作、听感、整线、物理卸载或真人多人验收。STATUS独立观察继续保留。未开展旧存档兼容/迁移、扩大许可研究或其他首发功能研究。实际公开发布及附件下载回核尚未执行，不在本报告中追认为完成。
