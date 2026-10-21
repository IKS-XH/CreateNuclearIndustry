# ACCIDENT-THERMAL-04B：熔融物本体、持续熔穿与凝固

## 目标、来源与职责

按[2026-10-21修订合同及最终参数确认](../../proposals/2026-10-20-accident-thermal-corium.md)，让有限真实熔融物在世界中独立流动、溶蚀、放射和冷却；原反应堆消失不影响它，凝固后回到已验固体/封存链。旧总计划04仅留历史接线，不能覆盖本卡。

- 用户已确认04A手测及本卡20/40tick、50%无额外体积上限。基准`9f53a7d5218d1a51b6fdc43bbeb98735a72d9acd`，同目录`E:/MyMC/NewMod/Create_NuclearIndustry-accident`/`codex/accident-thermal-04b`；原“新的世界”原位保留，不从main覆盖用户进度。开工日志全部维度已保存、无本树游戏进程及session.lock独占已核，同目录切分支无需搬存档。
- 唯一执行者实现/验证，随后一次组合窄审；PM管理核心文档/Git，不写功能或测试。执行者仅下列写集、无Git写/派发/核心文档权限，不启动用户世界；主目录3D工具及音效/美术树不碰。
- 必读AGENTS、治理5.1/5.2/6、STATUS、本卡/修订合同、04A报告及本树`build/reports/accident-thermal-04b-20261021/TECHNICAL.md`。实际使用本机`C:/Users/IKSXH/.codex/skills/`的minecraft-modding、minecraft-testing、minecraft-resource-pack，制品阶段minecraft-ci-release。锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，核实际Gradle；中文手写注释。

## 冻结行为与配置

1. 新事故真实废料数×0.5向上取整，逐份选为初始熔融团块，其余沿04A固体；无额外196或其它体积上限。每件已用/乏燃料仍恰好1废料，未用原件返还、工质全损；不能按全部库存或液面分支复制物料。
2. 每团空位20个加载tick最多移动1格、40个加载tick最多溶蚀1格；优先下方，下方可蚀时等待自身溶蚀进度而不是靠横移绕开每次处理。下方被其它团占用可向已加载相邻空格腾挪，避免原地往返；一份一格，不分裂/合并。无原堆距离/水平包围盒/累计删块上限，黑曜石不特殊抗蚀；基岩、不可破坏、最终取消保护仍生效。
3. 每团6000加载tick后凝固，再进入既有2400加载tickHOT；相邻已加载水使冷却×4。移动不重置剩余寿命/处理进度，卸载暂停且不追补、不强载；配置重载不改已有团的冻结规则。逻辑到期即无熔融热伤/溶蚀，物理转固可等待预算。
4. 熔融接触每实体同类20tick至多4HP直接热伤、点燃5秒，普通火抗有效；辐射仍只由真实载荷产生，沿既有4D/s源强/索引预算，不加第二份剂量算法。普通桶、管网、开口泵、软管不能抽取或删除它；不新增桶/热工质/制造配方。
5. 全世界合法出现的熔融格都建立本体状态，事故只负责第一次交出物料。原堆/仪表/ledger/complete不是运行前提。指令产生的零载荷熔融物仍有本体热、移动与溶蚀；不凭空产核废料/辐射，凝固为空残骸，保持04A空残骸不伤害。
6. 新配置键：`moltenWasteFraction=0.5`、`moltenCoolingLoadedTicks=6000`、`moltenMoveIntervalTicks=20`、`moltenCorrosionIntervalTicks=40`、`moltenContactDamageHp=4`、`moltenContactIgniteSeconds=5`。复用并冻结04A水倍率和凝固后热参数；合法范围、有限数/溢出与整组回退按既有模式，不引入距原堆/累计删除/额外体积配置。

## 架构和事务不变量

- 独立`MoltenCoriumState`存UUID、当前位置/修订号、唯一真实payload或空、剩余加载tick、移动/溶蚀进度、初始/移动/凝固交接收据。可序列化进同一ReactorAccidentSavedData文件，但有独立位置/区块/活跃索引；来源ID仅审计，删除来源ledger也继续运行，普通tick不扫历史账本。
- Column→本体在同一SavedData中唯一交接，随后原Column清payload/settled；液格只作投影、不存第二份物品。凝固→Corium BE沿身份收据单次接收，本体清payload后只保HOT引用/终态收据；原BE唯一take/掉落链保持。不能从审计原件或残留液面重发废料，普通保存读回可恢复，跨文件崩溃原子性不作未经证明承诺。
- 本体物理Fluid/LiquidBlock复用原生液面、原版熔岩still/flow贴图；关闭原生spread/randomTick/水岩浆反应，实际有限搬移由共享调度完成。getFlow/邻水/目标查询均先核hasChunkAt/getChunkNow；不强载，不借原生无限源伪装。
- 原事故动作、04A热/火与本体共用同维度64世界块写/t及保守收据额度，同tick重入不刷新。搬移先预留旧格删除/新格放置两份额度，转固、容器/多格连带写分别计；预算不足等待但不能延长热危险。原生物品掉落与块写预算口径在报告分别说明，不吞物品来满足数字。
- 腐蚀先限定内部环境作用域，专用FakePlayer投递BreakEvent，最终取消/异常保留目标；不调用带提交副作用的CommonHooks.fireBlockBreak当探针。仅本模组手动停机预检识别该作用域，其他监听器最终取消保留，原手动事务/Mixin语义不变。回调后重验加载/原状态/BE身份再动账。
- 原生普通容器走loot/onRemove原有掉落，不预清库存或转成事故废料；核燃料端口须从真实BE移出一次并立即使旧快照/能力失效，多格装配台的全加载和连带写先预留。不能沿旧“跳过所有BE”回避容器，也不能静默吞物料；未知无法证明安全的特殊回调先报PM入口，不扩大研究任意模组容器。
- 写前替换守卫仅保护已登记熔融格，本体授权移动/转固用有界作用域令牌；普通放块/流水/软管不能无账抹除。保留有权限原版setblock/fill的有意管理员清除，按显式指令作用域终止本体并记管理删除，不重生/掉第二份物料；不能凭FakePlayer名字、任意flags3或全局豁免绕过。其它模组直接section写入不承诺兼容。

## 精确允许写集

J=`src/main/java/com/iksxh/create_nuclear_industry/`，R=`src/main/resources/`，T=`src/test/java/com/iksxh/create_nuclear_industry/`；花括号仅枚举文件。

| 允许路径 | 最小职责 |
| --- | --- |
| 新增J/accident/{MoltenCoriumParameters,MoltenCoriumState,MoltenCoriumEffects,MoltenCoriumFluid,MoltenCoriumBlock}.java | 冻结参数、本体/索引/收据、移动/溶蚀/热接触及原生fluid投影；环境/写许可作用域放Effects内，不另开通用框架 |
| 修改J/accident/{ReactorAccidentLedger,ReactorAccidentSavedData,ReactorAccidentService,ReactorAccidentExecutor,ReactorCoriumBlockEntity,AccidentThermalEffects}.java | 初始选择/交接、同文件持久化/共享额度、本体到HOT支持，原complete不延长；不重写已验火实现 |
| 修改J/config/AccidentConfig.java、J/content/AccidentContent.java；新增J/accident/client/AccidentThermalClientEvents.java | 配置/FluidType与Fluid注册、客户端纹理及渲染层，不改加载器/版本 |
| 修改J/structure/ReactorStructureLifecycle.java、J/blockentity/{ReactorPortBlockEntity,ReactorInstrumentPortBlockEntity}.java | 仅环境探针和真实燃料移交/快照失效，保留原手动停机/旧事故空口重绑/管理员语义 |
| 修改J/production/{ShieldedAssemblyBlock,ShieldedAssemblyPartBlock,ShieldedAssemblyStructure}.java | 仅环境溶蚀时全加载、多格库存/连带写预算接线，普通装配流程不变；能不修改的文件不为凑清单触碰 |
| 新增J/mixin/{CoriumHosePulleyRestrictionMixin,CoriumWorldReplacementMixin,CoriumCommandReplacementMixin}.java；修改R/create_nuclear_industry.mixins.json | 精确软管发现/出队删除，已登记液格写前保护，原版授权setblock/fill作用域；禁止改变其它流体/普通命令 |
| 修改J/radiation/RadiationSourceIndex.java | 独立chunk索引的真实本体载荷，在现有栈预算内采样，移动/交接撤旧源和位置去重，不改剂量/护具 |
| R/assets/create_nuclear_industry/blockstates/molten_corium.json、models/block/molten_corium.json、lang/{zh_cn,en_us}.json；R/data/create_nuclear_industry/loot_table/blocks/molten_corium.json | 借原版流体纹理、空掉落和名称，不画新贴图，不建抗蚀标签/桶/配方 |
| 修改J/gametest/AccidentExecutionGameTests.java及T/accident/AccidentLedgerTest.java；新增T/accident/MoltenCoriumStateTest.java | 复用既有域/筛选/模板，必要旧断言适配新物理相态但不能删物料/安全断言，不改构建规则 |
| 本树build/reports/accident-thermal-04b-20261021/ | EXECUTION、REVIEW、必要失败/成功日志及候选身份；普通编译输出允许。执行者不写核心docs，跟踪包/日志先备份，变化交PM恢复 |

未列路径禁止；尤其ReactorBreakFinalizationMixin、热工/控制算法、管理员、剂量/封装算法、工业网络运输、Gradle及用户世界。确需扩写先给准确调用链，由PM处理，不自行重构。

## 实施与验证顺序

1. 先取得本次业务RED：有限数量/唯一载荷、20/40加载tick、6000→HOT/水×4、来源不存在仍继续、越过旧6格/2格/64累计边界、同维度共享64写。定向`.\gradlew.bat test --tests '*MoltenCoriumStateTest' --tests '*AccidentLedgerTest'`，不clean/强制重跑。
2. 最小native流体/本体/配置/独立来源与初始交接，再接移动、腐蚀容器/保护、凝固与辐射。先核本地注入签名再启动真实服；实际API与预检不符就报具体差异，不借一般兼容调查扩大范围。
3. 既有真实域新增聚焦键`molten_corium`，一次相关服集中核：真实事故/50%份额、独立零载荷世界入口、真实移动/熔穿黑曜石/基岩阻挡、超旧边界与累计64格、物料与辐射单源、伤害/火抗、邻水冷却/凝固采掘、普通箱子/核燃料端口/装配台物品守恒及连带写、最终取消、写前替换/软管旧队列、双事故及无事故本体共同预算。同步复用04A热/火与shutdown相关业务，旧默认相态变了可明确给旧夹具0熔融份额，必须另有新默认50%业务证明。
4. 当前版本BE/DataStorage读回与正常保存重进核本体位置、剩余时间、载荷、交接及无来源运行；优先复用既有resume入口做一次聚焦进程恢复，不增框架。物理卸载/真人多人未实际观察不补记，SKIP不计业务。失败保留；仅改动/失败相关项复验，不重开原事故/管理员完整矩阵。
5. 源码/必要自动验证稳定后交一份简短EXECUTION与准确SOURCE/资源清单；一次组合窄审读已有证据，重点库存在三阶段唯一、替换/抽取绕行、容器取消后副作用、预算/强载和旧拆除安全门。PM核准来源提交后唯一一次增量jar，原始产物/测试排除/来源class资源/SHA交报告目录，不覆盖main正式包或自行Git写。
6. 最后集中客户端体验液面/流动、持续下烧/基岩、接水凝固/回收、热与辐射、容器物品及正常重进；人工通过前不称视觉/完整玩法验收。本批不顺带事故教学或主分支合入。
