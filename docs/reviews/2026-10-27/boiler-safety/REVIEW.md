# BOILER-SAFETY-01/02 组合窄审

2026-10-27。**当前结论：同轮整改窄复审通过，可提交准确源并制作候选包；R4目的地完整成型交易及集中人工门仍未闭，详见末尾最终结论。** 以下保留初审“需整改”的问题及逐次复审过程。本报告不宣布用户验收，不改代码/测试/Git/核心文档。

## 范围与证据

- 审核树：`E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration`，`codex/boiler-safety-20261026`，基准`d884b2c3`及当前未提交01+02差异、新增五个事故类/测试/模板。实际写集符合两张卡，未发现反应堆物理、工业调度器、版本或素材扩项。
- 已读主工程AGENTS、治理5.1/5.2/6、STATUS、冻结设计及两卡，实际读取本机`C:/Users/IKSXH/.codex/skills/`下minecraft-modding/testing/resource-pack技能。核Gradle锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；没有照搬技能新版本API。
- 读取EXECUTION/SOURCE、TECHNICAL-02、EXECUTION-02/SOURCE-02及原始日志。01定向44/44；02最终五类锅炉51项通过；全JUnit865=851通过/3失败/11跳过、exit1。三项名称仍为两条HeatExchangerBoilerBridgeTest配置未加载及ReactorFissionDifferentialTest全状态差分既有基线，不作为新增缺陷，也不称build通过。
- 最终`native-02d.log`确实启动并完成12/12必需用例，无SKIP，正常All dimensions saved、shutdown、Gradle exit0；初轮空函数/夹具失败和02局部SKIP原记录分别保留。没有自行重跑测试、加载用户世界或构建JAR。
- 已核真实HU路径、双汽身份与实际排汽、阀集合/所有阀投影、512固定额度与关阀线、工业Level Post交付后Server Post LOWEST判定、最终CommonHooks和两个潜行扳手、环境窄抽取与共享额度。以下缺口不被已有12项用例覆盖。

## 必须整改

### R1 / P2：暂时不可执行的环境目标被永久领取

`boiler/BoilerAccidentExecutor.java:63-67`在环境事务前无条件推进`cursor`，再将`removeEnvironmentBlock`的全部0结果当终结收据。该入口的0还包括`accident/MoltenCoriumEffects.java:158-160`的预算不足连带目标数量和两格读取域未加载，不只是最终取消/对象替换。

具体触发：共享预算只余1，下一个喷爆目标为8格装配台；事务因额度不足返回0，执行器却永久跳过它。或干烧目标本格已加载而相邻读取域缺区块，计划可完成却一件也没有损毁。应区分DEFER与永久SKIP/提交成功，仅后两者推进游标；预算/加载恢复后仍执行原项，不重抽目标。

最小验证：余量不足→下一tick足额恢复，以及读取域暂缺→恢复，均守住原游标/原对象与共享上限。原最终保护取消仍须终结且不掉料。

### R2 / P2：旧干烧认领会吞同址新部件，BE和无BE均有问题

`boiler/BoilerAccidentLifecycle.java:80-83`仅按位置与BlockState认领，`heat/NuclearHeatExchangerBlock.java:77-78`及`boiler/BoilerShellBlock.java:14-15`据此直接返回空掉落。

具体触发：干烧提交后共享预算耗尽，原热部件尚待处理；旧控制器世代不变，先拆原目标再放同朝向新换热器/同态再加热段，随后生存挖掘或扳手新件。旧active认领仍命中，吞掉新件完整掉落。对于BE，执行器后来反而会由`Target.matches`发现对象已换而跳过；对于无BE段，`Target.matches`也只核状态，旧计划还可直接毁掉新段。

应把抑制与事故原件/真实移除收据绑定，并在本机替换放置时失效旧目标认领；不能只比较坐标/同态，不能把所有掉落抑制删除。注意原生普通挖掘的getDrops通常在removeBlock之后，不能简单改成查当前世界`matches(level)`；需要原LootContext的BE或明确移除/替换凭证。

最小验证：压满预算→原件领取/拆除→同态BE及无BE新件放置→正常生存采掘/扳手，原事故件无完整掉落，新件恰好一件，旧计划不再处理新件。

### R3 / P2：重载后的目标身份绑定使用易变完整NBT

`boiler/BoilerAccidentLedger.java:38-45`把提交时整份BE NBT冻结，在从计划NBT恢复后以其全等作为身份。待执行控制器的`BoilerState.idle`仍逐tick更新LastTick/散热；撤销归属的换热器也会更新HeatExchanger.LastTick等。正常保存时真实BE数据已经变化，事故计划仍保存提交快照。

因此同一原BE正常tick后保存重进，`Target.matches`也会为false，执行器把它作为被替换对象永久跳过。现有计划NBT往返用例没有把该重载计划再对变化过的原BE执行，不能证明这条恢复路径。

应使用稳定且可持久化的目标身份凭证，把运行状态/库存快照与身份区分；真实替换仍拒绝。最小验证：原BE继续tick使完整NBT变化，计划保存/恢复后仍认原对象并继续执行；同坐标新实例不匹配。只验证当前格式，不研究旧版本迁移。

### R4 / P2：带待执行ID的控制器跨维度放置会永久未成型

`boiler/BoilerControllerBlockEntity.java:426-427`将pending事故ID写入控制器携带物，`:184-185`仅查当前ServerLevel的SavedData，只有找到且完成才清pending。危险拆控制器后其掉落快照会带pending；若带往另一维度放下，当前维度没有原事故ID，pending永不解除，`currentForm`恒为null。

应分开旧现场持久任务与重新落位控制器的本地等待身份，原维度事故继续、库存不复制，新位置不因不存在的本地任务永久锁死。最小验证：正常生存/扳手得到带事故状态的携带控制器，跨维度重放，旧现场任务仍在且新实例可恢复合理状态。不是旧存档兼容需求。

## 一并修正的注释

`config/BoilerConfig.java:39`的`VALVE_OPEN_PRESSURE`注释仍说“开启安全阀及堵塞保护”；本批已删除堵阀自动停热兜底，应去掉不再成立的后半句（P3）。其他新增手写说明均为中文；无需为此单独测试。

## 后续边界

执行者一次处理以上同一整改集，补窄的反例/恢复证据；本审查只复审实际差异与相应结果，不重复整套JUnit/熔融物矩阵。原3项全量基线失败保留。客户端护目镜/粒子/定向效果、当前世界正常保存重进、物理卸载和真人多人尚未由本审查验收；候选人工清单仍按EXECUTION-02集中一次完成。当前没有JAR或制品审核结论。

## 同轮整改窄复审（第一次交接）

已读FIX.md、SOURCE-FIX.md/source-fix.json及实际修订源，复用`native-fix-final.log`和`fix-unit-final.log`/XML，无自行测试。最终五类52/52、0失败/错误/跳过；真实服19个注册场景的已执行核心断言通过，无整场SKIP，正常All dimensions saved/shutdown/exit0。原3项全JUnit失败继续按整改前同源未改范围复用，不重跑全量。

- **R1已闭技术问题**：公共窄事务区分DEFER/REJECTED/REMOVED，实际预算或读取域暂缺时保原游标，原项恢复后重试；最终取消/对象替换才终结。首个块读取前亦有加载守卫。预算不足装配台、实际读取域缺口及恢复均有定向实证。
- **R2已闭技术问题**：掉落用原LootContext BE与稳定凭证，BE/无BE本机实际新放置使旧认领和计划目标失效；新增最终许可监听内换件的调用方身份复核。原件不掉完整设备，新BE/无BE件生存采掘/扳手恰好一件的真实证据已核。
- **R3身份设计已修正，但需完成下述同一处dirty入口修正**：凭证改为BE的持久UUID，不再用完整运行NBT/LIT作身份；原BE正常tick、运行NBT变化后重载计划继续、真正替换拒绝的实证已核。
- **R4永久pending软锁已由源和已执行断言消除**：真实BlockItem重新落位走独立loadPlacedData，旧现场任务留原维度，新实例新世代并解绑已提交pending，未提交进度保留，普通chunk读取不解绑。已核真实生存扳手取出及Nether BlockItem.place耗尽物品、pending=null、新世代、inspect合法、唯一真实热量/工质和旧现场完成。
- **R4完整目的地成型/交易仍未覆盖**：Nether夹具的areEntitiesLoaded一直false，生产current未放宽。日志明确`SUBCASE_UNAVAILABLE actual formed transaction`；19个注册场景核心过不等于此子项过。实际玩家进入目的维度后成型并交易继续在同一次人工门，不能补记自动或用户通过。
- **P3注释已改**：开启压力说明不再声称已取消的堵塞停热保护。

### R3修订引入的无强载缺口（仍属同一整改集）

`BoilerAccidentLedger.Target.bind`给PersistentData写入凭证后调用`be.setChanged()`。已只读核锁定NeoForge的BlockEntity/Level源码：setChanged会调用updateNeighbourForOutputSignal，后者只检查第一邻格是否加载，遇导体继续读取第二邻格但不检查该格加载。计划冻结仅核目标本格，故边缘目标第一邻格为已加载导体、第二邻格在未加载区块时，凭证冻结可能在环境事务的两格读取域预检前就隐式加载区块。

PM已确认只退修此入口：标识数据不改变红石输出，应采用已加载chunk的无邻居通知dirty标记，保留持久化而不引发相邻读。只需相应边缘缺口窄证据；R1/R2/R4既闭技术问题和R4人工边界不重开。本段时点仍**需整改这一处，暂不出可打包结论**。

## 同轮最后R3差异复审及最终结论

已只读核最终`Target.bind`：新增内部UUID之后，仅通过`getChunkNow`取得原已加载区块并`setUnsaved(true)`，不再调用BE.setChanged或输出信号邻居读取；凭证依然随原BE的PersistentData正常保存。与`source-fix.json`比较，`source-final.json`仅这一个生产文件和对应既有GameTest文件有变化，其余本轮已核实现未变。

新增窄实证为`native-fix-read-domain.log`，**1个对应场景实际通过，18场明确SKIP**，正常保存/shutdown/exit0；不能把其19required标记当19次新的业务通过。实际夹具为区块x14箱子、x15实心导体、x16第二邻格未加载，建夹具及冻结身份前后均核未加载，生产执行返回DEFER且保原游标；之后仅测试夹具显式恢复缺口，原目标继续并完成。源码及这条原始证据消除了R3最后的无强载缺口。本人没有运行测试。

**最终技术结论：PASS，原R1～R4及P3已完成本轮整改；没有剩余阻止提交准确生产源、制作候选包的P1/P2。** 可以按既定PM/唯一执行者分工进入源提交及候选制品核验，不表示可以跳过用户手测或直接合main。

验证沿未变实现复用19场核心证据和五类52/52单元结果；全量865=851pass/3既有fail/11skip仍为整改前那一轮历史记录，不声称当前全量全绿或`build`通过。R4的真实生存取件、BlockItem跨维度重放、pending解除、新世代、合法几何、旧现场任务和唯一库存/热量已核；**实际玩家进入Nether后的完整成型与交易**因隔离夹具实体加载前置缺失仍属`SUBCASE_UNAVAILABLE`，留同一次人工门。客户端护目镜/粒子/喷爆视觉、用户世界保存重进、物理卸载及真人多人边界也不由自动/静态证据代替。当前结论覆盖源码与既有证据，JAR身份和PM最终交付仍待随后核验。
