# ART-BOILER-05-L1：锅炉成型表面显示合同

2026-10-11。主PM已只读核实美术负责人对话`01a11c6b-f440-77c1-9275-5b2dca5cfa0f`中用户原话“实施第一种设计稿吧”（原用户轮次`01a12a7f-42da-7a23-a4fc-30628c4e582b`），A横箍压力舱及成型连接表现已获授权。本合同是该表现所必需的只读逻辑前置，沿用已交付反应堆L1模式；不新增玩法决策、不改变锅炉结构、库存、热账本或尺寸配置。此处签名为实施合同，编译消费以最终HANDOFF实物为准。

## 权威来源与职责

服务端只从`BoilerControllerBlockEntity.currentForm()`已决定的缓存`BoilerStructure.Form`派生投影，不新增`inspect()`、客户端成型扫描或正常tick表面扫描。Form给出闭min/max、绝对`partitionY`和全部特殊表面角色；先将完整外表面视作casing，再按窗、四类口、换热器、安全阀和owner位置覆盖，能够纯由Form推出9种合法ID。`sections`为内部再热段，不作为外表面成员。

完整棱角保留多朝外面，不使用仅用于水平流体口的`Form.outward()`。隔层横箍由`pos.y==partitionY`判断；不由Width或控制器朝向反推边界，不由Status/Fill判断成型。红石停机、缺水/热、冷液满和成员运行冲突仍可具有有效成型几何。只有缓存Form失效、锅炉配置拒绝结构或可靠显示证据撤销时回单块；无效换热器运行配置不另改成型含义。

## 信封和最小API

- `boiler/BoilerSurfaceDescriptor`：不可变dimension、ownerPos、实例UUID generation、非负revision、valid、闭min/max、绝对partitionY、完整不可变表面成员。无世界、BE、NBT或库存引用；位置和集合防御复制。
- `boiler/BoilerSurfaceDescriptorFactory`：接收已验证Form及dimension/owner/generation/revision，纯投影9种角色，不读世界方块。公开输入不意味着任意手工Form可作为正式成型证据；生产调用仅在控制器已完成缓存决策后。
- 控制器公共`Optional<BoilerSurfaceDescriptor> surfaceDescriptor()`，仅提供已冻结描述，不触发扫描。公共BE不能引用客户端类型。
- `boiler/client/BoilerSurfaceSnapshots.capture(BlockAndTintGetter context)`返回`BoilerSurfaceSnapshot`；仅当前真实ClientLevel或原生精确RenderChunkRegion同世界有效，复用已有AT。未知包装、Ponder和旧世界返回`BoilerSurfaceSnapshot.empty()`。
- `snapshot.findSurface(BlockPos pos, Direction face)`返回`Optional<BoilerSurfaceSnapshot.Member>`，只索引可靠的真实朝外面。Member访问器为`dimension()`、`ownerPos()`、`ownerGeneration()`、`revision()`、`min()`、`maxInclusive()`、`partitionY()`、`pos()`、`expectedBlockId()`和`outwardFaces()`。

generation为每个服务端控制器实例新UUID，不复用能力epoch、不持久化。revision只随valid/bounds/partition/成员ID变化推进；同几何遥测重发不重建或刷新CT。显示字段只进入现有clientPacket包，不进入服务端保存或portable Ledger。编码只读取冻结值，禁止在write/getUpdateTag中扫描。

## 传输预算

固定schema1，9种合法ID为casing/window/controller/water/steam/hot/cold/safety_valve/nuclear_heat_exchanger的正式注册ID。闭边界确定唯一表面枚举顺序，wire使用每成员一byte类型索引；公开消费仍为完整不可变成员，不暴露可变数组。

工程配置上限32保持原值，最大表面5768块、6144朝外面，最多9个所需区块。描述NBT预算16KiB；以最大合法Form实际序列化核对，不复制逐成员CompoundTag的大包。解码先检查字段类型/schema、有限dimension、安全坐标、UUID/revision、各轴5..32、合法隔层、精确数组长度/索引和唯一owner；未知或坏包撤销，不能用其边界扩大刷新。预算属于显示安全，不改变成型或配置。

## 生命周期和消费边界

1. 服务端当前缓存形成/变化后发布一次；invalidateForm时撤销并必要即时同步，连续无效去重。不要改变原端口失效、压力、库存或tick提交顺序。
2. 客户端onLoad原本也调用invalidateForm，新投影只能在服务端路径撤销；首包早于onLoad时必须保留，随后凭真实BE/区块核实。区块卸载先suspend，随后的setRemoved/invalidate不能误退休同generation；真拆除才retire。
3. 游戏线程按当前世界、真实owner实例、generation/revision水位及所有bounds区块对象维护锅炉独立状态。成员/owner卸载、换槽/缩视距、坏包、歧义或世界切换撤销；可靠Load/新包恢复，不要求拆放控制器。旧对象延迟回调不得撤销新owner。
4. 每客户端tick只对已知描述必需ChunkPos做不强载的实例核实，不读方块、不遍历玩家世界。先原子发布，再刷新旧/新可靠bounds并集外扩一格的已加载区段；去重且同描述不刷新。不引入运行租约或heartbeat。
5. 美术一次模型构建只捕获一次快照；双方同dimension/owner/generation/revision/bounds/partition、同外平面且当前局部块ID仍匹配才连接。材质、窗共享面、Create索引和客户端注册归美术消费者，本前置不写这些文件或资源。

## 实施、验证与交付

执行范围和必要检查见[实施卡](../plans/2026-10-11-boiler-surface-display-01.md)。仅一次锅炉表面定向JUnit＋原BoilerStateTest回归及增量assemble，一次专属namespace真实控制器探针，一次独立规格/质量窄审。原反应堆L1 API/加载次序和锅炉玩法证据复用，不跑全量、管网/汽轮机、旧存档或已验收教学。

主PM核实实物API与源/class/JAR绑定后，净合main并只同步共享精确写集到美术树；不覆盖美术dirty docs/素材/窗口模型，不重复美术最终assemble。现已交[实际API与准许消费者写集](../reviews/2026-10-11/boiler-surface-display-01/HANDOFF.md)：main `eb3ec43`、美术净同步`53d699b`，29/29及1/1真实探针、独立窄审通过。美术负责人依HANDOFF继续其批准消费者，最终视觉仍由用户复看。
