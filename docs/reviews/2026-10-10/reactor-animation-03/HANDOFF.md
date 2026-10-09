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
