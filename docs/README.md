# 项目文档入口

**整理日期：2026-09-29。** P1 固定实验反应堆已完成最终交接。下一阶段优先矿物获取、原材料与零件、设备制备及燃料生产。当前进度维护在[实施路线图](./implementation-roadmap.md)，本页负责导航。

## 当前工作

**开发环境（2026-10-01）：** [JEI 加载整改](./superpowers/plans/2026-10-01-dev-jei-01a.md) 已同步主工程和整合候选的三个开发客户端；真实隔离启动确认模组注册及资源加载。用户随后确认 JEI 界面出现，安装请求完成；配方查询交互未单独确认。该环境维护不改变以下玩法验收状态。

**本轮执行：** 三矿资源入口与现有素材重绘/冷却剂回退已于2026-10-01完成验收并合入 main，见 [收尾记录](./reviews/2026-10-01/ore-art-acceptance.md)。[铅锡粗矿熔炼与压板](./superpowers/plans/2026-10-01-ext-a-material-02.md) 及两张板材素材现已形成候选，自动验证与独立审查完成，暂停等待 [客户端验收](./reviews/2026-10-01/material-02-client.md)；本批未合入main。

**最新反馈（2026-10-01）：** 三矿生成、采集、粉碎轮、粗矿9:1合成/拆解、保存重进、其他新素材及两冷却剂回退均获用户确认；JEI界面恢复也已确认。本批人工门全部解除，不等于完整材料/设备链已实现。

| 入口 | 用途与状态 |
| :--- | :--- |
| [首发扩展准备计划](./superpowers/plans/2026-09-22-first-release-extension-preparation-plan.md) | 主线入口：已确认决策、材料与设备依赖、未决参数和任务骨架；骨架不是派发授权 |
| [青金石粉 01A](./superpowers/plans/2026-09-24-ext-a-material-01a.md) | 未合并候选：GameTest、纹理整改/评审和客户端验收仍待完成 |
| [首套生产线成本草案](./superpowers/plans/2026-09-24-first-production-cost-draft.md) | 已批准试验配比与未批准候选分开记录；不是完整配方冻结表 |
| [损伤倍率专项](./superpowers/plans/2026-09-08-damage-heat-burn-balance-plan.md) | 游戏端 02A 已完成；模拟器 02B 仍待离线浏览器验收，未合并 |

## 设计与执行规则

| 文档 | 权威职责 |
| :--- | :--- |
| [AGENTS.md](../AGENTS.md)、[治理协议](./project-governance.md) | 开工规则、角色权限、技能、任务卡、验收和 Git 管理 |
| [项目策划](./project.md) | 产品目标、首发范围与玩法规则 |
| [反应堆局部控制](./reactor-local-control-revision-design.md) | 状态归属、热工/控制公式与运行不变量 |
| [内容清单](./content-catalog.md) | 注册身份、内容阶段、资源与 Ponder 矩阵 |
| [配方关系](./recipes.md) | 材料来源、设备工序、标签与守恒 |
| [彩蛋与进度](./easter-eggs-and-advancements.md) | 非主线趣味内容 |
| [实施路线图](./implementation-roadmap.md) | 当前状态、阶段顺序、出口与阻塞项 |

已确认规则按上述职责维护；计划负责拆解任务，不另立玩法合同。发现矛盾时由项目经理核对，涉及未决玩法取舍再交用户决定。

## 后置与辅助工作

- [事故后的 Ponder 教学](./superpowers/plans/2026-09-12-post-accident-ponder-plan.md)：具体事故验收后继续，仍属首发发布门。
- [P1 PNG 素材说明](./superpowers/plans/2026-08-18-p1-png-generation-brief-for-gptimage2.md)：旧生图说明保留作历史，当前绘制方式由 SVG 启动计划取代；复杂结构视觉仍后置。
- [三维核电站工具计划](./superpowers/plans/2026-08-31-nuclear-plant-3d-html-visualization-plan.md)：未确认整体验收完成的辅助工具；恢复前须复核已有交付和剩余任务，不作为游戏功能完成证据。
- [反应堆模拟器说明](../tools/reactor-simulator/README.md)：工具使用与限制；当前 main 不能当作 02B 新倍率已同步。

## 历史与交接

- [归档索引](./archive/README.md)：已完成、被替代的任务合同及验收记录。
- [P1 最终交接](./archive/P1-VERIFY-03.md)、[2026-09-29 原始证据包](./handoffs/2026-09-29/README.md)：核心切片的验收依据与限制。
- [2026-09-28 换机检查点](./handoffs/2026-09-28/README.md)：历史工作树、候选和恢复线索；当前状态以路线图为准。

归档保留历史证据，不把仍待验收的候选记为完成。执行者只领取项目经理明确派发、前置满足且有精确写集的任务。
