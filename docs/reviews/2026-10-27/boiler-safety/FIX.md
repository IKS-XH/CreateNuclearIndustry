# 同轮组合审 R1～R4 整改交接

2026-10-27。基线仍d884b2c3，两卡允许写集；遵PM5a67c001补充边界，执行者无Git写/核心文档/JAR。当前未提交源已稳定待同审者只复审整改差异。最终路径/SHA为source-fix.json，历史source-02.json及失败原始记录保留，不追改原审查语境。

## 逐项对应

- **R1**：MoltenCoriumEffects公共窄事务返回RemovalReceipt(DEFER/REJECTED/REMOVED,writes)。首个getBlockState前loaded守卫；预算不足整组或目标/两邻读取域暂缺为DEFER，不动Boiler游标。最终取消/异常/原对象替换终结拒绝，实际提交才计writes。corrode仍读取同一收据writes，保既有回调/重试节奏；没有改反应堆或熔融物算法。预算/读域恢复后重试同目标；追加调用方稳定身份在最终BreakEvent回调之后、写入之前复核，监听中同态换件也不会被旧事务移除。
- **R2**：干烧掉落抑制核原LootContext的BE与事故目标凭证，不再查移除后已为空气的现场。Shell及Exchanger只在实际不同旧方块的新onPlace失效同址旧目标（连同旧计划执行资格）；onRemove不触发事故/不提前释放原件认领。原件普通生存移除不掉完整设备，随后同态BE/无BE新件真实扳手/采掘均恰好一件，不被旧计划处理。
- **R3**：目标BE在真实事故冻结时获得一次NeoForge PersistentData UUID；计划仅存稳定身份和原块类型，不比较整个运行NBT或易变LIT。原BE自然tick及当前NBT保存/恢复后仍匹配，物品实际重新放置的新BE没有原凭证；无BE部件依R2的实际放置失效收据。不是save时刷新旧整份快照，也未研究旧格式。
- **R4**：仅真实BlockItem.setPlacedBy走loadPlacedData：真实Ledger原样载入，新放置实例使用新世代，解绑已经交付旧现场的pending和已领取事故计时；未提交危险进度保留，普通chunk NBT读取不解绑。旧SavedData任务仍留旧维度继续，爆裂已清工质不会重新出现，局部干烧携带热量不复制。
- **P3**：配置开启压力注释删去已取消的“堵塞保护”。

## 定向实证与保留缺口

- fix-unit-final.log及fix-unit-final-xml：五类锅炉**52/52，0失败/错误/跳过，exit0/17s**。新增实际落位只解绑已提交收据的纯状态断言。未重复全JUnit；前一轮865=851pass/3既有fail/11skip仍按原来源复用，不称当前全量全绿。
- native-fix-final.log：**19个注册场景的已执行核心断言通过、0整场SKIP、exit0/24s，正常All dimensions saved/shutdown**。含原12回归，以及不足8格装配台预算保持游标后下tick足额提交、真实读取域缺口生产不加载/夹具显式恢复后提交、BE与无BE原件/同态新件掉落守恒、最终监听内部同态换件、原BE自然tick/LIT/运行NBT变化后重载计划仍实际执行并拒新实例。
- R4核心是**真实生存扳手取出一件带pending的Controller，再以原版BlockItem.place在Nether耗尽该物品重新落位**；pending=null、新世代、结构inspect合法、旧现场任务继续完成、真实热量仍在且无额外工质均已实跑。
- **R4完整“目的地已成型交易”子项未能自动验证**：Nether隔离夹具即便建立ENTITY_TICKING/RegionTicket、blocksTick=true/border=true并有界等待，areEntitiesLoaded仍为false，故owner.current按生产规则拒绝。明确日志`SUBCASE_UNAVAILABLE actual formed transaction`，不把这个子项算作通过，也不放宽生产current或用SKIP伪过。票据仅测试夹具并恢复原状态，生产没有强载；实际玩家进入Nether放置并交易加入本批集中手测，交同审者判断。

原native-fix-a编译类型缺口、fix-b与relocate各轮失败/诊断日志保留；fix-b实际5个整改场景过、R4夹具前置失败，relocate各聚焦轮其余场景明确SKIP。最终报告没有把这些SKIP当业务通过。没有另起全熔融矩阵、全JUnit、用户世界加载或额外人工批次。

除R4所列子项及原客户端显示/粒子/事故视觉、正常真人保存重进、物理卸载/多人人工边界外，无新增未交付范围。原人工清单仍集中一次；本报告只表示执行者交件，最终结论由PM/同审者决定。待源审稳定提交后才按PM指令唯一增量jar。

## 同轮R3最后窄差异：凭证置脏不通知输出邻居

Target.bind写一次内部UUID后改用getChunkNow(...).setUnsaved(true)，保该原区块真实保存，不调用be.setChanged()触发输出信号的第二邻格读取。只此一处生产差异；读取域夹具复用为区块x14箱子、x15实心导体、x16第二邻格未加载，凭证冻结前后断言未加载，环境事务DEFER后由测试夹具显式恢复读取域，原游标/目标最终完成。

native-fix-read-domain.log：**仅1个对应场景实跑通过，其他18场明确SKIP**；Gradle exit0/23s，正常All dimensions saved/shutdown。不把日志的19required标记当作19次新业务通过；其它19核心/52单元沿未变实现复用，R4完整目的地形成交易子项继续独立未验。无全量/全域重跑。最终路径/SHA更新为source-final.json，旧source-fix.json保历史，生产源再冻结等待同审者看这一delta。
