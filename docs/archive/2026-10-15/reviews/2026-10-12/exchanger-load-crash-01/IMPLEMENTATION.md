# EXCHANGER-LOAD-CRASH-01 执行报告

执行者按Task 1在唯一同级候选 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration` 完成窄修，未执行任何Git写操作。基线 `d819fb66da0b1c355a324201f4456d73406b3f4f`，分支只读核实为 `codex/reactor-variable-size`。本报告不作独立审查或客户端验收结论。

## 事实与改动

原报告 `run/crash-reports/crash-2026-10-12_08.19.33-server.txt` 的实际栈为 `beforeServerTick→auditLoadedSources:36→List.copyOf(LOADED)→WeakHashMap.HashIterator.nextEntry`，异常为ConcurrentModificationException。真实BE的onLoad/suspend两侧均调用静态弱集合；原current/canTick未排除客户端。原崩溃全文与原latest.log只复制到本批证据目录，原世界未打开、搬动或改写。

仅修改生产 `src/main/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerBoilerBridge.java`，新增专属 `src/test/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerBoilerBridgeTest.java`。

- registerLoaded和track(active=true)先核非空服务端Level，客户端/无世界来源不登记。
- SOURCES_LOCK统一保护LOADED与ACTIVE全部add/remove及List.copyOf。任何侧的移除也走相同锁，且不要求世界仍附着。
- 快照保持短暂强引用，原全局表仍为WeakHashMap弱键；没有增加全局强来源集合。
- current/canTick、pauseHeat、HeatExchangerLine.find全部在复制结束、锁释放后执行；不catch CME、不吞快照错误、不关闭审计。
- PENDING、BE生命周期、Create缓存刷新、热工/库存/负载/速率/配置/美术与用户存档未修改。

## RED和最终GREEN

全部日志、XML、退出码及冻结清单在 `build/reports/exchanger-load-crash-01/`。首次生产源与旧组合JAR身份见 `original-sha.txt`，原组合JAR备份 `original-refueling-art09-combined.jar`。

1. `01-red.log` 是首次夹具误继承final实体的编译失败，退出1，不算RED。
2. `02-red.log/xml`：8项、7失败。已取得客户端两表泄漏、无世界撤热及两张真实弱表CME行为失败；额外两项因真实pauseHeat读取未加载配置而失败，夹具问题不算业务RED。
3. `03-red.log/xml`、`04-red.log/xml`：配置装载Proxy碰到锁定FML的sealed ILoadedConfig，初始化失败，退出1；不算RED。
4. `05-red.log/xml`：默认缓存边界已补齐，8项、6失败；多出的sendBlockUpdated空renderer仍属局部夹具问题。
5. **有效完整RED**：`./gradlew.bat test --tests '*HeatExchangerBoilerBridgeTest'`，`06-red.log/xml`，退出1/15秒，9项中恰5项失败：clientLoadedSourceIsNeverAudited、clientActiveSourceIsNeverAudited、unattachedSourcesAreRejectedByBothRegistries及两张弱表并发用例。两并发用例均明确Caused by ConcurrentModificationException；4项撤热/移除/世界回调/实际pauseHeat回调检查通过。生产仍为原源码。
6. **唯一最终定向JUnit**：`./gradlew.bat test --tests '*HeatExchangerBoilerBridgeTest' --tests '*HeatExchangerStateTest'`，`07-final-green.log`，退出0/16秒；新桥9/9、原状态账本21/21，共30/30，零失败/错误/跳过。原始XML为 `07-bridge-green.xml`、`07-state-green.xml`。

线程用例强持有2048个真实注册BE，每张弱表分别由后台单线程反复register/unregister，JUnit主线程调用300次真实beforeServerTick以触发生产snapshot。后台仅操作登记表及获取Level引用，不调用世界查询；夹具断言世界读取与能力撤销在JUnit主线程，且必须实际观察到审计读取。世界读取回调与真实pauseHeat→invalidateCapabilities回调另派线程移除两表，并要求2秒内返回，证明世界/热回调未持登记锁且重入移除安全；移除后下一次事件无新增审计。

局部世界复用仓库已有Unsafe ClientLevel外部边界方式；设定Level逻辑侧标记，服务侧夹具利用真实BE的非ServerLevel停tick分支，不冒充完整ServerLevel。BE按真实注册类型正常构造，available置为已加载。实际pauseHeat仅需8个原规格默认值缓存，测试后还原原缓存；不装载/保存配置文件，不增加依赖或生产测试API。该夹具不能证明距离票据、真实区块tick门或用户进入存档。

## 原独立真实服务端回归

先只读核实 `ExtensionHeatExchangerBasinGameTests` 的GameTestHolder为 `create_nuclear_industry_ext_b_basin`、PrefixGameTestTemplate(false)、7个原用例及现有 `data/create_nuclear_industry_ext_b_basin/structure/basin_empty.nbt`，没有改模板或原测试。

命令：`./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_ext_b_basin -PgameTestDirectory=build/gametest-exchanger-load-crash-01`。

`08-server-basin.log` 记录实际启动7项，All 7 required tests passed，随后所有维度保存完成、Game test server shutting down；退出0/19秒。这覆盖真实服务端BE加载审计、空盆持续付费、供液中断恢复、两种热加工及原生烈焰人路径。测试只使用build下独立目录，没有打开run/saves。不是零用例通过，也不是只断言成功后保存挂起。

## 唯一assemble与候选冻结

`./gradlew.bat assemble`，`09-final-assemble.log`，退出0/1秒；compileJava/processResources均UP-TO-DATE，仅jar必要增量执行。不完整build、不clean，不重跑旧快捷换料/美术/尺寸证据。

- 最终JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，2678313字节，SHA256 `15c0ab893d080c6aa93c11f05e5e09ef55931979bd4fc8693ad502802db14ddd`；同源冻结副本 `final-candidate.jar`。
- 桥class SHA256 `1f4e4a0fd7cf4c71325554214f351cf87c02983b5120c35b5f478f540c30c1e7`，与当前JAR一致；两源与3个新测试class身份见 `final-binding.json`，两源/class证据副本在同目录。
- 按原 `build/reports/refueling-quick-swap-01/final-binding.json` 核34条来源/编译class/资源/ART09输入，全部保持原SHA。逐个比较旧、新JAR的全部2120条目，条目数量一致，唯一字节差异为本桥class。原组合备份SHA仍为 `896c2c38d984680eb61452d2a6ba5757e4a08e8931a2f19335acc5e26a57d6bf`。
- `freeze-identities.ps1` 是本批只读身份取证脚本，输出 `10-final-binding.log`，不会重建素材或改写旧证据。

## 技能与边界

实际读取并应用根AGENTS、活动计划、Task 1，以及 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`；superpowers 6.4.2的systematic-debugging、test-driven-development/writing-good-tests及verification-before-completion。核定Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82；未照技能新版本示例升级栈。按任务卡/现行治理的定向验证优先规则执行，未机械跑全套。

未扩大代码/构建/核心docs写集。JUnit/FML自动更新已有根logs，属于运行输出，未主动编辑或回退；PM同期文档修改保留。报告与本批证据为允许路径。

**仍需独立窄审与用户在同一目录重启进入原世界复看。** 本批自动证据不宣称客户端崩溃门已验收，不关闭快捷换料、可变尺寸、ART08/09原人工门，不推进其他主线。
