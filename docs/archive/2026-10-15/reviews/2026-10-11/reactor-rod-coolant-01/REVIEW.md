# REACTOR-ROD-COOLANT-01 独立规格与质量窄审

## 最终差异复核结论

**通过；首轮唯一 P2 已关闭，最终必改项无。** 同一审查的整改差异复核，基线仍为 `e50ef067b19296a22e7b2538ccc367f3cda6022f`。首轮发现与中间候选证据完整保留于下文及原证据目录，最终结论以本节为准。真实 GPU 视觉门仍未通过，本结论可供 PM 集成候选，不替代用户复看。

实际重读活动卡追加授权、当前 helper/test、`06-helper-delta.diff` 与 `06-test-delta.diff`。生产差异仅辅助类第 35 行新增 `buffers.endBatch(RenderType.translucent())` 及相应中文注释，阶段过滤、客户端注册与监听、256 槽和原液体层不变。当前源码及 `06-final-helper.javap.txt` 一致：仅在 AFTER_BLOCK_ENTITIES 按 cutoutMipped → translucent → 固定液体定向结束，无无参全批次刷新。仪表最后留下的共享辉光在液体写深度前已提交，原 P2 的可达错误次序被消除；此前原生路径、生命周期、材质与深度状态审查可复用。

专属测试仅新增两项：`lastInstrumentGlowFlushesBeforeLiquidWithSharedOrFixedRod` 和 `interleavedOwnersFlushEveryGlowBeforeLiquidAndKeepUnrelatedSolid`。均通过原 NativeBuffers 父类执行真实调度，覆盖共享/固定棒体、仪表末尾辉光、多 owner 交错及 glow→液体→后续棒体，验证辉光先于最终液体、无遗留辉光、阶段过滤、无关固定 solid 保留与空边界不重复。未放宽原五项断言，也未复制缓冲算法。

独立核读原始 `04-glow-red.log/.exit/.xml`：7 项中仅新增两项发生真实 AssertionFailedError，0 error/skip，exit 1，compileJava 为 UP-TO-DATE；错误列表实际缺失末尾辉光提交，能捕获首轮 P2。最终 `05-final-command.txt`、log、exit、run.json 与三份 XML 相符：7+15+7=29 项，0 fail/error/skip；UTC 2026-10-10 17:21:50–17:22:07，16 秒、exit 0，实际 compileJava/test/jar，compileTestJava 复用 RED 已编译的同一新增测试。未依赖旧全量结果，也未重新执行任何 Gradle 或测试。

独立重算当前/`05-final-candidate/sources/` 三源码，均逐字匹配 `06-final-identities.json`；Renderer 仍为首轮 SHA `51701712aa483fa170566abc88252165140c0db7b72528c16ff4c2a026b286b7`，helper 为 `fabc64b39f5fc0c3158916b5a328348673eb1622226d41a7de2763a0dd7e7db7`，test 为 `e3c761dbbc11cd8d99d055ad5c731070454dda67736285aba2fe5d16f8fd93cf`。实际 build/libs JAR 与最终冻结 JAR 逐字一致：2507570 字节，SHA-256 `0de3aa3c20a99850f56d68f2ab8fd0a5538aadcfb9cfd2628f84553f8500c2d5`。三个生产 class 条目均匹配当前编译输出与最终身份清单；首轮三份冻结源码及首轮 JAR 的原散列仍一致。

再次只读核实六个原受保护源/测试 diff 为零，tracked 实现差异仍仅原 Renderer 入口；原 dirty 日志保留，`06-diff-check.exit` 为 0。本次只增补此报告，无代码、Git、测试、构建、生成器、客户端或真实服操作。最终空/部分/满液位、棒体升降与运行蓝辉需用户真实 GPU 复看，既有美术独立视觉门不由本审查关闭。交 PM，审查者停写。

## 首轮审查记录（历史中间候选）

结论：**需整改；必改项 1 项 P2**。基线 `e50ef067b19296a22e7b2538ccc367f3cda6022f`，审查未提交冻结候选。控制棒先于液体的修复路径成立，但遗漏同一仪表 BER 的燃料辉光批次，改变了原来的辉光/液体绘制次序。本报告不验收真实 GPU 外观。

## 必改项

**[P2] 在提交液体前定向提交燃料辉光，避免加液遮掉运行蓝辉。** 位置：`src/main/java/com/iksxh/create_nuclear_industry/structure/client/ReactorInternalRenderBuffers.java:34–35`。

`ReactorInternalRenderer.java:40–47` 先把有效燃料辉光写入 `RenderType.translucent()`，随后第 56 行请求液体缓冲。原生 `RenderBuffers.java:25` 的固定键是 `Sheets.translucentCullBlockSheet()`，不是 `RenderType.translucent()`。原路径请求非固定液体层时，`MultiBufferSource.java:56–57` 会先结束共享辉光；现在液体有固定键，走第 52–54 行，不再结束辉光。

具体可达顺序为：棒体 BER → 最后一个仪表 BER 的辉光 → 该仪表的固定液体 → `AFTER_BLOCK_ENTITIES`。辉光请求已结束原共享棒体，当前 `lastSharedType` 是辉光；`finish()` 再结束 `cutoutMipped()` 时没有剩余棒体，随即把液体先画入深度，辉光仍留在共享批次。原生 `LevelRenderer.java:1096–1117` 的阶段前刷新不含辉光层；它只能在阶段后的共享层切换或第 1160 行 `endLastBatch()` 才绘制。候选生产字节码也确认 `finish()` 仅结束棒体和液体，没有辉光。

普通透明渲染模式（`Minecraft.useShaderTransparency()==false`）下，`RenderStateShard.java:303–311` 的辉光 `TRANSLUCENT_TARGET` 不切换目标，辉光与液体使用主目标；两层默认 `LEQUAL`，液体保留 `COLOR_DEPTH_WRITE`（`RenderType.java:153–163,764–771,1336,1343`）。较近液体包络先写深度后，后画的浸没辉光会受深度拒绝，破坏原运行蓝辉/浸泡效果。这里确认的是原生调度和深度路径，不声称已观察到 GPU 截图结果，也不推断所有图形模式表现相同。

最小整改建议：仅在现有 `finish()` 中，于棒体之后、任何液体之前定向 `endBatch(RenderType.translucent())`，同步中文说明；不全局 `endBatch()`，不改变 alpha、深度、shader、几何或其他层。仅扩充本卡已有 `ReactorInternalRenderBuffersTest.java`，用真实原生 BufferSource 覆盖棒体→辉光→液体及辉光→液体→棒体，含固定/共享棒体、多液体和重复边界；断言辉光先于液体且阶段后无遗留辉光。当前五项测试全部未请求 `RenderType.translucent()`，原 VisualState/Materials 测试也不能证明该绘制次序。该整改仍在原三文件写集内，由 PM 安排执行者实施，审查者未改实现或重跑验证。

## 已核实证据与边界

- 实际读取 AGENTS、活动卡、DIAGNOSIS、IMPLEMENTATION、治理 5.1/5.2 与 Minecraft modding/testing 技能；依实际 Java 21、MC 1.21.1、NeoForge 21.1.219、Create 6.0.10 审查。八份原生源码逐字匹配本树锁定 sources JAR 的对应条目。
- 注册事件确在原生 RenderBuffers 构造中以 mod 总线发布，游戏阶段事件位于全部 BER 及不透明 Sheet 后。候选 `javap -v` 确认注册类 `Dist.CLIENT + Bus.MOD`、嵌套阶段监听 `Dist.CLIENT` 和 SubscribeEvent；使用同一原生 memoized entityTranslucent 层。256 个现有槽、初始 393216 字节、无类初始化原生缓冲分配成立；液体固定后不因普通共享层切换提前结束，原 shader/深度/混合保持。
- 原 RED 日志、exit 与 XML 一致：5 项、4 项真实 AssertionFailedError、0 error/skip、exit 1。唯一 GREEN 原始日志和三份最终 XML 一致：5+15+7=27 项、0 fail/error/skip；UTC 17:11:58–17:12:16，17 秒，exit 0，实际执行 compileJava/compileTestJava/test/jar。新测试使用原生 BufferSource 的空批次调度，未复制刷新公式；这些通过项不覆盖上述辉光缺口。未将旧 82 套全量结果当作本次证据。
- 独立重新读取并计算当前/冻结三源码，均匹配 `03-candidate-identities.json`；实际与冻结 JAR 逐字一致，2507556 字节，SHA-256 `f3519cc6afb9dad55815228480ee4aa07cebdc9b080084925fceebed5723ff76`。Renderer、Buffers 与 GameEvents 三 class 的 JAR 条目均匹配当前编译输出及清单散列，原生产 javap 与源码路径一致。
- 实际 diff 只改变原 Renderer 的液体入口及窄接缝；棒 BER、Materials、VisualState、生命周期类和两份原测试六个保护路径的只读 diff 为零。原纹理清理仍先全批次提交再回收，槽身份和生命周期规则未改。构建/服务端/模型/控件等无源差异，既有 dirty 日志保留；作用域 diff 空白检查通过。

审查仅写本报告，没有 Git 写、构建、JUnit、生成器、客户端、真实服或再派发。原始证据位于 `build/reports/reactor-rod-coolant-01/`，保持原字节。真实事件运行/GPU 像素未验；整改后仍需用户复看空/部分/满液位、控制棒升降和运行蓝辉，不关闭既有美术独立视觉门。交 PM 处理本项，审查者停写。
