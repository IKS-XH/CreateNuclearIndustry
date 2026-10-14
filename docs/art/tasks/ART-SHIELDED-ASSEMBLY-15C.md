# ART-SHIELDED-ASSEMBLY-15C 屏蔽装配室分工动画实施卡

> 原执行者使用subagent-driven-development按本卡分工接续；本项目治理1.2/5.1优先于技能中的新树、Git提交、重复审查和全量测试建议。

**目标：** 在已选C版屏蔽装配室内，按真实制造/封装工序和加工进度绘制双臂与夹具动作，停机冻结；正式同源动画预览和一个增量候选包交用户实看。

**结构：** 服务端已同步的`state().operation()/progress()/work()/input()/output()`、`WORKING`、原实际RPM及过载条件是唯一输入。动画仅客户端读取，既有三件整机坐标partial按原元素拆成固定/活动组；渲染器对活动组执行局部变换后再绕主格中心转朝向。固定底部Create半轴沿原Kinetic渲染。

**技术基线：** MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Flywheel1.0.6、Ponder1.0.82；本地`E:/MyMC/NewMod/block-model-renderer`2.20.0用于真实模型预览，不安装/升级依赖。

**已批准设计：** 用户先要求“接着制作动画吧”，并对下述动作明确“采用这套分工动作（推荐）”：新燃料制造时双臂交替送入、对齐，夹具合拢压装后松开；乏燃料封装时夹具固定组件，双臂合拢并围绕封口作短行程锁紧，完成后退回。动作跟随实际加工进度，断电或过载时停在当前位置。

## 保护范围与角色

复用`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`、`codex/art-studio`、HEAD88b8fd2及既有dirty来源。R1窗黑面已由用户实机关闭，当前JAR2812546B/SHA602985f85d2522c7914d7a4cef0b1c822f8c50ce11687e60af2be697428e3cd3为本次前置，唯一包作者先精确备份。

所有Git/main/其它树/世界/配置只读，不复制存档、清缓存、启动用户游戏或停止已有服务。原C静态八格机壳、窗cutout_mipped、九PNG/SVG、完整物品与旧三partial原字节保持；通过新增分件保留待机形状。原玩法/工时/配方/库存/服务器tick/网络/NBT/注册/碰撞/采光不改，不增加物料/粒子/独立GUI。需要这些路径时先报告最小必要链再协调，不用美术侧改账本或新增同步。

root只维护本卡/PLAN/README/候选及只读审核。沿用原资产作者、原预览/包作者与一次原组合审查者，执行者只写各自路径；不另派新审查或重复验证。

实际使用brainstorming（已完成短设计确认）、writing-plans（本卡）、subagent-driven-development、minecraft-resource-pack、minecraft-modding/testing、test-driven-development、verification-before-completion。开工实际读取AGENTS/本卡/治理1.2与5.1/5.2及所用技能，中文手写注释与职责/单位/客户端边界保持。

## 接口与关键不变量

1. 原几何以模型像素0..32计，运行位移除16后使用方块单位。四朝向围绕主控[8,*,8]转向，不能以整机中心[16,*,16]重复旋转；新分件保留整机原点。
2. 资产组名固定为`left_base/left_tool/right_base/right_tool/fixture_base/fixture_carriage/fixture_jaw_left/fixture_jaw_right`。每个原元素只归一组；总和必须精确还原原三partial（29+29+28元素），无新增/删面/重叠重复渲染。base不随工具运动，夹具底座不随升降台运动。只有实际无法表达已批准短行程动作时报告所需新组再扩卡。
3. 资产作者交`rig.json`：上述组模型路径、原元素名、实际pivot、允许位移/角度范围和局部变换顺序。pivot按真实关节定位，不直接沿用15B未实施动画的静态估计。运动包络在原2×2×2内部、窗后可见，无穿壳或左右工具相交；接触工作位与运动极值分别记录，不用删几何掩盖。
4. 原进度按每活动tick增加`min(abs(RPM),256)`，原256工时跨段同步，低速32最多约8tick一份新进度。动画在原更新间隔内有界平滑，不能仅用全局时间循环而脱离工序；不得显示提前完成、收到产出仍重复开工或把反转当反向减工时。
5. 无动力/过载/WORKING false冻结现有姿态；恢复接真实进度。单人真暂停冻结时间；长时间未渲染、区块重载/新主控、工序改变、进度清零及完成回零有明确重置或待机语义，不能跨实体/世界共享旧相位或无限外推。未开工/缺料/输出堵塞不空转，成品已生成时回待机；不同设备独立。
6. 工序动作只定义视觉阶段，不改真实事务时点。阶段边界缓入缓出；制造呈交替对齐与压装，封装呈同时合拢和短锁紧，整批结束前退回待机，使下一批不突跳。
7. 同一份声明式动作数据由Java渲染和预览消费；若锁定API使此不可行，先报告实际原因与最小同源方案，不悄悄用两套手写动画。正式预览只加载生产分件/PNG，不用替代SVG或设计稿图片；物品保持静态。

## A：原资产作者，分件与真实运动约束

写集仅：
- 新`tools/art-assets/shielded-assembly-animation-15c/assets/`、`produce.py`、`verify-assets.py`、`assets.json`、`rig.json`和工具说明（geometry部分）。
- 新生产`src/main/resources/assets/create_nuclear_industry/models/block/shielded_assembly_station/animation/{八组名}.json`；原三partial/C模型/纹理不改。
- `build/reports/art/ART-SHIELDED-ASSEMBLY-15C/assets/`与`docs/art/reports/ART-SHIELDED-ASSEMBLY-15C-ASSETS.md`。

先发布组/pivot/约束rig供B消费，再安装8分件，候选→生产身份绑定。有限检查原元素/UV/纹理精确分区、待机组合一致、实际允许动作包络/接触和四向映射；禁止整套旧素材重生成或运行Gradle。读入原15B资源，不修改旧manifest/R1冻结结果。

- [x] 实际模型分组/pivot合同与可见运动范围交B。
- [x] 精确新增8分件、有限几何/引用校验及来源身份。

## B：原预览/包作者，客户端消费、同源预览与唯一构建

写集仅：
- 原`src/main/java/com/iksxh/create_nuclear_industry/production/ShieldedAssemblyRenderer.java`。
- 原`ShieldedAssemblyClientEvents.java`仅把“三个PartialModel”的相关中文注释改为“全部活动分件”；注册/事件方法与其它文本不改。PARTIAL_MODELS八分件沿原预读完成类初始化，旧三件可作为私有坏数据待机组同时初始化，不增加注册入口。
- 新同包`ShieldedAssemblyAnimation.java`（纯姿态/阶段）和`ShieldedAssemblyAnimationClock.java`（客户端样本有界平滑/冻结）；只在确实需要时使用其它新客户端辅助类并先报告路径。不改BlockEntity/State/Structure/Recipe/ClientEvents等其它既有Java。
- 新`src/main/resources/assets/create_nuclear_industry/shielded_assembly/animation.json`声明式动作合同，A的rig/八模型只读。
- 新同目录`src/test/java/.../production/ShieldedAssemblyAnimationTest.java`与`ShieldedAssemblyAnimationClockTest.java`，专用输出在15C/client，不改其它测试。
- 新`tools/art-assets/shielded-assembly-animation-15c/preview/`、`index.html`、`server.mjs`、工具说明（客户端/预览部分）；A文件只读。
- 新`build/reports/art/ART-SHIELDED-ASSEMBLY-15C/{preview,client}/`及`docs/art/reports/ART-SHIELDED-ASSEMBLY-15C-{CLIENT,PREVIEW}.md`。
- 唯一必要增量assemble更新本美术树build/libs，先备份602原包；打包日志与核验只在本批client，不覆盖旧证据。

先实现定向失效/冻结/不同工序/速度及边界RED→GREEN，避免只逐字段镜像断言。消费实际ClientLevel游戏tick/partialTicks和同步样本；缓存只在客户端按当前BE/世界隔离，不在服务端增tick。少量关键姿态与四向变换用真实原生矩阵/模型数据验证，不启动全量服务端或旧业务GameTest。

如纯姿态需要加载数据，加载器限当前客户端生命周期、坏数据安全待机并中文说明；纯算法测试注入同一生产JSON，不让测试触发Minecraft单例。优先无需改ClientEvents的最小加载方式，支持资源重载的实际入口有证据后再报路径扩卡；不能因为方便扩永久共享权限。

预览服务新端口4195（先确认未占用），只以普通exec托管前台会话运行并保session/PID；原4191/4192/4193/4194不启停或改源。服务只127.0.0.1固定白名单，使用本地block-model-renderer和既有Create底轴参考。模式切换/真实32～256速度/进度拖动/运行与暂停/过载及完成待机/剖视/正常窗内应可看；标明示范输入，不冒充连接用户游戏。capture包含实际资源/动作JSON身份及阶段/变换、Java和预览数据匹配。新服务路由应覆盖新增8分件、原C机壳/PNG/item及同一动画JSON，不开放任意目录。

只B持有Gradle定向测试与最终包，A/reviewer/root不重复。最终组合审查无必改、root实看后执行一次必要增量assemble --no-daemon；若审查整改需要重新测试/打包，只跑受影响并保留首次失败/原包，不能手工造JAR。核源/新增资源/class/build/JAR同源、原602包精准备份，其它载荷零漂移。测试工具/测试class不进JAR。

- [x] 动作数据/两个客户端辅助和Renderer消费，必要定向RED→GREEN。
- [x] 正式模型/PNG/动作同源预览、有限实际输入校验与负责人实看。
- [x] 原同次组合审查后唯一增量包及制品核。

## C：原组合审查者

仅写`docs/art/reports/ART-SHIELDED-ASSEMBLY-15C-REVIEW.md`。一次规格/质量组合窄审：批准动作与真实输入/客户端边界、冻结恢复/时序、原分件还原和pivot、四向变换、透明层/灯/采光/底轴不回归、Java与预览同源；读已有原始日志/身份/负责人实看证据，不重复运行测试/Gradle。打包后同次补核实际制品，不另开一轮审查。

## 交付与范围验证

必跑：本批两个新定向测试类、8新增分件/引用/真实包络与动作JSON、有限预览载荷/姿态同源、一次增量assemble、最终差异/包身份。原R1原生16模型cutout检查、原C29资源/PNG/物品/业务证据复用；不全build、旧79HTTP、旧全部模型/所有设备或服务器工序测试。

扩大仅因新增共享逻辑必要链、实际穿模/停止生命周期失败或资源加载缺陷，并在卡中精确补范围。不研究旧存档迁移、不代跑用户客户端。候选集中只请用户看两工序运行与中途停恢复，已验黑窗外观不重开；main/Git集成交主PM/用户。

- [x] root实际来源/日志/窄审/预览复看与候选入口；最终用户Minecraft动画门独立保留。

## 原生入口核实及窄补充

B已只读核锁定`BlockEntityRenderDispatcher.onResourceManagerReload`会重建renderer，Context没有ResourceManager getter。准许仅在本Renderer构造时从当前Minecraft资源经理读同一生产animation.json，纯Animation算法接Reader，加载失败绘原三待机组。资源重载产生新renderer/独立clock，不增加事件或新的同步字段。原ClientEvents仅上述一处过时数量注释窄修；注册逻辑保持。本说明不放开任何其它既有Java路径。

A实际OBB/SAT检查后采用制造工具Z轴左右±14°及向内位移≤0.25模型像素，封装另域向内≤1.5及Y轴±6°、Z=0；升降台≤1且工具活动时不升降。20～30°倾斜的真实碰撞及1.25升降脱离导柱的探索证据保留，不混用两个动作域或宣称已碰到未绘制工件。rig当前SHA为`c2a25527cfa2983f6ff246f730a43fa8d34d57d7c7333177f735845ddac056ba`。

Clock原9项RED→GREEN后，root发现新进度样本与elapsed先后错误会重复计入一段工时。B补独立到包用例RED并整改为先推进旧visual再与新progress校正，实际green-arrival XML为整改单项1/1；旧RED/9GREEN保留，不把阶段相加冒称当前整类10项新GREEN，最终需以当前两个测试类定向结果绑定新源。姿态red-pose旧日志仅328B停在准备阶段、无exit，不计真实RED。2026-10-14执行会话pending_init解除后原三位恢复，B保旧记录并仅在red-pose-recovery续该必要检查；未终止其它任务或修改主PM正在运行的reactor-load-follow进程。

最终green-client实际Clock10+Pose7共17/17、exit0；之后仅同一姿态导出方法补真实mesh角点/四向证据，两个窄导出1/1各自记录，不重复整类测试。A核实际同源28帧/26区间，原86元素及45受保护旧资源零漂移。root已实看正常窗制造/封装、夹具升降、页面暂停/过载/恢复/完成；东向取景两次失败另保，B只修预览世界中心/相机/剖面，既有Java/JSON/rig不变。原同次组合审查与唯一assemble仍待最终取景复看。

## 最终候选登记

最终scene47a60f43…按实际库mesh世界Box3/view投影适配画布，root E/S/W完整实图及北向两工序正常窗复看通过，两轮取景失败保持。原一次组合审查与同次制品补核无剩余必改；唯一assemble实际10.5092882秒exit0，新2,844,567B/SHA`dd6e25b8b04be4567f8d55d24918d75fad2c37a26ea41fb2c7675e420c6a9414`，9新资源/567当前class同源，1953旧非批载荷（含555class）保持。首核脚本漏列Renderer$1的失败保留，真实旧新opcode一致、仅调试行号50→75后精确分类，同包重读通过，不重打包。

原三位已停写，root实际读回最终CLIENT/PREVIEW/REVIEW、log/exit、final-identity及包/602备份身份，交[集中候选与启动入口](../ART-SHIELDED-ASSEMBLY-15C-CANDIDATE.md)。预览4195保留；游戏两工序及中途停恢复仍待用户，不把网页/原生/测试结果登记为Minecraft验收，main/Git/其它树/全部世界配置保持。上段“仍待取景/assemble”为此前实施时点。
