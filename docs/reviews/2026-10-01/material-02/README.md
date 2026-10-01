# 铅锡加工与板材候选证据

**PM结论：** EXT-A-MATERIAL-02 与 EXT-ART-03 的实现、自动证据和独立审查已收齐，进入客户端待验收；不代表新材料批已合入或完整生产链完成。

- 候选实现：`3a57a88525aa3f716e838a9c4454331db0b9c71c`，基线 `7d01b86d9ac7e07fe73700052af62a904cdb7a7c`；工作树为主工程同级的 `Create_NuclearIndustry-ore-acquisition`。
- [A实现报告](./EXT-A-MATERIAL-02.md)、[整批独立审查](./EXT-A-MATERIAL-02-REVIEW.md)。
- [B素材报告](./EXT-ART-03.md)、[B独立审查](./EXT-ART-03-REVIEW.md)。
- [客户端启动与具体操作](../material-02-client.md)、[运行目录偏差记录](../material-02-isolation-incident.md)。
- [原始证据包](./evidence.zip)：A的RED/初次GREEN/有效隔离GameTest日志、最终build日志与退出码、52份JUnit XML、重载日志、隔离init和测试标签包；B导出/拒绝检查及02A报告恢复哈希。SHA-256为 `56DD35F57F4481F7D9833E50CB486647A2A5CE2A2C35945990E1202F50246EB7`。不含世界、缓存或用户存档，受影响世界备份留在候选本地。

PM实际应用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`、`minecraft-ci-release` 核对锁定版本、行为证据、素材范围和候选版本边界；按计划拆分、独立审查和验证后交付流程派发执行者，未编写功能、测试或构建实现。

最终构建退出0，265项JUnit全过；125项GameTest断言全过但保存挂起，Gradle退出1。标签实际重载三态有日志断言，普通服全维保存有日志，退出0来自A工具执行报告而非独立退出码文件。堵塞机器只覆盖铅熔炉，外部等价标签只模拟铅侧；不扩大为所有机器、第三方模组或客户端验收。

最终JAR SHA-256为 `BE7D16211E1E0600AAF0CE9E697159FFD1FB00E27807BBECDD0800197499F80A`；219项已打包assets/data资源与源一致，含53张游戏PNG。原51张游戏图未改。B曾误写两份02A报告，PM从已提交证据ZIP恢复旧字节；A曾误写默认开发服务器目录，现场备份并修正隔离后复验，但旧目录未恢复。两项过程偏差均保留记录。

自动推进暂停于本批客户端门：熔炉/高炉、压片及动力恢复、JEI、四物品外观、新材料保存重进。未获用户确认前不派发依赖它的钢材或设备，不把本候选合入main。
