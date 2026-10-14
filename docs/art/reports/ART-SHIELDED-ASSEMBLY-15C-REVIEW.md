# ART-SHIELDED-ASSEMBLY-15C 原同次组合审查

2026-10-14，原唯一独立规格/质量审查者 `shielded15_review`。已完整读取活动卡、最新主目录AGENTS及相关资源模型/客户端/测试技能；按治理1.2/5.1只审本批批准的两工序动画、八分件与真实预览，不承接主PM锅炉或02工作。本报告是15C原一次组合窄审；审查者仅写本文件，没有修改实现、另派Agent、运行生成/测试/HTTP/Gradle/游戏、操作Git或写main/世界/配置。

**结论：资产、客户端源码与负责人实际网页GPU证据组合审查，以及原唯一增量assemble后的同次制品补核均通过，无剩余必改。最终候选2844567B / dd6e25b8…与已审来源一致。实际Minecraft动画、停转/过载/暂停/恢复及资源重载效果仍待用户集中观察。** R1黑窗已获用户验收，本批保持其八窗模型metadata及原PNG，不重开已验静态门。

## 同源范围与实际读回

审查输入为[任务卡](E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/tasks/ART-SHIELDED-ASSEMBLY-15C.md)、[资产报告](E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/reports/ART-SHIELDED-ASSEMBLY-15C-ASSETS.md)、[客户端报告](E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/reports/ART-SHIELDED-ASSEMBLY-15C-CLIENT.md)、[预览报告](E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/reports/ART-SHIELDED-ASSEMBLY-15C-PREVIEW.md)及[负责人GPU记录](E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/reports/ART-SHIELDED-ASSEMBLY-15C-GPU/README.md)。实际写树为 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，来源基线 `88b8fd2` / `codex/art-studio`。

组合审查时只读SHA补核合并资产15来源、原45保护项、客户端29非报告来源和最终GPU52磁盘输入，共97个唯一文件，全部与各自证据相等；未重造作者检查。第一轮31来源身份另存 `client/pre-review-camera-first-identity.json`，包含scene5f7f…与当时报告；当前 `client/pre-review-identity.json` 是最终取景通过、打包前的31来源身份，scene47a60…及当时两报告已实际补核。包后 `client/final-identity.json` 绑定31最终来源；仅报告/README与制品核脚本的后续变化在末节补核，其余实现保持。

| 当前输入 | SHA256 |
| --- | --- |
| rig.json | `c2a25527cfa2983f6ff246f730a43fa8d34d57d7c7333177f735845ddac056ba` |
| 生产animation.json | `f94c46fd1fb8d4c1bf99a7fa1553953bb6e9cebc3ae26a47eb3be77ff32cb47f` |
| ShieldedAssemblyAnimation.java | `919b61385e051e9a063bf36c386738a404380e1f447e774cebbf69254fc56c19` |
| ShieldedAssemblyAnimationClock.java | `503f970f5d7d38327e5083066406164256e3b0bbe204d25f4c53f6c24f8b81ea` |
| ShieldedAssemblyRenderer.java | `9fdac7556aa8e4253e9fbad41b9b9b38f65799d415a01a7a0cb697e4b55f1c7c` |
| ClientEvents（唯一数量注释变动） | `458cd54d97b7eeb8c03e79c08c3655576aa31ff9f0da52dad29e88ef0acddd04` |
| 最终预览scene.mjs，21571B | `47a60f430d1aa5806ca67c40bc4f1a01f1de1d1bfa737f946f6b9fdc46720de2` |
| 当前原生pose-parity.json | `0a817e431046a1229f1efd2f858335aefddedefa81674440fcad3e09c94842cc` |

## 八分件、运动范围与工序分工

读取实际模型/rig及原作者checks、installed-checks、window-contact-details和actual-animation-checks。原三partial的29+29+28共86元素唯一分到11/18/11/18/12/8/4/4八组，原from/to、面、UV、纹理槽保持；八候选→生产SHA相等，原45保护项零变化。固定肩座、夹具底座与导中柱留在base，执行杆/末端在tool，台与两jaw独立，待机还原原三件。原C壳体、完整物品、九PNG、R1窗层和旧partial保持。

工具pivot实际为 `[10,20,14.5]` / `[22,20,14.5]`，台 `[16,15,16]`，左右jaw以carriage为父；不借旧静态估计转轴。global模型单位0..32，消费时除16；局部动作后最终围主控x=z=8朝向，原DOWN底轴 `[8,0,8]` 保持。

制造只用左右Z旋转0..14°/-14..0°及内滑≤0.25，先交替送入/对齐再共同保持，工具退回后夹具压装；封装Z=0、独立内滑≤1.5及Y±6°短锁紧，台固定。台Y≤1，升台时两tool严格待机；左右jaw局部X分别[-0.5,0.1]/[-0.1,0.5]，只继承一次台Y。28关键帧（制造16、封装12）与26区间130代表姿态绑定当前生产动作SHA；共享单调smoothstep及分量凸域保持连续范围，相邻端点另约束工具/升台互斥。复用原OBB/SAT、角点导数临界角与四向包络，不重跑扫掠。

允许接合仅钢芯顶端→同侧肩中央socket与台锁圈→固定导柱；原探索17°黄铜碰肩轨、20/25°夹指碰锁圈等负证据仍保留，因此最终限值有实际几何依据。工具到台侧最小余隙约0.086单位、闭jaw隙0.05单位，全部在2×2×2内；没有工件模型，也没有将机构动作表述为已碰到物料。网页实际正常窗/剖视显示这套层次，数学约束不代替游戏视觉验收。

## 客户端与真实原生入口

完整读取Renderer、Animation、Clock及两专属测试。Renderer静态登记八活动PartialModel并保留私有旧三件坏数据fallback，原ClientEvents仍只预初始化同类、注册同Renderer，唯一diff为中文“三个PartialModel”改“全部活动分件”。实际dispatcher资源重载字节码重新构造Renderer/Context，Context无ResourceManager入口；当前构造从真实客户端ResourceManager读取生产JSON，纯算法通过Reader注入。IO/声明错误保留安全待机fallback，无服务端注册或事件改造。

每BE持有独立时钟，WeakHashMap键与弱Level身份避免跨机器/换世界混用，重建Renderer自然清除旧轨迹。矩阵为原生F*D：分组位移、pivot旋转和父台位移在D中，之后F绕主控(.5,0,.5)转北0/东-π/2/南π/西π/2；没有全机中心16改轴、重复facing或global分件二次偏移。PoseStack真实矩阵/法线与JOML定向结果及实际模型源角点有有限绑定。原BBox、packed light、原生Create SHAFT_HALF/DOWN及其动力旋转保持；八活动分件继续cutoutMipped，未做整壳透明或全亮。

Clock对应实际服务端每tick整数min(absRPM,256)及跨256同步量。当前实现先推进旧visual再取max(received)，关闭新进度到达双计；外推不越received+256且不生成完成，低速32和有符号/分数RPM按实际整数步处理。output/缺料/失效优先复位；真实完成tick中WORKING晚一tick仍由output/进度规则回待机。停止、过载或真暂停不推进已收到的有效姿态，恢复不补停时；模式/work变化、权威倒退、坏时间/长渲染空档重建基准。未向BE写progress、库存、网络或服务端时间。

## 定向证据的阶段边界

实际读取作者原始exit/XML/log，而非只接报告“通过”措辞：

- 原clock行为RED9项→green-clock原9/9；arrival双计整改另有RED及green-arrival单项1/1，不冒称当时整类10/10。
- 21:08原red-pose中断只有createMinecraftArtifacts输出，没有exit或断言，保留中断身份，不计有效RED。恢复red-pose-recovery为7项/5真实断言失败/exit1。
- 最终green-client为Clock10+Pose7，17/17、0失败/错误、exit0，30.4468361秒；绑定当前客户端生产实现与动作。
- 后续只扩Pose导出方法的真实模型角点/四向原生证据，green-mesh-point和green-mesh-facing分别只该方法1/1、exit0；不将其冒称第二套17。当前oracle包含9姿态、72组原生矩阵及36四向矩阵。
- A首次实际动作局部检查的TypeError属于检查工具变量错误，保留原失败，修正工具后同一生产JSON有效通过；不把它计生产动作RED。

没有旧业务/兼容全套或新游戏观察。手写非显然单位、矩阵、客户端/权威边界与时钟不变量已有中文说明。

## 真实网页GPU与取景纠错

预览4195由原B持有普通exec托管前台服务，页面明确示范输入，未连接用户游戏。实际库加载生产八件、16壳off/on、九PNG、完整item与Create来源半轴；库减[8,8,8]由+.5和1/16还原，之后D/F再场景偏移[-1,0,-1]。实际8组Box3和108源角点与原生oracle比对；原52HTTP磁盘/响应身份、72组+36四向矩阵及29 JS clock步骤复用，不重跑。

审查者实际查看最终East/South/West默认斜视制造40%剖视、North正面制造10%/封装40%、制造84%升台及完整静态item PNG。窗内交替倾斜与同时短移锁紧可区分，固定肩座/底座保持；升台时两工具待机，台/jaw共同上移。读取暂停前后JSON：running=true/paused=true，phase均0.4、progress均5120、pose逐字段相等；过载仍5120，恢复再停至约5280而不补等待工时；完成output=true、phase0、八组变换全零。

两次East失败原图/JSON均保留。第一轮相机还看固定原点；第一次改target后第二次真实349×475画布仍裁右边，因此target数字和ready/errors[]不足以放行。最终仅scene依据真实整机world Box3八角→camera view包络同时适配横纵视野，并将剖面随S*F变换；Java/动作/rig保持。最终East包络[-2,0,-1]至[0,2,1]，view水平半宽1.392728，frustum水平半宽1.532001、纵半高2.085102；NDC水平极值约±0.909091，三向八角XYZ均在裁剪范围内，host/canvas/WebGL viewport均349×475、scissor关闭。实际三张完整像素边缘无裁切，North两动作复看亦完整；不同朝向背侧半壳遮挡未误登记模型消失。

最终五份捕获ready=true/errors=[]/withinViewport=true，44实际fetch输入与52磁盘来源身份记录当前scene47a60…；据真实包络、投影及像素闭合本批网页取景问题。该结果仅覆盖网页预览；没有声称Minecraft暂停、Flywheel重载或游戏工序动画已经由用户验收。

## 同次制品补核

组合审查通过后，负责人通知原唯一包作者调用已批准入口。实际 `client/assemble.log` 为BUILD SUCCESSFUL，`assemble-result.json` 为exit0 / 10.5092882秒；compileJava/processResources均UP-TO-DATE，仅jar执行一次。没有第二次assemble、测试或全build。开工前实际Cfree18.496753664GB，使用本进程E盘tmp，没有以C盘真实失败追认通过，也没有清盘/缓存或环境变更。

审查者只读当前包与前置精确备份，完整SHA实际读回与原作者结果相等：

| 制品 | 字节数 | 实际SHA256 |
| --- | ---: | --- |
| 当前 `build/libs/create_nuclear_industry-0.1.0.jar` | 2844567 | `dd6e25b8b04be4567f8d55d24918d75fad2c37a26ea41fb2c7675e420c6a9414` |
| `client/backup/create_nuclear_industry-0.1.0-602985f8.jar` | 2812546 | `602985f85d2522c7914d7a4cef0b1c822f8c50ce11687e60af2be697428e3cd3` |

首轮制品核验exit1/log/checks及原脚本在 `client/jar-first/` 保留，实际唯一失败为核脚本将编译器生成的 `ShieldedAssemblyRenderer$1.class` 误列成非本批旧载荷。没有隐藏失败或改生产/重打包来消除它。实际读取旧新JAR导出的四份javap文件：两份opcode完整相同，含EAST/SOUTH/WEST映射1/2/3和NoSuchFieldError异常表，SHA均 `93cd302162913afe9a943157416b3ed658bfff3136d3c9e5c6365570be74ff86`；两份带debug文本仅LineNumberTable由50改75。当前Renderer第75行正是同一Direction switch，生成class853B / SHA `65ff789e86279ccf3251373b8a39bbb930b479b390ce07e80c891e97241a606d` 与新JAR一致。因而分类依据是原/新实际字节码和源switch链，而非 `$1` 名字。

读取修后的verify-jar.ps1，旧载荷例外只精确列该生成切换表且强制opcode证据SHA一致，没有放开Renderer任意内部class。仅修核脚本后读取同一dd6包，最终 `jar-verify.exit` 为0、`jar-checks.json` passed=true/errors=[]：八分件+动作JSON九资源production/processResources/JAR相等；567class与当前compileJava输出相等；19新增载荷仅九资源及十客户端class；两个旧载荷变化仅Renderer.class与上述生成表，555旧class/1953非本批旧载荷保持（1953含class），无旧载荷丢失、testLeaks为空。原壳/物品/九PNG/R1窗层和服务器载荷继续保持。

最终31来源身份与打包前31来源比较，仅两报告、工具README及verify-jar.ps1四项变化；四项当前SHA已逐一补读，分别为CLIENT `0b3c0d2aeb8318067cda82c9693408f44803335fbf0a5c40f53d1b6d930ba355`、PREVIEW `1355ea0133f37293a43afd73d0fbfe5a31c062cfa328622a092557d4c9ed2e6f`、README `5d8d091171d42a40ecf7c51789cb79dd2cde4d59336f0b9e2229597de46fac29`、核脚本 `72d730213f9c6991da79b5803cc21f2077d02fc5590b04be2473d6f42f4370f2`，均与final-identity相等。未重复97来源大读回；最终身份中的reviewBeforePackageSha256是补核前本报告身份，报告按本卡追加制品，未冒充其仍为当前报告SHA。

**本次组合审查及同次制品补核结束，无剩余必改；审查者现停写。** 原失败/中断、两轮East裁切与历史身份均保留，游戏动画集中人工门和main集成权限独立；不再运行检查/构建或扩大审查。
