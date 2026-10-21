# ACCIDENT-THERMAL-04B 原始候选制品

2026-10-21；准确已提交来源 `afb16899972cbe1495e31857dd2d3b75286ada59`，分支 `codex/accident-thermal-04b`。打包前tracked干净，32源/资源逐项匹配提交及SOURCE，51个受测class、6个处理资源保持原SHA，见 `15-build-input.json`。

唯一增量命令 `.\gradlew.bat jar --console=plain`，exit0、Gradle1秒；原始输出 `15-jar.log` / `15-jar.exit.txt`。compileJava、processResources、classes均UP-TO-DATE，只有jar实际执行；未clean/rerun/test/assemble/build、重编或手工重封包。实际读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-ci-release/SKILL.md` 来源/增量制品核验，原锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6保持。

原始 `build/libs/create_nuclear_industry-0.1.0.jar` 同字节复制为 `candidate-accident-thermal-04b.jar`：**3589016 bytes**，SHA256 **`0F950A1B7379F0D017339DFF94A0747445F9076857265F3CBC99D68C70BE08C9`**。两者字节一致，未改zip内容。

`ARTIFACT.json`与`JAR-CONTENTS.json`逐项绑定全部783个class、1700个processResources资源及1个原生生成manifest，0不匹配。SOURCE全部51个class保持；其中46个生产class准确入包，2个真实域class及3个JUnit class排除。9个新增main源码衍生13class全部在包内并与受测SHA一致，另外既有源新增匿名class亦在SOURCE/全包清单内。熔融液块/FluidType/Fluid、三项精确Mixin及配置/本体/辐射class、molten blockstate/model/空loot/双语、原HOT/事故火资源均按构建输出入包。

本批 `AccidentExecutionGameTests`（含三个边界fixture方法）、`data/cni_accident_execution/`和全部已编译JUnit类不在包内。广义扫描另发现85个历史GameTest/probe class，已与pre-validation原81E包逐路径/SHA核对完全一致；它们沿既有Gradle“旧域保持”规则保留，本报告不称全仓历史测试类全部剔除，不擅自修改构建规则或再打包。

业务证据复用10轮4业务+10SKIP、11轮1续接+13SKIP、12轮定向JUnit10/10及14轮三项整改业务+14SKIP，均exit0；未重复业务或启动用户世界。人工客户端/多人等边界仍见EXECUTION。

根新JAR作为原始输出保留交PM恢复旧81E备份，当前唯一tracked变化为该根JAR；两份根日志仍3489bytes / B094FA55…未变。未Git写、未操作main正式17A/其它候选或核心docs。原REVIEW闭合记录与SOURCE不改；制品交付不等于人工验收或main合入。完成后停工。
