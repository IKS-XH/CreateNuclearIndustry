# 高压锅炉最大外尺寸16³窄调整计划

2026-10-15完成：用户确认尺寸上限和锅炉新外观手测通过，已按本批合同将尺寸净增量适配并提交当前main。当前验收与组合制品见[集成记录](../../../reviews/2026-10-15/boiler-size-accepted-integration/INTEGRATION.md)。下文分阶段候选和待测文字保留历史，不重新打开人工门。

> 主PM按已授权自动派发，用一位执行者完成 Task 1、一次独立规格/质量窄审。应用subagent-driven-development；执行者无Git/核心文档/子代理权限。

**Goal：** 高压锅炉三边分别允许5～16整数外尺寸，允许偶数、长方体及跨区块；配置默认和合法上限一致，超过16拒绝。

**Architecture：** 沿用现有锅炉可变结构，调整BoilerConfig.dimensionRange及BoilerState.Settings的上界/默认；扫描读取配置，水/汽分区继续按实际格数计容量，不重写扫描器、温压或美术。

**Tech Stack：** MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82。

**Spec：** 用户2026-10-12“将锅炉的尺寸限制也改为16^3”，结合原锅炉最小5及已批准可变尺寸/容积温压合同。

## 全局约束、范围和权限

- 默认dimensionRange从[5,11]改为[5,16]，两个整数须满足5≤min≤max≤16；配置仍可降低上限，提高下限。BoilerState.DEFAULT与配置默认同源或显式一致；运行Settings.valid也拒绝max17，保留long容量/阀流乘积预检。
- 不限制区块对齐，不限制仅立方体。外壳/隔层/换热器/端口现行规则保持；普通/超临界双库存、每口独立流量、有效热力回路、压力滑块及热损耗不改。
- 实际水汽格决定容量和热容，扩大几何不会凭空产生现有工质/能量。16³居中示例隔层localY=8时内截面14×14，水区7层1372格、汽区6层1176格；默认水2744000mB、汽2352000mB。隔层仍按原规则可选，不固定新高度。
- 唯一实现树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，分支codex/reactor-variable-size，生产基线 `edb5b653c51867df26a2f16030f5ed52cf1923f3`。原已验收来源不重写；PM将它们净集成main，锅炉新源独立留候选。
- 用户存档来源问题待答复时，不复制/覆盖世界。只允许候选已有 `run/config/create_nuclear_industry-boiler.toml` 的dimensionRange本次从[5,11]更新[5,16]，原配置先备份，仅该键改变，其余配置内容逐字保持；先由PM核最新保存退出日志。这是明确要求生效的候选运行配置调整，不是旧版本迁移。执行者必须等PM放行再改此运行文件。
- 实际阅读AGENTS、此卡、minecraft-modding/testing（C:/Users/IKSXH/.codex/skills/）、test-driven-development和verification-before-completion（superpowers6.4.2技能目录）。不升级栈。
- 生产写集仅 `config/BoilerConfig.java`、`boiler/BoilerState.java`；测试原 `boiler/BoilerStateTest.java` 和新增 `gametest/BoilerSizeLimitGameTests.java`，单个独立域空模板 `src/main/resources/data/create_nuclear_industry_boiler_size/structure/boiler_size_empty.nbt`（逐字复制正式空模板）。Java根为src/main/java/com/iksxh/create_nuclear_industry，JUnit根为src/test/java/com/iksxh/create_nuclear_industry。
- 扫描器/描述符的保守工程上界32无需改成玩法配置值；若实际16结构受原实现阻挡，报告PM后扩明确路径，不自行重构。历史32³传输格式测试不当成仍可合法搭建的证明，不做旧存档研究。
- 报告仅 `docs/reviews/2026-10-12/boiler-size-limit-01/IMPLEMENTATION.md`、本批 `.superpowers/sdd/boiler-size-limit-01/task-1-report.md`；证据build/reports/boiler-size-limit-01。其他docs、Git、用户世界、配方、美术、依赖和构建脚本禁止改动。

## Task 1：上界、实际结构与候选配置

- [x] RED：新增默认最大16与Settings17拒绝的行为断言，原生产上先运行；检查当前配置默认，不能用运行文件改值掩盖旧默认11。
- [x] 最小实现两个生产路径的默认和合法性，相关注释中文。原overflow用例应在合法最大16下仍证明乘积溢出拒绝，不能仅因尺寸32非法而失去原断言含义。
- [x] 独立真实服务端有限用例搭16³且跨区块、不强载，并验证Form与1372/1176格及实际容量/配置来源；补一个17尺寸拒绝。复用现有锅炉搭建法，无需多份完整夹具或改变原测试。空腔非边框/端口绑定与隔层按原规则。
- [x] 最终定向 `./gradlew.bat test --tests '*BoilerStateTest' --tests '*BoilerDualSteamInventoryTest'`；新增独立域 `./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_boiler_size -PgameTestDirectory=build/gametest-boiler-size-limit-01`。实际注册非零，保存退出自然exit0才称完整通过。
- [x] PM放行后候选运行配置备份/仅dimensionRange更新，给前后SHA/差异；不改服务器测试配置以伪造默认。最终唯一增量assemble，旧接受组合包备份；冻结来源/class/模板/JAR及旧功能保护身份，不完整build、不clean、不重复已审矩阵。
- [x] 完整报告区分代码默认、运行配置及客户端结果；一次独立窄审复用原证据不重跑。PM提交候选停在16³/17拒绝与尺寸显示/容量的本批手测门，不重复原锅炉生产线或视觉全套。

## Review Focus

两个默认一致；config与Settings均拒绝17；原整数容量/泄放预检仍有效；真实Form格数和运行配置实际生效；未动原热工/库存或误把保守传输32上界改为本批兼容任务。

## 同批发现的旧边界断言补充（2026-10-12）

首次真实2/2检查后发现ExtensionBoilerGameTests.elevenBoundaryAndOffCenterPartition仍把12格作为默认非法边界。为避免已知断言与本批默认16矛盾，额外批准原gametest/ExtensionBoilerGameTests.java仅该方法：保留11格偏心分区参考，非法宽度12改17及相关中文提示；其他方法逐字保持。在原新增BoilerSizeLimitGameTests内增一个委托该方法的隔离用例，最终独立域3/3验证。原29/29JUnit未变不重复；原2/2与首次assemble证据保留为中间候选。由于改动测试class，允许在同域最终通过后新增一次必要增量assemble，报告分别记录，禁止称整个过程只构建一次。没有增加生产写集或全量旧域验证。

## 本批存档来源确认（2026-10-12）

用户答复采用“继续用当前候选的测试存档”。沿用Create_NuclearIndustry-art-integration/run/saves/内已保存世界，本批未创建或切换新分支，不复制/覆盖/回写世界；既有备份与主目录原件保留。原“来源待答复”文字为问答前时点，实际本批来源以此确认为准。
