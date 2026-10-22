# CORIUM-CREATIVE-REMOVAL-01 组合窄审

2026-10-22。**本卡范围内未发现未闭P1/P2代码阻断。** 只读审查生产增量、既有API、短聚焦夹具与RED/GREEN证据；未运行测试、构建、服务端或用户世界，无Git写操作，唯一写入为本REVIEW。

- 事故树 `E:/MyMC/NewMod/Create_NuclearIndustry-accident`，分支 `codex/accident-thermal-04b`，HEAD/基准 `afb16899972cbe1495e31857dd2d3b75286ada59`。以主工程 `E:/MyMC/NewMod/Create_NuclearIndustry/docs/superpowers/plans/2026-10-22-corium-creative-removal-01.md` 为准，已读AGENTS、治理5.1/5.2/6、STATUS并复用04B已封存背景；实际使用minecraft-modding/testing技能。Gradle仍为MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。
- 当前仅 `paths.json` 所列两个源文件dirty。生产代码为 [ReactorCoriumBlock.java:26](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/ReactorCoriumBlock.java:26) 新增服务端创造玩家的playerWillDestroy覆盖；加import共6行。先调用现有take，丢弃返回的真实载荷，再执行super。随后原onRemove再次take只得到空栈，不能产生废料；新增中文注释与实际边界一致。
- 已对照本地锁定NeoForge sources及 `locked-api.txt`：ServerPlayerGameMode.destroyBlock:247–265 先调用CommonHooks.fireBlockBreak，最终取消/权限失败即返回，之后才调用playerWillDestroy。CommonHooks在事件post返回后读取最终取消；既有ReactorBreakFinalizationMixin未改。因此本修复不会在最终取消前预清库存，也不把LOWEST预检当成提交边界。
- 未改 [ReactorCoriumBlockEntity.java:31](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/accident/ReactorCoriumBlockEntity.java:31) 的take：清唯一payload，并经removeSolid终止热引用；原调度继续撤HOT活跃位置。生存与环境仍沿原onRemove唯一掉落链，空残骸不造料；没有改冷却、辐射、材料、容器、熔融本体或管理员规则。
- [AccidentExecutionGameTests.java:94](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/gametest/AccidentExecutionGameTests.java:94) 新增一个短业务，复用原域、模板及focus `corium_removal`；真实FakePlayer.gameMode.destroyBlock覆盖创造冷态/HOT/空残骸、LOWEST最终取消、生存铁镐，环境另用Level.destroyBlock(true)。没有直接调用onRemove冒充真实玩家拆除，冷态前置明确复用凝固后热终态而不重跑长时钟。

## 证据与身份

- 命令均为既有 `runGameTestServer -PgameTestNamespace=cni_accident_execution -PgameTestDirectory=build/reports/corium-creative-removal-20261022/server-{red,green} --console=plain`，环境 `CNI_ACCIDENT_FOCUS=corium_removal`；原始完整命令与时段在两份 `*-exit.json`。本审查只读取这些记录。
- `01-red.log:104` 实际创造冷残骸 destroyed=true、drops=1、payload=0，随后业务断言失败；1业务失败+17显式SKIP，Java1/Gradle1，正常保存退出。保留此真实业务RED，不称编译RED或隐藏失败。
- `02-green.log:108–112` 创造冷/HOT/空三次均drops=0/payload=0/heatActive=false，取消保留同一BE/具名payload/热剩余；生存及环境分别恰好一份原具名载荷，重复移除不再产物。`:122` 后续tick确认已拆HOT索引撤销、取消对象仍保料/活跃。最终为 **1业务通过+17显式SKIP**；`:141`全部维度保存，Java0/Gradle0，29秒。不能把18 required passed表述为18业务实测。
- 2个源、3个编译class及两份主测试日志的SHA均匹配SOURCE；受保护旧根JAR和两份根日志当前SHA匹配运行前清单，未要求恢复或改写。`git diff --check -- src`通过，无额外源/资源/构建规则变化。
- EXECUTION SHA256：`003D62ADAD1F2C833661CB7DC872D8EE72BA657C7507EB6AEC0F5EF3CA370FF7`；SOURCE SHA256：`C1F27E8E7FCE98F7BEE1B655C8B13E5BE1BF04BF429D0E17BE0B79C32AF6D784`。生产源SHA为 `F263FEF57923BBF0F1E773AB387DA1FB6714B5D58E68D8C30E0C93E430945ECF`。

尚无本次修复候选JAR，未审未来制品；根旧81E包不含本修复的身份承诺。用户世界及main正式包未由本审查触碰。原04B其余手测反馈与证据不重开，本轮客户端仍只待创造拆残骸修复的复看；物理卸载、真人多人未补记通过。以上结论不替代该客户端复看或main合入。
