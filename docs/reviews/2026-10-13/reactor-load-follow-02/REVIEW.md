# REACTOR-AUTOMATION-02 组合规格与质量审查

2026-10-14，原独立审查者 `/root/reactor_automation_review`，本批唯一一次组合审查。只读当前02候选、基线差异、新路径、原始证据及冻结制品，仅写本REVIEW；未运行测试、Gradle、客户端、服务器，未写实现、Git或治理文档，未转派。

## 范围与输入

目录 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-load-follow`，开工HEAD `3647fcc821d3386db008c73faf8cd04c9872bf1e`。已读当前AGENTS、02精确计划、02设计、DISPATCH、稳定IMPLEMENTATION及待验CLIENT-CHECKLIST；本轮复用已实际读取的minecraft-modding/testing、requesting-code-review审查模板、verification-before-completion要求。技术栈保持MC1.21.1／Java21／NeoForge21.1.219／Create6.0.10-280。03／04及承接01／01R已经联合验收并由06合main，本次没有重开前置。

实际检查新增PI／样本／参数、当前表绑定与网权、会话生命周期、原仪表/控制器/首核付款接入、GUI完整草稿和原权限、语言及精确排除。28条当前来源逐项与 `22-freeze/source-manifest.json` 和 `sources.zip` 相同；IMPLEMENTATION实际SHA-256 `977ea1cf08b62a2ed61d8664f647bd4f37ee22fa3e9a5c0959e22745d1484d2e`。以下发现均来自这一稳定源，不把测试GREEN当作其未覆盖输入的免责。

## 已符合的边界与技术裁定

- 正最低功率使用独立101档最近正档选择，不复用旧恒功率“不得超过目标”的零档策略；相同距离取较深棒，范围来自真实曲线及服务端上限，无合法正档拒绝。旧恒功率目标0仍停堆，LOAD模式无关恒功率草稿0不造成停堆。
- PI单位为毛HU/t，真实tick差除20进入秒，40tick指数滤波，原始负余量保留、计算误差限幅；死区保持实际选档目标，上下限及执行器限速阻止同向积分，反向可恢复。当前预警接入的时间问题单列Important 1。
- 原生采样只读已加载同维度StressGaugeBlockEntity的容量／负载及network字段，不靠getSpeed判断有效，不调用getOrCreateNetwork或强载。当前样本有限值及派生比例有护栏，C0没有百分比。表同坐标换实例及已建立容量后失容量/换网会FAULT并清会话；C0已知网的互斥缺口单列Important 2。
- 两步意图由服务器当前游戏线程重核主手真实扳手、目标可达性、加载/维度、1200tick寿命、玩家序列、同一表实例与管理员generation/epoch；仅STOPPED且无pending续接允许改绑，绑定推进设置revision与epoch。GUI仍要求当前玩家菜单、随机nonce、实际实例和序列；APPLY一次验证五字段，旧世界HEAT/DEPTH入口不复活。STOP保持当前真实菜单身份下无配置/旧显示版本阻碍，清pending与闭环；新增隐藏草稿缓存问题单列Important 3。
- 04正常保存意图只追加余量/最低毛HU偏好，不保存PI、样本、表引用、网权或付款票据。首核预算/真实低沿/足额付款保持原顺序且没有C前置；付款后才建立新当前会话。正常LOAD的C0有限loading-rebind保棒位、真实核费及每tick保护，无PI或新预热；有效容量才初始化PI。null未知网有限等待不制造网络，不能为修互斥而改变该首付合同。
- 库存/逐列有效积热与下一tick真实冷却需求先保护，最低功率不能覆盖FAULT或全插棒；正常仍10tick、提出1/插入5百分点，原逐棒、热工与核热付费未重构。新增手写职责/单位/两侧边界使用中文，玩家文案区分毛HU、容量等待、失联与机组启动门槛，不承诺默认2HU可启动大型汽轮机。

## Findings

### Critical

无。

### Important 1：预警解除后可能补算整段保护时间的提棒积分

位置：`automation/ReactorAutomationService.java:223`～`:225`；`automation/ReactorStressPiController.java:40`、`:49`、`:53`～`:55`、`:64`。

库存/积热预警期间Service只调用trackApplied并返回。trackApplied保留previousTick和filteredReserve，因此解除预警后的step用距最后正常调节的全部elapsed更新积分。当保护期间误差由负变正时，sameLimitedDirection为false；只要新请求未达HU上限，整个保护时段会被补算为正积分。比如Hmax1000、I100、旧余量.3、目标.2，预警10秒后余量变-.5，现算法首次解除可新增约69HU积分，而保护期间未许可该提棒需求。该例是源码公式推导，不冒称已运行新测试。

影响：保护解除时携带并未执行过的积累需求，破坏“库存保护不累积提棒需求”的PI时间边界，可能增加恢复后的过冲及调节时间。原限速与冷却验证仍在，不能把此问题夸大为直接绕过全部保护。

最小整改：只在现PI／Session／预警接入范围明确冻结或重初始化策略，保护期间推进同龄时间，恢复只计算下一正常许可周期；过滤与实际输出跟踪保持明确，不能补算保护窗口。补一个保护期间误差跨零、解除后积分不计保护时长的定向代表，不重跑原整套。

### Important 2：C0且已有真实网ID时没有同网互斥

位置：`automation/ReactorAutomationService.java:192`、`:214`；`automation/ReactorLoadFollowSession.java:29`～`:30`。

START只在样本VALID时检查available，会话只在established后claim。已加载表C=0但networkId非空时，NO_CAPACITY直接返回而不记录该已知身份；两台管理员可连续START并同时LOAD_WARMUP，各自升到最低功率，整个C0期没有同网持权登记。直到首次C>0，后处理的一台才FAULT。等待期间此已知网身份变化也没有被固定/保护。

影响：已知同一动力网允许两位同时持控制意图和预热，违反运行前拒绝第二位及同网唯一合同。此发现只针对原生当前非空networkId，不要求推测null网络、强载或搭两个完整热力机组。

最小整改：在现Session／Service／Registry范围对当前已知非空网ID于ARMING、C0预热及加载等待建立互斥和清理，第二位START预检拒绝，等待期间已知身份换网须保护。保留null/未知网有限等待、04真实首付先行及C0不执行PI；正常LOAD重进不能为了提前持权获得免费许可。补同一已加载非空networkId/C0的两个正式START入口代表即可。

### Important 3：同模式seq0未同步隐藏草稿，最新版本可覆盖他人新偏好

位置：`automation/client/ReactorAdministratorScreen.java:30`～`:31`、`:88`～`:97`、`:113`。

未dirty且确认模式不变的seq0只刷新可见两个EditBox，没有同步另一模式的两份缓存字符串。真实操作：A在MANUAL且未编辑；B在GUI预览LOAD、改余量/最低值后切回MANUAL并APPLY，仍是同MANUAL模式但四份偏好已有新值。A收到最新revision/epoch后，首次编辑棒深会冻结最新版本，却从旧reserveText/minimumText构造完整Draft；服务器正常接受并覆盖B刚保存的隐藏偏好。LOAD页面可反向覆盖隐藏手动/恒功率偏好。这不是旧版本被服务器放行，而是客户端用新版本提交了未同步旧值。

影响：玩家只改当前模式字段，却静默丢失另一玩家或另一模式的设置；完整五字段原子提交与多人版本合同未覆盖此缓存路径。

最小整改：仅Screen中未dirty的确认投影同步全部四数值缓存，再原位更新可见框；保留焦点/光标/选区，不重建普通seq0，不给dirty草稿升级版本。补一个同模式外部更新隐藏字段后首次编辑/APPLY的定向边界即可。

### Minor

无。

## 原始证据与范围

已读Task1纯PI及四真实场景实现、原始模型摘要、20最终XML、18自然保存日志及21assemble日志。Task1以SU/HU比例10、热惯性5秒、额外0/5/20秒时滞和1/5百分点限速的有限模型整定：Kp保持.25，Ki由.005调整到.01；三模型摘要末段余量约.178、换向均4次，不将其说成完整热力机组稳定保证。数学秒数单测仍用.005验证单位。初值未通过及误名03-green实际RED均按原报告保留。

06原29/29、11原36/36及16原18/18按各自阶段复用。本次实际20冻结XML五类为3＋3＋3＋2＋11＝22项，失败/错误/跳过均0；18真实4/4，三维度自然保存完成、24秒exit0。17真实2/4失败保留；末两断言在已去重同tick读取，被移至下一真实tick后通过，生产去重未改。没有把这些阶段拼成一次新全套，也没有用原生受控updateFromNetwork证明整条锅炉—汽轮机闭环。

21唯一assemble实际2秒exit0、墙钟2.5598538秒，只jar执行，编译/资源任务UP-TO-DATE，无clean/test/full build。正式与22冻结包实哈一致：3237666B／SHA-256 `652aa431758d23e7cc1b18c08ba6396acda8a06cfc26668f5ce897cfc55203d3`。

## 精确来源、载荷与保护

28路径均在02卡允许范围；build.gradle只加本批GameTest通配和本域NBT排除。实际逐项读取2226个正式文件：677class＋1548资源＋1生成Manifest，全部与实际输出、冻结manifest一致，无重复/缺失或排除文件泄漏；旧管理员排除保持。234个ZIP目录中14个真正空目录单列，本批仅两个空namespace/structure占位，不当作模板文件。原错误目录元数据副本保留，正式metadata已纠正。

相对50d5新增16class、没有删除；26既有class与两语言实质改变，另外1217个既有文本仅CRLF/LF，实际规范化对照逐字相同。两语言旧654键逐值不变，各新增25键至679；没有借换行变化新增素材/玩法。保护清单记录携带世界38文件及配置27文件零差异；原包及main/原管理员候选保留。本次没有重哈全部用户存档，也没有操作用户世界。E盘Gradle/tmp仅本域进程路由，12/13磁盘环境失败不能计业务RED或借此清盘。

## Declined to judge

- 完整实际热—汽—动力闭环的趋稳、启动门槛和长时滞：有限模型与原生受控输入不能代替，本批集中手测仍待验。
- 新GUI真实鼠标、字体/GPU可读性：仅审输入/布局逻辑，未启动客户端；原管理员已验门不重开。
- 真正ChunkManager卸载及独立进程restart：现真实代表仅当前NBT/BE重建和原生联锁，不扩其证明范围，也不新增旧存档专项。
- 05GPU事故归因、压力/D项/其他设备验收：本批没有对应实现，不用02来源或GREEN追认这些结果。

## 结论与同批整改

**暂不可交本批手测：三项Important须集中整改后关闭。** 当前来源/制品身份和已有自动证据成立，但不覆盖上述真实保护时间、已知C0网权和隐藏草稿输入。交原唯一执行者在现02写集一次收敛，仅实际修改范围补定向证据；生产改动后做一轮必要重包，保留22初包及全部失败。随后在本REVIEW同次追加差异关闭与新制品证据，不重开完整01／03／04审查、不重复原用户验收。

## 同次差异复核：三项关闭

2026-10-14，接续本次组合审查。已读02卡集中整改的精确许可、IMPLEMENTATION追加段、23／24 RED、25／26 GREEN、27实际assemble及28-freeze-review。本次仅对照22冻结初审源与整改差异、三条正式消费链和新制品；未运行测试、构建或客户端，未重审旧管理员域。原三项发现及初审结论作为历史保留，以本节最终结论为当前状态。

1. **Important 1关闭。** `ReactorAutomationService.java:225` 的真实预警分支每tick调用 `ReactorStressPiController.trackProtected`。`:67` 起明确保持integral、更新同龄filteredReserve与previousTick并跟踪已提交档位，恢复step只看到最后保护tick之后的正常时间，不补算保护窗口。新增跨零用例连续200tick保护、积分保持100，解除后只计10tick正常周期；原限幅/步长路径保持。
2. **Important 2关闭。** `ReactorLoadFollowSession.java:26`～`:27` 首次非空当前networkId即固定身份，已知ID变更或变null保护；初始null仍按原firstTime有限等待。`ReactorAutomationService.java:192` 以非空ID预检available，`:215` 在ARMING、C0预热及首付后的加载等待同样claim；原STOP/FAULT/卸载清理会话时释放登记。04首付代码没有新增C或持网权前置，C0不执行PI。26正式入口中第二台真实成型/装料/冷却的反应堆绑定同一C0已知网，第二位START实际拒绝，reason为stress_network_busy。
3. **Important 3关闭。** `ReactorAdministratorScreen.java:93` 先同步全部四份未dirty缓存，`:106` 写回四文本；普通seq0继续原位更新EditBox，焦点框不setValue，既有dirty早退及版本冻结保持。`:116` 完整APPLY也消费同一 `ReactorAdministratorLayout.DraftFields`，不是测试专用并行缓存入口。Layout仅增加该模型，原几何及页面未变，符合本卡明确许可。25包含MANUAL／LOAD两方向隐藏偏好及dirty不变的行为检查；26两玩家正式菜单提交后实际hiddenPreserved=true。此证据不冒称已执行真实客户端鼠标/焦点或GPU操作。

23为三项行为全部RED；24实际hiddenPreserved=false、secondStartAccepted=true并有1个真实场景失败，原日志与独立RED XML保留。25冻结XML五类实际3＋4＋4＋2＋12＝25项，失败/错误/跳过均0，日志23秒exit0。26本域4/4，记录hiddenPreserved=true、secondStartAccepted=false、stress_network_busy，三维度自然保存完成、25秒exit0；未扩大为完整两套热汽动力机组。原22/22、29/29、36/36、18/18仅按原阶段复用，不拼成一轮新全套。

## 同次制品补核与最终结论

28冻结29条当前来源与source-manifest及sources.zip逐项相同，无漂移；相对原写集只增加本卡已允许的Layout。27必要纠错assemble实际 **BUILD SUCCESSFUL in 1s、exit0，wall2.5670263秒**，仅jar执行，其余任务UP-TO-DATE，无clean/test/fullbuild。

正式与28冻结包字节相同，均 **3239779B／SHA-256 `86225aea306cb69e3f96b30384ef82c519cbadb2ef65b649d86cb0404ad6c304`**。实际JAR为678class＋1548资源＋1生成Manifest＝2227非目录文件，所有排除通配/精确路径无测试文件泄漏，14真正空目录单列。相对已经核过的22初包，只新增Layout.DraftFields一个class、改变Service／Session／PI／Layout／Screen五个class，删除0、资源变化0；这6个新/变载荷与当前编译输出及28manifest逐字节相同，其余JAR载荷与22逐字节保持。原1217仅换行分类和世界38文件／配置27文件零差异保护记录复用，未再扫描或操作用户存档。原22初包652aa及全部失败保留，main50d5未由执行者覆盖。

**三项Important已在同次差异复核关闭，无剩余Critical、Important或Minor，可交本批集中人工门。** 本结论只关闭上述修复及源/制品身份，不等于02实际热—汽—动力整定、鼠标/字体/GPU、真正ChunkManager卸载或独立进程restart已验，也不重开前批已经通过的管理员门。候选净提交与交付由PM完成；02未手测通过前不合main或替换主包。
