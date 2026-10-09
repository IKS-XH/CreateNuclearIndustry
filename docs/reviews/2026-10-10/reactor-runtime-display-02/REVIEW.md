# ART-REACTOR-03-L2 独立规格与质量组合审查

2026-10-10，独立审查执行者。结论：**存在 1 项 P2 阻断，当前冻结候选须定向整改后才能净集成/交付消费。** 不改变任务状态，不宣告动画或 02R1 客户端视觉通过。

## 审查范围与依据

- 唯一工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-display`，实际 HEAD/基线 `c5f29f02e30e2d1f7244c0619d0d53c35a9ca8b3`。
- 已读 AGENTS、治理 5.1/5.2、`docs/art/ART-REACTOR-03-INTERFACE.md`、本次实施计划和 IMPLEMENTATION.md；按 `source-sha256.json` 精确 12 路径审查源码、测试和专属模板，没有扩大写集。
- 实际读取并应用 `minecraft-modding`、`minecraft-testing`、superpowers 的 `requesting-code-review`（含 reviewer 模板）与 `verification-before-completion`。`using-superpowers` 的 SUBAGENT-STOP 适用。治理优先于通用技能的重复测试/提交流程；本审查没有派发子 Agent、执行 Git 写操作或运行 Gradle/测试服/客户端。
- 已核实际 MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82 / Flywheel 1.0.6；只读 JDK 21 javap 核对当前本地 NeoForge 制品的 RenderChunkRegion、ClientChunkCache、Storage、ClientLevel、LevelChunk 和 BlockBehaviour 相关签名/字节码。
- 本文件为审查者唯一写入；已有 PM 文档、dirty 历史日志、首轮失败/自有 PID 停滞证据保留。

## 阻断项

### P2：当前 owner 的错误 dimension 信封未立即撤下旧显示

**位置：** `src/main/java/com/iksxh/create_nuclear_industry/structure/client/ReactorRuntimeSnapshots.java:93`；接收适配位于 `ReactorRuntimeClientEvents.java:97` 起的 `receive(owner)`。

**触发条件：** 当前 session、当前真实 owner token、相关 chunk 实例均正常，State 已有租约内可靠样本。后续该 BE 的 L2 NBT 字段类型/几何均完整，但 `Dimension` 被改成另一个合法 ResourceLocation，例如从 `minecraft:overworld` 改为 `minecraft:the_nether`；ownerPos/generation 不变。`ReactorRuntimeDescriptor.decode` 会返回完整对象，事件层也核实并接收当前真实 BE，因此会进入 State.receive。

**实际行为与影响：** 第 93 行先因 dimension 不匹配直接返回 false，尚未取得 owner，更未把 `owner.active` 置 false。事件层随后仍 publish `STATE.snapshot()`，所以原 owner、棒列与棒归属索引继续暴露旧信封，直到原 20tick 租约到期或另一个失效事件。第 96 行的几何矛盾撤销分支本来可以处理此冲突，却被提前返回绕过。该包没有续租，但这仍不满足合同对当前坏包/身份矛盾立即撤下旧动画的要求。

**必要修法：** 保持旧 world、未知/旧 token 等无权影响当前实例的投递直接忽略。在当前 session 且已核实当前 owner/token 后，再检查 dimension；若 dimension 不符，立即撤销该 owner 的 active 显示并保留已有 generation/revision/sample 水位。可以在 State.receive 内调整校验顺序，也可以在事件层对已经核实的 owner 明确调用 unavailable；不得把所有旧会话/旧实例拒收都改成撤销当前显示。

**必要定向回归：** 在现有 LifecycleTest 的 ready 场景中，对合法新样本 NBT 只改 Dimension，确认 decode 成功、receive 返回 false 且同次 snapshot 的 findOwner/findControlRod/findControlOwner 全为空；原样本重复不能恢复/续租，后续正确 dimension 的新可靠样本可恢复。同时确认旧 world、旧 token 投递不撤销当前可靠样本。该项由静态真实控制流确认，本审查没有新增或执行测试。

## 已核实的合同落实

| 范围 | 审查结论与依据 |
| --- | --- |
| 同龄结算与原正式顺序 | BE 原有 hydrate → 捕获物品 → advance → 提交预检 → 端口写入 → setSnapshot → 融毁事件 → 遥测顺序保留；完整 L2 在正式提交/事件完成后由同次 Result、CoolantInput 和缓存 scan/L1 构造。write/getUpdateTag 只编码冻结信封，没有现场拼新库存与旧产热。 |
| 成功 false / 失败 / 外部写入 | 成功零变化也推进 runtime sample；四个提交前早退各撤 L2，不调用 publishTelemetry 清改旧正式遥测；内部 hydrate/setSnapshot 由 try/finally 结算作用域保护。外部 setSnapshot、重扫、invalidate/onChunkUnloaded 撤销 L2。投影 RuntimeException 在窄 catch 内只撤显示，正式异常仍按原语义重抛。 |
| 真实列与热账本 | 有效从未装料列在 snapshot/fission 同时缺席时投影为完整燃料几何、false/0 HU/t；单阶段缺席拒收。FuelColumnState.isEffectiveFuel 与本次 fission HU 分开，耗尽 tick 的 falseUsable/正 HU 保留。卡死棒读取实际/目标/卡死，行程来自 body 数量；冷热库存、转换量及同次容量未重算。冷却空间仅 EMPTY/CONTROL body，零容量原样保留。 |
| 协议与不可变性 | decode 校验强类型、布尔、UUID、ResourceLocation、坐标、有限/非负数值、注册 ID、列帽/body 重复及完整 bounds/列几何；列/总 body/边长预算在生成部分对象前检查，构造器再次校验。不保存 Level/BE/NBT 可变引用。已解码错误 dimension 的接收例外即上列唯一阻断。 |
| 心跳与租约 | 原遥测包带新冻结信封时重置计数，否则恢复立即发送、正常每 5 成功 tick 心跳；稳定零产热继续推进。State 以新 sample 本地 receivedTick 的 20tick 租约显示，重复/旧 revision/sample 不续租，失效后等待新可靠样本。 |
| chunk / session / capture | false 查询不加载区块；实际 ClientChunkCache.drop 在清槽前发 Unload，显式 UNLOADING 屏障阻止重新认可卸载实例。Storage.replace 的原生清 BE 路径及缩视距/换槽无 Unload 路径由每 tick 实例核对补偿。State 保留卸载水位，实例替换先撤销，owner remove 用 token 防旧对象误撤，世界切换清会话；capture 只认当前精确 ClientLevel 或原生 RenderChunkRegion.level，未知/Ponder/旧世界为空。 |
| 同 capture 归属与歧义 | snapshot 先排除所有重叠 bounds 的双方，再建立棒与完整 owner 索引；findControlOwner 与 findControlRod 使用同份冻结 owner/rod，客户端快照没有跨代可变引用。 |
| common / 燃料注册 | JAR 中 BE 字节码无 net.minecraft.client 或 structure.client 引用；已有独立服务端成功加载。P1Blocks 差异只燃料 noOcclusion 与中文说明。锁定 BlockBehaviour 字节码及真实服断言一致：碰撞/选框仍完整，默认阻光 1；未扩展其他注册、shape/light 覆盖或玩法。 |

## 证据复用与只读核验

- 本轮实际重新读取并计算精确 SHA：12/12 源/测试/模板均匹配冻结清单；JAR SHA 为 `a45c8047bf939c37e257f057f34ca39096d8d743cf6dbc603b4079957cb2d266`，与 jar-sha256.json 相符。完整公共编译签名以原 `javap-api.txt` 为准，已核含 runtimeDescriptor、capture、findOwner、findControlRod、findControlOwner 及列/身份/单位访问器。
- 读取五份原始 XML，实际 Descriptor 3、Projection 4、Lifecycle 5、Telemetry 5、StructureSummary 7，共 **24/24，failure/error/skip 均 0**；`final-junit-assemble.log` 和 `final-assemble.log` 均记录 BUILD SUCCESSFUL。此为实施者原运行结果的核验/复用，不是本轮新运行。
- `gametest-second.log` 明确仅启用 reactor_runtime_display_probe、运行 1 个 required test、全部通过，随后完成各维度存盘、Game test server shutting down 和 BUILD SUCCESSFUL。复用本次 **1/1 真实仪表及正常退出** 证据。
- `red.log` 是入口缺失 red；`empty-result-red.log` 是正式空燃料 Result 的行为 red。首轮 `gametest-first-failed.log` 的 No value present 与 Saving worlds 停滞仍保留，`first-server-stop.json` 记载本轮 PID 23872/父 PID 15556；不混同第二轮通过或退出。
- 两个 root 历史日志当前 SHA 均为 `1b3cf6b9ffbe10093c4c2a0ba33a3be74c03117c9bbed6b83b0e5518e97a6368`，与 PM 窄允许的轮转原字节恢复记录相符；无 Git 恢复/清理。未改 L1 的 26/26 与 1/1 沿用既有 HANDOFF 原证据，不重复测试。

## 不在本次裁决的范围

- 动画模型、BER、透明排序、材质插值及 02R1 视觉：消费者尚未实际交付/客户端观察，按合同保持独立门。
- L1/CT 原行为、旧版本存档迁移、已验收玩法/Ponder、07 装配台教学：本批只读或未派发，治理 5.1/5.2 不允许借本审查重开或扩测；未以这些项目阻断本候选。
- PM 净 Git 整合与美术 API HANDOFF：不属于审查执行者权限，等待上述 P2 窄整改及实际 diff/对应证据复审后由 PM 处理。

没有发现其他 Critical/Important/Minor 项。后续只需复审该修复实际差异与相应定向证据，不重复本轮已核通过场景或组合审查。

## R1 唯一 P2 窄复审（2026-10-10）

**R1 结论：唯一 P2 已闭合，当前 R1 冻结候选可由 PM 净集成并交付实际 API 消费。** 上文首轮阻断结论及初版 SHA 作为历史保留，本节对应最新入口 `build/reports/art/ART-REACTOR-03-L2/R1/final-freeze-r1.json`。没有新增审查项，不宣告动画、透明排序或 02R1 视觉通过。

- 只复审 State 与 LifecycleTest 两路径。实际只读 no-index diff 与交付两份 diff 一致；before.java 的 SHA 分别匹配初版 `0eeba5b7...2358903` / `fa6dd28b...0eb64d8`，确认比较对象是首轮已审候选。R1/初版 12 路径清单只有这两项 SHA 变化，未重新审读或机械重算未变 10 路径。
- State.receive 先拒绝旧 world/空信封，再核当前 owner/token，随后错误 dimension 只执行 `owner.active = false` 并返回 false。旧 descriptor 的 generation/revision/sample/receivedTick 不改；snapshot 因 active false 立即排除 owner 与其棒/归属索引。原重复样本校验不改，正确 dimension 的新可靠 sample 仍能恢复。只读 R1 冻结 JAR 的 State.receive 字节码确认此校验顺序及 active 写入已进入编译实物。
- 新测试实际构造可 decode 的合法 `minecraft:the_nether` 错维度信封；同份 capture 的三个查询均验证即时为空；重复原 sample 不能恢复，正确新 sample 可恢复；恢复后旧样本也不能续租，第 20tick 三索引撤下。旧 world 与未知/旧 token 的错误包各验证当前三个可靠索引保留。测试未删改此前五个生命周期场景。
- 原始 `R1/red-lifecycle.xml` / red.log 是该新场景 **1 个真实断言失败**，失效 owner 仍可见，非基础设施失败；`R1/green-lifecycle.xml` 实际 **6/6、failure/error/skip 全 0**，green-assemble.log 记录 BUILD SUCCESSFUL，与实施报告退出 0 相符。本审查仅核验原运行，不重跑 Gradle/真实服/客户端。
- 本轮重新计算两条当前源码 SHA：State `f11c0262b57b45dd85d8fcc4ed7b9ed350ceae7076872339948affe2b3c71022`，LifecycleTest `e830863d1154e79f1d9f1c8f34b35b0cbad6619e485f939459e4cf8f6a357d62`，均匹配 R1 清单。当前 build/libs JAR 与 R1 冻结副本均为 `b4c56788146d672d7130a1231d0bce64f54afc91baba53a408d5f92ceaff83df`。
- 初版/R1 实际 javap API 文本 SHA 均为 `177de2fc074b2fe9ffde92c8af1134466f9fc3d7ff3a351b2f1acb93e68a013a`，与 api-comparison.json 的逐字节一致/退出 0 记录相符。无 ABI、同步字段或消费者入口变化。
- 受改范围只客户端 State 与对应测试；BE/common、投影、协议、GameTest、燃料注册等仍对应首轮冻结源码，复用首轮其他 19 个 JUnit 及 1/1 真实仪表/正常退出证据，不重开未变场景。原失败、原日志、初版清单与 PM 文档继续保留。审查者本次仍只追加本文件，无代码修复、Git 写或子 Agent。

## R2 真暂停租约规格与质量窄审（2026-10-10）

**结论：未发现 Critical、Important 或 Minor 项；R2 冻结候选可由 PM 净同步 main 与美术树。** 本结论只覆盖真暂停租约窄修，不改变任务状态或替代客户端人工观察。上文初版和 R1 历史完整保留。冻结入口为 `build/reports/art/ART-REACTOR-03-L2/R2/final-freeze-r2.json`，基线 HEAD 为 `18e2524a053f9ff997e66ae96085efa32f0897a3`。

### 实际合同及控制流核对

- 已读当前 AGENTS、治理 5.1/5.2、R2 窄修卡、ART03-L2 合同及实施报告。实际应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`，以及 superpowers `requesting-code-review` 的 reviewer 模板和 `verification-before-completion`；`using-superpowers` 的 SUBAGENT-STOP 适用。核对 gradle.properties/build.gradle 的 MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。按治理复用原运行，未重跑 Gradle、服务器或客户端，未派 Agent 或执行 Git 写操作；唯一写入为本节追加。
- 直接读取本地锁定 `build/moddev/artifacts/neoforge-21.1.219-sources.jar`。`Minecraft.java:1152`、1159～1161 的 timer/循环继续调用 tick；1787 在1789 的 pause 判断前发布 Pre，`ClientHooks.java:1069`～1070 直接 post。`DeltaTracker.java:61`～70 的活动 tick 计算没有 paused 判断，88～101、114～118 处理暂停残余及 partial。`Minecraft.java:1228`～1235 的 pause 依赖单人服务端、暂停界面且未公开到 LAN；2556～2557 的 isPaused 返回该实际状态。`IntegratedServer.java:93` 读取相同状态，102～110 有连接且真暂停时走 tickPaused，否则 super.tickServer。因此根因与修复判据均由本地实际版本控制流支持；多人菜单不会仅因打开界面而冻结租约。
- 生产位置 `ReactorRuntimeClientEvents.java:42`～54：onTick 先 synchronizeWorld，再 null guard，再以实际 isPaused 调用私有 advanceLease；只有 `!paused` 调用原 State.tick，随后 reconcile/publish 无条件照旧。直接只读 R2 冻结 JAR 的 `javap -c -p`（JDK21，退出0）确认 onTick 的调用顺序与 advanceLease 的 `ifne` 跳过时钟已进入编译实物。没有整段暂停 return、公共入口变更或服务器时钟变更；中文注释准确区分物理客户端、活动 tick 单位与持续撤销边界。
- State、包接收、owner/chunk/world 事件未改。暂停仍核实真实 chunk，卸载/替换可撤销 active；坏包及旧会话规则继续保留，publish 仍反映 snapshot 的三索引。snapshot 的原20tick条件与 sample 水位未动，冻结年龄不能续样本或使到期/撤销显示复活。

### 差异、行为与冻结证据

- 两份 before.java 的实际 SHA 分别匹配 R1 Events `78df9024...bc5d3d`、LifecycleTest `e830863d...357d62`。本轮真实 `git diff --no-index` 的全部 hunk 与两份冻结 `.diff` 一致（差异退出1），不是只读交付摘要。实际 Git 只有这两条源码/测试路径变化，其他 dirty 文档及历史日志保留；R1/R2 完整12项清单逐项比较也仅这两项变化，其余10/10相同，未机械重审未变源码。
- 当前两条源码 SHA 与 R2 冻结相符：Events `5be1544adf5ee8ac31c3a43554c7ceefd33b78a07a7df13ed5b825b5b85fc43b`，LifecycleTest `7eb5c181dff7dccc476b24aee95b5b8073a08b9fbb69a0ff26a350f4a1aec953`。IMPLEMENTATION.md 实际 SHA 为冻结值 `52146fa88639a565dc3f13ca1d3278faf0e519b05a6f411ebf4fd9bb60323577`；已读取 source-baseline、changed-sources、final-consistency 和日志原字节恢复记录，未覆盖旧证据。
- `ReactorRuntimeLifecycleTest.java:113`～178 的四个新用例经反射调用实际生产私有适配和真实 State fixture，未另写镜像计时逻辑。先7活动tick、40次真暂停后三索引仍同份可靠；恢复12tick仍可见，第20累计活动tick到期，重复样本不续期，到期后40次暂停及重复投递不能复活。其余用例验证暂停期间错 dimension 坏包、成员卸载/换槽、世界切换的撤销与恢复不复活。原六个用例保留，原助手改为 Optional 直接断言未弱化三索引要求；即时错维度撤销的原R1用例仍覆盖。
- 实际原始 `red-assertion-lifecycle.xml` 是1个用例、1 failure、0 error/skip，类型为 AssertionFailedError，期望可靠 Optional 而实际为空；`red-assertion.log` 与 command.json 记录测试失败/退出1。首次 NoSuchElementException red 另有保留，没有混同断言 red。`green-lifecycle.xml` 实际列出10个用例、failure/error/skip全0；`green-assemble.log` 同次 test/assemble 为 BUILD SUCCESSFUL，green-command.json 退出0。以上为实施者原运行的独立核验与复用，非本审查新跑测试，也不冒充真实客户端生命周期/视觉验证。
- 当前 build/libs JAR 与 R2 冻结副本实际 SHA 同为 `c4b3ef4c55135017071deada00f52bef25faa87f2e80a1ecb36a35a3e3d9ad3e`。完整 R1/R2 javap 公共API文本实际 SHA 均 `177de2fc074b2fe9ffde92c8af1134466f9fc3d7ff3a351b2f1acb93e68a013a`；额外 Events/State 公共文本实际 SHA 均 `1c0d96e2cf7a86f871a2713f43ad1297b692b4879fbc2623e16454b6e846a246`，与 api-comparison 的字节一致/退出0记录相符。私有适配没有扩展公共ABI。
- 未改服务端/common/协议/燃料注册、L1/CT及资源，复用原自动证据与 `gametest-second.log`：明确1/1 required通过、三维度保存、Game test server shutting down、BUILD SUCCESSFUL；没有因客户端计时窄修重复真实仪表或旧全量验证。

### 本轮明确不裁决的范围

- ART03动画、透明排序及真暂停/恢复客户端视觉：由美术最终候选绑定实际R2同步后观察，JUnit和源码控制流不足以关闭人工门。
- ART-REACTOR-02R1视觉：独立候选及观察门，不由L2暂停修复覆盖。
- 已验收Ponder/玩法、07装配台教学、旧存档迁移：未改或未派发；治理5.1/5.2与本任务写集不允许本审查重开、扩测或推进。
- Git净整合与main/美术树实际同步：由PM执行；本次审查结论允许进行该步骤，不宣称已同步。
