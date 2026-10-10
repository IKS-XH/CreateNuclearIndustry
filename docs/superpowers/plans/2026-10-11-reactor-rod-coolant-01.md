# REACTOR-ROD-COOLANT-01：反应堆加液后控制棒消失修复

状态：已自动修复并整合main `79ca4e4`，最终29/29定向检查、增量jar与独立差异复核通过；等待用户主分支客户端视觉复看，见[候选交接](../../reviews/2026-10-11/reactor-rod-coolant-01/CANDIDATE.md)。用户2026-10-11在主分支runClient测试报告：加注冷却液导致反应堆内部控制棒棒体不可见；截图已给出。授权修复该视觉回归，不改变棒体规格、材质、控制深度、冷却液浸泡感或玩法。基线main `e50ef067b19296a22e7b2538ccc367f3cda6022f`；原完整build454/454证据只用于基线，不作为本故障修复证据。

## 工作区与职责

复用同级`E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，分支`codex/art-integration`，已快进同一main基线；原未跟踪BUILD报告与main文字一致，原字节完整保存在该树`build/reports/reactor-rod-coolant-01/preflight/art-integration-BUILD.original.md`，原路径由main跟踪报告恢复。主目录既有.gitignore、日志、样例与运行客户端保留。美术树无并行代码授权；PM不写实现/测试代码，执行者禁止Git写、其他任务派发及核心文档修改。

指定执行者先只读调查原生MC1.21.1/NeoForge21.1.219、Create6.0.10-280/Catnip及本项目两BER：检查加液后真实棒体提交、液体渲染层的透明/深度行为及MultiBufferSource刷新顺序。引用锁定依赖源码或字节码，区分已证实根因与假设，不靠截图猜测或修改液体透明度掩盖。调查唯一允许报告`docs/reviews/2026-10-11/reactor-rod-coolant-01/DIAGNOSIS.md`；探针证据可写本树`build/reports/reactor-rod-coolant-01/`。先交根因和精确最小写集，再由PM确认实施边界。

## 修复与必要验证边界

生产最多涉及`structure/client/ReactorInternalRenderer.java`、`ReactorControlRodRenderer.java`及必要专属客户端渲染辅助类；测试沿用`structure/client/ReactorAnimationVisualStateTest.java`或新增一份有行为意义的渲染回归测试。具体路径由调查后PM补齐，当前不得先写实现。L1/L2同步、服务端库存/热工、控制深度、注册/配方/配置、碰撞、素材/模型/SVG、共享控件、依赖及Gradle保持只读。

先用锁定真实渲染层/缓冲入口建立能捕获实际缺陷的RED；避免仅比较源码字符串或复制新实现公式。修复后只跑受影响渲染/视觉定向JUnit和一次增量jar（同一执行者持有），不clean、不全量、素材生成、GameTest或旧存档矩阵。独立窄审核根因、原生产路径、新回归、实际class/JAR绑定及保护边界。若无客户端GPU可自动执行，明确JUnit/字节码证据限制；最终需用户在主分支复看加液前后、部分/满液位及控制棒升降，不以自动检查关闭视觉门。

执行者实际读取AGENTS、治理5.1/5.2、本卡及`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`，按实际版本应用；原生来源优先本地锁定依赖。应用系统排错、测试先行及完成前验证，新增/修改注释中文。实施与审查仅允许本批`IMPLEMENTATION.md`、`REVIEW.md`报告；原美术与此前构建失败证据保留，不推进其他工程主线。

## 已核实根因与实施授权

PM已实际读取[DIAGNOSIS](../../reviews/2026-10-11/reactor-rod-coolant-01/DIAGNOSIS.md)及锁定原生MultiBufferSource、RenderBuffers、RenderType、注册事件和LevelRenderer边界：液体与棒体原RenderType都不在固定缓冲键中，后续共享层切换会先画液体并写深度，棒体被LEQUAL拒绝。并非库存或actualDepth使棒体停止提交。此为明确客户端排序缺陷，不构成新玩法决策。

精确实施写集只允许：修改`src/main/java/com/iksxh/create_nuclear_industry/structure/client/ReactorInternalRenderer.java`中的液体取缓冲入口及必要测试接缝；新增同包`ReactorInternalRenderBuffers.java`；新增`src/test/java/com/iksxh/create_nuclear_industry/structure/client/ReactorInternalRenderBuffersTest.java`。原控制棒BER、Materials、VisualState、生命周期事件类和全部原测试只读。

通过原生RegisterRenderBuffersEvent注册现有256个槽ID的原entityTranslucent固定批次，初始缓冲共384KiB，随顶点正常增长，不增DynamicTexture或容量。在原生AFTER_BLOCK_ENTITIES事件中先定向刷新cutoutMipped棒体，再定向刷新液体；该边界位于全部BER和原不透明Sheet刷新之后、主要区块透明层之前。类初始化不分配原生缓冲，事件订阅严格限客户端。保留原shader、LEQUAL、COLOR_DEPTH_WRITE、alpha、采光、UV、几何和素材，不全局改变别的渲染器或深度状态。

先建立真实原生BufferSource/注册事件的顺序回归并证明原共享路径失败，不能把缺类编译、无GPU启动失败或源码字符串检查当RED；必要窄测试接缝须保持原行为。覆盖液体先/棒体先、多液体纹理交错、固定棒体键与最终边界顺序，核原渲染层状态。仅最终新类＋原`ReactorAnimationVisualStateTest`与`ReactorAnimationMaterialsTest`定向test及一次增量jar，单执行者持有完整日志/退出码/XML、源与class/JAR身份。复用未变库存/控件/模型验证，独立审查不重复测试。最终候选供主分支runClient复看，真实GPU视觉门仍需用户确认；不停止用户现有客户端。

## 独立复核中的燃料辉光排序整改

第一轮27/27定向测试和增量JAR通过后，独立审查核实仪表BER最后提交时仍有原`RenderType.translucent()`燃料辉光共享批次：液体改为固定缓冲后不再结束该共享批次，若只先刷新棒体再刷新液体，原生`endLastBatch`会在液体写深度之后才提交辉光。这是本修复引入的具体显示回归，须在同一写集中关闭，第一轮证据保留为中间候选。

授权仅修改新增辅助类与专属测试：最终阶段依次定向刷新原棒体`cutoutMipped()`、原辉光`translucent()`、固定液体批次；不全量结束无关层，不改辉光材质、几何、亮度或液体深度。先新增真实原生BufferSource中辉光待提交的失败回归，再做最小修正；因新的已证实风险补一次上述定向测试与增量jar，以新编号保存日志/XML/源/class/JAR，不覆盖第一轮。仍由同一独立审查者复核差异，不重复全量或旧视觉验收。
