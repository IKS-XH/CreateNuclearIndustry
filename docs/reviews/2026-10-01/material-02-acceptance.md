# 铅锡加工与素材整改：验收收尾

**结论：** 用户于2026-10-01明确表示“复测清单的检查项刚才都手动测试通过了”。PM据此登记EXT-A-MATERIAL-02/02A、EXT-ART-03/03A/04人工门全部通过，按当前实现验收并合入main。用户同时要求优先主线，铅锡水洗副产物暂不处理；保持现有每份粉碎料水洗9粒且无副产物，不新增设计或修复任务阻塞主线。

## 通过范围与整合

用户确认范围为当时的[完整复测清单](./material-02-client.md)：铅/锡普通矿石、深层矿石、粗矿、粉碎料在熔炉/高炉产锭；真实Create水洗九粒及动力恢复；九粒与锭双向合拆；压板及动力恢复；JEI配方/用途；六种新材料外观；保存退出后重进。它不等于第三方金属模组联调或整条设备/燃料链完成。

候选实现为 `7b27a14`，文档同步后 `060aeaf5340d8b5545ccdf84f5c82c041276ad44`。本次先在候选重新运行test/build，确认候选包含main、启动配置不在差异内，再将主分支从 `3fc1f86` 快进至 `060aeaf`；未在主目录进行三方冲突合并。合入后在主工程再次运行test/build。主工程既有.vscode/launch.json保持SHA-256 `65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`，未暂存、覆盖或夹带提交。

| 证据 | 本轮结果 |
| :--- | :--- |
| 候选 `test build --rerun-tasks --max-workers=1` | 退出0；52套件、265项JUnit，失败/错误/跳过均0 |
| 主工程相同命令 | 退出0；52套件、265项JUnit，失败/错误/跳过均0 |
| 主工程制品 | 全部240项源assets/data与JAR逐字节一致，含55张游戏PNG |
| GameTest | 沿用02A的141项required断言全过；保存挂起后精确停止测试进程，Gradle退出1。本次没有重复启动GameTest或服务端，不更改这项历史限制 |
| 客户端 | 用户明确确认完整复测清单通过；本轮PM未另行启动客户端 |

主工程JAR SHA-256：`18801D619ACCF80E0ADFFDB93C211849B509E35C92D2049591643416847DE4F4`。候选JAR仍为 `A4382526AD6AF43A7A82A80992C2F33681D950F5DA1142AFC792108A66FD3A5A`；对比发现177项JSON及pack.mcmeta、neoforge.mods.toml仅CRLF/LF差异，归一化后内容相同，其余已核对条目字节一致。两处构建源码为相同Git内容，不将两个文件哈希混写为同一制品。

[整合核对JSON](./material-02-final/merged-verification.json)与[本轮原始证据包](./material-02-final/evidence.zip)包含候选/主工程构建日志及退出码、两组JUnit XML、制品同源和JAR文本差异核对；ZIP SHA-256 `1EFF5E591B72F300DABEB187ABAD8D45C8F9B44727C2FD708969A07155E3A889`。先前的真实机器、RED/GREEN、美术及独立审查保留在[02A证据](./material-02a/README.md)，不覆盖历史记录。

两工作区运行test产生的根目录跟踪日志已分别备份到本地本轮报告，再只恢复logs/debug.log和logs/latest.log。未启动游戏、删除工作树、迁移存档或清理首轮服务器事故现场；原候选继续保留，供后续已批准主线任务复用。

PM实际使用minecraft-modding、minecraft-testing、minecraft-ci-release及分支收尾/完成前验证流程；按用户既有自动整合授权执行，无发布、推送或技术栈升级。下一步按[材料03方案](../../archive/2026-10-08-completed-plans/2026-10-01-mainline-material-03-proposal.md)核对碳粉—钢锭—钢板，未批准的产率、投入及加工参数单独列为决策，不重复询问已确认工序。
