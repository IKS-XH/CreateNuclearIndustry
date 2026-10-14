# EXT-B-CONDENSE-01 实现交付

执行者交付，待PM独立联合审查及后续闭环人工联调；本报告不改变任务状态，不代表人工验收通过。

## 起点、范围和技能

- 唯一目录：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`。
- 实际开工HEAD：`1aa6987870ec4e7f7106084ff945ee56313c0b7f`；交付读到`4657d06ffc41bc8af546cac03ff7d6d078bf0efa`，期间PM文档提交，执行者未进行任何Git写操作。
- 已读AGENTS.md、治理5.1/5.2、完整2026-10-05冷凝规格与实现卡。旧36mB/t、邻近冷源扫描不适用。
- 实际读取并应用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`与`minecraft-testing/SKILL.md`：沿用当前NeoForge能力及服务端ticker、原生Create管泵，按锁定版本使用`structure/`目录和独立GameTest namespace；未照搬技能新版本注册示例。失败定位使用systematic-debugging；交付检查使用verification-before-completion，验证频率由治理5.1限定。
- 实际核对：MC1.21.1、Java21（Gradle使用`C:/Program Files/Java/jdk-21/bin/java.exe`）、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。无依赖升级、GUI、配方/模型、注册或构建脚本改动。

## 交付行为

- 既有换热器背口接收普通`TurbineContent.STEAM`，前口输出原版水；拒绝超临界蒸汽。原核热工质、HU与锅炉供热入口保留。
- 首份成功实际输入选模式；SIMULATE/零接收不选模式。各台真实工质库存及核余热阻止异种输入，混列整体暂停交易/转换/热发布，分拆恢复且只携带本机份额。
- 冷凝同向直列最多16台，各台54mB/t，各台两罐4000mB，共享库存/空位但每台只结算自己的tick预算。回水产物量先预检再原子提交；千分比尾量属本机、范围0～999，随当前格式保存恢复。
- 每台只检查顶部一格已加载方块。标签默认water/snow_block/ice/packed_ice/blue_ice，标签之外或没有已定义消耗规则的扩展无效；流水/含水方块/雪层/侧面冷源均无效。
- 雪块/冰100000mB、浮冰900000mB后融为水源；水源100000mB后蒸发为空气；蓝冰持续。只以本机成功冷凝输入量结算，截在当前阶段边界，世界冷源变化不向机内加入额外水，暂停/能力模拟/存取不耗冷源。
- 冷凝不发布Create锅炉热，不领取专用锅炉HU，无Create应力来源。护目镜独立显示模式、蒸汽/水库存、实际mB/t与暂停原因，中英文键齐全；服务端View包含模式/容量/实际流量。
- SERVER文件`create_nuclear_industry-heat-exchanger.toml`实际生成并加载8项默认：rate=54、recovery=1000、steamCapacity=4000、waterCapacity=4000、snow=100000、ice=100000、packedIce=900000、water=100000；完整键名见HeatExchangerConfig与`build/runtime-condense-01-final/config/create_nuclear_industry-heat-exchanger.toml`。
- 非默认333‰回收率、350mB蒸汽容量及缩至100mB/回水50mB在真实服务端配置快照生效。测试在同一服务端回调中临时设置，`finally`恢复；没有中间世界tick供其他场景读取，未写开发配置/用户世界配置。原生Create共享额度与实际逐机空位统一；空机SIMULATE的工质提示仅在能力句柄内缓存，不选择账本模式、不预约库存、不保存。

## 精简验证与原始证据

所有命令在唯一目录执行，输出保存在本报告目录，退出码另存同名`*-exit.txt`。

| 命令 | 结果/日志 |
|---|---|
| `./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_condensation -PgameTestDirectory=build/runtime-condense-01-red` | 预实现代表性红灯，旧实现拒收普通蒸汽；退出1，`red.log` |
| `./gradlew.bat compileJava` | 初版增量编译退出0，`compile.log` |
| `./gradlew.bat test --tests '*HeatExchangerStateTest' --tests '*Condensation*Test'` | 首版退出0，`junit.log`；补风险边界后的最终退出0，`junit-final.log`。最终22项：原热账本16、新冷凝6，failures/errors均0 |
| `./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_condensation -PgameTestDirectory=build/runtime-condense-01-default` | 首轮7项中6项过、泵管守恒断言失败；退出1，`gametest-first-failure.log`及`gametest.log` |
| `./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_condensation -PgameTestDirectory=build/runtime-condense-01-diagnostic` | 加数量诊断后复现GameTestInfo.tickInternal/fastutil迭代异常，退出1，`gametest-diagnostic.log`，对应隔离目录保留crash-report |
| `./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_condensation -PgameTestDirectory=build/runtime-condense-01-diagnostic2` | 将onEachTick注册移到初始化并用started标志跳过注汽前，守恒断言保留；7/7过、退出0，`gametest-diagnostic2.log` |
| `./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_condensation -PgameTestDirectory=build/runtime-condense-01-final` | 最终8/8 required过，退出0，`gametest-final.log`。正常保存并退出，无遗留测试进程 |
| `./gradlew.bat assemble` | 最终增量打包退出0，`assemble.log`；compileJava/processResources均UP-TO-DATE，未重复JUnit |
| `git diff --check -- src/main src/test` | 退出0，无源码/资源差异空白问题；日志既有/运行生成差异未清理 |

首次守恒断言失败没有数量记录，不能从该日志单独断定产品丢液；诊断随后明确暴露运行中注册onEachTick导致调度集合遍历异常。修复夹具注册生命周期后原断言及新增逐tick源/列/回水数量诊断连续通过；并未削弱守恒断言或以忽略错误获得通过。

JUnit关键断言：默认54与1:1、同tick重复调用不增产、冷凝无HU、模拟和零量不选模式、核余热锁、333‰千次1mB小包回收333mB、有界尾量与当前格式保存恢复、冷源阶段边界/暂停、缩容保留存量、非法参数停止、剩余1mB回水空间下333‰原子限量及满罐不耗尾量。

最终8个GameTest：普通蒸汽能力/模拟/超临界拒收/服务端View；五种顶部源、近阶段边界融水与蒸发、蓝冰持续、当前格式恢复；侧面/流水/雪层/含水块无效及堵塞恢复；成员54预算/缺顶部成员停机/混列安全/拆分份额；333‰SERVER实际转换与View；非默认容量/首份模拟/共享额度/缩容空位；实际Create源罐→泵管→三台冷凝→泵管→回水罐，4000mB逐tick守恒且全部变水；代表性真实Create小锅炉核热18级/36mB/t回归。

冷源GameTest采用当前保存格式把进度设到剩余7mB，直接验证阶段截断与融化后新的水阶段；未把默认100000mB全寿命重复跑约1852tick，默认值由实际SERVER加载断言核对。

同namespace的三个模板分别复制既有已验证p0_probe_empty、chain_empty、boiler_empty。未发生Missing template。未clean、未--rerun-tasks、未全项目测试、未研究旧档。重复只由具体失败与新增共享容量风险边界触发；最终通过后仅assemble。

## 实际写集

源码：
- `src/main/java/com/iksxh/create_nuclear_industry/config/HeatExchangerConfig.java`
- `src/main/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerState.java`
- `src/main/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerLine.java`
- `src/main/java/com/iksxh/create_nuclear_industry/heat/NuclearHeatExchangerBlockEntity.java`
- 新类型`src/main/java/com/iksxh/create_nuclear_industry/heat/CondensationState.java`
- 新类型`src/main/java/com/iksxh/create_nuclear_industry/heat/HeatExchangerMode.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionCondensationGameTests.java`
- `src/test/java/com/iksxh/create_nuclear_industry/heat/CondensationStateTest.java`

资源：`src/main/resources/assets/create_nuclear_industry/lang/{zh_cn,en_us}.json`、`src/main/resources/data/create_nuclear_industry/tags/block/condensation_cold_sources.json`、`src/main/resources/data/create_nuclear_industry_condensation/structure/{p0_probe_empty,chain_empty,boiler_empty}.nbt`。

自动产物与报告：本报告目录、`build/runtime-condense-01-*`测试运行目录、常规Gradle build输出。现有logs/debug.log、logs/latest.log保留（JUnit加载器刷新运行日志），tools/art-assets/__pycache__/保留。没有编辑用户世界/客户端进程/开发run配置。PM同期docs/server-config.md变更与PM提交不属于执行者写集。

## 制品与剩余门

- `build/libs/create_nuclear_industry-0.1.0.jar`
- SHA256：`ECEB5EDE6415859A953390040A2696555CD1DBE57E102CA8B1C9EAA25A521CAB`
- 已用jar tf核对新状态类型、冷源标签、三个namespace模板实际打包；中英文新增9键及冷源JSON已解析核对。

未测边界：完整锅炉→汽轮机→冷凝→给水整回路人工联调、客户端视觉及护目镜实际渲染、默认冷源全寿命墙钟运行、多区块卸载重新加载的冷凝整列。当前格式账本/冷源进度的保存载入已自动覆盖，未重启整个服务端重开保存世界；既有生命周期检查保留，不新增旧档兼容门。17台冷凝拒绝和无冷源方块加载由源码防御检查，未为它们另扩全量测试。

交付停在PM审查及已列人工门；未合main、未提交功能、未派发其他执行者、未转到后续玩法。
