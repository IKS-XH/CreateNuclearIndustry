# 多级专用管网：分级流量实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: 使用superpowers:subagent-driven-development。主PM按既有自动授权派发一位执行者顺序完成本卡，一次独立规格/质量组合审查；执行者无Git、核心文档修改或子代理权限。技能通用提交/重复复审/开工全量步骤由AGENTS与治理5.1覆盖。

**Goal：** 热冷却液和超临界蒸汽只经专用管泵运输，新增二/三级管与阀及单一专用泵，现有反应堆冷热口2048、锅炉冷热口2048、水汽口4096mB/t，保留Create储罐与其他普通流体运输。

**Architecture：** 独立服务端拓扑与预算调度，管段不存液、泵最多一tick额定周转缓存；所有源/目标交易均有实际路径、动力和预算授权。对受限流体收紧标准能力并适配同份Create库存，同时在原生Create网络真实drain前拒绝受限交易；机器热工与本模组已有SharedFluidFill修正保持。

**Tech Stack：** Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；不升级依赖或改变许可证。

**Spec：** [已确认规范](../specs/2026-10-12-tiered-fluid-network-design.md)，[锁定Create事务核查](../../reviews/2026-10-12/tiered-pipe-preparation/RESTRICTION-STUDY.md)。用户最后两次修订取消工业口，锅炉水汽4096、其余冷热2048；均已确认，不再等待开工许可。

## 全局约束、基线与唯一写者

- 单泵16384mB/t@256RPM、16SU/RPM（4096SU@256RPM），公式floor(额定Q×clamp(abs(RPM)/256,0,1))，无额外最低转速；每侧搜寻256管段、网络最多4096节点。二级管/阀8192、三级32768；阀开度0～100%，红石高电平强制关闭。所有数字SERVER配置；非法或溢出不交易并可读提示。
- 新内容只有reinforced_fluid_pipe、ceramic_fluid_pipe、industrial_fluid_pump、reinforced_fluid_valve、ceramic_fluid_valve五种ID。不新增工业流体端口、专用储罐、泵升级链、管网压力/泄漏/破裂、管理员或新原料。
- 反应堆复用reactor.perPortFlowMbPerTick，默认128→2048；锅炉须分开冷热2048、水汽4096设置与Settings。各物理口主动/被动/专用共用额度，SIMULATE不收费；端口位置/过滤/制造成本不改。
- 库存与热账本仅由原机拥有。SC罐可储存，但SC邻罐直推与原生加压贡献禁止；普通steam主动输出和汽轮机排汽保持。现有锅炉温压、双汽独立库存共容量、换热器直列和核热供盆/炉不改。
- 本批从已验收main生产源e99e09f建立同级独立树，不夹带待验16³锅炉30020e4或美术树未验源。主目录dirty project.md、模拟器、日志和艺术稿不覆盖/提交；不因本卡扩大美术消费者写集。
- 工作树由PM创建。按用户上批明确的当前候选测试世界继续携带：Create_NuclearIndustry-art-integration/run/saves/→新树run/saves/，先核08:52:31完整保存退出及无游戏进程；原件保留，同名目标先完整备份，核数量/SHA。不回写、不链接、不纳Git、不研究旧版本兼容。
- 执行者实际阅读AGENTS、本卡、spec、minecraft-modding/testing，test-driven-development与verification-before-completion。技能路径C:/Users/IKSXH/.codex/skills/及plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/。所有手写注释/Javadoc中文，状态/单位/权威边界与不变量说明真实行为。
- 仅一位执行者持有本批代码、JUnit、真实GameTest及最终构建。必要路径缺口先报告PM具体文件及理由，PM可在确认范围内扩卡；不得自行改docs、依赖、Git或用户世界。报告不会关闭人工门。

## 允许写集与职责

Java根J=src/main/java/com/iksxh/create_nuclear_industry，测试根T=src/test/java/com/iksxh/create_nuclear_industry。

| 写集 | 职责与限度 |
| --- | --- |
| 新J/industrial/** | 本批专用管/泵/阀方块及实体、掉落携液物品、服务端拓扑/事务/预算、机器/Create端点适配、流体限制策略。职责拆文件，不把整个系统挤进一个类；不得借包权限创建管理员/压力机制 |
| 新J/config/IndustrialFluidConfig.java、新J/content/IndustrialFluidContent.java | 新配置与仅五种内容、实体、物品、数据组件及能力注册；实际API由锁定代码核实，不复制高版本技能示例 |
| J/CreateNuclearIndustry.java、J/content/ModCreativeTabs.java | 新内容/配置/调度接线与创造页；唯一持液泵构造搬移防护，不重排其他注册或动Ponder插件名单 |
| J/config/P1ServerConfig.java | 仅反应堆perPortFlowMbPerTick默认2048及说明，不改尺寸/产热/损伤等参数 |
| J/config/BoilerConfig.java、J/boiler/BoilerState.java、J/boiler/BoilerControllerBlockEntity.java、J/boiler/BoilerSteamPressure.java | 分开冷热2048与水汽4096配置/Settings/实际物理口预算，停止SC原生加压/邻罐直推，适配工业事务；原温压/容量/尺寸默认5..11保持 |
| J/reactor/ReactorCoolantFluidHandler.java、J/blockentity/ReactorPortBlockEntity.java、J/blockentity/ReactorInstrumentPortBlockEntity.java、J/reactor/ReactorCoolantPortFlowBudget.java、J/reactor/ReactorCoolantLedger.java | 现有口同预算与受限热液能力/私有交易接入，遥测与真正handler一致；不新增端口身份，不改变热工或L1/L2显示投影 |
| J/heat/NuclearHeatExchangerBlockEntity.java、J/heat/HeatExchangerLine.java、J/content/HeatExchangeContent.java、J/content/BoilerContent.java、J/content/TurbineContent.java、J/turbine/TurbineControllerBlockEntity.java | 必要机器标准能力受限包装与工业端点复用；不改炉体/转子美术、换热/汽轮机性能和双端动力恢复算法 |
| 新J/mixin/FluidNetworkRestrictionMixin.java、新J/mixin/CreateTankCapabilityRestrictionMixin.java、必要新J/mixin/CreateTankInventoryAccessor.java；src/main/resources/create_nuclear_industry.mixins.json | 锁定Create真实drain前禁运和共享罐标准能力包装/私有同库访问；必要多个Accessor或分离创造罐Mixin必须先报告PM路径。不得修改现有SharedFillMixin或BoilerPipePressureMixin的原行为以掩盖冲突 |
| 新T/industrial/**；现有T/boiler/BoilerStateTest.java、BoilerDualSteamInventoryTest.java；原T/P1CoolantConfigurationContractTest.java、P1DataContractTest.java、P1ItemFluidRegistrationContractTest.java、HeatExchangerPonderContractTest.java、TurbinePonderContractTest.java、BoilerPonderContractTest.java、P1Ponder01ContractTest.java、P1Coolant05ContractTest.java | 新行为/预算/数据测试，准确更新被新数值或禁运取代的旧断言；保留守恒/零写/错误路径负例，不跳过旧测试或靠白名单让热/SC原生路径绿 |
| 新J/gametest/IndustrialFluidNetworkGameTests.java；必要原J/gametest/P1CoolantCreateNetworkGameTests.java、P1Coolant05CoverageGameTests.java、BoilerMultiportStopGameTests.java、BoilerSteamSelectionGameTests.java、BoilerTurbineFlowGameTests.java、ExtensionHeatExchangerLoopGameTests.java、ExtensionBoilerGameTests.java | 独立真实测试域及共享管路工况；只调整与新限流/专用运输直接冲突的夹具，不改尺寸、事故或其他既有业务合同 |
| src/main/resources/data/create_nuclear_industry_industrial/structure/industrial_empty.nbt | 独立GameTest空模板，复用既有合法模板字节，不创建大型测试世界 |
| src/main/resources/assets/create_nuclear_industry/blockstates、models/block、models/item、textures/block下仅上述五ID及其专用子模型/材质；lang/zh_cn.json、en_us.json | 完整六面、连接/朝向/碰撞一致、区分二/三级及泵/阀、本地化控件/诊断；初期可复用已许可素材，无需做正式美术重置或动画。新增像素用SVG源生成，禁止AI直接替换PNG |
| src/main/resources/data/create_nuclear_industry/recipe下仅上述五ID.json、loot_table/blocks下对应五ID.json；data/minecraft/tags/block/mineable/pickaxe.json | 精确有序成本、五种可掉落物与工具标签；泵掉落携缓存唯一一次，不能同时掉空泵和含液泵 |
| tools/ponder/heat_exchanger_scenes.py、boiler_scenes.py、turbine_scenes.py、fuel_sintering_scenes.py、现有反应堆模板生成器；J/ponder/HeatExchangerPonderScenes.java、BoilerPonderScenes.java、TurbinePonderScenes.java、FuelSinteringPonderScenes.java、P1PonderScenes.java；上述对应现有scene模板 | 仅替换受禁运影响的热液/SC管泵及必要流向/说明，不新增配方教学或重做已验取景；无法在原构图最小替换时报告PM，不自行拓展场景 |
| tools/art-assets/industrial-fluid-flow-01/** | 若新增像素，仅本批SVG与导出工具；不覆盖美术负责人现有工具 |
| docs/reviews/2026-10-12/tiered-fluid-flow-01/IMPLEMENTATION.md；.superpowers/sdd/tiered-fluid-flow-01/task-1-report.md；build/reports/tiered-fluid-flow-01/** | 执行报告、原始命令/日志、源码/制品清单。本卡以外docs禁止；不复制全量既有证据或擅自验收 |

PM持有spec/本计划、AGENTS、docs入口、server-config/recipes/项目合同同步与Git；用户/其他任务dirty project.md不能整文件覆盖或夹带。锅炉Settings新增两类流量影响现有构造调用：执行者先列清其他必须适配的生产/测试路径，PM精确扩卡，不能用忽略编译路径或虚假默认构造器掩盖分组额度。

## Review Focus

1. 流体已从源扣除后目标短收、满罐或口过滤改变：余量进入同泵缓存并守恒，源不重复扣。
2. 相同干线/同口被多个泵、主动推送和能力重复调用：共享同tick额度，不以每调用分别清账绕过。
3. 专用→Create罐→普通泵以及紧贴罐无管的原生路线：受限流体仍禁止，普通水/冷液/steam仍正常。
4. 热/汽网络断管、区块卸载与运行中切阀/反转：不强载、不凭失效句柄搬运、库存不复制，恢复无需拆泵。
5. 持液泵拆放/创造选取与配置缩小：唯一合法掉落恢复同份缓存，创造选取空泵，已有余量不丢。

## Task 1：完整分级流量交付

**Interfaces：** 对外普通能力保留IFluidHandler读取；受限fill/drain只有本网络验证后的内部事务可调用。同一工业端点适配返回原权威库存的模拟/执行入口并共用原口预算，不能公开裸无限handler或用ThreadLocal/调用栈假定授权。调度器唯一服务端tick入口，方块变化/加载/过滤变化只使对应拓扑失效；纯预算/路径算法与世界端点分离，便于JUnit验证。

- [ ] 读取实际栈、能力注册、Settings构造调用、真实Create管网/罐字节码和本卡写集；提交简短预计新增文件清单及需扩卡的准确路径，未批准路径不写。
- [ ] RED：写实质行为用例。256RPM泵16384、128RPM8192、停转0、超速封顶；二级路径8192、三级同路单泵16384；并行独立路线允许合计约20000但共用二级边≤8192，串泵不相加；模拟不改源/目标/缓存/额度；短收守恒。仅定向运行新测试，确认对应旧行为失败或缺失，不能只对文件字符串做TDD。
- [ ] 实现新配置/纯预算与拓扑、五种注册/世界部件、泵动力与周转持久化/拆放、阀控/本地化诊断和有序配方，按规范避免全世界每tick重扫。搜寻/节点上限、visited去重、加载边界和确定性公平分流实际生效。
- [ ] 接入原机器与同份Create普通/创造库存。标准能力受限包装和原生网络真实drain前禁运双层一致，SIMULATE也拒绝；不得只拦fill。验证普通流体原路不受影响、共享接收容量修正保持。创造罐无限源/销毁槽按原语义，不计入有限守恒总量。
- [ ] 现有反应堆口默认2048；锅炉Settings和真口按冷热2048、水汽4096区分，动态配置和物理口同时被主被动调用时共用额度。仅SC原生压力/直推关闭，普通汽保持；不改热参数或取消双汽过滤。
- [ ] 完成剩余生命周期/世界用例：停动力、关阀、反向、堵塞/拆管恢复、区块卸载/重载、缓存拆放恰好一次、创造选取空泵、配置缩小保留。实际getCapability/BlockCapabilityCache与Create管网必要证据，不以直接调handler替代全部联动。
- [ ] 按新增语义准确更新原有热/SC原生运输正例为专用正例，并保留原生禁运负例；当前教程必要管泵替换保持原取景/文案风格。用户现有世界不改写。
- [ ] 最终定向JUnit按实际类名运行，命令保留原始日志：`./gradlew.bat test --tests '*Industrial*' --tests '*BoilerStateTest' --tests '*BoilerDualSteamInventoryTest' --tests '*ReactorCoolantPortFlowBudgetTest' --tests '*ReactorCoolantLedgerTest'`；必要受影响资源/教程合同加入同轮，不机械补无关矩阵。
- [ ] 真实服务器独立域：`./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_industrial -PgameTestDirectory=build/gametest-tiered-flow-01`。非零注册，包含有限罐守恒、原生泵禁运（两种受限液）、专用↔Create普通/创造罐、真实端口2048/4096、至少10口及两条有效泵路线持续约20000mB/t、过滤/堵塞恢复；自然保存退出0才称整轮成功。不得用创造无限罐证明有限液守恒。
- [ ] 核心公共能力/原生管网闸门变化触发最终稳定后一次完整`./gradlew.bat build`，默认增量，不clean/--rerun-tasks；前期只定向。后续新修仅跑受影响组/必要打包，不重复同实现全量。候选JAR冻结报告计数、SHA与来源，首次未跑前不得预填成功。
- [ ] 自审写集/中文注释/数据和守恒不变量并交报告。PM安排一次独立规格/质量组合审，审查只读现有日志制品、不重复运行；必改项交回唯一执行者窄修与定向证据。
- [ ] PM核范围/制品，维护当前合同、提交候选；停在本批管泵阀操作、热/SC禁运、储罐联动和端口体验的集中人工门，不自动合main或推进管理员/压力系统，不重开无关教学/美术全套。

## 验证复用与开工安排

main已验收生产源及原功能证据作为基线复用，不再跑开工全量。锅炉16³的29/29、3/3和候选包仅证明独立尺寸批，不能拿来替代本卡端口分组或管网证据。正常当前保存加载属于本批必要生命周期；不做旧版本存档迁移和跨版本矩阵。

计划自审已对照spec：限制运输/储存、五种内容/成本、泵唯一预算/缓存、每口分组限流、配置、生命周期、受影响教程、最终公共机制回归与人工门均由Task1覆盖。允许写集之外的实际共享调用先申请精确扩卡，不派发笼统全src改造。

## 精确共享消费扩卡（2026-10-12，PM已批准）

- J/gametest/ExtensionConfigGameTests.java：原PORT_FLOW_MB_PER_TICK的保存/设置/恢复改为冷热、水汽两个真实键，并调整对应额度断言；原其他配置、生命周期及测试保持。不得保留一个旧别名掩盖两组真实配置。
- J/gametest/P1Loop02GameTests.java：forPort对外类型改为IFluidHandler的三个局部声明及必要import，原热口正例用本批合法专用网络/受控夹具，不绕受限包装；保持液量/热量守恒与输入空槽等原不变量。仅该API与禁运直接影响范围，不修改事故/热工设定。
- BoilerSteamSelectionGameTests原全限定Settings构造已在初始写集，随两组额度必要适配；Create创造罐继承同一handlerForCapability，经实际字节码核实无需新的分离Mixin。本扩卡不增加生产范围或运行压力。
