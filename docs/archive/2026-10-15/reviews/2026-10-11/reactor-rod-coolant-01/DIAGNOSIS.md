# REACTOR-ROD-COOLANT-01 根因调查

工作区 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，HEAD `e50ef067b19296a22e7b2538ccc367f3cda6022f`。本批只读源码/依赖，未运行 Gradle、GPU 或客户端，未修改代码、资源、世界或 Git。已读实际 AGENTS、活动卡、治理 5.1/5.2、Minecraft modding/testing 和 systematic-debugging，版本保持 MC 1.21.1 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82 / Flywheel 1.0.6。

## 已证实的缺陷与证据

**液体和棒体共享即时缓冲的刷新顺序依赖 BER 遍历；液面先绘制并写入深度时，会遮掉后来提交的浸液棒体。** 这是生产路径中确定存在的缺陷，与用户“加液才消失”一致。未运行用户 GPU，不能把这次源码调查表述为客户端复现或最终视觉通过。

1. `ReactorInternalRenderer.java:47-55` 仅在正液位时提交液面到 `RenderType.entityTranslucent(texture)`；`ReactorControlRodRenderer.java:30-42` 的有效归属/模型提交条件没有液量分支，shaft/head 均提交到 `RenderType.cutoutMipped()`。`rodPose` 仅消费 actualDepth 和行程，不消费冷却液库存。Catnip `DefaultSuperByteBuffer.renderInto` 字节码直接写入传入 VertexConsumer，不另行安排绘制顺序。
2. 锁定原生 `MultiBufferSource.java:42-65`：未注册固定缓冲的 RenderType 切换时，`getBuffer` 立即 `endBatch(lastSharedType)`；`91-104` 在此执行 `RenderType.draw`。**当前 cutoutMipped 本身也不是固定键。** `RenderBuffers.java:22-25` 固定的是 `Sheets.solidBlockSheet()`、`Sheets.cutoutBlockSheet()`、`Sheets.bannerSheet()` 等；将 banner 的 ByteBufferBuilder 取自 cutoutMipped 不会把 cutoutMipped 注册成固定键。对本项目源码及锁定 Create/Ponder/Flywheel JAR 的类常量扫描没有发现 `RegisterRenderBuffersEvent` 消费，因此没有这些模组追加固定 cutoutMipped 的证据。
3. `RenderType.java:153-164` 的 entityTranslucent 只显式设置透明混合、NO_CULL、采光与 overlay；`1333-1343` 的 builder 默认是 LEQUAL_DEPTH_TEST、MAIN_TARGET 和 COLOR_DEPTH_WRITE。`RenderStateShard.java:76-90,264-269,655-675` 证实透明混合没有关闭深度写入。液体 alpha 约 0.45～0.56 并不让深度缓冲半透明；近侧液面仍写入其完整深度。
4. `LevelRenderer.java:1048-1093` 按各可见区块的 BE 列表绘制，没有规定反应堆驱动先于仪表。仪表先调用时，液体进入共享批次；后续驱动请求 cutoutMipped 立即先绘制液体，其近侧深度使较远棒体不能通过 LEQUAL。驱动先调用时，棒体先刷新，后来液体在棒体颜色上混合，结果正常。深度/遍历关系解释了顺序敏感性；完全无液时没有这批液体。

本地原生源来自实际 `build/moddev/artifacts/neoforge-21.1.219-sources.jar`，SHA-256 `a6d08e695f0ac1066342b337d3d9d1689167e6017434820bd329fa97abc4858e`，已提取到 `build/reports/reactor-rod-coolant-01/native-sources/`，包含上述六类及两种事件的完整原生源码。对应项目入口带行号副本、Catnip javap 输出、RegisterRenderBuffersEvent 类常量扫描结果与输入身份清单同在本批证据目录。没有访问/拷贝用户世界，也没有利用旧 build 测试证明本故障通过。

## 建议的精确最小实施写集（尚未实施）

- 修改 `src/main/java/com/iksxh/create_nuclear_industry/structure/client/ReactorInternalRenderer.java`：仅把液体取得 VertexConsumer 的入口接到本批固定液体批次辅助入口；保留 mesh、alpha、UV、法线、采光、纹理更新、辉光与所有有效性条件。
- 新增 `src/main/java/com/iksxh/create_nuclear_industry/structure/client/ReactorInternalRenderBuffers.java`：仅客户端渲染辅助。按现有固定 256 槽纹理 ID，通过锁定 `RegisterRenderBuffersEvent.registerRenderBuffer` 注册原有 entityTranslucent RenderType 的独立固定缓冲；在 `RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES` 中，先定向 `endBatch(RenderType.cutoutMipped())`，再定向刷新本批液体 RenderType。新类自身订阅 mod/game 客户端事件，避免改原生命周期事件类。无新材质/模型/服务端状态，不修改纹理槽或库存含义。
- 新增 `src/test/java/com/iksxh/create_nuclear_industry/structure/client/ReactorInternalRenderBuffersTest.java`：真实原生 BufferSource/注册事件的行为回归；沿用现有 VisualState/Materials 测试作保护，不改旧断言。

这个顺序让每个液体 RenderType 在 BER 阶段固定积累，取得其他共享 RenderType 不会提前画液体；全部 BER 完成时，剩余棒体共享批次先画，然后液体混合。`LevelRenderer.java:1095-1117` 证实 AFTER_BLOCK_ENTITIES 发生在上述全部 BE 提交之后；`1159-1203` 证实其后才进入主要透明区块阶段。沿用原 entityTranslucent 的 shader、alpha、深度测试/深度写入和排序；不以禁用深度测试、降低透明度或加粗模型绕开缺陷。原材料清理入口已有全 BufferSource.endBatch，可提交新固定缓冲；固定槽数仍有界。辅助类必须避免在类静态初始化时分配原生缓冲，分配由注册事件完成。

固定缓冲初始占用为 256 × 原生 entityTranslucent.bufferSize() 的 1536 字节，即 **393216 字节（384 KiB）原生缓冲**，另有 Java 对象/映射与运行时顶点增长；不新增 256 个 DynamicTexture，仍使用原材质槽。真实边界两侧为：全部可见/全局 BER → 原生 solid/endPortal/endGateway 及 solid/cutout/bed/shulker/sign/hangingSign/chest sheet 的定向 endBatch → outline 提交 → AFTER_BLOCK_ENTITIES（本批先 cutoutMipped，后固定液体）→ 后续 debug/endLastBatch 与 translucentCull/banner 等提交 → 区块透明层。该事件不是在所有 BufferSource 批次全清之后；本批液体不会被之前那些不同键的定向刷新带出，边界时必须明确先刷新剩余棒体。

## 实施授权后的必要回归

先建立原生产液体取缓冲入口的窄测试接缝，旧行为不变。用原生 `BufferSource`、原生 entityTranslucent/cutoutMipped 与 `RegisterRenderBuffersEvent`，覆盖液体→杆、杆→液体、两个不同液体纹理以及已注册固定杆的组合，断言液体不会在 BER 交错时提前结束，边界刷新时棒体在液体之前。可以用 BufferSource 子类观察 startedBuilders 和空批次状态，无 GPU 调用实际切换/刷新入口；若增加有顶点的 draw 记录，应只替换 RenderType.draw 的 GPU 边界，不能复制 BufferSource 的调度算法。另核真实液体 RenderType 的 LEQUAL/COLOR_DEPTH_WRITE/混合状态没有变化。RED 必须在原共享液体行为下失败，不能仅查源码字符串或重写一个顺序公式。

获批后仅本批新回归及 `ReactorAnimationVisualStateTest`、`ReactorAnimationMaterialsTest` 定向 JUnit，加一次增量 jar，并核实际 class/JAR 来源绑定；无全量、素材重生成、GameTest、旧存档或客户端自动启动。最终由用户在主分支复看空/部分/满液位和控制棒升降，自动缓冲证据不能关闭 GPU/视觉门。

本轮根因调查完成，建议范围交 PM 补齐实施授权；执行者停在只读调查阶段。

## 独立复核后的已证实辉光排序补充

以上为首轮调查的历史记录。固定液体方案实施后，独立审查确认原生 `RenderType.translucent()` 辉光本身也不是 fixedBuffers 键：仪表 BER 先提交 glow，再取得固定液体缓冲，不再触发旧共享层切换。第一轮边界只刷新 cutoutMipped 和液体，因此末尾共享 glow 留在 startedBuilders，直到原生 LevelRenderer 的后续 endLastBatch 才结束；在普通主目标深度路径上，液面已写的深度会拒绝其后辉光。原生 translucent 的输出目标在不同图形模式下另有差异，不能用空批次测试声称覆盖 GPU/全部图形模式。

真实原生 BufferSource 新增 rod→glow→固定液体及两 owner 交错回归，在第一轮辅助类下得到 7 项中 2 项顺序断言失败（`04-glow-red.xml`），证实液体之前缺少末尾 glow 的结束。整改在原边界增加原生 translucent 的定向 endBatch，形成剩余棒体→辉光→液体顺序，不改材质、几何、亮度、深度/alpha或调用全量刷新。共享/固定棒体、多纹理、无关 solid 留存、阶段过滤和空边界幂等在最终 29/29 定向项中通过；准确最终身份见 IMPLEMENTATION 的最终整改候选与本批 05/06 证据。首轮 01/02/03 证据保留，真实视觉门仍须用户确认。
