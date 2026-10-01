# EXT-A-MATERIAL-03-REVISION-PRECHECK

只读技术预检；基线 `27bf103ecfe8da0a161e62039e47574bf414b06f`。依据主工程路线变更卡、Create 6.0.10-280 锁定 JAR/源码、NeoForge 21.1.219 标签。未运行 Gradle、游戏或资源导出；不构成参数批准、实现或验收。

## 结论

- Create 6.0.10-280 没有铁锭粉碎配方。新增 `create:crushing`：`minecraft:iron_ingot` → 本模组 `iron_dust` 不与其已有铁矿/粗铁粉碎配方竞争。Create 原有 `crushing/iron_ore.json` 输入铁矿石、产 `create:crushed_raw_iron`；`crushing/raw_iron.json` 输入 `c:raw_materials/iron`、也产粉碎粗铁。粉末 `c:dusts/iron` 与粉碎粗铁标签是不同形态，不应互相替代。
- 粉碎轮的新增精确铁锭配方是使获准新路线可用所需的本模组配方。煤粉、木炭粉配方也需本模组提供；卡要求保留 Create 的磨石染料路线，不应用制粉配方覆盖该路线。
- Create 的 Basin `mixing` 配方可表达 4 铁粉 + 1 煤粉/木炭粉 → 5 钢粉，且无需热级。配方以重复 `Ingredient` 项表示数量：铁粉标签重复四次、碳粉标签一次；单个 Ingredient 不用 `count: 4`。省略 `heat_requirement` 即为 `HeatCondition.NONE`。工作盆仍需动力；“无热”不等于无动力。
- Create 鼓风机的 `BlastingType.canProcess` 查找原版 `RecipeType.SMELTING`，再尝试 `RecipeType.BLASTING`。`process` 也按 smelting、blasting 顺序选择可应用的烹饪配方；它会额外读取 smoking 配方作排除判断：若 smoking 与选中的 smelting/blasting 产物相同，则返回空结果，不应用烹饪配方。故钢粉需有熔炉可用的 `smelting` 配方；同输出的 `blasting` 配方可供高炉。风扇复用原版烹饪 RecipeType，并非另有 Create smelting 类型。smoking 不是优先产出类型；不能把流程概括为 smoking→smelting→blasting 的产物优先链。
- 在锁定 NeoForge/Create 标签中未发现 `c:dusts/iron` 或 `c:dusts/steel` 细分标签；也未发现能覆盖本模组新物品的现有标签成员。实施时需新增叶子标签并汇入 `c:dusts`，`replace:false` 追加本模组物品。不得将所有粉末宽泛标签用于铁粉/钢粉配方输入。当前工程已有内容清单身份 `iron_dust`、`steel_dust`；仍需实现资源和通用标签成员。`create:crushed_raw_iron` 不应加入 `c:dusts/iron`。

## 精确依据

| 证据 | 观察结果 |
| --- | --- |
| 锁定 Create JAR：`C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280/92471e8fc5ed4e3c2279001d77ebcec6fd38bcab/create-1.21.1-6.0.10-280-slim.jar`；SHA-256 `f0652bee27460f2d26a748f537cb5d687981441378d3a526d17ffaab5a1072bb` | JAR 有 `data/create/recipe/crushing/iron_ore.json`、`raw_iron.json`；未见 `crushing/iron_ingot.json` 或 `milling/iron_ingot.json`。前者输入原版铁矿石，后者输入 `c:raw_materials/iron`；输出均为 `create:crushed_raw_iron`，与新铁粉不是同身份。 |
| Create 源码 `com/simibubi/create/content/processing/recipe/ProcessingRecipeParams.java:43-64` | Processing 配方含 Ingredient 列表；`processing_time` 可选；默认 `HeatCondition.NONE`。 |
| Create 源码 `com/simibubi/create/content/processing/recipe/BasinRecipe.java:93-105,196-223` | Basin 对每个 Ingredient 消耗一件；工作盆支持热级/时长，输入上限 64。重复 Ingredient 实现 4+1 输入。 |
| Create 源码 `com/simibubi/create/content/kinetics/fan/processing/AllFanProcessingTypes.java:101-166` | `BlastingType` 是风扇处理类型；优先级 100（处理类型优先级）。`canProcess` 查 SMELTING 后查 BLASTING；`process` 按该次序选择，另检查 SMOKING 产物，若与选中的烹饪产物相同则返回空结果。SMOKING 是排除判断，不是优先产出。RecipeType 查找顺序与处理类型优先级 100 不同。 |
| 锁定 NeoForge universal JAR `C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/21.1.219/300c6ecf584eab19b4dca5e69cc2ee68d0d21f1f/neoforge-21.1.219-universal.jar`；`data/c/tags/item/dusts.json` | 父标签包含 glowstone、redstone 细分标签以及可选 Forge 项；锁定输入中无 iron/steel 叶子标签定义，故需本模组补齐两项。 |

## 最小实现建议与验证边界

按卡建议仅包含：铁锭、煤、木炭三条粉碎；铁粉+煤粉、铁粉+木炭粉两条无热搅拌；钢粉 `smelting` 与 `blasting`；钢锭压制现有 `steel_plate`。煤/木炭的具体粉碎器具还应遵照 D-03d 的设备分工。无需新建铁锭粉碎冲突例外或假定 Create 会把粉碎粗铁当粉末。

自动验证应检查配方 JSON/标签成员、数据包重载，以及数量和错误形态：真实粉碎轮铁锭产铁粉；Create 原生铁矿仍产粉碎粗铁；4铁粉加任一碳粉得到5钢粉，少料/错误粉末不匹配；不加热但有动力时搅拌可加工；钢粉分别可走熔炉与高炉配方，风扇以实测验证 smelting/blasting 选择及 smoking 同产物排除；压片使用既有钢板身份。静态 schema 可表达不代表这些游戏内行为已经通过。

建议写集限于新增粉末物品及素材、粉碎/混合/熔炼/压片配方、`c:dusts/iron`、`c:dusts/steel` 和 `c:dusts` 追加成员，以及覆盖上述目标的配方/标签测试；不扩展铅锡、副产物、其他铁制品回收或设备冷却剂。

报告初稿时，4 铁粉 + 1 煤粉或木炭粉 → 5 钢粉已批准，其余参数仍待确认。PM 随后确认用户已批准整组参数：铁/煤/木炭粉各 1:1、粉碎 `processing_time=100`；混合无热、`processing_time=100`；钢粉熔炉 200 ticks、高炉 100 ticks、经验 0.1；钢锭压制为 1:1。该状态更新只记录参数已批准，不代表实现或运行验证。

技能：按任务卡实际应用 `minecraft-modding` 与 `minecraft-testing`；技能版本示例以项目锁定的 MC 1.21.1 / NeoForge 21.1.219 / Create 6.0.10-280 为准。本报告仅为锁定源码与资源静态核对。
