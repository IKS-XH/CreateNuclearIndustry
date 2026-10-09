# ART-REACTOR-02：成型连接纹理接入合同

日期：2026-10-09。维护者：主逻辑PM。状态：[逻辑前置L1](../superpowers/plans/2026-10-09-reactor-surface-display-01.md)自动与独立审查门通过，main `c5e5fc3`、美术树`8b83a1a`已净同步实际API，见[交付](../reviews/2026-10-09/reactor-surface-display-01/HANDOFF.md)。02A资产及审查独立记录，02B可按预留写集接续；最终CT/视觉门仍未关闭。

## 来源、前置与边界

美术负责人对话`01a11c6b-f440-77c1-9275-5b2dca5cfa0f`转交用户已确认的成型连接纹理需求及[美术设计](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/ART-REACTOR-02-DESIGN.md)，请求协调最小逻辑接口。需求为同一有效反应堆表面连续拼接、失效降级单块，并适配未来不同长宽高；不是扩展反应堆尺寸玩法的授权。

主工程核对基线`8087eca`；美术工作树HEAD为`6bcdbdd`，ART-REACTOR-01七图及其源稿/报告仍有未提交差异。现行反应堆仍为固定5×5×5。当前换热器五幕候选仍待用户播放；此合同不改变其人工门，不派发其他工程主线，也不追认美术视觉验收。

实际使用`brainstorming`、`minecraft-modding`、`minecraft-testing`与`minecraft-resource-pack`核对职责、生命周期、验证及图集消费；Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82保持。

## 已核实的源码事实

- `ReactorStructureScanner.WorldScanResult`已经给出服务端权威`origin`与有效扫描结果；当前扫描域仍来自`ReactorStructureDefinition.SIZE`，不是任意尺寸。
- `ReactorInstrumentPortBlockEntity.updateStructureCache`保存扫描结果/原点并同步仪表更新；`write(..., clientPacket)`及`getUpdateTag`已有只读结构摘要传输。摘要没有原点、表面成员或归属，因此现有字段不足以完成成型连接。
- 仪表`onLoad`安排服务端重扫，`invalidate`撤销归属并清除结构缓存。普通外壳和窗口没有方块实体，不能借端口的服务端绑定给所有表面提供客户端归属。
- 美术侧已核对当前Create的`CTModel`、`ConnectedTextureBehaviour`、RECTANGLE图集及`MODEL_SWAPPER`。本合同沿用该接入选择；具体客户端注册时机仍须在真实接入阶段用锁定依赖验证。

## 最小逻辑前置

优先扩展仪表已有的服务端到客户端BE更新数据，新增独立的表面描述，不改现有护目镜摘要字段含义，不给所有外壳添加BE，不新建另一套客户端结构扫描器。新增描述只用于显示，不持久化热工/库存，也不允许客户端写回成型状态。

描述包含以下内容，单位均为世界方块坐标；所有位置和集合必须不可变：

| 数据 | 冻结含义 |
| :--- | :--- |
| `dimension`、客户端世界会话 | 区分维度及退出重进/换世界的缓存；相同坐标不能跨世界复用 |
| `ownerPos`、`ownerGeneration` | owner为仪表端口；generation区分同坐标被拆除后新放的仪表实例，不复活旧归属 |
| `revision` | 同一owner实例的表面描述版本；只在有效性、bounds或成员变化时递增，不跟随每tick热工遥测递增 |
| `valid` | 服务端扫描有效且能建立完整表面描述；失败/未知不是有效描述 |
| `origin`、`maxInclusive` | 实际世界最小角及包含端点的最大角；宽、高、深分别为`max-min+1`，允许数据模型表达长方体 |
| 表面成员 | 世界位置、扫描时的方块注册ID及其真正朝外的面；仅有效结构外表面，不含内部燃料棒、控制棒或旁边同名方块 |

拟实现的投影适配层应使用扫描原点和权威`SIZE`得到当前实际固定边界，仅这一层依赖当前固定尺寸。客户端查询、连接判断、图集和面坐标算法不得硬编码5。假设6×5×8、9×7×5只用于纯几何测试/离线预览，不改变合法尺寸。

服务端在有效扫描结果更新时提取外表面成员；这不是另一轮成型判定。不得强制加载区块取得成员，也不在取景、每帧或每次连接查询时遍历结构。包解码须限制字段/集合大小并验证边界、重复成员、owner与外表面关系；错误描述降级不可用，不能把不可信坐标用于无限刷新。

### 客户端只读消费

预留客户端入口`structure.client.ReactorSurfaceSnapshots.capture()`，返回不可变`ReactorSurfaceSnapshot`。模型消费新增必要的上下文入口`capture(BlockAndTintGetter)`；无参入口不能作为旧/新世界区块工作任务或Ponder临时世界的关联证据。上下文入口须用锁定API实际验证所属会话，无法证明适用时返回空，不能只比较局部同坐标/同方块。公共查询为`ReactorSurfaceSnapshot.findSurface(BlockPos, Direction)`，返回`Optional<ReactorSurfaceSnapshot.Member>`；命中项提供上述结构身份、bounds、revision和预期方块ID。缺少当前世界数据时捕获空快照，查询返回空，不用null代替快照。

模型重建的一次调用只使用一个快照；不得每个邻接点重新捕获不同版本。快照和其成员均不保存Level、BE或可变NBT，不从模型工作线程访问远处仪表、客户端可变Map或扫描世界。发布由客户端游戏线程完成，通过原子替换只读索引供工作线程读取；刷新必须在发布之后执行。世界会话与模型重建上下文的关联在逻辑实现中验证，不能把新世界索引用于旧世界尚未结束的任务。

美术消费判断同时满足：两个成员属于同一结构身份、处于同一外平面、面方向一致、在各自bounds内、当前局部BlockAndTintGetter中的方块仍匹配成员预期ID，且美术材质连接表允许。仅同种方块邻接不构成连接证据；不同owner、歧义/重叠归属、无记录、失效或未加载均不连接。

无可靠描述时`getDataType`返回null，保留原模型quad与单块贴图。正式物品模型和未成型方块保持单块外观，Ponder临时世界缺少权威描述时同样降级，不为教学伪造服务端成型。

### 更新、卸载和恢复

- 有效描述变化或收到失效描述：原子替换成员索引，再标记旧/new bounds的并集及外扩一格的相关渲染区段重建。
- owner移除、BE卸载、成员区块卸载：及时撤销受影响的有效索引并重建仍可见的相关面；不能继续用最后一次有效快照假装成型。
- 区块重新加载：重新建立加载状态，只有可靠的当前owner描述及成员证据齐备时恢复。首次BE更新早于客户端`onLoad`的情况也必须处理，不能要求玩家拆放仪表才能恢复。
- 当前客户端部分缓存覆盖/视距调整路径缺少Unload事件，逻辑侧可在tick核对已知描述所需的有限去重ChunkPos真实加载实例；不读方块/扫描世界或强加载。BE明确区分卸载暂停与真正拆除退休，检查不到可靠区块时先撤销，不把`ClientLevel.hasChunk`当证据。
- 退出/换世界：清空该会话索引、旧generation与待刷新项；重复/过期revision不得使旧归属复活。
- 同一个描述随仪表遥测重发时可以复用不可变对象；没有几何/有效性变化不得每tick重建整个反应堆模型。

BE更新是优先传输路线。如果只依此路线无法满足成员区块重新加载或owner拆除的撤销/恢复证据，执行者须报告具体缺口，由PM调整逻辑写集；不得擅自增设C2S请求、每帧扫描或改造所有外壳注册。

## 写集分工

以下是后续真实接入的隔离边界，不表示逻辑前置已经交付或美术Java接入已派发。下列简写Java路径均以`src/main/java/com/iksxh/create_nuclear_industry/`为根。

**逻辑侧预留写集（独立执行者，另卡实施）：**

- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`：仅新增表面描述生成、BE同步及加载/失效通知；不改变运行、流体、燃料、控制、维护事务或原护目镜合同。
- 新建`structure/ReactorSurfaceDescriptor.java`、`ReactorSurfaceDescriptorFactory.java`、`ReactorSurfaceSyncEvents.java`：不可变展示合同、扫描结果投影和不加载客户端类的通知桥。
- 新建`structure/client/ReactorSurfaceSnapshot.java`、`ReactorSurfaceSnapshots.java`、`ReactorSurfaceClientEvents.java`：客户端发布/成员索引、世界/区块生命周期及区段刷新。
- 锁定API审计后的最小补充：`META-INF/accesstransformer.cfg`仅公开`RenderChunkRegion.level`，必要时构建只接入此文件。上下文捕获据真实所属ClientLevel关联会话；未知包装层、Ponder及旧世界降级空快照，不能用被委托的light-engine身份替代。
- 相应定向测试只验证描述编解码、几何/归属、revision、不可变发布和撤销；报告路径另卡明确。若需要修改上述之外的生命周期或注册文件，先报告PM。

**美术侧资产准备02A（当前可继续）：** 按已确认设计维护`tools/art-assets/reactor-ct/`源稿、导出器、mapping和generated及专属报告；不覆盖ART-REACTOR-01。RECTANGLE每格16×16、4×4图集共64×64，原sprite保持。

**美术侧真实客户端接入预留写集（逻辑接口交付后另卡）：**

- 新建`structure/client/ReactorConnectedTextureBehaviour.java`、`ReactorConnectedTextures.java`、`ReactorConnectedTextureClientEvents.java`，只消费已交付快照、映射实际sprite并包装原baked model。
- 消费侧允许新增专属`src/test/java/com/iksxh/create_nuclear_industry/structure/client/ReactorConnectedTextureTest.java`：只测空/可靠上下文快照、跨owner、预期ID、六面连接及ThreadLocal嵌套/异常恢复；不修改L1公共API/测试，不重复L1生命周期或02A资产矩阵。这是02B前置通过后任务卡的预留写集，不代表现在已派发。
- 14张目标图固定为`src/main/resources/assets/create_nuclear_industry/textures/block/reactor_ct/<原sprite名>.png`，对应源sprite为原`block/<原sprite名>`：`reactor_casing_side/top/bottom`、`reactor_hot_port_side/top`、`reactor_cold_port_side/top`、`reactor_window`、`reactor_instrument_port_side/top`、`reactor_refueling_port_side/top`、`control_rod_drive_side/top`。
- 不修改`CreateNuclearIndustry.java`、`P1Blocks.java`、注册ID、现有blockstate/model JSON、语言、碰撞、流体、热工、Ponder或配置。客户端事件独立注册；需要共享入口时先协调。
- 映射必须匹配原模型真实sprite，保留窗口透明区、功能孔/法兰及既有局部标识；是否跨壳体/窗口/端口连接背景由美术材质表决定，结构归属由逻辑接口决定。

## 验证与交付顺序

1. 美术02A可先交原创图块、mapping、14图集和多尺寸离线预览；不将它记为游戏成型接入通过。
2. 逻辑前置另卡核对锁定版本的BE更新、区块事件和模型线程边界后实施。必要定向测试与一次增量编译/打包；失效、同坐标owner置换、跨区块恢复及换世界是重点。只有新增证据缺口才扩大验证，不跑旧存档兼容。
3. 逻辑交付以编译后的公共类型/签名和生命周期证据冻结消费API，美术再接入CT包装。消费语义不得自行改变，模型加载时机和专用服务端隔离须验证。
4. 成型/拆坏/相邻独立结构、各面拐角与功能孔、跨区块和重新进入游戏的显示由最终候选定向验证；用户视觉门另记，不重复全部设备玩法测试。

初次协调只完成接口与分工文档；随后PM直接核实美术对话中用户“好”的确认，明确该显示接口是已批准美术任务的必要前置，按并行授权另卡L1实施。02A随后提交224SVG/224图块/14图集及独立审查，资产冻结不代表游戏接入验收；美术预检确认无参快照不足以排除旧世界延迟构建与Ponder上下文，故本合同增加必须实证的上下文入口。Create本版本`getModelData`为final，美术拟在`gatherModelData`作用域固定一次快照并finally恢复/清理，逻辑不得仅用猜测的API交付。换热器五幕待播放门与美术最终视觉门保持。文件与后续共享HANDOFF供美术负责人只读接续；未借收到协调消息向另一聊天发送回信。
