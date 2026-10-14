# EXT 启动实现卡独立审查（2026-09-29）

**结论：未发现需修改任务卡的正确性阻塞项。** 本次仅审查 `EXT-A-ORE-01`、`EXT-ART-02` 的范围和可验证性；没有运行 Gradle、Minecraft 或修改实现。两卡仍处于执行中，本文不代表实现或客户端验收。

| 定点核对 | 结果 |
| --- | --- |
| 批准内容与版本 | 三矿参数、工具等级、9:1 粗矿压缩、现有 Create 九条粉碎配方，与 `docs/reviews/2026-09-29/README.md`、`docs/recipes.md` 一致；锁定 Minecraft 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82 / Flywheel 1.0.6，未要求升级。 |
| 工具与标签 | 矿石/粗矿块的 `mineable/pickaxe`、`needs_stone_tool`、`needs_iron_tool` 写集与卡内最低镐合同对应；实现仍须用真实挖掘和掉落验证。Create 锁定 JAR 的 `data/create/tags/item/crushed_raw_materials.json` 已列出 `create:crushed_raw_{lead,tin,uranium}`，新增 `c:crushed_raw_materials/<矿物>` 的写集足以补齐项目通用标签合同。 |
| 世界生成与去重 | 卡片明确主世界维度门，避免把 `minecraft:is_overworld` 群系标签当维度；三角峰值分别是区间中点，单矿一次 placed feature 同时处理石/深层目标可避免次数翻倍。`worldgen/` Java、入口注册、配置/放置特征及 biome modifier 的写集可容纳维度和绑定后标签判定；数据包 `auto/enabled/disabled`、重载边界及三种子实测已列为验收要求。实际实现是否正确仍须证据证明。 |
| 资源与写集 | 基线游戏纹理恰为 51 张 RGBA：47 张 16×16、四张 flow 16×64；窗口有透明中央，物品有透明边缘，其余实体块及 flow 现为不透明。三矿需要的 12 张纹理均在这 51 张中；矿卡禁止 PNG、美术卡仅准改原 51 PNG，任务间没有缺失资源写口。美术卡保留尺寸、路径、alpha 语义、源图一致性与严格拒绝非法 SVG。 |
| 验收边界 | 矿卡区分 JSON/JUnit、已加载 GameTest、真实生成/数据包与客户端人工门；美术卡区分导出一致性、逐页看图、游戏中人工门。两卡均没有将静态审查或历史 P1 客户端结论写成本批通过。 |

技能实际应用：读取本机 `minecraft-modding` 核对 NeoForge 注册/资源目录；`minecraft-world-generation` 核对 configured/placed feature 与 biome modifier 分层；`minecraft-testing` 核对 GameTest 与真实加载证据边界；`minecraft-resource-pack` 核对 PNG/模型路径和纹理格式。技能的通用示例没有覆盖或改变仓库锁定版本。

只读依据：上述两张任务卡第 10–12、16–27、29–54 行和第 10–31 行；用户批准页第 18–44 行；`docs/recipes.md` 第 72、155 行；锁定 Create 6.0.10-280 slim JAR 内的 `crushed_raw_materials.json`；当前纹理目录的路径、尺寸及 alpha 清单。主工作区原有 `.vscode/launch.json` 改动未触碰。
