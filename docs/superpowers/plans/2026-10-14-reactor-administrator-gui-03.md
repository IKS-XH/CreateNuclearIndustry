# REACTOR-ADMINISTRATOR-03 三页GUI、连续换料与核热值班实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans 执行本卡；PM使用 superpowers:subagent-driven-development 编排。项目既定唯一执行者、一次组合窄审、执行者无Git写权限优先于技能的逐步提交及重复审查流程。

**Goal:** 任职烈焰人使用专用三页GUI管理反应堆，部分燃料耗尽不断运行，并由真实核热维持普通燃烧。

**Architecture:** 仪表仍是唯一反应堆权威，接口保存设置，菜单只拥有玩家会话与只读投影。原核tick在冷却前结算维护支出，成功提交后才发短期付热凭据；燃烧室保留原生tick，仅在有效付款期间暂停普通外部燃料计时并显示KINDLED。

**Tech Stack:** Minecraft 1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6、Gradle9；不得升级。

**Spec:** [已批准设计](../specs/2026-10-14-reactor-administrator-gui-design.md)。2026-10-14用户“可以，开始施工吧”直接授权；PM可以细化实施卡后自动派发，不再重复确认。

## 全局合同与基线

- 状态：执行中；原`reactor_automation_retrofit_impl`已收到任务并确认唯一接续，候选派发文档提交`6fcf9c0`。独立审查为原`reactor_automation_review`，待稳定组合候选；PM不写功能、测试、构建代码。
- 复用`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation`、`codex/reactor-automation-basic`，文档基线`d26018a`、生产基线`2dff787`，冻结JAR`5e04c667fc509a0eb9349bb95faf6c3a15b5d19a961ffdeb29458d00b5c2d434`。
- 原候选仅`logs/debug.log`和`logs/latest.log`有用户更新；01:35:15已保存各维度。保持当前候选世界原位，不开分支、不重新搬档、不启动/写用户世界或配置、不杀用户客户端。
- main管理员功能仍未集成；main已验Logo及34bedc81…JAR不动，不用main整类或整包覆盖候选。01/01R既有手测门合并进本次集中复看，不能登记通过。
- 本批不实施02 PI/应力表绑定，不改变反应堆几何、端口、裂变/燃耗公式、自动换料事务、工业管网、锅炉、汽轮机或美术模型；不研究旧存档迁移。
- 手写注释/Javadoc中文，跨包入口说明HU、tick、服务端权威及关键守恒。新界面只用现有字体/Create控件和代码绘制，不新增美术PNG/SVG或代派美术任务。
- 已批准312×224左右布局、最大14×14只读堆芯、显示40tick平均、维护1HU/t。维护配置正有限，范围`(0, 1000000]`；无管理员原核tick默认维护请求0。
- 必读根AGENTS、设计、本卡、原01与01R实现/审查；技能实际入口：`C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing}/SKILL.md`与`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/{executing-plans,test-driven-development,verification-before-completion}/SKILL.md`。版本按实际依赖，通用全量/提交例不沿用。
- 执行者只读Git，不暂存/提交/切分支/转派，不改治理；需要写集外路径先报PM，普通内部实现细节自行解决。

## Review Focus

1. 两玩家同时编辑、关闭/远离菜单、同位置换接口或燃烧室：旧提交不得写到新代际，停堆不依赖客户端显示可信。任务2真实会话验证。
2. 燃烧室先tick、仪表后tick以及反顺序：一份维护支付不能无限续燃或重复扣账，未提交核事务不得出票。任务1与3真实顺序验证。
3. 核热不足、停堆余热、特殊燃料耗尽交接：不部分扣热，外部燃料恢复原计时，核热不能无限超级加热。任务1与3验证。
4. 冷却堵塞/事故时小额维护：不能作为有效冷却暂停融毁或绕过原库存/损伤保护。任务1守恒与事故代表回归。
5. 最大14×14堆芯、长中文故障及低分辨率GUI：所有页停堆可见、选择信息可读、无图标挡字。任务2布局边界断言与客户端人工门。

## 精确允许写集

以下均相对候选根。同一执行者持有完整写集，分步自测后只交一次稳定组合候选；PM另写设计/卡/治理/候选说明。

现有文件：

- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomation{Service,Controller,State,Parameters,PortBlockEntity,BlazeBridge,Behaviours,Goggles,Network}.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/client/ReactorAutomationClientEvents.java`
- `src/main/java/com/iksxh/create_nuclear_industry/mixin/BlazeBurnerAutomation{Block,Entity}Mixin.java`
- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorServerTick.java`；必要时仅新重载及中文职责澄清的`ReactorThermalCalculator.java`。
- `src/main/java/com/iksxh/create_nuclear_industry/config/ReactorAutomationConfig.java`
- `src/main/java/com/iksxh/create_nuclear_industry/content/ReactorAutomationContent.java`
- `src/main/resources/assets/create_nuclear_industry/lang/{zh_cn,en_us}.json`：只本批管理员键。
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomation{Controller,Authority,Blaze,Data}Test.java`：只实际受影响断言，不能删保护证明。
- `build.gradle`：只新增本批GameTest类/同域模板的生产JAR排除；依赖、原域、生产构建流程不改。

新文件：

- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorMaintenanceHeat.java`：纯函数付热分配。
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationHeatSupply.java`：绑定、付热凭据与原燃烧室供能策略。
- `src/main/java/com/iksxh/create_nuclear_industry/automation/gui/ReactorAdministrator{Menu,Projection,Commands}.java`：菜单会话、只读投影、原子命令。
- `src/main/java/com/iksxh/create_nuclear_industry/automation/client/ReactorAdministrator{Screen,Layout}.java`：三页与共享可测试几何；Layout不依赖客户端类。
- `src/test/java/com/iksxh/create_nuclear_industry/reactor/ReactorMaintenanceHeatTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAdministrator{Commands,Layout,HeatSupply}Test.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ReactorAdministratorGameTests.java`
- `src/main/resources/data/cni_reactor_administrator/structure/administrator_empty.nbt`：原automation_empty字节相同测试别名，生产JAR排除。
- 证据仅`build/reports/reactor-administrator-03/**`；执行者报告仅`docs/reviews/2026-10-14/reactor-administrator-03/IMPLEMENTATION.md`。

不新增通用菜单/热工框架；若上述命名无法匹配真实API，先报告精确必要路径，PM扩卡后继续。旧源码英文异常文本不强制改协议，相关英文手写说明改中文。

## 任务1：真实维护热支出与连续换料

**接口合同：**

- `ReactorMaintenanceHeat.settle(ReactorSnapshot, ReactorFissionResult, double requestedHeatHu)`返回不可变Settlement：实际支付HU、新生支出HU、缓存支出HU、逐列维护移热map；0请求保持原行为，不足额全部返回0。
- `ReactorServerTick.advance(previous, parameters, coolantInput, double maintenanceHeatHu)`新重载；旧三参等价请求0。Result增加上述维护证据，原fission保持毛产热，原coolant/effectiveCoolingHeatHu仅真实冷却。
- 各列新生热按产热比例分配，缓存热按余额比例分配，确定性顺序最后列补误差。冷却分配从扣维护后的逐列剩余热按比例取得；Thermal总移热=维护+冷却，量化安全余数仅按实际冷却后余额计算。总账不多扣/重复扣。
- `ReactorAutomationHeatSupply.requestedHeat(owner,time)`只在当前实例绑定且管理员已正常点燃、非创造/有效SPECIAL燃料时返回配置费率；不能仅凭原生KINDLED方块状态自我续租。
- `commitPayment(owner,result,time)`只在原燃料端口/快照事务成功提交之后调用。失败/失联不得生成付热资格；凭据不持久化、不以L1/L2/墙钟续命。
- `burnerMaintenanceHuPerTick`默认1HU/t，新配置getter和参数入口完整校验。原毛功率目标与控制曲线不改，供热净额/缓存支出分别投影。
- 原Controller任意耗尽判断及Service前/后可用数下降撤权均改为可用数0；部分耗尽/补料更新可达功率，不撤销许可、不抹残余热。全耗尽仍FAULT，补料后人工复位/启动。

- [ ] 先写有意义RED：2列中1列耗尽仍许可，全部耗尽保持；维护1先从新生热扣、无新生时扣缓存、总热不足1则扣0；无管理员请求0原结果保持；维护不能算事故冷却。
- [ ] 在旧生产上取得可执行行为RED；新类不存在时允许先提供最小编译壳取得断言失败，不以编译失败或0用例算RED。
- [ ] 实现上述合同，核毛产热=维护+冷却+缓存变化及燃耗保持；只扩维护输入/结果，不重构事故状态机。
- [ ] 运行新维护、Commands/HeatSupply与原Controller，以及`ReactorServerTickTest`、`ReactorThermalCalculatorTest`、`ReactorMeltdownStateMachineTest`、`ReactorScramFissionRegressionTest`实际受影响代表。按失败/新改动再补定向，不自动全量。

## 任务2：三页GUI与服务端原子操作

**接口合同：**

- 菜单类型ID`reactor_administrator`在现有AutomationContent注册，客户端现有事件类注册Screen；普通空手右击当前绑定燃烧室打开，持燃料/扳手/机械臂保持原路径。读取真实`useItemOn`/继承空手入口后选定最小hook。
- `ReactorAdministratorMenu`使用无库存槽AbstractContainerMenu，服务端stillValid核当前玩家可达、非旁观、燃烧室/接口实际实例、owner代际及结构；关闭/失联释放查看者会话，不强载。
- `ReactorAdministratorCommands`采用APPLY、START、STOP、RESET四个明确动作。APPLY原子写`mode(MANUAL或CONSTANT)、depth(0..100)、heat(0..max)`，不自动启动；保存运行模式偏好与活动STOP分开，不能以STOP偏好代替启动操作。
- 运行中APPLY mode转换调用原安全交接，全插基准/红石降沿仍有效；只改同模式数值也校验预警与下一tick冷却。任一字段不合法整次不写。targetHeat0走安全停堆，不能留许可0目标。
- START独立进入原ARMING；STOP固定立即走原撤权/全插；RESET只解除健康稳定故障到STOPPED，不自动START。没有供能可查看/STOP，不可START/RESET。
- 请求字段含containerId、burner/port、ownerGeneration、authorityEpoch、settingsRevision、clientSequence与完整设置；服务端从当前menu/实际绑定取authority，不信任客户端任意owner。settingsRevision在成功设置变更递增，拒绝多玩家旧草稿；回包含accepted/reason及最新版本。旧四控件与旧网络入口撤下，不能留下绕过GUI会话的写入口。
- 三页Projection仅仪表/原snapshot/原观察和供热凭据派生；最大196格，字段数/字符串长度有界，不发送物品NBT或完整快照。打开给全布局，显示状态默认每10tick变化时更新，故障/代际失效立即更新，只有当前查看者接收。
- Layout约312×224，最大网格14×14，底部固定停堆；状态用文字+符号，堆芯点击只选中信息。直接输入与步进，应用确认后草稿清理；Esc丢未提交草稿。供热来源、剩余外燃料、毛/净与缓存支出不混写。
- 护目镜保留缩进首行及简短phase/actualHeat/主要原因，不再塞完整GUI数据或世界控件。

- [ ] RED证明多字段原子拒绝、旧revision/错实例/远距/关闭菜单拒绝、APPLY不启动、START与RESET不同、三页14×14几何不碰固定停堆区。
- [ ] 实现菜单注册、网络、投影、绘制及原四控件撤下，中文语言键同步英文；不引入新美术素材。
- [ ] 定向运行本批Commands/Layout与原Authority/Data/Blaze受影响合同。低分辨率实际GPU和14×14文字可读性留人工门，不用源码字符串检查代替GUI行为。

## 任务3：原燃烧室供能交接、真实验证与组合交付

- 正常外燃料点燃后才能取得核热值班；支付1HU维持KINDLED并暂停NORMAL剩余时间。供应不足时当tick回原燃料计时；无外燃料则停止供能。停堆后有真实缓存可继续付款，耗尽后停止。
- SPECIAL保持原计时/降级，有效SPECIAL时维护请求0，不能核热永久SEETHING；原创造燃烧室/未任职者不变。不要取消整个原tick、伪造remainingBurnTime或反复补满燃料。
- 单份付款资格只容忍BE顺序所需当tick/相邻tick，不跨暂停墙钟、不卸载续燃；不同owner/接口代际不复用。在服务端实际块状态/原renderer显示正常核热燃烧，客户端不能当authority。
- [ ] 新真实域`cni_reactor_administrator`少量代表组合：1）真实玩家开menu/并发旧稿/关闭和同坐标换接口；2）真实2列自然耗尽与原料端口补料持续运行、全部耗尽保持；3）真实燃烧室NORMAL/SPECIAL交接、足额核费/不足费、余热停堆与冷却堵塞、两种BE顺序/失联。不造第二种权威或只测纯fixture。
- [ ] 隔离`-PgameTestNamespace=cni_reactor_administrator -PgameTestDirectory=build/reports/reactor-administrator-03/game`，不覆盖用户run/saves/config。原模板原样别名；JAR仅精确exclude新增类和别名。
- [ ] 业务断言计数与自然保存exit0分别记录；保留RED/失败/超时及临时配置恢复。零用例不是通过，不终止用户进程。
- [ ] 稳定后一次必要`assemble`，完整备份原5e04包，冻结源SHA、受影响class/语言/JAR清单、生产无测试泄漏。实际新增/改动才触发重复增量包，不清理历史证据。
- [ ] 执行者交IMPLEMENTATION含各步、真实命令/计数/退出、技能实际使用、边界/剩余人工门和精确diff，不提交Git。
- [ ] PM安排原审查者一次组合规格/质量窄审；必要整改由同执行者一次收敛，同次差异复核。无必改后PM净提交候选并交用户本批集中复看，main不合。

## 人工集中复看与完成定义

候选保持原当前世界，用户从同一目录runClient：三页信息/14×14网格/小窗口、冷机投入一个普通燃料启动、核热维持与外燃料剩余暂停、停堆余热耗尽、部分列耗尽/自动换料不断许可、全耗尽保持、供能不足及红石故障不能自启、固定停堆与独立复位、原01R换接口与改装实际成型。未手测前只称内部验证及候选完成。

01/01R原保护/恒功率等未验项目仍在同一集中清单，已验收Logo/管网/美术不重开。本批客户端通过后才按原条件更新02到GUI接入卡；不自动开始02实现。

## 派发记录

PM自审：新需求已批准；GUI为用户指定的管理员特例，原通用无GUI规则不阻止本批；03是原01候选接续而非依赖01手测通过的02。已读真实原生燃烧室tick与当前Service三处燃料判定；SPEC和原安全交接无剩余玩法取舍。实际执行开始以Agent返回为准。

执行者已明确收到并接续同一候选，使用卡中技能和RED→定向→真实域→增量包流程；禁止Git写与转派。本条记录实际启动，不表示实现/审查/人工验收完成。
