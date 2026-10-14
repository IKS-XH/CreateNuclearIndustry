# REACTOR-AUTOMATION-02 应力余量闭环实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task after the manual prerequisite is accepted. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为已有烈焰人管理员新增可设应力余量的PI负载跟随，以正最低HU维持健康空载运行，库存/积热保护仍可完全停堆。

**Architecture:** 仪表仍唯一持有核状态、控制权与短期PI会话；接口只保存机组设置和应力表位置。原生Create应力表产生服务端观测，纯PI生成HU目标，原预测曲线和管理员执行器产生统一棒深；保护先于调节，专用包统一校验世界操作。

**Tech Stack:** MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6；现有Gradle9、JUnit与GameTest，无新增依赖。

**Spec:** [本轮确认设计与工程初值](../specs/2026-10-13-reactor-load-follow-design.md)。用户已授权手测通过后派发，并于2026-10-14明确保存重进及管理员其余项目全部通过，见[联合验收](../../reviews/2026-10-14/reactor-administrator-accepted-integration/ACCEPTANCE.md)。**条件授权前置已过，先完成06已验来源集成再派唯一执行者**；下列旧四控件与读档停机合同已准确映射到当前03 GUI和04续接，不重复问参数或执行方式。

## 前置与执行方式

- [x] 用户明确保存重进及管理员其余项目全过，03集中清单与04局部清单承接的01/01R门关闭；不以本批覆盖旧故障或追认弃用四控件。
- [x] PM核实际已验来源、活动任务、空索引及用户dirty。02必须包含已验01功能，不从仍缺自动化源码的旧main直接起步；如main已净接入01则用该main，否则明确从已验01候选起步。保留01冻结源码/JAR/证据及原分支。
- [x] 只在开工时确定适合复用的同级树及`codex/reactor-load-follow`分支。新目录采用`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-load-follow`，不得覆盖同名非本任务树。沿using-git-worktrees流程，PM管理Git；执行者不得Git写或再派发。
- [x] 携带已保存退出的当前01测试世界及实际CNI配置，来源由PM登记。新目标同名世界先完整备份，再整份复制且核对一次；同目录切新分支时世界原位保留并登记，不重复搬动。原世界和main正式包不回写、不共用链接。
- [x] 沿既定“唯一原执行者＋一次独立组合窄审”。原执行者不可用时仅由PM换一位唯一执行者；四任务为同一人的依赖顺序，不逐小步重开实现者/审查。

此卡已获用户条件派发授权，不因GUI技术映射或一般内部实现选择重问执行方式。新增玩法、材料、最低功率含义或保护阈值变更仍须讨论。派发依据真实联合验收；只有实际确定02基线/新树/世界来源并完成本卡读取后才开始源码，不把本次修卡写成已实现或已整定。

## 全局约束

- 保留03/04已验停堆/手动/恒定HU、部分燃料耗尽不停堆、核热值班和正常保存原状态续接；新增`Mode.LOAD_FOLLOW`只能追加，旧枚举与命令不重排。新增`LOAD_WARMUP/LOAD_FOLLOW`，逐棒仍锁定；只有手动运行允许精修。
- 玩家目标余量0～90%、默认20%；出厂最低毛HU按已批准维护成本＋1（当前默认1＋1＝2），实际配置/可达范围仍校验；死区0.03、平滑40tick、Kp0.25/Ki0.005每秒、预热1200tick。最低值是毛HU，不将SU或净HU直接当该设置；增益允许按卡内实际时滞模型整定，其他已批含义不改。
- 正常调节每10tick、提出最多1/插入最多5个百分点，保护每tick。库存是保护而非50%库存目标；不测外部流量，不动P1热工、端口、泵、锅炉、汽轮机效率或30%门槛。
- 复用当前三页管理员GUI，不复活四个世界控件或另开页面。总览模式按钮追加应力余量模式，该模式原数值行的两个输入分别显示目标余量%和最低毛HU/t；保留其他模式草稿、焦点/版本与固定停堆。堆芯仍只读，保护页保持原语义。保留燃料/机械臂/原扳手，不新增工具、配方、PNG、动画、Ponder或显示链接。
- 应力表单源、整网SU不重复汇总双轴；同维度、只查已加载实例、不强载。过载的实际转速0不能抹除有效容量/负载；容量0预热、已运行后容量丢失故障，不能除零或虚报100%余量。
- 原安全许可15/0、仪表红石SCRAM、维修解卡后的安全基准、旧会话隔离、故障人工复位保持。最低功率不妨碍全插棒保护，不能清余热、虚造冷液或操纵卡死棒。
- 所有新增/修改手写注释和Javadoc中文；说明HU/SU/秒/tick/百分点、两侧及状态寿命。无旧存档兼容研究；同版本正常保存与加载按实际写集检查。

## 文件结构和精确权限

下列生产路径相对`src/main/java/com/iksxh/create_nuclear_industry/`，测试相对`src/test/java/com/iksxh/create_nuclear_industry/`。

| 职责 | 新建路径 |
| --- | --- |
| 纯配置快照 | `automation/ReactorLoadFollowParameters.java` |
| 持久化应力表位置与原始同龄样本 | `automation/ReactorStressGaugeLink.java`、`automation/ReactorStressSample.java` |
| 纯PI算法、饱和处理及最低正档选择 | `automation/ReactorStressPiController.java` |
| 仪表拥有的暂态会话与滤波/预热 | `automation/ReactorLoadFollowSession.java` |
| 服务端选择/绑定、采样和同网唯一持权 | `automation/ReactorStressGaugeService.java` |
| 本批纯行为测试 | `automation/ReactorStressPiControllerTest.java`、`ReactorStressGaugeBindingTest.java`、`ReactorLoadFollowLifecycleTest.java`、`ReactorLoadFollowControlsTest.java` |
| 真实代表 | `gametest/ReactorLoadFollowGameTests.java`，域`cni_reactor_load_follow` |

允许修改既有：`config/ReactorAutomationConfig.java`；`automation/ReactorAutomationState.java`、`ReactorAutomationPortBlockEntity.java`、`ReactorAutomationController.java`、`ReactorAutomationService.java`、`ReactorAutomationBehaviours.java`、`ReactorAutomationNetwork.java`、`ReactorAutomationGoggles.java`、`ReactorAutomationResume.java`、`ReactorAutomationBlazeBridge.java`、`ReactorAutomationHeatSupply.java`、`automation/client/ReactorAutomationClientEvents.java`、`automation/client/ReactorAdministratorScreen.java`、`automation/client/ReactorAdministratorLayout.java`、`automation/gui/ReactorAdministratorCommands.java`、`ReactorAdministratorMenu.java`、`ReactorAdministratorProjection.java`；`blockentity/ReactorInstrumentPortBlockEntity.java`。只增本模式、会话/设置、GUI原子草稿/显示及本模式有限续接；不重构其他仪表、核热支付或原布局职责。旧Slider/SCRAM与生命周期源只读，实际缺口先报告扩卡。

资源仅`src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`、`en_us.json`，及新增`src/main/resources/data/cni_reactor_load_follow/structure/load_follow_empty.nbt`；模板沿01本域空模板格式，不修改原件。`build.gradle`仅增本类及本模板JAR排除，开发运行classpath保持。旧测试若枚举穷尽性实际编译失败，只能经PM精确扩卡补映射，不能删除原断言。

执行者报告仅`docs/reviews/2026-10-13/reactor-load-follow-02/IMPLEMENTATION.md`，原始日志/夹具/来源冻结只在`build/reports/reactor-load-follow-02/**`。其他文档、Git、验收和CANDIDATE/REVIEW/CLIENT-CHECKLIST归PM。报告目录在执行交付时创建，不为本次计划虚造报告。

## Review Focus

1. 空载r=100%与低于最小正功率档：健康运行不能最终100%全插棒；任务1/3用实际非线性曲线及最低档断言。
2. 容量0、过载停转及汽轮机尚未达到门槛：分别预热/有效负余量，不能混成无限提棒；任务1/2/4覆盖。
3. 长热力时滞、负载突增/撤除、执行器限速：积分不得在受限方向继续积累，死区稳定；任务1时滞纯模型和任务4真实机组人工门分开。
4. 应力表同坐标换实例、动力网重建/合并、两人改绑：旧控件请求无权、同网不双重调节；任务2/3权限、租约和原实例入口测试。
5. 热液堵塞与最低功率相冲突：原库存/逐列保护可覆盖下限并保持停堆，恢复不自启；任务3/4覆盖，原01联锁证据不重开。

## 任务1：纯PI与最低正功率档

**Files:** Parameters、Sample、PiController及`ReactorStressPiControllerTest`；原Config增加独立loadFollow快照方法，原`parameters()`不变。

**Interfaces:**

- `ReactorLoadFollowParameters`为设计第5节七参数的不可变快照；`ReactorAutomationConfig.loadFollowParameters(): ReactorLoadFollowParameters`一次取值校验。
- `ReactorStressSample(Status status,double capacitySu,double loadSu,Long networkId,long sampleTick)`；Status为`VALID/NO_CAPACITY/UNAVAILABLE`；`reserveFraction(): OptionalDouble`仅VALID计算。原始L/C非有限或负数为UNAVAILABLE，不裁剪坏数伪造健康。
- `ReactorStressPiController.Settings(int reservePercent,int minimumHeatHuPerTick)`；`State(double integralHuPerTick,double filteredReserve,double targetHeatHuPerTick,long previousTick,boolean initialized,boolean actuatorLimited)`仅暂态。
- `step(State previous,ReactorStressSample sample,Settings settings,ReactorLoadFollowParameters p,double attainableMaxHu,double actualHeatHu,boolean inventoryLimited,long gameTick): Decision`返回nextState/targetHeat/reason；`trackApplied(State state,double appliedHeatHu,boolean constrained): State`跟踪上次实际可执行HU。无世界访问。
- `chooseOperatingDepth(double[] curve,double targetHu,double minimumHu,double maximumHu): OptionalInt`遵守设计第4节正档规则；无档返回empty，调用者故障保持，旧`chooseDepth`保持恒功率含义。

- [ ] 写行为RED：先以原`chooseDepth`做最小正档探针，曲线最小正档7HU、目标1HU时会选100%，证明它不满足新模式的最低功率要求；实现后同用例消费新`chooseOperatingDepth`，原恒功率选择器及其断言保持。`idleRetainsMinimumPositivePower`约束空载多周期仍取7HU而非100%；另写`reserveDefinitionAndOverload`（C10000/L8000得0.2，L12000得-0.2）、`zeroCapacityIsNotFreeReserve`和`noOperatingBandRejectsStartup`。新类尚不存在造成的编译缺符号不计行为RED。
- [ ] 写PI断言：Hmax1000、e0.1、Kp0.25、Ki0.005、dt0.5时比例项25HU、积分增量0.25HU；死区内保持上次目标；最低/最高/限速/库存限幅时冻结同向积分，反向可恢复；非有限、反向tick不产生输出。
- [ ] 最小实现配置/算法。初始化跟踪实际HU，倍率基于真实Hmax而非SU；新增配置不使01构造或原15默认改变。纯测试显式传入参数，不依赖全局配置文件。
- [ ] 用受控阶跃/时滞模型覆盖0、5、20秒延迟、负载增加/撤除及最低档；记录调节方向、饱和/恢复、反复换向和稳态死区。必要整定只调Kp/Ki，模型和期望不为了GREEN改成零延迟或弱断言；模型不证明真实机组全工况稳定。
- [ ] Run：`.\gradlew.bat test --tests '*ReactorStressPiControllerTest' --tests '*ReactorAutomationControllerTest' --console=plain`。Expected：新行为与原控制回归通过，记录RED/GREEN及最终工程增益；不打中间包。

## 任务2：应力表绑定与服务端采样

**Files:** Link、GaugeService、Session采样部分、Network/ClientEvents、PortBE持久化，以及BindingTest。

**Interfaces:**

- `ReactorStressGaugeLink(String dimensionId,BlockPos gaugePos)`只保存位置，`gaugePos`不可变。
- `ReactorStressGaugeService.select(ServerPlayer player,BlockPos gaugePos,long sequence): LinkResult`；`bind(ServerPlayer player,ReactorAutomationService.Request stamp): LinkResult`消费该玩家服务器选择，不消费客户端任意远程目标；`sample(ReactorInstrumentPortBlockEntity owner,long gameTick): ReactorStressSample`核当前实例/已加载世界；`release(owner)`撤销暂态网络登记。
- `claimNetwork(owner,sample): boolean`按同维度当前networkId及当前所有者代际登记唯一闭环权，不登记容量0的虚构网络，不强载；失效owner清登记。同网冲突拒启动，运行中身份变化交Session安全撤销。
- Session含当前表实例、动力网身份、预热年龄及任务1State，由仪表创建/清理；不保存NBT，不设全局“当前世界”。

- [ ] RED覆盖过期选择、异维度/旁观者/不可达两次操作、错误类型、旧代际/旧epoch、两玩家选择隔离；测试真实现有入口，不能只检查源码文字。
- [ ] 实现Ctrl+扳手两次绑定及服务器选择寿命；未按组合键不发送/拦截本协议。包只表达选择或绑定意图，实际持物/距离/类型/控制权在服务器验证。停堆态改绑成功才推进epoch，旧回包不能覆盖新视图。
- [ ] 采样原生stress/capacity/network，只读已加载实例，不调用强载或创建网络入口。测试过载`getSpeed()==0`但C/L有效、C0不除零、表移除/同坐标重放不继承原会话、同网第二管理员拒持权、网络身份变化释放登记。
- [ ] Run：`.\gradlew.bat test --tests '*ReactorStressGaugeBindingTest' --tests '*ReactorAutomationAuthorityTest' --console=plain`。Expected：上述权限/身份规则及原管理员权限保持；未修改的旧域证据复用。

## 任务3：模式、现有GUI与库存优先执行

**Files:** State、PortBE、Controller、Service、Behaviours、Goggles、ClientEvents/Network、Instrument及Session；Lifecycle/ControlsTest，两语言。

**Interfaces:** `Mode.LOAD_FOLLOW`和`Phase.LOAD_WARMUP/LOAD_FOLLOW`只追加。PortBE扩Settings为原mode/depth/targetHeat加`reservePercent/minimumHeatHuPerTick`，另保存可空Link。新GUI完整Draft追加两字段，现有APPLY/START/STOP/RESET动作和会话/epoch/revision/sequence仍为唯一权威；APPLY一次校验全部再提交，不借旧DEPTH/HEAT写最低功率。绑定独立意图协议；原弃用世界控件不得复活。正常读档只保运行意图与棒位，经04新身份/实际核费核验继续，旧许可/网络权/热票据永不复活。

- [ ] RED：最小功率设定不能被旧HEAT/DEPTH包覆盖；切模式中正在预览的板拒提交；原恒功率目标0仍停堆，新模式空载在最小正档保持；热液超过95%时即使最低功率高也全插入并故障保持。
- [ ] 原服务tick接入Session：先核当前管理员/表实例和库存/积热/独立急停；红石ARMING成功后应力模式进LOAD_WARMUP，有效容量初始化PI，零容量只逐步到最低HU、1200tick超时故障；曾进闭环后容量丢失不重新循环启动。
- [ ] 全堆统一目标经仪表原原子提交，保留1/5限速和下tick可承载验证；安全预警可越过最低功率，FAULT/STOP幂等保持100%。每次step后用所选曲线HU和限速/保护标记调用trackApplied，不能把未执行请求当成实际输出。
- [ ] 将目标余量/最低毛HU接到总览既有数值行，模式切换保其他模式草稿，原子APPLY与多人旧版本/旧菜单拒绝保持；停堆按钮在三页始终可见。显示绑定表位置、实际/目标余量、最低/实际毛HU与限制原因，缺样本显示等待/故障、不造100%；沿原首行图标留白。旧硬编码只适配本模式，不改Create通用设值权威。
- [ ] 故障、表/管理员撤销、网络变更、退出该模式或真实失联清暂态PI/滤波与网络登记。正常保存重进沿04保模式/目标/实际与目标棒位，当前表/网络重新核验后以真实当前HU与新样本初始化PI，积分不读盘；加载仅有限等待，不强载、不先全插/重新START，不用新预热循环掩盖运行中失容量。容量建立与核热交接若有实际循环依赖，先报告最小调用链再定路径，不猜测免费许可或旧样本兜底。原手动/恒功率续接与核热事务不退化。
- [ ] Run：`.\gradlew.bat test --tests '*ReactorLoadFollowLifecycleTest' --tests '*ReactorLoadFollowControlsTest' --tests '*ReactorAutomationBlazeTest' --tests '*ControlRodSliderProtocolTest' --tests '*ReactorScramFissionRegressionTest' --console=plain`。Expected：模式/面板/保护及真正受影响01入口保持；任务1/2仅相关源码改变时复跑，不机械重复。

## 任务4：真实代表、冻结与交付

**Files:** 本域GameTests/空模板、唯一IMPLEMENTATION、build/reports证据；build.gradle只排除本批测试class与模板。

- [ ] 建立四代表：`binding_and_controls`（原生应力表、真玩家Ctrl两次选择/绑表与GUI原子草稿）；`real_network_response`（真实Create容量/负载、过载停转与调棒方向）；`blocked_return`（真实库存/积热保护覆盖最低HU）；`lifecycle_isolation`（表/管理员拆换、网络冲突、旧请求与同版本正常续接且积分重新初始化）。原11续接代表在未改范围复用，不机械扩大；真实表/网络不冒称锅炉—汽轮机完整闭环稳定。
- [ ] 执行`.\gradlew.bat runGameTestServer -PgameTestNamespace=cni_reactor_load_follow -PgameTestDirectory=build/reports/reactor-load-follow-02/server-probe --console=plain`；使用`@PrefixGameTestTemplate(false)`及本域`load_follow_empty`。Expected：仅本域4场景通过且自然保存exit0；只业务通过后挂起时分列限制，保留原日志/线程，不伪称成功或转跑全部旧域。
- [ ] 稳定后一次必要`.\gradlew.bat assemble --console=plain`，冻结实际来源/class/资源/JAR/默认及实际配置，对比已验01包。排除`ReactorLoadFollowGameTests*.class`与本域空模板，旧排除规则保持；不捆绑fixture/报告/第三方JAR。
- [ ] 执行者交IMPLEMENTATION：精确写集、RED/GREEN/退出码、绑定/信号原生入口、初始/最终PI增益、时滞模型边界、原包/原世界保留及未做GPU/完整热力手测，停止写入，不Git提交或自行派审查。
- [ ] PM派唯一一次组合规格/质量窄审。必改交原执行者，仅实际差异定向验证、生产修复才必要重包，初次失败和旧包保留。通过后PM净提交候选、写CANDIDATE及本批手测清单，未手测不合main或替换主包。
- [ ] 本批集中人工门：Ctrl绑定真实应力表；现GUI余量20%与最低毛HU可设；空载保留功率；负载增减后趋稳/死区不频繁调棒；最低HU按实际汽轮机门槛设定；堵热液保护且恢复不自启；表/管理员失联、正常重进原状态续接及新PI重新初始化；三页GUI和图标可读。不重手搭16³，不以纯时滞模型替代此门。

## PM计划自查与当前状态

设计1/4/5对应任务1/3，设计2/3对应任务2/3，最低HU/库存优先/零容量及Review Focus都有实际断言或人工边界。新增与当前GUI/原权威入口、单位和04续接相符；前置按用户真实联合反馈关闭，不造新现场数值。本批不加入流量测量、库存设定值、D项或锅炉控制。

**当前：06精确集成及主包交付完成，02同级工作树codex/reactor-load-follow已从main d9f415a建立；当前已保存世界38文件及运行配置27文件完整携带且哈希一致，详见[开工交接](../../reviews/2026-10-13/reactor-load-follow-02/DISPATCH.md)。前置及条件授权已过，交原唯一执行者按本卡实施。** 开工记录不宣称PI已实现、整定或手测通过；新增人工门待交付另验。
