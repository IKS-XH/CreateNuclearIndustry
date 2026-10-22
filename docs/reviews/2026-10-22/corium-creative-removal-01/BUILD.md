# 创造拆残骸修复：阶段B制品

2026-10-22，准确来源`1985d1b3172a5efd1f0c87f3aa39ce3fe2a6feb7`，基准`afb16899972cbe1495e31857dd2d3b75286ada59`。PM只提交阶段A两源，开工tracked干净；受测两源及三个编译class的SHA与SOURCE.json一致。按minecraft-ci-release技能核来源/输出身份，实际依赖及原构建规则保持；未照搬升级/发布示例。

唯一执行`./gradlew.bat jar --console=plain`，真实exit0、`BUILD SUCCESSFUL in 2s`。createMinecraftArtifacts/compileJava/processResources均UP-TO-DATE，jarJar为NO-SOURCE，仅jar执行。命令时段和退出见`jar-exit.json`，原日志为`jar.log`。未重复测试、clean、rerun、assemble或build。

| 文件 | bytes | SHA256 |
| --- | ---: | --- |
| candidate-corium-creative-removal.jar | 3589179 | 75a26775a9ba80e6850bd7ed002efb9895c2fd196296232c3192a7dc0e3051c3 |
| 根build/libs/create_nuclear_industry-0.1.0.jar原始输出 | 3589179 | 75a26775a9ba80e6850bd7ed002efb9895c2fd196296232c3192a7dc0e3051c3 |
| 旧04B独立候选，保持 | 3589016 | 0f950a1b7379f0d017339dff94a0747445f9076857265f3cbc99d68c70be08c9 |

候选直接复制本次原输出并核同字节，没有修改或重封JAR。全部交件位于本目录；根新JAR保留，交PM按`before/`原81E备份恢复，本执行者未做Git写或恢复。

2321个源/资源/测试及Gradle输入全部绑定准确提交，408项与Git blob原字节一致，1913项仅工作区CRLF/提交LF不同（逐项UTF-8、无NUL且CRLF→LF后完全相等），不放行其它内容差异。打包前后输入与受测class均未变。详单`build-inputs.json`，SHA256 `221423aa27deb02dba342da18b8605fd992ca107489af57b5987929e99cb161d`。

最终2484个非目录条目无重复：783class逐字节绑定`build/classes/java/main`，1700资源逐字节绑定`build/resources/main`，manifest绑定`build/tmp/jar/MANIFEST.MF`。完整路径/bytes/SHA/输出绑定见`payloads.json`；小型`ARTIFACT.json`只引用其身份，不重复清单。

与旧04B比较，路径集合不变，仅`com/iksxh/create_nuclear_industry/accident/ReactorCoriumBlock.class`由6544变7078bytes，SHA从`0ea00d830a1173c69c0ecac798647ed8251576c735c1b5e94cab7cc207f1eaa8`变为阶段A已受测的`72fbe326fbcf40674d0a7484516e5a20241c075ce45c1da7726c99f8c65c8b1f`。其余2483项逐字节保持，包含782个其它class、1700资源及manifest；BE相同，新增/删除/换行例外/额外内容差异均0。外层ZIP重新生成及压缩后总大小变化不代表其它载荷变更，详细双侧SHA见`payload-diff.json`。

既有`AccidentExecutionGameTests`（含内部类）、`data/cni_accident_execution/**`仍按原规则排除；实际199个JUnit输出class与包内无交集。85个历史gametest/probe class原边界保持且与旧04B同字节，没有扩大为清除全仓历史测试。mod元数据与旧04B同字节，本树原许可/依赖不属本次修复写集。

原81E包备份与两份B094根日志/备份仍保持原字节，未改用户世界、main包或其它树。阶段A真实RED和集中GREEN（1业务通过＋17显式SKIP、Java/Gradle exit0、全部维度保存）复用，没有重跑旧04B矩阵。仅留本修复创造拆残骸的客户端复看，不补记物理卸载或真人多人。执行交件后停止。
