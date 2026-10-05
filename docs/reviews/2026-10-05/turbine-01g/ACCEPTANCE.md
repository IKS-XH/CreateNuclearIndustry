# 汽轮机01G断汽停机验收

**日期：2026-10-05。功能基线：8273aff，候选归档基线：2b3402e。**

用户在原装置撤汽、等待流量窗口衰减、再次供汽的复测提示后回复“测试都通过了”。据此记录无其他动力源时外接轴与转速表不再持续零SU转动，重新供汽可恢复；01G现场人工门通过。

自动证据复用[17项真实GameTest](./evidence/power-network-final.log)、[增量构建](./evidence/assemble.log)和[独立审查](./evidence/review.md)。本轮只更新验收文档，源代码、资源、依赖和配置未变，按治理5.1不重跑JUnit、GameTest或构建。制品记录保持原始快照，不追改测试时的人工状态。

01G单项通过后，PM曾集中询问其他联调范围；用户进一步明确“所有测试项都通过了”，01F门槛/效率/周转、冷凝闭环及此前01D/R1外观一并验收，见[联合记录](../condense-01/ACCEPTANCE.md)。其后整合到main，不重复已通过测试。

本轮按锁定的MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280应用minecraft-modding、minecraft-testing的证据分层及minecraft-ci-release的版本边界，并使用finishing-a-development-branch组织收尾。沿用已批准技术栈和本地Git管理授权，保持0.1.0 Alpha；旧存档兼容不在本轮范围。
