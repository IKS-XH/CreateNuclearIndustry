# ACCIDENT-SHUTDOWN-03 组合窄审

结论：未发现本次范围内尚未关闭的代码阻断项。修复符合主工程活动卡阶段B合同，可交PM管理准确来源提交及其后唯一候选打包；本结论不审尚未生成的新JAR，不代表用户原中间堆拆除、客户端或整批事故人工验收通过。

审查基线为事故树`96a8421cc6073ea960fd331895f66eb949ea9008`，仅审未提交的`ReactorPortBlockEntity.java`生产增量及既有`AccidentExecutionGameTests.java`新增单例。实际diff为2文件、146新增/1删除，生产文件42行增量统计。已读两树AGENTS、主工程现行治理5.1/5.2、STATUS、实际活动卡及阶段A DIAGNOSIS、旧尾热/事故与ADMIN-RELEASE候选证据。应用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`核服务端BE、绑定和库存所有权，应用同根`minecraft-testing/SKILL.md`核正式事故/CommonHooks用例与证据边界；实际Gradle版本保持MC1.21.1/Java21/NF21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。审查者未测试、构建、实施、Git写、派发或改用户世界，只写本文件。

阶段A封存证据支持该根因：完整29文件/13363621 bytes已保存世界副本定位中间仪表(19,-59,4)，六口中两个空口仍带已完成旧事故锁，端口位置/物理代际匹配原六列收据；50动作及冲击完成、Captured/Settled均真且payload为空。当前三棒全插、活动余热0HU、冷9000mB/热0mB；原锁导致正式重绑失败、完整物理口捕获返回null、预检REJECTED，尚未到尾热评估。DIAGNOSIS明确这是只读现场与源码结论，未冒称原世界运行复现。旧850904d已包含且本次未动热工/安全门，不能将两者混为同一根因。

| 核查项 | 源码判断与证据 |
| --- | --- |
| 原预检先行，接收者真实占有本口 | [ReactorPortBlockEntity.java:201](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorPortBlockEntity.java:201)保留原参数/职责/有效结构校验；209行保留旧绑定所有权预检。全部通过后才检查事故收据。245行辅助入口要求服务端、当前真实端口和仪表实例、同Level及已加载；251行从当前有效扫描核FUEL列和精确cap坐标，不靠合法列号或相近位置提前解锁。 |
| 同维度完成收据、不同事故世代及空物料 | [ReactorPortBlockEntity.java:255](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorPortBlockEntity.java:255)按本口旧锁ID查本维度SavedData，显式核dimension、完整complete及新owner世代不同于旧事故。259行要求恰好一条本口坐标/本物理代际的Captured、Settled且payload空收据，当前物理口也须空。缺失/未完成/错代/非空/错误列或职责均在任何撤锁前返回false。没有按账本旧owner坐标放宽或串接另一所有者库存。 |
| 只结束本口旧权限，代际和能力不复活 | [ReactorPortBlockEntity.java:219](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorPortBlockEntity.java:219)仅在上述证明后清本口accidentLock并生成本口新物理代际；随后沿原绑定/绑定代数/失效路径。带锁时isBoundTo必为false，故changed及bindingGeneration失效路径确实执行；不改变其它口身份或旧账本/原件/动作/产物。成功后锁为空，重复重扫不会再次换代；既有NBT保存该新身份。 |
| 真实正例与守恒 | [AccidentExecutionGameTests.java:79](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/gametest/AccidentExecutionGameTests.java:79)从真实组件、正式倒计时末tick和真实Executor起步，保留原BE幸存空口后重建新仪表，未手工删锁。109行经CommonHooks验证仪表维护，134行另验证真实外壳FULL_SHUTDOWN_RESET及实际拆壳重扫；同时核六列重绑、新代际、端口保存/重扫幂等、补取料和旧冷端能力拒写。123行最终断言明确重新取得当前DataStorage对象比较完整旧账本，非旧悬空对象；5件具名全新原件逐栈保持、1件真实残骸废料回收，未从Original审计字段造料。 |
| 反例与范围保持 | [AccidentExecutionGameTests.java:143](E:/MyMC/NewMod/Create_NuclearIndustry-accident/src/main/java/com/iksxh/create_nuclear_industry/gametest/AccidentExecutionGameTests.java:143)在隔离夹具读回旧锁NBT，7组缺账本/错物理代际/非空口/原事故owner/职责错/列不占口/未完成账本都核false及完整持久状态不变。其它更保守检查（例如错维度及重复匹配收据）由生产分支直接核实，不冒称另做了真实用例。没有修改事故账本/触发、热工、epsilon、不可用保护、CommonHooks最终门、管理员、构建/配置/资源或main。新增非显然说明均为中文且与行为一致。 |

最终证据已按冻结版本核对：

- `01-red.log`为Player/ServerPlayer夹具接入编译失败、exit1，未启动业务，不计正式RED。
- `02-red.log`在原生产代码上正式完成6列/50计划/57收据动作后，以真实幸存空口重建冷堆，CommonHooks维护拆除被取消，正式业务RED、exit1；全部维度保存。
- `03-green.log`为修复后的早期GREEN，保留，不冒充收紧断言后的最终运行。
- 最终是`04-green-final.log`及对应exit文件：**1项实际业务通过、11项显式SKIP、exit0**，全部维度正常保存并结束。夹具实际留下4只原空口，现场是2只，测试按真实幸存列表验证而未硬编码数量。框架All12不等于12项业务通过，也不将03/04同例相加为两项。

已逐项核当前两源SHA与`SOURCE.json`/`frozen-source`冻结身份及`SOURCE.patch`一致，`git diff --check`通过。最终REPORT SHA256为`32129B63F3267581668394F1F7A2B4CED8EC3349B9707038C8BED428C2359D75`；生产Port原始SHA为`84F7D7AE11594F83754471EB48A29FD972258C4BDCFC75942E8BF4BF96210874`（LF为`6C5A54BBA2C0D81F2684AB57D76F8E456DE42DF57C9263F6907F8A74D4066F3B`），测试SHA为`598E475B2C0E563C26C3DB164086B9049A15F84DCD17D99325F33539CF0907FD`。日志冻结摘要与最终原始日志/退出码相符；根日志、基准JAR及用户源世界未变证明保留。当前未提交/未打包，审查没有为未生成制品作身份保证。

按治理5.1复用未变旧尾热、原事故矩阵及接口退出证据，不重复全套或追加性能/旧迁移检查。此次保存覆盖正式NBT往返和DataStorage读回，未证明新的完整停服重进、物理ChunkEvent.Unload或真人多人。用户仍需集中复看原中间堆可拆、处理中事故口继续保护以及已通过的接口退出逐棒解锁；main不含本段事故锁机制，无需无关移植。高温/熔融物扩展继续等本轮人工门。
