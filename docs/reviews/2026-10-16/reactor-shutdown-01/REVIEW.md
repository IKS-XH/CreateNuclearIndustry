# REACTOR-SHUTDOWN-01 组合窄审

日期：2026-10-16。审查角色：独立执行者。本轮规格/质量组合审查完成，未发现阻断项；两目标均有同批原生成功证据。项目验收、来源接入与Git由PM处理，用户原场景复看仍独立。

## 范围与根因

已读根AGENTS、治理5.1、STATUS、[任务卡](../../../archive/2026-10-16/plans/2026-10-16-reactor-shutdown-diagnosis.md)及封存诊断。基准为main `db1e6aae6237571e31ec5b54d32b7d290f2ec506`；候选为同级`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-shutdown-fix`、分支`codex/reactor-shutdown-fix`。实际Gradle仍锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。

实际读取并使用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`核查服务端快照、热工和事件边界，使用同目录`minecraft-testing/SKILL.md`核查冻结复现、JUnit与原生服务端证据；按治理5.1不重跑测试/构建、不增加全域回归或测试框架。本审查者只写本报告，不改候选代码、核心文档、Git、正式包、配置或世界，不派发其他执行者。

封存的[原算法探针](../../../../build/reports/reactor-shutdown-20261016/pure-probe-with-ports.jsonl)及[现场证据](../../../../build/reports/reactor-shutdown-20261016/port-and-structure-evidence.json)相互一致：实际仪表(-8,-59,0)，16³、149燃料列、47棒全插无卡死，冷/热库存658000/0mB，融毁未启动；新生热和燃耗为0，活动尾热`5.247766665661402e-10 HU`、安全量化标记0。推进1/20/2000tick均未变化，2000tick完整快照与初始相等，持续等待不能消除该状态。离线外壳/端口/物品一致性不冒充运行时owner绑定验证。

Adapter用`1e-9`的整数mB容差筛选HU安全尾数，而完全停机要求活动余热`<=1e-12 HU`，整数mB又不能移除这部分热，形成永久死区。另有3列各`7.5e-13 HU`、合计约`2.25e-12 HU`的原始探针：旧逐列过滤分配0列，独立证明ServerTick同族缺陷。相关Adapter/ServerTick/完全停机代码早于S1/S3已存在，不能仅凭时间邻近归因性能优化。

## 候选实现核查

候选实际功能差异仅卡内5文件：2个生产类、2个JUnit类及既有`ReactorPortLookupGameTests`。JUnit运行刷新日志不属于接入源码。无FullShutdown、Thermal、Ledger、NBT、缓存、管理员、网络、资源、Gradle或其他玩法代码差异。

生产修复只有两个谓词及相邻中文说明：

- `ReactorCoolantSimulationAdapter.hasCapacityForQuantizedRemainder`把HU资格改为严格`>0`，保留真实冷液能力`>WHOLE_MB_EPSILON`。冷液库存和实际已接收整数输入仍是原能力来源，零冷液不能获得安全子集。`wholeMillibucketHeat`、流体转换/输出/容量、余热总账和标记的`min`界限未改。
- `ReactorServerTick.allocateQuantizedRemainder`把每列余额资格改为严格`>0`，让小于单列epsilon的正数仍进入总账与分母。全堆预算/总余热的`1e-12`早退、按坐标有序分配、预算上界和最后列尾差原样保留。全堆总量已不超过停机门时早退不会形成本批死区。

这两处改变的是安全子集的资格，不删除或取整`cachedHeatHu`。下游Thermal继续将实际剩余热量完整存入缓存，只从有效损伤/传播口径扣除安全标记，标记不大于本列缓存；下一tick重新根据当前真实冷却条件计算。没有新增持久化字段、世界引用、跨线程状态或缓存失效路径，现有NBT格式不变。

完全停机`SAFETY_EPSILON=1e-12`和四条件未改，真实缺冷液余热仍阻止安全拆除。仪表预检/端口实际ItemStack捕获、提交前校验、安全清空事务以及新生热/融毁倒计时触发的危险拆除分支均未改。新增GameTest通过真实事件总线发布`BreakEvent`，安全例再实际移除外壳并检查结构失效；拒绝例要求取消事件且快照/实际燃料均不变，未复制维护算法或新增生产测试入口。该既有测试类仍受`ReactorPortLookupGameTests*.class`正式包排除覆盖。

## 原始验证证据

审查只读取实施者已有结果，本轮没有运行以下命令。

| 阶段 | 原始证据 | 结果和边界 |
| --- | --- | --- |
| 夹具准备 | `red-preparation.log` | 初次现场数据占位解析错误保留，不能当作有效业务RED |
| 正确RED | [red.log](../../../../build/reports/reactor-shutdown-20261016/red.log) | 原生产实现、正确最终夹具：18项中15通过、3预期失败，分别是Adapter微小正热、149列现场停机、3列低于epsilon聚合。断言没有为求过删除或放宽 |
| GREEN | [green.log](../../../../build/reports/reactor-shutdown-20261016/green.log)及候选`build/test-results/test/`原XML | 六类55项，0失败/错误/跳过，Gradle exit0。Adapter9、ServerTick9、FullShutdown13、Thermal合同11、Ledger7、PortAggregation6 |
| 首次真实入口准备 | [gametest.log](../../../../build/reports/reactor-shutdown-20261016/gametest.log) | 原生报`No test functions were given!`，尽管外层exit0也不计任何用例通过。锁定NeoForge实际`getTemplateNamespace`优先方法显式templateNamespace，解释仅启用Holder域时的空集；保留原日志，改用既有普通服务端精确运行入口 |
| 同次真实安全拆壳 | [native-final-tests.txt](../../../../build/reports/reactor-shutdown-20261016/native-final-tests.txt)、[native-final-result.json](../../../../build/reports/reactor-shutdown-20261016/native-final-result.json)和对应server.log | 安全尾热实际BreakEvent/清空/移除外壳/结构失效通过；当次1通过、1新增夹具前置失败，不能写成2/2。原生全维度保存后Java exit0；业务失败与进程成功分别保留 |
| 仅失败夹具补跑 | [native-unsafe-tests.txt](../../../../build/reports/reactor-shutdown-20261016/native-unsafe-tests.txt)、[native-unsafe-result.json](../../../../build/reports/reactor-shutdown-20261016/native-unsafe-result.json)和对应server.log | 拒拆前置修正后单例1通过、0失败，真实缺冷液余热仍取消BreakEvent，快照和实际燃料不变；全维度保存后Java exit0。未重跑通过例或全域，未使用用户世界 |

新增JUnit用保存副本中的149列真实坐标、缓存、燃耗尾数和耐久构建夹具，连续20tick检查零新热/燃耗/实际移热、缓存及燃料和整数冷/热库存不变，同时完全停机变为真。正式NBT往返按当前格式去除非权威燃料投影后严格相等；这不是旧存档迁移专项。分散3列边界检查缓存/库存保留，零冷液的现场量级和0.49HU仍为真实短缺。既有55项还覆盖0.5HU整数转换、部分冷却、堵塞热端、共享容积、损伤/传播、完全停机精确epsilon与倒计时拒绝。

真实拒拆首次失败是新夹具误把`tickReactor()`的布尔返回当作“是否完成结算”。已核生产入口实际返回`stateChanged || portChanged`，合法的零变化tick可以返回false。整改仅把该前置改为正式调用后验证`runtimeDescriptor.available`和`sample==旧sample+1`，同时新增`fullyStopped=false`断言；BreakEvent取消和快照/ItemStack不变断言均保留。源中样本只在完成权威结果投影后递增，改后的前置没有跳过tick、伪造成功或放宽拒拆。两生产谓词和55项JUnit实现未变，因此复用原GREEN，只有失败单例补跑符合治理5.1。

## 审查意见与交付边界

候选生产差异与已确认根因匹配，原始RED/GREEN及真实安全/拒绝拆除证据已补齐，未发现阻断项或需扩展范围的整改项。通过范围是本批局部尾热缺陷和对应维护路径；保留所有夹具/入口准备失败，不把两次原生结果合写成某次全部通过。独立旧2.25HU控制棒GameTest不属于本批，不删断言、不计通过。

按活动卡及用户2026-09-22既有自动验收授权，可由PM处理准确来源接入、Git和唯一assemble制品核验，无需另加局部bug合入许可。后续仅补同次来源/JAR身份和既有测试排除证据，不重开全量审查/性能回归。用户原场景拆除复看仍独立待反馈，不能把夹具通过写成用户已操作通过，也不擅自改写用户世界。
