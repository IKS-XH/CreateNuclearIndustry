# 项目文档入口

**更新：2026-10-09。** 汽轮机思索按截图整改三档取景与排汽措辞；换热器持续工作盆供热仍待手测，两项分别记录。用户任命独立美术负责人开展贴图、建模和动画并行工作；离心机、反应堆和锅炉教学已经通过，其他主线暂缓。

## 当前执行与接续

| 工作 | 状态与入口 |
| :--- | :--- |
| 汽轮机思索04 | [取景与文案整改](./superpowers/plans/2026-10-09-device-ponder-04-framing-fix.md)候选`a94f2ea`实现与静态复核通过；原三情景保留，同级候选定向复看两页 |
| 美术并行工作 | [美术负责人入口](./art/README.md)：独立工作树，专属规划/派发，跨逻辑协调与最终Git集成由本PM主持 |
| 换热器工作盆供热 | [EXCHANGER-02R1实施卡](./superpowers/plans/2026-10-08-ext-b-exchanger-02r1-continuous.md)候选`ab91356`实现及整改复核通过：持续超级加热，默认4mB/t，缺液或冷满立即停热；按[集中清单](./reviews/2026-10-08/exchanger-basin-02/MANUAL.md)与汽轮机手测 |
| 换热器分情景思索 | 工作盆功能验收后，分别介绍独立供热、高压锅炉内置换热、工作盆和蒸汽冷凝回水 |

候选目录：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。在该目录执行 `./gradlew.bat runClient`测试新供热并播放汽轮机教学；主目录提供已验收版本，两目录分别使用自己的运行配置和世界。

## 权威文档

| 入口 | 职责 |
| :--- | :--- |
| [实施路线图](./implementation-roadmap.md) | 当前阶段、已通过批次、未完成范围及人工门 |
| [项目策划](./project.md) | 现行玩法与明确的后续边界 |
| [反应堆局部控制](./reactor-local-control-revision-design.md) | 棒列邻接、反馈、热工、损伤与停堆合同 |
| [内容清单](./content-catalog.md) | 注册身份、资源、教学覆盖；阶段目标不等于已实现 |
| [配方表](./recipes.md) | 材料工序及现行配方规则 |
| [服务端配置指南](./server-config.md) | 锅炉、换热器、汽轮机配置键、单位和默认值 |
| [治理协议](./project-governance.md)、[根AGENTS](../AGENTS.md) | PM/执行者权限、技能、派发与精简验证 |
| [首发扩展入口](./superpowers/plans/2026-09-22-first-release-extension-preparation-plan.md) | 有效后续依赖与派发边界 |
| [彩蛋与进度](./easter-eggs-and-advancements.md) | 彩蛋、进度及其范围限制 |

## 仍有效的后置工作

- 反应堆可变尺寸、辅助热、二级耐压、自动控制、具体事故与辐射、动画及发布完善，按路线图安排；未实现行为不提前教学。
- [事故教学与发布总验收](./superpowers/plans/2026-09-12-post-accident-ponder-plan.md)继续保留；已有设备基础教学无需等待事故实现。
- [损伤倍率模拟器02B](./superpowers/plans/2026-09-08-damage-heat-burn-balance-plan.md)和[三维辅助工具](./superpowers/plans/2026-08-31-nuclear-plant-3d-html-visualization-plan.md)未完成整体验收，不阻塞近期任务，不在人工门等待期间自行接续。
- [生产成本草案](./superpowers/plans/2026-09-24-first-production-cost-draft.md)保留核算口径；旧比较值不是未批准的新配方授权。铅锡洗矿副产物议题仍暂缓。

换热器输入超临界蒸汽及降级供热路线已取消。现有核热换热和普通蒸汽冷凝保留，后续工作盆复用核热。首发前不研究旧存档兼容；当前版本保存加载仍按范围验证。

## 历史与验收

完成/被替代的计划见[归档索引](./archive/README.md)，验收报告保留在 `docs/reviews/`。历史待测和失败描述只代表原时点，不重新开启已关闭人工门。主要完成批次由[路线图](./implementation-roadmap.md#1-已验收范围)统一索引，不在此重复维护时间线。换机与P1证据见[2026-09-29交接](./handoffs/2026-09-29/README.md)。
