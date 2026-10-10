# MAIN-BUILD-01 主分支完整构建验收

2026-10-11，用户报告主分支`.\gradlew.bat build`失败。主PM按[活动任务卡](../../../superpowers/plans/2026-10-11-main-build-01.md)安排指定执行者修复、一次独立窄审并完成Git集成。**本次自动构建故障已关闭。**

根因为`ReactorConnectedTextureTest`两处原JSON输出依赖美术工作区已经存在的报告父目录。main修复提交`8fa63632ce1c37f6f2531d23c268af69b8a51345`仅在该测试新增两次`Files.createDirectories`及中文说明，共四行；输出路径、内容、17个原测试与全部断言保持。未修改生产功能、素材或构建脚本。

修改前目录不存在，唯一17项定向RED复现相同两个`NoSuchFileException`，退出1。修改后主目录唯一完整`.\gradlew.bat build --console=plain`于00:42:35～00:42:56实际执行，退出0、`BUILD SUCCESSFUL in 20s`；82套454项，0失败、0错误、0跳过。`compileTestJava`与`test`实际执行，两份原输出JSON实际创建47条上下文和7个场景。PM核对原始日志、退出值、运行时间、82份XML及源码SHA，并复用[一次独立规格/质量审查](./REVIEW.md)，无必改项。细节见[实施报告](./IMPLEMENTATION.md)，原始证据保留在主工程`build/reports/main-build-01/`。

已验证测试源码SHA256为`6d5cc31d258b35e72638833a30f9b7bb134605766f3836e53e566edac6da2fba`。本轮生产编译和JAR均UP-TO-DATE，**复用用户此前本地生成的制品**：`E:/MyMC/NewMod/Create_NuclearIndustry/build/libs/create_nuclear_industry-0.1.0.jar`，2,504,432字节，SHA256 `0974e4b3ee9c4c15931c4e0b59625252dfa0687903e60f072d3cfc7b07c8670b`。原美术集成报告的制品身份代表其原打包时点，未改写原证据。

同一测试修复净同步至美术分支`3e3045f8b3851c51aeb6da323892ec6f52c1ebe1`，无冲突；不重复运行同实现的测试或构建。主PM实际应用Minecraft modding/testing及系统排错、测试先行和完成前验证；全量build仅用于关闭用户本次明确的完整构建失败，服从治理5.1，不扩展旧存档或客户端矩阵。本项不需要新增手动测试，不改变既有视觉验收状态或主线排期。现有`.gitignore`、日志和未跟踪资料保留，未清理用户工作区或世界。
