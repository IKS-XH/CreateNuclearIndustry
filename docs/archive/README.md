# 文档归档索引

本目录保存已经完成、被替代或不再作为当前执行依据的设计与任务计划。归档文件保留决策历史，不作为开发者当前实现合同。

## 当前规则

- `docs/` 下的五份设计文档（项目、反应堆局部控制、内容清单、配方、彩蛋与进度）是活动设计依据；`docs/implementation-roadmap.md` 是第六份核心文档，负责活动阶段与验收依据。
- 活动任务放在 `docs/superpowers/plans/`；任务完成或被新计划替代后，才由项目经理移动到本目录。
- 任何归档动作必须由项目经理执行，并在提交说明中写明替代关系。

## 当前活动计划

当前活动计划为：

- `../superpowers/plans/2026-08-18-agent-developer-execution-plan.md`
- `../superpowers/plans/2026-08-18-p1-png-generation-brief-for-gptimage2.md`

## 已归档计划

- `2026-08-18-p0-api-probe-and-numeric-prototype-plan.md`：P0 API 探针、数值原型、GameTest 和人工验收已通过，作为 P1 开工门禁完成。
- `2026-08-17-project-manager-execution-plan.md`：已被 2026-08-18 的 Agent/开发者 P1 交接计划取代，保留作为历史任务记录。
- `2026-08-18-reactor-html-simulator-plan.md`：HTML 数值模拟器的 SIMWEB-01 至 SIMWEB-07、15 项离线回归和人工验收已完成；保留为数值实验工具的交接与决策记录。
- `2026-08-19-integrity-zero-simulator-alignment-plan.md`：P1-SIMWEB-08 已将零完整度继续运行、无全堆流量上限、`128 mB/t` 单端口、SCRAM 可用性和离线单文件同步到模拟器；项目经理复验 28 项 Node 测试通过，用户已完成人工验收。
- `2026-08-25-chinese-code-comments-execution-plan.md`：P1-COMMENT-00 至 P1-COMMENT-06 已完成；存量 Java 中文注释通过全量审计，永久规则已收敛到根目录 `AGENTS.md`。

## 已归档任务验收报告

- `P1-LOOP-02.md`：反应堆冷端、热端和换料端口已绑定到唯一结构所有者；capability 缓存随结构成型、失效和恢复正确刷新，隔离复验为 145 项 JUnit 与 29 个 required GameTest 全部通过。
