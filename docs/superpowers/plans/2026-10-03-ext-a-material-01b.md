# EXT-A-MATERIAL-01B：青金石制粉与无热冷却剂制备

**状态：已派发实施。** 用户确认无需加热，其余推荐参数采用。前置02E功能手测已通过，R2提交`86c92cd`已合入main；高速模型执行者`coolant_01b_impl`已收到START并接续本卡。PM使用方案梳理与任务计划技能，按既有自动派发授权执行，不增设派发审批。

**Goal：** 原版/Create材料可经粉碎轮及无热动力搅拌产出现有冷态复合冷却剂。

**Architecture：** 沿用现有DeferredRegister和具体粉末标签，新增两条Create原生JSON配方；不新增机器、流体、桶或状态算法。旧01A仅恢复适用的小范围实现，当前认可SVG直接接入。

**Tech Stack：** Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6、JEI19.27.0.340；不升级，不改许可证。

**Spec：** [已确认完整方案](./2026-10-03-coolant-production-proposal.md)。它以2026-10-03用户明确答复覆盖旧稿的冷却剂加热要求。旧01A任务中的重复全量验证由治理5.1及本卡定向验证替代，历史结果不改写。

## 角色与冻结合同

- PM维护核心文档、审核和Git，禁止代写实现。单个高速执行者实施并独占本批Gradle；首次只读准备，收到PM开始信号后才写实现，避免混入R2制品。候选路径`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`，实际起点由执行者记录。
- 开工读AGENTS、治理5.1、本卡和方案；实际应用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md`。手写注释中文；无Git写、核心docs修改、再派发或用户客户端/存档操作。
- `ModItems.LAPIS_DUST`注册普通可堆叠物品`lapis_dust`，无燃料状态，加入创造页，中英名“青金石粉 / Lapis Dust”。使用当前已认可16×16 SVG产物，不使用旧01A的1254×1254纹理。
- 粉碎：1 `c:gems/lapis` →1 `create_nuclear_industry:lapis_dust`，`processing_time=100`、无副产物。磨石保持Create原青金石→蓝色染料，不能被粉碎回退竞争覆盖。
- 搅拌：`c:dusts/lapis`、`c:dusts/redstone`、`c:dusts/glowstone`各1，加1000mB水→1000mB现有`compound_coolant`；`processing_time=100`，无需加热，无副产物。Create既有原生机制负责速度、动力、库存、流体与输出事务，不写补丁或新容器逻辑。
- 具体粉末标签及父标签只追加，不替换生态内容。等价标签成员能匹配，青金石原物、蓝色染料、错误粉末不能顶替青金石粉；核材料不进通用粉末表。

## 精确允许写集

1. `content/ModItems.java`仅新增`LAPIS_DUST`、`content/ModCreativeTabs.java`仅加入该项；不挪动其余注册。参考旧提交`8996fa78e76fd0ce4227763db7da9b103d32a518`的小改动，禁止整体checkout/cherry-pick旧分支。
2. `assets/create_nuclear_industry/models/item/lapis_dust.json`、`textures/item/lapis_dust.png`；双语言JSON只追加该物品键。
3. `data/c/tags/item/dusts/lapis.json`及`dusts.json`仅追加；两配方`data/create_nuclear_industry/recipe/crushing/lapis_dust.json`与`mixing/compound_coolant.json`。
4. `tools/art-assets/manifest.json`仅将已有lapis条目的`game:null`改为该物品贴图路径，保留SVG和已认可生成PNG；不运行全量install、不改共用导出器。该映射变动若触发既有专用检查的历史数量假设，报告PM，禁止顺手重写全管线。
5. 新`src/main/java/.../gametest/ExtensionCoolantProductionGameTests.java`、隔离模板`data/create_nuclear_industry_coolant/structure/p0_probe_empty.nbt`（复制现有空模板）。必要时新`src/test/java/.../CoolantProductionDataContractTest.java`。`P1DataContractTest.java`只在旧生存禁令中精确放行已批准的`mixing/compound_coolant.json`冷态产物，并更新相关中文解释；热态/污染/净化器禁令不放宽。
6. 测试专用等价标签成员：只允许隔离GameTest运行目录`build/gametest-coolant-production`内数据包，不把伪等价物写进发布资源。沿用现有框架，确实需要新的装载接入先报告，勿增建框架。
7. 报告`build/reports/extension/EXT-A-MATERIAL-01B.md`及同名证据目录，隔离运行目录`build/gametest-coolant-production`。不得修改Build/依赖、ModFluids/P1ContentIds、原生桶、任何其他PNG、旧设备或反应堆算法。

以上Java路径均位于`src/main/java/com/iksxh/create_nuclear_industry/`，资源位于`src/main/resources/`；test对应同包根。

## 一个实施任务与最小验证

- [ ] 只读核对当前注册/配方格式/磨石回退与旧01A可复用内容；确认既有lapis SVG/PNG和冷却剂桶身份。无需先跑基线全量测试。
- [ ] 实施冻结注册、标签、语言、素材映射和两条配方。检查新粉末PNG来自已认可工具产物，所有既有113张游戏PNG不变。
- [ ] 合并少量GameTest场景，代表性真实粉碎轮完成1:1、磨石仍匹配蓝色染料；真实无燃烧室搅拌器完成一批且三粉/水精确扣除、1000mB冷却剂入输出；故意堵满输出后不吞输入、疏通后正常产出。不得用只读JSON或直接调用配方apply冒充机器运行。
- [ ] 同组检查实际注册/具体及父标签、外部等价标签成员匹配与错误形态拒绝。模拟外部标签数据包必须明确标为模拟兼容测试；保留原NeoForge父标签成员。复用已验收容器、管网、机器恢复证据，不为原生配方新增全速率/全故障矩阵。
- [ ] 运行相关新合同测试（若有）及`P1DataContractTest`；使用隔离命名空间`-PgameTestNamespace=create_nuclear_industry_coolant -PgameTestDirectory=build/gametest-coolant-production runGameTestServer --console=plain`。单执行者Gradle，`JAVA_HOME=C:/Program Files/Java/jdk-21`，不全量/不clean/不rerun-tasks。真实失败按原因补验，不删断言。
- [ ] 最后一次增量assemble，核对新物品/标签/两配方及原生桶资源正确打包；报告命令、结果、JAR哈希、局限与三项合并手测。项目经理一次合并审查资源/行为/证据，小范围整改不另建多层审查。

人工门：①JEI、名称/粉末图标；②青金石粉碎1:1及磨石仍出蓝染料；③没有燃烧室也能三粉加1000mB水制1000mB冷却剂，用现有桶/管道取走。实现后只交付本批清单并暂停，不继续机组/热端/封存。
