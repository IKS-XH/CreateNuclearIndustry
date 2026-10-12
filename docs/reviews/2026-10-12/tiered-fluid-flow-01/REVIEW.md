# TIERED-FLUID-FLOW-01 独立规格与质量组合审查

日期：2026-10-12。审查者为独立只读执行者，不是 PM。本次只写本报告，不执行 Git 写入、测试、构建、游戏、存档修改或子代理派发。

## 最终审查结论

**本次独立规格与质量组合审查通过，无剩余必改项。** 三项生产 P2 与一项旧测试适配 P2 均已在同次差异复看中完成源码及实际行为闭环；最终交付报告、第二次标准完整 build、保留 XML 与冻结制品身份已核。可由 PM 登记为待客户端验收候选，本结论不表示用户人工验收或 main 合入。下方按时间保留首次发现、失败与整改记录，阶段性的“待核”以本节最终结论为准。

最终 `44-final-build-green.log` 成功，21 秒、`PROCESS_EXIT=0`；独立汇总保留的 `44-full-build-xml`：99 个 XML、543 项、失败/错误/跳过均 0。首次 `43` 的 1 项失败完整保留。本批实际两次标准完整 build；整改仅 `P1BalanceContractTest` 已确认默认值 128→2048 及准确中文职责注释，P0 历史参数和其他方法保持。第二次生产编译/资源/jar 均 UP-TO-DATE，未增加构建或重复测试。

已读最终 `IMPLEMENTATION.md`，运行范围与限制准确：旧代表域实际运行汽轮机 12、汽种 6、换热器回路 4；其余长工况只计源码适配/编译。已独立核对 `source-manifest.json` 的 102 个文件 SHA256/大小全一致，并直接比较生产编译类（含内部类）及资源与 JAR 的 **117 个独立 entry**，零差异；实现者所列 122 次比较包含 5 次前缀展开重复，已准确澄清。临时 review/chunk 结构 namespace 条目为零。

冻结 JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，2,790,597 bytes，SHA256 `364ec4277fc8c8b8ca2a99ef114a86ab17e5fff73a311b905c18d4d21edbf053`。被忽略的新 `IndustrialFluidConfig.java` 已在清单内并核对，PM 仍须按单路径精确纳入交付。基础模型/操作可读性、客户端 Ctrl 选取按键与受影响 Ponder 实际播放等按 `CLIENT-CHECKLIST.md` 保留；自动证据不关闭这些人工门。

## 审查边界与依据

- 生产基线 `07b2c1e`，首次审查 HEAD `da3aa41` 加未提交实现；检查 Git 差异及所有新增 `industrial/**`、`IndustrialFluidContent`、三项 Mixin、五种内容资源与测试，另直接读取被现行忽略规则隐藏的 `config/IndustrialFluidConfig.java`。
- 实际阅读本树 `AGENTS.md`、`docs/project-governance.md`（职责及 5.1/5.2）、流量实施卡与已确认设计；实际应用 `minecraft-modding`、`minecraft-testing`、`requesting-code-review` 的范围、证据和组合审查规则。
- 核对 `gradle.properties`：MC 1.21.1、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6，Java 21。未建议升级依赖、许可证或旧存档迁移。
- 本报告区分源码发现、已有自动证据、尚未提供的最终构建和客户端人工门；不表示功能验收或 main 合入。

## 首次源码结论：需要整改

共有三个 P2 必改项，已即时发给 PM。以下行号对应首次审查源码，整改后的最终结论将在同一报告追加。

1. **P2：入口校验之前按位置去重，合法路径可能被无效侧面吞掉。** `industrial/IndustrialTopology.java:92` 在 `106–112` 的阀/泵入口校验之前将位置加入 `visited`。同一支路较早触达阀的非轴向侧面，稍后经合法轴向的路线会直接跳过；串联泵同类。已知设备端点也在首次无效面被登记后忽略后来的有效面。结果是有实际合法路径却持续阻塞，拓扑重算仍不能恢复。应先排除非法入口，方向相关入口和端点按合法面处理，同时保持物理节点额度按位置共用；用侧面较早触达、轴向合法路线仍交付的真实用例覆盖。
2. **P2：阀红石查询可隐式加载相邻区块。** `industrial/IndustrialValveBlockEntity.java:36,40` 调用 `level.hasNeighborSignal`；调度与许可检查会反复进入 `effectiveLimit`。本地 MC 源确认 `SignalGetter.hasNeighborSignal → getSignal → Level.getBlockState → getChunk`，强导体还会检查二阶邻格。区块边缘阀因而可能加载或续票邻块，与本卡不强载约定冲突。应使用有加载边界的红石读取，缺块时安全关闭并允许加载后恢复；原长管卸载用例没有阀，不能证明该路径。
3. **P2：创造模式仍能复制持液泵缓存。** `industrial/IndustrialPumpBlock.java:41–45` 的放置恢复没有消费或移除持液物品数据，普通 `BlockItem` 在创造模式不扣物品，合法拆下的含缓存泵可以反复放置；`IndustrialPumpBlockEntity.write` 同时无条件保存 `Cache`。锁定 MC 的 `Minecraft.pickBlock` 会在普通 `getCloneItemStack` 后，对 Ctrl+中键额外执行 `saveCustomAndMetadata` 并附加 `BLOCK_ENTITY_DATA`，因此默认空 clone 不能保证所有创造选取为空。现有用例只直接查询 clone、获取 drops 和恢复数据。应保证合法持液项在创造放置中也只搬移一次，并覆盖原生 Ctrl+选取的后置 NBT 路径；不扩大到恶意内存修改或跨版本迁移。

## 已核的实现与自动证据

- 专用交易许可在包内构造，标准受限 fill/drain（含模拟）拒绝；已适配机器及 Create 普通/创造罐共用原库存。未知接收端不授权受限工质，PSI 包装其底层同库 handler，未增加工业 PSI。`FluidNetworkRestrictionMixin` 注入点在 `keepPortableFluidInterfaceEngaged` 之前，早于原生 drain；既有共享容量修正未修改。
- 泵按实转速封顶，管阀及泵共用物理节点预算。源侧按实际扣源收费，目标侧按实际实收收费，短收留同泵缓存。二/三级 8192/32768、单泵 16384@256 RPM、16 SU/RPM；锅炉默认冷热 2048、水汽 4096，反应堆 2048。SC 原生加压/邻罐直推关闭，普通汽及原热账本未重写；独立锅炉尺寸上限仍为默认 11，未夹带 16³ 候选。
- 五种有序配方的输入材料、排列、产量符合已确认成本。两级管、阀与泵有六面基础几何、物品模型、掉落、本地化及创造页；使用现有原版材质。资源静态审查不等于客户端材质、鼠标或取景验收。
- 已核 `09-port-core-green.log` 与 `17-resource-targeted-green.log` 的 Gradle 成功记录；这两个日志自身不打印各类 JUnit 数量，最终计数应以保留的 XML/执行报告绑定，不在本报告推造计数。
- 已核 `15-lifecycle-world.log`：13 个真实测试完成，保存全部维度并成功结束。已核 `26-frontier-reload-focused.log`：真实 `ChunkEvent.Unload=1`、3000+3000=6000 恢复、1/1 与自然保存成功。
- 已核 `28-expanded-industrial.log`：正式工业域 20/20、全部维度保存、`BUILD SUCCESSFUL`、`PROCESS_EXIT=0`。日志明确包含十个真口、两台有效泵、五 tick 各 20480 与有限总量 160000；共享输入边 8192=目标 2000+缓存 6192；锅炉水汽 4096/冷热 2048 与冷口同 tick 1024+1024；SC 过滤恢复 3904；新控制器 5000/旧控制器 3000 不变。该记录属于整改前证据，不覆盖上述三个新发现。

## 不在本次判定范围

- 正式美术动画、基础模型实际客户端可辨性与受影响 Ponder 播放：保留本批集中人工门，不以 JSON/JUnit 代替观察。
- 工程管网压力、泄漏、破裂、管理员、旧版本存档迁移：明确后置或排除，不据此提出实现门槛。
- 第三方直接修改内部库存、恶意内存/任意 NBT 伪造、移动整个 Create 罐：不扩大受限管道交易保证；普通原生创造操作仍在上述缓存问题范围。
- 16³ 锅炉独立候选和用户/其他任务 dirty 文件：不由本批验收，也不覆盖或改写。

## 阶段性整改复看记录

同次差异复看已确认四个生产文件及两语言文件的整改：拓扑在方向校验后去重，端点按位置与面登记；`LoadedSignals` 为原生 `SignalGetter` 提供已加载读视图，含强导体二阶读取，缺块安全关阀并逐 tick 重查；成功放置清除原物品携液数据；原生 `removeComponentsFromTag` 去除选取后的 `Cache`，普通世界保存保持。以上正面覆盖三个 P2 的源码原因，未在本次差异中发现新的必改项。

仍等待唯一实现者提供对应真实用例、受影响旧 GameTest 最终适配、最终一次完整 build、来源/制品身份及 `IMPLEMENTATION.md`。审查者只复核这些实际差异与对应原始证据，不重复运行。当前三个 P2 尚未完成证据关闭，不能写“可交付候选”。

### 旧汽轮机域适配的同次证据复看

已核 `33-turbine-fixtures.log` 的 12/12、自然保存与 `PROCESS_EXIT=0`。`feed/feedBelow` 经过有限源罐、真实动力专用泵与正式调度；`unreportedFeed` 仅读取上次登记库存与当前源库存之差，用于补齐定时断言先于该 tick 记数回调的观测，不改库存或热账本，守恒等式仍要求相等。

新增一项 **P2 测试适配必改**：`gametest/ExtensionTurbineGameTests.java:306` 将原第二物理口同 tick 剩余额度 `==0` 改为 `<=rated`，而 `remainingInput` 本身已用额定值取最小；此时标准 SC `secondAccepted` 又必为零，断言无法再证明同机预算。额外第二路专用 feed 没有统计同 tick 两路实收，额定平均排汽也可能由排汽额度和周转容量掩盖超收。已要求保留新禁运负例，并以调度后同 tick 账本/两路扣量或释放目标容量后的第二路合法交易补回原实质约束，只需该方法的定向证据。本项属于后续旧域适配复核，不是第二轮生产广审。

### 三项生产整改的首次真实证据

已核 `34-review-focused.log` 及新用例源码：本轮 3 项中 2 项通过，边界阀失败，进程自然退出 `1`，不得记为整轮成功。

- P2-1 对应真实绕行与多面用例通过：阀先受非法 WEST 触达，随后合法 NORTH 绕行仍交付 7000；换热器先受非法 WEST 触达，合法 SOUTH 热口交付 4000、有限源保留 3000。
- P2-3 对应真实创造放置与完整 Ctrl 后处理用例通过：同一物品经 `BlockItem.place` 连续放置只得到 3000 与 0；`saveCustomAndMetadata → removeComponentsFromTag → setBlockEntityData → applyComponents` 后选取项无 Cache，原世界缓存仍为 3000。该调用链对应锁定 MC 的原生后处理，未冒称客户端手动按键验收。
- P2-2 对应边界阀用例未通过，仍待定位与 GREEN 证据；不能以另外两项通过关闭不强载门。

### 后续受影响定向证据

已核 `35-review-focused.log`：仅运行边界阀与汽轮机同机两口两项，2/2、全部维度保存、`PROCESS_EXIT=0`。边界阀记录真实 `ChunkEvent.Unload=1`，二阶红石缺块保持卸载且阀关闭，加载后有限库存 6000 自动恢复；P2-2 关闭。原 `34` 的失败保留，不改写。

汽轮机补回调度前后两个优先级的实际采样：31 个同 tick 样本、`InputUsed=216`、两口剩余均零、两源累计 4968+1728、泵缓存零。该证据已不是原 `<=rated` 恒真检查，但默认一 tick 周转容量仍为 216，交付后满容量可能掩盖共享预算拒绝。已向 PM 提出最窄强化：只在此方法采样期间使用原合法配置 `turnoverTicks=2` 并恢复，证明调度后还有空间却不再接受第二口额外量；无需改生产账本或扩大测试域。该测试适配项待此隔离条件确认后关闭。

已核后续 `39-turbine-budget-ample-capacity.log` 及对应方法：31 个真实同 tick 样本均为两源扣量合计 216、`InputUsed=216`、`exhaust=216 < capacity=432`、两物理口剩余额度零、两泵缓存零；两源累计 4968+1728 均有实际成交，测试后 `turnoverTicks` 恢复为 1。1/1、自然保存、`PROCESS_EXIT=0`。容量掩盖已排除，第 4 项 P2 关闭；没有更改生产默认或汽轮机账本。结合 `34` 两项通过行为及 `35` 边界 GREEN，四项发现现均完成源码与定向行为闭环。

整体交付结论仍待最终受影响域、一次完整 build、临时域清除和 JAR/来源绑定。零注册命令不计作真实用例通过。

### 最后受影响域与运行范围

已实际汇总 `40-targeted-xml`：83 项、失败 0、错误 0、跳过 0；`40-final-targeted-junit.log` 为成功且 `PROCESS_EXIT=0`。已核 `41-steam-selection-focused.log` 的 6/6、全部维度保存及自然退出 0：普通汽同 tick 被动 128+主动 3968=4096；两真实汽池/外罐异种背压恢复；低压 SC 由专用路线实际输送 256 并保留真实 HU；再热后专用路线输送 128。原 `37` 的零注册命令不构成通过证据。

已核 `42-heat-loop-fixtures.log`，实际为 **4/4**，并非口头预期的 5 项；正式 `create_nuclear_industry_heat_loop` 域全部维度保存、`PROCESS_EXIT=0`。包含原生冷液单口与三口路线、原生罐分流，以及专用热液至换热器定向路线，有限库存分别保持 13000、4000、5000。

已只读核对其余旧夹具的最终适配差异：受限正例改用有动力专用路径，普通汽/冷液路线保留，原停转/守恒断言没有被整体删除。`BoilerMultiportStopGameTests`、`BoilerTurbineFlowGameTests`、`P1Coolant05CoverageGameTests` 等未运行范围只计源码适配，不声称其全部旧方法现已通过；多口旧方法中此前“无原生泵/直邻SC”的场景已按新合同改为有动力专用路线，旧方法名不再表示原禁运路线仍受支持。受影响源码检查不替代这些长时间现场夹具的实际运行。

首个完整 build 的 `43-final-build.log` 为 **失败**：543 项中 `P1BalanceContractTest.serverConfigUsesFrozenDefaultsAndHasNoAggregateFlowKey` 在第 32 行失败，`PROCESS_EXIT=1`。独立汇总 `43-full-build-xml` 的 99 个 XML，确认 543/1 失败/0 错误/0 跳过。该轮虽已执行 jar，也不能作为最终完整构建成功或交付依据；等待限定整改与后续证据。
