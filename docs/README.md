# 项目文档入口

**更新：2026-10-10。** 燃料烧结炉两幕播放通过并净整合main，屏蔽装配台三幕教学按用户明确指令开始实施。反应堆运行动画根据用户视觉反馈做03R1定向整改，材质连窗另待验收，其他主线暂缓。

## 当前执行与接续

| 工作 | 状态与入口 |
| :--- | :--- |
| 燃料烧结炉思索06 | [两幕播放验收](./reviews/2026-10-10/fuel-sintering-ponder-06/ACCEPTANCE.md)通过；复用3/3定向检查、增量assemble与一次独立审查，8路径一致，main净教学提交`57763f8` |
| 屏蔽装配台思索07 | [三幕实施计划](./superpowers/plans/2026-10-10-device-ponder-07-assembly.md)：搭建与接口、新燃料装配、乏燃料封装；用户明确开始，实施后独立审查并停在本台播放门，不新增玩法 |
| 汽轮机思索04 | 原三幕及`a94f2ea`取景/文案[播放验收](./reviews/2026-10-09/turbine-ponder-framing/ACCEPTANCE.md)通过，净教学整合main |
| 美术并行工作 | [美术负责人入口](./art/README.md)：独立工作树，专属规划/派发，跨逻辑协调与最终Git集成由本PM主持 |
| 反应堆内部运行动画03 | [原候选登记及反馈](./reviews/2026-10-10/reactor-animation-03/HANDOFF.md)：原63/63冻结、16/16定向检查和独立审查证据保留；用户指出过细、棒体黑和液体不可见，美术按03R1定向整改，视觉未通过、未合main，02R1门独立保留 |
| 反应堆运行动画共享前置 | 用户直接确认动画及按库存连续混色；[L2实物API/写集交付](./reviews/2026-10-10/reactor-runtime-display-02/HANDOFF.md)自动与独立审查通过，main `cb123c9`、美术净同步`4cb0d7f`；美术按边界接运行消费者，动画与02R1视觉门独立保留 |
| L2真暂停租约窄修 | [R2任务](./superpowers/plans/2026-10-10-reactor-runtime-display-02-pause.md)自动与独立窄审通过，main `93d0a20`、美术净同步`2bece5b`；10/10定向生命周期、一次增量assemble和12/12源核对；最新冻结/API见HANDOFF R2，暂停/恢复观察并入美术视觉门 |
| 成型反应堆连接纹理 | [L1实际API交付](./reviews/2026-10-09/reactor-surface-display-01/HANDOFF.md)已净同步；[02B候选登记](./reviews/2026-10-09/reactor-connected-texture-02b/HANDOFF.md)自动证据保留。用户反馈成型材质与窗组内部框需整改，[02R1范围补充](./art/ART-REACTOR-02R1-COORDINATION.md)已实际同步美术树，视觉门未通过，源码未合main；教学验收不覆盖该门 |
| 换热器工作盆供热 | `ab91356`实现及复核通过，用户手测通过，见[验收](./reviews/2026-10-09/exchanger-basin-02r1/ACCEPTANCE.md)；持续超级加热，默认4mB/t，缺液或冷满立即停热 |
| 换热器烧结炉供热 | `094a925`实现/复核及3/3真实测试通过，工时/物流不变；[用户手测验收](./reviews/2026-10-09/exchanger-sintering-03/ACCEPTANCE.md)通过，净功能整合main |
| 换热器分情景思索 | 五幕及R4剖面复看[用户手测通过](./reviews/2026-10-09/heat-exchanger-ponder-05/ACCEPTANCE.md)，复用定向合同、增量打包及独立复核；main净教学提交`b036776` |

主目录现提供已验收供热及离心机、反应堆、锅炉、汽轮机、换热器、烧结炉教学。逻辑候选目录`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`保留本批实现及历史证据；美术候选使用`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，各目录分别使用自己的运行配置和世界。

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
