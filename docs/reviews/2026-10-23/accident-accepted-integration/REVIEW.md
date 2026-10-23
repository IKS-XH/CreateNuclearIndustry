# 已验事故链与 R1 集成并集窄审

2026-10-23。**未发现本次并集新增的未闭 P1/P2 阻断。** 既有事故、创造拆除及 R1 证据可按下述身份复用；全量 JUnit 仍有 3 项已复现的 main 基线失败。本报告不代替 PM 验收或后续 JAR 身份核验。

## 范围与来源

审查树为 `E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration`，分支 `codex/accident-accepted-20261023`；基准 `cbd28c420ee2715cc024d8bed74399dfa64128ed`，待合来源 `c2b809154eec28efc7ca4069fa338b22815b9024`。该来源包含 d839bc7e 事故账本、接口退出、空口重绑、04A/04B、1985d1b3 创造拆除窄修及 R1。审查限于本次并集和实际交接差异，没有重开整条事故链或原美术审查。

已读主工程 AGENTS、治理 5.1/5.2/6、STATUS、最新集成卡，原事故/接口退出/空口/04A/04B（含三项 P2 最终关闭段）/创造拆除与 R1 报告，以及本目录 EXECUTION、SOURCE、路径、来源和原始日志记录。实际应用 `C:/Users/IKSXH/.codex/skills/` 下的 `minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md`、`minecraft-ci-release/SKILL.md` 核服务端边界、证据分层、资源并集及来源；实际 Gradle 仍锁定 MC 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。

## 并集判断

- `input-origins.json` 覆盖 2407 项输入；相对 main 净 78 路径为 42 生产 Java、29 资源、6 测试和 build.gradle。71 项沿事故来源，6 项自动并集，1 项在并集上删除两条失效调用；2202 项共享、127 项保 main。没有额外手工功能改写，P1DataContractTest 未修改。
- 已核七个共享文件及最终源码差异。仪表的手工差异严格只有删除 `lockForAccident`、`endAutomationTakeover` 内各一条 `setAutomationLoadFollowSession(null)`；字段、服务及应力网机制已随 main 删除，移除调用不丢失当前仍存在的清理职责。原热票据失效、控制会话撤销、运行数据清理及许可通知保留。
- 管理员仍只有统一调棒/恒功率两种业务模式，保留协议 5、核热值班、保护和续接。零接口退出仍要求当前真实仪表、有效且已加载结构、正式零接口及非正常停服；pending 分支先撤加载锁并采真实红石输入，再发布权限。事故 `accidentId` 仍使结构无效、扫描早退并阻断核 tick，退出入口不能解除事故托管锁。未恢复已取消闭环。
- 累计管网/汽轮机/直列优化保持；原 78 项防护服/辐射计输入保持 main 内容。MIT 的 LICENSE、NOTICE 及第三方 Create 许可输入保持，build.gradle 保原三项许可打包声明和既有排除，仅追加事故 GameTest 类及模板域排除；未改依赖、任务结构或发布方式。
- 双语只新增事故词条及沿候选将封装桶名称改为“封装核废料桶”/“Sealed Nuclear Waste Cask”，其余 main 键值保持。atlas 保原六项并新增熔融物 still/flow；mixins 保主线条目（含 TurbinePaletteRevisionMixin）并加入四项事故守卫，没有覆盖主线注册。
- 事故包 18 个源文件与已验候选内容一致，包含创造模式最终取消/权限检查后才清真实载荷的窄修；生存/环境仍沿原单次回收链。R1 的 17 个允许生产路径全部内容一致：8 项原字节相同、9 项仅 LF/CRLF 差异；16 项处理资源与当前源绑定，两个客户端 class 与原已审 R1 字节一致。未重画、重导或引入新素材/玩法。

上述来源复用以 `SOURCE.json`、`INVENTORY.json`、`manual-source.diff`、`r1-identity.json` 为索引；输入清单 SHA256 为 `0589c044a5104bfedf47e6b46bd27b10c832a75101ef011fa7b28a08cd625db7`。

## 已有验证与明确限制

| 本轮证据 | 实际结果 |
| --- | --- |
| `01-compile.log` | 两处旧闭环 API 编译失败，exit 1；原失败保留 |
| `02-retrofit-green.log` | 修正后增量编译/资源处理及真实域 7/7 业务，0 SKIP；Java/Gradle exit 0，全部维度正常保存 |
| `03-junit.log`、`junit-original/` | 唯一全 JUnit：854 项，840 通过、3 失败、11 SKIP，exit 1；不是全量通过 |
| `04-main-baseline.log`、`MAIN-BASELINE.json` | 未合事故 main `aa23d6921297a843522804f0ec4f21b5e744d75e` 两类定向共 15 项、同样 3 失败，exit 1；测试名和完整异常 message 与集成结果一致 |

三项基线失败为 `HeatExchangerBoilerBridgeTest.pauseCallbackCanReenterRemovalWithoutHoldingEitherRegistryLock`、`serverLoadedSourceIsPausedAndUnregisterStopsFurtherAudit`（配置尚未加载），及 `ReactorFissionDifferentialTest.evolvingTicksMatchAllStagesAcrossRodMotionScramJamDepletionAndMaintenance`（scenario 0/tick 4 全状态差分不等）。本次证据证明同失败在原 main 存在，没有证明或修复裂变差分根因，也没有放宽断言、隐藏失败或扩修无关算法。

事故本体/残骸保持来源，原 04B 长冷却/守恒/保存、创造拆除 RED/GREEN、R1 像素/动画/预览及用户 2026-10-23 联合手测反馈按原范围复用。未补记物理卸载、真人多人或所有光影环境通过，不新增视觉门或旧存档兼容门。

世界携带记录为 29 文件、13367770 bytes，源前后及目标身份一致且未打开；原目标世界完整备份。主工程无关 dirty 保留。现有正式包 EF3C 身份未替换，828 个开发编译 class 不冒充最终包清单；本报告不审未来 JAR。

审查者未运行测试、构建、生成器、客户端或用户世界，无 Git 写操作；唯一写入为本 REVIEW.md。
