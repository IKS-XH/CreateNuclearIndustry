# REACTOR-RUNTIME-COOLANT-03 执行报告

执行者交付未提交候选，等待 PM 的独立规格/质量窄审与后续净同步。自动验证说明普通库存事务不再撤销已有可靠完整运行样本；客户端液面视觉尚待 ART08 的既有人工门，本报告不关闭该门或可变尺寸01的人工门。

## 工作区、规则与技能

- 唯一写工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-runtime-coolant`，分支 `codex/reactor-runtime-coolant`，执行基线 HEAD `b9551380a5e938f04e34e2fd852c19b222e0a6d0`（含 PM 就位的活动卡；功能基线来自 main `8a8f6ad`）。
- 实际读取根 `AGENTS.md`、活动计划 `docs/superpowers/plans/2026-10-12-reactor-runtime-coolant-03.md`、`docs/art/ART-REACTOR-03-INTERFACE.md`。PM 在执行中同步批准了下列精确模板副本路径；活动卡的更新由 PM 完成。
- 实际应用 `minecraft-modding`、`minecraft-testing`、`superpowers:systematic-debugging`、`superpowers:test-driven-development`（含 writing-good-tests 参考）及 `superpowers:verification-before-completion`。按实际仓库锁定 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82，没有复制技能的新版本/许可证/发布示例。
- 无 Git 写操作、子委派、其他工作树源写入、完整 build、clean 或强制 rerun。GameTest 仅使用本树 `build/runtime-coolant-03-red` 与 `build/runtime-coolant-03-green` 独立测试目录；未写 `run/saves`、未使用用户世界。

## 净交付写集与行为

| 路径（相对本工作树） | 实际变化 |
| --- | --- |
| `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java` | 增加 `public void setCoolantInventories(long coldCoolantMb, long hotCoolantMb)` 和私有可靠缓存匹配判断；仅补充原作用域注释的“通用外部快照”限定。原 `setSnapshot` 方法正文原样。 |
| `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorCoolantFluidHandler.java` | 仅两处 EXECUTE 库存写调用改用新入口；数量计算、共享容量、单口预算、SIMULATE/零量、返回值和流体选择原样。 |
| `src/main/java/com/iksxh/create_nuclear_industry/gametest/ReactorRuntimeCoolantGameTests.java` | 新独立域 `reactor_runtime_coolant_probe`，一个真实成型/世界 capability/更新包解码用例。 |
| `src/main/resources/data/reactor_runtime_coolant_probe/structure/empty.nbt` | 原 `reactor_runtime_display_probe:empty` 的逐字副本，109 bytes，原模板只读；不是新生成内容。 |
| `docs/reviews/2026-10-12/reactor-runtime-coolant-03/IMPLEMENTATION.md` | 本执行报告。 |

新入口只接受非负冷热 mB（仍由既有 `withCoolantInventories` 验证），不接收列、燃料、控制、热工、尺寸或容量。只有服务端、未移除 owner、有效结构/列缓存、可靠有效 L1 和可用 L2 的 dimension/owner/generation/geometryRevision/origin/bounds 全部匹配时，才保留旧 `runtimeDescriptor` 原对象。判断只读取已有缓存，不重扫或查询/加载区块。原 `setChanged`、更新通知和控制棒同步沿用相同顺序。

普通库存包携带原完整 L2：sample、revision、serverGameTime、列、mB、容量、HU/转换量都不改；不会投影半份新数据。下一正式成功结算仍由原完整投影路径发布新样本。客户端 State、协议/Codec、Factory、Renderer、缓冲与美术资源均未改；20 活动 tick 租约只由新 sample 续期，重复旧包不能复活已过期显示。通用 `setSnapshot` 的同值外部调用、原提交前失败/结构变化/异常及生命周期撤销路径仍有效。

## RED、GREEN 与退出/保存边界

证据根：`build/reports/reactor-runtime-coolant-03/`。所有命令在唯一工作树执行，日志与 `.exit.txt` 原样保留。

1. 首轮 `01-gametest-red.log`：完整模板名放入 `template` 被锁定 NeoForge 前缀成双命名空间，测试结构启动失败，Gradle exit 1。没有行为断言，不能算 RED。
2. 第二轮 `02-gametest-red.log`：显式 `templateNamespace` 复用旧域，被新域过滤为空；`No test functions were given!`，但 Gradle exit 0。没有运行用例，不能算 RED 或通过。实际本地 `GameTestRegistry`/`GameTestHooks` 源确认 namespace 筛选以 template namespace 为准；因此 PM 批准逐字模板副本，未修改构建配置或原测试。
3. 真实行为 RED 的 `03-gametest-red.log`：仅本批一个用例。真实冷口 EXECUTE 后冷液 100→124mB，包 `available=false/fullEnvelopeFrozen=false`；恢复一次正式结算后，真实热口 EXECUTE 将热液 100→84mB，再次 `available=false/fullEnvelopeFrozen=false`。最终断言明确失败 `fillFrozen=false, drainFrozen=false`，正是原库存 setter 撤销 L2 的缺陷。
4. RED 业务失败后进程停在 `Saving worlds` 超过90秒。先保存 `03-gametest-red-save-thread-dump.txt` 与精确进程命令行 `03-gametest-red-process.json`：Server thread 在 `MinecraftServer.stopServer/ServerChunkCache.tick/ChunkMap.processUnloads`。只结束本批自有 GameTest 子进程 PID35268，保留 `03-gametest-red-termination.txt`；child exit -1、Gradle exit 1，不是自然退出或保存成功，没有为保存停滞重跑相同业务。
5. 修改两个生产文件后，原行为测试不放宽，`04-gametest-green.log`：1/1 required tests passed，27秒、自然 Gradle exit 0；各维度保存完成且 `Game test server shutting down`。原 RED 的保存限制仍按历史原样报告，不能被 GREEN 自然退出覆盖。

真实 RED/GREEN 命令分别为：

```powershell
./gradlew.bat runGameTestServer -PgameTestNamespace=reactor_runtime_coolant_probe -PgameTestDirectory=build/runtime-coolant-03-red --console=plain
./gradlew.bat runGameTestServer -PgameTestNamespace=reactor_runtime_coolant_probe -PgameTestDirectory=build/runtime-coolant-03-green --console=plain
```

## 验证覆盖与最终构建

新真实用例使用正式世界 capability 和 `owner.getUpdatePacket()` 解码后的 L2；同时比较 descriptor 原对象身份与全部 record 字段，不用字符串源码断言。

| 场景 | 实际断言结果 |
| --- | --- |
| SIMULATE 24mB 冷液输入、16mB 热液输出与零量 EXECUTE | 权威 snapshot 原对象、L2 原对象与完整包全部不变。 |
| 冷液 EXECUTE 100→124mB | 只有权威冷液数量改变，L2 原对象与完整包冻结且 available=true。 |
| 下一正式成功结算 | 新 sample 推进一份、revision 前进、time 为本次实际服务端时间、完整包为124/100mB并匹配真实容量与所有列。 |
| 热液 EXECUTE 100→84mB | 只有权威热液数量改变，L2 原对象与完整包冻结且 available=true；下一成功结算完整发布124/84mB。 |
| 同值外部 `setSnapshot` | 立即撤 L2；随后普通库存确实变为132/76mB，但不可用信封原对象保持，不复活；正式成功后才恢复。 |
| 缓存仍有效、真实换料端口已缺失 | 正式 hydrate 提交前早退撤销 L2；后续冷液库存仍能走原8mB事务，但不复活已撤销信封。 |
| 拆角点并重扫无效结构 | 旧冷/热 capability 均拒绝，权威库存不变，L2仍不可用；恢复结构后正式结算才恢复。 |

该真实 fixture 无装料，HU/转换量为0；它验证库存事务与完整信封边界。非零裂变HU、耗尽当tick实际功率、控制棒实际/目标和真实容量的同龄语义由本次原 `ReactorRuntimeProjectionTest` 定向运行覆盖。

唯一最终命令：

```powershell
./gradlew.bat test --tests 'com.iksxh.create_nuclear_industry.structure.ReactorRuntimeLifecycleTest' --tests 'com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptorTest' --tests 'com.iksxh.create_nuclear_industry.structure.ReactorRuntimeProjectionTest' assemble --console=plain
```

`05-directed-and-assemble.log`：25秒 exit 0。原始 XML 已逐字复制到本批 `junit/`：Lifecycle 10/10、Descriptor 3/3、Projection 4/4，共17/17，失败/错误/跳过均0；原断言未改。Lifecycle 明确覆盖重复sample不续20tick租约、新可靠样本恢复、坏包/错几何/错维度/旧世界、owner/成员卸载与替换、真暂停及恢复活动tick到期。未重跑原全量矩阵。

编译日志存在原工程已弃用 API 与 unchecked 提示（首个新树编译28条、受影响增量2条），Gradle 提示已有弃用特性；未更改这些范围。GREEN 新目录第一次启动的 `server.properties` 缺失日志未阻断用例或保存，按原日志保留。

现有 `test` 的 `workingDir=projectDir` 让 NeoForge 自动重写本树已跟踪的 `logs/debug.log`、`logs/latest.log`；已报告 PM，执行者未清理或还原越界生成日志。全树 `git diff --check` 的两处尾空白仅在这两份运行日志；本批四个源/资源路径的独立 `git diff --check` exit 0。活动卡差异由 PM 修改，本候选不能夹带这些路径。

## 冻结身份与交接

`build/reports/reactor-runtime-coolant-03/final-identity.json` 记录四个净源/资源文件 raw SHA256、五个相关 class、两份新/旧模板以及最终 JAR。五个 class 和两个资源的 build 内容逐项与 ZIP 内条目 SHA 一致。`instrument-public-api.txt` 和两份 `*-bytecode.txt` 是最终 `javap` 输出，供独立审查核对实际服务端新入口和两处调用。

- JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，2,637,995 bytes，SHA256 `0c54ddba94777ba45bfa1605410c43005fe6ace33aa80e9ce154241557dfc17d`。
- 仪表源 SHA256：`133943be0014cb9af5e293ddddf46c81a7da001ede9d175d04368399a3ae80fe`。
- FluidHandler源 SHA256：`72e35e558176abdc2a32fd0640dab58d3e9184a2cfc356d04bcab2d01954a13c`。
- 新GameTest源 SHA256：`d1ce8a9ac382e1293feb06c58caaff70d1a0ac5c0646fb9a0ec9cc5293d1cb53`。
- 新/旧NBT的源、安装及JAR SHA256均为 `f2f22ac0aba73d67682c2e07bc725b47fd83142b628da4dabff66b10860adb22`。

交未提交改动给 PM；由 PM 安排一次独立规格/质量窄审和 Git/跨树净同步。客户端显示包格式与原公共消费者签名未改，新增入口实际为 `public void setCoolantInventories(long,long)`，仅供服务端 FluidHandler 两处普通库存事务。ART08 液面闪烁、双面连窗与铅灰玻璃视觉观察仍待用户；不合可变尺寸候选或改其既有门槛。
