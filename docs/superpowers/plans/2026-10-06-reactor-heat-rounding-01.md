# REACTOR-HEAT-ROUNDING-01：反应堆总产热向上取整

**状态：** 用户2026-10-06确认联合手测全部通过；本项实现、49项有效定向测试、PM审查及人工门已完成，已随封存/配方批次合入main，见[最终验收](../../reviews/2026-10-06/store-01/ACCEPTANCE.md)。

## 范围与算法

- 单位为每座反应堆每tick的新生裂变热HU；先按原公式计算反馈、完整度倍率和总热上限，再对最终正总量执行一次`Math.ceil`。零仍为零，整数不变，新增热量小于1HU/t。例：99.875→100HU/t。
- 不逐列取整。按原列产热比例分配取整后的总量，确定性处理浮点尾差；无新生热的列保持零。各列之和与权威总量一致。
- 正式tick冷却可用热量采用权威新生热总量加现有缓存余热；仪表遥测采用同一新生热总量。列级热结算使用分配后的列热，不能只改显示或让冷却与损伤账本各用不同总量。
- `rawHeatHu`保留原始诊断值；控制深度、反馈计算、燃耗、完整度倍率和上限配置不改。上限先截断再向上取整，最终上限为原上限的向上取整值。
- 不对历史缓存热或整数mB换热余数再次取整，不修改流体事务、库存、端口流量、锅炉/换热器/汽轮机参数、存档格式或模拟器。

## 执行者与写集

执行者只实施，不修改核心文档/任务状态、不执行任何Git写操作、不再派发。PM管理版本及核心文档。

允许修改：

- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorFissionCalculator.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorServerTick.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorInstrumentTelemetry.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorFissionResult.java`（仅必要的语义说明或不变量）
- `src/test/java/com/iksxh/create_nuclear_industry/reactor/`下直接受影响的既有测试；回归优先放入既有FissionCalculator、ServerTick、Telemetry测试，避免新增测试框架。
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/P1Verify01GameTests.java`（仅更新受总量取整影响的原精确产热比断言；保留原始倍率和燃耗验证，不弱化为宽松通过）
- `docs/reviews/2026-10-06/store-01/reactor-heat-rounding.md`（本项唯一交付报告）

候选工作目录：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，源基线`22c43e31c4af2ed2a0063d2380b6614b625b3a66`。保留既有日志、缓存及主目录launch配置，不清理或修改用户世界。

## 必要验证与交付

按治理5.1增量验证。先以一个定向新增回归证明原实现在分数总量场景失败，然后同一轮相关JUnit与`assemble`；不`clean`、不全量GameTest、不重复启动客户端。只在真实失败或相关代码继续变化时复验。

必要断言：总量只取整一次、多列比例与总量一致、整数/零边界、先限幅再取整、燃耗不变、完全插棒不产新热、缓存余热继续按原账本冷却。默认吸热0.5HU/mB时，正式tick的稳定分数产热场景应形成稳定整数mB转换，且遥测、列热与冷却使用同一总量。保留存量控制/热余数测试；仅按新规则更新失效的数值期望，不删除机制断言。

首轮只运行上述直接相关测试类。GameTest代码需要同步原数学断言并随assemble编译，但无需本项单独启动测试服；真实闭环检查并入STORE-01联合手测。其他模块与封存已有证据复用。报告记实际命令、通过/失败数量、制品SHA256、改动文件和技能使用；不要把编译通过称为GameTest运行通过。

**执行与审查结果：** [唯一报告](../../reviews/2026-10-06/store-01/reactor-heat-rounding.md)记录原实现6.75→7红测失败；相关7类首轮49项中39项未改通过证据复用，两个旧数值期望修正后仅复验对应10项并assemble成功。最终有效49项全部通过；没有启动GameTest或客户端。PM逐项检查四个计算/遥测源码、既有GameTest数学断言和六个测试差异，确认取整只发生一次、权威总量贯通、燃耗及缓存热不另取整，无未解决审查项。当前候选制品与人工状态以[候选说明](../../reviews/2026-10-06/store-01/CANDIDATE.md)为准。

## 必读技能与版本

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/test-driven-development/SKILL.md`
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/verification-before-completion/SKILL.md`

先读AGENTS、治理5.1/5.2与本卡。中文注释，MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82，不升级依赖。技能通用重复测试或提交流程让位于用户/仓库的精简验证和执行者Git禁令。
