# EXT-B-BOILER-REWORK-01C：持续供热调压与汽轮机联动排错

**状态：** 出汽停流已复现并完成三入口最小修复，专用5项GameTest、增量assemble及一次规格/质量合并审查通过。用户随后反馈汽轮机持续残转未能复现，该项排查暂缓，不认定修复或要求本轮专项复现。指定的[01D汽口选择](./2026-10-07-boiler-steam-port-selection.md)已按纯过滤、不降级规则交付并通过新域4项、增量assemble和一次审查；01C源码保留其中，当前候选`99a526c`。锅炉集中人工门尚未关闭，未合入main，不接其他主线或教学。以下01C过程和证据保留历史语境。

**候选快照：** 050afe3e4eb70789804697b2880b7c76281ca8d7。PM仅同步文档与制品至主目录，源代码仍在同级候选；本批未改动力生产代码，不据此宣布残转修复。

**现场：** 用户持续输入热冷却液，将出汽压力下限从60调到10；炉压在80～90%循环，间歇输出大量超临界蒸汽，接新储罐未见蒸汽。锅炉直接接中型或大型汽轮机，0应力时外接轴残留转速会一直持续。

**目标：** 区分安全阀正常泄压、接收端背压和实际网络故障，修复已批准合同内的连续流动/停转缺陷，不用平衡改动掩盖故障。

## 已确认边界

- 01B单压力控件是出汽下限，不是目标炉压；实际汽温与炉压决定汽种，持续加热目标仍为配置超临界温度。不能仅因设置10就强制输出蒸汽。
- 保留尺寸、热工参数、安全阀90/80、汽轮机启动门槛/效率曲线/256RPM/一tick缓存；不添加自动降热、额外储罐、迟滞或免费HU。
- 蒸汽口应在实际汽种变化后自行恢复原生Create输送；冷热口不受调压打断，外部异种库存不改写。无其他动力源时，本机0SU后外接轴须自行停转。
- 本批需要真实持续生产、真实Create管道及汽轮机动力网络证据。01B人工热量种子测试没有覆盖此链路，不重复全套测试。
- 若发现需要新增玩法/设定，先给PM根因、证据和最小选项，暂停相关实现，已确认缺陷继续。

## 执行区与初始写集

工作区 E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition；HEAD de9297e7f43420fea82529d1957f2ba1f522a713。执行者禁止Git写入、治理文档修改、自行派发、客户端启动、用户存档/配置改写、旧存档研究、依赖升级和缓存清理。保留已有logs与__pycache__改动。

初始授权仅排错与定向回归夹具，生产代码只读：
- 新增 src/main/java/com/iksxh/create_nuclear_industry/gametest/BoilerTurbineFlowGameTests.java；
- 新增 src/main/resources/data/create_nuclear_industry_boiler_turbine_flow/structure/flow_empty.nbt，复用现有空模板，不另造框架；
- 专用报告 docs/reviews/2026-10-07/boiler-rework-01/continuous-flow-diagnosis.md；
- 原始日志/XML/制品 build/reports/extension/EXT-B-BOILER-REWORK-01C/。
生产修复范围由PM根据已复现根因补入本卡后实施；不得自行扩展。

### 已复现出汽根因与追加写集

专用持续工况首轮三场景、随后加入停流断言的失败轮和缓存轨迹均已保存。大型与空罐在60→10跨汽种资格后停止接汽；首管Layer II仍为完整超临界流、正确512压力，但Layer III的FluidNetwork.source.getCapability()为null。锁定Create源码确认失效BlockCapabilityCacheProvider不会复活，FluidNetwork.reset不清source；短暂切种未被两tick运输等待期观察到时保留旧network。人工种子长时间切种的01B测试未覆盖此情况。

PM据此追加以下最小生产写集，不改变任何热工或汽轮机平衡：

- boiler/BoilerPressureConnection.java：声明仅服务端调用的原生端点Network重建桥接；单位/范围注释明确。
- mixin/BoilerPipePressureMixin.java：仅丢弃该PipeConnection的Layer III network对象，使原生manageFlows从现有FlowSource重取有效provider。保留Layer II流体、所有压力份额及外部储罐，不改第三方通用reset行为。
- boiler/BoilerControllerBlockEntity.java：在已标脏蒸汽口的真实朝外首段管连接上调用桥接；时点为控制器tick的refreshPipes，不在drain执行中修改正在交易的network。兼顾原生泵邻接；非蒸汽口不因此撤销或重建。

上述路径均相对于src/main/java/com/iksxh/create_nuclear_industry/。修复后复用失败工况验证持续接汽，并证明实际汽种两侧及外部异种背压不丢mB/HU。0SU持续残转尚未稳定复现，继续在本卡新测试写集排查；未经根因确认不得改动力代码。

## 排错步骤与必要证据

1. 实际读取AGENTS、本卡、01B冻结合同及Minecraft开发/测试技能。核对锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280；中文注释说明服务端、单位、实际流量与SU关系。
2. 追踪BoilerState/Controller/SteamPressure、Create FlowSource/FluidNetwork/PipeConnection，以及TurbineState/Controller/ShaftPowerSource、GeneratingKineticBlockEntity/RotationPropagator。检查实际温压、产汽、正常出汽、阀泄放、汽种、输入/排汽流量、40tick均值、SU、双轴/外接轴source/network/speed。
3. 最小真实场景覆盖：持续供热与补水、持续冷液回收、锅炉60→10；直接向中/大型汽轮机供汽，排汽畅通，无额外动力源。与空接收罐比较，必要时变更热负荷区分正常过量产热。不可通过每tick重灌蒸汽/HU强制变种来代替持续运行。
4. 若捕获0SU持续旋转，保留首次失败日志与相关原生网络状态，确认出汽类型/库存/HU守恒；可用已验证当前版本种子缩短暖炉，但之后必须真实连续运行。
5. 排错完成先向PM报告明确根因、可复现轨迹及最小生产路径；只跑专用GameTest域，不跑全量，等待生产写集。
6. 修复后复用同一失败场景验证，仅增量assemble和必要受影响定向回归；一次规格/质量合并审查。PM维护文档/候选/Git，最终并入锅炉集中人工门。

## 本批结果与未决边界

- 原失败轮大型及空罐停止接汽，原生network.source能力为null而当前FlowSource能力有效。修复后800tick空罐实收77636mB、自然切种64次；大型实收75180mB、切种34次；中型实收56592mB，原故障不再出现。真实每tick汽质量及HU边界、外部异种背压、双端四齿轮箱回接停开均通过。
- 最终专用域5/5、服务端正常保存退出、Gradle exit0/24秒；只运行该域，复用未改账本单测。不将旧人工种子证据冒充持续生产证据。
- 增量assemble exit0/1秒，JAR2207578字节，SHA-256 B3BF640B296561533EA1FDBAC802DBEAC11AFCA530C58879940325DA76E6E911。实施及失败原始证据见[排错报告](../../reviews/2026-10-07/boiler-rework-01/continuous-flow-diagnosis.md)，一次独立审查见[合并审查](../../reviews/2026-10-07/boiler-rework-01/continuous-flow-review.md)。原始日志保存在同级候选build/reports/extension/EXT-B-BOILER-REWORK-01C/。
- 动力生产代码未改。真实直连与双端回接均能自行停止，用户现场永久残转未复现，不能宣称已解决。PM交付出汽修复后，请同次复测原装置；仍残转时收集前后轴及外接轴Source/Network/Speed，再继续针对性排错。
- 首段汽管夹具没有单独覆盖汽口直接邻接机械泵；该端型只有静态同接口依据，不记为实际测过。桥接不改drain/SIMULATE实现，本次未重复所有账本纯读单测；相关既有证据继续复用。
- 实施/root/boiler_turbine_flow_01c使用gpt-6.1-sol/high，跨真实流体、能力缓存和动力时序；审查/root/turbine_stop_cause_audit优先gpt-6-luna/high，未重复运行测试。PM读取锁定源码、差异、失败与最终日志及制品，维护文档/Git；客户端集中门仍保留。

## 技能实际应用

- C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md
- C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md
- C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/systematic-debugging/SKILL.md
- 同目录 test-driven-development、verification-before-completion、subagent-driven-development；本项目5.1精简验证及PM代码/Git分工优先于通用技能提交和重复测试步骤。
