# REACTOR-AUTOMATION-02 实施记录

本批唯一执行者，目录 Create_NuclearIndustry-reactor-load-follow，开工 HEAD 3647fcc821d3386db008c73faf8cd04c9872bf1e。原 main、管理员候选、用户世界/配置不写。世界38文件和配置27文件由PM已携带，00-save-copy-receipt.json保留。本批原包3193873字节，SHA256 50d5bd9b19f6f27e5556668f013b8844f983138117471e7cb98dac7bc2d844bb，证据备份00-baseline-50d5.jar。

只在02允许写集施工，不Git写、不转派。原三页GUI、菜单会话nonce/epoch/revision/sequence和STOP权威保持。Ctrl读取既有 CniAutomationView 取得戳，新绑定意图仍由服务器核当前实例/玩家/选择寿命，不复活旧世界设值协议。04首核预算及足额付款不增加容量前置；正常LOAD续接后仅新当前表C0允许1200tick暂态loading-rebind，保棒位/真实核热结算，不执行PI或重新预热，首份有效容量后失联/换网立即FAULT。

## 任务1当前证据

- 02-positive-band-red.log：真实1项/1失败，26秒exit1，原恒功率“不超过目标”选档使低目标落到100%全插。
- 03-positive-band-green.log：文件名不代表结果。编辑命令语法失败使旧测试未更新，实际19项/1失败、18秒exit1，保留为重复RED，未计通过。
- 04-pi-behavior-red.log：minimal hold-target骨架下29项/8失败，19秒exit1，PI/容量/时间及三时滞响应行为RED。
- 05-pi-first-green.log：实际29项/3失败，19秒exit1。Kp=.25/Ki=.005在0/5/20秒模型末段余量约.124/.126/.132，未达到断言。
- 06-pi-tuned.log：实际29/29、19秒exit0。原 Controller 18项与本批纯PI11项（含三时滞）通过。有限整定保持Kp=.25，将Ki取.01；CSV/摘要在本域，数学单位测试仍用工程初值.005证明实际tick秒数。

模型仅为SU/HU比例10、热惯性5秒及额外0/5/20秒时滞、1/5百分点限速，含负载4000→0→6000SU，非真实锅炉—汽轮机整条闭环稳定性保证。未做GPU/客户端/新增人工验收。

## 实际路由与保持边界

绑定由客户端 `InputEvent.InteractionKeyMappingTriggered` 仅截获Ctrl＋主手扳手；潜行、普通扳手与副手不截获。第一步选真实StressGaugeBlockEntity，第二步用既有CniAutomationView的Generation/Port/Epoch提交独立Bind意图。服务器游戏线程核持物、可达目标、维度、选择寿命、同一表实例、序列和当前管理员绑定，只在STOPPED提交Link并推进epoch/revision。旧HEAT/DEPTH仍返回gui_required。原GUI的随机session与epoch/revision/sequence、独立START/STOP/RESET保持；新的完整草稿含棒深、恒功率目标、余量和最低毛HU，模式预览保各模式草稿，不将新投影升级dirty版本。

原生表只读getNetworkCapacity/getNetworkStress和当前network字段；不以转速0判失联，不调用getOrCreateNetwork。容量0没有百分比，过载余量可为负；原始和派生非有限值拒绝。登记按世界实例与networkId互斥，退出模式/STOP/FAULT/真实失联清会话；换网如并入另一持权网也撤其许可。新表同坐标不会继承旧引用。

每tick原库存、逐列热、冷却可承载与真实红石保护先处理；正常PI仍每10tick、提出1/插入5百分点。预警可插过最低HU。初次C0仅有限预热到最低正档；已建立容量后再丢容量立即FAULT，不重新预热。新接口出厂默认最低毛HU2（默认维护1＋净1），维护非默认时按维护＋1构造；玩家明确保存值不改。运行范围来自真实101档曲线和原管理员上限，旧恒功率0目标/选档含义保持。

04正常保存意图追加两偏好字段，不存积分、样本、网络权或付款票据。先依原当前身份、原生低沿与真实首核费恢复运行意图/逐棒actual与target/调节进度，随后仅真实同实例C0允许新的1200tick loading-rebind，保棒位、每tick真实核费和原保护，不执行PI/新预热。首份有效当前容量才初始化PI；无表/错身份不是C0等待，之后失容量/换表/换网FAULT。总览单独显示原状态续接等待，未造100%余量或免费热级。

## 定向与真实结果

| 证据日志 | 实际结果 | 口径 |
| --- | --- | --- |
| 07-binding-red | 5项/2失败，18秒exit1 | 选择寿命与同网互斥行为RED |
| 08-session-controls-red | 8项/3失败，20秒exit1 | 有限等待、实例/容量失效与完整草稿RED；前述绑定GREEN |
| 09-resume-warmup-red | compileTestJava失败，1秒exit1 | 测试所需只读getter缺符号，不计行为RED |
| 10-resume-warmup-red | 5项/5失败，20秒exit1 | 真实新增阶段、保存偏好及会话行为RED |
| 11-session-controls-green | 36/36，21秒exit0 | 绑定/原权限、新生命周期/草稿、原Controller与原Resume |
| 16-gui-affected-e-runtime | 18/18，91秒exit0 | GUI完整动作及原Blaze、Slider协议、SCRAM受影响入口；首次E缓存重算 |
| 17-real-first | 2/4，24秒；游戏进程exit2、Gradleexit1 | 最后换表/容量0断言在同tick显式tick后立即读状态，触原去重；三维度自然保存完成 |
| 18-real-next-tick | 4/4，24秒exit0 | 两断言移至下一真实tick，原保护/去重不改；三维度自然保存且退出 |
| 19-finite-wire-red | 2项/1失败，19秒exit1 | 有限double原始值可产生无穷余量；完整五字段/绑定戳codec回环已通过；原XML冻结 |
| 20-final-affected | 22/22，26秒exit0 | 最窄派生有限护栏、PI含三时滞、完整草稿/协议、绑定、生命周期及原菜单动作 |

上述不同阶段不合称为某一轮全套。Task1原29/29、11原36/36与16原18/18未改范围复用；没有full test/build或重开原01/03/04全域。

真实四代表范围：binding_and_controls走真实玩家持物/可达性、两步选择绑定、原子拒绝/应用、C0预热、运行拒改绑、当前菜单旧显示版本STOP及投影；real_network_response使用已加载原生表updateFromNetwork受控输入，证明getSpeed0时仍C100/L200/r=-1、增减功率方向、空载正档及下一真实tick失容量保护；blocked_return证明热库存96%覆盖最低HU、全插目标、恢复不自启；lifecycle_isolation走真实BE/NBT同版本重建、原生反相线、零外燃料/零缓存的新1HU核费，C0保0.5棒位与真实收费且无PI，容量恢复后新PI，同网登记拒另一旧owner身份及同坐标新表保护。完整两个现场热力机组的同网竞争、实际鼠标/字体/GPU、真正ChunkManager卸载或独立进程重启不在这四代表的证明范围；纯登记互斥/不同世界隔离另有unit证据，不将受控原生输入称为真实整条热汽动力机组。

最终工程Kp=.25、Ki=.01；初值Ki=.005收敛偏慢已保留失败。`pi-delay-{0,5,20}s.csv`与摘要记录实际换向/毛HU/余量/积分/限速。模型与断言未为GREEN降低时滞、取消限速或放宽最低正档。新增人工机组门仍待PM交候选后用户验收。

## C盘空间与本批环境

12注册文件写失败（1秒exit1），13classpath报磁盘空间不足（4秒exit1）；C Free=0，E约382GiB，均为测试前环境失败，不计业务RED。PM精确扩卡后只读复制原wrapper、modules-2、Gradle9、jars-9和NeoForm缓存到本域runtime，排除锁/daemon；C原件不删/不移，不停用户进程。真实NeoFormRuntimeTask字节码以GradleUserHome/caches/neoformruntime为home、本树build/tmp为work。15环境help6秒exit0，16实际资产根properties已E盘；TEMP/TMP/java.io.tmpdir仅进程级E目录，工程原-Xmx3G和技术栈保持。

`prepare-runtime.ps1`、`invoke-gradle.ps1`与runtime-copy/14API/15日志在本报告域。候选手测入口可用 `& .\build\reports\reactor-load-follow-02\invoke-gradle.ps1 runClient`；这只是E盘环境调用，不自动运行用户世界，本批未使用该入口启动客户端。全局配置、驱动、画质及用户运行配置均不写。

## 唯一增量包、来源与保护

21命令 `invoke-gradle.ps1 assemble --console=plain`，Gradle2秒、实测wall2.5598538秒exit0；无clean/test/fullbuild，没有中间包。原3193873B／50d5bd9b…包保留。本批正式/冻结候选 **3237666B**，SHA256 **652aa431758d23e7cc1b18c08ba6396acda8a06cfc26668f5ce897cfc55203d3**。

冻结目录：`build/reports/reactor-load-follow-02/22-freeze/`：

- `source-manifest.json`：HEAD3647fcc、精确28路径（20个main Java含本批GameTest、4个新JUnit、2语言＋1新模板、build.gradle）；`sources.zip`逐字节来源，可直接作为PM净暂存写集。
- `artifact-manifest.json`：按build.gradle实际正式排除，**677 class＋1548资源＋1生成Manifest＝2226非目录文件**；全部class/resource输出与JAR哈希相同，零漏项/零不一致；不把开发输出额外测试类/模板计为生产载荷。新ReactorLoadFollowGameTests及本域NBT、原管理员所有排除均无文件载荷泄漏。
- 234 ZIP目录项中14真正空目录；本批新域只有root/structure两个空占位，另列而非当作泄漏。最初把所有目录项标空的元数据副本保留，现正式清单已按非目录子项纠正，无源码/包变更或重构建。
- `baseline-delta-classification.json`：相对50d5，新增16 class、删除0文件，26既有class与2语言为实质变化；另外1217既有文本资源只有CRLF/LF字节差（候选检出CRLF、06干净导出LF），规范化后逐字相等，不是素材/玩法更改。1245改变文件口径完整披露，不宣称全包其余字节一致。
- `world-config-preservation.json`：本候选用户世界38文件及运行配置27文件与PM00携带收据哈希/字节均相同，零差异；main/原管理员树、原世界原件和原JAR均未写。`actual-probe-automation-0.toml`是隔离真实域实际配置，含最终Ki=.01，独立于用户配置。
- `final-junit/`、summary/21退出JSON、原失败/原包及全部此前证据保留。`git diff --check -- src build.gradle`通过；用户/工具日志正常变化及PM计划补充不属于28来源，不清理/暂存。

源码/包/本报告已稳定，执行者交回并停止写入。未Git提交、未合main、未覆盖主包，未自行派审或登记人工通过；由PM沿原一次组合窄审接续，新增集中客户端/实际闭环门保持。

## 本批组合审查集中整改

上节为22初次交审时点。本节记录同批三项Important的集中整改；原22来源、包、失败证据及全部既过结果保留，不将本节检查合称为原完整套，也不由执行者宣布审查关闭。

1. 保护期PI不计算新目标、不累计积分，但每个保护tick用当前真实样本推进滤波及previousTick，并跟踪当前实际档位的热量。跨零后解除保护仅积分最后保护tick之后的正常许可时间，原同方向限幅抗积分饱和保持。新行为测试连续200tick保护、I保持100、解除后只计10tick正常周期，直接调用Service实际采用的trackProtected入口。
2. NO_CAPACITY的当前networkId非空时固定本会话网络身份并参加available/claim互斥，涵盖ARMING、C0预热和04真实首核付款之后的loading-rebind。第二位正式START拒绝；等待中已知换网或已知变null仍保护。初始null网继续有限等待，不生成网络、不造余量、不执行PI；04首核预算与付款未添加C前置。
3. 未dirty同模式seq0确认投影同步棒深、恒功率、余量及最低毛HU四份缓存，Screen实际确认同步与完整Draft提交共同消费Layout.DraftFields。原位更新可见EditBox，保原对象；焦点框不setValue，dirty草稿仍保原版本且不被新投影升级。原模式切换的明确重建逻辑、完整五字段协议及START/STOP权威保持。

| 证据 | 实际结果 | 行为范围 |
| --- | --- | --- |
| 23-review-behavior-red | 3项/3失败，24秒exit1 | 保护跨零恢复补积分、已知C0漏身份、同模式漏隐藏偏好；RED XML独立保留 |
| 24-review-native-red | 3/4，27秒，游戏/Gradle exit1；三维度自然保存 | 实际日志hiddenPreserved=false、secondStartAccepted=true；有效真实行为RED，不把保存完成说成业务通过 |
| 25-review-affected-green | 25/25，23秒exit0 | Commands 3、LoadControls 4、LoadLifecycle 4、GaugeBinding 2、StressPI 12；只跑受影响定向 |
| 26-review-native-green | 4/4，25秒exit0；三维度自然保存并退出 | hiddenPreserved=true、secondStartAccepted=false、reason=stress_network_busy；旧未改代表同域保留 |
| 27-review-assemble | Gradle1秒、wall2.5670263秒exit0 | 唯一必要纠错增量assemble；仅jar执行，其余UP-TO-DATE；没有clean/test/fullbuild |

真实binding_and_controls内，两个ServerPlayer先后打开同一反应堆实际菜单。B在同MANUAL模式正式APPLY隐藏偏好37%/3HU，A用当前新投影调用Screen共享缓存模型，再改棒深并走完整实际菜单APPLY；断言偏好仍37%/3HU。另搭第二台真实形成、装料、冷却的反应堆，绑定同一已加载C0且已知ID的原生应力表；两位分别用实际菜单正式START，断言第二位拒绝而非直接测试登记表。这些代表没有搭建完整两套热汽动力机组。UI证明覆盖实际共用模型和服务器提交路径，未运行GPU字体/鼠标或实际客户端焦点操作；这些仍属集中人工门。

源码稳定后只作27必要打包。最终正式/冻结候选 **3239779B**，SHA256 **86225aea306cb69e3f96b30384ef82c519cbadb2ef65b649d86cb0404ad6c304**，独立冻结目录 `build/reports/reactor-load-follow-02/28-freeze-review/`：

- `source-manifest.json`/`source-paths.txt`/`sources.zip`：HEAD3647fcc，29精确来源路径（21个main Java含本批GameTest、4个JUnit、2语言、1测试模板、build.gradle）；新增Layout为PM明确许可的既有GUI模型适配。源码未Git写。
- `artifact-manifest.json`：**678 class＋1548资源＋1生成Manifest＝2227非目录文件**；所有实际class/resource输出与JAR逐字节一致，零output_mismatch和excluded_file_leaks，原管理员及本批GameTest所有类/NBT正式排除保持。234 ZIP目录项中14真正空目录，空占位单列。
- `initial-review-delta-classification.json`：相对22初包新增Layout.DraftFields一个class，改变Service、LoadFollowSession、StressPiController、Layout、Screen五个class；删除0，所有既有资源文件逐字节未变。
- `baseline-delta-classification.json`：相对50d5原包新增17 class，删除0；1246变化中1217仍仅CRLF/LF，另27既有class与2语言为实质变化。换行分类保持初次披露，不冒称其余原包全字节一致。
- `world-config-preservation.json`：用户候选世界38文件和运行配置27文件与00携带收据全等，零差异；原main/旧管理员树及各正式包未写。`final-junit/`保留25实际XML；23 RED XML、24/26原始日志、27实际秒数/exit及22初包全部保留。
- `handoff-identity.json`补录当前来源与冻结来源零漂移、正式JAR与冻结JAR相同、来源ZIP及本报告哈希。`git diff --check -- src build.gradle`通过。PM计划/REVIEW/人工表及用户工具日志不计入29来源，不清理或暂存。

本轮集中整改源码、制品与报告稳定后交回并停写；未合main、未覆盖主包、未自行派审或登记人工通过。由PM交同一原审查者做同次差异窄复核，实际客户端和整条闭环人工门保持。
