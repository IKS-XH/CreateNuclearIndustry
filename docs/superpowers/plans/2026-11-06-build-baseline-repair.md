# 构建三项基线失败修复

用户2026-11-06明确要求先解决构建测试失败。本批目标是查明并修复已记录的两条换热器桥接测试和一条反应堆差分测试，使正常`.\gradlew.bat build`可通过；不修改玩法、公式、平衡、依赖、跳过规则或放宽原断言。

## 基线与工作区

- main起点`1b8899d543c3d8c86e483ac949508dd6e3881a30`，正式来源`0656b112`，Git内正式JAR为E0ABFC7E…6D15EF。主目录JAR工作副本已删除，3D工具有既有dirty，均先保留，不当作本次源码修改。
- 复用干净`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix`，同目录切`codex/build-baseline-20261106`，保留原位存档、不启动客户端或读写世界。原思索/美术分支及报告保留。工具清单未发现更合适的活动托管工作树，不另建目录或重复搬世界。
- 仍锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6及Gradle9.0.0。

## 职责与写集

PM维护本卡、STATUS及结果文档、管理Git，不写功能/测试/脚本。唯一执行者按本卡实施，一次独立规格与质量组合审查；执行者/审查者无Git写、核心文档、派发或人工验收权限。实际读取并使用`C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-ci-release}/SKILL.md`，按既有锁定版本和治理5.1执行。

初始只允许修改候选目录的以下测试源码：

- `src/test/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerBoilerBridgeTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/reactor/ReactorFissionDifferentialTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/reactor/ReactorTickReference.java`
- `src/test/java/com/iksxh/create_nuclear_industry/reactor/ReactorFissionReference.java`
- `build/reports/build-baseline-20261106/`内诊断/执行/制品报告、必要临时诊断工具、证据备份及正常Gradle输出；正式包仅在准确源提交后允许构建。

生产Java、build.gradle、配置、资源和其它测试先只读。若证明是实际生产缺陷或需要扩集，先交根因、最小写集与必要验证给PM，不自行修改。新改手写注释中文；不借故更换数值、加入未加载配置的生产默认值兜底、关闭测试、吞异常、跳过失败、将严格相等改为容差或直接复制被测实现取代独立参考算法。

## 执行与验收

1. 先在未改来源运行两类定向JUnit，保存日志、完整XML与输入身份；确认实际失败是否仍为记录三项。无需先跑全量。
2. 换热器测试跟踪真实pauseHeat/直列/冷凝配置读取，检查测试夹具已有默认缓存是否完整及清理是否还原；只在确证测试初始化遗漏时修夹具，不修改真实热源审计/锁顺序。
3. 反应堆以scenario0/tick4为起点定位首个不等字段及首个分歧阶段，记录精确数值和原始double位模式，区分独立参考落后于已批准算法、输入夹具错误、求和顺序和生产缓存失效。不得凭猜测定性浮点问题。保留700随机布局/1400裂变比较及6×100完整tick严格比较，控制棒运动/卡死/燃耗/维护场景不减。
4. 修复后先跑必要定向测试；唯一执行者交短`EXECUTION.md`和变更清单。审查者只读源码/日志一次组合窄审，必要整改仅查delta，不重跑同一测试。
5. 稳定来源由PM提交后，由同一执行者执行一次正常全量`.\gradlew.bat build --console=plain`；本批明确修复构建出口，故全量JUnit有实际触发理由，不加clean、rerun或test排除。保留计数/跳过原因、exit与任务状态。若新增失败先报告，不自动扩改无关模块。
6. 记录已提交源、构建前后源未漂移、JAR大小/SHA与实际生产载荷差异。若仅测试改动，生产源码保持，正式包预计与原已验来源等价；构建字节不同须定位，不能泛用旧换行差异豁免。保留旧正式包。准确源与证据审查通过后PM按已授权缺陷修复接main，再在主目录运行正常build验证用户入口，若缓存命中明确记录，不强制重跑。只在对应已提交来源并核验后更新跟踪JAR。
7. 仅测试夹具/独立参考修正且生产字节不变时，不设置无意义客户端手测门；若确需生产行为变化，PM另定受影响回归及人工门。本批不自动推进整线观察、值班长、压力或旧存档兼容。

结果和未闭门统一由PM在STATUS维护。报告只说明自动验证，不替代任何未报告的客户端/多人验收。
