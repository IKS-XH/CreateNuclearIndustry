# EXT-B-TURBINE-01C 资源交付报告

## 范围

按冻结接口生成汽轮机搭建外观资源。`located` 决定外观，资源选择器不再检查 `formed`；后者继续保留给游戏运行状态。原有 `TurbineGeometry.piece(1..206)`编号、三档尺寸和现有物品模型路径均保留。独立面编码 `0/207..211` 选用以世界面为基准预旋转的单件模型，不再叠加 `machine_facing`。

## 交付

- 更新 `tools/art-assets/turbine_models.py`：壳体与观察窗覆盖 `located`、四向机身和 `piece=0..211`，每份各1696个selector，省去 `formed` 维度；转子、控制器、端轴和蒸汽口也改为由 `located` 选择外观。
- 为壳体和透明观察窗生成五个额外独立放置面OBJ/模型JSON，顶部继续复用原模型。贴图继续引用既有汽轮机贴图，本批未增加或重绘PNG。
- 更新 `tools/art-assets/turbine_data.py`：新增中英文轴列未定位、落点阻挡、已定位档位语言键；`--languages-only` 可单独安装这些键，不触及配方、掉落、标签或纹理。
- 新增 `tools/art-assets/turbine_01c_preview.py`，按安装后的blockstate JSON和OBJ绘制搭建过程与中、大档截面预览。

## 验证

- 资源生成器通过：253个模型JSON、253个OBJ、7个物品模型；206个几何piece按01B各档/轴向区段数量不变。有效窗口模型12个，均为translucent；所有模型OBJ、粒子贴图引用和物品GUI/手持显示路径解析通过。
- 状态预览脚本解析8440个完整状态样本，确认 `formed=true/false` 不改变 `located` 外观；六向独立面不应用机身旋转。
- 六向壳板和观察窗的OBJ包围范围符合3/16格面板合同：上下面分别为Y=13/16..1及0..3/16，南北面分别为Z=13/16..1及0..3/16，东西面分别为X=13/16..1及0..3/16。
- `--languages-only --install`只写入本批两个语言文件的三个新键。正式资源已冻结，后续未再写入 `src/main/resources`。
- 未运行Gradle或Minecraft客户端；离线网格预览与状态/引用检查不能替代游戏内透明窗、手持外观和碰撞人工验收。

## 预览

- [小型轴列、半包壳、缺件、壳体几何齐全、拆件过程](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/build/reports/extension/EXT-B-TURBINE-01C-ASSETS/turbine-01c-small-assembly-preview.png)
- [D5/D7完整几何与开壳剖面](/E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition/build/reports/extension/EXT-B-TURBINE-01C-ASSETS/turbine-01c-tier-section-preview.png)
- 状态检查详情：`preview-state-check.json`；生成摘要：`model-generation.json`、`language-generation.json`。
