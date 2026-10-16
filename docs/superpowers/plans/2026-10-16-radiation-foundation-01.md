# RADIATION-01：辐射基础与封存防护实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: 使用`superpowers:subagent-driven-development`或`superpowers:executing-plans`按复选步骤执行。本项目已有执行方式：唯一执行者连续实现与验证，结束后一次组合窄审；不逐步骤另派执行者/审查者。执行者只读Git，提交与核心文档由PM管理。

**Goal:** 让已用组件和乏燃料具有可测量的辐射，使防护、屏蔽和现有封存链产生实际作用。

**Architecture:** 真实燃料物品保存使用历史；服务端源索引只是可重建的只读投影，玩家剂量独立持久化。按区块查询附近源，复用结构边界并缓存外壳完整性，有限预算计算屏蔽和剂量；客户端只显示服务端结果。

**Tech Stack:** Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；JUnit5与现有独立GameTest启动入口，不新增依赖。

**Spec:** [已批准事故与辐射设计](../specs/2026-10-16-accident-radiation-design.md)第3、4、5.3、6.1、7节及第一批范围。当前结果/未闭门仅见[STATUS](../../STATUS.md)。

## Global Constraints

- 用户2026-10-16已批准整稿和推荐值；本计划只交付设计第8节第一批，第二批事故账本、世界破坏、残骸、事故废料及其封装配方不借此实施。
- 不改裂变、燃耗、热工、控制棒、管理员、管网、贫化铀装备平衡、旧配方或现有库存所有权；不增加设备GUI、序列装配、旧存档迁移或专属美术。
- 原料、全新组件和贫化铀材料/装备源强为0；只对已使用燃料、乏燃料以及其真实封装载荷计源。剂量为游戏单位D，不能写成现实医学单位。
- 锁定上述实际Gradle基线；手写注释使用中文，公开入口说明单位、线程/端边界和不变量。常量/配置使用有限数和范围校验。
- 本卡是管理文档，PM不代写实现/测试。执行者依次完成任务1～4，测试进程由同一执行者持有，审查只读差异与已有证据。
- 必用技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md`；编码前读superpowers同安装根下的`test-driven-development/SKILL.md`，交付前读`verification-before-completion/SKILL.md`。报告列实际入口和用途，治理5.1优先于通用重复全量流程。

## Review Focus

1. 首次裂变不足1点耐久、事务取消或仅邻列传热：只在成功提交的真实燃耗中标记；任务1的`fractionalBurnMarksOnlyCommittedFuel`与`heatWithoutBurnDoesNotMark`验证。
2. 同一物料经端口、机械臂、传送带、桶与嵌套容器流转：既不丢历史也不重复计源；任务2的`handoffCountsPayloadOnce`及`invalidCaskDoesNotEraseSource`验证。
3. 停机、缺燃料、堆内失效但外壳完好，以及远端区块未加载：屏蔽不跟运行状态混用、不强载；任务2的`containmentTracksShellNotRuntime`与`unloadedChunkIsNotRead`验证。
4. 死亡、非死亡克隆/换维、配置停用和其他来源同种药水：剂量有正确归属，不越权移除效果；任务3的`doseLifecycleAndDisabledRecovery`及真实效果用例验证。
5. 无人区库存、跨区块/世界重进、大量静止源与多玩家：不能因索引过期/队列延迟变成无辐射，不能重建整堆逐tick扫描；任务2/4的生命周期、计数器与固定场景对照验证。

## 1. 派发门与工作区

- 已批准需求来源：本设计整稿确认；执行方法沿用唯一执行者＋一次组合窄审。计划审阅后才派发，不重问数值/成本。
- 编制基准：main `89b3287`，已提交生产来源`850904df6220487f8903b00cc9936b8f68307d32`。派发时PM再记实际HEAD，文档增量不冒充生产改动。
- 独立树由PM优先复用合适的同级树，否则创建`E:/MyMC/NewMod/Create_NuclearIndustry-radiation`、分支`codex/radiation-foundation`。不复用仍待手测的贫化铀候选，不要求它先合main。
- 新批次开始前PM核实际客户端目录、保存退出证据，再完整携带指定世界（默认main的`run/saves`）；目标同名世界先完整备份，逐文件数量/哈希核对。派发记录来源、备份、分支/HEAD及既有dirty。世界不进Git、不开用户原件做自动化测试。
- 保留main无关dirty、已删除的正式JAR工作副本和全部候选证据；不顺手恢复、打包或提交它们。自动化使用独立`run/radiation-foundation-gametest/`。

## 2. 文件职责与允许写集

以下别名只缩短路径，均相对候选根：`J=src/main/java/com/iksxh/create_nuclear_industry`，`T=src/test/java/com/iksxh/create_nuclear_industry`，`R=src/main/resources`。未列路径只读，需要扩展先报PM。

| 写集 | 职责/限制 |
| --- | --- |
| 新增`J/radiation/`与`J/radiation/client/`下Java文件 | 仅本卡列出的标记、源/库存适配、屏蔽、剂量、事件、网络和客户端显示；不建立通用自动化框架 |
| 新增`J/config/RadiationConfig.java`、`J/content/RadiationContent.java` | 本批配置、四件防护服及辐射计注册 |
| `J/CreateNuclearIndustry.java`、`J/content/ModCreativeTabs.java` | 仅接本批注册/事件/网络与物品 |
| `J/blockentity/ReactorInstrumentPortBlockEntity.java`、`J/blockentity/ReactorPortBlockEntity.java` | 真实tick成功提交时标记、物品/结构变更通知；原有事务顺序和权限保持 |
| `J/reactor/FuelAssemblyItemCodec.java`、`J/reactor/FuelRefuelingTransaction.java` | 保留完整物品历史，防止当前版本正常提取/转换丢标记；不研究旧格式 |
| `J/structure/ReactorStructureLifecycle.java`、`J/storage/DryStorageBlockEntity.java` | 已知边界、生命周期/库存变更及护目镜简要屏蔽信息；不修改原结构合法性或架容量 |
| 新增`J/mixin/Radiation*.java`、`R/create_nuclear_industry.mixins.json` | 只允许本批原版方块变更通知、Create机械臂/带库存只读访问、放射性掉落伤害保护；不改第三方运输行为 |
| `R/data/create_nuclear_industry/recipe/crafting/radiation_*.json` | 恰好四件防护服和一个仪表的5条有序配方 |
| `R/data/create_nuclear_industry/tags/block/radiation_shielding.json`、`R/data/create_nuclear_industry/damage_type/radiation.json`、`R/data/minecraft/tags/damage_type/bypasses_armor.json` | 明确屏蔽名单与辐射伤害，仅追加本批身份 |
| `R/assets/create_nuclear_industry/models/item/radiation_*.json`、现有`lang/zh_cn.json`与`en_us.json` | 复用原版皮甲/原生仪表图标及防护服饰纹变体；本批玩家文案，不绘图 |
| `R/data/minecraft/tags/item/`中的`head_armor.json`、`chest_armor.json`、`leg_armor.json`、`foot_armor.json`、`enchantable/armor.json`、`trimmable_armor.json` | 只追加对应防护服，保留原版附魔/饰纹机制；不修改无关装备 |
| 新增`T/radiation/`，既有`T/reactor/FuelRefuelingTransactionTest.java` | 本批数学/事务/配置测试；不放宽旧断言 |
| 新增`J/gametest/RadiationFoundationGameTests.java`、`R/data/cni_radiation_foundation/structure/radiation_empty.nbt` | 独立真实游戏测试域 |
| `build.gradle` | 只加本批GameTest类/模板制品排除，沿用既有筛选属性，不改版本/依赖 |
| `build/reports/radiation-foundation-20261016/` | 执行报告、配置样例、测试/性能原始证据；其他正常Gradle产物可生成，不能提交 |

源文件拟按责任设置为`FuelUseHistory`、`RadiationParameters`、`RadiationItemSources`、`RadiationSourceIndex`、`RadiationSourceAdapters`、`RadiationContainment`、`RadiationDoseState`、`RadiationDoseCalculator`、`RadiationServerEvents`、`RadiationNetwork`、`RadiationExposurePayload`、`RadiationEquipment`和客户端`RadiationClientEvents`。同目录辅助record可按需要拆出，不借此重构原巨型仪表类。

## 3. 配置与跨任务接口

独立服务端文件`create_nuclear_industry-radiation-server.toml`。任务1产出不可变`RadiationParameters`，配置加载/重载均先校验完整参数组；非法组回退默认并节流告警，不让NaN进入结算。本批只注册辐射配置；事故专属配置随第二批实现。

| 分组与键 | 已批准默认值/约束 |
| --- | --- |
| `general.enabled`、`sampleIntervalTicks`、`detectionRadiusBlocks` | true、20 tick、32格；间隔正整数，半径1～64 |
| `sources.usedFuelDosePerSecond`、`spentFuelDosePerSecond`、`accidentWasteDosePerSecond` | 1、2、4 D/s，有限非负；保留4的参数定义但本批不注册事故物品/残骸 |
| `sources.maxFieldDosePerSecond` | 10 D/s，有限正数，在穿戴之前限幅 |
| `shielding.caskTransmission`、`rackTransmission`、`reactorTransmission`、`blockTransmission` | 0.05、0、0、0.05，各[0,1]；完整架/壳才适用对应比例 |
| `dose.safeAbsorptionPerSecond`、`maxDose`、`warningDose`、`symptomDose`、`damageDose` | 0.01 D/s、100、25、50、80 D；0≤warning<symptom<damage≤max |
| `dose.recoveryDelayTicks`、`recoveryPerSecond`、`damageIntervalTicks`、`damageHealthPoints` | 100 tick、1 D/s、40 tick、1HP；等待非负、伤害间隔正整数，其余有限非负 |
| `protection.head`、`chest`、`legs`、`feet`、`maxReduction` | 0.25、0.30、0.25、0.15、0.95，各[0,1] |

索引审计和射线预算属于技术限额：按公平游标分帧、合并同位置同屏蔽路径的源；实际限额及每次采样延迟写执行报告。未完成读数不得当成无源启动恢复或覆盖为0；标准性能夹具必须在默认20tick内完成一次采样，达不到即报告整改，不静默减源/扩大间隔。半径外、不加载区块不做读取。

接口冻结为以下职责（均服务端调用，返回值为只读值或独立副本）：

- 任务1：`FuelUseHistory.isUsed(ItemStack): boolean`、`markUsedCopy(ItemStack): ItemStack`；`RadiationConfig.parameters(): RadiationParameters`。
- 任务2：`RadiationItemSources.describe(ItemStack, RadiationParameters): ItemSourceSummary`，summary包含未屏蔽源强、施加自身容器屏蔽后的源强及是否含放射性载荷；不能只返回“合法/非法”。`RadiationSourceIndex.sample(ServerPlayer, RadiationParameters): RadiationFieldSample`，sample包含场强D/s、完成标记与采样gameTime；物品库存不是其权威数据。
- 任务3：`RadiationDoseCalculator.advance(RadiationDoseState, double absorbedDosePerSecond, int elapsedTicks, RadiationParameters): RadiationDoseState`；state含剂量D、安全等待tick、伤害计时tick。伤害/效果由`RadiationServerEvents`应用，纯计算器不访问世界。
- 任务3/4：`RadiationExposurePayload`仅S2C，含维度、采样gameTime、场强、累计剂量、吸收强度及样本是否完成；客户端无上报剂量/清空/设置源接口。

## Task 1：使用历史和参数边界

**Files:** `J/radiation/FuelUseHistory.java`、`RadiationParameters.java`，`J/config/RadiationConfig.java`，上述仪表/端口、codec与换料事务；`T/radiation/FuelUseHistoryTest.java`、`RadiationParametersTest.java`及既有换料事务测试。

**Consumes:** 原`ReactorServerTick.Result.fission().columns()`与端口真实栈；**Produces:** 第3节历史/配置接口，不改纯热工快照的燃料所有权。

- [ ] 写回归用例并运行定向JUnit确认失败：`fractionalBurnMarksOnlyCommittedFuel`要求正燃耗但damage仍0时标记；拒绝整tick提交时原物品不变；`heatWithoutBurnDoesNotMark`要求仅缓存热/传热及全插棒无燃耗不标记。
- [ ] 实现标记：在真实物品的`DataComponents.CUSTOM_DATA`内保留命名空间子项`create_nuclear_industry:FuelHistory`、布尔`Used`，不覆盖其他组件。仪表准备提交时同时读取本次裂变结果，只有正燃耗且整tick正式提交成功才写入新栈；不能每tick把所有装入物品一律标记。
- [ ] 补真实栈取出、机械臂往返、枯竭转换和快捷换料测试：原件保留历史/附魔/名称，换入新件不继承。当前正常世界的有物品提取不得走重建物品而丢组件；无原件的纯模拟辅助入口不成为生产库存来源，不为历史迁移扩写。
- [ ] 实现参数与重载整组验证，覆盖负值、NaN/Infinity、阈值乱序、比例越界和极端间隔。配置停用不清除标记。
- [ ] 跑任务1定向测试及既有`reactor.FuelRefuelingTransactionTest`、`reactor.ReactorServerTickTest`；RED/GREEN证据写报告。数学测试不启动客户端。

## Task 2：源索引、物流、屏蔽与掉落保护

**Files:** `J/radiation/RadiationItemSources.java`、`RadiationSourceIndex.java`、`RadiationSourceAdapters.java`、`RadiationContainment.java`、相关`Radiation*.java`Mixin及其配置、仪表/端口/结构/架通知、屏蔽标签；`T/radiation/RadiationItemSourcesTest.java`、`RadiationSourceIndexTest.java`、`RadiationShieldingTest.java`，本批GameTest中的真实库存/保护用例。

**Consumes:** 任务1历史与配置；**Produces:** 第3节物品分类/附近场强接口，以及只有真实载荷才受保护的掉落判断。

- [ ] 写并运行定向失败用例：新组件/原料/DU为0，已用1、乏燃料2；距离2格为1/4，半径外0；16个合法桶各2×0.05 D/s而不是32份载荷。`invalidCaskDoesNotEraseSource`要求含乏燃料的非法桶按裸载荷计，空桶0；普通潜影盒套桶只施加一次桶屏蔽。
- [ ] 做只读库存适配清单：玩家主/副手、背包/装备与原版随身容器组件；ItemEntity；原版箱子/陷阱箱/木桶/潜影盒/漏斗；本模组换料端口、离心机、烧结炉、装配台、干式架；Create传送带、置物台、机械臂、工作盆及物品保险库。其他公开设备库存记录实际可读入口，私有背包/远程网络不作通用保证。嵌套读取使用显式栈和分帧预算，不因深度/预算未完成返回0或递归溢出。
- [ ] 核Create6.0.10实际源码：带从`BeltBlockEntity`控制端的`BeltInventory`读取真实运输栈，机械臂`ArmBlockEntity.heldItem`需只读Accessor，置物台`getHeldItem()`可直接读；不能在同一时刻又按segment capability把同份栈重算。保险库只读控制库存，本模组多方块只读唯一所有者，不枚举六个面后直接相加。
- [ ] 建每维度/区块索引；区块加载/卸载、实体移动/离开、真实库存变化更新，普通容器/物流缺少变更通知时用附近已加载库存公平低频审计。索引不写回源库存；源一旦迁出检测区不留下永久幽灵。`handoffCountsPayloadOnce`覆盖端口→臂→带→箱→桶→架及移除/同坐标新BE身份，不丢标记、不双算；玩家主/副手和装备已属于其库存，不能额外再算。新加入玩家要能发现已加载静止容器。
- [ ] 外壳缓存保存已验证结构的边界与完整性派生信息，不保存第二份燃料；来源强度仍逐份读真实栈。燃料端口按堆内对应列位置计源，不能因为端口位于壳面而把载荷错误当外部；端口源与堆聚合源只选一份。持久化已知边界，使正常重进及后续仪表损坏后仍能按同一壳体判断；后续事故复用它，不在本批预建事故任务。
- [ ] `containmentTracksShellNotRuntime`覆盖完整壳停机/缺料/堆内机构无效仍对外0、壳有真实破口取消整壳屏蔽、补全恢复、堆内不屏蔽。初次形成/载入可有界核壳，之后仅真实相关方块变化使缓存失效；方块变化通知要覆盖玩家、爆炸及命令，不仅监听取消前的BreakEvent。`unloadedChunkIsNotRead`证明只读已加载块、没有强载；不确定完整性不能凭空授予整壳屏蔽。
- [ ] 路径用有界体素射线读取明确标签中的屏蔽混凝土/反应堆外壳/观察窗，每穿一格乘0.05；同一格不重复叠乘。先自身容器和路径屏蔽，再求和限10 D/s。完整架合法载荷对外0；移走桶后场强随载荷改变，护目镜首行沿用既有缩进工具，图标不挡字。
- [ ] 给真正含放射性载荷的ItemEntity阻止自然消失、火/岩浆、仙人掌及爆炸伤害；含载荷的封装桶同样保护。新组件/空桶/普通物品保持原版行为，正常拾取和Create搬运不受阻，管理员/创造删除不拦截。真实用例区分ItemExpire、伤害与拾取三条路径。
- [ ] 跑任务2定向JUnit，真实用例先纳入同一批GameTest，最终集中启动一次。报告附支持矩阵、缓存失效/索引清理入口和工作量计数，不能以JSON存在替代物流验证。

## Task 3：玩家剂量、症状与防护物品

**Files:** `J/radiation/RadiationDoseState.java`、`RadiationDoseCalculator.java`、`RadiationEquipment.java`、`RadiationServerEvents.java`，`J/content/RadiationContent.java`及注册/创意栏入口，辐射伤害类型/标签、五配方/物品模型/护甲标签；`T/radiation/RadiationDoseTest.java`、`RadiationEquipmentDataTest.java`，本批GameTest玩家用例。

**Consumes:** 任务2完整场强样本、任务1参数；**Produces:** 持久化玩家剂量和第3节纯计算接口，四件皮甲属性防护服及手持仪表身份。

- [ ] 写并跑失败测试：1 D/s连续25秒达预警，50秒进症状，80秒进伤害区；同一场强下全套吸收5%。安全阈值0.01两侧及等待100tick前后各测，离线不计时，配置关闭停止暴露伤害并按安全区恢复；已完成样本才推进安全等待。
- [ ] 服务端每20tick结算，以在线实际tick换算秒，剂量限0～100。用玩家NeoForge持久化子项`create_nuclear_industry:RadiationDose`保存剂量和两种计时进度；非死亡克隆/换维保留，死亡清零。只读客户端不拥有剂量，创造/旁观不累积伤害剂量但仍可测真实场强；该状态不依赖持有仪表。
- [ ] 25D预警，50D短时刷新虚弱I/缓慢I，80D每40tick扣1HP；伤害计时只在伤害区累计，离开伤害区复位，不积攒低剂量时期的补扣伤害。辐射伤害类型绕过普通护甲，防护只减吸收。不主动清除其他来源同种效果；牛奶不改剂量。`doseLifecycleAndDisabledRecovery`和真实药水/伤害用例分别覆盖持久化及效果边界。
- [ ] 注册`radiation_helmet`、`radiation_chestplate`、`radiation_leggings`、`radiation_boots`和`radiation_meter`。防护服皮甲属性、耐久和皮革修复；只认对应穿戴槽，头/胸/腿/脚减0.25/0.30/0.25/0.15，合计限0.95。持有/装背包不叠加，不减少携带源对旁人的场强。
- [ ] 五条有序配方：L=皮革、P=现有铅板；头`LLL/LPL`，胸`LPL/LLL/LLL`，腿`LLL/LPL/L L`，脚`L L/LPL`。仪表竖排`S/C/G`，S=现有辐射传感器、C=Create黄铜外壳、G=玻璃板。全套恰24皮革＋4铅板，仪表各1，不改变既有传感器制造。
- [ ] 沿原版皮甲模型/穿戴贴图及饰纹显示；仪表先复用原版钟物品外观，不加时间用途。不绘制新资源。数据测试核5配方及实际装备属性/修复/防护槽位，新增标签只追加本批身份。
- [ ] 跑任务3定向JUnit；正常保存重进的剂量、使用标记与屏蔽恢复放入同一集中游戏验收，不另造旧存档格式矩阵。

## Task 4：提示、真实验证与交付

**Files:** `J/radiation/RadiationNetwork.java`、`RadiationExposurePayload.java`、`J/radiation/client/RadiationClientEvents.java`及必要只读客户端状态，语言文件、本批GameTest/模板、`build.gradle`制品排除；`build/reports/radiation-foundation-20261016/REPORT.md`。

**Consumes:** 任务3权威结果及物品；**Produces:** 无独立GUI的仪表/预警，以及可供PM审查和用户集中手测的候选证据。

- [ ] 实现仅S2C必要数值包。主手/副手持仪表时1Hz显示场强D/s、剂量D、吸收D/s；两手同时持有不双发/双画。客户端换世界、断线清缓存，旧维度/过期采样不覆盖新世界；服务端不接受客户端改剂量。专服启动不得加载客户端类。
- [ ] 玩家文案：已使用组件标出使用状态，乏燃料/桶说明放射性和屏蔽；无开发措辞或现实剂量保证。进入暴露、跨阈值各一次提示，计数声每次采样最多一次、脱离/收起仪表停止；不要每tick聊天/全量库存同步。界面布局避开Create护目镜、手持物和原版状态信息。
- [ ] 定向JUnit命令使用`./gradlew.bat test --tests '*radiation.*' --tests '*reactor.FuelRefuelingTransactionTest' --tests '*reactor.ReactorServerTickTest'`；前面已验证且实现未变的结果复用，不强制重跑。新增`RadiationFoundationGameTests`限定域`cni_radiation_foundation`，命令`./gradlew.bat runGameTestServer -PgameTestNamespace=cni_radiation_foundation -PgameTestDirectory=run/radiation-foundation-gametest`。用现有模板/注册模式；玩家库存夹具优先FakePlayer，不伪造未完成Create握手的登录连接。
- [ ] 一轮真实服覆盖：真实小数燃耗/提取标记、代表性嵌套库存与Create带/臂/置物台运输、合法/非法桶屏蔽、完整壳/破口/补壳、原掉落保护/拾取、真实穿戴/伤害/效果以及配置重载。人工不方便稳定断言的显示和多人体验留手测，不能写成自动通过。记录断言、维度保存和进程exit分别结果；若保存退出异常保留证据，不反复启动同一批业务。
- [ ] 在同一隔离测试场景记录功能启用前后：无堆、16³空闲/已用燃料堆、满架、4个玩家在半径内；分别记录服务端tick采样分布、库存读取/射线格数/最大排队延迟与S2C次数。完整堆/满架屏蔽应提前剪枝，无变化时不反复扫描整壳。不得杜撰历史基准；无法获得真实多人进程时标为待多人观察，不能拿4次纯函数调用冒称4玩家实测。
- [ ] `./gradlew.bat assemble`增量打包；核JAR不带本批GameTest类/模板，登记源码、字节数和SHA256，只交候选。若公共燃料事务/结构生命周期改动引出受影响旧测试失败，保留失败并扩大相关回归；不机械跑所有Ponder/全部GameTest，不删断言凑通过。
- [ ] 交一份`REPORT.md`：精确写集、技能、接口/配置、支持矩阵、实际命令/失败整改、制品身份、性能对照和手测入口。PM随后做一次组合窄审；整改只复核实际修改及对应证据，执行者不提交Git或宣布人工验收。

## 4. 集中手测与完成定义

候选交付后用一张清单验收：

1. 新组件读数0；实际运行后即使耐久显示未变也有辐射；取出、臂/带搬运、再装入和正常重进保留。换入新组件仍为0。
2. 裸露乏燃料、普通箱/潜影盒、合法桶、完整架依次比较；桶/架移走后现场读数跟随真实物料。全新/DU装备不放射，防护服只保护穿戴者。
3. 完整反应堆对外屏蔽；停机不破坏屏蔽。可安全操作的壳破口与补壳能改变读数，不以强制事故测试替代；保留原液体/控制棒显示与热工。
4. 裸露逐步预警/症状，离开后恢复；防护服降低吸收、满套仍非零。核主/副手仪表、中文、提示/声音节流和护目镜无遮挡。
5. 剂量中途正常保存重进可续接，离线不恢复；联机观察手持源对旁人的影响。16³现场看向/背向和开GUI分别观察新增卡顿，自动性能记录不能代替体验。

完成定义：本卡合同的自动证据、一次组合审查及本批人工项各自闭合，PM再按已有授权管理合入/正式包；当前仅编制计划，不登记实现/手测通过。第二批事故与清理在此基础稳定后另编精确卡，不交付没有封装出口的废料。

## 5. 计划自审

已按设计逐节核对：第3/4/5.3/6.1及第7节辐射参数对应任务1～4；第5.1/5.2/6.2的事故动作、账本、废料封装和事故专属参数明确留第二批。五项Review Focus均落到具体测试/证据；接口单位和S2C边界一致。没有新增成本/源类别/永久污染，未把贫化铀或其他未反馈手测追认为通过；本次文档检查不运行Gradle或用户世界。
