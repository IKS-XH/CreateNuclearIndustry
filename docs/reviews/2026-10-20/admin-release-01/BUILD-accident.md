# ADMIN-RELEASE-01 阶段B事故候选

执行者打包报告快照，PM仅调整归档链接并明确空目录口径。原始日志/清单在`E:/MyMC/NewMod/Create_NuclearIndustry-accident/build/reports/admin-release-20261020/`；打包后的正式包恢复由CANDIDATE记载。

- 准确生产来源：`96a8421cc6073ea960fd331895f66eb949ea9008`，分支`codex/accident-execution-02`；打包前工作树干净，2148项源/资源/构建输入经批量Git树读取确认归属此提交，前后原字节SHA256均不变。
- 唯一打包命令：`.\gradlew.bat jar --console=plain`，仅执行一次，exit0；compileJava/processResources等为UP-TO-DATE，jar执行，1秒完成。原始命令、完整日志、退出码分别为`jar.command.txt`、`jar.log`、`jar.exit.txt`。未执行test/build/assemble/clean或再次打包。
- 原始输出`build/libs/create_nuclear_industry-0.1.0.jar`直接同字节复制到报告目录[candidate-admin-release-accident.jar](/E:/MyMC/NewMod/Create_NuclearIndustry-accident/build/reports/admin-release-20261020/candidate-admin-release-accident.jar)，3533989 bytes，SHA256 **4B37F69DC6997F5BA0FFFED5F722FF99302B0E458FA38FD1041758EEFE9E3499**；未重封包。
- 身份核验exit0：762个class、1692个resource、1个manifest额外条目，共2455个文件条目与本次编译/资源输出逐项SHA一致。31个被排除输出均匹配现有Gradle规则；无重复条目、无哈希差异、无未解释遗漏；本次Retrofit测试类和`data/cni_reactor_automation_retrofit/`域文件条目均为0，保留2个零长度空目录；原事故测试域继续按原配置排除。
- 机器记录见`ARTIFACT.json`；源输入清单`BUILD-INPUT-HASHES.json`含Git blob和原字节SHA；包条目/输出绑定见`JAR-ENTRY-HASHES.csv`；合法排除见`EXCLUDED-OUTPUT-HASHES.json`；核验工具/日志/退出码为`Verify-Candidate.ps1`、`verify.log`、`verify.exit.txt`。
- 复用阶段A已核JUnit30/30和`review-high-green.log`真实域7/7、exit0及已关闭P2的唯一组合审查，不将此构建记作新测试或客户端验收。原事故接管/闭环/许可及版本配置保持，未移植其它主线增量。
- 打包后Git唯一差异为本树`build/libs/create_nuclear_industry-0.1.0.jar`，按PM要求暂留供核对/恢复；执行者未Git写或从HEAD恢复。旧`pm-formal-before.jar`仍3478572 bytes、SHA256 `81E7CD98D589DDC9C0AAF398C06776DC4318EA54C70CD19376F0C127E4A3DE7B`。未提升到main正式包、未触碰事故用户手测世界或推进热危害；等待PM收件和用户复看。
