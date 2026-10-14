# EXT-B-TURBINE-01F：汽轮机零库存直通流体可行性调查

调查日期：2026-10-05
工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`
平台：Minecraft 1.21.1、NeoForge 21.1.219、Create 6.0.10-280（`gradle.properties` 6-11 行）
范围：只读源码调查；未修改功能代码、测试、docs 或 Git 状态；未运行测试；不涉及旧存档兼容。

## 结论

严格“无持久流体库存”的汽轮机，不能仅靠目前的 Create `IFluidHandler` 端口边界，可靠地把上游管道泵已执行的输入事务原子地转成下游管网可提取的 steam。输入口和排汽口分别是 Create 管网访问的能力端点，而每个 `FluidNetwork` 独立模拟、执行；Create 不提供跨两张管网的共同提交/回滚接口。零库存设计若在 `fill(EXECUTE)` 里即时向下游转发，连接的下游是 Create 管道时通常没有可调用的容器 `IFluidHandler`；Create 管网会在自己的 tick 里通过排汽口 `drain` 取货。若把虚拟 `drain(SIMULATE)` 伪装成有汽以启动下游流动，则也必须确保后续 `drain(EXECUTE)` 只兑现真实已收输入。二者的 tick 次序、堵塞状态和提交结果不能由一个普通端口 handler 原子协调。

**最低复杂度可行方向是 B：一个有严格上限的内部事务暂存量**，它不作为玩家可访问或可见的储罐，不落入独立的游戏库存玩法；它只承接已经执行的入口 fill，供排汽侧下一次真实 `drain` 事务提取。所有 simulation 必须纯查询；只在 execute 成交处修改暂存量/流量账本，并且一笔输出只记一次实际吞吐。该方案可适配 Create 的端点 pull 模型，且能让下游堵塞时不扣掉已经暂存的蒸汽。缓冲额度应严格封顶为每 tick 额定处理量；下游堵塞时已暂存残留保留，不得超时、过期或卸载时直接清除。若要求区块卸载/重载仍守恒，这一小份事务残留也必须随当前状态保存恢复。

但 B 与“堵塞时完全拒绝第一滴进汽”有玩法差异：普通入口能力无法预知另一张 Create 输出管网在当前 tick 是否能让终点接收，因此最多一 tick 额定处理量可能已由上游成功灌入；残留必须保留，后续输入在额度占满后背压拒收。Create 自己也先 execute source.drain、再 execute target.fill，没有通用回滚协议；在普通单服务器线程、端点模拟与执行稳定的前提下可按其事务约定工作，不能声称覆盖会在同一轮改变接收量的任意第三方 handler。

如果“堵塞时入口必须在同一笔交易中返回 0”是不可让步条件，则 A/B 都不能只靠原生两套 Create 管网的公开 `IFluidHandler` 边界同时做到：需要新增能同时协调上下游的专用跨网事务桥，或限制输入/输出必须直接面对可模拟容量的真实流体容器并避开管网串接。前者复杂且容易与 Create 的网络重入、重复模拟及泵 tick 顺序冲突；后者改变原生泵/管道使用方式。

## 当前汽轮机行为

- 能力注册位于 `src/main/java/com/iksxh/create_nuclear_industry/content/TurbineContent.java:105-117`。只在端口外侧且机组有效时暴露入口/排口能力。`TurbinePortBlockEntity` 自身明确不持流体；端口 handler 由控制器创建。
- `TurbineControllerBlockEntity.Port` 位于 `src/main/java/com/iksxh/create_nuclear_industry/turbine/TurbineControllerBlockEntity.java:337-376`。入口只接受超临界蒸汽；`fill` 委托 `ledger.fillInput(..., action.simulate(), now)`。排口 `drain` 只读 ordinary steam，并委托 `ledger.drainExhaust`。能力带 `epoch`，拆件/换型后旧句柄失效。
- `TurbineState` 位于 `src/main/java/com/iksxh/create_nuclear_industry/turbine/TurbineState.java`。`tick`（118-133 行）每控制器世界 tick 把入口库存按额定流量及排口容量限制，以 1:1 数量扣入口、加 ordinary-steam 排口、记入平滑窗口。`fillInput`/`drainExhaust`（146-179 行）分别有 simulation 与 execute 分支，以及按物理端口、同一世界 tick 限流的账本。当前库存并非 Create 储罐 BE，而是控制器拥有的两个整数库存，保存逻辑在 203 行以后。
- 控制器 `serverTick`（86 行起）先 `ledger.tick`，后推送普通相邻容器并刷新管路压力（164-168 行）。`pushAdjacentSteam`（380-394 行）故意跳过相邻 Create 管道；非管道 handler 采用目标 `fill(SIMULATE)` 后 `fill(EXECUTE)`，再扣排口库存。管道则由其自己的网络从排口 handler 拉取。这样现有逻辑区分了“容器主动 push”与“Create 管网主动 pull”。
- `TurbineSteamPressure.refresh`（`src/main/java/com/iksxh/create_nuclear_industry/turbine/TurbineSteamPressure.java:31-45`）按连接拓扑对 `PipeConnection` 写入压力贡献，是管路方向/流动可视化和传播的一部分；当前仅当控制器库存已有排汽时供压。压力或动画本身不能证明发生过成功 steam 交易，也不能作为发电依据。

## Create 6.0.10-280 调用语义

源码来自用户指定的本地文件：
`C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280/23e1219501c0debfa0bb56c30ef8e0193341aae5/create-1.21.1-6.0.10-280-sources.jar`

- `com/simibubi/create/content/fluids/PipeConnection.java:163-186`：端点识别通过 `FluidPropagator.hasFluidCapability` 查询邻接方块能力，并包装成 `FlowSource.FluidHandler`；否则识别为其他管段或 blocked。`manageFlows`（92-152 行）在压力和流体存在时建立/维护流动；有 endpoint 的 inbound flow 会维护 `FluidNetwork` 并调用其 `tick`。
- `com/simibubi/create/content/fluids/FlowSource.java:33-55`：`provideFluid` 首先调用 handler 的 `drain(1, SIMULATE)` 探测流体，再回退到 `getFluidInTank` 的内容探测。空库存、且 simulation 不报告可提取流体的排汽端，不会作为可供流体端启动常规流动。反之虚报模拟可用量可能只让网络识别出某种流体/出现流动状态，不能证明 execute 有已提交蒸汽。
- `com/simibubi/create/content/fluids/FluidNetwork.java:184-260`：每次 tick 先 SIMULATE、后 EXECUTE；每轮从 source 调 `drain`，再给目标调 `fill`。target 模拟接收量用于规划；execute 段 source 的 drain 先于 target 的 fill。源码没有跨 source 与多个 target 的回滚/二阶段提交接口。下游标准端点可满时，模拟 fill 返回不足，剩余量不会作为实际转移完成；但事务正确依赖模拟结果在执行期间稳定。
- `com/simibubi/create/content/fluids/pump/PumpBlockEntity.java:120-205,272-293`：机械泵依据管线拓扑收集 handler endpoint，按 pull/push 方向传播压力/流向；端点最终仍是 `Capabilities.FluidHandler.BLOCK`。Create 机械泵不是可被汽轮机 `fill` 的管段容器。
- `com/simibubi/create/content/fluids/FluidPropagator.java:198-208`：泵程来自 Create server config；能力检查只判断 endpoint handler 是否存在。

## A/B 比较

| 方案 | 对原生管泵的配合 | 守恒/堵塞 | 复杂度与风险 |
|---|---|---|---|
| A：严格即时转发，零库存 | 两侧网络是独立 pull/execute 循环，入口 `fill` 时下游 Create 管道没有普通 fluid handler 可同步调用；排口 `drain` 又可能先于新的入口事务。要求多网络同一事务协调。 | 普通端口无法原子预留下游网络目标容量。仅按管压/动画或虚拟模拟 drain 计发电会产生“看起来有流动但没有成功流体成交”的假阳性；若无虚拟可提取量，Create 又无法启动排口流量。 | 表面库存最小，实际需要侵入/协调 Create 网络生命周期；有重入、跨网络 tick 顺序、simulate 被调用多次和 source drain 已执行但 target 执行偏差的风险。严格要求下不可用作简单实现。 |
| B：至多一 tick 额定量的事务暂存 | 与 `IFluidHandler` 的源/目标端点契约吻合：输入 execute 成交后记录数量；输出 `getFluidInTank`/`drain(SIMULATE)` 展示暂存的 ordinary steam；输出网络 execute drain 时消费暂存量。 | 堵塞时排口模拟不会让 Create 从 handler 提取，暂存量保留、不发电；暂存达到一 tick 额定处理量后可拒绝额外入口 fill；堵塞时残留保留，不超时或过期删除。不能保证第一笔输入在输出堵塞时即时拒收，除非能获得目标网络实际可接收容量。守恒以 execute 实际成交量为准；flow/压力不入账。 | 最低复杂度；缓冲容量限定为一 tick 额定处理量，同 tick 重复 simulate 不得占额或重复记账。堵塞残留保留，不得超时、过期或卸载时直接清除；需要跨区块卸载恢复守恒时保存这份有限残留。推荐前提是允许最多一 tick 未输出蒸汽作为内部事务状态，而不向玩家表现为可积累库存。 |

## 建议与待用户确认的玩法取舍

建议采用 B，暂存容量严格等于最多一 tick 配置的额定处理量，不向玩家显示为汽轮机储罐；下游堵塞时已暂存残留必须保留，不设过期删除，并在需要跨区块卸载恢复守恒时保存这份有限残留。记录发电流量时，以排汽侧成功 execute drain 的唯一数量作为已转换/处理量：simulation 不改计数，同一笔 execute 不重复计数，压力动画/管网流动状态不单独发电。需要按世界 tick 归集实际输出量，继续套用已确认的 40 tick 窗口与效率曲线。

用户需要明确接受以下玩法定义之一：

1. **推荐的 B 语义**：下游刚堵塞时允许最多一 tick 额定处理量已进入汽轮机；汽轮机不据此发电，暂存占满后拒绝新蒸汽；已接受的堵塞残留保留，不超时或过期删除。下游恢复后继续排出并按真实输出计发电。这里“拒绝进汽”指无可用暂存额度时拒绝，不保证堵塞发生瞬间回绝第一笔。
2. **绝对零库存/即时拒收**：要求入口 fill 同一事务确认下游已经接收后才成功。需接受仅支持能同步模拟目标容量的直连容器，或批准专用 Create 网络桥的高复杂度方案；管泵相接时不能以当前原生端点能力合同承诺全条件可靠。

在选定上述差异之前，不应把“移除库存”写成“只有流量检测”的实现合同：它还必须定义入口成功、出口成功、处理量记账和背压之间的事务边界。

## 技能与验证

- 实际读取：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`。本次只读评估，没有使用技能示例作为版本 API 依据；所有 Create 结论均基于上述锁定版本 sources.jar。
- 依治理协议第5.1节，本次属于流体交易/共享管网设计调查，应以真实 Create 管网交易作为后续实现关键验收；本次按任务要求未运行测试。依第5.2节，不研究旧存档兼容。
