# AUTO-REMOVE-LOAD-20261020：移除管理员应力余量闭环

- 需求：用户2026-10-20明确要求“反应堆管理员的应力闭环控制功能也在主分支里去掉吧”。沿用已批准的自动派发方式，执行完整删除，不新增电站值班长、机组投退或替代闭环。
- 基准：main `222930f005a797df6a48e16bd34542b425492804`；复用同级隔离树`E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration`，分支`codex/remove-reactor-load-follow`。main原45项tracked状态及未跟踪路径已记录，禁止覆盖夹带。
- 工作区：已核main保存退出、无Minecraft进程，session.lock可独占读取；携带完整main世界27文件/13337721 bytes、哈希0差异，目标原世界整份备份，不混合/回写/入Git。
- 唯一目标：玩家不再能选择应力余量模式、绑定应力表或设置PI/余量/最低功率参数；服务端无该调节和占网路径。三页GUI、统一深度、恒功率、核热值班、故障停堆、正常保存续接保留。锅炉汽口修复和最新累计优化不回退。
- 服务端边界：移除相关模式/阶段、会话/绑定事件、PI与测量、配置、NBT投影和协议字段及客户端控制。无效或已移除模式不能通过网络解码的截断/默认值被提升为可运行模式；沿用或补齐通用非法输入安全拒绝，不设计历史格式迁移。
- 执行者：`/root/optimization_integration`。第一步只读盘点精确写集和受影响测试；PM补列允许路径后才编码。执行者不得修改核心文档、任务状态、Git或派发子Agent。
- 技能：实际读取并应用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md`、`minecraft-ci-release/SKILL.md`，版本仍锁MC1.21.1/Java21/NF21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。
- 验证：运行管理员保留行为与删除边界相关JUnit；现有必要真实服务端管理员场景用于启动/停止/保护/续接，不重跑闭环专属场景和无关全量。删掉只测试被废弃功能的测试；保留功能测试不得以删断言获得通过。语言JSON及GUI字段/引用校验。最终准确生产来源提交后一次增量assemble并绑定全部输入、class、资源与正式JAR；不clean/强制重跑。
- 审查：一轮组合规格/质量窄审，读日志不重复执行测试；发现问题回同一执行者整改。GUI视觉与真人操作未观察时单独留门，不伪称手测通过；用户已明确要求直接移除main，不据此新增重复确认。
- 中文注释及文档：手写注释保持中文；PM同步现行管理员合同、配置、策划、路线图、STATUS与值班长构想中的当前边界，历史交付/验收记录不改写。
- 允许报告/本轮检查工具：隔离树`build/reports/reactor-load-follow-removal-20261020/`；PM维护本卡及`docs/reviews/2026-10-20/reactor-load-follow-removal/`。
- Git/制品：只有PM操作；仅本批源提交后核验、更新被跟踪正式JAR，再窄范围合main。保留来源、世界和无关dirty，不推送、不发布。

## 精确允许写集

PM已核只读盘点，授权以下32项，不扩展其它实现。修改路径相对`src/main/java/com/iksxh/create_nuclear_industry/`：

```text
automation/ReactorAutomationState.java
automation/ReactorAutomationController.java
automation/ReactorAutomationService.java
automation/ReactorAutomationResume.java
automation/ReactorAutomationPortBlockEntity.java
automation/ReactorAutomationNetwork.java
automation/ReactorAutomationGoggles.java
automation/client/ReactorAutomationClientEvents.java
automation/client/ReactorAdministratorScreen.java
automation/client/ReactorAdministratorLayout.java
automation/gui/ReactorAdministratorCommands.java
automation/gui/ReactorAdministratorProjection.java
blockentity/ReactorInstrumentPortBlockEntity.java
config/ReactorAutomationConfig.java
```

删除路径相对同一Java根：

```text
automation/ReactorLoadFollowParameters.java
automation/ReactorLoadFollowSession.java
automation/ReactorStressGaugeLink.java
automation/ReactorStressGaugeService.java
automation/ReactorStressPiController.java
automation/ReactorStressSample.java
gametest/ReactorLoadFollowGameTests.java
```

JUnit路径相对`src/test/java/com/iksxh/create_nuclear_industry/automation/`：修改`ReactorAdministratorCommandsTest.java`、`ReactorAdministratorLayoutTest.java`、`ReactorAutomationResumeTest.java`，保留原断言并补实际协议解码非法模式、两模式草稿及当前格式续接；删除`ReactorLoadFollowControlsTest.java`、`ReactorLoadFollowLifecycleTest.java`、`ReactorStressGaugeBindingTest.java`、`ReactorStressPiControllerTest.java`四个被废弃功能专属测试。

其它修改仅双语`src/main/resources/assets/create_nuclear_industry/lang/{zh_cn,en_us}.json`的闭环专用键和`build.gradle`的被删除测试/模板两条exclude；删除`src/main/resources/data/cni_reactor_load_follow/structure/load_follow_empty.nbt`。管理员协议由4改5，避免两端按不同字段布局读包；这是本次协议维护，不实施旧版兼容。

本批必跑：管理员包保留9类JUnit；共享一次现有管理员/启动/续接三个定向真实域（预计19项，以实际发现清单为准），使用独立开发测试目录，先核实际NeoForge筛选入口；双语JSON/引用与删除类/模板无残留核验。针对删除边界先获得实际RED，再实施、运行最终定向GREEN；不以源码字符串检查替代网络编解码验证。生产源提交后一次增量assemble及制品绑定，测试由唯一执行者持有。进度与未闭门只在STATUS维护。
