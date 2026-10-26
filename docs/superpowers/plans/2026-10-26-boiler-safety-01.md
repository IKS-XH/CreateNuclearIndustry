# BOILER-SAFETY-01：多泄压阀与固定单阀排汽

## 合同、职责与工作区

用户2026-10-26已批准[锅炉事故整组规则](../specs/2026-10-26-boiler-accidents-design.md)，危险拆除选择直接触发事故。本卡是其第一技术步骤，随后接干烧/超压/危险拆除，不在此中途增加一轮重复用户手测；本卡不提前取消原堵阀/空炉收热保护，不实现事故伤害。两步稳定后集中交付候选，未手测前不合main。

PM保文档/Git，唯一执行者实施/验证，无Git写/核心文档/派发权。复用干净`E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration`，PM从当前main建立`codex/boiler-safety-20261026`。默认携带main的`run/saves/新的世界`：当前无Java游戏进程，最新主目录日志为进入世界前退出，源level.dat为已保存文件；执行者先核源/目标session.lock可独占，目标原29文件世界完整移入本树`run/save-backups/boiler-safety-20261026/`，再整份复制并核数量/bytes/SHA，源不回写/不开世界。主目录3D dirty、日期旧包及缺失的根正式包状态不动。

必读主工程AGENTS、治理5.1/5.2/6、STATUS、冻结设计及本卡；实际使用`C:/Users/IKSXH/.codex/skills/`的minecraft-modding、minecraft-testing，语言涉及资源时minecraft-resource-pack，最后制品minecraft-ci-release。核实际Gradle锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。中文手写注释，不改版本/许可/发布方式。

## 第一技术步骤行为

1. 顶面非棱角允许至少1个阀，数量由合法位置限制。Form持不可变阀集合，CT/结构投影覆盖所有阀，不保留生产用“第一阀”单点假设，不逐tick重扫体积。
2. 每阀固定512mB/t，新配置`valveFlowMbPerTick`替代按汽区格数的旧配置/字段，合法正整数。无旧键迁移或别名研究。保90%/80%炉压迟滞、实际mB/HU和正常汽口下限/双汽纯过滤。
3. 各阀上方1格仅已加载空气算畅通；堵塞/未加载只失去该阀额度，不强载。控制器每tick对缓存阀集合采样，一次调用统一结算并保各口实际额度/排汽粒子；同tick重复调用、重复阀位置或拆装不得放大额度，恢复下一tick才得新额度。
4. 普通/SC按原真实库存分配，保关阀压力不超抽；总上限为畅通阀数×单阀上限，但不能假装所有阀都实际排满。Controller护目镜简示畅通/总阀数和可用总泄放上限，沿现有GoggleTooltip排版，不挡图标。

## 允许写集

J=`src/main/java/com/iksxh/create_nuclear_industry/`，T=`src/test/java/com/iksxh/create_nuclear_industry/`，R=`src/main/resources/`。

- J/boiler/{BoilerStructure,BoilerState,BoilerControllerBlockEntity,BoilerSurfaceDescriptorFactory}.java。
- J/config/BoilerConfig.java。
- T/boiler/{BoilerStateTest,BoilerDualSteamInventoryTest,BoilerStructureAllocationTest,BoilerSurfaceDescriptorTest}.java；已有断言仅适配已批准多阀/固定额度，保物料/压力/缓存/双汽断言，不删测试。
- J/gametest/BoilerSteamSelectionGameTests.java仅Settings新字段访问名机械适配，不重写既有业务。
- 新增J/gametest/BoilerSafetyGameTests.java；复用既有真实锅炉build/owner helper和`create_nuclear_industry_boiler_rework:boiler_empty`模板，锁定API确认后使用本批holder `cni_boiler_safety`，允许轻量focus筛选。不得增加通用测试框架/复制世界；确需模板适配先报准确缺口。
- 锁定API实证补充：GameTestRegistry按模板namespace筛选，跨域借用会得到0测试。允许新增且仅新增R/data/cni_boiler_safety/structure/boiler_empty.nbt，逐字节复制上述既有模板，注解改用本域；不造新几何/框架，Jar已有本域精确排除。首轮0函数启动失败留证，不能计通过。
- build.gradle仅沿现有Jar排除块增加本批BoilerSafetyGameTests及本批测试域的精确排除，不清理历史probe/改依赖/任务结构。
- R/assets/create_nuclear_industry/lang/{zh_cn,en_us}.json仅新增本批阀状态提示，不动其它词条/格式。
- 本树`build/reports/boiler-safety-20261026/`允许实施/技术/审查/失败日志/来源清单及必要窄核验工具；允许本卡世界复制/备份路径和普通编译输出，跟踪包/日志先保原字节。无其它路径写权。

源码签名变化确需额外调用方时，先报精确文件/编译错误再由PM扩集，不为API留无用兼容层。暂不改事故/热伤/熔融物或管理员/工业管网算法，原3项JUnit基线失败不顺带修。

## 验证与接续

先留相关业务RED（两阀成型/独立固定排汽），再最小实现。一次定向四类JUnit及本批真实服聚焦：双阀/三阀合法、无阀或边角非法、逐阀遮挡/非空气/恢复、512独立及合计、体积不放大、迟滞与库存/HU守恒、同tick去重/保存后恢复、CT所有阀角色投影。优先共用真实夹具与同一次运行，旧完整蒸汽/工业网络矩阵不机械重跑；SKIP与业务分开，进程/保存实证。

先交简短EXECUTION/SOURCE和阶段01结果，暂不打JAR。可在本树报告TECHNICAL-02.md只读核第二步骤：持续真实收热/过热、持久事故/全维度共享预算、普通拆除/潜行扳手最终取消后提交、携带掉落的实际接线/必要精确写集；不先实施02。PM据实际入口落02卡后同执行者接续，一次最终组合窄审核两步最终差异，PM提交准确源后唯一增量jar，最后一次集中手测，不自行Git写或正式包提升。
