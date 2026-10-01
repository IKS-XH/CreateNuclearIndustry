# 项目文档入口

**整理日期：2026-09-29。** P1 固定实验反应堆已完成最终交接。下一阶段优先矿物获取、原材料与零件、设备制备及燃料生产。当前进度维护在[实施路线图](./implementation-roadmap.md)，本页负责导航。

## 当前工作

**开发环境（2026-10-01）：** [JEI 调试依赖](./superpowers/plans/2026-10-01-dev-jei-01.md) 已安装到主工程和整合候选的三个开发客户端；重启客户端加载。该环境维护不改变以下玩法验收状态。

**本轮执行：** [资源、生产与 SVG 美术启动计划](./superpowers/plans/2026-09-29-resources-production-art-start-plan.md)。用户已授权自动派发与审核；三矿资源入口与现有 51 张贴图重绘已形成整合候选，代码/静态视觉审查与自动验证完成；现暂停于 [客户端测试包与验收清单](./reviews/2026-09-29/implementation/README.md)。候选未合入 main，不沿用旧 P1 手测结论。

**最新反馈：** 三矿自然生成、采集及其他新素材已获用户认可；两种冷却剂已按用户选择 [恢复旧外观](./superpowers/plans/2026-09-29-ext-art-02a-coolant-restore.md)，候选 `b4d78d5` 等待冷却剂复验与其余人工项，见上述验收清单。

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
