# EXT-B-BOILER-REWORK-01E：多汽口出汽与断汽残转整改

> 执行方式：使用subagent-driven-development；执行者不得Git写入或自行派发。PM负责文档、审核与版本管理，不能代写功能或测试代码。

**状态：** 汽轮机当前实体恢复后断汽残转的已捕获路径已局部修复，功能快照`5ced309a46b85b3c0de7e98069bb1bf90f67e53f`；17项定向GameTest、一次增量assemble及一次独立合并审查通过，等待客户端复测。锅炉现场停流原因尚未定位，本批没有修改锅炉生产代码。用户确认控制器为`(28,-59,-11)`，创造储罐已拆除；原故障拓扑须恢复后再取证。锅炉集中人工门未通过，不合入main、不接其他主线或教学。

**目标：** 修复真实Create管路多汽口出汽失效，以及无外部动力源时汽轮机断汽持续0SU/256RPM；不新增玩法。

**结构与基准：** 同级候选`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，HEAD `d0e9c64f06925bd25448549f058a72bd76fde374`，功能`99a526c97697e9de134cbd090674927c2147c2bd`。MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。保留logs和三个__pycache__未提交改动，main仅同步文档和候选快照。

**规格：** 用户本次复现及[01D冻结规则](./2026-10-07-boiler-steam-port-selection.md)、[01C流动排错](./2026-10-07-boiler-turbine-continuous-flow-fix.md)，以及AGENTS/治理5.1和5.2。

## 不变量

- 6×6×5、四汽口（三超临界、一蒸汽）、持续供热、出汽压力下限60→10：用户明确四口汇入同一管路。记录实际温压、当前汽种、四口选择、汇流支路压力/流量和流体身份，比较直罐与原生管/泵。
- 出汽压力下限是最低可放汽门槛，不是目标炉压。纯过滤仍只接受炉内当前汽种，禁止降级、免费升级、重标库存或改写外部异种流体；不把正常背压和过滤等待误称为故障。
- 汽口能力声明、实际drain返回类型、同tick逐口额度及质量/HU守恒保持一致；能力代次与原生网络源缓存更新必须安全，不在正在执行的network交易内替换其对象。
- 无其他源的中/大型汽轮机真实运行后切断供汽，40tick平均窗口与实际周转量耗尽后两轴及外接原生轴均停止，不仅修改浮窗。用户补充一旦首次发生，此后稳定复现，直到拆控制器重新成型；首次是否加载触发仍未确定。重新成型与当前版本正常加载均为必要生命周期边界；不研究旧存档兼容。
- 双轴发布同一份总SU、256RPM、效率曲线和周转缓存不变；有合法外部动力源时不得误清其网络。禁止改写用户世界、启动/终止用户客户端、改用户配置、清外部储罐或升级依赖。
- 全部新增/修改手写代码注释与必要Javadoc用中文；只注明有信息的单位、事务、tick顺序和服务端边界。

## 诊断与允许写集

一个实施者持有全部Gradle/服务端测试。高速只读分析者并行核对锁定Create原生动力源和加载/拆源路径，不执行Gradle，不修改代码。独立合并审查仅一轮，整改后只复审差异。

实施者写集（生产代码先报告根因与失败证据，再经PM按本卡确认局部方案；不得凭猜测修改）：

- `src/main/java/com/iksxh/create_nuclear_industry/gametest/BoilerMultiportStopGameTests.java`及`src/main/resources/data/create_nuclear_industry_boiler_multiport_stop/structure/multiport_empty.nbt`：本批独立筛选域，复用现有空模板；可复用既有夹具，不放宽原断言。
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/turbine/TurbineKineticProbeGameTests.java`：仅本次真实动力停转/当前加载边界所需用例；不以强制写坏状态替代自然复现。
- `src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerControllerBlockEntity.java`、`BoilerSteamPressure.java`；`src/main/java/com/iksxh/create_nuclear_industry/mixin/BoilerPipePressureMixin.java`；已存在`BoilerPressureConnection.java`的实际路径：仅来源缓存、连接资格和真实流动必要修复。
- `src/main/java/com/iksxh/create_nuclear_industry/turbine/TurbineShaftPowerSource.java`、`TurbineOutputShaftBlockEntity.java`、`TurbineControllerBlockEntity.java`：仅本批真实原生动力发布/撤销/当前加载生命周期修复。
- 报告`docs/reviews/2026-10-07/boiler-rework-01/multiport-stop-diagnosis.md`；只读分析者仅`multiport-stop-source-audit.md`，独立审查者仅`multiport-stop-review.md`。日志与制品仅`build/reports/extension/EXT-B-BOILER-REWORK-01E/`；测试世界仅`run/verification/boiler-multiport-stop-01e`。

账本公式、配置数值、原生汽种选择UI及无关素材不在写集；需增路径必须向PM报告原因后再修改。测试构造可用当前格式NBT还原正常状态，但不得注入异常native source掩盖自然缺陷。当前保存/加载只测真实本版本运行链，不做历史格式矩阵。

## 执行步骤与必要验证

- [ ] 读取AGENTS、本卡、相关活动合同、Minecraft开发/测试和systematic-debugging技能。锁定本地Create source，核对FluidNetwork/FlowSource/PipeConnection与GeneratingKineticBlockEntity/RotationPropagator。
- [ ] 建立失败复现，先输出关键边界：实际汽种、炉压/热量、四口资格、首段pressure/flow/cached source；两轴及外接轴speed/generatedSpeed/source/network/capacity、当前加载与重新成型差异。未复现时明确范围，不把猜测写成根因。
- [ ] 向PM报告一条可验证的根因假说与最小修复范围；确认后修改生产代码，保留首次失败日志。
- [ ] 新域定向GameTest：自然连续运行6×6×5多口60→10，经真实管路/机械泵匹配流体可排、异种背压保留；中/大型重复运行断汽及当前加载/重成型路径，外接轴停止、外源不误清。参数与实际夹具拓扑记录，不伪称覆盖未搭的现场。
- [ ] 仅本次影响共享机制时复验相关01D汽口/动力停转用例；不机械重跑全域、无关JUnit或完整build。最终一次增量assemble，原始失败/成功、退出码、服务端退出和制品一致性分别记录。
- [ ] 一次高速规格/质量合并审查；PM检查路径/中文注释/差异与制品。源码留同级候选，未接受功能不合入main。
- [ ] 集中客户端复测：原6×6×5四口管路调压与无额外源汽轮机重复断汽；停在人工门，不接其他教学或主线。

## 实际技能入口

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/systematic-debugging/SKILL.md`
- 同目录`writing-plans`、`subagent-driven-development`、`test-driven-development`、`verification-before-completion`。用户治理优先于通用Git、重复全量及旧存档要求。

## 诊断阶段记录

- 用户进一步明确现场实际16个有效热力回路；最新四个超临界口共同接一台中型汽轮机，并在同一管路分接创造储罐销毁多余汽。最终诊断须包含此双接收端，不以单台108mB/t小于满回路288HU/t为充分解释；保留原三超临界/一蒸汽切换对照。原生压力传播范围按锁定Create配置处理，超范围无泵不能当产品停流红测。

- 实施`/root/boiler_multiport_stop_01e`使用gpt-6.1-sol/high（多汽口原生流体缓存及动力源生命周期交叉缺陷）；只读原生动力分析`/root/turbine_native_stop_audit_01e`使用gpt-6-luna/high，不运行Gradle。最终独立规格/质量审查另由高速执行者完成，复用现有证据。
- `03-natural-current-red.log`：真实运行后保存本版本两轴NBT并重建实体，不注入Source/Network异常；中型断汽后gen0/SU0仍256RPM，前轴与外接前轴自然出现Source互指，165tick断言失败。它证明实体当前格式恢复边界，不声称已做整区块磁盘卸载或客户端退出重进。自然未恢复中型三轮断汽通过；大型未发电属于待修夹具，不记产品红测。
- PM已读该失败日志/源码并允许局部修改`TurbineShaftPowerSource.initialize()`：核销保存容量后在首次attach前通过原生拆源传播撤销本轴已保存生成来源，合法外源应可重连。不能按零SU清整网；须补外源对照及大型有效运行。此为本卡写集内既定停转合同修复，不新增玩法或旧存档迁移。
- 同侧短共同管路目前出汽通过，锅炉生产代码尚未获改动确认；继续真实汽轮机吞吐/延长汇流/原生泵边界诊断，不把纯过滤背压或未达目标压力10当失败。
- 当前恢复生产修复已完成定向7/7（`05-initialize-green.log`，exit0），但尚不足以证明已故障机组当前保存后的恢复。PM允许实施者只对其本批initialize十行差异做受控临时基线回换，捕获自然互指环的本版本NBT后立即恢复修复；不Git回退、不碰其他改动。若需要可新增已授权精确资源`data/create_nuclear_industry_boiler_multiport_stop/current_faulted_shaft.nbt`保存该本版本自然故障快照，注明其来源、位置重定位与边界，不能依赖仅本机scratch的最终回归。
- 合法传播距离内“四SC＋16回路＋中型＋共同创造罐”定向出汽通过后，PM补充只读现场取证：分析者可读取当前客户端`run/saves/新的世界`的本版本MCA/NBT和当前配置，核对机组坐标、共管路径长度、泵/阀/方向、实际连接压力与动力Source链；仅追加分析报告和`build/reports/extension/EXT-B-BOILER-REWORK-01E/site-readonly.json`。不得写入用户世界/配置、运行或终止客户端，也不读取另一个历史世界；磁盘快照不能冒充当前内存或客户端复现。

## 候选收尾与剩余人工门

### R1：50%附近炉压波动的保存现场

用户复测表示汽轮机残转没有复现；锅炉仍在约50%炉压上下波动，并已保存退出`新的世界`。PM检查原分辨率截图确认：炉压50.97%、出汽压力下限10%、汽温/水温2.00、汽16311/32000mB、16回路、当tick产汽63mB，当前产汽为超临界蒸汽；初次缩略图温度误读为3已纠正。这只是该时点数据，不等于完整流量记录，也不将“残转未复现”扩写为所有动力场景验收。

R1第一阶段继续01E已授权只读诊断，当时不改生产代码或运行构建。实施者核对汽种门槛、逐口选择、主动压力刷新和真实流量测试中263次跨越的含义；现场分析者只读保存退出后的相同世界、同一控制器及其已连接支路，记录当前NBT、实际选择、管线和创造储罐流体/连通状态。两者分别写`multiport-pressure-r1-analysis.md`、`multiport-pressure-r1-site.md`及本批制品目录`site-readonly-r1.json`，不覆写01E历史证据。红测和后续局部生产授权见本节后面的阶段记录。

必须区分：出汽压力下限不是目标炉压；50%汽种门槛附近的正常过滤等待，与原生管网应接收匹配汽却永久停流不同。保留用户“纯过滤、不降级、不转换外部库存”的明确规则。若根因属于新玩法取舍，PM先整理具体证据与选项供用户决策；若属于既定合同的代码缺陷，仍须报告最小红测/修复范围后实施，验证按治理5.1精简。不追加其他主线。

R1限定现场已确认三个SC口共管接中型与首段旁空创造罐，独立NORMAL口经13管节点接普通空罐；保存时普通支路无Flow/Pressure。实施者重读锁定Create发现来源短暂消失会清LayerII，重建LayerIII后存在2tick传播等待；实际资格窗口长度尚未知。PM授权在本批原`BoilerMultiportStopGameTests.java`增加最小R1独立筛选场景，复现3SC近旁创造罐＋独立13管NORMAL空罐、16回路、热输入约63HU/t与60→10。逐tick记录汽种窗口、两支真实成交及首段native flow/network等待，不注入异常缓存或Source，不改生产代码。用现有分域机制只运行本次一到两个R1用例，保留基线日志；因已知管路/对照不足产生的夹具错误必须区分。源码修复另待根因证据审核。

R1筛选的必要模板别名获PM批准：仅新增`src/main/resources/data/create_nuclear_industry_boiler_pressure_r1/structure/pressure_empty.nbt`，复制既有空模板相同字节，配合方法的`templateNamespace`运行两项R1；不更改Gradle筛选框架，不重跑旧17项。不得为避免短窗等待私改SC门槛、低压续出SC或汽种滞回，新增玩法取舍仍须用户确认。

R1红证据：`13-r1-baseline.log`中NORMAL资格295tick、103窗、最长5tick，13管普通罐实际接收0，直邻对照收到8000mB但因计数BE未重触成型而实际罐容量仅8000。限定修正夹具后`14-r1-capacity-source-baseline.log`主场景仍红，普通罐确为56000mB且SIMULATE/EXECUTE均0；直邻对照收到56000后满罐，不能称900tick持续低压已通过。已证明短窗中的真实来源SIMULATE可抽，5tick窗刚进入网络pause=1便再次失效。

PM批准本卡既有生产写集内的单点候选：仅`BoilerControllerBlockEntity.java`区分汽口脏原因。真实结构/端口选择变更继续原拓扑传播；温压资格变化撤销旧句柄并在安全的控制器tick重取首段来源，不再把未变管路当作changedPipe进行全段wipePressure/WAIT_FOR_PUMPS。原生flow因真实来源空而清理及原生网络2tick等待仍保留。`outputFluid`、温压、旧句柄失效、SIMULATE/EXECUTE资格、逐口额度及mB/HU不变，禁止虚报可抽库存或强保非法汽种流。

同一R1两项验证红→绿；若仍失败，不叠加滞回或玩法补丁，先报告剩余机制。若通过，本次共享刷新影响要求定向运行01C持续切种与01D独立选择/旧句柄域，复用未变热工单测和汽轮机17项，避免全量。最终仅一次增量assemble、一次合并独立审查及集中人工复测；主工程不提前合入未验收生产代码。

R1终态：`15-r1-source-refresh-candidate.log`仍为exit1，13管NORMAL红、直邻对照绿；去掉额外拓扑等待后，NORMAL累计合格304tick、196窗、最长3tick，普通空罐实际接收仍0。原生来源按真实EMPTY清流，新网络尚未完成自身两tick等待便再次失去资格。独立复核未从本轮证据识别出保留瞬时过滤规则即可关闭该红测的接口修复；这不是穷尽所有潜在问题的断言。

PM批准受控撤回该单点生产候选，执行者先保存`15-source-refresh-rejected.patch`（SHA-256 `56689D84ACC6AF22577B7357F07C4F19BAA05E347E5DCC36F8DD3F26EE13E859`），再仅恢复自身控制器差异。PM确认该文件相对HEAD差异为空；两项R1诊断测试、模板和全部失败日志保留，没有运行条件性9项回归或新assemble，没有交付新锅炉修复制品。用户世界只读保留。

R1诊断转入[01F双汽库存](./2026-10-07-boiler-dual-steam-inventory-01f.md)：用户明确确认两种库存分开记录、共用总容量、各口只取对应库存。周转缓存建议已撤下不实施；失败候选/原始记录保留，按01F新合同验证后集中手测，不接其他主线/教学。

### 上一轮01E候选与当时的人工清单

- 最终`11-final-scoped-green.log`为17/17、exit0，包含本批12项及既有真实动力探针5项；`12-assemble.log`为exit0。只做一次最终增量打包，没有重跑无关单测或全域。原失败轮及夹具错误保留，详见[实施报告](../../../reviews/2026-10-07/boiler-rework-01/multiport-stop-diagnosis.md)。
- 汽轮机修复只在核销当前实体保存的生成容量后、首次原生attach前撤销本轴保存的动力来源；不按零SU清整网，不改256RPM、效率、周转容量或合法外源。正常运行实体NBT恢复的中/大型基线均自然出现Source互指并断汽残转，局部修复后停止，合法外源对照保持。完整自然故障四实体快照在基线和修复版重建后均自行恢复，该对照不能证明任意既有故障的恢复；实体重建也不等同完整客户端退出重进。
- 锅炉短汇流、有效传播距离内的汽轮机和原生创造储罐共同支路、实际机械泵及直接储罐对照通过并保持汽量/HU守恒。超过原生16格传播范围的失败已归为夹具限制，没有扩大配置或改产品绕过。仍未复现用户原始停流，不认定锅炉已修复。
- 只读现场显示前三SC口汇入汽轮机管路，第四蒸汽口另接普通储罐；用户已确认是目标锅炉且创造储罐已拆除。因此当前存盘不能代替原故障现场。读取期间区块仍更新，不把非同时快照视为内存状态；不同时间制式混读的旧结论已撤回，见[原生与现场分析](../../../reviews/2026-10-07/boiler-rework-01/multiport-stop-source-audit.md)。
- [独立审查](../../../reviews/2026-10-07/boiler-rework-01/multiport-stop-review.md)允许局部汽轮机候选进入人工测试，保留锅炉未定位项。PM独立核对JAR中生产class、测试class和两NBT与编译输出/资源一致。
- 最新JAR为2,247,712字节，SHA-256 `080A09302E7D5E596F30CB6D75E0E2D42093227A409597AADDA9B35263CF03FB`，两目录的`build/reports/extension/EXT-B-BOILER-REWORK-01E/client-candidate/`保存相同制品与PM核对记录。源码仅在同级候选，main仅同步文档和制品。
- 剩余集中手测：重启同级候选客户端，重复正常运行后切断供汽，确认实际轴和转速表停止；恢复锅炉原共管创造储罐支路，调压60→10，若停流则保留布置并保存退出，采集护目镜温压/汽种、库存和管路现场继续定位。不要求重复已通过的无关基础项目。
