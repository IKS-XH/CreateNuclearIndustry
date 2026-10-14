# EXT-A-MATERIAL-02 验证目录偏差记录

2026-10-01 验证时，临时 init 仅设置 `JavaExec.workingDir`；锁定的 ModDevGradle 2.0.143 在 `RunGameTask.exec()` 内按 `gameDirectory` 重新设置工作目录。因此首轮 RED、首轮 GREEN 和 15:28 启动的普通服务器实际使用候选树 `run/`，不满足任务要求的报告目录隔离。发现后 PM 停止后续启动，未清理默认运行目录。

## 影响与保留

- 候选路径：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。`run/world` 创建于9月29日，已有历史 GameTest 区块；本轮 RED/GREEN 又写入测试区块，不能称为空目录或全新世界。
- 普通服于15:28:56就绪，15:29:50收到 `stop`，15:29:51保存并正常退出，Gradle退出0。日志记录没有既有世界数据，随后生成 `level.dat`；这不代表目录里没有旧区块。该次未记录操作系统PID，工具会话号不能充当PID。
- `run/server.properties`、`usercache.json` 和若干服务器名单、日志被自动写入。`server.properties` 没有运行前内容备份，未猜测还原，也未声称已恢复。当前残留原位保留。
- 单人世界位于另一目录 `run/saves/`。事后检查其中最新文件修改时间为14:42:42，早于本轮15:22起的测试；这只是时间戳观察，没有运行前哈希快照，不能表述为逐字节校验通过。
- PM 将当前完整 `run/world` 以及上述配置、名单和运行日志复制到候选的 `build/reports/extension/EXT-A-MATERIAL-02/isolation-incident/`，记录相对路径、时间、大小和SHA-256。现场约19 MB；未删除、移动或覆盖任何原存档。该本地备份不随文档提交。

## 修正与复验

执行者已在报告目录的临时 init 中直接设置 `neoForge.runs.server.gameDirectory` 和 `neoForge.runs.gameTestServer.gameDirectory`，正式构建文件不变。PM运行 `materialRunDirectories prepareServerRun prepareGameTestServerRun --max-workers=1`，退出0；模型与任务两层的实际目录一致：

- 普通服务器：`build/reports/extension/EXT-A-MATERIAL-02/isolated-reload`。
- GameTest：`build/reports/extension/EXT-A-MATERIAL-02/isolated-default`。

原始检查输出为 `pm-directory-check.log`。后续验证仅可在修正后的目录运行，并记录启动实际路径和精确PID；最终结果由同批交付报告另行记录。原RED/GREEN证据继续保留，但不再标注为隔离运行成功。
