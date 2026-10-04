# EXT-B-BOILER-01B：Create 无外泵蒸汽出口核查

只读静态核查，HEAD `9e8690b`；未运行 Gradle / 客户端 / GameTest，未执行 Git 写操作。已对照 `AGENTS.md`、治理协议5.1、01A卡/方案及 `minecraft-modding`、`minecraft-testing` 技能；技能的新版本示例未用于推导当前API。

## 结论

当前蒸汽口只能作为 Create 管网的流体能力端点。普通 Create 管路的流动由 `FluidTransportBehaviour` 中 `PipeConnection` 的压力启动；相邻 Create 储罐也不会主动抽取。故现状需有动力机械泵，无法满足“储罐相邻、普通管线均无外部动力泵也能出汽”。不需要 Mixin 才能暴露端口能力；但无泵管输必须实现一个使用 Create 原生管道连接/压力数据的主动压力适配器。单纯添加能力或 `FluidTransportBehaviour` 不会产生压力。

获批整炉蒸汽提取预算256mB/t可映射为 Create 起流压力512：Create `FluidNetwork.tick()` 以初始管连接压力除以2得到传输速度（最小值1），故单一路径标称512/2=256mB/t；多并行支路的压力会由 Create 泵算法按支路数分摊。此映射是Create内部压力数值，不是RPM，也不应与锅炉库存百分比/物理压力绑定。Create没有“锅炉端口虚拟压力”的标准值。

## 源码证据（本机锁定 `create-1.21.1-6.0.10-280-sources.jar`）

- `FluidPropagator.hasFluidCapability(...)`检测相邻方块的`Capabilities.FluidHandler.BLOCK`；`PipeConnection.determineSource(...)`据此把储罐/锅炉端口识别成`FlowSource.FluidHandler`。这解释了当前泵管能识别并搬运锅炉蒸汽。
- `FluidTransportBehaviour.tick()`逐连接调用`manageSource`与`manageFlows`；`PipeConnection.manageFlows()`在无压力时直接不启动流动，并仅在已有流向由端点进入管道时创建`FluidNetwork`。
- `FluidNetwork.tick()`从起始连接读取`pressure.get(true) / 2f`作为本tick传输速度。Create机械泵通过`PumpBlockEntity.distributePressureTo(...)`沿管网遍历、按并行支路分配压力，再调用`PipeConnection.addPressure(...)`。
- `PumpBlockEntity.distributePressureTo`为`protected`实例方法，不可由锅炉端口直接调用。可复用的公开机制包括`FluidPropagator.getPipe/getPipeConnections/getPumpRange/resetAffectedFluidNetworks`与`PipeConnection.addPressure`；完成无泵输出需锅炉侧维护受影响管网的压力传播/刷新。
- 锁定源码`CFluids.mechanicalPumpRange`默认16格、最小1格，描述为机械泵两侧最大推/吸距离，属于服务器可配置值。复用它比新增硬编码距离更贴近原生规则。
- 当前`BoilerPortBlockEntity`继承普通`BlockEntity`且未添加Create行为；`BoilerControllerBlockEntity.Port`实现`IFluidHandler`/`SharedFluidReceiver`，抽汽由`BoilerState.drainSteam`共享限额。现有`ExtensionBoilerGameTests.realCreateSteamPumpFillsNativeTank`布置`PumpBlock`并断言转速非0，验证的是有动力泵场景。

## 最小可靠实现边界与风险

1. 相邻储罐由锅炉控制器主动查询外向相邻块能力，对可接受蒸汽做模拟，再在实际接受量内提交抽汽与填充；此路径和所有汽口共用同一个256mB/t账本，不能再额外开放同tick的管网预算。
2. 管线仍用Create原生`FluidTransportBehaviour`/`PipeConnection`/`FluidNetwork`。炉口在有效成型、未红石停机且有汽时作为虚拟压力源，压力512的标称总供汽仍受源端256mB/t总账限制；只向汽口`facing`外的连接传播。沿用`getPumpRange()`（当前默认16，可配置），不能把它写成固定新参数。
3. Create压力是连接上无来源归属的累计数值：`addPressure`会相加，原生泵网络`wipePressure()`会清除连接全部压力。不得每tick重复累加512；拓扑变化、其他泵刷新及并行分支可能造成压力丢失/叠加或方向变化。必须避免改动无关管线行为，并针对同网原生泵刷新、多个出口/分支、外向校验和预算共享验证。若无法给锅炉贡献建立可靠生命周期，主动压力机制尚不能称为无冲突兼容。
4. 端口只能在外向面对Create网络供汽；`StorageOnlySteamFluid`无桶且不成为世界液体，保留开放管口拒绝放置蒸汽，避免汽液在无容器出口时消失。

## 几何与吞吐

现行`BoilerStructure.inspect`校验固定3×3×4壳体：底面中心必须为普通外壳，外围最多8格可放热段；`BoilerState`按每段18HU/t及1HU/mB换算，故一个热段对应18mB/t、8段144mB/t。用户已更新需求，首机尺寸/换热段数仍由PM核对；本报告只确认现行实现的一段额定值，不据此冻结新结构。整炉出口预算当前为256mB/t。
