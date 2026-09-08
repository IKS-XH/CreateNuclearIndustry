# P1-MELTDOWN-02 验收归档：服务端融毁事件占位符

**验收日期：** 2026-09-08
**状态：** 已完成
**执行基线：** `c027b077c492bd0a841e0deb039302b1f1083527`
**后续：** `P1-MAINT-01` 必须复用同一事件入口发布 `DANGEROUS_DISASSEMBLY`

## 验收结论

正式服务端反应堆 tick 首次进入 `MeltdownStatus.COMPLETE` 时，会在权威快照提交后发布不可取消的内部 `ReactorMeltdownEvent`。载荷固定包含触发原因、`ServerLevel`/维度键、结构原点、仪表端口位置和完成后的只读 `ReactorSnapshot`；本任务只接入 `COUNTDOWN_COMPLETE`，并为危险拆除预留 `DANGEROUS_DISASSEMBLY`。

`ReactorSnapshot` 使用持久化 `meltdownEventPublished` 标记保证重复 tick、区块重载和存档重载不重发，NBT 格式升级到 `5`；格式 `4` 及更早数据仍按既有燃料投影迁移规则读取，缺少新字段时使用 `false`。全列完整维修重置融毁周期时同步清除该标记，使之后真正进入的新一轮 `COMPLETE` 可以再次发布。

P1 没有注册事故监听器。事件发布不修改方块、实体、物品、燃料组件、冷/热冷却剂、污染或玩家状态，也不生成声音、粒子、火焰、爆炸、废物、残骸或事故日志。

## 实现范围

- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorMeltdownEvent.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorMeltdownEvents.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorSnapshot.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorSnapshotNbtCodec.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorMeltdownStateMachine.java`
- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/P1Meltdown02GameTests.java`
- `src/test/java/com/iksxh/create_nuclear_industry/reactor/ReactorMeltdownEventsTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/reactor/ReactorSnapshotNbtCodecTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/reactor/ReactorMeltdownStateMachineTest.java`

## 验收证据

项目经理在 Minecraft `1.21.1`、Java `21`、NeoForge `21.1.219`、Create `6.0.10-280`、Ponder `1.0.82` 基线上独立审查并验证：

| 验证 | 结果 |
| :--- | :--- |
| `.\gradlew.bat test --rerun-tasks` | 47 个测试文件，228 项 JUnit，0 失败、0 错误、0 跳过 |
| `.\gradlew.bat runGameTestServer --rerun-tasks --max-workers=1` | `All 93 required tests passed :)` |
| `.\gradlew.bat build` | `BUILD SUCCESSFUL` |
| `git diff --check` | 任务差异无空白错误 |

首次独立 GameTest 运行在 NeoForge 原生 `GameTestInfo.tickInternal` 的 FastUtil 迭代器中发生瞬态崩溃，未产生具体测试失败；该故障已在仓库历史基线上记录。立即以单 worker 复跑后 93/93 required GameTest 全部通过，随后运行器停在已知的 `Saving worlds` 退出阶段，由项目经理终止已完成的精确会话。

新增自动覆盖包括首次完成发布、完成前不发布、重复 tick/重载去重、载荷正确、真实 `ClientLevel` 拒绝、NBT 新旧格式兼容、完整维修重新武装，以及发布前后世界方块、实体、端口燃料和冷/热库存不变。本任务无玩家可见效果，按任务卡不要求客户端人工验收。

## 技能与治理

执行者和项目经理均实际使用 `minecraft-modding` 与 `minecraft-testing`，并按仓库技术基线核对 NeoForge 事件、逻辑端边界、JUnit 和 GameTest；没有采用技能中的 26.x 或其他加载器示例。执行者未执行 Git 写操作，提交、文档归档和版本管理由项目经理完成。
