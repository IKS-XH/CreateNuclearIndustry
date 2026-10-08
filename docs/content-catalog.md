# 《机械动力：核工业》内容注册与素材清单

**更新：2026-10-08。** 注册身份、目标内容及资源职责由本清单维护；进度和完成证据统一见[路线图](./implementation-roadmap.md)。表中P1/P2/P3是目标阶段，不代表已实现；当前已验收基础链/热端/封存与三台教学，汽轮机思索04尚待本台播放。

命名空间`create_nuclear_industry`，版本为Java21 / Minecraft1.21.1 / NeoForge21.1.219 / Create6.0.10。玩法见[项目策划](./project.md)，材料数量与工序见[配方表](./recipes.md)。ID保持稳定，但首发前不研究旧存档兼容，不把旧半成品身份列为新加工路线。

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

P1核心反应堆已经交接，基础生产、锅炉、汽轮机与轻量封存也已按批验收；事故世界效果、辐射、污染、烈焰人管理员和反应堆可变结构仍后置。命名或阶段清单不能授权创建空内容。

## 2. 流体

| 阶段 | 注册 ID | 中文名 | 来源/去向 | 容器策略与关键行为 |
| :--- | :--- | :--- | :--- | :--- |
| `P1` | `compound_coolant` | 复合冷却剂 | 搅拌制造 → 反应堆冷端 | 提供桶；低温一回路工质 |
| `P1` | `hot_compound_coolant` | 热复合冷却剂 | 反应堆热端 → 核换热器 | 不允许普通桶徒手取用；换热后回到 `compound_coolant` |
| `P1` | `uranium_slurry` | 铀料浆 | 铀精矿混合 → 富集离心机 | 提供密封容器或桶；不得直接变成普通铀材料 |
| `P1` | `supercritical_steam` | 超临界蒸汽 | 专用高压锅炉 → 超临界汽轮机 | 换热器拒收；无生存桶/世界放置；采用Create原生管道/泵/储罐，二级耐压限制后置 |
| `P1` | `steam` | 蒸汽 | 高压锅炉普通汽产出或超临界汽轮机排汽 → 换热器冷凝模式/旁通口 | 唯一普通蒸汽流体；“乏蒸汽”不另注册 ID；不能输入 Create 原版蒸汽引擎 |
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
| `P1` | `steel_ingot`、`steel_plate`、`steel_rod` | 钢锭/板/杆 | 机器、管道和结构基础；兼容 `c:* /steel` 标签 |
| `P1` | `reinforced_steel_plate` | 强化钢板 | 高压锅炉、汽轮机和高温设备 |
| `P1` | `solder_ingot` | 锡合金焊料 | 密封、仪表和燃料棒封端 |
| `P1` | `steel_mesh`、`steel_grate`、`steel_frame` | 钢网/格架/框架 | 燃料组件、热室网格和多方块骨架 |
| `P1` | `steel_pipe_blank` | 钢管坯 | 01A核换热管束；未来耐压管段，本期不改既有燃料包壳路线 |
| `01A实施中` | `nuclear_heat_exchange_bundle` | 核换热管束 | 整机制造专用部件，工作台制造，无序列装配 |
| `P1` | `seal_ring`、`pressure_fitting` | 密封环、耐压接头 | 流体与蒸汽接口 |
| `P1` | `industrial_sensor`、`radiation_sensor` | 工业传感器、辐射传感器 | 仪表、联锁和危险检测 |
| 材料04中间态 | `incomplete_industrial_sensor`、`incomplete_radiation_sensor` | 工业传感器半成品、辐射传感器半成品 | Create原生序列装配进度；非独立成品，不加入模组创造页；不赋予检测功能 |
| 材料05中间态 | `incomplete_heavy_bearing` | 重型轴承半成品 | 已完成人工验收并合入main；Create原生单件及进度组件，不加入模组创造页 |
| `P1` | `maintenance_seal` | 维护密封件 | 停机维修泄漏部件 |
| `P1` | `heavy_bearing` | 重型轴承 | 汽轮机、泵和离心机 |
| `P1` | `coal_dust`、`charcoal_dust`、`quartz_dust`、`glass_dust` | 煤粉、木炭粉、石英粉、玻璃碎料 | 钢材、陶瓷和灌封材料 |
| `P1` | `iron_dust`、`steel_dust` | 铁粉、钢粉 | 粉末制钢中间材料；分别加入 `c:dusts/iron`、`c:dusts/steel` 及父标签 `c:dusts` |
| `P1` | `lapis_dust` | 青金石粉 | 首发扩展冷却剂中间材料；加入 `c:dusts/lapis` 并汇入 `c:dusts`，配方接受同标签等价材料 |
| `P1` | `obsidian_dust` | 黑曜石粉 | 首发扩展的基础材料阶段获取，先于耐热玻璃和相关设备制备 |
| `P1` | `industrial_ceramic`、`refractory_brick`、`insulation_plate` | 工业陶瓷、耐火砖、隔热板 | 锅炉和高温结构 |
| `P1` | `neutron_absorbing_ceramic` | 中子吸收陶瓷 | 控制棒 |
| `P1` | `heat_resistant_glass`、`shielded_glass` | 耐热玻璃、铅屏蔽玻璃 | 观察窗与热室 |
| `P1` | `vitrification_medium` | 玻璃固化基材 | 首发基础封存前提供制备路线；不要求先于全部设备实现，高放处理仍后置 |

复合冷却剂直接使用原版下界资源 `minecraft:glowstone_dust`（中文名“荧石粉”）。本模组不新增荧石矿物或荧石粉，也不为原版荧石复制注册项和素材。

用户于 2026-09-23 确认保留青金石制粉工序并对接 NeoForge 通用材料标签。青金石粉正式身份为 `create_nuclear_industry:lapis_dust`，不依赖第三方模组提供基础产物；`c:dusts/lapis` 是按通用约定补充的标签，并非 NeoForge 21.1.219 已内置的粉末。原料读取 `c:gems/lapis`，粉末用途读取 `c:dusts/lapis`，不能把整颗青金石或蓝色染料加入粉末标签绕过工序。D-02a 已确认粉碎轮承担制粉、磨石保留 Create 原有染料配方，并接受本模组制粉路线的粉碎轮门槛；该需求已验收合入main，见[合并验收](./reviews/2026-10-03/reactor-01/ACCEPTANCE.md)。

用户于 2026-09-23 确认先完成基础材料、设备与燃料生产，再完成机组运行闭环和乏燃料基础封存，复杂再处理后置。上述两种材料的 `P1` 标记表示后续首发扩展，不加入当前固定实验堆核心切片；本次只调整阶段，不表示已经注册或实现，不提前引入 P3 辐射、污染或高放处理系统。

钢板的正式注册 ID 冻结为 `steel_plate`，显示名为“钢板”，并加入对应的 `c:plates/steel` 通用标签。`alloy_steel_plate` 是早期需求记录中使用过的描述性旧名，不注册为物品、别名或兼容转发 ID；燃料列和控制棒列维修统一消耗 `steel_plate`。

用户于2026-10-01将制钢改为铁锭经粉碎轮制铁粉，4铁粉+1煤粉或木炭粉动力搅拌成5钢粉，再熔炼成钢锭。`iron_dust` 不复用 `create:crushed_raw_iron`，两者分别代表金属粉和粉碎粗矿；`steel_dust` 是熔炼前的独立中间物。原有 `steel_plate` 保持身份与维修语义，本批不新增钢材逆向拆粉路线。用户已确认本批完整客户端清单通过，并将普通钢材显示名由“合金钢”简化为“钢”（钢粉、钢锭、钢板；英文Steel Dust/Ingot/Plate）；ID、标签和配方不变。完整自动回归及最终整合状态见[03A收尾卡](archive/2026-10-08-completed-plans/2026-10-01-ext-a-material-03a.md)。

### 3.3 燃料、乏燃料与废物

| 阶段 | 注册 ID | 中文名 |
| :--- | :--- | :--- |
| `P1` | `low_enriched_uranium_dust`、`depleted_uranium_dust` | 低浓缩铀粉、贫化铀粉 |
| `P1` | `green_fuel_pellet`、`sintered_fuel_pellet` | 生燃料芯块、烧结燃料芯块 |
| `P1` | `fuel_cladding_tube` | 燃料包壳管 |
| `P1` | `fresh_fuel_assembly` | 浓缩铀燃料组件 |
| `P1` | `cooled_spent_fuel_assembly` | 枯竭铀燃料组件；首发燃料耗尽直接输出冷却态 |
| `暂缓` | `hot_spent_fuel_assembly` | 未来命名预留；首发不注册、不生成、不作为运行输入 |
| `P1` | `sealed_spent_fuel_cask` | 已封装乏燃料桶；2026-10-06确认单次灌封装桶，每桶一个枯竭组件，STORE-01已验收合入main |
| 预留，非首发 | `encapsulated_spent_fuel` | 灌封中间件；轻量方案取消首发注册，不作为前置 |
| `P3` | `spent_fuel_rod`、`contaminated_cladding`、`contaminated_grid` | 乏燃料棒、受污染包壳、受污染格架 |
| `P3` | `reprocessed_fuel_dust`、`reprocessed_fuel_blend` | 再生燃料粉末、再生燃料混合粉 |
| `P3` | `green_reprocessed_fuel_pellet`、`reprocessed_fuel_pellet` | 再生燃料生芯块、再生燃料芯块 |
| `P3` | `reprocessed_fuel_rod`、`reprocessed_fuel_assembly` | 再生燃料棒、再生燃料组件 |
| `P3` | `high_level_waste`、`vitrified_high_level_waste`、`sealed_high_level_waste_cask` | 高放残渣、高放玻璃固化体、已封装高放废物桶 |
| `P3` | `recovered_alloy_scrap` | 回收合金碎料 |

用户于2026-10-03取消首发`fresh_fuel_rod`中间物品规划，改为烧结燃料芯块＋燃料包壳管＋锡合金焊料＋钢格架直接装配既有`fresh_fuel_assembly`，不消耗工业传感器作为组件直接原料。四材料装配参数已验收，见[02E验收](./reviews/2026-10-03/fuel-02e/ACCEPTANCE.md)。该中间物品未注册；反应堆结构方块`reactor_fuel_rod`及P3乏燃料/再生燃料规划不受本次变更影响。

燃料组件剩余燃料由 Minecraft ItemStack 原版损伤值与最大耐久唯一确定，`remainingFuelFraction`是运行时派生比例。2026-10-03核对`ModItems`与`FuelAssemblyItemCodec`确认：当前未注册独立的`fuel_assembly`数据组件，原文所述燃料身份/`decayHeatHu`/format自定义字段不得视为已实现或成为02D装配的额外前置；`decayHeatHu`仍仅作未来回收资格预留，首发不使用。每个燃料列一个组件，其完整实体`ItemStack`由对应`reactor_refueling_port`方块实体唯一持久化；仪表端口只保存燃耗小数余量和热工模拟状态，不保存第二份组件。燃料列有效高度决定基础耐久消耗速度；1格高、满功率、不超频的基准寿命为3小时，基础燃耗通过配置项控制。反应堆运行温度、燃料列完整度和控制棒列完整度保存在多方块结构模拟状态，不为每个燃耗阶段创建新的注册ID。只有处理规则和安全等级发生质变时才使用不同物品ID。

受损燃料列的维修沿用燃料制造链的钢板`steel_plate`，不因移除新燃料棒中间物品而新增维修物品 ID。维修物品只改变目标燃料列完整度，不补充组件耐久、不解锁卡死控制棒，也不回退融毁进度。

实施优先级上，`fresh_fuel_assembly`（玩家名称“浓缩铀燃料组件”）、`cooled_spent_fuel_assembly`（玩家名称“枯竭铀燃料组件”）及其数据组件属于当前 P1 核心与后续燃料扩展共用的 **G1 合同**；`hot_spent_fuel_assembly` 只保留命名预留，首发不注册、不生成、不作为运行输入。燃料耗尽直接得到冷却乏燃料，热/冷转换与乏燃料池玩法后置。上述首发身份必须先于换料端口和完整铀生产线冻结。可以先让首发身份通过创造模式或开发测试获得，以解除运行与换料开发阻塞；这不代表后续生存生产线已经完成，也不得据此通过生存扩展验收。

燃料组件、冷/热冷却剂和生产来源已实现，当前使用独立注册身份及已验收资源；早期原版纹理占位合同只在历史归档中保留。原版煤炭、木炭、熔岩和水不充当本模组燃料组件或冷却剂的运行时别名。

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
| `P1` | `reactor_instrument_port` | 反应堆模拟状态锚点、工程师护目镜全堆信息接口和红石 SCRAM 输入端；保存热工、控制、冷/热冷却剂量与事故状态，不保存燃料组件实体物品；护目镜显示结构尺寸、列/端口计数、空列与控制棒列内部空气格派生的共享容量、冷/热当前库存、全堆发热量及冷却剂转化速率，不枚举逐列详情；所有玩家可用 Create 扳手普通右键请求服务端重扫，并在快捷栏获得本地化的成型结果或失败原因 |
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
| `P1` | `fuel_sintering_furnace` | 燃料烧结炉 | 生燃料芯块 → 烧结燃料芯块 | 单格无GUI、底部普通热源、顶进四侧出，1件烧结400有效tick；整格八棱外观、工作台/高炉制造；02A/02B全部手测通过并合入main，见[最终验收](./reviews/2026-10-03/fuel-02b/ACCEPTANCE.md) |
| `P1` | `shielded_assembly_station` | 屏蔽装配台 | 芯块/包壳/焊料/格架8/4/2/1 → 1新组件；另支持已验收的乏燃料灌封装桶；再生燃料后置 | [02E已合入main](./reviews/2026-10-03/fuel-02e/ACCEPTANCE.md)：2×2×2、四周漏斗物流、21格制造和动画分件；`shielded_assembly_part`为无独立物品/库存代理，不提前实现屏蔽机械臂 |
| `P1` | `nuclear_heat_exchanger` | 换热器 | 热复合冷却剂→复合冷却剂；蒸汽+冷源→水 | 核热/冷凝互斥，拒收超临界蒸汽；核热默认最高18锅炉热级，冷凝不供热；工作盆供热另批 |
| `暂缓` | `spent_fuel_pool_port` | 乏燃料池控制/流体端口 | 水、循环能力和热乏燃料 → 冷却状态 | 后置玩法；首发不实现热/冷转换、乏燃料池冷却或相关流体路线 |
| `P1` | `dry_storage_rack` | 干式贮存架 | 已封装乏燃料桶 → 贮存位置状态 | STORE-01已验收合入main：单格无GUI，默认16单件槽，保留完整桶记录；不消除辐射物质 |
| `P3` | `shielded_disassembler` | 屏蔽拆解机 | 冷却乏燃料组件 → 乏燃料棒 + 受污染格架 | 在屏蔽环境拆解乏燃料，禁止普通机械手直接处理 |
| `P3` | `sealed_reprocessor` | 密闭再处理器 | 乏燃料棒 + 处理介质 → 再生燃料粉末 + 高放残渣 + 受污染包壳 | 回收部分燃料价值，并保证同步产生不可消除的高放废物 |
| `P3` | `shielded_manipulator` | 屏蔽机械臂 | 危险物品搬运 | 在热室和屏蔽机器之间自动转移高辐射物品，不执行加工配方 |

这些机器是否最终使用单方块外形或小型多方块外壳，在不改变注册 ID 和输入输出职责的前提下可以调整。拥有库存、流体、应力或状态的设备必须使用方块实体；纯结构方块不创建无意义方块实体。

`control_rod` 是制造驱动器的不可放置组件物品，不是控制棒柱方块。世界中只注册 `control_rod_drive`；其下方内部有效高度全为空气，控制棒由驱动器方块实体渲染为无碰撞箱模型动画，不在空列中放置逐段控制棒方块。`reactor_refueling_port` 的方块实体是其下方单列燃料组件 `ItemStack` 的唯一持久化库存；仪表端口只在正式 tick 中读取临时燃料投影，不复制库存。有效结构中只有所有对该列生效的相邻控制棒均完全插入且该列裂变产热为零，或该列燃料耗尽而被动停机时，端口才允许正常换料。未成型时仅允许玩家空手安全取回端口本地组件，不允许装入、动力机械臂或通用物流访问；危险状态解绑留下的持久化安全锁必须继续拒绝取料。端口拒绝普通漏斗和通用物品管道；详细状态只通过工程师护目镜浮窗显示，其中耐久直接读取端口内部物品，列完整度/损坏度和当前 `HU/t` 发热量读取服务端结构缓存。结构规则从本 ID 加入起直接采用严格配对：完整燃料列顶面必须是换料端口，旧规则中的普通外壳顶盖不再合法；旧仪表快照中的组件必须迁移到空端口且不得复制。

### 4.4 组成多方块结构的可放置方块

**高压锅炉（2026-10-08已验收合入main）：** 采用配置范围内可变完整长方体，默认三边[5,11]；底层非边框放核换热器，完整再加热隔层分开水区/汽区。换热器与再加热段数量的最小值决定有效热力回路，各区容积分别决定水/汽容量。热液口与换热器同层、可组成底层非角点棱边，冷液口位于隔层侧面；两种汽独立记账、共用总容量和压力，各蒸汽口只取所选库存。见[验收](./reviews/2026-10-08/boiler-rework-01/ACCEPTANCE.md)。辅助热和二级耐压仍后置，`boiler_blaze_heater_port`不属本批。

本节列出玩家能够拿在手中、逐块放进世界、需要分别注册模型和纹理的组成方块。它们本身通常不能独立运行：外壳负责封闭结构，端口负责连接管道或物流，控制器负责扫描并组装结构。第 5 节则描述这些方块组装成功后形成的整台逻辑机器；两节不是重复注册两套内容。

| 阶段 | 所属结构 | 可放置组成方块（注册 ID） | 各类部件作用 |
| :--- | :--- | :--- | :--- |
| `P1` | 专用高压锅炉 | `high_pressure_boiler_casing`、`high_pressure_boiler_window`、`high_pressure_boiler_water_port`、`high_pressure_boiler_steam_port`、`high_pressure_boiler_hot_coolant_port`、`high_pressure_boiler_cold_coolant_port`、`boiler_safety_valve`、`boiler_heat_exchange_section`、`high_pressure_boiler_controller` | 外壳/观察窗封闭锅炉；热/冷口统一供回内置核换热器冷却剂；再加热段构成分区隔层；给水口进水，汽口按选择输出对应库存；安全阀泄压，控制器保存整炉账本 |
| `P1` | 超临界汽轮机 | 七件：`turbine_casing`、`turbine_rotor`、`turbine_inlet`、`turbine_exhaust`、`turbine_output_shaft`、`turbine_controller`、`turbine_window`；后续保留 `turbine_governor`、`turbine_brake` | 三档机组、侧控制器、双端共享应力、透明窗和薄壳叶片均已验收。调速器、制动器仍属后续规划 |
| `P3` | 屏蔽热室 | `hot_cell_casing`、`hot_cell_window`、`hot_cell_item_port`、`hot_cell_fluid_port`、`hot_cell_controller` | 外壳/观察窗提供辐射屏蔽；物品口和流体口限定危险物流；控制器检查屏蔽完整性并允许内部机械臂工作 |

## 5. 多方块结构

| 阶段 | 结构 ID | 中文结构名 | 组成与规模 | 组装条件 | 运行职责 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `P1` | `experimental_reactor` | 实验反应堆 | 固定 `5×5×5`；外壳、观察窗、燃料列、控制棒驱动器列、逐列换料端口、冷/热端口和仪表端口 | 完整长方体、冷/热端口和唯一仪表端口齐全、燃料与控制棒布局合法；控制端口不参与 P1 成型；燃料列顶面使用换料端口，控制棒驱动器下方全为空气 | 产热、燃耗、余热、逐列完整度与热负荷、控制棒列卡死、四向损伤传播、20% 融毁计时、SCRAM、冷却剂转化、关键合成物品维修与停机重置 |
| `P1` | `high_pressure_boiler` | 专用高压锅炉 | 配置范围内可变长宽高；内置核换热器/再加热隔层，水汽容量分别随区体积计算，双汽独立库存共用汽容量 | 密闭分区结构、有效热力回路、实际温压与出汽下限 | 将实际已付核热转成蒸汽或超临界蒸汽新批次 |
| `P1` | `supercritical_steam_turbine` | 超临界汽轮机 | 默认宽×轴长×高为3×5×3、5×8×5、7×11×7，256RPM；仅接受服务端配置中的三档规格，成型后显示匹配的薄壳多边形模型 | 匹配某个预设规格，转子、空腔及双端完整；不接受任意尺寸 | 规模决定额定流量与总SU上限等性能；消耗超临界蒸汽并输出SU、产生 `steam` |
| `暂缓` | `spent_fuel_pool` | 乏燃料冷却池 | 水池内衬、循环端口、储存格 | 后置玩法；首发不实现热/冷转换或乏燃料池冷却 |
| `P2` | `variable_reactor` | 可变尺寸反应堆 | 5×5×5 至 11×11×15 | 从固定实验堆扩展；有效燃料长度受限 | 结构自由度和规模化产热 |
| `P3` | `shielded_hot_cell` | 屏蔽热室 | 屏蔽外壳、观察窗、机械臂、物品/流体端口 | 屏蔽完整，危险物料不能从非端口穿过 | 高放废物灌封与处理 |

**核换热器现行边界（2026-10-09）：** `nuclear_heat_exchanger`显示名“核换热器”，核热→Create锅炉、专用锅炉及[01D定向直列](archive/2026-10-08-completed-plans/2026-10-04-ext-b-exchanger-01d.md)均已验收。顶供热、前冷出后热入，最多16台同向首尾相连共享库存，各台贡献双4000mB容量、独立储热与NBT份额；左右/底面不提供流体能力。配方不变，无GUI。[冷凝回水01](archive/2026-10-08-completed-plans/2026-10-05-ext-b-condense-01.md)已实现并[联合验收合入main](./reviews/2026-10-05/condense-01/ACCEPTANCE.md)，各台顶部接触水源、雪块、冰、浮冰或蓝冰，按实际冷凝量融水/蒸发。工作盆02R1持续核热已通过用户手测；本轮补齐顶部烧结炉使用同一持续热源。

**范围修订：** 用户取消换热器超临界蒸汽输入，原降级供热模式及9级热值规划撤销。入口只接受热复合冷却剂或 `steam`。工作盆已验收，新增[烧结炉03](./superpowers/plans/2026-10-09-ext-b-exchanger-03-sintering.md)可在汽轮机取景复看期间实施，见[接续排期](./superpowers/plans/2026-10-08-turbine-exchanger-ponder-sequence.md)。

Create 流体储罐锅炉和其他原生受热设备不是本模组多方块。`nuclear_heat_exchanger` 对锅炉注册 `BoilerHeater`：核热默认最高18级，实际按储热自适应；冷凝不提供加热。工作盆直接放在换热器顶部即持续超级加热，默认4mB热液/t转等量冷液，断液或冷满立即停热，不按加工状态或批次收费；烧结炉本批沿用同一持续负载和共用配置，原燃烧室加热及400有效tick/件保持。

所有本模组多方块结构使用稳定结构 ID 和结构局部坐标保存状态，不把世界绝对坐标写入纯模拟核心。结构只在组装、拆除或端口变化时重扫，正常 tick 不逐方块扫描。

## 6. 第一批素材清单

**2026-09-29 制作方式更新：** 用户授权美术重绘采用 Agent 绘制像素 SVG 源稿，再确定性导出游戏使用的 16×16 PNG，不直接使用生图模型。用户现已批准青金石粉、铅锭、铅矿石、钢板四张样稿风格，并明确将现有全部 51 张 PNG（含反应堆平面纹理）按此重绘。源稿保留在开发资产目录，候选游戏接入按 [EXT-ART-02](archive/2026-10-08-completed-plans/2026-09-29-ext-art-02.md) 审核及客户端验收；四张 flow 保留现有 16×64 布局。具体合同见 [启动计划](archive/2026-10-08-completed-plans/2026-09-29-resources-production-art-start-plan.md)。该授权提前基础贴图工作，不提前复杂设备模型或动画。

**同日客户端反馈修订：** 用户认可其他新素材，要求两种冷却剂恢复重绘前外观。因此当前采用 43 张新 SVG 贴图与 8 张保留原始 PNG 的冷却剂贴图（block/fluid 各含冷/热 still/flow）。冷却剂原图例外由 [EXT-ART-02A](archive/2026-10-08-completed-plans/2026-09-29-ext-art-02a-coolant-restore.md) 管理，批量导出不得重新覆盖为撤回的新外观；流体渲染、颜色和行为不变。

**2026-10-01 板材与粒素材验收：** EXT-ART-03增加两板、03A按用户反馈方正化、04增加铅粒/锡粒，当前游戏PNG共55张，历史基线仍为51项。原51图保持，新增四图已随铅锡加工通过客户端清单验收并合入main。后续优先主线功能，不在本批继续扩展美术范围。

原始基础素材清单如下；当前重绘范围已扩展为 EXT-ART-02 的 51 张现有贴图，不以本表限制：

1. **6 张矿石方块纹理：** `lead_ore`、`deepslate_lead_ore`、`tin_ore`、`deepslate_tin_ore`、`uranium_ore`、`deepslate_uranium_ore`。
2. **3 张粗矿物品纹理：** `raw_lead`、`raw_tin`、`raw_uranium`。
3. **3 张粗矿块纹理：** `raw_lead_block`、`raw_tin_block`、`raw_uranium_block`。
4. **2 张锭纹理：** `lead_ingot`、`tin_ingot`。
5. **1 张铀精矿纹理：** `uranium_concentrate`，替代当前没有用途的“铀锭”素材。
6. **2 张金属储存块纹理：** `lead_block`、`tin_block`。

第一批合计 **17 张本模组基础纹理**。三种粉碎粗矿直接使用 Create 自带物品及素材，不计入本模组绘制清单。矿石、粗矿块和金属块先使用原版 `cube_all` 模型，物品先使用 `item/generated`；此阶段不需要 Blockbench。每张纹理采用 16×16 像素、禁用抗锯齿、保持原版/Create 的清晰像素边缘。

第二批再绘制粒、粉、板、线、钢材和机器零件；第三批进入反应堆、锅炉、超临界汽轮机与换热器的设备表现，避免首批素材范围失控。现有反应堆及设备的平面贴图已按各批推进。用户于2026-10-04最新明确汽轮机提供几种预设规格，每档在成型后呈现与规模匹配的整体多边形模型；静态模型与对应功能一同实现和验收。其余未派发的连接纹理、动画、粒子和音效仍按对应任务另行安排。

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

基础生产来源、燃料组件、冷/热冷却剂、锅炉及汽轮机回水闭环均已按批验收；证据统一见[路线图](./implementation-roadmap.md)。首发耗尽终态为`COOLED_SPENT`；`HOT_SPENT`仅为未来热态路线保留命名，首发不注册、不生成、不依赖。

当前执行顺序见 [实施路线图](./implementation-roadmap.md)，活动计划见 `docs/superpowers/plans/`，已验收计划见 [文档归档索引](./archive/README.md)。

## 9. 需求变更与注册评审

本清单由项目经理维护，是注册 ID、内容阶段和素材责任的唯一索引。用户提出的新方块、物品、流体或结构需求必须先经过玩法用途、存档身份、输入输出、资源成本和阶段依赖评审，再写入本清单；开发者不得先注册一个临时 ID 再要求设计追认。

- 已进入可游玩或开发合同的注册 ID 视为兼容接口；需求变更优先调整显示名、纹理、模型和数值，不直接改 ID。
- 新内容必须同时声明阶段、来源/去向、危险等级、容器策略、自动化入口和完成定义；没有明确玩法用途的空壳内容暂缓注册。
- 项目经理只维护清单与任务计划，不实现注册代码。执行者需依据 [文档入口与当前任务](./README.md) 提交注册、资源契约和游戏内证据。
- 内容表中的阶段与身份合同不等于实现状态；P1 运行接入完成也不代表完整燃料生产线、热端或后续生存扩展已完成。

## 10. Create Ponder（“思索”）覆盖合同

已实现设备基础教学逐台验收，具体事故教学后置。当前离心机一幕、反应堆四幕、高压锅炉三幕已通过，汽轮机三幕正在本台候选；先完成工作盆核热功能再制作换热器多情景教学。原11项反应堆入口保留，相似构件可以共用情景；只演示实际已实现规则，不用假设备或未来事故填充。

### 10.1 多方块结构

| 阶段 | 结构 ID | Ponder 必须演示的内容 |
| :--- | :--- | :--- |
| `已验收`四情景；事故后补齐 | `experimental_reactor`及三个独立情景 | [本批任务](archive/2026-10-08-completed-plans/2026-10-06-device-ponder-02-r1.md)：搭建、棒列关系、运行与停机、装料与换料分别选择；包括自由布局、多冷热口、并联超频、四向控制及平均插深、未受控列无法中止、可见冷却回路、红石停机/恢复、余热冷却及玩家/动力机械臂装卸。保留11项入口，已通过播放；损伤、维修、卡死、融毁与具体事故教学按后置计划补齐。 |
| `已验收`三幕 | `high_pressure_boiler` | 搭建内置换热/汽水分区、可见管路运行、双汽库存与汽口过滤/调压；辅助热与具体事故后补 |
| `04候选待播放`三幕 | `supercritical_steam_turbine` | 核心先行包壳、三档尺寸、可见进排汽、双轴共享应力及实际流量/效率；调速器、制动器、超速风险不提前教学 |
| `暂缓` | `spent_fuel_pool` | 水池内衬、循环端口、储存格和后置状态；首发不实现热/冷转换 |
| `P2` | `variable_reactor` | 尺寸边界、燃料有效高度、控制棒邻接、局部反馈和多端口布置 |
| `P3` | `shielded_hot_cell` | 屏蔽完整性、观察窗、物品/流体端口、屏蔽机械臂和危险物流边界 |

### 10.2 单方块设备

| 阶段 | 注册 ID | Ponder 必须演示的内容 |
| :--- | :--- | :--- |
| `已验收`基础教学 | `enrichment_centrifuge` | [联合验收](./reviews/2026-10-07/ponder-acceptance-01/ACCEPTANCE.md)：两格放置、底部动力、顶部进浆、水平面双粉/回水与黄铜过滤、稳定转速/输出堵塞暂停、停转轴承维修；当前失稳行为为暂停，不展示未实现的效率曲线 |
| `P2` | `fuel_sintering_furnace` | 生燃料芯块输入、密闭高温烧结、烧结燃料芯块输出，说明普通鼓风加热不可替代 |
| `P2` | `shielded_assembly_station` | 芯块/包壳/焊料/格架直接装配组件、屏蔽边界、燃料组件和危险物品装配限制 |
| `P2` | `nuclear_heat_exchanger` | 核热换热与供热、普通蒸汽冷凝回水、顶部冷源和定向直列；工作盆核热接入定稿后独立教学。拒收超临界蒸汽，不演示取消的降级供热路线；数值可由配置覆盖 |
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
| `P1` | `reactor_instrument_port` | 唯一模拟状态所有者、Create 扳手普通右键成型诊断、工程师护目镜显示尺寸/列数/冷热端口数/可计空气格数/布局派生共享容量、红石高电平保持 SCRAM/低电平恢复停堆前位置、无控制棒列时拒绝 SCRAM、不得重复安装；燃料组件实体物品由各换料端口保存 |
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

`control_rod` 是不可单独放置、仅用于制造 `control_rod_drive` 的组件，不创建脱离驱动器的独立使用场景；它必须在控制棒驱动器 Ponder 中展示。钢板不创建纯材料独立 Ponder，但必须在实验反应堆 Ponder 中演示右键换料端口维修燃料列和右键控制棒驱动器维修控制棒列。Create 工程师护目镜属于外部模组物品，不新增本模组注册项，但反应堆仪表端口和换料端口 Ponder 必须演示其观察结果。

### 10.5 通用 Ponder 场景标准

- 每个入口至少包含“用途 → 正确搭建/放置 → 输入输出或交互 → 自动化/红石 → 故障与安全”的连续步骤；纯静态展示不算完成。
- 场景 ID 默认按 `create_nuclear_industry:<registry_id>` 命名；一个 ID 需要多个主题时使用稳定后缀，并在入口清单中记录所有主题。
- 场景只使用已注册、已批准的方块、物品、流体和配方；配方 Ponder 与 `recipes.md` 不一致时以项目经理文档为准并阻止交付。
- Ponder 是客户端教学层，不能读取或修改服务端权威状态；SCRAM、换料许可、燃耗、损伤和辐射仍由服务端逻辑执行。
- 中文和英文标题/步骤必须可本地化；场景中的数值使用配置或“示意值”标记，不把未冻结的平衡数值写死。
- 资源契约检查入口ID与场景ID对应；固定实验堆基础场景已通过客户端验收，现有设备按2026-10-06用户指示逐台播放验收，发布前仍须全入口总验收。事故教学只在事故实际实现后补齐；不追溯阻塞已完成的功能批次。
