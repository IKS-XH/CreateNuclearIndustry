# EXT-B-TURBINE-01C-REFERENCE：AeroEngine-1.2.4 多方块搭建与结构判定只读调查

**性质：** 基于目标 JAR 的运行时代码反编译与随附资源核对；不是本项目玩法决策、实现或验收。报告与提取材料只写入 `build/reports/extension/EXT-B-TURBINE-01C-REFERENCE/`。本次未运行 Gradle、游戏或网络操作；未改 Java、资源、核心文档或 Git。开始前只读 Git 状态已有 `logs/debug.log`、`logs/latest.log` 修改及 `tools/art-assets/__pycache__/` 未跟踪项，均保留。

## 样本与方法

- 样本：`E:/MyMC/NewMod/AeroEngine-1.2.4.jar`，长度 1,456,552 bytes，SHA-256 `52B40C8D84855D0A0AE3C63F6FCAEAD16B3D40037F146A58D8CFABC2B112CAC0`。
- 用本机 Java 21 与 Gradle 缓存内 Vineflower 1.10.1 对目标类反编译。反编译源与 JAR 资源副本位于报告目录内的 `decompiled/`、`largefan/`、`kinetics/` 子目录；下面 Java 行号指这些由目标 class 反编译生成的文件，不是上游源码行号。重要方法名和算法可在原始 `.class` 中独立复核。
- 读取了活动任务卡 `docs/superpowers/plans/2026-10-04-ext-b-turbine-01b.md`、汽轮机结构方案 `docs/superpowers/plans/2026-10-04-turbine-structure-revision-proposal.md`、治理协议 §5.1、仓库 `AGENTS.md`，以及 `minecraft-modding`、`minecraft-testing` 技能。技能版本示例未用于推断 AeroEngine API；这次是只读参考调查，未做测试。
- 01B 方案此前列的 Aero 证据是状态 JSON 和模型资源。本报告进一步核了方块类、风扇方块实体、结构扫描器和 Ponder storyboard，结论以目标 JAR class 为准。

## AeroEngine 的实际机制

### 1. 发动机核心以风扇为扫描锚点，不是独立 controller

`EngineFanBlock` 继承 Aeronautics 的 `BasePropellerBlock`，使用 `FACING` 与 `REVERSED` 属性；Aero 类构造器把 `REVERSED` 默认设为 `true`（反编译 `EngineFanBlock.java:31-42`）。其方块状态模型按 `facing` 旋转（`assets/aeroengineering/blockstates/engine_fan.json`）；未覆写 wrench 行为，`onWrenched` 返回 `PASS`（`EngineFanBlock.java:91-93`），也没有自己的面向扳手切换分支。它继承的放置方向逻辑不在目标 JAR 内，本报告不猜测“看向/放置面”的具体映射。

`EngineCasingPlacementHelper.isEngineCore` 把风扇、压气机、燃烧室和涡轮列为核心块，`getCoreAxis` 从相应核心块 `FACING` 得轴（`EngineCasingPlacementHelper.java:132-148`）。风扇 BE 的状态结果保存为 `EngineStructure.Result`，字段是 `coreComplete/casingComplete/nozzleComplete/engineType`；没有 owner/controller 坐标、逐件绑定表或组装后主控制器迁移（`EngineFanBlockEntity.java:66-87,855-890`）。所以这个参考的“形成”只是风扇扫描到邻近有序核心并更新状态缓存，不会把周边块替换成一个多方块控制器或重建整体方块模型。核心部件间也不是插入 Create shaft 方块后再检查 shaft ring：风扇 `hasShaftTowards` 恒 false（`EngineFanBlock.java:72-74`），相邻 turbine/combustor/compressor/fan 的 BE 通过 `EngineKineticConnections.canConnect` 依相对方向和各块 facing 建立 Create 动力连接（反编译 `EngineKineticConnections.java:17-76`; `EngineFanBlockEntity.java:544-552`）。

### 2. 放置方向和壳体局部方位

普通壳体自身状态为 `axis + angle + segment`（`EngineCasingBlock.java:31-47`）：`axis` 是发动机轴，`angle` 为截面八个方位（水平/斜角），`segment` 为与轴向相邻壳板连接的段形（single、end_a、end_b、middle）。

直接放置时，`getStateForPlacement` 先检查六邻方块；若邻方块是核心块，就从该核心朝向取轴、从相对位移取八向角，并立刻计算段形，否则退回默认 `axis=Z, angle=SOUTH, segment=SINGLE`（`EngineCasingBlock.java:59-77`）。

非潜行时对发动机核心或壳体使用壳体物品，会进入 Create `IPlacementHelper`：点核心时按八个 3×3 环位找到首个可替换格，自动填入与中心轴/角匹配的状态；点壳体时优先沿壳体轴方向找相邻可替换格，且仅在候选位置反向映射到同轴核心时放置。优先方向由点击面、点击高度或玩家方向选择（`EngineCasingPlacement.java:16-28,39-46`; `EngineCasingPlacementHelper.java:30-54,57-107,110-137`）。壳体上 `Shift` 会绕过 placement helper、交还普通方块交互；风扇上 `Shift` 也会停用 helper（`EngineCasingPlacement.java:19-28`）。风扇 wrench 返回 PASS；目标 JAR 中未见通过扳手调节风扇朝向的实现。

### 3. 邻接更新改变的是壳体的段模型

壳体 `updateShape` 仅当更新来自与其 `AXIS` 相同的方向时调用 `withSegment`（`EngineCasingBlock.java:79-81`）。`withSegment` 要求其中心仍是同轴核心，再检查轴向正、负邻居是否为相同轴和相同 angle 的壳体；按连接数量/侧向把 segment 设为 single/end_a/end_b/middle（`EngineCasingBlock.java:144-165,253-276`）。因此壳片不是一个静态的重复板面：轴向邻接会本地切换端段/中段模型；它不代表整机成型，也不绑定 owner。

视觉网格与碰撞需要分开理解：`base_middle.json` 可见主体只占 3/16 方块厚（`elements` 第一面板 y=0..3），另有边肋；但 `EngineCasingBlock.getShape` 直接返回 `Shapes.block()`（`EngineCasingBlock.java:49-51`），也就是完整方块选择/碰撞体，并非 3/16 薄碰撞板。状态模型 JSON 将不同轴、八向角与四种 segment 映射到 base/corner 与端/中段网格（`assets/aeroengineering/blockstates/engine_casing.json`）。

### 4. 结构检查分核心、壳环和喷口，壳环不是运行必需项

`EngineStructure.evaluate` 以风扇的 `FACING` 定前后，沿反向逐格检查发动机核心件种类及各自预期朝向。基础涡扇核心为 fan → compressor → combustor → turbine → exhaust cone，每个都必须紧贴、顺序正确、面向匹配（`EngineStructure.java:191-280,394-444`）。它只返回 Result，没有将壳板改为“已成型”状态。

壳体独立按 8 个截面偏移扫描每层。普通风扇从风扇本体所在截面开始检查四层，large fan 跳过进气风扇截面，仅检查后续三层；按每层8格计，分别是普通风扇32格、large fan 24格；每个格须是 `EngineCasingLike`，或白名单管道/齿轮箱；若为 `EngineCasingBlock`，还要求轴与该格八向角匹配（`EngineStructure.java:259,446-477`）。壳体扫描结果单独记录为 `casingComplete`，不并入核心是否完整。Ponder 明确演示无壳仍能旋转产生推力，补齐壳环会显著提高推力（`AeroEngineeringPonderScenes.java:750-785`；中文语言键 `assets/aeroengineering/lang/zh_cn.json` 中 `aeroengineering.ponder.basic_turbofan_engine.text_1..7` 讲核心顺序，text_8..9 讲油路/红石）。因此壳体是性能/完整度条件，不是控制器形成的硬门槛。LargeEngineFanBlock 仍是单个 intake 方块，并限制水平朝向；其自身方块/碰撞体为 16³ 整格（反编译 LargeEngineFanBlock.java:15-42）。较大的螺旋桨由 BE 渲染尺度处理，数值可从 1.5 调至 2.0 倍（LargeEngineFanBlockEntity.java:16-63），不是在外侧继续摆风扇方块。大型风扇引擎仍是五件核心顺序；Ponder 说以 Large Engine Fan 替换 intake fan，并给 bypass airflow（AeroEngineeringPonderScenes.java:824-872）。其壳完整性扫描从 startDistance=1 开始，跳过 intake 风扇所在截面，后续 compressor、combustor、turbine 三层共24格需要包壳完整；Ponder 特意框出 intake 外圈并说明 large fan 不需要这圈来满足 casing 完整度，之后才展示 compressor/hot core 三层壳体可增加推力（同文件 EngineStructure.java:259,446-477; AeroEngineeringPonderScenes.java:847-854,944-957）。

### 5. 修改/拆除时如何恢复

风扇 BE 缓存结构结果；缓存非 dirty 且距上次扫描少于 20 game ticks 时直接复用，否则调用 `EngineStructure.evaluate` 并重置 dirty（`EngineFanBlockEntity.java:855-879`）。`markStructureDirty()` 会清理推力方向、风扇/尾锥引用及气流几何缓存（`EngineFanBlockEntity.java:881-892`）。所以缺少或方向错误的轴心核心块会在下一次扫描后成为 incomplete；壳体格被拆时，其轴向邻片经 `updateShape` 立即改端/中段，风扇 BE 的总结构缓存有最长约 20 tick 的刷新窗口。目标 JAR 中没有整台机器拆解事件、壳体 owner 解绑或把残留壳体批量重置成未成型网格的流程。

注意：结构扫描器有 `EngineStructure.markDirtyAround` 方法（`EngineStructure.java:84-103,156-189`），可找附近相符朝向的风扇标 dirty；在反编译的目标类中未发现调用该方法的上游引用。故报告只把明确实现的 20-tick 定时扫描和壳板局部 `updateShape` 当作有证据的路径，不把“方块改变后必定即时标记风扇 dirty”写成事实。

### 6. Ponder 教程的搭建顺序

Ponder plugin 注册 `engine/basic_turbofan_engine` storyboard，并绑定 `basicTurbofanEngine`（`AeroEngineeringPonderPlugin.java` 注册段与 `AeroEngineeringPonderScenes.java:634-685`）。教程把五个核心按 intake 到 exhaust 逐个亮出，提示 fan 在入口、compressor 紧邻其后、combustor 在中间、turbine 在其后、exhaust cone 收尾；随后以“顺序与方向正确”为运行检查条件。后续才演示管路和传动，再演示机壳和喷口环；对应 JAR 中也随附 `assets/aeroengineering/ponder/engine/basic_turbofan_engine.nbt` 场景结构。故其教程重点是“先让核心顺序可读，再加可选包壳和外围系统”，不是把数百块外壳散点清单当作主要搭建指引。

## 和当前 Create Nuclear Industry 汽轮机的差别

- **Aero 是线性机器核心 + 可选环壳。** 方向由 intake 风扇向 tail 固定；核心紧邻排列。环壳是八个截面位置的局部视觉件，形成结果由风扇 BE 轮询缓存；核心不需要外壳才能运行。
- **本项目是 controller 检查的完整腔体设备。** 当前实现让控制器作为唯一库存/账本 owner，检查配置尺寸、壳格、内部空气、转子与轴/端口；成型后把组件块状态改为 `formed` 并使用相应渲染/碰撞。`TurbinePartBlock.java:49-101,168-180`、`TurbineControllerBlockEntity.java:199-235`、`TurbineStructure.java:87-143,146-169,201-234` 可核对实际布局扫描、存活检查、owner claim/invalidation 路径。本调查不建议放弃已有蒸汽流体事务、双轴动力或 controller owner 合同。
- **局部放置反馈不同。** 当前汽轮机未成型外壳模型是顶部水平板（`tools/art-assets/turbine_models.py:926-929`，`turbine_casing_unformed.obj` 顶点 y=0.8125..1），但它的选取/碰撞形状是底部水平薄板（`TurbinePartBlock.java:132-137`），二者刻意分离；尚未按核心邻接自动给出轴/八向角及 segment。Aero 的 helper 与 state 至少使每片在放置时获得轴心、角位、端/中段显示。Aero 网格很薄但方块碰撞仍整格，本项目薄壳几何/占格/碰撞合同比它更严格，不应把 Aero 的完整方块碰撞照搬。
- **视觉切换边界不同。** Aero 不进行全机形成/拆除的网格重建；仅相邻壳块 segment 局部变化。当前汽轮机在扫描完成时成套切换形成状态和部件模型。不可把 Aero 误解为“先摆散板再压缩成整机控制器”。

## 调查结论与建议（决策未获批准）

**调查结论：** AeroEngine-1.2.4 的可取学习点是核心次序清楚、朝向一眼可读、壳片放置时能借助邻接自动定位、壳片能依相邻方向显示端段/中段，以及 Ponder 把搭建分成核心、包壳、外围连接三步。它没有蒸汽机那种主体结构的 formed 变换，也没有整机 controller/owner 绑定。它实际发动机含进气、燃烧、压气机、排气喷口和推力/推进计算；这些气动燃烧行为不属于本项目蒸汽汽轮机。

**最小建议供项目经理评估：** 优先把汽轮机玩家流程从“大量逐件找位置”改为有序的现场搭建说明/短 Ponder：按已批准几何给出两端轴与转子列、壳体截面、控制器/进排汽口的步骤和方向图；先明确控制器定位作为结构扫描基准，再提示成功后整机显示变化。若仍觉得壳板定位太繁琐，可单独讨论借鉴 Create `IPlacementHelper`：对着转子/控制器时自动预览并放到合法壳格、自动写入朝向；这会改变放置行为，需 PM 与用户确认后另立任务。不要照抄 Aero 推进/燃烧逻辑，也不建议直接复制 Aero 的整格碰撞。

**待 PM 与用户确认：** 以上放置辅助、controller 提示/显示或搭建教程是否属于本批/后续范围。此调查不构成玩法批准。





