# REACTOR-EXPLOSION-01 执行交付

2026-10-27；执行者交付，未执行Git写操作，未宣布验收。树：`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix`；分支：`codex/reactor-external-explosion-20261027`；基准HEAD：`b9bedf22a7a15b42ff4bd157ecc7abb556ededbe`。

已读取AGENTS、治理5.1/5.2/6、STATUS、活动卡、事故设计及既有事故接管卡。实际技能入口均为`C:/Users/IKSXH/.codex/skills/`下的`minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-ci-release/SKILL.md`、`minecraft-resource-pack/SKILL.md`：分别用于锁定服务端Mixin生命周期、定向JUnit/隔离真实服、保原正式制品和发布排除边界、独立域模板及资源namespace。以实际Gradle核对MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，未更改版本/依赖/许可证。

首次构建前已保原跟踪F29 JAR至`original-F29.jar`，SHA256为`F29D0F8B20646E577A273CF5B768FBF9E9244B30699F2EB28B923DD783FBB13C`，原跟踪日志在`original-tracked/`。本批未运行jar，跟踪JAR仍为原SHA。复核无游戏Java进程且可独占主树`run/saves/新的世界/session.lock`，完整复制到本树同路径，27文件/13,337,721bytes；`world-source-before.json`、`world-source-after.json`、`world-target.json`逐项路径/bytes/SHA完全相同。没有打开或回写用户世界，测试仅使用隔离目录。

根因及修复：原生产入口只有玩家CommonHooks BreakEvent。新增窄Mixin在原生`Explosion.finalizeExplosion`洗牌后、首个方块loot/删除前读取最终列表，先为全部受损有效机组只读预检，再复用原多所有者提交/后检/批量发布/回滚及唯一SavedData实际组件托管。原因标为`EXTERNAL_EXPLOSION`；仅DESTROY/DESTROY_WITH_DECAY参与。正常tick不新增扫描；按缓存真实成员确认后按所有者去重，冷态也不反复捕获端口。危险判断/完全停机评估/核热工/辐射/工质损失/事故破坏预算均沿原实现。

| 验证 | 实际结果 | 原始证据 |
| --- | --- | --- |
| 原生产路径真实RED | 独立域1函数实跑，外爆前真实tick产热3HU、accident=null；真实TNT毁仪表后damaged=true/ledger=false，业务断言失败；正常保存，进程exit1 | `red.log:223-264` |
| 首修同例GREEN | 同一真实TNT函数1/1；正常保存，exit0 | `green-initial.log` |
| 定向JUnit | ReactorFullShutdownAssessmentTest 13/13、ReactorMeltdownEventsTest 2/2、ReactorStructureLifecycleContractTest 2/2；总17/17，0失败/错误/跳过 | `concentrated.log`、`junit/TEST-*.xml` |
| 集中真实服 | 新独立域7/7必需函数实跑、0 SKIP；所有世界正常保存，Gradle/进程exit0，45s | `concentrated.log:101-229`、`gametest-concentrated-latest.log` |
| 资源/差异 | 空模板逐字节同源SHA：`05D32A221FB90431D8422559549C411AC331BF61794C5516321E67BDE8B5E28D`；Mixin注册JSON有效；git diff --check无错误 | `SOURCE.md`、`source-manifest.json` |

集中命令：

```powershell
./gradlew.bat test --tests '*ReactorMeltdownEventsTest' --tests '*ReactorFullShutdownAssessmentTest' --tests '*ReactorStructureLifecycleContractTest' runGameTestServer -PgameTestNamespace=cni_reactor_external_explosion -PgameTestDirectory=run/reactor-external-explosion-concentrated --console=plain
```

7函数覆盖：

- `nativeTntDamagesRunningReactor`：自然tick的真实PrimedTnt射线破坏运行堆创建唯一事故；未受损邻堆继续运行。
- `nativeCreeperDamagesRunningReactor`：点燃真实苦力怕经原生MOB交互爆炸，结构确实被毁并接管。
- `firstInstrumentAndFirstRefuelingConserveCustody`：两个最终列表分别只含仪表/换料口，故该口必为原生首处理成员；各2份废料+原名新料、真实端口清空、无先loot燃料、HU/123mB冷液/456mB热液精确入账、重复finalize去重、当前SavedData保存加载、仪表随后毁后继续至任务完成、无强载。
- `finalCancellationAndFinalRemovalDoNotEnqueue`：LOWEST Start取消、HIGHEST加入成员后LOWEST Detonate清空，原快照/组件不变且不建事故。
- `keepTriggerColdAndNonMemberDoNotEnqueue`：KEEP/TRIGGER、最终列表仅非成员土块真实破坏、冷停机空堆真实破坏均不建事故。
- `multiOwnerRollbackAndUniqueCommit`：同列表多块/共享成员命中两机组；故障注入第二所有者发布失败后全批保原块/原快照/原组件且无账本；随后成功每机仅一任务，重复finalize仍无重复。
- `playerFinalCancellationAndDangerousBreakRemainCorrect`：原生CommonHooks最终玩家取消无事故，正常危险拆除仍为DANGEROUS_DISASSEMBLY。

边界说明：TNT/苦力怕主体保留原生射线列表；端口顺序、多机、取消等精确夹具经真实TNT.tick产生Explosion，并由Detonate限制最终列表，未直接发布事故事件。当前保存加载是SavedData roundtrip及本次正常停服保存，不称真人客户端重进或历史存档迁移。集中日志114-190行异常为有意的第二所有者发布失败探针，155行原入口记录回滚，191行断言证明回滚及后续唯一提交成功；不是隐藏业务失败。

失败证据保留：`fixture-compile-failure.log`为首版夹具误用assessment()方法，改为现有shutdownAssessment()后取得业务RED；`fixture-import-failure.log`为扩场景误写ReactorCoriumBlockEntity包路径，已删除错误import。两项均不计RED/业务通过。

未运行全JUnit、完整融毁/辐射矩阵或锅炉候选测试；3条无关主线JUnit基线没有修改或重新测量。未生成新JAR、未操作main/锅炉树/用户包。本批后续门为PM组合窄审、精确提交生产源，再按独立指令增量jar，最后用户客户端集中手测；客户端视觉、真人重进与多人效果尚未验收。

2026-10-27同轮窄审FIX：失败后直接ci.cancel会跳过原生profiler.pop，且ServerLevel稍后仍可能发送非空affectedBlocks。已仅修失败分支为clearToBlow并正常收尾，附`FIX.md`；新定向真实服1实跑/6明确SKIP、正常保存exit0。此前7/7及17/17保持原轮证据，不冒写为此次重新运行；最终源清单已更新，原清单保留。
