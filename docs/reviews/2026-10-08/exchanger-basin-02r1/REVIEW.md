# EXCHANGER-02R1 合并规格与代码质量审查

**当前结论（2026-10-08 对应整改复核）：通过02R1规格与代码审查。** 原P1/P2已关闭，对应差异没有剩余整改项。已读取账本JUnit 21/21、最终GameTest 7/7及增量assemble成功证据；审查者未运行测试。客户端持续供热与汽轮机分别验收仍待用户确认，旧02按批次的报告不替代02R1验收。

**初审历史结论：需要整改，尚不通过。** 当时确认P1/P2各一项，初审自动证据为20/20、6/6，未覆盖两项问题。以下初审正文保留当时行号、判断和证据；当前状态以本文末尾整改复核为准。

**范围与方法：** 候选 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，文档基线 `8dbb9b65a9dddcdc7233031b97c5ef9a058b91e7`，冻结未提交实现。实际读取最新 AGENTS、治理5.1/5.2、02R1实施卡与 IMPLEMENTATION，实际读取/应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`，核对 Minecraft1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82。只读代码、锁定原生源码和既有日志；未运行Gradle、测试或客户端，未派发其他执行者，未执行Git写操作。唯一写入本报告，既有日志/cache不纳入功能缺陷。

## 初审必须整改（历史，现已关闭）

### [P1] 当前世界tick严格相等使供热依赖方块实体执行顺序

位置：`src/main/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerState.java:158-159`，调用为 `heat/HeatExchangerBasinBridge.java:18`。

`basinHeatingAt(now)` 只允许 `basinHeatTick == now`。世界进入tick T后，如果搅拌器在换热器之前执行，它读到的最后已付记录仍是 T−1，于是原生匹配和实际apply都读取NONE；之后换热器才转换4mB、记录T并唤醒盆检查。下一tick搅拌器先执行时又只见T记录，继续被当作无热。该顺序下需热加工不能启动/提交，换热器却一直耗液。先放盆/搅拌器再放或重放底部换热器，以及重新登记ticker，都可能出现这种合法顺序。

原生依据：既有锁定Minecraft/NeoForge源码 `build/reports/extension/EXT-B-EXCHANGER-01A-DEVICE/Level.java:523,553,557-565` 按登记的列表顺序调用ticker，没有“热源先于搅拌器”的排序合同。锁定Create `BasinOperatingBlockEntity.java:69-83,119` 在自身tick内匹配/提交；`BasinRecipe.java:73-76` 当时读取盆热级。已直接读Create源码JAR中的 `DeferralBehaviour#tick`，它只在当前操作器tick执行待检查回调，不能把回调移到热源之后。实时绕开Create缓存没有解决这项时间先后问题。

需要让最后一次真实设备付款的发布状态在下一次设备结算前按合同保持可读，并继续用当前实例、可tick、盆存在/用途、模式与直列有效性拒绝停用源；查询不能补做付款，也不能恢复40tick余热。最小复验为一个明确“mixer先登记、exchanger后登记/重放”的真实需热加工代表，附断流后下一设备tick撤热、恢复及停止ticker的相关断言；不重复已有全部配方/锅炉矩阵。现有GameTest `setup` 在`:148-150`先放换热器后放mixer，未证明反向顺序。

### [P2] 进入盆用途时未清旧锅炉HU，旧储备被当作舍入余额

位置：`src/main/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerState.java:83-96,128-148`。

`tickBasin` 只在跳tick或部分配置变化时清reserve，没有在普通锅炉用途切入盆用途时清掉旧储备。与之相反，盆转回普通用途已有`:281-283`清理。连续tick且密度/窗口不变时，旧锅炉的几十/几百HU直接成为 `roundingBalance`，违反盆路径唯一储备只能小于1mB等效HU的界限。

该问题不仅是显示余量：在合法密度5HU/mB、默认盆费2HU/t下，具有旧锅炉储备的换热器改放盆，首个盆tick计算 `requested=floor(2/5)=0`；旧储备足额使代码不向上补1mB，随后 `converted=0` 也发布SEETHING，并从旧锅炉储备扣2HU。只要仍有热液及冷罐空间，就能跨用途用旧余热支付多个盆tick。默认密度下也会保留远大于1mB的旧余额。无需旧存档：当前版本先给Create锅炉供热积储，再把上方负载改成盆即可触发。

需要在进入持续盆路径时明确清除非盆用途储备及相关分数状态，只保留来自连续有效盆结算的有界舍入余额。最小复验为一项同一账本先走普通锅炉tick产生非零储备、下一tick切盆的断言，覆盖密度大于单tick费用时不借旧储备、余额严格小于密度、冷热等量；不是历史格式迁移测试。

## 对每tick唤醒的质量判断

`HeatExchangerBasinBridge.java:32-35` 每个有盆设备tick都 `scheduleUpdate()`，即使供热状态没变或未付款。锁定Create的Deferral行为会在每个mixer tick调用 `updateBasin`；有效动力、非运行且盆可继续处理时，即使输入长期没有合适配方，也反复运行原生候选/过滤/输出模拟。它不解决P1的先后问题，也没有必要用它维持固定供热；Create已经处理内容、过滤、速度和周期结束的检查通知。

这项静态重复工作没有本轮性能测量，不单列第三项阻断。建议随P1定向整改把热桥的额外唤醒限制在热源有效性/供热状态变化或首次接入等必要边界，保留由Create自身事件处理输入变化，不重新引入配方需求探测。新热查询本身仍须纯读取。

## 其余合同核对

- 负载仅取顶部盆存在及设备/直列有效性，不读配方、过滤、输入输出、动力或盆工作状态。源tick中的mixer查找只用于通知，不决定固定耗热。
- 原批次 ThreadLocal、matcher/apply入口、批次预留/费用字段已撤下；三个旧Mixin文件及注册已删除，保留的热查询Mixin仅识别本设备。原生燃烧室走原缓存/热级逻辑，两项真实互操作有最终GameTest证据。
- 成本为 `basinHeatLevelEquivalent × huPerLevel`，默认2HU/t、0.5HU/mB对应4mB/t；配置只改变费用，盆热查询始终SEETHING。`validBasinSettings` 独立拒绝费用高于额定功率等非法组合，未使旧锅炉共享配置失效。整数mB采用同一reserve结算，连续零初储测试的分数余额有界；P2是用途入口缺失，不能由这些零初储断言覆盖。
- 断流/冷液空间不足的实际设备tick撤热且不使用40tick余热；移除盆后改走无盆分支，`validSource` 拒绝停止ticker、失效实例、锅炉接管、模式和直列冲突。除P1时间边界之外，没有静态发现查询扣款、重复同tick换液或新增热池。未扩大审查未变锅炉/冷凝算法或旧存档。

## 实际读取的最终证据

- `build/test-results/test/TEST-com.iksxh.create_nuclear_industry.heat.HeatExchangerStateTest.xml`：20项，0失败/0错误；连续费用、倍率/额定配置、同tick幂等、分数mB、冷罐近满/恢复等5项盆账本测试。`build/reports/extension/EXT-B-EXCHANGER-02R1/unit-assemble-final.log` 构建成功，`.exit` 为0。本轮读取而未运行，不计入此前冷凝6项。
- `.../gametest-final.log`：6/6 required通过，全部维度保存及正常关闭，Gradle成功；`.exit` 为0。结合测试代码确认空盆无mixer仍耗热、断流恢复、普通/超级各一条实际产物和持续换液，以及原生普通/超级燃烧室互操作。成功夹具尚未覆盖P1反向ticker顺序与P2非零旧储备进入盆。
- 首轮完整失败日志确被末轮误覆盖。`gametest-initial-failure-observation.txt` 明确只是可确认的失败摘要，记录恢复用例误用单tick精确液量的失败和夹具调整；本报告没有把它当原始完整日志，也不要求为补日志重跑。
- 本轮只读核对JAR SHA-256为 `ED713FF510AEA1FD24E2DF3D957596E70DA27CCE5C55B7F1BC256022E0A7A66B`，与实施报告一致。`git diff --check -- src/main/java src/main/resources src/test/java` 返回0，Mixin/两种语言JSON解析成功。

报告及所审实现保持冻结，交PM统一安排两项定向整改。修复后只复核实际差异和上述最小必要证据；客户端持续耗热/断流冷满恢复、护目镜显示与汽轮机播放仍分别待人工验收，不由自动结果提前认定通过。

## 2026-10-08 对应整改复核

**复核范围：** 仅读原P1/P2和关联通知改动、对应单元/真实机器用例及最终日志；没有重启全轮审查、扩大未变锅炉/冷凝范围或运行测试。文档基线仍为 `8dbb9b65a9dddcdc7233031b97c5ef9a058b91e7`，实现保持冻结未提交差异。

- **P1关闭。** 当前 `HeatExchangerState.java:166-168` 接受付款tick及紧邻下一tick，超过一个相邻tick拒绝读取；查询不修改时间标记/余额。源下一次实际结算先撤旧标记，缺液或冷满时不再发布；桥仍检查当前实例、可tick、盆用途及直列/模式有效性。`mixerRegisteredBeforeSourceProcessesHeatedRecipe` 实际先登记盆/mixer、最后登记换热器，断言原生铅玻璃产出1个和100tick等量转冷400mB，列于最终7项通过。原断流恢复及账本冷满代表仍通过；相邻tick可读、超过相邻tick不可读已有当前JUnit断言，不恢复40tick余热。
- **P2关闭。** 当前 `HeatExchangerState.java:85-92` 仅允许相邻连续盆tick保留有界舍入额；新进入盆用途先清普通锅炉reserve、锅炉flowFraction及盆尾差。新增 `enteringBasinDiscardsOrdinaryBoilerReserveAndFlowTail` 先以密度5HU/mB通过普通锅炉tick形成15HU，再切盆并断言实际转1mB、总冷液4mB、热液96mB、盆余额3HU且小于5HU。该真实账本状态转换已覆盖初审跨用途借热问题，不是旧存档迁移测试。
- **唤醒质量判断已落实。** `HeatExchangerBasinBridge.java:29-30,33-48` 只在供热状态变化或当前mixer实例首次接入/替换时安排原生检查，稳定状态不每tick安排。设备暂停、卸载和冲突路径提供撤热通知；实例、区块和热源有效性仍为只读查询门。首次接入/暂停通知为静态对应检查，未冒称新增独立生命周期GameTest；其最终桥接候选已完成下列7项真实回归。

**实际新增证据：**

- 当前 `build/test-results/test/TEST-com.iksxh.create_nuclear_industry.heat.HeatExchangerStateTest.xml` 为21项，0失败/0错误，包含两项对应边界断言；`build/reports/extension/EXT-B-EXCHANGER-02R1/unit-assemble-review-fix-2.log/.exit` 为构建成功、0。之后只有桥/通知改动，账本实现未变，依治理5.1复用该21项结果，不伪称最后一次桥整改后又运行JUnit。
- 最终 `assemble-review-fix-3.log/.exit` 为增量打包成功、0；`gametest-review-fix-3.log/.exit` 为7/7 required通过、0，全部维度保存并正常关闭。原6项持续供热、断流恢复及原生普通/超级代表和新增反向登记代表均已通过。
- 本轮只读实算最终JAR SHA-256为 `3271FAFB1CEC8E9D977223D3CF461022081388074C53B1DEBC00D8B2F1385D6A`，与最终 IMPLEMENTATION 一致。初审首轮完整失败日志被覆盖、仅剩摘要的历史限制继续保留；本轮新增证据使用独立日志名，不为补历史日志要求重跑。

**交付判断：** 对应问题已消除，可交PM管理候选版本与集中手测。工作盆持续耗热、断供/冷满恢复、护目镜界面及汽轮机播放仍分别待人工；本报告不替代用户客户端确认或授权合入主工程。本轮只修改本审查报告，未执行Git写、代码/构建修改、Gradle或客户端操作。
