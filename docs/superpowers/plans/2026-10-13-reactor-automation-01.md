# REACTOR-AUTOMATION-01 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 通过可选反应堆自动化接口让一只Create烈焰人燃烧室提供整堆手动控制、恒定HU/t及可布线的失效停堆，全部控件显示在燃烧室。

**Architecture:** 仪表端口继续唯一持有反应堆权威快照；接口保存设定，燃烧室提供操作与许可信号。新增纯控制器只产生棒目标和运行许可决策，世界桥接在正式tick边界校验绑定、代际和控制权后提交。红石SCRAM仍由仪表入口触发，管理员持锁使其下降沿不能恢复旧提棒目标。

**Tech Stack:** Minecraft1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6，沿用Gradle9.0.0与既有JUnit/GameTest入口。

**Spec:** [已批准完整设计](../specs/2026-10-13-reactor-automation-design.md)。用户2026-10-13明确采用整稿及推荐参数，并确认“计划无误，按既定方式开工”。已交[稳定候选](../../reviews/2026-10-13/reactor-automation-01/CANDIDATE.md)`4757f27`，停集中客户端门；本卡开工过程、RED/扩卡与同次审查整改按实际阶段保留。

## 全局约束与执行方式

- 使用既有用户授权的“唯一实现执行者＋一次独立规格/质量组合窄审”。PM只维护文档、Git和证据；执行者实际使用executing-plans，禁止Git写、转派或自行验收。这里的四个任务是同一执行者的依赖顺序，不按每个小步重开执行者/审查。
- 只新增 `reactor_automation_port`；侧壁非棱边非角点、可无但最多一个；燃烧室在炉外相邻格，控件必须在燃烧室可见表面。FADING/KINDLED/SEETHING值班有效，NONE/SMOULDERING不自动值班；原燃料和机械臂行为保持。
- 参数严格采用规范第5节：10tick调节、40tick显示平均、正常提出1/插入5个百分点、max(1HU/t，目标2%)死区、目标可设上限1000000HU/t、冷液启动25%/预警15%、热液启动上限75%/预警85%/故障95%、热预警为原损伤阈值50%、100tick健康复位、20tick最长租期、40tick联锁建立超时。保护每tick检查。
- 配方为 `STS/PIP/SCS`：S=`c:plates/steel`，T=`create:electron_tube`，P=`create:precision_mechanism`，I=`create_nuclear_industry:industrial_sensor`，C=`create_nuclear_industry:reactor_casing`；有序工作台产1个接口。
- 不修改裂变、超频、燃耗、每列损伤阈值、原棒物理到位/动画、冷热口流量、锅炉/汽轮机性能、L1/L2美术协议或既有渲染兼容。未受控/卡死剩余发热不归零，量化安全余热不误报故障，无摄氏温度假字段。
- 不实现负载跟随、管理员动画专项、新PNG/SVG、独立管理台、换料/修复、压力系统、旧存档迁移或新Ponder。所有手写注释/Javadoc中文；公共入口说明两侧、单位、tick顺序及不变量。
- minecraft-modding/testing及相关Superpowers技能按真实入口读取；原已验管网、Sodium、素材、教学证据复用。原泵/现场流量/16³锅炉人工门保持。

## 开工与来源

- [ ] PM核主分支、空索引、当前源码与用户dirty，阅读本卡及批准规范。先检查已有同级树；不适合则从当前已批准main创建 `codex/reactor-automation-basic`，目录 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation`，禁止覆盖同名非本任务目录。
- [ ] 使用using-git-worktrees流程，用户同级目录要求优先于工具默认路径。执行时才创建，不在计划阶段创建。确认实际主客户端已保存退出，再完整携带 `run/saves`；已有目标世界先备份，不混合覆盖，不回写主世界。核复制数量/内容一次，记录实际世界来源。
- [ ] 保留实际生效的CNI相关配置及其身份，候选独立使用，不重置用户P1热工设置。新增管理员配置采用本卡已批准默认值；GameTest使用隔离目录。新树不携带主目录无关模拟器/美术/日志改动。
- [ ] 执行者登记基线、版本、写集、来源和原包。生产基线包括已验Sodium窄修；源对应的原8c03dbd…包可作载荷审计基线，主目录745e/261012包不覆盖。工作/构建命令必须使用候选目录。

## 文件结构与权限

下列短路径均相对于 `src/main/java/com/iksxh/create_nuclear_industry/`；Java测试对应 `src/test/java/com/iksxh/create_nuclear_industry/`。每个类单一职责；任务未列的源码/依赖/生成资产不得修改，需要扩写先报告PM。

| 职责 | 精确新建路径 |
| --- | --- |
| 方块/物品/BE注册与ID | `content/ReactorAutomationContent.java` |
| 服务端配置及一次参数快照 | `config/ReactorAutomationConfig.java` |
| 结构接口方块和设定持久化 | `automation/ReactorAutomationPortBlock.java`、`automation/ReactorAutomationPortBlockEntity.java` |
| 纯参数、绑定、状态、观测和决策 | `automation/ReactorAutomationParameters.java`、`ReactorAutomationBinding.java`、`ReactorAutomationState.java`、`ReactorAutomationObservation.java`、`ReactorAutomationController.java` |
| 服务端权限、提交和生命周期 | `automation/ReactorAutomationService.java` |
| Create燃烧室识别、许可和显示桥接 | `automation/ReactorAutomationBlazeBridge.java`、`ReactorAutomationBehaviours.java`、`ReactorAutomationGoggles.java` |
| 有代际/会话戳的命令与纠正 | `automation/ReactorAutomationNetwork.java`、`automation/client/ReactorAutomationClientEvents.java` |
| 燃烧室方块/BE的窄接入 | `mixin/BlazeBurnerAutomationBlockMixin.java`、`BlazeBurnerAutomationEntityMixin.java` |
| 定向测试 | `automation/ReactorAutomationStructureTest.java`、`ReactorAutomationControllerTest.java`、`ReactorAutomationAuthorityTest.java`、`ReactorAutomationBlazeTest.java`、`ReactorAutomationDataTest.java` |
| 真实独立测试域 | `gametest/ReactorAutomationGameTests.java`，`@GameTestHolder("cni_reactor_automation")` |

允许修改的既有源码：`CreateNuclearIndustry.java`、`content/ModCreativeTabs.java`；`structure/ReactorStructureDefinition.java`、`ReactorStructureScanner.java`、`ReactorStructureLifecycle.java`、`ReactorStructureDiagnostics.java`、`ReactorSurfaceDescriptorFactory.java`；`blockentity/ReactorInstrumentPortBlockEntity.java`、`ControlRodDriveBlockEntity.java`；`control/ControlRodSliderService.java`、`ControlRodSliderBehaviour.java`、`ControlRodScramService.java`。只补可选接口识别、权威桥接和必要只读锁定；其他方法行为保持。

允许修改资源：`src/main/resources/create_nuclear_industry.mixins.json`、`assets/create_nuclear_industry/lang/zh_cn.json`、`en_us.json`；新建同namespace下 `blockstates/reactor_automation_port.json`、`models/block/reactor_automation_port.json`、`models/item/reactor_automation_port.json`，以及 `data/create_nuclear_industry/recipe/crafting/reactor/reactor_automation_port.json`、`loot_table/blocks/reactor_automation_port.json`。接口模型只引用既有外壳/仪表/工业传感器资源，完整方块碰撞；不替换旧PNG或重置美术。新建 `data/minecraft/tags/block/mineable/pickaxe.json` 时仅用replace=false补本ID；已有同路径则只追加本ID。真实测试模板唯一新增 `data/cni_reactor_automation/structure/automation_empty.nbt`，16×8×16，编码沿用现行 `data/reactor_surface_display_probe/structure/surface_probe_empty.nbt`，不修改原件。

执行者报告仅 `docs/reviews/2026-10-13/reactor-automation-01/IMPLEMENTATION.md`；原始日志/临时夹具/冻结来源仅 `build/reports/reactor-automation-01/**`。PM负责CANDIDATE/REVIEW/CLIENT-CHECKLIST和治理文档；其他源码测试如因新增枚举需要窄适配，报告精确失败后由PM扩卡，禁止自动删断言。

### 开工后的必要写集补充

2026-10-13任务1真实RED为10项中2项失败：合法单接口被旧结构扫描拒绝及配方资源不存在，见`01-red.log`。执行者随后指出`structure/ReactorSurfaceDescriptor.java`的`BLOCK_IDS`只接受旧壳成员，PM核实际29～32行白名单及174行校验确认：新接口会使完整表面投影失效。允许该文件仅向白名单追加`create_nuclear_industry:reactor_automation_port`，不改L1字段、NBT、成员坐标/朝外面、代际或旧接收规则；必要断言写在本批StructureTest。此为新结构成员的同合同接入，不重构渲染或再次测试已验Sodium。

任务1`01-green-2.log`实际编译失败指出既有`src/test/java/com/iksxh/create_nuclear_industry/structure/ReactorInstrumentStructureSummaryTest.java:147`的`switch(type)`未覆盖新增AUTOMATION。允许该文件仅追加AUTOMATION到新接口ID的转换分支，原摘要断言不改、不删除；属于枚举穷尽性必要适配。

继上述适配后，`01-green-3.log`实际编译指出`src/test/java/com/iksxh/create_nuclear_industry/structure/ReactorStructureContractTest.java:249`相同ID转换分支未覆盖AUTOMATION。该测试也允许仅补新类型到新接口ID的映射，原合同断言保持。两处同型分支已核，避免通过删除枚举穷尽校验绕过问题。

任务4指出现有Gradle默认把`src/main/gametest`类及测试模板一并打入JAR，原基线也已包含旧域。PM核实际构建与本卡“新增fixture/测试类不得入生产包”的要求，允许`build.gradle`仅新增本批JAR排除规则：`com/iksxh/create_nuclear_industry/gametest/ReactorAutomationGameTests*.class`和`data/cni_reactor_automation/structure/automation_empty.nbt`。保留开发编译/资源/运行classpath，使四代表仍走现有GameTest；不新增source set、改变依赖、排除其他旧域或清理历史制品。规则使用中文注释，最终载荷检查证明两项本批测试输入未入JAR；既有旧GameTest类不能据此宣称全包均不含测试。

任务3/4发现手动精修后各棒深度可能不同，统一预警深度会提起较深棒或超出正常插入步长。保护语义按原批准“预警禁止提棒、正常每次插入最多5个百分点”执行：允许Decision在现有可选整组命令之外增加逐棒安全插入指示，Service以一个权威映射提交。每tick先阻断目标低于实际/既有较深目标的待执行提棒，到调节周期才各可动棒在安全基准上追加最多配置插入步长；卡死棒保持。预警下全堆/逐棒请求只能进一步插入，若全堆请求会提起任一可动棒则整次拒绝。只在已列Controller/Service/仪表/旧SliderService与本批测试实现，无新路径、新玩法值或正常手动后台重写；补不同深度与周期前待执行提棒的定向断言。真实组合旧证据在未改对应路径时复用，权威逻辑改变才补相关代表。

## 同一次组合审查整改

2026-10-13初轮独立组合审查发现两项Important，PM按已批准控制安全合同接受，交原执行者同批整改：

1. 接口同一服务器回合拆下原位重放，队列合并后的结构内容相同，旧请求可能沿用原代际/epoch。使用原Service/PortBlock/PortBE/仪表写集，已加载接口拆/换即时撤权并推进控制epoch，核对实际PortBE身份，防命令先于正式tick漏过；普通库存/自动步进不换epoch。真实代表不在remove和replace之间人为rescan，证明新实例绑回后旧请求仍被拒绝。
2. 首次急停时已卡死棒未进入旧savedTargets；原钢板维修解除卡死后，管理员持锁降沿必须对当前全部可动棒建立100%恢复基准，不能只遍历旧savedTargets。无管理员的保存/恢复语义保持；原SCRAM/Service/Controller允许范围内实现，真实代表走原维修入口后降沿/正式tick，证明不会按旧浅目标提棒。

保留初包`f29c4ea7...`与所有首轮证据，实际生产修复后只补必要定向检查、本域代表及第二次增量包；同一审查者仅复核实际差异，不另开审查或重跑旧域。审查同时指出Javadoc错挂与复位拒绝提示写死100tick；前者按AGENTS注释准确性要求修正，后者改为适用于配置的原因提示，仍限原仪表/两语言写集，文案不另建镜像测试。生产热工、原维修玩法和人工门保持。

## Review Focus

1. 燃烧室熄火/被拆时，许可消失且控件仍可查看或安全停堆；火焰重新出现不能自行启动。由任务2/4真实喂料与拆除用例覆盖。
2. 管理员故障后仪表红石下降沿不能恢复旧提棒目标；没有管理员的旧SCRAM仍恢复原目标。由任务2原子交接/原回归覆盖。
3. 同坐标重新成型、换管理员或两人同时提交时，旧控件包/拖动不能越过新控制代际；所有玩家规则一致。由任务2权限与旧滑块会话用例覆盖。
4. 冷启动实际流量0、整数mB余热、非线性超频及燃料耗尽不能导致死锁、假停堆或反复调棒。由任务3纯控制/真实小堆覆盖。
5. 普通未接入燃烧室、原燃料/机械臂、专用服务器和缺区块场景保持原行为；不能强载或让客户端显示控制物理反应。由任务2/4原生代表和卸载用例覆盖。

## 任务1：可选结构接口、内容与配置

**Files:** 新内容/配置/接口Block和BE、Parameters与State基础类型，结构definition/scanner/lifecycle/diagnostics/surface factory，入口与creative tab，资源及Structure/Data测试。

**Interfaces:** `ReactorAutomationContent.ID="reactor_automation_port"`与`PORT/PORT_ITEM/PORT_ENTITY`注册字段；结构`PortType.AUTOMATION`只允许0或1个。State中先声明纯 `Mode {STOP,MANUAL,CONSTANT}`；PortBE内定义 `Settings(Mode mode,int manualDepthPercent,int targetHeatHuPerTick)`，`settings(): Settings`和`applySettings(Settings): void`不拥有核反应快照。`ReactorAutomationConfig.parameters(): ReactorAutomationParameters`返回一次不可变已验证参数，Parameters包含规范15项同名字段。

- [ ] 写行为RED：原纯结构测试加入自动化接口ID侧壁用例，旧实现应拒绝；数据测试断言有序配方和材料数量，旧资源缺失应失败。先证明规则缺口，不把测试编译缺符号作为行为RED。
- [ ] 注册接口；结构0接口仍正常、1接口可成型、2接口拒绝，框架/角/顶底拒绝；错误明确到位置。扫描和事件缓存包含新ID，但旧结构冷却容量、列图和L1成员含义保持。接口沿用原结构拆除边界，不因可选控制功能绕过运行中拆壳/危险拆除后果；安全拆装与外部燃烧室移除分别验证。
- [ ] 新配置使用SERVER类型及独立文件名 `create_nuclear_industry-reactor-automation-server.toml`，避免覆盖原SERVER配置；15项默认值与批准规范一致。校验正周期/步长、目标非负和比例顺序，异常配置不能默默允许提棒。设定默认STOP/100%/0HU，重进只保留值而不恢复运行权。
- [ ] 加完整模型、loot、recipe与两语言。数据断言 `STS/PIP/SCS`、4/2/1/1/1且产1，禁止无序/序列装配或新管理台注册；模型资源引用均能解析，未改任何原材质。
- [ ] Run：`.\gradlew.bat test --tests '*ReactorAutomationStructureTest' --tests '*ReactorAutomationDataTest' --tests '*ReactorStructureContractTest' --console=plain`。Expected：新增0/1/2和位置/数据规则通过，旧结构合同保持；保存RED/GREEN日志及XML，不打包中间半成品。

## 任务2：控制权、燃烧室操作与安全交接

**Files:** State/Binding/Observation/Service/BlazeBridge/Behaviours/Goggles/Network/ClientEvents、两Mixin；仪表/驱动器、旧SliderService/Behaviour/ScramService、语言；Authority/Blaze测试。

**Interfaces:**

- `ReactorAutomationBinding`含dimension、ownerPos、ownerGeneration、portPos、burnerPos、authorityEpoch；epoch随绑定改变、每次成功控制设置/复位及失效撤销变化，绝不使用纯库存tick或正常自动步进来制造控制会话变化。
- `ReactorAutomationState`由仪表持有，包含claimed、Phase(STOPPED/ARMING/MANUAL/CONSTANT/FAULT)、保持原因、commandedDepthPercent、authorityEpoch、调节/健康/租约年龄；不保存第二份燃料/冷却剂状态。
- `ReactorAutomationService.beforeReactorTick(owner, parameters, gameTime)`、`afterReactorTick(owner, result, parameters, gameTime)`、`onTickFailure(owner, gameTime)`集中控制；对应owner只公开必要状态与同龄观测入口。
- `ReactorAutomationService.applyCommand(ServerPlayer, Request): Response`；Request包含burnerPos/portPos/ownerGeneration/authorityEpoch/command/value/clientSequence，Command为MODE/DEPTH/HEAT/RESET；Network注册和处理只调用该入口。
- `ReactorAutomationBlazeBridge.findBinding(Level, BlockPos): Optional<ReactorAutomationBinding>`、`runPermission(Level, BlockPos): int`、`isDutyValid(BlazeBurnerBlockEntity): boolean`；不强载、不猜全局当前世界。Goggles只读服务端同步view。

- [ ] 权限RED：旧拖动在管理员交接后仍能COMMIT、管理员持锁SCRAM降沿仍恢复旧目标，应由真实原入口失败复现。新状态用例约束旧代际/两人乱序/超范围/旁观者提交拒绝且不改变设定，不能只检查源文件文字。
- [ ] 实现可见操作：燃烧室背对接口的可见面分开布置模式/插深/HU/复位4控件，显示与命中同源且互不覆盖；隐藏接口面和底面不命中。有效绑定但无燃料时仍显示故障/STOP，启用运行/复位再验证值班。持燃料普通右击与机械臂仍走Create喂料路径。
- [ ] 用专用BehaviourType区分4控件，客户端收集绑定戳及序号，原生Create通用设值包不得另行绕过CNI校验。仅游戏线程受理、按玩家当前世界及 `canInteractWithBlock(burnerPos,0)` 校验可达性，验证当前绑定/代际/数值；旧响应不能覆盖新预览/新绑定。
- [ ] 全堆目标只通过仪表一次提交；常量模式/FAULT锁定逐棒控件，手动允许逐棒修整，仅确认全堆值时重写。旧SliderService新增按owner撤销会话入口，旧服务端BEGIN/PREVIEW/COMMIT和Create提交路径均检查锁，驱动器同步只读锁标记；无管理员路径保持原接受/恢复行为。
- [ ] 原子安全交接：先记录持权和全插入恢复基准、撤销旧会话，再发邻居许可更新。管理员持锁时原SCRAM释放使用100%恢复基准而非旧提升目标；无管理员分支逐字语义保持。故障不伪造红石输入、不另设直接SCRAM入口。
- [ ] 实现燃烧室许可15/0及普通未绑定的原行为。锁定Create6.0.10的BlazeBurnerBlock没有声明getSignal/isSignalSource，禁止对不存在的方法写必须命中的Inject。采用窄Block Mixin增加仅绑定时有效的信号派生标记/必要继承方法，BE Mixin接入原addBehaviours/tick/read-write；信号真值仍由当前服务端授权决定，标记不能成为运行权限。原比较器、燃料和HEAT_LEVEL路径保持。
- [ ] 每tick读取已加载绑定；已知拆除/熄火立即撤销，未知心跳最多20tick，任职恢复只STOP/FAULT。接口移除但owner仍加载时持锁不能丢失；owner重进或代际变化不恢复旧许可。未加载部分结构不读区块、不运行正式反应，恢复后先建安全基准。
- [ ] Run：`.\gradlew.bat test --tests '*ReactorAutomationAuthorityTest' --tests '*ReactorAutomationBlazeTest' --tests '*ControlRodSliderProtocolTest' --tests '*ReactorScramFissionRegressionTest' --console=plain`。Expected：新安全/绑定用例与旧无管理员语义均通过。客户端可见面、命中和goggle首行留白用实际Transform/Component入口，GPU留人工门。

## 任务3：恒定功率与每tick保护

**Files:** Parameters/Observation/Controller/Service、仪表正式tick观测、Controller测试，真实GameTest。

**Interfaces:** `ReactorAutomationController.predictCurve(snapshot, reactorParameters): double[101]`，返回0～100%统一实际棒深的原裂变预测；`chooseDepth(curve,targetHu): int`；`decide(state, observation, automationParameters): Decision`。Decision含nextState、可选整组depthCommand、runPermission、保持/调节原因；仅Service可把其提交到owner。

- [ ] 纯控制RED/夹具：曲线 `ceil(1000*(1-d/100.0)^2)` 的250HU选择50%，目标2000选可达0%并报告上限；安全同值取更深档。100%向0%一次最多99%，50%向100%一次最多55%；第9tick不调整、第10tick可调整。原真实FissionCalculator另做超频/指数/损伤配置预测，副本评估不改变原snapshot、耐久或库存。
- [ ] 在101档中选择不超目标的最近可达值，不假定线性或未经证明的单调性；正常每10tick按1/5步长逼近，max(1HU，2%)内停止来回调节。预测缓存只依赖实际计算器影响发热的配置/燃料/布局/完整度字段，未变时复用，不能漏掉有效燃料或真实耐久影响；缓存不保留旧世界/代际，不每tick重扫结构。显示40tick平均仅影响显示；实际保护每tick。目标0进入STOP，可设上限1000000与真实可达上限分开。
- [ ] 构造一次成功结算的同龄观测：发热、冷/热库存、容量、逐列 `max(0,cachedHeatHu-quantizedHeatRemainderHu)`、新损伤、覆盖/卡死、当前配置和冷却结算。owner失败/样本失效不给旧观测续龄；不用L1/L2客户端快照决定物理运行，不增加温度或模拟额外热量。
- [ ] 保护断言：冷50%/热10%/流量0且健康允许冷启动；cached0.24HU且安全余数0.24HU不会因0.25阈值误报；有效0.125HU预警、0.25HU故障。冷低于15%、热高于85%预警禁止提棒，热高于95%故障；实际阈值0时用正有效热量判断，不除零。覆盖缺失/卡死/新损伤/倒计时/断燃料立即FAULT。
- [ ] 启动要求冷≥25%、热≤75%、棒全插入、配置吸热正、样本/绑定有效、无卡死且所有有效燃料被邻接棒覆盖。共享容量0不计算比例，按冷却不可用处理。下一tick冷液需求必须检查拟提交棒目标推进后的发热与真实余热，不能只用全插入时的旧0HU样本；使用现有热账本的量化与吸热规则，不把安全量化余数当成缺冷。实际库存/结算约束优先于端口额定数。
- [ ] 固定正式tick顺序：读取并校验输入→接续安全红石/持锁→一次管理员目标提交→原控制棒/裂变/冷却/热工/损伤结算→冻结新观测→保护降权及通知许可。所有目标/授权写完后才通知邻居；红石重入只消费完整状态，不重复执行核反应。保护或失败追加请求不能拼装旧新L2样本。
- [ ] ARMING从全插入基准检查仪表高电平，发15许可后最多40tick确认解除；其他独立急停保持高电平时不开始提棒。FAULT稳定健康100tick后，显式RESET只返回STOP且许可0；补燃料、指标恢复、模式包或旧GUI回包不能自动复位。
- [ ] Run：`.\gradlew.bat test --tests '*ReactorAutomationControllerTest' --tests '*ReactorAutomationAuthorityTest' --console=plain`。Expected：非线性/量化/零流量启动/保持/边界的具体断言通过；任务2只在本任务触及其权威逻辑时复跑，不机械复跑任务1。

## 任务4：真实组合验证、交付和人工门

**Files:** `gametest/ReactorAutomationGameTests.java`、原允许资源；本批IMPLEMENTATION与build/reports证据。无额外生产重构。

- [ ] 新GameTest域只放4个代表场景，使用 `@PrefixGameTestTemplate(false)` 与本域 `automation_empty` 模板：`operator_controls`（真燃烧室喂煤、全堆/逐棒交接、绝对HU）；`fault_latch`（真实红石反相线路、外部燃烧室移除/断燃料、100tick复位及降沿不反弹）；`lifecycle_isolation`（真实安全拆装换代、旧请求拒绝与BE现行NBT读写）；`native_burner`（未绑定燃烧室原燃料/机械臂与比较器代表、专用服加载）。BE读写或模拟失联不得称作实际区块卸载/完整重启；真正跨区块卸载和保存重进列入本批客户端集中复看，不强载以伪造结果。
- [ ] 使用现有build.gradle支持的 `-PgameTestNamespace=cni_reactor_automation -PgameTestDirectory=build/reports/reactor-automation-01/server-probe`：`.\gradlew.bat runGameTestServer -PgameTestNamespace=cni_reactor_automation -PgameTestDirectory=build/reports/reactor-automation-01/server-probe --console=plain`。Expected：仅本域4场景通过、自然保存关闭、exit0；若只业务断言通过后退出挂起，分别记录，不伪称进程成功，不重跑所有旧GameTest域。
- [ ] 一次必要增量 `.\gradlew.bat assemble --console=plain`；稳定后冻结实际source/class/resources/JAR身份、配置默认/实际消费链、原包。对比基线列出必要新路径/变更，解释同源伴随类/行号变化；禁止第三方JAR、fixture、报告或测试类入生产包。
- [ ] 执行者交一份IMPLEMENTATION：精确写集、真实RED/GREEN/退出码、首次失败、使用技能和版本、原生入口保持、未做GPU事项，以及启动候选目录。来源稳定后停止写入，不Git提交或派发审查。
- [ ] PM派本批唯一一次组合规格/质量窄审，只读既有证据与制品。若有必改，仅原执行者修实际差异并定向验证；实际生产修复才允许必要重打包，保留旧失败/包。无源码差异不再次构建。
- [ ] 审查通过PM净提交候选分支源码，写CANDIDATE与一张人工清单，停客户端门；未手测不合main、不替换主目录正式包或世界。
- [ ] 人工集中复看：侧壁接口与燃烧室任职；4控件在燃烧室且不挡补料；全堆/逐棒交接；恒定HU逐步调棒；真实反相安全线路、断燃料/拆除/恢复/复位；小堆跨区块失联/返回和同版本保存重进均不自动提棒；goggle图标与文字。用户反馈不自动扩大到其未报告的细项；其他原门保持。

## PM计划自查与交接

规范1～2对应任务1/2，功率/保护及15参数对应任务3，红石/代际/输入冲突对应任务2/3，配方资源对应任务1，真实与客户端验证对应任务4；5类Review Focus均有归属。各任务消费的类型/入口已在前述Interfaces声明，文件职责、允许写集、版本、数量/单位与规范一致。

本计划只细化已批准玩法，不增加后端仪表绑定、棒物理速度或新材料。执行方式沿用原唯一执行者/一次审查授权，不向执行者转授PM/Git权。完整计划已提交且用户审阅通过；内部实现的实际结果见下节，客户端验收仍待用户操作。

## 当前交付状态

同级候选`codex/reactor-automation-basic`已完成任务1～4内部实现与审查，源码净提交`4757f27`；详[CANDIDATE](../../reviews/2026-10-13/reactor-automation-01/CANDIDATE.md)、[REVIEW](../../reviews/2026-10-13/reactor-automation-01/REVIEW.md)及[IMPLEMENTATION](../../reviews/2026-10-13/reactor-automation-01/IMPLEMENTATION.md)。27文件世界与6配置已携带，原件保留；47来源与最终4204ab17…包一致。

初轮31/31和整改28/28、同一生产真实4/4自然保存exit0、两次必要增量assemble与一次独立组合审查/同次窄复核均有原始证据。无下降沿补充的业务4/4与保存挂起非成功分列，提交前四份EOF纯格式经正文实核且未重测试/打包。未合main、未替换主正式包/世界，当前停[管理员集中人工门](../../reviews/2026-10-13/reactor-automation-01/CLIENT-CHECKLIST.md)。原清单中用户操作、视觉、真正卸载/保存重进仍未勾通过，不因此继续其他主线。
