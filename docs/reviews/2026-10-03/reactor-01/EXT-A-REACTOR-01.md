# EXT-A-REACTOR-01 执行交付报告

**交付状态：** 执行者已完成实现并提交审查；项目经理验收与用户客户端手测仍待进行。候选未合入 main。

**代码基线：** `5353152867a6c4b6ecb4f1ec0f74c20849ae24d5`。本批开始后项目经理只更新了任务/治理文档，当前候选 HEAD 为 `3d43df9e229e711c6bb1b3cc6aceb37eda5ee7c5`。隔离工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支 `codex/ore-acquisition`。

## 实现

- 新增 `ReactorCraftingContent`，单独注册六种材料、屏蔽混凝土方块及五种 Create `SequencedAssemblyItem` 半成品；未扩展 `P1ContentIds.FORMAL_IDS`，未改反应堆运行状态、燃料组件耐久、Create 管网或 GUI。屏蔽混凝土硬度 3、爆炸抗性 6、石材音效、镐采掘且至少铁镐，掉落自身。
- 在模组入口接入注册，并把七种可直接使用的新产品加入模组创造页；五种加工中半成品不进入创造页。
- 增加任务卡冻结的 16 条原生配方：石切、压片、机械手、搅拌、序列装配和工作台。五条序列配方均一轮、必成、无热；换料端口按完整 `create:deployer`、工业传感器、密封环顺序加工。屏蔽混凝土输入标签只枚举 16 种硬化混凝土。
- 增加钢杆细分/父标签、混凝土输入标签、混凝土挖掘标签和掉落表，以及双语共 12 个新身份键。美术 Agent 交付 12 张 16×16 纹理、11 个物品模型和屏蔽混凝土方块资源；其报告记录旧 114 张游戏纹理的哈希保持。
- 按 PM 指定修正美术导出器：取消“原纹理必须恰为 114 张”的硬限制，精确排除本批 12 条相对纹理路径，其他现存 PNG 仍逐路径比较前后 SHA-256。重新运行脚本成功，24 个本批游戏/生成 PNG 的运行前后哈希一致；完整脚本 stdout 和资源核对在 `EXT-A-REACTOR-01-ART/`。

## 验证

实际技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 用于按项目锁定版本实现 NeoForge 注册、原生配方和资源引用；`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md` 用于定向 GameTest 范围、配方管理器检查和真实 Create 机器测试。使用版本为 Minecraft 1.21.1、Java 21.0.7、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。

`P1DataContractTest` 五项通过，0 failures；本次未增加重复镜像 JSON 的 JUnit。定向 GameTest 使用隔离命名空间 `create_nuclear_industry_reactor_crafting` 和目录 `build/gametest-reactor-crafting`，七项 required tests 全部通过：

- 五种工作台配方实际匹配并核对输出身份/数量；检查钢杆的 `c:rods/steel` 与 `c:rods` 标签。
- 运行时配方检查陶瓷普通加热、混凝土无热要求和输入/输出数量；真实 Create Basin、Mixer、Blaze Burner 验证无热不加工，投入普通燃料后产两份工业陶瓷；无热 Mixer 消耗一块硬化混凝土和一粒铅并产一块屏蔽混凝土。
- 五条序列配方均已加载，代表性检查最长换料端口序列；真实 Deployer 和 Press 按完整机械手、工业传感器、密封环的顺序推进并得到一只成品，逐步确认输入消耗。
- 正式八种 P1 方块注册身份与既有 5×5×5 纯结构扫描模板组合检查通过，包含 24 个燃料柱结构块、一个空内部控制棒柱及控制棒驱动器；该项没有在用户客户端搭堆，也不证明客户端/Ponder实物体验。

实际命令和原始证据路径见 [`verification.json`](EXT-A-REACTOR-01/verification.json)，包括五项 JUnit 的 XML、成功 GameTest 日志、首次启动的崩溃报告和最终 JAR。第一次 GameTest 启动因注解模板名错误地重复带 namespace，导致 `ResourceLocationException`；修正为 holder namespace 下的短模板名后重跑通过。失败发生在测试结构准备阶段，没有业务用例运行。

增量 `assemble` 成功，制品 `build/libs/create_nuclear_industry-0.1.0.jar` SHA-256：`E457541CAA5691454D592BA5825D86A7098B7D6129F5FCD2AE44976655A61942`。编译只出现仓库既有 `FluidType.initializeClient` deprecation 警告。构建与原始日志位于隔离目录的 `build/gametest-reactor-crafting/logs/latest.log`；Gradle 测试 XML 位于 `build/test-results/test/`。

## 未完成的验收门

本轮没有运行 Minecraft 客户端。真实 Create 配方/机器自动测试已覆盖本批代表工序，但不能替代用户在客户端检查新材料外观、JEI、其余工作台/序列配方的实际操作和 Ponder 固定堆搭建。按任务卡保留这些人工验收并与 01B 一起进行；本报告不宣告人工通过或任务最终关闭。
