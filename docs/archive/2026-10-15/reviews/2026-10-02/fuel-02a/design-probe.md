# EXT-A-FUEL-02A 设计探针（未批准候选）

**基线：** `Create_NuclearIndustry-ore-acquisition`，HEAD `b1a4022`；MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280。工作区只读检查时无既有差异。实际读取并应用 `minecraft-modding`、`minecraft-testing` 技能；按治理 5.1 本探针不运行构建/测试。

**结论：** `green_fuel_pellet`、`sintered_fuel_pellet`、`fuel_sintering_furnace` 在本候选源码、资源和注册清单中均未实现。任务表 EXT-A-FUEL-02 仍受前项、热源运行合同、装配入口和 D-03 参数阻塞；本报告仅提供技术可行性建议，不将新参数/布局记为已批准。

- **热级接口可直接读取。** 锁定版本地 `create-1.21.1-6.0.10-280-sources.jar` 的 `com.simibubi.create.content.processing.burner.BlazeBurnerBlock#getHeatLevelOf(BlockState)` 从方块状态的 `HEAT_LEVEL` 取等级；无该属性时返回 `NONE`。`HeatCondition#testBlazeBurner` 的 `HEATED` 判定明确排除 `NONE`、`SMOULDERING`，接受 `FADING`、`KINDLED`、`SEETHING`。普通燃料燃烧通常为 `KINDLED`，燃料将尽时为 `FADING`；特殊燃料为 `SEETHING`。因此候选炉每个服务端 tick 读正下方方块状态，以 `HEATED` 门槛判定即可；固定处理计时不随等级改变，可满足无热/阴燃不加工、普通热可加工、超热不提速。无需新建热能力或依赖 Blaze Burner 方块实体。
- **热源与制造件是两件事。** 同一 Create 源码的 `AllBlocks.BLAZE_BURNER` 注册 `create:blaze_burner` 方块物品（`BlazeBurnerBlockItem::withBlaze`）；`AllItems.EMPTY_BLAZE_BURNER` 是独立的 `create:empty_blaze_burner` 物品。捕获烈焰人的完整燃烧室放置初始为 `SMOULDERING`，之后需正常加燃料才达到普通加热；空燃烧室不是可立即供热的替代品。PM提出的 3×3 动力合成器布局 `RSR/RBR/SIS` 数量为耐火砖4、钢板3、完整燃烧室1、工业传感器1，静态上可表达；若炉体配方消耗其中一台，实际运行时炉底还需另放一台完整燃烧室，首台有效产线成本为两台。此布局及数量仍是建议。
- **配方数据表达没有阻碍。** `create:pressing` 是单物品输入的压片配方；项目已有 `recipe/pressing/steel_plate.json` 范式。`create:compacting` 属于 Basin 配方族，可列出重复输入；项目 `recipe/compacting/uranium_tailings_brick.json` 已采用该格式。故“1低浓缩铀粉→1生燃料芯块”两种方式均可静态表达；就文档现有“压片机”路线，建议只新增一条 pressing 配方，避免竞争配方。输出 `green_fuel_pellet` 尚未注册，配方落地仍需对应材料实现。烧结转化是专用热加工，不能只靠压片/压实 JSON 代替机器运行逻辑。
- **机器草案无显见 Create 冲突。** 单方块、无轴动力、无 GUI；候选可用独立方块实体保存一个输入格和一个输出格（各64），上方仅插入、四侧仅提取、底面不开放物品接口；服务端按一件/400 tick 转换。下方 Blaze Burner 与输入/输出面不冲突。需实现输出堵塞时保留输入与进度、断热暂停并恢复、存档恢复；输出容器不得先扣输入再丢产物。仓库没有可直接复用的通用热加工机器；最小路径是专用轻量 BlockEntity/有限状态逻辑，复用上述 Create 热级接口和现有燃料物品注册模式，不复用离心机的转轴/流体状态机。
- **材料来源核查。** 文档正式链条已命名现成上游材料：铀精矿/铀浆/低浓缩铀粉、钢板、耐火砖、工业传感器、燃料包壳管、锡合金焊料、钢格架；没有证据支持额外发明中间材料。上游现有候选配方可查 `centrifuging/uranium_slurry.json`、`mixing/refractory_brick.json`、`pressing/steel_plate.json` 和工业传感器序列装配。**但端到端生存可达尚未成立：** 本候选未见生芯块、烧结芯块配方或烧结机；第8.2节燃料包壳/两级装配和第11节设备配方当前是文档关系，且锡合金焊料、钢网/格架实现依赖待冻结/待实施的 D-03 与材料路线。不能据文档列项宣称已全部可制作。

**主要证据：** `docs/recipes.md` 第8.2、11节；`docs/superpowers/plans/2026-09-22-first-release-extension-preparation-plan.md` 第2.5、4节；`src/main/resources/data/create_nuclear_industry/recipe/{pressing/steel_plate.json,compacting/uranium_tailings_brick.json}`；上述锁定版 Create 本地源码类。未做视觉/游戏内验证；热级结论限于源码静态语义。
