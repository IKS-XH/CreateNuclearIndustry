# 主目录build合同测试修复

> **For agentic workers:** 用户任命的PM按`superpowers:subagent-driven-development`派发执行者。PM不编写测试代码；执行者仅交未提交改动及指定报告，禁止Git写操作。仓库精简验证规则优先。

**任务ID / 状态：** BUILD-CONTRACT-01 / 完成；R1原命令build及独立复核通过，测试修复已提交main并同步候选。
**Goal:** 主目录执行`.\gradlew.bat build`能通过现行JUnit合同并生成JAR，保留禁止路线及思索显示时序保护。
**Architecture:** 只同步两个失效的静态测试，不修改正式配方、玩法、已验收思索或构建脚本；以用户实际失败XML作为修前证据，修后仅执行一次完整build验证原命令。
**Tech Stack:** Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82；核对`gradle.properties`，不升级。
**Spec:** 用户2026-10-09在主目录build失败：370 tests、2 failures。现行已验收配方见`docs/recipes.md`及`docs/reviews/2026-10-06/store-01/ACCEPTANCE.md`；取景整改见`docs/reviews/2026-10-09/turbine-ponder-framing/ACCEPTANCE.md`，main源码/模板与`a94f2ea`一致。换热器05仍待播放，本修复不提前合入05。

## 根因与当前证据

- `P1DataContractTest`失败消息为锅炉外壳配方中的`reinforced_steel_plate`被裸字符串`steel_plate`包含匹配误判。当前禁止列表与三条白名单来自早期P1；已验收`shielded_assembly/sealed_spent_fuel_cask.json`使用`cooled_spent_fuel_assembly`作原料，旧测试也会误拦此已批准消费路线。
- `TurbinePonderContractTest`断言找`showSection(tier.add(pad...)`及`hideSection(tier,...)`，现行`showTier`实际先`showIndependentSection(tier,...)`、15tick、`moveSection(section,...)`、15tick、正文，再`hideIndependentSection(section,...)`、15tick。实现已手测验收，失效的是旧API字符串检查。
- 修前证据为主目录`build/test-results/test/TEST-com.iksxh.create_nuclear_industry.{P1DataContractTest,TurbinePonderContractTest}.xml`。改动前复制这两个XML至本任务证据目录，避免build覆盖；不再机械重跑修前失败。

## 权限、技能与写集

- 执行目录`E:/MyMC/NewMod/Create_NuclearIndustry`，`main`，派发时HEAD为`db6f354`。这是用户报告的打包入口；本次低风险测试维护直接修主目录，功能源码无未提交改动，不另建工作树。PM最后审查并提交；候选目录的同两测试文件另由PM同步，不能覆盖换热器05候选。
- 保留既有`logs/debug.log`、`logs/latest.log`及`tools/art-assets/create-style-samples-2026-10-08/`，不改启动配置、存档、依赖或美术。
- 必读`AGENTS.md`、治理5.1/5.2、本卡、现行配方与取景验收；实际使用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`及`superpowers:systematic-debugging`。
- 只允许修改`src/test/java/com/iksxh/create_nuclear_industry/P1DataContractTest.java`及`TurbinePonderContractTest.java`，所有新增/相关修改注释中文。
- 唯一报告`docs/reviews/2026-10-09/build-contract-01/IMPLEMENTATION.md`；原始输出、退出码与XML快照在`build/reports/extension/BUILD-CONTRACT-01/`。

## 必须保持的合同

1. 配方引用按解析后的完整注册ID或精确标识匹配，强化钢板及未完成强化钢板不得误当钢板；不要靠再增加设备名白名单掩盖包含匹配错误。
2. 现行三条批准生产路线继续通过并核对真实产物字段：钢板压片、新燃料组件屏蔽装配、冷态冷却剂搅拌。已批准封装仅允许冷却后乏燃料组件在灌封配方中作为输入，不能因此放行其制造配方。禁止热冷却剂制造、污染冷却剂/净化器及其他未批准的受保护生产路线；完整注册ID精确匹配同样适用于配方中的直接tag字符串。此处标签检查维持原扫描边界，不要求解析任意别名标签文件及其引用图；旧检查也未覆盖间接标签图，本批不新增这一合同。
3. 用少量必要正/反例说明完整ID匹配、合法封装消费与禁止生产之间的区别；可在原测试类内复用小辅助函数，不建立通用配方引擎或批量复制设备测试。不删/禁用测试，不改Gradle为跳过测试。
4. 汽轮机时序检查对应当前独立区段：先显示、至少原有15tick合并、移动后再正文、隐藏后15tick淡出；仍检查同一个section句柄、三档隔离和正文寿命。不要仅以“找到showIndependentSection单词”代替顺序/句柄断言，不改已验收场景去迎合旧测试。

## 精简验证与交付

- [x] 执行者核对修前两个XML、实际源码和批准合同；保存快照，修复两类测试及必要正反例。
- [x] R0只执行一次`.\gradlew.bat build`，不先重复定向test、不加clean/--rerun-tasks、不启动GameTest/客户端。用户明确报告全build失败，验证原命令是本批全量JUnit的实际触发理由。R0已370/370通过；R1因独立审查新增产物字段及相似名称反例，测试代码再次变更，执行一次最终build补验，保留R0证据，不按阶段机械重复。
- [x] 记录退出码、JUnit有效用例数/失败/跳过、制品路径/大小/SHA-256；冻结后交报告。R1为370例、0失败/错误/跳过，原命令退出码0。
- [x] PM完成独立规格/质量复核及R1定向复看，复用本次build证据；合格后提交主目录，并同步净测试差异至同级候选，保持两处合同一致。

本批只修改测试，完成后不新增客户端人工门。换热器05播放门维持待验收，不自动推进其他主线。

## PM收尾记录

修复提交main `b9af215`，同一净差异挑拣至候选`72e4fd4`；两个测试文件在两分支的Git差异为空。集成仅含两个测试及交付报告，功能、美术、配方和候选教学不改。R1独立复核通过见[报告](../../reviews/2026-10-09/build-contract-01/REVIEW.md)，实际构建证据见[实施记录](../../reviews/2026-10-09/build-contract-01/IMPLEMENTATION.md)。候选复用相同测试修复的证据，未另跑全量；不把主目录370项结果宣称为换热器05全量或人工验收。

主目录JAR为`E:/MyMC/NewMod/Create_NuclearIndustry/build/libs/create_nuclear_industry-0.1.0.jar`，2,319,066字节，SHA-256 `B428F571F316BB21C18082A849C0A672208C2503593395B380F159FAA7ABDD5B`。本任务无需客户端手测。
