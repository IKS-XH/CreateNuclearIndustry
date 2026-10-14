# EXT-B-TURBINE-01B 资源执行报告

## 交付范围

本批只处理汽轮机资源、SVG/PNG 纹理、观察窗配方与语言资源，以及本报告和离线预览。模型/方块状态按 `EXT-B-TURBINE-01B-RUNTIME-interface.md` 生成。没有修改 Java、核心文档、六条既有汽轮机配方或蒸汽流体纹理；旧日志与 `__pycache__` 保留。

- 新增 13 张 16×16 RGBA 像素纹理的 SVG 源稿/更新稿与运行时 PNG；钢灰为主体、黄铜收敛用于轴承/连接环，另含薄壳面、叶片金属、轮毂金属和透明观察玻璃材质。
- 生成并安装 243 个 OBJ 模型包装 JSON 与 243 个 OBJ 网格，覆盖 206 个壳片、12 个直边观察窗、3 档转子叶片、轴承轴心、前后轴、四向侧控与进排汽部件，以及单件未成型模型。`turbine_window` 的 12 个成型模型和未成型回退模型均使用 `render_type: translucent`。
- 进汽口与排汽口各生成 384 个状态组合，覆盖六个 `outward` 方向；成型状态只在 `ring_role`、`machine_facing` 与实际外向面吻合时选择四向模型，非法组合回退未成型模型。
- 转子网格为 12 片宽弦、带轻微轴向倾角的叶片与空心轮毂，不含重复轴。轴线为局部 +Z，旋转中心 `(0.5, 0.5, 0.5)`；D3/D5/D7 半径分别为 1.1875/2.1875/3.1875 格，Z 范围 0.13–0.87。前后输出轴含 3/16 格承轴端板与轴承座，轴身完全位于本方块 0..1 格内，轴端平接邻接 Create 轴。
- 新增观察窗工作台配方、掉落表、工具标签条目及中英文语言键。语言合并保留运行时执行者已写入的诊断文案；`turbine_output_shaft` 中文名为“汽轮机输出轴”。
- 三张最终预览：[`turbine-assets-preview.png`](EXT-B-TURBINE-01B-ASSETS/turbine-assets-preview.png) 展示三档真实 OBJ 机身与剖面；[`turbine-01b-parts-preview.png`](EXT-B-TURBINE-01B-ASSETS/turbine-01b-parts-preview.png) 展示单件部件并按视口边界适配；[`turbine-01b-texture-sheet-3x3.png`](EXT-B-TURBINE-01B-ASSETS/turbine-01b-texture-sheet-3x3.png) 直接读取九张运行时纹理 PNG。几何图使用平面材质色检查网格和占位，最终图集采样、光照及客户端表现仍由客户端手测确认。

## 静态验证

- 13 个 SVG 纹理解析与 16×16 导出检查通过。
- OBJ/JSON 生成器静态检查通过：206 piece 映射及各档 front/middle/rear 数量为 9/8/9、25/16/25、45/24/45；完整状态变体、模型引用、粒子纹理和物品展示项齐全；普通单格网格均在 0..1 范围。
- 12 个观察窗模型透明层检查通过；三个叶轮半径、局部轴心与 Z 范围符合接口；前后输出轴顶点均在单格范围内。
- 进汽口/排汽口各 384 个状态及底向端口模型选择检查通过；`outward` 不符 `ring_role` 的组合回退检查通过。
- 配方/掉落表/标签/语言目标 JSON 解析通过。六条既有配方 SHA-256 与写入前记录一致：`turbine_casing` D801167D、`turbine_rotor` 8941301E、`turbine_controller` 4C7878E3、`turbine_output_shaft` 7BA33F01、`turbine_inlet` 2603E8D9、`turbine_exhaust` D65CDED5。
- `git diff --check` 无空白错误；输出的 CRLF 提醒来自候选工作树内其他已修改文件。
- 未运行 Gradle 构建或 Minecraft 客户端；运行时 GameTest 由执行者 A 单独验证。本批预览为离线几何/纹理证据，不视为客户端验收。

## 技能

按任务要求读取并应用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`；按实施流程读取 `superpowers:brainstorming` 与 `superpowers:executing-plans`。本资源批没有新增 Java 测试；Minecraft 技能的示例版本未用于改变项目技术栈。
