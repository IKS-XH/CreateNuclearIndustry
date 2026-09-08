# 文档归档索引

本目录保存已经完成、被替代或不再作为当前执行依据的设计与任务计划。归档文件保留决策历史，不作为开发者当前实现合同。

## 当前规则

- `docs/` 下的五份设计文档（项目、反应堆局部控制、内容清单、配方、彩蛋与进度）是活动设计依据；`docs/implementation-roadmap.md` 是第六份核心文档，负责活动阶段与验收依据。
- 活动任务放在 `docs/superpowers/plans/`；任务完成或被新计划替代后，才由项目经理移动到本目录。
- 任何归档动作必须由项目经理执行，并在提交说明中写明替代关系。

## 当前活动计划

当前活动计划为：

- `../superpowers/plans/2026-08-18-agent-developer-execution-plan.md`
- `../superpowers/plans/2026-09-08-damage-heat-burn-balance-plan.md`
- `../superpowers/plans/2026-08-18-p1-png-generation-brief-for-gptimage2.md`
- `../superpowers/plans/2026-08-31-nuclear-plant-3d-html-visualization-plan.md`

## 已归档计划

- `2026-08-18-p0-api-probe-and-numeric-prototype-plan.md`：P0 API 探针、数值原型、GameTest 和人工验收已通过，作为 P1 开工门禁完成。
- `2026-08-17-project-manager-execution-plan.md`：已被 2026-08-18 的 Agent/开发者 P1 交接计划取代，保留作为历史任务记录。
- `2026-08-18-reactor-html-simulator-plan.md`：HTML 数值模拟器的 SIMWEB-01 至 SIMWEB-07、15 项离线回归和人工验收已完成；保留为数值实验工具的交接与决策记录。
- `2026-08-19-integrity-zero-simulator-alignment-plan.md`：P1-SIMWEB-08 已将零完整度继续运行、无全堆流量上限、`128 mB/t` 单端口、SCRAM 可用性和离线单文件同步到模拟器；项目经理复验 28 项 Node 测试通过，用户已完成人工验收。
- `2026-08-25-chinese-code-comments-execution-plan.md`：P1-COMMENT-00 至 P1-COMMENT-06 已完成；存量 Java 中文注释通过全量审计，永久规则已收敛到根目录 `AGENTS.md`。
- `P1-REFUEL-01.md`：单列燃料组件放入、取出、耐久度往返、燃尽产出和多列独立性已通过隔离自动验收。
- `P1-REFUEL-02.md`：玩家顶部换料端口交互已通过隔离自动验收与客户端人工验收。
- `P1-GOGGLE-INSTRUMENT-01.md`：仪表端口静态结构摘要已通过 158 项 JUnit 与 38 个 required GameTest；摘要只读取结构缓存和服务器配置，不触发重扫或复制运行快照。
- `P1-GOGGLE-INSTRUMENT-02.md`：仪表端口 Create 护目镜静态摘要已通过 162 项 JUnit、39 个 required GameTest 和客户端人工验收；显示使用客户端只读副本与本地化无效状态。
- `P1-GOGGLE-INSTRUMENT-03.md`：仪表端口动态运行遥测已通过 169 项 JUnit 与 45 个 required GameTest；遥测由最后一次正式服务端 tick 只读派生，重载与结构失效不会泄露陈旧数据，真实更新包路径满足 10 tick 可见性上限。
- `P1-GOGGLE-INSTRUMENT-04.md`：护目镜动态信息按用户确认的现场分层方案完成；仪表端口显示全堆总量，换料端口和控制棒驱动器显示绑定列详情，176 项 JUnit、47 个 required GameTest 与客户端人工验收通过。
- `P1-CONTROL-04.md`：控制棒滑块已移除异步预览回包驱动的游标反馈环，改为客户端本地拖动预览与服务端最终提交；181 项 JUnit、47 个 required GameTest 与客户端人工验收通过。
- `P1-COOL-04.md`：真实 Create 储罐—动力泵—管道已能向成型反应堆冷端输入复合冷却剂并从热端输出；181 项 JUnit、51 个 required GameTest 与客户端人工验收通过。后续热量量化和超频控制问题不属于管网接入范围。
- `P1-THERMAL-01.md`：整数 `mB` 转化产生的安全热量余数已与真实冷却短缺分离，并由权威快照/NBT v3 守恒保存；192 项 JUnit、52 个 required GameTest 与客户端人工验收通过。
- `P1-CONTROL-05.md`：正式 Java 裂变计算已按“控制抑制 × 反馈倍率”门控三行 `F-C-F` 反馈簇；195 项 JUnit、53 个 required GameTest、30 项模拟器 Node 测试与客户端人工验收通过。
- `P1-COOL-05.md`：原位冷/热端口和既有 Create 管网在结构失效、仪表替换与跨区块重载后可自动恢复；199 项 JUnit、60 个 required GameTest 与客户端人工验收通过。
- `P1-REFUEL-02A.md`：换料端口成为单列完整燃料 `ItemStack` 的唯一持久化所有者，仪表快照升级至 v4 并完成旧存档迁移；202 项 JUnit、65 个 required GameTest 与客户端人工验收通过。
- `P1-REFUEL-02B.md`：未成型换料端口在服务端确认安全时允许空手取回本地燃料，危险状态通过持久化锁拒绝绕过；202 项 JUnit、72 个 required GameTest 与客户端人工验收通过。
- `P1-REFUEL-03.md`：换料端口已接入 Create 专用机械臂交互点，模拟只读、正式提交原子且失败回滚；202 项 JUnit、75 个 required GameTest 与客户端人工验收通过。
- `P1-REPAIR-01.md`：玩家可用 `steel_plate` 维修停止放热的燃料列，每块恢复 `0.25 / internalHeight` 且不清除余热、燃料或融毁进度；208 项 JUnit、82 个 required GameTest 与客户端人工验收通过。
- `P1-REPAIR-02.md`：玩家可用 `steel_plate` 维修绑定控制棒列，跨过服务端失效阈值后解除卡死并保留插入深度；集成基线 213 项 JUnit、86 个 required GameTest 与客户端人工验收通过。
- `P1-MELTDOWN-01.md`：完整度归零且燃料未耗尽的失效源列计入融毁覆盖，支持覆盖并集去重、耗尽退出及持久化倒计时；217 项 JUnit、89 个 required GameTest 和完整构建通过。
- `P1-BALANCE-02A.md`：游戏端已接入可配置的损伤产热/燃耗差异倍率，默认满损伤 `2.0/3.0`，并通过联合配置校验、正式 tick 与燃料/NBT 连续性验证；225 项 JUnit、90 个 required GameTest 和完整构建通过。

## 已归档任务验收报告

- `P1-LOOP-02.md`：反应堆冷端、热端和换料端口已绑定到唯一结构所有者；capability 缓存随结构成型、失效和恢复正确刷新，隔离复验为 145 项 JUnit 与 29 个 required GameTest 全部通过。
