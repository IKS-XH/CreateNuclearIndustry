# DEVICE-PONDER-03-BOILER 合并规格与质量审查

## 结论

**须整改，当前尚不能进入四幕播放候选。** 规格审查未通过；质量审查发现一个实际 Ponder API 使用错误、三处教学展示缺口和一处模板合同测试缺口。以下结论针对基准 `9bb514e137d8dad8b57f687fa132a06db620e221` 上尚未提交的实现，审查根为 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。本轮只读取实际源码、差异、精确未跟踪文件、现存 NBT、锁定依赖源码及已有证据；仅写此报告，未运行生成器、JUnit、GameTest、assemble 或客户端，未修改 Git、代码或存档。

## 必须整改

### 1. [P1] 剖视选择实际上只有首个边框格

位置：`src/main/java/com/iksxh/create_nuclear_industry/ponder/BoilerPonderScenes.java:332`；影响第1幕 `69～90` 行和第2幕 `128～129` 行。

`frontCasing()` 首次用 `util.select().position()` 创建单格选择，后续调用 `casing.add(...)` 却未接回返回值。锁定 Ponder 1.0.82 的 `PonderSceneBuildingUtil.PonderSelectionUtil.position()` 返回 `SelectionImpl.Simple`；其 `add()` **创建并返回** `Compound`，不修改原 `Simple`。因此三个搭建剖视分别只隐藏 `(3,1,3)`、`(3,3,3)`、`(3,4,3)`，运行剖视也只隐藏 `(3,1,3)`。这些均是左前棱边外壳，正面炉壳和隔层仍然遮挡讲解目标。实施报告中“隐藏正面壳层来讲解”的结论与实际 API 行为不符。

最小修法：接回 `casing = casing.add(...)`；同时按实际模板精确识别端口格，而非以 `(x==4 || x==8) && y∈[1,4]` 排除两列全部四层。当前掩码还把实际外壳 `(8,1,3)`、`(4,2,3)` 误当端口保留下来。应核对实际返回选择能露出水区、汽区和隔层且保留真实端口/窗口，不用注释或源字符串断言代替实际选择集合检查。

### 2. [P2] 缺水与冷液回路故障尚未淡出就恢复，且说明与故障错开

位置：`BoilerPonderScenes.java:294～305`。

两段正文均先完整播放并等待 `duration+20`，之后才隐藏给水/冷回路线，并在仅 `8tick` 后重新显示。锁定 Ponder 的 `hideSection()` 通过固定 `15tick` 淡出指令实现；当前隐藏尚未完成，原线路就已重新显示。正常播放只会看到很短的叠加淡入淡出，说明出现时线路仍正常，缺水/冷回路故障与恢复没有清楚的实物阶段。第三处出汽检查先隐藏、保持到正文结束再恢复，时序比这两处完整。

最小修法：先展示断路/缺水状态并等待淡出完成，在该状态期间显示对应短文案，最后明确恢复线路。缺水可同时降低临时水窗液位、恢复时回填；仅操作 Ponder 临时显示，不调用正式锅炉交易或增设机制。

### 3. [P2] 第三幕没有实际调压步骤，遗漏已有SC可排至公共下限的关键规则

位置：`BoilerPonderScenes.java:219～224`；相关双语键 `high_pressure_boiler_steam.text_4`、`text_5`。

当前控制器仅画滚动与右键提示，整个第三幕没有改变临时控制器的压力下限。末句只说“所选且热量合格的库存”，没有明确任务卡要求的“已有热量合格SC可继续排至公共出汽下限”。这无法帮助玩家区分新产汽分类所用炉压门槛和已存SC的输出下限，也缺少题目“调压”的可观察操作。

最小修法：通过现行原生控件对应的临时状态展示一次下限调整，保留两种已有库存身份；用独立短句说明“已付热合格的超临界蒸汽可继续排至出汽下限，低于新产汽分类门槛也不会自动变成普通蒸汽”。不要改生产门槛、混汽、降级或安全阀线。Java英文fallback、en_us与zh_cn及正文时序须同步。

已核实的可用API：`scene.world().modifyBlockEntity(CONTROLLER, BoilerControllerBlockEntity.class, ...)`，在临时实体的公开 `getAllBehaviours()` 中取 `instanceof ScrollValueBehaviour` 并调用 `setValue(10)`，可使原生控件从默认60变为10。控制器的压力行为自定义了自己的TYPE，因此不能复用汽口的 `getBehaviour(ScrollValueBehaviour.TYPE)`；`selectMinimum()`在客户端直接返回，不执行正式账本交易。另一条现存API是 `modifyBlockEntityNBT` 修改 `PressureControl.ScrollValue`，同样无需改生产类或开放private字段。只展示原生选值及解释，不需要跑热工。

### 4. [P2] 模板测试只要求palette含部件，漏检实际缺件和错件

位置：`src/test/java/com/iksxh/create_nuclear_industry/BoilerPonderContractTest.java:69～87`。

`names` 从palette收集后断言包含部件，遍历实际方块时只对已识别端口、管路及泵/马达执行局部检查。删去所有实际底部换热器条目而保留palette，删去隔层中一个实际再加热段，或把安全阀实际坐标改成palette中的外壳，现有测试都不会失败；缺失/替换后不再命中相应 `assertPortLayout` 分支。该问题正是本卡要求避免的“部件名存在但实际模板不能成型”。实际当前四个NBT完整，这是一处保护合同缺口，不能据3项测试通过宣称已验证完整结构。

最小修法：按NBT中实际引用的state统计和检查部件；检查完整隔层占位、必要换热器和控制器/安全阀实际存在及合法位置，安全阀上方净空。沿用本批一个资源合同测试即可，无需全量负例、GameTest、额外模拟器或逐句镜像实现。

### 5. [P2] 启动幕同时启泵，漏掉先给水/接回收再供热的操作顺序

位置：`BoilerPonderScenes.java:134～148`、`163～166`。

给水、热液、冷回收三组线路同时显示，储罐同一时点填入，四泵及驱动也同一时点启动；相应正文只有“独立管路”与“冷回路堵塞”。前面的临时窗口35%水位不展示给水操作。玩家无法从操作或短文案获得任务卡明确要求的“先给水并接通冷热液循环，再持续供热”，会把先接热源也当作教学顺序。

最小修法：在同一场景中先展示并启动给水回路、增加水窗液位，再显现/启动冷回收，最后打开热液供给；插入一条短句说明顺序。复用已有 `showSection`、`setKineticSpeed`、`setTankFluid` 和 `fillWindow`，不修改正式机制，也无需增加新测试框架或全量验证。

## 已核实的静态部分

- 已完整读取根AGENTS、任务卡、治理第4/5.1/5.2节、REWORK-01/01A/01F；实际应用 `minecraft-modding` 和 `minecraft-testing`，并读取 `subagent-driven-development` 的审查流程。锁定依赖与任务卡一致：MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82；使用本机对应sources.jar核实Selection、泵、控件和淡出API。
- `P1PonderPlugin` 实际注册九种锅炉部件及四故事板，顺序正确；反应堆四幕/11入口和离心机注册代码未改动。没有提前注册其他设备教学。
- 用独立只读解码读取四份实际gzip NBT，无导入/执行生成器：尺寸均为13×9×13，实际块数243/264/249/270；7³炉体闭合。底面有3台炉内换热器，隔层内部25格完整，水区y=1～2、汽区y=4～5。两热口均为底边非角点，两冷口处在隔层非棱边，水/汽/控制器层位合法且全部朝north；安全阀实际位于 `(6,6,6)` 顶面非棱边，上方y=7、8为空。对照 `BoilerStructure` 未发现现存模板成型缺件。
- 现存两种蒸汽管路分别从 `(4,4,3)`、`(8,4,3)` 沿z=2、1连接各自z=0储罐，无错位、混管或管线穿壳。给水、热液和两个冷回收泵分别对齐实际端口；外部马达朝south、齿轮轴z，与泵平行啮合。马达/齿轮+64、泵-64的符号匹配；锁定Create的泵流向由facing决定，给水/热液south为+Z，冷回收north为-Z。四组驱动均加入运行/停机幕showSection选择。
- 临时汽口设置通过 `ScrollValueBehaviour.TYPE` 获取实际 `ScrollOptionBehaviour<BoilerSteamKind>`，0=NORMAL、1=SUPERCRITICAL，与正式Enum一致；不依赖默认SC冒充普通汽。两种库存共用容量/炉压、调压不改已有种类、过滤不降级、压力下限不是目标炉压或安全阀线的既有正文正确。
- 停机拉杆邻接控制器并实际cycle POWERED；外部四泵动力没有随红石停止，两种汽罐在切换后填入，构成临时余热出汽提示。全部状态修改限定于Ponder世界，没有改锅炉/汽轮机/换热器运行代码。
- 逐条核对现有28个标题/正文英文fallback与en_us完全一致；zh_cn/en_us仅新增本批键。`note`等待duration+20，锁定正文寿命duration+10，净间隔≥10tick；运行末段95/115也一致。正文均附关键帧，无事故/辐射或未实现热源文案。缺失知识见整改项3。

## 已有验证证据及边界

读取 `build/reports/extension/DEVICE-PONDER-03-BOILER/` 的 `generator.log`、`generator.exit-code.txt`、`targeted-test.log`、`targeted-test.exit-code.txt`、`assemble.log`、`assemble.exit-code.txt`，均exit0；生成块数与实际NBT一致。读取JUnit XML：`BoilerPonderContractTest` 3项、`P1Ponder01ContractTest` 6项，均0失败、0错误、0跳过；定向测试25秒，最终增量assemble2秒。候选JAR为2,282,008字节，实施报告已记录SHA-256与复制位置。原始首轮失败日志保留；未以删除的源文本/注释断言作为当前证据。

已读取最终 `implementation.md`，其中对剖视成功和清晰故障恢复的描述须随整改纠正。`PLAYBACK.md`由PM管理，不属于实现者越界写入；既有logs和pycache保留。未要求旧档兼容、扩大热端回归或重复构建。

**静态资源、编译、定向测试已通过，但当前场景代码尚须整改。** 整改后仅需复查实际差异及相应定向证据；默认镜头遮挡、窗口/储罐实际液位、原生控件外观、故障恢复动画、字幕和关键帧回放仍属客户端集中播放门，本审查没有人工播放证据，不宣称人工通过或可合入main。

## R1实际整改差异复核（2026-10-08）

**最新结论：五项原问题全部关闭，规格与质量静态审查通过，可进入本台四情景集中播放候选。** 本节是同一批整改的定向复核，取代上文“须整改”的当前结论；原问题及首轮证据保留历史含义。未重做整轮审查，未重复测试、生成器、构建或实际播放。

| 原问题 | 实际源码核对与关闭依据 | 状态 |
| :--- | :--- | :--- |
| 1 剖视集合 | `BoilerPonderScenes.java:352～360`按真实端口坐标、控制器和窗口保留，并接回`casing = casing.add(...)`；按锁定API语义得到多格Compound，而非单格Simple。y=1～5正面隐藏外壳共20格，含此前误保留的`(8,1,3)`与`(4,2,3)`；未改实际合法模板。 | 已关闭 |
| 2 故障与恢复 | `308～339`三处先hide、等待20tick，再在故障保持期间播放正文；正文后show、等待20tick，再显示恢复流向。缺水还清空临时水罐、降低窗口液位，恢复时回填。20tick覆盖固定15tick淡入淡出。 | 已关闭 |
| 3 调压与SC规则 | `227～238`显示交互后在`230`实际将临时控件设10，讲解后向独立SC接收罐填入SC。`398～407`从控制器`getAllBehaviours()`取得实际滚动行为，绕开私有自定义TYPE；客户端回调不写正式交易。正文解释已有合格SC低于新产汽分类炉压仍可排至公共下限，身份不变；Java`233`及双语`text_4`也已同步补回0～100范围。 | 已关闭 |
| 4 实际NBT合同 | `BoilerPonderContractTest.java:74～143`从每个实际state引用建立`referencedIds`和`actualBlocks`，检查全部必要部件真实引用、各类端口/控制器/阀具体占位、阀上方净空、合法底部至少一机和完整25格隔层/至少一段。原报告列出的实际缺机、缺隔层格、阀被外壳替代均会命中现有断言；没有新测试类或镜像源码断言。 | 已关闭 |
| 5 启动顺序 | `BoilerPonderScenes.java:134～174`先给水泵/水罐/窗口和正文，再冷回收泵/双罐和正文，最后热液泵/热罐和正文；各阶段动力及流向与原合法模板一致。运行正文更新8条，双语同步。 | 已关闭 |

只读逐条核对本次新增/改写正文与英文fallback，数量为搭建7、运行8、蒸汽5、停机5；未引入正文寿命/净间隔问题，所有新正文仍通过`note`等待duration+20。复核期间发现`text_4`用10%例子替换原范围后遗漏0～100，已交回同一实现者在同一句补回，现Java、en_us、zh_cn一致，未扩大范围。

读取最终实施报告的R1附录及`build/reports/extension/DEVICE-PONDER-03-BOILER-R1/`：`targeted-test.log`/退出码0，JUnit XML为锅炉3项、0失败/错误/跳过；`assemble.log`/退出码0。0～100文案补回后仅读取`assemble-final.log`/退出码0的增量打包证据，没有重跑JUnit。旧反应堆6项与未改模板/生成器证据复用首轮，本轮不冒充重新运行。最终候选为`create_nuclear_industry-0.1.0-R1-final.jar`，2,283,254字节；最终实施报告记录SHA-256 `A5EA3E8B371FB542CB171B4017392AF58981A2B37CA43347E198AF2FBD33FD3A`，旧R1与首轮制品保留。

**本轮只有静态/编译/定向证据，不是客户端播放通过。** 动态剖视实际视线、端口与储罐区分、窗口/流体显示、原生控件外观、故障恢复动画、字幕及关键帧回放仍由用户集中播放确认。结论仅允许进入本台播放候选，不改变任务状态、不授权合入main或推进下一台教学。

## R2首次进入崩溃的定向复核（2026-10-08）

**结论：本次R2源码修正与锁定Ponder的调度/合并语义一致，未发现本次差异仍需整改的问题，可交付修正候选供用户重新按W进入及播放验证。** 基线为`90aa89d61615e72bcda1f06c4ee3fa2bba0e06e9`。仅读取本次两代码文件差异、当前显隐调用及R2报告/证据；未重复整轮审查、运行测试、构建、生成器或客户端，未Git写入、子派发或改存档，仅追加此报告。重新阅读当前AGENTS及任务要求，并沿用已读的`minecraft-modding`/`minecraft-testing`，实际核对本机Ponder1.0.82源码。

原始崩溃报告`run/crash-reports/crash-2026-10-08_03.06.39-client.txt`及R2快照中的堆栈明确为`PonderScene.tick`→`hideSection`回调→`WorldSectionElementImpl.erase`，其中`section == null`。旧搭建幕首批指令仅排入基础板显示，随后立即擦除尚未显示的整炉；`showSection`不是立刻初始化基础区段。R2删除该次`hideSection(whole, ...)`，符合模板默认未展示的生命周期，不需要先显示整炉再擦除。

### 15tick合并边界的实际源码依据

- `PonderSceneBuilder.showSection`（sources.jar第428～429行）排入`DisplayWorldSectionInstruction(15, ...)`，该指令非阻塞。
- `TickingInstruction.tick`（第33～37行）每次调用将`remainingTicks`减1；第15次tick降到0。`DisplayWorldSectionInstruction.tick`（第45～49行）在同次tick看到0后调用`element.mergeOnto(baseWorldSection)`；`WorldSectionElementImpl.mergeOnto`（第92～97行）若基础区段为空则`set(section)`，否则`add(section)`。
- `idle(15)`实际是`DelayInstruction`，它是15tick的阻塞指令。`PonderScene.tick`（第311～321行）按原顺序tick指令，处理完成的阻塞等待后仍会`break`结束当前调度循环。因此第15次tick先完成显示合并、再完成等待并退出；其后的hide回调到第16次tick才执行。这里15足够，不存在第15次tick先erase再merge的边界竞争。

### 显隐、选择及回放核对

`BoilerPonderScenes.java:57～66`的新基础板选择由四个首尾衔接且不重叠的Y=0矩形组成：z=0～2、z=10～12各39格，z=3～9的x=0～2、x=10～12各21格，总120格；与炉底x/z=3～9的49格不相交。四段通过完整链式`add`表达式赋给`basePlate`，没有丢弃`Simple.add`返回值；后续不修改此选择。该结论是选择覆盖，未把模板中空气坐标误称为新增实体地板。基础板与炉底同时淡入只覆盖各自坐标，炉底仅显示一次。

搭建幕水区`71～74`、隔层`81～84`、汽区`90～93`均在对应显示后等待15，再隐藏其正面子集。上一层剖面恢复与下一层显示不重叠；新等待让两段都完成合并后再剖视。顶层显示后8tick即开始文字没有后续erase，仍可完成淡入，不构成此次空基础区段问题。删除整炉隐藏后仍按底层→水区→隔层→汽区→屋顶顺序展示，没有提前把上层全部显现。

同时核对本文件其余hide：运行幕`133`位于整炉显示、12tick等待及95tick正文的115tick等待之后；停机幕给水`314`、冷回收`325`、汽路`336`位于开场显示和长等待/正文之后。三处故障恢复均继续保留20tick等待，各被隐藏的实际选择已在基础区段完成合并；没有新增未展示选择先erase的问题。

回放方面，`PonderScene.begin`（第225～245行）会重置全部指令计数、恢复世界、清除元素/链接，并将基础区段置空；`DisplayWorldSectionInstruction.firstTick`重新以`initialSelection`复制设置临时显示元素。`WorldSectionElementImpl.set`使用选择副本，基础区段的add/substract不会改写本场景的初始选择。`PonderUI.seekToTime`向后跳转会先`replay()`→`begin()`，向前seek通过原`tick()`循环推进，保留阻塞顺序。因此本次删除初始erase并等待合并的修法在静态语义上也覆盖重播/关键帧恢复，不依赖上一轮已初始化的基础区段。这仍不是实际回放体验证据。

### R2现有证据与人工门

已读取`build/reports/extension/DEVICE-PONDER-03-BOILER-R2/`的最终合同红版`targeted-test-red-contract.log`/exit1（4项中新增生命周期断言失败，未显示整炉初始擦除）、`targeted-test-green.log`/exit0、JUnit XML（4项，0失败/错误/跳过）及`assemble.log`/exit0（增量2秒）。较早初步红版保留，未将它与最终合同红绿混淆。读取最终R2实施附录：候选`create_nuclear_industry-0.1.0-R2.jar`为2,283,313字节，实施报告记录SHA-256 `9EC12EB5F923697D9D1D24A6140BB169180915C8BA6FF4CC2DA8F6C8F23FA17F`。

新增回归只从当前搭建源码提取明确选择坐标和指令等待，能捕获本次已知初始erase、底板重叠及不足等待的回退；它没有实际驱动`PonderScene.tick`/渲染，也不能替代所有可能写法的生命周期验证。本轮以锁定依赖源码的实际顺序推导补充了这一静态证据，未为此追加模拟器、全量测试或客户端运行。

**当前状态是R2静态修正/定向合同/编译证据通过，用户尚须重新进入并集中播放。** 必须确认按W首次进入不再崩溃，四幕可完整播放，以及搭建切层、重播和关键帧跳转正常；此前R1“可进入候选”不代表这项真实客户端门已通过。本结论不宣称客户端崩溃已获用户确认修复，不授权合入main或继续其他教学/主线。

## R3 删页差异复核

**结论：R3删减范围通过，可交项目经理进行本台三幕定稿的必要定向核对及合入后增量打包。** 本结论只覆盖停机与排查第四幕的移除，不代替项目经理执行整合，也不另行宣布人工验收。前三幕播放通过依据为用户本次反馈；本审查者未播放客户端。

复核基准为候选当前 `HEAD 65836f4`。对照未提交代码差异，第四幕场景方法、专属常量与 `BlockStateProperties` import、插件中的第四故事板ID及绑定、生成器构造/输出分支、NBT文件和中英文对应键均已移除；九个锅炉入口仍绑定搭建、运行、蒸汽输出与调压三幕。锅炉Ponder源码、合同测试、生成器及两份语言文件中未发现停机故事ID、方法或生成分支残留；搜索到的 `P1PonderScenes` 红石拉杆引用属于反应堆教学，不在锅炉删页范围。代码差异未改写前三幕场景或R2显隐时序；仅删除原第四幕独有内容并将三幕故事数同步至注册、测试和生成器说明。新增/修改的手写注释为中文。

通过 `git hash-object` 与 `git rev-parse HEAD:<path>` 逐项比较，保留的 `high_pressure_boiler_build.nbt`、`high_pressure_boiler_operation.nbt`、`high_pressure_boiler_steam.nbt` 分别与HEAD blob完全一致；停机NBT在工作树中缺失。读取既有 `build/reports/extension/DEVICE-PONDER-03-BOILER-R3`：范围记录列出三模板未变、九入口/三故事板、双语JSON有效及第四幕资源为零；定向测试日志和退出码记录 `BoilerPonderContractTest` 成功、退出码0。再读取JUnit XML确认4项测试、0失败、0错误、0跳过。源文件差异的 `git diff --check` 无空白问题。本次未运行测试、生成器、构建或客户端。

本候选状态同时包含 `AGENTS.md`、`docs/README.md`、活动计划、实施报告、日志等本审查合同之外的改动，以及未跟踪的Python缓存目录；本结论不审查、不认可这些改动。项目经理整合前应按其职责确认并处理这些范围外内容。技能已实际读取并应用 `minecraft-modding`、`minecraft-testing`、`receiving-code-review`；核对版本为Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10、Ponder 1.0.82。
