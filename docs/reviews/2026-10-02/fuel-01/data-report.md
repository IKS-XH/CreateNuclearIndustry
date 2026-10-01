# EXT-A-FUEL-01 任务2：原生配方与标签交付

- 基线：`codex/ore-acquisition`，`a4f36e7c02a3f43f7bef522ff99d2afcde56ed33`。
- 技术基线：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。未升级依赖。
- 技能：已读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`，核对 1.21.x singular `recipe`、`loot_table`、`tags/item` 和 Minecraft 配方/标签资源路径；已读取 `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`，按任务限定只做静态资源检查，没有写镜像测试或运行游戏。

## 交付文件

- `src/main/resources/data/create_nuclear_industry/recipe/splashing/crushed_raw_uranium.json`：以 `c:crushed_raw_materials/uranium` 为输入，固定产出 1 铀精矿和 1 铀尾矿。
- `src/main/resources/data/create_nuclear_industry/recipe/pressing/uranium_tailings_brick.json`：以专用铀尾矿标签为输入，产出 1 铀尾矿砖。
- `src/main/resources/data/create_nuclear_industry/recipe/mixing/uranium_slurry.json`：消耗 8 铀精矿和 1000 mB `minecraft:water`，普通加热（`heated`）、`processing_time=200`，产出 1000 mB 铀料浆。
- `src/main/resources/data/create_nuclear_industry/recipe/mechanical_crafting/enrichment_centrifuge.json`：锁定 5×5 图样且禁用镜像，材料计数 S=10、G=6、B=2、I=1、W=1、P=1，总计21；钢/金使用 `c:plates/steel` 和 `c:plates/gold`，W 为 `create:whisk`，无动力搅拌器或铜外壳。
- 四个专用标签：`src/main/resources/data/create_nuclear_industry/tags/item/nuclear_materials/{uranium_concentrate,uranium_tailings,low_enriched_uranium_dust,depleted_uranium_dust}.json`。每个标签仅含对应的本模组物品，没有把核粉加入通用粉末标签。
- `src/main/resources/data/create_nuclear_industry/loot_table/blocks/uranium_tailings_brick.json`：常规挖掘掉落自身物品，并保留爆炸存活条件。
- `src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json`：仅追加离心机与尾矿砖。
- `src/main/resources/data/minecraft/tags/block/needs_iron_tool.json`：仅追加离心机；尾矿砖保留普通镐要求。

## 核查结果

- 对上述 11 个资源 JSON 执行 PowerShell `ConvertFrom-Json`，全部解析成功。
- 程序化统计动力合成器图样为 21 个占用格，字符计数与配方合同一致；制浆配方有8条固体精矿输入、1000 mB水、1000 mB流体结果、`heated` 和200处理时间；未引用 `create:mechanical_mixer`。
- 与基线的两个 Minecraft 挖掘标签比较，原有成员全部保留；新增项及铁镐要求符合任务合同。`git diff --check` 对这两个已跟踪修改文件通过。
- 实际检查锁定的 Create slim JAR：`C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280/92471e8fc5ed4e3c2279001d77ebcec6fd38bcab/create-1.21.1-6.0.10-280-slim.jar`，SHA-256 `F0652BEE27460F2D26A748F537CB5D687981441378D3A526D17FFAAB5A1072BB`。其中 `data/create/recipe/mechanical_crafting/crushing_wheel.json` 确认图样使用 `pattern`、`key`、`result`、`create:mechanical_crafting`；`data/c/tags/item/plates/gold.json` 确认金板通用标签包含 `create:golden_sheet`。Create 6.0.10-280 的 `ProcessingRecipeParams` 源码以单条 `Ingredient` 表示单件输入，故8份精矿写成8条相同标签输入；流体结构采用该 JAR 实际存在的 `neoforge:single` 格式。
- JAR 内另有三条按模组加载条件启用的水洗配方，分别位于 Create 对 Immersive Engineering、Mekanism、IC2 的兼容目录，均处理 `create:crushed_raw_uranium`。本项目当前 `build.gradle`、`gradle.properties` 和 `neoforge.mods.toml` 未声明这些模组；按本批默认依赖配置判断条件未启用。若后续启用它们，同一原料会存在配方竞争。本任务未修改这些范围外的旧兼容配方，也不据此声明第三方组合已支持。

## 验证限制与交接

未运行 Gradle、游戏、配方加载器或客户端；本报告证明 JSON 语法、配方结构/数量和标签追加的静态结果，不证明运行时注册、游戏内加工或掉落体验。离心机配方与四个标签路径现已稳定，可由运行时执行者继续做整合验证；方块注册 ID 已与其确认一致。离心机自身加工配方及其状态掉落表由运行时执行者负责。
