# 核换热器模型部件与枢轴

模型使用Minecraft方块单位，原点位于方块西北下角`(0,0,0)`；X向东、Y向上、Z向南。所有盒体坐标和显式UV均限定在`0..16`。水平朝向只绕方块中心`(8,8,8)`旋转Y轴，不改变基座/鳍片的高度分层。

| 部件 | 模型 | 用途与位置 |
| :--- | :--- | :--- |
| 深灰方形基座 | `base.json` | 完整覆盖单格底部，Y=0..12；北面中央带蓝色冷液出口与向外箭头，南面中央带橙色热液入口与向内箭头 |
| 铜散热鳍片 | `fins.json` | 七条独立平行长片，沿Z轴布置于Y=12..16；鳍片间是真实开放空气槽，不依赖纹理伪造凹槽 |
| 热态槽内提示件 | `fins_lit.json` | 三条短橙色提示条位于鳍片槽内，Y=14..16；不覆盖铜鳍片 |
| 冷热状态与朝向 | `assets/.../blockstates/nuclear_heat_exchanger.json` | 模型北面为`FACING`侧冷液出口，南面为背侧热液入口；`facing=north/east/south/west`分别绕Y旋转0/90/180/270度；`lit=true`附加槽内提示层 |
| 静态物品组合 | `item_static.json` | 将基座与鳍片合并，省略状态提示件；作为完整物品模型父模型 |
| 物品展示变换 | `assets/.../models/item/nuclear_heat_exchanger.json` | 定义第一/第三人称手持、GUI、地面和展示缩放，不改变方块内模型坐标 |

基座上表面止于Y=12，鳍片从Y=12开始，整体最高到Y=16；不伸入相邻方块。鳍片与基座接触，槽内提示条处于独立空槽，三者体积互不相交。只有北/南端面有方向面板；东/西侧与底面维持封闭暗钢外观。方向面板只是视觉标记，不改变Java碰撞盒或流体逻辑接口。

导出器检查模型纹理引用、面UV/坐标界限、部件体积相交，以及同法线同平面的面片正面积重叠；最后一项为零时，可排除相同朝向面片重叠导致的共面闪烁。客户端渲染结果仍需人工检查。

可编辑SVG源稿位于`sources/`。修改造型时编辑本目录的`generate.py`和SVG源稿，再用工作区Python运行：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/heat-exchanger-device/generate.py
```

`generated/`是可重建导出物；游戏实际引用的PNG、blockstate与模型位于`src/main/resources/assets/create_nuclear_industry/`。运行生成器会输出冷/热静态等距预览、端面方向预览与资源静态检查摘要到`build/reports/extension/EXT-B-EXCHANGER-01D-ART/`；不会覆盖01A/01B历史证据。
