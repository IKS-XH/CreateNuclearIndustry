# 已验事故链与R1集成：阶段B制品

准确来源`3b905260537bc1989a4465a6c0109a59d21218a7`。PM提交合并并同步文档后tracked干净；2407个阶段A输入及全部828个受测开发编译class的SHA保持，生产相对原main的精确78路径与SOURCE清单相同。按minecraft-ci-release技能核来源/输出，不改版本、源码、规则或许可。

唯一`./gradlew.bat jar --console=plain`，UTC 2026-10-23T14:18:38.1826152Z至14:18:40.4972139Z，真实exit0、`BUILD SUCCESSFUL in 2s`。compileJava/processResources保持UP-TO-DATE，仅jar执行；未重测、build、assemble、clean或rerun。原日志/退出为`jar.log`、`jar-exit.json`。

| 原始输出及同字节候选 | bytes | SHA256 |
| --- | ---: | --- |
| candidate-accident-accepted.jar | 3677228 | f29d0f8b20646e577a273cf5b768fbf9e9244b30699f2eb28b923dd783fbb13c |
| 根build/libs/create_nuclear_industry-0.1.0.jar | 3677228 | f29d0f8b20646e577a273cf5b768fbf9e9244b30699f2eb28b923dd783fbb13c |

候选直接复制本次原输出，无重封包。根新JAR留PM核原输出后恢复EF3C；旧3572469bytes/SHA `ef3c0feb3d914c0c2b290d1e5017c169ec606717237645e525065fe82bc966f8`备份仍在`baseline/`。

全部2407输入绑定准确提交：499原字节一致、1908仅工作区CRLF与提交LF不同，逐项UTF-8/无NUL及仅CRLF→LF后完全一致。清单`build-inputs.json`，SHA256 `2c717b329962476ac3c2b86f9a35493babbeff8f6f7920709306e6f0ccf90116`。打包前后受测输入/class保持。

最终2588个非目录载荷无重复：809class、1775资源、manifest及三许可声明全部逐字节绑定实际构建输出/许可源。class另按真实SourceFile绑定已提交Java；完整SHA/bytes/源与输出路径在`payloads.json`。R1的17源、16处理资源/包内资源、2客户端class身份保持；三许可及MIT元数据保持。事故GameTest/测试域未入包，214个JUnit输出class与包内无交集，85个历史probe类政策及字节保持。

相对EF3C，2499项原字节保持（748class、1747资源、manifest及三许可），31项修改（26class/5资源）、58项新增（35class/23资源）均绑定SOURCE的本批允许源。零删除、零非批内容变化、零额外LF/CRLF或调试信息例外。逐项分类见`payload-diff.json`；没有扩查或修复旧业务。

阶段A验证结果继续如实保留：retrofit一次7/7业务、0SKIP、exit0/正常保存；唯一全JUnit为854项中840通过、3失败、11跳过、exit1，main两类15项已精确复现同三失败，见`MAIN-BASELINE.json`。本次仅增量jar成功，不能称全量测试通过；未推定裂变差分根因或顺带修复。

SOURCE/EXECUTION保留阶段A当时语境，最终包身份以本报告及小型`ARTIFACT.json`为准。原始失败XML、旧包及运行日志备份保持，未操作用户世界、main日期旧包/3D dirty、Git或发布。阶段B交件后停止。
