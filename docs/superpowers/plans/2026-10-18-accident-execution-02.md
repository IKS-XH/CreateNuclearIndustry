# RADIATION-02：局部事故、残骸与回收封存实施计划

> **For agentic workers:** 使用`superpowers:subagent-driven-development`或`superpowers:executing-plans`按步骤实施。项目既定方式为唯一执行者连续实现与验证，最后一次规格/质量组合窄审；不按每个步骤重复派发或审查，执行者无Git写权限。

**Goal:** 融毁后留下有界损坏与真实放射性残骸，玩家可经既有装配台和贮存架完成清理。

**Architecture:** 既有不可取消融毁事件只触发唯一事故接管。每维度SavedData保存独立事故账本及冻结参数；真实组件先进入可续接托管，再按预算转为残骸载荷或返还。世界动作不依赖尚存的仪表端口，辐射读取真实载荷，封装复用已有单件桶而非另建废物链。

**Tech Stack:** Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，现有JUnit/隔离GameTest入口，不加依赖。

**Spec:** [已批准事故与辐射设计](../specs/2026-10-16-accident-radiation-design.md)第5、6.2、7、8.2节。用户已批准整稿和按批派发，2026-10-18确认首批及头罩手测通过并要求继续；不重问已冻结数值。当前结果见[STATUS](../../STATUS.md)。

## Global Constraints

- 第一批与头罩及最新main合入为前置，准确基线和工作树由PM在本卡派发记录补齐后才允许写源；用户原件/第一批候选、失败证据和main无关dirty保留。
- 不改裂变、燃耗、热工、倒计时阈值、管理员常态控制、工业管网、已验配方成本或外观；只增加事故接管与必要封装类型支持，不实施压力管网、永久区块污染、再处理或教学。
- 所有新手写说明中文；公开状态/交接入口说明服务端、单位、载荷所有权、失败/恢复与幂等条件。当前格式正常保存加载必须测，旧存档迁移不研究。
- 每个已用/原乏燃料单件组件恰好1废料，不按棒高、热量或源强复制；全新未用组件原件返还。冷热工质100%事故损失，热量记录释放/耗散，不生成可回收世界流体。
- 外伸默认2格、壳/外部合计最多32块；冲击最多8点生命值，每玩家每事故最多一次；每维度所有事故共享每tick最多64个世界动作。堆内动作受原结构体积上限约束，不能调用大范围原版爆炸或强载区块。
- 事故废料源强用首批已存在`accidentWasteDosePerSecond`默认4D/s；桶0.05/完整架0传输、玩家剂量/恢复、防护服和头罩功能保持。无实际载荷的空残骸不放射。
- 必用实际技能`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-ci-release/SKILL.md`；资源读取`minecraft-resource-pack/SKILL.md`，编码前用TDD、交付前用完成核验。治理5.1优先于通用全量/多层流程。

## Review Focus

1. 事故在正式tick发布后，该tick后半段不能再用旧result恢复功率、管理员供热/许可、遥测或库存；危险拆除的多所有者事务失败/取消不能留下可执行的孤立事故。任务1的`handoffStopsSameTickAndCanceledBreakDoesNotEnqueue`覆盖。
2. 事件快照只是投影，物理换料口才是组件所有者；小数已用、新组件、原乏燃料、已空端口各不混淆，托管/端口/残骸不能同时有可提取副本。任务1/2的`custodyConservesPhysicalStacksAcrossReplay`覆盖。
3. 仪表先被破坏、远端区块未加载、重复事件及正常保存后在中间动作恢复，不能复产/丢料或按位置误吞新建反应堆。任务2的`ledgerResumesWithoutInstrumentAndWithoutChunkLoads`覆盖。
4. 同时多事故、已毁/不可破坏目标、相邻独立机组及配置重载时，预算/破坏/冲击不乘机组数重置，旧任务按冻结规则。任务2的`sharedBudgetFrozenRulesAndBoundedDamage`覆盖。
5. 新废料经带/臂/普通箱、装配台中途停恢复、桶/架和残骸采掘后只计一次源；原乏燃料封装/制造模式保持。任务3的`wasteSealingAndSourceOwnershipStayAtomic`覆盖。

## 写集与职责

以下`J=src/main/java/com/iksxh/create_nuclear_industry`、`T=src/test/java/com/iksxh/create_nuclear_industry`、`R=src/main/resources`，均在本批候选树，未列路径只读。

| 允许范围 | 唯一职责/限制 |
| --- | --- |
| 新增`J/accident/` | 下列参数/账本/计划/执行/残骸职责，可按文件拆小，不创建无关通用框架 |
| 新增`J/config/AccidentConfig.java`、`J/content/AccidentContent.java` | 独立SERVER事故配置；注册`reactor_corium`方块/实体及`accident_fuel_waste`物品，借用原版表现 |
| `J/CreateNuclearIndustry.java`、`J/content/ModCreativeTabs.java` | 仅事故注册、配置/事件接线和创意栏入口 |
| `J/blockentity/ReactorInstrumentPortBlockEntity.java`、`J/blockentity/ReactorPortBlockEntity.java` | 事故锁/持久化交接、即时撤销热工及交易/管理员许可、按真实物品托管；原正常路径保持 |
| `J/reactor/ReactorMeltdownEvent.java`、`J/reactor/ReactorMeltdownEvents.java`、`J/structure/ReactorStructureLifecycle.java` | 既有两事故原因/去重与多所有者交接；不另起倒计时或篡改原安全拆除语义 |
| 新增`J/mixin/ReactorBreakFinalizationMixin.java`、`R/create_nuclear_industry.mixins.json` | 仅锁定NeoForge CommonHooks.fireBlockBreak的BreakEvent最终post返回值接线：LOWEST只读预检，最终未取消才提交原多所有者事务/事故；失败取消并由原生流程通知客户端，不改其它方块拆除 |
| `J/radiation/RadiationItemSources.java`、`RadiationSourceAdapters.java`、`RadiationContainment.java`、`RadiationServerEvents.java`、`RadiationSourceIndex.java` | 废料/残骸真实源、变更失效与既有掉落保护拓展；索引仅按事故ID＋列ID读取本维度账本真实非空托管载荷，定位原列底部并在移交时失效，不扫描全堆或更改剂量算法/半径/常态采样预算 |
| `J/storage/SpentFuelPayload.java`、`J/production/ShieldedAssemblyRecipe.java`、`ShieldedAssemblyBlockEntity.java` | 桶校验/封装支持一种新载荷；现有配方选择从仅一条sealing扩为两条已知sealing，按实际槽位/配方ID锁定，不能串配/复制工序 |
| `R/assets/create_nuclear_industry/lang/{en_us,zh_cn}.json`、新增对应corium方块/物品模型及blockstate、`models/item/accident_fuel_waste.json`，`R/data/create_nuclear_industry/loot_table/blocks/reactor_corium.json`（如原生掉落接线需要） | 仅本批玩家措辞与可加载原版占位；不绘新贴图/动画，不改既有资产 |
| 新增`R/data/create_nuclear_industry/recipe/shielded_assembly/sealed_accident_fuel_waste.json` | 1废料＋1既有固化基材＋1空铅桶→单件既有封装桶，12800工作量；沿用原料标签与真实载荷 |
| 现有`R/data/minecraft/tags/block/mineable/pickaxe.json`（可新增） | 只补残骸可用普通镐回收的原生标签，`replace=false`，不引入新清理装备成本 |
| 新增`T/accident/`、既有`T/radiation/RadiationItemSourcesTest.java`与`T/production/ShieldedAssemblyStateTest.java` | 账本/计划/参数/幂等、源/封装回归；需要其他原断言调整先报具体文件及被新合同取代的内容，不删除旧正常回归凑通过 |
| 新增`J/gametest/AccidentExecutionGameTests.java`、`R/data/cni_accident_execution/structure/accident_empty.nbt`、`build.gradle` | 一个隔离定向测试域及制品排除；不改版本、依赖、原排除或启动器通用行为 |
| `build/reports/accident-execution-20261018/` | 唯一报告/临时核验程序/原始日志；正常编译输出允许，不进Git |

## 任务1：唯一接管与真实载荷账本

**文件/接口：** `ReactorAccidentLedger`保存事故ID、堆世代、维度/边界、原始工质/HU账、逐列真实组件及动作收据；`ReactorAccidentService.begin(ReactorMeltdownEvent)`幂等返回本事故。`ReactorAccidentSavedData`每维度保存/加载全部未完成及去重所需记录，不能持有强世界引用。仪表提供只在服务端原所有者核验成功的托管/锁定入口；具体内部签名由同一执行者保持一致并在报告列明。

- [ ] 先写失败用例：小数已用组件产1废料，未用组件完整返还，原乏燃料只转一次；空列/棒高不增量。保留物品数据，事件投影不能当作第二份物品。用正常保存/重载及逐阶段重复调用核守恒。
- [ ] 接既有COUNTDOWN_COMPLETE与DANGEROUS_DISASSEMBLY，不改触发阈值/原子发布。建立可持久化任务后才有不可逆动作；真实端口→托管→残骸/返还每次只有一个可访问所有者，记录任务/列/action身份与收据，用于当前格式正常恢复。
- [ ] 立即锁定该堆，撤销旧流体/换料/管理员运行许可；正式tick事件返回后不得继续发布旧运行结果或核热付款。重登/重扫不能把事故堆自动恢复运行，事务失败/取消不得遗留事故任务；不破坏原安全停机拆除/维修及其它机组。物料/热量已完成事故接管的残余结构须可正常清理，不能因旧COMPLETE快照或“尚未完全停机”永久锁死残骸；加入`settledAccidentDebrisCanBeDismantled`用例。
- [ ] 工质与HU原数值入账且只扣一次，工质不喷成可回收流体；不可获得端口/区块时保留等待，不用猜测物品、强载、跳过即当已完成或无限重复查找。

## 任务2：有界分批动作、冲击与残骸

**文件/接口：** `ReactorAccidentPlan`冻结确定性目标和参数；`ReactorAccidentExecutor.tick(ServerLevel)`共享维度动作预算；`ReactorCoriumBlock`/`ReactorCoriumBlockEntity`保存事故/列身份和单件真实废料载荷。`AccidentParameters`默认外伸2、外部破坏32、冲击8、世界动作64及破坏开关true；`AccidentConfig`文件`create_nuclear_industry-accident-server.toml`加载/重载原子校验完整参数组，非法回退默认并节流告警。

- [ ] 先写失败用例：事件重放不重复任务；处理中保存加载、拆仪表、跨区块部分不可用仍可续接；多任务每tick合计≤64动作、不可破坏格不修改，已经执行动作重放无第二份产物。
- [ ] 确定性计划处理原堆内部机构，在每个有废料的燃料列底部放单件载荷残骸；只有壳/外部破坏计入32格上限，目标不超外伸边界。不能复制高度、原方块库存或掉落；无法安放时托管继续保留，不销毁不丢弃。
- [ ] 普通采掘取回残骸内真实废料，残骸本体不额外复制载荷；空残骸不凭名称放射/产废。无GUI，不新增专用清理耗材；占位外观直接引用原版模型/材质。
- [ ] 每事故每玩家最多一次、按距离衰减且≤8生命值的近距冲击，标记随任务保存，不在分批/重载后重复补伤；配置关闭壳/外部破坏同时关闭该冲击，但内部报废、载荷和工质损失仍执行。冻结参数，不让中途重载改产物/预算收据。
- [ ] 仅对当前可用区块执行并保留等待队列，轮转不饿死其它已加载任务；工作预算包含本批实际世界变更及返还，不能一个动作内部再无界批量放物品。邻堆不被直接设为融毁，实际损伤由其原热工独立判定。

## 任务3：废料源与既有封存链闭环

**文件/接口：** `accident_fuel_waste`为单件物品，源强读取原辐射参数；残骸适配器只读取其真实载荷。`SpentFuelPayload.isValid/seal`仅接受一个数量1的原乏燃料或事故废料，依旧是单一封装所有者。装配台按配方ID/三槽实物挑选并锁住正确sealing配方，制造/原封装保持。

- [ ] 先写失败用例：残骸→掉落→装配台→桶→架全程载荷守恒/源不双算；废料栈数、混装/空载荷/任意异物拒绝；原乏燃料封装及制造仍通过。
- [ ] 加注册、配方、双语玩家文案及原版占位资源，不改原12800工作量、桶材/固化基材或架容量。制造模式不能误吞废料，改配方/原料或中途停恢复不沿用另一批次已付进度产生免费桶。
- [ ] 复用原物流/容器/掉落保护：废料和封装真实载荷不自然消失、不受原版火/岩浆/仙人掌/爆炸删除；创造/管理员/第三方虚空边界保持。使用现实所有者变化通知源/壳缓存，不增全堆逐tick扫描。

## 任务4：一次必要实机验证及候选交付

- [ ] 定向JUnit：`./gradlew.bat test --tests '*accident.*' --tests '*radiation.RadiationItemSourcesTest' --tests '*production.ShieldedAssemblyStateTest' --tests '*reactor.ReactorMeltdownEventsTest' --console=plain`。按风险补既有安全拆除/当前格式续接用例，不跑全build或强制重跑。
- [ ] 唯一隔离真实服：`./gradlew.bat runGameTestServer -PgameTestNamespace=cni_accident_execution -PgameTestDirectory=run/accident-execution-gametest`。覆盖真实小/16³、危险拆除、取消/拒绝拆除、两原因去重、停旧句柄/管理员、已用/全新/原乏燃料、残骸采掘与12800工作封装/架屏蔽、相邻机组/边界/不可破坏、关闭破坏配置、处理中SavedData及区块事件。按现有夹具操作真实库存/机器，不把纯函数调用当实机。
- [ ] 当前格式正常停服/再进与真实区块不可用恢复用受控独立世界证明；未做真人多人和实际ChunkEvent.Unload的部分标未测，不把hasChunk=false夹具冒称原生卸载。测试断言、保存与exit分别登记；退出停滞保留证据，按治理处理本批自有进程，不重跑相同业务矩阵。
- [ ] 记录16³一次事故实际计划大小、每tick动作峰值/耗时、载荷总数、全局动作/外部破坏/冲击收据；不造新性能基准承诺，不扫描或制造用户世界事故。
- [ ] 一次增量assemble，核新事故配置/物品/资源及全部入包class匹配来源，本批GameTest类/模板/域排除。已确认第一批原证据按未变实现复用，新增公共交接风险在上面定向场景覆盖。
- [ ] 执行者交唯一`REPORT.md`、原始失败/命令/exit与制品身份；PM派一次组合窄审，整改只复核实际差异。没有人工验收宣告、Git写、合main、正式包替换、推送或发布权限。

## 集中人工门及后续

候选内只用测试世界完成一次“触发事故→看有界破裂/停机→穿防护靠近→采掘废料→装配台封装→入架→现场读数下降”的连续体验；再复看处理中正常保存重进、旧正常反应堆/管理员及原乏燃料封装保持。通过前不派第三批教学/美术或其它辅助项目。缺口/玩法变更报PM，不能自行发明处理成本或无损清除废料。

## 计划自审与派发记录

第5.1/6.2的唯一接管、物料/工质账、跨区块进度归任务1/2；第5.2的回收/封装同批归任务3，原5.3掉落保护复用并新增类型覆盖；第7节全部第二批数值在任务2冻结，源强沿首批已有配置。五项Review Focus分别有对应失败用例/真实场景。没有新增永久污染、额外燃料链、现实剂量承诺或旧存档迁移，原第一批人工结果不会代替本批事故体验。

首批准确生产基线`f819fcfadf75509c7b23899daa9a7b9c59dbc56b`，正式包3478572 bytes、SHA81E7CD98D589DDC9C0AAF398C06776DC4318EA54C70CD19376F0C127E4A3DE7B已核。PM从包含该来源/正式包/本卡的main创建`E:/MyMC/NewMod/Create_NuclearIndustry-accident`、分支`codex/accident-execution-02`，实际创建HEAD和携带清单写入本树报告。按默认约定完整携带已保存main世界27文件/13337721 bytes并核冻结SHA，用户原辐射手测世界不动；不重复扫描或制造用户原世界事故。

唯一实现/构建执行者按本卡连续完成任务1～4，不向子Agent转授任务。精确派发与实际开始以工具返回和STATUS登记，不因本卡存在就预称已运行。结束后一次独立组合窄审，再停本批集中人工门；源/正式包仅PM管理。

实际派发：新树从main `b39d11690fdbd8861e5d5fa7d06efa7d53d9e8de`创建，初始干净，携带世界与上述冻结清单27项全同，`PREFLIGHT.json`已写候选报告；唯一执行者`/root/accident02_impl`、`gpt-6.1-sol`/xhigh已启动（跨库存/事件/持久化与世界动作提高思考强度，不改变权限）。主分支47项原dirty及已验rad手测原世界保护记录保留。

执行中补齐（2026-10-18）：真实组件由SavedData托管以保证仪表先毁及分批等待期间不丢料，因此精确追加RadiationSourceIndex.java既有采样入口。托管期间按唯一真实所有者计源，转入残骸/返还后原源立即失效；仍遵守原已加载源及跨帧预算规则，不强载区块。此为既定物料/辐射连续性的必要接线，不新增玩法参数。

拆除最终判定补齐（2026-10-18）：NeoForge21.1.219没有MONITOR，单独LOWEST监听不能得知后续同优先级取消。追加上述两处原生post返回接线，不能只列未支持边界。验证直接调用CommonHooks.fireBlockBreak，覆盖后注册LOWEST取消、拒拆、二所有者事件监听抛错；最终取消不得留下事故任务或撤销原运行权限。接管准备同时按完整计划燃料列坐标/代际核实真实物理端口，不能把暂未加载或漏绑定视为空列。
