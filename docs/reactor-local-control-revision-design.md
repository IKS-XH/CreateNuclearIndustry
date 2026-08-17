# P1.2 反应堆局部控制修订设计

**状态：** 用户已批准
**修订对象：** `2026-08-16-experimental-reactor-design.md` 中的结构核心、交互入口和统一控制模型

## 1. 方块职责修订

- 删除 `reactor_frame`。反应堆外壳 `reactor_casing` 是棱边、转角、顶面和底面的唯一基础结构方块。棱边和转角不能被观察窗或功能方块替换。
- `reactor_instrument_port` 是反应堆必装的核心功能方块、结构锚点和唯一权威状态所有者。每个结构必须且只能安装一个。第一版扳手右键点击此方块检测结构,通入红石信号会触发SCRAM。
- `reactor_control_port` 是可选外设，不参与成型必需条件，也不拥有反应堆状态。它只在相邻烈焰人燃烧室时提供自动控制桥接；没有该方块时反应堆仍可正常成型和工作。
- `reactor_instrument_port` 实现 Create 护目镜信息接口。玩家佩戴工程师护目镜并注视它时显示浮动信息，不需要右键交互，不打开菜单。
- 每个 `control_rod_drive` 提供 Create 风格的独立深度滑块，范围 `0～100%`，滚轮/值框修改由服务端验证并写入所属结构的逐棒设定。
- 控制棒不注册为可放置柱方块；`control_rod_drive` 是唯一相关方块。驱动器下方内部有效高度保持空气，控制棒由驱动器方块实体渲染为无碰撞箱模型动画，渲染不参与结构扫描和碰撞判定。

## 2. 自动成型与运行条件

反应堆第一版没有“组装”“启动”“停止”“功率设定”按钮。仪表接口自动检测合法结构。运行状态只由以下两项决定：

1. 当前仍有可用核燃料；
2. 局部控制棒深度计算出的有效裂变强度是否大于零。

燃料耗尽或所有可控燃料的有效强度为零时，裂变停止，但余热继续结算。直接相邻燃料形成的反馈超频无法用控制棒压低；含这种布局的反应堆只能通过耗尽或移除燃料终止该部分裂变，因此它是明确的高风险布局。

## 3. 局部邻接控制算法

活性区只考虑同一水平截面内的北、南、东、西四向邻接，不考虑对角线。列沿高度保持统一类型和统一控制设定。

对每个未超频燃料列 `f`，取其四向直接相邻控制棒集合 `C(f)`。控制棒插入深度 `d ∈ [0,1]`，`0` 为完全拔出，`1` 为完全插入：

```text
meanDepth(f) = C(f) 为空时 0，否则 average(d(c), c ∈ C(f))
controlledIntensity(f) = (1 - meanDepth(f)) ^ controlResponseExponent
```

`controlResponseExponent` 是服务端配置项。多个控制棒共同影响同一燃料时取平均值，避免控制棒数量本身放大或缩小燃料额定功率。

## 4. 直接相邻燃料反馈超频

若燃料列 `f` 至少有一个水平四向直接相邻且未耗尽的燃料列，则它进入反馈超频簇。所有超频列从 `1.0` 倍开始，以同步轮次读取上一轮相邻燃料的产热强度并迭代到稳定值：

```text
signal(f) = Σ heatIntensity(n), n 为 f 的四向直接相邻有效燃料列
activation(f) = 1 - exp(-overclockFeedbackGain × signal(f)^overclockFeedbackExponent)
heatIntensity(f) = 1 + (overclockHeatMultiplier - 1) × activation(f)
burnIntensity(f) = 1 + (overclockBurnMultiplier - 1) × activation(f)
```

`overclockHeatMultiplier` 与 `overclockBurnMultiplier` 是硬上限而非固定结果。迭代使用上一轮完整快照，最大 256 轮，所有列变化不超过 `1e-9` 时收敛；映射单调且有界，从 `1.0` 开始会得到确定的最小稳定解。反馈规则为：

- 相邻燃料越多，输入信号越大，超频强度非线性升高；
- 相邻燃料自身越强，下一轮给邻居的反馈越强，因此连续燃料簇会形成有界正反馈；
- 控制棒和中空列没有燃料信号，会完全切断反馈簇；
- 进入反馈簇的燃料不读取控制棒深度，仍属于无法用控制棒停下的高风险布局；
- 孤立燃料仍严格按第 3 节公式控制。

## 5. 总产热与燃耗

设每个垂直燃料棒方块的额定产热和额定消耗分别为 `baseHeatPerFuel`、`baseBurnPerFuel`，结构内部高度为 `internalHeight`。局部邻接仍按整列在水平截面中只计算一次，但一整列的产热、燃耗和可装燃料库存均随内部高度线性增长：

```text
rawHeat = baseHeatPerFuel × internalHeight × Σ heatEquivalent(f)
nominalInstalledHeat = baseHeatPerFuel × internalHeight × installedFuelColumnCount
heatCap = nominalInstalledHeat × totalHeatMultiplierCap
totalHeat = min(rawHeat, heatCap)

totalBurn = baseBurnPerFuel × internalHeight × Σ burnEquivalent(f)
fuelCapacity(f) = fuelCapacityPerBlock × internalHeight
```

数值锚点：`internalHeight = 1`、满功率、无超频时，一个组件必须在 3 小时后耗尽。按 20 tick/秒换算，`baseBurnPerFuel` 的基准量级为 `1 / (3 × 3600 × 20)` 每格高度，再由实际强度、超频燃耗倍率和配置单位折算；该反推结果必须作为数值平衡验收记录。玩家可修改 `baseBurnPerFuel` 或等价的 `burnHoursPerBlock` 配置项。

普通燃料的 `heatEquivalent` 与 `burnEquivalent` 均等于 `controlledIntensity`。超频燃料分别使用配置的热倍率和燃耗倍率。高度只放大每列包含的有效燃料棒方块数，不重复叠加超频倍率。总产热受 `totalHeatMultiplierCap` 截断；燃耗不随产热截断而回退，防止用低上限无成本承受危险相邻布局。同样装满且控制深度相同的不同高度反应堆，其总库存与燃耗速率同比增长，因此理论满载续航不因高度单独改变。

当某根燃料库存耗尽时，该列从下一 tick 起不再视为有效燃料，不再产热、燃耗或触发相邻超频；邻接关系随之局部重算。

## 6. 服务端配置项

第一版至少提供并验证以下有限非负配置：

- `controlResponseExponent`，默认 `1.0`；
- `baseHeatPerFuel`；
- `baseBurnPerFuel`；
- `burnHoursPerBlock`（如采用寿命配置表达，则与 `baseBurnPerFuel` 二选一，不能形成两个权威）；
- `overclockHeatMultiplier`，反馈产热硬上限，默认 `10.0`；
- `overclockBurnMultiplier`，反馈燃耗硬上限，默认 `10.0`；
- `overclockFeedbackGain`，默认 `0.15`；
- `overclockFeedbackExponent`，默认 `0.5`；
- `totalHeatMultiplierCap`，默认 `20.0`。

反馈增益、反馈指数、超频热/燃耗硬上限和全堆产热倍率上限彼此独立。模拟器允许实时调整全部参数，用于决定最终默认值；游戏实现仍以服务端配置为权威。

## 7. 信息显示

工程师护目镜浮动窗口显示：运行状态、总产热、余热、温度、冷/热冷却剂、总燃耗率、剩余燃料、当前总倍率、配置倍率上限、最高警告。对于超频布局，窗口明确列出超频燃料列数量和“控制棒无法调节直接相邻燃料”的危险提示。

每个控制棒值框显示该棒的插入百分比。没有全堆统一深度滑块，也没有控制端口右键菜单。烈焰人控制端口的自动算法只允许改变逐棒目标深度，不得绕过同一服务端范围验证。

## 8. 数据与测试影响

- 状态所有权从控制端口方块实体迁移到仪表接口方块实体，存档格式版本递增；既有实验存档迁移不属于第一版可玩切片，待切片完成后单独处理。
- 结构快照保存每个控制棒局部坐标及独立深度；不再只保存一个全堆深度。
- 结构检测要求一个仪表接口，不要求控制端口；冷端和热端仍是必需流体接口。
- 删除框架方块的注册、创造栏条目、模型、纹理、语言、掉落和挖掘标签；原框架世界方块不提供静默映射。当前未发布开发存档需由玩家手工替换为外壳。
- 纯模拟测试覆盖四向邻接、平均控制深度、无控制棒满功率、两棒反馈、邻居数量与邻居强度的非线性增长、反馈不跨控制棒或中空列传播、倍率硬上限、不受全堆产热上限保护的燃耗，以及燃料耗尽后的局部重算。
- NeoForge 集成测试覆盖自动检测、仪表接口唯一所有权、护目镜只读信息、逐棒滚轮同步和可选烈焰人控制端口。

