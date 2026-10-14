# 锅炉美术集成打包记录

2026-10-11，执行者仅持有本轮打包。唯一工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，构建前后 HEAD 均为 `eae012eed3fe7a834c24d7635369d67756d841ca`。

仅运行一次 `.\gradlew.bat assemble --console=plain`，原始退出码 **0**，日志 `BUILD SUCCESSFUL in 4s`。北京时间 2026-10-11 23:49:16.808 至 23:49:22.036，实测命令耗时 5.226 秒。增量执行 compileJava、processResources、jar；没有 test、GameTest 或客户端任务。编译记录六项现有 NeoForge EventBusSubscriber 弃用警告，没有编译错误。

本轮产物 `build/libs/create_nuclear_industry-0.1.0.jar`：**2,630,603 字节**，SHA256 **`81dc04a8d8101b4b0acf6f83c9118072e6ba6cfd8e351895ae3643b0903ea1de`**。

完整原始日志、退出码、UTC 时间与 JAR 身份仅保存在 `build/reports/art/ART-BOILER-INTEGRATION-2026-10-11/` 的 `01-assemble.log`、`01-assemble.exit`、`01-assemble.json`、`jar-identity.json`。源码、资源与已审候选的制品一致性由主 PM 按本批卡核实，本记录只证明这一轮集成编译打包。

已实际读取根 AGENTS、本批集成卡、治理5.1及 `minecraft-modding` / `minecraft-testing` 技能，核对并沿用 MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。按本卡和治理范围复用 PM 已核的原10/10、11/11、R1受影响2/2及独立窄审；本轮未重跑测试，也没有 clean、全量 build、客户端或存档操作。

除正常 Gradle 产物、上述证据及本报告外没有写入；未修改源码、测试、资源、工具、构建或治理文件，未执行 Git 写操作或派发。原日志与缓存 dirty 保留。打包完成后停止写入，交主 PM 集成与制品更新。
