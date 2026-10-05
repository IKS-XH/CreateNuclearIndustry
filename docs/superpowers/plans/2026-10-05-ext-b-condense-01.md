# EXT-B-CONDENSE-01：蒸汽冷凝回水实现计划

> PM使用writing-plans和subagent-driven-development组织实现与一次规格/质量联合审查。执行者按下方步骤实施，不派发其他代理、不改核心文档、不执行Git写操作。治理5.1与用户精简验证要求优先于技能的固定重复流程。

**状态：实现、定向验证及独立审查通过，待人工闭环联调。** 2026-10-05用户确认[完整方案](./2026-10-05-steam-condensation-proposal.md)，包括顶部冷源、消耗量、蓝冰持续冷源及首轮吞吐；功能提交`fdfba38`，交付见[本批记录](../../reviews/2026-10-05/condense-01/README.md)。

**目标：** 复用换热器把汽轮机排出的蒸汽冷凝成水，接通锅炉给水闭环，并结算顶部冷源的融化和蒸发。

**架构：** 现有每机库存与HU账本增加工质模式，直列先核对同一工质，再原子流转输入与输出。各台分别检查顶格并结算本机冷凝额度与冷源消耗；冷凝彻底隔离锅炉供热入口。能力模拟保持纯查询，权威结算和世界方块变化只在服务端执行。

**技术栈：** MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；不升级依赖。

**规格：** 上述完整方案、AGENTS.md、治理5.1/5.2、现有换热器直列已验收合同。只读参考`build/reports/extension/EXT-B-CONDENSE-01/feasibility.md`，其中旧36mB/t和邻近冷源建议已被规格取代。

## 全局约束与写集

- 工作目录`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`，派发前HEAD `57156c5`；该HEAD之后仅本批PM文档会先提交，执行者在报告记录实际起点。现存`logs/debug.log`、`logs/latest.log`及`tools/art-assets/__pycache__/`保留。
- 必须实际读取`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`与`minecraft-testing/SKILL.md`，核对锁定版本源码与现有测试入口；需要时使用systematic-debugging/TDD，不能按示例升级技术栈或新增测试框架。
- 中文注释与Javadoc明确服务端/客户端界限、模式不变量、mB及mB/t单位、事务模拟、逐台冷源预算、tick顺序和NBT所有权。
- 无GUI，不改配方/模型/汽轮机性能；不实现超临界蒸汽降级供热、工作盆加热或事故；不研究旧存档/旧配置迁移，不动用户世界及客户端进程。
- 允许写现有`heat/HeatExchangerState.java`、`HeatExchangerLine.java`、`NuclearHeatExchangerBlockEntity.java`、`HeatExchangerBoilerBridge.java`、`NuclearHeatExchangerBlock.java`。必要的新模式/冷凝状态类型只放`heat/`目录，本批名称须在报告列明。
- 允许写`config/HeatExchangerConfig.java`；新定向测试`gametest/ExtensionCondensationGameTests.java`及同目录`gametest/condensation/`；现有`gametest/ExtensionHeatExchanger*GameTests.java`仅适配本批相关接口或精确复现，不削弱旧断言；`src/test/java/com/iksxh/create_nuclear_industry/heat/`。
- 允许新建`src/main/resources/data/create_nuclear_industry/tags/block/condensation_cold_sources.json`，以及`data/create_nuclear_industry_condensation/structure/`本批模板；使用现有已验证的独立GameTest命名空间/模板，不改构建脚本。
- 允许语言`assets/create_nuclear_industry/lang/zh_cn.json`、`en_us.json`中的换热器提示；若发现语言生成源确实包含对应旧提示，先报告路径，由PM精确扩写集，不重新生成美术资产。
- 报告及临时隔离配置只写`build/reports/extension/EXT-B-CONDENSE-01/`，测试运行目录`build/runtime-condense-01-*`。禁止docs、注册入口、其他设备、全局mixins、构建脚本及任何Git写入；需要扩大范围先报告PM。
- 必要时可备份后定点追加开发实例`run/config/create_nuclear_industry-heat-exchanger.toml`新增字段，使候选runClient使用已批准默认值，保留无关参数。仅报告用户世界同名SERVER覆盖，不编辑世界。

## 冻结行为合同

1. 普通蒸汽为`TurbineContent.STEAM`，输出`minecraft:water`；超临界蒸汽`BoilerContent.SUPERCRITICAL_STEAM`不属于本批输入。核热仍收热复合冷却剂、出冷复合冷却剂。不能让数字库存被重新解释为另一种流体。
2. 首份成功实际输入选择模式；SIMULATE、不接收任何量及读取能力均不能选模式。直列非空成员必须同模式；空罐但仍有核余热时不能切成冷凝。有异种存量的直列整体暂停且保留真实份额，拆开后恢复；能力也不能混报或抽走另一种工质。
3. 保持同向首尾相连最多16台，背橙输入/前蓝输出，仅列尾/列首外端接管；左右与底部不增接口。每台冷凝蒸汽/水罐各4000mB，逐台相加共享库存，各台只结算自己的54mB/t额度；同tick重复调用不能增产。
4. 默认1mB蒸汽→1mB水，配置回收率1～1000千分比。有界小数尾量属单机；模拟、拆分、重组、存取不增加尾量或复制产物。出水空间不足时原子限制转换，不允许先扣汽再发现放不下。默认闭环回水守恒。
5. 每台只检查正上方相邻一格，不触发区块加载；冷源标签默认原版water/snow_block/ice/packed_ice/blue_ice。水仅真正水源，流水与含水方块无效；雪层无效。无冷源、输入空、出水满、模式冲突安全暂停并显示原因。
6. 按该台实际成功冷凝的输入mB累计：雪块与冰100000mB后顶格变水源；浮冰900000mB后变水源；水源100000mB后变空气；蓝冰不消耗。达到边界只能转换当前阶段剩余预算，下一次重新检查。融化后新水阶段从零累计，外部冷源改变需重新核对当前类型；默认满流量水阶段约93秒。
7. 冷源融化/蒸发不增减机内回水库存；暂停、SIMULATE、填充和抽取不消耗冷源。只修改顶格，水流更新沿用原版；水补充后按新的水源阶段处理。冷源进度随当前版本正常保存加载，不因重进世界刷新寿命。
8. 冷凝模式不发布Create锅炉热、不响应专用高压锅炉HU领取，也不产生应力；原核热余热、直列流体守恒和两种锅炉供热保留现行行为。
9. 8项新增数值使用SERVER配置：`condensationRateMbPerTick=54`、`condensationRecoveryPermille=1000`、`condensationSteamCapacityMb=4000`、`condensationWaterCapacityMb=4000`、`condensationSnowMeltAfterMb=100000`、`condensationIceMeltAfterMb=100000`、`condensationPackedIceMeltAfterMb=900000`、`condensationWaterEvaporateAfterMb=100000`。检查范围与蓝冰规则固定，不引入扫描半径。配置调低容量不能截断存量，非法数值安全停止。
10. 护目镜沿用已经修复的Create标题缩进，显示模式、蒸汽/水容量、实际冷凝量及暂停原因，冷凝页面不冒充热等级；中文英文完整，服务器同步不能让客户端常显0或代码键。

## Task 1：同一换热器接通冷凝闭环

**接口入口：** 保留`NuclearHeatExchangerBlockEntity.serverTick(...)`、流体能力`Port`、`HeatExchangerLine.find(...)`及`HeatExchangerState.save/load`的既有调用契约；由模式控制流体报告和转换。`HeatExchangerConfig`追加冷凝配置快照；新增内部接口由同一执行者一次完成，避免跨代理签名争用。

- [x] 先用一个代表性断言记录旧实现拒收普通蒸汽或未冷凝的失败；不为每个配置字段机械重复红绿流程。
- [x] 实现模式/库存/直列事务、冷凝小数尾量、逐台冷源预算、服务端方块变换及正常保存恢复。
- [x] 接入流体能力、核热隔离、SERVER配置及护目镜同步，完整核对八项配置与中英文键。
- [x] 定向JUnit仅覆盖受影响`HeatExchangerStateTest`及新冷凝状态测试：54mB/t与1:1守恒、模拟纯查询、模式切换余热锁、非默认回收率小包尾量、冷源预算边界、当前格式恢复。
- [x] 一组定向GameTest覆盖实际Create管泵蒸汽→水、输出堵塞/缺冷源恢复、顶部五种源和侧面无效/流水无效、融水后水蒸发、蓝冰持续、逐成员额度/混列安全与代表性原核热供热。组合场景，避免每类冷源复制整套测试；仅临时隔离配置缩短消耗阶段，完成后恢复。记录真实SERVER默认和至少一个非默认值生效。
- [x] 运行建议：`./gradlew.bat test --tests "*HeatExchangerStateTest" --tests "*Condensation*Test"`、`./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_condensation -PgameTestDirectory=build/runtime-condense-01-default`及增量`assemble`。准确命令以实际入口为准，退出0、必需断言全部通过。禁止clean/--rerun-tasks/全项目测试；公共事务出现疑点才扩大热域回归并报告原因。
- [x] 自审后交付`build/reports/extension/EXT-B-CONDENSE-01/implementation-report.md`与原始日志索引，记录实际写集、技能应用、配置键、制品哈希、未测边界。PM安排一次联合独立审查，复用已审运行证据；只对具体发现整改。

## 审查重点与人工门

重点：不以模拟锁模式，混列不串工质；出水/顶部消耗预算预检与提交一致；核热无回归、冷凝无HU；小包尾量、配置缩容与正常保存守恒；真实Create管网不会因模式空罐而反复撤销连接。上述均由定向断言或审查对应，不研究历史存档。

人工联调合并为一张清单：锅炉→汽轮机→冷凝→给水在关闭泄压时守恒；孤立水、雪/冰/浮冰融水与蒸发，蓝冰、缺冷源及堵塞恢复；顺带观察护目镜修复和01F门槛/效率/周转行为。不单独要求重做已通过的三档搭建或两项小显示修复。实现与自动证据通过后停在该门，不提前合main或进入后续玩法。

## PM执行记录

- 2026-10-05：用户确认全部推荐参数；一个高级执行者负责共享账本与世界变化，一位高速审查者合并规格和质量审查。PM只维护文档与Git，未决取舍仍报用户。
- 派发基线`1aa6987`，代理`/root/condense01_impl`使用gpt-6.1-sol/high；模型升级理由为共享工质事务、核热隔离与冷源世界修改耦合。派发时尚无交付证据，独立审查计划优先gpt-6-luna/high。
- 最终执行者交付：原核热16项＋冷凝6项JUnit、8项独立真实GameTest、增量assemble均退出0。`condense01_review`采用gpt-6-luna/high联合审查，无发现；未重复执行测试。PM核对原始XML、日志及制品hash后提交功能，最终仅移除新增测试EOF空行，复用已审证据。
- 现在停在[合并联调](../../reviews/2026-10-05/condense-01/manual-checklist.md)；01F与护目镜顺带观察，尚未人工通过，不合main或派发后续玩法。开发与用户世界配置未覆盖，新SERVER字段使用NeoForge默认补齐。
