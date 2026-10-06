# STORE-01-ART 素材交付

本批完成四种物品图标、干式贮存架模型与静态状态展示，以及四条原生配方和架子掉落表。所有手绘纹理均保留 16×16 像素 SVG 源稿；导出只使用仓库现有严格 SVG 渲染器，不调用图像生成模型。

实际读取并按本仓库版本应用 `minecraft-modding`、`minecraft-testing` 和 `minecraft-resource-pack` 技能。项目为 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280；本批沿用仓库现有 1.21.1 单数目录 `recipe`、`loot_table`、`tags`。素材执行者未运行 Gradle 或游戏，构建和真实运行检查由代码执行者统一负责。

物品 `glass_dust`、`vitrification_medium`、`lead_shielding_cask`、`sealed_spent_fuel_cask` 已导出 RGBA PNG 和物品模型。空桶图标保留开放的深色桶口与金色箍带，封装桶增加蓝绿色封口，能够在物品栏中区分。既有 `lead_shielding_cask.png` 是本任务明确写集中的资源，已按本批新 SVG 更新。

`dry_storage_rack` 的方块状态资源覆盖 `facing=north/east/south/west` 与 `storage_level=0..4`。静态框架、门片和五种占用显示分别建模；一至四档逐个点亮四个可见储藏区。四格只表示占用程度，实际库存容量仍由设备逻辑决定。框架模型坐标边界为 `[0,0,0]` 至 `[16,16,16]`，覆盖一个完整方块单元。物品模型以零档方块模型为父模型，继承其标准显示行为。Java 碰撞形状由代码执行者维护，本报告只核对资源模型的外形边界。

配方分别为：1 个 `minecraft:glass` 经 Create 磨制、100 工时产出 1 个玻璃碎料；玻璃碎料、石英粉和黏土球经普通加热搅拌、100 工时产出 4 个固化基材；4 个 `c:plates/lead`、1 个 `c:plates/steel` 和 1 个 `seal_ring` 无序制作 4 个铅屏蔽桶；1 个屏蔽混凝土与 2 个钢板无序制作 1 个干式贮存架。铅板在配方 JSON 中分别占四个 ingredient；未添加与磨制相竞争的 `crushing` 配方，也未新增钢框架或封装中间件。

复用的 `c:plates/lead`、`c:plates/steel` 和 `c:dusts/quartz` 已检查实际标签成员，分别包含项目铅板、钢板和石英粉。新增专属标签为 `create_nuclear_industry:vitrification_media` 与 `create_nuclear_industry:lead_shielding_casks`；密封环继续使用其正式物品 ID。`c:dusts/glass` 已纳入 `c:dusts` 总标签；架子加入 `minecraft:mineable/pickaxe`，并新增只掉落架子物品本身的方块战利品表。

静态预览见 [store-01-static-preview.png](../../../../tools/art-assets/store-01/evidence/store-01-static-preview.png)，检查明细见 [resource-checks.json](../../../../tools/art-assets/store-01/evidence/resource-checks.json)。源稿、导出器和说明均在 `tools/art-assets/store-01/`。

运行仓库 Python 运行时的 `generate.py` 后，静态检查通过：13 个 JSON 模型、9 张纹理、4 条配方及新增/追加标签可解析；模型纹理引用可解析；所有模型坐标和 UV 均在 0–16 内且每个元素具有六个面；物品图标 alpha 只有 0/255，透明边界保留，非透明像素边界在 16×16 安全边界内；框架、门片、每个占用档自身及其组合没有实体交叠；方块状态完整覆盖 4×5 共 20 个方向/档位组合；框架模型外形边界为完整单格。已有公共标签成员、配方输入数量、工时、加热要求和产量也由导出检查脚本核实。代码执行者的 `ShieldedAssemblyStateTest.java` 与并行代码改动不属于本报告写集；候选中原有日志和 `tools/art-assets/__pycache__/` 均保留。

尚待统一构建和游戏内验收：确认 NeoForge/Create 实际加载本批资源与四条原生配方，检查客户端正反方向外观、碰撞以及五档状态。素材执行者未运行 Gradle 或游戏。

## 联合审查 P2 整改

审查发现干式贮存架物品模型继承的 `dry_storage_rack_0` 没有原版方块模型父级，因而不能取得标准 GUI/手持变换。现已在该零档模型补入 `parent: minecraft:block/block`，并同步 `generate.py` 的模型生成逻辑；物品模型继续继承零档模型，两个本批模型均不覆盖 `display`。

修后静态验证重新运行通过。检查脚本从本地 Minecraft 1.21.1 客户端 JAR 读取 `assets/minecraft/models/block/block.json`，沿实际模型父链核对：`create_nuclear_industry:item/dry_storage_rack` → `create_nuclear_industry:block/dry_storage_rack_0` → `minecraft:block/block`。原版父模型确实提供 `gui`、`ground`、`fixed`、`thirdperson_righthand`、`firstperson_righthand` 与 `firstperson_lefthand` 变换；这些 display 沿未覆盖的父链继承到柜架物品。校验使用的客户端 JAR SHA-256 为 `499f6897d1837516680f3114072d8106e11c9adcd933fe5cf051b551089b0c99`，详细结果在 `tools/art-assets/store-01/evidence/resource-checks.json` 的 `rack_item_display_chain` 字段。其余模型、纹理、配方及几何验证均由同次生成/静态检查复核；未运行 Gradle 或游戏。
