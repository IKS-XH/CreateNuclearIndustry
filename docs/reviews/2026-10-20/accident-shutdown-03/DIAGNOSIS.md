# ACCIDENT-SHUTDOWN-03 阶段A只读诊断

2026-10-20，候选树 `E:/MyMC/NewMod/Create_NuclearIndustry-accident`，来源 `96a8421cc6073ea960fd331895f66eb949ea9008`。本阶段只写本报告目录；未写生产/测试、未运行测试/构建、未启动或修改用户世界，未操作Git或制品。阶段A交付后等待PM补卡。

## 结论

中间小堆仪表为 **(19,-59,4)**。它的保存状态没有真实活动余热；拆除被拒的可确定原因是：**旧事故完成后保留下来的两个空换料口仍永久持有旧 `AccidentLock`，不能向后建的新仪表重新绑定**。正式停机预检无法捕获全部六个物理换料口，失败关闭形成 `REJECTED`，生命周期统一显示“尚未完全停机”。这次尚未进入数值完全停机评估，不能用尾热修复、放宽epsilon或清库存解决。

此结论来自完整保存副本与当前生产源码，不是原世界运行复现。用户报告的游戏内拒拆为人工现场证据；本阶段没有新发BreakEvent、测量实时缓存或运行服务端。后续仍须用隔离真实服取得正式业务RED/GREEN。

## 现场与保存身份

- 唯一源世界 `run/saves/新的世界`。最新日志21:54:05依次记录停止单人服务器、保存玩家/世界，overworld/nether/end全部保存，最终 `All dimensions are saved`。原始尾部摘录为 `latest-shutdown-tail.txt`，完整日志原路径未写。
- 22:02:50进程快照只有VSCode的Gradle/JDT服务器及GradleDaemon，没有Minecraft/NeoForge游戏进程；`processes-before.json`也包含正在作此查询的PowerShell自身，不能误算游戏进程。22:09:17复查游戏进程仍为空。
- `session.lock`以 `FileMode.Open / FileAccess.Read / FileShare.None`独占打开成功，打开前后SHA256同为 `51643361C79ECAEF25A8DE802DE24F570BA25D9C2DF1D22D94FADE11B4F466CC`，未创建/截断/写入锁。完整复制期间持有独占锁。
- `world-snapshot/`完整复制 **29文件 / 13,363,621 bytes**，源复制前后及副本数量/内容SHA均一致；副本文件已设只读。`world-source-before.json`、`world-source-after.json`、`world-copy.json`、`world-copy-verified.json`和 `world-copy-proof.json`保存证明。最终源世界再次逐项核对，29项全部仍等于首次清单，见 `final-readonly-proof.json`。
- `source-evidence.json`登记准确HEAD、相关源SHA、配置SHA和日志SHA；tracked状态始终干净。本批不是main世界携带或旧格式迁移诊断。

## 三座结构的位置与状态

存档实际只有两个现存 `reactor_instrument_port` 方块实体，不能伪造第三个现存仪表。左侧大堆的原仪表坐标由独立事故账本给出，其现状已有149个 `reactor_corium`；三个范围在X方向明确左/中/右，无需猜测旧报告坐标。

| 现场位置 | 仪表 / 账本原仪表 | 原点～最大点 | 当前能读取的状态 |
| --- | --- | --- | --- |
| 左侧16³大堆残骸 | 原仪表(-8,-59,0)，当前BE不存在 | (-16,-60,0)～(-1,-45,15) | 149个残骸BE；旧危险拆除事故149/149列交接、2118/2118动作完成；不把旧账本释放热当当前堆活动余热 |
| **中间5³小堆** | **(19,-59,4)** | **(15,-60,1)～(19,-56,5)** | 3棒实际/目标全为1.0，均未卡死；4件真实全新燃料、另2个空口带旧锁；冷9000mB/热0mB；缓存热、量化余数、燃耗小数均0；无SCRAM、倒计时、事故id |
| 右侧5³小堆 | (30,-59,4)；旧事故原仪表(26,-59,3) | (26,-60,1)～(30,-56,5) | 6个空换料口，无事故锁；3棒实际/目标为1、1、0.09，均未卡死；冷9000mB/热0mB；无燃料列热、无倒计时/事故id |

两现存仪表 `AutomationClaimed=false`，保存信封 `STOPPED`，当前仪表代际均不同于各自旧事故代际。原 `AccidentGeneration` 是物理身份字段，不等于当前仍处于事故；须区分它与 `AccidentId`。

中间堆四个燃料列的CachedHeatHu、QuantizedHeatRemainderHu及FuelBurnRemainder全部精确0。真实物品栈均为count=1的fresh_fuel_assembly，没有damage或Used组件。四个实际有燃料的位置分别(0,0)、(0,2)、(1,0)、(1,2)，各自四向邻接的控制棒都在z=1且深度精确1。按现行FissionCalculator，控制强度 `(1-meanDepth)^exponent=0`，反馈乘积仍为0，因此新生热与计划燃耗也为0。这是源码公式的离线推导，未冒称正式tick运行结果。

## 几何、保护缓存及物理端口证据

`instruments-nbt.json`保存两个完整仪表NBT；`reactor-0-block-entities.json`、`reactor-0-heat.json`和 `structure-evidence.json`保存中间堆明细。

- 中间堆 `BreakSafetySurface.Valid=1`，Owner/Dimension/5³尺寸与当前仪表一致，**98个表面成员全部与副本实际方块ID相符**，未发现缺壳/错误接口；9个列帽及其三格主体均符合现行结构契约（6燃料列、3控制棒列）。六个换料口BE均存在，坐标/方块职责正确。
- 结构跨chunk(0,0)/(1,0)，两者保存Status均为minecraft:full。这仅是已保存区块内容，不能证明故障当时服务器实时FULL可用或真实卸载回调。
- `breakSafetyAwaitingRestoreScan`不持久化；read()在读回有效BreakSafetySurface后暂设true（Instrument:1914–1916），成功正式扫描后设false（Instrument:425–431）。`protectsUnavailableComponent`(460–489)在区块可用且扫描有效时返回false。静态几何完整且两口永锁已足以解释稳定拒拆，不需要宣称实测了瞬态等待标记。
- `updateStructureCache`先确立有效结构并调用重绑，再独立更新breakSafety证据；重绑失败不会把整个结构扫描判为无效。因而“结构有效”与“所有库存口可用”在当前实现中可能同时出现真假差异。

| 中间堆物理口 | 相对燃料列 | 保存物料 | 端口物理代际 | 旧锁/收据 |
| --- | --- | --- | --- | --- |
| (16,-56,2)、(16,-56,4)、(17,-56,2)、(17,-56,4) | (0,0)、(0,2)、(1,0)、(1,2) | 各1件全新燃料 | 详见原始NBT | 无AccidentLock |
| **(18,-56,2)** | **(2,0)** | **空** | **52dfb8d7-ef93-4292-bebd-7a65cdd35ab1** | 下述已完成事故中的同坐标同代际列 |
| **(18,-56,4)** | **(2,2)** | **空** | **ecd98548-a3b9-426e-bba0-34e38b1023db** | 下述已完成事故中的同坐标同代际列 |

两空口均保存 `UnformedExtractionAllowed=false` 和 `AccidentLock=6b2fd30c-acc7-42df-8b9d-6c61ac83c935`。`lock-receipt-evidence.json`逐字段证明：

- 该账本同维度、原仪表(19,-59,4)、范围与当前中间堆完全一致；原因COUNTDOWN_COMPLETE。
- 旧仪表代际 `69110384-39a5-4290-9e4a-9a3fa06a4107`；当前新仪表代际 `6c2106ab-1860-4b98-89d4-bf1f5655513f`。
- 六列Captured=true、Settled=true，Payload全空；50/50动作Done，ShockComplete=true，按生产complete()公式已完成。两口各自的位置、Generation均与对应托管列完全匹配；它们原来各有1件全新燃料，已交接，不能从Original审计字段再产一件。
- 两口不在冻结动作集。这与 `ReactorAccidentPlan.java:31–33`明确规定的“壳上限耗尽时保留空口”一致，属于设计允许的残余机构，不能靠扩大破坏上限消除症状。

## 正式拦截调用链

相关行号均针对上述96a8421源码，源SHA见 `source-evidence.json`。

1. **旧锁产生与永久保存**：Port:44–50的takeForAccident核实原件后设置accidentLock、移空真实物品并失效capability。Port:549–550、573–574持久化/读回该锁。全文件搜索仅这两种赋值，没有完成后撤锁路径。
2. **新结构重绑被锁拒绝**：Instrument:1419–1461按当前扫描的六燃料列调用port.bindTo；Port:198第一条件 `accidentLock != null`直接返回false，Port:75–77的isBound也始终false。这两口不会进入当前refuelingPortsByColumn。
3. **正式只读预检拒绝**：Lifecycle:123–128的LOWEST onBlockBreak先查不可用保护，再收集全部所有者拆除计划。Instrument:544–580确认当前成员后调用captureFuelPortsForReset；Instrument:1156–1181逐列验证真实BE、绑定及库存，碰到(2,0)的isBoundTo=false即返回null；Instrument:566–569形成REJECTED。此时未调用第578行的FullShutdownAssessment.assess。Lifecycle将其统一翻译成not_fully_stopped，因此提示不能证明有余热。
4. **最终门保持**：ReactorBreakFinalizationMixin:15–20位于原生CommonHooks.fireBlockBreak的IEventBus.post返回边界。若LOWEST已取消，finishBlockBreak第133行直接返回；若前层未取消，最终门再次执行不可用保护及完整计划，仍会拒绝同一失败。没有库存/事故写入。
5. **事务不是本次根因**：仅全部预检通过才进入Lifecycle:186的提交，commit再次复核真实端口/快照/评估并验证结果；任何异常经abortBatch、rollbackDisassemblyPlans取消为transaction_failed。此次固定空口锁在提交前就失败，不应改回滚或CommonHooks门绕开它。

## 与旧微小尾热修复的关系

`git merge-base --is-ancestor 850904df6220487f8903b00cc9936b8f68307d32 HEAD`返回0。当前Adapter、ServerTick、FullShutdownAssessment与main逐文件Git diff为空，旧修复已在祖先。旧病例5.2477e-10HU活动尾热与本次0HU不同；本次没有需要下降却不下降的热量。只读诊断未触及热工、安全epsilon、燃耗、倒计时、管理员权限或配置。

## 建议后续最小写集与正式回归

等待PM确认补卡，**以下尚未实施**。

| 建议文件 | 最小目的 |
| --- | --- |
| `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorPortBlockEntity.java` | 在既有服务端有效结构重绑入口，核定旧锁对应本维度已完成账本、同坐标/同物理代际且Captured/Settled/Payload空的真实交接收据，当前物品为空且接收仪表为不同于旧事故的有效世代后，才撤销过期锁并按原绑定流程失效旧handler/同步。保留未知/未完成/不匹配/非空口的失败关闭；不编辑NBT、不重造物料、不主动扫描世界或逐tick历史账本。可用现有SavedData.byId及Ledger.Column，无需新框架或新增持久字段。 |
| `src/main/java/com/iksxh/create_nuclear_industry/gametest/AccidentExecutionGameTests.java` | 复用现有cni_accident_execution域/模板/筛选和真实原生门，新增本根因的事故后残余空口重建冷堆用例及对应守恒/拒绝边界。无需新GT域或build.gradle变更。 |
| 本报告目录 | 保存RED/GREEN、退出及后续制品证据，具体实施/核验由补卡冻结。 |

正式业务RED建议：在隔离世界由正式末tick事故接管，按32壳上限完成执行，保留真实旧空口/代际；补齐事故后结构并放置新仪表，按本现场恢复6燃料列、3全插棒和冷态输入。先证明几何有效、账本完成、残余口确为原BE，再经 **CommonHooks.fireBlockBreak** 对新仪表发起维护拆除，期待放行；当前版本应因残余口不能重绑而业务失败。不能把直接调用assessment、手工删除锁或单纯函数真值当RED。

GREEN应证明正式重扫重绑后该门放行、正常事务按原规则进行；撤锁/重绑自身不改四件原料、9000mB冷液、任何CachedHeat/燃耗余数、账本Original/Payload/输出收据。覆盖当前格式端口和账本正常保存读回后仍可恢复、动作/事件重放不重产及旧handler不复活。补足最相关反例：缺账本/同坐标不同代际、未完成或未交接、当前口非空、事故原仪表仍锁定均不得被这条恢复路径放行；真实新生热/倒计时危险拆除与真实余热拒拆规则保持。无需重跑16³/预算/冲击整矩阵。

接口解锁先复用ADMIN-RELEASE-01已有用户通过记录，后续只复看本次重建/重绑可能触及的正常逐棒入口；不把该通过反记为本阶段新增测试。

## 实际执行及证据边界

实读两树AGENTS、治理5.1/5.2、STATUS、活动卡、旧shutdown Integration、事故及ADMIN-RELEASE候选报告，核实际Gradle版本保持MC1.21.1/Java21/NF21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。

实际使用minecraft-modding/testing技能核本版本BE/事件/真实GT入口，systematic-debugging按现场→边界→根因取证；已读TDD要求，遵守阶段A不编写/运行测试，RED/GREEN留待补卡。没有涉及新制品，未使用发布构建或换包。

实际诊断命令为CIM进程查询、只读独占锁、PowerShell Copy-Item/Get-FileHash、只读git status/rev-parse/merge-base/diff及rg/Get-Content；Python仅运行报告内三个离线辅助：inspect_snapshot.py、inspect_geometry.py、diagnose_locks.py，均exit0。前两脚本复用旧performance-diagnosis/diagnostic.py的NBT读取器，记录其SHA，不调用该工具的RCON类或inspect主入口。没有Gradle、服务端、客户端、RCON、BreakEvent运行或物理区块卸载/多人测试。原始数据和所有工具输出均仅本报告目录，不替代待做的真实回归。
