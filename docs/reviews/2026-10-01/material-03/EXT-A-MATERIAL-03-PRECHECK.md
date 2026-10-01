# EXT-A-MATERIAL-03-PRECHECK：煤/木炭粉—钢锭—钢板

只读预检，基线候选 `060aeaf5340d8b5545ccdf84f5c82c041276ad44`。依据主工程方案卡、D-03a/D-03d、`docs/recipes.md` §5.4、成本草案、内容清单、现有资源/测试，以及锁定 Create 6.0.10-280。Create slim JAR SHA-256：`f0652bee27460f2d26a748f537cb5d687981441378d3a526d17ffaab5a1072bb`。实际读取并应用 `minecraft-modding/SKILL.md` 与 `minecraft-testing/SKILL.md`，按 MC 1.21.1 / Java 21 / NeoForge 21.1.219。没有运行 Gradle、游戏或导出器，没有改生产树、核心文档或 Git。

## 可表达性与锁定原生事实

- D-03d 已确认煤和木炭粉碎轮制粉，磨石保留原有染料用途。Create JAR 中没有以煤/木炭物品为输入的 crushing 配方；已有 crushing 仅处理煤矿石/深层煤矿石并输出煤等。`milling/coal.json` 为煤→2黑色染料、10%副产灰色染料；`milling/charcoal.json` 为木炭→1黑色染料、10%概率副产2灰色染料。故加 `create:crushing` 煤和木炭配方不会与染料 `milling` 重叠；不要增加相同输入的 `create:milling` 粉末配方。粉碎轮配方应作为本模组工序实现，不宣称是 Create 原生煤粉方案。
- NeoForge 锁定 JAR 提供 `c:ingots/iron`（原版铁锭并可选并入 `forge:ingots/iron`）、汇总 `c:ingots`；也提供 `c:dusts` 汇总（红石、荧石及可选 forge dust）。它没有 `c:ingots/steel`、`c:dusts/coal` 或 `c:dusts/charcoal` leaf。主工程也尚无这些叶标签或对应注册。应以 `replace:false` 增加 `steel_ingot`、`coal_dust`、`charcoal_dust` 三个正式身份和 leaf tags，并通过非覆盖式父标签纳入通用 `c:ingots` / `c:dusts`；兼容外部同类产品时保留通用标签输入。`steel_plate` 已注册，正式 ID、双语、模型、纹理和 `c:plates/steel` 均已有，不再注册别名或重画。
- Create `mixing` 的 schema 是 `ingredients` 数组（每项是普通 `Ingredient`，不是带数量的输入对象）、`results`、可选 `processing_time` 与 `heat_requirement`。锁定 `brass_ingot.json` 用 `c:ingots/copper` + `c:ingots/zinc`、`heated`、2 brass。锁定源码显示 BasinRecipe 每条 ingredient 消耗一件，最多64条；所以4铁的候选表达应把 `{"tag":"c:ingots/iron"}` 写四次，再加一条煤粉或木炭粉 tag。不要假定单个 ingredient 的 `count:4` 表达可生效。`BasinRecipe` 允许热级和 processing duration；100 是时长参数，Create 混合机仍按运行转速计算实际处理时间，不能写成固定100 ticks/5秒。
- 锁定 Create `pressing/compat/immersiveengineering/plate_steel.json` 仅在 `immersiveengineering` 加载时启用，输入 `c:ingots/steel`，输出 IE 钢板；它不是 Create 本体制钢配方。它与本批拟议 `c:ingots/steel`→本模组 `steel_plate` 使用同一输入 tag。Create Press 只查找一个匹配的 pressing recipe，因此 IE 同载时存在结果竞争风险。当前工程没有声明 IE 依赖；Create-only 目标下不构成阻塞，但若要承诺 IE 联用，须明确本模组钢板维修来源如何保证可达，或先做兼容联测，不能把条件兼容配方当成原生钢材路线。

## 参数候选与成本

卡中候选在锁定 schema 上可表达，但全部仍是待用户确认的试验值，不是 Create 原生合同或既有授权：

| 工序 | 候选表达 | 状态 |
| --- | --- | --- |
| 煤/木炭→对应粉 | 各自原版物品输入；1→1本模组粉末；`create:crushing`；`processing_time:100`；无副产物 | D-03d只确认机器分工；产率、时长、无副产物均待确认 |
| 粉→钢锭 | 4×`c:ingots/iron` + 1×`c:dusts/coal`（另有木炭粉平行配方）→4本模组 `steel_ingot`；`create:mixing`；`heated`；`processing_time:100` | 配比、热级、时长、无副产物待确认 |
| 钢锭→现有钢板 | 1×`c:ingots/steel`→1×本模组 `steel_plate`；`create:pressing`；不加固定 processing time | 试验产率待确认；Create pressing 原生 schema 不声明固定时间 |

按该候选且不计加工设备/动力：16块钢板消耗16铁锭、4份煤粉或木炭粉；64块消耗64铁锭、16份粉。若粉末维持候选1:1来源，分别是4或16块煤/木炭。即每块钢板成本为1铁锭+0.25煤粉或木炭粉；没有余产钢锭。此为整数批次成本，不是已批准的游戏平衡结果。

## 身份、测试与最小写集建议

内容清单已冻结 `steel_ingot`、`coal_dust`、`charcoal_dust` IDs；目前三者未注册/无模型、纹理或语言项。已有 `steel_plate` 是反应堆维修使用的唯一正式 ID（`content-catalog.md` §3.2，`P1ContentIds.java`）；需保持原项与可维修身份不变。

当前 `P1DataContractTest.noP1SurvivalRecipesOrPollutedCoolantRouteWereAdded`（`src/test/java/com/iksxh/create_nuclear_industry/P1DataContractTest.java:125-149`）会因任何配方 JSON 含 `steel_plate` 而失败。实现者需只为经批准的 `pressing/steel_plate` 精确输出开口，并保留对燃料组件、污染冷却剂、净化器及其他未授权 P1 路线的拒绝；不应删掉整项保护测试。

最小建议写集：三物品注册/创造页/双语/模型与新纹理；对应 item dust/ingot leaf tag 及非覆盖汇总 tag；两条粉碎轮配方、两条搅拌配方、一条钢板压片配方；上述既有 P1 数据契约测试的精确例外；仅覆盖本批流程的真实机器测试与本报告。不要包含钢杆、密封环、钢网/格、设备、反应堆规则、维修数值或冷却剂改动。

验证边界应覆盖：两条 `create:crushing` 输入与各自产物、原 Create 磨石染料配方仍加载；四份铁 tag + 单粉 tag 实际进入工作盆并消耗/产4钢锭；错金属/错误形态拒绝；普通 `heated` 可加工、无热/阴燃不满足、超热满足但不额外增产；钢锭 tag 压成原 `steel_plate`；保存重进/数据重载后仍匹配。测试应通过真实压碎轮、加热动力搅拌盆和 Create 压片，不以只解析 JSON 的镜像测试代替。

## 集中待用户确认

1. 是否接受1煤/木炭→1粉、粉碎 `processing_time=100` 且无副产物？
2. 是否接受4铁锭+1粉→4钢锭，最低普通加热（`heated`）、搅拌 `processing_time=100` 且无副产物？
3. 是否接受1钢锭→1现有钢板？

以上均是 PM 候选值，未确认前不应派发实现。IE 竞争只在目标安装包含 Immersive Engineering 时需要决定；当前基线不依赖 IE。

结论仅为静态可表达性与成本预检，不批准参数、不派发实现、不表示任何配方已运行验证。
