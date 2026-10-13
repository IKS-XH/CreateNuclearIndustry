# REACTOR-ADMINISTRATOR-03 独立组合审查

2026-10-14。本批原独立审查者一次规格与质量窄审；未转派、未运行测试/Gradle/客户端/服务器，未修改生产、测试、资源、配置、世界或 Git。唯一写入为本报告。

审查对象为 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation` 的未提交组合候选：文档 HEAD `6fcf9c0`，生产差异相对 `2dff787`，并读取全部相关新增路径。已读本树 AGENTS、冻结 GUI 设计、03 活动卡及执行报告；实际采用 minecraft-modding、minecraft-testing、requesting-code-review/code-reviewer 和 verification-before-completion。验证技能的一般重跑示例由本批明确的只读、复用原始证据边界覆盖。

## 结论

**当前有四项 Important，需同批整改后由本席窄复核，再交集中客户端复看。** 没有 Critical；不判断可以直接合 main。客户端 GUI/GPU、14×14 实际字体与鼠标、联网显示、保存/卸载/重进仍未验收，01/01R 未通过项目保持，02 不派发。

## 已核实的实现与技术取舍

- 当前玩家菜单、随机 session UUID、实际 owner/接口/燃烧室对象、结构代际、epoch、设置 revision 和递增 sequence 共同校验。STOP 仅容忍当前有效会话的设置版本过期；旧 nonce、实例或关闭菜单不能借此获得权限。`ReactorAdministratorMenu.java:62–87`、`ReactorAdministratorCommands.java:27–46`。
- APPLY 校验完整草稿后才提交；START、STOP、RESET 有独立分支。配置非法保留真实身份的最小投影，STOP 不解析运行参数且不清 FAULT；非 STOP 动作继续拒绝。`ReactorAdministratorCommands.java:42–46,88–101`、`ReactorAdministratorProjection.java:47–58`。下面两项 ARMING 问题是此交接中遗漏的窗口，不能被正常运行分支的检查覆盖。
- 公共 Menu/Projection 不引用 Minecraft 客户端类；屏幕注册在 Dist.CLIENT 的 MOD 事件订阅者，服务端菜单注册使用 IMenuTypeExtension。供热热级依靠服务端真实燃烧室 tick 和原块状态同步，客户端不授予许可。`ReactorAutomationContent.java:24–32`、`ReactorAutomationClientEvents.java:14–25`、`BlazeBurnerAutomationEntityMixin.java:22–39`。
- 旧四控件不再加入 Create behaviour，旧专用写入口始终拒绝；保留旧开发夹具签名而不删除既有测试域是合理取舍。客户端旧 prepare/send 也不发送写包。`ReactorAutomationBehaviours.java:26–45`、`ReactorAutomationService.java:167–169`、`ReactorAutomationClientEvents.java:27–29`。
- 维护先支出真实新生热，再按燃料与控制棒真实缓存总余额比例分摊，确定性尾差留给最后正余额列；不足整笔为零。燃料维护进入原 Thermal 移热图，杆维护只扣杆缓存。冷却预算仅减燃料维护支出，杆机械字段保留，融毁暂停仍仅由实际冷却移热布尔决定。`ReactorMaintenanceHeat.java:23–63`、`ReactorServerTick.java:97–159`。零请求保留原三参行为，毛裂变及燃耗未改。
- Instrument 完成原燃料端口/快照提交、原事件与后续正式阶段后才出票；已知失败撤票。请求、提交、付款、消费分别去重，clear 撤票但保留本 owner 同 tick 去重时刻。相邻 tick 容忍仅服务两种 BE 顺序；完成供能来源文字不授权再消费。`ReactorInstrumentPortBlockEntity.java:719–805`、`ReactorAutomationHeatSupply.java:19–34,62–83,90–115`。
- NORMAL 仅跳过已实际消费票据的单次减一，Create 整段 tick 仍运行，SPECIAL 到期 NORMAL5000 不被拦截；普通未任职/创造/SPECIAL 分支保留原路径。`BlazeBurnerAutomationEntityMixin.java:22–39`，已对照锁定依赖的 native-burner/native-block 证据。部分耗尽与原端口补料不再因可用数下降撤权，全耗尽仍 FAULT，补料不自动恢复。`ReactorAutomationService.java:79–93`、`ReactorAutomationController.java:80–93`。
- 显示投影不发送物品 NBT/完整核快照，限制 196 格、根/列字段、有限数与字符串，并复制持有的数据。最大网格点击只选择信息，各页固定 STOP 按钮。静态布局检查不能证明真实字体、GPU 和鼠标体验。

## 必改发现

### Important 1：ARMING 期间切换模式未执行安全交接

- **位置：** `src/main/java/com/iksxh/create_nuclear_industry/automation/gui/ReactorAdministratorCommands.java:55–70`；后续消费为 `automation/ReactorAutomationController.java:119–125`。
- **触发：** 当前有效菜单 START 后处于 ARMING、真实火把尚未完成降沿的数 tick 内，再 APPLY 另一模式。该窗口可由同一玩家或另一持有最新版本的玩家操作，不需要伪造请求。
- **问题与影响：** APPLY 的 `running` 只包括 MANUAL/CONSTANT。ARMING 被当成未运行，只更新 Settings.mode，仍保留旧 state.requestedMode 和 runPermission=true。降沿到达后 Controller 按旧 requestedMode 进入旧分支，例如 GUI 确认恒定 HU，实际进入 MANUAL 并提交手动目标；转换没有撤权/全插入安全交接或独立重新 START。
- **最小修法：** 在现有 Commands 的 APPLY 内，将 ARMING 中活动请求模式改变也纳入原全插入撤权交接，保留新偏好但回到 STOPPED，要求独立 START。补当前真实菜单在此窗口 APPLY 的行为证明；不要新增模式或另一权威。

### Important 2：ARMING 转手动运行绕过当前预警禁提棒

- **位置：** `src/main/java/com/iksxh/create_nuclear_industry/automation/ReactorAutomationController.java:114–124`；预警处理目前位于 `129–140`。
- **触发：** START 时冷液达到启动阈值，等待火把降沿期间外部运输使冷液降到预警阈值以下，但剩余量仍足够真实下一 tick 冷却。保存的 MANUAL 目标比当前全插入基准浅，signal 转低。
- **问题与影响：** ARMING 分支仅查 trip 和 coolantCanCarryNextTick，随后直接返回浅目标；不会执行下面的 warning/逐棒钳位。Service 在正式棒推进前提交该目标，因此预警期间先实际提棒一次，下一次 MANUAL 决策才阻止后续提棒。冷却预算通过不等价于预警解除，违反沿用的每 tick 禁提棒合同。
- **最小修法：** 手动交接也检查当前 warning，并使用原逐棒安全基准，保证预警未解除时不提交浅目标。保留已确认设置与独立启动语义；补“启动等待期间进入预警且拟目标可冷却”的定向证明，不能只测试缺冷 FAULT 或常规 MANUAL 预警。

### Important 3：保护页缺少冻结规格要求的真实红石联锁检查

- **位置：** `src/main/java/com/iksxh/create_nuclear_industry/automation/client/ReactorAdministratorScreen.java:133–140`；已有投影字段在 `automation/gui/ReactorAdministratorProjection.java:82`。
- **触发：** 用户未连接反相线路、断线，或 ARMING 的真实仪表输入没有按期转低。其他供能/覆盖/冷却项可以正常。
- **问题与影响：** 服务端发送 SignalHigh，但屏幕未读取或显示。START 因 `Commands.java:83` 的联锁条件拒绝后，界面只提示“请查看保护页”，该页却没有仪表实际输入/预期状态，用户无法区分安全线未接、降沿未到与其余启动条件。冻结设计明确列出红石联锁检查，这不是 GPU 可读性待手测的例外。
- **最小修法：** 在现有 Screen 和中英文键内显示仪表真实输入高/低与当前阶段预期：停堆启动基准要求高，ARMING 等待低，运行要求低。勿将“有信号”一律显示为通过，也不修改原红石/火把算法。

### Important 4：普通显示更新重建控件，破坏真实输入框焦点及内容

- **位置：** `src/main/java/com/iksxh/create_nuclear_industry/automation/client/ReactorAdministratorScreen.java:94–99`，重建为 `37,46–52`，提交读取为 `73,81–84`。
- **触发：** 用户在总览点入输入框、移动光标或全选，尚未改变字符，因此 dirty=false；下一份正常 seq0 投影到达，例如外部燃料剩余时长每 10 tick 更新。
- **问题与影响：** receive 调 init，clearWidgets 后新建两只 EditBox。锁定 MC 的 Screen.clearWidgets 仅清 renderables/children/narratables，不清 focused；旧输入框仍能接键却已不绘制。用户随后首次键入会落到旧对象，显示的新框未改；send 的 saveText 又从新框覆盖文本，造成不可见或丢失的输入。即使清掉旧焦点，也会每约半秒打断尚未输入的编辑准备。
- **最小修法：** 普通数值投影刷新原位更新，不重建正在使用的输入控件，保留焦点、光标及选区。页面或配置有效性变化确需重建时显式清理/转移焦点，且不升级已冻结草稿版本。静态调用链已足以确定该缺陷；真实鼠标/键盘和字体复看仍是后续客户端门。

## Minor

`src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json:600` 及对应 en_us 的 gui.power 将均值写死为“40tick”，但实际平均窗口仍消费可配置 displayAverageWindowTicks。用户改该参数后单位说明错误。可在现有两语言键改为配置无关“窗口均值”，或显示实际窗口；不需要增加镜像文字测试。

## 原始证据与保存挂起边界

本席读取原始日志、冻结 XML、执行报告、源差异/新增夹具和制品清单；没有重跑任何验证。各阶段结果保持各自源码范围：

| 阶段证据 | 可采用的结论 |
| --- | --- |
| 17-directed-final.log | 63/63，杆缓存补齐之前的定向结果。 |
| 20-rod-heat-green.log | 35/35，杆/混合缓存、燃料冷却独立性、机械字段及事故冷却代表。 |
| 22-real-final.log | 配置纠错前真实 6/6；三维度保存完成，自然 exit0，24 秒。不是最终配置纠错源码的退出证明。 |
| 23-final-narrow.log / 23-unit-xml | 配置纠错前 42/42；不与后续计数合并冒称最终全量。 |
| 26-invalid-config-real-green.log | 世界锁，零用例；即使 Gradle0 也不是业务通过。 |
| 27-invalid-config-real-green.log | 最终配置纠错后的真实 6/6 业务成立，之后 Saving worlds 挂起，精确终止隔离进程，Gradle1/Java-1。最终自然保存未确认。 |
| 28-config-final-narrow.log / 28-unit-xml | 配置纠错后的窄 4/4，17 秒 exit0。 |
| 29-correction-assemble.log | 纠错增量 assemble，1 秒 exit0。 |

夹具使用原生 ServerPlayer.openMenu，仅借 FakePlayer 的隔离无网络连接；真实空手 useItemOn、nonce 与循环 containerId、并发旧稿、关闭/越距/换成员均走正式命令。两种 BE 顺序、原 NORMAL/SPECIAL5000、不足整笔、失联不出票、纯杆支付和部分/全耗尽走正式核 tick/原端口。临时跨项配置在 finally 恢复，不写实例配置。其业务证明不包含真实客户端渲染，下一 tick 菜单来源的真实回调也不冒称发生在 BE 之前。

27-saving-threads.txt:386–400 的 Server thread 为 RUNNABLE，位于 CompletableFuture 完成链与 ChunkMap.processUnloads/stopServer，未出现管理员算法帧。03 对已有 Instrument 卸载/失效入口没有新增区块装载逻辑；新增清票及 NBT 撤票为内存字段清理，不持久化票据。现有静态证据不足以把保存挂起归因为本批算法，也不足以宣布最终保存/卸载/重进通过。不据此改事故/存档合同或要求机械全量测试；实际保存与卸载仍保留集中客户端门。

制品清单读取为最终 `e1c19659fce04465535260665b575e337d31567d1b97170c9cbb6bb3919cb808`、3182446 bytes，32 源、658 生产 class、1542 资源。逐项源/编译输出/JAR 哈希零漂移复用 PM 本批独立核查，不重复全清单；正式/冻结一致，baseline5e04 与 24-d372 保留。相对基线为非目录载荷新增 14、删除 2、变化 25；新测试 class/NBT 泄漏为零，两个新域空目录不当成正式模板。build.gradle 仅精确排除本批 GameTest 和模板别名，旧域不清理。根日志/PM 文档 dirty 不纳入功能源码写集。

## Declined to judge

空。没有把合理使用者行为因规格未列触发而搁置。客户端人工门、02 未授权实现、main 合入及旧存档/全环境兼容分别依照任务边界保留，不借此次静态和阶段证据追认。

## 同批接续

四项 Important 汇总交回原唯一执行者一次整改，本席只对实际差异和相应定向证据做同一次窄复核，不逐任务重新审查/重跑旧域。Minor 单独列出，不抬高为安全阻断。整改关闭后才可给出“可交本批集中客户端复看”的结论。

## 同一次审查的整改差异窄复核（当前结论）

2026-10-14。前文四项及 e1c 制品/阶段结果是初审历史，完整保留。本席只读取本轮实际差异、执行报告整改尾部、30～36 原始证据及必要依赖 API；没有重审整包、转派、执行测试/构建/客户端或写 Git。当前正式/冻结候选为 `879c3d72bf5a80f9cf93263abe847d8e0354d22499f7312bc1a9e661925506fb`，3183722 bytes。

**四项 Important 和一项 Minor 均已关闭，无剩余必改，可交本批集中客户端复看。** 此结论只表示已审组合候选可交付人工门；不表示 GUI/GPU、14×14 真实可读性、鼠标/联网操作、用户保存/卸载/重进、01/01R 未验项目通过，也不授权合 main 或派发02。

### 逐项关闭依据

| 原发现 | 当前源码与对应证据 | 裁定 |
| --- | --- | --- |
| Important 1：ARMING 改模式 | `ReactorAdministratorCommands.java:55–59` 比较 ARMING 的活动 requestedMode，改变时先形成 STOPPED/全插/撤权状态；完整草稿仍在原检查后的统一提交区写入，通知红石保持最后一步。`ReactorAdministratorGameTests.java:60–75` 真实菜单 START 后同窗口 APPLY CONSTANT，立即与14tick后均断言 STOPPED/许可false，新偏好保持，棒目标100%。31 为行为RED，33 为GREEN。 | 关闭。新偏好不会再复用旧启动意图，同模式保存不误入运行中即时调棒分支。 |
| Important 2：ARMING 预警提棒 | `ReactorAutomationController.java:120–128` 在手动交接前检查 warning；真实安全基准不可冷却仍FAULT，可冷却的预警则保持 ARMING/requestedMode、无浅目标并逐棒阻止提升。保留意图等待预警解除，随后才提交确认目标，避免进入MANUAL后丢掉一次性全堆意图。Controller新增断言明确先证明40%浅目标可冷却，再连续五次检查ARMING/无深度命令/安全钳位，恢复库存后进入MANUAL40%。真实 `GameTests.java:78–99` 经过正式tick和真实反相线路，11/12/13tick实际低输入时仍全插，14tick补冷、18tick恢复实际/目标40%；30/31 RED、32/33 GREEN。 | 关闭。采用“保留已授权ARMING意图、全插等待预警解除”的最小取舍；START仍是显式授权，STOP/FAULT不会借此自动重启，原高输入超时/其他trip分支保持。 |
| Important 3：红石联锁显示 | `ReactorAdministratorScreen.java:153–162` 消费原 SignalHigh，显示真实高/低及 STOPPED 高启动基准、ARMING 等待低、MANUAL/CONSTANT 运行低、FAULT 高保持；两语言键在 `lang/zh_cn.json:614–620` 与 en_us 同步。原红石算法/投影未改，固定STOP区域保持。 | 关闭。静态信息链已完整，实际文字截断/最小窗口仍留人工门。 |
| Important 4：普通刷新破坏编辑 | `ReactorAdministratorScreen.java:77–91,114–119` 在实际父 setFocused 获得鼠标/键盘输入焦点时固定草稿版本；普通seq0只原位刷新，dirty直接返回，未聚焦字段也仅在值改变时setValue。模式按钮原位改标签。确需重建的 `38–41` 先撤销旧输入框和父焦点；失败APPLY仍保留草稿，成功才清理。34-native-focus-api证明 ContainerEventHandler.mouseClicked 与 ComponentPath.Path.applyFocus 均调用父 setFocused；34-screen-bytecode 的实际编译调用链对应上述源码，seq0无init重建。 | 关闭。焦点获得即冻结版本是合理技术取舍，避免选择/移动光标的准备期间被另一玩家新投影升级。普通更新不替换对象或重置选区。真实键鼠、选区画面仍未手测。 |
| Minor：40tick硬文案 | `lang/zh_cn.json:600` 和 en_us 对应键改为“窗口均值/window avg”。平均窗口的真实配置及计算保持。 | 关闭，无需文字镜像测试。 |

### 本轮证据范围与精确增量

- **30-review-arming-warning-red.log：** 1项实际 Controller 行为失败，预警且浅目标可冷却时原代码进入MANUAL；16秒Gradle1。不是编译或夹具失效冒充RED。
- **31-startup-real-red.log：** 两个真实启动窗口都在指定业务断言失败；随后Saving worlds挂起，保留身份/线程后终止隔离进程，Gradle1/Java-1。`31-saving-threads.txt:358–370` 仍是 CompletableFuture/ChunkMap.processUnloads/stopServer，无管理员帧；不据此追加无证据的生产归因。
- **32-review-directed-green.log / 32-unit-xml：** Controller18＋Commands3，共21/21，0失败/错误/跳过，21秒exit0。采用本轮受影响范围，不与原42、4等合并冒称最终全量。
- **33-startup-real-green.log：** 只两个新增窗口2/2，20秒自然exit0；原始日志71～77行记录三维度全部保存。该结果证明本轮两个窗口与本轮保存完成，不追认27轮原六业务的自然退出或用户世界保存/卸载/重进。
- **34原生focus/Screen字节码：** 只用于核实际API、对象与草稿版本保持；没有把字节码或静态布局变成GPU/真实联网键鼠验收。
- **35-review-correction-assemble.log：** 本轮必要纠错增量assemble，1秒exit0。未改的维护热工、票据、部分/全部耗尽和非法配置STOP等复用初审所列各阶段证据，未机械重复原六业务。

PM精确扩卡仅新增 `src/main/resources/data/cni_reactor_administrator_startup/structure/startup_empty.nbt`，以及原 GameTests 中独立 namespace 的 StartupWindows 嵌套holder，原因是锁定框架只有namespace过滤。实际新模板111 bytes，与原automation_empty及administrator_empty的SHA-256均为 `71343a478b844ae6b0044f0f2142fae309499d727cfc731d6e5bbf9f6bc104e4`。本席对照保存的review-e1c源ZIP，去除新增StartupWindows块后原六代表的整份正文保持；新窗口复用原正式菜单/反相线，未伪写红石输入、状态phase或健康年龄。

`build.gradle:141–145` 的原 GameTests 通配排除覆盖新增嵌套class，仅增加startup模板的精确排除；旧域、生产依赖/流程不改。读取36冻结结果并复用PM本批独立逐项身份核查：33来源、658生产class、1542资源在实际编译/处理输出与正式JAR零漂移，正式/冻结一致，两管理员域class/NBT载荷泄漏0。相对e1c无新增/删除文件载荷，只有Controller、Commands、Screen三生产class和两语言JSON变化。四个新域空目录占位与模板载荷区分，原e1c/5e04/d372和全部失败证据保留；main包/用户世界/实例配置/候选HEAD及暂存区未由本席写入。

Declined to judge仍为空。本次差异未产生新的必改项；保存挂起历史与集中客户端门保持初审边界，不扩旧存档或全部环境兼容结论。
