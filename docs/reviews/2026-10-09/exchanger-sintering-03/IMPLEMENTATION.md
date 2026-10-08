# EXCHANGER-03 实施记录

## 行为

核换热器正上方放置燃料烧结炉时，换热器替代烈焰人燃烧室给该炉供热。烧结炉复用工作盆的唯一持续供热账本：默认每tick支付2 HU，并在0.5 HU/mB密度下将4mB热液等量转成冷液。空炉仍耗热；热液不足或冷液罐满后，换热器下一次结算停止发布热级。恢复供回液后继续加工，原有400有效tick工时与进度保留。没有核换热器时，原生普通/超级燃烧室检查保持原路径。

烧结炉服务端仅在其下方核换热器完成实际付款后读取该tick热源。为兼容换热器与负载的tick先后，读取沿用账本既有当前/相邻tick付款窗口；客户端只查看换热器同步的`viewHeat`与`sintering_*`状态，不推算客户端账本。没有为烧结炉伪造Create烈焰人`HEAT_LEVEL`方块属性，也没有新增HU储备、换液事务或配方执行引擎。

换热器护目镜对该负载显示“燃料烧结炉持续供热中”及热液/回液阻塞原因。`basinHeatLevelEquivalent`原键和默认值不变，中文配置说明现在明确其同时作用于工作盆和燃料烧结炉顶部负载。

## 改动范围

- 扩展`HeatExchangerBasinBridge`识别顶部工作盆或烧结炉；热量查询只读账本付款tick，烧结炉客户端路径只读同步视图。
- 扩展`NuclearHeatExchangerBlockEntity`主ticker将烧结炉送入现有`tickBasin`结算，并同步负载专属护目镜状态；工作盆唤醒行为保留。
- 扩展`FuelSinteringBlockEntity`优先查询下方换热器的已付款热源；原烈焰人路径保留。
- 更新`HeatExchangerConfig`既有配置键中文说明，并仅向中英文语言文件添加烧结炉负载状态与速率键。
- 新增隔离GameTest类`ExtensionHeatExchangerSinteringGameTests`。为满足NeoForge 21.1.219的注册器过滤规则，PM裁定将原109字节盆空结构原样复制到`data/create_nuclear_industry_ext_b_sintering/structure/basin_empty.nbt`；复制前后SHA-256相同，模板内容未改。
- 未修改`HeatExchangerState`、共享直列流体账本、配方、模型、纹理、汽轮机文案或用户存档。保留原有`logs/debug.log`、`logs/latest.log`及三个`tools/**/__pycache__/`。

## 验证

实际读取并使用技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`。技术版本核对为Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。

| 命令 / 证据 | 结果 |
|---|---|
| `./gradlew.bat assemble --console=plain`（`build/reports/extension/EXT-B-EXCHANGER-03/assemble-final3.log`及`.exit`） | exit 0；最终JAR已由最后源码增量打包。 |
| `./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_ext_b_sintering -PgameTestDirectory=build/gametest-ext-b-sintering-03-review5 --console=plain`（`gametest-review5.log`及`.exit`） | exit 0；3/3通过。覆盖空炉持续耗热、先登记炉后登记热源的实际加工、冷液满但热液仍有余量时暂停并保留工时、热液耗尽时暂停并恢复。 |
| `git diff --check`（限本批Java、语言文件及新模板） | 通过；仅报告Git既有换行格式提示。中英文JSON均可解析。 |
| 账本JUnit | 复用已验收02R1的`HeatExchangerStateTest` 21/21证据（`build/reports/extension/EXT-B-EXCHANGER-02R1/unit-assemble-review-fix-2.log`及`.exit`）；本批未改账本。 |

验证入口调整记录：首次隔离GameTest把完整模板ID误解析为测试命名空间中的路径，退出1；原始日志为`gametest.log`，崩溃报告在`build/gametest-ext-b-sintering-03/crash-reports/`。第二次显式指定旧模板`templateNamespace`后，锁定版`GameTestRegistry`按该namespace过滤测试，本批新namespace没有测试函数；Gradle虽返回0，但不计作测试通过。执行者从本地NeoForge 21.1.219 `GameTestRegistry`及`GameTestHooks`字节码确认，`templateNamespace`同时决定测试过滤和结构寻址。PM随后授权同字节复制模板，使本批namespace可独立运行。之后的真实断言失败分别保留在`gametest-review2.log`和`gametest-review3.log`；review4曾3/3通过。PM复核指出冷堵用例需证明热液仍有剩余，review5补入该断言并再次3/3通过。所有失败日志均未覆盖。

最终制品：`build/libs/create_nuclear_industry-0.1.0.jar`

SHA-256：`B2CD6BF6AF05E51AF4CB99018178844ADF8260A6EA5A2F740A97C1A37E20D49F`

本记录只证明静态检查、增量打包、账本既有证据复用及本批自动GameTest。客户端护目镜和热液烧结的人工验收仍由PM按任务卡安排；未启动用户客户端或存档。

本次最终文字校正只修正上述Javadoc及本报告首句，不改变运行语义；测试结果与制品哈希沿用此前记录，未机械重跑构建或测试。
