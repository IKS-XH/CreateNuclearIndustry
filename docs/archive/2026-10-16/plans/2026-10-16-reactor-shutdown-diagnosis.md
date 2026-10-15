# REACTOR-SHUTDOWN-01：超大反应堆拆除停机判定排查

本卡保留排查与实施依据；当前交付、制品及原场景复看统一见[STATUS](../../../STATUS.md)。

2026-10-16，用户报告main有一台超大反应堆始终提示尚未完全停机、无法拆除。已确认唯一世界为main/run/saves/新的世界，仪表(-8,-59,-1)，用户明确“已经退出”。基准main db1e6aae6237571e31ec5b54d32b7d290f2ec506。

## 第一阶段：根因证据

PM按systematic-debugging及minecraft-modding/testing安排唯一诊断执行者，只允许写build/reports/reactor-shutdown-20261016/；main源码/测试/Gradle/正式JAR/核心文档/.git和所有用户世界/配置均只读，不Git写、不派发、不运行用户世界。实际读取根AGENTS、治理5.1、STATUS、反应堆合同及对应技能，锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。

只读定位该仪表保存的当前快照、端口燃料/棒位、热库存/量化尾热、融毁倒计时与结构/维护绑定状态；先完整复制所需region/level/config原件到报告目录，核原件身份后读取副本。用户已退出，不再重复询问同一保存确认；若观察到实际写入/世界锁仍占用，停止复制相关现场并报PM，不终止客户端或改用户世界。

沿破坏事件→仪表维护准备→完全停机四条件→当前tick冷却/余热/事故状态追踪，报告具体拒绝分支及数值；区分正确保护（无相邻棒/卡死未插入、真实余热或倒计时）与不可达的安全停机缺陷。核与近期S1/S3优化是否相关，不预设归因。

优先复用已有NBT读取/诊断工具和当前纯Java计算器，必要复现工具仅写报告目录。不修改NBT、不删除热量/燃料、不绕过保护、不安排旧存档迁移、完整build或全量GameTest。交简短DIAGNOSIS.md、必要原始数据及建议精确修复/测试写集；根因明确前不实施或打包。

## 后续边界

修复写集与必要隔离树由PM依据第一阶段证据追加。若是已批准行为的局部缺陷，自动派发最小修复、受影响JUnit和必要真实拆除用例及一次组合窄审；保留危险拆除和库存/热量守恒。若涉及未决玩法先讨论。候选人工拆除复看独立，不把自动通过当用户验收。

## 已核现场与修复安排

只读副本实际仪表为(-8,-59,0)，与用户给出的外侧坐标差一格；16³、149燃料列、47棒实际/目标均1.0、无卡死、融毁未启动，冷液658000mB/热液0。缓存尾热约5.247766665661402e-10HU、量化标记0。Adapter以1e-9阻止安全尾数标记，完全停机却要求活动尾热不超过1e-12，整数mB无法移除该尾数，形成停机死区。须先完成并封存原算法逐tick复现与端口/拒绝分支证据，再写修复。

- PM已建同级隔离树E:/MyMC/NewMod/Create_NuclearIndustry-reactor-shutdown-fix，分支codex/reactor-shutdown-fix，基准db1e6aa。使用using-git-worktrees流程；原生工具没有指定同级根目录参数，遵从用户显式E:/MyMC/NewMod目录规则使用Git创建，不申请重复许可。主分支及其正式包保持。
- 原执行者先把main已保存的run/saves/新的世界完整复制至新树同路径，并核数量/哈希；新目标应不存在，若出现已有同名世界须完整备份后再复制，不能合并或链接。仅携带当前create_nuclear_industry-*.toml有效配置（不带.bak），记录配置身份，原件只读。用户已明确退出且只读捕获已确认世界锁释放，无需再次询问。
- 精确生产写集仅reactor/ReactorCoolantSimulationAdapter.java及reactor/ReactorServerTick.java，限与正数尾热资格/逐列安全余数分配相关的阈值修正；不能提高完全停机epsilon、删除/取整缓存热、放宽缺冷却剂的真实余热或改流体数量/燃耗/保护/其他算法。ServerTick只有最小复现证明同类逐列过滤死区后才修改对应分支。
- 测试写集仅src/test/java同包ReactorCoolantSimulationAdapterTest.java、ReactorServerTickTest.java，以及已排除正式包的gametest/ReactorPortLookupGameTests.java。先RED再最小GREEN，覆盖现场量级/149列聚合、每列低于阈值但汇总超过阈值、无冷液真实短缺仍拒绝、热量/工质守恒、反复tick及正常保存读取（优先复用现有测试）。不改已有断言求过，不处理独立旧2.25HU GameTest。
- 实施者只可写上述候选文件、候选run/saves/新的世界及run/config/create_nuclear_industry-*.toml、候选独立GameTest运行目录和本批build/reports；main仍只写原报告目录。不得Git写/改核心文档/.git/main正式包或派发其他人。注释说明热量单位、标记与真实冷却不足的边界。
- 必跑：这两类JUnit及既有FullShutdownAssessment/P1Thermal01Contract/相关冷却守恒测试；现有cni_reactor_port_lookup隔离域的一次真实服务端场景，验证低尾热冷却后走安全拆除事务、无冷却真实余热仍拒绝，使用现有BreakEvent/维护路径和空模板，不新建测试框架或模板。旧同源客户端/性能不重跑，失败才扩大相应范围。
- 原审查者按本卡作一次规格/质量窄审，只写docs/reviews/2026-10-16/reactor-shutdown-01/REVIEW.md、读证据而不重跑；必要整改只复核差异。源由PM核对提交后再唯一准确源码assemble。此为既有安全量化规则的局部缺陷，无新增玩法取舍，按2026-09-22用户对明确任务的自动验收授权，受影响JUnit、真实安全/拒绝拆除用例及组合审查通过后可接main和正式包；不人为增加一次合入许可。用户原场景复看仍单独记录，未反馈不冒称其已通过，保留隔离树/世界供观察。

只读原算法探针已确认现场推进1/20/2000tick均新生热0、燃耗0、活动尾热5.247766665661402e-10HU、冷/热库存658000/0不变，2000tick快照完全相同。另3列各7.5e-13HU汇总2.25e-12HU时，已有量化分配预算仍分配0列，证明逐列过滤存在同族死区，ServerTick对应正数筛选纳入最小修复。保存外壳1352成员、149顶部端口/合法组件一致；这不冒称当前ServerLevel绑定已实测，真实维护路径另由定向服务端用例验证。原始证据在本批报告目录pure-probe-with-ports.jsonl和port-and-structure-evidence.json。
