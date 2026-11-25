# 0.2.0 生物辐射与铀料浆无限抽取

用户在首个公开 Alpha 后要求所有生物受辐射、铀料浆支持软管滑轮的万格无限抽取，以及设计枯竭组件回收铅锡钢的方案。本卡实现前两项。回收方案独立讨论，实施范围以用户确认的方案为准。

- 基准 `main` 为 `a473f116`，候选位于 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix`，同目录切到 `codex/0.2.0-radiation-slurry`。沿用原位世界，自动验证使用隔离 GameTest 目录。
- 本批按用户版本规则定为 `0.2.0`。锁定 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。开工读 AGENTS、治理 5.1 与 6、STATUS、本卡和 `C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-resource-pack,minecraft-ci-release}/SKILL.md`。
- 唯一执行者写以下范围，PM 管文档和 Git，独立审查合并规格和质量。实验和交付报告放 `build/reports/radiation-slurry-20261125/`。源码确认后用增量打包交付 0.2.0 手测候选。人工验收记录与公开发布按现行治理。

允许写集：

- `src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationServerEvents.java`、`RadiationSourceIndex.java`，必要时同目录增加一个非玩家结算类。现有剂量、场强、装备、持久化及客户端测量入口优先复用。
- `src/main/java/com/iksxh/create_nuclear_industry/content/RadiationContent.java` 允许接入一个 NeoForge 实体剂量附件注册。实体附件只序列化非零剂量及计时，在线调度状态是内存数据。现有玩家存储格式不变。
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/RadiationFoundationGameTests.java`，追加覆盖非玩家真实剂量和软管滑轮判定的定向测试。使用既有测试域和排除规则，沿用现有 `src/test/java/com/iksxh/create_nuclear_industry/radiation/` 内回归，原则上无需新增单元测试。必要真实夹具模板仅置于已排除的 `src/main/resources/data/cni_radiation_foundation/structure/`。
- 新增 `src/main/resources/data/create/tags/fluid/bottomless/allow.json`，追加 `create_nuclear_industry:uranium_slurry` 和其 `uranium_slurry_flowing` 流动态，`replace` 为 `false`，保留水和熔岩的原有标签。阈值、距离和策略由 Create 服务端现有配置决定。默认策略 `ALLOW_BY_TAG`，阈值 10000。
- `gradle.properties` 的 `mod_version` 和 `src/main/resources/META-INF/neoforge.mods.toml` 的模组 `version` 同步至 `0.2.0`。依赖、配方、流体注册、伤害来源、资产、构建脚本与其他测试保持现行范围。

辐射行为：

- 参与服务端实体 tick 的非玩家 `LivingEntity` 与玩家使用同一套场强、距离/屏蔽、对应穿戴槽防护、剂量累积、恢复、短时症状和高剂量伤害。包含动物、村民、敌对生物、亡灵与模组生物。伤害和药水效果继续经原版接口，保留实体本身的生命状态、无敌及特殊免疫。创造/旁观、在线玩家结算、消息与仪表网络按原规则。
- 扩展的是受照对象，辐射源种类按现有系统。实体的非零剂量与恢复/伤害进度随其 NBT 保存。外部辐射源移除或屏蔽后继续正常恢复至零。卸载或移除清理临时排队/采样工作，已加载的生物可以在没有玩家靠近时受照。
- 采用固定每维度采样开工预算与分散/可续接调度。现有玩家采样优先，复用维度的射线、栈、方块实体预算。避免每只生物每 tick 扫描附近所有区块/库存，也避免每次实体 tick 给零剂量生物分配 NBT。非玩家采样用独立身份/取消边界，保留失败重试与公平性。预算未完成不当作安全样本，禁止强加载区块。保存停止/移动时避免重复应用剂量或陈旧样本。

必要验证：

- 跑辐射定向 JUnit。通过真实服务端 GameTest 覆盖靠近源的生物实际累积剂量，确认屏蔽/远处安全、既有剂量触发真实伤害与症状、离开后恢复、原玩家行为保持。至少检查一种动物和一种非动物生物，检查正常实体 NBT 保存载入与无玩家的服务器调度。若旧域已有环境条件或已知无关失败，分别记录。
- 软管验证优先在真实 Create 对象或隔离 GameTest 里核实标签白名单和无限判定，若用缩小的 Create 配置阈值验证同一分支，保留默认 10000 的独立证据与最终实景人工项目。优先使用现有定向测试入口，不需要手工放置 10000 格来证明 JSON 已打包。构建后检查 JAR 的实际标签、元数据、无开发探针泄漏和 SHA。
- 候选手测重点是乏燃料附近动物和怪物积累剂量、屏蔽与撤离恢复、创造玩家仪表及保护规则，以及在正常池塘与万格连通液池对比软管滑轮抽取。
