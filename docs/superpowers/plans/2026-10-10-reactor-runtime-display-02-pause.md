# ART-REACTOR-03-L2-R2 真暂停租约整改

2026-10-10，主PM。状态：已核实根因，派发窄修；R1既有审查/自动证据保留，不重开动画或02R1视觉门。美术消费者可继续独立实现，最终候选在本次实际同步后绑定。

## 依据与目标

美术负责人报告单人真正暂停时Pre tick仍推进20tick租约。本PM实际读取本地`build/moddev/artifacts/neoforge-21.1.219-sources.jar`：`Minecraft.java`1152、1159～1161行仍调用timer及tick，1785～1789行先无条件fireClientTickPre再检查pause，2556～2557行isPaused返回实际pause；`DeltaTracker.java`61～69及88～101、114～118行只在partial路径使用paused；`ClientHooks.java`1069～1070行直接发布Pre。现有`ReactorRuntimeClientEvents.onTick`无条件调用STATE.tick，服务端真暂停不续样本，确实可令可靠显示到期。此结论是本地控制流核实，尚非客户端人工复现。

只在`Minecraft.getInstance().isPaused()==true`时停止推进L2租约时钟。仍同步当前世界、执行reconcile、发布快照，卸载/换槽、坏包、owner失效和会话切换继续立即撤销。多人菜单isPaused为false仍正常计时。暂停不续样本、不复活过期或已撤销owner；恢复后从原租约年龄继续，非暂停累计满20tick到期，不补计暂停时长。

## 写集与技术合同

- 隔离树：`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-display`，分支`codex/reactor-surface-display`，开工HEAD `18e2524a053f9ff997e66ae96085efa32f0897a3`。main `8ad6eac`、美术`ad805ec`保持旧已审L2。原L1文档与logs保留；先在专属证据目录复制当前日志，JUnit轮转后只恢复原字节、不用Git覆盖dirty日志。
- 实现只修改`src/main/java/com/iksxh/create_nuclear_industry/structure/client/ReactorRuntimeClientEvents.java`和`src/test/java/com/iksxh/create_nuclear_industry/structure/ReactorRuntimeLifecycleTest.java`。允许私有暂停计时适配方法供行为测试调用，不新增公共签名；不能只用字符串断言代替行为red。不需要直接调用或mock真实Minecraft tick。
- 保持现有公开ABI、State.tick、协议字段/服务端心跳/租约20tick预算、L1/CT、注册/碰撞/素材/模拟/构建不变。若本写集无法形成真实行为测试，先报告具体缺口，PM决定扩大范围；不能偷偷更改共享State或美术消费类。
- 实施报告只追加`docs/reviews/2026-10-10/reactor-runtime-display-02/IMPLEMENTATION.md`的R2；独立审查只追加同目录`REVIEW.md`的R2。证据独占`build/reports/art/ART-REACTOR-03-L2/R2/`，保留原R1及失败。
- 新增/修改注释中文，解释物理客户端、租约单位、暂停和撤销边界；不逐行复述。
- 实际使用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`及superpowers systematic-debugging、test-driven-development、verification-before-completion。核对MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，不套其他版本示例。

## 单一实现与验证

1. 保存源基线与日志原字节。增加最小行为red，调用生产暂停计时适配：可靠样本暂停超过20次Pre仍三索引可见；累计恢复活动tick到20仍正常过期/重复不续期；真暂停期间坏包、chunk卸载或世界切换仍撤销，不能恢复旧样本。必要样本沿用现有fixture。
2. 修复唯一计时入口；不能把onTick整个return或停reconcile。执行`./gradlew.bat test --tests '*ReactorRuntimeLifecycleTest' assemble`一次green，核对XML实际条数和0failure/error/skip及退出0。不重跑common投影、GameTest、全量或客户端。
3. 冻结两路径与报告SHA、完整既有12路径源清单、JAR及公共javap；原R1公共API文本逐字节一致，其他10源SHA不变。PM复用未改服务端/原自动证据。
4. PM一轮独立规格+质量窄审，只读本次差异/red/green/ABI证据；有新问题再派定向整改。执行者不派发、Git写入或改核心文档。
5. PM净整合并实际同步main和美术树、更新HANDOFF。只核对源/文档一致性，复用已审打包，不机械重复打包；美术最终动画视觉阶段观察真暂停/恢复，不能将本次JUnit当作客户端视觉通过。

依据既有PM自动授权修复已确认显示合同中的暂停缺陷，没有新增玩法决策。模型用高速`gpt-6.1-sol`及high核查生命周期边界；两位执行者不同时编辑源码。
