# ART-REACTOR-02B：成型反应堆客户端连接纹理

日期：2026-10-09。状态：`reactor_ct_assets`（gpt-6.1-sol/high）已实现并冻结；11/11定向测试、打包及一次独立规格/质量审查通过，必改项无。**内部候选通过，客户端视觉门待用户观察**，见[候选入口](../ART-REACTOR-02-CANDIDATE.md)。用户已批准设计及接口协调，无需再次确认同一视觉方案；只读已交付逻辑接口，不得伪造结构有效性。

## 目标、依赖与角色

在同一可靠成型反应堆的真实外表面，按Create RECTANGLE连接已通过审查的02A图集；失效或归属未知保留原单块贴图，物品/Ponder无权威描述同样回退。宽高深从实际bounds读取，适配未来长方体，不实施可变尺寸玩法。

依赖[主PM接口合同](../ART-REACTOR-02-INTERFACE.md)、[批准设计](../ART-REACTOR-02-DESIGN.md)、[02A资源](../reports/ART-REACTOR-02A.md)及[独立审查](../reports/ART-REACTOR-02A-REVIEW.md)。逻辑前置须交编译后的快照公共API、模型上下文与世界会话关联、发布/撤销/区块恢复/旧revision拒绝及刷新顺序证据，且主PM把必要代码同步到美术树。美术负责人不复制或修改逻辑代码，不做Git写操作。

主PM已交[编译API与准许写集](../../reviews/2026-10-09/reactor-surface-display-01/HANDOFF.md)，美术树实际基线`8b83a1a97f1b539e6dcc7b6714b9ee96c13854e0`。若执行中发现公共API缺口，先报告，不猜测访问器或新增同步。

唯一工作区`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，每个shell显式workdir。现有ART01七项源稿/PNG、02A冻结目录及负责人/主PM文档完整保留。实现交单一执行者；执行者不是美术负责人/PM，禁止派发其他代理、任务状态/Git/全局治理写入。候选审查一次合并规格/质量，不重复整支审查或旧批次验证。

实际读取最新主工程及本树AGENTS、治理1.2/5.1、美术入口、本卡与依赖；应用`minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`，入口为`C:/Users/IKSXH/.codex/skills/<名称>/SKILL.md`；应用`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/test-driven-development/SKILL.md`及其中`writing-good-tests.md`。项目5.1优先于通用技能裸全量suite/每方法镜像测试/删除既有实现/Git提交规则，不扩大本批检查。核对MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；代码职责、单位、线程边界及非显然算法注释使用中文。

## 预留精确写集

Java路径以`src/main/java/com/iksxh/create_nuclear_industry/`为根，仅新增：

- `structure/client/ReactorConnectedTextureBehaviour.java`
- `structure/client/ReactorConnectedTextures.java`
- `structure/client/ReactorConnectedTextureClientEvents.java`

主PM已在接入合同明确允许新增专属测试`src/test/java/com/iksxh/create_nuclear_industry/structure/client/ReactorConnectedTextureTest.java`，只验证消费行为；不修改L1公共API或测试、不重复其生命周期矩阵。测试写集随同本卡在实际前置通过后派发。

仅新增14张game图：`src/main/resources/assets/create_nuclear_industry/textures/block/reactor_ct/<原名>.png`。原名完整集合以02A mapping的14项为准，来自冻结`generated/<原名>_ct.png`逐字节复制；game名无`_ct`，不缩放或再绘制，不改原sprite。

允许报告`docs/art/reports/ART-REACTOR-02B.md`及本批`build/reports/art/ART-REACTOR-02B/`定向命令/证据。其余文件只读，尤其全部逻辑快照/同步及其access transformer/构建接入、CreateNuclearIndustry、P1Blocks、现有模型/blockstate/item/atlas JSON、语言、Ponder、配方、热工、配置、02A工具/资源与共享美术管线。专用客户端事件可独立注册；如需共享入口、上述之外的正式测试或扩大范围，先向负责人报告并由主PM明确协调写集，不自行实现。

## 消费架构

1. 映射14个`CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, original, target)`，original/target与02A mapping逐项一致；保留shift entry，不跨资源重载缓存atlas sprite对象。只包装七种实际block模型，不注册item模型、不迁移DeferredRegister。MODEL_SWAPPER注册键必须是`create_nuclear_industry:<方块注册ID>`，不能加`block/`：`reactor_casing`、`reactor_window`、`reactor_instrument_port`、`reactor_cold_port`、`reactor_hot_port`、`reactor_refueling_port`、`control_rod_drive`；模型路径由Create自行展开。
2. 客户端专属MOD事件监听`ModelEvent.RegisterGeometryLoaders`，**处理器内直接**调用幂等initializeOnce，完成14shift与七MODEL_SWAPPER包装注册；不enqueue、不实际添加geometry loader、不捕获世界快照、不重复Create监听器。锁定ModelManager.reload同步调用GeometryLoaderManager.init/该事件后，才启动模型与atlas准备，确保早于首次bake/stitch。不能退回FMLClientSetup排队：只可证明它早于post-stitch，不能证明早于准备阶段的ModifyBakingResult，且CustomBlockModels首次解析结果会缓存。block目录由原blocks atlas目录源收集，不增JSON引用；资源重载再次发事件须避免重复注册，不跨重载保存sprite对象。
3. Create的`getModelData`为final。在CTModel子类覆写`protected gatherModelData`，进入super前根据已交付的上下文关联API一次捕获不可变快照；该次super计算全部面及邻接点均使用它。ThreadLocal作用域保存旧值，finally恢复或remove；无作用域直接回退，不隐式重新capture。快照不带到getQuads，不能滞留模型工作线程。
4. `getDataType`验证当前面成员、真实朝外方向、局部BlockState与描述预期ID、适用上下文及材质表；不可靠返回null，保留原quad。`getShift`按真正quad sprite匹配14项original，未知sprite返回null，不凭block身份猜贴图。
5. 覆写六参数`connectsTo`，只在两端同结构身份、同朝外面与外平面、各自bounds内、局部当前ID均匹配且材质表允许时连接。七类背景可在同一结构共面连接，功能孔/标识仍由各自sprite保留。不同owner、歧义、失效、无记录和未加载均不连接。
6. 上下左右位移、正负面翻转沿用Create原生buildContext，不手写第二套UV索引算法，不调用默认同block邻接规则。连接判断不得扫描世界、读取远处仪表/可变Map/NBT、强制加载区块或修改服务端成型。

任何模型上下文无法可靠对应会话时降级原图，不能只凭坐标和block一致证明属于当前世界。Ponder临时世界无权威描述时回退；不能为教学假造valid。真实API若未提供安全关联，停止此依赖范围并报告，禁止用全局当前快照掩盖缺口。

## 实施与验证顺序

- [x] 前置实际通过、API签名/上下文适用性冻结并同步美术树；负责人已核对HANDOFF、实际HEAD与源文件，并据此派发。
- [x] 保存3新增Java目标与14目标game路径的开工状态；只读确认七block模型/14sprite及原窗口渲染合同，不重新全量扫描旧资产。
- [x] 在专属ReactorConnectedTextureTest中先写真实失败用例，再实现可靠/空上下文快照、跨owner、局部ID变化、六面连接与ThreadLocal嵌套/异常清理；不以与实现镜像的测试或只有编译代替行为证据。新增其他测试路径先协调范围。
- [x] 完成3客户端文件与14game图，相关编译/定向测试通过；确认专用服务端入口不引用新增客户端类、一次重建仅一次捕获、失效为null原quad路线。
- [x] 最终一次`./gradlew.bat test --tests '*ReactorConnectedTextureTest' jar`，记录实际非零用例数及退出码，14图逐字节核对打包、七原模型/item JSON保持；资源重载不复用旧sprite。未发生源稿/工具变化不重复02A导出和负例。
- [x] Git只读差异/空白及精确范围检查；提交未提交候选与原始命令、退出码、报告，交一次独立规格/质量审查。
- [ ] 最终客户端人工门由用户或已协调主PM执行：成型/拆坏、相邻不同owner、六面边角/冷热口/透明窗、管道遮挡、远近/昼夜、跨区块重载及退出重进。保留各步实际观察；离线或测试不能代替用户视觉确认。

按治理5.1复用已审逻辑前置/02A证据；不重复全量设备测试，不研究旧存档兼容，不启动用户客户端。API、静态/定向自动化、客户端状态与用户视觉结果分别记录，不把本批描述为可变反应堆玩法交付。

## 内部候选交付

负责人实际读取[执行报告](../reports/ART-REACTOR-02B.md)、11项最终XML/退出0日志、验证摘要和三客户端源/专属测试；一次独立[规格/质量审查](../reports/ART-REACTOR-02B-REVIEW.md)通过，必改项无。冻结19份交付与26份证据绑定一致；14份工具图集/game/JAR逐字节相同，899既有保护输入保持。真实red为10项/7断言失败，首轮新增原生方向预期写反的失败保留，修正测试预期后11项全通过，没有改生产原生方向算法。

JAR2367612字节，SHA256 `4cd8f15fd6108596cd37831bec7501b48b2add668ed83cecca648847350a9c71`。全树空白检查退出2只自动logs两项尾空白，原样保留；手写Java定向/逐行检查通过。负责人不重复已审测试/构建，未执行Git写。候选入口合并当前客户端观察，人工复看未关闭，不宣称已合main。

## 派发前实际记录

逻辑候选`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-display`，源提交`c5f29f0`，main净集成`c5e5fc3`、美术净同步`8b83a1a`；主PMHANDOFF与本树实施/独立报告均已实际读取。L1最终26/26定向JUnit、1/1真实仪表专用服、增量assemble及独立审查通过，净同步15路径与本树既有候选无交叠。日志与compiled javap留显示树`build/reports/art/ART-REACTOR-02-L1/`，复用这些证据。

公共API：`ReactorSurfaceSnapshots.capture(BlockAndTintGetter)`返回不可变`ReactorSurfaceSnapshot`，模型必用上下文入口；无参仅诊断，禁止消费者使用。`snapshot.findSurface(BlockPos, Direction)`返回Optional<Member>；`empty()`为空回退。Member访问器为`String dimension()`、`BlockPos ownerPos()`、`UUID ownerGeneration()`、`long revision()`、`BlockPos origin()`、`BlockPos maxInclusive()`、`String expectedBlockId()`、`BlockPos pos()`、`Set<Direction> outwardFaces()`；inclusive边界单位为方块，集合/坐标不可变。真实ClientLevel或精确原生RenderChunkRegion的所属世界须与原子发布会话同对象，未知包装/Ponder/旧世界为空。

已只读核实主PM的L1卡与最终HANDOFF，逻辑自动/独立审查及净同步门均通过。窗口现行模型`models/block/reactor_window.json`已为translucent，P1Blocks为noOcclusion；本批保留该合同，不额外改变窗口层或注册。

主PM已同步编译后的最小access transformer实物，仅公开RenderChunkRegion.level并保留final，ModDevGradle自动应用且build.gradle未改；02B只调用捕获API，未知包装层/Ponder/旧世界为空，不读取或修改该逻辑字段。专属消费测试路径已获合同授权，本卡全部派发前置通过。

已用锁定JAR追加核查注册键：CustomBlockModels.loadEntries直接查询BuiltInRegistries.BLOCK，查到AIR即跳过；CreateRegistrate采用RegisteredObjectsHelper.getKeyOrThrow(Block)，ModelSwapper随后展开全部blockstate模型。错误加block/会静默不包装，定向测试须覆盖七注册键与14sprite映射。

注册时序追加核对：ModelManager.loadModels在准备屏障前发ModifyBakingResult，client setup排队没有必然先后；reload:104–105同步调用GeometryLoaderManager.init，后者42–47同步发RegisterGeometryLoaders，再进入模型/atlas准备。自动订阅先按Dist过滤再Class.forName(...,true)，故独立Dist.CLIENT入口不会从专服加载。采用该明确事件里直接初始化，避免静态初始化提前触碰Create注册表；新增客户端类不能被公共逻辑引用。
