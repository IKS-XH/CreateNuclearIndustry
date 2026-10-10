# ART-REACTOR-02：成型反应堆连接纹理设计

日期：2026-10-09。用户已确认方案并授权向主PM协调接口；美术负责人范围。本文件只管理本批美术与跨逻辑接口需求，不改变全局玩法或活动主线。

当前资源02A、逻辑前置L1及客户端消费02B均已交付并通过内部审查；[客户端候选与合并视觉门](ART-REACTOR-02-CANDIDATE.md)待用户实际观察。下文保留设计时的初始基线和接口协调历史，最终消费基线为`8b83a1a`，不把历史缺口当作当前阻塞。

## 用户需求与实际前置

用户要求在反应堆成型后制作连接纹理，使结构外观具有更强整体感，同时考虑可变尺寸。沿用上一批要求：尽量贴近原版Create，提高辨识度和可视度；低分辨率素材手绘整数像素SVG，再确定性导出PNG，不使用生图工具。

当前美术工作树为`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，HEAD为`6bcdbdd`；ART-REACTOR-01七项未提交候选及报告完整保留，不重置、不覆盖。该批内部检查已通过，用户正式客户端视觉门仍保持其原状态。

已只读核对主工程、美术工作树及`Create_NuclearIndustry-ore-acquisition`：`ReactorStructureDefinition.SIZE`仍为5，`ReactorStructureScanner`扫描固定5×5×5候选；[路线图](../implementation-roadmap.md#3-有效后续范围)将反应堆可变尺寸列为待设计实施。用户随后确认本方案，按未来尺寸预留推进，不将本批作为可变反应堆玩法已经实现的证明。

## 视觉方案

采用可重复的面板与按结构外沿选择的框边。每个图块保持16×16像素密度，不把一张大图拉伸到整个反应堆，不为每种长宽高单独绘制整面图。

- 四侧：沿用暖灰钢、铅灰压板及浅暖灰混凝土。中间区域减少每格重复粗框与角螺栓，只保留克制的材料接缝；整面外沿形成连续钢框，转角有清楚且连续的加固关系。
- 顶底：同一材料体系，按实际面边界拼接封闭钢板与加固边；不绘制必须落在奇数尺寸中心的图标、假轴口或仪表。
- 冷热口、窗口、仪表口及顶部功能件：连接周围壳体的背景与框边，保留各自局部法兰、窗口、仪表、换料口与驱动器身份。红色横标/蓝色竖标继续用于冷热辨识。功能孔与透明区域不被面板覆盖。
- 拆坏、未成型或缺少可靠客户端结构数据时：显示独立方块外观。不得让零散相邻外壳看起来已经成型。

尺寸适配按面局部坐标与实际边界判定角、边和中间；可用于长方体，宽/高/深互相独立，不能写死5，也不能只支持正方形面。预览用当前5×5×5及若干不同长宽高的假设几何展示铺设行为；假设几何仅用于美术尺寸适配，不宣称它们是已批准的游戏合法尺寸。

简单相邻同种方块连接会缺少成型与归属证据；按尺寸制作整面大图则需要重复资产并改变像素密度。推荐沿用已依赖Create的原生连接纹理机制，采用下述已经锁定版本核查的图集约定。

## 已核查的Create原生接入

只读执行者`reactor_ct_audit`实际使用现有JDK21的`javap -c -p`检查Create6.0.10-280 slim JAR；未使用其他版本教程或修改依赖。审查读取并应用`minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`技能，入口均为`C:/Users/IKSXH/.codex/skills/<技能名>/SKILL.md`。

本批外壳是完整长方体表面，端口/窗口保留自己的孔与法兰，并作为同一外表面的合法成员参加背景连接。因此优先使用`AllCTTypes.RECTANGLE`：4×4图集、每格16×16、整图64×64，只判断贴图面的上下左右。可支持奇偶边长、不同长宽高和任意合法面内端口位置，无需中心大图。若后续允许真实缺角、凹面或把孔洞排除出连接背景，需另核对对角规则后升级，不能擅自套通用47格CTM排列。

本版本`AllCTTypes$8.getTextureIndex`的列为：0左右均不连、1仅右连、2左右均连、3仅左连；行为：0仅下连、1上下均连、2仅上连、3上下均不连。索引=`列+4×行`，孤立格为12。`CTSpriteShiftEntry`按`index % sheetSize`与`index / sheetSize`定位图格。

候选客户端入口使用`CTSpriteShifter.getCT(CTType, ResourceLocation original, ResourceLocation target)`建立映射，包装`new CTModel(BakedModel, ConnectedTextureBehaviour)`。现有DeferredRegister无需迁移；Create已有`CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(ResourceLocation, NonNullFunction)`可注册包装，正式客户端入口写集仍须主PM协调。

自定义`ConnectedTextureBehaviour.getDataType(BlockAndTintGetter, BlockPos, BlockState, Direction)`在无效/未知结构返回null；本版本`CTModel`此时保留原quad和单块贴图。覆写六参数`connectsTo`，要求两格同一有效结构、同一外表面、同一归属、位于实际bounds且材料允许；不采用`SimpleCTBehaviour`默认相同Block相连判断。`getShift(BlockState, Direction, TextureAtlasSprite)`只映射实际原模型sprite，避免错误替换功能孔或其他资源。

模型重建可能在工作线程使用`BlockAndTintGetter`，不能假设持有完整ClientLevel，不能在该路径扫描世界或查找远处仪表。主PM提供可供此路径安全读取的结构快照/成员查询接口；传输和缓存的具体代码实现由逻辑侧设计。

## 需要逻辑侧协调的最小合同

仅连接同一有效结构、同一外表面的成员；接壤的独立结构仍保留各自边框。美术渲染不自行扫描世界、证明成型或计算冷却容量。

| 只读展示输入/事件 | 必须确定的含义 |
| :--- | :--- |
| 成型有效性 | 服务端扫描结果为权威；失效后及时撤下连接外观 |
| 结构身份 | 可区分两台相邻反应堆的稳定归属，不以方块类型或距离猜测 |
| 世界边界 | 实际最小角与三个独立外尺寸，单位为方块；为未来可变尺寸预留 |
| 成员/面归属 | 外壳、窗口、端口与顶部功能件合法参与范围，排除内部棒体和结构外同名方块 |
| 客户端刷新 | 成型、失效、边界/成员改变及区块加载/卸载时，受影响面应重新生成；避免每帧遍历结构 |
| 缺数据 | 使用独立贴图，直到获得可靠数据；不能保留错误的大面连接 |

现有壳体注册为普通Block，没有独立BlockEntity；仪表摘要同步valid和宽高深，但当前宽高深均来自固定SIZE，且没有原点，也没有公开客户端摘要读取入口。端口绑定由服务端结构扫描建立，不写入NBT，不能替代壳体的客户端归属。完整核查确认当前同步不足以完成“只在成型后连接”，新增同步或注册改动不在美术侧自行实现。

关键代码证据（美术工作树）：`content/P1Blocks.java:31`、`structure/ReactorStructureDefinition.java:27`、`structure/ReactorInstrumentStructureSummary.java:17`与`:108`、`blockentity/ReactorInstrumentPortBlockEntity.java:174`与`:1388`、`blockentity/ReactorPortBlockEntity.java:170`与`:489`，均在`src/main/java/com/iksxh/create_nuclear_industry/`下。仪表结构变化会发送自身位置更新，但没有全壳体模型重建证据。`ReactorStructureLifecycle.java:177`在服务端tick末重扫，因此失效显示门定义为收到可靠失效信息后刷新，不能承诺零网络延迟。

逻辑协调候选：提供世界身份、owner或结构ID、valid、真实origin/bounds、revision，以及合法表面成员查询的客户端只读描述；单次模型重建使用稳定快照。发布新快照后刷新旧/新bounds并集覆盖的渲染区段及必要邻接面；owner拆除、区块卸载、换世界时清理。未知、未加载或过期数据降级单块，不保留最后一次有效外观。具体字段形式、传输与缓存不得由本美术设计替代逻辑侧评审。

按[治理1.2节](../project-governance.md#12-美术负责人的专项授权2026-10-09)，新增逻辑状态/同步字段和共享客户端注册入口先由主PM协调写集。美术侧只制定资产及渲染消费要求，不扩展反应堆尺寸玩法，不修改扫描、热工、容量、配方或全局任务状态。向其他既有聊天发送协调消息须取得用户明确授权。

## 交付与验证边界

用户已确认本方案，拆为资源准备与真实接入两段；当前不派发缺少前置的同步/接入实现。资产段按[ART-REACTOR-02A](tasks/ART-REACTOR-02A.md)成卡，新增目录保存SVG、图集、映射、导出器及多尺寸预览，避免覆写ART-REACTOR-01与共用历史基线。

拟定资产写集：`tools/art-assets/reactor-ct/sources/`、`palette.json`、`mapping.json`、`export.py`、`README.md`与`generated/`；报告`docs/art/reports/ART-REACTOR-02A.md`；证据`build/reports/art/ART-REACTOR-02/`。14个原sprite分别为：`reactor_casing_side/top/bottom`、`reactor_hot_port_side/top`、`reactor_cold_port_side/top`、`reactor_window`、`reactor_instrument_port_side/top`、`reactor_refueling_port_side/top`、`control_rod_drive_side/top`。每个图集由16份16×16整数rect SVG导出图块再按上述映射拼合，不修改共用渲染器尺寸合同；窗口透明区单独检查，其他实体面保持不透明。所有图块先实际看图，不复制Create素材。未来接入game目录的目标名与Java写集交主PM冻结后另卡列出，资产准备阶段不覆写现有游戏图或共享源稿。

素材检查覆盖整数像素、有限色板、导出一致性、角边与重复铺设、长方体和相邻独立结构预览。尺寸预览至少包含5×5×5、6×5×8、9×7×5假设几何以及两台接壤但身份不同的结构，标注仅为几何适配样例，不用这些数字定义合法玩法范围。

实际接入后仅执行相关编译/打包和定向成型/拆坏/区块刷新验证；正式游戏内人工门看各面拐角、功能孔、管道遮挡、昼夜与距离。资源准备、接口可用、游戏接入及用户视觉验收分别记录，不以离线多尺寸图证明可变玩法或成型接入完成。

## 协调记录

用户于2026-10-09回答“好”，确认方案并允许把接口需求发给主PM。已向“梳理项目进度与后续任务”（`01a0ec74-ee73-7ff2-bb8b-f4956bc5f358`）发送一次协调消息；仅请求冻结最小结构显示合同与接入写集，不改变主PM的人工门或Git职责。图集资源段可独立推进；游戏接入仍等逻辑前置。

主PM初次完成并同步[成型接入合同](ART-REACTOR-02-INTERFACE.md)时，逻辑前置尚未实施；美术消费与客户端文件写集分开。game目标sprite固定为`create_nuclear_industry:block/reactor_ct/<原sprite名>`，工具图集保留`generated/<原sprite名>_ct.png`，已同步02A卡片/执行者。此调整仅对齐命名，不改变图集像素或现有原game资源。

在第二次授权范围内协调后，主PM确认该显示接口属于已批准美术任务的必要前置，独立于换热器教学，可按现有并行授权在同级独立工作树安排实现；换热器播放人工门和反应堆尺寸/运行规则保持。美术侧只等实际公共API与生命周期证据交付，不把这项派发确认视为接口已经可用。

## 客户端消费只读预检

`reactor_ct_audit`复核锁定Create/Catnip/NeoForge API：`BakedModelWrapperWithData.getModelData`为final，可在CTModel子类的`protected gatherModelData`进入super前一次捕获快照，覆盖全部面和邻接点；ThreadLocal作用域保存旧值，在finally恢复或remove，没有作用域则直接回退。连接索引保存在ModelData，不能把快照滞留到后续getQuads或重用工作线程。getDataType按可靠成员/当前方块/朝外面判断，getShift按真正quad sprite映射14项；跨材质连接覆写原生六参数connectsTo，面方向沿用Create上下文，不另造UV算法。

独立Dist.CLIENT事件初始化全部shift及七种block模型包装；不注册item模型，不重复注册Create自己的模型事件。进一步核对实际资源准备顺序后，撤下初步FMLClientSetup enqueueWork建议：它只保证早于post-stitch，不能保证早于准备阶段的首次ModifyBakingResult，且CustomBlockModels首次解析会缓存。02B改在ModelEvent.RegisterGeometryLoaders处理器内直接幂等初始化；该事件在ModelManager.reload同步入口发出，之后才启动模型/atlas准备，早于bake与stitch。target在block目录，原blocks atlas目录源收集14图，不修改模型/atlas JSON；资源重载仍需最终客户端确认。

预检发现接口合同的无参数capture若只读当前全局AtomicReference，无法证明旧世界排队模型任务或Ponder临时世界不会消费新世界索引。已向主PM说明具体场景，要求逻辑交付提供可验证的上下文/会话关联，必要时由主PM调整捕获/适用性API；美术仅传递模型回调的BlockAndTintGetter，不自行访问Level、制造session或补同步。公共Member访问器与最终API以编译交付为准，当前不据此启动接入。

后续L1已经完成上述缺口：带上下文capture、精确原生RenderChunkRegion所属世界及最小AT实物通过编译/26定向单元/1真实仪表服和独立审查。主PM[最终HANDOFF](../reviews/2026-10-09/reactor-surface-display-01/HANDOFF.md)已净同步到美术`8b83a1a`，全部前置通过后按[02B卡](tasks/ART-REACTOR-02B.md)派发真实接入；前文预检历史不再作为当前阻塞状态。
