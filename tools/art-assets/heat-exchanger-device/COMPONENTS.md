# 核换热器模型部件与枢轴

模型使用Minecraft方块单位，原点位于方块西北下角 `(0, 0, 0)`；X向东、Y向上、Z向南。所有盒体与UV都限定在`0..16`。水平朝向只绕方块中心`(8, 8, 8)`旋转Y轴。

| 部件 | 模型 | 用途与位置 |
| :--- | :--- | :--- |
| 承力外壳 | `shell.json` | 四角立柱、十二条棱梁与六面钢框；每个窗框中央保留凹入窗口 |
| 静态热芯 | `core.json` | 封闭暗色衬板、铜色方形盘管、顶部铜热扩散板；默认方块与物品显示该层 |
| 热态提示 | `core_lit.json` | 只含顶部三条橙色鳍片和浅黄色提示镜；由`lit=true`附加，不重复绘制热芯 |
| 方块状态 | `assets/.../blockstates/nuclear_heat_exchanger.json` | `facing=north/east/south/west`控制Y轴旋转；`lit=false/true`只控制热态提示外观 |
| 物品展示 | `assets/.../models/item/nuclear_heat_exchanger.json` | 引用`item_static.json`完整静态组装；手持、地面和GUI变换在物品根模型单独定义 |

顶面窗口中的铜扩散板、热鳍片和提示镜最高到`Y=16`，与上方锅炉底面齐平，不伸入相邻方块。外围钢框同样到`Y=16`，热面位于框内中央；连接锅炉后从上方观察时热面被相邻锅炉遮挡属于正常接触表现。

承力外壳和热芯由独立方盒组装。各部件立方体内部没有体积相交；相邻表面只接触。六面开窗在内部以热芯衬板封闭，模型不形成贯通空洞。模型坐标和UV不超出单位格，结构纹理完全不透明。

可编辑矢量源稿位于`sources/`。修改造型时编辑本目录的`generate.py`和SVG源稿，再用工作区Python运行：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/heat-exchanger-device/generate.py
```

`generated/`是可重建导出物；游戏实际引用的PNG、blockstate与模型位于`src/main/resources/assets/create_nuclear_industry/`。
