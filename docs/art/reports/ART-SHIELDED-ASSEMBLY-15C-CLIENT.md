# ART-SHIELDED-ASSEMBLY-15C 客户端消费与候选制品

**最终候选：** 本批唯一assemble实际10.5092882秒exit0，`build/libs/create_nuclear_industry-0.1.0.jar`为2844567B/SHA `dd6e25b8b04be4567f8d55d24918d75fad2c37a26ea41fb2c7675e420c6a9414`。九新增资源/567当前class与处理输出/JAR同源，原1953个非本批旧载荷保持（包含555旧class），无测试/探针泄漏；实际Minecraft动画门仍待用户。

## 当前实施

已实现卡内Renderer、纯Animation/Clock、同源声明式动作和两专属测试。制造双臂依次送入/共同对齐后退回，夹具合拢升降压装；封装夹具先固定、双臂同向合拢/Y短锁紧，退回后松开。原服务端账本/配方/网络/结构、旧C静态资源与R1窗层保持；用户已验黑窗门不重开。游戏内动画运行/中途停恢复仍由用户验收。

Renderer沿现行客户端注册预初始化八分件及私有三件坏数据fallback。实际原生dispatcher资源重载字节码证明重新构造Renderer，Context无ResourceManager入口，所以仅构造读取当前客户端资源经理；纯算法注入Reader，不触发Minecraft单例。原ClientEvents仅中文“三个PartialModel”数量注释改为“全部活动分件”，注册逻辑保持。证据`build/reports/art/ART-SHIELDED-ASSEMBLY-15C/client/renderer-reload-api.txt`。

每台BE/弱世界身份独立时钟。真实工时每tick按整数min(absRPM,256)，一份256同步量内有界推进，不能插帧生成完成；收到新进度先推进旧visual再取max，防止把该间隔双计。output优先于完成tick滞后的WORKING。无动力/过载/真暂停保持已有姿态、恢复不追停时；超过20tick渲染空档、坏时间、工序/work变化或权威进度倒退以收到的值重建基准；缺料/输出堵塞回待机。

Java与网页共同消费生产animation.json，SHA `f94c46fd1fb8d4c1bf99a7fa1553953bb6e9cebc3ae26a47eb3be77ff32cb47f`，绑定rig `c2a25527cfa2983f6ff246f730a43fa8d34d57d7c7333177f735845ddac056ba`。所有位移/pivot除16，父carriage升降只继承一次；实际矩阵F*D在局部动作后绕主控[8,*,8]作北0/东负π/2/南π/西正π/2。声明式28帧与smoothstep轨迹由A同批有限核，passed/0errors，复用其actual-animation-checks.json。

## 实际测试证据

- `red-clock`：9个真实行为失败，非编译占位失败；`green-clock`：原9/9通过。
- `red-arrival`：256RPM一tick及32RPM八tick新样本重复计工时用例真实失败；`green-arrival`仅该1项通过，不冒称整类10项。
- 原`red-pose`于21:08仅createMinecraftArtifacts后外部中断，未有exit/断言，不计RED；原log/attempt保持，red-pose-interrupted.json另记。恢复`red-pose-recovery`仅姿态7项，其中5项真实断言失败，exit1。
- `green-client`：当前Renderer/纯算法两新测试类实际Clock10+Pose7=17/17，30秒exit0；仅既有事件注解的两条deprecation警告，无生产代码修补。
- 为实际库中心补偿证明，Pose导出方法追加实际模型角点及四向原生结果；`green-mesh-point`、`green-mesh-facing`分别仅该1方法真实exit0，当前Java客户端/动作数据没有因此变更。没有全量旧测试/业务GameTest或全build。

全部原始命令、stdout/完整日志、exit和XML在本批client各阶段目录。测试使用E盘本进程tmp并恢复环境，原全局缓存保持；主PM另一候选Gradle与既有服务未终止或更改。

## 包与范围

当前前置602包精确备份在`client/backup/create_nuclear_industry-0.1.0-602985f8.jar`，2812546B/SHA `602985f85d2522c7914d7a4cef0b1c822f8c50ce11687e60af2be697428e3cd3`，backup-identity.json绑定。此时尚未assemble；等待负责人GPU实看及原同次组合审查无必改后唯一必要增量assemble。最终资源/class/JAR和非本批旧载荷差异核将写本批client，不覆盖旧7d/d12/a92/602历史证据。

## 负责人同批实际预览

root实际检查正常窗制造10%/封装40%、84%升台剖视、暂停/过载保持、恢复后停止与output完成回零，通过本批网页观察。两轮East取景失败原图/JSON保留，最终仅scene按实际世界Box3/view投影/画布宽高比适配修好，scene47a60f43…；E/S/W完整入画/withinViewport=true，Java/动作/rig/17项测试保持。当前31来源身份及最终GPU均供原同次组合审查，尚未assemble；Minecraft动画实际验收仍由用户进行。

## 唯一构建与真实制品核

原同次组合审查实际通过无剩余必改后，root单独授权执行`preview/build-client.ps1 -Reviewed`，脚本先核当前31来源与602精确备份，再调用`./gradlew.bat assemble --no-daemon`及本进程E盘tmp参数。真实退出0，10.5092882秒，compileJava/processResources均UP-TO-DATE，仅jar执行；未重跑测试/全build或改main/Git/世界。assemble-attempt.json、完整assemble.log、exit/result均在本批client。开工备份时Cfree约11.535GB、构建前18.496753664GB为实际外部变化，本执行者未清盘/缓存或改变全局环境。

首轮JAR核验真实失败仅因核脚本漏列编译器生成`ShieldedAssemblyRenderer$1.class`，没有通过或隐藏该失败。first脚本/log/exit/checks保存client/jar-first。实际旧/新JAR `javap -p -c`文本SHA同为`93cd302162913afe9a943157416b3ed658bfff3136d3c9e5c6365570be74ff86`，`-c -l`唯一差异LineNumberTable50→75；当前生成class SHA `65ff789e86279ccf3251373b8a39bbb930b479b390ce07e80c891e97241a606d`等新JAR。因此仅修核脚本分类，强制opcode一致证据及当前class→JAR绑定，再读同一个dd6包；未改生产或再次assemble。

最终jar-checks.json passed=true/0errors：9新增资源（8分件+动作JSON）production/processResources/JAR一致，567class与当前compileJava输出一致；19新增载荷为10客户端class+9资源，2旧变化仅Renderer和其生成切换表，555旧class及1953个非本批旧载荷保持，testLeaks空。原602/7d/d12/a92包与旧证据保持，窗口/原C/static item和所有服务器载荷保持。原审查者只补同次制品核，用户观察入口为本美术候选JAR，两工序运行与中途停恢复；不以网页或纯算法结果补记Minecraft验收。

## 基线与技能

实际树`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`/`codex/art-studio`/HEAD88b8fd2。实际应用minecraft-resource-pack、minecraft-modding/testing、test-driven-development、verification-before-completion及已批准brainstorming/本卡writing-plans/subagent-driven-development分工；版本核按锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Flywheel1.0.6/Ponder1.0.82，治理1.2/5.1优先，无升级或另派Agent。当前31来源、结果与最终两报告身份将以client/final-identity.json读回冻结。
