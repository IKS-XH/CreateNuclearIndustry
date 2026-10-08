# EXCHANGER-02R1 实施记录

## 行为

工作盆位于核热换热器正上方时，换热器按 `basinHeatLevelEquivalent × huPerLevel` 固定HU/t持续消耗核热冷却剂，并将热液等体积转为冷液。空盆、缺料、无搅拌器、停转和输出堵塞均不改变负载。默认费用为2HU/t，在0.5HU/mB下为4mB/t。实际付款视图允许热源结算后的当前或相邻一个game tick被先行执行的搅拌器读取；热源下一tick发现缺液/冷满会撤销发布。查询路径只读取账本，不换液、不扣账。

整数mB造成的舍入由唯一HU储备字段保存，且严格小于1mB等效HU：有热液和足够回流空间时，以必要的向上取整保持固定费用连续，累计实际换液差额；断流、冷罐满、盆移除、配置/模式变化、tick中断、保存或切回普通锅炉路径时清除舍入余额，不形成独立预热池。盆费用配置为 `basinHeatLevelEquivalent`，默认2、范围1～18；非法或超过额定能力时停用盆供热，不改变旧锅炉共享有效性。

进入盆用途时先清除普通锅炉HU储备及锅炉流量尾差；只有连续有效的盆tick可携带受限舍入额。Create检查仅在热源状态变化/首次接入等必要边界被唤醒，稳定供热不逐tick重复安排检查。

上一版按批次探测和收费的三个Mixin及注册已移除，原生Create BasinRecipe处理入口未被拦截。无本设备时，原生普通/超级烈焰人加工代表仍通过实际GameTest。

## 改动范围

- 更新 `HeatExchangerState`、`HeatExchangerBasinBridge`、`NuclearHeatExchangerBlockEntity` 和 `HeatExchangerConfig`，实现固定连续付款、同tick只结算一次、最多相邻tick的只读已付热级及配置边界。
- 删除 `BasinRecipeHeatMixin`、`BasinOperatingMixin`、`BasinOperatingAccessor` 及对应Mixin注册。
- 更新中英文运行提示和工作盆GameTest；账本测试覆盖倍率、同tick幂等、相邻tick热视图及过期撤热、分数mB连续供热、<1mB舍入余额、普通锅炉储备切盆隔离、断流、近满冷罐停热及恢复。GameTest增加搅拌器先登记/热源后登记的原生heated加工。
- 未修改 `HeatExchangerLine`，未修改任务卡、治理文档或用户世界。原有 `logs/debug.log`、`logs/latest.log` 和三个 `tools/**/__pycache__/` 保留。

## 验证

技能：实际读取并应用 `minecraft-modding`、`minecraft-testing`，版本按Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82核对。

| 命令 / 证据 | 结果 |
|---|---|
| R1初次实现：`./gradlew.bat test --tests '*HeatExchangerStateTest' assemble`（见 `unit-assemble-final.log`） | exit 0；20项JUnit通过；增量assemble成功。 |
| R1初次实现：`./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_ext_b_basin -PgameTestDirectory=build/gametest-ext-b-basin-r1`（见 `gametest-final.log`） | exit 0；6/6通过，包含空盆持续供热、断流恢复、真实普通/超级加工及原生普通/超级燃烧室加工。 |
| `git diff --check -- src/main/java src/main/resources src/test/java` | 通过；仅有Git换行格式提示。日志路径未纳入检查且保留原样。 |

## 审查整改复验

- P1：`basinHeatingAt`接受当前付款tick或其后紧邻的一个tick，仍要求真实热源身份有效；源在下一tick结算缺液/冷满时立即清除标记，过期两tick以上返回无热。搅拌器先于热源登记的真实原生heated配方通过；唤醒改为Basin供热状态转换/首次接入时调度，普通冲突路径也只在状态变化时通知Create。
- P2：新进入盆用途时清除普通锅炉 `reserve`、锅炉 `flowFraction` 和旧盆流量尾差。新增密度5HU/mB账本断言先由普通供热储入15HU，再切盆时真实转换1mB、留下3HU（小于5HU）、热冷总量守恒。
- 最终整改复验命令：`./gradlew.bat test --tests '*HeatExchangerStateTest' assemble`，exit 0、21项通过；复测控制台 `build/reports/extension/EXT-B-EXCHANGER-02R1/unit-assemble-review-fix-2.log`，退出码文件同名 `.exit`。最终JUnit XML为 `build/test-results/test/TEST-com.iksxh.create_nuclear_industry.heat.HeatExchangerStateTest.xml`；前版XML另存为 `HeatExchangerStateTest-before-review-fix-2.xml`。
- 最终整改GameTest命令：`./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_ext_b_basin -PgameTestDirectory=build/gametest-ext-b-basin-r1-review2`，exit 0、7/7通过；控制台 `gametest-review-fix-2.log` 与 `.exit`。原6项代表及新增反向ticker顺序原生heated配方均通过。
- 为覆盖稳定热状态下后加入/替换mixer的首次连接，以及源暂停/卸载撤热通知，追加了实例变更追踪；冲突/暂停只在状态变化或首次接入时调度。仅通知路径变化，复用上述21项账本XML。最终增量 `assemble` exit 0（`assemble-review-fix-3.log/.exit`），再次隔离GameTest 7/7通过（`gametest-review-fix-3.log/.exit`），使用全新目录 `build/gametest-ext-b-basin-r1-review3`。此次日志名均为新文件，未覆盖既有证据。

首次构建曾因测试直接调用Create包可见性受限的 `BasinBlockEntity.getHeatLevel()` 而编译失败；测试改为断言公开的只读 `HeatExchangerBasinBridge.heatLevel()`，实际原生配方GameTest继续覆盖Mixin注入。首次GameTest的断流恢复用例把恢复后的冷液量固定为单tick值，实际回调跨过两个设备tick；夹具改为检查冷液增加及热级恢复。可见失败摘要见 `build/reports/extension/EXT-B-EXCHANGER-02R1/gametest-initial-failure-observation.txt`；最终成功的完整运行见 `gametest-final.log`，此前成功重跑见 `gametest-final-retry1.log`。末次重跑误复用最终日志路径，首轮完整GameTest失败日志被覆盖；证据目录明确记录此情况，摘要不是原始完整日志。各命令保留相应exit文件。

构建JAR：`build/libs/create_nuclear_industry-0.1.0.jar`  
SHA-256：`3271FAFB1CEC8E9D977223D3CF461022081388074C53B1DEBC00D8B2F1385D6A`

人工客户端与汽轮机集中播放验收仍待PM安排；本记录只覆盖自动化与构建证据。
