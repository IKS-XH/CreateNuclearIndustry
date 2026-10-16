# RADIATION-01 第二轮定向整改 RR1

2026-10-16；执行者 `/root/radiation_foundation_impl`；候选 `E:/MyMC/NewMod/Create_NuclearIndustry-radiation`、`codex/radiation-foundation`、基准 `5e706e535d97cbcfe167fa4338265b44a610964d`。只修复审RE-REVIEW-01.md的RR1，不重做已闭合R1/R2/R3，不新增玩法/依赖/数值/预算，不派Agent、不改Git/核心文档/用户世界。

## 根因与精确变化

先按 receiving-code-review 核对源码：Load仅dirty几何，Unload撤对应origin源和target任务；几何结果已算而整体pending的Group不会再进入stage3，故可能用旧containment/pathTransmission完成。两项RED在只加入只读查询夹具、未改生命周期失效逻辑时实际复现。修复改动恰以下4个既有文件，61项来源集合没有新增/删除，前后哈希见review-fix-02-source-delta.json：

- `src/main/java/com/iksxh/create_nuclear_industry/gametest/RadiationFoundationGameTests.java`
- `src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationContainment.java`
- `src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationServerEvents.java`
- `src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationSourceIndex.java`

- SourceIndex的Job新增 `pathChunkDependencies`，每个实际处理的DDA格都记其区块，包括当时未知的区块。`chunkChanged` 只合并命中路径区块与既有frameDependencies的组，并复用dirty/射线队列；无关区块不重置任务，不更改实体/源游标。Unload继续先排除源/取消目标任务，并通知相关屏蔽组；Load也通知相同入口。原R1/R2/R3的续接和独立发送控制流不变。
- Containment新增 `chunkFrames` 只读查询既有framesByChunk，覆盖整个已知壳范围，不扫描壳或读取区块。缓存仍变更驱动，8192壳格上限不变。
- ServerEvents在ChunkEvent.Load仅dirty几何/任务；不读取尚未FULL的块、不等待提升、不强载。Unload原有几何dirty仍保留。
- 为可控复现，Containment公开只读ChunkVisibility查询边界，SourceIndex构造器可注入较保守可见性。正式构造器固定真实getChunkNow；即使注入返回true也必须真实已加载才可读取，不能借此加载或制造物料。注入只涉及屏蔽查询，来源/owner仍走实际加载校验，无客户端或配置开关。
- GameTest仍使用本卡文件/原模板。为只选本轮两项，在本类内使用RegisterGameTestsEvent注册：默认原9+fix1四项共13；显式 `cni.radiation.reviewChunkOnly=true` 才注册ChunkLifecycleReview两项。旧方法/断言保留，原9项补明确templateNamespace以保持去除自动holder后的模板选择。Jar原通配排除覆盖这些嵌套类；没有改build.gradle或新建模板/测试框架。默认13项为注册分支/注解静态核对，本轮没有运行这13项；显式分支日志实际运行2项，绝非无测试exit0。

## 命令与失败证据

实际定向命令：

```text
./gradlew.bat runGameTestServer -PgameTestNamespace=cni_radiation_foundation -PgameTestDirectory=run/radiation-foundation-gametest --init-script build/reports/radiation-foundation-20261016/review-fix-02-select.init.gradle
```

报告目录内init脚本仅向runGameTestServer转发 `cni.radiation.reviewChunkOnly=true`，不变更工程构建脚本、依赖或测试目录。测试选择状态另存review-fix-02-selection.json。

1. `review-fix-02-red.log`：首次夹具编译时把Block传给setBlockAndUpdate，缺少defaultBlockState，2个编译错误，Gradle1；修正仅夹具后继续，不把它当行为RED。
2. **有效RED** `review-fix-02-red-compiled.log`：实际运行2项、2项均失败。路径期望6.377551020408164E-5、实际0.0012755102040816326；外壳事件后仍交付0。Java2/Gradle1，三维度保存。此时已具备只读可见性夹具，但生产生命周期仍为fix1逻辑。
3. 修复后 `review-fix-02-green.log`：2/2、三维度保存、exit0。随后仅补事件前后真实源/目标区块仍加载的明确断言；**最终冻结证据** `review-fix-02-delivery.log`：2/2 required、三维度全部保存、Java正常退出、GRADLE_EXIT=0。冻结点为该运行完成的实际来源，此后只做清单核验和报告；assemble已在生产源固定后完成。
4. 生产源在首次GREEN前已固定，随后 `./gradlew.bat assemble`，日志 `review-fix-02-assemble.log`，增量成功/exit0；其后仅补两条被JAR排除的测试断言，未再改生产源。最后核JAR全部生产class与当前编译class逐字节一致，不重跑assemble。不是build，不跑未变JUnit，也没有clean/rerun-tasks。`git diff --check -- src build.gradle`通过。

## 两项断言与证据边界

外壳用实际跨区块合法小堆、扫描器登记的已知几何、真实ItemEntity放射源及外部目标坐标。栈预算2使同一生产Job暂停在已算完几何的stage4；此时只把另一壳区块在注入查询中隐藏，向NeoForge总线投递实际ChunkEvent.Unload事件对象。源/目标区块在事件前后均明确断言真实仍加载且不是隐藏区块。几何查询此时不再授予整壳屏蔽，Job完成场为 **1.953125E-4 D/s**（大于0；路径壳块仍可衰减），随后还原可见性并投递Load。

路径用真实已用组件、28格目标和中间区块里预先存在的实际屏蔽混凝土；初始查询隐藏中间区块，DDA按未知不授予屏蔽并走完，stage4暂停后恢复可见性、投递ChunkEvent.Load。没有setBlock变化可替代该通知，Job必须重算，最终 **6.377551020408162E-5 D/s = 0.05/28²**。

这是实际GameTest服务端、实际源/方块/Job算法、可注入只读可见性和NeoForge总线生命周期入口的证据。两个ChunkEvent由夹具投递，区块实际一直在GameTest加载器中，**没有证明真实区块卸载/从磁盘加载**，也没有真人玩家/多人/客户端验收。外部目标是坐标查询，不是登录玩家。stage4仅通过反射观察以确认夹具触发点，没有篡改Job、Group或Frame缓存。

## 复用、来源与候选

41项JUnit复用targeted-delivery-junit.log/junit-xml原证据；旧13项与配对性能复用fix1的review-fix-01-delivery.log，其来源由second-review-input/source-sha256.json保留。它们是先前实现对应证据，不冒称fix2新跑的13项/41项/性能。本轮新风险仅由上述2项覆盖；未变功能和人工项不扩面。两轮全部RED/编译失败/复审原文均保留。

覆盖当前清单/JAR前已保存fix1的source-sha256/source-files、source-review-fix-01、candidate-jar/candidate-review-fix-01、REPORT、REVIEW-FIX-01、freeze-verification、RE-REVIEW-01和原JAR实物至second-review-input/，原JAR哈希复核一致；first-review-input/保持原样。

| 输入 | 字节数 | SHA256 |
| --- | ---: | --- |
| 初审候选（first-review-input/） | 3417633 | `9F78F1100D0EEAE6545B6A1A92B0416AB504CC081ED160D25B9CB53B0982B46A` |
| fix1候选（second-review-input/） | 3427684 | `6EEFAACBA5F86686016AEC72D4313B9870A52F3F4070FFC67E09BD973057593A` |
| fix2当前候选 | 3429128 | `AFAD58C122257112BD4BA05C275D86D30293AB5546F2D0531FFEDE5D38905817` |


当前source-sha256.json与固定副本source-review-fix-02.json均61项（相对fix1改4项），candidate-review-fix-02.json给出当前身份。候选JAR路径 `E:/MyMC/NewMod/Create_NuclearIndustry-radiation/build/libs/create_nuclear_industry-0.1.0.jar`；本批GameTest/模板/域目录条目0，配置类/索引/仪表资源存在。被ignore的RadiationConfig.java仍显式在61项清单中；它本轮未变，PM提交时force-add。候选未提交，不是main正式包。

用户携带世界没有启动或修改，27文件/13337924 bytes原哈希一致（saved-world-after-verification.json）；原saved-world-sha256.json未改。源码/制品/关键证据最终复核见review-fix-02-freeze-verification.json。客户端视觉/声音/真人重登换维、实际区块装载体验、连续动力物流、多人携带影响和性能仍未验证；验收/Git交PM。
