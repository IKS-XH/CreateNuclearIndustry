# 项目文档入口

**整理日期：2026-09-29。** P1 固定实验反应堆已完成最终交接。下一阶段优先矿物获取、原材料与零件、设备制备及燃料生产。当前进度维护在[实施路线图](./implementation-roadmap.md)，本页负责导航。

## 当前工作

**控制棒组件01C：** 已按用户要求改为工作台竖排合成，原用料各1、产量1不变；定向验证与独立复审通过，客户端待验，见[01C卡](./superpowers/plans/2026-10-03-ext-a-reactor-01c.md)。

**仪表端口21格修订：** 按用户要求提升到离心机同级，[REACTOR-01B](./superpowers/plans/2026-10-03-ext-a-reactor-01b.md)已实现并通过定向验证和复审，实际动力合成待[合并手测](./reviews/2026-10-03/reactor-01/README.md)。

**当前主线：** [铀原料加工与离心机](./reviews/2026-10-02/fuel-01/ACCEPTANCE.md)、[生芯块与专用烧结炉02A/02B](./reviews/2026-10-03/fuel-02b/ACCEPTANCE.md)、[02C包壳/焊料/钢网/格架材料](./reviews/2026-10-03/fuel-02c/ACCEPTANCE.md)均已全部手测通过并合入main，焊料为3锡锭＋1铅锭→4件。[02D/02E屏蔽装配台](./reviews/2026-10-03/fuel-02e/ACCEPTANCE.md)已完成功能手测并合入main，四材料直接装配燃料组件，取消新燃料棒中间步骤。

**推进方式（2026-10-02）：** 按用户要求启用[按改动范围验证](./project-governance.md#51-按改动范围验证2026-10-02起生效)：默认增量构建和定向测试，未变实现复用已审证据，普通批次一轮合并审查，全量仅在明确风险触发时运行。

**开发环境（2026-10-01）：** [JEI 加载整改](./superpowers/plans/2026-10-01-dev-jei-01a.md) 已同步三个开发客户端，用户确认界面出现；随后铅锡复测清单中的配方/用途查询也获确认。该证据只覆盖实际测试范围，不代表未来配方已验收。

**已完成生产段：** 三矿、铅锡、钢材、锡条与传感器、石英粉/耐火砖/重型轴承，以及铀洗矿/制浆/两格离心机、生芯块/烧结炉均已完成各批人工验收并合入main。普通钢材名称简称“钢”，铅锡水洗副产物暂缓；主目录runClient已包含烧结炉。历史验证按各批报告保留，不重复全量。

**剩余主线：** 青金石粉与冷却剂制备 → 反应堆/管网生存制造与后续机组闭环。四材料直接装配组件已完成；后续只确认新增取舍，无GUI原则与已批准工序直接沿用。

**01A配方修订：** 用户新增三项修改已记录于[01A卡](./superpowers/plans/2026-10-03-ext-a-reactor-01a.md)，同一候选已完成修订、6项定向测试及独立复审；现行配方与手测清单以修订为准。

**本次连续推进结果：** 按用户委托的推荐参数，01B及[固定实验堆生存制造01](./superpowers/plans/2026-10-03-ext-a-reactor-01.md)均已实现、定向验证和复审通过，等待睡醒后的[合并手测](./reviews/2026-10-03/reactor-01/README.md)；新内容均保留在同级候选工作树。

**当前任务：** [02E功能验收与模型R2修复](./reviews/2026-10-03/fuel-02e/ACCEPTANCE.md)已合入main，不重复功能清单。[材料01B候选](./reviews/2026-10-03/material-01b/README.md)已完成实现和定向验证：青金石1:1、两道加工参数100、冷却剂无需加热。该批三项人工验收按最新授权与反应堆制造一起进行，新配方仅在同级候选工作树。R2新画面不追记为已人工复看，动画另批。

| 入口 | 用途与状态 |
| :--- | :--- |
| [首发扩展准备计划](./superpowers/plans/2026-09-22-first-release-extension-preparation-plan.md) | 主线入口：已确认决策、材料与设备依赖、未决参数和任务骨架；骨架不是派发授权 |
| [首台富集离心机实施](./superpowers/plans/2026-10-02-ext-a-fuel-01.md) | 全部人工门通过、已合入main，见[最终验收](./reviews/2026-10-02/fuel-01/ACCEPTANCE.md) |
| [燃料02A/02B：生芯块与专用烧结炉](./superpowers/plans/2026-10-02-fuel-sintering-furnace-proposal.md) | 全部手测通过并合入main；[最终验收](./reviews/2026-10-03/fuel-02b/ACCEPTANCE.md)，运行证据复用 |
| [材料04：锡条与传感器实施](./superpowers/plans/2026-10-01-ext-a-material-04.md) | 人工通过并合入main；[最终验收与启动说明](./reviews/2026-10-01/material-04/ACCEPTANCE.md) |
| [材料05：石英粉、耐火砖与重型轴承实施](./superpowers/plans/2026-10-02-ext-a-material-05.md) | 完整人工清单通过，已合入main；[最终验收](./reviews/2026-10-02/material-05/ACCEPTANCE.md)，后续离心机见当前实施卡 |
| [青金石粉与冷却剂01B](./superpowers/plans/2026-10-03-ext-a-material-01b.md) | 候选实现与审查通过，待[三项手测](./reviews/2026-10-03/material-01b/README.md)；未合入main，不整体合并旧01A |
| [固定实验堆生存制造01](./superpowers/plans/2026-10-03-ext-a-reactor-01.md) | 7种材料、16条配方已实现；7项定向GameTest与5项合同测试通过，待[两批合并手测](./reviews/2026-10-03/reactor-01/README.md) |
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
