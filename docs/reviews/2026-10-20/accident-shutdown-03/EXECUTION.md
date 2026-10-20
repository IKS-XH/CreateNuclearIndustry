# ACCIDENT-SHUTDOWN-03 源码与验证交付（待审查/提交/打包）

2026-10-20；唯一工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-accident`，基准 `96a8421cc6073ea960fd331895f66eb949ea9008`。本交付尚未提交、合入、打包或替换正式制品，停在活动卡的组合窄审门。生产源码自03绿色验证后未再修改，最终测试断言收紧后04重新通过；准确输入和编译产物见 `SOURCE.json`、`frozen-source/`、`SOURCE.patch`。

## 现场根因与改动

封存的阶段A诊断见 `DIAGNOSIS.md`、`STAGE-A-SEALED.json` 和 `stage-a-source/`。唯一源世界29文件/13,363,621 bytes；中间5³仪表(19,-59,4)三棒实际/目标全插、活动余热0HU、冷9000mB/热0mB，无倒计时/事故id。其两只幸存空换料口仍持已完成事故6b2fd30c…的锁，位置和物理代际与Captured/Settled收据完全匹配。它们不能重绑，正式拆除预检捕获全体燃料口失败，尚未执行数值停机评估就以“尚未完全停机”拒绝。旧微尾热修复已在祖先，相关三文件与main相同，没有改热工或安全阈值。

唯一生产改动为 `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorPortBlockEntity.java`：

- 保留原绑定参数、职责及旧所有权可接管预检；只有这些先通过，才考虑带锁的空口。
- 只在既有服务端有效重绑边沿核当前端口/仪表真实实例、同Level且已加载、结构对应燃料列帽确为本口。不能凭传入合法列号或同坐标新物件撤锁。
- 以旧锁ID直接查同维度账本，必须完整事故完成、接收仪表为不同于旧事故的世代，并且恰好一条同坐标/同物理代际的列收据Captured/Settled均真、Payload为空，当前端口也为空。
- 证明全部通过后，才结束本口旧锁并给本口新物理代际；随后沿原绑定代数/capability失效和同步路径。重复扫描不再更换代际。原账本、Original审计栈、真实产物、工质与热账均不改，不触发旧任务再生产。

未改Service、Lifecycle、正式停机评估/CommonHooks事务、热工/epsilon、管理员、构建、配置、资源或main。唯一测试改动是既有 `src/main/java/com/iksxh/create_nuclear_industry/gametest/AccidentExecutionGameTests.java`，增加本根因的一个聚焦业务，无新域/构建规则。

## 实际验证与失败

所有命令先设 `CNI_ACCIDENT_FOCUS=shutdown`，调用：

`./gradlew.bat runGameTestServer -PgameTestNamespace=cni_accident_execution -PgameTestDirectory=build/reports/accident-shutdown-20261020/<对应隔离目录> --console=plain`

| 原始证据 / 隔离目录 | 实际业务及退出 |
| --- | --- |
| 01-red.log / server-red | 初次夹具误用返回Player的makeMockPlayer，CommonHooks要求ServerPlayer，编译失败exit1；改用现有FakePlayerFactory正式入口。未启动业务，不计RED。 |
| 02-red.log / server-red | **1项业务预期失败、11项显式SKIP**。正式末tick事故完成6列、50计划（外部32）、57收据动作后，实际保留原空口并重建不同仪表世代冷堆；CommonHooks维护拆除被not_fully_stopped取消，业务断言失败。全部维度正常保存，Java及Gradle均exit1。生产仍原版。 |
| 03-green.log / server-green | 最小生产修复后，**1项业务通过、11项SKIP**；仪表维护及外壳FULL_SHUTDOWN_RESET均经CommonHooks放行。全部维度正常保存，Gradle/JavaExec成功、exit0。 |
| 04-green-final.log / server-green-final | 只将“旧账本未变”断言从初始对象收紧为当前DataStorage已读回对象，生产未变；同一**1项业务通过、11项SKIP**，保存全部维度并exit0。此为最终源码对应证据。 |

框架显示All12只代表含SKIP的返回总数，不称本次12项业务通过。03/04重跑同一个场景，不累计为两种新业务。首次新建隔离目录日志含原版server.properties缺失后创建的ERROR/NoSuchFileException和现有ApiStatus注解缺失WARN；未隐藏，之后原生测试与正常保存/退出明确成功。

最终单例实际覆盖：

1. 六燃料/三全插棒真实结构，以带真实组件与余热的正式末tick触发事故，等待真实Executor完成；32块壳上限后此夹具有4个幸存空口（用户现场为2个，目标均为真实残余口，无硬编码幸存数量）。
2. 同时正常保存/读回整个独立SavedData，再补缺失方块并放置真实新仪表；全体6列正式重绑。CommonHooks仪表维护放行，没有手工删事故锁。
3. 新物理代际有变化；端口当前格式保存读回及正式重扫后身份/绑定代数幂等；实际补料、取料成功，旧冷端能力不能向新结构写入。
4. 对同一真实口只在隔离夹具读回旧锁NBT构造7组拒绝输入：缺账本、物理代际不匹配、当前口非空、原事故owner、职责错误、列不实际占用本口、账本未完成。每次bindTo=false且完整持久状态不变。未完成反例使用当前格式收据副本并单独完成夹具清理，不代表运行了另一真实事故。
5. 比较当前已读回的完整旧账本（不是悬空旧对象）与冻结收据一致，Payload全空；5件具名全新原件仍以唯一实体ID逐栈matches，1件真实残骸废料经take回收由夹具持有，未从审计Original再造。重绑过程不动这些原件/废料。
6. 重播已完成Executor无新动作；再正式补入一件新燃料，经CommonHooks拆一格壳，执行原FULL_SHUTDOWN_RESET和全部真实口setFuelAssembly路径，燃料/冷液按既有清空规则归空；随后实际移除该壳并正式重扫，结构失效。没有绕过安全门或仅以仪表维护代替完整清空验证。

没有改纯计算逻辑，按补卡复用旧微尾热55项、原事故预算/多所有者/冲击矩阵、接口退出域与用户已通过的接口解锁，不新跑JUnit或整事故矩阵。端口/账本恢复证据为本版本正式NBT读写与DataStorage重建，未冒称本轮完整停服重进业务、物理ChunkEvent.Unload、真人多人或用户原世界拆除通过。

## 冻结身份与保护状态

- `SOURCE.json`含两份手写源的原始及LF归一SHA256、准确基准、当前Port编译class身份、全部四轮日志SHA/退出码、完整Git状态及只读保护证明。`SOURCE.patch`为当前准确差异，`git diff --check`通过。
- 生产Port原始SHA256 `84F7D7AE11594F83754471EB48A29FD972258C4BDCFC75942E8BF4BF96210874`；Git LF内容SHA256 `6C5A54BBA2C0D81F2684AB57D76F8E456DE42DF57C9263F6907F8A74D4066F3B`。测试SHA256 `598E475B2C0E563C26C3DB164086B9049A15F84DCD17D99325F33539CF0907FD`。
- tracked仅上述两源码dirty；根logs首轮验证前完整备份至 `pre-validation/root-logs/`，最终全部文件数/SHA仍等于原始基线，没有需要执行者恢复的跟踪日志。隔离服原始日志均在本报告目录。
- 用户世界最终所有源文件SHA仍匹配首次清单；未运行/改写原世界。候选tracked基准JAR与预验证备份逐字一致：3478572 bytes，SHA256 `81E7CD98D589DDC9C0AAF398C06776DC4318EA54C70CD19376F0C127E4A3DE7B`。没有运行jar/assemble，也没有替换main或候选正式包。
- 未Git写、派发、改核心文档或状态；报告/辅助仅本目录。实际应用minecraft-modding/testing、systematic-debugging、TDD与verification-before-completion，版本保持MC1.21.1/Java21/NF21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。

下一门为一次组合窄审；PM准确提交此源后，才按卡执行唯一增量jar与候选身份核验。集中人工清单：候选原中间堆可正常拆；仍在处理的事故口受保护；已通过的接口退出逐棒解锁保持。当前自动证据不替代这些玩家复看。
