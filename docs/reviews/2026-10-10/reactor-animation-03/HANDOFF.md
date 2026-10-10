# ART-REACTOR-03：主PM候选登记

2026-10-10。主PM按美术并行授权登记已冻结的内部运行动画候选。**自动验证与一次独立规格/质量审查通过，03客户端视觉门待用户观察；02R1材质连窗门独立保留。** 源码和资源未合main，不推进装配台07或工程主线。

## 候选身份

- 工作区：`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，分支`codex/art-studio`。冻结时HEAD为`36ea83355fab672cf1fdc7010b3e448e6396eaa6`，包含主PM的L2 R2净源码同步`2bece5b`及文档同步。03消费者和既有美术输入为未提交候选，HEAD本身不包含这批美术实现。
- [客户端候选说明](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/ART-REACTOR-03-CANDIDATE.md)、[美术消费任务](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/tasks/ART-REACTOR-03B.md)、[实施报告](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/reports/ART-REACTOR-03B.md)、[独立组合审查](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/reports/ART-REACTOR-03-REVIEW.md)。登记后的纯PM文档提交不改变冻结实现身份。
- [冻结候选JAR](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-REACTOR-03B/candidate/create_nuclear_industry-0.1.0-ART-REACTOR-03B.jar)：2,473,054字节，SHA256 `7ef99a946c3be88c5b3d6f33fbc49181024c70a4360ce365624c8039e35d441f`。当前美术树`build/libs`与副本一致，后续构建可能覆盖同名制品，以冻结副本为准。
- [冻结清单](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-REACTOR-03B/frozen-manifest.json)SHA256 `6eafcbcaa90853bd63277d0669c2e5fa453607819217a88258ab77c55704efaf`；28份交付文件和35份证据，共63项。独立审查报告SHA256 `6e0823f0f8c90a8cc39e2bd48ea568605c9bb931e9b8352423a444b4165d15f3`，报告在冻结清单之外单独绑定。

本JAR来自含ART01、02R1及03素材/消费者的美术工作区，不是从干净HEAD构建的main发布包。主目录仅含已验收功能、教学及共享显示前置，不含03美术消费者。

## PM核对与复用证据

本PM实际读取消费任务、候选说明、实施及独立审查记录，复用独立审查对六个客户端类、两份专属测试与五张离线预览的审读；规格和质量均通过，没有必改项。只追加候选绑定核对，不发起第二轮代码审查。

本次实际核对结果：

- 63/63冻结文件当前SHA一致；冻结清单及独立审查报告SHA与负责人记录一致。
- 最终两份原始JUnit XML合计16项：VisualState 9、Materials 7，失败、错误、跳过均0；唯一最终`test --tests '*ReactorAnimation*Test' jar`退出0，日志记录`BUILD SUCCESSFUL in 19s`。
- 冻结JAR与当前美术树构建JAR长度和SHA一致；19项安装资源与冻结JAR条目一致。复用已审17份模型/纹理逐字节来源映射和两份mcmeta语义映射。
- 12/12共享L2源码与R2冻结一致，公共ABI保持。复用R2生命周期10/10、增量assemble及独立窄审，以及未变服务端/真实仪表证据。

PM核对证据留于主树`build/reports/art/ART-REACTOR-03B/pm-candidate-binding.json`。原始证据在美术树`build/reports/art/ART-REACTOR-03B/`。原red、green和素材保护记录保留；原1038项assets的1037项保持、唯一授权燃料包装变化、原item/CT和59份素材源保护结论复用，不重跑矩阵。

本轮实际应用minecraft-modding、minecraft-testing、minecraft-resource-pack及verification-before-completion技能，锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。技能通用流程服从项目精简验证与PM/执行者权限，不升级依赖或改变许可证。

## 显示合同与人工门

候选显示九根八边燃料管及钢板，可靠可用列按实际裂变HU/t出现蓝辉；控制棒完整杆体按实际深度整体升降，拔出部分从上方露出；合法空列和控制棒列中的透明液体按实际库存显示液位、冷热连续混色。所有显示仅消费只读投影，不改变固定5×5×5玩法、热工、库存、控制速度或碰撞。

用户在美术树已有复制世界观察，新增Java需退出旧客户端后重新启动该树；具体命令和逐项预期以客户端候选说明为准。集中看静态九管及接头、实际功率/停热蓝辉、完整控制棒行程与卡死、真实液位/混色/透窗、真暂停/恢复，以及资源重载/拆坏恢复。离线预览、GPU替身及自动测试不替代真实客户端体验。

本登记只写PM文档，未修改、暂存或提交美术源码、测试、安装资源、素材源和负责人计划，未启动客户端或改写存档。03运行视觉与02R1材质连窗分别等待用户确认；既有设备功能与教学人工门保持关闭。收到视觉反馈后按对应范围整改或净集成，不因候选登记自动推进下一台设备。

## 后续用户视觉反馈：03R1整改中

2026-10-10主PM直接只读核实美术对话中的用户三项反馈：燃料/控制棒太细、控制棒黑且缺乏细节、冷却液未正常显示。**03视觉未通过；上方原自动和独立审查结论仅对应冻结历史候选，不能作为03R1新实现的完成证据。** 原JAR、清单和报告保留，不合main。

美术负责人按[其树03R1任务](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/tasks/ART-REACTOR-03R1.md)在原六客户端类/两测试、19安装资源及专属SVG工具范围整改厚度、材质与实际采光；继续核实冷却液不可见根因，不从存档历史库存推定截图实时值，不改变世界光照、L2、服务端库存/容量/热工或碰撞。发现需共享写集时另行协调，当前未追加跨逻辑实现授权。

新的03R1定向证据、候选及用户复看另行登记；02R1材质连窗门保持独立。用户已单独要求开始装配台07教学，该教学继续实施，不因本美术整改停止或越过本台播放门。

## 03R2组合候选登记

2026-10-10。美术负责人交付0.6格方杆和原生滑块避让的组合候选，主PM只追加来源与制品绑定。**本节对应最新03R2增量；上文原03/03R1状态与原build/libs相等结论均为历史记录，不宣称当前美术树仍等于旧JAR。** 03R1既有自动/审查作为本批前置复用，其冷却液客户端视觉门仍未通过；本次不重审整套03R1、不改世界或启动客户端。

- 美术冻结HEAD `6631353d4a5cfc4d635572ae37e63f3d4de2ae33`，源码与素材仍为未提交候选。后续纯PM文档提交不改变冻结实现身份。
- [最新候选说明](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/ART-REACTOR-03R2-CANDIDATE.md)、[任务及六面坐标](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/tasks/ART-REACTOR-03R2.md)、[实施报告](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/reports/ART-REACTOR-03R2.md)、[一次独立组合审查](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/reports/ART-REACTOR-03R2-REVIEW.md)。审查规格/内部质量均通过，无必改项；复用其实际四图、151冻结、75保护和4只读依赖检查，不另派审查或重跑矩阵。
- [冻结清单](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-REACTOR-03R2/frozen-manifest.json)41交付+110证据，SHA256 `a502efc1f4843b6aedf0b1f09b3ea41f694af58af2cba02be7b853519396ef56`；独立审查报告SHA256 `0d3eb6fdbeca571cf92a070854b1d46898f8be746c90da695dfca379c369eef9`。
- [冻结JAR](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-REACTOR-03R2/candidate/create_nuclear_industry-0.1.0-ART-REACTOR-03R2.jar)，2,475,501字节，SHA256 `20b7223f899f7b75b56cdfb7d8374c7c7b629b26384add13202939ebc92e97ba`。实际与本次美术build/libs一致；两OBJ生成源/安装/JAR及三个生产class/JAR均逐字节一致，测试替身未入JAR。

PM实际读取生产Behaviour、原共享基线与来源映射，核对41/41交付SHA、候选/任务/独立报告及主PM授权SHA、13/13服务端/协议等共享保护、非几何方法逐字保持。控件显示与点击使用同一坐标和尺度，切面同步Create缓存，0..100、服务端提交、锁定、SCRAM及原BE未改。

最终专属原始XML为6项，failure/error/skip均0；确认退出0及`BUILD SUCCESSFUL in 14s`。增量JAR复用本批未变生产输出，最终确认仅重测变化的测试夹具；首三组环境失败及两组成功原证据保留，环境失败不作为业务行为red。未重跑此前18项动画/L2/协议检查或再次构建。

PM实核证据保存于主树`build/reports/art/ART-REACTOR-03R2/pm-candidate-binding.json`，包含上述直接核对及复用界限。两OBJ变化、1054非目标assets、31未变generated的字节/mtime、19SVG和旧03R1制品保护均复用冻结独立审查，不宣称PM重复执行全部矩阵。仍是含既有美术候选的工作区JAR，不是main发布包。

### 本次客户端门

退出旧Minecraft后重新启动美术树，按最新候选说明在既有测试世界复看：0.6格方杆完整升降；驱动器顶/底及四侧控件的数字、遮挡与真实鼠标点击/滚动。离线旋转、命中与包围盒检查不证明游戏文字或手感。03R2门、03R1液体显示门及02R1材质连窗门各自记录，不互相覆盖；主树未合本批美术源码，装配台07三幕播放门保持独立。

本轮应用已读Minecraft模组/测试/资源技能及完成前验证，按锁定版本与精简验证治理执行；仅登记PM文档，不改功能/测试/构建代码、美术计划或用户存档，不重复实施、构建或独立审查。

## 03R3用户反馈与共享窄修

2026-10-10用户反馈控件只在侧面、成型后无法调节，以及液体过透明且燃料没有浸泡感。主PM核实真实Create的最终框/文字位移后确认R2顶面中心内缩造成遮挡，原6/6未覆盖最终绘制深度；上文R2内部检查和冻结记录保留，不能据此关闭本次视觉门。按[03R3共享补充](../../../art/ART-REACTOR-03R3-COORDINATION.md)只续批原几何及专属测试路径；本批整改候选尚未交，不合main。液体消费由美术既有权限定向整改，不改L2、容量、实际库存或资源。
