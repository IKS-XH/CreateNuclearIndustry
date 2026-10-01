# EXT-A-MATERIAL-03 独立功能与整批审查

审查日期：2026-10-01。阶段：静态、整改及最终专项证据补审完成；完整155项回归与客户端人工门仍未完成。

工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，HEAD `909733887bedba39bea70a9a4470d8ca4c637540`，本批未提交。审查者为只读执行者，不是项目经理；唯一写入为本报告。未运行 Gradle、游戏、素材导出或 verify，未改实现、核心文档、Git，也未派发其他执行者。

## 结论

- **合同符合性：生产实现符合冻结合同，本批14项专项与实际标签重载证据通过独立补审。** 五身份、八配方、具体通用标签及旧钢板复用正确。输入保留断言已补齐并在最终版本真实机器执行中通过。任务要求的完整155项 GameTest 仍未成功完成，因此本批整体门槛不能宣告全部满足。
- **实现质量：通过本次实现与专项证据审查，无未关闭的代码整改项。** 原生数据驱动接入合理，未新增自定义机器算法、客户端状态或不必要的配方类型。机器测试确实搭建并等待真实 Create/原版方块实体执行，未以直接调用配方 apply 代替加工。唯一测试补强项 R1 已关闭。
- **整批边界：可交 PM 整理为客户端测试候选；不能据此验收或合入 main。** B 离线审查、最终源码/制品与证据包、默认 run 保持证明均已核对。PM保留完整155项回归未完成，不作豁免；用户客户端外观、JEI、实际操作与保存重进仍未执行。

## 审查发现与整改闭环

### R1 / P2：在下一阶段替换输入之前，断言所有原料仍保留

文件 `src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionSteelProcessingGameTests.java`，以本次初审版本行号为准。

- `mixerRejectsInsufficientAndWrongInputs`，255–265 行：120 tick 只检查三铁粉和零产物，没有检查煤粉仍为一件；230 tick 同样未检查煤粉以及刚投入的铁锭。265 行清空煤槽、271 行重新补煤，264 行替换铁锭，因此这些操作可能掩盖错误形态或欠料阶段提前吞料。最小补强：120 tick 增加 `coal_dust == 1`；230 tick 增加 `coal_dust == 1` 和 `Items.IRON_INGOT == 1`。
- `mixerRejectsOtherPowderAndWrongCarbon`，291–301 行：110 tick 后覆盖 redstone，220 tick 后覆盖错误碳槽，现有断言不证明被替换原料未提前丢失。最小补强：110 tick 增加煤粉一件、redstone 一件；220 tick 增加 redstone 一件。
- `mixerStopsAndResumesWithoutDuplicateOutput`，319–322 行：停机中仅检查铁粉四件和零产物，缺少碳粉仍为一件的直接断言。最小补强：90 tick 同时检查 `coal_dust == 1`。

这是冻结合同“无动力/材料不足不提前扣料、错误输入保持”的证据缺口，不是已证明存在生产吞料缺陷。交原执行者只补这些断言即可；不要求改 Create 原生实现，不要求重跑与本次修改无关的已通过同码检查。

**修订复核：** 已直接读取 A 修订后的新类，未沿用旧 review-inputs 快照。新行257、264–265、296–297、305、328分别补齐上述碳粉、铁锭、redstone与停机碳粉保留；312另补成功后煤粉耗尽。R1代码整改已落实，待该版本机器运行结果。此刻文件 SHA-256 为 `4b02efe3295e689f12c75f5211b67887fa27b68fa353a32fa523932ef347b9d1`；下面未标“新行”的测试行号仍对应初审版本。

**最终标记版源码复核：** A 随后为普通服增加 `verified(helper, scenario)`。已在内存中对初始 review-inputs 新类快照与当前源码做逐行差异比较：差异仅上述 R1 追加断言、三处更准确中文注释、原最终 succeed 调用替换为 verified，以及新增该辅助方法；没有删除断言或改变时序/投入产出。新方法535–539行先 `helper.succeed()`，再记录 `MATERIAL_03_TEST_RESULT`。锁定 `GameTestHelper.java:850–851` 的 fail(String) 直接抛出 `GameTestAssertException`，不存在断言失败后继续执行最终标记的吞异常分支。标记不修改库存或调度图。当前标记版 SHA-256 为 `60f738c3bdc06bac82f4176f3436e6c73f070a2e7589273c48379419f4439fca`；`final-test-build.log` 在 `2026-10-01T12:10:42.6555269Z` 明确 `BUILD SUCCESSFUL`、退出0。最终运行及收尾证据待A交付后补审。

**R1最终关闭：** 已核对冻结后源码哈希与上述标记版相同；`normal-server-final-latest.log:80,84,88` 分别记录欠料/铁锭/缺碳恢复、错粉/错碳恢复、动力中断恢复的断言完成标记。原发现要求的新增断言在此版本执行通过，不再保留为未决整改。

## 生产合同核对

| 合同 | 静态结论与位置 |
| --- | --- |
| 五普通物品且保留旧身份 | `BasicMaterialContent.java:22–26` 新增五个 `registerSimpleItem`；原六材料与 `ModItems.STEEL_PLATE` 未改。`ModCreativeTabs.java:56–60` 接入五物品。没有新的状态、NBT 或副作用。 |
| 三条制粉 | `recipe/crushing/{iron_dust,coal_dust,charcoal_dust}.json:1` 各一输入、一输出、100 processing_time。铁锭用 `c:ingots/iron`，煤和木炭各用精确 vanilla ID。未覆盖 Create milling 命名空间。 |
| 两条混粉 | `recipe/mixing/steel_dust_from_{coal,charcoal}.json:4–8` 各重复四项 `c:dusts/iron` 加一具体碳粉标签；100 processing_time，确定输出五钢粉，无额外副产物或加热要求。不存在无效 Ingredient count。 |
| 熔炼 | `recipe/smelting/steel_ingot_from_dust.json:4–11` 为200 tick、0.1 XP；对应 blasting 文件为100 tick、0.1 XP；均以 `c:dusts/steel` 输入、确定输出本模组一钢锭。 |
| 压片与维修身份 | `recipe/pressing/steel_plate.json:4–7` 用 `c:ingots/steel`，输出原 `create_nuclear_industry:steel_plate`，未自设时长；维修代码及钢板图未变。 |
| 精确 P1 保护例外 | `P1DataContractTest.java:135,146` 仅指定相对路径 `pressing/steel_plate.json` 且仅 `STEEL_PLATE_ID` 跳过；同文件中其他受保护身份仍检查，其他路径中的钢板仍拒绝。 |
| 标签互通 | `data/c/tags/item/dusts.json`、四 dusts 子标签、ingots 父与 steel 子标签均 `replace:false`。没有把铁锭或 Create 粉碎粗铁加入铁粉标签，未增加逆向回收、钢粒、钢块或铁粉直烧路线。 |
| 资源集成 | 五模型均 `minecraft:item/generated`，纹理路径分别命中五个新 PNG；中英文名称符合冻结合同。旧语言仅增加五键。 |

本表 recipe 路径均在 `src/main/resources/data/create_nuclear_industry/` 下，Java content 路径均在 `src/main/java/com/iksxh/create_nuclear_industry/content/` 下。

另只读检查锁定 Create `6.0.10-280` 的源 JAR：`HeatCondition.java:32–37` 对 NONE 返回 true，因此语义是“不需要加热”，并非“禁止加热”；`BasinRecipe.java:85–109,146–173` 先模拟累计每槽消耗及输出可接收，再实际逐项扣料，当前重复 Ingredient 写法支持同槽堆叠四件。上述源码依据不替代运行断言。

## 测试真实性与覆盖边界

`ExtensionSteelProcessingGameTests.java` 有14项 GameTest。三原料粉碎轮通过真实成对轮、控制器 capability 投入并检查所选配方和落地物；搅拌通过真实盆、小齿轮及 Creative Motor 动力，含4件同槽铁粉、无热、停机恢复、错误投入、欠料及九输出槽满后恢复；炉子和高炉检查时长前后输入/输出；风扇用真实熔岩气流与置物台；压片通过真实压片机加工旧钢板。测试没有自行 set 钢材产物，也没有直接调用 apply 强制完成配方。

- 无热精确4+1扣料与5产物：224–243 行，两种碳粉各独立覆盖；停机前后主要路径检查两原料。
- 输出堵塞：331–352 行，先填满所有输出槽，170 tick 核对两输入完整及无钢粉，再腾出一槽，350 tick 核对两输入耗尽与五钢粉；具备实际事务守恒断言。
- 熔炉/高炉：361–374 行，真实炉子在 duration−2 时保留钢粉，duration+3 时得到单件钢锭；XP与精确配置从加载配方检查，未宣称测试实际拾取经验。
- 风扇：377–409 行，真实 BLASTING 气流加工一件钢粉为一件钢锭；只证明风扇入口和单件数量，**不证明64件等大堆叠吞吐或风扇时长/经验等于炉子**。这属于覆盖边界，无需为本批额外扩展原生算法测试。
- 外部标签：438–463 行按 `/reload` 后运行时 `c:dusts/steel` 实际成员判断 flint 匹配，存在成员时通过真实炉子产钢锭。默认不含 flint 时该项只能证明“不匹配”；必须结合 A 的实际加包、reload、执行、移除、reload、恢复证据，不能把默认绿灯当作完成重载测试。该夹具不代表第三方模组联用认证。
- 机器由测试直接填入库存/能力，不能据此声称覆盖玩家操作、所有自动输入输出设备、保存退出重进、JEI显示或客户端视觉。

## GameTest 框架异常专项核对

`green-retry-gametest.log:275–284` 在 `defaultBatch:2` 起始后出现 `Object2LongOpenHashMap$MapIterator.nextEntry` 的 wrapped NPE，首次 GREEN 亦同类异常。没有完整155项通过记录；不能写为完整 GREEN 或成功退出。

已直接读取当前 NeoForge 缓存源包 `sourcesAndCompiledWithNeoForge_4a83a73d95fdbeba9a47b7d6bbaabb6346ecca49_output.jar`：

- `GameTestInfo.java:120–140` 在 `startTest()` 返回之后才取得 `runAtTickTimeMap` iterator，133 行执行回调，138 行 iterator.remove；169–170 行 `setRunAtTickTime` 会向该 map put。
- `GameTestHelper.java:804–809` 的 `runAfterDelay` 调到该 put；若在延迟回调内再注册任务，确有迭代期间修改同一 map 的风险。
- 当前本批新类的所有 `runAfterDelay` 都在测试首次调用阶段注册。逐一检查 lambda、`powerMixer`、`putMixInputs` 等被调用辅助方法，没有在延迟回调内再次调用 runAfterDelay/onEachTick/succeedWhen 或其他调度 API。真实方块操作不持有这个 GameTestInfo 的调度 map。

因此**没有找到本批新增回调嵌套注册导致该异常的代码证据**。这不是已定位框架根因，也不是已证明本批绝无关联；仅凭历史同栈不能完成因果排除。普通服新14项与实际 reload 的后续结果仍需结合报告核对，不能用它们冒充全量155项回归。

## 初审时已核对的证据

1. RED：`red-gametest.log:156–190,284–309` 明确13个新路线测试因身份/配方缺失等预期原因失败；原煤/木炭 milling 检查不依赖新增身份。RED不是最终成功证据。
2. `test-build.log` 明确 `BUILD SUCCESSFUL`，`EXIT_CODE=0`。独立只读汇总 `build/test-results/test/TEST-*.xml`：265 tests、0 failures、0 errors。未由审查者重复运行。
3. 独立读取当前 `build/libs/create_nuclear_industry-0.1.0.jar`：60张游戏纹理，逐条与当前资源 PNG 比较 SHA-256，差异0。
4. 独立读取 B 开工55图记录并对当前路径重新算 SHA-256，差异0；其中含原八冷却剂路径与旧钢板。B报告及 `EXT-ART-05-REVIEW.md` 的离线结果与本次核对相容。素材细节沿独立 B 审查，此处不重复宣称实际看图。
5. A 的隔离 init 同时设置 NeoForge run 模型和 JavaExec gameDirectory，且检查 canonical 路径必须处于03报告目录，不得处于默认run；GREEN retry 日志75–76行打印预期独立目录。
6. 初审时 A 主报告仍为准备阶段文本，最终报告尚未更新；默认run收尾清单、普通服重载证据及精确退出状态仍待补审。`logs/debug.log`、`logs/latest.log` 有运行生成差异，按卡由PM用已备份原副本处理，审查者未更改。

## 最终冻结交付补审

已读取 A 最终 `EXT-A-MATERIAL-03.md`、`artifact-verification.json`、最终普通服日志与命令记录、前后清单及 ZIP；没有再次运行自动测试。报告明确区分普通服专项、完整155项框架中断、素材离线和客户端人工门，未将14项专项写成全量成功。两次早先普通服没有完成标记，未被计入成功。

| 最终证据 | 独立核对 |
| --- | --- |
| 冻结测试源码 | SHA-256 `60f738c3bdc06bac82f4176f3436e6c73f070a2e7589273c48379419f4439fca`，与最终报告一致；R1与verified语义见上。 |
| 最终构建 | `final-test-build.log` 在12:10:42 UTC完成，退出0；当前52份JUnit XML共265 tests、0 failures、0 errors，记录0 skipped。 |
| 本批14项 | `normal-server-final-latest.log:59–113` 含14个默认场景成功标记；涵盖三路真实粉碎、两碳粉无热混粉、欠料/错料、动力恢复、堵塞恢复、熔炉/高炉、单件风扇、旧钢板压片及默认外部标签排除。没有本批失败标记。 |
| 标签重载 | 20:16:26 external=false；实际启用包及20:17:18 reload后，20:17:26 external=true，20:17:36真实炉产一钢锭；停用包及20:18:12 reload后，20:18:19 external=false。共有16个完成标记、15个不同scenario，对应14个方法加外部标签启用/恢复两次复测，并非16项新测试。 |
| 普通服退出 | 最终Java PID2988；20:19:47全部维度保存完成，`normal-server-final-command.txt` 与 console 同时记录12:19:48 UTC退出0。成功断言与成功退出均有证据。 |
| 默认世界隔离 | `default-run-before.tsv` 与最终 `default-run-after.tsv` 字节完全相同；此前亦独立按路径/长度/SHA-256/UTC修改时间规范化比对179/179，差异0。测试数据包留在03报告下隔离世界中且已停用，datapack list显示仅available，不在默认run内；不要求物理删除夹具。 |
| 最终JAR | SHA-256 `d23732abbff58c2742671bf7a57a541d1ec930deab791815ef14ff10b895b33b`；独立打开ZIP逐字节比较全部264项assets/data与当前src/main/resources的264个文件，缺失0、差异0，含60PNG。JAR内新测试class亦与build/classes/java/main对应class字节一致。 |
| 证据ZIP | `evidence.zip` 为124269字节、80项，SHA-256 `0b73e3cc04a845bf080b24f0350b97a7d0e408c15ffbc264f16d92b97b83ccd1`，与sha256文本及PM给定值一致。目录含52份JUnit XML、原始RED/GREEN/final构建日志、最终普通服日志与退出记录、隔离init、外部包模板、run前后清单及artifact记录。独立比对包内最终latest日志与artifact JSON均等于对应冻结文件；没有把隔离世界、缓存或JAR夹入。 |

证据准确性限制也已核对：`normal-server-final-commands.txt` 明确是实际命令及服务端时间整理，不是原始PTY转录；PowerShell transcript未完整捕获子进程输出，因此成功、重载和保存均以最终服务端日志为依据。PM已恢复运行引起的根目录两份跟踪日志；审查者最终只读比较两文件与各自本轮before副本SHA-256，两项均相同，`git diff --name-only` 无未暂存差异。本审查未自行修改日志或执行Git写操作。

## 技能与剩余门槛

实际读取并应用：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`（原生身份、数据路径、具体标签与服务端权威）；`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`（真实世界断言、RED/GREEN证据区分及人工门）。读取主工程最新任务卡、批准方案、AGENTS与治理，并对候选 `gradle.properties`、`build.gradle:13` 核对 MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6；未套用技能新版本例子或升级依赖。

本次独立审查及R1整改闭环完成。仍未完成的门槛是完整155项GameTest和用户客户端人工验收；本批14项专项不替代全量回归，PM未豁免该门。可以保留未提交/候选分支交付并由PM整理客户端测试候选，不批准合入main，不推进后续批次。审查者没有重新运行已通过同码测试，也没有承担项目经理验收权限。
