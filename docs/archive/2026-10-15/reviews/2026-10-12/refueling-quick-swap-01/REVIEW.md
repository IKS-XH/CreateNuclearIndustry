# REFUEL-QUICK-SWAP-01 独立规格与代码质量窄审

2026-10-12。结论：本次最终差异的规格与代码质量审查通过，无必改项；可交主PM登记候选并进入本功能人工观察。此结论不代表用户手测通过，不关闭可变尺寸、ART08/09或快捷换料人工门，也不是源码合入main的授权。

## 范围、方法与权限

审查树为 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`，实际HEAD为 `26f4ba25ff71085009bf799ded418e9b796c141d`，功能基线为 `31554094f6b8c2ae3726d4c60fd83c0872eb80a7`。已读根AGENTS、活动计划、`.superpowers/sdd/refueling-quick-swap-01/task-1-brief.md`、完整 `review-package.md`、IMPLEMENTATION及final-binding。

实际应用技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`、`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/verification-before-completion/SKILL.md`。核对gradle.properties、build.gradle及真实启动日志，版本为Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82；未采用技能中的更新版本示例。

本次只读审查六个tracked差异、完整新增GameTest及单个独立域空模板，按一次合并规格/质量审查处理。另读既有Codec、列状态、Snapshot、仪表提交校验及锁定原版Inventory/GameTestHelper源码，确认调用链。实际以 `git diff --unified=8` 比较审查包六路径差异，逐字一致；新增GameTest正文与工作文件一致（仅规范化换行后比较）。定向 `git diff --check` 退出0，无差异空白错误。无Git写、无代码/核心文档修改、无子代理派发、无世界访问。本审查只新增本报告。

按项目治理5.1及任务卡复用原始测试/构建证据，未重跑Gradle；下述测试结果均为独立读取原日志/XML/退出文件所得，不声称本审查重新执行了测试。新鲜检查仅包含只读差异、冻结SHA及JAR内容核对。

## 合同与代码核对

| 审查点 | 核对结果 |
| --- | --- |
| 枯竭与停热 | replaceExhausted明确先检查当前HU/t，再独立要求exhausted；剩一耐久即使停热也返回TARGET_NOT_EXHAUSTED。只接受正式新燃料且有可用耐久，不强制零损伤；一件消费且输入原栈不变。 |
| 精确物品权威 | 旧栈必须为合法单件并与本次列投影一致；失败没有输出。世界入口先取端口权威栈构造瞬态投影，以既有带投影的热计算检查，允许端口刚转乏燃料而运行投影尚未刷新。原Snapshot的结构状态仍保留。 |
| 原子提交与失败 | 成功才进入原validateFuelColumnCommit，再检查世界中的实际端口实体、所有者/列绑定和完整ItemStack匹配。直接写新栈和完整Snapshot，不调用公开extract/insert、不发布空槽。复用原双状态RuntimeException回滚；失败结果由玩家入口消费前检查，输入/旧组件仅在成功后转移。 |
| 数据与耗尽转换 | 正式冷却乏燃料的全部数据组件逐件复制保留；尚未转换的耗尽新燃料沿用原extract创建冷却乏燃料的规则。未擅自改写原提取语义。输入耐久及数据组件随完整单件栈写入端口。 |
| 主副手、背包与创造 | 成功后先写交互手余量；空手直接回旧栈，否则使用placeItemBackInInventory入包/掉落。创造也消费一件。返还路径避开Inventory.add在创造满包清零输出的分支，生存/创造满包均有真实能力场景证明单件掉落。 |
| 状态隔离 | withFuelAssembly仅重置上一组件的燃耗小数余量，保持余热、完整度和安全热余数；withFuelColumn保留实际尺寸、其他列、冷热库存、SCRAM及其他全堆状态。6×8×7夹具的目标(3,4)在原默认范围外；完整Snapshot比较和无棒放热断言支持该结论。 |
| 旧路径 | 玩家仅正式新燃料且非空端口走新增替换；钢板维修、空槽、空手、未绑定入口未改。机械臂仍调用原insert/extract/simulate，真实用例检查占用拒绝、模拟只读和空槽取放，未增加自动替换。 |
| 范围与质量 | 三个生产Java、原JUnit、新GameTest、两语言键及模板在授权范围；新增/修改解释性注释为中文。未新增GUI、配置、配方、生命周期/热工实现或无关重构。 |

## 原始证据核对

所有下列文件位于 `build/reports/refueling-quick-swap-01/`。

- `02-player-red.log`与exit文件：原占用拒绝路径运行1个真实required测试，在正式BlockState交互入口失败“快捷替换未把精确枯竭组件交回消耗后空出的主手”，exit1；不是缺方法编译失败。零函数的01日志不计RED。
- `03-transaction-green.xml`明确12 tests、0 failures/errors/skipped；日志BUILD SUCCESSFUL，exit0，20秒。五个新增替换测试及七个原取放测试均列出，后续玩家返还修复不涉及这些事务源码，复用合理。
- `11-creative-full-real-red.log`与exit文件：显式同步真实创造能力后，创造满包在“满背包没有只掉落一件精确旧组件”处失败，1/1失败、exit1。此前10的假创造能力通过不作为有效RED；07包保持中间身份。
- `12-player-final-green.log`与exit文件：1个required测试完成且All 1 required tests passed，exit0，20秒。日志逐项记录副手单件、主手三件入包、生存副手两件满包、创造主手单件、创造副手两件满包；后两种满包分别为inventory=0、drops=1。初始主手单件、拒绝/机械臂和未成型检查在同一有限场景中完成。代码明确断言另一只手、实际输入余量、输出精确数据和完整Snapshot，而不是仅凭日志文案作判断。
- 最终GameTest日志记录各维度All chunks saved、All dimensions saved、Game test server shutting down及BUILD SUCCESSFUL；不存在仅业务断言通过而保存挂起的冒称。
- `13-final-combined-assemble.log`与exit文件：最终必要替代增量assemble退出0、1秒，仅jar实际执行。计划收尾明确授权真实故障触发的必要再assemble，07未冒作最终交付。未安排完整build、clean或无关GameTest。
- `15-final-abi.txt`含replaceExhausted和tryReplaceExhaustedFuel正式公开签名，与源合同一致。

已从实际 `build/moddev/artifacts/neoforge-21.1.219-sources.jar` 独立读出Inventory.java与GameTestHelper.java，相同源码包SHA为 `a6d08e695f0ac1066342b337d3d9d1689167e6017434820bd329fa97abc4858e`。Inventory的248/265行创造分支清零输出；placeItemBackInInventory的291～298行先找合并/空槽，找不到则drop。GameTestHelper.makeMockPlayer只覆盖isCreative/isSpectator，最终夹具显式updatePlayerAbilities并断言hasInfiniteMaterials，故12确实覆盖原版创造满包分支。

## 冻结身份及证据边界

以final-binding.json逐项重新计算实际文件SHA并打开最终JAR核对：8/8功能源、13/13本地生产class与JAR、3/3语言/模板源与processed/JAR、2/2JUnit class、原模板以及ART09的10/10净输入全部匹配，零身份不一致。新增模板与原正式模板均109字节、SHA `f2f22ac0aba73d67682c2e07bc725b47fd83142b628da4dabff66b10860adb22`。

最终 `build/libs/create_nuclear_industry-0.1.0.jar` 为2,677,917字节，SHA `896c2c38d984680eb61452d2a6ba5757e4a08e8931a2f19335acc5e26a57d6bf`；冻结副本final-refueling-and-art09-combined.jar相同。13个打包类包含本批10类与ART09控件3类，JUnit class不混称打包类。ART09源码/资源与本功能无写集交叉，仅核组合身份，原独立审查及素材证据复用，不重审美术行为。

必改项：无。未发现阻止本次候选交付的规格或质量问题，也未发现测试/构建结果和最终冻结身份之间的缺口。

验证边界：真实用例是一个有限GameTest，使用模拟Player直接调用服务端BlockState入口，不能扩写为真实客户端鼠标、动作栏/网络回显或所有尺寸排列均通过。提交前精确栈二次校验与RuntimeException回滚沿用旧代码，本批未做并发篡改或异常注入运行测试；本次只据实际调用链确认复用，不宣称新增此类动态证明。钢板维修分支逐字保持，未重跑维修专项。上述均不构成本卡必改项；用户右击、主副手提示/返还、满包掉落及剩余耐久拒绝仍待人工观察，尺寸与ART08/09独立人工门保持。
