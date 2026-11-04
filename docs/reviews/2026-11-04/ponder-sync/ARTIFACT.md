# PONDER-SYNC-01 候选制品

2026-11-04；准确源提交 `78f84c398add24cae45098422aee4d2baff86b4d`，唯一组合窄审已通过。本树 `codex/ponder-sync-20261104`，仅交候选，未合main/发布，未替代客户端集中播放人工门。

- JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，**3745990 bytes**，SHA256 **`64D8A4FCFA2608E1A8A29509EA25B1802A20FAE5CF032D13A9575220505DCA8A`**。
- 构建：一次增量 `assemble --console=plain`（只附加本报告目录的只读输入观察init脚本），exit0，实测 **8.871秒**，Gradle显示8秒。compileJava/processResources均UP-TO-DATE，仅jar执行；没有test、clean或rerun。原始记录见[assemble.log](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/assemble.log)、[assemble-result.json](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/assemble-result.json)。
- C3原包已完整保留在[baseline-C3A2C178-create_nuclear_industry-0.1.0.jar](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/baseline-C3A2C178-create_nuclear_industry-0.1.0.jar)，3714927 bytes，SHA256仍为`C3A2C178CEDEBD05436DB3E1F3244CF361752AF7B8945C2A1AF154C82057D5C3`。

25条冻结写集构建前后均与源清单SHA256一致。只读Gradle观察器在任务执行前记录实际source roots、主源/编译/资源/归档输入：mainSource2255、compileInputs552、processResourcesInputs1792、jarInputs2609、构建定义7条，合并去重 **4960个实际输入文件**；构建后逐文件大小/哈希及主源文件集合全部未变。见[gradle-production-inputs-before.json](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/gradle-production-inputs-before.json)、[artifact-before.json](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/artifact-before.json)、[artifact-inputs.init.gradle](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/artifact-inputs.init.gradle)。观察器只读取任务输入，不改变构建任务或源。

完整JAR **2609条文件载荷**全部与当前输出绑定：825个class、1780个处理后资源、4项清单/许可；没有重复条目或未绑定载荷。详细逐项哈希/输出路径见[artifact-payload-binding.json](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/artifact-payload-binding.json)。5新增NBT、3新增scene类、P1PonderPlugin及双语各187个Ponder键均在包内。

相对C3原包，解压内容仅 **新增8、修改10、删除0**：新增为5个教学NBT和3个教学类，修改为7个既有Ponder类、双语、锅炉出汽NBT；变化严格落在本批Ponder写集。所有生产机器、配置、美术模型和其它原载荷字节保持。两个事故测试域`data/cni_reactor_external_explosion`/`data/cni_boiler_safety`及对应`ReactorExternalExplosionGameTests*`/`BoilerSafetyGameTests*`均未入包，新JUnit合同也未入包。完整变化与检查结果见[ARTIFACT.json](ARTIFACT.json)、[artifact-verification.log](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/artifact-verification.log)。

构建后Git状态仅正式JAR路径有预期修改；未执行Git写入、未复制main、未操作世界或核心文档。静态/合同证据沿用[EXECUTION.md](EXECUTION.md)，未重跑业务测试。报告分类曾因Windows分隔符前缀断言失败，已在报告工具中纠正，原错误说明见[artifact-reporting-error.log](/E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/reports/ponder-sync-20261104/artifact-reporting-error.log)，无JAR/源影响或再次构建。候选构建与制品身份已交付，客户端播放仍待用户反馈。
