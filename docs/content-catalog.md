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
| `P1` | 第一版可玩垂直切片必需 |
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

## 2. 流体

| 阶段 | 注册 ID | 中文名 | 来源/去向 | 容器策略与关键行为 |
| :--- | :--- | :--- | :--- | :--- |
| `P1` | `compound_coolant` | 复合冷却剂 | 搅拌制造 → 反应堆冷端 | 提供桶；低温一回路工质 |
| `P1` | `hot_compound_coolant` | 热复合冷却剂 | 反应堆热端 → 核换热器 | 不允许普通桶徒手取用；换热后回到 `compound_coolant` |
| `P1` | `uranium_slurry` | 铀料浆 | 铀精矿混合 → 富集离心机 | 提供密封容器或桶；不得直接变成普通铀材料 |
| `P1` | `supercritical_steam` | 超临界蒸汽 | 专用高压锅炉 → 超临界汽轮机或核换热器蒸汽供热模式 | 无生存桶；仅二级耐压管和机器能力可输送 |
| `P1` | `steam` | 蒸汽 | 超临界汽轮机排汽或换热器蒸汽供热输出 → 换热器冷凝模式/旁通口 | 唯一普通蒸汽流体；“乏蒸汽”不另注册 ID；不能输入 Create 原版蒸汽引擎 |
| `P3` | `contaminated_compound_coolant` | 受污染复合冷却剂 | 破损燃料/事故 → 净化器 | 屏蔽管线和机器容器；净化产生放射性滤芯 |
| `P3` | `contaminated_water` | 受污染废水 | 紧急注水/去污 → 净化或封存 | 不可直接倒入世界清除；服务器配置决定事故泄漏行为 |

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
| `P1` | `steel_mesh`、`steel_grate`、`steel_frame` | 合金钢网/格架/框架 | 过滤、燃料组件和多方块骨架 |
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

### 3.3 燃料、乏燃料与废物

| 阶段 | 注册 ID | 中文名 |
| :--- | :--- | :--- |
| `P1` | `low_enriched_uranium_dust`、`depleted_uranium_dust` | 低浓缩铀粉、贫化铀粉 |
| `P1` | `green_fuel_pellet`、`sintered_fuel_pellet` | 生燃料芯块、烧结燃料芯块 |
| `P1` | `fuel_cladding_tube` | 燃料包壳管 |
| `P1` | `fresh_fuel_rod`、`fresh_fuel_assembly` | 新燃料棒、浓缩铀燃料组件 |
| `P1` | `hot_spent_fuel_assembly`、`cooled_spent_fuel_assembly` | 枯竭铀燃料组件；首发耗尽输出为冷却态，热态 ID 仅作未来兼容保留 |
| `P1` | `encapsulated_spent_fuel`、`sealed_spent_fuel_cask` | 灌封乏燃料件、已封装乏燃料桶 |
| `P3` | `spent_fuel_rod`、`contaminated_cladding`、`contaminated_grid` | 乏燃料棒、受污染包壳、受污染格架 |
| `P3` | `reprocessed_fuel_dust`、`reprocessed_fuel_blend` | 再生燃料粉末、再生燃料混合粉 |
| `P3` | `green_reprocessed_fuel_pellet`、`reprocessed_fuel_pellet` | 再生燃料生芯块、再生燃料芯块 |
| `P3` | `reprocessed_fuel_rod`、`reprocessed_fuel_assembly` | 再生燃料棒、再生燃料组件 |
| `P3` | `high_level_waste`、`vitrified_high_level_waste`、`sealed_high_level_waste_cask` | 高放残渣、高放玻璃固化体、已封装高放废物桶 |
| `P3` | `radioactive_filter`、`encapsulated_filter`、`sealed_low_level_waste_cask` | 放射性滤芯、灌封滤芯、已封装低放废物桶 |
| `P3` | `recovered_alloy_scrap` | 回收合金碎料 |

燃料组件的剩余燃料值由唯一的 `remainingFuelFraction` 耐久字段表示，并遵循 Minecraft ItemStack 耐久语义；自定义 `fuel_assembly` 数据组件还保留燃料身份、预留 `decayHeatHu` 回收资格标识和 format，但不保存温度或完整度。每个燃料列一个组件，燃料列有效高度决定基础耐久消耗速度；1 格高、满功率、不超频的基准寿命为 3 小时，基础燃耗通过配置项控制。反应堆运行温度保存在多方块结构的燃料列状态，不为每个燃耗阶段创建新的注册 ID。只有处理规则和安全等级发生质变时才使用不同物品 ID。

实施优先级上，`fresh_fuel_assembly`（玩家名称“浓缩铀燃料组件”）、`cooled_spent_fuel_assembly`（玩家名称“枯竭铀燃料组件”）及其数据组件属于 P1.2/P1.3 的 **G1 共享合同**；`hot_spent_fuel_assembly` 保留注册 ID，但首发不生成、不作为运行输入。燃料耗尽直接得到冷却乏燃料，热/冷转换与乏燃料池玩法后置。上述身份必须先于换料端口和完整铀生产线冻结。可以先让它们通过创造模式或开发测试获得，以解除运行与换料开发阻塞；这不代表 P1.3 生存配方已经完成，也不得据此通过 P1.3 游戏内验收。

G1/G2 前置注册与占位素材已经落地；当前临时 PNG 来源固定为：煤炭→浓缩铀燃料组件、木炭→枯竭铀燃料组件、熔岩→热复合冷却剂、水→复合冷却剂。这里仅指纹理/图标占位；四种原版 ID 不得作为本模组运行时输入、存档身份或自动化绕过。后续接口和任务以 [G1/G2 决策包](./superpowers/plans/2026-08-17-g1-g2-decision-package.md) 与 [Agent 任务分派计划](./superpowers/plans/2026-08-17-g1-g2-agent-handoff-plan.md) 为准。

### 3.4 工具、防护与设备物品

| 阶段 | 注册 ID | 中文名 | 备注 |
| :--- | :--- | :--- | :--- |
| `P1` | `control_rod` | 控制棒组件 | 仅作为 `control_rod_drive` 的装配材料；不可单独放置，不注册同名方块 |
| `P1` | `dosimeter` | 手持辐射计 | 显示环境与物品辐射 |
| `P1` | `lead_lined_helmet`、`lead_lined_chestplate`、`lead_lined_leggings`、`lead_lined_boots` | 基础防护服四件套 | 不合并成一个不可穿戴物品 |
| `P1` | `lead_shielding_cask` | 铅屏蔽桶 | 空容器；装填后变为对应封装物品 |
| `P3` | `coolant_filter_medium` | 冷却剂过滤介质 | 净化器消耗品 |

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
| `P1` | `reactor_instrument_port`、`reactor_control_port` | 仪表、控制端口 |
| `P1` | `reactor_fuel_rod` | 燃料列内部的连续燃料柱方块；完整燃料列顶面必须配对 `reactor_refueling_port` |
| `P1` | `reactor_refueling_port` | 单列换料端口；只能安装在燃料列正上方的顶面，无 GUI，供人工或 Create 动力机械臂逐列取放燃料组件 |
| `P1` | `control_rod_drive` | 控制棒驱动器；唯一可放置的控制棒相关方块，方块实体渲染无碰撞箱模型动画并保存逐棒控制状态 |
| `P1` | `reactor_interlock` | 独立 SCRAM 联锁器 |
| `P2` | `blaze_reactor_manager` | 烈焰人反应堆管理台 |
| `P1` | `pressure_pipe_tier_1`、`pressure_valve_tier_1` | 一级耐压管和控制阀 |
| `P1` | `supercritical_steam_valve`、`turbine_exhaust_port` | 超临界蒸汽阀、排汽接口 |
| `P2` | `pressure_pipe_tier_2` | 二级强化管 |
| `P3` | `pressure_pipe_tier_3` | 三级陶瓷内衬管 |
| `P1` | `main_coolant_pump`、`feedwater_pump` | 主冷却泵、给水泵 |
| `P1` | `nuclear_heat_exchanger` | 三模式核换热器；核热供热、超临界蒸汽降级供热、冷源方块旁蒸汽冷凝 |
| `P1` | `steam_bypass_vent` | 超临界汽轮机安全旁通排汽口 |

### 4.3 单方块机器与储存设备

| 阶段 | 注册 ID | 中文名 | 输入/输出 | 运行职责或功能 |
| :--- | :--- | :--- | :--- | :--- |
| `P1` | `enrichment_centrifuge` | 富集离心机 | 铀料浆 → 低浓缩铀粉 + 贫化铀粉 + 工艺水 | 消耗稳定 Create 转速和应力完成铀富集；转速不稳降低效率 |
| `P1` | `fuel_sintering_furnace` | 燃料烧结炉 | 生燃料芯块 → 烧结燃料芯块 | 在密闭高温下烧结芯块；普通鼓风加热不能替代 |
| `P1` | `shielded_assembly_station` | 屏蔽装配台 | 芯块/包壳/格架或危险废物/容器 → 燃料棒、组件或封装件 | 隔离放射性物品并执行屏蔽装配，支持专用机械臂自动化 |
| `P1` | `industrial_gauge` | 工业仪表 | 机器状态 → 屏幕、比较器和显示链接信号 | 显示温度、压力、流量、转速、换热模式、热源品质与绿黄红状态，不改变机器状态 |
| `P1` | `nuclear_heat_exchanger` | 三模式核换热器 | 热复合冷却剂→复合冷却剂；超临界蒸汽→蒸汽；蒸汽+冷源→水 | 按输入选择唯一模式；核热/蒸汽供热默认返回 18/9 锅炉热值；冷凝缺少冷源时安全停机 |
| `P1`（后置） | `spent_fuel_pool_port` | 乏燃料池控制/流体端口 | 水、循环能力和热乏燃料 → 冷却状态 | 后置玩法；首发不实现热/冷转换、乏燃料池冷却或相关流体路线 |
| `P1` | `dry_storage_rack` | 干式贮存架 | 已封装乏燃料桶 → 安全贮存状态 | 检查封装完整性并提供稳定堆放，不消除辐射物质 |
| `P3` | `coolant_purifier` | 冷却剂净化器 | 受污染冷却剂 + 过滤介质 → 可复用冷却剂 + 放射性滤芯 | 回收工质并把污染转移到必须封存的滤芯，保持物质与风险守恒 |
| `P3` | `shielded_disassembler` | 屏蔽拆解机 | 冷却乏燃料组件 → 乏燃料棒 + 受污染格架 | 在屏蔽环境拆解乏燃料，禁止普通机械手直接处理 |
| `P3` | `sealed_reprocessor` | 密闭再处理器 | 乏燃料棒 + 处理介质 → 再生燃料粉末 + 高放残渣 + 受污染包壳 | 回收部分燃料价值，并保证同步产生不可消除的高放废物 |
| `P3` | `shielded_manipulator` | 屏蔽机械臂 | 危险物品搬运 | 在热室和屏蔽机器之间自动转移高辐射物品，不执行加工配方 |

这些机器是否最终使用单方块外形或小型多方块外壳，在不改变注册 ID 和输入输出职责的前提下可以调整。拥有库存、流体、应力或状态的设备必须使用方块实体；纯结构方块不创建无意义方块实体。

`control_rod` 是制造驱动器的不可放置组件物品，不是控制棒柱方块。世界中只注册 `control_rod_drive`；其下方内部有效高度全为空气，控制棒由驱动器方块实体渲染为无碰撞箱模型动画，不在空列中放置逐段控制棒方块。`reactor_refueling_port` 的燃料库存仍由反应堆权威状态持有，端口只提供下方单列的受控访问，不复制一份独立库存。只有所有对该列生效的相邻控制棒均完全插入且该列裂变产热为零，或该列燃料耗尽而被动停机时，端口才允许交互。端口拒绝普通漏斗和通用物品管道；详细状态只通过工程师护目镜浮窗显示。结构规则从本 ID 加入起直接采用严格配对：完整燃料列顶面必须是换料端口，旧规则中的普通外壳顶盖不再合法；当前开发阶段不提供旧结构迁移。

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
| `P1` | `experimental_reactor` | 实验反应堆 | 3×3/5×5 可调高度；外壳、观察窗、燃料列、控制棒驱动器列、逐列换料端口、冷/热端口、仪表和控制端口 | 完整长方体、端口齐全、燃料与控制棒布局合法；燃料列顶面使用换料端口，控制棒驱动器下方全为空气 | 产热、燃耗、余热、SCRAM 与不停整堆的局部隔离换料 |
| `P1` | `high_pressure_boiler` | 专用高压锅炉 | 首版固定尺寸；水接口、换热段、蒸汽接口和安全阀 | 密闭结构、至少一个合格核换热段 | 将一回路热量转成超临界蒸汽 |
| `P1` | `supercritical_steam_turbine` | 超临界汽轮机 | 首版固定尺寸；进汽段、转子、轴承、排汽段、主轴端 | 转子连续、端部与外壳完整 | 消耗超临界蒸汽并输出 SU、产生普通 `steam` |
| `P1`（后置） | `spent_fuel_pool` | 乏燃料冷却池 | 水池内衬、循环端口、储存格 | 后置玩法；首发不实现热/冷转换或乏燃料池冷却 |
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

第二批再绘制粒、粉、板、线、钢材和机器零件；第三批才进入反应堆、锅炉、超临界汽轮机与换热器结构纹理和 Blockbench/Flywheel 动画，避免首批素材范围失控。

## 7. 每项内容完成定义

一个注册项只有同时满足以下条件才算完成：

- Java 注册、中文/英文翻译、创造模式标签页分类齐全；
- 物品模型或方块状态/模型/纹理引用有效；
- 方块具有掉落表、挖掘标签和正确工具等级；
- 通用材料进入正确 `c:*` 标签，危险材料只进入专用标签；
- 配方或明确的游戏内来源存在，不产生无法获得的孤立物品；
- 方块实体状态可保存、重载且只同步客户端需要的数据；
- 数据生成和资源契约测试能发现遗漏；
- 已进入可游玩版本的注册 ID 不再改名。
## 8. 当前状态与文档入口

燃料组件身份、持久化 `fuel_assembly` 数据组件、冷却剂身份和确定性占位素材已完成开发前置注册，并有资源/数据契约覆盖；这不等于 G1/G2 运行接入或 P1.3 生存生产线完成。首发耗尽终态直接使用 `COOLED_SPENT`；`HOT_SPENT` 注册 ID 为未来热态路线保留，当前不生成、不依赖。煤炭、木炭、熔岩和水只作为占位纹理来源，不能作为配方输入、燃料别名、流体能力或运行时存档身份。

当前执行顺序见 [实施路线图](./implementation-roadmap.md)，活动计划见 `docs/superpowers/plans/`，已验收计划见 [文档归档索引](./archive/README.md)。

## 9. 需求变更与注册评审

本清单由项目经理维护，是注册 ID、内容阶段和素材责任的唯一索引。用户提出的新方块、物品、流体或结构需求必须先经过玩法用途、存档身份、输入输出、资源成本和阶段依赖评审，再写入本清单；开发者不得先注册一个临时 ID 再要求设计追认。

- 已进入可游玩或开发合同的注册 ID 视为兼容接口；需求变更优先调整显示名、纹理、模型和数值，不直接改 ID。
- 新内容必须同时声明阶段、来源/去向、危险等级、容器策略、自动化入口和完成定义；没有明确玩法用途的空壳内容暂缓注册。
- 项目经理只维护清单与任务计划，不实现注册代码。执行者需依据 [项目经理执行计划](./superpowers/plans/2026-08-17-project-manager-execution-plan.md) 提交注册、资源契约和游戏内证据。
- 当前已注册的燃料组件与冷却剂只是 G1/G2 前置身份，不代表完整燃料生产线、运行热端或 P1.3 生存可达性已经完成。
