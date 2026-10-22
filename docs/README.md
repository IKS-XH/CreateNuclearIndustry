# 项目文档入口

更新：2026-10-21。文档按职责组织；旧方案、阶段候选与执行时间线进入[归档](archive/README.md)，旧待实现/未合main不是当前状态。

| 要查什么 | 入口 |
| --- | --- |
| 已合入、正式JAR、验收和未闭门 | [当前状态](STATUS.md) |
| 下一步、后置范围与依赖 | [路线图](implementation-roadmap.md) |
| 产品与现行设备规则 | [项目设计](project.md) |
| 堆芯、局部控制、热工和损伤 | [反应堆合同](reactor-local-control-revision-design.md) |
| 管理员GUI、恒功率、闭环和续接 | [管理员](reactor-automation.md) |
| 真实ID与资源路径 | [内容清单](content-catalog.md) |
| 实际加工关系与配方规则 | [配方](recipes.md) |
| 文件、键、默认值、单位 | [配置](server-config.md) |
| 角色、技能、验证和Git | [AGENTS](../AGENTS.md)、[治理](project-governance.md) |
| 美术来源和现候选 | [美术入口](art/README.md) |
| 音效负责人、首样与试听边界 | [音效入口](audio/README.md) |
| MIT、Logo例外及第三方许可 | [LICENSE](../LICENSE)、[NOTICE](../NOTICE.md) |
| 原方案/失败/审查/验收 | [归档索引](archive/README.md) |

当前活动卡：

- [创造模式拆残骸窄修](superpowers/plans/2026-10-22-corium-creative-removal-01.md)：只修流体熔融物手测发现的创造掉废料问题，复看与制品见STATUS，不重开整组手测。
- [事故与辐射设计](superpowers/specs/2026-10-16-accident-radiation-design.md)：整稿和推荐参数/成本已批准；[第一批原计划](archive/2026-10-18/plans/2026-10-16-radiation-foundation-01.md)保留基础范围，[第二批计划](superpowers/plans/2026-10-18-accident-execution-02.md)接事故账本、残骸与回收封存，当前结果见STATUS。
- [04A高温固体与有限火点](superpowers/plans/2026-10-21-accident-thermal-04a.md)：该批来源/验收范围见STATUS；[04B独立熔融物卡](superpowers/plans/2026-10-21-accident-thermal-04b.md)对应[集中体验候选](reviews/2026-10-21/accident-thermal-04b/CANDIDATE.md)。旧总计划04的距离/累计量限制及事故绑定接线停用，当前人工门以STATUS为准。
- [现有生存整线集中观察](reviews/2026-10-16/main-flow-01/READINESS.md)：按实际衔接验证连续制造、启动、工质循环与封存出口；电站值班长构想后置。
- [装配台15C动画](art/tasks/ART-SHIELDED-ASSEMBLY-15C.md)：已合main，游戏观察未报告。
- [模拟器02B](superpowers/plans/2026-09-08-damage-heat-burn-balance-plan.md)、[三维工具](superpowers/plans/2026-08-31-nuclear-plant-3d-html-visualization-plan.md)：辅助任务仍无整体验收，不自动改跑。

已完成卡不列作待派发，文档存在不授权实现新功能。工具说明在各tools目录，main和候选各用独立世界/配置。
