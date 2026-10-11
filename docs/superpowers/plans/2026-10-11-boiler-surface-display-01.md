# 锅炉只读表面显示接口01实施计划

> 执行方式：按项目PM自动派发权限使用`superpowers:subagent-driven-development`，实现与独立窄审分开。执行者不写Git或核心文档，不派发其他执行者。

**状态：** 已核用户在美术对话直接批准A方案，必要共享前置已明确；实施派发中。任务ID：`ART-BOILER-05-L1`。

**目标：** 让美术按真实成型锅炉边界、归属和再热隔层绘制连接外壳/窗面，保持玩法不变。

**架构：** 纯投影现有已验证Form，紧凑只读客户端信封，锅炉专属不可变快照/生命周期；复用反应堆L1模式与已有AT，不泛化或修改原L1。

**技术栈：** MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。

**合同：** [ART-BOILER-05-L1-INTERFACE](../../art/ART-BOILER-05-L1-INTERFACE.md)。原用户证据、API、单位、生命周期和预算以该合同为准；不是新玩法或再次选稿。

## 工作区与职责

复用`E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，源码已与main `8cef13c`一致；PM同步后源码基线`5472036c76971925ffe7b128a113299cb3c1cbb5`。随后同步本卡与合同的文档提交，实际实施HEAD在派发消息中记录。主目录及美术树只读，独立美术消费者/素材仍由其负责人主持。PM仅合同、证据审核和Git，不写实现/测试代码。

按用户存档携带规则，客户端退出后已复制主目录`run/saves/新的世界`到复用树，27源文件逐一一致，来源不变；原候选27文件完整备份在`run/save-backups/2026-10-11-before-boiler-surface-copy/新的世界`。清单在`build/reports/maintenance/2026-10-11-boiler-surface-world-copy/manifest.json`。本批不启动用户客户端或再改存档。

## 精确写集

Java根`src/main/java/com/iksxh/create_nuclear_industry/`：

1. 修改`boiler/BoilerControllerBlockEntity.java`，仅Form投影缓存决策、独立撤销、clientPacket字段及加载/移除通知；原tick/热账本/端口/压力语义保持。
2. 新增`boiler/BoilerSurfaceDescriptor.java`、`BoilerSurfaceDescriptorFactory.java`、`BoilerSurfaceSyncEvents.java`。
3. 新增`boiler/client/BoilerSurfaceSnapshot.java`、`BoilerSurfaceSnapshots.java`、`BoilerSurfaceClientEvents.java`。
4. 新增`gametest/BoilerSurfaceDisplayGameTests.java`，专属holder `boiler_surface_display_probe`，复用同包已有`ExtensionBoilerGameTests.build/owner`，不改旧测试类。
5. 新增`src/main/resources/data/boiler_surface_display_probe/structure/boiler_empty.nbt`，只作本探针空模板，不包含用户世界或美术。
6. 新增测试根`src/test/java/com/iksxh/create_nuclear_industry/boiler/BoilerSurfaceDescriptorTest.java`、`BoilerSurfaceSnapshotTest.java`、`BoilerSurfaceLifecycleTest.java`。

以上12个交付路径；`BoilerStructure`、Port/Window、Block、注册、配置/BoilerState、管路、反应堆L1/L2/CT、AT/构建、美术资源和其他教学只读。若发现必须扩大写集，先报告，不擅自改动。所有新增/修改手写注释中文，职责、单位、线程边界和非显然分支必须准确。

执行报告仅`docs/reviews/2026-10-11/boiler-surface-display-01/IMPLEMENTATION.md`，审查仅同目录`REVIEW.md`；原始证据仅`build/reports/art/ART-BOILER-05-L1/`。PM维护最终HANDOFF与核心文档。

## 任务一：信封、纯Form投影及控制器同步

- [ ] 读取AGENTS、本卡、合同、治理5.1/5.2及Minecraft modding/testing技能；核实实际基线及无越界改动。
- [ ] 先建立有意义的定向RED，覆盖显示信封/投影与撤销缺失；编译失败不能替代行为结果，测试不要只镜像字段或源字符串。
- [ ] 实现完整外表面纯投影：5³、6×6×8偏心隔层、11³、32³；六面棱角、多面底边热口、窗/阀/底机、唯一owner与不可变副本。
- [ ] 实现schema1紧凑ID数组，实际最大NBT≤16KiB，坏类型/schema/长短数组/非法索引/隔层/边界/owner拒绝。
- [ ] 接入已决定cachedForm及invalidateForm，不新增inspect或发包时扫描，不写持久化/portable数据。首包早于客户端onLoad不能被invalidateForm清掉。

## 任务二：不可变快照与生命周期

- [ ] 实现合同消费签名及Member访问器；公共BE不引用客户端类。未知包装/Ponder/旧世界安全为空，复用原RenderChunkRegion AT。
- [ ] 定向行为测试旧revision、generation重放、旧实例remove、坏包、水位保留、成员与owner卸载两序、可靠恢复、跨区块/世界切换、历史快照不可变和刷新去重。
- [ ] 生命周期仅核已知区块实例，不强载/读块/扫描；先发布再刷新旧/新可靠范围。同值遥测不触发重复索引和CT刷新，不新增heartbeat或运行租约。

## 任务三：唯一探针、冻结与独立窄审

- [ ] 一轮定向`test --tests '*BoilerSurface*Test' --tests '*BoilerStateTest' assemble --console=plain`；原始XML明确测试数与失败/错误/跳过、退出码。只对继续改动或实际失败复验受影响范围，不clean/rerun/build重复执行。
- [ ] 仅一次隔离真实控制器探针：`runGameTestServer -PgameTestNamespace=boiler_surface_display_probe -PgameTestDirectory=run/boiler_surface_display_probe --console=plain`。搭6×6×8偏心隔层，核实际update tag/packet、正常遥测不增revision、拆窗/恢复、非法锅炉配置/恢复、新owner代次及服务器/portable无显示字段；配置finally恢复。
- [ ] 实际断言通过后若发生已知保存挂起，保留原始通过/退出证据并报告，不为相同退出问题重复业务测试，不终止用户进程。
- [ ] 交实际编译ABI、12源SHA、对应class/模板与JAR绑定、必要保护路径差异清单及短报告。复用原L1/锅炉/教学证据，不重建全仓冻结矩阵。
- [ ] PM安排一次独立规格/质量窄审，核真实语义/API/证据，不重复运行已通过测试或构建。若有必改项，仅整改与复审对应差异。
- [ ] PM净集成共享12路径，编写实物HANDOFF并同步美术树。该前置的自动/真实同步验证不等于美术CT或游戏视觉通过，不启动其他工程主线。

## 重点复核

合法结构下红石/待液/满罐/member_conflict不能变成单块；配置只影响原成型决策。底部换热器与顶阀不可从完整成员漏掉。32格边界错位最多9区块，刷新边界外扩后须有界去重。客户端卸载suspend与真remove退休必须区分；已有首包不能被onLoad撤销。唯一owner/同炉同面/局部ID消费条件明确交给美术，不在本卡实现消费者。
