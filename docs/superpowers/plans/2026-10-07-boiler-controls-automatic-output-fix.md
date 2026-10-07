# EXT-B-BOILER-REWORK-01B：压力控件、自动汽种与冷液连续输出整改

> **2026-10-08收尾：** 用户确认最终01F精简手测通过；本卡继续适用的锅炉规则已随REWORK-01～01F[验收合入main](../../reviews/2026-10-08/boiler-rework-01/ACCEPTANCE.md)。被01F替代的整炉瞬时切种不再要求复测。下方候选、失败及待验收描述保留历史含义，不作为当前人工门；不扩展为所有动力场景或首发验收，不自动接其他主线/教学。

> **执行技能：** 使用superpowers:subagent-driven-development，一名执行者持有本次控件与共享账本修改，一次规格/质量合并审查。治理5.1的定向验证、PM持有Git和既有人工门优先于通用技能的重复全量、执行者提交或清理步骤。

**目标：** 修复控制器操作后冷液管路停流，移除汽种开关，按实际汽温与炉压自动选择汽种，并交付0～100逐整数的出汽压力下限控件和准确遥测。

**架构：** 整炉沿用唯一BoilerState账本，保留既有已付HU、结构与逐口额度。控制器只提供一个Create原生数值控件；结构生命周期撤销全部能力，压力调整不撤销冷液等能力，汽种变化仅更新蒸汽运输所需状态。

**技术栈：** MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。

**规格与授权：** 用户2026-10-07六项手测反馈及明确选择“按实际汽温和炉压自动决定：达标出超临界蒸汽，否则出蒸汽”。这是对[原方案](./2026-10-07-high-pressure-boiler-rework-proposal.md)手动两汽种及“不自动切换”规则的替代；01A底层热口几何规则保留。无需重复确认。

**状态：** 实现、定向验证及一次合并审查通过，已保存候选功能快照`32e6f897c03d8d8813f776dd4702bdf5cfe87b7f`。实施基线候选HEAD `7b263d7`，主工程HEAD `8a5f598`；锅炉仍未人工验收或合入main。本卡并入[现有锅炉人工门](../../reviews/2026-10-07/boiler-rework-01/CANDIDATE.md)，不派发下一主线或教学。

## 已冻结规则

1. 移除模式枚举、汽种行为、手动selectMode及两套保压状态。压力唯一设置为**出汽压力下限**；控件、数值板与护目镜用0～100的百分数，逐整数设置，包括0、17、43、70、100。内核归一值为百分数/100，不强制跳回10或60、不因超临界资格将设置钳到50。
2. 默认压力下限沿用此前默认运行设置60%，统一配置键`outputMinPressure=0.6`，合法归一范围[0,1]；替代normalOutputMinPressure/supercriticalOutputMinPressure。显式机器值保存且不被默认覆盖。设置高于安全阀开启线仍合法，含义是正常出口不能达到下限时停止出汽，不能据此改变安全阀阈值或判整炉配置非法。
3. 有已付汽化热且存在蒸汽时，实际汽温达到supercriticalTemperature、炉压达到supercriticalPressure两门槛才标识为超临界蒸汽，否则标识为“蒸汽”；无汽或未付足汽化热时不得凭空输出。正常出汽另受单一压力下限、逐口额度和接收方空间限制。
4. 本次不改变既有默认热工目标：仍向配置的超临界温度预热/再热，汽种只是当前输出资格，不替代目标温度。不新增迟滞、延迟或热力倍率。温压变化、汽种变化与控件调节不得改变已有汽量、实际HU或冷却剂量，所有输出仍扣实际比焓。
5. 原生Create网络必须能识别自动汽种变化，不靠玩家拆放管道恢复。只更新蒸汽端点及运输压力；非蒸汽端点既有能力/真实泵送持续有效。SIMULATE纯读；同一交易的声明汽种与实际drain返回相符，多口/重复调用不丢量。不得删除、强制改写外部储罐/管道中的异种流体；接收方不能混装时遵循原生背压规则。
6. 护目镜“有效配对”改为**有效热力回路**；新增**当前产汽：蒸汽／超临界蒸汽／无**，使用已同步实际汽种，不能照配置目标或已删除开关显示。炉压和出汽下限显示百分数，与安全阀泄放文案分开。其余设备与思索文案不改。

## 排错证据及审查重点

- 已读锁定Create源码：ValueSettingsPacket按ValueSettingsBehaviour.netId分发，而两个旧控件均继承默认netId=0；自定义BehaviourType只影响BE检索，不能区分网络收件者。这解释压力操作误走模式回调及默认10/60覆盖，应以实际原生数值提交验证。
- 旧selectMode递增全局epoch并撤销全部端口能力，含冷液口；不得用每次设置后刷新全锅炉来掩盖串扰。保留结构拆装真正撤销旧句柄的规则。
- Create FluidNetwork缓存fluid并按同种流体抽取，自动汽种切换须刷新蒸汽网络；不能让旧网络generic drain执行抽走新汽后因类型不符丢弃。热量守恒与冷液连续输出同等重要。
- 单控件按正确Create原生路径提交、服务端持久保存中间值，客户端同步不能逐tick把它写回旧默认。
- 不仅改getFluidInTank的显示；真实出汽路径、压力源、事务和中文遥测要消费相同实际资格。

## 唯一实现任务与写集

工作区`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，复用同级隔离树。执行者非PM，不Git写入、不改核心文档或任务状态、不另派代理、不启动客户端或操作用户存档/配置。保留logs及三个__pycache__已有无关改动；只交付未提交变更。

必须实际读取并应用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`与`minecraft-testing/SKILL.md`、系统排错及验证技能；中文注释/Javadoc明确服务端、单位、能力生命周期和流体事务不变量。禁止升级依赖、改构建脚本、全文件格式化或研究旧存档迁移。

允许修改：

- `src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerControls.java`、`BoilerControllerBlockEntity.java`、`BoilerState.java`、`BoilerSteamPressure.java`：仅本卡单控件、自动汽种、定向网络刷新、当前持久化与遥测。
- `src/main/java/com/iksxh/create_nuclear_industry/config/BoilerConfig.java`：统一默认压力项及合法性。其他热工值不变。
- zh_cn/en_us语言JSON：仅锅炉控件、炉压、有效热力回路及当前产汽键，删除不再使用的汽种开关键，不改其他内容。
- `src/test/java/com/iksxh/create_nuclear_industry/boiler/BoilerStateTest.java`；`src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionBoilerGameTests.java`、`ExtensionConfigGameTests.java`、`BoilerReviewGameTests.java`：只更新被移除API/配置的直接调用或相关断言，不删守恒断言。
- 新增专用`gametest/BoilerControlsGameTests.java`及`src/main/resources/data/create_nuclear_industry_boiler_controls/structure/boiler_empty.nbt`；复用已有空模板及已验证泵/齿轮/创造马达夹具。不要另造框架。
- 唯一报告`docs/reviews/2026-10-07/boiler-rework-01/controls-automatic-output.md`；原始日志/XML/制品`build/reports/extension/EXT-B-BOILER-REWORK-01B/`。

若发现需其他生产入口改动，先给PM准确路径、调用证据及最小改法，不自行扩范围。

- [x] 读取本卡、原账本/控制器与锁定Create路径；写有意义回归验证实际问题，再实施最小修复。项目精简规则不要求每一步重复启动完整服务端。
- [x] 实现单控件0～100、单默认/机器设置、实际温压自动汽种、蒸汽定向刷新及遥测；保留已付热和所有库存。
- [x] 同步被移除API直接影响的旧测试，编译保持一致；不重跑所有历史任务。
- [x] 定向JUnit：`./gradlew.bat test --tests com.iksxh.create_nuclear_industry.boiler.BoilerStateTest --console=plain`。15项通过，覆盖门槛两侧、单压力中间值/0/100、无免费HU、模拟/同tick逐口额度、当前保存恢复。既有独立换热器16项未改不重跑。
- [x] 专用实际GameTest域：`./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_boiler_controls -PgameTestDirectory=run/verification/boiler-controls-01b --console=plain`。3项实际通过，证明原生数值提交17/43/70和0/100正确且tick不覆盖；真实Create冷液泵持续运行时调压与自动汽种跨门槛无需拆管；真实蒸汽管路能更新种类且不因类型改变丢汽/HU，旧结构句柄仍按原规则撤销。首轮夹具观测基线错误保留，仅修该夹具复验专用域，不重跑全套。
- [x] 增量assemble一次，exit0、2秒；最终JAR2,193,605字节，SHA-256 `94F5B6481AB62BDB4B8594AAD1044B2FB693DCAB009A6FA644A77CEBE818AA2F`。不clean/build全套，失败证据保留，未操作用户客户端。
- [x] `/root/boiler_controls_01b_review`（gpt-6-luna/high）一次规格/质量合并只读审查，未发现确定性阻断缺陷，无全套复测；报告[controls-review](../../reviews/2026-10-07/boiler-rework-01/controls-review.md)。
- [x] PM读取实现、审查及原始证据，核对封包语言/NBT与源码相同、旧ModeBehaviour不再封包、最终哈希，并维护文档及候选快照/Git；停在已有锅炉集中手测门。两目录均保存01B制品，原01/01A快照保留。不得把自动通过写成人工通过。
- [x] 本卡继续适用的规则已随2026-10-08最终01F精简验收通过；被后续方案替代的旧操作不作为现行测试要求，具体范围见本页收尾说明及验收记录。

## 派发记录

PM已核对工作树、锁定Create ValueSettingsPacket/ValueSettingsBehaviour与FluidNetwork源码、旧控件和能力调用。已派发`/root/boiler_controls_01b`（gpt-6.1-sol/high）；此次跨控件网络及共享流体事务，使用较强核心模型，范围内文案由同一执行者完成，避免并行写语言JSON。审查优先高速模型。原01/01A证据和制品保留，本批只替代已变更行为的证据。

PM本轮实际读取Minecraft开发/测试、systematic-debugging、writing-plans、subagent-driven-development、requesting-code-review、verification-before-completion、using-git-worktrees及Minecraft版本/发布治理技能；复用已有链接工作树，不开新工作树、不跑基线全套。技能示例不改变锁定版本或本项目提交权限。
