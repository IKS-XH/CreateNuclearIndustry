# 干式贮存架思索08：存取与自动化

**状态：** 用户2026-10-11明确要求开始，已确认一页用法教学；正在实施，尚无自动/审查/播放通过结论。任务ID：`DEVICE-PONDER-08-STORAGE`。

## 授权、基线与职责

- 单页介绍封装桶手动存取、漏斗自动化、护目镜占用量与满架等待；思索只介绍用法，不介绍具体配方。
- 复用同级隔离树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，已快进至main `2f6337491f2d7b3c99559247d416c5684911c0c2`。此树已结束上一批修复，不占用独立美术树。旧任务卡仅状态文字落后，原字节备份在该树 `build/reports/reactor-rod-coolant-01/preflight/`；既有脏日志保持。
- 主PM仅设计、文档、审核和Git；复用原教学执行者与独立审查者。执行者不进行Git写、不派发子代理、不修改核心文档。美術并行候选不随此教学扩改或验收。
- 当前正式功能已按[STORE-01验收](../../reviews/2026-10-06/store-01/ACCEPTANCE.md)通过；本批不修改或重复验收正式贮存逻辑。

## 真实规则与单页步骤

1. 正面可见、居中的单格贮存架放在地台之上。手持有效封装乏燃料桶右击，每次存入一桶；随后以空手右击取出一桶。没有潜行要求，不新增GUI或动力要求。
2. 展示两条可区分的输入/输出漏斗路径。顶部及四侧均可输入；各面可输出，底部只可输出。演示路线可选择两个可见侧面，不能误画底面输入；不添加原版物流基础课或创造马达说明。
3. 随存入显示占用增加；护目镜查看已用/容量，默认16桶须明确为默认值，不能声称配置始终固定16。外观占用档位与实际教学库存同步。
4. 演示满架拒收，额外待输入桶仍留在上游；取出一桶后空位允许继续存入。不表现吞物、删除桶或自动销毁乏燃料。手动取出时背包满会保留桶，可用简短正文说明，无需另开页。

正文一次只出现一段，等待时长至少覆盖显示、10tick淡出和额外10tick净空。物体、漏斗、桶及文字远离底部播放条；画面小而集中，不使用大面积空地台。高亮指向贮存架本体及当前操作路径，不用大范围高亮替代细节。

所有字幕为面向玩家的操作文字，不包含“画面仅作示意”等开发措辞，不包含材料清单、具体封装配方、加工时间、未实现辐射/再处理/永久安全承诺。原七类教学顺序、正文和模板不变。

## 精确实现写集

1. 新增 `src/main/java/com/iksxh/create_nuclear_industry/ponder/DryStoragePonderScenes.java`：唯一公开故事线 `usage(SceneBuilder, SceneBuildingUtil)`，标题ID `dry_storage_usage`，只读/演示当前规则，中文说明Ponder客户端与正式服务端账本边界。
2. 修改同目录 `P1PonderPlugin.java`：仅增加干式贮存架物品到 `dry_storage_usage` 的直接绑定；不改变冻结P1入口列表和其他设备列表/顺序，不新增第二页。
3. 新增 `src/main/resources/assets/create_nuclear_industry/ponder/dry_storage_usage.nbt`：唯一教学模板，注册ID、方块状态和物流方向必须匹配锁定版本，所有设备在地台上方。
4. 新增 `tools/ponder/dry_storage_scenes.py`：复用现有NBT编码工具；只生成或只读核对本页，确定性压缩及独立落盘回读，不写其他模板或游戏世界。
5. 修改 `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json` 和 `en_us.json`：仅新增本页对应键，保留其他键和值，不全文件重排。
6. 新增 `src/test/java/com/iksxh/create_nuclear_industry/DryStoragePonderContractTest.java`：针对入口/双语/真实模板与时序的窄合同；不得为教学重测正式库存算法。

上述为7个交付路径。正式 `storage/`、注册、配方、配置、模型/贴图/动画、构建脚本、其他教学和用户存档均只读。测试证据只写 `build/reports/extension/DEVICE-PONDER-08-STORAGE/`；执行报告只写 `docs/reviews/2026-10-11/dry-storage-ponder-08/IMPLEMENTATION.md`，独立审查仅写同目录 `REVIEW.md`。范围缺口先报告，不擅自扩大。

## 教学库存与资源边界

- 只有教学世界的库存快照与 `storage_level` 随演示变动，不调用正式服务端的插入、取出、tick或同步入口，不新增生产setter/客户端放宽权限。`DryStorageBlockEntity.current()`在客户端拒绝正式事务，这个边界必须保留。
- 封装桶实际包含数量1的正式 `cooled_spent_fuel_assembly`，通过既有 `SpentFuelPayload.seal` 或同格式教学快照创建，不使用空壳或非法容器假装可存入。占用槽保存整件有效桶；满架为16个单件槽，不能以一个栈的count=16伪造。
- 模板可从空库存开始；播放时用既有客户端读入路径或独立教学快照更新。生成器不需要重复制造正式工序；保留一份独立载荷样例核对完整组件容器格式即可，不向正式配方加入教学物品。

## 技能、验证和交付

开工实际读取根AGENTS、本卡、治理5.1/5.2和以下技能入口：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`
- 适用的 `superpowers` 子代理执行、TDD与完成前验证技能。保持MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，不升级或改许可证。

- 先保存本页缺失时有意义的定向RED，再实施。实际NBT回读检查物流方向、方块高度、唯一贮存架与载荷；检查唯一入口及双语正文齐全、正文淡出净空和收尾。
- 只运行新 `DryStoragePonderContractTest`、受改插件的既有 `P1Ponder01ContractTest`，以及一次增量 `assemble`。最终XML核对测试数、失败、错误、跳过及退出码。不上全量、GameTest或自动启动客户端；旧正式贮存功能与已验收教学复用历史证据。
- 生成器确认确定性与落盘回读；冻结JAR、精确7路径源SHA、JAR SHA和对应class/模板/语言打包核对。原七类教学与正式贮存代码/资源逐路径保护，保存实际差异清单；不清理日志或pycache。
- 实现冻结后安排一次独立规格/质量窄审，审查者核查上述7路径和证据，不重跑同一测试/构建。发现实际必改项只整改受影响处并复验对应检查。
- PM绑定隔离树净候选后交用户按W播放：只有一页；存入/空手取出清楚；两路漏斗分开；占用/满架等待正确；正文不重叠、机体不被播放条遮挡。**人工门通过前不合main，不推进新主线。**

## 开工一致性核对

| 子项 | 输出与消费/验证 | 核对结论 |
| :--- | :--- | :--- |
| 本页实现 | `dry_storage_usage`方法/模板/插件/双语同ID；七路径由专属合同与JAR核对消费 | 一致；只绑定贮存架物品，不改变P1冻结列表 |
| 库存演示 | 有效单件桶、16槽满架、外观与占用同源 | 复用真实载荷格式；客户端只读/快照，不修改正式权限 |
| 验证和交付 | 定向RED→GREEN及增量JAR→一次窄审→播放 | 自动与人工分开；正式功能与旧教学不重复测试 |

本卡已明确范围，不存在待用户决定的新玩法参数；进入实现。
