# RADIATION-01 审查整改第1轮执行报告

2026-10-16；唯一执行者 `/root/radiation_foundation_impl`。只实施PM确认的R1/R2/R3与同场景性能补证，没有派新审查/子Agent、Git写操作、核心文档修改或启动用户携带世界。生产源在 `review-fix-01-delivery.log` 对应实现后冻结，随后只assemble和收集/写报告。

## 先核控制流，再整改

实际读取 `REVIEW.md` 和最终源码，按 `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/receiving-code-review/SKILL.md` 验证三项成立：旧局部源聚合/射线缓存依赖精确目标；旧实体列表每次从头；旧incomplete重试没有独立发送时钟。新增真实断言先在旧生产实现获得三项失败，再修改实现。继续使用minecraft-modding/testing以及原TDD、systematic-debugging、verification-before-completion技能；本轮没有资源/数值/依赖变化。

## 精确变化

- `src/main/java/com/iksxh/create_nuclear_industry/gametest/RadiationFoundationGameTests.java`
- `src/main/java/com/iksxh/create_nuclear_industry/mixin/RadiationBlockChangeMixin.java`
- `src/main/java/com/iksxh/create_nuclear_industry/mixin/RadiationItemEntityMixin.java`
- `src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationContainment.java`
- `src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationExposurePayload.java`
- `src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationServerEvents.java`
- `src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationSourceIndex.java`
- `src/main/java/com/iksxh/create_nuclear_industry/radiation/client/RadiationClientEvents.java`

`RadiationSourceIndex` 按玩家UUID保留一轮明确Job：固定目标与参数、已加载BE引用、随身库存/实体引用快照、库存/实体/源校验/累加游标、源位置分组、射线队列和单条DDA格游标。玩家逐tick小幅移动继续当前轮；完成后下一轮使用新位置。位移超过检测半径、参数/世界改变则重建，直接 `sampleAt` 诊断入口仍按明确目标键使用。来源只读快照不拥有真实物料；静态集合超单tick预算能走到后缀，单条长射线也能暂停。完成时间戳取实际完成gameTime，部分结果始终complete=false，不能按零推进剂量。

方块变化按实际源owner坐标、源区块、已遍历路径格及已知完整壳边界依赖，撤销或重算相关内容；无关块/库存变化不清整维度进度。`RadiationContainment` 仅新增几何依赖查询，保留dirty缓存与壳合法性。ItemEntity的setItem/离开、BE脏通知、区块卸载撤销对应旧源；交付前还核对了传送带controller的运输实例仍在正式表/toInsert且不在toRemove，避免运输提走后旧Job继续计源。每轮交付前预算化验证真实owner、物品组件、实体、载入边界，来源失效只排除本轮旧投影，新内容下一轮发现。

退出/换维/克隆取消UUID任务和显示时钟，世界卸载撤销所有索引（包括注入测试预算的实例），目标区块卸载撤销对应任务；40tick无人请求任务撤队。实体列表引用和BE列表的初始原生空间查询/集合枚举仍是同步快照，逐项源读取、校验、射线与求和才受工作预算；本轮未声称任意实体规模的硬纳秒上界或多人调度公平性已实测。持续更改相关源/屏蔽仍可能推迟当前轮，本轮保证的是静态超预算、移动目标和无关更新不能永久抹进度。

发送改为独立DisplayState：latest、尚未发送的complete和lastSent。采样重试与剂量时钟照常，complete/incomplete均每20tick至多发一次，未发complete优先于后续pending。生产网络与计数接收器共用offer/flush，FakePlayer不伪登录Create。Payload/client允许同维度完成结果接替较新pending，同时仍核有限数、维度与60tick有效期。伤害每个在线tick运行，incomplete不推进恢复/吸收，原持久化/死亡语义保持。

默认预算没有提高：每维度每tick32768栈工作、4096 BE、32768射线格、8192外壳格。较小WorkLimits只供注入同一生产算法验证，不是另一套镜像算法，也不更改源强/数值上限。

## RED/GREEN与实际命令

- 命令：`./gradlew.bat runGameTestServer -PgameTestNamespace=cni_radiation_foundation -PgameTestDirectory=run/radiation-foundation-gametest`。保持既定独立域/运行目录，不加载携带世界。
- `review-fix-01-red.log` 首次尝试用新holder域过滤，NeoForge按templateNamespace过滤导致 `No test functions were given`。即使Gradle退出0也不是测试通过；随后使用既定模板域修正测试选择，原日志保留。
- **有效RED** `review-fix-01-red-registered.log`：12项真实测试中旧9项通过，新3项失败，Java退出3/Gradle退出1并保存世界。移动/无关更新70tick仍未完成，静态实体50tick仍不到后缀，60次发送入口得到60包。
- 初次修复 `review-fix-01-green-first.log` 13/13，直接依赖映射收尾 `review-fix-01-green-final.log` 13/13，均保存/正常退出0。增加传送带pending提走断言后，**当前冻结来源**为 `review-fix-01-delivery.log`：13/13 required、全部三维度保存、BUILD SUCCESSFUL、GRADLE_EXIT=0。旧轮结果不替代该来源。
- R1用12个真实ItemEntity、射线预算8，FakePlayer每个真实tick移动，同时远处方块逐tick变化，正式 `RadiationServerEvents.tick` 在第25个续接tick得到持久剂量 **0.11757867639469602D**；断言起始确为pending。
- R2用20个静态实际ItemEntity、栈预算8，第20个为放射源，第5个续接tick完成并读到1D/s。没有创建32769个实体拖垮服。
- R3共用生产发送入口，60个逻辑在线tick输入仅3次计数传输，包含pending和complete；另核较旧完成结果在下一20tick时隙优先送达并可接替pending。日志中的全局packets=6是这两段计数接收器接受总数，**不是6个真实S2C网络包**，没有真实登录客户端。
- 第4项新增真实回归：pending单条射线的已处理格新建屏蔽墙，重算得到0.05/400D/s；下一轮pending移除真实ItemEntity、随后pending带载荷提走，完成场均为0。
- 公共源/同步流程改变，因此同一次运行复核旧9项，含库存/壳/有限数/NBT保存/配置卸载、65536嵌套续接、incomplete不恢复且已有高剂量伤害继续。最后嵌套样本2个真实tick完成。源参数/纯剂量/配方及旧热工事务未变，复用既有41项JUnit（`targeted-delivery-junit.log`与`junit-xml/`），本轮没有重跑或冒称新的41项结果。
- 增量编译 `./gradlew.bat compileJava` 见 `review-fix-01-compile.log` / `review-fix-01-compile-final.log`；最后增量 `./gradlew.bat assemble` 见 `review-fix-01-assemble-final.log`，均exit0。没有clean、rerun-tasks或无关全量。`git diff --check -- src build.gradle`通过；Git只读。

## 同场景配对性能

原始记录均在最后同一次 `review-fix-01-delivery.log`，没有另开性能服。

| 同一16³/196已用组件/满架场景 | 样本 | P50 ms | P95 ms | P99/最大 ms |
| --- | ---: | ---: | ---: | ---: |
| 不调度采样 | 40 tick | 3.7506 | 6.6842 | 25.4380 |
| 每20tick调度采样 | 40 tick | 3.3997 | 5.9918 | 8.6439 |

这两个相邻阶段保持场景、服务端版本和实现一致，先关闭、再开启手动诊断查询调度；不是切换辐射玩法配置。Pre/Post服务端tick区间包含其他已加载夹具，GameTest可快进，样本短且未随机顺序/JIT/GC校正。观察差值P50为−0.3509ms、P95为−0.6924ms，不能据此推断采样带来加速或长期因果净成本。源查询实际使用生产默认预算，完整场景835栈工作/203 BE/0射线，配对后段834/203/0；增加的验证/累加工作亦计入栈预算，稳定外壳读取增量0，一个回调完成，默认调度最大等待20tick。

同日志其他各40tick阶段：无16³关闭P50/P95/最大0.6491/2.3495/4.5759ms，无16³开启0.5754/1.1170/1.3526ms，16³空闲开启2.7218/7.1825/26.6904ms，完整已用堆配对前开启5.6013/11.1276/25.9640ms。它们是背景记录，不代替上述同场景配对。所有数据是服务端GameTest；没有四真人、客户端看向/背向/GUI或真实网络性能证据。


## 可追溯来源与制品

覆盖旧manifest/候选前已复制首审输入至 `first-review-input/`，包含source-sha256.json、candidate-jar.json和原JAR实物；旧完整REPORT亦备份。保留REVIEW.md、PM的review-source.diff及全部失败。前后61项集合相同，8项内容改变，未增加卡外源文件。

| 输入 | 文件数/字节 | SHA256 / 位置 |
| --- | ---: | --- |
| 首审源清单 | 61项 | `first-review-input/source-sha256.json` |
| 首审候选JAR（已备份实物并复核） | 3417633 bytes | `9F78F1100D0EEAE6545B6A1A92B0416AB504CC081ED160D25B9CB53B0982B46A` |
| 本轮源清单 | 61项，其中8项变更 | `source-review-fix-01.json`；逐项前后身份见 `review-fix-01-source-delta.json` |
| 本轮候选JAR | 3427684 bytes | `6EEFAACBA5F86686016AEC72D4313B9870A52F3F4070FFC67E09BD973057593A` |


当前JAR路径 `E:/MyMC/NewMod/Create_NuclearIndustry-radiation/build/libs/create_nuclear_industry-0.1.0.jar`；本批GameTest类/模板/命名空间目录0条，配置类/索引/仪表资源存在。`RadiationConfig.java`被原config/规则忽略但明确列在61项清单及JAR中，PM提交须单独force-add，执行者未改ignore或Git。此包仍是未提交来源的候选。

携带世界27文件/13337924 bytes与原清单逐项相同（`saved-world-after-verification.json`），`saved-world-sha256.json`未改。没有启动用户世界或客户端。客户端视觉/声音/真人重登换维、动力物流连续运行、多人携带影响及性能仍待原集中手测，不用本轮FakePlayer/计数传输代替。原审查者只需核本轮8项差异及上述新证据；验收/合入由PM负责。
