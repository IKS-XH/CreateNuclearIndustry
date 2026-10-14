# EXT-B-EXCHANGER-01D V 定向接口外观交付

**基线：** `9e8690bd6e271202c47a04580c4093f7c7a7e7e4`
**范围：** 仅执行01D任务卡V美术写集：换热器模型、纹理、方块状态、生成器、SVG源稿、允许范围内的`COMPONENTS.md`和本报告/01D证据目录。

## 交付结果

- 基座保持Y=0..12、鳍片保持Y=12..16；北面（模型`north`，对应`FACING`）新增蓝色冷液出口面板及向外箭头，南面新增橙色热液入口面板及向内箭头。
- 左右侧及底部保持完整暗钢封闭表面，不再出现可接管的管口、法兰、凹槽或铜芯。方向面板使用外表面纹理，不增添厚度或突出接口几何。
- 保留独立基座、鳍片和槽内热态提示模型层、方块四朝向0/90/180/270度旋转，以及原静态物品组合和第一人称0.5、第三人称0.4缩放。
- `generate.py`现在重建方向纹理、模型、SVG稿和冷热/方向预览；证据固定写入`EXT-B-EXCHANGER-01D-ART/`，不覆盖01A/01B历史。
- 组件映射和复现命令已更新在`tools/art-assets/heat-exchanger-device/COMPONENTS.md`。未改Java、语言、配方或锅炉资源。

## 对照预览

01B历史等距冷态预览（保留旧四侧管口外观）：[heat-exchanger-preview.png](EXT-B-EXCHANGER-01B-ART/heat-exchanger-preview.png)。

01D新模型冷态与顶部结构：[heat-exchanger-preview.png](EXT-B-EXCHANGER-01D-ART/heat-exchanger-preview.png)；热态鳍片分层：[heat-exchanger-hot-preview.png](EXT-B-EXCHANGER-01D-ART/heat-exchanger-hot-preview.png)。

北侧蓝色外向与南侧橙色内向方向面板并列图：[directional-interfaces-preview.png](EXT-B-EXCHANGER-01D-ART/directional-interfaces-preview.png)。模型北/南面由现有`facing` multipart旋转，因此方块世界模型与静态物品组合均引用同一对方向纹理；物品尺寸沿用01B审定值。图像是资源级静态预览，不能替代游戏内四朝向及手持客户端检查。

## 静态验证

运行任务指定Python命令：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/heat-exchanger-device/generate.py
```

命令退出码为0。`resource-validation.json`记录检查结果：37个模型元素及资源引用已解析；14张纹理为不透明16×16 PNG；基座/鳍片范围分别为Y=0..12和Y=12..16，7条鳍片与3条热态槽内提示均在边界内；部件体素相交数和同法线共面正面积重叠数均为0；四个朝向旋转及手持缩放符合预期；方向面板映射为北冷出、南热入、东西及底面封闭钢面。

本次没有运行Gradle、资源打包、JUnit、GameTest或客户端。图像方向和物品展示仍待任务卡规定的客户端人工检查；本报告只记录V静态素材交付，不代表01D整体验收。

## 使用技能

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：按仓库Minecraft 1.21.1 / NeoForge 21.1.219 / Create 6.0.10-280资产组织实施。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：依改动范围采用资源生成器静态检查；没有运行无关Java测试。
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：使用现有1.21.1方块状态、显式面UV、PNG纹理和item静态父模型，不套用新版本物品模型格式。
