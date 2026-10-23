# ACCIDENT-ACCEPTED-01：已验事故功能与R1美术合入

用户2026-10-23在联合交付后明确反馈“测试过了，合并进去吧”。该反馈覆盖本次外观及创造拆除联合候选门；沿已反馈的接口解锁、拆除、04A/04B手测范围验收，不扩大为物理卸载、真人多人或所有光影环境。已验美术R1来源`c2b809154eec28efc7ca4069fa338b22815b9024`，功能前置`1985d1b3`，完整事故前置链从`d839bc7e`至04A/04B及接口退出/遗留空口窄修一并接入。不得恢复已取消应力闭环或旧熔穿上限。

## 工作区与职责

- 复用干净`E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration`，PM从当前main新建`codex/accident-accepted-20261023`。源事故树只读保留，主工程3D工具dirty及外部移走正式包的状态保留至正式新包提升，不reset/stash/clean。
- 新批次携带当前已验事故树`run/saves/新的世界`。源日志2026-10-23 21:05:29已全部维度保存，派发仅有Gradle daemon；执行者复制前再核session.lock可独占。目标同名世界完整移入本树`run/save-backups/accident-accepted-20261023/`后整份复制，逐文件数量/bytes/SHA核同源；不混合/共享链接/打开/回写源世界，不进Git。
- 唯一执行者处理源码冲突、验证及制品，一次组合窄审读现有证据；PM维护核心文档、Git/文档冲突。无Git写/派发权限。必读主工程AGENTS、治理5.1/6、STATUS、联合卡及各现成事故/美术R1报告，技能真实入口`C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-resource-pack,minecraft-ci-release}/SKILL.md`。核实际锁定版本，不升级或改许可/发布方式。

## 集成与精确写集

PM先标准merge --no-commit合入已提交事故分支。无冲突内容保持Git合并结果；PM解决文档冲突。代码冲突由执行者在下列文件最小合并：

- `src/main/java/com/iksxh/create_nuclear_industry/automation/{ReactorAutomationService,ReactorAutomationState}.java`；保main闭环删除及协议5，只添加已验退出接管。
- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`；保main热工/优化/闭环删除，添加已验事故接管/注销、退出接管及遗留空口安全重绑。
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ReactorAutomationRetrofitGameTests.java`；仅真实冲突接线/移除对已删除闭环的失效引用，保退出/红石高输入/事故托管断言。
- `build.gradle`仅在实际冲突时保main许可声明/原排除，加事故域排除；不改依赖、任务结构或全仓历史probe政策。
- `src/test/java/com/iksxh/create_nuclear_industry/P1DataContractTest.java`仅真实失败确认其旧“无新增”断言与本次批准事故配方冲突时最小更新合同，保原制造/受污染工质禁止规则；先留失败证据，不删测试或放宽其它禁项。
- 本树`build/reports/accident-accepted-20261023/`允许实施/审查/制品报告及必要核验脚本；允许上述世界复制/备份目录及普通编译输出，跟踪JAR/日志先留原字节备份。

其它代码/资源不自行修改。出现额外实际源码冲突/编译失败先给文件与依赖，由PM扩准确写集，不将本批扩大为修任意旧测试。17美术允许路径与R1原件SHA/语义保持；不重新生成素材，不带其它美术/音效/辅助工具。原main所有非事故生产源、MIT、护具/动态指针及累计优化保持。

## 必要检查与制品

1. 保存来源/主线/合并差异与精确输入清单，核本批外写保持及事故/R1来源绑定。自动合并无源冲突且实现未变则复用既有业务，仅必要增量编译；若实际存在跨模块代码冲突，最终稳定后一次全JUnit作为对应集成回归，失败据实分析，不提前跑重复定向JUnit。
2. 管理员/仪表冲突须用既有`cni_reactor_automation_retrofit`真实域一次核退出接管、稳定红石高输入、事故托管和原保留功能。事故本体/残骸源码若与已验来源相同，则复用其04B/创造拆除/保存证据，不再跑长冷却/回收矩阵；若发现实际额外变化再报具体风险。测试不打开用户世界，显式SKIP与业务/退出分开记录。
3. 一次组合窄审核最终冲突/并集、主线保留及既有证据，不重复执行测试。PM收件提交准确合并源后，唯一一次增量jar，核全部class/resources/三许可声明与准确源、事故域及JUnit排除、17美术源/处理输出/包内身份。将原始输出同字节存报告目录，保留旧包及所有失败证据，不重封包。
4. PM快进main，提升已核原始JAR到跟踪正式路径并提交，记录生产源和SHA。用户手测证据按来源复用，不追加同一视觉门。各源树/世界/日期名旧包和3D工具dirty保持；不推送、发布或删树。

## 实际交接与失败核界（2026-10-23）

标准merge只有文档冲突，源码自动合并；首次编译发现仪表事故锁定/退出接管两处仍调用main已删除的`setAutomationLoadFollowSession`。仅移除这两条失效清理，不恢复闭环，随后真实retrofit域7/7、exit0/正常保存。由实际跨模块API失败触发的一次全JUnit为854项、3失败、11跳过：HeatExchangerBoilerBridgeTest两项配置未加载，ReactorFissionDifferentialTest一项全状态差分。先保留XML/日志，不擅自放宽测试或改非事故算法。

为判断这3项是否本批回归，唯一执行者额外允许在主工程当前未合事故的`main`运行既有两测试类的定向test一次，允许普通build输出；先备份根跟踪日志，不改源码/根JAR/配置/世界/3D工具。准确命令、主线输入身份和同名失败须归本树集成报告目录。只有证实本批新增的问题才扩修；原基线问题保留证据/边界，不称全量通过，也不借本批顺带优化换热器或裂变数值。
