# EXT-A-MATERIAL-04 独立功能规格与质量审查

**审查身份：** 执行者，只读审查；不作项目经理验收。
**基线与工作树：** `9295e48`，`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。
**范围：** 本批功能注册、创造页、五模型与中英文、三配方二标签、`ExtensionSensorProcessingGameTests`及其运行证据。PM 文档、美术独立交付、历史未改代码不列为本次功能缺陷。

## 结论与问题分级

可交项目经理继续核对最终交付证据，具备进入客户端人工候选检查的功能基础；**此结论不批准合入 main 或整批验收**。本次审查未发现可复现的 Critical、Important 或 Minor 功能代码缺陷，不为历史或正在收尾的运行材料捏造问题。仍需功能执行者正式交付报告、最终制品清单与进程/退出说明，项目经理再行终审。

| 等级 | 发现 | 影响与整改建议 |
| --- | --- | --- |
| Critical | 未发现 | 无。 |
| Important | 未发现 | 无。 |
| Minor | 未发现 | 无。 |

## 合同与源码核对

- `BasicMaterialContent.java:28-36` 注册三项普通可堆叠成品与两项 Create `SequencedAssemblyItem`；`ModCreativeTabs.java:61-63` 仅加入三项成品。锁定的 Create `6.0.10-280` 源码中 `SequencedAssemblyItem` 构造器使用 `stacksTo(1)`，进度取 `AllDataComponents.SEQUENCED_ASSEMBLY`。未见本批新增主动传感器行为、逆向拆解、机器协议、NBT 或全局配置覆盖。
- `stonecutting/tin_wire.json` 为 `c:ingots/tin` → 2 锡条的单一切石配方；未发现同输入的新增 cutting 配方。锁定 Create 源码的 `SawBlockEntity.getRecipes()` 在 `allowStonecuttingOnSaw` 为真时才纳入 `RecipeType.STONECUTTING`，`CRecipes` 默认值为 true；`SawBlockEntity.applyRecipe()` 对每个输入取原切石结果，故原生机械锯路径与本批设计吻合。
- `sequenced_assembly/industrial_sensor.json` 以 `c:plates/iron` 为基底，按 `c:wires/tin`、红石、电子管、压片共四步；`radiation_sensor.json` 以 `c:plates/lead` 为基底，按工业传感器、电子管、压片共三步。均仅一轮、唯一单件结果，无加热、概率副产物或自定速度字段。锁定 Create `SequencedAssemblyRecipe.advance()` 每步产生带配方 ID、步数和进度的原生半成品，末步 `rollResult()`；单一结果池的 `getOutputChance()` 为 1。
- `c:wires` 与 `c:wires/tin` 两文件均为 `replace:false` 的追加标签；没有把锡条写入其他通用材料标签。五个模型均指向本批同名纹理；中英文五项显示名与合同逐项一致。

## 测试入口与实际证据

- 新 GameTest 使用仓库已有 `@GameTestHolder`、`@PrefixGameTestTemplate(false)` 与 `p0_probe_empty` 入口。`realStonecutterMenuTakesTwoWires()` 走原版 `StonecutterMenu` 选择并取料；`realSawRespectsConfigAndBlockedOutput()` 用带轴动力的实际机械锯、输入库存和被占用的输出置物台。默认开关下先保留 2 锡条，清障后转交 2 条；独立普通服务器在开关关闭时记录 `real-saw-stonecutting-disabled` 通过，配置文件现已恢复 true。
- 两条完整序列的测试由实际 Deployer 和 Mechanical Press 驱动，分步检查手中投入扣为零、半成品原生组件的步数/进度和终产 1 件。工业路线还覆盖先投入红石的错序拒绝、第二步电子管错物品、暂缺正确材料和断动力后恢复。置物台直接放入底板属于夹具准备；没有直接造终态或以反射跳过机器。`incompleteStackKeepsNativeProgressWhenSerialized()` 只证明 ItemStack 组件编码往返，不等同玩家存档重进。
- 外部隔离数据包将 `minecraft:flint` 追加到 `c:wires/tin`。普通服务器日志显示 disable/reload 的 false、enable/reload 的 true 与真实 Deployer 首步成功、再 disable/reload 的 false；临时包未进入正式资源。隔离初始化脚本同时锁定 NeoForge run 模型与 JavaExec 目录，`directory-check.log` 显示两者均指向本批报告目录。
- RED 日志由锡条未注册触发，156 项 required 中 1 项业务失败。探针依次有 163、165、166 项 required 通过。最终 `final-test-build.log` 为 `test build --rerun-tasks --max-workers=1` 成功；本轮 52 份 JUnit XML 合计 265 测试、0 failure、0 error、0 skipped。最终 `final-gametest.log` 为 **167 项 required 全通过**，随后停在 `Saving worlds`，运行进程以 -1 退出，Gradle 任务失败；这不是测试断言失败，也不能称该 Gradle 命令正常退出 0。先前一次夹具 100 tick 超时已改为 180 tick 且由后续真实重跑通过，不能当作当前生产配方失败。
- 只读逐字节核对当前 `src/main/resources/assets` 与 `data` 共 279 文件、65 PNG，最终 JAR 对应条目缺失/不一致数为 0。默认 `run` 目录前后均 180 文件；按快照的路径、长度、UTC 修改时刻和 SHA-256 比较，差异为 0。当前仅有开工前记录的 Java PID 21468，未见本批遗留 Java 进程。

## 证据边界与后续门

- `build/reports/extension/EXT-A-MATERIAL-04.md` 在本报告写入时尚未出现；运行命令原文、精确终止 PID、退出记录、精简 ZIP、最终制品哈希清单须按执行者最终报告补核。当前逐字节 JAR 核对是本审查现场只读结果，不替代执行者的交付清单。
- 机械手测试使用创意马达与置物台离散步进夹具，证明真实单设备按原生序列推进及恢复；不宣称真实整线物流有原子事务。客户端 JEI 顺序与数量、五项外观、半成品保存重进及玩家操作下断电/堵塞体验，仍需用户在候选客户端人工验证。

**实际使用技能：** `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 用于 NeoForge/Create 注册与配方版本核对；`minecraft-testing/SKILL.md` 用于真实 GameTest 入口、断言与运行证据边界；`minecraft-resource-pack/SKILL.md` 用于 1.21.1 单数资源路径、模型/纹理和语言核对。技能中其他版本示例未用于本批。

## 最终交付限定补审（2026-10-01）

初审列出的交付余项现已补核。`build/reports/extension/EXT-A-MATERIAL-04.md` 逐项记录实际命令和退出码：JUnit/构建退出0，最终 GameTest 167/167 required 断言通过后在 `Saving worlds` 停滞；`final-gametest-stop.txt` 记录只停止本轮 PID 32544（创建于 22:56:20、父 PID 21468、命令指向候选的 `gameTestServerRunProgramArgs.txt`），Gradle 因该进程被停止而退出1。PID 32544 当前不存在，`java-after.json` 只记录原有 Gradle 守护进程 21468。

`reload-commands.txt` 与对应普通服务器原始日志相互吻合：外部 flint 包 disable/reload → false，enable/reload → true 且真实 Deployer 消耗，随后 disable/reload → false；服务器 stop 退出0。关闭机械锯切石配置的独立普通服务器试验亦通过，隔离配置现为 true。

精简 `evidence-small.zip` 有78条目，含52份原始 JUnit XML、必要日志、PID/重载命令、资源哈希和脚本；列项无世界、缓存或 JAR。`artifact-verification.json` 记录源与 JAR 各279资源、65 PNG、279逐项一致、无问题，脚本还核对五项中英文共10名；现场对最终 JAR 重算 SHA-256 为 `5D47E144FA0909C73B513D8B21BB88589696E4536F9C3F6EE98A567A699791AD`，与报告一致。默认 `run` 两份180项 CSV 的 SHA-256 完全一致，均为 `EE6C639CEB55512C52B8B12D9850B790CD80CE53BC6B44DE12BA99EF6633136B`。

因此初审所列“正式报告/命令/PID/ZIP/制品哈希待补核”已关闭。**功能审查结论：可进入候选客户端人工验收；人工门及 main 整合仍未通过。** 根跟踪日志的 Git 恢复由项目经理负责，本执行者未作 Git 写操作。
