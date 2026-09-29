# P1-VERIFY-02 客户端与服务端总验收

**验收日期：** 2026-09-29
**状态：** 已完成；GameTest 世界保存阶段不能正常退出的已知限制继续保留，不作为正常停服通过。
**验收人：** 用户明确任命的项目经理 Codex。

## 验收结论与边界

用户于 2026-09-29 回复“客户端手动测试都通过了”，承接项目经理上一轮列出的本卡 B–D，包括 D6 配置恢复；记录为用户实际操作并确认通过。项目经理没有代操作，没有新采集截图、采样值或配置前后哈希。历史服务端 A 继续引用 2026-09-22 已核验的启动、控制台 stop、全部维度保存和退出码 0，不把它写成本轮新运行。

人工后的自动出口回归由执行者在原生隔离工作树 `C:/Users/IKSXH/.codex/worktrees/p1-final-verification/Create_NuclearIndustry` 执行，基准 `3469a3a42cdee8d0bf450dba7def70be39f573a1`。项目经理已核对 `src/`、`build.gradle`、`settings.gradle`、`gradle.properties` 与原人工验收基线 `8584986` 无差异；本轮没有修改实现、测试或构建。

| 验收层 | 证据与结论 |
| :--- | :--- |
| 独立服务端 A | 2026-09-22 历史证据：正常启动，控制台 stop，全部维度保存，自然退出 0，配置恢复哈希一致。 |
| 客户端 B | 用户确认两布局容量、真实 Create 管网、换料与机械臂、分层护目镜、控制棒/SCRAM、保存重进全部通过。 |
| 客户端 C | 用户确认仪表维护保留、安全清空重组和危险拆除事件占位通过。 |
| 客户端 D | 用户确认磁盘容量/倍率配置加载、非法回退及配置恢复通过；字节级恢复由用户确认，PM 未独立取得本次备份与哈希。 |
| JUnit | `test --rerun-tasks --max-workers=1` 退出 0；PM 独立解析 51 份 XML，261 tests、0 failures、0 errors、0 skipped。 |
| required GameTest | 本轮一次运行取得 `111 GAME TESTS COMPLETE` 和 `All 111 required tests passed :)`；没有模组断言失败或 fastutil 崩溃。随后世界保存挂起，见下节，不能声称 Gradle GameTest 任务成功退出。 |
| 完整构建 | `build --rerun-tasks --max-workers=1` 退出 0，`BUILD SUCCESSFUL`；其中再跑的 51 份 JUnit XML 经 PM 独立复算仍为 261/0/0/0。 |
| 差异范围 | 执行者仅产生报告、测试产物及两份自动日志；无源码变更。生成日志保全后由 PM 定向恢复，隔离工作树 `git status --short` 为空、`git diff --check` 退出 0。 |

## GameTest 退出限制

2026-09-29 18:11:41（UTC+08:00）取得 111 项完整通过汇总后，进程停在 `Saving worlds`。执行者保留两次进程/线程快照，Server thread 位于 `MinecraftServer.stopServer` → `ServerChunkCache.tick` → `ChunkMap.processUnloads`；约两分钟后仅结束已核对命令行的本轮 GameTest JVM PID 2164。Gradle 因子进程退出 -1 最终返回 1。

该事实与既有活动卡明确区分“required 全部通过”及“成功后保存挂起”的验收口径一致。本卡接受完整断言成功结果，保留未正常退出限制，不将其当作已定位或已修复的根因，也不冒充独立服务端 A 的优雅停服证据。未反复重跑争取偶然通过。

首次运行缺少 `server.properties` 的启动日志、既有编译/依赖警告均在原始日志中保留；服务端随后正常运行并完成测试。此次没有因此修改依赖、生产逻辑或测试。

## 证据保全与技能

- [本轮原始报告及证据 ZIP](../handoffs/2026-09-29/P1-VERIFY-02-evidence.zip)：报告、两套 JUnit XML、命令输出/退出记录、GameTest 保存诊断、历史引用副本及两份自动日志原样备份。SHA-256：`0FF8E990B9A33266765B97D4867BF6A01DDA6C16340FCF1B65321A7A5A58869A`。报告中曾误引未生成的排除日志检查文件，PM 发现后已由执行者纠正为实际工具输出证据，未补造历史日志或重跑测试。
- [历史 A 及换机资料](../handoffs/2026-09-28/README.md)：2026-09-22 独立服务端完整原始证据继续保留在其 `previous-task-evidence.zip`，没有改写历史报告。
- 主工作区交付入口：`build/reports/p1/P1-VERIFY-02.md` 及同名证据目录；ZIP 使忽略目录中的证据可随仓库保存。
- 本轮构建制品 `create_nuclear_industry-0.1.0.jar` 的 SHA-256：`21C6723F4F60CB8200364FEF7D4457DDE8C06BF1B215D658091DC2A83321D3BA`。这是开发快照构建，不是公开发布或版本升级。
- 执行者及 PM 实际读取并应用本机 `C:/Users/IKSXH/.codex/skills/` 下的 `minecraft-modding`、`minecraft-testing`；PM 使用 `minecraft-ci-release` 核对交接和制品边界。固定 MC 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。

## 后续

本卡整体验收通过，解除 `P1-VERIFY-03` 前置。P1 最终交接报告仍需单独交付并审核；本次不验收青金石粉或模拟器候选，不提前合并或宣布完整生存生产/发电链完成。
