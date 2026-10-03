# EXT-B-EXCHANGER-01A ART 资源交付

**执行状态：** ART执行者交付，等待项目经理审查；本报告不代表游戏内视觉验收。

候选工作树`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，基线`3f64678263151f418112a5a7cb6d0c6d03a74743`。本任务为ART分工，仅新增换热器方块外观、物品模型、独立SVG源稿、确定性生成器与部件说明。视觉由蓝灰钢制承力框、凹入式铜盘管窗口、黄铜接头点缀和顶部铜热扩散面构成。外壳、热芯、热态提示拆成三个模型层，方块状态静态组合；没有加入动画渲染。

预览使用离线矢量几何投影生成，供人工审阅造型与层次，不是Minecraft运行画面。静态与`lit=true`外观分别见[静态预览](EXT-B-EXCHANGER-01A-ART/heat-exchanger-preview.png)和[热态预览](EXT-B-EXCHANGER-01A-ART/heat-exchanger-lit-preview.png)。项目经理已查看两张预览并确认蓝灰框体与铜热芯方向可保留；Minecraft客户端实际显示仍待后续人工验收。

## 实现与检查

- `blockstates/nuclear_heat_exchanger.json`覆盖`facing=north/east/south/west`与`lit=false/true`。每个朝向旋转外壳、热芯；热态只增加独立鳍片与提示镜，不重复基础几何。`lit`只是本机视觉状态，不表达Create热级。
- `models/block/nuclear_heat_exchanger/{shell,core,core_lit,item_static}.json`分别提供框体、基础热芯、热态提示和完整物品静态组装。物品根模型引用完整静态组装，并显式设置GUI、地面、固定展示和左右手变换；手持缩放为`0.42`，GUI为`0.55`。
- `textures/block/nuclear_heat_exchanger/`下10张不透明16×16 RGBA PNG供模型引用。UV使用`[0,0,16,16]`，所有部件端点和面UV均处于`0..16`。
- 顶部铜扩散板、热鳍片与提示镜最高至`Y=16`，不越入上方锅炉方块。框体顶部环梁围绕中央接触面；锅炉安装后从上方遮住热面是贴合关系的正常表现。
- 生成器对所有分件逐对检查体积相交，结果为0；沿X/Y/Z分别检查全部`3×16×16=768`条穿过模型的轴向线均碰到实体，未发现贯通空洞。接触边界仅相邻，不存在体积重叠，因此模型几何没有共面穿插导致的z-fighting风险。
- 生成器检查blockstate全部模型路径、物品父模型、模型纹理路径、所有盒体坐标、所有面UV、纹理尺寸和不透明度，结果记录在[资源检查](EXT-B-EXCHANGER-01A-ART/resource-validation.json)。
- 使用随附Python依赖运行`python.exe -B tools/art-assets/heat-exchanger-device/generate.py`，退出码0；未启动Gradle、Minecraft客户端或测试任务。`-B`避免写入Python缓存。

## 修改写集

- `src/main/resources/assets/create_nuclear_industry/blockstates/nuclear_heat_exchanger.json`
- `src/main/resources/assets/create_nuclear_industry/models/block/nuclear_heat_exchanger/`下`shell.json`、`core.json`、`core_lit.json`、`item_static.json`
- `src/main/resources/assets/create_nuclear_industry/models/item/nuclear_heat_exchanger.json`
- `src/main/resources/assets/create_nuclear_industry/textures/block/nuclear_heat_exchanger/`下10张PNG
- `tools/art-assets/heat-exchanger-device/`下生成器、部件/枢轴说明、两份SVG源稿和可重建导出物
- 本报告及同名证据目录

**技能实际应用：** `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`用于核对NeoForge 1.21.1的`assets/<modid>/blockstates`、模型与物品资源位置，以及方块状态模型引用；`minecraft-testing/SKILL.md`用于确认ART任务不新增JUnit/GameTest，本次只检查模型合同与资源引用，不启动客户端或测试服务器；`minecraft-resource-pack/SKILL.md`用于采用1.21.x模型/blockstate结构、16×16不透明纹理并核对UV与纹理路径。项目基线为Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82；本次没有涉及模组运行代码。
