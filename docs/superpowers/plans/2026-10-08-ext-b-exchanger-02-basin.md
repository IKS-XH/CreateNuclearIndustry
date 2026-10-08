# EXT-B-EXCHANGER-02：换热器工作盆供热实施卡

> 执行方法：用户已授权PM自动派发，由执行者实现，单轮独立审查合并规格与质量。通用技能的重复审批、逐层复审和执行者提交步骤以本仓库治理为准。

**目标：** 现有核换热器可替代烈焰人燃烧室给Create工作盆供热，按成功批次结算与锅炉相称的真实HU，保留原生配方和冷热守恒。
**结构：** 在现有服务端热账本增加工作盆需求充热/批次费用事务；桥接仅识别本设备正上方盆，以原生匹配探测需求，在原生apply实际提交前预留、成功确认、失败释放。
**技术栈：** Minecraft1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6；禁止升级依赖。
**合同：** [用户已确认的具体方案](./2026-10-08-heat-exchanger-basin-proposal.md)；源码审计在候选`build/reports/extension/EXT-B-EXCHANGER-02-PREP.md`，不视为运行证据。
**基线：** main c755c53 / 候选5bdd085；PM本次文档提交后的HEAD须由执行者记录。候选路径`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，主目录功能不改。

## 永久边界

- 执行者实际读取AGENTS、治理5.1/5.2、本卡、具体方案和审计；实际使用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`。中文手写注释遵循AGENTS。
- 执行者不写Git、核心文档/治理/状态，不派发其他代理，不操作用户客户端、世界或运行配置。仅PM管理文档、Git和验收。
- 保留`logs/debug.log`、`logs/latest.log`及三个`tools/**/__pycache__/`既有状态。不清理、不stash、不改主工程代码。
- 无GUI、不新增设备/配方/素材/全世界扫描。仅核热冷却剂给盆供热，普通蒸汽冷凝保留，拒收超临界蒸汽。
- 已归属高压锅炉的换热器排除；盆和Create储罐上方用途互斥，原有锅炉/冷凝分支语义不改，不研究旧存档迁移。

## 唯一允许写集

根前缀`src/main/java/com/iksxh/create_nuclear_industry/`：

- 修改`heat/HeatExchangerState.java`：同一HU储备增加盆需求充热及可撤销批次预留，锅炉tick行为保留。
- 修改`heat/NuclearHeatExchangerBlockEntity.java`：盆负载分支、只读可用性、状态同步与生命周期；冷热共享库存沿用`HeatExchangerLine`事务，不修改该类。
- 修改`config/HeatExchangerConfig.java`：两项等效tick配置及费用快照，不改旧参数默认值。
- 新建`heat/HeatExchangerBasinBridge.java`：限定设备、源可用性、原生候选探测、费用/热级、原生提交事务；必要辅助类型只放`heat/basin/`。
- 新建`mixin/BasinHeatLevelMixin.java`、`mixin/BasinRecipeHeatMixin.java`、`mixin/BasinOperatingMixin.java`及必要`mixin/BasinOperatingAccessor.java`、`mixin/MechanicalMixerAccessor.java`；访问器仅暴露需求检查所需原生状态，不重写配方引擎。PM于2026-10-08核对后明确增加`BasinOperatingMixin.java`：仅将原生候选匹配调用导向盆底换热器的短作用域探测，保留Create的候选排序和过滤，非本设备原样调用。
- 新建`gametest/ExtensionHeatExchangerBasinGameTests.java`，相关独立测试域模板只可放`src/main/resources/data/create_nuclear_industry_ext_b_basin/structure/`。
- 修改`src/main/resources/create_nuclear_industry.mixins.json`及`assets/create_nuclear_industry/lang/zh_cn.json`、`en_us.json`，只添加本批注册及玩家提示。
- 修改`src/test/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerStateTest.java`；必要新测试限`HeatExchangerBasinTest.java`（同目录）。
- 报告只可写`docs/reviews/2026-10-08/exchanger-basin-02/IMPLEMENTATION.md`及`build/reports/extension/EXT-B-EXCHANGER-02/`。PM维护其它文档。

如果最小原生接入需要写集外入口，先报告具体必要性，PM明确扩展路径后才能写；这不要求重新确认已批准玩法。

## 一个完整实现任务

- [x] 核对现有账本和锁定Create源码，并记录原生query/match/apply及mixer阶段；需要热的配方才需求充热，NONE/盆压片原样免费。
- [x] 增加少量有意义的定向断言，体现费用、模拟无扣账、失败释放、一次结算/连续批次以及锅炉平衡。默认普通40/超级80HU；`huPerLevel=2`时80/160HU，额定充热功率同步变化。每个等效tick配置范围1～1200；费用超容量、NaN/非正等组合安全停用。
- [x] 实现工作盆账本路径：`tickBasin(now, demandHu, cfg, exchange)`每源每tick最多充热一次，最多转换到本批缺额且不越既有HU/t上限；无需求不转换/耗HU，待机保留真实余量。盆路径不执行锅炉的按tick扣热/40tick失效分支。
- [x] 实现费用预留对象：足额时扣入本次预留，正常成功确认；预检失败或正常退出释放到同一储备。同一预留不能二次提交，重入不能复用HU。源失效/拆除不得发布有效热，不向新实例退款；持久化不得制造正HU。
- [x] 源BE tick只读探测原生可加工候选，限定盆正上方搅拌器有效速度、未过载、输入输出可加工；优先顺序/过滤/流体和容器逻辑依Create，短作用域热探测必须finally复位，不写方块状态/配方/缓存热。
- [x] 足额后唤醒原生盆检查，真实getHeatLevel绕开本设备的原生过时缓存，返回所需已付热。真实apply前再次检查有效动力/源/费用，预留后调用原生apply；按结果确认或释放，连续加工每批重新付费。无效输入/输出不持续充热，概率输出仍由Create生成。
- [x] 保存真实储备、恢复后重新检查负载；不保存可跨实例重复兑现的临时预留。区块停tick、卸载/拆机、盆移除、模式冲突立即使热查询无效；热转换1:1且只在服务端。
- [x] 更新护目镜：区分工作盆待料/待热/热储备与普通锅炉等级，中文短句不显示开发术语；仍用公共GoggleTooltip缩进，不改模型。
- [x] 在稳定实现上一次定向JUnit+增量assemble，必要真实GameTest集中一次运行；写报告后冻结，交PM单轮独立审查。不提交Git。

方法名可按既有风格细化，职责、成本单位及事务顺序不得改变；新增跨包入口说明服务端/客户端与不变量。

## 审查重点与必要证据

1. 单台预留中的HU与另一台/下一批不可重复使用；测试固定费用下的失败释放和连续批次。
2. 配方探测与同tick热缓存不得变成免费热或扣账入口；真实普通/超级热加工至少各一个用例，不以标签/JSON存在替代。
3. 空盆、缺料、停转/过载、堵塞不继续充热；真实搅拌器的一个恢复/停转代表用例配合账本断言，不复制全设备负例矩阵。
4. Native apply不提供任意第三方handler回滚；本批只保障热费用在原生预检失败时释放，不伪称重写上游全部事务。测试原生过滤及容器/概率产物一个代表，不另建matcher。
5. 锅炉旧tick与冷凝路径保留；复用`HeatExchangerStateTest`、`CondensationStateTest`必要覆盖，不全量GameTest，不改共享桥接和锅炉参数。

推荐命令：`./gradlew.bat test --tests "com.iksxh.create_nuclear_industry.heat.HeatExchanger*Test" --tests "com.iksxh.create_nuclear_industry.heat.CondensationStateTest" assemble`。实际JUnit类名按允许写集核对。

GameTest沿用现有Gradle定向入口：`./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_ext_b_basin -PgameTestDirectory=build/gametest-ext-b-basin`；模板/注解须依锁定版本核对，不临时扩建框架。只由实现者持有构建和测试进程。已知退出保存卡住按治理5.1保留断言和退出状态，核对本轮自有进程后处理，不杀用户进程。

交付报告记录实际技能、差异路径、费用公式与默认HU/mB、测试命令/结果、GameTest真实断言和退出状态、制品路径/哈希及仍待手测。构建成功不代表机器验证；用户下班手测由PM集中清单记录，与汽轮机播放分开验收。仅费用/提示整改不反复跑未变测试。

## PM验收与停止点

**候选进度（2026-10-08）：** 实现及对应整改复核通过，功能提交`023337a`；定向账本19项、真实GameTest4项与增量打包通过。[实施](../../reviews/2026-10-08/exchanger-basin-02/IMPLEMENTATION.md)、[审查](../../reviews/2026-10-08/exchanger-basin-02/REVIEW.md)分别记录证据与初审问题。集中候选已按[手测清单](../../reviews/2026-10-08/exchanger-basin-02/MANUAL.md)就绪，人工门仍未通过，不视为主工程已合入。

PM审查通过后，仅提交同级候选的本批代码和文档；主工程仍保留已验收功能。候选集中手测：铅玻璃普通热、Create圆石熔岩超级热、低流量/停转或堵塞恢复、冷热守恒与提示，加上汽轮机三幕播放。等用户确认两项实际通过后再整合，不自动接换热器思索或其他主线。
