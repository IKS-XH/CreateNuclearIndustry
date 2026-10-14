# EXT-A-FUEL-01 / EXT-ART-08 合并规格与质量审查

**身份与结论：** 本轮为执行者只读审查，基于 `codex/ore-acquisition` 的 `a4f36e7` 及当前未提交改动；**需整改**，暂不进入客户端人工门。本报告不代表最终验收或合入 main。根目录 `logs/` 是运行生成物，未作为候选实现审查。

## 有证据的问题

1. **[P2] 料浆流体贴图引用不存在。** `src/main/java/com/iksxh/create_nuclear_industry/content/FuelProcessingContent.java:74,79` 把静止/流动贴图声明为 `create_nuclear_industry:block/uranium_slurry_{still,flow}`；实际新增资源只有 `src/main/resources/assets/create_nuclear_industry/textures/fluid/uranium_slurry_{still,flow}.png`（对应 `tools/art-assets/manifest.json:985,997`），没有 `textures/block/uranium_slurry_*.png` 或将 `fluid/` 映射进方块图集的资源。现有冷却剂在 `textures/block/` 有对应文件，可作同版接线参照。**触发：** 客户端在 Create 储罐、工作盆或其他流体视图绘制料浆时查询该精灵。**玩家影响：** 料浆液面/流体视图显示缺图，违背本批外观和容器可视化合同；`assemble` 与无 JEI 专服加载不能发现客户端精灵缺失。**建议：** 让流体类型返回实际可加载且已进入方块图集的贴图路径，或按现有冷却剂方式安装对应 `textures/block/` 文件；仅核受影响资源引用与客户端外观。
2. **[P2] 护目镜在无料空闲时可持续显示错误暂停原因。** `src/main/java/com/iksxh/create_nuclear_industry/production/CentrifugeBlockEntity.java:74-76` 每 tick 增加 `stableTicks` 时只调用 `setChanged()`；`sendData()` 仅在开批、有效推进、流体/物品变动或维修等路径调用。`pauseReason()` 在同文件 `:114-118` 用 `stableTicks` 区分“稳定中”和“等待料浆”，而 `:219-231` 将该结果供护目镜显示。锁定的 Create `GoggleOverlayRenderer` 在客户端调用 `addToGoggleTooltip`。**触发：** 给空离心机接入非零稳定转速，20 tick 内不加入料浆且不发生库存交易。**玩家影响：** 服务端已进入“等待料浆”，客户端护目镜仍可停留在“转速稳定中”，停机诊断不准确；菜单的服务端 `ContainerData` 路径不受此问题影响。**建议：** 在服务端稳定状态跨越阈值或暂停原因变化时同步显示状态，避免每 tick 无条件发包；只复查该状态路径与对应客户端显示。

## 审查范围与证据边界

- 实际读取 `AGENTS.md`、`docs/project-governance.md` 第 5.1 节、实施卡和已确认设计；读取 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack` 技能，并按仓库 MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 使用其边界。检查了 `git status`、已跟踪差异和 untracked 清单，追阅新生产 Java、注册/语言/菜单/JEI、配方、模型、资源管线及锁定 Create 的扳手、护目镜与流体容器源码。未作 Git 写操作或修改功能代码。
- 复用既有原始证据：`build/test-results/test/TEST-com.iksxh.create_nuclear_industry.production.CentrifugeStateTest.xml` 为 5/5，`...CentrifugeContainerTransactionTest.xml` 为 2/2，均零失败；`EXT-A-FUEL-01-runtime.md` 记录末次 `assemble` 成功，隔离专服日志记录 2872 配方加载、`Done` 和正常保存退出；`EXT-A-FUEL-01-data.md` 与 `EXT-ART-08.md`/`EXT-ART-08/verification.json` 记录 JSON、69 旧图和 13 新图的静态核对。此次未重跑 Gradle、GameTest 或素材负例；针对已跟踪非日志改动运行 `git diff --check` 无空白错误。
- 静态追踪未见标准处理器下确定的双产物/水部分提交或普通挖掘/扳手双份掉落：密闭批次在开始时扣料，完成前复查双槽和水罐；Create 扳手当前版本从同一 `getDrops` 入口取单件，再无掉落地移除方块。此结论仅限代码路径，不代替客户端拆放验证。
- **未实测、暂不评判：** 客户端生存制造及 Create 水洗/制浆实产；Create 储罐/工作盆与料浆桶的手工互通、管道及原版/Create 漏斗；真实轴网变速/过载与维修、菜单/JEI、保存重进、旋转后能力缓存、普通挖掘/扳手放置状态、全部外观和流体动画。无 JEI 专服加载只证明服务端可启动，不能替代这些人工门。第三方组合配方竞争、爆炸破坏与后续燃料链不在本批承诺内，不作阻塞项。

## 两项整改限定复查（2026-10-02）

**复查结论：可进入人工门。** 上述两项 P2 在当前候选中已静态闭合；原“需整改”是首轮审查时点的结论，现由本节替代。本结论仅允许进入既定客户端清单，不是最终验收或合入 main。

1. `src/main/java/com/iksxh/create_nuclear_industry/content/FuelProcessingContent.java:74,79` 已改为 `fluid/uranium_slurry_{still,flow}`；新增 `src/main/resources/assets/minecraft/atlases/blocks.json:1-12` 的两个 `single` 来源与返回 ID 一致。两张 `textures/fluid/uranium_slurry_*.png` 均在新 JAR 内。复查锁定 Create 6.0.10-280 JAR，其 `assets/minecraft/atlases/blocks.json` 也使用 `single` 来源格式。原不存在的 `block/` 引用已消除；客户端实际精灵与液面外观仍由同一人工清单确认。
2. `src/main/java/com/iksxh/create_nuclear_industry/production/CentrifugeBlockEntity.java:73-84` 已在服务端比较旧转速、稳定 tick 和停机原因；转速变化、达到第 20 个稳定 tick 或原因变化时额外 `sendData()`。空机稳定完成会向客户端同步 `NEED_SLURRY` 所需状态，原仅 `setChanged()` 的缺口已闭合。该分支不会在稳定窗中间每 tick 额外发包；有效加工仍沿原路径同步。护目镜实际显示和网络体验留人工门。

限定证据为上述当前文件、锁定 Create 的 atlas 资源、新 JAR 内 atlas/两张 PNG，以及 `EXT-A-FUEL-01-runtime.md` 追加的增量 `assemble` 退出 0；本人只读复核当前 JAR SHA-256 为 `82DBDF2D04A0A49B2FFD213C2A4FF31ABB42F5B368C6CC00965EA7719425EEB9`。既有 7/7 JUnit 与无 JEI 隔离专服证据按治理第 5.1 节复用；本次未重跑测试、专服或客户端，亦未重审其他实现范围。未发现两项改动邻近路径的确定回归。
