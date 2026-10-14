# EXT-B-TURBINE-01B 运行与资源接口

本接口固定机器局部坐标和模型名字，供资源执行者生成 JSON/OBJ。世界朝向 `machine_facing` 取水平 `north/east/south/west`，代表从前端指向机外；局部 `+x` 是 `facing.getClockWise()`，局部 `+y` 向上，局部 `+z` 是 `facing.getOpposite()`。前端轴心 `(0,0,0)`，后端轴心 `(0,0,L-1)`。资源以 `machine_facing=north` 为基准；Java 成型状态按世界朝向旋转模型。

## Blockstate 合同

| ID | 属性及全集 | 成型模型入口 |
| --- | --- | --- |
| `turbine_casing` | `formed=false/true`, `machine_facing=north/east/south/west`, `piece=0..206` | `piece>0` 指向下表的对应网格；`formed=false` 或 `piece=0` 用 `turbine/turbine_casing_unformed` |
| `turbine_window` | 与 casing 同三属性 | 只对中段平直侧面有效的 piece 使用 `turbine/window/<piece-name>`；其他组合用 `turbine/turbine_window_unformed` |
| `turbine_rotor` | `formed=false/true`, `machine_facing=north/east/south/west`, `diameter=d3/d5/d7` | 成型时方块模型 `turbine/rotor_axle`；未成型时 `turbine/turbine_rotor_unformed`。成型叶片仅由 rotor BE renderer 绘制 `turbine/rotor_blades_d3/d5/d7`，方块模型不得再含叶片，避免重绘 |
| `turbine_output_shaft` | `formed=false/true`, `machine_facing=north/east/south/west`, `end=front/rear` | 成型 `turbine/output_shaft_front` 或 `turbine/output_shaft_rear`；未成型 `turbine/turbine_output_shaft_unformed`。两端相同方块 ID，`end` 决定仅向机外露出的轴面 |
| `turbine_controller` | `formed=false/true`, `machine_facing=north/east/south/west`, `side=up/down/left/right` | 成型 `turbine/controller_up/down/left/right`；未成型 `turbine/turbine_controller_unformed`。控制器是独立侧面板，无动力源 |
| `turbine_inlet`, `turbine_exhaust` | `formed=false/true`, `machine_facing=north/east/south/west`, `ring_role=top/upper_left/upper_right/left/right/lower_left/lower_right/bottom`, `outward=down/up/north/east/south/west` | 成型合法四侧 `turbine/inlet_up/down/left/right` 或 `exhaust_*`；非法旧/残留组合和未成型使用对应 `turbine/turbine_<id>_unformed`。`outward` 是实际世界能力面，模型用 ring_role 选相对侧 |

所有 blockstate JSON 必须覆盖每个属性组合，非法组合映射合法回退模型，不能留下 missing model。状态空间可由现有 Python 生成，不为每个状态生成独立 OBJ。`piece` 同时在碰撞和模型选择中使用，避免模型与占格漂移。

## `piece` 编码与有限模型集合

`piece=0` 为未成型回退。其余 1..206 以以下确定顺序连续编号：外径 D 依次 3、5、7；每档 section 依次 `front`, `middle`, `rear`；每个 section 的 `y` 从 `-r` 到 `+r`，同一 y 内 `x` 从 `-r` 到 `+r`。端面收录八棱外轮廓与方格有正面积交集的格，中心仍编号但实际由输出轴取代；中段仅收录真实薄壳与方格有正面积交集的格，中心由转子取代，其他内格为空气。`r=(D-1)/2`；外轮廓半宽 `h=D/2`，切角深度 `c=D/(2+sqrt(2))`。外轮廓为 `|X|<=h, |Y|<=h, |X|+|Y|<=2h-c`；内轮廓沿各面法线缩入 `3/16` 格，即直面界 `h−3/16`、斜面界 `2h−c−(3/16)√2`。中段格须有外轮廓正面积且与外内轮廓之间薄壳区域有正面积交集，不能只用 `max(|x|,|y|)=r`：D7 的四个 `(±2,±2)` 属于斜角壳格，四个 `(±3,±3)` 在外轮廓外。各档 `front/middle/rear` 计数分别为 9/8/9、25/16/25、45/24/45，总数 206。

每个 `piece` 的 canonical 名字 `d{D}_{section}_x{x}_y{y}`，例如 `d5_middle_x-2_y0`；casing 模型为 `create_nuclear_industry:block/turbine/casing/<名字>`，window 模型为 `create_nuclear_industry:block/turbine/window/<名字>`。window 仅允许 section=`middle` 且 `(x=0,y=±r)` 或 `(y=0,x=±r)`，即每档四个平直面中心格，沿 z 可连续排列；其他斜角/端盖格不可替换。资源可通过镜像/旋转复用相同网格，不必导出 206 份独立几何，但 JSON 引用必须存在。

成型叶片扫掠半径 D/2−3/16−1/8，即 1.1875/2.1875/3.1875 格。BER 的 `getRenderBoundingBox` 覆盖中心向四周 `D/2` 格，静态时仍绘制完整叶片，未来绕机器轴线旋转时只变换该 partial。窗口玻璃按实际图集与透明渲染层注册。所有模型的 `particle`、物品模型及未成型状态必须有合法资源。

## 客户端旋转

基准模型沿局部 +z 指向机身后方。`machine_facing=north` 不转，east/south/west 分别绕世界 Y 轴旋转 90/180/270 度；具体 JSON y 角以现有汽轮机生成器映射为准，Java 与资源统一读取该属性。`side=up/down/left/right` 依局部坐标定义；up/down 是世界上/下，left/right 对应局部 x=-r/+r。端轴 `end=front` 对外为 `machine_facing`，`rear` 对外为相反方向；两个模型分别保留外向轴头。端口的 `outward` 在成型时由局部侧面推导，不能由玩家视线决定。
