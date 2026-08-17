# P1 PNG 素材生成文档：交给 GPTimage2

**项目：** Create: Nuclear Industry
**目标版本：** Minecraft 1.21.1 + NeoForge + Create 6.0.10
**命名空间：** `create_nuclear_industry`
**用途：** 本文可直接交给 GPTimage2，按清单逐张生成 PNG 素材
**生成范围：** P1 当前核心实验反应堆；P1 规划但暂缓实现的锅炉、汽轮机和后续机器不在首批生成范围

## 一、给 GPTimage2 的总指令

请为 Minecraft Java Edition 1.21.1 的 Create 风格核工业模组生成像素纹理 PNG。所有图片必须服务于方块纹理或物品纹理，不要生成概念图、宣传图、UI 截图、3D 渲染图、文字标签或精灵表。

### 统一美术风格

- 16-bit 像素艺术，清晰方格边缘，接近原版 Minecraft 与 Create 的工业机械风格。
- 以深灰、黑铁、铜、黄铜、钢蓝、暗红、琥珀橙和核能绿色作为统一色板。
- 金属表面有少量磨损、铆钉、接缝和油污，但必须保持清晰，不能变成照片写实材质。
- 反应堆相关设备使用深灰钢与暗铜边框；危险/高温部分使用低饱和红橙色；冷却剂使用青蓝色；热冷却剂使用橙红色夹少量青蓝反光。
- 不要在纹理中生成任何可读文字、数字、Logo、水印、字母或复杂警告牌；危险提示使用抽象三叶辐射符号或颜色条即可。
- 保持同一组素材的光照方向、像素密度、线宽和色板一致。

### PNG 技术要求

- 输出格式：PNG、RGBA、32-bit。
- 方块和普通物品纹理：精确 16×16 像素，透明背景只用于物品纹理需要透明的区域。
- 如果 GPTimage2 不能直接输出 16×16：先生成带明确 16×16 逻辑像素网格的 512×512 像素主图，再用最近邻缩放为 16×16；禁止双线性、双三次、抗锯齿和锐化。
- 流体静止纹理：16×16；流体流动动画：16×64，垂直排列 4 帧，每帧 16×16，四帧必须能无缝循环。
- 禁止把多张素材放在一张图中。每次只生成一张 PNG，并使用清单中的目标文件名。
- 不要生成模型 JSON、方块状态 JSON、语言文件、`.mcmeta`、声音或 Blockbench 工程；本任务只生成 PNG。

### 统一负面提示

不要照片写实、不要高模 3D、不要平滑渐变、不要抗锯齿、不要模糊、不要随机透视、不要斜角概念图、不要文字、不要 Logo、不要水印、不要 UI、不要精灵表、不要把多个方块拼成一张图、不要改变同组素材的视角和像素比例。

## 二、输出目录规则

文件路径均相对于：

`src/main/resources/assets/create_nuclear_industry/textures/`

- 方块纹理放在 `block/`。
- 物品纹理放在 `item/`。
- 流体纹理放在 `fluid/`；如果代码最终采用 `block/`，由开发者在资源接入时调整引用，不改变图片内容。
- 文件名全部小写下划线，不能添加版本号、颜色后缀或 `final` 后缀。

## 三、第一批：P1 实验反应堆核心方块纹理

以下素材优先生成，直接服务固定 `5×5×5` 实验反应堆、控制棒、换料和冷却剂交互。

| 目标文件 | 类型 | 生成要求 |
| :--- | :--- | :--- |
| `block/reactor_casing_side.png` | 16×16 方块 | 深灰合金钢侧面，铜色接缝，四角铆钉，适合平铺 |
| `block/reactor_casing_top.png` | 16×16 方块 | 深灰钢顶面，中央低调圆形检修盖或分段钢板 |
| `block/reactor_casing_bottom.png` | 16×16 方块 | 深灰钢底面，承重钢板和少量阴影 |
| `block/reactor_window.png` | 16×16 方块 | 厚重耐热观察窗，深色玻璃，暗红/琥珀反光，金属边框 |
| `block/reactor_cold_port_side.png` | 16×16 方块 | 冷端接口，钢制法兰，青蓝色冷却剂环形标识 |
| `block/reactor_cold_port_top.png` | 16×16 方块 | 冷端顶部法兰和青蓝色中心接口 |
| `block/reactor_hot_port_side.png` | 16×16 方块 | 热端接口，钢制法兰，橙红色热量标识 |
| `block/reactor_hot_port_top.png` | 16×16 方块 | 热端顶部法兰和橙红色中心接口 |
| `block/reactor_instrument_port_side.png` | 16×16 方块 | 反应堆仪表端口，深灰金属、黄铜边框、琥珀色指示灯 |
| `block/reactor_instrument_port_top.png` | 16×16 方块 | 顶部仪表盖板、简单刻度环和中央接口，不出现文字 |
| `block/reactor_refueling_port_side.png` | 16×16 方块 | 换料端口侧面，厚钢法兰、锁定结构、黄铜机械边 |
| `block/reactor_refueling_port_top.png` | 16×16 方块 | 可被准星观察的换料端口顶部，中央圆形燃料通道和安全锁 |
| `block/reactor_fuel_rod_side.png` | 16×16 方块 | 内部燃料列，深钢包壳、细窄琥珀/橙红能量线，不能有火焰 |
| `block/reactor_fuel_rod_top.png` | 16×16 方块 | 燃料列截面顶部，规则金属圆孔/栅格，适合连续堆叠 |
| `block/control_rod_drive_side.png` | 16×16 方块 | 控制棒驱动器，深灰钢、黄铜滑轨、青灰控制组件和小型指示灯 |
| `block/control_rod_drive_top.png` | 16×16 方块 | 顶部浮动滑块基座，滑轨和刻度槽；不要画可读数字 |
| `block/pressure_pipe_tier_1.png` | 16×16 方块 | 一级耐压管，深灰钢管、铜色环箍、低调压力标识 |
| `block/pressure_valve_tier_1.png` | 16×16 方块 | 一级耐压阀，深灰阀体、黄铜手轮、青蓝/橙红双向可读色带 |
| `block/main_coolant_pump_side.png` | 16×16 方块 | 主冷却泵，深灰泵体、铜色法兰、青蓝旋转部件 |
| `block/main_coolant_pump_top.png` | 16×16 方块 | 主冷却泵顶部检修盖和对称法兰 |

### 反应堆方块组总提示词

```text
Generate one 16x16 RGBA Minecraft pixel-art block texture for:
create_nuclear_industry:block/<TARGET_FILE>

This is a Create-style nuclear industry block for a fixed 5x5x5 experimental reactor. Use dark alloy steel, worn copper/brass joints, clear pixel clusters, vanilla Minecraft readability, and the shared palette: charcoal gray, dark steel blue, brass, amber, coolant cyan, and restrained thermal orange. The texture must be tile-friendly, front-direction neutral unless the filename explicitly says top, and contain no readable text, no logo, no watermark, no UI, no perspective rendering, and no multiple assets. Keep edges crisp and use no anti-aliasing.

Specific role of this texture: <ROLE_FROM_TABLE>
Output only the single PNG texture at the requested pixel size.
```

将 `<TARGET_FILE>` 和 `<ROLE_FROM_TABLE>` 替换为表中的目标文件和生成要求，每个文件单独调用一次。

## 四、第一批：P1 流体纹理

| 目标文件 | 尺寸 | 生成要求 |
| :--- | :--- | :--- |
| `fluid/compound_coolant_still.png` | 16×16 | 深青蓝复合冷却剂，玻璃般高光，稳定平静液面感 |
| `fluid/compound_coolant_flow.png` | 16×64 | 同一冷却剂的四帧流动动画，青蓝条纹从上向下流动，四帧无缝 |
| `fluid/hot_compound_coolant_still.png` | 16×16 | 橙红高温液体，保留少量青蓝冷却剂反光，不画火焰 |
| `fluid/hot_compound_coolant_flow.png` | 16×64 | 四帧橙红热流动画，像高温液体而非岩浆，循环无缝 |

### 流体总提示词

```text
Generate one seamless Minecraft fluid texture PNG for:
create_nuclear_industry:fluid/<TARGET_FILE>

Use crisp 16-bit pixel art, no anti-aliasing, no text, no container, no bucket, no background object. The fluid must tile seamlessly on all edges. For a still texture output 16x16. For a flow texture output a vertical 16x64 strip containing four separate 16x16 animation frames with consistent lighting and a seamless loop.

Fluid identity: <COMPOUND_COOLANT_OR_HOT_COMPOUND_COOLANT>
Visual behavior: <STILL_OR_FLOW>
Output only the single PNG.
```

## 五、第一批：P1 物品纹理

### 5.1 燃料、控制棒和维修

| 目标文件 | 类型 | 生成要求 |
| :--- | :--- | :--- |
| `item/fresh_fuel_assembly.png` | 16×16 透明物品 | 小型屏蔽燃料组件，钢制包壳、规则燃料棒束、少量琥珀色核心，不出现辐射光晕 |
| `item/cooled_spent_fuel_assembly.png` | 16×16 透明物品 | 冷却乏燃料组件，颜色变暗、轻微旧化和封存标记感，不出现液体或火焰 |
| `item/control_rod.png` | 16×16 透明物品 | 深色控制棒组件，黑灰吸收陶瓷段、金属端帽，轮廓清楚 |
| `item/steel_plate.png` | 16×16 透明物品 | 合金钢维修板，厚重矩形钢板、四角铆点、少量磨损 |
| `item/dosimeter.png` | 16×16 透明物品 | 手持辐射计，黄铜/钢外壳、琥珀色小屏，但屏幕不能出现可读文字 |
| `item/lead_shielding_cask.png` | 16×16 透明物品 | 铅屏蔽桶，灰铅色圆桶、厚盖、黄色危险色带，不出现文字 |

正式注册 ID 已冻结为 `steel_plate`，显示名为“合金钢板”，现有纹理文件保持 `item/steel_plate.png`。不要生成 `alloy_steel_plate.png`，也不要把它作为第二物品或兼容别名。

### 5.2 P1 基础材料首批 17 张纹理

这些纹理用于 P1.1 材料链和实验堆前置内容。每张单独生成，不能拼成物品表。

| 目标文件 | 类型 | 视觉要求 |
| :--- | :--- | :--- |
| `block/lead_ore.png` | 16×16 方块 | 石质底色，少量暗灰铅矿斑 |
| `block/deepslate_lead_ore.png` | 16×16 方块 | 深板岩底色，低饱和铅矿斑 |
| `block/tin_ore.png` | 16×16 方块 | 石质底色，冷银灰锡矿斑 |
| `block/deepslate_tin_ore.png` | 16×16 方块 | 深板岩底色，冷银灰锡矿斑 |
| `block/uranium_ore.png` | 16×16 方块 | 石质底色，暗橄榄绿色铀矿斑，避免荧光过强 |
| `block/deepslate_uranium_ore.png` | 16×16 方块 | 深板岩底色，暗绿色铀矿斑 |
| `item/raw_lead.png` | 16×16 透明物品 | 不规则暗灰铅粗矿块 |
| `item/raw_tin.png` | 16×16 透明物品 | 不规则银灰锡粗矿块 |
| `item/raw_uranium.png` | 16×16 透明物品 | 不规则暗绿色铀粗矿块 |
| `block/raw_lead_block.png` | 16×16 方块 | 多块铅粗矿压成的储存块，暗灰和铅蓝色 |
| `block/raw_tin_block.png` | 16×16 方块 | 多块锡粗矿压成的储存块，冷银灰色 |
| `block/raw_uranium_block.png` | 16×16 方块 | 多块铀粗矿压成的储存块，暗橄榄绿色 |
| `item/lead_ingot.png` | 16×16 透明物品 | 厚实暗灰铅锭，低光泽 |
| `item/tin_ingot.png` | 16×16 透明物品 | 明亮冷银色锡锭 |
| `item/uranium_concentrate.png` | 16×16 透明物品 | 密封感的暗绿色铀精矿，小颗粒聚合，不要粉尘飞散 |
| `block/lead_block.png` | 16×16 方块 | 平整暗灰铅块，细微金属边缘 |
| `block/tin_block.png` | 16×16 方块 | 平整银灰锡块，细微冷色高光 |

### 物品总提示词

```text
Generate one 16x16 RGBA transparent-background Minecraft item texture for:
create_nuclear_industry:item/<TARGET_FILE>

Use crisp vanilla/Create-style pixel art with a readable silhouette at 16x16. Render only one centered item, with no hand, no inventory slot, no background scene, no text, no logo, no watermark, no multiple items, no anti-aliasing, and no 3D perspective. Keep the palette and lighting consistent with the Create: Nuclear Industry industrial-nuclear set.

Specific item appearance: <ROLE_FROM_TABLE>
Output only the single PNG texture.
```

## 六、第二批：P1 规划资产，暂不要求首轮生成

以下内容在内容清单中标记为 P1，但当前执行计划的 P1 核心只先实现固定实验反应堆；这些 PNG 等对应代码和玩法任务启动后再生成，不能用概念图提前占用注册 ID：

- 高压锅炉：`high_pressure_boiler_casing`、`high_pressure_boiler_window`、`high_pressure_boiler_water_port`、`high_pressure_boiler_steam_port`、`boiler_safety_valve`、`boiler_blaze_heater_port`、`boiler_heat_exchange_section`、`high_pressure_boiler_controller`。
- 超临界汽轮机：`turbine_casing`、`turbine_window`、`turbine_rotor`、`turbine_inlet`、`turbine_exhaust`、`turbine_output_shaft`、`turbine_governor`、`turbine_brake`、`turbine_controller`。
- 后续自动化设备：`enrichment_centrifuge`、`fuel_sintering_furnace`、`shielded_assembly_station`、`nuclear_heat_exchanger`、`dry_storage_rack`、`feedwater_pump`、`steam_bypass_vent`、`supercritical_steam_valve`、`turbine_exhaust_port`。
- P1 材料链的剩余纹理：钢材、陶瓷、耐火砖、隔热板、中子吸收陶瓷、耐热玻璃、屏蔽玻璃、燃料芯块、包壳管、燃料棒、燃料封装件和其他尚未进入当前核心切片的材料。

这些暂缓资产需要等开发任务确认实际注册 ID、模型朝向、方块实体动画和配方后再生成；GPTimage2 不要自行补全。

## 七、交付命名和验收清单

GPTimage2 生成后，交付人按以下规则整理，不要把生成平台的随机文件名直接放入项目：

- 每个文件重命名为本文清单中的精确文件名。
- 所有文件均为 PNG；检查 RGBA、尺寸、透明区域和是否误带文字/水印。
- 方块纹理检查四边平铺、相同材质的色板一致、顶部/侧面/底面方向正确。
- 物品纹理检查 16×16 下轮廓仍可识别，透明背景没有白边或黑边。
- 流体动画检查为 16×64，四帧尺寸正确且循环自然；动画 `.mcmeta` 由开发者后续编写，不由 GPTimage2 生成。
- 不修改核心设计文档，不新增未经批准的注册 ID，不把暂缓设备素材冒充 P1 核心素材。
- 资源接入前由开发者按 `minecraft-resource-pack` 技能检查路径和引用；PNG 本身由项目经理审查后再进入版本库。

## 八、建议的 GPTimage2 调用顺序

1. 先生成反应堆外壳、窗口、冷/热端口、仪表端口、换料端口和控制棒驱动器。
2. 再生成燃料列、一级管道、阀门和主冷却泵。
3. 再生成复合冷却剂与热复合冷却剂的静止/流动纹理。
4. 再生成燃料组件、控制棒、合金钢板、辐射计和铅屏蔽桶。
5. 最后生成 17 张基础矿物/材料纹理。

每一批生成后先进行风格一致性审查，再进入下一批；不要一次性生成锅炉、汽轮机、反应堆和所有材料的混合图集。
