# ACCIDENT-THERMAL-04：高温残骸、火点与有限熔融物实施计划

> **有限熔融物部分已被取代，禁止据下文旧接线派发。** 用户2026-10-21明确改为熔融物自身持续向下溶蚀，不设来源距离/累计溶蚀量上限；以[修订设计](../../proposals/2026-10-20-accident-thermal-corium.md)为准。下文原底面−6、水平＋2、64格总量、默认黑曜石抗蚀、按事故调度/所有权接线和验证仅保留历史上下文；后续需形成独立本体状态实施卡。04A独立卡及其已交付行为不受影响。

> **执行方式：** 由PM使用superpowers:subagent-driven-development派发，沿用唯一执行者＋一次组合窄审；执行者不做Git写操作。下列步骤为实施顺序，完成结果与人工门只在STATUS登记。

**目标：** 事故后出现可灭的有限火点、会灼烧的残骸和能够有限流动、溶蚀并凝固的熔融物；每份真实废料最终仍可沿现有封存链回收。

**架构：** 原事故账本继续负责接管和初始物料交接，另加热活跃记录；熔融载荷唯一托管在同维度SavedData，液格只作位置和液面投影。原执行器统一推进已加载热记录，所有实际世界写入共用既有64动作/t预算。

**技术基线：** Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；已核实际Gradle版本，执行时再核无漂移，不升级依赖。

**设计合同：** [已批准的热危害扩展](../../proposals/2026-10-20-accident-thermal-corium.md)。前置整改和手测范围见[STATUS](../../STATUS.md)，本卡不扩大其验收范围。

用户2026-10-21要求继续；先按[04A限定写集](2026-10-21-accident-thermal-04a.md)实施已冻结的高温固体/有限火点，不等待无依赖的熔融移动参数。下方总计划是后续完整范围，不授予04A执行者熔融物相关写权限。

## 派发门与职责

- 04A的历史起点为`37f50b9e`；后续熔融物/溶蚀以事故树`E:/MyMC/NewMod/Create_NuclearIndustry-accident`的`9f53a7d5218d1a51b6fdc43bbeb98735a72d9acd`为基线。实际工作树/分支由PM执行时登记，优先复用合适隔离树。下一批开工时确认保存退出及指定来源，当前不读取或搬世界；搬运遵守完整备份、完整复制和数量/哈希核对，不覆盖当前候选进度。
- 已批准的伤害、时间、份额、边界和预算不重问。两项尚缺具体值，已集中询问用户：建议每20tick移动1格、每事故初始熔融体积上限196格；默认50%份额下当前最大实际98格。这是建议，答复前不当作冻结值，不派发相关实现。
- 本卡当前仅规划；PM不写功能/测试代码。执行者只在下列写集内交未提交差异与报告，不能更新核心文档、状态、Git或用户世界；一次组合窄审复用实现证据。
- 必读根AGENTS、治理5.1/5.2、STATUS、设计合同、本卡及原事故/停机整改报告。实际使用`C:/Users/IKSXH/.codex/skills/`下minecraft-modding、minecraft-testing、minecraft-resource-pack、制品阶段minecraft-ci-release，以及对应systematic-debugging/TDD技能；手写注释中文。

## 行为与不变量

- 高温残骸每实体每20tick最多一次2HP直接热伤、点燃3秒；熔融物对应4HP、点燃5秒。同类多格不叠加，普通火抗规则保留，辐射服不自动防火。
- 熔融6000个加载tick后凝固，再经历2400个加载tick残骸冷却；相邻已加载水使冷却每tick扣4，否则扣1。卸载暂停，不按离线时间追补；接水不产新水源、黑曜石或额外废料。
- 真实废料份数的50%向上取整、受已确认体积上限约束，逐份选作初始熔融物，其余高温固体；未用燃料仍原件返还。采用一份一格、不分裂/不合并的有限搬移，优先向下，水平按固定顺序选未访问空位，拥挤等待，不复制分支源。
- 熔融载荷唯一由SavedData热记录持有；凝固后唯一移交原残骸BE，转移过程不产生第二份可提取载荷或辐射源。提前采掘高温固体仍掉现有废料，现场热量不变成可利用工质；熔融态不可采集。
- 最多8处火点、各600个加载tick；只在原破裂区可放置位置尝试一次，可扑灭、不蔓延/复燃。火点身份与终止收据持久保存，过期清理不能删除后来放置的其它方块。
- 原底面向下最多6格，水平投影外伸2格；每事故独立最多64格溶蚀、每40个活动tick最多完成1格，移动不重置锚点/额度。每个周期最多尝试一目标，实际删除才扣成功数，取消不在同tick反复试探。
- 默认黑曜石/哭泣黑曜石抗蚀；不可破坏、取消保护、新机组世代、任意BE或带物品/流体能力目标保守跳过。不清空未知容器、不扩大普通爆裂32格额度；不声称兼容全部领地插件。
- 原`complete()`继续表示原接管及初始世界结算完成，不能等冷却结束；独立热活跃集合继续调度。已完成事故的遗留空口重绑不等待300秒，原停机/权限/管理员规则保持。
- 全维度所有事故共用64次世界写/t；液体搬移的旧格删除和新格放置分别计数，补偿写也计数。无强载、无全世界实体扫描、无历史事故全表逐tick扫描、无工质回收/新GUI/制造配方。

## 精确写集与接口分工

下文J=`src/main/java/com/iksxh/create_nuclear_industry/`，R=`src/main/resources/`，T=`src/test/java/com/iksxh/create_nuclear_industry/`；花括号只枚举文件，不授权整个目录。

| 写集 | 职责 |
| --- | --- |
| 修改J/accident/{ReactorAccidentLedger,ReactorAccidentSavedData,ReactorAccidentService,ReactorAccidentExecutor}.java | 冻结参数、载荷交接、热索引/保存、共享调度；保留原complete语义 |
| 新增J/accident/{AccidentThermalParameters,AccidentThermalState,AccidentThermalEffects}.java | 参数值对象、持久热记录、已加载热推进/接触/世界动作；与原接管模型分开 |
| 修改J/accident/{ReactorCoriumBlock,ReactorCoriumBlockEntity}.java及J/config/AccidentConfig.java | 固体冷热投影、单件接收/采掘/引用失效，冻结配置及范围校验 |
| 新增J/accident/{MoltenCoriumFluid,MoltenCoriumBlock,AccidentFireBlock}.java；修改J/content/AccidentContent.java | 原生FluidType/FlowingFluid/LiquidBlock渲染接线，禁止原生无限扩散/抽取；火点不继承随机蔓延行为 |
| 新增J/accident/client/AccidentThermalClientEvents.java | 流体及火渲染层；借原版资源，不新增美术创作 |
| 修改J/radiation/RadiationSourceIndex.java | 加入熔融真实托管索引，搬移立即撤旧位置，转固后沿原残骸源读取；不改剂量算法 |
| 新增J/mixin/CoriumHosePulleyRestrictionMixin.java；修改R/create_nuclear_industry.mixins.json | 仅拦该熔融类型的Create软管抽取发现与实际出队删除，不泛改其它流体 |
| R/assets/create_nuclear_industry/blockstates/{reactor_corium,molten_corium,accident_fire}.json；models/block/{hot_reactor_corium,molten_corium,accident_fire}.json；lang/{zh_cn,en_us}.json | 原版熔岩/火/高温方块资源引用及必要名称；实际新建/修改按存在情况处理 |
| R/data/create_nuclear_industry/tags/block/corium_resistant.json；R/data/minecraft/tags/block/fire.json；R/data/create_nuclear_industry/loot_table/blocks/{molten_corium,accident_fire}.json | 抗蚀标签、火属性、不可凭空掉落；不新增桶/物品 |
| 修改J/gametest/AccidentExecutionGameTests.java、T/accident/AccidentLedgerTest.java；新增T/accident/AccidentThermalStateTest.java | 复用既有真实域和聚焦入口，验证本次状态/守恒/保护，不改构建规则 |
| 候选树build/reports/accident-thermal-20261021/ | EXECUTION/REVIEW、真实日志、失败证据、来源及制品清单；正常编译输出允许 |

Port、Lifecycle、CommonHooks最终事务、停机评估、热工、管理员、工业管网及封存生产实现不在写集。发现独立抽取绕行或必须改其它入口时先报准确调用链，由PM增补范围，不自行扩写。

## 实施任务与检查顺序

### 任务1：证明原生液面、提取保护与最小状态合同

**接口：** `AccidentThermalParameters`持有本卡冻结值；`AccidentThermalState`以事故ID＋原列ID识别记录，阶段MOLTEN/HOT/COLD，记录当前位置/原锚点、唯一载荷、剩余加载tick、移动访问及交接收据。后续任务使用同一模型，不另建载荷库存。

- [ ] 先在既有真实域取得失败证据：液格可以真实搬移及显示，普通桶/开口泵/工业抽口/软管的新发现和已有队列均不能抽走或清掉它；不能以“没有桶物品”替代真实抽取检查。
- [ ] 按本地锁定源码核精确注入点：`LiquidBlock.pickupBlock`可能先删块再返回空桶；Create `FluidDrainingBehaviour.pullNext`会直接删非零液面。覆写桶拾取为空且不写世界，软管同时挡发现及真正删除路径；禁止原生流体tick自发扩散。
- [ ] 接触/火只走服务端；客户端只借原版液面及火动画。先取得最小GREEN并报告不可证明入口；不为探索另起模拟器或升级依赖。

### 任务2：载荷、计时、辐射与凝固

**接口：** 原Column向热记录移交后清空原payload并settled；热记录到Corium BE沿`accept`身份收据移交，`take`仍为唯一采掘出口。SavedData新增热活跃索引，Executor在原账本complete后仍能推进热记录。

- [ ] 纯逻辑先RED：1/2/196份载荷选择及体积上限、6000→2400→冷、接水×4、卸载暂停、交接重入不增料、complete与热活跃独立。定向运行`.\gradlew.bat test --tests '*AccidentThermalStateTest' --tests '*AccidentLedgerTest'`，不使用clean或强制重跑。
- [ ] 实现单一所有权与可恢复交接意图；世界投影失败不丢托管、不重发物料。正常保存重进后阶段/计时/位置/收据/辐射源不重置，不宣称跨文件崩溃原子性。
- [ ] 真实事故→移动→转固→提前采掘/封装各阶段核载荷和源数量，温度消退不清辐射；已完成仍热时重建空口可重绑，复用停机聚焦业务。

### 任务3：有界移动、火点与溶蚀

**接口：** `AccidentThermalEffects`从Executor领取同一维度预算，按记录推进候选动作；世界写入前复核加载、目标身份/世代与保护，预算不足等待。原事故创建时登记完整可能作用盒中的已有世代，不把邻堆误当新建堆或反之。

- [ ] 先RED锁定原底面−6、水平外伸2、64次溶蚀/40tick、两个事故合计64写/t、火点8处/600tick/灭火不复燃；保护取消、容器、新世代均不被删除。
- [ ] 腐蚀用专用FakePlayer直接投递BreakEvent并读取最终取消态，随后复核目标；不调用会提交本模组拆除事务的`CommonHooks.fireBlockBreak`作探针。监听异常保守保留；实际入口需真实验证。
- [ ] 热伤害按实体/类型20tick节流；火抗与辐射相互独立。火点使用不蔓延的独立火方块和身份租约；逻辑到期立即失去危险，物理清理排队仍受共享预算约束。
- [ ] 同一真实域覆盖加载暂停/当前格式保存重进、有限实际流动、取液保护、取消/边界和物料守恒。未观察到的物理卸载/真人多人明确记为证据边界，SKIP不计业务通过。

### 任务4：组合窄审、候选和集中手测

- [ ] 唯一执行者交差异/全部退出码/必要RED与GREEN；仅运行受影响JUnit、一个现有真实域及资源引用校验。GameTest沿`CNI_ACCIDENT_FOCUS`筛选，实际聚焦键由本卡用例登记；不因注册API示例新增测试框架。
- [ ] 一次组合窄审重点检查：软管旧队列绕行；热记录与物理BE双所有权；热阶段延长旧事故锁；卸载追补/双计辐射；保护监听后的二次目标变化。整改只复验变化和未闭风险。
- [ ] PM核源码来源并提交后，执行者唯一一次增量jar交本批报告目录；跟踪旧JAR/日志若改变先备份交PM处理，不自行恢复。PM核来源与制品身份，候选包不自动覆盖main正式包。
- [ ] 集中客户端体验：热伤/火抗、扑灭不复燃、液面真实搬移与围挡、接水快冷、凝固采掘封存、正常保存重进、接口解锁及冷态拆除。服务端通过不代替视觉/操作验收；完成后才做事故教学。

## 本卡自查与当前边界

已将热、有限火、载荷、流动、溶蚀、保护、加载和原链回收分别落到任务；保留所有旧安全门。技术输入为事故树`build/reports/accident-thermal-plan-20261021/TECHNICAL.md`，仅本地源码阅读，未据此声称接口运行通过。本轮只形成计划和登记用户整改反馈，没有实施、运行游戏或构建；两项新增数值答复及执行工作区/存档条件齐备后再派发。
