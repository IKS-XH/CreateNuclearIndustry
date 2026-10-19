# OPT-INTEGRATION-20261020：最新优化累计候选合入

- 需求来源：用户2026-10-20“把最新的优化分支合并进主分支里”。本卡只执行既有候选集成，不新增优化或玩法。
- 基准：main `60c484803149fa3a3317ec5521664259a7f570d0`；隔离树 `E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration`，分支 `codex/latest-optimization-accepted`。
- 来源：`E:/MyMC/NewMod/Create_NuclearIndustry-thermal-performance-diagnosis-20261017/H1-heat-line-20261020`，已核实精确继承链为main正式0763→T2→T3→T4→T5→T7→T8→T9→H1，不含T6或B1。须按相对main的净增量核对，不整树覆盖。事故第二批、美术候选、辅助工具不借此合入。
- 保护：main原48项tracked状态已登记。IndustrialFluidConfig/IndustrialFlowBudget原始字节均与HEAD完全一致、LF，本次只接入授权源增量并保留LF；正式JAR原先缺失，仅在已提交来源核验后替换；其余45项逐项原始哈希保持，不把全部dirty称作换行差异。原存档27文件/13337721 bytes已完整携带、0差异，目标原世界已整体备份。来源用户包、世界、原始失败证据不修改。
- 唯一执行者：`/root/optimization_integration`。第一阶段只读盘点并回报精确净写集；PM确认该清单后方可复制或处理实现冲突。执行者无Git、核心文档、状态或派发权限。
- 验证：同一实现复用T2～T9/H1原定向、真实服务端及受控性能证据，明确已知基线失败；仅在集成改变逻辑、发现新失败或无法绑定原实现时补受影响测试。源码提交后一次增量`assemble`，核对生产输入、全部class/资源与候选差异及正式JAR身份。不运行候选setup/世界重置脚本，不重跑整线性能。
- 审查：一次规格与质量组合窄审，读取累计差异、原报告及本次构建绑定；不重复业务测试。发现代码问题交唯一执行者处理。
- 必读：AGENTS.md、治理5.1、STATUS、候选各批PLAN/REPORT、实际Gradle依赖。
- 技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-ci-release/SKILL.md`；PM另用finishing-a-development-branch、using-git-worktrees、requesting-code-review及verification-before-completion。锁定MC1.21.1/Java21/NF21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，不因原整合包测试环境调整项目版本。
- 报告允许写集：隔离树`build/reports/optimization-accepted-20261020/`；PM维护本卡、STATUS及`docs/reviews/2026-10-20/optimization-accepted/`。
- 完成定义：累计净增量和原证据经过组合窄审；生产源提交；增量构建exit0、制品与来源绑定；main仅合入已审核文件及正式包，45项非本批tracked状态逐项原始哈希保持。来源树/分支和证据保留，不推送、不发布。

## 精确允许写集

2026-10-20只读盘点已核，PM授权执行者从H1/source复制以下28项，保持候选语义和原始LF。不新增相邻优化，不改其它代码或资源。生产路径相对`src/main/java/com/iksxh/create_nuclear_industry/`：

```text
boiler/BoilerStructure.java
config/IndustrialFluidConfig.java
content/TurbineContent.java
heat/HeatExchangerLine.java
heat/NuclearHeatExchangerBlockEntity.java
industrial/IndustrialFlowBudget.java
industrial/IndustrialNetworkGraph.java
industrial/IndustrialNetworkScheduler.java
industrial/IndustrialPipeBlock.java
industrial/IndustrialRoutePlan.java
industrial/IndustrialTopology.java
mixin/TurbinePaletteRevisionMixin.java
turbine/TurbineBlockStateRevision.java
turbine/TurbineControllerBlockEntity.java
turbine/TurbineShaftPowerSource.java
turbine/TurbineStructure.java
```

资源仅`src/main/resources/create_nuclear_industry.mixins.json`。测试路径相对`src/test/java/com/iksxh/create_nuclear_industry/`：

```text
boiler/BoilerStructureAllocationTest.java
industrial/IndustrialAllocationReuseTest.java
industrial/IndustrialDispatchBindingsTest.java
industrial/IndustrialExhaustedBudgetTest.java
industrial/IndustrialGraphBudgetTest.java
industrial/IndustrialGraphTopologyTest.java
industrial/IndustrialPathAllocationTest.java
industrial/IndustrialPumpSkipTest.java
industrial/IndustrialUnlimitedPipeTest.java
turbine/TurbineLiveCacheTest.java
turbine/TurbineMemberIndexTest.java
```

执行者先复制并报告净差异，等待PM提交来源；提交后持有唯一构建进程，一次增量assemble，并在允许报告目录保存输入清单、构建日志、制品/class/资源绑定及必要只读检查工具。窄编候选未带`-g`，若与Gradle包class字节有差异，逐项解释调试属性并核方法字节码/字段/注解/描述符，不擅自重封装或直接把窄编包当作assemble输出。进度和未闭门仅在STATUS记录。

## 原批准夹具随规则同步

组合审查核到T8报告第145行明确要求正式集成同步`VALIDATION.patch`，故补充允许修改`src/main/java/com/iksxh/create_nuclear_industry/gametest/BoilerIndustrialActiveOutputGameTests.java`和`IndustrialFluidNetworkGameTests.java`两项开发夹具：四汽口共享普通主干按4×4096=16384断言并增加接收容量；短收守恒例将原8192限额明确转移到强化阀、原守恒值不变。同步对应中文注释，保持测试名称、其它断言及生产实现不变。这两项实际夹具已有T8/T9同实现真实服证据复用；不为同步源码重启整线或扩大修复已知基线失败。最终总写集30项（18 Java含2项开发夹具、1资源、11新增JUnit）。
