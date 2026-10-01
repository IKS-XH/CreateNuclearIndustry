# 材料03收尾：钢材生产链已合入主工程

**PM验收：2026-10-01完成。** 用户已确认[七组客户端清单](../material-03-client.md)全部通过；[03A名称修订](../material-03a/README.md)和本轮完整回归现已整合。钢材实现`a1353dd`、名称修订`c860cf7`、测试调度修复`5a796f5`随`e8ef02e`快进合入main。主工程可以直接运行`gradlew.bat runClient`加载钢粉、钢锭、钢板及完整制钢路线；已启动的客户端需重启。存档仍使用各自工作区的原目录。

## 实现与异常收尾

本轮先以锁定fastutil最小夹具重现迭代期间扩容异常，再在真实普通服单独运行`P1LoopGameTests.dynamicTelemetryPacketReachesClientWithinTenTicks`复现同栈：第5 tick回调中的`onEachTick`向当前任务表追加115项，第6 tick初始化错误重入，随后`wrapped == null`。历史两次全量日志缺少测试名，不能反向声称已逐项追踪到相同对象。

正式修复只调整这一旧测试的调度：初始化移到任务表遍历之后的sequence阶段，仍在第5 tick执行，原模拟客户端、快照、全部断言及十tick上限保留；初始化异常仍记为测试失败。控制棒候选及其他少量嵌套方法未改，临时诊断全部移除。修复后单项在第7 tick成功，随后用最终源码完成155项全量。

## 本轮证据

| 验证 | 结果 |
| :--- | :--- |
| 候选完整GameTest | 四批155项required断言全部通过；无原fastutil异常 |
| GameTest退出 | 通过汇总后停在`Saving worlds`；只结束本轮PID23404，子进程-1、Gradle退出1，**不是正常退出成功** |
| 候选JUnit/build | 新跑52套件265项，失败/错误/跳过均0；build退出0 |
| 主工程JUnit/build | 合入后新跑52套件265项，失败/错误/跳过均0；`test build --rerun-tasks --max-workers=1`退出0 |
| 制品资源 | 候选及main分别核对264项assets/data与各自JAR逐字节一致，60张游戏PNG；钢粉/钢锭/钢板名称正确 |
| 独立审查 | 无阻断项；核对时序、失败语义、临时诊断清除及红绿/全量/JUnit证据 |
| 人工验收 | 沿用用户本次完整手测确认；本轮仅测试调度修复，不增设重复人工门 |

保存停滞是既有测试环境限制，继续单列跟踪；不能将155项断言通过写成整个GameTest进程退出0。控制棒诊断普通服曾正常保存并退出0；遥测修复单项在成功后也有保存停滞，其精确PID1364由PM结束。原Gradle守护进程21468保留。

- [执行报告](./EXT-A-MATERIAL-03B.md)、[独立审查](./EXT-A-MATERIAL-03B-REVIEW.md)、[调度补审](./EXT-A-MATERIAL-03B-SCHEDULER-AUDIT.md)。执行报告前段保留第一阶段诊断过程，最终结论见其“修复后验证”。
- [候选精简证据包](./evidence.zip)：75项，含红绿日志、完整GameTest、JUnit XML、构建、隔离配置、默认run清单与最小夹具；无世界、缓存或JAR。SHA-256：`88E97A5951CA6850BD3802A8AC5B5548DFF07A5168EE4861CCD3450450933546`。
- [本轮生成的根日志](./generated-root-logs.zip)按原字节压缩保存，避免在源码差异中混入日志格式空白；工作区根日志已恢复开工内容。
- [PM候选制品复核](./pm-candidate-artifact-verification.json)、[主工程制品核对](./main-artifact-verification.json)、[主工程构建日志](./main-test-build.log)、[JUnit汇总](./main-junit-summary.json)、[JUnit原始XML](./main-junit-xml.zip)。[复核工具](./verify-artifact.ps1)是本轮证据工具，不参与模组构建。

最终主工程JAR SHA-256：`0722E25E2BC51E983AD67938A22D191D4B499F8F2FE23CB821C428748BE8AD23`。候选本次制品为`E48FA4129DC20E642AA62F8DEDD62809585EDE38C0F31CE8B00EBD959F89152B`，各自打包验证分别记录，不声称两个独立构建的JAR哈希相同。

候选默认run的180文件前后路径、大小、哈希和修改时刻无差异；前后CSV SHA-256均为`4FA17EE79C4E613DF99F645CC5F51D2CDEDADDE2BC50318D29A6A3ECDA8EE90F`。PM保存运行生成日志后，仅恢复两工作区本轮根跟踪日志。主工程既有`.vscode/launch.json`保持SHA-256 `65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`，未纳入提交。

PM按[03B任务卡](../../../superpowers/plans/2026-10-01-ext-a-material-03b.md)与modding/testing、ci-release、完成前验证及分支收尾流程验收并管理Git；执行者未做Git写。没有发布或推送。钢材整合门解除，下一步按首台设备的真实依赖整理最小基础零件参数；未经确认的数量、时间、热级不派发实现。
