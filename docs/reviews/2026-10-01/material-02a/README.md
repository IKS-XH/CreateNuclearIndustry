# 铅锡加工与素材整改候选证据

**PM结论：** 实现、自动验证、离线美术检查与独立审查已收齐，可交用户进行 [客户端复测](../material-02-client.md)。用户尚未确认本批人工门，功能未合入main，不自动推进钢材或设备。

- 候选工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支 `codex/ore-acquisition`。本轮基线 `0d38c99`，素材提交 `fa94d67`，功能提交 `7b27a14bfbd6992f0d3fb61f44dd058413826459`；后续仅同步main文档，不改已验证功能。
- [原生路线审计](./EXT-A-MATERIAL-02A-NATIVE-AUDIT.md)、[A实现与测试](./EXT-A-MATERIAL-02A.md)、[独立整批审查](./EXT-A-MATERIAL-02A-REVIEW.md)。
- [两板整改](./EXT-ART-03A.md)、[两粒与管线](./EXT-ART-04.md)、[板材前后对照](./plate-comparison.png)、[四形态对照](./nugget-preview.png)。
- [原始证据包](./evidence.zip)，SHA-256 `9A572CACDE8338341D034DCD2903C7BCF825DF5580EC01BC131D24D0A7AA7615`。内含锁定Create配方摘录，A目录诊断、RED/GREEN、进程与退出码、最终构建、52份JUnit XML、默认目录哈希、资源核对，以及B/C的离线验证与快照。不含世界、缓存或用户存档；报告内相对原始证据链接按ZIP内对应目录查找，原文件也保留在候选build/reports/extension。

本轮最终265项JUnit全过，build退出0。新增16项真实炉加工、风扇水洗和粒锭合拆检查，加既有测试共141项required GameTest断言全过；随后Saving worlds挂起，有限等待后只结束本次Java PID32348，Gradle退出1。RED的16个预期缺配方/注册失败之后出现fastutil异常，未形成完整汇总；不把失败阶段或保存挂起包装为成功退出。运行器限制延续记录，本轮未扩大到修复测试框架。

最终JAR SHA-256为 `A4382526AD6AF43A7A82A80992C2F33681D950F5DA1142AFC792108A66FD3A5A`。A核对174项assets/recipe/nugget-tag资源，PM另行核对全部240项assets/data文件均与JAR逐字节一致，含55张PNG。历史51张游戏图与8冷却剂不变；B修改两板，C新增两粒并保持当时已有53图原字节。B最后仅删除SVG末尾空行，不影响像素；C正式验证器不再依赖被忽略的历史报告快照。

本轮默认run的176文件及其run/saves内68文件前后SHA-256无增删改。A交付时自动改动的根目录两份跟踪日志，PM已备份到本轮报告的tracked-logs-backup后仅恢复这两个路径。首轮02误写的服务器世界和配置仍按 [历史记录](../material-02-isolation-incident.md) 保留现场，不宣称已还原。主工程既有.vscode/launch.json未改动或夹带提交。

独立审查只提出旧02任务卡范围表达需明确；PM已将其标为首轮历史合同，并写明水洗、粉碎料直熔与粒锭合拆排除项由02A取代。其余实现无阻塞。当前不包含第三方金属模组联调、客户端JEI交互、真实客户端保存重进或视觉最终验收；这些不能由自动测试和离线图片代替。

PM按minecraft-modding、minecraft-testing、minecraft-resource-pack及版本治理技能复核当前锁定版本与交付边界，按独立审查、完成前验证流程处理交付；未编写功能、测试或构建实现。
