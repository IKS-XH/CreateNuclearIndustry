# ART-REACTOR-03A：内部模型与SVG流动素材

> 执行者按superpowers:subagent-driven-development/task-by-task执行本卡；只有美术负责人维护计划状态。无Git写/子代理/客户端启动/主工程写权，禁止修改正式资源、Java、共享工具或冻结02R1。

**Goal：** 为用户明确要求的反应堆运行表现交付方形顶底板＋多边竖管燃料架、独立控制棒和冷/热SVG流动素材的可审查候选。

**Architecture：** 静态OBJ、辉光管身partial、单位控制棒杆身/端部及两组8帧流动纹样分别可编辑。全部候选生成到新增独立工具目录；动态数值在预览中明确为示意，正式运行接口另交PM。

**Tech Stack：** MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，已有NeoForge OBJ和共享严格SVG renderer，捆绑Python，无依赖安装。

**Spec：** [03设计](../ART-REACTOR-03-DESIGN.md)，用户已确认按实际冷/热库存连续混合渐变。

## 全局约束与基线

- 唯一workdir`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，每条shell显式设置，HEAD`8b83a1a97f1b539e6dcc7b6714b9ee96c13854e0`；保留所有既有未提交ART01/02A/02B/02R1输入/输出和存档。
- 实际读AGENTS、治理1.2/5.1、美术入口、设计/本卡；应用已读minecraft-modding/testing/resource-pack与相关建模/验证技能，中文手写注释/Javadoc。资源本批无Java行为，不增加逻辑JUnit/服务器/构建测试。
- 根美术负责人只设计/派发/审查，你负责素材实现。只写本卡名单，不修改活动计划/核心治理或任何共享Java/注册/碰撞/逻辑/同步/全局流体纹理。
- 新独立低分辨率PNG只能由保存的SVG渲染导出，允许从已渲染候选拼合/投影预览；禁止图片生成器、PNG直接重绘/改色、复制外部Mod资产。

## 精确写集与消费产物

仅新增`tools/art-assets/reactor-animation/`下：

| 路径 | 职责 |
| :--- | :--- |
| `sources/fuel_rod_steel.svg`、`fuel_rod_glow.svg`、`control_rod.svg` | 各16×16整数rect材质，RGBA≤16色，使用共用严格renderer |
| `sources/coolant_cold/frame_00.svg`..`frame_07.svg`及`coolant_hot/`同名8帧 | 各16×16整数rect；色彩/波纹连续，透明由消费顶点alpha控制，source alpha0/255 |
| `palette.json`、`rig.json`、`mapping.json`、`generate.py`、`README.md` | 色板/几何尺寸/枢轴/材质引用/帧表、确定性生成与精确消费说明 |
| `generated/models/` | `reactor_fuel_rod.json`及mesh OBJ/MTL；`fuel_rod_glow.json`、`control_rod_shaft.json`、`control_rod_head.json`及相应mesh OBJ/MTL |
| `generated/textures/` | 三个模型材质PNG；两组8帧独立图块及16×128帧表PNG（帧元数据记录于mapping） |

正式消费ID：静态模型仍`create_nuclear_industry:block/reactor_fuel_rod`；新增partial为`create_nuclear_industry:block/reactor_animation/fuel_rod_glow`、`control_rod_shaft`、`control_rod_head`，材质目标`create_nuclear_industry:block/reactor_animation/<name>`。OBJ/MTL引用随以后安装的资源路径生成，不使用工具目录绝对路径。默认只生成工具内候选，不安装`src/main/resources`。

允许唯一报告`docs/art/reports/ART-REACTOR-03A.md`及证据/预览`build/reports/art/ART-REACTOR-03A/`；不改其他目录、旧报告/冻结证据或共享SVG工具。源与候选格式调整仅限本名单；遇到真实API问题先报告负责人，不能扩展为逻辑实现。

## 几何、材质与预览合同

1. 燃料架每模块方块边界0..16：底板Y0..1、顶板Y15..16，X/Z0..16；九根管中心X/Z4/8/12，八边外径2.5，Y1..15。实体钢板/格栅色，竖管棱面可见，堆叠模块接头封闭；蓝辉管身独立且不覆盖顶底板，每管径向偏置0.0005方块直接烘焙进辉光mesh，轴心不变，消费者不二次缩放。OBJ以方块为单位，法线/绕序/UV正确，不额外画两侧重复面。
2. 控制棒轴X/Z=8、八边外径3；独立单位长度杆身及小端部，钢芯/吸收陶瓷/配方支持的黄铜箍件。shaft Y0..1方块规范化用于未来真实行程；端部尺寸固定，rig写明消费如何整体平移而不是按深度缩短。驱动器壳体/CT不改。
3. 冷液蓝青、热液参考原热复合冷却液暖色，各8帧低对比流动纹样；16×128 sheet逐帧16px，边界可平铺，帧00→07→00过渡自然。混合连续渐变、无上下层硬分界，0液不显示；控制透明度让9管与控制棒仍看得清。
4. 预览实际投影生成的OBJ/UV，包含单模块/3格堆叠的正侧顶，蓝辉0/低/中/高；控制棒0/50/100%同一驱动器示意；液位0/50/100%及热占比0/50/100%通过窗口观察。标明功率/深度/库存为示意输入，未与游戏运行接通。可用离线HTML循环展示8帧，放在本批证据目录；不另起站点/服务/浏览器。

## 执行步骤与必要验证

- [x] 只读核对原模型/材质/配方、依赖实际OBJ/partial模式，保存本批确切旧输入hash与02R1 JAR标识，保护基线不重复全树944矩阵。
- [x] 创作上述19份SVG、rig/色板/映射和可复现生成器，保留作者源；输出候选OBJ/MTL/JSON及图块/帧表。
- [x] 定向检查16px、颜色/alpha与引用；mesh顶点/面法线/绕序/UV/边界/分件位置，两个板/九个八边管、单位杆体/端部；合法输入整批验证后才写出，非法源或mapping写前拒绝。只覆盖实际新增生成器的不变量，不机械复制旧工具负例。
- [x] 一次重复生成字节一致，明细记录源/产物/引用哈希；无需构建，因为未安装正式资源。
- [x] 自行实际打开生成的模型/动画预览，确认闭合、不黑孔、不悬浮、不伸缩杆、不遮住内部构件；提交负责人实际看图并定向整改。预览达到目标后冻结，消费任务再安装和最终打包。
- [x] 交未提交资产与报告：真实命令/退出码、技能使用、原输入保护、源码/候选清单、模型单位/枢轴/帧表、冻结及未验证客户端项。停本段，不自行开始Java/共享接口/正式安装或Git集成。

素材段已交并由负责人核对：19SVG、33候选，四主图与实际GIF解码联系页已看；58交付＋35证据共93冻结哈希一致。[实施报告](../reports/ART-REACTOR-03A.md)。此状态只关闭本素材段，正式运行接入及用户客户端视觉门另记。

## 审查关注点

- 八边管不是贴在完整cube上的假竖线，顶底/棱面绕序与堆叠闭合需实际看图。
- 辉光强弱不同且保留钢材层次，离线输入不能变成假实时状态。
- 控制杆整体上下移动，固定端部不过度拉长；原驱动壳/CT仍只读。
- 8帧循环首尾及平铺连续；混合透明能辨杆体，不改全局冷却液图。
- 全新目录的复现/拒绝只覆盖本批，不改既有候选、存档或冻结证据。
