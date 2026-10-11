# ART-BOILER-06：只读技术核实

2026-10-11。仅按06卡核实现有源码和锁定本地源，不改生产/测试/素材、未构建或运行游戏、无Git写。实际读取AGENTS、治理1.2/5.1、art README/PLAN/06卡及minecraft-modding/testing/resource-pack技能；MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6不变。

## 结论与最小共享缺口

现有同步足以取得两种真实库存和可靠几何，不必增加网络协议。Controller `ledger()`（BoilerControllerBlockEntity.java:67）返回客户端已加载的账本；write/read（:362–385）在客户端包中完整保存/加载BoilerState。BoilerState.java:59–60、300–327明确NormalSteam/SupercriticalSteam各自为非负mB，steam()仅合计；NormalSteamHu/SupercriticalSteamHu是HU。不能把SteamKind（最近生产类型）、出口所选类型或Window.Fill当库存。

**容量存在公开消费缺口**：服务器实际SteamCapacity已同步（Controller:368），客户端存入私有steamCapacityView（:48、383）；没有getter。BoilerState.steamCapacity()（:81）使用settings容量系数，而save/load不保存Settings（:300–343），Controller只在服务端路径setSettings（:159–163、293–296）。所以远端配置变更时客户端ledger.steamCapacity()可能仍用DEFAULT 2000mB/格，不可作为真实浓度分母。建议主PM仅提供只读客户端容量访问器或小只读运行值record（normalMb、supercriticalMb、steamCapacityMb）；不用新增包、同步或租约。本轮不自定义其ABI。若设计只用mB绝对浓度、不按容量归一化，可无需共享改动，但那是待确认视觉取舍，不替代实际容量。

## 汽区和可靠归属

BoilerSurfaceSnapshot.java:20–40提供findSurface(pos,face)及Member.dimension/ownerPos/ownerGeneration/revision/min/maxInclusive/绝对partitionY/expectedBlockId/outwardFaces；Snapshots.java:30–38的capture(context)绑定真实ClientLevel对象，未知/Ponder/旧世界为空。Controller.surfaceDescriptor()（:69–70）可核本次BE与Member同generation/revision/边界且valid，Controller.current()（:96–100）可核当前BE槽位而不扫描结构。每次绘制捕获一次L1、一次读取两库存和实际容量，后续网格全部复用；未知/局部控制器ID冲突/撤销不绘制。不得调用currentForm()或客户端BoilerStructure.scan。

真实汽区从服务器Form得到：Structure.java:130隔层位于内域，:149–157把partitionY层排除并要求其余内部air；py以上计steamCells。方块cell范围X=minX+1..maxX-1、Z同，Y=partitionY+1..maxY-1，对应几何连续范围[minX+1,maxX]×[partitionY+1,maxY]×[minZ+1,maxZ]；六面可向汽区内缩小epsilon避共面。始终填满该汽区，库存仅控制浓度/两种配比；总量0无任何雾，不做液位。总量>0即使停止加热仍有真实蒸汽，不应以running/Produced>0强制隐藏。实际转换/出流方向没有公开实时合同，纹样循环只能表示材质运动，不能自称真实流向/流量。服务器每5tick或产生/排放时sendData（Controller:200），可显示客户端最新库存，不能保证每tick到达；现有接口没有库存独立时戳/过期租约。

## 既有窗口与渲染接入

BoilerClientEvents.java:21–34只注册WINDOW_BE水位BER，.07..93局部水盒取Window.fill；Window BE:14–25只同步Fill。服务器Controller:190–195明确汽区窗口Fill=0。这条水位路径应保留，汽雾由**唯一Controller BER**绘整炉，不能每窗重复整汽区，不能用fill判蒸汽。建议独立新增boiler/client/BoilerSteamRenderer.java、BoilerSteamVisualState.java、BoilerSteamClientEvents.java；若选择两纹样连续逐像素混色，再新增BoilerSteamMaterials.java及BoilerSteamRenderBuffers.java，独立复用反应堆算法经验，不改原Reactor辅助或BoilerClientEvents。两个定向测试可覆盖实际入口库存0/混合/边界/失效和批次裁剪，具体写集须负责人/PM另批准；当前不实施。

原反应堆ReactorInternalRenderer.java:25–31已有全域AABB、:64–86已有合法内部空气采光与局部顶点/法线；ReactorInternalRenderBuffers.java:25–50已有有界固定纹理层及AFTER_BLOCK_ENTITIES单次提交。建议汽雾采汽区内侧对应空气格的LevelRenderer.getLightColor，不复用外壳Controller light、不满亮；几何局部于owner作差，避免大坐标浮点精度。先采用一个有界外表面网格/少量固定雾片而非逐体积cell或逐窗完整透明体；只画一次，几何缓存键为世界session/归属代次/修订/闭边界，浓度变更仅改顶点alpha/材质配比。外表面仅表达可见雾壳，不能声称默认管线是真正体积散射；若要求深入体积读感，可在同一个批次增加少量裁剪到真实汽区的雾片，数量须有界并待设计确认。

## 裁剪、深度、生命周期

锁定源JAR为build/moddev/artifacts/neoforge-21.1.219-sources.jar。BlockEntityRenderer.java:14–23默认shouldRenderOffScreen=false，默认距离64按BE中心；IBlockEntityRendererExtension.getRenderBoundingBox(T)默认单格。只扩AABB不足以保证Controller所在section不可见时远端汽区仍提交：SectionCompiler.java:129–135按shouldRenderOffScreen分入globalBlockEntities，LevelRenderer.java:1081–1091仍对global使用ClientHooks可见性检查。因此推荐Controller BER shouldRenderOffScreen=true、可靠汽区AABB、shouldRender按汽区AABB到camera距离（非无限包围盒/无限距离）；L1未知恢复单格/不绘制。每炉O(表面积或固定雾片数)，全局仅加载owner，原生frustum/距离仍裁剪，不遍扫世界。

RenderType.java:153–163、846–851原生entityTranslucent使用NEW_ENTITY/QUADS、NO_CULL、光照、alpha混合及批次内排序；默认深度写入仍存在，不能凭透明alpha期待后面的几何无条件透过。LevelRenderer.java:1117在所有BER后触发AFTER_BLOCK_ENTITIES，:1177/1196随后提交区块translucent窗玻璃。因此独立固定缓冲可仿反应堆在此阶段统一提交汽雾，避免材质切换提前flush；不要在单owner BER endBatch。它只保证原生阶段顺序，不保证跨炉/跨RenderType透明体与玻璃全局精确排序；雾面必须位于玻璃内侧、避免两种完整盒叠加，FAST/FANCY/FABULOUS与斜视/窗反光仍须实际客户端观察。不能本轮承诺GPU排序通过。

L1 ClientEvents.java:37–68世界切换/Unload清空，chunk及owner生命周期撤销/恢复；没有按运行状态年龄过期，暂停不把仍可靠成型撤销。新视觉时钟需Minecraft.isPaused()时冻结tick及partial，相同Level读取当前L1，卸载/换世界清几何/材质身份；动态纹理方案还需资源reload安全释放、坏资源无雾，并在所有旧批次提交后release，不能沿用旧世界槽。无新共享运行租约必要，但不得称其为网络停滞检测。

本报告给出接入建议与容量公开缺口，不构成新写集、接口ABI或视觉验收。后续最少验证库存0/两库存配比/真实容量、汽区不侵水区、可变bounds、不同owner/撤销/换世界、全域裁剪和单次批次；L1既有同步测试复用，不重跑生命周期矩阵。


### 已批准短设计与精确接入请求

用户已批准普通乳白灰、超临界青白、全汽区半透明向上翻卷；实际总mB/服务器汽容量调浓度、超临界mB/总mB调连续混色，无液位，四普通流体sprite共用SVG雾纹循环。向上翻卷属于已批准材质动画，不推导真实流向。为实施该设计，向主PM请求最小一处共享生产改动：BoilerControllerBlockEntity新增公开只读客户端实际汽容量getter（int，mB，读取已同步steamCapacityView；未同步/非正返回值由消费者安全不绘制，是否需已收到首包flag请PM明确）。两库存继续只读ledger().steam(kind)，不用新增库存字段/包。容量字段目前初值为18000，必须联合合法surfaceDescriptor/当前L1身份门控，不能把初始默认当同步证据；surfaceDescriptor在同一个read包赋值，首次可靠描述表明同包容量已读取。

全域裁剪无需修改Controller BE/Block或新增共享包围盒字段：新独立BoilerSteamRenderer通过现有NF BER扩展覆盖getRenderBoundingBox/shouldRenderOffScreen/shouldRender即可；独立Dist.CLIENT EntityRenderersEvent.RegisterRenderers订阅登记现有BoilerContent.CONTROLLER_BE，既有WindowRenderer保持。实际新类与测试路径仍待负责人追加：Renderer、VisualState、Materials、RenderBuffers、ClientEvents及两专属测试；资源四sprite/SVG写集由素材作者另持有。不改变BoilerClientEvents、窗口Fill、L1或反应堆批次。
