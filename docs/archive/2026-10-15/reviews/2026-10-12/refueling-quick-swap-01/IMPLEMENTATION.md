# REFUEL-QUICK-SWAP-01 执行报告

2026-10-12，执行者交付状态：DONE。功能写集未提交，等待主PM安排独立规格/质量审查与本功能人工观察；不关闭可变尺寸、ART08/09或快捷换料人工门，不合main。

## 范围与实际技能

唯一实现树：`E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，`codex/reactor-variable-size`。任务三源基线为 `31554094f6b8c2ae3726d4c60fd83c0872eb80a7`；最终只读核实HEAD为PM独立美术提交 `26f4ba25ff71085009bf799ded418e9b796c141d`。美术提交未暂存本功能源码。

已实际读取根AGENTS、活动卡及 Minecraft modding/testing、test-driven-development；按实际失败读取并应用 systematic-debugging，交付前应用 verification-before-completion。技能入口分别为 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md` 及 `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/` 下对应SKILL。锁定 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82，与实际gradle.properties一致；未升级栈或调整许可证。

仅实施三个原生产Java、原纯事务测试、新独立GameTest、中英文两消息键及PM追加授权的单个空模板副本。未改生命周期/热工/配置/配方/Ponder、未写美术源或生成素材、未读写用户世界、未执行Git写操作或派发子代理。AGENTS、核心文档与其他无关dirty保持；Gradle生成的既有logs未清理或回退。

## 行为与边界

- 纯事务新增 `replaceExhausted(current, incoming, currentFissionHeatHu, exactStoredFuel)`、REPLACED与TARGET_NOT_EXHAUSTED。正式新燃料仍有耐久即可；只消费一件，失败输入及原列保持且输出为空。停止放热不等于枯竭，剩一耐久仍拒绝。
- 精确旧栈与本次列投影必须一致。正式冷却乏燃料保持全部数据组件；尚未转换的耗尽新燃料沿用原extract生成冷却乏燃料的规则。本事务不发布中间空槽。
- 世界新增 `tryReplaceExhaustedFuel`：端口完整栈是物品权威，复用既有currentFuelColumn构造瞬态投影，使用现有带投影的服务端HU/t热校验，避免正常tick刚转乏燃料时被旧运行投影误拒绝。提交前复用实际端口/绑定/精确栈二次校验，复用原端口与完整快照双状态异常回滚；未改原insert/extract或机械臂入口。
- 提交仅替换目标组件、清零上一组件燃耗小数余量；原列完整度、总余热、安全热余数、全堆尺寸、其他列、冷热库存、SCRAM均保持。没有相邻控制棒时新组件继续按原规则放热。
- 玩家只有正式新燃料且端口非空时走替换，成功后才写输入余量和返还。单件消费后回实际主/副手；多件入背包，满包只掉落一件旧组件。创造模式也消费一件，未额外复制输入。空槽、空手、错误物品、未绑定、钢板维修的分支保持。
- 新消息键仅 `refueling.replaced`、`refueling.target_not_exhausted`，中英同步且JSON解析通过。

## RED/GREEN 与必要复验

全部原始日志、退出文件、XML、冻结身份位于 `build/reports/refueling-quick-swap-01/`。下表时间为Gradle日志显示值，`.exit.txt`另保存真实elapsed。

| 原始证据 | 命令/结果 | 判断 |
| --- | --- | --- |
| 01-player-red.log / exit.txt | 独立域GameTest，exit0，零函数，15秒 | NeoForge实际按模板namespace筛选；不算RED/GREEN。PM批准复制原空模板至独立域，原模板不变 |
| 02-player-red.log / exit.txt | 原生产代码，1/1失败，exit1，23秒 | 非5尺寸结构正确成型，正式BlockState.useItemOn失败“快捷替换未把精确枯竭组件交回消耗后空出的主手（原占用槽拒绝路径）”，行为RED成立；正常保存关闭 |
| 03-transaction-green.log / exit.txt / xml | 定向JUnit，12/12，0失败/错误/跳过，exit0，20秒 | 原7项+新增5项；仅执行一次，后续复用 |
| 04-player-green.log / exit.txt | 1/1失败，exit1，19秒 | 主副手、多件、生存满包通过；测试把主手槽重复计入背包，修正统计，不改生产 |
| 05-player-green.log / exit.txt | 1/1失败，exit1，20秒 | 返还、拒绝、机械臂与空槽/空手通过；测试移除已初始化控制棒，旧权威列保留导致仍停热。只改夹具顺序：初始无棒先验放热，再新增棒；未改生命周期 |
| 06-player-green.log / exit.txt | 中间1/1通过，exit0，19秒 | 正常保存关闭；此时创造模拟玩家尚未设置原版instabuild能力，不能用于创造满包最终结论 |
| 07-combined-assemble.log / exit.txt | 中间assemble，exit0，1秒 | 后续发现实际创造满包吞物品，降为中间证据；冻结包保留为07-intermediate-combined.jar |
| 10-creative-full-red.log / exit.txt | 新增创造满包，仍1/1通过，exit0，20秒 | 不是有效RED：makeMockPlayer(CREATIVE)只重写isCreative，不设instabuild。随后显式同步并断言真实能力 |
| 11-creative-full-real-red.log / exit.txt | 原返还add代码、真实创造能力，1/1失败，exit1，19秒 | “满背包没有只掉落一件精确旧组件”；原版add吞输出的实际RED，正常保存关闭 |
| 12-player-final-green.log / exit.txt | 最终真实GameTest，1/1通过，exit0，20秒 | 创造/生存满包均inventory=0、drops=1，input=2余量=1；全部有限场景通过，正常保存与进程退出 |
| 13-final-combined-assemble.log / exit.txt | 必要替代最终assemble，exit0，1秒 | PM按治理5.1实际故障触发允许；只增量jar，无clean/完整build/强制重跑 |

实际命令：

```powershell
./gradlew.bat test --tests '*FuelRefuelingTransactionTest'
./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_quick_refuel -PgameTestDirectory=build/gametest-refueling-quick-swap-01
./gradlew.bat assemble
```

GameTest只用一个6×8×7夹具、一个required用例，目标CoreColumnPosition(3,4)超出原默认3×3坐标范围。有限场景在同一服务端tick中核对：主手单件，副手单件，主手三件入包，生存副手两件满包，真实创造主手单件、真实创造副手两件满包；新旧自定义名称与耐久保持、另一只手保持、完整快照仅允许目标组件变化；剩一耐久且停热拒绝，错误/耗尽输入拒绝，原机械臂占用拒绝/模拟只读/空槽取放，玩家副手空槽装入和空手取回，无棒新组件开始放热，缺壳未成型与直接失效绑定拒绝。夹具自身失败日志与中途已通过断言没有冒充最终整项通过。

没有本次保存停滞；最终进程成功由12日志与exit文件共同证明。编译保留仓库既有deprecation/unchecked警告与NeoForge/Create开发启动提示，无新增故障。功能写集定向diff --check通过；全树检查只见原运行logs尾随空白，未越权清理。

## 创造满包原版分支证据

`locked-inventory-and-mock-player-source.txt`摘自实际锁定 `build/moddev/artifacts/neoforge-21.1.219-sources.jar`，SHA256 `a6d08e695f0ac1066342b337d3d9d1689167e6017434820bd329fa97abc4858e`。

Inventory.java的add分支在未接收且hasInfiniteMaterials时清零输入并返回true（原文件234行起）；placeItemBackInInventory在找不到可并入槽和空槽时直接player.drop并break（原文件289～305行），不会进入创造删除分支。最终返还用该原版有空位/掉落入口。GameTestHelper.java原269～286行只覆盖isCreative/isSpectator；最终夹具显式 `mode.updatePlayerAbilities(player.getAbilities())` 并断言 `hasInfiniteMaterials()` 与模式一致，因此最终满包证据确实覆盖原版创造能力。

## 最终冻结与ART09组合

最终包：`build/libs/create_nuclear_industry-0.1.0.jar`，2,677,917字节，SHA256 `896c2c38d984680eb61452d2a6ba5757e4a08e8931a2f19335acc5e26a57d6bf`。同内容副本：`build/reports/refueling-quick-swap-01/final-refueling-and-art09-combined.jar`。

最终清单 `final-binding.json`包含以下8项源SHA，以及13个生产class的本地/JAR逐项SHA，3项语言/独立模板的源/processResources/JAR逐项SHA，2个JUnit class身份、原正式模板身份、ART09两源及8PNG与PM净来源的SHA核对。13class包括本批10class与控件3class；JUnit class不纳JAR，不混称打包类。`15-final-abi.txt`实际最终JAR已确认新增两个公开方法。

| 本批源路径（Java根src/main/java/com/iksxh/create_nuclear_industry） | SHA256 |
| --- | --- |
| block/ReactorPortBlock.java | aff24a3a289d8726abf49cda5eeb0ed8c08473536c1917521cd058b08daf5637 |
| blockentity/ReactorPortBlockEntity.java | 1911842f327c0ce3ed0a7e25c6dba2d4be1757708a6644bc290ab4f5611ac878 |
| reactor/FuelRefuelingTransaction.java | ebadc722a35429ab7a3efcdd15d269d411426d0a6523859862a6ef7d7e92ebbc |
| gametest/RefuelingQuickSwapGameTests.java | 499d23a897296ac5f2affb742c158e48c0c02772c58264d16e2f5410ad7b1484 |
| src/test/java/com/iksxh/create_nuclear_industry/reactor/FuelRefuelingTransactionTest.java | 98cf7b9b53d354f803a5a34b6eec4ae28cbafb83aca358bca59d19355fb5bce2 |
| src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json | 5e496f365d7077c6ae6f9989392a987b9988de89b848370dbe28dfa76f199497 |
| src/main/resources/assets/create_nuclear_industry/lang/en_us.json | 7c3dfa71bdb4aa64496d26197889ae442809d49ec123f92411e7b016b2e35fdf |
| src/main/resources/data/create_nuclear_industry_quick_refuel/structure/p0_probe_empty.nbt | f2f22ac0aba73d67682c2e07bc725b47fd83142b628da4dabff66b10860adb22 |

ART09由PM精确同步并独立提交：两源与8PNG最终10/10仍与 `art09/final-net-art-sources.json` 原身份一致，8PNG安装/processResources/JAR一致，控件3class本地/JAR一致；复用原87工具证据与独立审查，未重跑美术测试、未用固定尺寸美术JAR覆盖本包。两份美术交接报告不作为JAR不可变输入；PM只规范化CONSUMER报告换行的说明已在最终身份注明。

中间包SHA `beb4b12323e3a0c2d732f8700364f02812f0d42c4322c2e380810f083d11c887`与其清单已保留为07-intermediate-*，不能用于本批最终交付；原02及11两次实质RED、所有夹具定位日志均保留，未覆盖旧证据。

## 待人工与接续

交主PM独立审查同一最终diff和原始证据；执行者不自行验收、提交或集成。用户在本候选实际观察右击替换、主副手提示/返还、背包满掉落与剩余耐久拒绝，才能关闭本功能人工门；可变尺寸与ART08/09集中视觉/控件人工门仍独立保持。无已知未关闭实现疑点；自动测试不宣称客户端视觉或真实鼠标验收。
