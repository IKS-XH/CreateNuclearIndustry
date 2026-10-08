# EXCHANGER-02R1：工作盆持续超级加热

**状态：** 功能`ab91356`实现及整改复核通过，用户2026-10-09明确确认工作盆供热手测通过，见[验收](../../reviews/2026-10-09/exchanger-basin-02r1/ACCEPTANCE.md)。汽轮机取景复看独立保留。实现前文档基线`8dbb9b6`（旧功能基线`bb5c0d7`），实施路径`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；PM单独整合已验收供热净功能，不合入未验收教学。

## 已确认合同

- 本设备正上方有Create工作盆，即作为持续核热负载；不读取配方、输入输出、过滤、搅拌器、转速、过载、加工进度或盆是否能继续加工。空盆、缺料、停转、输出堵塞照常耗热。
- 成功供热始终发布`SEETHING`超级加热，不按配方切换普通/超级等级。普通热及无热加工照常沿用Create，不再按加工批次扣费。
- 固定成本按一个超级燃烧室的锅炉热级计：`2 × huPerLevel HU/t`，默认2HU/t、密度0.5HU/mB时4mB/t（20TPS下80mB/s）。实际热液转成等量冷液，沿用同一HU账本和直列库存事务。
- 供液不足或冷液满时在设备下一tick停止超级加热，不使用40tick余热延长、不从已有储热免费续烧；恢复供液/回流即可恢复，不新增固定预热。移除工作盆即取消该负载，不继续耗热。
- 已归属高压锅炉、冷凝模式或直列冲突不为盆发热。原生燃烧室、Create锅炉、高压锅炉和普通蒸汽冷凝语义不改；无GUI、新配方、模型或旧存档兼容研究。

## 实现边界与写集

执行者须读根AGENTS、治理5.1/5.2、本卡，实际使用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`；核对Minecraft1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82。PM只设计、审查和管理文档/Git，执行者不写Git、不派发、不开用户客户端/存档。

唯一允许功能写集（Java根前缀`src/main/java/com/iksxh/create_nuclear_industry/`）：

- 修改`heat/HeatExchangerState.java`、`heat/NuclearHeatExchangerBlockEntity.java`、`heat/HeatExchangerBasinBridge.java`及`mixin/BasinHeatLevelMixin.java`：持续固定耗热、实时已付热查询、原生检查唤醒与玩家提示；删除批次账本/ThreadLocal及需求探测，不新建第二份储热池。
- 修改`config/HeatExchangerConfig.java`：撤下两个批次等效tick字段，改为`basinHeatLevelEquivalent`（默认2，范围1～18），费用为该值×`huPerLevel`；此配置只改固定消耗，供热等级始终超级。保留既有密度、额定功率/罐容量配置。成本高于额定转换能力等无效组合须停用本盆路径并提示，不使未变锅炉路径失效。
- 删除仅用于上一版的`mixin/BasinRecipeHeatMixin.java`、`mixin/BasinOperatingMixin.java`、`mixin/BasinOperatingAccessor.java`并移除`src/main/resources/create_nuclear_industry.mixins.json`相应注册；不新增/改写原生配方引擎入口。
- 修改`gametest/ExtensionHeatExchangerBasinGameTests.java`、`src/test/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerStateTest.java`，复用现有隔离模板`src/main/resources/data/create_nuclear_industry_ext_b_basin/structure/basin_empty.nbt`（必要时可修改）。语言只改`src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`、`en_us.json`的本批字段。
- 唯一报告路径`docs/reviews/2026-10-08/exchanger-basin-02r1/IMPLEMENTATION.md`，证据`build/reports/extension/EXT-B-EXCHANGER-02R1/`；其他核心文档由PM维护。

`HeatExchangerLine`只复用、不修改；保留既有`logs/debug.log`、`logs/latest.log`和三个`tools/**/__pycache__/`。整数mB尾差必须有界累计，不能截断丢工质或虚增HU；查询不转换液体、不扣账、不借缓存发免费热。每源每tick仅结算一次，服务端真付款后才发布超级热，区块不tick/源失效/拆除时查询无效。实现方法可按既有风格调整，复杂单位和生命周期分支用中文说明。

## 精简验证与停止点

用少量账本断言验证固定消耗、重复tick/查询无重复计费、冷热守恒、断流/冷满停热及`huPerLevel`联动。删去失效的批次预留/候选费用测试；未变锅炉/冷凝证据复用。真实GameTest集中验证空盆且无搅拌器仍超级供热、实际普通/超级原生配方、断流或冷满停热后恢复；既有原生燃烧室代表可保留，不展开矩阵。

一次定向`./gradlew.bat test --tests '*HeatExchangerStateTest' assemble`；一次隔离`./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_ext_b_basin -PgameTestDirectory=build/gametest-ext-b-basin-r1`。同一实现者持有构建进程，失败先诊断，只因实质改动或用例失败复跑受影响范围；不clean、不全量、不机械重跑。

报告列实际技能、删除/修改路径、配置与消耗公式、精确用例/退出状态及最新制品哈希。实现冻结后单轮合并规格/质量审查，必要整改仅对应复核；审查者不重跑测试。PM提交同级候选、更新集中清单，停在工作盆与汽轮机分别手测，不自动接下一台教学或主线。旧02按批次的实现/审查证据仅保留历史，不作为新行为已通过。

## 候选交付

定向账本21/21、真实GameTest7/7及增量打包通过；[实施记录](../../reviews/2026-10-08/exchanger-basin-02r1/IMPLEMENTATION.md)保留命令、退出码与日志限制，[复核记录](../../reviews/2026-10-08/exchanger-basin-02r1/REVIEW.md)关闭执行顺序和跨用途储热两项问题并保留初审历史。最终制品和客户端步骤统一在[集中清单](../../reviews/2026-10-08/exchanger-basin-02/MANUAL.md)，本卡交付不表示手测已通过。
