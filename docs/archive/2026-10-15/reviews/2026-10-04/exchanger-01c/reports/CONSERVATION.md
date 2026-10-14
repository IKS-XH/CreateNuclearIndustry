# EXT-B-EXCHANGER-01C-CONSERVATION 诊断报告

日期：2026-10-04。候选：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`；只读核对 HEAD=`3ea9e89`。

## 结论与证据等级

发现与用户保存拓扑直接对应的强根因候选：**同一 Create 管网向同一反应堆的多个冷端分流，模拟阶段重复使用共享剩余容量；实际抽取已经发生，后续接收不足的余量被原生 FluidNetwork 丢弃。** 代码链和离线拓扑均已核对；本报告没有运行真实 GameTest，不能将静态推导或单份保存快照宣称为客户端损失的最终复现证明。

用户已明确合计全部设备后冷热总量仍下降。本报告不再以“只看外部罐”解释该反馈。

换热器存在同类独立风险：同一台机器的多个面返回不同 Port 对象，但共享热罐。只有同一网络同时向多个面填入且剩余空间成为瓶颈时才触发；本次保存的四台换热器各自北入南出，未发现同台多面热入。双面换热器用例可以证明同一机制，不能单独代表用户拓扑。

## 范围与技能

已读 AGENTS.md、治理第1/4/5.1节、01C诊断卡和01A运行合同。实际读取并应用：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：核对世界 capability、服务端库存所有权与锁定API。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：区分直接handler、真实泵管和人工证据；复用既有真实Create布局。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/systematic-debugging/SKILL.md`：先追踪事务和用户拓扑，不猜测修复。

版本由 gradle.properties、build.gradle:13 核对：Minecraft1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82。没有改依赖。未运行Gradle、未修改生产代码/测试/核心文档、未执行Git写操作、未操控客户端、未改原存档、未清理pycache。

本次仅写本报告及同名目录。Create源码摘录来自锁定sources.jar，完整保留原第三方文本与行号。

## 流体事务链

下述路径均相对候选根目录。Create路径均相对本报告同名目录。

| 环节 | 已核对位置 | 结果 |
|---|---|---|
| 反应堆共享冷入 | `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorCoolantFluidHandler.java:127-140,204-215` | 剩余空间为容量减冷热总量；模拟不写库存/预算；执行仅增加本次实际accepted |
| 反应堆热出 | 同文件`:152-169` | 逐端口预算约束，执行只扣实际drained；返回同量热液 |
| 反应堆冷转热 | `reactor/ReactorServerTick.java:94-105`（同Java包前缀）及`ReactorCoolantLedger.java:209-220` | 正式tick输入/输出观测均传0，不重复扣capability输出；冷减converted、热加converted |
| 整数适配 | `reactor/ReactorCoolantSimulationAdapter.java:91-109,135-145` | 同一结算转换到整数快照；未发现稳定运行总mB逐tick减少路径 |
| 换热器转换 | `heat/HeatExchangerState.java:38-48,86-93` | fill/drain返回实际量；转换热减多少、冷加多少；HU散失不减少冷却剂mB |
| 换热器对外端口 | `heat/NuclearHeatExchangerBlockEntity.java:132-134,192-218` | 五面共享两罐；每次fluidPort返回new Port；模拟纯读 |
| 管网目标获取 | `FlowSource.java:82-115` | 每个连接面独立BlockCapabilityCache；不同面可以缓存不同handler |
| 管网模拟累计 | `FluidNetwork.java:184-187,245-255` | `IdentityHashMap<IFluidHandler,Integer>`仅按对象身份识别此前模拟接受量 |
| 管网实际抽取 | `FluidNetwork.java:187-217` | 先模拟整轮，再执行整轮；执行阶段先sourceCap.drain，再给目标fill |
| 管网实际分配与丢弃 | `FluidNetwork.java:219-266` | 目标部分接收会继续分配；所有目标已拒收后剩余transfer直接置EMPTY，无退回或持久暂存 |

### 静态定量例

源罐至少128mB、泵本轮可搬128mB、同反应堆剩余空间36mB、三个冷端各自本tick尚有128mB预算。三个不同handler各自模拟可收36mB，模拟合计108mB。执行先从源罐扣108mB，然后三个口实际总共仅能接收36mB，余72mB在FluidNetwork.java:265-266被丢弃。于是源罐+反应堆总量减少72mB。

这个算例是锁定源码的路径推导，**不是实际运行日志**。具体每tick损失取决于源库存、当前共享空位、流速和参与分流端口数。若热端不断排出、冷入不断补满，就可反复进入剩余空间不足的状态；容量完全满时模拟均返回0，本身不持续扣空源罐。

独立设备、每设备仅一个handler且各自空间足够的普通多分流没有相同共享容量重复计算；不能据此推断所有Create分流都会丢液。

## 用户存档关联：仅离线保存证据

脚本 `EXT-B-EXCHANGER-01C-CONSERVATION/read_region.py` 只读读取 `run/saves/新的世界/region/r.0.0.mca` 和 `r.0.-1.mca` 的有限邻近区块。解析结果为 `offline-region.json`。它不是当前在线内存，也不保证两个区域文件保存同一时刻；管道Flow中的amount=1是流型记录，不计入实际库存。

关键拓扑：

1. 冷液储罐控制器 `(35,-60,-2)`，保存冷液73235mB。
2. 向西泵 `(34,-60,-2)`，保存绝对转速256；east为流入、west为流出，均为compound_coolant。
3. 西向主管 `(33..25,-60,-2)`，沿 `(25,-60,-3..-5)` 上升至 `(25,-59,-5)`。
4. 三个管道 `(25,-59,-5/-6/-7)` 分别向东接冷端 `(26,-59,-5/-6/-7)`。三个管道NBT的east均为Flow In=0，流体compound_coolant；三者均属于同一段连续分流主管。
5. 共享仪表端口 `(28,-59,-4)` 保存ColdCoolantMb=9000、HotCoolantMb=0，SCRAM=true；此刻的静态全满状态不是持续丢液证明，但与此前运行中接近容量的触发条件相容。容量公式按空/控制棒主体格派生，见`structure/ReactorCoolantCapacity.java:39-63`；未将旧快照当实时容量查询。
6. 四换热器位于 `(40/43/46/49,-60,-5)`；北侧热液主管z=-6，南侧各泵z=-4经玻璃管z=-3回z=-2冷液主管，再进上述冷液罐。四台保存冷热罐均0；本次未观察到同一换热器多个热液入口。
7. 水管在y=-58，五个供水泵位于x=44..48,z=-9，其Flow为minecraft:water，通向锅炉北侧；四锅炉控制器保存的TankContent均为minecraft:water。保存拓扑未显示冷却剂回路和水路混接。

这些数据已足以把“三冷口共享容量”列为首要真实复现对象，不能从一份快照计算流失速率或宣称精确丢失总量。

## 其他候选核对

- 正常锅炉入口不会把本模组冷却剂当水消耗。`BoilerData.java:471-482`只接受`FluidHelper.isWater`；`FluidHelper.java:38-39`严格比较convertToStill后是否为Fluids.WATER，非标签匹配。模组冷热液注册的是独立BaseFlowingFluid.Source（`content/ModFluids.java:55,69`）。
- 换热器储备HU上界尾差、余热散失只影响HU；`hot -= converted; cold += converted`保持流体体积，不应改为补液或降低消费来掩盖事务损失。
- 正式ReactorServerTick没有重复计入capability冷入/热出；其TickInput对应项都是0。没有证据支持“反应堆每tick再次扣热液”候选。
- FluidNetwork还有generic drain回退，若读出的流体与network.fluid不匹配会丢弃；本次反应堆热出和换热器冷出各自只有一种可抽液，未发现能够在同一调用内触发该分支错液的生产路径。

## 最小真实Create复现建议

首要用例：真实成型反应堆 + 一个普通Create冷液罐 + 真实泵 + 三个分支冷口，无换热器或锅炉也可隔离本次疑点。使用没有燃料运行的结构，或按旧夹具创建后仅预置冷热库存，保持测试阶段无外部液体产生/消耗。

- 可复用`P1Coolant05GameTests.java:318-356`的真实玻璃管、泵、马达接线及propagateChangedPipe模式；`P1Coolant05CoverageGameTests`多端口用例是直接capability调用，不能替代真实分流。
- 在canonical 5×5壳体同一外壁设置三个相邻冷端，外侧三通主管连接三口，单泵从真实罐向它们推冷液。预置反应堆总量为其实际容量减36mB、源罐2000mB。让世界真实tick传播，断言移动量>0，且每次采样`源罐+反应堆冷热总量`保持初始值。独立单冷口同余量作为对照。
- 辅助同机制用例：单换热器热罐预置3936mB，无顶部负载；一个普通热液罐和真实泵分支到换热器相邻两个侧面。预计只剩64mB空间时双handler模拟允许128，实际仅接64。断言源罐+换热器冷热总量，另有单面同样拓扑对照。
- 可在新`src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionHeatExchangerLoopGameTests.java`与独立`create_nuclear_industry_heat_loop`命名空间/模板实现；由PM授予精确写集并指定唯一Gradle执行者。诊断阶段不改生产实现、不把预期失败改成通过合同。

## 最小修复思路与边界（未实施）

1. **同一换热器多面**：在同一有效capability epoch内，所有可用面复用一个稳定Port实例，使Create对象身份累计覆盖共享热罐。epoch变更时换新handler，保留旧句柄永久失效合同。单纯全生命周期final缓存会破坏卸载/恢复边界，不能这样改。
2. **同一个反应堆物理口多个面**：可在同一绑定生命周期内缓存稳定handler以消除同口别名，但必须处理owner重绑/旧句柄失效；只能解决同一物理口。
3. **多个不同反应堆冷口共享库存**：缓存每口handler仍无法解决用户保存的三个不同端口。把全反应堆返回同一handler又会失去物理口独立128mB/t预算和绑定边界，违反现有合同。不能以该“简化”当修复。
4. 在保持无独立端口库存、模拟纯读、共享容量和逐物理口限流合同的前提下，后续需评估**Create分流模拟对共享库存所有者的识别**，同时分别保留各端口预算；应先以真实失败用例锁定，不直接引入通用管网重写。当前无现成Mixin接线，本报告不批准/实施扩建。临时把网络只接一个冷口可作为诊断对照，不作为多端口合同最终交付。

### PM追加问题：保留合同的最窄共享接收计划

Create现有算法仅以一个handler身份同时代表“库存空间”和“接收限速”。本项目多个物理口共享库存、各口独立限速，天然需要两层身份。仅靠无上下文的纯读`IFluidHandler.fill(SIMULATE)`，无法判断上一次其他口的模拟调用究竟是同一管网计划的一部分，还是另一个独立查询；因此不能在handler内按tick累积模拟预约解决。

建议比较结果：

| 方案 | 能否保持合同 | 边界 |
|---|---|---|
| 同BE/同物理口按epoch缓存handler | 可以 | 修同一物理实体多个面对象别名；不解决多个反应堆物理口 |
| 全反应堆所有冷口返回同handler | 不可直接采用 | handler丢失物理口上下文，容易把全堆限速合并为128；原Create缓存不会每次重新提供端口上下文 |
| 各口均分共享剩余空间 | 不保持既有任一口可用共享容量语义 | 未接管道的口也会占虚拟份额，近满时可能让实际接入口永久得不到尾数空间 |
| 每tick保存SIMULATE预约 | 不保持模拟纯读/重复查询语义 | 外部查询即占位，取消模拟或其他网络会影响真实吞吐，tick结束也不是一次事务边界 |
| 仅该次Create分流SIMULATE的临时计划 | 可保持，需真实测试 | 最窄可行兼容方向；仅改目标模拟接收估算，生产库存/预算仍由原EXECUTE处理 |

临时计划只在一次原生FluidNetwork分流模拟作用域内存在，识别三层键：handler对象、物理端口（或共享的port budget对象）、库存owner。对本项目已识别冷端才执行规则；其他mod/原生handler完全沿原路径。

对一次原生传入的累计请求R，令H为当前handler上次累计模拟接受量，I为同owner所有handler已计划接受量，P为同物理口所有handler已计划接受量，C为该owner实际共享剩余容量，B为该物理口本tick实际剩余预算：

```text
newH = min(originalFill(R, SIMULATE), max(0,C-(I-H)), max(0,B-(P-H)))
I = I - H + newH
P = P - H + newH
H = newH
```

原生`accumulatedFill`传入的是累计量，因此必须先移除本handler旧计划份额再重算，不能把R或newH每次直接累加。不同物理口拥有不同P/B，但共享I/C，从而保留各128mB/t和单一总容量。多个面返回不同handler时也可通过物理口键归并预算。

实现前需确认的约束：

- handler提供只读owner和物理port/budget身份及实际可用量；原fill/drain语义不改，普通重复SIMULATE返回值仍相同。
- 原生EXECUTE仍走现有真实fill，临时计划不能直接写库存或消费流量；服务端同一次同步管网调用内没有世界tick插入，才可以使用当前只读容量。不要改为先填后抽制造另一种源不足复制风险。
- 作用域需要明确开始、结束与异常finally清理，不能跨网络、跨tick或通过猜测调用栈识别事务。可由明确的局部Create调用兼容入口承载；本报告没有批准具体Mixin技术/构建改动。
- 配额紧张时要覆盖原Create多轮重新分配和不同输出顺序；其执行分配未必逐目标等于模拟计划，但总容量及各物理口预算限制必须仍可接纳已抽取总量。最终以真实泵管守恒用例确认，不能仅凭以上公式宣称修复。
- 必要验证：三个同owner冷口近满守恒、容量足够时不同物理口各128未合并、同口多面不绕过128、独立owner不相互占位、不同网络顺序运行无遗留计划、重复SIMULATE纯读、阻塞零抽取、原生不支持目标透传、生命周期失效拒收。按实际修复触及范围挑选已有用例，不机械全量。

## 本次验证与交付状态

PM追加的锁定Mixin环境、唯一fill接入、每次方法调用的临时计划边界、精确建议写集及显错诊断方案，见同名目录`compat-design.md`。最小建议已收敛为`WrapOperation + @Share LocalRef`，不使用ThreadLocal或WrapMethod全局作用域。

执行：只读Git状态/HEAD、锁定源码追踪、只读NBT解析脚本。Python命令：`C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe build/reports/extension/EXT-B-EXCHANGER-01C-CONSERVATION/read_region.py`，退出0，读取687个目标模组方块/586个邻近BE记录。没有启动Gradle或Minecraft进程。

Python/py的WindowsApps别名不可用，随后使用已发现的内置Python；未安装工具。PowerShell一次花括号路径语法错误后改为目录rg，不影响源码/存档。

本交付是根因候选及用户拓扑关联报告；真实复现、生产修复与人工闭环验收仍待各自证据。既有未跟踪诊断卡和tools/art-assets/__pycache__/均保留。
