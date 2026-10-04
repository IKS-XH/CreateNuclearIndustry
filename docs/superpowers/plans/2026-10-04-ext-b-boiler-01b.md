# EXT-B-BOILER-01B：手测后结构、主动出汽与外观整改

**最新状态（2026-10-04）：** 提交d10e1af已获用户联合手测通过，见[人工验收](../../reviews/2026-10-04/boiler-01b/MANUAL-ACCEPTANCE.md)。下文保留执行与交付时记录，其中待手测状态由本条取代；新增锅炉01C每口独立限流候选及审查通过，待本轮复测，尚未合main，不启动汽轮机。

**状态：2026-10-04用户反馈基本手测通过并提出五项调整，随后确认5×5×5、最多9段；候选实现、定向验证和独立审查通过，暂停于联合人工门、不合main。** 既有锅炉与材料候选为`9e8690b`，位于`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`的`codex/ore-acquisition`。用户日志、缓存、主目录启动配置、所有世界均保留。

## 已确认要求与影响

1. 固定5×5×5、98壳位、内部3×3×3空气。十二条棱及其端点只能由锅炉外壳构成；侧面非棱边位置可放观察窗。底面中央3×3允许1～9个换热段（含正中心），其余底格外壳；顶面中心仍是1安全阀，其余顶格外壳。每段18HU/t、暖炉3600HU保持，满9段最高162HU/t、162mB/t，默认热冷却剂吞吐合计324mB/t。原3×3×4和本卡单段推导被取代，不实现可变尺寸或7×7×5。
   端口沿用原各侧面中央列布局：控制器位于下层y=1、向外；同层另外三侧中央可放1～3个水口；汽口位于上层y=3四侧中央、1～4个；侧面其他非棱边位置可用外壳/观察窗。y以底面为0。外围边框禁止窗口/端口/热段。此定位保留原单控制器确定锚点方式，扩大到半径2、上层升到y=3。
2. 旧结构不符合新规则时明确报错并停产，库存不删除或复制；用户重搭5×5×5，热段及下方热源移至底面中央3×3位置后可重扫恢复。锅炉存档原Sections范围0～8扩到0～9；原暖炉HU按实际容量限缩而不同比例放大。禁止修改用户存档、偷偷替换已放世界方块。
3. 观察窗须在世界中可透视，沿用反应堆窗口的透明渲染契约；七种锅炉方块统一为已认可反应堆/核换热器的金属面板、黄铜铆点及克制高光风格。外壳顶面左右、前后对称，接缝规整；全尺寸、朝向、窗口四侧可视与动画分件保持，不引入共面闪烁。
4. 蒸汽口沿外向面主动输出，必须覆盖邻接储罐及无外部动力泵的Create普通管道；整炉主动/被动出汽共用原256mB/t额度，给水256mB/t不变。复用Create机械泵的可配置距离，默认16格；内部压力按Create原生pressure/2对应流率换算，不关联锅炉库存百分比，不称为机械RPM。不得只实现邻罐推送却宣称支持无泵管线，不另造管网库存或开放口丢汽。适配须解决管线压力无来源归属、原生泵刷新清空压力的风险；不能逐tick累计加压，不能覆盖别的泵贡献。红石仍仅停收热/产汽，成型炉内已有汽允许输出。
5. 强化钢板去掉1精密构件和相应机械手步骤：1钢板基底→加1坚固板→压片→1强化钢板；仍单轮100%、无热无副产，原配方/成品/半成品ID不变。旧3工序半成品不得复制产物或免费跳过尚未投入的坚固板；须核对Create原生进度兼容并报告实际边界。

## 分工与写集

**新增依赖（用户本轮追加并已确认）：** 3×3换热器直接满铺时中央机原有冷热管口被包围，不能据此宣称9台可持续满载。用户已确认[换热器定向接口/首尾共享库存](./2026-10-04-heat-exchanger-chain-proposal.md)及[01D实施卡](./2026-10-04-ext-b-exchanger-01d.md)；保留已选5×5×5和9段目标，不退回8台环绕。锅炉结构/主动出汽、素材与强化板减料可独立继续；最终9段真实闭环验收依赖01D。

执行者均须读AGENTS、治理5.1、本卡及`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`；素材另读同根`minecraft-resource-pack/SKILL.md`。MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82不变。中文注释；禁止Git写、核心文档与转派。PM维护文档与Git，只读代码。默认高速模型；原生管网压力/流体守恒若复杂允许高级模型。

### A：窗口、七方块外观和材料配方

- 允许修改`tools/art-assets/boiler_01a_assets.py`、`tools/art-assets/svg/block/high_pressure_boiler/`、`src/main/resources/assets/create_nuclear_industry/textures/block/high_pressure_boiler/`、七块各自的`models/block/`及必要`models/item/`文件；七块为`high_pressure_boiler_{casing,window,water_port,steam_port,controller}`、`boiler_safety_valve`、`boiler_heat_exchange_section`。不更名资源ID，不改无关素材/蒸汽纹理/全局导出器/其他配方。
- 允许修改`src/main/resources/data/create_nuclear_industry/recipe/heat_exchanger/reinforced_steel_plate.json`仅移除精密构件步骤。Java和测试由D负责，A不运行Gradle。
- 对照当前游戏资源`textures/block/reactor_casing_{side,top,bottom}.png`、`reactor_window.png`及核换热器，而非历史baseline；SVG为源，不使用生图模型。修复生成器根因，重导出保持七块旧配方及蒸汽等非本批文件字节不变。独立证据目录使用01B，不能覆盖01A历史报告。
- 静态检查贴图/模型引用、窗口渲染与透明区域、顶面对称性以及旧非目标资源未变；提供与反应堆同图对照预览，窗口实际透视仍待手测。
- 交付`build/reports/extension/EXT-B-BOILER-01B-ASSETS.md`和同名证据目录。

### D：结构、端口及受影响测试

- 允许修改`src/main/java/com/iksxh/create_nuclear_industry/boiler/`、`content/BoilerContent.java`、`gametest/ExtensionBoilerGameTests.java`、`gametest/ExtensionHeatExchangerCraftingGameTests.java`、受影响`src/test/java/com/iksxh/create_nuclear_industry/boiler/`及锅炉专用测试模板、两语言文件锅炉诊断键。共享Create/mixin接入先报告具体文件，PM追加精确写集后实施，不借机重构其他模块。
- **PM批准的精确追加写集：** `src/main/java/com/iksxh/create_nuclear_industry/mixin/BoilerPipePressureMixin.java`和`src/main/resources/create_nuclear_industry.mixins.json`。仅为原生`PipeConnection`跟踪锅炉自有压力贡献、按差值增减，原生wipe时同步撤销归属；读盘剔除已保存的旧锅炉贡献后由活动汽口重建，禁止重载翻倍或覆盖原生泵压力。算法本体在`boiler/BoilerSteamPressure`等本包类。需要覆盖汽口/管路拆除、泵切向/刷新、多个锅炉与原生泵共存和保存恢复的归属撤销；不改Create流体守恒Mixin。
- D独占Gradle/测试运行权，使用现有namespace与独立build测试目录。A完成后仅一次增量assemble；定向锅炉GameTest覆盖结构合法/棱边拒绝/旧库存重扫、无泵真实管线、邻罐/堵塞/开放口、多口共享额度及主动被动共存；材料namespace只更新并验证两工序真实制造，保留全部无关旧测试。
- 不改原生Create锅炉供热、反应堆、热值、泄压或水口规则。旧账本未变时复用已审JUnit；若更改库存/流量事务，再仅跑受影响类。不跑全量、不删旧断言来提速。
- 交付`build/reports/extension/EXT-B-BOILER-01B-DEVICE.md`及同名证据目录。

## 派发与验收

- 原生出汽只读核查：`/root/boiler_outlet_probe`（高速模型）已交付`build/reports/extension/EXT-B-BOILER-01B-PROBE.md`，明确原生pressure/2换算、标准泵距和压力刷新风险；未宣称运行通过。
- A：`/root/boiler01b_assets`（高速模型）已启动；炉体尺寸变化不影响本批单格素材和强化板减料。
- D：`/root/boiler_review`接续为实现执行者（较高能力模型）；角色仅本批实现，不承担本轮独立审查，已开始5×5×5结构修改。
- A与D写集不重叠，可以并行；D主动出汽实现须先有确定的原生接入方案。完成后一轮合并规格/质量审查，复用定向证据。
- 最终用户复测仅本轮五项及旧结构数据保留；之前基本通过的项目不要求全部重跑。锅炉和材料仍待本轮复测，不自动推进汽轮机。

## 本轮候选交付

[联合交付与证据](../../reviews/2026-10-04/boiler-01b/CANDIDATE.md)：22项相关JUnit、40项required GameTest及最终增量assemble通过，独立审查发现的Create接面恢复问题已整改并复验。PM核对代码/资源写集和原始日志，未重复全量；[三组人工清单](../../reviews/2026-10-04/boiler-01b/CLIENT-CHECKLIST.md)仍待用户，未合main、不启动汽轮机。
