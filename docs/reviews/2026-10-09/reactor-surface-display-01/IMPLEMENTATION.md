# ART-REACTOR-02-L1 实现交付

2026-10-09，执行者交未提交差异，待 PM 独立审查。工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-display`，分支 `codex/reactor-surface-display`，开工 HEAD `000f8e800ba823c559fcedde60b2048dd34bb516`，开工 Git 状态干净。已实际读取根 AGENTS、治理 1.2/5.1/5.2、任务卡、接口合同，以及 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 和 `minecraft-testing/SKILL.md`。实际按锁定 NeoForge 生命周期、现有 JUnit/FML 与隔离 GameTest 模式实施；未套用技能的新版本示例、通用提交或全量测试流程。

锁定 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。只新增显示描述、仪表传输、客户端快照/生命周期和指定测试；固定 5×5×5、运行/库存/热工、原摘要与护目镜保持既有实现。没有 Git 写操作、子 Agent 派发、外树写入或客户端 CT 消费者改动。

## 已编译的消费 API

包 `com.iksxh.create_nuclear_industry.structure.client`：

```java
public static ReactorSurfaceSnapshot ReactorSurfaceSnapshots.capture();
public static ReactorSurfaceSnapshot ReactorSurfaceSnapshots.capture(BlockAndTintGetter context);
public static ReactorSurfaceSnapshot ReactorSurfaceSnapshot.empty();
public Optional<ReactorSurfaceSnapshot.Member> ReactorSurfaceSnapshot.findSurface(BlockPos pos, Direction face);
```

`Member` 是不可变 record，实际访问器类型：`String dimension()`、`BlockPos ownerPos()`、`UUID ownerGeneration()`、`long revision()`、`BlockPos origin()`、`BlockPos maxInclusive()`、`String expectedBlockId()`、`BlockPos pos()`、`Set<Direction> outwardFaces()`。坐标单位为世界方块，bounds 包含两端；位置防御复制，集合不可变。快照没有 Level、BE、NBT 引用；空快照不使用 null。

模型消费者必须在一次 `gatherModelData` 构建范围仅调用一次 `capture(context)`，随后复用返回值。无参入口仅供诊断，不能作为模型会话证明。当前真实 `ClientLevel` 与快照放在同一原子发布信封中；直接真实世界对象或原生 `RenderChunkRegion.level` 必须与该对象完全相同，未知包装、Ponder、旧世界上下文均返回空。消费者仍须按合同检查当前局部方块 ID、同身份/同外平面和材质连接表；此交付没有替消费者决定材质连接。

实物签名见 `build/reports/art/ART-REACTOR-02-L1/api-signatures.txt`，AT 后的原生字段实物见 `render-region-signatures.txt`。

## 描述与生命周期

- `ReactorSurfaceDescriptor` schema 1：dimension、owner、generation UUID、revision、valid、origin/max、成员坐标/预期 ID/外面位掩码。严格校验字段类型、UUID、有效性字节、合法 ID、坐标、边界、重复成员、朝外面和唯一仪表 owner；有效成员数量必须完整覆盖长方体外壳，不能发布部分表面。未知或坏包为空并撤销当前显示。
- 传输安全预算：dimension 最长 128 字符，坐标各轴绝对值不超过 30,000,000，三边各 2～64，成员最多 24,000，成员 ID 仅当前 7 种合法表面方块，坐标数组恰好 3 项，面掩码 1～63 且必须与几何一致。固定键及白名单字符串使合法编码的体积有有限上界（小于 3 MiB）；未增加合法尺寸或配置。失效信封边界收缩到 owner 且成员为空，坏包坐标不进入刷新。
- factory 只在现有扫描缓存更新时投影已经有效的扫描，以 `ReactorStructureDefinition.SIZE` 适配当前结构；读取实际已加载外表面，不再决定成型、不加载缺失区块，内部棒体不进入成员。独立长宽高仅在纯数据模型和测试中表达。
- 仪表新实例产生新 UUID；描述不持久化到服务端存档。sameGeometry 去重后仅 valid/bounds/members 变化推进 revision，重复遥测沿用描述。数据搭载 Create 原有客户端 BE 更新；早于 onLoad/缓存安装的读入保留于 BE，Chunk.Load 的真实 BE 列表或后续 onLoad 核实当前对象后接入。
- 当前 owner 对象可靠且全部必要成员区块加载才有索引。成员卸载整台暂停，成员重新加载自动恢复；owner 卸载仅 suspend，可靠新客户端 BE 包可恢复同一服务端 generation；真拆除 retire，重放新 generation，旧对象/旧 revision/退休身份不能复活。
- 明确覆盖卸载通知在 ChunkEvent 之前/之后两种次序。BE `onChunkUnloaded` 保留原因，随后的 `invalidate` 继续 suspend。世界结束立即撤销；防止 Level.Unload 到 Minecraft.level 清空之间的旧回调重开会话。重叠位置整体降级为空，不择一归属。
- 原生缓存换槽/缩视距并非所有路径都发 Unload，因此客户端游戏 tick 仅核实当前已知描述所需的去重 ChunkPos 的 `getChunk(FULL,false)` 对象/存在性，不查方块或远处 BE、不遍历玩家世界、不强加载。每描述最多 25 个必要区块，N 个可靠 owner 的查询集合上界为 25N，实际按集合去重；失效仅需 owner 区块，卸载后丢弃无用途加载对象。dirty 门避免每 tick 重算成员索引。常规 drop 在缓存移除前发事件，卸载对象有临时标记，不能在同一回调中被重新视为加载。
- 原子发布先于重建。仅变化成员所涉旧/new bounds 的并集外扩一格，区段去重并跳过未加载区块，单描述外扩后最多 216 个区段；重复包、旧包和重复失效没有刷新风暴。

## 锁定 API 审计与最小扩展

证据目录 `build/reports/art/ART-REACTOR-02-L1/api-sources/`：Minecraft/NeoForge 源文件直接从本树生成的 `build/moddev/artifacts/neoforge-21.1.219-sources.jar` 提取；Create/Ponder 源摘自共享树既有 `build/p0-sources/`（只读），并用当前锁定 slim.jar 的 javap 核对 Create 签名。依赖和产物哈希见 `artifact-hashes.txt`。

1. Create `BakedModelWrapperWithData.getModelData` 为 public final，`CTModel.gatherModelData` 为 protected；因此消费须在 gather 构建作用域捕获快照，不能覆写 final 方法。见 `create-signatures.txt` 与对应源码。
2. `RenderRegionCache.createRegion` 将所属 Level 传入 `RenderChunkRegion`；原字段 protected final，没有公开 getter。光照身份方案不能排除 Ponder，因 `WrappedLevel.getLightEngine` 委托真实世界。向 PM 报告后获得仅公开此字段的 AT 扩展，新增 `META-INF/accesstransformer.cfg` 一行目标。ModDevGradle 默认发现并应用，编译/javap 已证明 public final；无需修改 build.gradle，不使用反射或 mixin。
3. Create SmartBE 的 `readClient` 调用 read(clientPacket=true)，真实更新 tag/data packet 路径成立。`LevelChunk.replaceWithPacketData` 可在 ClientChunkCache 缓存安装前读取 BE tag；其后 ChunkEvent.Load 发于安装之后，故通过事件真实列表恢复而不是要求拆放。
4. 常规 ClientChunkCache.drop 先发 Unload，再缓存替换→ClientLevel.unload→LevelChunk.clearAllBlockEntities→onChunkUnloaded→setRemoved→SmartBE.invalidate；Storage.replace 和 updateViewRadius 另有无 Unload 路径，依据上述源码增加有限证据补偿。`ClientLevel.hasChunk` 恒 true，不能用作加载证明，实际采用 getChunk(FULL,false)。
5. AT 只改变客户端字段访问性；公共 BE/同步桥不引用客户端类。客户端订阅严格 Dist.CLIENT，且区块接入先过滤真实客户端与游戏线程。专用服实际启动、真实 BE 与更新包断言均成功，无客户端类加载故障。
6. GameTestRegistry 按 template namespace 筛选，不能通过跨 namespace 模板同时维持指定隔离域。因此按卡许可复制既有空模板到 `data/reactor_surface_display_probe/structure/surface_probe_empty.nbt`，未改原模板或注册入口。

## 自动证据

原始证据均在 `build/reports/art/ART-REACTOR-02-L1/`。必要检查最终结果：

| 检查 | 实际结果 | 证据 |
| --- | --- | --- |
| 指定定向 JUnit + assemble | exit 0，26/26：Descriptor 3、Snapshot 2、Lifecycle 5、原 Summary 7、LifecycleContract 2、GoggleDisplay 7 | `final-unit-assemble.log`、`final-unit-assemble-exit.txt`、`final-xml/`、`final-test-counts.json` |
| 唯一一次隔离 GameTest 专用服 | exit 0，1/1 required passed，476.4 ms，正常保存退出 | `gametest.log`、`gametest-latest.log`、`gametest-exit.txt`、`server-start-time.txt` |
| 公共 API / AT 实物 | javap 验证 capture 两入口、Member 类型与原生 public final level | `api-signatures.txt`、`render-region-signatures.txt`、`create-signatures.txt` |

最终完整命令：

```powershell
.\gradlew.bat test --tests '*ReactorSurface*Test' --tests '*ReactorInstrumentStructureSummaryTest' --tests '*ReactorStructureLifecycleContractTest' --tests '*ReactorInstrumentGoggleDisplayTest' assemble
.\gradlew.bat runGameTestServer -PgameTestNamespace=reactor_surface_display_probe -PgameTestDirectory=run/reactor_surface_display_probe
```

JUnit 覆盖 NBT 往返/非法/巨大/重复、5×5×5 与假设 6×5×8/9×7×5 朝外面和棱角、相邻及歧义 owner、不可变历史快照、revision/重放/旧 token、成员和 owner 撤销恢复、加载前描述、坏包去重、旧世界不可复活。真实 GameTest 直接搭现行合法结构，检验实际更新包 98 块表面、内部棒排除、同扫描版本稳定、拆坏/修复版本递增、同坐标新仪表 generation 变化和显示数据不写存档。

红阶段与必要整改历史保留：`red.log`/exit 1 为 API 尚不存在时的编译失败；`lifecycle-red.log`/exit 1、`lifecycle-red.xml` 是真实 1 用例失败（先移除回调导致同 generation 重载拒绝）。初次 unit-assemble 为初始实现；r1 对应显式卸载、可靠成员区块补偿；r2 对应世界结束防复活及 dirty 去重；r3 对应集成服事件线程隔离/过期区块事件拒绝；final 对应 drop 事件早于缓存移除的标记修正，并归档最终全部 XML。中间受影响检查均有日志与退出码，r3 XML 另保留；未重跑 GameTest、全量 build、旧存档或其他设备玩法。中间候选不是最终验收依据。

最终 JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，SHA256 `67644908B604A11488A35A3EDEF68EF01985A0DDFF3F4998DF48A31936D5923B`。GameTest 2026-10-09 11:01:42 报 All 1 required tests passed，11:01:43 完成正常退出，没有 Saving worlds 停滞或进程终止。

## 差异与剩余门

PM 在执行期间更新本树任务卡和接口合同，属于 PM 文档差异；执行者没有编辑它们。JUnit 自动写入已跟踪 `logs/debug.log`/`logs/latest.log`，因此全树 diff --check 仅在这些运行日志中报告尾空白，未自行清理或 Git 回退；任务 BE 源差异检查通过。集成只取任务代码/专属模板/本报告，不夹带这些运行日志或中间制品。

自动检查不证明客户端视觉或实际并发渲染已人工通过。L1 尚待独立审查；后续美术 CT 接入需按编译 API 使用上下文捕获，并在最终候选定向复看成型/拆坏/相邻结构、棱角/功能孔、跨区块恢复、退出重进、Ponder 降级以及后台区块构建中的世界切换。此交付不宣布美术或换热器人工门通过。
