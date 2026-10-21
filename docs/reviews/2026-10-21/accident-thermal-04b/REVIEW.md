# ACCIDENT-THERMAL-04B 唯一组合窄审

2026-10-21。**存在 3 项未闭 P2，需整改；未发现 P1。** 本次合并规格、代码与已有证据审查，不构成人工验收或制品放行。未运行测试、构建、服务端或用户世界，无 Git 写操作；唯一写入为本报告。

## 未闭问题

### P2-1：原生箱子移除回调仍可间接强载第二邻格

位置：[MoltenCoriumEffects.java:137](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/MoltenCoriumEffects.java:137)、[MoltenCoriumEffects.java:151](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/MoltenCoriumEffects.java:151)。普通箱子仅将目标格纳入 loaded 检查/写集；flags18 不阻止 onRemove 内部读取。

已直接核本地锁定 `build/moddev/artifacts/neoforge-21.1.219-sources.jar`：ChestBlock.onRemove:228 → Containers.dropContentsOnDestroy:51–55 → Level.updateNeighbourForOutputSignal:1063–1074。最后一段只对第一邻格执行 hasChunkAt；若第一格为红石导体，继续 getBlockState 第二格而未核其加载状态。具体触发：箱子在区块内 x=14，x=15 已加载且为石块，x=16 所在区块未 FULL。溶蚀该箱子会从原生通知回调读取 x=16；AUTO 写前守卫只拦写，无法阻止这个读。玩家在区块边缘放置普通容器即可触发额外区块加载，违反本卡不强载合同；现有箱子测试未覆盖此边界。

最小整改：在任何移料/掉落前，预核该原生容器通知的读取域。对目标六方向，第一格若参与原生通知须满足实际加载双核；其为导体时第二格也须先核实际加载。可以保守核两格范围，合法世界边界外按原生空格规则处理；不足时等待。保留原生库存掉落与回调，不扩成第三方容器研究。

### P2-2：HOT位置重新出现熔融格时漏建本体

位置：[MoltenCoriumEffects.java:62](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/MoltenCoriumEffects.java:62)，关联 [ReactorAccidentSavedData.java:30](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/ReactorAccidentSavedData.java:30)。appeared 只在 moltenAt(pos)==null 时新建；位置索引同时保留 HOT 记录。

触发：一团已凝固、仍处2400tick HOT阶段时，管理员在原位置执行合法 setblock molten_corium。旧 Corium.onRemove/take 终止热引用，却不立即移除本体 HOT 位置索引；新液格 onPlace 看见 HOT 记录便不建新 UUID，也不将其置为 MOLTEN。下一tick旧 HOT转DONE并撤索引，世界液格随后长期没有本体、热/移动/溶蚀及写前保护。提前采掘HOT后同tick放入新液格亦有此窗口，违反“所有合法世界出现入口建立本体”。

最小整改：出现入口仅复用当前 MOLTEN 所有者；遇到非MOLTEN位置引用，应终止/撤销旧位置和活跃引用后创建新的空载荷本体，或在索引设计上使HOT不占熔融位置所有权。不得继承旧物料、热计时或重发废料。

### P2-3：移动会覆盖另一未投影本体预留的位置

位置：[MoltenCoriumEffects.java:117](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/MoltenCoriumEffects.java:117)，关联 [ReactorAccidentExecutor.java:86](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/ReactorAccidentExecutor.java:86)、[ReactorAccidentSavedData.java:31](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/ReactorAccidentSavedData.java:31)。Column先交出 placed=false 本体、清空并结算；其已登记位置可以暂时仍为空气。移动只判断目标空气，没有检查 moltenAt(next)。

触发：团A已达到移动间隔，另一事故刚将其下方列底交为未投影团B；A随后移动到这个空气格。reindexMolten 的 put 覆盖B的位置索引，但B仍持唯一真实payload。A停留时B不能投影/扣冷却；A离开后，B的出现回调还可能因位置索引缺失而建立空载荷影子记录。真实B因此长期不可回收，位置与载荷所有者不再一致。横向空气目标也有同一入口。

最小整改：集中 move 写前同时校验目标未被其它本体登记，包括 placed=false 预留；冲突先等待，不改位置/修订号/进度/索引。位置索引应阻止静默覆盖其它活跃所有者，未投影放置也应确认自己仍是该位置所有者。

以上均为静态调用链/状态交错发现，未在本审查运行复现；不以现有GREEN宣称这三个边界已覆盖。已一次汇总交PM，不直接指挥执行者。

## 其余范围的核对

- 以主工程 `E:/MyMC/NewMod/Create_NuclearIndustry/docs/superpowers/plans/2026-10-21-accident-thermal-04b.md`、批准修订设计为准，已读 AGENTS、治理5.1/5.2/6、STATUS、04A证据及本批TECHNICAL。实际使用 minecraft-modding/testing/resource-pack 技能；实际Gradle锁定 MC1.21.1、Java21、NF21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。旧04距离/累计cap规则不作为合同。
- 除上述位置冲突外，Column→独立本体→Corium BE为真实单份载荷交接；液格不保存第二份物品，本体交接后清payload，旧Column/终态不重发。独立UUID/位置/区块/活跃索引无来源ledger执行前提，未引入普通tick历史账本扫描。
- 6000加载tick、水×4、20/40进度随同一记录移动，默认4HP/5秒/同类20tick及火抗接线一致；int上界进度饱和避免溢出。到期先停热伤/溶蚀，转固等待预算，空载荷转空残骸无固体热害。原complete、04A火实现和手动最终提交Mixin未改。
- 共享额度保持同tick已收紧cap，读回保留已用额度；两格搬移先核两份，装配台先核全加载/连带写集，LevelChunk逐次计自动写，原事故收据另保守占额。不能将普通物品实体掉落计成块写，报告已分开说明。
- 环境作用域只跳过本模组手动停机预检，直接BreakEvent post返回后判断最终取消；取消/异常时不移料，成功后重验状态/BE身份。核燃料真实移出并撤旧快照/能力，正常手拆分支保持。容器不强载仍受P2-1阻断。
- 辐射从独立chunk索引读取真实payload，复用原栈预算和剂量；位置/修订号/phase/载荷校验及changed/custodyChanged撤旧采样，液面无第二份源。普通桶/水/放块/无cap、软管发现和既有队列两条液面分支有对应防护；管理员清除限定原版有权限setblock/fill的最终目标作用域。P2-2是该合法入口的剩余缺口。
- 新六键0.5/6000/20/40/4HP/5秒、有限数及整组回退与保存冻结一致，无额外体积/距离/累计cap。原生液面不执行spread/randomTick/水岩浆反应；客户端lava_still/lava_flow接线、无桶/空loot、双语及PM三份同步docs无明显入口失配。中文注释覆盖主要跨包事务/时序职责，未为风格额外列阻断。

## 来源和已有证据

- 审查树 `E:/MyMC/NewMod/Create_NuclearIndustry-accident`，branch `codex/accident-thermal-04b`，HEAD/基准 `9f53a7d5218d1a51b6fdc43bbeb98735a72d9acd`，本报告针对其未提交增量。32个源码/资源/测试文件、51个编译class、6个处理资源逐项SHA匹配SOURCE；6资源也匹配实际源。`git diff --check -- src`通过。三份docs为PM写集；根测试日志待PM恢复，未擅自改回。
- EXECUTION SHA256：`6A051F5C1A1C6535CFB34370D378900E4787187B196993C79E283FEE5B9EA54F`；SOURCE SHA256：`95C267BF951AF4B4DF39802ED9DF4ED8C07AC7A5C88C6B0063F338DAB03DC18A`。
- 最终 `12-unit-boundary.log`/exit与unit-results XML相符：Ledger4 + Molten6，**10/10，0失败/错误/SKIP，exit0**。10/11服务端之后仅补int上界饱和分支及定向断言，没有把12轮编译身份冒称为重新跑过服务端。
- `10-server-final.log:653` 的14 required passed实际为 **4项业务 + 10显式SKIP**，分别为04B、04A热火、shutdown和原生待续保存；`:665`全部维度保存，Gradle成功、Java正常结束，exit0。新默认3废料→2熔融/1固体独立证明；旧04A/shutdown显式0份额复用，不冒充默认覆盖。
- `11-server-resume.log:77` 在同一server-final世界读回相同UUID/位置、单件载荷及无ledger状态，remaining4242→4240；**1续接业务 + 13SKIP**，exit0、全部维度保存。实际服务端任务为既有runGameTestServer，聚焦molten_corium及续接筛选；本审查未自行执行命令。
- 01业务RED、02客户端API编译失败以及04–08失败原日志/exit均保留，未隐藏为成功。04软管旧队列失败、05掉落燃烧计数失败、06–08核端口夹具坐标失败与最终10/11分开；故意保护异常被捕获并解除监听，不等于那些实际失败已通过。03/09单测9/9历史证据不代替最终10/10。
- 尚无本批候选JAR，未审不存在制品。根旧包仍3478572bytes / SHA `81E7CD98D589DDC9C0AAF398C06776DC4318EA54C70CD19376F0C127E4A3DE7B`，它不是04B代码的身份凭据。

客户端液面/真人操作、物理ChunkEvent.Unload、真人多人及开口泵真实传动管网均未实证；开口泵只有本地非源入口/无cap/替换守卫依据。普通箱子、核燃料及装配台的已测成功不扩大成任意第三方容器或直接section写兼容；不承诺跨文件崩溃原子性。既有封装/采掘和管理员全矩阵按卡复用，不另开旧存档迁移、无关性能或发布审查。

## 三项P2整改的最终窄复核（2026-10-21）

**P2-1、P2-2、P2-3均已闭合；本次改动切面未发现新的未闭P1/P2代码阻断。** 本节取代上文初审的未闭状态，初审问题、来源和证据原文保留。只复核指定三文件增量及13/14对应证据，未运行测试/构建/世界，无Git写；候选仍未打JAR，人工及未观测边界保持。

| 原问题 | 整改与复核 |
| --- | --- |
| P2-1 容器通知间接强载 | [MoltenCoriumEffects.java:146](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/MoltenCoriumEffects.java:146) 在保护探针前、[同文件:156](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/MoltenCoriumEffects.java:156) 在回调复验后再次检查读取域，均先于移料/loot。`:164` 保守核各目标六方向两格的hasChunkAt/getChunkNow；高度界外不读取，水平界外等待。实际缺FULL夹具先证第二邻格 hasChunkAt=false/getChunkNow=null；GREEN为spent0、原3钻石不动且无掉落/强载，测试显式加载后原生掉落3钻石。 |
| P2-2 HOT出现入口漏建 | [MoltenCoriumEffects.java:62](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/MoltenCoriumEffects.java:62) 仅复用MOLTEN；非MOLTEN先终止旧热引用、置DONE并撤旧索引，再建新空UUID/6000时钟。实际核直接setblock与采掘后同tick setblock两条入口；旧废料仍恰好一次原生掉落，新体零payload并继续扣时/移动。[ReactorAccidentSavedData.java:42](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/ReactorAccidentSavedData.java:42) 按旧id条件移除，旧清理不会撤新位置/区块/活跃索引。 |
| P2-3 预留位置被移动覆盖 | [MoltenCoriumEffects.java:98](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/MoltenCoriumEffects.java:98) 将未投影等入口统一限定为当前位置所有者，`:125` 在move改状态前拒绝其它本体预留。 [ReactorAccidentSavedData.java:32](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/ReactorAccidentSavedData.java:32) putIfAbsent拒绝覆盖，`:40` 在撤旧索引前核新位置冲突。实际核下落/横向两种冲突：spent0、位置/修订号/20与17处理进度不变、两份载荷各1，冲突索引原封不动，随后原预留所有者成功投影。 |

- `13-boundaries-red.log:103–113` 三项业务均实际失败：HOT仍旧HOT；预留冲突spent2/进度清零/修订号增加；箱子spent1/inventory0/spawns3/secondFull=true。非编译失败；Java exit3、Gradle exit1，14项显式SKIP，原日志保留。
- `14-boundaries-green.log:103–113` 与三个新增夹具的前置/断言相符；`:124` 的17 required passed实际为 **3项业务 + 14显式SKIP**，`:136` 全部维度保存，Java正常退出、Gradle成功且exit0。不把SKIP算新业务，也不将这轮冒称为重跑10/11/12完整矩阵。
- 当前SOURCE与 `SOURCE-before-boundaries.json` 比较，恰好只有 `MoltenCoriumEffects.java`、`ReactorAccidentSavedData.java`、`AccidentExecutionGameTests.java` 三文件源码SHA改变；其余源/资源未漂移。32源文件、51class、6处理资源全部匹配最新SOURCE，源码diff-check通过。main编译输出对应14轮，未改定向test输出仍对应12轮。
- 最新EXECUTION SHA256：`6091A7623DF8C6650E11B985DABC788724E746C82ECA5032DDF5F24E25A6B056`；最新SOURCE SHA256：`2E975A0DB35E3B5D95E1CA7085137C5183FC5DCE07E892BC1F81B60502920548`。初审清单独立保留，初审身份不冒充整改后身份。
- 读取域检查保守等待，不新增块写或预算豁免；新UUID不继承旧payload；索引冲突检查先于普通移动状态变更。未见这三处对原20/40、6000→2400、原取消/手拆、辐射或共享额度形成明显回归；这些未变部分复用已封存10/11/12证据，不重复执行。
