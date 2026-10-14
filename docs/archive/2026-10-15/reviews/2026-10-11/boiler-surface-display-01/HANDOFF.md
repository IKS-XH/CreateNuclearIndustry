# ART-BOILER-05-L1 实物接口交付

2026-10-11，主逻辑PM。**只读前置的自动检查、真实控制器同步探针及独立窄审通过；净源码已合main并同步美术树，05B可以接续已批准的独立CT消费者。** 本交付不关闭锅炉美术视觉门，不改变锅炉玩法，不推进其他工程主线。

## 基线与实物

- 实施基线`951935fe7043c879ebad2225fa7e438da41ce7d3`，净源码提交`faea668b053d2ac4bd4607281ae099a8fa648eef`，位于`E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`。
- main净整合`eb3ec43acf56f07e3b6dcedbf152d14e6a66c68a`；美术树`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`净同步`53d699b419a850da5ded8193be65868c338cedd7`。仅12个实施路径及两份报告；三树控制器原基线一致，新增目标无交叠，14件复制逐字一致。美术dirty素材、窗口模型、负责人文档及日志保持。
- [实施报告](IMPLEMENTATION.md)、[独立审查](REVIEW.md)、[显示合同](../../../art/ART-BOILER-05-L1-INTERFACE.md)、[实施卡](../../../plans/2026-10-11-boiler-surface-display-01.md)。实际Java21编译ABI和原始证据位于逻辑树`build/reports/art/ART-BOILER-05-L1/`，不是推测签名。
- 唯一增量JAR为`build/libs/create_nuclear_industry-0.1.0.jar`，2550044字节，SHA256 `f95f60040b6e82aff3a907cd3f7f447c2e1f4f5505ba203b7590d7d671632e93`。main制品已更新为同一文件，原`35c4befd197051338c7840c8f4e80d66e4462d1090de2f7ab1a27b6853b17da7`完整备份于main `build/reports/art/ART-BOILER-05-L1/pre-integration/`。未把逻辑JAR复制到美术树冒充带新素材的最终候选；05B只在最终消费者验证时持有一次增量assemble。

## 实际消费API

包`com.iksxh.create_nuclear_industry.boiler.client`；实际`compiled-abi.txt`与源码一致。

| 入口 | 消费边界 |
| :--- | :--- |
| `BoilerSurfaceSnapshots.capture(BlockAndTintGetter context)` → `BoilerSurfaceSnapshot` | 模型必用，当前真实ClientLevel或精确原生RenderChunkRegion的同一世界对象；Ponder、未知包装、旧世界为空 |
| `BoilerSurfaceSnapshots.capture()` → `BoilerSurfaceSnapshot` | 仅诊断，不作为模型世界会话证据 |
| `BoilerSurfaceSnapshot.empty()` → `BoilerSurfaceSnapshot` | 无可靠数据的单块降级 |
| `snapshot.findSurface(BlockPos pos, Direction face)` → `Optional<BoilerSurfaceSnapshot.Member>` | 仅可靠、全加载、无歧义的真实朝外面；内部/未知为空 |
| 公共BE `surfaceDescriptor()` → `Optional<BoilerSurfaceDescriptor>` | 已冻结描述，仅供诊断/同步核对，不触发成型扫描；模型不访问BE |

Member实际访问器为`String dimension()`、`BlockPos ownerPos()`、`UUID ownerGeneration()`、`long revision()`、`BlockPos min()`、`BlockPos maxInclusive()`、`int partitionY()`、`BlockPos pos()`、`String expectedBlockId()`、`Set<Direction> outwardFaces()`。bounds闭区间，各轴长度独立，partitionY为绝对世界Y；位置/集合不可变，快照无Level、BE、NBT或库存引用。尺寸不要写死5，不从控制器facing或Fill猜边界。

服务端纯由已验证缓存Form构造完整外表面，包含壳、窗、控制器、四类口、底部换热器和顶阀9种正式ID。内部再加热段不属于外表面。棱角可有多个朝外面，底边热液口同时保留底面和水平面。停机、待水/热、冷液满等运行状态不改变成型几何。

一次`gatherModelData`捕获一次带context快照，所有邻接/面mask在同一作用域复用，嵌套/异常用finally恢复；Create的`getModelData`为final，沿既有protected gather入口。双方dimension、owner、generation、revision、min/max、partition相同，当前局部两格ID各自匹配expectedBlockId，且同一朝外方向和实际外平面才连接。只相邻同块不够。无可靠快照保留原quad。共享窗面过滤只针对同炉同外平面窗口的切向接口，不能删整个窗口外表面。

保留已有`META-INF/accesstransformer.cfg`的只读final `RenderChunkRegion.level`；不改构建、反射或light-engine身份判断。包schema1、generation/revision水位、世界/区块/owner生命周期和发布后有界刷新由前置维护；美术不增加扫描、同步、租约或重复生命周期状态机。

## 05B准许接续写集

美术负责人依据用户专项任命维护其05B卡并派发原消费者；本条明确共享边界，不授予执行者负责人或Git权限。

- 只新增三个独立生产类：`boiler/client/BoilerConnectedTextureBehaviour.java`、`BoilerConnectedTextures.java`、`BoilerConnectedTextureClientEvents.java`。
- 只新增两个同包测试：`src/test/java/com/iksxh/create_nuclear_industry/boiler/client/BoilerConnectedTextureBehaviourTest.java`、`BoilerConnectedTexturesTest.java`；短报告和证据沿其05B卡。共享12路径、反应堆、BoilerClientEvents、注册/Block、配置、碰撞、账本、构建和其他素材只读。
- 复用已批准并安装的21PNG及窗口JSON，不重新生成/安装。八个锅炉Block ID可登记独立CT包装：casing、window、controller、water_port、steam_port、hot_coolant_port、cold_coolant_port及`boiler_safety_valve`；均使用正式完整ID。独立`nuclear_heat_exchanger`仅作同炉邻接证据，不包装或改其素材；内部`boiler_heat_exchange_section`亦不包装。
- 九个shift沿已批准mapping：side/top/partition_side、window_frame、水/汽/控制/热/冷口图集。窗口原生OMNIDIRECTIONAL，其他原生RECTANGLE；只在可靠成员实际Y==partitionY的水平壳面使用隔层强调。未映射sprite保持原quad，接口标志、安全阀几何/标牌及facing保持。
- 独立Dist.CLIENT/MOD的RegisterGeometryLoaders内同步幂等登记，不enqueue、不提前取DeferredBlock、不改既有订阅；模型键用正式block ID。getQuads只读本次ModelData，指定side和side=null分别处理，复制过滤super列表。
- 只做消费侧必要行为断言、一次定向JUnit与增量assemble、一轮整批独立窄审；不重复前置29项/真实探针、已读未改素材矩阵、全量或旧存档验证。最终源/class/资源/JAR绑定以已审共享源码为准，游戏视觉仍由用户在现有存档复看。

## PM核实与保留

原始四份XML共29/29，零失败/错误/跳过；唯一增量assemble25秒exit0。专属namespace真实探针1/1，24秒exit0，正常保存关闭；其中客户端模型/CT未被运行，客户端事件依据锁定原生次序与生命周期行为测试。最大32³实测5768成员、6144面、错位9区块、5978 bytes，低于16KiB。

PM与独立审查均只读核对12源、16生产class、3测试class、模板source/install/JAR及完整JAR一致，独立窄审无必改项；未重跑测试或构建。冻结manifest中的HEAD是实施基线，随后三个净提交为集成身份，不改写原证据。主目录和美术无关改动保留，逻辑树27文件存档及原完整备份保留，不反向同步或再运行用户世界。通过共享文档交接，没有未经用户授权向另一聊天回信。
