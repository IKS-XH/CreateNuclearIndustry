# REACTOR-RUNTIME-COOLANT-03：独立规格与质量窄审

本轮仅审查冻结候选相对 `b9551380a5e938f04e34e2fd852c19b222e0a6d0` 的两份生产 Java、新 GameTest 和逐字复制模板。实际读取本树 `AGENTS.md`、活动卡、治理协议第5.1节、`IMPLEMENTATION.md` 及原始证据；实际应用 `minecraft-modding`、`minecraft-testing`、`superpowers:requesting-code-review`（含 reviewer 模板）与 `superpowers:verification-before-completion`。锁定栈为 Minecraft 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82。

按本项目5.1节及本轮派发范围复用实现者最终日志、XML和制品，不重跑测试或构建；没有代码、Git、用户世界写入或子委派。本审查者唯一写入为本报告。

## 核查结果

- `ReactorInstrumentPortBlockEntity.java:141` 的新入口只接受两个 mB 数量，调用原 `snapshot.withCoolantInventories`，保留所有列、控制和热工字段。客户端仍拒绝写入；原 `setChanged`、更新通知及控制棒同步的顺序一致。通用 `setSnapshot` 正文逐字未改，同值外部调用仍撤销 L2。
- `ReactorInstrumentPortBlockEntity.java:156` 只读已有缓存，要求真实服务端、owner未移除、有效扫描/列角色、有效L1和可用L2，再核对 dimension、owner位置、当前generation、geometryRevision、origin及bounds。可靠条件成立时不赋值 `runtimeDescriptor`，也不推进 sample/revision/serverGameTime 或心跳计数；没有中途投影、字段拼接、世界重扫或区块加载调用。条件不成立仍走原撤销方法，已不可用对象不会被库存入口复活。
- `ReactorCoolantFluidHandler.java:152`、`:179` 是新入口仅有的两处生产调用，分别为正数量 EXECUTE fill/drain。原返回值、流体选择、共享容量、单物理口预算、SIMULATE/零量、旧capability绑定校验均未改。新入口仍使用原非负数量验证与原保存路径，没有新增独立库存。
- 原正式结算完整投影及五成功tick心跳未改。客户端 `ReactorRuntimeSnapshots.State.receive` 继续拒收重复revision/sample；20活动tick到期、坏包与卸载保留水位，因此库存重复包不能续租或复活已撤显示。客户端State/Codec、Factory、Renderer、缓冲与美术源均没有本批差异；原提交前失败、异常、结构缓存与生命周期撤销路径逐字保持。
- 新 `ReactorRuntimeCoolantGameTests` 只有一个 `reactor_runtime_coolant_probe` 用例：真实搭建、正式世界capability以及 `getUpdatePacket()` 的实际L2解码；比较原descriptor对象身份和完整record，不使用mock或源码字符串断言。SIMULATE/零量、两个方向EXECUTE、下一成功结算、同值外部setter、正式hydrate早退及结构失效均有实际断言。fixture无燃料、转换HU为0这一限制已在执行报告准确说明，非零热工同龄投影复用原Projection测试。
- 新旧 `empty.nbt` 实读源SHA相同，均109 bytes、`f2f22ac0aba73d67682c2e07bc725b47fd83142b628da4dabff66b10860adb22`；原模板未改。独立域过滤与真实运行日志一致，不改变注册、构建配置或原测试。新增职责、单位及边界注释为中文，并与实现一致。

## 原始证据与身份

证据位于 `build/reports/reactor-runtime-coolant-03/`，完整命令见同目录任务的 `IMPLEMENTATION.md`。

| 证据 | 本轮只读核查结果 |
| --- | --- |
| `01-gametest-red.log`、`02-gametest-red.log` | 分别为模板启动错误、namespace过滤后无用例；第二轮虽Gradle exit0仍不是通过。两者未计入行为RED或GREEN。 |
| `03-gametest-red.log:58`、`:59` | 冷液100→124mB、热液100→84mB两处均为available=false/fullEnvelopeFrozen=false；同一真实用例明确失败，准确复现本缺陷。 |
| `03-gametest-red-save-thread-dump.txt:424`、`03-gametest-red-termination.txt`、`.exit.txt` | Server thread停在stopServer/ServerChunkCache.tick；只结束本批自有子进程，child exit -1、Gradle exit1。该次没有自然退出或保存成功声明。 |
| `04-gametest-green.log:101`、`:102`、`:105`及`.exit.txt` | 两处均available=true/fullEnvelopeFrozen=true；1/1通过，27秒exit0。日志另有全部维度保存完成和服务端自然关闭。 |
| `05-directed-and-assemble.log`、`.exit.txt`及`junit/`三份原始XML | 25秒exit0；Lifecycle10、Descriptor3、Projection4，共17/17，失败/错误/跳过0，原断言未放宽。唯一最终命令同时完成增量assemble。 |
| `final-identity.json`、`instrument-public-api.txt`、两份`*-bytecode.txt` | 记录五个相关class和新旧两模板与build/JAR条目一致；实读字节码含新 `(JJ)V` 入口、相应保留条件和FluidHandler两处调用。 |

冻结后一次核对四份净源/资源和最终JAR的当前raw SHA及长度，均与 `final-identity.json` 相符；未重建重复资源矩阵。最终JAR为 `build/libs/create_nuclear_industry-0.1.0.jar`，2,637,995 bytes，SHA256 `0c54ddba94777ba45bfa1605410c43005fe6ace33aa80e9ce154241557dfc17d`。四路径源码差异空白检查通过；活动卡由PM修改，NeoForge生成的两份已跟踪运行日志及其尾空白不属于净生产交付，审查者未清理或修改它们。

## 明确不作本轮判定的范围

- ART-REACTOR-08真实GPU液面、窗口双面CT、铅灰玻璃及鼠标/视觉验收：本批只验证包与库存行为，视觉仍归既定人工门。
- REACTOR-VARIABLE-SIZE-01跨区块成型、运行和视觉验收，以及跨树净应用后的源码冲突处理：本轮只审当前基线，候选尺寸和人工门由原任务及PM后续同步核对，不由本报告接受或推进。
- 首发前旧存档迁移及无关设备、素材、测试警告整改：活动卡和治理5.1/5.2明确排除，未扩大审查或写集。

## 问题与结论

Critical：无。Important：无。Minor：无本批需整改项。

**规格与质量窄审通过，无必改项；此冻结净写集可由PM进行后续Git及跨树集成。** 修复将普通库存写入与外部完整快照改写分开，保留原完整样本及原租约/失效边界，并有准确RED、自然退出GREEN和最终受影响回归证据。此结论不是客户端液面视觉通过，也不关闭可变尺寸候选或ART08的人工门。
