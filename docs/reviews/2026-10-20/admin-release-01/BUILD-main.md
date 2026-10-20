# ADMIN-RELEASE-01 阶段B主线候选

执行者打包报告快照，PM仅调整归档链接并明确空目录口径。原始日志/清单在`E:/MyMC/NewMod/Create_NuclearIndustry-admin-release-fix/build/reports/admin-release-20261020/`；打包后的正式包恢复由CANDIDATE记载。

- 准确生产来源：`ca9f484a992537394ec30acc55677a8884989e6b`，分支`codex/admin-release-fix`；打包前工作树干净，2128项源/资源/构建输入经批量Git树读取确认归属此提交，前后原字节SHA256均不变。
- 唯一打包命令：`.\gradlew.bat jar --console=plain`，仅执行一次，exit0；compileJava/processResources等为UP-TO-DATE，jar执行，1秒完成。原始命令、完整日志、退出码分别为`jar.command.txt`、`jar.log`、`jar.exit.txt`。未执行test/build/assemble/clean或再次打包。
- 原始输出`build/libs/create_nuclear_industry-0.1.0.jar`直接同字节复制到报告目录[candidate-admin-release-main.jar](/E:/MyMC/NewMod/Create_NuclearIndustry-admin-release-fix/build/reports/admin-release-20261020/candidate-admin-release-main.jar)，3526970 bytes，SHA256 **2257F708816870A42F7AE9DA74F1A82D545E580A0DE7FBE4636B4A2A3DF08C1D**；未重封包。
- 身份核验exit0：762个class、1687个resource、4个额外条目（manifest/原许可文件），共2453个文件条目与本次编译/资源输出或配置来源逐项SHA一致。26个被排除输出均匹配现有Gradle规则；无重复条目、无哈希差异、无未解释遗漏；本次Retrofit测试类和`data/cni_reactor_automation_retrofit/`域文件条目均为0，保留2个零长度空目录。
- 机器记录见`ARTIFACT.json`；源输入清单`BUILD-INPUT-HASHES.json`含Git blob和原字节SHA；包条目/输出绑定见`JAR-ENTRY-HASHES.csv`；合法排除见`EXCLUDED-OUTPUT-HASHES.json`；核验工具/日志/退出码为`Verify-Candidate.ps1`、`verify.log`、`verify.exit.txt`。
- 复用阶段A已核JUnit32/32和`review-high-green.log`真实域6/6、exit0及已关闭P2的唯一组合审查，不将此构建记作新测试或客户端验收。版本/配置和源码未改。
- 打包后Git唯一差异为本树`build/libs/create_nuclear_industry-0.1.0.jar`，按PM要求暂留供核对/恢复；执行者未Git写或从HEAD恢复。旧`pm-formal-before.jar`仍3525495 bytes、SHA256 `17A0A41616C27B4C2FDC7EE5A2E005973AB5C1EB3D1CA1BA16DB6F2B904ACB88`。候选未提升到main正式包，未触碰用户世界；等待PM收件和用户复看。
