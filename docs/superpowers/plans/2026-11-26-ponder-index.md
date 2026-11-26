# 0.2.0 思索索引入口

用户于 2026-11-26 报告本模组的思索在 Create 索引中无入口。现有客户端插件注册了 25 个故事板，但未注册列入索引的 Ponder Tag。Ponder 1.0.82 的分类页读取 `getListedTags()`，因此为本模组新增一个可浏览分类，覆盖现有教学。

- 当前候选分支 `codex/0.2.0-spent-fuel-recovery` 的 `58d9b9d6` 为基准，工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix`，沿用原位存档。保持已冻结的 0.1.0 正式包及早期 0.2.0 快照。依赖锁定 MC 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。
- 使用本命名空间 `nuclear_industry` 分类，名称「机械动力：核工业」／「Create: Nuclear Industry」。调用 Ponder `registerTags`、`addToIndex`，沿用已有 Logo 物品作为分类图标，主条目仍从有教学的真实设备里选择。清单使用离心机、烧结炉、屏蔽装配台、反应堆仪表端口、反应堆自动化接口、换热器、锅炉控制器、汽轮机控制器、干式贮存架、辐射计十个代表入口，入口合集覆盖现有全部 25 幕。保持既有按 W 指向和场景顺序，避免将同一多方块教学的所有部件塞入分类。沿用原版分类界面。
- 仅改 `src/main/java/com/iksxh/create_nuclear_industry/ponder/P1PonderPlugin.java`、`content/ModItems.java` 相关注释、`src/main/resources/assets/create_nuclear_industry/lang/{zh_cn,en_us}.json` 及 `src/test/java/com/iksxh/create_nuclear_industry/PonderCurrentFeaturesSyncContractTest.java`。不新增场景、模型或玩法。代码与测试由一名执行者，PM 管核心文档、冻结和 Git。
- 开工读 AGENTS、治理 5.1/6、STATUS 和本卡，实际使用 `C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-resource-pack,minecraft-ci-release}/SKILL.md`，以本地锁定依赖源码核对索引与本地化行为。定向测试验证可见分类、代表入口确有故事、全部故事覆盖和双语键。客户端索引视觉/点击随本批集中手测，必要时可在隔离客户端冒烟；无需启动服务端 GameTest。提交冻结后一次增量 `assemble`，核包内容和摘要。报告落 `build/reports/ponder-index-20261126/`。执行者不可 Git 写操作或打开用户世界。
