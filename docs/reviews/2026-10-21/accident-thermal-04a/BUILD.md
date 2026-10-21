# ACCIDENT-THERMAL-04A 已提交来源候选

2026-10-21；准确源码 `9f53a7d5218d1a51b6fdc43bbeb98735a72d9acd`，事故树 `codex/accident-thermal-04a`。打包前 tracked 干净，23份受测源码/资源及20个受测编译class均与 SOURCE 原清单逐字一致，见 `PREBUILD.json`；三份核心docs为PM已提交变更。

唯一命令 `./gradlew.bat jar --console=plain`，exit0；Gradle显示1秒，外层计时2.1877265秒。`compileJava`、`processResources`、`classes`均UP-TO-DATE，仅jar执行；无clean/rerun/tests、手工重封包或真实重编。原始输出 `jar.log`、`jar.exit.txt`、`jar.elapsed-seconds.txt`。

原始Gradle输出 `build/libs/create_nuclear_industry-0.1.0.jar` 原样复制为 `candidate-accident-thermal-04a.jar`，两者同为 **3554973 bytes**，SHA256 **`87D81CE03E02EACFB252BE87662EC304D4FAF39A98B33146C43ECA5D04ED4C16`**。

`ARTIFACT.json`、`ENTRY-MANIFEST.json`、`verify-artifact.log`核明：全部769个入包class与当前编译输出逐字相同，1697个资源与processResources输出逐字相同，另有原生生成manifest。此次20个受测class全部保持原SHA，其中18个准确入包、2个AccidentExecutionGameTests测试class按既有规则排除；`data/cni_accident_execution`及定向JUnit类也没有入包。20指本批受测变更class集合；相对受保护旧81E包，完整候选共有21个新class条目，逐项另列，二者不混为同一计数。

新AccidentFireBlock/Lease、热状态/配置class、HOT blockstate/模型、事故火blockstate/模型、fire标签、空loot和双语资源均在包内。复用 `03-unit.log` 的9/9与 `06-server-final.log` 的2业务/11 SKIP/exit0，不重复测试；当前格式进程重启续热、物理卸载、真人多人和客户端人工门仍保留。

实际读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-ci-release/SKILL.md` 的来源/增量制品核验；MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6及原构建/发布规则不变。无Git写、推送、发布或用户世界操作。

根新JAR保留为本轮原始输出，当前唯一tracked变化是该JAR，交PM恢复原81E备份；执行者未恢复或覆盖main正式17A包，其他候选保持。旧81E备份仍在 `pre-validation/build_libs_create_nuclear_industry-0.1.0.jar`。本候选不代表用户体验验收或main集成放行。交付后停工。
