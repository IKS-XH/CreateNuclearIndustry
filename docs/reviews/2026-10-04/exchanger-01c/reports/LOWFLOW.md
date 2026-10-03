# EXT-B-EXCHANGER-01C LOWFLOW 诊断报告

日期：2026-10-04

- 候选工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；检查时 `HEAD=3ea9e89`，与任务指定基线一致。
- 本报告及唯一附属脚本：`build/reports/extension/EXT-B-EXCHANGER-01C-LOWFLOW.md`、`build/reports/extension/EXT-B-EXCHANGER-01C-LOWFLOW/lowflow-ledger.js`。未改源码、测试、核心文档、构建文件、用户客户端或存档；未运行 Gradle/GameTest；未执行 Git 写操作。
- 必读已读取：`AGENTS.md`、`docs/project-governance.md` 第1/4/5.1节、`docs/superpowers/plans/2026-10-04-heat-exchanger-loop-diagnosis.md`、01A实施卡、01A设备报告及锅炉API源码报告。
- 实际读取并应用技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`（按锁定版本追踪服务端BE与Create接入）；`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`（区分纯账本复现、真实锅炉用例与客户端验收；本任务按指示不运行Gradle）；`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/systematic-debugging/SKILL.md`（先追踪状态数据流、构造可复现假设，再给建议）。锁定 Minecraft 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82；未套用技能中更新版本样例。
- Create 源码证据来自指定的 `create-1.21.1-6.0.10-280-sources.jar`，SHA-256 `376DE15CA5ACF720106A075CA4EB2EF53E63E0E5D9EC93523A1ABBDD0F9F0CB4`；JAR 内源码位置与行号在下文标明。

## 结论

**高置信度的代码级根因：** 默认模式只有一个离散输出态：账本储备够付本tick时，来源发布完整18级并扣18HU；否则发布 `-1`。热液转换量却按当tick真实可转换的整数mB计入HU。因此，长期输入少于36mB/t时，源不能连续维持18级：已付储备先托住额定供热，随后热源关断，低量输入在停机阶段重新蓄满720HU后再启动。输入越低，开机段占比越低，动力表现为18级阶跃而非平滑降级。逐tick复现见下表和附属脚本。

**高置信度：不是账本把不足一整数mB的流体丢掉。** 默认密度0.5HU/mB与18HU/t恰好对应36mB/t整数目标。每tick实际转换量为 `min(请求预算, 热罐库存, 冷罐剩余空间)`，同一个整数 `converted` 从热罐扣除、加到冷罐，再按 `converted × 0.5` 增加储备；未转换的热液保留在热罐。请求目标的小数余量也保存在 `flowFraction`。输入填充和冷液抽取均以账本实际接受/抽取量返回。由反应堆输出、管路分配、外罐总库存变化引起的损失不在本LOWFLOW报告范围内，另由CONSERVATION任务检查。

**锅炉的低供水限级和换热器的固定耗热是两条独立路径。** Create 将18作为聚合热源分数；锅炉的有效等级另取热源总分、锅炉尺寸上限及近期供水上限三者最小值。桥接只要求锅炉带引擎/汽笛、尺寸至少一级、供水至少一级；一旦合格，它不按锅炉最终有效等级调低换热器耗热。故供水仅支持1级时，18级热源仍每tick付18HU，而锅炉有效等级可以只有1。若水样本降到不足一级，桥接会判为无负载；账本进入 `no_load` 分支并停止转换、按18HU/t消耗已储HU。单一来源时锅炉会从该来源的18级直接降至无热。

上述是源码和明示账本的结论，**不是对用户存档或截图参数的实机测量**。当前没有获取运行日志、精确反应堆流量、各热换热器分流量或Create近期供水采样值，所以可以高置信度解释“为什么低量会周期停热”，但尚不能把某一台实际锅炉的每次短停唯一归因于热液断续或供水资格跌破阈值。

## 源码追踪

- [HeatExchangerState.java](../../../src/main/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerState.java#L17)：`Settings.rate()` 是 `heatLevel × huPerLevel`，`capacity()` 是 `rate × bufferTicks`（第17–27行）；罐库存以整数mB存储，储备及目标流量余数是有限浮点值。配置入口 [HeatExchangerConfig.java](../../../src/main/java/com/iksxh/create_nuclear_industry/config/HeatExchangerConfig.java#L14) 默认 `heatLevel=18`、`huPerLevel=1`、`bufferTicks=40`（第14–19行），密度读取既有P1配置（第26–30行）；P1默认密度0.5HU/mB，见 [P1ServerConfig.java](../../../src/main/java/com/iksxh/create_nuclear_industry/config/P1ServerConfig.java#L71-L73)。所以额定热流是18HU/t、容量720HU，额定冷却剂流量为36mB/t；40个额定tick对应1440mB转化量。
- [HeatExchangerState.java](../../../src/main/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerState.java#L51)：同世界tick只处理一次（第56–61行）；无效配置安全清储备（第63–69行）；没有合格负载时不转换且扣当前额定HU（第74–78行）。有负载时先尝试支付完整18HU并只发布18级，否则停止运行（第80–85行）；随后计算 `36mB + flowFraction` 的目标、保留不足1mB的目标余数、对当前热罐和冷罐空间进行整数限流，热罐扣除量与冷罐增加量相同（第86–93行）；储备满720HU才重新置为运行，输出状态在18级和无热间切换（第94–96行）。
- [NuclearHeatExchangerBlockEntity.java](../../../src/main/java/com/iksxh/create_nuclear_industry/heat/NuclearHeatExchangerBlockEntity.java#L59)：服务器tick用桥接资格作为 `load`，调用同一账本，再把账本结果发布到Create（第66–95行）。Create回调只是读已发布值（第59–63行）；尺寸限制和供水限制仅用于状态显示（第83–84行），不会把 `Settings.heatLevel` 改成锅炉实际有效热级。流体口使用账本实际接受和抽出的整数数量（第192–218行）。
- [HeatExchangerBoilerBridge.java](../../../src/main/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerBoilerBridge.java#L48)：来源回调返回当前已发布热级或 `NO_HEAT`（第48–53行）。负载资格要求上方Create储罐锅炉、活动引擎/汽笛、尺寸级别至少1、供水级别至少1（第55–71行）。多热源聚合重新读取锅炉底面的每个热源，对正值强转整数求和并赋入 `activeHeat`（第73–96行）。
- Create `BoilerData.java`（JAR内路径 `com/simibubi/create/content/fluids/tank/BoilerData.java`）：服务器锅炉每tick处理热级更新；供水采样每5tick一次，将最近一个5tick窗口收到的实际填充mB除以5，并在十个采样窗口轮转时重新选取这十段中的最大值（第46、89–129行）。供水有效热级为 `min(18, ceil(waterSupply)/10)`（第161–167行）；锅炉实际热级为 `min(activeHeat, sizeLevel, waterSupplyLevel)`，锅炉尺寸等级为 `min(18, size/4)`（第161–192行）。活动仅看已连接引擎或汽笛（第410–412行）。因此供水样本会滞后/保留近期峰值；不供水后并非立刻失去资格，样本更新轮转后才会跌落。因为源码先 `ceil` 再整除，一级资格的精确边界是样本值大于9mB/t，而不是数学上的精确10mB/t（例如9.1→`ceil=10`→1级）。
- Create `BoilerData.BoilerFluidHandler.fill`（同JAR源码第453–483行）只在执行填充时累计 `gatheredSupply`（第475–483行）；查询/模拟不增加供水样本。`getEngineEfficiency`再以该锅炉实际热级和引擎数计算效率（第178–185行）。

01A历史证据只表明其对应合同和原用例曾验证真实Create满水供热及10mB/t供水受限，不是本次低流量实机测试。见 [01A设备报告](../../../docs/reviews/2026-10-03/exchanger-01a/EXT-B-EXCHANGER-01A-DEVICE.md#L9) 第9–13、52行和 [Create API报告](../../../docs/reviews/2026-10-03/heat-api-01a/EXT-B-API-01A.md#L10) 第10–17行。01A合同明确18级、40tick预热/余热和小锅炉仍按额定流量，见 [01A实施卡](../../../docs/superpowers/plans/2026-10-03-ext-b-exchanger-01a.md#L32) 的冻结行为第2–5项。

## 账本逐tick计算

附属脚本 `lowflow-ledger.js` 逐句复现账本的默认满负荷分支和默认参数，不调用游戏代码。模拟假设：换热器一直有合格锅炉负载；冷罐生成液体每tick及时回流排空，避免混入“冷罐堵塞”；已注入热液不溢出4000mB罐；离散输入为实际交付的整数mB。脚本运行命令：

```powershell
node build/reports/extension/EXT-B-EXCHANGER-01C-LOWFLOW/lowflow-ledger.js
```

稳定段输出摘要（模拟第1001–10000 tick；开/关段为代码边界，有限窗口比例受截窗相位影响）：

| 实际热液输入 | 账本行为 | 18级发布比例 |
| --- | --- | ---: |
| 36mB/t持续 | 预热40tick后持续18级；每tick输入热值恰好抵消18HU扣款 | 100% |
| 18mB/t持续 | 每段79tick开 / 79tick关；储备在额定供热和半额转化间往返 | 约50% |
| 9mB/t持续 | 每段53tick开 / 159tick关 | 约25% |
| 每20tick一次180mB（平均9mB/t） | 热液罐缓和输入脉冲，但HU平均流入仍为需求四分之一；复现为50tick开 / 150tick关 | 25% |
| 1mB/t持续 | 可转换每个整数mB，没有丢弃；每段41tick开 / 1435tick关 | 约2.8%稳态 |
| 每40tick一次36mB（平均0.9mB/t） | 输入更稀疏；每段41tick开 / 1599tick关 | 约2.5%稳态 |

能量收支核对：长期平均冷却剂输入 `q mB/t` 可产生 `0.5q HU/t`。若固定18级连续供热，需求是18HU/t，因此持续运行必要条件是 `q ≥ 36mB/t`（忽略配置密度改变及溢流）。例如q=18时每tick净储备变化在开机时为 `-18 + 18×0.5 = -9HU`，储备下降至无法先付18HU后停止；关闭时不花输出热，只把18mB转成冷液、回充9HU/t。720HU用约80tick开机段消耗，停机段再约80tick充回，正是79/79离散序列。此循环不可能在平均热输入不足的条件下长期保持18级，除非凭空补能或降低输出需求。

## 根因置信度与诊断边界

- **高：低于额定36mB/t会产生18/无热周期。** 账本源码有直接条件，脚本对恒定输入与批量输入可重复得到周期；无需假设管道整数损失。
- **高：供水/尺寸低限级不会把换热器耗热等比例降下来。** 热源回调只公开完整18或无热；Create单独限制锅炉有效级，桥接没有按最终有效级扣减的回路。
- **高：账本未转换的整数mB不会消失。** 热罐、冷罐采用同一实际转换整数；小于本tick36mB预算的已有输入也会被部分转换。目标流量余数写入NBT，停tick的缺口只耗已付HU。
- **中：用户观察到的某台锅炉短暂停机是否恰为本账本造成。** 若该台只有一个有效热源且该源在上面的 `heat=-1` 区间，则热源总级归零，锅炉当前有效热可归零；若同一锅炉底面有其他有效热源，则总热会由其他源保留。还需排除水样本资格（约50tick采样环）跌到一级以下、罐尺寸低于1、引擎/汽笛断开或冷回液受阻。任务输入没有实机逐tick日志/参数，截图不能代替这些测量。

## 守恒可行方案与取舍（仅建议，不改平衡）

1. 恒定低流量且可接受服务端全局热级时，可用现有配置匹配输入：`L = floor(q × density / HU-per-level)`。例如18mB/t、密度0.5时选9级，需求18mB/t，储备容量为360HU；代价是全服换热器峰值都降为9级，输入断续时仍会波动。
2. 若需要保留18级峰值并让低流量自动稳定，采用文末“按720HU储备逐级供热”候选方案；它改变现有固定18级合同，必须先由用户确认。固定18级在平均热液输入低于36mB/t时无法连续维持，任何算法都不能绕过这一守恒边界。
## 已执行检查

- `git rev-parse --short HEAD`：`3ea9e89`。
- `Get-FileHash` 对锁定Create sources JAR 得到与既有API报告一致的SHA-256。
- 账本诊断脚本 `node build/reports/extension/EXT-B-EXCHANGER-01C-LOWFLOW/lowflow-ledger.js` 退出码0，输出上表的逐段结果。脚本不是游戏运行证据。
- 未运行Gradle、JUnit、GameTest或客户端；本次静态诊断与最小账本计算不触发治理5.1扩大测试条件。真实系统总冷/热液守恒仍属于并行CONSERVATION报告和后续闭环实机人工门。




## 待用户确认：按720HU储备逐级供热（纸面候选）

本节是当前唯一推荐的自适应候选。按实际储备决定输出档位，不另设流量估计器；20tick输入均值方案已撤回，因其会形成输入估计与720HU转换上限之间的自限反馈。

**逐tick规则（纸面建议）：**

1. 读本tick结算前储备 `R`，计算 `L = min(18, floor(R / 40HU))`。`L` 是本tick输出上限：每一级必须先从储备支付恰好1HU，然后才能通过只读热源回调发布；发布范围仍是0–18。此处保留当前默认1HU/级/tick。
2. 扣除 `L HU` 后，按当前配置目标、热罐实有量及冷罐空间计算本tick整数mB转换量；转换只增加 `converted × 0.5HU/mB` 的实际热。再把转换量限制在剩余HU容量能完整容纳的mB数内。默认密度下，储备头寸不足0.5HU时不转换该mB，热液留在热罐；不得先转换再用720HU上限截掉热值。
3. 若本tick `converted=0`（热液耗尽或冷液回罐无空间），连续无转化计数加一；任何实际转化都清零计数。达到40个连续无转化tick后，不晚于第40个tick结束时撤销热发布并散掉剩余储备，保持断料/堵塞余热窗口上限。原世界时间戳/计数随保存、携物及卸载恢复，不因查询、模拟或拆放重置。负载资格消失时仍沿用旧规则：立即不发布、不转换，并按18HU/t消耗储备。

该递推可直接手算，不存在输入估计与储备上限互相卡住的回路。令每tick实际热输入为 `H` HU，供热等级 `L=floor(R/40)`，未触及容量上限时储备变化为 `ΔR=H-L`。恒定18mB/t对应 `H=9HU/t`：当储备处于约360HU时 `L=9`，每tick支付9HU、转换18mB补回9HU，储备停在约360HU而持续发布9级。恒定36mB/t对应 `H=18HU/t`：储备增长到720HU时 `L=18`，每tick支付18HU并补回18HU，停在720HU而持续发布18级。超过36mB/t的部分仍留在热罐，因为转换预算上限保持36mB/t。一般情况下低于36mB/t会在相邻整数级之间调整；例如9mB/t输入给出4.5HU/t，储备围绕约200HU运行，输出为4/5级交替、平均4.5级。

长期输入低于2mB/t意味着不足1HU/t；储备达到约40HU前等级为0，越过阈值后只偶发支付并发布1级，随后回落到0再蓄积。它不会承诺持续1级。间歇性输入先进入最多720HU的真实储备，因此其能量以逐级输出而非整段18级脉冲释放；若任意连续40tick完全没有转换，余热计时仍强制终止并清除余额。

**相对01A冻结行为的变化：** 当前方案必须先完整预付720HU，之后固定发18级；默认36mB/t需约40tick满预热，18mB/t需约80tick才满预热。新方案不再等待720HU满仓：储备每跨过40HU就逐步提高可付整数级，输入36mB/t时会逐渐升至18级、18mB/t时逐渐收敛至9级。升满速度取决于实际输入、当前储备和整数门槛，**不承诺40tick达到18级**。断料/堵塞时等级随储备下降自然降低，40个连续无转化tick仍作为最迟撤热/散储备门；无负载仍按既有规则散热。因预热功率曲线、输出热级和余热耗散时序都改变，必须由用户确认后另派实现；锅炉供水、尺寸及Create限级规则不变。

本方案为纸面候选，未编写或运行模拟器、Gradle、JUnit、GameTest或客户端。后续若获确认，定向核验可只针对36mB/t→18级、18mB/t→约360HU/9级、低于2mB/t的0/1级间歇、36mB/t脉冲、冷回液堵塞40tick、无负载18HU/t散热、720HU上限保留未转换热液、整mB守恒及查询/模拟纯度。
