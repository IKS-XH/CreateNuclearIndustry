# EXT-B-TURBINE-ASSETS-01 资源交付记录

状态：正式汽轮机资源已按 C 冻结接口写入候选 `src/main/resources`，静态交叉校验通过并交还 C 做集成检查。D 的资源制作/安装范围已交付；C 的资源加载与 GameTest、Minecraft 客户端外观检查仍未由本任务验证，不代表整项汽轮机已验收。

## 资源内容

- `tools/art-assets/svg/block/turbine/` 保存 8 张16×16 SVG正式源稿；`tools/art-assets/turbine_assets.py` 使用项目捆绑 Python 3.12 和 Pillow 确定性导出8张机身PNG。已沿用既有SVG风格，没有重绘旧资产或使用生图工具。纹理在 `assets/create_nuclear_industry/textures/block/turbine/`。
- 新增 `tools/art-assets/turbine_models.py`，导出 NeoForge `neoforge:obj` 静态模型：24种机壳分段OBJ、控制器/转子/后轴3种中心OBJ、6种进排汽口OBJ和6种未成型OBJ，共39个OBJ与39个模型包装JSON；另有共享MTL、6份item模型和6份blockstate。item模型覆盖GUI、地面、固定展示、头部、左右手及第一/第三人称变换。
- 模型顶点以1方块为1.0，全部限定在本格0..1。OBJ只输出三角形和四边形；末端五边形裁切拆为三角面。每个包装JSON含必需的`textures.particle`。锁定的NeoForge 21.1.219 `neoforge:obj` loader与MTL相对路径按其源码格式使用。
- 成型机壳按`ring_role × axial_role`生成同一套前/中/后段。端盖仅在`front z=0`和`rear z=1`出现，中段无轴向封盖。控制器有0.08格厚前面板和缩进机身；后轴增加带方孔的1.92像素端板，轴在孔中贯通。进汽口、排汽口均有围绕管孔的方形安装板和圆管；橙色/浅蓝端面分别区分入口、排汽。
- 下部中央机壳格与左右下角机壳主体底边均抬至本格`y=3/16`；左右下角各有一条宽3.2/16格、高3/16格、贯穿分段的真实支座。端层支座端面并入端盖材料；相邻中段只在边界接触，不加重叠端盖。
- 六份正式配方放入`data/create_nuclear_industry/recipe/`。控制器使用5×5动力合成图样，空格以空格字符表示，不把说明用的`.`写入recipe；实际21格计数为S12、R4、P2、I2、A1。六个方块都有自掉落表。控制器便携库存由C的`getDrops()`写入`CniTurbine`自定义数据，loot table不再重复写入库存或给普通部件复制库存。
- 已在既有`minecraft:mineable/pickaxe`与`needs_iron_tool`标签中追加六个ID，保留了原条目。`zh_cn.json`、`en_us.json`仅追加汽轮机与普通蒸汽键，包含`state.overlap`、`issue.overlap`、`hint`及纠正后的`wait_stock`。新增普通蒸汽still/flow的2张PNG和2张可复现SVG源稿；超临界蒸汽纹理没有被覆盖。

## 冻结几何合同

局部机器北向基准为`+x=东、+y=上、+z=南`；`front z=0`、`rear z=L-1`。状态通过`machine_facing`绕Y轴旋转0/90/180/270度；端口模型由`ring_role`和机身方向选择，`outward`保留为实际连接方向，不改变网格。

每格占位坐标为x/y/z=0..16像素。机壳ring映射为：top `(x=16..32,y=32..48)`、bottom `(16..32,0..16)`、left `(0..16,16..32)`、right `(32..48,16..32)`；upper_left `(0..16,32..48)`、upper_right `(32..48,32..48)`、lower_left `(0..16,0..16)`、lower_right `(32..48,0..16)`。斜角格不能按实心整格碰撞：upper_left保留`y−x≤8`，upper_right保留`x+y≤24`；lower_left主体为`y≥max(3,8−x)`，lower_right主体为`y≥max(3,x−8)`。等号是45度斜面。bottom主体为`y=3..16`，其余top/left/right主体保留完整单格可见包络。

两条脚分别是lower_left格`x=10.4..13.6、y=0..3、z=0..16`与lower_right格`x=2.4..5.6、y=0..3、z=0..16`；与主体碰撞在y=3相接但不重叠。C的碰撞实现可将切角按阶梯近似，但须保留脚与主体分离的占位并避免整格隐形阻挡。

中心件与管口的精确占位供C调整碰撞时使用：

| 构件 | 本格局部占位 |
| :--- | :--- |
| rotor_middle | 轴心(8,8)，半径6.72，z=0.64..15.36；16边圆柱 |
| controller_front | 前板x/y=0..16、z=0..1.28；后壳x/y=1.92..14.08、z=1.28..6.4 |
| output_shaft_rear | 轴心(8,8)、半径3.04；z=1.6..16；后端板z=14.08..16，孔x/y=4..12 |
| inlet left / exhaust left | 沿本地−X，r=5.12、x=0..16；内端板x=14.72..16，方孔两横轴2.4..13.6 |
| inlet/exhaust right | 沿本地+X，r=5.12、x=0..16；内端板x=0..1.28，方孔两横轴2.4..13.6 |
| inlet/exhaust top | 沿本地+Y，r=5.12、y=0..16；内端板y=0..1.28，方孔x/z=2.4..13.6 |

中心件边界按可见薄板、转子、轴或管体提供碰撞形状，不使用整格碰撞代替网格。四向旋转由结构的`machine_facing`处理；如C的最终分件碰撞对以上静态网格做了保守近似，需在其实现记录中明确。

## 验证与待办边界

使用捆绑 Python 3.12 (`-B`)生成并安装两类资源，再运行静态检查：

- `turbine_models.py`：39模型包装JSON、39 OBJ、6 blockstate、6 item模型；各state引用均有模型文件，模型粒子纹理存在，全部OBJ顶点在0..1，8种物品显示上下文齐全。blockstate变体为机壳192、控制器/转子/后轴各8、进汽和排汽各320；端口8值ring enum均有安全模型映射。
- `turbine_data.py`：六recipe、六loot、两tag、两语言文件与普通蒸汽两张纹理均解析通过；机械合成的空格、21格及材料数量断言通过。生成快照与`src/main/resources`安装内容逐文件字节相同。
- `turbine-model-mesh-preview.png`是从生成的OBJ面按5/8/11段拼接的离线预览；`turbine-assets-preview.png`仍是SVG样张和方案比例示意。二者都不是Minecraft客户端截图。

本任务没有运行Gradle、服务端或客户端。C应在其获准的服务端回归窗口校验配方/注册与资源打包；客户端blockstate/OBJ模型烘焙及三档、四向、物品/手持和真实灯光纹理效果留待客户端检查。若这些检查要求改动已冻结资源，先由PM安排资源窗口；本任务不在客户端验证前宣称可视效果通过。

实际读取并应用的技能入口：minecraft-modding（C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md）、minecraft-testing（C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md）、minecraft-resource-pack（C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md）。
