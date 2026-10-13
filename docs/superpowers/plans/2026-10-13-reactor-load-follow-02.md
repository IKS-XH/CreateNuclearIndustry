# REACTOR-AUTOMATION-02 应力余量闭环实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task after the manual prerequisite is accepted. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为已有烈焰人管理员新增可设应力余量的PI负载跟随，以正最低HU维持健康空载运行，库存/积热保护仍可完全停堆。

**Architecture:** 仪表仍唯一持有核状态、控制权与短期PI会话；接口只保存机组设置和应力表位置。原生Create应力表产生服务端观测，纯PI生成HU目标，原预测曲线和管理员执行器产生统一棒深；保护先于调节，专用包统一校验世界操作。

**Tech Stack:** MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6；现有Gradle9、JUnit与GameTest，无新增依赖。

**Spec:** [本轮确认设计与工程初值](../specs/2026-10-13-reactor-load-follow-design.md)。用户已要求形成计划，并授权在01手测通过后派发；**当前等待01人工门，尚未派发**。首批源码4757f27及4204ab17…包的历史自动证据、保存挂起限制和未合main状态保持。

## 前置与执行方式

- [ ] 等待用户明确报告[01集中清单](../../reviews/2026-10-13/reactor-automation-01/CLIENT-CHECKLIST.md)手测通过，按实际反馈登记范围；缺失的保存重进、联锁或操作问题先交原执行者窄修，不用本批覆盖01故障。
- [ ] PM核实际已验来源、活动任务、空索引及用户dirty。02必须包含已验01功能，不从仍缺自动化源码的旧main直接起步；如main已净接入01则用该main，否则明确从已验01候选起步。保留01冻结源码/JAR/证据及原分支。
- [ ] 只在开工时确定适合复用的同级树及`codex/reactor-load-follow`分支。新目录采用`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-load-follow`，不得覆盖同名非本任务树。沿using-git-worktrees流程，PM管理Git；执行者不得Git写或再派发。
- [ ] 携带已保存退出的当前01测试世界及实际CNI配置，来源由PM登记。新目标同名世界先完整备份，再整份复制且核对一次；同目录切新分支时世界原位保留并登记，不重复搬动。原世界和main正式包不回写、不共用链接。
- [ ] 沿既定“唯一原执行者＋一次独立组合窄审”。原执行者不可用时仅由PM换一位唯一执行者；四任务为同一人的依赖顺序，不逐小步重开实现者/审查。

此卡已获用户条件派发授权，不因一般内部实现选择重问执行方式。新增玩法、材料、最低功率含义或保护阈值的变更仍须讨论。派发时再次核前置，不能因等待时间到了而推定手测通过；本次不新建工作树、不复制世界、不改代码、不执行构建/测试。

## 全局约束

- 保留01停堆/手动/恒定HU行为；新增`Mode.LOAD_FOLLOW`只能追加，旧枚举数值、旧控制命令不重排。新增阶段`LOAD_WARMUP/LOAD_FOLLOW`，逐棒仍锁定；只有手动运行允许精修。
- 玩家目标余量0～90%、默认20%；正最低HU默认1，需核当前可达范围；死区0.03、平滑40tick、Kp0.25/Ki0.005每秒、预热1200tick。配置单位与整定权限见设计第5节，不硬编码覆盖原15参数。
- 正常调节每10tick、提出最多1/插入最多5个百分点，保护每tick。库存是保护而非50%库存目标；不测外部流量，不动P1热工、端口、泵、锅炉、汽轮机效率或30%门槛。
- 四个燃烧室控件槽不增加；应力模式右上余量、左下最低HU。保留燃料/机械臂/原扳手路径，不新增工具、配方、PNG、管理员动画、独立GUI、Ponder或显示链接。
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

允许修改既有：`config/ReactorAutomationConfig.java`；`automation/ReactorAutomationState.java`、`ReactorAutomationPortBlockEntity.java`、`ReactorAutomationController.java`、`ReactorAutomationService.java`、`ReactorAutomationBehaviours.java`、`ReactorAutomationNetwork.java`、`ReactorAutomationGoggles.java`、`automation/client/ReactorAutomationClientEvents.java`；`blockentity/ReactorInstrumentPortBlockEntity.java`。只增本模式、会话/设置、控件映射和原生命周期撤销，不重构其他仪表职责；旧Slider/SCRAM源只读，确需修改先报告精确缺口扩卡。

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

## 任务3：模式、四控件和库存优先执行

**Files:** State、PortBE、Controller、Service、Behaviours、Goggles、ClientEvents/Network、Instrument及Session；Lifecycle/ControlsTest，两语言。

**Interfaces:** `Mode.LOAD_FOLLOW`追加到原三模式；`Phase.LOAD_WARMUP/LOAD_FOLLOW`追加。PortBE扩Settings为原mode/depth/targetHeat加`reservePercent/minimumHeatHuPerTick`，另保存可空Link；缺设置使用设计初值，运行权永不读盘恢复。命令保留MODE/DEPTH/HEAT/RESET原序，追加RESERVE/MINIMUM_HEAT；绑定用独立协议。四UI槽显式选择当前命令，不能遍历扩长后的Command.values生成六个控件。

- [ ] RED：最小功率设定不能被旧HEAT/DEPTH包覆盖；切模式中正在预览的板拒提交；原恒功率目标0仍停堆，新模式空载在最小正档保持；热液超过95%时即使最低功率高也全插入并故障保持。
- [ ] 原服务tick接入Session：先核当前管理员/表实例和库存/积热/独立急停；红石ARMING成功后应力模式进LOAD_WARMUP，有效容量初始化PI，零容量只逐步到最低HU、1200tick超时故障；曾进闭环后容量丢失不重新循环启动。
- [ ] 全堆统一目标经仪表原原子提交，保留1/5限速和下tick可承载验证；安全预警可越过最低功率，FAULT/STOP幂等保持100%。每次step后用所选曲线HU和限速/保护标记调用trackApplied，不能把未执行请求当成实际输出。
- [ ] 保留四槽及原整数HU局部板。应力模式右上余量%、左下最低HU，模式改变时同步新label/formatter/board/命令戳；旧3、4命令上限等实际硬编码统一适配，不改通用Create设值包无写权规则。护目镜显示实际/目标余量、最低/实际HU和限制原因，缺信号显示等待/故障，不显示100%；沿首行图标留白。
- [ ] 故障、表/管理员撤销、网络变更、退出该模式、卸载及同版本读盘清会话与积分。原手动、恒功率、补料、比较器、安全许可和复位保持；原锁不由客户端信号决定。
- [ ] Run：`.\gradlew.bat test --tests '*ReactorLoadFollowLifecycleTest' --tests '*ReactorLoadFollowControlsTest' --tests '*ReactorAutomationBlazeTest' --tests '*ControlRodSliderProtocolTest' --tests '*ReactorScramFissionRegressionTest' --console=plain`。Expected：模式/面板/保护及真正受影响01入口保持；任务1/2仅相关源码改变时复跑，不机械重复。

## 任务4：真实代表、冻结与交付

**Files:** 本域GameTests/空模板、唯一IMPLEMENTATION、build/reports证据；build.gradle只排除本批测试class与模板。

- [ ] 建立四代表：`binding_and_controls`（原生应力表、真玩家服务器选择/绑表与四槽命令映射）；`real_network_response`（真实Create容量/负载、过载停转与调棒方向）；`blocked_return`（真实反应堆库存/积热和堵塞保护覆盖最低HU）；`lifecycle_isolation`（表/管理员拆换、网络登记冲突、旧请求拒绝与现版本BE读写）。真实表/网络入口不等于锅炉—汽轮机整条热力反馈已稳定，说明夹具边界。
- [ ] 执行`.\gradlew.bat runGameTestServer -PgameTestNamespace=cni_reactor_load_follow -PgameTestDirectory=build/reports/reactor-load-follow-02/server-probe --console=plain`；使用`@PrefixGameTestTemplate(false)`及本域`load_follow_empty`。Expected：仅本域4场景通过且自然保存exit0；只业务通过后挂起时分列限制，保留原日志/线程，不伪称成功或转跑全部旧域。
- [ ] 稳定后一次必要`.\gradlew.bat assemble --console=plain`，冻结实际来源/class/资源/JAR/默认及实际配置，对比已验01包。排除`ReactorLoadFollowGameTests*.class`与本域空模板，旧排除规则保持；不捆绑fixture/报告/第三方JAR。
- [ ] 执行者交IMPLEMENTATION：精确写集、RED/GREEN/退出码、绑定/信号原生入口、初始/最终PI增益、时滞模型边界、原包/原世界保留及未做GPU/完整热力手测，停止写入，不Git提交或自行派审查。
- [ ] PM派唯一一次组合规格/质量窄审。必改交原执行者，仅实际差异定向验证、生产修复才必要重包，初次失败和旧包保留。通过后PM净提交候选、写CANDIDATE及本批手测清单，未手测不合main或替换主包。
- [ ] 本批集中人工门：真实机组选择应力表；余量20%与最低HU可设；空载保留功率；负载突增/撤除后趋稳、死区不频繁调棒；最低HU足以越过所接汽轮机启动门槛；堵热液后保护停堆且恢复不自启；表/管理员失联和现版本保存重进；四槽及图标可读。不要求再手搭16³，但不以纯时滞模型替代此门。

## PM计划自查与当前状态

设计1/4/5对应任务1/3，设计2/3对应任务2/3，最低HU/库存优先/零容量及所有Review Focus均有具体断言或人工边界。新增与原接口职责、参数名、单位及四槽映射一致；没有把01未通过人工项登记通过，也没有在本计划中加入流量测量、库存设定值、D项或锅炉控制。

**当前：计划形成，等待01手测通过后按本卡派发。** 本次只保存设计、实施卡和文档索引，尚无实现、测试、构建、工作树、存档或实例配置操作。原01保存挂起限制及其他独立人工门保持。
