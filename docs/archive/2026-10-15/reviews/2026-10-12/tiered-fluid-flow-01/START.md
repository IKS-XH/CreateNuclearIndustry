# TIERED-FLOW-01 开工与存档携带

日期2026-10-12。主PM创建并派发；本记录不是实现完成、自动通过或客户端验收。

- 工作树：E:/MyMC/NewMod/Create_NuclearIndustry-fluid-network；分支codex/tiered-fluid-network-flow，基线07b2c1e。从接受生产main建立，未夹带30020e4的待验16³锅炉源码。
- 原生worktree工具无法指定用户要求的E:/MyMC/NewMod/同级路径；list_artifacts仅归档旧树，实际现有逻辑候选含待验尺寸，美术树也有独立工作，均不能干净复用本基线。因此PM依用户既定目录要求使用git worktree add创建同级新树；主树无关dirty源保持，未清理或stash。
- 用户上一批明确继续当前候选测试世界，本次保持该进度来源：Create_NuclearIndustry-art-integration/run/saves/完整复制至新树run/saves/。复制前核实际日志08:52:31所有维度完整保存及停止，无游戏进程；只有IDE/Gradle Java进程。
- 新目标此前不存在saves目录，没有同名旧世界需备份；来源未修改。复制后的“新的世界”27文件，逐文件SHA256差异0，文件数相等；run/saves仍被Git忽略，无链接/共用目录、不回写。新树未复制运行配置，最终新的默认会由本批源码产生；不把候选世界重写为测试夹具。
- 唯一执行者/root/tiered_flow_impl（gpt-6.1-sol/high）工具已启动，读取[实施卡](../../../plans/2026-10-12-tiered-fluid-network-flow-01.md)与[已批准规范](../../../designs/2026-10-12-tiered-fluid-network-design.md)，持有实现与必要验证。主PM只管理文档/Git/审核，未编写功能或测试代码。
- 实施内容仅两档管/阀、单泵与现有口限流；新管网压力、工业口/对应配方、管理员已明确排除。旧锅炉尺寸人工门独立保持。

当前阶段：执行者开工；尚无本批通过的自动测试、GameTest或最终JAR，不预填结果。原主分支制品与其他已验收功能不由本记录改变。
