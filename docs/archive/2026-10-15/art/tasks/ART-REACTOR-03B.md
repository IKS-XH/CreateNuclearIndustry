# ART-REACTOR-03B：反应堆内部运行动画消费

**状态：运行消费内部交付完成，用户视觉待观察。** 前置L2 R2实际同步核对，执行者`reactor_ct_assets`交六类/两测试/19资源和16/16定向检查＋JAR；负责人核63冻结，原`reactor_asset_review`一次组合独立窄审通过、必改项无。最终视觉门保持待用户观察。

**Goal：** 将已冻结燃料架/蓝辉/完整升降控制棒及按真实库存连续混色冷却液接入客户端，交可运行的增量候选JAR与一次整批独立审查，最终视觉留用户。

**Architecture：** 只消费不可变L2；既有仪表BE绘制一次燃料列辉光与合法液体空间，既有驱动BE绘制自己的完整棒体。静态燃料模型保持原ID；专用动态材质用两套SVG导出帧CPU插值，无新BE/着色器/依赖或世界状态。

**Tech Stack：** MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，本地实际API与既有CachedBuffers/PartialModel模式。

## 依据、前置与权限

- 用户直接要求实施三个部分，并确认“按实际库存混合成连续渐变”“完整棒体升降，上方露出”。[03设计](../ART-REACTOR-03-DESIGN.md)及[03A](../reports/ART-REACTOR-03A.md)/[03A1](../reports/ART-REACTOR-03A1.md)源候选链保持。
- 主PM的`docs/art/ART-REACTOR-03-INTERFACE.md`冻结客户端六类及专属测试/资源范围，必须在编译HANDOFF交付后生效。公共字段只用实际交付签名，不从原`snapshot()`、目标滑条或自行扫描猜数据。
- 唯一workdir`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，每条shell显式设置。实际读本树AGENTS/治理1.2、5.1、5.2、美术入口与本卡，应用已读minecraft-modding/testing/resource-pack及TDD/实施/验证技能；中文手写注释说明单位、权威/客户端边界和资源生命周期。
- 无Git、主工程写、构建/配置/共享代码写、客户端/服务启动、存档或子Agent。保留所有旧未提交美术、02R1和其他设备工作。
- L2信封/客户端索引、仪表/驱动BE、P1Blocks、L1/CT/AT及已有测试/全局流体贴图只读；需要额外公共入口先报告负责人协调，不擅自扩权。

### 已核实的实际L2绑定

- [正式HANDOFF](../../reviews/2026-10-10/reactor-runtime-display-02/HANDOFF.md)对应实现`ff3bacbd14307700e77eaf343bcb1bb9f6fc0489`；美术源码前置`4cb0d7f998989d3f320569a5ff683eee09c49e82`，交付文档同步后的实际HEAD为`ad805ec2dff2cc822b150f2b079ce11e14258f5e`。负责人和执行者未执行Git写。
- 实际读取显示树`build/reports/art/ART-REACTOR-03-L2/R1/final-freeze-r1.json`、`source-sha256-r1.json`和`javap-api-r1.txt`；12/12本树源/测试/模板SHA匹配，API文本SHA256为`177de2fc074b2fe9ffde92c8af1134466f9fc3d7ff3a351b2f1acb93e68a013a`。HANDOFF/接口文档SHA分别为`a78ed4ccc810e092006dbb4341d278fef6b2813b3053e8de507deb56f0d1e148`、`209823f84f2563fb3595f5c8bd7b4b0ee69ac8c79219cfc8463d3ba8952a24af`。
- 只用实际入口`ReactorRuntimeSnapshots.capture(BlockAndTintGetter)`，快照的`findOwner`、`findControlRod`、`findControlOwner`。列类型为`ReactorRuntimeDescriptor.FuelColumn/ControlRodColumn/EmptyColumn`，访问器`capPos/bodyPositions/expectedCapBlockId/expectedBodyBlockId`；运行值为`fuelUsable/fissionHeatHuPerTick`、`actualDepth/targetDepth/jammed`。
- descriptor已编译`coolantSpace()`、`coldCoolantMb/hotCoolantMb/coolantCapacityMb/convertedCoolantMbPerTick`及`dimension/ownerPos/ownerGeneration/geometryRevision`、`origin/maxInclusive`。20tick租约、5成功tick心跳、当前错dimension即时撤销及仅燃料noOcclusion由已审L2提供，不再实现或修改。
- 复用L2初版24/24及真实仪表1/1正常退出、R1新增行为red与6/6生命周期green、增量assemble和唯一P2窄复审；本消费不重跑该前置。原02R1 JAR仍是旧美术候选，不能作为03运行消费者制品。

消费实施中负责人从锁定NeoForm源进一步发现单人真暂停边界：`Minecraft.tick()`无条件触发ClientTick Pre，而L2接收端`STATE.tick()`目前每次推进；暂停服务端不发新sample时会在20客户端tick撤下显示。已向主PM协调共享侧窄修，消费继续实现自身时间/partial冻结，不旁路L2租约或改共享源码。最终构建先核实主PM实际补充与同步；只复核受改生命周期证据，ABI及旧未改前置复用，不重复旧组合/真实服。

R2补充现已实际核实：负责人读取显示树REVIEW的R2独立窄审及最终12路径清单、API比较，原10/10生命周期/增量assemble退出0；已同步美术源码HEAD`2bece5b09bf90951d158f45f068a81b38bf774e8`，本树12/12匹配R2清单。仅Events与LifecycleTest相对R1变化，SHA分别`5be1544adf5ee8ac31c3a43554c7ceefd33b78a07a7df13ed5b825b5b85fc43b`、`7eb5c181dff7dccc476b24aee95b5b8073a08b9fbb69a0ff26a350f4a1aec953`；公共API文本SHA仍`177de2fc074b2fe9ffde92c8af1134466f9fc3d7ff3a351b2f1acb93e68a013a`。实际isPaused只冻结租约年龄，失效核查继续，私有适配不扩公共入口。共享暂停等待项解除，允许执行唯一最终16项消费test＋jar；文档净同步后的HEAD另在最终报告记录，不把主PMGit操作算作美术写集。

## 精确写集

新建`src/main/java/com/iksxh/create_nuclear_industry/structure/client/`下仅：

| 文件 | 职责 |
| :--- | :--- |
| `ReactorAnimationClientEvents.java` | 物理客户端MOD在`ModelEvent.RegisterGeometryLoaders`同步初始化partial，`RegisterRenderers`注册两种既有BE的BER，并注册资源重载；独立嵌套游戏事件订阅处理显示缓存清理，不改L1事件类 |
| `ReactorAnimationModels.java` | 三个PartialModel，复用02B已核实的同步早期事件，在首次模型/atlas准备前初始化；不得等FMLClientSetup排队 |
| `ReactorInternalRenderer.java` | 仪表owner唯一绘制真实列辉光及液体并集外面，按实际bounds扩展AABB |
| `ReactorControlRodRenderer.java` | 驱动绘制实际深度的完整棒体，按真实行程覆盖完全拔出/插入AABB |
| `ReactorAnimationVisualState.java` | 仅展示的比例尺、实际深度短插值、合法单元体积分配/边界网格与自有数值缓存；可用嵌套类型，不能加卡外文件 |
| `ReactorAnimationMaterials.java` | SVG导出帧读取、ABGR像素连续插值、稳定有界纹理槽与资源释放/重载；数值/槽管理核心可纯测 |

新建专属测试仅`src/test/java/com/iksxh/create_nuclear_industry/structure/client/ReactorAnimationVisualStateTest.java`、`ReactorAnimationMaterialsTest.java`。

正式资源仅下列19路径，根均`src/main/resources/assets/create_nuclear_industry/`：

1. 修改`models/block/reactor_fuel_rod.json`，逐字节安装03A1静态包装。
2. 新建`models/block/reactor_animation/fuel_rod_glow.json`、`control_rod_shaft.json`、`control_rod_head.json`。
3. 新建`models/block/reactor_animation/mesh/reactor_fuel_rod.obj`、`reactor_fuel_rod.mtl`、`fuel_rod_glow.obj`、`fuel_rod_glow.mtl`、`control_rod_shaft.obj`、`control_rod_shaft.mtl`、`control_rod_head.obj`、`control_rod_head.mtl`。
4. 新建`textures/block/reactor_animation/fuel_rod_steel.png`、`fuel_rod_glow.png`、`control_rod.png`、`coolant_cold.png`、`coolant_hot.png`。
5. 新建`textures/block/reactor_animation/coolant_cold.png.mcmeta`、`coolant_hot.png.mcmeta`，精确来自冻结mapping的动画元数据。

16个独立PNG帧图块只留工具侧作为来源验证，游戏只安装两份16×128 sheet供CPU读取；不复制无需消费的图块。其余工具/03A/03A1源码及冻结链只读；无直接PNG画图或生图。报告唯一`docs/art/reports/ART-REACTOR-03B.md`，证据唯一`build/reports/art/ART-REACTOR-03B/`。

## 数据捕获与回退

每个BER的每次绘制仅`capture`一次，所有身份/几何/运行值来自该快照。instrument按owner查询，drive必须从同一快照取得控制棒owner身份和该列实际深度；首帧或ownerGeneration/geometryRevision/客户端会话改变直接定位，不能跨身份沿用插值。稳定材质/插值缓存键不包含每心跳变化的sample/runtime revision，避免重复分配。

按冻结列记录的预期cap/body ID核对本帧已加载本地方块；不强制取区块、不以自己的扫描补缺失投影。任一相关成员矛盾时撤下所属动态画面，不在别的堆或失效状态继续播放。静态燃料几何由正常block模型保留，缺接口熄辉，无可靠控制归属不画浮棒，无合法库存/容量不画液体。

缓存只保留自有数值、不可变几何与身份键，不持有BE/NBT或外部可变集合；世界生命周期由客户端事件清理，不强持旧Level。L2租约/心跳/旧包校验由已审接口负责，消费不实现第二套租约或重新结算。

## 三项可见合同

**燃料架与蓝辉。** 正式静态JSON/OBJ/材质逐字节安装03A/03A1，保留item父ID及完整原版展示parent。三个同列body格共享权威该列HU/t，只绘制可靠可用燃料列；耗尽tick可用=false但HU/t>0仍按合同熄辉，不改数据。可靠正产热`H`的管身alpha为`0.12+0.53×H/(H+6)`，零值/无燃料/未知立即0；6 HU/t只为视觉比例尺。原生局部自亮、无diffuse暗化，钢板不发光；0.0005每管偏置已经烘焙，消费者不得全局XZ放大导致轴心漂移。无世界照明/bloom规则。

**完整控制棒。** 杆长为实际body行程，shaft仅按行程缩放一次，head始终2/16固定。轴X/Z来自cap；杆底`bodyTopY-travel×actualDepth`，杆顶为杆底+travel，shaft/head整体平移。0拔出、1插入，顶部露出段全画，不裁成收纳/伸缩。实际值改变可最多4客户端tick插值；目标单独改变不动棒，卡死保持权威实际值，未知/恢复/换身份不从旧位置继续缓动；超过4tick未绘制后重新出现时直接定位当前实际值。包围盒包括最低棒底、完全拔出杆顶及固定端部。

**连续混合冷却液。** 只取EMPTY及CONTROL_ROD body单元，燃料列不填液。`total=cold+hot`以防溢出方式计算，容量>0且total>0时`fill=clamp(total/capacity)`、`hotRatio=hot/total`；无固定上下冷热层。按Y层真实单元面积自下向上分配等效体积，不能写死尺寸/三格或填整个outer bounds。液体为合法单元并集外表面，剔除相邻液格共享面；分离空间、部分液位和侧向UV接缝正确。侧面顶点alpha约.22至.32的连续轻渐变，顶面适度通透，避免遮住杆体；不给每空气格叠冷/热两盒。

两套8帧sheet在资源重载prepare读取成不可变像素，关闭InputStream/NativeImage；CPU先混各套相邻帧再按真实hotRatio混色。NativeImage为ABGR，顶点使用显式RGBA，考虑entityTranslucent的alpha<.1丢弃门槛。每堆复用一个16×16 DynamicTexture和稳定槽ID，材质在提交该堆顶点前更新一次，不能提交后在同帧复用槽给另一堆。纹理槽硬上限256；超过时液体安全不画且不生成无限ID，不影响静态模型/辉光/控制棒。超过20显示tick未绘制的材质/位姿缓存在安全tick边界回收，避免已离开视野的可靠owner长占槽；清理须避开尚未提交完成的材质，同帧不能抢用。材质相位以客户端显示tick+partial平滑累积，换热mB/t只调节活跃度，停止换热保留缓慢环境纹样；不声称真实局部泵速/方向。暂停冻结显示时间/相位/位姿，不按墙钟或仍可能变化的partial继续推进。

原生`entityTranslucent`单纹理/NEW_ENTITY顶点按真实Pose和法线变换、NO_OVERLAY与正确light提交；其批次排序不能保证跨窗/跨堆全局顺序，必须留客户端透窗门。owner失效/区块卸载/世界切换/资源重载释放已注册纹理用TextureManager.release，关闭旧帧/动态像素，重新加载已安装sheet；缺失/尺寸错误安全撤液，不保持旧GPU内容或每帧刷日志。

## 必要执行与证据

- [x] 生效前：负责人核对实际L2 HANDOFF/签名/同步HEAD，12/12源码SHA与R1一致。
- [x] 执行者保存精确27写集前状态、19资源安装映射及原02R1 JAR身份。不重复944矩阵或L1/L2/02R1已审测试。
- [x] 先写少量真实行为red：功率零/单调有界及耗尽；目标≠实际与卡死/换身份/4tick插值；多尺寸/不同Y面积的液体体积及无共享内面/0库存；ABGR冷/热端点和连续帧混色；槽回收/有界/会话或重载清理数值核心。12项实际AssertionFailedError保留，依赖/编译失败不冒充red，不用源码字符串测试。
- [x] 实现六类与19资源消费，复用实际1.21.1 API及既有renderer初始化顺序，自查首次加载、AABB、透明材质与资源生命周期；不增加共享setter/BE/GUI/依赖。
- [x] 一个持有者运行定向`./gradlew.bat test --tests '*ReactorAnimation*Test' jar`，最终真实XML16（9＋7）项、0failure/error/skip与exit0。无本批新服务端逻辑，不重跑GameTest/旧玩法/全量构建。
- [x] 核对17模型/PNG安装字节（含03A1包装）及两mcmeta语义和JAR条目；原02R1资源中仅静态燃料包装为允许变化，其他1037既有assets保持。源/安装/JAR映射、最终SHA与对应XML/命令/退出已记录。
- [x] 报告并冻结本批源/资源/证据；负责人实际读实现/测试/原日志/XML并核对63/63冻结哈希一致。
- [x] 一次整批独立规格＋质量窄审`reactor_asset_review`已通过，实际读取已有证据和五张预览，不重复生成或构建，必改项无；[报告](../reports/ART-REACTOR-03-REVIEW.md)。
- [x] [最终候选与集中复看说明](../ART-REACTOR-03-CANDIDATE.md)已完成并交用户。
- [ ] 用户人工门：运行/停热蓝辉、实际深度/卡死与完整拔出、真实液位/冷热连续混色、昼夜/透窗/资源重载/拆坏恢复/真暂停。待实际观察，不自动启动客户端、不改复制来的存档，02R1视觉门继续独立。

自审：三项目标分别由静态资源+辉光、实际位姿、真实库存混色覆盖；写集与主PM合同六类一致，数值/网格/像素纯验证集中两专属测试，唯一仪表绘制避免重复体积；实际API和共享写集现已核实交付，本卡按上述冻结绑定执行。
