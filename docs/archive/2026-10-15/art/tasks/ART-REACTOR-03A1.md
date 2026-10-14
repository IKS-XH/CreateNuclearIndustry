# ART-REACTOR-03A1：燃料架物品展示继承

2026-10-10，美术负责人定向补充。仅纠正新静态OBJ包装缺少原版物品展示变换的问题，属于已要求的燃料架建模；不扩展运行接入或玩法。

## 已核实的问题与输出

原item模型只继承`create_nuclear_industry:block/reactor_fuel_rod`。03A静态OBJ包装无parent/display，锁定MC1.21.1 `BlockModel.getTransform`在无parent/本地变换时使用NO_TRANSFORM。原版`minecraft:block/block`资源实际含GUI `[30,225,0]`旋转及`.625`比例、第一人称`.4`和第三人称`.375`变换。静态燃料模型应继承该parent，保留原版背包/手持角度和比例；三个动画partial继续无parent。模型世界几何、SVG纹理与配方不变。

证据来自本地锁定NeoForm源JAR与`C:/Users/IKSXH/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar`，不以工具斜视预览代替item变换合同。

## 唯一写集与保护

工作区`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，每条shell显式workdir。实际读取03设计/03A/本卡及项目规则，复用已读Minecraft技能，中文说明。无Git/Java/正式资源安装/构建/客户端或子Agent。

- 修改`tools/art-assets/reactor-animation/generate.py`：仅静态`reactor_fuel_rod`包装加入`parent: minecraft:block/block`；生成路径仍按原合同。
- 修改该生成器输出`tools/art-assets/reactor-animation/generated/models/reactor_fuel_rod.json`；其他32候选及全部SVG/rig/mapping/README只读。
- 新报告仅`docs/art/reports/ART-REACTOR-03A1.md`。
- 原始记录/旧两文件副本/模型展示投影仅`build/reports/art/ART-REACTOR-03A1/`。

原03A报告与冻结清单/证据不能重写。修改前在新证据目录保存上述两文件原字节及原freeze SHA，核对原93项。修改后明确只有该两项授权漂移，其余91项保持；新冻结绑定当前两文件及新报告/证据，并关联历史03A。03A全部几何/纹理验证复用，不再重跑导出负例、19源检查或全量重复生成。

## 必要定向步骤

1. 保存旧两文件，记录真实默认item展示缺失。修改生成器后仅一次生成，核对33候选中恰好静态包装改变，其余32字节/mtime保持。
2. 核对仅静态包装继承`minecraft:block/block`，三partial无parent、材质/OBJ/particle引用不变；将实际OBJ按锁定原版GUI变换作一个离线投影，与默认正面视图对照并实际打开。此为模型展示预检，手持最终观感留客户端。
3. 核对原冻结的91项及两份旧副本，保留原候选链；写报告、只读冻结，交负责人。不生成正式资源，不接Java，不另开完整审查。
