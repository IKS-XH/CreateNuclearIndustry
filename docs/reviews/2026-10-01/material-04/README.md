# 材料04与素材06：锡条和传感器候选

**状态（2026-10-01）：自动检查、功能与素材分项审查及整批终审通过，候选可以开始客户端验收，功能未合入main。** 主工程继续保留已验收钢材。派发基线`9295e48`，候选为同级`Create_NuclearIndustry-ore-acquisition` / `codex/ore-acquisition`。

## 本批内容

- 1锡锭经切石机产2锡条；机械锯遵从Create原生切石开关。
- 铁板依次部署锡条、红石、电子管，再压片得到1工业传感器。
- 铅板依次部署工业传感器、电子管，再压片得到1辐射传感器。
- 两条装配都是1轮、100%成功，无热和副产物；两类半成品保留Create原生进度。本批仅提供制造材料。
- 五项SVG新素材、模型和双语名称齐备；旧60张游戏贴图（含8张冷却剂）保持不变。

## 验证证据

| 检查 | 本轮结果 |
| :--- | :--- |
| JUnit与构建 | 52套件265项，失败/错误/跳过均0；`test build --rerun-tasks --max-workers=1`退出0 |
| 完整GameTest | 167项required断言全部通过；新增12项覆盖注册、资源、真实加工和状态合同 |
| GameTest进程 | 汇总后停在`Saving worlds`；超过60秒核对本轮PID32544后停止，子进程-1、Gradle退出1，不能称正常退出成功 |
| 真实加工 | 切石菜单取2锡条、机械锯及输出堵塞恢复；两种完整装配准确扣料/进度/终产1件；工业路线代表性错序、错物品、欠料及断动力恢复 |
| 真实标签重载 | 隔离临时包停用→启用→停用并实际reload，等价锡条成员false→true→false；启用时真实机械手消耗并产step1半成品 |
| 锯配置边界 | 关闭`allowStonecuttingOnSaw`时保留锡锭、不产锡条；配置已恢复。两轮隔离普通服均正常stop退出0 |
| 制品与语言 | 源/JAR资源279项逐字节一致，65PNG，5项中英文共10条名称正确；PM独立运行现成核对工具PASS |
| 素材 | 66项清单、65游戏图；原60图及51历史基线保留；重复导出/安装一致，30类SVG负例拒绝、6类CLI失败不写 |
| 工作区保护 | 默认run的180文件前后路径、大小、mtime和哈希完全一致；仅保留原Gradle守护进程，无本批游戏残留；根跟踪日志已恢复 |

保存停滞是既有环境限制，保留原始日志和非零退出记录。夹具早期一次外部标签测试把120tick断言排在默认100tick超时后，已改为180tick并完整重跑通过；没有因此改生产配方或删减测试。真实设备离散步进夹具和ItemStack序列化不能替代玩家的整线体验与存档重进。

- [功能执行报告](./EXT-A-MATERIAL-04.md)、[功能独立审查及证据补审](./EXT-A-MATERIAL-04-REVIEW.md)。
- [整批终审](./EXT-A-MATERIAL-04-FINAL-REVIEW.md)：跨功能、素材和制品核对，无开放问题，允许进入候选人工门；PM据此保存候选，保留人工及main整合门。
- [素材报告](./EXT-ART-06.md)、[素材审查及Minor复审](./EXT-ART-06-REVIEW.md)、[五项预览](./art/five-items-comparison.png)。
- [功能精简证据包](./functional-evidence.zip)：78项，含52份JUnit原始XML、完整GameTest/构建/重载日志、命令、精确PID、默认run快照、根日志前后备份与临时标签包；无世界、缓存或JAR。SHA-256：`C1E2DA17715EFB69255DCD41DD7B2A77F08EE673FF613092CA7606BBE9B5A3E8`。
- [PM制品复核](./pm-artifact-verification.json)、[只读复核工具](./verify-artifact.ps1)；运行工具时将`-ProjectRoot`指向候选根目录。
- [素材验证](./art/verification.json)、[素材命令](./art/commands.json)、[最初60图快照](./art/baseline-60-game-png-sha256.json)。最终素材verification为文字整改后的65→65复跑；原60图另与`9295e48` Git blobs独立比对通过，不把两个起点混为一次验证。

候选JAR SHA-256：`5D47E144FA0909C73B513D8B21BB88589696E4536F9C3F6EE98A567A699791AD`。默认run两份CSV SHA-256均为`EE6C639CEB55512C52B8B12D9850B790CD80CE53BC6B44DE12BA99EF6633136B`。主工程原`.vscode/launch.json`保持`65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`，不纳入本批提交。

## 剩余人工门

按[客户端清单](./CLIENT-CHECKLIST.md)从同级候选运行`gradlew.bat runClient`。需用户确认两条装配与耗料、锡条入口、JEI、五项外观、两类半成品保存重进和中断恢复。未取得这些结果前，不验收整批、不合入main、不派发下一材料或设备批。PM依据既有授权及分支收尾流程保留候选分支和同级工作树，没有推送或发布。
