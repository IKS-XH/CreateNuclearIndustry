# ACCIDENT-SHUTDOWN-03 阶段C候选制品

2026-10-20。原始执行者报告与清单在`E:/MyMC/NewMod/Create_NuclearIndustry-accident/build/reports/accident-shutdown-20261020/`，本归档仅调整链接；打包前源码交付见EXECUTION。本页为组合窄审通过、PM准确提交后的打包结果，不代表已提升main或用户原现场已通过。根包随后由PM核备份恢复，见CANDIDATE。

- 来源提交 **37f50b9e3425992f74beecb8ecd6e2c79514257e**，打包前事故树tracked干净。复用既有批量方法，以ls-tree和一次cat-file --batch核2148项实际Java/资源/构建输入与准确提交blob一致（按Git CRLF归一），原始字节SHA在打包前后不变；两份源码及Port编译class仍等于最终04绿色证据。
- 唯一命令 **`./gradlew.bat jar --console=plain`**，仅执行一次，**exit0**；Gradle报告1s，compileJava/processResources/classes均UP-TO-DATE，jarJar NO-SOURCE，仅jar实际执行。没有运行test、assemble、build、clean或重新打包。原始命令/日志/退出码为 `jar.command.txt`、`jar.log`、`jar.exit.txt`，实际耗时另存 `jar.elapsed-seconds.txt`。
- 原始输出 `build/libs/create_nuclear_industry-0.1.0.jar` 同字节复制为报告目录 [candidate-accident-shutdown-03.jar](/E:/MyMC/NewMod/Create_NuclearIndustry-accident/build/reports/accident-shutdown-20261020/candidate-accident-shutdown-03.jar)：**3,535,322 bytes**，SHA256 **A2D7EE399D32C6392A2872072450971FA700BC73893AFCFE6F168926B0759DF3**。未修改ZIP或重封包。
- 核验exit0：**762个class、1692个resource、1个manifest额外文件，共2455个文件条目**逐项匹配当前编译/资源输出SHA；每个class均关联当前已提交源。没有重复文件、哈希差异或未解释遗漏；31个未入包输出全部匹配既有Gradle排除规则。
- ZIP另有**249个目录条目**，其中16个目录没有文件后代；这是独立目录口径，不计入2455文件。AccidentExecutionGameTests主体/内部class文件为0，`data/cni_accident_execution/`域文件及目录均为0。既有其它测试域的空目录保留原样，没有为此改构建或重打包。
- 与上一事故接口退出候选4B37F69D…FE9E3499比较：无新增/删除文件，仅ReactorPortBlockEntity主体和4个内部class载荷变化；其余2450文件（含全部资源）逐字一致。细表见 `ARTIFACT.json` 的previous_candidate_comparison。
- 来源与逐项身份在 `PREBUILD.json`、`BUILD-INPUT-HASHES.json`、`ENTRY-MANIFEST.json`、`EXCLUDED-OUTPUT-HASHES.json`、`ARTIFACT.json`。工具/日志/退出为 `verify_artifact.py`、`preflight.log/.exit.txt`、`verify.log/.exit.txt`，目录口径补记见 `finish_artifact_metadata.py`、`metadata.log/.exit.txt`。它们只读取源码/编译输出并写本报告目录。
- 复用最终04真实聚焦业务1通过/11 SKIP、正常保存与exit0及唯一组合审查，无新测试。用户原世界、root文档/日志、main正式包未写；执行者没有Git写或恢复任何跟踪文件。
- 打包后唯一tracked差异为事故树根 `build/libs/create_nuclear_industry-0.1.0.jar`，按PM要求保留原始新产物供核对/恢复。PM备份 `pm-tracked-jar-before.jar`仍 **3,478,572 bytes / 81E7CD98D589DDC9C0AAF398C06776DC4318EA54C70CD19376F0C127E4A3DE7B**。执行者未从HEAD/备份恢复，不提升main，不推进高温。

集中人工复看仍为：候选原中间堆正常可拆、处理中事故口继续受保护、已通过的接口退出逐棒解锁保持。
