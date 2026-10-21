# ACCIDENT-THERMAL-04A：高温残骸与有限火点

本卡是[实施计划04](2026-10-21-accident-thermal-04.md)中不依赖未决流动参数的独立实现范围，用户2026-10-21要求“继续下一步”。玩法依据仍为[已批准热危害合同](../../proposals/2026-10-20-accident-thermal-corium.md)；不重新冻结参数、不提前实现熔融物。进度/结果只在[STATUS](../../STATUS.md)维护。

## 工作区、职责与基线

- 在`E:/MyMC/NewMod/Create_NuclearIndustry-accident`同目录切分支`codex/accident-thermal-04a`，来源`37f50b9e3425992f74beecb8ecd6e2c79514257e`。仅同目录切分支，按治理6保留原位`run/saves/新的世界`，不覆盖、合并或回写世界。开工核日志全部维度已保存且无本树游戏进程，检查session.lock可独占；执行者不启动用户世界。
- 唯一执行者实现/验证，一次组合窄审后交候选。执行者无Git写、核心文档或验收权限，PM只管理文档/Git，不写功能代码。主目录3D工具无关dirty不碰。
- 必读AGENTS、治理5.1/5.2/6、STATUS、上述两合同、原事故及拆除修复证据；实际使用本机`C:/Users/IKSXH/.codex/skills/`的minecraft-modding、minecraft-testing、minecraft-resource-pack，打包阶段minecraft-ci-release。
- 核实际版本：MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；不升级、不新增构建规则，手写注释中文。

## 本批玩家可观察结果

1. 新事故产生的真实废料固体残骸初始为高温；每实体每20tick最多一次2HP直接热伤并点燃3秒，同类多格不叠加，正常火抗生效；无真实载荷的空残骸不能凭外观灼烧或发辐射。
2. 高温经过2400个实际加载tick冷却；相邻六面已加载水每tick扣4，否则扣1。卸载暂停、无离线追补、不强载；冷却后仅热危害消退，物料/辐射不变。高温可提前采掘，仍只掉既有单件废料，不新增热态掉落物。
3. 同次事故原破裂区内最多8个可灭火点、各最多600个加载tick；无蔓延、无额外门户副作用、已灭不复燃。位置/剩余时间/尝试与终止收据持久保存，过期动作不能删掉后来方块。
4. 原事故触发/旧爆裂限额、工质全损、未用组件返还和已用组件每件恰好一份废料不变。本批暂全部仍为固体，熔融份额/凝固/流动/溶蚀与取液限制留04后续，不注册未实现流体或写入其未定配置。
5. `complete()`保留旧含义，热活跃另记；热未结束也不能重新锁住已结算空口。热/火实际世界写与原动作共用同维度64/t预算，不按事故数扩大；到期先失去危险，物理清理排队仍计预算。
6. 客户端冷热可辨，先借原版高温/火焰资源，不另做美术。服务端持有权威状态；无GUI、配方、音效或粒子扩项。当前格式保存重进不重置热计时/物料/火点；不做旧格式迁移。

## 唯一允许写集

J=`src/main/java/com/iksxh/create_nuclear_industry/`，R=`src/main/resources/`，T=`src/test/java/com/iksxh/create_nuclear_industry/`；花括号仅枚举文件。

- 修改J/accident/{ReactorAccidentLedger,ReactorAccidentSavedData,ReactorAccidentService,ReactorAccidentExecutor,ReactorCoriumBlock,ReactorCoriumBlockEntity}.java。
- 新增J/accident/{AccidentThermalParameters,AccidentThermalState,AccidentThermalEffects,AccidentFireBlock}.java。只包含本批固体/火状态、索引及调度，不占位实现熔融阶段。
- 修改J/config/AccidentConfig.java、J/content/AccidentContent.java；确有客户端渲染层需要时新增J/accident/client/AccidentThermalClientEvents.java。
- 修改或新增R/assets/create_nuclear_industry/blockstates/{reactor_corium,accident_fire}.json、R/assets/create_nuclear_industry/models/block/{hot_reactor_corium,accident_fire}.json、R/assets/create_nuclear_industry/lang/{zh_cn,en_us}.json、R/data/minecraft/tags/block/fire.json、R/data/create_nuclear_industry/loot_table/blocks/accident_fire.json。
- 修改J/gametest/AccidentExecutionGameTests.java、T/accident/AccidentLedgerTest.java；新增T/accident/AccidentThermalStateTest.java。
- 报告/原始日志/候选仅该树`build/reports/accident-thermal-04a-20261021/`，正常编译输出允许。跟踪JAR/日志若生成变化，先备份并报PM，不自行恢复。

其它路径禁止；尤其Port、Lifecycle、CommonHooks最终事务、热工/停机评估、管理员、剂量算法、封存链、工业管网/软管、共享构建与用户世界。发现必须扩写的真实入口，先报告具体原因，由PM核范围。

## 实施与证据

- [ ] 先补本次实际RED：2400/接水×4/暂停、热与旧complete分离、火点8/600/灭后不复燃、同维度64写；定向运行`.\gradlew.bat test --tests '*AccidentThermalStateTest' --tests '*AccidentLedgerTest'`。
- [ ] 实现冻结配置、热状态/索引与单件残骸、有限火注册/调度；只遍历热活跃位置，未加载不读取。计时与世界写预算分别记，原载荷不复制到热记录。热源撤除/采掘/卸载使引用正确失效，持续空闲不扫全部历史事故。
- [ ] 在既有`cni_accident_execution`域添加聚焦键`thermal_solid_fire`，通过既有`CNI_ACCIDENT_FOCUS`入口跑一次相关真实服。真实验证新事故物料守恒、伤害节流/火抗、接水快冷、火点扑灭/到期、两事故总预算、当前格式保存恢复、热活跃期间旧空口重建/拆除；旧尾热/接口退出矩阵复用，不重跑全量。明确SKIP不计业务、退出码及未观察到的卸载/多人边界。
- [ ] 交EXECUTION.md与必要失败/通过日志；一轮组合窄审读现有证据，不另跑相同测试。整改只检查改动及其影响。
- [ ] PM核写集/来源并提交后，唯一一次增量jar交报告目录，保留原始Gradle产物并绑定已提交源码、class/资源、SHA；不覆盖main正式包、不把未提交源码包称正式包。
- [ ] 最后集中手测高温外观、接触/火抗、灭火、接水冷却、采掘封装及正常重进。本卡完成不代表有限熔融物已实现，后者等两个参数答复后接续。
