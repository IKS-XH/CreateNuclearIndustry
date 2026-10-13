# REACTOR-AUTOMATION-01R 组合规格/质量窄审

2026-10-13。接续原01唯一组合审查，审查候选 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-automation`，分支 `codex/reactor-automation-basic`，HEAD `6d593df` 及其未提交实际差异。依据主目录已批准的 `docs/superpowers/plans/2026-10-13-reactor-automation-retrofit-fix.md`、候选AGENTS、执行报告与本批原始证据；原01规范及审查边界保持。

实际使用此前已读取的 minecraft-modding/testing、requesting-code-review/code-reviewer.md、verification-before-completion，并读取 systematic-debugging。按锁定 MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 核实际入口，不升级依赖、不使用技能示例代替当前源码。审查未运行测试、Gradle、客户端或服务器，未修改生产、测试、构建、资源、世界、配置或Git；唯一写入为本REVIEW。

## 结论

无剩余Critical、Important或Minor必改项。可交本次改装接口/护目镜局部客户端复看候选；这不是本次现场手测通过，也不关闭原01集中人工门，不允许据此派发02或合入main。

## 规格与质量核对

| 检查点 | 实际源码与裁定 |
| --- | --- |
| 安全清空保留实际尺寸 | `ReactorInstrumentPortBlockEntity.java:531-543` 仅改变FULL_SHUTDOWN_RESET的nextSnapshot构造为原空状态加原实际dimensions。危险、未完全停机及仪表维护分支不变，原快照、端口物品和待迁移信封仍进入同一计划。 |
| 初始化门使用新容量 | `ReactorInstrumentPortBlockEntity.java:306-321` 先提交本次有效几何scan，才调用 `canInitializeScannedDimensions`。`:252-262` 的容量来自该新scan及原服务端配置；`:352-362` 判断冷量不超过该新派生容量，没有读取旧5³容量或截断冷量。 |
| 核状态不得借门重组 | 初始化门逐项拒绝任何燃料列、控制棒列、热液、SCRAM请求/保存目标、事故进度/启动/发布以及 `pendingLegacyFuelAssemblies`。列内燃料、余热、完整度、卡死等字段均因列必须为空而不能遗漏。Snapshot构造器保证库存非负；无库存干净状态仍可绑定另一合法尺寸。 |
| 待迁移燃料不绕过 | `ReactorInstrumentPortBlockEntity.java:1756-1780` 读取现行迁移信封到pending map，尺寸门在 `:361` 检查该map，且在 `:325` 原迁移消费之前执行。拒绝分支的clearPortBindings只解绑，不清pending map或快照。 |
| 提交/后检/回滚保持 | 原 `validateDisassemblyPlan:1067-1111`、`verifyFullShutdownReset:1135-1146` 与 `rollbackDisassemblyPlan:619-644` 按计划的前/后快照和精确端口捕获校验、恢复，不写死默认empty。生命周期 `:170-194` 仍先提交、后检、发布危险事件，再收尾安全证据；此次没有更改这些顺序。 |
| 扳手显示权威接受结果 | `ReactorStructureLifecycle.java:330-338` 保留原扫描、登记和updateStructureCache顺序，之后返回仪表实际contract/origin，保留原 `scan.blockReadCount()`。原 `ReactorInstrumentPortBlock.onWrenched` 继续使用该结果生成消息；拒绝后的更新标签摘要无效，正式tick仍由原有效性门阻止，不再只报告几何成功。 |
| 真实改装窗口 | 新GameTest `:64-69` 经真实NeoForge BreakEvent提交安全清空、物理拆壳，再在同回调Post之前调用原冷端handler的EXECUTE fill；中间没有人为rescan或清状态。换接口后才重扫，`:74-87` 核实际尺寸、冷量、全插入、更新标签、正式tick与燃烧室安全任职。 |
| 保存型/拒绝/重建代表 | 新GameTest `:99-113` 通过当前格式NBT加载新BE与正常onLoad/重扫恢复5³+32768mB；`:115-127` 十项拒绝均走真实BE读取和几何/权威重扫；`:196-214` 核拒绝结果、完整快照及实际更新标签。`:134-151` 核同一仪表的干净6→7→5重建及5³原empty清空。没有把BE代表描述为完整重启。 |
| 写集、注释和发布边界 | 仅批准的五条源码/构建/测试模板路径有本批实现变化，新增非显然分支说明为中文。原日志dirty由JUnit正常写出，未纳入实现写集。模板为原空模板的字节别名；新测试class和NBT均从发布文件载荷排除，原开发入口及旧域保持。 |

本次采用的技术取舍合理：保留原拆壳事务，只在其nextSnapshot保存dimensions；用严格无核状态及新容量门恢复纯冷液；显式扳手重扫返回所有者最终结果。没有改全局Snapshot、流体handler、事故/热工、红石或管理员控制算法，也没有追加旧版本迁移研究。

## 原始证据核对

- `01-red.log` 是零用例启动诊断，含No test functions及Gradle成功，不能计行为RED。`02-red-behavior.log`、`03-red-diagnostic.log` 是误设控制棒主体的COLUMN_LAYOUT夹具失败，亦不计根因RED。
- `04-red-corrected.log` 确为旧生产三项行为失败：有效6³经原BreakEvent和128mB补冷后变5³并被owner拒绝，当前保存型5³+32768mB拒绝，干净6→7拒绝；原几何/旧返回值与owner矛盾在日志中可见。该次所有维度自然保存完成，三项失败如实保留。
- `05-junit-green.log` 与冻结XML为18/18，零失败、错误、跳过：摘要7、可变尺寸4、变量scanner4、接口结构3。原断言未修改。
- `06-server-green.log` 三项真实代表全部通过；日志显示改装保6³/128mB、保存型保32768mB、十项拒绝返回false且冷量未变、6→7→5合法重建。三个维度自然保存并关闭，Gradle成功/20秒，未把业务通过扩大为客户端验收。
- `07-assemble.log` 为本批唯一必要1秒增量assemble，jar实际执行、其他编译/资源复用，Gradle成功。审查没有再运行任何检查批次。

## 来源与制品

五项本批source-manifest长度/SHA与当前文件实核一致；原模板与本域别名的实哈一致：`71343a478b844ae6b0044f0f2142fae309499d727cfc731d6e5bbf9f6bc104e4`。

正式 `build/libs/create_nuclear_industry-0.1.0.jar` 与冻结 `build/reports/reactor-automation-retrofit-fix/reactor-automation-retrofit-candidate.jar` 实哈一致：

`5e04c667fc509a0eb9349bb95faf6c3a15b5d19a961ffdeb29458d00b5c2d434`，3138597B。

保留的原包实哈为 `4204ab17618b6dbbfa056218d6aa3a5c1c4a460c688d233388feb56aa3dd9301`。本批冻结清单记录646已发布class及1546资源；审查实际ZIP文件计数相符。相对原包仅Instrument、Lifecycle两个生产class及Instrument `$1`/`$PortBindingSpec`两个伴随class文件载荷变化；审查只读javap核两个伴随类的实际指令相同。

“无新增/删除”严格指文件载荷。实际ZIP额外保留两个空目录占位 `data/cni_reactor_automation_retrofit/`、`data/cni_reactor_automation_retrofit/structure/`；没有本批测试class或NBT文件泄漏。这不是需要重打包的语义问题，不扩大排除规则或重复构建。

## 未裁定与人工门

Declined to judge：空列表。批准范围内没有功能被静默放弃；以下为本次没有取得人工证据的门，不能由静态审查或GameTest关闭：

- 用户现有16³世界改装后正常加载/成型、实际护目镜显示的局部复看；本次没有写入或启动该用户世界。
- 原01四控件/GPU/鼠标、真实跨区块卸载/返回、同版本完整保存重进及其余集中人工门；原01nofall保存挂起的限制保持。
- 原泵/现场流量/16³锅炉等独立门，以及02后续计划；本次不推进这些范围。
