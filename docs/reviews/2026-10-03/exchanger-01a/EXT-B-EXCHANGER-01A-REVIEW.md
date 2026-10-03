# EXT-B-EXCHANGER-01A 合并规格与质量复审

- 身份：独立审查执行者；基线 `3f64678263151f418112a5a7cb6d0c6d03a74743`，候选 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。只读代码、资源、既有日志；未改实现、核心文档或 Git，未启动 Gradle、GameTest、客户端。
- 实际读取并应用：`AGENTS.md`、`docs/project-governance.md` 第 1/4/5.1 节、`docs/superpowers/plans/2026-10-03-ext-b-exchanger-01a.md` 与确认方案、API 静态报告；`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 核对 NeoForge/Create 注册和生命周期，`minecraft-testing/SKILL.md` 区分账本、真实 GameTest 与客户端人工门，`minecraft-resource-pack/SKILL.md` 核对模型和资源。按 `gradle.properties` 锁定 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82；通用技能中的新版本示例未用于本批。

## 发现、定向整改与集成门

1. **原 P1 跨区块停 tick 免费热已按源代码边界整改。** 初版 `current()/publishedHeat()` 只看 `available`、`hasChunkAt` 和 BE 身份，只有源 `serverTick` 扣 HU。锁定 NeoForge 源码 `Level.java:560-566` 让 BE ticker 受 `shouldTickBlocksAt` 控制，`ServerLevel.java:436-439` 由 block-ticking 距离决定；`LevelReader.java:177-184` 与 `ServerChunkCache.java:255-260` 表明 `hasChunkAt` 只要求 FULL。故源 chunk FULL 可读但不 tick、邻区块锅炉仍 tick 时，初版的旧 `viewHeat`/Create `activeHeat` 可继续供热而无扣账。整改后 `NuclearHeatExchangerBlockEntity.canTick():49-56` 对齐原生外层距离门与 `LevelChunk.java:373-380,700-711` 的内部 FULL 状态、实体加载和世界边界门；`publishedHeat` 与流体 Port 均拒绝停 tick 源。`HeatExchangerBoilerBridge` 仅跟踪已发布热源，在 `ServerTickEvent.Pre/Post` 审计；停 tick 时撤销热、失效 capability 并重算仍加载的锅炉，恢复 tick 时沿账本原 `LastTick` 扣逝去时间，再按实际转液预热。没有改写原生锅炉缓存来伪造测试输入。
2. **定向边界证据通过，仍不是自然 ticket 迁移。** 最初测试直接把 `TickingTracker.chunks` 的源层级置 33，原生 `runAllUpdates` 从区域票据重新传播至 31；`validation-liveness.log` 与 `liveness-diagnostic.log` 留有这次夹具失败，不能算生产回归。最终用例 `ExtensionHeatExchangerGameTests.java:171-250` 在独立 batch 内将源真实 `LevelChunk` 的 `FullStatus` supplier 临时置 FULL，并在 `finally` 恢复原 supplier；保留原 BE 与相邻仍 tick 的控制器。50 tick 内核对账本快照不变、公开热=-1、锅炉缓存=0、旧 Port 无效，恢复后 `BlockCapabilityCache` 得到新端口、旧端口仍无效、余热不免费刷新并重新有偿预热到 18。`DEVICE/validation-liveness-fixed.log` 显示 **10/10 required GameTest** 和 `assemble` 通过，测试服正常退出；此证据覆盖受控原生 FULL 停 tick 门，不宣称真实玩家移动造成的 ticket 迁移已自动验收。
3. **配置源文件的 Git 集成门已在索引中消除，最终提交仍须核对。** `src/main/java/com/iksxh/create_nuclear_industry/config/HeatExchangerConfig.java` 实际存在且被 `CreateNuclearIndustry` 引用，`git check-ignore -v` 命中 `.gitignore:15` 的 `config/`。PM 已显式仅将此文件加入索引；只读 `git ls-files --stage` 验证为 `100644 4a6dca39dbf73a9378f6b96096520f21533de32b`。最终候选提交必须包含该文件，否则新 checkout 会缺源文件而无法编译；审查执行者未执行 Git 写操作。
## 已核对的实现与证据

- `HeatExchangerState` 以 mB 和 HU 为单位，默认 18 HU/t、0.5 HU/mB、40 tick 储备，先实际转换 1440 mB 得 720 HU 后下一 tick 发布 18；运行先扣已付热，再按实际转换量补回。热/冷分别限 4000 mB，冷罐空间不足时只转换可容纳量，分数流量不发放免费 HU。无有效负载不转换、只消耗已付储备；无效/非有限密度停止。NBT 保存两罐、储备、分数与时间，恢复先发布 `NO_HEAT`。`HeatExchangerConfig` 采用独立 SERVER 规格和既有 P1 密度，未改反应堆配置/公式。
- 真实负载由上方 Create 储罐底层控制器、引擎/汽笛及原生尺寸/水量限制共同判定；判定没有依赖 `activeHeat`，热回调只读。`BoilerHeater.REGISTRY` 接线为本设备，不暴露盆热级属性/被动标签。上面流体 capability 关闭、其余五面共享热入冷出；端口的 `capabilityEpoch` 使卸载恢复后的旧 handler 保持失效。锁定 NeoForge 源码 `LevelChunk.java:616-622` 在真实卸载调用 `onChunkUnloaded`、`setRemoved` 并清空 BE；`CapabilityHooks.java:160-169` 在 chunk 卸载/加载使该 chunk capability 缓存失效，`BlockCapabilityCache.java:137-152` 下一次查询重新走 provider，本设备 provider 会创建新 Port。因此旧端口失效不应挡住真实重载后的新管道连接。直接调用同 BE 钩子的 GameTest 不产生这些事件，不能替代真实 ticket 回归。活塞和 Create 构造移动受阻。扳手/正常破坏的物品携带账本数据；真实普通破坏 GameTest 使用生存 FakePlayer 铁镐检查单件掉落。
- 材料/配方遵循合同：`c:tubes/steel` 汇入 `c:tubes`；强化板进 `c:plates/reinforced_steel` 与总 `c:plates`，未混入普通钢板；专用管束、耐压接头、工业传感器各使用相应专用标签。同 SKU 合成量与形状为切石 1→2、坚固板/精密构件/钢板竖排、管束 `P P/CRC/P P`，整机 5×5 去四角共 21 格（12 钢板、4 铜片、2 接头、2 管束、1 传感器），`accept_mirrored=false`。没有序列装配、热液桶、新 GUI 或工作盆热级。
- 资源报告 `EXT-B-EXCHANGER-01A-ART.md` 的体积相交为零，单靠它不能排除共面闪烁。我对实际渲染组合的模型面做了独立同朝向、同平面、正面积重叠检查：`shell+core` 与 `shell+core+core_lit` 都是 **0 对**。相接盒体有反向内表面共面，不等同于同向可见面冲突。方块状态实际 79/83 元素；报告 162 个包含独立物品静态组合，性能规模尚属合理。四朝向与两热态的模型/纹理引用见 ART 资源检查；离线预览已看，不替代客户端外观验收。
- 第一轮日志 `build/reports/extension/EXT-B-EXCHANGER-01A-DEVICE/validation.log`：5 个定向 JUnit 通过，9 个 GameTest 中 7 过、2 失败。机械合成失败源于测试输入缺少 Create 原生 `GroupedItems.calcStats()`，修正后继续调用 `MechanicalCraftingRecipe.matches/assemble`；裸罐用例源于 BE `onLoad` 前灌液，已改真实加载后操作。复测 `validation-retry.log`：9/9 GameTest 通过、`assemble` 通过并正常退出；同轮受影响的 2 个新增 JUnit 见 `build/test-results/test/TEST-com.iksxh.create_nuclear_industry.heat.HeatExchangerStateTest.xml`，首轮原始 5 项见 `DEVICE/junit-first.xml`，两轮合计 7 个独特方法。此证据支持已运行用例，不等于真实 chunk ticket 迁移或客户端管路/JEI/视觉门。

## 保留的人工门

客户端仍须核查生存制造、JEI、模型/护目镜，真实反应堆热液经 Create 管道/泵驱动锅炉再回冷端，堵塞/断供/拆放/重载，以及真实跨 chunk ticket 卸载/恢复。现有 `onChunkUnloaded()` 用例直接调用生命周期钩子，新 FULL 停 tick 用例临时控制原生 `LevelChunk` 状态 supplier；两者均不能宣称真实 ticket 迁移和磁盘重载路径已经覆盖。已读定向整改与自动证据，复审范围内未留下已确认的 P1/P2 运行缺陷；候选 Git 保存和客户端人工门由 PM 决定，本报告不作项目验收结论。





