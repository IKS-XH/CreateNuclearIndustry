# REACTOR-VARIABLE-SIZE-01 实现交付

2026-10-12。执行者交付未提交候选；未执行任何 Git 写操作，未合 main，人工门保持待验。

工作目录：`E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`；实际分支 `codex/reactor-variable-size`，开工/冻结 HEAD `bf25343be89ac420e82c691269390a8ac44fb028`。按派发顺序完成尺寸模型、快照/热工、世界生命周期、显示与字幕、验证。没有写集外生产调用者，没有新增玩法取舍。工作区原有 logs、.superpowers、工具缓存及 PM 的计划/检查单改动保持原状，不作为本批交付。

实际读取并应用根 AGENTS、活动计划、project.md 4.1、reactor-local-control-revision-design，以及 minecraft-modding、minecraft-testing、superpowers test-driven-development/writing-good-tests、systematic-debugging、receiving-code-review、verification-before-completion。先从锁定本地源码核对 API；栈保持 Minecraft1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82。

## 实现

- 新增 `ReactorDimensions`：三边5～16，X/Y/Z对应宽/高/深；实际内部截面及高度进入结构扫描结果、权威 `ReactorSnapshot` 和版本6当前 NBT。所有 with*、SCRAM、燃耗/传播/融毁转换保留尺寸；默认5³构造/模板只保留原示例。列坐标可到13，由实际宽/深再次校验；生产四向邻接消费入口全部传入实际尺寸。
- `ReactorStructureDefinition` 验证实际棱边、非棱边侧面、完整棒列及列帽。仪表无朝向、允许偏心和直接邻接燃料；唯一仪表、至少一燃料列与冷热口规则保持。独立6×8×7、16³夹具不修改原5³布局；旧测试只更新扩大后的坐标/容量边界语义和显式已知尺寸的无效快照入口。
- 新增 SERVER `reactor.minDimension`/`maxDimension`，默认5/16，联合非法组合拒绝扫描成型。每格容量安全上限按2730格计算；long乘积预检再验证int可表示。冷热共容积、每口限流及配置缩小保留库存的正式处理不变。fuelBurnTimeHours保持3小时，配置注释明确它是内部高度1、满功率且无超频的寿命锚点。
- 扫描只对四个水平法向有限寻边，找最近棱边/对面，再验证确定包围盒。同次缓存每个世界坐标（包括不可读值），不枚举上千尺寸与原点。Level适配先查hasChunkAt，不强制加载。相邻但不重叠设备保持各自闭合框架；双仪表、缺角、不完整列拒绝。
- 服务端已加载仪表索引覆盖onLoad、卸载/移除、世界卸载和端口反向加载；远端变化通过索引有限距离通知，不枚举31³邻域。普通tick/cap/goggles不重读结构方块；每次可用性校验只涉及缓存包围盒相交的至多4个区块，不读结构方块，不承诺每tick全部入口的总调用数。理由：宽/深各≤16，因此各轴最多跨2个chunk。FULL可用性可能先于物理Unload事件消失，此保护立即暂停、撤销绑定并保留权威尺寸/库存；缓存等待框架在可读边沿只安排一次恢复扫描。
- 换料口和远端控制驱动器从实际列图绑定/维护。维修调用者传真实高度。流体capability捕获端口绑定代数，解绑/恢复后旧handler不能复活，即使端口本体一直留在已加载仪表区块。
- 仪表摘要、L1表面边界和L2列主体使用实际几何。审计现有客户端按bodyPositions及descriptor包围盒消费棒长、液体、采光和六面控件；保留既有加液棒体缓冲修复，未更换素材。协议64边/256列/4096主体格预算保持为工程限制，玩法仍5～16。
- 反应堆Ponder保留5³例子，仅两条搭建字幕/fallback改为可变范围和填满内部高度；双语另补DIMENSIONS/CHUNKS_UNAVAILABLE诊断。其他教程/配方未改。

## 公式锚点与自动断言

每列仍一个真实燃料ItemStack，位于顶部换料端口。设内部高度h=外高−2；默认无超频、满功率单列产热h HU/t，燃耗h/216000组件/t，单钢板维修0.25/h。

| 内部高度 | 单列产热 | 燃耗（组件/t） | 钢板恢复完整度 | 默认无超频满功率组件寿命 |
| --- | --- | --- | --- | --- |
| 3（5³） | 3 HU/t | 3/216000 | 0.25/3≈0.0833333333 | 1小时 |
| 14（外高16） | 14 HU/t | 14/216000 | 0.25/14≈0.0178571429 | 3/14小时≈12分51.43秒 |

6×8×7一燃料列：20列、主体高6，可计19×6=114格，容量114000mB，默认满功率6HU/t。16³一燃料列：196列、主体高14，可计195×14=2730格，容量2730000mB；控制列与空列同样计容。

新增热工断言覆盖高度3/14、两种维修各消费一件且输入不变、断开燃料簇raw2.8只整堆ceil成3HU（不逐列取整）、far列12/13处控制邻接/失效热传播/融毁、完整tick和with*/当前NBT保持尺寸。6×16×7全插棒冷却0.75HU缓存只转换1mB：128/11→127/12，冷热总量139不变，0.25HU缓存与量化余数均保留，完整度不误伤。

新增最大描述符断言：L1/L2当前NBT往返、196列各14主体格、2730合法液体格，以及客户端全插棒travel14、bottom=originY+1、top=originY+15。自动数值证据不关闭视觉/鼠标人工门。

扫描理论封顶：4×4096完整验证＋四面寻边480＋穿径60＋仪表1=16925，硬预算17000次唯一读。16³跨区块纯reader实测4131次；缺区块reader77次，缺失数据未读入方块ID。见最终Scanner XML system-out；这项模拟缺区块用例与真实世界证据分开。

## 原始验证证据

证据根目录为仓库 `build/reports/reactor-variable-size/`。文件名中的green不是结论，以下按真实输出记录。

| 日志 | 实际结果/边界 |
| --- | --- |
| 01-model-red.log → 02-model-green.log | 独立长方体/16³先因固定尺寸RED；模型3/3 GREEN，exit0/26s |
| 03-thermal-red.log / 03b-thermal-red.log | 首次测试访问器编译修正；随后真实RED暴露高度仍3及NBT没有尺寸 |
| 04-thermal-green.log | 实际26项中3失败，旧固定坐标边界断言需与扩大后的模型一致；未算通过 |
| 05-world-red.log | 真实6×8×7成型RED，exit1/28s |
| 06-reactor-green.log / 06b-reactor-green.log | 首次不存在ID编译失败；随后258项中2失败（显式体积外坐标语义、缺诊断翻译）；未算通过 |
| 07-scanner-check.log | 相关扫描/热工/结构/诊断真正GREEN，exit0/25s |
| 08-binding-repair-red.log | 矩形远端撤销通过；16³成型/远端列绑定后在维修固定高度RED。保存挂起后仅结束自有进程；08-process-stop.json保留 |
| 09-state-green.log | reactor/structure/slider定向GREEN，exit0/24s |
| 10-world-green.log | 实际2个真实用例失败，exit1/28s；同步解除模板票据干扰矩形探针，远端FULL消失但尚无物理Unload |
| 10b-world-diagnostic.log | 隔离时序后矩形通过；owner同实例、removed=false、indexed=1，远端FULL不可读但Unload=0，max用例RED，exit1/26s |
| 10c-world-green.log | 可用性缺口已冻结；保持仪表FULL33仍保留邻区生成级对象，物理Unload断言RED，exit1/27s |
| 10d-world-green.log | 2/2 required业务断言通过，随后Saving worlds挂起；仅结束本轮PID34268。Gradle exit1、子进程−1、总1m33s，不能声明GameTest进程exit0 |
| 11-final-directed.log | 最终定向13/13（Thermal6、Scanner4、Size3），0失败/错误，exit0/20s |
| 12-build.log | 唯一完整增量build，exit0/24s；最终94套514/514，0失败/错误/跳过（包含补充最大客户端几何项） |
| 13-waiting-break-red.log | 审查P1真实RED：进入WAITING后近端仪表BreakEvent没有被拒绝；exit1/29s |
| 14-waiting-break-green.log | 实际RED：原已加载owner等待保护通过，物理卸载后仅近端恢复的新owner尚未进入索引；exit1/28s |
| 15-waiting-break-green.log | 实际RED：新增远端drive读产生UNKNOWN短期票据，远端FULL不可用预条件未成立；exit1/26s，未跳过断言 |
| 16-waiting-break-green.log | 1/1 required通过，近端当前NBT恢复、精确组件保护和reset回滚通过；正常保存、exit0/26s |
| 17-full-restore-red.log | 新确认的同项P1真实RED：全FULL当前NBT新实例首次Post前的保护返回false；exit1/24s |
| 18-waiting-final-green.log | 实际RED：直接保护已修复，但手工fresh不是Level当前BE，被严格实例索引过滤，BreakEvent仍走原实例；exit1/26s |
| 19-waiting-final-green.log | 用Level当前BE及其onLoad修正夹具身份后，最终同项1/1 required通过，正常保存、exit0/26s |
| 20-directed-final.log | 受影响9套51/51，0失败/错误/跳过，exit0/21s；生命周期、维护、两种维修、NBT和变量模型/扫描/热工 |
| 21-assemble-final.log | 最终稳定后唯一必要增量assemble，exit0/1s；包含审查窄修及13文件EOF空行整理 |

完整build复用缓存，未clean/强制rerun，没有重跑已验收其他设备GameTest或旧版本存档迁移。完整JUnit触发依据是本批公共快照与生命周期影响较广，活动计划明确允许一轮。编译有GameTest已过时公开API提示、原Gradle弃用提示；未升级技术栈。

真实世界GameTest位于独立域 `create_nuclear_industry_variable_reactor`、目录 `build/gametest-reactor-variable-size`、40×20×40空模板，不读取或改写 `run/saves` 用户世界。

1. 6×8×7实际控制器成型20列、顶部换料口绑定；普通10tick扫描计数不变，远端外壳直接变化后整台缓存与口归属撤销。
2. 16³远端控制列13,13、换料口绑定，燃料及控制棒维修均恢复0.25/14；正式tick发布196列L2且保留高度16，当前保存/读取保持权威状态。
3. 第一阶段只保留仪表FULL33 ticket：远端失去FULL可用性但owner仍加载。日志同实例、removed=false、indexed=1、Unload=0；缺区块立即暂停、库存/尺寸保留，近端口旧handler失效。恢复后同一近端口实例的新handler有效，旧代数handler持续为0。
4. 第二阶段解除所有本轮票据：真实远端ChunkEvent.Unload计数1（tick380）；最终旧owner removed=true、index=0。恢复实际包围盒后new owner != old owner、index=1、dimensions16³，快照完整度/控制状态及111/222mB库存恢复，旧handler仍拒绝。

模板强制票据、ChunkLevel FULL33、ChunkMap延后的saveSyncFuture/unloadQueue依据均已从锁定本地源读取，保存在StructureUtils.java、DistanceManager.java、ChunkLevel.java、ChunkMap.java、ServerChunkCache.java等证据文件。快速GameTest缺少普通帧空闲预算时只使用公开 `chunkSource.tick(()->true,false)`消费真实卸载队列；没有人工发送区块事件。诊断打印及最终探针源码另存 `final-probe-source.java.txt`，物理Unload与FULL缺失分别声明。

## 独立审查窄修

P1根因：缺FULL进入等待后，原有效扫描被不可用诊断替换、origin清空；原拆除事务因此没有计划，近端热堆组件被放行。当前版本正常保存后仅近端重新加载还会遇到同样缺口；全部区块已经FULL但新实例首次Post之前也不能据可用性证明安全。

最后有效扫描的L1表面和燃料列占据证据现在保存在仪表当前服务端NBT `BreakSafetySurface`/`BreakSafetyFuelColumns`。证据必须匹配具体owner坐标、维度和权威snapshot外尺寸，表面还匹配原格的预期方块ID；只防护原已验证表面/列帽/燃料主体，空腔及其他设施不入选。它不能宣告成型，不接管库存、燃料或热工权威，也不发给客户端。恢复期间证据保持到实际有效扫描，成功完全停机清空时才撤销；全部拆除计划提交、后检和事件发布成功前不清证据，失败回滚保留原防护。原全部可读后的危险事件和停机破坏性重组仍使用既有事务。

ChunkEvent.Load使用事件自带LevelChunk的实体/NBT坐标补登记本区块仪表，覆盖onLoad尚未到达的正常恢复边沿；不通过Level查远端实体。索引仍校验 `level.getBlockEntity(ownerPos)==owner`，18的夹具身份失败保留，生产身份校验没有放宽。

最终19的独立域为 `create_nuclear_industry_variable_reactor_waiting`，目录 `build/gametest-reactor-variable-size-waiting`，仅重跑这一受影响用例。它验证：当前NBT装入Level当前新实例并走onLoad、全FULL但首次Post前实际BreakEvent保守拒绝；有效扫描撤销保护；原owner失去远端FULL但Post尚未运行及WAITING两态均拒绝原仪表、冷液接口、换料帽及燃料主体，状态/真实ItemStack完全不变；原空腔新放的普通外壳不误拒绝；之后真实owner物理卸载（isRemoved），只加载近端的新实例indexed=1、guard=true，NBT热状态/123+456mB保留并拒绝仪表和燃料主体拆除；全部区块恢复后危险评估保持；安全reset提交再回滚仍保存BreakSafetySurface。全FULL首次Post前阶段是当前实例替换/NBT读取探针，不冒充一次物理卸载；物理Unload事件计数仍复用10d原始1次证据。

P2根因：锁定Level.getBlockEntity经getChunkAt进入LevelReader.getChunk(FULL,true)，旧远端drive清遥测与Create.getPipe入口都可能隐式加载。现在clearControlRodTelemetry只访问hasChunkAt的drive；notifyFluidNetworkAround先检查endpoint，再检查每个邻接坐标。旧已加载端口缓存仍完成解绑，库存不清。锁定源码见 `Level.java:771`、`LevelReader.java:129`，索引见 `22-loaded-api.txt`；Create6.0.10实际javap见 `FluidPropagator.javap.txt`，其propagateChangedPipe内部队列在读取邻居前已有Level.isLoaded，故补的是入口边界，其他正常成型路径未机械改写。

19真实探针对原已绑定远端drive、未加载endpoint、已加载边界endpoint旁的未加载邻区调用实际清理/通知入口，随后均断言远端FULL仍不可用；没有强制加载来通过防护。夹具只用公开chunkSource.tick清理寿命1的UNKNOWN票据及真实卸载队列，保持pre-Post前提。它不扩大为完整远端Create管网验收。原13/14/15/17/18的RED日志保留；原2业务和完整build没有重复运行。

## 冻结与待人工

`12-stable-identity.json`及`12-stable-probe-source.java.txt`保留完整build当时的30源冻结；`12-xml-summary.json`及`12-test-results/`保留94套514项原始XML与SHA。审查整改后只复验受影响范围，`20-xml-summary.json`及`20-test-results/`保留9套51项最终XML。13文件仅整理多余EOF空行，无关logs空白保持；最终增量制品与功能稳定完整build的证据分开。本报告不改写历史失败日志。

最终`final-identity.json`记录实际31个src路径SHA256、50个相关生产class与JAR逐字节一致验证、两语言及两个隔离模板打包一致。最终生产/测试源码对应19真实探针、20定向及21增量assemble。PM精确暂存检查后，仅移除ReactorVariableThermalTest.java末尾额外空行，测试语义与生产源码不变；未再运行测试或构建，最终JAR保持原件。final-identity.json仅刷新该测试源SHA，仍为31源/50class；当前清单SHA256：`67e417661cba142195779555747b756ff0c1c80ec0daa849457994fd01f20b3b`。

最终候选JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，2659488字节，SHA256：
`794a9ca78ec091116c5500be70999ac0082d1f219357cbe62ab9be66f18c4cef`。

完整build当时的历史JAR为2652934字节、SHA256 `cacbdf047f446b4d50a70dbbdbdfaa25f3730280a8a5346027e36461e3d02fce`；当前交付以上述最终增量制品为准。

主世界携带证据复用PM准备的 `world-copy.json`，本批测试仅用build隔离域。自动实现与证据交PM独立规格/质量审查；仍需候选客户端人工检查5³原例、偶数长方体、16³跨区块的成型、供热、棒体/液位、顶面控件、远端拆件及保存重进。具体操作见PM的 [CLIENT-CHECKLIST.md](CLIENT-CHECKLIST.md)。未声明视觉通过、未关闭人工门、未更新主目录正式JAR。
