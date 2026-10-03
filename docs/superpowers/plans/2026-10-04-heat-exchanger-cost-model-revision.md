# 核换热器成本与外观修订

**任务ID：EXT-B-EXCHANGER-01B；状态：候选整改、定向自动验证及独立审查通过，待客户端人工验收。** 用户于2026-10-04明确要求降低成本、缩短合成链，采用顶部散热鳍片格栅＋底部基础方块造型，随后指定上排3铜板、中央1换热管束、其余5钢板。当前候选基线`3aa7764`，尚未人工验收，也未合入main运行代码。旧21格配方及笼架模型由本卡取代；PM此前建议的“3铜片＋1反应堆外壳”未采用。

## 已确认方向

- 成本朝反应堆多方块结构方块靠拢，最终数量与布局以用户指定的9格方案为准，保留1个核换热管束及其既有配方。
- 单格设备上部是明显的散热鳍片格栅，下部是完整方形基座。沿用Create金属质感、SVG源稿与JSON模型；不使用生图模型。
- 本次仅修改制造与外观。冷热罐、流体接口、供热账本、锅炉接入和存档行为沿用01A；用户本次没有确认这些功能的手测通过。

## 冻结配方

工作台3×3有序合成，产1核换热器，无副产物。C为铜板（现有铜片标签），H为核换热管束，S为钢板：

```text
CCC
SHS
SSS
```

C使用现有`c:plates/copper`，H使用`create_nuclear_industry:nuclear_heat_exchange_bundles`，S使用`c:plates/steel`。以原配方ID`heat_exchanger/nuclear_heat_exchanger`替换为`minecraft:crafting_shaped`，产物ID与数量不变。原21格动力合成不再保留第二入口；原生Create对工作台配方的兼容沿用默认机制，不另做重复配方。钢管坯、强化钢板、管束的注册和制造保持原样。

原整机直接投入12钢板＋4铜片＋2耐压接头＋2管束＋1工业传感器，改为5钢板＋3铜片＋1管束。减少7钢板、1铜片、2耐压接头、1管束、1工业传感器，并取消21格装配门槛。管束仍含强化板/精密构件链，这是用户最终指定布局的一部分，本轮不擅自继续修改其配方。

## 外观细化与验证范围

按已确认造型实施基座Y=0～12、鳍片区Y=12～16，总体仍在一个方块范围。深灰钢制完整方形基座搭配顶部平行铜鳍片，鳍片间保留真实凹槽；四侧可辨识管道接口。移除当前全高笼架与环绕盘管的视觉主体。热态通过鳍片/小型提示件表现，保持独立部件以便未来动画，避免同向共面叠层闪烁。不得修改碰撞体、逻辑接口、流体或供热逻辑；单格几何与既有四朝向/冷热点亮状态兼容。

## 分工、写集与验证

沿用同级候选`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`与`codex/ore-acquisition`。PM负责文档/Git，执行者不得做任何Git写操作、修改核心文档或自行派发。现有`tools/art-assets/__pycache__/`保留，主目录用户`.vscode/launch.json`不动。新改手写注释使用中文。

**RECIPE（高速代理）：** 仅允许修改`src/main/resources/data/create_nuclear_industry/recipe/heat_exchanger/nuclear_heat_exchanger.json`及`src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionHeatExchangerCraftingGameTests.java`。将旧21格匹配用例更新为真实工作台类型/3×3匹配与单件产量，沿用既有帮助方法；不要为此新建框架，不动其他材料用例。报告与必要证据写`build/reports/extension/EXT-B-EXCHANGER-01B-RECIPE.md`及同名目录。初次只改不运行Gradle，待PM模型合流通知后独占一次验证。

**ART（高速代理）：** 仅允许修改`src/main/resources/assets/create_nuclear_industry/blockstates/nuclear_heat_exchanger.json`、`models/block/nuclear_heat_exchanger/`、`models/item/nuclear_heat_exchanger.json`、`textures/block/nuclear_heat_exchanger/`及`tools/art-assets/heat-exchanger-device/`（后四个资源路径相对同一assets命名空间）。保留SVG源稿、确定性导出器和部件说明；不改材料素材、Java、配方或语言。报告与预览写`build/reports/extension/EXT-B-EXCHANGER-01B-ART.md`及同名目录，不重写01A历史证据。检查冷/热模型组合的引用、坐标、显式UV、物品变换、同向共面重叠，并交付可查看预览。

**合并必要检查：** 一次增量assemble与对应资源打包检查。更新后的工作台用例优先使用当前已有筛选入口；若锁定版本仅方便筛选命名空间，允许一次既有`create_nuclear_industry_heat_exchanger`批次，不扩建测试基础设施。无需重跑JUnit或旧全量；未变运行逻辑复用01A证据。一次只读合并审查聚焦本次差异，发现问题只复验受影响部分。

**人工门：** 客户端检查JEI/工作台9格产量、物品与手持、放置/四朝向/热态造型；此前未报告通过的01A真实循环及停机/重载仍保留在同一清单。自动通过后停在此门，不能直接合入main运行或推进下一功能。

**必读：** `AGENTS.md`、`docs/project-governance.md`第1/4/5.1节、本卡。实际读取`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`与`minecraft-testing/SKILL.md`；ART另读`minecraft-resource-pack/SKILL.md`。锁定Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82，不升级依赖。报告注明实际技能应用与未验收项。
