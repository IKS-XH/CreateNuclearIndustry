# REACTOR-EXPLOSION-01：运行反应堆遭原生爆炸破坏不进入事故

## 来源、前置与目标

用户2026-10-27报告TNT/苦力怕摧毁反应堆部分结构却不融毁，随后明确是在main版本、被炸前正在运行。本卡修复这一实际路径漏接，不改变裂变/冷却/燃耗/控制棒/融毁阈值，不把冷停机空堆或仅邻近爆炸强制融毁；锅炉候选仍待原集中手测，不夹带合入。

PM已实际读取minecraft-modding/testing技能及锁定NeoForge21.1.219源码：Explosion.explode的Detonate还允许其它监听器修改affectedBlocks；finalizeExplosion在此后处理最终列表；BlockBehaviour.onExplosionHit先取loot、后onBlockExploded，后者再setBlock(AIR)。现有接管只连接CommonHooks玩家BreakEvent，原生爆炸没有走它；onRemove后再找仪表或燃料已太晚。设计采用最终块处理前的原生爆炸边界，对最终列表命中的有效机组先预检并接管，不伪造玩家BreakEvent或只监听早期Detonate。先受损仪表、换料口及同爆炸多块/多机组均须守恒与去重。

PM保文档/Git，唯一执行者保源/测试/构建，最后一次组合窄审；不转授PM。新树`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix`，分支`codex/reactor-external-explosion-20261027`从当前main创建。现有shutdown树有跟踪日志dirty，保留不用；工作树工具不能指定治理要求的同级绝对路径，本批由PM按该路径使用git worktree add。main的3D dirty与已缺失正式JAR/日期包不动。

main默认已保存世界`run/saves/新的世界`，无游戏Java进程，PM已只读独占session.lock成功，level.dat最后保存2026-10-18 01:47:21。执行者再核锁，完整复制到新树run/saves，核文件数/bytes/逐文件SHA及源前后相同；不打开或回写用户世界。新目标不存在同名世界；若实际发现，先停报并完整备份，不合并两份世界。

## 必读与技术边界

AGENTS、治理5.1/5.2/6、STATUS、本卡、既有[事故设计](../specs/2026-10-16-accident-radiation-design.md)和[接管卡](2026-10-18-accident-execution-02.md)。实际技能入口为`C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-ci-release,minecraft-resource-pack}/SKILL.md`，按逻辑/验证/最终制品/注册资源实际应用。核MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，不升级依赖/版本/许可/发布。

- 在最终affectedBlocks已确定且任何成员loot/删除之前统一采样。优先锁定Explosion.finalizeExplosion的实际块处理边界；支持等价更窄而不漏“非反应堆块先坏导致机组状态改变”的路径。KEEP/TRIGGER等无结构破坏交互、Start被取消、Detonate最终剔除全部成员、无效/冷停机空堆、仅相邻且未受损机组均不得创建事故。
- 沿既有危险判定：真实生成热量超过既有安全epsilon或已开始融毁倒计时，才走立即事故接管。不新增余热/功率阈值、不添加新倒计时；原完全停机评估和玩家拒拆规则不改。用户只确认运行堆，不能顺带让任意冷态爆破成为新事故。
- 可新增事件原因EXTERNAL_EXPLOSION区分来源，但复用既有ReactorMeltdownEvent/Service/唯一SavedData/实际燃料托管；已用组件/原乏燃料每件一废料、全新组件原件返还、工质损失/热量/原事故破坏预算不变。原生外部TNT自身的破坏不被误算为本模组新增32块额度。
- 先全组只读预检，仍存在原机组/端口时原子提交、验证与事件接管；首命中仪表或换料口不丢真实组件、不先掉一份又托管一份。同一世代同爆炸多块/重复入口只建一场，事故任务在仪表随后消失后继续。预检/发布失败不得清物料或伪记已交付，按既有回滚/失败证据处理，确实无法闭合的入口报PM。
- 只处理最终列表真实命中的当前缓存成员，前置检查已加载，不为探测遍历全世界/16³或强载；正常无爆炸tick不加体积扫描。相邻未受损机组保持运行，不把事故本身的分批setBlock或熔融溶蚀转换成外爆事件。
- 新/改手写注释中文，说明原生调用顺序、服务端边界、实际物料所有权与幂等/取消。当前保存恢复按风险验证，旧版本迁移不研究。

## 精确写集

J=`src/main/java/com/iksxh/create_nuclear_industry/`，T=`src/test/java/com/iksxh/create_nuclear_industry/`，R=`src/main/resources/`，只在新候选树。

- J/structure/ReactorStructureLifecycle.java：复用现有多所有者只读计划、提交/回滚/去重，增加外爆前入口或让窄辅助类使用；不改正常重扫、区块/管理员/玩家拒拆算法。允许新增J/structure/ReactorExplosionLifecycle.java将最终列表筛选与接线隔离，不新建事故框架。
- J/blockentity/ReactorInstrumentPortBlockEntity.java：仅必要的外爆发布/原事务复用入口及相关注释；保原危险/完全停机评估、实际端口捕获与锁定算法。
- J/reactor/ReactorMeltdownEvent.java：仅新增上述原因及修正“唯二”说明；J/reactor/ReactorMeltdownEvents.java仅该原因发布必要适配。
- 新J/mixin/ReactorExplosionFinalizationMixin.java；R/create_nuclear_industry.mixins.json仅注册该窄原生边界。不新增任意LevelChunk全局替换钩子，不改原Corium/玩家Break/锅炉Mixin。
- 新J/gametest/ReactorExternalExplosionGameTests.java；J/gametest/AccidentExecutionGameTests.java只将现有build/port/fresh/used夹具中确需复用的方法改为包内可见，不改既有业务/筛选/断言。不复制完整测试框架。
- 新R/data/cni_reactor_external_explosion/structure/explosion_empty.nbt，逐字节复制现有data/cni_accident_execution/structure/accident_empty.nbt。本域注解/模板namespace保持一致，沿锁定GameTest实际筛选入口运行，防止再次零函数伪过。
- build.gradle仅沿现有Jar排除块精确增加ReactorExternalExplosionGameTests*.class与本测试域排除，不改依赖/任务/旧探针策略。
- T/reactor/ReactorMeltdownEventsTest.java仅新增原因必须的枚举覆盖，不放宽服务端/事件去重断言。若纯状态逻辑无变化，不为路由新增照抄实现的单元测试。
- 本树build/reports/reactor-external-explosion-20261027/允许执行/源/审查/失败/真实服/制品报告和必要窄工具，run/saves复制及普通编译输出。原跟踪JAR和既有日志在首次构建前保原字节；其它build不跟踪。禁止Git写、核心文档、其它资源/算法/用户包/来源世界写入。准确额外入口先报PM扩集。

## 必要验证与交付门

先用原生外爆实际破坏运行堆留下RED，再实施。定向单元选既有ReactorMeltdownEventsTest、ReactorFullShutdownAssessmentTest与受改动影响的ReactorStructureLifecycleContractTest（若该源码合同确需新增外爆断言先报准确缺口）。不修3条主线已知JUnit失败，不机械全量；只有新共享风险或失败证据再由PM决定扩大。

同一次本域真实服集中覆盖：真实TNT与苦力怕爆炸破坏运行堆进入唯一事故；最终顺序先仪表/先换料口仍单次托管；同机多块去重、仪表毁后任务继续；真实废料/新料/掉落数量与HU/工质不复制；Start最终取消/Detonate后置剔除/无破坏模式不接管；冷停机空堆/相邻未受损机组不误爆；原玩家最终取消/危险拆除路径仍正确。可复用原夹具和本版本SavedData恢复，不重跑整个熔融/辐射/管网矩阵。SKIP、0函数、异常退出和子项缺口分别记录，真实必跑行为不能以JSON存在替代。

交一份短EXECUTION.md、SOURCE.md及原始日志索引；先不JAR。PM安排一次规格/质量组合窄审，整改只复审对应差异。稳定后PM精确提交源，执行者按指令一次增量jar并核身份/本批测试排除，PM提交候选包。最终交用户手测，不自动合main/提升正式包；锅炉原未闭门仍独立保留，不把本缺陷反馈当它的通过。
