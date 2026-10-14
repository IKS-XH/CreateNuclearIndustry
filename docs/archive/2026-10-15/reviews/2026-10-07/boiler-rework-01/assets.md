# EXT-B-BOILER-REWORK-01 素材交付记录

本次仅新增高压锅炉热液入口 `high_pressure_boiler_hot_coolant_port` 与冷液出口 `high_pressure_boiler_cold_coolant_port` 的静态资源。接口面沿用现有锅炉钢壳纹理的灰钢、黄铜角钉和同规格内嵌面板；热口用橙色并以向内箭头标识，冷口用蓝色并以向外箭头标识。

新增游戏资源各四项：两份 `blockstates`、两份完整方块模型、两份继承方块模型的物品模型、两张16×16 RGBA纹理。模型为完整16³立方体，北面是接口面，其余五面使用既有锅炉壳侧纹理。方块状态覆盖 north/east/south/west，分别旋转0/90/180/270度；物品继续使用项目现有父模型显示变换方式，没有额外放大。

SVG源稿、冷热独立色板、生成清单和复现入口位于 `tools/art-assets/boiler-rework-01/`。复现命令：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' tools/art-assets/boiler-rework-01/export_boiler_ports.py
```

入口调用项目受限SVG渲染函数 `tools/art-assets/export.py::render_svg`，只写本任务两种端口的纹理、方块状态、模型和素材工具目录；不运行原锅炉批次导出入口，不覆盖旧素材或通用工具。预览文件为 `build/reports/extension/EXT-B-BOILER-REWORK-01/coolant-port-preview.png`。

已实际读取并应用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack` 技能。技能中的示例面向1.21.x；本仓库仍为Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82，因此按现有1.21.1模型目录和旧式 `models/item` 父模型格式制作。本任务是静态资产，没有运行Gradle、客户端或游戏测试。

静态检查结果：两种方块JSON均可解析；四向状态名和旋转值齐全；模型覆盖六个面且方块边界为0至16；north引用各自前贴图，其余五面引用现有 `high_pressure_boiler/casing_side`；物品模型父级正确；每张PNG均为16×16 RGBA并含各自7色板；复现脚本实际导出并生成预览。核心执行者尚需在统一候选中接入方块注册及配方/掉落等游戏数据，当前资源交付不代表客户端验收。
