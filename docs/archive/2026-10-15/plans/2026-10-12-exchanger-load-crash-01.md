# 换热器加载登记并发崩溃窄修计划

> 主PM已获自动派发授权，由一位执行者按 subagent-driven-development 完成下列Task 1；一位独立审查者合并规格/质量检查。执行者无Git/核心文档/派发权限。

**Goal：** 修复候选进入“新的世界”时 HeatExchangerBoilerBridge 的 ConcurrentModificationException，恢复存档进入路径。

**Architecture：** 本桥服务端登记与客户端生命周期隔离；弱引用集合的登记/移除/快照采用一致同步边界，复制后在锁外执行世界读取和热撤销。保留热工/负载/停tick撤销及未加载区块拒绝规则，不吞异常或跳过热审计。

**Tech Stack：** MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82。

**Spec：** 用户2026-10-12报告候选进入存档崩溃；实际 `run/crash-reports/crash-2026-10-12_08.19.33-server.txt` 和 `run/logs/latest.log`。

## 根因与不可变边界

- 栈明确为服务端Pre→auditLoadedSources:36→List.copyOf(LOADED)→WeakHashMap迭代并发修改。
- NuclearHeatExchangerBlockEntity.onLoad与suspend两侧均调用登记/移除；LOADED和ACTIVE为无同步static弱集合，客户端实例可进入服务端审计。current/canTick在客户端可为true，不能靠审计隐式排除客户端。
- registerLoaded与track(active=true)仅接受具有非空服务端Level的实体。客户端/未附世界不能进入服务端热源表；移除仍须安全且不遗留强引用。
- LOADED/ACTIVE保持弱引用语义，所有结构修改与快照复制使用相同同步边界。snapshot只复制引用，世界访问、pauseHeat/HeatExchangerLine.find/回调必须在锁外执行；重入移除安全。不得用catch ConcurrentModificationException、吞快照失败或禁用审计掩盖问题。
- PENDING已有服务端事件过滤，保持其逻辑及现有Create锅炉热缓存/输入输出/损耗/工作盆/烧结炉规则。不得扩为其他设备弱表专项、调整余额/速率或改配置。
- 不删除/迁移/改写用户世界，不新建分支；直接在已带世界的当前候选修复。无需重新复制存档，也不研究旧版本兼容。

## 工作树、技能和写集

- 唯一实现树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，分支 `codex/reactor-variable-size`，基线 `d819fb66da0b1c355a324201f4456d73406b3f4f`。原快捷换料/ART09/尺寸来源与人工门保持；主目录仅同步计划/报告，不提前合生产。
- 执行者实际读根AGENTS、此卡、minecraft-modding/testing（`C:/Users/IKSXH/.codex/skills/`），systematic-debugging、test-driven-development及verification-before-completion（`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/`）。技能升级示例不改变锁定栈。
- 生产只允许 `src/main/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerBoilerBridge.java`。新增 `src/test/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerBoilerBridgeTest.java`；优先复用已有JUnit世界夹具方式，不新增依赖/通用测试框架或生产测试专用API。若确实需要额外辅助生产路径，先报告PM。
- 执行者仅写 `docs/reviews/2026-10-12/exchanger-load-crash-01/IMPLEMENTATION.md`、本批 `.superpowers/sdd/exchanger-load-crash-01/task-1-report.md`、`build/reports/exchanger-load-crash-01/` 原证据。审查者仅同目录REVIEW。不得改任何其他代码、核心docs/计划/AGENTS/Git/美术/世界/构建；所有新增修改手写注释中文。

## Task 1：侧隔离、原子弱集合快照与定向验证

- [x] 先保存实际崩溃日志副本及源SHA。以真实生产登记/事件入口编写RED：客户端/无世界来源被拒绝，服务端可登记；原并发登记/移除与快照复制应有可观察复现，不以缺方法编译错误充作RED。强引用持有用例对象，不能将GC随机结果混作线程证据。
- [x] 实施上述最小修复。必要调用链/锁职责和客户端服务端边界写中文说明，不改变BE生命周期主体。
- [x] GREEN覆盖客户/服务侧、LOADED和ACTIVE两表、并发登记移除与snapshot、回调重入移除且不持集合锁、移除后不再审计。真正世界操作不在后台测试线程执行；并发线程只操作登记表。
- [x] 仅一次最终定向JUnit：`./gradlew.bat test --tests '*HeatExchangerBoilerBridgeTest' --tests '*HeatExchangerStateTest'`。复用既有独立真实服务端盆负载回归 `./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_ext_b_basin -PgameTestDirectory=build/gametest-exchanger-load-crash-01`；先核实际模板筛选，零用例不计通过，不改原测试/模板。此用于确认真实BE加载审计和热发布仍运行，不能称客户端进入已通过。
- [x] 最终唯一增量assemble，旧组合JAR先备份，冻结两源、受影响class/资源与当前JAR；原快捷换料/ART09功能输入身份核对保持，原证据不重复。扩大验证仅因实际新失败。
- [x] 报告原RED/GREEN命令、计数/退出码、线程复现方法及未覆盖范围、实际栈/改动、当前源/class/JAR身份；不把单元夹具或服务端测试当作用户客户端验收。
- [ ] 一次独立规格/质量审查读原证据，不重跑。PM核对净diff、提交新候选，最后请用户用同一目录重启并进入原世界，只复看本故障及原未关闭门；不推进其他主线。

## Review Focus

客户端实体不能由服务端线程读取/暂停；两弱表的所有访问与复制均走同一锁边界；world/回调不在锁内，弱键不被第二份全局强引用保存；服务端卸载/停止tick仍撤热、未加载区块不强载；不改变既有候选功能和世界。

## PM候选登记

2026-10-12：功能候选5d01314c1af38ba4665a6b1af32ea355e49770fc。30/30定向、真实Basin7/7、唯一增量assemble自然退出0，独立窄审无必改项；生产仅本桥33增/8删，原候选34输入保持，JAR仅该桥class变化。源码/JAR已核并提交，最后用户进入原世界的复看仍未通过；不关闭其他人工门，不合main生产或推进主线。当前包身份和启动见本批CANDIDATE。
