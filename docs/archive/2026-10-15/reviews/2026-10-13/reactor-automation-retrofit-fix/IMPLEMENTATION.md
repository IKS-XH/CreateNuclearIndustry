# REACTOR-AUTOMATION-01R 只读诊断报告

> 下方诊断段保留初始只读时点；PM随后精确扩卡后的实际实现、检查与冻结见文末“实施结果”。当前已完成窄修内部验证，待PM独立组合审查与用户局部复看，未自行验收或提交。

2026-10-13。当前阶段仅诊断，尚未修改生产源码/测试/构建/配置，尚未运行Gradle或用户世界，未作Git写入。活动卡为主目录 `docs/superpowers/plans/2026-10-13-reactor-automation-retrofit-fix.md`；01计划/设计、候选AGENTS及 minecraft-modding、minecraft-testing、systematic-debugging 已实际读取。技术栈实际仍 MC1.21.1 / NF21.1.219 / Create6.0.10-280 / Java21。候选 HEAD `6d593df`，原生产来源 `4757f27` 与原4204ab17包保持；原两份日志dirty未处理。

## 结论及现场

故障层是**服务端结构缓存拒绝恢复实际尺寸**，不是接口侧壁位置非法，也不是单纯遥测不同步。原几何扫描成功，因此扳手报告“已成型”；仪表接纳扫描时另作状态尺寸安全校验，失败后客户端静态摘要为无效，所以护目镜显示“未成型或数据不可用”。正式tick同时被无效结构阻止，不能只修改提示掩盖。

候选 `run/logs/latest.log` 已在19:02:37记录所有维度正常保存。随后只读四份region，解析证据写 `build/reports/reactor-automation-retrofit-fix/saved-reactor-entities.json`；没有回写世界。该保存晚于PM开卡时18:39现场，报告按新保存时点陈述，不声称重放了玩家全过程。

- 唯一自动化接口：`(-10,-59,0)`，仪表：`(-8,-59,0)`，相距两格，与用户“隔一格”吻合。
- 周边完整核设备实际包络：`[-16,-60,0]` 到 `[-1,-45,15]`，即16×16×16；底/顶层各256核设备方块。
- 该仪表保存快照却为 `Width=Height=Depth=5`，`ColdCoolantMb=32768`，热液0，燃料/控制棒列均空，SCRAM/融毁标记均false，AutomationClaimed=false。
- 这正满足 `snapshot.dimensions != scan.dimensions` 且 `snapshot != ReactorSnapshot.empty()` 的拒绝分支。由于不再等于默认空快照，反复扳手无法自行恢复。

## 精确链与新建差异

1. `ReactorInstrumentPortBlockEntity.prepareDisassemblyPlan:511–523` 对完全停机拆非仪表部件生成FULL_SHUTDOWN_RESET，nextSnapshot在518行写死 `ReactorSnapshot.empty()`；该构造在 `ReactorSnapshot:113–114` 使用默认5³。
2. `commitDisassemblyPlan:544–550` 清空端口燃料/迁移信封并提交该快照。此时原 `structureScan` 与冷热口绑定仍保留至Post重扫；没有立即撤销旧有效结构缓存。
3. `ReactorStructureLifecycle:139、245–268` 只将变化安排到ServerTick.Post；原流程的物理拆件/补件、各BE ticker与Post之间存在时间窗口。`ReactorCoolantFluidHandler.isCapabilityAvailable:222–239` 核原绑定/structureValid，不核快照尺寸。其 `fill:164–177` 因旧有效缓存可接受冷液并 `setCoolantInventories` 保留当前5³尺寸，于是把默认空快照变为非空。此中间窗口具体输入次序未由运行日志捕获，必须由行为RED锁定，不能冒称已动态复现；现场默认5³+冷液32768与源链一致。
4. `updateStructureCache:306–319` 先存扫描结果，然后仅在快照等于默认empty时允许尺寸恢复；被冷液填充的5³快照被转为 `DiagnosticCode.DIMENSIONS`。调用 `clearPortBindings`、投影不可用和 `invalidateTelemetry` 后早退。
5. `tickReactor:687–691` 无有效结构就不推进原控制/裂变/热工。`sendTelemetryUpdate:809–816` 实际每次均sendBlockUpdated；`write:1696–1700` 写当前无效结构摘要，`read:1760–1766`同步；`addToGoggleTooltip:1637–1639`显示用户所述措辞。静态有效、仅动态缺失会走另一“等待运行数据”提示，不符合此现场。
6. `ReactorStructureLifecycle.rescanInstrumentPortNow:330–337` 返回原几何scan，尽管 `instrument.updateStructureCache(scan)` 可以拒绝该结果；`ReactorInstrumentPortBlock.onWrenched:87–92` 按原scan发消息。两入口相互矛盾因此完全解释。

新建堆在无原绑定可输入冷液前直接由纯empty指定实际尺寸，正常；5³完全停机拆壳仍与empty尺寸一致，因此原五格夹具没有覆盖此次16³退回5³故障。该缺陷由现行可变尺寸与原安全清空边界组合暴露，自动化接口只是本次替换触发点。

## 建议的最小修复与必要写集（待PM扩卡）

优先修原仪表入口，不改流体事务、拆壳安全或热工：

1. FULL_SHUTDOWN_RESET的空状态保留原已验证快照实际dimensions；清燃料、控制状态、冷热液和事故状态的既定语义保持。原5³快照仍与默认empty一致，原事务验证/回滚使用实际nextSnapshot保持。
2. 尺寸接纳门将“完全清空状态”按字段判定，不用包括尺寸在内的record相等判断。允许库存0且没有任何核状态的原安全重建适配合法新尺寸，避免保留实际dims后阻断原完全停机重建。
3. 同版本本次已经受影响的“只有冷液、无核状态”现场可在扫描有效、实际新容量足以容纳全部冷液时恢复尺寸，并保留全部冷液。必须同时无FuelColumns/ControlRodColumns、无SCRAM保存目标/请求、无融毁进度/标记、无热液、无pending燃料迁移；任何一项存在或冷液超过新容量均保持原拒绝。此为当前缺陷状态的无热恢复边界，不允许任意库存/棒/余热热重组，不增加NBT迁移或改用户保存文件。普通后续同尺寸路径保持。
4. 显式扳手重扫返回仪表最终接受后的结构结果，使真实有核状态的尺寸拒绝继续向玩家显示失败，不再返回原几何成功。不能仅改文案或移除尺寸拒绝。

拟生产写集仅：

- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`：reset实际尺寸、精确无核状态恢复门。
- `src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorStructureLifecycle.java`：显式重扫返回最终有效/拒绝结果。

拟验证写集：一个独立 `gametest/ReactorAutomationRetrofitGameTests.java`，命名空间 `cni_reactor_automation_retrofit`，复用原 `cni_reactor_automation:automation_empty`16×8×16模板、构建6³小堆；`build.gradle`仅增加该新类JAR排除，既有开发classpath/测试模板排除保持。无需新模板/材质/语言/配方、修改原01四代表或重跑其他域。若PM偏好沿原01类新增代表，可免新类/构建修改，但运行该namespace会连带原四项，不是最窄运行域。

## 最小RED/GREEN与真实代表

- 6³无管理员安全成型、冷液输入有效；通过真实BreakEvent预检/提交清空，按原事务验证，物理移除一侧壁外壳；在同回调Post之前向仍持原绑定的冷端EXECUTE一次，补自动化接口，再显式重扫。RED应为原几何valid但owner DIMENSIONS、client同步摘要无效/正式tick不产生可用遥测；GREEN必须保持6³、仅保留新输入冷液、控棒新建全插入、摘要与正式遥测有效，外接燃烧室能安全STOP任职。
- 同一代表再构造现场型默认5³、32768冷液、所有核状态空的快照；新尺寸容量足够时恢复且数量不减少。另检查容量不足、热液、燃料/控制棒、SCRAM、事故或迁移信封存在则拒绝，不重组核状态；扳手返回实际拒绝结果。
- 干净6³清空可在完全停机无库存/无核状态时重建另一合法尺寸；固定5³安全清空仍兼容原empty判定。危险或未完全停机拆壳原事件拒绝/事故边界保持，不能测试中绕过这些前提。

正式运行仅新窄域；RED在修源前，GREEN同一行为夹具；真实业务通过和自然保存退出分别记录。生产修复稳定后一次必要增量assemble，原4204ab17包和失败证据保留、源/class/JAR核对，一次独立组合窄审及同次整改。最终只交此次改装/护目镜局部复看，未关闭01全人工门、不派02。当前没有执行这些检查，所有结论除源码/保存事实外均明确为待验证。

## 实施结果

PM精确扩卡后沿同一执行者在原目录完成，实际写集仅五路径：原Instrument、原Lifecycle、新`ReactorAutomationRetrofitGameTests.java`、`build.gradle`、新域`data/cni_reactor_automation_retrofit/structure/retrofit_empty.nbt`。最后一项是PM按锁定NeoForge筛选机制补充批准的唯一测试模板字节别名，不是新美术/玩法资源；与原automation_empty字节一致，SHA256 `71343a478b844ae6b0044f0f2142fae309499d727cfc731d6e5bbf9f6bc104e4`。新测试类与模板均从生产JAR排除，开发classpath保留。生产改动为上述两个入口，无全局Snapshot/handler/红石/控制算法变更。

实际安全清空nextSnapshot保留已验证的旧实际尺寸；新初始化门仅接受无任何核状态且容量容得下的纯冷状态，全部冷液原量保留。干净无库存状态的不同合法尺寸重建保持；任何热液、列、控制、事故或待迁移燃料均不能借该门改尺寸。显式重扫返回owner最终contract/origin和本次原read-count，原Post队列/流体通知顺序保持。

### 真实失败与通过证据

所有日志/夹具/冻结位于 `build/reports/reactor-automation-retrofit-fix/`。实际使用TDD、executing-plans、systematic-debugging与verification-before-completion；任务卡的窄写集、定向验证、禁止Git/转派/重复全量规则优先于通用技能示例。

| 证据 | 实际结果与解释 |
| --- | --- |
| `01-red.log` | 旧生产首轮未执行用例；原模板namespace导致新域筛选为空，server报No test functions，Gradle却exit0。只计启动诊断，不计行为RED。读取锁定NF GameTestRegistry/Hook证明按模板namespace过滤；PM随后批准唯一字节模板别名。 |
| `02-red-behavior.log`、`03-red-diagnostic.log` | 三场夹具错误失败：误把控制棒主体放燃料棒，实际原规则是air；原scan为COLUMN_LAYOUT。因此均不计根因RED，原失败保留。只改本新夹具后继续，生产仍旧。 |
| `04-red-corrected.log` | 旧生产有效行为RED3/3：6³前置扫描valid；真实BreakEvent完全停机清空后、Post前原冷端EXECUTE接受128mB，再换接口，geometry=true/returned=true却owner=false、快照5³。当前保存型5³+32768通过原BE读取后同样拒绝，干净6→7也拒绝。server业务exit3、Gradle1，所有维度自然保存结束；这是确切原故障证据。 |
| `05-junit-green.log`、`junit-green-xml/` | 18/18定向通过，Gradle exit0/20秒：结构摘要7、可变尺寸4、变量scanner4、自动化接口结构3；原断言未改。旧管理员控制、管网/美术/设备域未重跑。 |
| `06-server-green.log` | 同一三个真实代表全部通过，所有维度自然保存、server/Gradle exit0，20秒；不是保存挂起后终止。 |
| `07-assemble.log` | 唯一必要增量assemble，compile/resources up-to-date，jar实际生成，1秒exit0。未clean、未完整build、未重跑其他域。 |

GREEN覆盖范围：

1. **改装窗口**：真实6³→安全BreakEvent→物理拆壳→原handler接受128mB→放自动化接口→显式重扫，owner保6³和128冷液；全棒新建100%实际/目标；生产getUpdateTag中的静态摘要有效，正式tick动态遥测及同龄运行描述可用。再走原换料口装新组件、原Create燃烧室喂煤，管理员可任职且无自行运行许可。
2. **当前保存型恢复**：新Instrument实例使用当前格式NBT读取5³、32768冷液、无核状态，并在正式onLoad/重扫接纳6³；32768原量未丢失，摘要与正式tick恢复。这仅证明当前BE正常读取/重扫代表，不冒称用户世界已改写或完整客户端重启验收。
3. **十项拒绝门**：热液、燃料列、控制棒列、SCRAM请求、SCRAM保存目标、事故已建立、事故进度、事故已发布、60001mB超过本夹具60000mB新容量、待迁移燃料。每项均用实际BE读取→原几何成功→owner拒绝→显式重扫返回失败、摘要无效，完整快照与冷量未改。保存目标/事故发布等构造器相关不变量仍保留。System.out中文分支标签有Windows终端转码，原输出保留；源码十项实际断言和Logger三项通过为验证依据，未据乱码猜结论。
4. **原重建边界**：同一个仪表实例无核/库存状态6→7→5合法重建通过；5³真实安全BreakEvent立即清成原默认empty，改接口后正常成型、摘要/tick可用。危险拆壳、未完全停机拒绝入口未修改，旧已审安全证据复用；本次不冒称重新运行全部旧维护域。

### 制品身份与交接

原包逐字节保留 `baseline-4204.jar`，SHA256 `4204ab17618b6dbbfa056218d6aa3a5c1c4a460c688d233388feb56aa3dd9301`，3138045字节。候选正式 `build/libs/create_nuclear_industry-0.1.0.jar` 与冻结 `reactor-automation-retrofit-candidate.jar` 为同一制品：

`5e04c667fc509a0eb9349bb95faf6c3a15b5d19a961ffdeb29458d00b5c2d434`，**3138597字节**。

`artifact-verification.json`/`source-manifest.json`记录五条本批精确来源；原01冻结47来源中45原样保持，只有本批允许的Instrument/build.gradle变化，Lifecycle本来不在那47条内而本次单独冻结。646已发布class、1546已发布资源与当前编译/复制输出逐项匹配。相对4204只有4个class载荷变化：Instrument、Lifecycle两实际生产入口；Instrument `$1`、`$PortBindingSpec`为行号伴随变化，`companion-instruction-check.json`用原/新JAR的javap实际指令核一致。非目录文件载荷没有新增/删除、既有资源文件未变；新域仅有两个空目录占位，无测试class/NBT文件泄漏。

原用户世界/配置/主目录JAR均未写入、覆盖或搬动，用户runClient进程未终止；测试只在三个本批隔离服务器目录运行。候选root两原日志dirty及其他范围保持；执行者未作任何Git写、未派审查。当前来源已冻结，等待PM按原01审查者做一次组合窄审。此次改装/护目镜局部客户端复看及01全人工门仍待用户，02负载跟随未推进。
