# EXT-B-BOILER-REWORK-ACCEPTANCE-01：锅炉重构验收与主目录整合

**状态：** 用户于2026-10-08明确确认上一轮01F精简清单“手动测试通过了”。PM已将最终候选`e721797`无冲突合入main，整合提交`611d3ca7ad0ba29475dd9a0a3fd4dfb3a83a892a`；唯一一次主目录增量打包和24项封包核对通过，文档收尾已完成。此验收关闭REWORK-01～01F现行锅炉集中人工门，不自动开启其他主线或设备思索，不宣布首发可发布。

**前置与证据复用：** 01F功能快照`fcb4b6d`及最终候选`e721797`、既有独立审查和25项账本/11个不同真实用例（07十项＋08单项）通过证据不变；07整轮失败与早期夹具失败仍保留。PM核对本次65个源码/资源/美术源文件与已验收候选一致，原两台思索、主目录README、启动配置、构建依赖和既有文档未因合入改变。按治理5.1不重跑JUnit、GameTest或全量build。

**执行技能：** 开工实际读取AGENTS、本卡、01F计划及`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`；按实际Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6、JEI19.27.0.340、mod0.1.0使用。治理5.1优先于技能通用重复测试、clean、客户端与Git步骤。

## 唯一执行者任务

- 目录：`E:/MyMC/NewMod/Create_NuclearIndustry`，分支main，基准`611d3ca`。同级候选日志和__pycache__保持原样。
- 只执行一次`$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'; .\gradlew.bat assemble --console=plain`。完整日志、退出码、JAR快照及静态封包核对写入`build/reports/extension/EXT-B-BOILER-REWORK-ACCEPTANCE-01/`。
- 不运行test、GameTest、build、clean、runClient、生成器；不改生产/测试/构建/模拟器/配置/世界，不Git写、不派代理。
- 允许文档写集仅`docs/reviews/2026-10-08/boiler-rework-01/integration.md`。允许构建生成目录和上述证据目录；不得改其他docs、AGENTS、任务状态或治理。
- 使用本批01F `pm-artifact-validation.json`中的24条已知路径，只读比较main新JAR条目与实际编译class/源资源，记录全部比较是否一致；不是重新做旧包封包审查。不复跑无关素材/模板矩阵。
- 保存新JAR大小、SHA-256及本批快照。不要覆盖01F/01E历史制品。新包编译时间不同不要求与旧候选整包哈希相同。
- 报告准确列明一次assemble结果、当前制品核对、复用证据来源、用户人工确认及保留边界。遇打包失败报告具体错误，不自行改代码或机械重跑。

## PM完成定义

- [x] 记录用户本轮锅炉集中手测通过，且不扩大为所有场景或发布验收。
- [x] 无冲突合入候选，仅65个已审源码/资源/美术源路径；与候选一致，主目录独有README及启动配置保持。
- [x] 读取执行者一次增量打包与封包报告；退出码0、8秒，24/24条目一致，新包与快照大小及SHA相同。
- [x] 更新AGENTS、活动任务、文档入口/配置/路线图及验收记录，纳入本轮中文提交；同级候选保留，不自动接新任务。