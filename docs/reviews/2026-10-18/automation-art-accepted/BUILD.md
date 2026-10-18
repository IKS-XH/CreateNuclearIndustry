# 自动化接口C稿集成包身份核验

2026-10-18。本报告仅交付一次增量构建与制品身份核验，不登记项目验收或新增Minecraft视觉反馈。

- 准确已提交来源：`6fb97c40dfd50d0bf6c8a1835cd609374f514a51`，基准main为`a00b1b908cd933e048e45cdf78a82ecd0a6915a8`。美术来源`4c354d9caf8cbeeb60763f113b3142200f65f12d`，生产来源差异严格只有模型修改与侧面PNG新增，见`source-delta.txt`。
- 唯一构建：`./gradlew.bat assemble --console=plain`，exit 0，Gradle报告14s；compileJava UP-TO-DATE，processResources/jar执行，jarJar NO-SOURCE。无clean、rerun-tasks、build、JUnit、GameTest、客户端、浏览器或预览重跑。原日志及时间见`assemble.log`、`assemble-command.json`。
- 原始Gradle候选：`candidate/create_nuclear_industry-0.1.0.jar`，**3490000 bytes / SHA256 `0763c210f654ccb1e3679e6d70ec9fb3a648f43f29997ef1685dace1b4fbcbed`**。未重封包；PM可提升此精确文件。
- 2137项生产源/资源/Gradle输入与准确Git来源逐项绑定，工作副本仅允许原字节相同或CRLF归一后相同；无额外生产文件，构建前后输入完全一致。见`source-inputs-before.json`、`source-inputs-after.json`。
- JAR全部2436非目录entry均解压/CRC通过；748个class逐项绑定`build/classes/java/main`，1687资源逐项绑定`build/resources/main`，manifest绑定`build/tmp/jar/MANIFEST.MF`，0绑定差异。完整清单见`jar-output-bindings.json`。
- 两资源与art集成清单精确一致：模型363 bytes / SHA256 `03497963005d2f891974ff54cadba15d70f4d45a69a82d53368acaa02edaa870`，PNG237 bytes / SHA256 `f9cecc1fcb5f9b4348e222dbc82154f07a77f565a7fe5d978a041fe591449d13`。模型沿用`minecraft:block/cube_bottom_top`，side/particle指新PNG，top/bottom指现有casing，item wrapper/blockstate未变。

## 与上一正式81E的精确差异

原81E为3478572 bytes / SHA256 `81e7cd98d589ddc9c0aaf398c06776dc4318ea54c70cd19376f0c127e4a3de7b`。原始entry比较共1327项变化：预期模型修改、PNG新增，以及1325项文本资源LF→CRLF。所有class、manifest及其余二进制/资源均无字节差异，无其他新增或删除entry。

1325项额外字节差异均严格UTF-8解码成功，LF归一后与81E逐字节相等，且全部等于准确来源的Git blob；旧81E这些entry全LF，本次全CRLF。实际`core.autocrlf=true`，工作副本CRLF经processResources原字节进入JAR。metadata字段没有变化。依据PM明确指示保留该原始Gradle输出，未改源或重新打包。原始严格“两项字节变化”核验失败也保留在`ARTIFACT-strict-byte-check.json`，不隐藏初次失败。

必要完整证据索引：`entry-changes.json`为原始SHA/size差异；`entry-changes-classified.json`为全部逐项归类；`line-ending-diagnosis.json`含严格UTF-8、两侧换行计数、规范化SHA及Git blob绑定；`ARTIFACT.json`记录最终身份核验结果与边界。

## 排除、保护和复用边界

构建规则要求排除的本批/近期10类探针共18个编译class及10份测试模板均未入包；事故d839新增12个类型及其内类、事故资源与对应测试域均未混入。既有正式基线保留的85个GameTest class保持原字节，未擅自清理或声称整个包没有任何历史测试类。准确排除清单见`ARTIFACT.json`。

本树根JAR原字节已恢复为81E，4份根日志均已保存基线和构建后副本并恢复/核对原字节，最后tracked状态为空。候选仅保留报告目录，未提升正式包、改main/art来源、操作Git写、改docs或访问run/saves。世界及main47项dirty保护复用PM的`PREFLIGHT.json`、`world-source-manifest.json`、`main-dirty-before.json`，未重复扫描。

已只读复用art源树`docs/art/tasks/ART-AUTOMATION-17B.md`、`docs/art/reports/ART-AUTOMATION-17B-REVIEW.md`与`build/reports/art/ART-AUTOMATION-17B/integration-manifest.json`。原像素/alpha、父链/引用、组合审查、生产PNG预览证据未重跑；用户选C及授权合入与真正Minecraft视觉观察分别记录，本轮不新增未报告的客户端结论。

实际读取并应用`C:/Users/IKSXH/.codex/skills/`下四项技能：`minecraft-modding/SKILL.md`核模组来源/加载边界；`minecraft-testing/SKILL.md`按治理5.1复用已有资源证据、区分构建与游戏验收；`minecraft-resource-pack/SKILL.md`核1.21.1原生模型引用与打包；`minecraft-ci-release/SKILL.md`核准确来源/JAR身份。另读`verification-before-completion`实施证据后结论，`systematic-debugging`只读定位意外换行差异。实际Gradle版本锁保持MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，未改依赖、许可或发布方式。

交件后停工；最终main包提升、Git提交与验收状态由PM处理。
