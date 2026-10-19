# AUTO-REMOVE-LOAD-20261020 规格与质量组合审查

2026-10-20；审查者 `/root/optimization_review`。只写本报告，不改生产、测试、核心文档或Git；没有派子Agent或重复运行测试。本报告是审查交付，最终验收和main集成由PM记录。

## 结论与来源

本批未发现可证实的P1/P2回归、未完成删除或未授权实现范围。32项准确来源及定向验证、一次增量assemble和完整制品绑定已核，可按用户明确授权继续窄范围集成；客户端现场观察仍独立记录。

- 用户需求为删除管理员应力闭环，不实施值班长或替代控制。
- 基准：`222930f005a797df6a48e16bd34542b425492804`。
- 来源：`2561b1d1021d4ccf52db2eabab44e1648dd16623`，20修改、12删除，共32项，+122/-982。已读完整实现差异、[SOURCE](SOURCE.json)、[范围/验证核对](validation-summary.json)及活动卡；写集外src原始哈希无变化。
- 实际读取并应用AGENTS、治理5.1、STATUS、本批活动卡，以及 `C:/Users/IKSXH/.codex/skills/` 下 minecraft-modding、minecraft-testing、minecraft-resource-pack、minecraft-ci-release 的SKILL.md。沿用已读verification-before-completion核实际日志/差异，遵守治理5.1不重复业务测试。
- 实际Gradle保持MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。

## 合同核对

| 边界 | 审查结果 |
| --- | --- |
| 功能删除 | LOAD_FOLLOW及LOAD_WARMUP、6个闭环专用源、PI/应力采样/同网占用、接口表坐标、仪表暂态会话及服务调节/重绑入口全部撤下。4个专属JUnit、1个专属GameTest和模板删除，保留功能测试仅增加断言。 |
| 网络与非法输入 | 协议4→5，仅保留Input/Reply，Bind包及客户端Ctrl事件类删除。Settings/Draft仅模式/棒深/毛HU目标，真实CODEC不再读写余量和最低功率。删除值3及越界值解成null，由完整草稿校验整次拒绝，没有截断成MANUAL/CONSTANT。STOP/RESET/START独立动作与菜单身份、版本和序列校验保留。 |
| NBT与续接 | 端口未知保存模式进入STOP，START明确拒绝STOP。Resume严格读取13字段当前信封，模式越界或Phase.valueOf失败均返回无session、无运行许可；不做旧格式迁移。当前MANUAL/CONSTANT目标与调节进度可作为无许可意图恢复，仍须当前实例、低沿、冷却/库存和真实首付重新核验。 |
| GUI/投影 | 两种业务模式和三页布局保留；余量/最低值/表状态编辑与展示撤下。已从Projection.from追到ReactorAutomationGoggles.view全部helper写入，无ReserveTarget、MinimumHeat、Gauge、StressValid/StressReserve或LoadingRebind续写。客户端默认编辑草稿不改变服务端STOP偏好或授予运行许可。 |
| 保留行为 | 统一深度、恒功率选档、手动逐棒、覆盖/卡死/冷却/急停与故障锁存、核热真实付款及正常停服续接链保留；正常停服事件仍由ReactorStructureLifecycle/Resume持有。未更改核结算、库存、锅炉汽口或T2～H1优化。 |
| 配置/语言/打包 | 7项闭环配置及对应读取删除，核热维护和其它管理员参数保持。双语各精确删除同一25键，其余键值不变；生产引用无残留。build.gradle仅删已删除专属测试和模板的两条exclude。手写新注释为中文。 |

[残留检查](residue-check.json)使用包含被忽略源码的实际入口，生产源码、资源及构建规则无闭环类型/字段引用；无相邻玩法或历史归档修改。

## 验证证据

已读原始RED/GREEN日志、JUnit汇总和真实服日志，未只依赖执行者结论：

- [RED-codec.log](E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration/build/reports/reactor-load-follow-removal-20261020/RED-codec.log)：新增断言实际经过RegistryFriendlyByteBuf和Input.CODEC，旧模式值3仍被接受为运行草稿，拒绝断言按预期失败。此前ByteBuf包装编译失败单独保留，不当作有效RED。
- [GREEN-junit.log](E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration/build/reports/reactor-load-follow-removal-20261020/GREEN-junit.log)及[汇总](validation-summary.json)：9类48/48，0失败、错误或跳过，exit0。新增检查覆盖非法网络值、两模式完整负载消费、草稿同步/编辑保护、当前格式两模式续接及非法保存模式/阶段不授许可。原保留功能断言未删减。
- [筛选依据](gametest-selection.json)核锁定NeoForge GameTestHooks的实际命名空间入口。一次共享运行管理员6项、启动2项、续接11项，共[19/19](gametest-summary.json)；[原始日志](E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration/build/reports/reactor-load-follow-removal-20261020/gametest.log)确认19项完成、正常保存与关闭、Gradle和进程exit0。没有运行闭环专属域或无关全量。
- 隔离测试使用报告目录内新game世界，未使用携带的用户世界。首次新目录缺server.properties初始化提示、既有Mixin/命令提示均保留。测试自动通过、进程退出和人工操作分别记录。

客户端GUI视觉、两模式/三页和Ctrl/普通扳手交互未新增现场观察；不能把布局/引用与服务端断言写成人工通过。用户明确要求此次main移除，该独立观察不成为再次确认门。原有无关失败/未闭门保持历史语境，不重开或补写全量验收。

## 准确来源与制品

已读[BUILD](BUILD.md)、[ARTIFACT](ARTIFACT.json)、输入前后清单、唯一assemble日志和载荷差异：

- 已提交来源 `2561b1d1021d4ccf52db2eabab44e1648dd16623`；唯一一次 `gradlew.bat assemble --console=plain`，exit0、1秒。仅jar执行，编译/资源3项UP-TO-DATE，复用本轮已测同一实现；无clean、强制重跑、重复测试或候选重封。
- 2282项输入与准确提交及冻结验证来源一致，构建前后原始哈希保持，无额外未跟踪生产输入。
- assemble原输出为 **3523047 bytes**，SHA256 **`74e95f6dcd024802f21b25a7028ebb067ff3fd83ab21d8e90b6a4e79eadd4090`**。
- 全2450项载荷已绑定：762 class逐字对应编译输出并回指已提交Java，1687处理资源对应输出及源，manifest对应jar任务。
- 相对基线88f21a64…e51bbb，仅16个闭环/Bind/WorldBinding class删除、27个class修改（全部归属14个允许生产Java）及两份双语资源各删25键；无新增载荷。其它735 class、1685资源及manifest逐字相同，锅炉汽口与最新优化载荷保持。
- 旧类型、阶段、Bind包、WorldBinding和模板条目/常量池符号残留为0；专属GameTest及模板本来未进入正式包，删除源码与exclude后未重新混入。

本报告不执行正式JAR跟踪或main操作。隔离树自身两份运行日志的保留/恢复、main原45项tracked及未跟踪状态保护，由PM在最终集成记录另核；不把测试日志副作用当作生产写集。
