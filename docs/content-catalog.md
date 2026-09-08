# 《机械动力：核工业》内容注册与素材清单

**适用版本：** Java 21 + Minecraft 1.21.1 + NeoForge + Create 6.0.10  
**玩法规则：** [project.md](./project.md)  
**材料与配方：** [recipes.md](./recipes.md)  
**实施顺序：** [implementation-roadmap.md](./implementation-roadmap.md)

本文是策划案与代码注册之间的单一内容索引，记录需要新增的流体、物品、方块和多方块结构。注册 ID 一旦进入可游玩版本就视为存档兼容接口，后续只调整显示名、纹理、模型和数值，不随意改 ID。

## 1. 状态与命名规则

| 标记 | 含义 |
| :--- | :--- |
| `已有` | 当前样例已经注册，必须保持兼容 |
| `P1` | 首发内容目标；当前 P1 实施只先完成固定实验堆核心子集 |
| `P2` | 结构自由度与自动化阶段加入 |
| `P3` | 风险、污染和长期玩法阶段加入 |
| `暂缓` | 当前生产链不需要，不创建空内容 |

- 命名空间固定为 `create_nuclear_industry`。
- ID 使用小写下划线，例如 `lead_ore`、`compound_coolant`。
- 方块物品默认与方块共用 ID；表中不重复列出对应 `BlockItem`。
- 普通物品纹理使用 `assets/create_nuclear_industry/textures/item/<id>.png`。
- 方块纹理使用 `assets/create_nuclear_industry/textures/block/<id>.png`，模型和方块状态使用同名 JSON。
- 流体至少提供静止、流动纹理和桶/容器策略；危险蒸汽不提供生存模式手持桶。
- 材料标签按 [recipes.md 的 NeoForge 通用标签目录](./recipes.md#23-neoforge-通用标签目录)生成。

当前 P1 核心实施子集只包括：固定 `5×5×5` 实验反应堆、反应堆所需燃料/冷却剂状态、控制棒拖动滑块、燃料列降频与直接相邻超频、逐列热负荷损伤、可配置的损伤产热增益与更快增长的燃耗代价、控制棒列卡死、四向失效传播与 20% 融毁倒计时、燃料列关键合成物品维修、停机重组成型重置、SCRAM 和冷却剂冷/热状态转化。损伤倍率的新默认值与配置尚待 [P1-BALANCE-02A/02B](./superpowers/plans/2026-09-08-damage-heat-burn-balance-plan.md) 实施，不新增物品或方块 ID。锅炉、汽轮机、完整燃料生产线、烈焰人管理员、可变结构和乏燃料封存虽可保留首发内容设计，但不得提前按当前 P1 任务实现。

## 2. 流体

| 阶段 | 注册 ID | 中文名 | 来源/去向 | 容器策略与关键行为 |
| :--- | :--- | :--- | :--- | :--- |
| `P1` | `compound_coolant` | 复合冷却剂 | 搅拌制造 → 反应堆冷端 | 提供桶；低温一回路工质 |
| `P1` | `hot_compound_coolant` | 热复合冷却剂 | 反应堆热端 → 核换热器 | 不允许普通桶徒手取用；换热后回到 `compound_coolant` |
| `P1` | `uranium_slurry` | 铀料浆 | 铀精矿混合 → 富集离心机 | 提供密封容器或桶；不得直接变成普通铀材料 |
| `P1` | `supercritical_steam` | 超临界蒸汽 | 专用高压锅炉 → 超临界汽轮机或核换热器蒸汽供热模式 | 无生存桶；仅二级耐压管和机器能力可输送 |
| `P1` | `steam` | 蒸汽 | 超临界汽轮机排汽或换热器蒸汽供热输出 → 换热器冷凝模式/旁通口 | 唯一普通蒸汽流体；“乏蒸汽”不另注册 ID；不能输入 Create 原版蒸汽引擎 |
| `P3` | `contaminated_water` | 受污染废水 | 紧急注水/去污 → 封存或按事故规则处理 | 不可直接倒入世界清除；服务器配置决定事故泄漏行为 |

以下内容明确不注册为新流体：

- 冷凝回水使用 `minecraft:water`，核换热器通过流量账本控制回收率，不创建重复的“冷凝水”流体。
- Create 流体储罐锅炉内部的“蒸汽”是原版锅炉状态，不是可抽取流体。本模组只通过 `BoilerHeater` 提供热级。
- 燃料燃耗、热量和辐射不是流体，也不以伪流体保存。

## 3. 物品

### 3.1 三种矿物与基础形态

| 阶段 | 材料 | 注册 ID | 用途/标签 |
| :--- | :--- | :--- | :--- |
| `P1` | 铅 | `raw_lead`、`lead_ingot`、`lead_nugget`、`lead_dust`、`lead_plate`、`lead_rod` | 粗矿、锭、粒、粉、板、杆使用对应 `c:*` 标签 |
| `P1` | 锡 | `raw_tin`、`tin_ingot`、`tin_nugget`、`tin_dust`、`tin_plate`、`tin_wire`（显示名：锡条） | 粗矿、锭、粒、粉、板、线使用对应 `c:*` 标签 |
| `P1` | 铀 | `raw_uranium`、`uranium_concentrate`、`uranium_tailings` | 粗矿使用通用标签；精矿和尾矿使用本模组专用标签 |
| `暂缓` | 铀金属形态 | `uranium_ingot`、`uranium_nugget`、`uranium_plate`、`uranium_rod` | 当前燃料链不使用，不为了与铅锡对称而创建；未来出现真实配方后再注册 |

铀加工品不能笼统加入 `c:dusts/uranium`：`uranium_concentrate`、低浓缩铀、贫化铀和再生燃料具有不同安全语义，必须保持独立 ID。

三种粉碎粗矿不由本模组重复注册。铅、锡、铀加工链分别使用 Create 6.0.10 自带的
`create:crushed_raw_lead`、`create:crushed_raw_tin` 和 `create:crushed_raw_uranium`。

### 3.2 通用工业材料与零件

| 阶段 | 注册 ID | 中文名 | 主要用途 |
| :--- | :--- | :--- | :--- |
| `P1` | `steel_ingot`、`steel_plate`、`steel_rod` | 合金钢锭/板/杆 | 机器、管道和结构基础；兼容 `c:* /steel` 标签 |
| `P1` | `reinforced_steel_plate` | 强化合金钢板 | 高压锅炉、汽轮机和高温设备 |
| `P1` | `solder_ingot` | 锡合金焊料 | 密封、仪表和燃料棒封端 |
| `P1` | `steel_mesh`、`steel_grate`、`steel_frame` | 合金钢网/格架/框架 | 燃料组件、热室网格和多方块骨架 |
| `P1` | `steel_pipe_blank` | 合金钢管坯 | 耐压管段和燃料包壳 |
| `P1` | `seal_ring`、`pressure_fitting` | 密封环、耐压接头 | 流体与蒸汽接口 |
| `P1` | `industrial_sensor`、`radiation_sensor` | 工业传感器、辐射传感器 | 仪表、联锁和危险检测 |
| `P1` | `maintenance_seal` | 维护密封件 | 停机维修泄漏部件 |
| `P1` | `heavy_bearing` | 重型轴承 | 汽轮机、泵和离心机 |
| `P1` | `coal_dust`、`charcoal_dust`、`quartz_dust`、`glass_dust` | 煤粉、木炭粉、石英粉、玻璃碎料 | 钢材、陶瓷和灌封材料 |
| `P2` | `obsidian_dust` | 黑曜石粉 | 高级耐热材料 |
| `P1` | `industrial_ceramic`、`refractory_brick`、`insulation_plate` | 工业陶瓷、耐火砖、隔热板 | 锅炉和高温结构 |
| `P1` | `neutron_absorbing_ceramic` | 中子吸收陶瓷 | 控制棒 |
| `P1` | `heat_resistant_glass`、`shielded_glass` | 耐热玻璃、铅屏蔽玻璃 | 观察窗与热室 |
| `P3` | `vitrification_medium` | 玻璃固化基材 | 放射性废物灌封 |

复合冷却剂直接使用原版下界资源 `minecraft:glowstone_dust`（中文名“荧石粉”）。本模组不新增荧石矿物或荧石粉，也不为原版荧石复制注册项和素材。

合金钢板的正式注册 ID 冻结为 `steel_plate`，显示名为“合金钢板”，并加入对应的 `c:plates/steel` 通用标签。`alloy_steel_plate` 是早期需求记录中使用过的描述性旧名，不注册为物品、别名或兼容转发 ID；燃料列和控制棒列维修统一消耗 `steel_plate`。

### 3.3 燃料、乏燃料与废物

| 阶段 | 注册 ID | 中文名 |
| :--- | :--- | :--- |
| `P1` | `low_enriched_uranium_dust`、`depleted_uranium_dust` | 低浓缩铀粉、贫化铀粉 |
| `P1` | `green_fuel_pellet`、`sintered_fuel_pellet` | 生燃料芯块、烧结燃料芯块 |
| `P1` | `fuel_cladding_tube` | 燃料包壳管 |
| `P1` | `fresh_fuel_rod`、`fresh_fuel_assembly` | 新燃料棒、浓缩铀燃料组件 |
| `P1` | `cooled_spent_fuel_assembly` | 枯竭铀燃料组件；首发燃料耗尽直接输出冷却态 |
| `暂缓` | `hot_spent_fuel_assembly` | 未来命名预留；首发不注册、不生成、不作为运行输入 |
| `P1` | `encapsulated_spent_fuel`、`sealed_spent_fuel_cask` | 灌封乏燃料件、已封装乏燃料桶 |
| `P3` | `spent_fuel_rod`、`contaminated_cladding`、`contaminated_grid` | 乏燃料棒、受污染包壳、受污染格架 |
| `P3` | `reprocessed_fuel_dust`、`reprocessed_fuel_blend` | 再生燃料粉末、再生燃料混合粉 |
| `P3` | `green_reprocessed_fuel_pellet`、`reprocessed_fuel_pellet` | 再生燃料生芯块、再生燃料芯块 |
| `P3` | `reprocessed_fuel_rod`、`reprocessed_fuel_assembly` | 再生燃料棒、再生燃料组件 |
| `P3` | `high_level_waste`、`vitrified_high_level_waste`、`sealed_high_level_waste_cask` | 高放残渣、高放玻璃固化体、已封装高放废物桶 |
| `P3` | `recovered_alloy_scrap` | 回收合金碎料 |

燃料组件的剩余燃料值由唯一的 `remainingFuelFraction` 耐久字段表示，并遵循 Minecraft ItemStack 耐久语义；自定义 `fuel_assembly` 数据组件还保留燃料身份、预留 `decayHeatHu` 回收资格标识和 format，但不保存温度或完整度。每个燃料列一个组件，其实体 `ItemStack` 由对应 `reactor_refueling_port` 方块实体唯一持久化；仪表端口只保存燃耗小数余量和热工模拟状态，不保存第二份组件。燃料列有效高度决定基础耐久消耗速度；1 格高、满功率、不超频的基准寿命为 3 小时，基础燃耗通过配置项控制。反应堆运行温度、燃料列完整度和控制棒列完整度保存在多方块结构模拟状态，不为每个燃耗阶段创建新的注册 ID。只有处理规则和安全等级发生质变时才使用不同物品 ID。

受损燃料列的维修使用燃料棒制造链中的关键合成物品；该物品沿用既有燃料制造链的注册身份，不在尚未评审具体配方前擅自新增维修物品 ID。维修物品只改变目标燃料列完整度，不补充组件耐久、不解锁卡死控制棒，也不回退融毁进度。

实施优先级上，`fresh_fuel_assembly`（玩家名称“浓缩铀燃料组件”）、`cooled_spent_fuel_assembly`（玩家名称“枯竭铀燃料组件”）及其数据组件属于当前 P1 核心与后续燃料扩展共用的 **G1 合同**；`hot_spent_fuel_assembly` 只保留命名预留，首发不注册、不生成、不作为运行输入。燃料耗尽直接得到冷却乏燃料，热/冷转换与乏燃料池玩法后置。上述首发身份必须先于换料端口和完整铀生产线冻结。可以先让首发身份通过创造模式或开发测试获得，以解除运行与换料开发阻塞；这不代表后续生存生产线已经完成，也不得据此通过生存扩展验收。

G1/G2 前置注册与占位素材合同已经定义；当前临时 PNG 来源固定为：煤炭→浓缩铀燃料组件、木炭→枯竭铀燃料组件、熔岩→热复合冷却剂、水→复合冷却剂。这里仅指纹理/图标占位；四种原版 ID 不得作为本模组运行时输入、存档身份或自动化绕过。实际注册与实现状态以[实施路线图](./implementation-roadmap.md)和[P1 Agent/开发者交接计划](./superpowers/plans/2026-08-18-agent-developer-execution-plan.md)为准。

### 3.4 工具、防护与设备物品

| 阶段 | 注册 ID | 中文名 | 备注 |
| :--- | :--- | :--- | :--- |
| `P1` | `control_rod` | 控制棒组件 | 仅作为 `control_rod_drive` 的装配材料；不可单独放置，不注册同名方块 |
| `P1` | `dosimeter` | 手持辐射计 | 显示环境与物品辐射 |
| `P1` | `lead_lined_helmet`、`lead_lined_chestplate`、`lead_lined_leggings`、`lead_lined_boots` | 基础防护服四件套 | 不合并成一个不可穿戴物品 |
| `P1` | `lead_shielding_cask` | 铅屏蔽桶 | 空容器；装填后变为对应封装物品 |

## 4. 方块

### 4.1 矿石与储存方块

| 阶段 | 材料 | 注册 ID | 备注 |
| :--- | :--- | :--- | :--- |
| `P1` | 铅 | `lead_ore`、`deepslate_lead_ore`、`raw_lead_block`、`lead_block` | — |
| `P1` | 锡 | `tin_ore`、`deepslate_tin_ore`、`raw_tin_block`、`tin_block` | — |
| `P1` | 铀 | `uranium_ore`、`deepslate_uranium_ore`、`raw_uranium_block` | — |
| `P1` | 尾矿与屏蔽 | `uranium_tailings_brick`、`shielded_tailings_block`、`depleted_uranium_weight_block` | — |
| `P1` | 工业材料 | `steel_block`、`shielding_concrete` | — |
| `暂缓` | 铀金属储存 | `uranium_block` | 当前生产链没有铀锭，不注册 |

每种矿石必须同时具有掉落表、镐挖掘标签、正确工具等级、普通石/深层板岩纹理、世界生成配置与数据包关闭入口。

### 4.2 结构件、接口与管网

| 阶段 | 注册 ID | 中文名/职责 |
| :--- | :--- | :--- |
| `已有` | `experimental_reactor_casing` | 样例实验反应堆外壳；保留 ID，后续可升级纹理或作为 P1 外壳原型 |
| `P1` | `reactor_casing`、`reactor_window` | 反应堆外壳、观察窗；外壳同时是棱边、转角、顶面和底面的唯一基础结构方块 |
| `P1` | `reactor_cold_port`、`reactor_hot_port` | 一回路冷端、热端接口 |
| `P1` | `reactor_instrument_port` | 反应堆模拟状态锚点、工程师护目镜全堆信息接口和红石 SCRAM 输入端；保存热工、控制、冷却剂与事故状态，不保存燃料组件实体物品；护目镜显示结构尺寸、列/端口计数、冷/热缓冲总容量、冷/热当前库存、全堆发热量及冷却剂转化速率，不枚举逐列详情；所有玩家可用 Create 扳手普通右键请求服务端重扫，并在快捷栏获得本地化的成型结果或失败原因 |
| `P2` | `reactor_control_port` | 反应堆控制端口；随烈焰人管理员提供可选自动控制桥接，不拥有反应堆状态 |
| `P1` | `reactor_fuel_rod` | 燃料列内部的连续燃料柱方块；完整燃料列顶面必须配对 `reactor_refueling_port` |
| `P1` | `reactor_refueling_port` | 单列换料端口；只能安装在燃料列正上方的顶面，方块实体唯一保存该列一个完整燃料组件 `ItemStack`，无 GUI，供人工或 Create 动力机械臂逐列取放；未成型时仅允许玩家安全取出本地组件，不允许装入或自动化；工程师护目镜指向它时显示端口内部组件的剩余/最大耐久与比例，以及下方燃料列的完整度/损坏度和当前发热量 |
| `P1` | `control_rod_drive` | 控制棒驱动器；唯一可放置的控制棒相关方块，方块实体渲染无碰撞箱模型并保存逐棒控制状态；不接受红石输入，所有玩家都可用拖动滑块调节，拖动采用客户端本地预览且只在最终提交/拒绝时接受服务端校正；工程师护目镜显示其绑定控制棒列的行列编号与完整度；管理员桥接后置 |
| `P2` | `blaze_reactor_manager` | 烈焰人反应堆管理台 |
| `P1` | `pressure_pipe_tier_1`、`pressure_valve_tier_1` | 一级耐压管和控制阀 |
| `P1` | `supercritical_steam_valve`、`turbine_exhaust_port` | 超临界蒸汽阀、排汽接口 |
| `P2` | `pressure_pipe_tier_2` | 二级强化管 |
| `P3` | `pressure_pipe_tier_3` | 三级陶瓷内衬管 |
| `P1` | `main_coolant_pump`、`feedwater_pump` | 主冷却泵、给水泵 |
| `P1` | `steam_bypass_vent` | 超临界汽轮机安全旁通排汽口 |

### 4.3 单方块机器与储存设备

| 阶段 | 注册 ID | 中文名 | 输入/输出 | 运行职责或功能 |
| :--- | :--- | :--- | :--- | :--- |
| `P1` | `enrichment_centrifuge` | 富集离心机 | 铀料浆 → 低浓缩铀粉 + 贫化铀粉 + 工艺水 | 消耗稳定 Create 转速和应力完成铀富集；转速不稳降低效率 |
| `P1` | `fuel_sintering_furnace` | 燃料烧结炉 | 生燃料芯块 → 烧结燃料芯块 | 在密闭高温下烧结芯块；普通鼓风加热不能替代 |
| `P1` | `shielded_assembly_station` | 屏蔽装配台 | 芯块/包壳/格架或危险废物/容器 → 燃料棒、组件或封装件 | 隔离放射性物品并执行屏蔽装配，支持专用机械臂自动化 |
| `P1` | `nuclear_heat_exchanger` | 换热器 | 热复合冷却剂→复合冷却剂；超临界蒸汽→蒸汽；蒸汽+冷源→水 | 按输入选择唯一模式；核热/蒸汽供热默认返回 18/9 锅炉热值；冷凝缺少冷源时安全停机 |
| `暂缓` | `spent_fuel_pool_port` | 乏燃料池控制/流体端口 | 水、循环能力和热乏燃料 → 冷却状态 | 后置玩法；首发不实现热/冷转换、乏燃料池冷却或相关流体路线 |
| `P1` | `dry_storage_rack` | 干式贮存架 | 已封装乏燃料桶 → 安全贮存状态 | 检查封装完整性并提供稳定堆放，不消除辐射物质 |
| `P3` | `shielded_disassembler` | 屏蔽拆解机 | 冷却乏燃料组件 → 乏燃料棒 + 受污染格架 | 在屏蔽环境拆解乏燃料，禁止普通机械手直接处理 |
| `P3` | `sealed_reprocessor` | 密闭再处理器 | 乏燃料棒 + 处理介质 → 再生燃料粉末 + 高放残渣 + 受污染包壳 | 回收部分燃料价值，并保证同步产生不可消除的高放废物 |
| `P3` | `shielded_manipulator` | 屏蔽机械臂 | 危险物品搬运 | 在热室和屏蔽机器之间自动转移高辐射物品，不执行加工配方 |

这些机器是否最终使用单方块外形或小型多方块外壳，在不改变注册 ID 和输入输出职责的前提下可以调整。拥有库存、流体、应力或状态的设备必须使用方块实体；纯结构方块不创建无意义方块实体。

`control_rod` 是制造驱动器的不可放置组件物品，不是控制棒柱方块。世界中只注册 `control_rod_drive`；其下方内部有效高度全为空气，控制棒由驱动器方块实体渲染为无碰撞箱模型动画，不在空列中放置逐段控制棒方块。`reactor_refueling_port` 的方块实体是其下方单列燃料组件 `ItemStack` 的唯一持久化库存；仪表端口只在正式 tick 中读取临时燃料投影，不复制库存。有效结构中只有所有对该列生效的相邻控制棒均完全插入且该列裂变产热为零，或该列燃料耗尽而被动停机时，端口才允许正常换料。未成型时仅允许玩家空手安全取回端口本地组件，不允许装入、动力机械臂或通用物流访问；危险状态解绑留下的持久化安全锁必须继续拒绝取料。端口拒绝普通漏斗和通用物品管道；详细状态只通过工程师护目镜浮窗显示，其中耐久直接读取端口内部物品，列完整度/损坏度和当前 `HU/t` 发热量读取服务端结构缓存。结构规则从本 ID 加入起直接采用严格配对：完整燃料列顶面必须是换料端口，旧规则中的普通外壳顶盖不再合法；旧仪表快照中的组件必须迁移到空端口且不得复制。

### 4.4 组成多方块结构的可放置方块

本节列出玩家能够拿在手中、逐块放进世界、需要分别注册模型和纹理的组成方块。它们本身通常不能独立运行：外壳负责封闭结构，端口负责连接管道或物流，控制器负责扫描并组装结构。第 5 节则描述这些方块组装成功后形成的整台逻辑机器；两节不是重复注册两套内容。

| 阶段 | 所属结构 | 可放置组成方块（注册 ID） | 各类部件作用 |
| :--- | :--- | :--- | :--- |
| `P1` | 专用高压锅炉 | `high_pressure_boiler_casing`、`high_pressure_boiler_window`、`high_pressure_boiler_water_port`、`high_pressure_boiler_steam_port`、`boiler_safety_valve`、`boiler_blaze_heater_port`、`boiler_heat_exchange_section`、`high_pressure_boiler_controller` | 外壳/观察窗封闭锅炉；水口输入给水；蒸汽口输出超临界蒸汽；安全阀泄压；辅助加热口连接烈焰人燃烧室；换热段接收核热；控制器组装并保存锅炉状态 |
| `P1` | 超临界汽轮机 | `turbine_casing`、`turbine_window`、`turbine_rotor`、`turbine_inlet`、`turbine_exhaust`、`turbine_output_shaft`、`turbine_governor`、`turbine_brake`、`turbine_controller` | 外壳/观察窗封闭轮机；转子形成连续主轴；进汽口接收超临界蒸汽；排汽口输出 `steam`；输出轴注册 SU；调速器控制进汽；制动器抑制超速 |
| `P3` | 屏蔽热室 | `hot_cell_casing`、`hot_cell_window`、`hot_cell_item_port`、`hot_cell_fluid_port`、`hot_cell_controller` | 外壳/观察窗提供辐射屏蔽；物品口和流体口限定危险物流；控制器检查屏蔽完整性并允许内部机械臂工作 |

## 5. 多方块结构

| 阶段 | 结构 ID | 中文结构名 | 组成与规模 | 组装条件 | 运行职责 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `P1` | `experimental_reactor` | 实验反应堆 | 固定 `5×5×5`；外壳、观察窗、燃料列、控制棒驱动器列、逐列换料端口、冷/热端口和仪表端口 | 完整长方体、冷/热端口和唯一仪表端口齐全、燃料与控制棒布局合法；控制端口不参与 P1 成型；燃料列顶面使用换料端口，控制棒驱动器下方全为空气 | 产热、燃耗、余热、逐列完整度与热负荷、控制棒列卡死、四向损伤传播、20% 融毁计时、SCRAM、冷却剂转化、关键合成物品维修与停机重置 |
| `P1` | `high_pressure_boiler` | 专用高压锅炉 | 首版固定尺寸；水接口、换热段、蒸汽接口和安全阀 | 密闭结构、至少一个合格核换热段 | 将一回路热量转成超临界蒸汽 |
| `P1` | `supercritical_steam_turbine` | 超临界汽轮机 | 首版固定尺寸；进汽段、转子、轴承、排汽段、主轴端 | 转子连续、端部与外壳完整 | 消耗超临界蒸汽并输出 SU、产生普通 `steam` |
| `暂缓` | `spent_fuel_pool` | 乏燃料冷却池 | 水池内衬、循环端口、储存格 | 后置玩法；首发不实现热/冷转换或乏燃料池冷却 |
| `P2` | `variable_reactor` | 可变尺寸反应堆 | 5×5×5 至 11×11×15 | 从固定实验堆扩展；有效燃料长度受限 | 结构自由度和规模化产热 |
| `P2` | `segmented_supercritical_steam_turbine` | 分段式超临界汽轮机 | 可变长度高压/低压段和多个主轴端 | 每段类型、转子和轴承合法 | 可变流量与多端口应力分配 |
| `P3` | `shielded_hot_cell` | 屏蔽热室 | 屏蔽外壳、观察窗、机械臂、物品/流体端口 | 屏蔽完整，危险物料不能从非端口穿过 | 高放废物灌封与处理 |

Create 流体储罐锅炉和其他原生受热设备不是本模组多方块。`nuclear_heat_exchanger` 对锅炉注册 `BoilerHeater`：核热模式默认热值 18，蒸汽供热模式默认热值 9；盆式加工分别暴露超级加热与普通加热。多个换热器可累加锅炉数值热量，但普通热源品质绝不能通过数量提升为核级。

所有本模组多方块结构使用稳定结构 ID 和结构局部坐标保存状态，不把世界绝对坐标写入纯模拟核心。结构只在组装、拆除或端口变化时重扫，正常 tick 不逐方块扫描。

## 6. 第一批素材清单

第一批只服务 P1.1 的矿物与材料注册，按以下顺序绘制：

1. **6 张矿石方块纹理：** `lead_ore`、`deepslate_lead_ore`、`tin_ore`、`deepslate_tin_ore`、`uranium_ore`、`deepslate_uranium_ore`。
2. **3 张粗矿物品纹理：** `raw_lead`、`raw_tin`、`raw_uranium`。
3. **3 张粗矿块纹理：** `raw_lead_block`、`raw_tin_block`、`raw_uranium_block`。
4. **2 张锭纹理：** `lead_ingot`、`tin_ingot`。
5. **1 张铀精矿纹理：** `uranium_concentrate`，替代当前没有用途的“铀锭”素材。
6. **2 张金属储存块纹理：** `lead_block`、`tin_block`。

第一批合计 **17 张本模组基础纹理**。三种粉碎粗矿直接使用 Create 自带物品及素材，不计入本模组绘制清单。矿石、粗矿块和金属块先使用原版 `cube_all` 模型，物品先使用 `item/generated`；此阶段不需要 Blockbench。每张纹理采用 16×16 像素、禁用抗锯齿、保持原版/Create 的清晰像素边缘。

第二批再绘制粒、粉、板、线、钢材和机器零件；第三批才进入反应堆、锅炉、超临界汽轮机与换热器结构纹理和 Blockbench/Flywheel 动画，避免首批素材范围失控。项目当前明确采用功能优先顺序：第三批结构视觉资源暂不启动；多方块连接纹理、正式多边形模型、动态渲染、粒子和音效均等待对应功能闭环完成后再单独立项。现有占位纹理和基础方块模型只需保持路径有效、对象可区分和无紫黑缺失材质。

## 7. 每项内容完成定义

一个注册项只有同时满足以下条件才算完成：

- Java 注册、中文/英文翻译、创造模式标签页分类齐全；
- 物品模型或方块状态/模型/纹理引用有效；
- 方块具有掉落表、挖掘标签和正确工具等级；
- 通用材料进入正确 `c:*` 标签，危险材料只进入专用标签；
- 配方或明确的游戏内来源存在，不产生无法获得的孤立物品；
- 方块实体状态可保存、重载且只同步客户端需要的数据；
- 需要护目镜观察的端口信息必须有服务端权威来源、客户端同步路径和无效结构/空列的明确显示；仪表端口静态摘要复用结构缓存与服务器配置，不新增重复 NBT，也不因观察触发整堆扫描；
- 数据生成和资源契约测试能发现遗漏；
- 已进入可游玩版本的注册 ID 不再改名。
## 8. 当前状态与文档入口

首发燃料组件身份、持久化 `fuel_assembly` 数据组件、冷却剂身份和确定性占位素材合同已经定义，但当前代码基线尚未完成这些运行接入；这不等于 G1/G2 运行接入或后续生存生产线完成。首发耗尽终态直接使用 `COOLED_SPENT`；`HOT_SPENT` 仅为未来热态路线保留命名，首发不注册、不生成、不依赖。煤炭、木炭、熔岩和水只作为占位纹理来源，不能作为配方输入、燃料别名、流体能力或运行时存档身份。

当前执行顺序见 [实施路线图](./implementation-roadmap.md)，活动计划见 `docs/superpowers/plans/`，已验收计划见 [文档归档索引](./archive/README.md)。

## 9. 需求变更与注册评审

本清单由项目经理维护，是注册 ID、内容阶段和素材责任的唯一索引。用户提出的新方块、物品、流体或结构需求必须先经过玩法用途、存档身份、输入输出、资源成本和阶段依赖评审，再写入本清单；开发者不得先注册一个临时 ID 再要求设计追认。

- 已进入可游玩或开发合同的注册 ID 视为兼容接口；需求变更优先调整显示名、纹理、模型和数值，不直接改 ID。
- 新内容必须同时声明阶段、来源/去向、危险等级、容器策略、自动化入口和完成定义；没有明确玩法用途的空壳内容暂缓注册。
- 项目经理只维护清单与任务计划，不实现注册代码。执行者需依据 [P1 Agent/开发者交接计划](./superpowers/plans/2026-08-18-agent-developer-execution-plan.md) 提交注册、资源契约和游戏内证据。
- 当前文档定义的燃料组件与冷却剂只是 G1/G2 前置身份，代码基线尚未完成对应运行接入；即使未来完成注册，也不代表完整燃料生产线、运行热端或后续生存扩展已经完成。

## 10. Create Ponder（“思索”）覆盖合同

Ponder 是所有可玩内容的第一入口，不是开发者调试工具。每个已注册且对玩家有主动交互、结构搭建、物流连接、运行控制或安全意义的 ID 都必须有独立 Ponder 入口；同一套场景可以被多个语义相同的 ID 复用，但创造栏/JEI 中不能出现无说明的可玩内容。当前 P1 核心内容随固定实验堆切片完成，P2/P3 内容随对应扩展阶段交付，不能用尚未实现的机器或配方制作假演示。

### 10.1 多方块结构

| 阶段 | 结构 ID | Ponder 必须演示的内容 |
| :--- | :--- | :--- |
| `P1` | `experimental_reactor` | 固定 5×5×5 外壳与观察窗、仪表端口唯一性、冷/热端口和多端口汇总、燃料列与换料端口、控制棒驱动器、所有玩家拖动滑块、启动与正常运行、红石高电平保持 SCRAM/低电平恢复停堆前位置、冷却剂转化、逐列热负荷损伤、可配置的损伤产热增益与更快增长的燃耗代价、控制棒列卡死、当前热量四向传播触达 20% 后融毁计时、SCRAM/冷却暂停但不回退、合金钢板维修、停机重组成型清空 NBT、运行中拆件喷出热冷却剂/高放废物、工程师护目镜查看仪表静态摘要与换料端口列级信息、错误结构拒绝 |
| `P2` | `high_pressure_boiler` | 密闭结构、水口、换热段、烈焰人辅助热源、超临界蒸汽出口、安全阀泄压与缺水/出口阻塞的安全停机 |
| `P2` | `supercritical_steam_turbine` | 外壳、连续转子、进汽/排汽、主轴输出、调速器、制动器、超速风险和蒸汽回路连接 |
| `暂缓` | `spent_fuel_pool` | 水池内衬、循环端口、储存格和后置状态；首发不实现热/冷转换 |
| `P2` | `variable_reactor` | 尺寸边界、燃料有效高度、控制棒邻接、局部反馈和多端口布置 |
| `P2` | `segmented_supercritical_steam_turbine` | 高压/低压段、分段转子、轴承连续性、多个主轴端和流量分配 |
| `P3` | `shielded_hot_cell` | 屏蔽完整性、观察窗、物品/流体端口、屏蔽机械臂和危险物流边界 |

### 10.2 单方块设备

| 阶段 | 注册 ID | Ponder 必须演示的内容 |
| :--- | :--- | :--- |
| `P2` | `enrichment_centrifuge` | 铀料浆输入、稳定转速/应力、低浓缩铀粉与贫化铀粉/工艺水输出、失稳时的效率变化 |
| `P2` | `fuel_sintering_furnace` | 生燃料芯块输入、密闭高温烧结、烧结燃料芯块输出，说明普通鼓风加热不可替代 |
| `P2` | `shielded_assembly_station` | 芯块/包壳/格架装配、屏蔽边界、燃料棒/燃料组件和危险物品装配限制 |
| `P2` | `nuclear_heat_exchanger` | 核热供热默认值 18、超临界蒸汽降级供热默认值 9、普通蒸汽冷凝三种互斥模式、冷源与堵塞安全停机；数值可由配置覆盖 |
| `暂缓` | `spent_fuel_pool_port` | 乏燃料池控制/流体接口和后置状态，不展示首发不存在的冷却转换 |
| `P2` | `dry_storage_rack` | 已封装乏燃料桶的装架、完整性检查和“贮存不等于消除辐射” |
| `P2` | `blaze_reactor_manager` | 烈焰人燃烧室安装、仪表绑定、逐棒调节、红色阈值向 `reactor_instrument_port` 输出 SCRAM 信号 |
| `P3` | `shielded_disassembler` | 屏蔽环境、冷却乏燃料组件输入、乏燃料棒/受污染格架输出及普通机械手拒绝 |
| `P3` | `sealed_reprocessor` | 处理介质、乏燃料输入、再生燃料与高放废物/受污染包壳同步产出 |
| `P3` | `shielded_manipulator` | 热室内危险物品搬运、允许端口、屏蔽边界和不执行加工配方 |
| `P1` | `main_coolant_pump` | 一回路冷却剂输入/输出、流量不足警告、与反应堆冷/热端口的正确连接 |
| `P2` | `feedwater_pump` | 冷凝水输入、锅炉给水输出、缺水保护和与蒸汽闭环的连接 |
| `P2` | `steam_bypass_vent` | 超临界蒸汽旁通、泄压方向、负载丢失时的安全用途和不可替代正常排汽回路 |

### 10.3 管道、阀门和接口

以下每个 ID 都必须有自己的入口；场景可以用同一套管网布置，但必须突出对应等级、流体方向、压力限制或交互职责。

| 阶段 | 注册 ID | Ponder 必须演示的内容 |
| :--- | :--- | :--- |
| `P1` | `pressure_pipe_tier_1` | 一级管的连接、复合冷却剂用途和基础压力边界 |
| `P1` | `pressure_valve_tier_1` | 冷却剂流向、红石/转速调节和关闭阀门后的安全行为 |
| `P2` | `supercritical_steam_valve` | 超临界蒸汽专用流向、压力等级和误接普通管的拒绝/警告 |
| `P2` | `turbine_exhaust_port` | 汽轮机排汽到 `steam` 回路、旁通和冷凝入口的连接 |
| `P2` | `pressure_pipe_tier_2` | 强化管的材料等级、超临界蒸汽连接和与一级管的适用边界 |
| `P3` | `pressure_pipe_tier_3` | 陶瓷内衬管的极限温压用途、损坏风险和禁止低级管替代的提示 |
| `P1` | `reactor_cold_port` | 复合冷却剂冷端输入、流向和反应堆结构必需性；允许多个端口汇总，每端口默认上限 `128 mB/t`，不设全堆流量上限 |
| `P1` | `reactor_hot_port` | 热复合冷却剂输出、冷却剂转化和热端不能徒手桶取；允许多个端口汇总，每端口默认上限 `128 mB/t`，不设全堆流量上限 |
| `P1` | `reactor_instrument_port` | 唯一模拟状态所有者、Create 扳手普通右键成型诊断、工程师护目镜显示尺寸/列数/冷热端口数/冷与热缓冲总容量、红石高电平保持 SCRAM/低电平恢复停堆前位置、无控制棒列时拒绝 SCRAM、不得重复安装；燃料组件实体物品由各换料端口保存 |
| `P2` | `reactor_control_port` | 随烈焰人管理员提供可选自动控制桥接、无状态所有权、不能替代仪表端口 SCRAM |
| `P1` | `reactor_refueling_port` | 只能位于燃料列顶面、唯一保存单列燃料组件、提供停堆/耗尽许可、人工或动力机械臂换料、受危险锁保护的未成型人工取料，以及本地组件耐久与结构列级护目镜信息 |
| `P2` | `high_pressure_boiler_water_port`、`high_pressure_boiler_steam_port` | 锅炉给水/超临界蒸汽方向；两者分别有入口，不能混接 |
| `P2` | `boiler_safety_valve`、`boiler_blaze_heater_port`、`boiler_heat_exchange_section` | 泄压、烈焰人辅助热源和核热输入的差异；组件入口由锅炉场景与独立入口共同覆盖 |
| `P2` | `turbine_inlet`、`turbine_exhaust`、`turbine_output_shaft` | 汽轮机进汽、排汽和主轴输出方向；演示流体接口与 SU 接口不可互换 |
| `P3` | `hot_cell_item_port`、`hot_cell_fluid_port` | 热室物品/流体危险物流的唯一通道；非端口穿越必须拒绝 |

### 10.4 可使用物品

“可使用”限定为主动使用、穿戴、装填或作为端口交互输入；矿石、金属、粉末、板材等纯材料不单独创建 Ponder，但必须在相关配方/设备场景中出现。

| 阶段 | 注册 ID | Ponder 必须演示的内容 |
| :--- | :--- | :--- |
| `P3` | `dosimeter` | 手持读取环境辐射和物品辐射，说明读数范围、危险提示和不替代工程师护目镜状态层 |
| `P3` | `lead_lined_helmet`、`lead_lined_chestplate`、`lead_lined_leggings`、`lead_lined_boots` | 每件装备独立入口，演示穿戴、防护覆盖范围和辐射不是被装备删除而是被减缓 |
| `P2` | `lead_shielding_cask` | 空桶装填、封装物品输出、搬运限制和屏蔽不等于废物消失 |
| `P1` | `fresh_fuel_assembly` | 燃料身份、耐久语义、通过换料端口装入空燃料列和禁止在运行列强行替换 |
| `P1` | `cooled_spent_fuel_assembly` | 耗尽后的冷却乏燃料、取出、封装和干式贮存；不暗示首发存在热/冷转换 |
| `暂缓`（兼容保留） | `hot_spent_fuel_assembly` | 仅在该 ID 可见时说明未来兼容用途，不展示首发运行会生成或依赖它 |
| `P3` | `reprocessed_fuel_assembly` | 再处理燃料组件来源、再次装填限制和与普通浓缩燃料的身份区别 |

`control_rod` 是不可单独放置、仅用于制造 `control_rod_drive` 的组件，不创建脱离驱动器的独立使用场景；它必须在控制棒驱动器 Ponder 中展示。合金钢板不创建纯材料独立 Ponder，但必须在实验反应堆 Ponder 中演示右键换料端口维修燃料列和右键控制棒驱动器维修控制棒列。Create 工程师护目镜属于外部模组物品，不新增本模组注册项，但反应堆仪表端口和换料端口 Ponder 必须演示其观察结果。

### 10.5 通用 Ponder 场景标准

- 每个入口至少包含“用途 → 正确搭建/放置 → 输入输出或交互 → 自动化/红石 → 故障与安全”的连续步骤；纯静态展示不算完成。
- 场景 ID 默认按 `create_nuclear_industry:<registry_id>` 命名；一个 ID 需要多个主题时使用稳定后缀，并在入口清单中记录所有主题。
- 场景只使用已注册、已批准的方块、物品、流体和配方；配方 Ponder 与 `recipes.md` 不一致时以项目经理文档为准并阻止交付。
- Ponder 是客户端教学层，不能读取或修改服务端权威状态；SCRAM、换料许可、燃耗、损伤和辐射仍由服务端逻辑执行。
- 中文和英文标题/步骤必须可本地化；场景中的数值使用配置或“示意值”标记，不把未冻结的平衡数值写死。
- 资源契约检查入口 ID 与场景 ID 一一对应；每个 P1 核心内容至少完成一次客户端人工验收，P2/P3 在各自阶段验收。
