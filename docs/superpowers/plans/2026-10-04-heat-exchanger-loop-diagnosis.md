# EXT-B-EXCHANGER-01C：闭环冷却剂与低流量诊断

**状态：守恒修复及自适应供热已通过定向验证，2026-10-04用户确认手测通过，已合入main。** 见[最终验收](../../reviews/2026-10-04/exchanger-01c/ACCEPTANCE.md)。用户于2026-10-04在封闭反应堆/换热器/Create锅炉系统中反馈冷却剂持续减少、低流量时输出动力波动和单台锅炉短暂停机。下文保留分阶段诊断与派发过程，后续已证实结论不追改早期假设；本次人工通过依据为用户最新反馈，不能追溯以01B自动检查替代。

## 边界

- 候选`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，基线`3ea9e89`；保留`tools/art-assets/__pycache__/`，主目录用户`.vscode/launch.json`及用户客户端/存档不改。
- 冷热冷却剂全系统总量应守恒；先区分外部罐降低与转移到内部库存、储罐/锅炉混接和真实事务损失。低于额定供热流量导致停机是否属于既有18级/40tick方案的预期，必须以代码及复现判断；禁止擅自加产热、补液或改配置掩盖根因。
- 锁定Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82，不升级依赖。
- 必读AGENTS.md、治理协议第1/4/5.1节、本卡及01A运行合同。实际使用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`与系统化排错技能。报告区分静态推断、真实复现、人工信息与待决取舍。

## 并行诊断

**CONSERVATION：** 只读反应堆流体账本/能力、换热器能力、锁定Create管网与锅炉源码，追踪simulate/execute、部分接收、多输出分配和真实容器消耗。可只读`run/logs`和`run/saves/新的世界`中相关区域快照，仅作离线证据；不操控或关闭用户客户端，不修改原存档。唯一允许写`build/reports/extension/EXT-B-EXCHANGER-01C-CONSERVATION.md`及同名目录（源码摘录、诊断脚本、离线快照均可）。先提交根因候选及最小复现建议；真实GameTest或生产修复另由PM明确写集授权。

**LOWFLOW：** 只读HeatExchangerState、锅炉桥接、服务端tick与Create热级/水量采样。按实际代码解释小量/间歇热液输入、分流争抢、预热/残热周期，区分整数热级量化与过度消耗/错误停机。唯一允许写`build/reports/extension/EXT-B-EXCHANGER-01C-LOWFLOW.md`及同名目录。若稳定输出须改已批准的固定额定18级行为，只提出守恒的推荐方案，不实施新平衡。

执行者禁止Git写操作、核心文档修改及转派；不运行全量回归，不同时启动Gradle。若需要动态复现先报告具体场景，由PM分配一个执行者跑最小定向用例。PM不编写功能、测试或诊断脚本；代码修改须有已证实根因和明确修复写集。

## 动态复现补充派发（同日）

用户进一步明确已经合计全部设备，冷热总量仍持续下降。静态检查发现Create FluidNetwork按IFluidHandler对象身份累计模拟接收，而换热器每面查询均返回新Port，多面共用一个近满热罐可能重复许诺容量；这是待真实复现的假设，不等于已经证实用户布局根因。

**REPRO：** 允许新增`src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionHeatExchangerLoopGameTests.java`及`src/main/resources/data/create_nuclear_industry_heat_loop/structure/loop_empty.nbt`（可复用既有空模板形状），只写这些测试文件，不改生产实现。报告`build/reports/extension/EXT-B-EXCHANGER-01C-REPRO.md`及同名目录。使用真实Create源罐、泵、管道和多侧接入同一换热器，填充热罐到接近满，关闭上方负载使其不转换；记录源罐＋设备热/冷总量，测试前后必须一致。独立单面连接为对照，优先复用现有真实Create管网夹具。不能仅手动重复fill/drain后宣称真实泵管已复现。

REPRO独占一次新的`create_nuclear_industry_heat_loop`命名空间运行，隔离目录`build/gametest-heat-loop-01c`，不clean、不rerun、不启动旧全量，保留原始失败和准确库存数据；无需为了制造失败改变断言。若用例未形成有效真实输送，应说明夹具问题，不能将总量不变的空转当通过。只有发现具体夹具错误才定向修复重跑，不反复盲改布局。生产修复仍待根因确认后续派。

### 用户拓扑核对后的复现优先级

只读保存快照显示，同一冷液回流泵及主管接入同一反应堆的三个冷端，三个端口共用内部容量；四台换热器各自单面输入，锅炉水路独立。首要复现因此调整为真实成型反应堆、一个源罐和泵、三个冷端分流，在接近满罐时逐tick验证冷热总量；单冷端为对照。REPRO可在上述同一个新测试类和空模板内实现或扩大夹具，不扩展其他写集。换热器多面用例作为同机制补充，不能替代用户场景。

静态证据已经定位Create按handler对象身份累计模拟容量、而反应堆不同冷口共用库存的冲突。真实失败门尚未完成；不把静态算例或单份存档当作流失速率实测。LOWFLOW独立查明当前固定18级/无热两档会在长期不足36mB/t时周期运行，此现象不等同于丢失流体。自适应热级仍仅为待确认建议。

## 交付与下一门

提供带路径/行号的流体事务链、已证实与未证实结论、必要最小复现证据。能在既有合同内修复的实际缺陷由PM续派；涉及可变热级、缓冲时长、功率/流量平衡等取舍，保存证据后只询问新增决策。人工门与其余热端功能保持暂停。

## 守恒修复（2026-10-04真实失败后由PM启动）

三端真实普通三通管网已复现：初始源4000mB、堆冷9000mB、热0；32tick后源928mB、堆冷12000mB、热0，少72mB。三条支路完整向北出流均有原始日志；单端对照末态源1000mB、堆冷12000mB，保持总13000mB。证据为REPRO的`gametest-console-delayed-branch-flow.log`。前两轮分别存在玻璃直管不横连、过早要求首笔三路均完成的夹具缺陷，保留日志并明确不作三端守恒证据。按下述写集正式派发FIX。

唯一目标：Create同一次分流模拟不能重复许诺本模组的共享空间或同物理口配额；原生执行扣液/接收顺序、逐冷端128mB/t、反应堆共享库存、普通SIMULATE纯读、换热器五面规则和生命周期均不变。

采用锁定环境已有MixinExtras 0.5.0，在`FluidNetwork.tick()`唯一`IFluidHandler.fill`调用点用`WrapOperation + @Share LocalRef`建立方法局部接收计划。仅本模组显式接口的SIMULATE参与；EXECUTE和外部handler原样调用一次。以handler、库存owner、物理预算三层对象身份去重累计容量，重算累计请求时排除该handler旧份额；局部引用不跨调用/tick保留。无需改Create依赖、构建脚本或全局管网算法；注入目标失配必须显错，不可静默绕过。

**FIX精确写集（MASS执行者）：**

- 新增`src/main/java/com/iksxh/create_nuclear_industry/compat/create/SharedFluidReceiver.java`与`SharedFluidFillPlan.java`。
- 新增`src/main/java/com/iksxh/create_nuclear_industry/mixin/FluidNetworkSharedFillMixin.java`。
- `reactor/ReactorCoolantFluidHandler.java`与`heat/NuclearHeatExchangerBlockEntity.java`仅实现只读共享接收视图；不改既有fill/drain记账或热工算法。
- 新增`src/main/resources/create_nuclear_industry.mixins.json`；`src/main/resources/META-INF/neoforge.mods.toml`仅追加本配置接线。
- 新增`src/test/java/com/iksxh/create_nuclear_industry/compat/create/SharedFluidFillPlanTest.java`，覆盖共享近满、累计重分配、同口多面、不同口独立配额、不同owner隔离和新作用域不残留。
- 报告`build/reports/extension/EXT-B-EXCHANGER-01C-FIX.md`及同名目录。无Git写入、核心文档写入、用户存档或客户端操作。

REPRO继续独占真实测试类和模板及Gradle运行。正式修复交付后由PM通知一次定向验证：新增计划JUnit、现有相关反应堆handler/预算JUnit、真实本批泵管复现、增量assemble。因触及共享模拟接入，补一个原生罐目标透传和换热器多面共享热罐的代表性用例；可归入同一独立测试域，不启动无关全量。原修复前失败日志永久保留，不削弱守恒或实际移动断言。修复后同用例转绿及人工闭环复测是各自独立门。
