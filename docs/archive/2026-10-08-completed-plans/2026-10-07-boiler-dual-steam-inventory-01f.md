# EXT-B-BOILER-REWORK-01F：双汽库存与共享容量实现计划

> **执行技能：** 使用writing-plans组织合同、subagent-driven-development派发单一核心执行者，minecraft-modding与minecraft-testing核对实际API及定向证据。用户已明确提出并确认库存方式，不重复确认同一方案。项目经理不改代码，执行者不Git写、不改治理或核心文档、不另派代理；治理5.1的按范围验证与集中人工门优先于通用技能中的重复全量流程。

**当前状态（2026-10-08）：** 用户确认本卡精简手测通过；锅炉重构REWORK-01～01F现行人工门关闭并无冲突合入main `611d3ca`。65个源码/资源/美术源路径与已验收候选一致，复用原25项账本/11个不同真实用例及独立审查，仅新增一次main增量assemble和24项封包核对。详见[验收](../../reviews/2026-10-08/boiler-rework-01/ACCEPTANCE.md)。早期失败与旧候选制品保留，不接其他主线/教学。

**目标：** 两种汽各自保存真实数量与已付HU，共用一份几何蒸汽容量；已生成库存不自动换种，汽口按选择抽取对应库存，关闭R1长管短窗口饥饿。

**架构：** 锅炉控制器继续持有唯一账本。普通汽与超临界汽各有mB/HU，`steam()`与`steamHu()`继续表示两种合计；汽口只访问选定种类，不拥有额外库存。共同炉压从两种汽的实际温度贡献计算，不按某个口或其中一罐计算。

**技术栈：** Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6、JEI19.27.0.340、mod0.1.0，均不升级。

**需求依据：** 用户先要求两种汽不再自动切换、共用蒸汽容量、按过滤口输出，随后明确选择“分开记录两种库存，共用总容量；各口只取对应库存”。本卡替代01B/01D/01E中按全炉瞬时汽种重标库存与撤销汽口资格的部分；纯过滤、不降级、不转换外部库存及现有付费热工继续有效。周转缓存建议已撤下，不是本卡实现范围。

**起点与状态：** 候选`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，`codex/ore-acquisition`，起点HEAD`e4c95fe`。R1两项诊断测试及模板未提交，13/14/15日志保留；失败19行生产候选已受控撤回。01F由同一核心执行者顺序实施；不在main写新锅炉代码，不改用户世界或配置文件。最终停在集中人工验收，不接其他主线/教学。

## 已确定的运行合同

1. **唯一共享容量：** `Vnormal + Vsc <= steamCapacity`约束新增产汽；满罐时两种都不能继续增加量，但仍可对库存付费再热。容量继续为有效汽区格数乘`steamCapacityPerCellMb`，不为两种各开一个完整容量。当前几何或配置缩容保留真实库存，只拒收新增，不裁切。
2. **只给新批次定种：** 沿用现有预热、库存再热、汽化顺序及所有温压配置。每次真实生产完成后，用共同炉压和这批实际汽温决定该整批新增汽：同时达到既有`supercriticalPressure`和`supercriticalTemperature`才归SC，否则归普通汽。已有普通汽再热后仍是普通汽，已有SC降压/降温后仍记录为SC，不自动升级或降级。不得借分类重复付热或制造额外量。
3. **逐種热量：** 每种保存自己的真实HU，抽取按该种当时比焓扣账。普通汽达到原汽化焓方可输出；SC达到`h(supercriticalTemperature)`方可输出。SC自然冷却后保留身份/占容量，但热量不足时等待真实再热，不因汽轮机只认ID而白送SC额度。该条件维持既有“已付热才出汽”的合同，不新增数值。
4. **共同炉压与保压：** `P = (Vnormal×Tnormal + Vsc×Tsc实际)/(共享汽容量×Tsc配置)`，无汽项为0。出汽压力下限仍是0～100的公共最低门槛，不是目标炉压。抽某种汽的保压余量必须按该种实际温度计算；不能用合计平均温度扣异温库存而越过下限。已有SC在共同炉压低于50%时可继续排至出汽下限，只要该SC已付热合格；50%门槛用于新批次定种，不再锁住已生成SC。
5. **原生输出与额度：** 每口选择沿用原生蒸汽/超临界选项，默认SC；两种可从各自口同时输出，单口只输出对应真实库存。主动邻罐、原生管路/泵、桶能力如现有入口共用每口`portFlowMbPerTick`额度和实际账本；SIMULATE不改量/HU/额度。每次EXECUTE重算共同保压余量，不能多个口同时重复花掉压力余量。
6. **能力与管路：** 炉压跨SC门槛或另一种库存变化不再撤销当前口的选种句柄；句柄动态读取该种真实库存/已付热/保压余量。端口同tick额度耗尽仅拒绝额外交易，不据此清除尚有库存及保压余量的管路拓扑。真实结构或该口选项改变仍使旧能力失效，仅刷新受影响输出面；冷热液能力不受串扰。空库存仍真实返回EMPTY，Create原生启动/清流/传播范围不改。两种流体的管路应分开，不能把同一原生管网当作混汽库存。
7. **再热、散热和安全阀：** 两种库存实际欠热都计入需求，按各自欠热比例分配真实再热且不改种；原蒸汽散热总预算只结算一次，按各自可散显热比例分配，不因两种而翻倍，散失显热不得吃掉潜热。原安全阀90%开/80%关及每汽格32mB/t继续共用一份泄放额度；按两种库存数量比例分配真实泄放并扣各自比焓，再按该种实际温度限制不越过关阀压力线。有限整数尾量不能造量或重复泄放。红石停热/已付余热行为不改。
8. **保存与遥测：** 当前版本正常保存加载包括两种mB/HU、合计量、几何及同tick额度，不做旧版迁移或兼容研究，不写用户世界。护目镜分别显示两种库存和共享已用/总容量；“当前产汽”只说明实际新批次，不再暗示整罐被重标。热量/压力、图标留白与中文玩家文案保持准确。

## 审查重点

- 两种库存不能被同一个总`Steam`反复重标或在客户端再建一份；容量与总HU各只计一次。
- 异温库存逐口抽取、多个口同tick竞争、SIMULATE与真实接收拒绝，不越过保压线、不重复花额度。
- 已有SC低压仍可取，但热量不足SC不能免费输出；普通汽再热不变SC。
- 原生13管NORMAL支路与3SC共管机组/创造罐自然联动，能力/压力不再追着全炉相位重建；不注入缓存故障或清外部fluid。
- 拆件失效、端口选项变更与当前版本保存恢复，不重结算库存或扰动冷液回路；安全阀和散热预算不翻倍。

## 任务1：同一核心实施与定向验证

**允许写集：**

- `src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerState.java`：双汽mB/HU、总量/温度/共同P、新批次归类、逐种取汽、再热/散热/安全阀、当前保存。
- `boiler/BoilerControllerBlockEntity.java`、`BoilerPortBlockEntity.java`、`BoilerSteamKind.java`、`BoilerSteamPressure.java`：仅双汽能力、原生源生命周期、各口对应库存与遥测的必要接入。必要纯账本辅助类型只可新增于`boiler/`包，职责/单位/不变量用中文说明，不拆无关设备。
- `src/main/java/com/iksxh/create_nuclear_industry/config/BoilerConfig.java`仅修正原字段中文语义，不新增或改变数值；其他配置类不改。
- `src/test/java/com/iksxh/create_nuclear_industry/boiler/BoilerStateTest.java`与必要`BoilerDualSteamInventoryTest.java`；GameTest限`BoilerMultiportStopGameTests.java`、`BoilerSteamSelectionGameTests.java`、`BoilerTurbineFlowGameTests.java`、`BoilerControlsGameTests.java`、`ExtensionBoilerGameTests.java`、`BoilerReviewGameTests.java`、`ExtensionGoggleSyncGameTests.java`。旧夹具只更新本版本双汽seed和明确被替代的跨种断言，不改其他设备行为或测试框架。
- 本批必要独立空模板`src/main/resources/data/create_nuclear_industry_boiler_inventory/structure/inventory_empty.nbt`仅复制已有相同字节，配合现有namespace筛选；R1已有独立模板保留。
- 两种语言JSON仅双汽库存/共同容量/实际产汽、已付热等待的锅炉键；不格式化或夹带其他教学资源。
- 执行者报告仅`docs/reviews/2026-10-07/boiler-rework-01/steam-inventory-01f-design-analysis.md`及`steam-inventory-01f-implementation.md`；证据`build/reports/extension/EXT-B-BOILER-REWORK-01F/`。不修改本卡、AGENTS/治理/核心文档或Git。

**接口合同：** 原`steam()`、`steamHu()`、`steamTemperature()`继续供合计遥测/共压使用；新增按种类的数量、HU、温度、已付热资格、可抽量与实际取汽查询，选择类型使用`BoilerSteamKind`或包内等价纯状态类型。控制器交易必须显式传递该口选择；不得让旧无种类drain为接口偷选一种。方法签名由同一执行者在报告中记录并同步全部本卡调用者。

- [x] 读取AGENTS、01F与既有R1根因记录/实际技能，列清接口与本批生成分类时点；不再尝试失败拓扑候选。
- [x] 先写独立失败合同：两种共同容量不翻倍；普通再热不升级、SC低压身份保持；热不足SC拒取后真实再热恢复；异温按种抽取刚好保压；两个口/同tick模拟与执行守恒。用本版本真实seed，不伪造原生Source或调整阈值。
- [x] 实施唯一账本和原生能力接入，公共入口/热工/事务/生命周期写中文说明；保持实际mB/HU付费和空能力语义。
- [x] 新定向账本用例先红→绿；修改共享热工后合并运行受影响`BoilerStateTest`与新双汽合同一次，不额外跑全工程或其他设备单测。
- [x] 更新R1两项为同样拓扑/热负荷/60→10的固定库存复现：原生NORMAL罐真实成交、SC库存可继续交付、总量/HU守恒，按足容量时段看降压，不断言连续热源下恒定10%；日志分别记录新生产种类与已有两种库存。
- [x] 一个集中GameTest轮包括R1两项、01D独立口/真实双支路与冷液控件实际受影响合同，以及少量本批新混合库存/已付热/保存合同；01C只保留真实持续供热与必要机组联动，不重复17项停转矩阵。通过后仅实际整改引发的新风险才复跑对应项。
- [x] 实现稳定后唯一一次增量`assemble`、保存源码/日志/退出码/JAR大小SHA与本批候选副本。实际自动证据未过不能称修复，不能覆盖01E历史制品。
- [x] 写一份简短交付报告，列改动、实际定向检查、守恒及仍待客户端项；停止在PM一次合并审查，不自行启动客户端、改世界或扩下一项。

命令沿用`JAVA_HOME=C:/Program Files/Java/jdk-21`、现有`gameTestNamespace`/`gameTestDirectory`和`test --tests`筛选；具体集中域列表由核心确认已有实际注册后写入报告，不改Gradle或跑空域。新增SteamEnum/UI依赖不能让纯账本JUnit或独立服务端加载客户端类。

## 任务2：PM一次审查与集中人工门

- [x] PM读取实际差异、完整必要退出结果与原始交易证据；派高速只读代理一次合并规格/质量审查，严禁重跑测试。审查报告只写`docs/reviews/2026-10-07/boiler-rework-01/steam-inventory-01f-review.md`。
- [x] PM更新候选/配置/玩法入口与Git快照，仅暂存本批审核文件，中文提交；生产仍只在同级候选。R1失败候选及报告保留历史含义，周转缓存不实施。
- [x] 用户于2026-10-08确认上一轮精简清单手测通过；原6×6×5现场60→10、两种库存/共同容量、分别接管输出、汽口选择和冷液持续等现行人工门关闭，已按独立验收合入main。未列场景与首发验收不在本确认范围。
