# EXT-A-MATERIAL-02A：Create 原生铅锡矿物路线只读核对

**结论：** 锁定 Create 6.0.10-280 已提供铅/锡矿石、粗矿和粗矿块的原生 `create:crushing` 路线；候选已有它们依赖的全部通用输入标签与 Create 粉碎粗矿标签，故不应复制这些粉碎配方。粉碎粗矿之后，Create 没有无条件的铅/锡洗矿、熔炼/高炉或压板输出；现有后段条目全是第三方模组兼容配方，不能作为本模组自身的成品身份配方。

**核对边界：** 只读审查候选 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，HEAD `0d38c99fe8d3e041c6148057fc4f62827b9e45b4`，分支 `codex/ore-acquisition`，接单时 `git status --short` 为空。锁定 JAR 为 `create-1.21.1-6.0.10-280-slim.jar`，SHA-256 `F0652BEE27460F2D26A748F537CB5D687981441378D3A526D17FFAAB5A1072BB`。未运行 Gradle、游戏或导出。JAR 中被引用的原始配方已只读摘取至本报告同名证据目录。

实际读取主工程 `AGENTS.md`、`docs/project-governance.md`、`docs/superpowers/plans/2026-10-01-ext-a-material-02.md`、批准页 `docs/superpowers/plans/2026-10-01-lead-tin-material-proposal.md`；完整读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`（锁定版本、注册/tag/数据配方边界）与 `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`（只读数据证据与运行验收的区别）。技能通用例子没有替代锁定 JAR 的实际数据。

## Create 中可直接复用的粉碎配方

六个路径都在 `data/create/recipe/crushing/`；共同的 `type` 为 `create:crushing`、`processing_time` 为 **400** ticks。带条件的配方以 `neoforge:not` 包装 `neoforge:tag_empty`，也就是对应 C 标签非空就加载，不要求某个第三方模组名：

| 锁定配方 | 输入 | 固定输出及概率 |
| --- | --- | --- |
| `lead_ore.json` | `c:ores/lead` | `create:crushed_raw_lead` ×1；同物品额外 ×1，概率 0.75；`create:experience_nugget` ×1，概率 0.75 |
| `tin_ore.json` | `c:ores/tin` | `create:crushed_raw_tin` ×1；同物品额外 ×1，概率 0.75；经验颗粒 ×1，概率 0.75 |
| `raw_lead.json` | `c:raw_materials/lead` | `create:crushed_raw_lead` ×1；经验颗粒 ×1，概率 0.75 |
| `raw_tin.json` | `c:raw_materials/tin` | `create:crushed_raw_tin` ×1；经验颗粒 ×1，概率 0.75 |
| `raw_lead_block.json` | `c:storage_blocks/raw_lead` | `create:crushed_raw_lead` ×9；经验颗粒 ×9，概率 0.75 |
| `raw_tin_block.json` | `c:storage_blocks/raw_tin` | `create:crushed_raw_tin` ×9；经验颗粒 ×9，概率 0.75 |

对应证据在 `build/reports/extension/EXT-A-MATERIAL-02A-NATIVE-AUDIT/evidence/create-1.21.1-6.0.10-280/crushing/`。候选的 `data/c/tags/item/ores/{lead,tin}.json`、`raw_materials/{lead,tin}.json`、`storage_blocks/raw_{lead,tin}.json` 已非空；`data/c/tags/item/crushed_raw_materials/{lead,tin}.json` 已包含 Create 的 `create:crushed_raw_{lead,tin}`。因此上述 Create 配方在当前候选数据下会自行生效，无需新增同输入、同输出的本模组 crushing JSON 或更改这几组标签。

## Create 原生路线的缺口和条件兼容

| 工序 | 锁定 JAR 实际数据 | 含义 |
| --- | --- | --- |
| 洗矿（Create `create:splashing`） | 没有不带条件的 lead/tin 配方。铅仅有 `splashing/{immersiveengineering,mekanism,oreganized,thermal}/crushed_raw_lead.json`；锡仅有 `{mekanism,ic2,thermal}/crushed_raw_tin.json`。条件分别为 `neoforge:mod_loaded`；均从 `create:crushed_raw_*` 产相应第三方 nugget ×9，没有其他副产物，也未声明固定 processing time | `c:nuggets/lead` 或 `/tin` 不会让这些兼容配方出现；输出物品 ID 属于对应模组。当前候选没有铅/锡 nugget 注册或 `c:nuggets` 子标签 |
| 熔炼/高炉 | 没有不带条件的铅/锡 `crushed_raw` cooking 配方。铅的 smelting/blasting 兼容目标为 Immersive Engineering、Mekanism、Oreganized、Thermal；锡为 IC2、Mekanism、Thermal。均以 Create 粉碎粗矿为输入、输出目标模组锭；smelting 200 ticks、blasting 100 ticks、经验均 0.1 | 这是带 `neoforge:mod_loaded` 的目标模组 recipe，并不是 Create 为所有铅/锡物品提供的统一默认成品路线 |
| 压片 | 默认 Create `pressing` 有 copper、iron、gold、brass 的一锭一板；lead 仅有 `pressing/compat/immersiveengineering/plate_lead.json`，条件为该模组已加载，输出 `immersiveengineering:plate_lead`。没有默认 tin/lead 成品板配方 | 候选自己的 `pressing/{lead_plate,tin_plate}.json` 输入 `c:ingots/{lead,tin}`、各产对应本模组板 ×1、无 processing_time，已符合用户批准的一锭一板和原生 pressing 动作形式；这是本模组补齐输出身份，不是 Create 已有默认 lead/tin recipe |

第三方洗矿/烹饪目标可能同时加载（例如多个金属模组都定义同一粉碎料输入）；若本模组再给相同 `create:crushed_raw_*` 增加自身输出，混装这些模组时会出现同输入多配方。锁定 Create 的多模组兼容数据本身就采用此种条件兼容模式；本轮没有验证具体混装模组集。

## 与候选和既定 ID 的逐项对照

- **候选已有且应保留：** `lead_ingot`、`tin_ingot`、`lead_plate`、`tin_plate` 注册；`c:raw_materials/{lead,tin}`、`c:ores/{lead,tin}`、`c:storage_blocks/raw_{lead,tin}`、`c:crushed_raw_materials/{lead,tin}` 及 `c:ingots/{lead,tin}` / `c:plates/{lead,tin}`。本模组现有 `smelting|blasting/{metal}_ingot_from_raw_{metal}.json` 为 1 粗矿→本模组锭，分别 200/100 ticks、经验 0.7；本模组 `pressing/{metal}_plate.json` 为 1 锭→1 板。此次只读 JAR 中无相同的无条件铅/锡原生配方可与之重复。
- **粉碎无需新增：** 原生 Create 六条配方已经由候选标签接通。候选旧测试拒绝粉碎料只说明其旧 `_from_raw_*` Cooking 配方不接收粉碎粗矿；不能据此推断应禁用新增、独立的 `create:crushed_raw_*` cooking 配方。
- **待本模组补齐的原生等价输出身份：** 若采用用户最新“完全照搬原版 Create 矿物处理路线”，铅/锡仍需本模组自己的粉碎料洗矿产物，以及 Create compat 所对应的粉碎料熔炉/高炉输出本模组锭。洗矿输入应是既有 `create:crushed_raw_lead/tin`，输出候选内容清单已预留的 `lead_nugget` / `tin_nugget` ×9；Create compat 中同类配方就是九粒、无其他产物。粉碎料熔炉/高炉等价配方参数为 200/100 ticks、经验 0.1，固定输出本模组 `lead_ingot` / `tin_ingot`。此处是按已读 Create compat 数据做本模组身份映射，并非声称 Create 默认已给出这些自有输出。
- **粒的内容身份：** `docs/content-catalog.md:55-56` 已把 `lead_nugget`、`tin_nugget` 列为 P1 既定 ID；候选当前尚未注册。要让无额外金属模组的洗矿路线实际给出铅/锡粒，需要注册这两个既定身份并加入 `c:nuggets/lead`、`c:nuggets/tin`（及汇总 `c:nuggets` 的嵌套标签）。不需要另起新命名身份。要完整实现标签兼容/配方解锁仍需模型、语言、创造栏和现有样稿风格纹理按 PM 任务卡界定；本审计不改这些文件。
- **九粒与锭的互换：** Create JAR 没有通用 `c:nuggets/<metal>`→`c:ingots/<metal>` 9:1 配方模板；所查唯一同类 Create 自有铸锭粒拆解示例是专用 `crafting/materials/zinc_nugget_from_decompacting.json`，输入 `c:ingots/zinc`、输出 `create:zinc_nugget` ×9。把既定铅/锡粒实现成 9 粒→1 对应锭，及是否同步提供反向 1 锭→9 粒，是标准材料接口层的本模组配方建议，不是可由 Create 默认 lead/tin 配方唯一推出的规则，需由 PM 在任务合同中明确；候选本来已有的 raw→锭路线可按 PM 最新指示保留。

## 对参数冲突的结论与建议写集

用户后来明确“对锡铅两种矿物的处理完全照搬原版的 create 矿物处理路线”后，旧的 0.7 经验只能继续适用于已经批准的 **raw tag→锭**配方；新增 **Create 粉碎粗矿→锭**等价路径的 Create compat 参照是 0.1 经验。已有 raw 路线无需因 Create 不提供默认铅锡 raw cooking 而删除或改数。raw 与 ore 的 200/100 时间正好与对应 cooking 规则一致，但这是参数对照，不能把 Create 无条件原料配方说成已经存在。Create 原矿石路径本身是 400 ticks 粉碎，不是 200/100 cooking。

交 PM 的推荐功能写集仅供任务卡使用，不是本报告实施范围：

1. 保持当前四个物品 ID、六条 cooking/pressing 和既有粗矿、矿石、粗矿块通用标签。
2. 新增 2 条 `create:splashing`（Create 粉碎粗铅/锡→各 9 个既定铅/锡粒，无副产物）以及 4 条 `minecraft:smelting`/`minecraft:blasting`（对应粉碎粗矿→本模组对应锭；200/100 ticks、0.1 XP）。原 raw tag→锭参数 200/100、0.7 XP 保留。
3. 注册既定 `lead_nugget` / `tin_nugget`，补 `c:nuggets` 子/汇总标签及必要资源。9 粒↔1 锭的 crafting 关系应由 PM 明确是否作为本批写集；Create 本体没有 lead/tin 或通用模板可直接复用。若本批需要“完全闭环”，建议正向压合与反向拆解都写明数量与标签合同。
4. 不新增已有原生粉碎 JSON；也不因新粉碎料 Cooking 路径而放开旧 raw 专属 Cooking 配方输入。粉碎加工是 Create 独立配方种类，输入合同互不覆盖。

**不确定性边界：** JAR 确认的是配方文件、条件表达式、ID、数量/概率与 cooking/processing 时间；未运行游戏检查条件加载后的运行时配方管理器，也没有测试其他金属模组共存。该报告不是客户端、GameTest、构建或候选验收证据。
