# OPT-INTEGRATION-20261020 规格与质量组合审查

2026-10-20，只读审查者 `/root/optimization_review`。本报告只交付审查结论；最终验收、状态与Git操作由PM记录。

## 结论

累计生产实现和最终导入差异未发现可证实的P1/P2回归或未授权生产范围。审查发现的两条过期GameTest夹具已由唯一执行者按原T8验证补丁同步并复审闭合。已核本次增量assemble及完整来源/class/资源绑定，可按用户授权继续窄范围集成。没有重复运行JUnit、GameTest、benchmark或修改用户包/世界。

## 来源与实际范围

- 基准main：`60c484803149fa3a3317ec5521664259a7f570d0`。
- 实际链：正式0763→T2→T3→T4→T5→T7→T8→T9→H1。T6、B1、事故第二批及相邻美术/辅助工具不在本链。
- 最新H1：`E:/MyMC/NewMod/Create_NuclearIndustry-thermal-performance-diagnosis-20261017/H1-heat-line-20261020`；候选SHA256 `50f51e376dd373eff4bcebb67cee79c3ba5e261d7ea6c79010fe995eeb22484b`。
- 集成来源提交：`a1ca0a6c249e81fd6ed66a25243964f72bab61b2`，实际30项：16个H1生产Java、1个Mixin清单、11个H1新增JUnit、2个T8开发GameTest夹具。已读完整累计生产差异、实际提交清单、[导入分类核验](E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration/build/reports/optimization-accepted-20261020/copy-verification.json)及[盘点](E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration/build/reports/optimization-accepted-20261020/INVENTORY.md)。28项与H1归一内容一致，另2项是明确授权夹具覆盖；不能将30项统称全同H1。
- 已读main AGENTS、治理5.1、STATUS、本批活动卡和各批PLAN/REPORT。实际应用技能入口为 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-ci-release/SKILL.md`；完成前另读verification-before-completion，以实际差异及构建输出核结论，遵守治理5.1不重复业务测试。
- 实际Gradle仍锁MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；未因原整合包NF21.1.249/Create6.0.10测试环境升级工程。

## 关键不变量审查

| 范围 | 实际核对 |
| --- | --- |
| T2/T3归属与绑定 | 实际成员long索引按Form身份建立/撤销，重叠走原顺序回退；几何归属不授予运行资格。仅自有正句柄在单次调度复用，世代/世界/实体/方块状态守卫保留；未知和空能力实时查询，真实Port操作仍查红石、区块与内腔。 |
| T4内容见证 | 只缓存原内容谓词成功结果；方块容器版本/busy及chunk/section/palette身份失效时完整读取。票据、世界边界和交易门实时检查；客户端/非服务端主线程不使用该内容快路。Mixin只记瞬态元数据，不取消或改变世界写入。 |
| T5/T7图与预算 | BFS原FIFO/方向/端点顺序保留；未知提供者、安全见证不足或建图失败回退。取消距离门属于用户批准，物理节点封顶与不强载仍保留。几何不存库存/容量/许可；同tick分割重建不清额度，输入/输出过路按max收费，重入临时容器隔离。 |
| T8管/泵/阀 | 普通管用独立无额度标记，介质/几何检查保留；阀、泵、设备口及接收方额度保留。串联不能叠额、独立并联可叠属于已批准规则。耗尽跳过在原混液检查和轮转后，只用于可证明无缓存/不遗漏瓶颈的情形。 |
| T9补投与动力 | 仅因自有轮机周转满的意图在真实排汽后有界补投；重新检查库存/混液/绑定/版本/路径/许可，共享本tick额度，游标只推进一次。动力审计仅沿已加载Source链，证明无源/环路才撤依赖分支；合法外源、未知及未加载链保守保留，撤销自身贡献时同步清自身保存字段。 |
| H1直列 | 缓存只存几何/身份；每次查全部成员、朝向、炉内归属、tick资格及两端加载边界。模式/库存/容量/可收空间不跨交易缓存，模拟不预约空位。64位脏成员覆盖配置64台上限，实际库存或模式变化成员通知持久化，所有成员即时刷新整列显示。 |

未改热工/效率/取整、周转容量、账本/收据与退款公式、流体许可、配置版本、渲染/资源外观、首发能源或事故范围。新增/修改手写生产注释为中文。

## 发现与闭合

本次初始28项遗漏T8/REPORT明确要求同步的两条开发夹具，现已加入活动卡和来源提交：

- `BoilerIndustrialActiveOutputGameTests`：扩大接收罐，四主动口断言改为16384、源余43616，合计60000；历史方法名保留以对应原结果。
- `IndustrialFluidNetworkGameTests`：输入源北移一格，用强化阀保留8192瓶颈并补连接管；源7808、目标2000、缓存6192及总16000守恒断言完全保留。仅另将中文注释准确改为阀限额。

已逐hunk对照T8/VALIDATION.patch和实际集成diff；没有借此修改旧失败或放宽守恒。Boiler夹具仍按既有打包规则排除；Industrial夹具原本就在开发载荷中，其允许class变化已单列。

## 原证据复用与边界

本轮读取各批final-audit、功能对照及关键原始结果，来源SHA连续相接，未将旧结果称为新运行。T2定向15项、T3/T4/T5相关32/38/50项、T7/T8相关70/87项、T9相关87项JUnit及其真实服务端证据按原范围复用，不把重复回归累计为独立总数。

- T9 [真实GameTest](E:/MyMC/NewMod/Create_NuclearIndustry-thermal-performance-diagnosis-20261017/T9-steam-kinetic-20261019/evidence/gametest-summary.json)为34/37，包含5个动力边界用例通过；3个既有失败与前置基线相同。真实完整场景满流量、断汽停轴、外源/最后源、当前格式保存重进及单泵/双串联SC泵见原advanced-assertions，不能扩大为长期/所有并联实站通过。
- H1账本JUnit27/27、新增真实断言10/10；原直列/冷凝[对照](E:/MyMC/NewMod/Create_NuclearIndustry-thermal-performance-diagnosis-20261017/H1-heat-line-20261020/evidence/native-comparison.json)为9/14，T9/H1同5项失败且错误相同。正常新保存重进和连续3456mB/t见H1原证据。不能称全GameTest通过。
- T4早期票据/退休chunk代理不冒充真实Unload；T7/T8真实Unload释放整组夹具票据，H1 FULL降级是原生supplier资格门，不冒充物理卸载。原STOP/SCRAM重载失败、相机失效性能轮及工具失败均保留，未混为成功证据。
- T7只观察到小幅收益；T8本组MSPT增加4.03%，不支持提速。T9吞吐1728→3456mB/t，不能称等负载加速。H1仅本机受控六窗观察到MSPT约-1.88%、分配约-8.26%，不能叠加各批百分比外推累计/多人/长期收益。
- 原整合包自动化、锁定工程构建、客户端人工视觉、真人多人和长期实服结论分别保留。本次未新增人工验收，未关闭STATUS中原独立未闭门。

## 本次构建和身份

已读[BUILD](BUILD.md)、[artifact](ARTIFACT.json)、assemble完整日志/exit、inputs-before/after、payload-manifest及ASM工具源码/逐class表：

- 来源`a1ca0a6c249e81fd6ed66a25243964f72bab61b2`，唯一一次`.\gradlew.bat assemble --console=plain`，20秒、exit0；12条既有弃用警告，无编译错误。没有clean、强制重跑或候选重封。
- 2294项输入与已提交归一内容一致，构建前后原始哈希保持，无额外未跟踪生产源。
- assemble原输出：3560094 bytes，SHA256 **`88f21a64cd483d2767bc0e5016282052b1f226835cf06f59bca833de71e51bbb`**。
- 全2466项载荷已绑定：778 class逐字对应编译输出并回指已提交Java，1687处理后资源逐字对应输出及源，manifest对应本次jar任务。
- 相对H1条目集合一致：710 class逐字相同，66仅局部变量调试表、1个匿名夹具class仅行号变化；唯一非调试变化限制在授权短收夹具及其lambda两方法。已读ASM比较实现，字段/方法签名/参数元数据/注解/指令/异常表/验证帧保留比较，未发现其他非调试差异。
- Mixin JSON唯一资源差异为CRLF→LF，归一字节和JSON相同；其余资源及manifest逐字相同。因此可复用H1生产实现证据，同时明确正式包与窄编候选SHA不同。

本报告仅写本允许报告路径；未改生产、测试、核心文档、Git、来源树、用户包或世界。main无关dirty保护及最终正式JAR/Git身份由PM在集成记录核验，本审查不代替该项。
