# 防辐射服21B与辐射计20B→20C集成：阶段B制品交付

2026-10-21。按PM阶段B授权，准确生产来源`889f7e7e598681a999374040fa5c5c615bfe2532`，基准`4ad0c91fa22dbbfb3db6fc698aafe55a89d2686b`。二者间恰78项批准源/资源/测试及一份活动卡文档，无其它生产变更。阶段A的SOURCE/EXECUTION保留当时未打包语境，最终制品以本报告和ARTIFACT.json为准。

## 唯一打包及原始身份

唯一执行`./gradlew.bat jar --console=plain`，Java21，UTC 2026-10-21T13:47:11.8062049Z至13:47:13.7776778Z，真实exit0，`BUILD SUCCESSFUL in 1s`。createMinecraftArtifacts/compileJava/processResources均UP-TO-DATE，jarJar为NO-SOURCE，只有jar执行。原始输出见`jar.log`、`jar-exit.json`。

| 交付 | bytes | SHA256 |
| --- | ---: | --- |
| candidate-radiation-art-accepted.jar | 3572469 | ef3c0feb3d914c0c2b290d1e5017c169ec606717237645e525065fe82bc966f8 |
| 根build/libs/create_nuclear_industry-0.1.0.jar | 3572469 | ef3c0feb3d914c0c2b290d1e5017c169ec606717237645e525065fe82bc966f8 |
| 原17A备份，保持 | 3525495 | 17a0a41616c27b4c2fdc7ee5a2e005973ab5c1eb3d1ca1ba16db6f2b904acb88 |

候选是本次原构建输出的直接同字节复制，没有改包、重封或第二次打包。原17A位于`backups/before-import/build/libs/create_nuclear_industry-0.1.0.jar`。根新JAR保留，交PM按同源提升；本执行者未暂存、提交或操作main。

## 全部输入与载荷绑定

2355个构建/生产/测试/资源/许可输入均绑定准确提交，499项与Git blob原字节一致，1856项仅工作区CRLF与提交LF不同；后者逐项UTF-8/无NUL、仅CRLF→LF规范化相同，不允许其它内容差异。打包前后输入无变化。`build-inputs.json`登记每项Git blob、工作字节及规范化分类，SHA256为`c0794f455241fe0b9c137a765b49e70c67ff63a38c8f95d82ee86407210b882e`；不在小型ARTIFACT.json重复整份清单。

最终2530个非目录载荷无重复路径：774 class、1752资源、3许可声明和1 manifest。774 class逐字节等于`build/classes/java/main`；1752资源逐字节等于`build/resources/main`；manifest逐字节等于`build/tmp/jar/MANIFEST.MF`；LICENSE、NOTICE.md、META-INF/licenses/Create-LICENSE.md分别逐字节绑定已提交许可源的当前工作字节。完整载荷、SHA及绑定路径见`payloads.json`。

16个本批class与70项资源仍逐字节等于阶段A身份和原21B/20B/20C已审候选，含20B十资源、20C四资源完整依赖。其余758个正式class保持原17A字节，没有额外class差异；本批两个JUnit类型均未入包。

排除本批16class/70资源后，2444项非批载荷中2138项与17A原字节一致，其中已包含758个class、1379个资源及manifest，不能相加重复计算。另306项仅LF/CRLF不同：303资源（5 mcmeta、73 JSON、223 OBJ、2 MTL）和三许可声明。逐项UTF-8/无NUL且CRLF→LF规范化后完全一致；无内容例外、无遗漏旧载荷、无额外新载荷。详细双侧SHA、字节数及规范化SHA见`non-batch-payloads.json`。

既有历史开发probe边界沿原build.gradle保持，最终包中85个gametest class和41个structure模板的路径集合及字节均与17A一致，清单在`historical-probes.json`；不把它们写成本批新增JUnit或擅自改排除规则。

## 复用证据及范围

MC1.21.1、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6、Java21及MIT保持；构建文件、依赖和许可源均在提交绑定清单。阶段A唯一10项测试exit0及原像素、预览、三次审查证据复用，没有重复test/clean/assemble/build、开服、客户端或世界操作。本轮Git状态仅根跟踪JAR被本次jar更新；无其它跟踪变化。

21B用户四件套手测原文仍按ACCEPTED记录；20B/20C细项与真人多人反馈未报告，不以构建或来源核验补记游戏验收。全部交件位于本批报告目录，候选、世界、主目录dirty及其它树保持。执行完成，交PM收件后停止。
