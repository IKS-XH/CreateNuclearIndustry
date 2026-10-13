# 汽轮机机壳贴图三稿

本目录只提供机壳贴图设计稿，不改模型或正式游戏资源。首页`index.html`默认显示A/B/C同尺度总览；点击页首链接可分别放大方案。`overview.svg`可单独打开，`A/board.svg`、`B/board.svg`、`C/board.svg`展示单格纹理、平铺/连接边界示意与色板。

三稿共用同一5×8×5八棱外形投影：A为铜质上盖与深钢下壳，B以厚钢束带和窄铜缝分段轮舱，C以连续纵筋包住凹入铜面。候选侧面、顶面、端盖、接缝各为16×16整数像素SVG，使用crispEdges及既有钢/铜色板。颜色来自只读参考`tools/art-assets/turbine_assets.py`中的钢与铜色值；不使用黄铜亮面色，不包含旋转叶轮、轴、控制器、端口或观察窗设计。

## 只读来源

- `src/main/resources/data/create_nuclear_industry/recipe/turbine_casing.json`：机壳配方材料为钢板、铜板和强化钢板。
- `src/main/java/com/iksxh/create_nuclear_industry/turbine/TurbineGeometry.java`：实际结构提供3/5/7格八棱截面，本稿取5格截面并示意8格轴向长度。
- `tools/art-assets/turbine_assets.py`及`tools/art-assets/svg/block/turbine/turbine_casing_panel.svg`、`turbine_casing_endcap.svg`：仅作现有纹理和色值参考，未修改。

## 再现

从美术树根运行bundled Python：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/turbine-casing-concepts-07/build_concepts.py
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/turbine-casing-concepts-07/build_concepts.py --check
```

源检查只覆盖本目录的SVG尺寸/像素合法性、色板上限、A/B/C纹理结构差异、共用外形、页面入口及PNG为0。本稿未接入游戏；接缝后的外框移除仅为连接效果示意，不代表已经实现连接纹理。正式资源替换须在用户选择方案后另行授权。
