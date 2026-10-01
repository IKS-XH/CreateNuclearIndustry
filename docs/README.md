# 项目文档入口

**整理日期：2026-09-29。** P1 固定实验反应堆已完成最终交接。下一阶段优先矿物获取、原材料与零件、设备制备及燃料生产。当前进度维护在[实施路线图](./implementation-roadmap.md)，本页负责导航。

## 当前工作

**开发环境（2026-10-01）：** [JEI 加载整改](./superpowers/plans/2026-10-01-dev-jei-01a.md) 已同步三个开发客户端，用户确认界面出现；随后铅锡复测清单中的配方/用途查询也获确认。该证据只覆盖实际测试范围，不代表未来配方已验收。

**本轮执行：** 三矿、铅锡和粉末制钢及对应素材均已验收并合入main，普通钢材名称为钢粉、钢锭、钢板，见[材料03最终验收](./reviews/2026-10-01/material-03b/README.md)。材料04锡条、两种传感器及5项SVG素材的候选已完成自动验证和独立审查，停在[客户端人工门](./reviews/2026-10-01/material-04/CLIENT-CHECKLIST.md)：265项JUnit/build通过、167项required断言全过，GameTest保存退出停滞单列，功能尚未合入main。水洗副产物暂缓；用户反馈前不推进下一批。

**已确认反馈（2026-10-01）：** 三矿、铅锡和钢材的各批人工门均已解除；不等于设备、燃料、冷却剂和发电链已实现。[完整制钢客户端清单](./reviews/2026-10-01/material-03-client.md)保留实际人工范围。

| 入口 | 用途与状态 |
| :--- | :--- |
| [首发扩展准备计划](./superpowers/plans/2026-09-22-first-release-extension-preparation-plan.md) | 主线入口：已确认决策、材料与设备依赖、未决参数和任务骨架；骨架不是派发授权 |
| [材料04：锡条与传感器实施](./superpowers/plans/2026-10-01-ext-a-material-04.md) | 同级候选待人工验收；[证据与启动说明](./reviews/2026-10-01/material-04/README.md)，自动推进已停在本批人工门 |
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
