# DEVICE-PONDER-08-STORAGE 实现交付

2026-10-11，执行者交付未提交候选；自动检查完成，独立窄审及用户W播放尚未进行。工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，实际基线 `96fde12fe54b65112f1790ede812cd1a797c27fd`；卡中 `2f633749` 为同步历史，PM已明确本次实际基线。

单页 `dry_storage_usage` 依次讲手持右击存入、空手右击取出与背包满保留、东侧输入/底部向南输出、护目镜默认16桶可配置、满架上游等待、腾出空位继续存入。六段正文逐段播放，每段等待 `duration+20`，不讲配方。五格地台、设备最低y=1；有效桶由正式 `SpentFuelPayload.seal` 创建，16桶为16个独立单件槽。仅通过现有 `loadWithComponents` 读入Ponder临时快照，外观占用档位同源更新，不调用正式存取/tick/同步事务，不放宽 `current()` 的客户端拒绝边界。

## 精确七路径与冻结SHA256

| 路径 | SHA256 |
| :--- | :--- |
| `src/main/java/com/iksxh/create_nuclear_industry/ponder/DryStoragePonderScenes.java` | `9b37540e0e5c9056772f401cb8ed330c93028282afea7f3488d882874ed499c3` |
| `src/main/java/com/iksxh/create_nuclear_industry/ponder/P1PonderPlugin.java` | `daa3ff22db31baeacf79d1d5d2d20d36f9d944069b19f138df62731705ab79a5` |
| `src/main/resources/assets/create_nuclear_industry/ponder/dry_storage_usage.nbt` | `d16e955cc8c59a133f1b6a7f6ce491fde2fa11e8200ff734dca79a6c2843a63a` |
| `tools/ponder/dry_storage_scenes.py` | `7b303977987f44e13518c5ac8e73c2947b22d4e82cddeefc5f8d1ec0d65ef0a5` |
| `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json` | `9f9834c4b6b7e69129b311a716ed930420861f30045b2ad2451ba7076bf6c453` |
| `src/main/resources/assets/create_nuclear_industry/lang/en_us.json` | `d7aaf1808a54aff844c15f1bd3de0ce44c2668c5bb605d3ed89aeacb1b055ecf` |
| `src/test/java/com/iksxh/create_nuclear_industry/DryStoragePonderContractTest.java` | `379b5df6a10f93b27164866771c66de3cd0f1948107b3bdd8c30880e2190741b` |

## 检查与证据

证据根目录：`build/reports/extension/DEVICE-PONDER-08-STORAGE/`。

- `01-red-*`：`./gradlew.bat test --tests '*DryStoragePonderContractTest' --console=plain`，退出1，4/4因实际缺少入口、场景或模板而失败；不是编译或环境异常。
- `02-generator*`：生成及 `--check` 均退出0，独立落盘回读30块、唯一架、完整组件载荷、合法漏斗方向及设备高度；重复编码/确定性gzip字节一致。
- `03-final-*`、`03-intermediate.jar` 与 `03-intermediate/`：首轮两类定向test+增量assemble退出0，20秒，10/10。随后核到原生GUI只翻转Y，发现初选70°镜头仍朝北面，本轮作为中间候选保留。
- 原生 `preflight/SceneTransform.javap.txt`、`UIRenderHelper.javap.txt` 与依赖JAR SHA：默认X=-35°、Y=145°；修正为180°增量后Y=325°。`04-camera.json` 记录观察射线南/东分量均为正，并不代表客户端播放通过。PM明确该卡内取景修正及对应重复定向验证可以直接执行。
- 最终 `./gradlew.bat test --tests '*DryStoragePonderContractTest' --tests '*P1Ponder01ContractTest' assemble --console=plain`，Java21，退出0，17秒；9项任务中4执行、5UP-TO-DATE。`compileJava`、`jar`、`compileTestJava`、`test` 实际执行，资源复用；未clean、强制重跑、全量或启动客户端。
- 最终XML `04-final-storage.xml` 为4项，`04-final-p1.xml` 为6项，总10/10，失败/错误/跳过均0。完整命令、日志及退出码见 `04-final-command.txt`、`04-final.log`、`04-final.exit`。
- `04-manifest.json` / `04-frozen/`：七源原字节、两个生产class及最终JAR冻结；两个class与JAR相等，新增模板/两语言的来源、build资源、JAR逐字相等。1715个范围外已跟踪生产/测试/工具路径按Git文本过滤规则逐路径保持；原19模板及正式贮存资源与JAR相等。旧语言所有键值与顺序保持，只各加7键。未改变正式贮存、美术、配方或其他教学。
- `04-diff.txt`、`04-status.txt` 保存跟踪路径差异与含新文件的实际状态；新增文件完整冻结在 `04-frozen/`。范围内差异空白检查退出0；全树空白检查只报告自然变化日志的既有末尾空格，日志不清理。生成器导入产生的pycache保留。

最终JAR：`E:/MyMC/NewMod/Create_NuclearIndustry-art-integration/build/libs/create_nuclear_industry-0.1.0.jar`，**2514642字节**，SHA256 **`35c4befd197051338c7840c8f4e80d66e4462d1090de2f7ab1a27b6853b17da7`**。冻结副本：`build/reports/extension/DEVICE-PONDER-08-STORAGE/04-frozen/create_nuclear_industry-0.1.0.jar`。

实际应用 `minecraft-modding`、`minecraft-testing`（本会话已完整读取，复用）、`minecraft-resource-pack`（本批读取）、superpowers TDD/完成前验证/计划执行规则；子代理执行技能的派发步骤服从执行者禁令，未委派。使用锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，未升级依赖。

无未决范围缺口。代码冻结后停止写入，交PM安排唯一独立窄审及单页W播放；字幕净空、物品识别、实际屏幕播放条遮挡仍属用户播放门，不以自动合同或取景推导替代。未进行任何Git写或世界操作。
