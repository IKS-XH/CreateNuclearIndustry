# REACTOR-ADMINISTRATOR-03 执行报告

2026-10-14。唯一执行者接续原候选 `Create_NuclearIndustry-reactor-automation` / `codex/reactor-automation-basic`。生产基线 `2dff787`，开工文档 HEAD `6fcf9c0`，原包 `5e04c667…`。功能源码未提交；不代表客户端手测或 main 集成完成。

## 最终结果

- 三页管理员 GUI：总览、最大14×14只读堆芯、保护；各页固定立即停堆，数字直接输入及步进（Shift×100、Ctrl×10），完整草稿原子应用，独立启动与确认复位。
- 原四世界控件不再注册，旧专用写入口始终拒绝；原手持燃料、工具、机械臂及原生计时仍保留。
- 部分燃料自然耗尽或补料不再单独撤权；全部可用燃料归零仍故障保持，补料不自启。
- 默认真实1HU/t维护成本，正有限配置范围 `(0,1000000]`。先使用新生裂变热，再按燃料列与控制棒列真实缓存余额比例支付，整笔不足不扣款。杆维护只扣缓存并保留机械字段；原冷却只减燃料维护支出，维护不是事故有效冷却。
- 每份付款只能消费一次，供热票据不持久化。仅真实普通外部燃料或上一短期已付凭据有请求资格；假KINDLED不续燃；SPECIAL原计时及到期赠NORMAL5000保持。已知失联/失败撤票，普通外燃料恢复原倒计时。
- 33个写集文件冻结，正式与冻结候选JAR一致；新测试类和NBT不进入生产包。原组合审查四项Important已按下方同次整改收敛，35轮必要纠错增量包为当前最终制品，待同一审查者窄复核及用户集中复看；原e1c、d372与5e04完整保留。

## 技能与边界

实际阅读本树AGENTS、03设计/卡、01及01R报告；使用 minecraft-modding、minecraft-testing、executing-plans、TDD、systematic-debugging 和 verification-before-completion；同次整改实际读取 receiving-code-review 与原REVIEW。技能的全量构建、逐步Git提交、转派例由任务卡的唯一执行者/一次组合审查/精确写集覆盖。

未写用户 `run/saves`、实例配置或 main；没有启动用户世界、新建/搬动工作树、Git写或派人。根 `logs/debug.log` / `logs/latest.log`原本已有用户改动，定向JUnit也按原工作目录产生测试日志，不纳入功能写集。活动卡/候选说明为PM的独立文档改动。02 PI、管网、锅炉、汽轮机和美术未推进。

## 实际接口与事务

菜单注册 `reactor_administrator`；真实 Create只声明useItemOn，因此空手最小注入同时保留继承空手入口。GameTest实际空手useItemOn打开原生ServerPlayer菜单；FakePlayer自身openMenu空返回，测试只借其无网络连接，不借其菜单替代品。

当前菜单同时检查玩家身份、可达性、旁观、owner/接口/燃烧室实际实例、结构代际、随机session UUID、epoch、设置revision和单调sequence。每次打开新session，避免原生containerId循环复用旧请求。APPLY/START/RESET拒绝过期设置；STOP仅在当前有效会话身份下容忍旧epoch/revision，不能以旧session停新菜单。真实代表循环openMenu至containerId复用，保持几何/epoch/revision不变，旧会话APPLY/START/STOP全部拒绝。

客户端第一次编辑固定草稿revision和epoch，显示更新不升级草稿；对应APPLY成功才清理，失败保留草稿与中文原因。START读服务器已确认设定。服务器只向当前查看者同步初始投影/变化数据，默认10tick数字刷新；phase/epoch变化立即刷新，绑定失效关闭菜单。Projection最多64根字段、196格、12列字段，限制数值、字符串与位置，禁止嵌套库存/完整快照。UUID只作身份元数据。

正式核tick先完成原燃料端口与快照事务，再commitPayment。管理员调节沿原beginAutomationTick去重；维护另以requestedAt/committedAt/lastPaymentAt去重，包含“本tick先零请求提交、随后改燃料再入”的实际边界。clear撤销票据及消费显示，但保留该owner同tick去重时刻，换成员/失败再入不能重收费或把旧消费视为新成员已消费。

原NORMAL倒计时仅在实际燃烧室tick消费了付款时跳过原单次减一；不取消整段tick、不补满燃料、不冻结SPECIAL。热级只允许当前tick实际消费，菜单来源可读当tick/相邻tick已完成供能事实，显示不授予冻时或热级。生成热为毛HU/t，净新生、缓存和缓存维护支出分别投影。

## 实际验证与失败历史

所有命令在候选目录运行，证据位于 `build/reports/reactor-administrator-03/`。退出码以Gradle实际结果/外部进程结果登记，不以PowerShell末尾Get-Content的退出码冒充。

| 证据 | 实际结果与范围 |
| --- | --- |
| 01-heat-fuel-red.log | 22项中4项预期行为失败：新生优先、缓存分配、非法输入、部分耗尽。编译壳提供可执行RED。 |
| 02-heat-green.log | 核热/Controller及原热工定向退出0；未保留该轮XML，不补推计数。 |
| 03-menu-supply-red.log | 11项中4项预期失败：草稿、布局、付热窗口；已有7项核热通过。 |
| 04/05-menu-supply-green.log | 实际是客户端Font/Layout API编译诊断失败；名字中的green不是通过证据。 |
| 06-menu-supply-green.log + 06-unit-xml | 18/18通过，退出0。 |
| 07/08-real-game.log | 4个真实代表先分别3失败/1失败；SPECIAL NBT误用String、ControlRod构造字段误设为完整度0，均属夹具诊断。原菜单业务已通过，不计为旧生产回归RED。 |
| 09-real-game.log | 4/4业务，全部维度保存并自然退出0；属于后续扩展前证据。 |
| 10-wire-red.log | 测试缺少Mode限定名的编译诊断，不算行为RED。 |
| 11-wire-red.log | 2项中1项真实codec失败：独立START/STOP/RESET没有draft，旧encode空引用。补固定宽度空占位后正常往返。 |
| 12-directed.log | 62项中1项旧“15字段”断言失败，新维护参数已16字段；只更新批准字段及正有限维护边界，原保护断言保持。 |
| 13-display-red.log | 3项中1项预期失败：下一tick已完成供热来源；补纯显示事实窗口，不放宽消费。 |
| 14/15-real-expanded.log | 扩展夹具误把同tick先前付款统计当成SPECIAL当前新收费、Create空笼默认无BE、以及NBT读入后未调用原热级更新；如实保留失败，修正夹具不修改SPECIAL生产规则。 |
| 16-real-final.log | 5/5业务后Saving worlds挂起；jstack在ChunkMap.processUnloads/CompletableFuture忙循环。采16-saving-threads及精确进程身份，只结束本隔离GameTest PID83928，最终Gradle1/子进程-1；不能称自然保存成功。 |
| 17-directed-final.log | 63/63定向退出0，范围是杆余热补充前源码；不冒称最终完整套。 |
| 18-rod-heat-red.log | 新杆缓存/混合分配/燃料冷却独立性：10项中3项真实失败。 |
| 19-rod-heat-green.log | 35项中1项夹具误将空燃料列当成事故危险覆盖；原状态机正确PAUSED，改为真实可用失效燃料后验证，不放宽状态机。 |
| 20-rod-heat-green.log | 35/35受影响热工通过，退出0；包含纯杆支付、混合缓存、不足额、机械字段不变、维护不暂停事故。 |
| 21-dedup-real-red.log | 真实SPECIAL先零维护提交后同tick改NORMAL再正式结算，扣了1HU但commit去重不出票，6代表扩展前5项中1失败；补请求检查committedAt。 |
| 22-real-final.log | 配置窄补齐前6/6业务：真实菜单/并发/会话复用/操作、自然耗尽与原端口补料和100tick健康复位、两顺序实际扣热与NORMAL时长、SPECIAL/创造/未任职/假KINDLED/不足额/失败/换成员、杆缓存真实付热及菜单总缓存。全部维度保存完成，自然exit0，Gradle24秒。 |
| 23-final-narrow.log + 23-unit-xml | 配置窄补齐前42/42相关热工、收据、草稿/codec/投影、布局，0失败/错误/跳过，自然exit0，20秒。配置边界纠错前的受影响窄回归，不推算最终全量套。 |
| 24-assemble.log | 首轮稳定后增量assemble，4秒exit0；原5e04包完整备份后写候选build/libs，main未动。 |

最终窄命令：

```powershell
.\gradlew.bat test --tests '*ReactorMaintenanceHeatTest' --tests '*ReactorServerTickTest' --tests '*ReactorThermalCalculatorTest' --tests '*ReactorMeltdownStateMachineTest' --tests '*ReactorScramFissionRegressionTest' --tests '*ReactorAdministratorHeatSupplyTest' --tests '*ReactorAdministratorCommandsTest' --tests '*ReactorAdministratorLayoutTest' --console=plain
.\gradlew.bat runGameTestServer -PgameTestNamespace=cni_reactor_administrator -PgameTestDirectory=build/reports/reactor-administrator-03/game --console=plain
.\gradlew.bat assemble --console=plain
```

真实两种顺序分别有1HU正式付款，核缓存20→19；重复owner/燃烧室同tick调用不二次收费/冻时，额外燃烧室结算恢复原生减一。纯receipt证明下一tick燃烧室消费前，已完成来源仍可显示nuclear且不能再次消费；真实下一tickProjection也读取nuclear，但本轮真实回调发生在当前燃烧室消费之后（日志currentConsumed=true），不冒称已验证真实玩家菜单早于BE的绘制时序。维护支付事务/身份检查仍保持严格窗口，源码来源读取路径与投影相同。

GameTest的100tick稳定复位在原正式tick推进，不用人为写healthyAge；日志复位后healthy=0属于新STOPPED状态重置。全耗尽补料后故障保持不自动恢复。小额维护不冒充冷却/暂停事故由正式热工纯函数代表证明，未运行旧长工况或用户世界。

冻结脚本首次git diff输出按Windows默认GBK解码失败；已用UTF-8重跑diff证据读取，不改源码、制品或重建。原失败工具输出保留在会话，artifact JSON及冻结ZIP实际核验成功。

## 配置窄补齐阶段冻结制品（29轮历史）

正式候选：`build/libs/create_nuclear_industry-0.1.0.jar`。

- SHA-256：`e1c19659fce04465535260665b575e337d31567d1b97170c9cbb6bb3919cb808`
- 大小：3182446 bytes。
- 完整原包：`build/reports/reactor-administrator-03/baseline-5e04.jar`，原5e04 SHA保持。
- 冻结候选：同目录 `final-candidate.jar`，与正式逐字节一致。
- `artifact-verification.json`冻结32个本批源/数据/构建文件、全部2054个src文件SHA、生产658 class及1542资源SHA；所有JAR载荷与实际编译输出/处理资源一致。
- `frozen-source.zip`保存32个写集文件，`source-diff.patch`为已跟踪源码差异；新文件在ZIP/源树/manifest中完整保存。
- 相对基线，非目录文件新增14、删除2、变化25；新测试class及administrator_empty.nbt没有泄漏。新域仅有两个空目录占位；原资源文件只改两种语言JSON。
- 新模板与原automation_empty逐字节一致；仅新增两条精确生产JAR排除，不改变依赖/既有域/构建流程。

## 精确写集

- `build.gradle`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationBehaviours.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationBlazeBridge.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationController.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationGoggles.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationHeatSupply.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationNetwork.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationParameters.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationService.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/client/ReactorAdministratorLayout.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/client/ReactorAdministratorScreen.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/client/ReactorAutomationClientEvents.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/gui/ReactorAdministratorCommands.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/gui/ReactorAdministratorMenu.java`
- `src/main/java/com/iksxh/create_nuclear_industry/automation/gui/ReactorAdministratorProjection.java`
- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`
- `src/main/java/com/iksxh/create_nuclear_industry/config/ReactorAutomationConfig.java`
- `src/main/java/com/iksxh/create_nuclear_industry/content/ReactorAutomationContent.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ReactorAdministratorGameTests.java`
- `src/main/java/com/iksxh/create_nuclear_industry/mixin/BlazeBurnerAutomationBlockMixin.java`
- `src/main/java/com/iksxh/create_nuclear_industry/mixin/BlazeBurnerAutomationEntityMixin.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorMaintenanceHeat.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorServerTick.java`
- `src/main/resources/assets/create_nuclear_industry/lang/en_us.json`
- `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`
- `src/main/resources/data/cni_reactor_administrator/structure/administrator_empty.nbt`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAdministratorCommandsTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAdministratorHeatSupplyTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAdministratorLayoutTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationControllerTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/reactor/ReactorMaintenanceHeatTest.java`

## 配置故障下固定停堆窄补齐

交审前自审发现：STOP分支前无条件解析管理员跨项配置；Projection在非法配置时丢失Generation，客户端不能发停堆。PM确认按原固定停堆合同在同卡补齐，不增功能/写集。

- STOP免运行参数解析，仍严格验证当前玩家菜单、nonce、实际成员/代际/sequence；APPLY/START/RESET完整解析配置，非法配置拒绝为invalid_config。
- 配置错误只保留真实身份/phase的最小故障投影，ConfigurationValid=false；客户端各页只保留查看与STOP，隐藏其他写控件并给出准确原因。配置恢复时重建控件，原未提交草稿/版本保持。
- 25-invalid-config-real-red.log取得真实6项中1项行为失败：合法单项warningCold=0.3违反跨项顺序时丢身份。仅修改隔离GameTest的运行配置值并finally恢复0.15，不写用户config。
- 25轮随后Saving worlds挂起，线程在ChunkMap.processUnloads/CompletableFuture，保存精确PID/线程后只停止本隔离进程。26-invalid-config-real-green.log曾在25进程未退出时误启动，世界锁导致0用例；Gradle虽然返回0，明确不是业务通过。未对用户世界操作，保留这次流程失误/启动诊断。
- 27-invalid-config-real-green.log：最终配置补齐后6/6业务通过，含非法配置身份保留、仅STOP可写与finally恢复；随后再现Saving worlds挂起。27-saving-threads.txt和27-process-identity.json保留，精确停止隔离进程，最终Gradle1/子进程-1。最终源码的自然保存退出未确认；22轮6/6自然exit0仅为配置窄补齐前证据。
- 28-config-final-narrow.log及28-unit-xml：4/4草稿/codec/投影预算/布局通过，17秒exit0。23轮42/42保留各自范围，不合并冒称最终全量或46项同轮。
- 29-correction-assemble.log：必要纠错后的1秒增量assemble exit0。原24-d372-candidate.jar/source.zip/artifact-verification.json/source-diff.patch均完整保留；最终正式/冻结包及32源/658class/1542资源重新冻结核验。

## 剩余门与交接

请PM沿既定一次组合窄审检查这份未提交候选。不要把本报告自动变成main集成或用户手测通过。待用户同一候选目录runClient集中复看：三页及14×14/最小窗口可读性、初次点燃与燃料/工具交互、完整设置与独立启停/复位、多玩家编辑、普通核热值班及备用燃料时长、停堆余热、部分/全列耗尽与自动补料、保护/红石、01R实际换接口成型。实际GUI/GPU、联网画面和鼠标体验尚未验收。

不以纯布局断言代替真实GUI，不以当前GameTest或原通过项目扩大其他设备/02授权。

## 同次组合审查集中整改（30～36轮，当前候选）

原审查者四项Important由本唯一执行者在原写集集中处理；PM仅批准一个逐字节模板别名，使两个新增启动窗口可独立执行。未改原六个真实业务，未重复已过热工/票据/配置STOP套，也未启动用户客户端/世界。

1. Commands在ARMING应用与活动requestedMode不同的模式时，撤销旧启动许可、全插并回STOPPED；新偏好仍原子保存，必须另行START。ARMING同模式仅保存设定，不误入运行中即时调棒路径。
2. 当前低沿已到但有预警时，Controller仍保留ARMING意图，使用原逐棒安全基准，无浅目标；真正冷却不足仍FAULT。连续预警期间不提棒，库存恢复后才进入已保存的MANUAL目标；原高输入超时与其他trip分支保持。
3. 保护页消费既有SignalHigh，显示实际高/低及当前阶段要求：STOPPED高启动基准、ARMING等待低、运行低、FAULT高保持。只重排原文字行，中英文同步，不改红石或投影来源。平均功率改为“窗口均值”，避免与可配置窗口矛盾。
4. 普通seq0刷新不再init/clearWidgets，新投影在原EditBox上更新未编辑字段。鼠标或键盘获得输入焦点即beginDraft并冻结原revision/epoch，后续普通更新不触碰dirty草稿、光标或选区；模式按钮也原位换标签。页面/配置/尺寸或成功APPLY确需重建时先清旧输入框和父焦点，避免旧不可见对象继续接键。失败APPLY仍保留草稿和原版本。

| 原始证据 | 实际范围与退出 |
| --- | --- |
| 30-review-arming-warning-red.log | 1项Controller真实计算器预警RED，浅40%目标确可冷却后，expected ARMING but was MANUAL；Gradle1，16秒。 |
| 31-startup-real-red.log | 新startup域两个真实菜单/反相线场景均到指定业务失败：ARMING改模式未撤权；实际低输入后预警提前手动。业务2/2失败。随后Saving worlds挂起，保留31-saving-process.json及31-saving-threads.txt后只结束明确隔离PID82808；Gradle1/Java-1，不能称自然退出。线程仍为ChunkMap.processUnloads/CompletableFuture，358～373行，没有新增管理员调用帧。 |
| 32-review-directed-green.log / 32-unit-xml | 当前Controller18＋Commands3，共21/21，零失败/错误/跳过，21秒exit0。包括原高输入超时和保护回归，不与此前42、4等阶段合并冒称最终全套。 |
| 33-startup-real-green.log | 仅新增两个启动窗口2/2通过，20秒自然exit0，三维度保存完成。模式交接保存CONSTANT偏好并保持STOPPED；预警11/12/13tick实际低输入时仍ARMING且真实各棒实际/目标100%，14tick补冷，18tick恢复MANUAL40%。这证明本轮两个窗口的业务与保存，不追认此前六业务最终保存或用户卸载/重进通过。 |
| 34-native-focus-api.txt / 34-screen-bytecode.txt | 锁定MC1.21.1真实API和当前编译Screen调用链；界面焦点整改的代码证据，不是GPU/实际玩家键盘鼠标验收。 |
| 35-review-correction-assemble.log | 本轮唯一必要纠错增量assemble，1秒exit0。 |
| 36-freeze-review.py / 36-freeze-review-result.json | 当前源、编译类、处理资源、正式/冻结JAR与模板同字节检查；两管理员测试域无class/NBT载荷泄漏。 |

本轮实际命令：

```powershell
.\gradlew.bat test --tests '*ReactorAutomationControllerTest.armingWarningHoldsEachTickAndKeepsManualIntentUntilWarningClears' --console=plain
.\gradlew.bat runGameTestServer -PgameTestNamespace=cni_reactor_administrator_startup -PgameTestDirectory=build/reports/reactor-administrator-03/startup-game --console=plain
.\gradlew.bat test --tests '*ReactorAutomationControllerTest' --tests '*ReactorAdministratorCommandsTest' --console=plain
.\gradlew.bat assemble --console=plain
```

输入焦点证据按真实链界定：34-native-focus-api的ContainerEventHandler.mouseClicked第30～56行，成功子控件调用父setFocused；ComponentPath.Path.applyFocus第678～697行，键盘焦点路径同样调用父setFocused。当前Screen字节码第558～576行覆盖该方法并冻结草稿；第577～674行原位刷新在dirty时直接返回，其他字段只有未聚焦且值确变时调用setValue；第864～914行显示普通seq0无重建分支。原editbox-api证明setValue会重置光标/选区，当前聚焦对象不调用它也不被替换。init第94～116行明确清旧焦点后才重建。这里只确认调用链和对象/版本保持，字体、选区视觉、14×14点击和真实网络输入仍留人工门，没有新增源码文字镜像测试。

### 当前冻结与精确增量

- 正式及final-candidate.jar SHA-256：`879c3d72bf5a80f9cf93263abe847d8e0354d22499f7312bc1a9e661925506fb`，3183722 bytes。
- 原e1c完整保存为review-e1c-candidate.jar、review-e1c-source.zip、review-e1c-artifact-verification.json及review-e1c-source-diff.patch；5e04/d372与全部历史失败/线程证据保持。
- 当前artifact-verification.json/frozen-source.zip/source-diff.patch冻结33个写集文件、全部2055个src SHA、658个生产class与1542资源。正式/冻结包相同，各class/资源与实际编译/处理输出逐项相同。
- 相对e1c，非目录文件无新增/删除，仅Controller、Commands、Screen三class及两语言JSON变化。相对5e04的新增14/删除2/变化25口径保持。
- 两管理员域无测试class或NBT文件泄漏；JAR含四个空目录占位（administrator及startup各两），不是正式模板载荷。两模板均与原automation_empty逐字节相同。
- 在上列32文件之外，仅新增已批准`src/main/resources/data/cni_reactor_administrator_startup/structure/startup_empty.nbt`。本轮实际修改build.gradle、Controller、Commands、Screen、GameTests的StartupWindows嵌套holder、ControllerTest和两语言键；其余本批源保持e1c。全部src范围没有写集外漂移；源码/build.gradle diff --check通过，原用户logs和PM卡dirty不清理。
- 冻结脚本首次在Windows的未关闭ZIP writer上读取进行校验，触发BadZipFile（目录与头部斜杠表现不同）；改为关闭后重新打开ZIP校验，再完成冻结。该工具失败见实际会话输出，未修改生产源码或再次构建，不当成业务失败。

当前组合源码/制品/报告稳定，仅交原审查者同次窄复核。本轮2/2自然退出不关闭01/01R或03的客户端、保存/卸载/重进门；main及02保持未推进。
