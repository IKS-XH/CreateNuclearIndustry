# EXT-B-BOILER-REWORK-01D：蒸汽出口独立汽种过滤

> **2026-10-08收尾：** 用户确认最终01F精简手测通过；本卡继续适用的锅炉规则已随REWORK-01～01F[验收合入main](../../reviews/2026-10-08/boiler-rework-01/ACCEPTANCE.md)。被01F替代的整炉瞬时切种不再要求复测。下方候选、失败及待验收描述保留历史含义，不作为当前人工门；不扩展为所有动力场景或首发验收，不自动接其他主线/教学。

**后续现场反馈：** 用户在6×6×5、四汽口汇入同一管路的场景发现调压后出汽异常，并成功复现汽轮机持续残转（首次发生后稳定复现，重新成型消除）。由[01E](./2026-10-07-boiler-multiport-turbine-stop-01e.md)接续整改；本卡下方“暂缓排查”为01D交付时历史状态，不再作为当前排期。01D自动证据保留，集中人工门未通过。

**状态：** 用户明确选择“只过滤当前汽种，不允许降级输出”，已完成实施、最终4项定向GameTest、唯一增量assemble、一次合并审查与PM封包核对。功能快照`99a526c97697e9de134cbd090674927c2147c2bd`在同级候选；本批停止在锅炉集中客户端门，不开启其他主线或教学。汽轮机持续残转用户未能复现，排查暂缓，不记为修复或锅炉整体验收。

**授权与目标：** 用户要求给蒸汽输出口增加汽种选择，避免同一管路自动交替混装造成背压；集中问答已确认每口独立选择“蒸汽 / 超临界蒸汽”、默认超临界。这是01B自动出汽规则的局部替代：炉内汽种仍按实际温压决定，出口只过滤，不转换。

## 冻结合同

1. 每个蒸汽出口各有一个Create原生世界内选项控件，位于端口朝外面；无独立GUI。只提供“蒸汽 / 超临界蒸汽”，默认超临界。选择服务端校验、客户端同步、当前版本保存加载；控制器仍只有0～100的出汽压力下限控件。
2. 真实温压和已付汽化热仍决定当前炉内汽种。只有当前汽种等于该口选择才声明并输出相应流体；不匹配时该口不出汽。不得降级、不免费升级、不重标库存或增减HU，不新增分离储罐/独立汽种库存。两个不同选择的口不是同时把同一份汽转换成两种。
3. 类型校验覆盖主动直填、原生Create管路/泵、被动能力抽取、getFluidInTank和SIMULATE。每次声明与实际drain返回一致；旧句柄不能抽走新类型，SIMULATE不得改变库存、HU、选择、额度或网络。逐口额度继续共享主动和被动交易，不因切换刷新。
4. 切换一个口只撤销该口的旧流体能力并在合适tick刷新该口的Create来源网络；不撤销其他汽口、水/热液/冷液能力，不重扫结构，不在正在执行drain的原生network内改写网络对象。实际温压改变时，仅更新输出资格确实变化的汽口。无匹配汽种的出口不贡献主动输送压力。
5. 复用01C首段来源缓存修复，管道与直接邻接原生机械泵均能在选择切换后重新识别端点；外部异种库存完全保留，不能被清空或强改类型。外部储罐不接受异种时维持原生背压；必要时玩家排空或分设管路。
6. 每口护目镜显示选定汽种及等待匹配等实际状态，中文玩家文案简短；使用现有GoggleTooltip缩进避免图标遮字。控制器“当前产汽”继续显示炉内实际资格。尺寸、热工、安全阀、加热目标、出汽压力下限、原料成本、汽轮机效率/转速/缓存全部不改。
7. 不匹配导致积汽和安全阀泄放是用户所选纯过滤规则的可能结果，本批不通过自动降热、降低超临界阈值或自动转换规避它。当前温压尚未达标时，选择超临界的口须等待，不能先送蒸汽。

## 执行区与允许写集

工作区：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；派发基准HEAD `200073cf17ce3e3aec53cd75bff9902e45a01029`，最新功能`050afe3e4eb70789804697b2880b7c76281ca8d7`。保留logs和三个__pycache__现有改动。MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6不升级。

执行者仅允许修改以下路径；新增路径如无必要不用建立：

- `src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerPortBlock.java`、`BoilerPortBlockEntity.java`：原生选项行为、ticker、当前选择持久化/同步与端口遥测。
- 同包`BoilerControllerBlockEntity.java`、`BoilerSteamPressure.java`：逐口过滤、能力代次/缓存更新、逐口压力资格及真实交易；`BoilerControls.java`：必要的共用控件布局。
- 同包新增`BoilerSteamKind.java`：两选项模型及原生图标/中文翻译入口，若内聚于已有类可不另建。
- `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`、`en_us.json`：仅本批键。
- 新增`src/main/java/com/iksxh/create_nuclear_industry/gametest/BoilerSteamSelectionGameTests.java`及`src/main/resources/data/create_nuclear_industry_boiler_steam_selection/structure/selection_empty.nbt`：独立筛选域，复用已有空模板。
- 原`gametest/BoilerControlsGameTests.java`、`BoilerTurbineFlowGameTests.java`、`ExtensionBoilerGameTests.java`：仅调整受纯过滤新合同影响的夹具/断言，不删测、不放宽质量/HU/冷液断言；未实际运行的旧域须明确标为未重跑。
- 交付报告`docs/reviews/2026-10-07/boiler-rework-01/steam-port-selection.md`；独立只读审查者仅可写同目录`steam-port-selection-review.md`。原始日志/XML/JAR仅`build/reports/extension/EXT-B-BOILER-REWORK-01D/`。

执行者禁止Git写入、修改治理/核心文档、自行派发、启动用户客户端、改写用户存档/配置、研究旧存档兼容、修改BoilerState公式或汽轮机实现。若需扩大写集先向PM报告具体原因。所有手写新改代码的必要注释/Javadoc用中文，说明服务端与客户端、mB/HU、句柄和同tick事务不变量。

## 执行与必要验证

1. 实际读取AGENTS、本卡、01B/01C相关实现，以及Minecraft开发/测试技能；核对锁定Create的ScrollOptionBehaviour、ValueSettingsPacket、SmartBlockEntity及原生流体缓存接口，不能照新版本示例升级依赖。
2. 用一个执行者完成全部紧耦合代码与测试；先建立有意义的失败证据/夹具，再实施。无需开工全量基线。PM只写文档、审核和Git。
3. 只运行新汽口域：实际原生选项提交与两口独立当前保存恢复；两类汽种的过滤拒收与实际交易/SIMULATE质量、HU、逐口额度；真实Create双管路选择切换，原句柄仅该口失效，其他汽口及真实冷液泵持续，管道/直接邻接机械泵可恢复。证明外部异种储罐保留背压。可合并场景，避免按每个参数拆大量测试。
4. 账本公式不改，复用01B的15项定向单测、01C已审HU/停转边界；不重复无关JUnit、全域GameTest或完整build。最终仅一次增量assemble。发现新增共享事务缺陷或实际失败才扩到受影响域。
5. 交付一份报告：实际技能、变化与写集、首次失败/最终日志路径和命令/退出状态、定向断言范围、未验证边界、JAR。一次高速模型规格/质量合并审查读现有证据不重跑；整改只复验被改部分。

## 派发和收尾记录

- [x] 用户明确选择纯过滤、不降级；规格冻结。
- [x] 实施与新域定向验证。
- [x] 增量assemble、PM差异/封包核对、一次合并审查。
- [x] 同级候选源码快照与集中手测说明同步；不合入main的功能代码。
- [ ] 用户集中验证出口控件、实际选择/等待及切换后无需拆管、冷液持续、当前设置保存恢复。

人工门前停在本候选；不要求为已暂缓的持续残转另做专项复现。

实施`/root/boiler_steam_selection_01d`使用gpt-6.1-sol/high（逐口真实能力与已知来源缓存风险），唯一只读审查`/root/boiler_steam_selection_01d_review`使用gpt-6-luna/high。最终新域4/4、exit0/19.79秒，唯一assemble exit0/5.42秒；增量JAR2,227,784字节，SHA-256 `C699C2A97E84D318CFBDA33A79A4409AD3ED0D88C038DD5E33678620A72A5412`。PM核对11个生产class、两语言及NBT与构建输出/资源一致；原始失败轮保留，未重跑旧域/账本/汽轮机测试。最后仅遥测和槽位布局由assemble核对，客户端点击与当前设置退出重载待人工；见[实施报告](../../reviews/2026-10-07/boiler-rework-01/steam-port-selection.md)、[合并审查](../../reviews/2026-10-07/boiler-rework-01/steam-port-selection-review.md)及[候选说明](../../reviews/2026-10-07/boiler-rework-01/CANDIDATE.md)。PM使用Minecraft开发/测试技能设计与审证，CI/release技能仅核对候选版本/制品边界，不改变版本、许可证或发布范围。

## 技能入口

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/brainstorming/SKILL.md`：本批短设计已通过集中问答确认，用户纯过滤决定优先于推荐降级方案。
- 同目录`subagent-driven-development`、`test-driven-development`、`verification-before-completion`、`requesting-code-review`；按项目5.1仅一次合并审查，执行者Git禁令及用户手测门优先。
