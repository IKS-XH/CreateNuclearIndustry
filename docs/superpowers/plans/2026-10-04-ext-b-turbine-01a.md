# EXT-B-TURBINE-01A：配置改造与三档汽轮机执行计划

> PM使用 `superpowers:subagent-driven-development` 自动派发；执行者按 `superpowers:executing-plans` 完成自己的写集。角色、Git禁令、人工门和精简验证以AGENTS及治理5.1为准，优先于技能通用步骤。

**状态：已获用户确认，允许自动执行至人工测试门。** 基线 `6017ff2`，文档合同提交后写入候选；代码只在 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition` 开发。主目录已有 `.vscode/launch.json`、候选已有日志及pycache均不触碰。

**Goal：** 将既有锅炉/换热器及新汽轮机平衡值配置化，交付默认三档、多边形成型、无GUI的超临界汽轮机候选。

**Architecture：** 服务端配置生成有界快照，纯账本负责守恒，控制器拥有唯一库存，两轴仅发布各自份额；真实Create探针先验证生命周期。世界结构/模型按有限档位匹配，资产与代码通过明确渲染接口交接。

**Tech Stack：** Minecraft 1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；不升级构建依赖。

**Spec：** [用户已确认方案及配置补充](./2026-10-04-supercritical-steam-turbine-proposal.md)。

## 全局约束与技能

- PM只改文档、审查证据和管理Git；执行者不改治理/核心文档、不Git写、不派发其他Agent。
- 新增/修改手写代码使用有意义中文注释，单位、客户端/服务端边界和非显然事务必须说明。
- 必读技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`；资产执行者另读 `minecraft-resource-pack/SKILL.md`；依赖与发布操作由PM按 `minecraft-ci-release` 处理。
- 禁止生图模型；新资产沿用SVG可复现导出。无GUI，不改变既有配方或技术栈。
- 实际客户端测试留给用户；配置/探针/实现自动验证合并审核后只给一份人工清单。不得因其他任务尚在写代码而重跑无关全量测试。
- Gradle/游戏服共享同一候选。每轮命令前向PM申请执行时段；只有一个执行者持有验证时段，其他执行者暂停会影响编译的写入。禁止清理或启动用户存档、常规客户端。

## 审查重点

1. 降低容量、保存/重载后原有液量不得被clamp删除；超限输入拒绝、原库存仍可排出/处理。
2. 配置实际进入运行、流体能力、管压和护目镜；不能只是生成TOML而后台仍用常量。
3. 双轴同网、分网和外部源接管不重复登记容量；部分机身/任一轴区块卸载不留下幽灵SU。
4. 主动排汽与外部抽取共享同一物理口预算，旧能力/旧结构引用失效，不用查询次数多产汽。
5. 三档模型和可交互/碰撞位置一致，四向、区块边界、端盖拼接、物品/手持均有效。

## A. EXT-B-CONFIG-01：锅炉与换热器配置

**唯一结果：** 既有两类设备运行与平衡参数全部通过独立SERVER配置进入真实路径，默认行为保持已验收基线。

**允许写集：**

- `src/main/java/com/iksxh/create_nuclear_industry/config/HeatExchangerConfig.java`、新增 `BoilerConfig.java`；可在同目录新增本任务的纯设置辅助类型。
- `src/main/java/com/iksxh/create_nuclear_industry/boiler/`、`heat/` 中配置读取、账本、能力、管压、生命周期及显示所需调整；不重设锅炉结构尺寸/端口规则。
- `src/main/java/com/iksxh/create_nuclear_industry/CreateNuclearIndustry.java` 仅注册BoilerConfig；这份共享文件在A交还前不由其他任务编辑。
- 对应已有锅炉/换热器JUnit与必要配置集成GameTest；新增测试限本批配置合同。报告/日志只写 `build/reports/extension/EXT-B-CONFIG-01*`。
- 若现有tooltip占位参数必须改变，可修改中英文语言文件中锅炉/换热器自身键；先报告资产任务避免同文件并发。

**接口：** 保留旧HeatExchangerConfig文件名及三键；新锅炉配置建议文件 `create_nuclear_industry-boiler.toml`。实现者先向PM报告准确键名、范围、快照接口和存量兼容策略，供后续配置指南引用；不再询问已经批准的默认值。

- [ ] 逐项列出当前常量、配置入口和派生值，剔除坐标/协议等非调参常量。
- [ ] 补齐SERVER规格、中文单位与校验。锅炉基准：双16000mB、每口256mB/t、每段18HU/t、1HU/mB、3600HU暖炉/段、0.9HU/t散热/段、25%复热阈值、安全阀90%/80%与256mB/t。换热器：双4000mB、直列16、热等级18、1HU/级/t、40tick、密度引用P1。
- [ ] 将账本/能力/管压/显示改为同一快照；保留存量，容量变小不得静默删液或因负剩余空间反向增液。旧NBT兼容和已有有效配置不重置。
- [ ] 用非默认容量/流量/热工参数验证实际改变行为；降容、保存恢复和转换系数变更不重复记账。保留默认回归。
- [ ] 交还入口文件与参数报告后，由PM安排一次锅炉/换热器受影响JUnit、必要真实配置/管网用例和增量assemble，不重跑矿物/生产线测试。

## B. EXT-B-TURBINE-API-01：双轴真实探针

**唯一结果：** 明确并验证可用于正式汽轮机的双轴容量发布与生命周期路线；失败不能以加区块搭建限制替代。

**允许写集：** 新增 `src/main/java/com/iksxh/create_nuclear_industry/gametest/turbine/` 内探针与测试专用注册、`src/main/resources/data/create_nuclear_industry_turbine_probe/structure/` 空测试模板、必要测试资源；未来可复用的轴源/生命周期辅助类可放新增 `turbine/` 子目录，但不接完整生产设备或抢A的入口文件。报告 `build/reports/extension/EXT-B-TURBINE-API-01*`。

**接口：** 基于 `GeneratingKineticBlockEntity`，实际每轴SU除以其生成RPM后才是 `calculateAddedStressCapacity()`；共同所有者提供两份份额，默认各50%。不得复制完整总SU给每轴。测试专用注册应仅在探针namespace启用，不污染正常客户端创造栏。具体辅助类型由执行者报告后冻结到C任务。

- [ ] 创建能由服务端账本切换额度/启停的最小真实轴源，以真实Create网络验证单端、双端合网与分网。
- [ ] 验证停机、外部同速源、拆并网、控制器兼前轴/后轴跨区块、局部机身卸载和重载的缓存清理；不以调用onLoad/onUnload函数代替实际区块覆盖而宣称通过。
- [ ] 检查原生异速/反向接入边界并保留其原生行为；探针不改变Create冲突规则。
- [ ] 使用独立namespace/目录运行最少必要GameTest；明确成功断言、退出码、保存完成，结束仅处理本任务启动的进程。
- [ ] 交付确切可用接口和后续必须复用的验证点。若所有可行技术方案都无法满足跨区块要求，报告真实限制，PM按用户决策规则暂停。

## C. EXT-B-TURBINE-DEVICE-01：配置、账本与三档设备

**前置：** B获得可用结论，A交还共享入口后才写该文件；不能仅凭静态核查跳过真实探针。

**允许写集：** 新增 `turbine/` 运行/客户端类、`config/TurbineConfig.java`、`content/TurbineContent.java`；`CreateNuclearIndustry.java` 的注册/搬移禁令接入；`content/ModCreativeTabs.java` 汽轮机项目；`boiler/StorageOnlySteamFluid.java` 如需复用时保留超临界类型并与A协调只限该文件；新增对应JUnit、`gametest/ExtensionTurbineGameTests.java` 和独立测试结构；报告 `build/reports/extension/EXT-B-TURBINE-DEVICE-01*`。不改D资产写集及既有配方。

**接口交接：**

- 新内容ID严格为方案六件与 `steam`。普通蒸汽FluidType不得复用后仍返回超临界类型；无桶、无世界放置。
- `TurbineConfig` 使用 `create_nuclear_industry-turbine.toml`。三档转子数默认3/6/9，轴向长度派生为转子数+2；配置仅保留三个有效且不同的档位，3×3截面不变。各档流量54/108/162、入口及排汽容量4000/8000/12000均有配置字段；长度/性能改动后仍按当前档位集合验证结构。
- 全局默认128RPM、32768系数、40tick窗口、各类物理口256mB/t、前端份额0.5。配置范围防止非有限容量、数组失控和Create转速超限；不把非法配置作为正常状态悄悄运行。
- 单独纯账本接收settings，测试不依赖全局TOML加载。server owner提供双库存、实际处理率、总SU、每轴份额及结构有效性；客户端仅读同步快照。
- C在制作Java模型/渲染接入前向PM交付D所需的资源名、坐标/轴向、形成状态、分件模型与collision接口。优先分段可复用的八棱体模型，禁止超界JSON、重复端盖与共面叠加。

- [ ] 接入可配置纯账本：1:1交易、40tick实际处理量平滑、两端按一次总量分配、超限/坏配置/停机/卸载行为按方案。
- [ ] 实现三档结构、四向、可选多个端口、唯一owner、普通蒸汽、主动排汽与原生管路接入；底座/角位/错误长度拒绝。
- [ ] 实现红石停止、扳手组装诊断、配置化容量与护目镜快照、存量随控制器安全拆放；禁用Create构造搬移；跨区块SU撤销复用B实际证据。
- [ ] 冻结与D的资源/模型接口，接入成型整体轮廓、匹配碰撞及正确渲染边界，不新增GUI。
- [ ] 定向JUnit覆盖配置不同值、全40tick预算、部分流量、堵塞、保存/降容、时间跳跃；真实GameTest覆盖三档/多口/实际Create排汽、双轴及拆装恢复，复用B已证的未改逻辑。

## D. EXT-B-TURBINE-ASSETS-01：SVG、模型和配方

**前置：** C交付资源/状态接口后制作模型；可以先按方案做独立SVG和六件配方。不得自行改C接口。

**允许写集：** `src/main/resources/assets/create_nuclear_industry/` 下汽轮机/普通蒸汽新增纹理、模型、blockstates；`lang/zh_cn.json`与`en_us.json`仅新增汽轮机/普通蒸汽键（必须读取最新内容后合并，保留A同文件修改）；`data/create_nuclear_industry/recipe/` 六件批准配方、对应loot/tags；`tools/art-assets/` 新增汽轮机导出器/SVG源/预览及必要manifest新增项；报告 `build/reports/extension/EXT-B-TURBINE-ASSETS-01*`。不写Java、不重绘既有素材、不改核心文档。

- [ ] 六配方数量/布局严格按方案，标签复用当前钢/铜板，控制器21格；数据文件可由数据包覆盖。
- [ ] 制作静态壳、端盖、支座、转子/轴分件、可选端口贴片与六件物品显示；SVG风格与现有机器一致。
- [ ] 按C接口输出合法模型/状态，保证三档长度可复用，配置档位长度变化时模型仍能拼接而非只画三张固定贴图。
- [ ] 校验JSON引用、UV、模型范围、共面、端盖交界、GUI/手持变换，生成三档对照预览；仅资源验证，不另起Gradle。

## 集成与人工门

- [ ] PM核对改动写集、复用证据、配置键覆盖、无硬编码回退；一次合并审查（规格与代码质量），按实际缺陷派发定向整改，不机械重复全量。
- [ ] 最终稳定候选增量assemble；检查运行classpath资源/JEI配方与SERVER配置默认文件实际生成。测试可改的模板写在隔离测试目录，不能改用户世界配置。
- [ ] PM维护配置指南、当前状态和精简人工清单；用户从候选启动手测。本批人工通过前不合入主目录功能，明确启动路径，避免用户在主目录旧版本误测。
- [ ] 到人工门保存候选和证据后暂停；冷凝回水及可变锅炉另批，当前不得自行穿插。
