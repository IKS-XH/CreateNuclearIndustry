# ART-REACTOR-03-L2 只读运行显示合同

2026-10-10，主逻辑PM。用户在美术负责人对话直接要求反应堆内部模型及运行动画，并确认按实际冷热库存混合连续渐变；主PM已只读核实该用户消息（对话`01a11c6b-f440-77c1-9275-5b2dca5cfa0f`，消息`01a1220a-3b2d-72c1-83d1-d557454b6996`、`01a1220c-6cf0-7de3-a8fd-eb37de1d0e5f`）。此合同只冻结必要的共享显示前置；不是动画或02R1视觉验收。

## 数据与边界

- 普通仪表BE只发布一份不可变、完整同龄的运行信封；不改变反应堆计算、库存、热工、控制速度、结构、配方或玩家控件。保留原L1表面描述、CT、遥测及持久化合同。
- 每份有效样本对应一次完整成功结算：`ReactorServerTick.Result`的列裂变HU/t、tick后冷热mB与控制棒实际/目标深度、卡死状态、同次`coolantInput`的共享容量，以及已结算转换mB/t。不得重新计算或混合不同tick数据。
- 使用缓存`structureScan.columns()`和已确认L1 bounds生成列类型、cap/body及预期注册ID。冷却空间只包含EMPTY及CONTROL_ROD的body，不包含燃料列，也不填整个bounds。深度0为完全拔出，1为完全插入；实际行程由body长度给出，不写死三格。
- 燃料可用状态与实际产热分别传输：耗尽tick可能已经不可用但本tickHU/t仍大于0，不伪造产热为0。蓝辉消费者按可靠可用列决定显示，不能以目标、库存或存量热代替真实裂变功率。
- 信封身份包括dimension、owner、L1 generation、独立runtime revision、匹配geometry revision、成功sample序号及serverGameTime、available和bounds。客户端session仅由当前ClientLevel实例绑定，不伪装为服务端会话。
- NBT解码必须有类型、数量、坐标、有限数值、重复位置和几何完整性校验；采用不超过L1的64边长预算，最多256列、4096个body位置。预算不定义玩法尺寸。错误或缺字段返回不可用，不能保留当前owner旧动画。

## 同步与失效

`tickReactor()`返回false既可能是早退，也可能是成功但无状态变化。只在四个明确提交前失败、异常、重扫描/失效、外部setSnapshot及owner生命周期撤销L2；成功零变化样本仍有效。正式tick内部的setSnapshot及hydrate原有中间包只能带旧完整样本，不能在write时拼新snapshot和旧telemetry；用明确内部结算作用域区分外部改写，finally恢复，原正式提交顺序保持。

成功恢复/失效立即同步，正常运行每5个成功tick最多一次完整心跳；稳定库存和零产热也要续期。客户端20tick租约按新sample本地收到时间计算，重复/旧sample不续期；过期撤下，后续新可靠样本恢复。它仅是显示保险，不影响服务器运行或动力。

真暂停边界（R2）：仅Minecraft.isPaused为true时冻结本地租约年龄，不续样本、不复活已失效显示；当前世界同步、区块核查、坏包及卸载撤销仍执行。非真暂停的多人菜单正常计时，恢复后累计活动tick满20仍到期。已核实本地Pre在真暂停仍触发，[窄修卡](../superpowers/plans/2026-10-10-reactor-runtime-display-02-pause.md)自动/独立审查通过并净同步main `93d0a20`、美术`2bece5b`，最终源/API入口见HANDOFF最新R2；公共API与服务器不改，视觉观察仍独立。

客户端独立订阅既有`ReactorSurfaceSyncEvents.Update`及必要世界/区块事件，不修改L1。capture必须验证当前真实ClientLevel或既有AT公开所属世界的原生RenderChunkRegion；Ponder、未知包装、旧世界返回空。相关区块只用非加载查询，并记录真实chunk实例：卸载、替换、视距缩小、owner更换/拆坏、世界切换立即撤销，恢复要求新可靠样本。保留代次/revision水位，拒绝过期包和歧义归属。

## 冻结公共入口

包`com.iksxh.create_nuclear_industry.structure`：`ReactorRuntimeDescriptor`及其列记录；BE提供`Optional<ReactorRuntimeDescriptor> runtimeDescriptor()`，不得引用客户端类。公共访问器须覆盖上列语义；精确构造器/字段表由编译产物HANDOFF登记，消费者只能使用实际交付签名。

包`structure.client`：

- `ReactorRuntimeSnapshots.capture(BlockAndTintGetter context)` → `ReactorRuntimeSnapshot`。
- `ReactorRuntimeSnapshot.empty()` → `ReactorRuntimeSnapshot`。
- `snapshot.findOwner(BlockPos ownerPos)` → `Optional<ReactorRuntimeDescriptor>`。
- `snapshot.findControlRod(BlockPos capPos)` → `Optional<ReactorRuntimeDescriptor.ControlRodColumn>`。
- `snapshot.findControlOwner(BlockPos capPos)` → `Optional<ReactorRuntimeDescriptor>`：同一次capture取得棒列归属身份；与findControlRod同龄，歧义/不可用为空，同cap换owner/代次不能跨身份延续插值。仅从既有owners索引构建，不增加同步字段或扫描。

一次绘制只捕获一次，不保留Level/BE/NBT或可变集合；消费者还须按预期ID检查当次本地可见方块，矛盾时撤下相应动画，不能用自己扫描补全缺失投影。

## 静态遮挡适配

仅`content/P1Blocks.java`的REACTOR_FUEL_ROD注册使用`new Block(properties.noOcclusion())`。锁定1.21.1的OBJ automatic_culling=false不能关闭块级邻面剔除/VisGraph完整遮挡；该适配关闭燃料格完整遮挡，保留完整碰撞/选框、结构和配方。默认阻光由满级变1，并非0；不新增shape/采光覆盖，不将此变化推广到别的块。

## 后续美术消费范围（编译HANDOFF交付后生效）

美术负责人可冻结新增`structure/client/ReactorAnimationClientEvents.java`、`ReactorAnimationModels.java`、`ReactorInternalRenderer.java`、`ReactorControlRodRenderer.java`、`ReactorAnimationVisualState.java`、`ReactorAnimationMaterials.java`，以及相应专属定向测试。只注册既有instrument/drive两种BE的BER、初始化PartialModel及管理显示插值/专用动态材质/资源重载；渲染包围盒由BER按真实bounds与完整棒体行程覆盖，无需改BE。不得改共享同步、P1Blocks、L1/CT、全局流体纹理或世界状态。

专用材质允许读取03A正式安装帧，通过CPU插值冷热/动画帧、每堆复用16×16 DynamicTexture，原生entityTranslucent绘制唯一合法空间并集外面；有界缓存，失效/卸载/会话切换/资源重载释放，不逐帧新建纹理。不增加着色器或依赖。离线样例不是客户端透明排序证据。

正式资源安装仅限03A冻结清单映射到燃料棒新静态模型、专用动画模型/材质及其引用；美术任务卡另列精确路径，不能笼统取得整目录写权。03A素材、消费自动验证、最终动画视觉门与02R1门各自独立。屏蔽装配台教学07仍处于已准备未派发状态，不被本次接口前置覆盖。
