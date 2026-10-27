# REACTOR-EXPLOSION-01 规格与质量组合窄审

2026-10-27；唯一组合审查者，非PM。审查树 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix`，分支 `codex/reactor-external-explosion-20261027`，基线 `b9bedf22a7a15b42ff4bd157ecc7abb556ededbe`。初审结论为需整改一项P2失败收尾问题；**同轮整改后最终审查PASS，见文末复审结论**。本文是执行审查报告，不宣布项目验收、人工通过或合入。

## 唯一整改

**[P2] 接管失败须同时清除客户端受损列表并完成原生收尾。**

位置：`src/main/java/com/iksxh/create_nuclear_industry/mixin/ReactorExplosionFinalizationMixin.java:25-26`。当前失败时 `ci.cancel()`，但 `Explosion.finalizeExplosion` 已经执行 `getProfiler().push("explosion_blocks")`，取消会跳过配对 `pop()`。更直接影响是：锁定 `neoforge-21.1.219-sources.jar` 的 `ServerLevel.explode` 在 finalize 返回后仍以 `explosion.getToBlow()` 构造并发送 `ClientboundExplodePacket`；当前 DESTROY 列表仍非空，服务端保留的块会被客户端按原爆炸列表删除，形成幽灵结构。PM提出此包路径后，审查者已直接读取本树锁定sources.jar中的 `net/minecraft/server/level/ServerLevel.java:1153-1183` 独立确认。

把它与 profiler 配对合为一次窄整改：失败时 `clearToBlow()` 并让原生流程自然完成 `pop()`/空列表收尾，或等价正确方案。沿已有第二所有者发布失败场景，补证原块/物料/快照/事故账本仍回滚、返回或实际发包的 affectedBlocks 为空且 profiler 配对。只复核这一差异及对应证据，无须重跑整个矩阵。

## 已核通过的其余范围

- 必读AGENTS、治理5.1/5.2/6、STATUS、本卡与事故设计/原接管卡已读。实际技能入口 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md`，用于锁定生命周期/服务端边界、真实服与定向证据、测试域模板/注册资源。实际Gradle为MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，未改依赖/版本/许可。
- 全部11项源/资源变化在本卡写集；当前bytes/SHA与 `source-manifest.json` 逐项吻合，`git diff --check` 无错误。AccidentExecution夹具仅四方法改包内可见；原玩家取消/拒拆分支、事故Service/Executor/SavedData及热工未改。新增/修改手写注释中文。
- Mixin位于最终列表shuffle后、任何成员loot/删除前；Start最终取消、Detonate后置修改已结束。服务端限定，KEEP/TRIGGER排除；按缓存实际成员和当前真实块ID确认，确认成员后每owner仅收一次预检计划，不因命中多块重复做昂贵捕获。冷态不执行玩家安全清空，不新增危险阈值。
- 危险计划沿既有 generatedHeatHu epsilon/已开始倒计时，复用多owner提交、后检、事件批次与真实组件唯一托管；发布故障发生在任何账本/移料之前，已有第二发布故障日志和断言证明全批回滚。正常成功同机多块/重复finalize去重，仪表/换料口先坏时先托管，账本独立于已毁仪表续接。
- 新路由仅最终列表/已加载owner与成员检查，无新增常态tick体积扫描或强载入口。原生TNT自身破坏未计入事故32块预算；事故执行器未变。新增模板与原事故模板SHA均为 `05D32A221FB90431D8422559549C411AC331BF61794C5516321E67BDE8B5E28D`，Mixin JSON有效；Jar排除仅增加本批测试class与测试域。

## 复用证据及边界

审查只读差异、依赖源、报告、日志和XML，未重新运行测试、启动客户端或打开用户世界。

| 证据 | 核到的实际结果 |
| --- | --- |
| `red.log` | 原生产路由下真实tick产3HU/事故为空，原生TNT实际毁仪表而ledger=false；1个必需函数业务失败，正常保存，进程exit1 |
| `green-initial.log` | 同一原生TNT例1/1，ledger=true，正常保存，exit0 |
| `concentrated.log` 与 `gametest-concentrated-latest.log` | 独立域7/7必需函数、无SKIP，正常保存，exit0/45s；日志异常是刻意注入的第二所有者发布故障，随后回滚和唯一提交断言通过 |
| `junit/TEST-*.xml` | 3类17项：停机评估13、事件2、生命周期合同2；失败/错误/跳过均0 |
| `EXECUTION.md`、`SOURCE.md`、源清单 | 写集/机制/报告描述相符；世界来源前后及目标三份清单相同；本批尚未生成新Jar |

7函数包含真实TNT/点燃苦力怕、仅仪表/仅换料首坏、每例2废料与原名新料返还、HU/冷热工质、重复finalize、当前SavedData往返、仪表毁后完成、Start取消/后置剔除、KEEP/TRIGGER、冷空堆/非成员/无损邻堆、多owner多块/发布失败、原玩家最终取消与危险拆除原因。精确顺序等场景通过Detonate限制真实TNT最终列表；主体TNT/苦力怕保持原生射线，不把辅助夹具冒称自然爆炸全部情况。

本轮当前格式恢复证据为SavedData往返及正常停服保存，不是客户端真人重进或物理卸载。客户端视觉/操作、真人多人及最终候选Jar身份仍未验；原主线三条JUnit失败未改未复测，不称全量build通过。锅炉候选未并入本树，本报告不改变其原手测门。整改后仅复审该失败收尾差异及定向证据。

## 同轮失败收尾差异复审及最终结论

2026-10-27，按PM稳定源信号仅复审本轮三文件及对应证据。**最终审查PASS：唯一P2及同处profiler配对问题已闭合，无剩余P1/P2，可由PM按卡提交稳定源并安排候选构建。** 未代行Git、构建、项目验收或人工门。

- `ReactorExplosionFinalizationMixin.java` 移除 `cancellable`/`ci.cancel()`，失败时清空 `Explosion.toBlow`，随后正常返回原生方法。锁定原生调用顺序下，后续方块/loot及火点循环为空，原生 `profiler.pop()` 仍正常执行；`ServerLevel.explode` 后续读取的同一列表为空。成功分支仍按原流程执行。
- `ReactorExplosionLifecycle.java` 仅同步返回值说明。新本域测试给既有第二发布故障场景追加返回 `Explosion.getToBlow().isEmpty()` 断言，保留原块/快照/物料/无孤立账本与随后多机唯一提交断言；测试选择开关只在值为1时显式跳过另外六函数，默认完整业务仍执行。
- 最终 `source-manifest.json` 与当前11项bytes/SHA完全吻合；相对 `source-manifest-before-finalization-fix.json` 仅上述三项变化，其余八项字节不变。`git diff --check` 无错误。`FIX.md`、`EXECUTION.md`、`SOURCE.md` 的追加说明与实际差异相符。
- `failure-finalization-fix.log:211` 明确返回 affectedBlocks=0，212行原回滚及随后唯一提交断言成功；128-131/213/215行合计六项显式SKIP。实际为 **1项对应业务通过、6项SKIP**，不是七项业务重跑；正常保存、exit0/26s。profiler闭合由移除提前返回及锁定原生 `pop()` 的确定调用链确认，未冒称开启真实客户端debug或捕获真人网络包。

审查者未运行测试。此前17/17定向JUnit与集中7/7的未变实现证据复用；本次仅以上失败分支定向运行由唯一执行者持有。客户端视觉/操作、真人重进/多人和候选Jar身份继续未验，下一步候选构建及集中手测由PM按原门安排，锅炉候选独立门保持。
