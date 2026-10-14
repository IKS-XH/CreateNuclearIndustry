# REACTOR-VARIABLE-SIZE-01 独立规格与质量审查

审查者为执行者 `/root/reactor_variable_review`，非PM。审查对象是 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration` 的 `codex/reactor-variable-size`，HEAD基线 `bf25343be89ac420e82c691269390a8ac44fb028`；功能与测试仍为未提交候选。本报告为一次合并规格/质量审查，首轮发现交PM后仅复审授权窄修及新增原始证据。审查者未运行Gradle/GameTest、未改生产/测试/治理或其他报告、未执行Git写操作；唯一写入是本文件。

## 合同、技能与范围

实际阅读根 `AGENTS.md`、`docs/project-governance.md` 第5.1/5.2节及活动计划 `docs/superpowers/plans/2026-10-12-reactor-variable-size-01.md`。按实际 `gradle.properties`/构建配置锁定 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82，没有以技能新版本示例升级技术栈。复用必要原验证，不研究历史存档迁移，不将自动检查替代客户端人工门。

实际使用的技能入口：

- [minecraft-modding](C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md)：核对真实注册、方块实体、服务端/客户端及锁定API边界。
- [minecraft-testing](C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md)：区分纯模型、真实世界/区块、capability与人工证据，核查断言而非日志文件名。
- [requesting-code-review](C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/requesting-code-review/SKILL.md)及同目录 `code-reviewer.md`：按合同审规格/质量，明确未作判断范围，再给候选结论。
- 同技能根的 `using-superpowers/SKILL.md`、`systematic-debugging/SKILL.md`、`verification-before-completion/SKILL.md`：定位安全缺口、核实锁定源码与原始输出；没有据此突破本任务禁止重复运行和禁止Git写的范围。

首轮逐文件读取实际生产、测试、资源差异及必要调用者；最终31个src路径全部由 [final-identity.json](E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/build/reports/reactor-variable-size/final-identity.json) 精确列出并核对当前SHA。包括21个main Java（其中本批GameTest）、6个JUnit文件、两语言和两个隔离域NBT模板：

- 几何/扫描/摘要：`ReactorDimensions`、`ReactorStructureDefinition`、`ReactorStructureScanner`、`ReactorCoolantCapacity`、`ReactorInstrumentStructureSummary`、`ReactorSurfaceDescriptorFactory`。
- 运行/维护：`ReactorInstrumentPortBlockEntity`、`ReactorPortBlockEntity`、`ReactorStructureLifecycle`、`P1ServerConfig`、`ControlRodSliderService`、`CoreColumnPosition`、`ReactorSnapshot`、`ReactorSnapshotNbtCodec`、`ReactorFissionCalculator`、`ReactorHeatPropagation`、`ReactorCoolantFluidHandler`、两种ColumnRepairTransaction。
- 教学/验证：`P1PonderScenes`、`ReactorVariableSizeGameTests`；`ReactorSnapshotTest`、`ReactorVariableThermalTest`、`ReactorCoolantCapacityTest`、`ReactorStructureContractTest`、`ReactorVariableScannerTest`、`ReactorVariableSizeTest`；`en_us.json`、`zh_cn.json`及两个`variable_reactor_empty.nbt`。

还只读检查现有燃料权威入口、停机/危险拆除事务、融毁/SCRAM、控制驱动器、L1/L2协议/客户端消费及 `ReactorInternalRenderBuffers`。冻结后复审只集中在仪表、生命周期、本批探针及新增waiting模板；其他13文件仅EOF空行整理。PM暂存后另发现新测试文件的一处EOF空行，本报告追加只读复核这一行删除及manifest更新，未重做全审或运行测试/构建。PM活动计划/CLIENT-CHECKLIST、用户存档、无关美术树、logs、`.superpowers`及工具缓存未改。

## 规格与质量核查

- 三轴独立5..16，SERVER min/max联合校验；偶数矩形、四侧偏心仪表、内部fuel贴壁、相邻不重叠结构、双仪表和非法框架均有实际扫描路径与断言。有限四表面寻边后完整验证，读缓存包含不可读结果，不按所有尺寸/原点穷举。最大唯一读取实测4131；理论16925在17000硬预算内；缺区块纯reader为77次，这不是物理卸载证据。
- 实际dimensions贯穿snapshot、所有with转换、当前NBT、正式tick、热传播、局部四向控制/超频、维修及运行投影。默认5³入口仅保留给旧fixture；正式坐标/遍历不截成九列。外高16时主体高14、196列，一列仍一个端口真实组件，未增加燃料权威副本。
- 裂变与燃耗按实际主体高缩放，两种维修恢复量为0.25/h；满功率内部高1的3小时寿命锚点保留。先整堆汇总/限幅后ceil一次，最后一项承接分配余量；断开燃料簇raw2.8得到3HU，而非逐列取整。亚mB缓存及量化余数仍守恒，0.75HU只转换1mB、留0.25HU。
- 冷热共用空列/控制列主体容量，16³且至少一燃料列时最多2730格；默认2730000mB。最大每格配置值按2730反推，long乘积预检后才转int，配置缩小时保留库存并拒绝超额输入；每口额度及fuel独占语义保留。
- 已加载owner索引有加载/移除/区块及世界卸载清理，变化通知按有限距离筛选；普通tick仅轮询缓存包围盒至多四chunk可用性，cap/goggles不做世界几何扫描。缺FULL立即暂停/撤销绑定且保留热状态、尺寸和库存；恢复重绑，handler捕获代数并校验端口/owner实例身份，旧handler不能复活。
- L1实际包围盒与L2列/主体位置同步，最大当前NBT往返验证196列各14主体、2730合法液体格；客户端棒travel14、底originY+1、顶originY+15。现有渲染消费者使用实际包围盒/主体位置；棒体与辉光先、液体后提交的缓冲修复保持。自动几何核查不证明真实GPU、字体、液面或鼠标效果。
- 修改的公共状态与非显然生命周期/热工/事务分支有中文职责或不变量说明；此次安全保护注释与实际状态一致。没有新增GUI、配方、其他设备玩法或扩大Ponder教程，5³示例保留且字幕只说明现行搭建规则。

## 发现及窄修复核

以下是首轮与同项窄复审发现，已及时交PM；括号内为最终候选行号。当前没有待整改P1/P2/P3。

| 优先级 | 发现、影响和精确位置 | 整改/复核结果 |
| --- | --- | --- |
| P1，已关闭 | 缺FULL后原扫描变无效/origin为空，原拆除事务不再产生计划，近端热堆组件可绕过拆除门；当前NBT新实例仅近端恢复，以及全部FULL但首次Post/有效扫描前也存在窗口。入口：[ReactorStructureLifecycle.java:110](E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorStructureLifecycle.java:110)，保护：[ReactorInstrumentPortBlockEntity.java:261](E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java:261)。 | 最后已验证surface/fuel列只作为拆除占据证据，保存于当前服务端NBT并限定owner坐标、维度、实际外尺寸、原格预期ID；不宣告成型、不接管库存。ChunkEvent.Load只从事件已有LevelChunk登记仪表，实例索引仍严格校验世界当前BE。恢复标记保持至首次实际有效扫描；无关空腔位置不阻断。`13`和`17`分别真实RED；最终`19`实际Break入口在全FULL新实例首次Post前、FULL刚失去但Post前、WAITING、物理卸载后仅近端新实例均拒绝原组件，状态/真实ItemStack不变；有效恢复保持原危险评估。 |
| P1同项事务边界，已关闭 | 初版窄修若在单个FULL_SHUTDOWN_RESET提交内提前清除占据证据，后续事务失败回滚不能恢复安全门。最终入口：[ReactorInstrumentPortBlockEntity.java:481](E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java:481)，调用：[ReactorStructureLifecycle.java:194](E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorStructureLifecycle.java:194)。 | 清除延后至所有计划commit、verify和危险事件发布成功后；回滚保留原证据。最终`19`实际reset提交后rollback仍保存BreakSafetySurface，`20`受影响停机/维护回归通过。 |
| P2，已关闭 | 清遥测时读取未加载远端drive，以及通知管道时无守卫getPipe可能隐式加载chunk，破坏“缺区块暂停/不强载”。位置：[ReactorInstrumentPortBlockEntity.java:1424](E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java:1424)、[ReactorStructureLifecycle.java:309](E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/src/main/java/com/iksxh/create_nuclear_industry/structure/ReactorStructureLifecycle.java:309)。 | 锁定本地Level/LevelReader源码确认getBlockEntity→getChunkAt→FULL,true；Create实际字节码确认getPipe入口未自检，propagateChangedPipe内部邻居已有isLoaded。最终增加drive、endpoint及每个邻接坐标hasChunkAt守卫。`19`对原绑定远端drive、未加载endpoint和已加载边界endpoint的未加载邻区调用正式入口，断言远端FULL始终不可用。 |
| P3，已关闭 | 首轮13个src文件新增多余EOF空行，影响差异整洁；例如[ReactorFissionCalculator.java:184](E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorFissionCalculator.java:184)的文件末尾。PM暂存后另识别新文件[ReactorVariableThermalTest.java:106](E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/src/test/java/com/iksxh/create_nuclear_industry/reactor/ReactorVariableThermalTest.java:106)末尾原第107行空行。 | 首轮普通`git diff --check -- src` exit0仅覆盖当时已跟踪差异，不覆盖该未跟踪新测试文件，不能据此断言所有新文件EOF已检查。追加实际diff确认只删除最后一条空行（107→106行），断言/代码不变；新测试源SHA与最终manifest一致。此次没有重跑测试/构建，JAR不变；未整理无关logs或缓存。 |

`14`、`15`、`18`仍是真实RED/夹具诊断，不能因文件名green算通过；`18`手工fresh不是Level当前BE，被生产严格身份过滤，最终`19`改为世界当前新实例并显式验证身份。无需放宽索引来迁就测试。最终探针直接调用生产BreakEvent规则并核对取消结果，没有把它夸大为客户端实际挖掘或完整事件总线手测。

## 原始证据与最终身份

所有下列日志位于 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/build/reports/reactor-variable-size/`。审查者直接读取日志及原XML；没有重复运行。

| 证据 | 独立核对的结论 |
| --- | --- |
| `11-final-directed.log` | 20s正常成功，原13/13新增定向项；完整build后来加入最大描述符项。 |
| `12-build.log`、`12-xml-summary.json`、`12-test-results/` | 唯一完整增量build，24s正常成功；实际94套514/514，失败/错误/跳过均0。原30源冻结保存在`12-stable-identity.json`；这是窄修前完整证据，未冒充最终再全测。 |
| `10d-world-green.log`、`10d-process-stop.json` | 真实2/2 required业务断言通过：6×8×7远端维护，16³绑定/维修/正式tick/当前NBT/旧handler；FULL失去与物理Unload分开，实际ChunkEvent.Unload=1，旧owner removed且index0，恢复为不同新实例/index1/16³。之后Saving worlds停滞，仅结束本轮PID34268；Gradle exit1、子进程−1。不能称整个GameTest命令exit0。 |
| `13-waiting-break-red.log`、`17-full-restore-red.log` | 拆除等待态及全FULL当前NBT首次扫描前分别真实RED；保留失败原因。 |
| `19-waiting-final-green.log` | 最终窄探针1/1 required、保存正常结束、26s成功；SHA256 `37439fb9291fa2eb1055c90ac99d6461fa4b9302cfe4af3db0825071c78e0b95`。覆盖上述安全边沿、无关位置放行、无强载与reset回滚。 |
| `20-directed-final.log`、`20-xml-summary.json`、`20-test-results/` | 21s正常成功；实际9套51/51，失败/错误/跳过均0，涵盖受影响生命周期、维护/停机、两维修、NBT及变量模型/扫描/热工。 |
| `21-assemble-final.log` | 最终必要增量assemble，1s正常成功。未重复完整build或原2业务。 |
| `final-identity.json` | 最终SHA256 `67e417661cba142195779555747b756ff0c1c80ec0daa849457994fd01f20b3b`；31源、50生产class及JAR范围不变。此前已独立核全部源SHA、class/JAR及四资源三方零差异；追加EOF复核仅更新测试源SHA `b39586301b74b896bb1c727a7f185b1ff8f3dc956406a8b4504c645e61eb59ff`，与当前文件匹配，其余验证与制品证据复用。 |

最终候选 [create_nuclear_industry-0.1.0.jar](E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/build/libs/create_nuclear_industry-0.1.0.jar) 为2659488字节，实际SHA256：`794a9ca78ec091116c5500be70999ac0082d1f219357cbe62ab9be66f18c4cef`。完整build原JAR `cacbdf...fce` 是历史身份，当前交付以此最终增量制品为准。实现报告已区分原完整证据与窄修最终制品。

## 未作判断的范围（Declined to judge）

- 客户端真实成型/持续运行、液面/棒体/辉光/GPU深度与采光、Font、顶面鼠标命中及人工挖掘行为：自动描述符与服务端探针不能确认这些。
- 完整外部Create远端管网现场表现：本次只审capability/端口生命周期及已授权的通知入口边界，不把局部探针推广为所有管网拓扑验收。
- 历史版本存档迁移/兼容：首发前明确不研究；只审当前版本正常保存读取及物理卸载后的恢复。
- 用户世界候选游玩进度、其他美术工作、其他设备/教程、main集成及最终用户验收：不在本执行者审查权限内。

## 结论

最终候选没有未关闭的P1/P2/P3必改项，规格与代码质量审查可交PM安排正常客户端人工门。客户端仍按PM的 [CLIENT-CHECKLIST.md](E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/docs/reviews/2026-10-12/reactor-variable-size/CLIENT-CHECKLIST.md) 检查5³原例、偶数长方体及16³跨区块的成型、实际供热、棒体/液位、顶面控件、远端拆件与保存重进。本报告不关闭视觉/玩法人工门、不验收交付、不授权或执行合main。
