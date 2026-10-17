# RADIATION-02 执行报告

本报告只记录候选实现与自动证据；不代表玩家验收、Git提交、合入main或正式包提升。工作树`E:/MyMC/NewMod/Create_NuclearIndustry-accident`，分支`codex/accident-execution-02`，创建基线`b39d11690fdbd8861e5d5fa7d06efa7d53d9e8de`，既有生产来源`f819fcfadf75509c7b23899daa9a7b9c59dbc56b`。未启动、扫描或改写携带的`run/saves`及用户辐射世界；所有世界操作限于`run/accident-execution-gametest`。原始携带证明沿用`PREFLIGHT.json`。PM最终冻结源码为`d839bc7e7f281a935a22a8d6121b45e3fa6027f3`，候选输入与该提交绑定，未因HEAD变化重建。

## 实现与边界

- 既有`COUNTDOWN_COMPLETE`/`DANGEROUS_DISASSEMBLY`接独立每维度`ReactorAccidentSavedData`；任务先登记，再逐口托管真实组件，仪表立即锁定、清工质/热账并撤销交易、运行样本与管理员许可。正式tick发布返回后短路，不能再用旧result投影/付款。未修改原触发公式、倒计时或正常热工。
- `prepareAccidentFuelCustody()`按已验证燃料列图交叉读取每个实际顶口，任一缺口/不可用/绑定不符即拒绝准备，不能把`boundPorts`缺项当空。托管一件已用（含小数使用历史）、损伤原件或原乏燃料只生成一件废料；全新物保留完整原栈返还。原始组件仅作不可提取审计，辐射和输出只读真实托管payload。
- 事故参数默认外伸2格、壳/外部共32格、近距冲击8HP、每维度共享64动作/t；创建时冻结，非法整组回退默认。工质100%损失，不喷可回收流体；缓存HU及热液所含HU记入释放/耗散账。
- 世界计划固定目标/原状态/实体世代，分批轮转，仅访问实际FULL可用部分；无法安放的载荷留托管。残骸只持一件真实废料，空残骸无源、无免费产物。已结算仪表残余可正常拆除。
- `CommonHooks.fireBlockBreak`内原生post返回后，`ReactorBreakFinalizationMixin`调用`finishBlockBreak`；LOWEST只做预检。最终取消无托管，多所有者监听失败在任何事故commit前回滚；成功后再一次接管全组。
- 事故写块使用`UPDATE_CLIENTS|UPDATE_KNOWN_SHAPE`，避免原版邻居/形状更新越过目标检查而读取远端区块；显式失效本模组结构与辐射缓存。已有邻堆世代冻结到`knownOwners`，只保护事故后新成型世代；邻堆库存/BE仍不作为可销毁目标，不直接强制其融毁。
- `complete()`完成后缓存派生终态，避免历史16³计划在普通tick反复全扫；缓存不保存，当前格式加载后重建。未改未完成收据、预算或不可用判断。
- `accident_fuel_waste`走既有装配台，1废料+1既有固化基材+1空铅桶/12800工作；原乏燃料/制造保持。两种已知sealing按实物与完整配方ID/材料谓词锁定，切配方不继承工时。单件桶只允许一种合法单件载荷，混装/异物/数量错误拒绝。
- 托管源按区块索引、事故ID+列身份去重，原列底部定位；消耗既有栈预算且不读取未加载源。托管→残骸/返还即时撤销受影响旧采样，防止完整零读数或双源。原物品保护自动拓展废料，未改剂量/恢复/半径/常态预算。

## 实际技能与版本

实际读取并应用`C:/Users/IKSXH/.codex/skills/`下`minecraft-modding/SKILL.md`（注册、服务端权威和生命周期）、`minecraft-testing/SKILL.md`（定向JUnit/隔离真实GT）、`minecraft-ci-release/SKILL.md`（版本/候选身份）、`minecraft-resource-pack/SKILL.md`（1.21.1原版占位、资源引用）。使用已批准计划的inline执行、TDD、系统定位和完成核验；按治理5.1不运行clean、全build、强制重跑、无关矩阵。Gradle实际锁MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，无版本/依赖/许可/发布变更。

## 写集

新增`accident/`八类、`config/AccidentConfig.java`、`content/AccidentContent.java`；修改入口、创意栏、仪表/端口、`ReactorMeltdownEvent`与结构生命周期。修改`RadiationItemSources`、`RadiationSourceAdapters`及经PM批准追加的`RadiationSourceIndex`；修改`SpentFuelPayload`、装配配方/方块实体。未修改剂量算法、`RadiationContainment`、`RadiationServerEvents`或`ShieldedAssemblyState`。

资源只有双语名称、残骸blockstate/block+item模型、废料item模型、普通镐标签、空本体loot及新封装配方；均引用原版素材，无新贴图/动画。经PM批准追加`mixin/ReactorBreakFinalizationMixin.java`和mixin JSON接线。测试为`accident/AccidentLedgerTest.java`、两项原测试增量及一个隔离`AccidentExecutionGameTests`/模板；`build.gradle`仅补本域排除。报告、脚本、原始日志只在本报告目录。

活动卡及`docs/project.md`、`server-config.md`、`content-catalog.md`、`recipes.md`文档由PM维护，本执行者没有写治理/STATUS/核心文档或Git。根logs为测试产生的输出，交付前归档并恢复基线。候选tracked基线JAR须恢复原`81E7CD98D589DDC9C0AAF398C06776DC4318EA54C70CD19376F0C127E4A3DE7B`，新的唯一assemble输出另存报告目录。

## 自动证据

所有完整命令输出保留同目录。原生Java退出值和Gradle/脚本退出分别以原始日志为准；下表的exit为Gradle进程。各次窄核使用同一真实服域/独立目录，不把显式SKIP算作业务通过。

| 证据 | 实际业务/结果 |
| --- | --- |
| `01-red.log` | 账本/参数未实现时编译RED；原命令尾部读取使shell exit为0，不能把它当构建成功，日志明确BUILD FAILED |
| `02-compile.log`、`05-unit.log` | AABB构造参数及测试ScanResult getter接入错误，exit1；按实际锁定源码纠正 |
| `03-source-red.log` | 废料4D/s/封装用例先失败，exit1；实现后`04-unit.log`/`06-unit.log`通过，未据此宣告整体完成 |
| `07-gametest.log` | 首轮5业务，3通过/2夹具失败：装配台取内部面、返还物下落离开小AABB；均保留真实失败后改正确外表面/唯一实体ID检查，Gradle exit1 |
| `08-gametest.log` | 7实际业务全部通过；正式tick/物理组件守恒/16³/共享预算/废料封装/原生最终取消与双所有者失败/待办保存。保存全部维度且exit0 |
| `09-gametest.log` | 新增实际Create动力三链及原生正常停服重进托管源4→残骸4通过；两新增夹具失败（过早检查已耗尽本tick预算的冲击、未观察到物理Unload），exit1，不称整轮通过 |
| `10-gametest.log` | 冲击3正式tick等待、每事故8HP一次/保存收据、关闭破坏仍内部报废与损失通过；物理Unload未见的夹具仍失败，exit1 |
| `11-gametest.log` | FULL撤销后远端被原版形状通知读取并重新可用的实际失败，exit1；据锁定Level/NeighborUpdater调用链改事故写块标志18 |
| `12-chunks.log` | 仅测试筛选helper编辑命令转义失败导致编译错误，exit1，未启动真实业务 |
| `13-chunks.log` | **1项实际FULL失效/等待/恢复业务通过，9夹具显式SKIP**；355+200tick等待中远端不被读取/任务保留，恢复后196列完成；原生物理Unload事件0，保存/exit0 |
| `14-ownership.log` | 废料采掘/四伤害保护/封装/架业务通过，邻堆夹具揭示旧newOwnerAt把原有邻堆也当新世代保护，exit1；PM批准冻结knownOwners窄修 |
| `16-ownership.log` | **2项实际所有权业务通过，9夹具显式SKIP**；共享壳确实排定并删除，邻堆保留原组件/自行失效；残骸/废料保护/封装/架通过，保存/exit0 |
| `17-cache-red.log`→`18-unit-final.log` | 终态拒绝重扫列表先RED，再GREEN；当前格式加载重建。最终卡内定向JUnit **18/18，0失败，exit0** |
| `19-ownership-final.log` | 1项废料链通过；邻堆逐字段matches夹具失败（壳破裂前允许正常燃耗），exit1；只修夹具为单件燃料/唯一原件标记，不扩生产 |
| `20-ownership-final.log` | **2项实际所有权业务通过，9夹具显式SKIP**；原共享壳确实排定/删除、邻堆原件身份保留；后建机组未进knownOwners且共享壳/结构完整保留。保存/exit0 |
| `21-assemble.log` | 唯一一次增量assemble，compileJava/processResources UP-TO-DATE，仅jar执行，exit0；无test/clean/full build |

卡内JUnit命令：`./gradlew.bat test --tests '*accident.*' --tests '*radiation.RadiationItemSourcesTest' --tests '*production.ShieldedAssemblyStateTest' --tests '*reactor.ReactorMeltdownEventsTest' --console=plain`。

真实服命令：`./gradlew.bat runGameTestServer -PgameTestNamespace=cni_accident_execution -PgameTestDirectory=run/accident-execution-gametest --console=plain`；13窄核临时环境`CNI_ACCIDENT_FOCUS=chunks`，16/19/20为`ownership`，每次命令后移除环境值。未选夹具输出SKIP并即时成功返回，因此框架的All10/11不是本次业务数量。主业务按对应未变实现沿用08/09/10等原始证据。

16³实际每次196份物理组件，计划2776目标（2744内部+最多32壳/外部），含输出和保守收据处理共2973预算项，峰值64项/t。13完成回合51项、2252µs；其它实际回合耗时在原日志，属于这次独立GT的动作测量，不是新的客户端/MSPT性能基准。外部实际破坏≤32，冲击每玩家每事故≤8HP一次；FakePlayer夹具不等于真人多人。

## 仍需明确的证据边界

真正的`ChunkEvent.Unload`本批未观察到，未冒称原生物理卸载通过；已证明真实FULL可用性撤销/恢复（没有注入hasChunk假的替身）和原生独立服正常保存退出/再进后托管守恒。真人多人、客户端视觉与玩家清理体验未验。已有首批辐射/头罩体验只沿原用户确认，不扩大为本批事故验收。

## 候选制品与集中人工门

唯一增量assemble/exit0。候选`candidate-accident-execution-02.jar`：3533416 bytes，SHA256 `BF6D4D14D6A4C1D55C48C8719A024E5488E339D10ABFBA7F3E7744FCFE0FC4FE`。`ARTIFACT.json`、`ENTRY-MANIFEST.json`逐项证明全部762个class与当前编译输出完全一致且各有当前源文件绑定、1692个资源与processResources输出完全一致；JSON解析通过，新配置/注册/mixin/资源入包，本批GT类/模板/域排除。`SOURCE-BINDING.json`证明2120项入包相关来源输入的Git规范化工作文件与该提交blob一致，并逐项绑定44个改动文件（含被exclude-standard隐藏、PM明确纳入的AccidentConfig）；762class关联源SHA及1692资源的已编译输出身份同时保持。`WRITESET.json`区分39项执行写集与5项PM文档，`UNIT-RESULTS.json`保留18项精确汇总。

`ROOT-RESTORE.json`证明原根logs完整哈希还原，验证根日志另存`validation-root-logs/`；候选工作树tracked `build/libs/create_nuclear_industry-0.1.0.jar`已还原3478572 bytes、SHA256 `81E7CD98D589DDC9C0AAF398C06776DC4318EA54C70CD19376F0C127E4A3DE7B`，新包只留本报告目录。本执行者未执行Git写、未提升正式包、未写main；PM已提交候选源码`d839bc7e7f281a935a22a8d6121b45e3fa6027f3`。

按批准计划的TDD还原：账本缺类RED→实现，废料源断言RED→实现，真实FULL等待失败→写块标志18，邻堆扩大保护失败→冻结knownOwners，终态缓存拒绝重扫RED→缓存GREEN。未把编译错误、夹具错误或显式SKIP当成功；未证物理卸载保留边界。

只在新建测试世界进行一次连续体验：触发事故→观察局部破裂和原堆/管理员停机→穿防护并读辐射计靠近→普通镐采掘每列一份废料→带/臂/箱转运→既有装配台封装→入完整架→现场读数下降。复看处理中正常保存重进后数量不重复/不丢失，全新原件返还；原正常反应堆/管理员和原乏燃料封装保持。由PM收这一次集中反馈，未通过前不推进教学、美术或其它批次。
