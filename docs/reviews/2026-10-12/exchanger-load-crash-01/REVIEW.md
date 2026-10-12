# EXCHANGER-LOAD-CRASH-01 独立规格与代码质量窄审

结论：本批限定修复符合任务合同，未发现必改项。可交由PM处理候选登记及Git；真实客户端在同一候选目录进入原世界的故障门仍待用户复看，本审查不关闭该门或其他既有人工门。

## 审查范围与方法

- 唯一目录：`E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`；只读基线为`d819fb66da0b1c355a324201f4456d73406b3f4f`、分支`codex/reactor-variable-size`。
- 已读根AGENTS、活动计划、`.superpowers/sdd/exchanger-load-crash-01/task-1-brief.md`、完整生产diff与新增测试正文审查包、IMPLEMENTATION，以及本批原始崩溃/RED/GREEN/服务器/assemble与冻结证据。
- 实际应用`minecraft-modding`、`minecraft-testing`、`verification-before-completion`；从gradle.properties/build.gradle及真实服务器日志核定Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82。未采用技能的新版本示例或扩大技术栈。
- 按任务与治理的定向证据复用规则，仅现时读取日志、源码、XML、Git差异和文件/JAR内容哈希，不重跑测试、构建或客户端。不审原候选无关业务，不操作世界，不写Git；本审查唯一写入为本REVIEW。

## 规格与实现核对

1. 原`08:19:33`报告明确为Integrated Server的`beforeServerTick→auditLoadedSources:36→List.copyOf(LOADED)→WeakHashMap.HashIterator.nextEntry`抛CME。当前原报告与冻结副本SHA256均为`9d0944acfac0163c12804387e8fa2763ead155729cef1d20d9c7df2472ba3caa`。这证明真实崩溃发生在弱表快照；具体交错时序由调用链及回归复现支持，不声称栈本身识别了修改线程。
2. `registerLoaded`和`track(active=true)`在登记前检查非空Level及`!level.isClientSide`；客户端onLoad不能再进入服务端两表。`serverSource`仅读取实体Level引用与侧标志，不调用世界查询。撤销路径不依赖当前Level/侧，卸载、脱离世界或客户端撤销仍可安全清理。
3. 两表仍为`newSetFromMap(new WeakHashMap<>())`，全部add/remove和引用快照共用唯一`SOURCES_LOCK`。`auditLoadedSources`的失效移除改走`unregisterLoaded`，ACTIVE撤销改走`track(false)`；没有残留直接无锁结构访问，也没有全局强来源副本。每次List快照只有本次审计期间的临时强引用，保留弱键生命周期。
4. 锁内只做表操作及`List.copyOf`。`current/canTick/pauseHeat/HeatExchangerLine.find`全部在snapshot返回后执行；世界查询、能力失效及其回调不会被表锁覆盖。重入撤销不会迭代原弱表。没有catch CME、吞错、关闭审计或将世界逻辑搬入后台。
5. 原BE的onLoad/suspend、Pre/Post审计次序、停tick热撤销、未加载区块拒绝、Create控制器刷新及PENDING事件过滤保持。原ACTIVE失效仍调用pauseHeat再移除；非供热已加载成员停tick仍撤销能力。生产差异仅本桥的侧过滤与同步边界，不改变热账本、库存、速率、配置、负载、其他弱表或美术。
6. 现时`git diff --name-only -- src/main src/test build.gradle gradle.properties`仅列出已跟踪桥文件；新增未跟踪专属测试已读完整正文并与审查包末尾逐字核对一致。其他既有文档/运行输出改动不纳入本次实现审查，也未被本审查处理。

## 测试夹具与证明范围

新增9项直接调用真实桥的登记/事件入口、实际注册类型正常构造的BE与真实pauseHeat。客户端两表拒绝、无世界拒绝、服务端停tick撤热、移除后不再审计均有行为断言。两张表分别以2048个强持有来源、后台登记/撤销和主线程300次Pre事件做并发回归；后台仅取Level引用与操作表。世界查询及能力撤销断言运行在JUnit主线程，末尾还要求实际观察到审计读取，不能把整段审计跳过作为通过。

世界读取回调与实际`pauseHeat→invalidateCapabilities`回调分别派后台线程撤销两表，等待2秒内返回；若世界/热回调仍持表锁，该后台无法完成，断言即失败。这比同线程可重入锁的自调用更能区分锁是否已释放。静态代码同时核对canTick与line查找在锁外；夹具不声称运行真实ServerLevel下成功line查找的全部路径。

Unsafe只分配测试外部世界边界并写逻辑侧标志，服务侧仍是ClientLevel子类，利用原BE对非ServerLevel的拒绝tick分支；available置为加载状态。真实BE并非Unsafe分配。此夹具能证明登记边界、实际表快照与撤销回调，不能证明真实距离票据、区块tick资格、完整世界加载或用户客户端进入。

`@BeforeAll`只临时写8个既有ConfigValue的cachedValue：HEAT_LEVEL、HU_PER_LEVEL、BUFFER_TICKS、HOT_CAPACITY_MB、COLD_CAPACITY_MB、MAX_LINE_LENGTH、BASIN_HEAT_LEVEL_EQUIVALENT及coolantAbsorptionHuPerMb；每项先保存原缓存，`@AfterAll`逐项恢复原对象并清空记录。缓存以规格默认值支撑原settings读取，未装载/保存文件、改配置规格或增加生产测试API。当前测试配置无启用JUnit并行的设置；绿色XML无初始化/清理错误。该准备不证明真实配置加载，亦不替代真实服务器回归。并发压力回归属于有限交错的观察证据，修复完整性的依据还包括所有弱表访问共锁的静态核对。

## 原始自动证据核对

| 证据 | 实际观察 | 证明边界 |
|---|---|---|
| `06-red.log/xml`及exit | 退出1，9项、5失败；客户两表、无世界及两张弱表CME失败；4项撤销/回调通过 | 有效行为RED，CME栈落在原真实桥快照；01编译失败及03/04夹具初始化失败不算RED |
| `07-final-green.log`、两XML及exit | 退出0/16秒；新桥9项＋原状态21项，共30项，failure/error/skipped均0 | 定向桥与账本回归；不等同进入世界 |
| `08-server-basin.log`及exit | 仅启用`create_nuclear_industry_ext_b_basin`，实际启动7项，All 7 required tests passed，所有维度保存完成并shutting down，退出0/19秒 | 原真实BE加载审计与持续盆供热、断流恢复、原生加工回归；非零用例、非断言后保存挂起 |
| `09-final-assemble.log`及exit | 退出0/1秒；compileJava/processResources均UP-TO-DATE，唯一jar增量 | 现候选打包，不是完整build或客户端验收 |

实际核对原Basin GameTestHolder、PrefixGameTestTemplate(false)、7个原用例和已有`data/create_nuclear_industry_ext_b_basin/structure/basin_empty.nbt`；Gradle的gameTestNamespace/gameTestDirectory属性入口保持，测试目录为build下独立目录。服务器日志首次缺server.properties的异常之后确实继续启动、完成7项和保存退出，不能因此将正常完成误判为挂起，也不隐去该启动信息。

## 冻结身份与组合保护

审查时独立重算`final-binding.json`对应43条当前文件SHA，全部一致，其中34条原快捷换料/ART09输入仍保持原身份；其当前JAR资源/class条目也逐项一致。两源码及桥class证据副本与当前文件相同，测试源码与完整审查包正文一致。

- 最终JAR及`final-candidate.jar`：2678313字节，SHA256 `15c0ab893d080c6aa93c11f05e5e09ef55931979bd4fc8693ad502802db14ddd`。
- 生产桥源码SHA256：`bda4a9f9464876ac92005e50c126edfd2bc83f63cdf842ae7c923751ad6255e9`；测试源码：`f6851bead3dd77ee68e6c0ebc9aceb6c42217a9b02e77df2c5ec055362328272`。
- 当前桥class、冻结class及JAR身份：`1f4e4a0fd7cf4c71325554214f351cf87c02983b5120c35b5f478f540c30c1e7`。
- 旧组合备份SHA256：`896c2c38d984680eb61452d2a6ba5757e4a08e8931a2f19335acc5e26a57d6bf`。
- 独立逐条读取旧、新JAR各2120条目内容：数量相同、无新增，唯一内容变化为`com/iksxh/create_nuclear_industry/heat/HeatExchangerBoilerBridge.class`；其余2119项一致。仅做内容取证，未执行冻结脚本、重新构建或覆盖旧证据。

## 必改项与交付边界

必改项：无。实现及现有自动证据足以支持本次服务端登记/弱表并发窄修的候选交付。仍需用户在同一候选目录重启客户端并进入原“新的世界”，确认原进入存档崩溃未再发生；快捷换料、可变尺寸、ART08/09及其他既有人工门按原合同保持。本审查不验收客户端、不合main、不更改任务状态或推进其他主线。
