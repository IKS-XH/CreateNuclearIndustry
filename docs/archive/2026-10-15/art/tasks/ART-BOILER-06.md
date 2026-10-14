# ART-BOILER-06：锅炉汽段雾效与两种蒸汽材质

用户2026-10-11直接要求锅炉运行中的上部汽段透窗可见蒸汽，并重做普通/超临界两种蒸汽的贴图与流动动画。沿用已选A压力舱、Create风格与SVG源，不重开05B外壳或既有人工门。下述短设计已获用户批准，素材已安装、实际容量前置已交接，客户端消费接通中。

唯一工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`；实际HEAD `9878fd8`，05B未提交候选及证据保持，所有Git写操作禁止。主工程/逻辑树只读。美术负责人主持方案/任务与报告，执行者实现；共享网络、服务端与注册不属于自主写集，需要时与主PM协调。根AGENTS、治理1.2/5.1、美术README/PLAN及本卡必须实际读取；minecraft-modding、minecraft-testing、minecraft-resource-pack技能按锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6应用，中文手写注释、不升级。

## 当前派发：只读技术核实

原消费者 `reactor_ct_audit` 只读Window/Controller/BoilerState/Structure/L1、客户端渲染及原反应堆透明批次辅助，核实真实蒸汽同步、绝对隔层/内域、实际采光与遮挡/裁剪入口、暂停/卸载边界，给出最小接入方案和精确拟写路径。禁止扫描客户端BoilerStructure或据窗口Fill/输出口种类猜成型/库存；两种蒸汽始终独立真实库存。无代码/测试/构建/运行游戏写权。只允许短报告 `docs/art/reports/ART-BOILER-06-AUDIT.md`；没有新增共享授权，不自创未交接口。

原SVG作者 `reactor_svg_impl` 可只读现有两种蒸汽SVG/PNG、实际FluidType引用、atlas语义与04工具，核实准确四sprite路径、现有动画元数据/alpha与新动画可复用规则。当前不画新稿、不生成/安装PNG、不修改任何旧源或工具；仅回复拟映射与必要素材范围。

## 保留边界

用户指定的是美术运行动画与蒸汽材质，不改两种蒸汽身份/产量/温压/容量/热账本/输出选择/阀门/碰撞/配方/存档。低分辨率素材必须原创整数rect SVG，有限色板与crispEdges，游戏PNG只能确定性导出。待实际技术核实与短设计确认后追加唯一作者、精确写集、状态输入/安全降级、一次必要定向验证/增量制品及同轮独立审查；真实透窗/GPU/流体atlas动画仍交用户复看，不把自动结果当视觉验收。

## 已批准短设计（2026-10-11）

用户明确回复“按此方案制作（推荐）”：普通蒸汽乳白灰，超临界蒸汽青白；绝对隔层上方的真实汽区呈半透明缓慢翻卷/向上流动。总真实库存/服务端汽容量控制浓度，超临界库存/总量控制连续混色，气体始终覆盖汽区而无液位，库存零无雾，停热但仍有库存保留雾。运动表示材质动画，不声称真实流向或流量。两流体still/flow与内部效果共用SVG雾纹来源。没有重开05B风格或给其他视觉门追认。

只读[审计](../reports/ART-BOILER-06-AUDIT.md)已由负责人完整读取：两种mB与真实几何已同步，无需新网络。唯一共享缺口为Controller私有steamCapacityView的公开只读访问器；不得用客户端ledger默认Settings重算。公共ABI与共享同步由PM给实际HANDOFF后再消费，当前不能修改Controller/L1/共享同步。新Controller BER可通过原NF扩展负责汽区AABB、offscreen与距离裁剪，保留原窗口水位入口，单炉单雾网格，不每窗重复画整域。

## 阶段一：SVG雾纹与素材候选（现生效）

原作者 `reactor_svg_impl` 仅新增 `tools/art-assets/boiler-steam-06/` 的32份原创帧SVG（普通/超临界、still/flow各8帧）、有限色板/明确映射、专用导出/检查工具、generated与预览/README；报告仅 `docs/art/reports/ART-BOILER-06-ASSETS.md`，证据仅 `build/reports/art/ART-BOILER-06/assets/`。旧steam SVG、04/05B工具与所有旧generated只读，不调用旧全量pipeline。

映射保持现有四sprite：`fluid/steam_still`、`fluid/steam_flow`、`fluid/supercritical_steam_still`、`fluid/supercritical_steam_flow`，即原 `textures/fluid/` 下四同名PNG；当前每帧16×16，alpha只0/255。新帧同样16×16整数rect、crispEdges/有限色板，云纹有大中小团的灰阶层次及可追踪连续上移/翻卷，不能只是整块平色或四散单像素。同类型still/flow共享语言，flow方向更清晰；两类型保留乳白/青白辨识度。PNG确定性导出四张16×128垂直帧图，四mcmeta采用8帧、frametime=2、interpolate=true。保留流体alpha0/255，内部半透明由渲染顶点控制；不是水液位。

内部连续混色消费四sprite同源帧。候选工具应另外导出32张独立16×16内部帧，按 `textures/boiler_steam/{normal,supercritical}/{still,flow}_<0..7>.png` 的候选映射提供；当前只在generated导出，不安装。两类型同坐标遮罩与纹理细节便于逐像素连续混色，配色不同，不改变蒸汽注册/材质层。内部消费者对真实配比与动画帧混合，不能以普通atlas循环冒充实际库存混色。

一次源/帧/引用及连续循环检查，必要少量写前拒绝例；由bundled Python `C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe -B -X utf8`执行。预览包含普通/超临界still+flow动画、透窗低/高浓度、混合与汽区范围示意；负责人实际复看后才追加四正式PNG/四mcmeta/内部帧的安装写权。当前不写游戏资源/Java/测试、Gradle/游戏/Git；不要建设全仓冻结矩阵。

## 当前后续消费边界

等待PM容量getter的实际ABI及精确同步证据后，负责人再激活独立Renderer/VisualState/Materials/RenderBuffers/ClientEvents与两测试写集。一次gather/render只读同一可靠归属/几何及库存/实际容量；异常/未知/撤销不绘制，不扫描结构。采真实内部空气光，owner局部坐标；汽雾批次在全部BER之后、区块窗玻璃之前统一结束，避免在单owner中提前flush。原反应堆批次只读，真暂停冻结动画及partial，换世界/卸载/reload撤销几何与安全释放纹理。

### 客户端独立准备写集（现在生效，实际容量消费仍待交接）

原消费者 `reactor_ct_audit` 可新增 `boiler/client/BoilerSteamVisualState.java`、`BoilerSteamMaterials.java`、`BoilerSteamRenderBuffers.java`、`BoilerSteamClientEvents.java` 与 `BoilerSteamRenderer.java`，测试仅同包 `BoilerSteamVisualStateTest.java`、`BoilerSteamRendererTest.java`；报告 `docs/art/reports/ART-BOILER-06-CONSUMER.md`，证据 `build/reports/art/ART-BOILER-06/consumer/`。没有既有生产文件写权。当前先实施不依赖未交ABI的纯显示状态/几何、混色/资源/时钟/批次和对应测试；Renderer采样入口及完整验证在PM实际getter同步后才接通，禁止反射private字段、NBT绕取或写推测getter调用作为候选。

同一消费者持有全部客户端类/两测试，内部接口由其统一定义，不产生新的公共网络/服务端合同。显示输入只包含可靠身份、闭边界/绝对隔层、两非负mB与服务端实际汽容量mB；Long合计防溢出，非法/容量非正/库存零安全无雾。密度按clamp(total/capacity,0,1)单调增加，可对alpha作柔和曲线，但不能改变真实数据；颜色按sc/total连续混合并使用同源32帧。全汽区world连续bounds为[minX+1,maxX]×[partitionY+1,maxY]×[minZ+1,maxZ]，向内epsilon，owner局部坐标/UV世界锚定；少量有界横/竖雾片增加深度和翻卷读感（单炉最多24个quad，非每cell枚举/非每窗绘整域），任何顶点不可落入水区/隔层/壳体。一个已混色材质配一个网格，不能把两种完整透明盒叠加。

材质原始帧不可变、动态槽有界，逐像素混色/帧插值含alpha，绑定世界session/owner代次和可复用几何；资源reload/坏资源无雾，不用过期GPU槽。只在安全边界提交后释放，已有Reactor类/材质槽/批次只读。Renderer用本次快照与实际Controller槽位/descriptor identity对齐，不以运行Status/Produced/窗口Fill门控存在；独立MOD事件只登记现有CONTROLLER_BE，原Window BER完整保持。覆盖真实汽区AABB/offscreen/到汽区距离，未知数据不画且恢复小包围盒；不无限距离。内部实际空气采光，禁止宿主黑格通用光或满亮；世界时间tick/partial真暂停冻结，不运行服务端账本或真实流体转换。

测试先写必要行为断言：0/单种/混合与容量、单调浓度/极值合计、真实可变汽区所有顶点/隔层、可靠身份撤销/局部冲突/换世界、暂停tick+partial、真实材质混色与循环帧、AABB/offscreen/距离裁剪和指定批次阶段。禁止源码字符串镜像、默认配置冒充服务器值、重复L1/反应堆生命周期矩阵或纯test-only算法。纯准备阶段可保留小范围真实RED/失败性质，但缺类编译不是行为RED；最终候选稳定后唯一命令 `./gradlew.bat test --tests '*BoilerSteamVisualStateTest' --tests '*BoilerSteamRendererTest' assemble --console=plain`，不clean/rerun/full build/GT/游戏/Git。待素材安装与PM实际接口一起绑定源/class/36PNG+4mcmeta/JAR并记录原始退出码/XML，不先重复assemble；一次整批独立审查之后交用户透窗/管/罐复看。

### 阶段一安装生效

负责人已完整读素材报告/映射和真实author/build/check原日志：均退出0，32源/四atlas/四mcmeta/32内部帧同源、尺寸/alpha与配对遮罩通过。已实际浏览器看四组循环、48%混色及100%超临界的低22%/高82%浓度透窗，并看32帧总览；乳白/青白辨识、像素云团/上行纹与明暗保持批准设计。此为素材复看，不是最终网格/GPU或游戏视觉验收。

现只追加原SVG作者40资源安装写权：`src/main/resources/assets/create_nuclear_industry/textures/fluid/{steam_still,steam_flow,supercritical_steam_still,supercritical_steam_flow}.png`及四个对应`.png.mcmeta`，以及 `src/main/resources/assets/create_nuclear_industry/textures/boiler_steam/{normal,supercritical}/{still,flow}_0..7.png`。按mapping仅复制已有generated，36PNG+4mcmeta一次逐字节核对；不重生成、不改其他旧素材/模型/注册/Java/Gradle。原素材报告追加实际安装结果与工具README进度，候选源/历史检查保持；完成即停写，最终唯一test+assemble由消费者持有。

### 实物接口及最终客户端消费生效

主PM已交[实际HANDOFF](../../reviews/2026-10-11/boiler-steam-capacity-view-01/HANDOFF.md)，美术净同步`a3540b2`、文档同步后HEAD `b8cb60a`。负责人已完整读合同/HANDOFF/独立窄审，核美术控制器源SHA256为`843abb4ec1a3fbe5517db4f248cf5505582e90b9f5baad1d5b5078f15e48c796`、原始编译ABI为`public int syncedSteamCapacityMb()`、唯一共享增量jar退出0。现激活原消费者全部五类/两测试写集，新增Renderer采样并以该实物getter为分母。只能在完成原clientPacket读取后的客户端渲染入口消费，不能在L1通知回调混用新几何与旧容量；核当前owner对象/世界与descriptor/L1全部身份字段后一次采两真实库存及容量。共享一源仍只读。

素材已一次复制安装40/40逐字节一致、exit0，来源和历史校验复用，不再生成。消费者透明插值旧版已由单项真实行为RED核实（白色半透明误变灰色）；修为按alpha加权RGB后反除alpha，三层混色共用，必要独立数值回归并入最终两测试。单项RED证据保持，不另跑整批准备验证。

最终仍只执行上述一次定向test+assemble。原05B冻结JAR保留，06在`build/reports/art/ART-BOILER-06/consumer/`独立冻结交付JAR、原始日志/退出码/XML和精确源/class/40资源/共享一源依赖清单；不全仓矩阵，不重测旧L1或反应堆。消费者完成停写后由原独立审查者只读同一实现，报告仅`docs/art/reports/ART-BOILER-06-REVIEW.md`，同轮规格/质量合审，不重跑测试/构建。必改项仅相应窄修及必要验证，真实透窗/GPU、普通两蒸汽的管/罐/桶/重载动画由用户复看。

### 本批内部交付完成

原消费者五类/两测试已接通实物接口；唯一最终test+assemble为19秒exit0、11/11零失败/错误/跳过。原独立审查者同轮规格/质量通过、必改项无；核7源/21专属class/40资源/Controller只读依赖和最终JAR一致，没有重跑。负责人已完整读消费/素材/审查报告、原始日志/XML及实际浏览生产CPU透明度样本；一次实核当前7源、共享源、manifest与live/frozen JAR身份。预览尾部进度文字已由原素材作者更正，pipeline没有对应生成模板，资源与运行代码未改，不重复验证。

冻结JAR SHA256为`b5764e6f2630542975223c4b2a2bff16de0b3f663d1868a95903ce4fbad63ee5`；manifest为`be680503a9f8e72c5240f0fa1ef19e920adbb77ebee5bf11412cc67c394a4bae`，独立报告为`1dcbe0e6d2d20067c9c54404b2fadf7c41986fc2b5927351ca484887cf512e9b`。本批保留未提交美术候选，详见[交付与客户端复看](../ART-BOILER-06-CANDIDATE.md)；GPU透窗/实际气室光/管罐及暂停重载仍待用户，不关闭05B或其他人工门，不自动推进下一台或Git集成。
