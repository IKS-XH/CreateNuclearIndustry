# EXT-B-BOILER-REWORK-01：可变分区锅炉与温压实现计划

> **2026-10-08收尾：** 用户确认最终01F精简手测通过；本卡继续适用的锅炉规则已随REWORK-01～01F[验收合入main](../../reviews/2026-10-08/boiler-rework-01/ACCEPTANCE.md)。被01F替代的整炉瞬时切种不再要求复测。下方候选、失败及待验收描述保留历史含义，不作为当前人工门；不扩展为所有动力场景或首发验收，不自动接其他主线/教学。

> **执行技能：** 使用superpowers:subagent-driven-development；共享事务作为一个核心任务执行，不按文件切给多个同时写状态的代理。下面步骤用复选框跟踪。项目治理5.1的集中定向验证、PM持有Git、人工门优先于通用技能中的重复全量测试、执行者提交或清理工作区步骤。

**目标：** 同批交付内置核换热器、完整再加热隔层、配置范围内可变长宽高、水汽分区容量及可付费验证的温压/保压出汽。

**架构：** 控制器持有整炉唯一水、汽、冷热冷却剂及已付HU账本；带明确边界的结构快照定位分区与成员。炉内换热器只由锅炉结算，独立机保留原用途；当前按01B采用唯一Create原生出汽压力下限控件、实际温压自动汽种，所有流体能力、主动输出和泄放复用同一权威账本。

**技术栈：** MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。

**规格：** [完整已确认方案](2026-10-07-high-pressure-boiler-rework-proposal.md)。用户于2026-10-07回复“采用整组推荐参数（推荐）”，方案第3节全部数值、模式、简化散热和新口成本已冻结，不重复询问。

**状态（2026-10-07）：** 核心、配置、资源和素材已交付；29项定向JUnit通过，最终15项真实GameTest通过，增量assemble成功。一次合并审查的两项Important已由同一核心执行者整改、同一审查者复核关闭，可进入[集中人工候选](../../reviews/2026-10-07/boiler-rework-01/CANDIDATE.md)，尚未人工验收或合入main。核心`/root/boiler_rework_core`（gpt-6-astra/high）、素材`/root/boiler_rework_assets`（gpt-6-luna/medium）、审查`/root/boiler_rework_review`（gpt-6.1-sol/high）；实施起点`d303913`，功能快照`d5d3604`。各轮失败及整改证据保留，自动通过不替代人工门。

## 全局约束

**2026-10-07控件与汽种整改：** 用户确认按实际汽温/炉压自动决定汽种、移除开关，只留出汽压力下限0～100逐整数；修复调节导致冷液停流，文案改为有效热力回路并显示当前产汽。[01B定向卡](2026-10-07-boiler-controls-automatic-output-fix.md)为当前活动实施合同。下文原两模式控件及“失格停汽、不自动换种”的已完成记录保留历史含义，现行规则以01B为准；仍独立等待锅炉集中人工门。

01B功能快照`32e6f89`，15项账本单测、3项真实管路GameTest及一次合并审查通过，最新JAR2,193,605字节；本合同后文29/15/01A3项及其制品为对应历史合同证据，最新启动和重点复测统一见候选页，不机械重复全套。

**2026-10-07层位整改：** 用户要求热入口与底部换热器同层并允许占棱边，[01A定向卡](2026-10-07-boiler-hot-inlet-layer-fix.md)已实施，新增真实3项GameTest与增量构建、审查通过，整改快照`cca0dbe`。原方案的水区非边框热入口及全部棱边仅外壳规则在该例外上被替代，原自动证据保留；锅炉人工门继续有效。

- 工作区`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，代码基线`51dedfc`；派发时实际HEAD另记交付。本锅炉代码仍仅在同级候选；两台思索已另行验收并单独合入主工程，用户客户端/测试世界不动。
- 执行者不是PM，不修改AGENTS/治理/核心文档、不Git写入、不另派代理；只写本卡代码范围及自己的指定报告。中文注释/Javadoc、无独立GUI、配方有序、SVG风格不变。
- 不改反应堆、汽轮机效率/输入规则、冷凝玩法、其他教学、依赖/构建脚本、发布出口或旧存档迁移。当前版本状态保存加载、缩放不造流体/HU属于本批必要生命周期。
- 环境工质密度继续读取P1配置`coolantAbsorptionHuPerMb`；每机功率/冷热容量继续读取换热器配置。新锅炉字段以已确认表为准，旧固定暖炉/容量不并行生效。
- 新方块注册ID固定为`high_pressure_boiler_hot_coolant_port`、`high_pressure_boiler_cold_coolant_port`。现`boiler_heat_exchange_section`保持资源身份、改显示为“锅炉再加热段”；不以重命名为由做旧存档研究。
- 素材执行者独占两新口静态资源及新工具目录；核心执行者独占Java、测试、语言、配方/标签/掉落和构建。审查只读，不复跑已足够的测试。
- 必须实际读取并应用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`及`minecraft-testing/SKILL.md`；素材执行者另读`minecraft-resource-pack/SKILL.md`。锁定版本API以仓库及依赖源码为准，不照抄新版本升级。

## 审查重点

1. 接入蒸汽管道后能完成预热/充压，不因主动输出、固定分热或保压重复领取导致永远不出汽；稳定连续流量与能量对账。
2. 满罐低温汽仍能再热，补冷水真正混合降温，断热/红石仅能使用实际已付余热，温压达标不免费升级整罐HU。
3. 多口、SIMULATE、重复调用、成员与独立直列邻接及拆装旧句柄不重复结算、不丢冷却剂。
4. 偶数、长方体、隔层高度变化和32工程封顶不沿用固定中心/±2坐标，不强制加载区块或每tick重新全体积扫描。
5. 原生控件切模式正确撤销运输压力，出汽身份不因降温/降压自动切换；管网运输压力不是炉压，窗口/护目镜及时反映实际状态。

## 任务1：共享核心、配置与接入

**执行者：** 单一高级模型持有，实现完成后统一验证。代码写集：

- `src/main/java/com/iksxh/create_nuclear_industry/boiler/`：既有类及该包必要新类型；结构、账本、控制器、四类端口、原生控件、压力和当前持久化。
- `src/main/java/com/iksxh/create_nuclear_industry/heat/NuclearHeatExchangerBlockEntity.java`、`NuclearHeatExchangerBlock.java`、`HeatExchangerLine.java`、`HeatExchangerState.java`、`HeatExchangerMode.java`、`HeatExchangerBoilerBridge.java`：仅炉内单一归属/共享库存/禁止外部重结算的必要接入。
- `src/main/java/com/iksxh/create_nuclear_industry/content/BoilerContent.java`及`config/BoilerConfig.java`；`CreateNuclearIndustry.java`仅锅炉部件/炉内换热器结构失效的放拆通知分支及两新口沿用既有锅炉部件的构造搬移禁令。`content/ModCreativeTabs.java`仅在既有锅炉段加入两新口。上述精确接入已由PM批准；其他入口先报告准确位置，不自行扩大写集。
- 观察窗可在`boiler/`包内新增轻量BE、renderer与客户端事件并由`BoilerContent`注册；客户端仅持显示快照，不能另建库存，服务端不能加载客户端渲染类。
- 两种语言JSON：仅锅炉、再加热段、炉内换热状态、控件/诊断/护目镜键；保留思索等其余键与值。
- 数据：两个新口有序配方及掉落；采掘标签新增两口。现有设备配方原料/产量不变。不得批量格式化资源。
- `src/test/java/com/iksxh/create_nuclear_industry/boiler/`及热账本测试：修改相关断言，保留独立机行为覆盖。新配置影响的`ExtensionConfigGameTests.java`、`ExtensionBoilerGameTests.java`及锅炉相关真实集成用例；可新增该任务专用GameTest类，不改无关断言或测试运行框架。
- 指定交付报告：`docs/reviews/2026-10-07/boiler-rework-01/implementation.md`，原始日志/XML可写`build/reports/extension/EXT-B-BOILER-REWORK-01/`。这是执行者唯一文档报告写口。

**接口约定：** `BoilerStructure.Form`升级为明确长宽高/包围盒、隔层、水汽有效格数及全部成员/端口位置的只读快照；所有归属、失效和管道刷新消费同一快照，不分别猜中心。`BoilerState`在服务端持有配置/几何快照、冷热/水汽量、Ew/Es和有界尾量；提供同一库存的逐口模拟/执行、温压/需求/资格和持久化入口。实现者可选择内部方法签名，不改变合同单位及公共调用语义；新增公共入口用中文说明输入输出和边界。

- [x] 读取规格和当前相关调用，确认所有固定5³/9段假设、旧暖炉字段与配置测试调用；列出要更改的接口后顺序实现。
- [x] 配置与结构：`dimensionRange=[5,11]`、每区2000mB/格，任意整数长方体；唯一完整隔层，上下各≥1层，底部≥1机/隔层≥1段；明确非边框规则、四类多口、控制器/顶阀各1。按long验证容量/功率；失效与占位检查适用于新范围。
- [x] 热量账本：实现规格第4节Ew/Es与温压、1:1质量转换、全库存付费再热、保压取汽及简化散热。先水侧预热/已有汽再热、再新增产汽，避免满汽再热或稳定补水死锁；默认冷水到SC稳态1HU/mB，不能把暖水焓又收费一次。
- [x] 冷却剂与成员：整炉冷热共享，最多`min(机,段)*pairHeatHuPerTick`且受换热器额定功率限制，热转冷同量。当前成员已有库存/储热与控制器之间一次性转移且总量不变；归属失效立即撤销旧能力，不能独立线和炉内同时持有同一份量。拆件停机、保留当前账本，不复制库存。
- [x] 控件与运输：默认SC模式、保压0.6，普通模式保压0.1；每口256mB/t，按每次EXECUTE最新库存限量，原生管道与邻罐主动出汽可用。切模式保留实际焓，管外流体不删除；降温/降压返回零而不改流体。阀按0.9/0.8炉压开关、每汽格32mB/t；红石/缺水/堵塞保护及已付余热符合规格。
- [x] 资源数据与遥测：注册两口和原生控件，制作规定有序配方/掉落/采掘；新口和控制器放置朝向正确。窗口可观察水区液位，护目镜显示分区尺寸/容量、有效配对、温度/炉压/保压、实际HU/t及产汽/泄放/瓶颈，不夹带开发措辞，图标留白沿用现有helper。
- [x] 写并运行任务3的有意义定向验证、记录实际技能、差异文件、接口选择、守恒/生命周期与仍待人工项；交付未提交改动，不自行Git或启动客户端。

## 任务2：两种冷热口SVG与静态模型

**执行者：** 高速模型，可与任务1并行，不能运行Gradle或写Java/语言/数据。

**独占写集：** 两新ID的`assets/create_nuclear_industry/blockstates/*.json`、`models/block/*.json`、`models/item/*.json`、`textures/block/*.png`及其必要仅新口辅助模型/纹理；`tools/art-assets/boiler-rework-01/`新SVG、导出入口/清单。不得改通用导出器、旧外壳/端口资源，不得ImageGen。报告只写`docs/reviews/2026-10-07/boiler-rework-01/assets.md`。

**接口约定：** 两口使用现有水平`facing`四态、完整方块壳体、向外接口。核心读取固定注册ID，素材不要求核心改渲染器；复用已有锅炉灰钢、铜/黄铜细节和外壳背/侧/顶纹理，新front为橙色热入口/蓝色冷出口及明确方向标记。单独和成型都是完整无缺面模型，项目既有block显示变换，不放大手持模型。

- [x] 检查现行锅炉四态/父模型与SVG色板；画两种可辨方向的入口/出口素材。
- [x] 只导出独占资源，核查4种朝向、纹理/父模型引用、JSON可解析与合法PNG；输出一张小预览供PM检查，不启动客户端。
- [x] 报告新文件、SVG复现命令和实际检查；通知核心资源就绪，核心统一打包。见[素材报告](../../reviews/2026-10-07/boiler-rework-01/assets.md)，仅静态通过，不视为客户端验收。

## 任务3：集中验证、一次审查与人工门

**持有人：** 核心执行者唯一持有JUnit、GameTest与assemble；PM安排一轮只读规格/质量合并审查，整改只复验受影响项，不再加重复全套。

- [x] 定向JUnit：`BoilerStateTest`与实际受影响热账本/几何类。断言参考5³上下各9格、各18000mB、满水预热32400HU；普通0.8/SC1HU/mB，9000mB沸点汽再热须1800HU；冷热量、HU收支、补冷水、满汽再热、保压多口、模拟纯读/同tick额度、默认/非法/非默认配置和当前保存加载。
- [x] 按实际存在测试类运行一次`./gradlew.bat test --tests <相关完整类名> --console=plain`，不用clean/rerun/build重复同套；失败修改后只跑对应项。数字误差用合理浮点容差，禁止靠删守恒断言通过。
- [x] 沿用既有Gradle属性`gameTestNamespace`与`gameTestDirectory`，本批专用GameTest域设为`create_nuclear_industry_boiler_rework`；空模板按锁定API引用已有模组模板，必要仅增加该域空模板资源。一次运行`./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_boiler_rework -PgameTestDirectory=run/verification/boiler-rework-01 --console=plain`；报告注册用例数及实际断言，不把空筛选/仅编译当成运行通过，不改构建脚本或新造测试框架。
- [x] 真实重点：5和11边界、偶数长方体/非居中隔层、边框/分区非法格，多热/冷/水/汽口能力，成员排除独立线与stale句柄，实际Create抽/推SC与普通模式、配置非默认值，当前保存恢复；独立换热器供Create锅炉/冷凝和汽轮机SC接入做受影响冒烟。
- [x] 待素材完成后增量`./gradlew.bat assemble --console=plain`一次，核对新资源/配方/语言及制品，记录JAR大小/SHA。构建失败只修实际错误；已知Saving worlds停滞记录断言结果，只处理本轮自有测试进程，绝不杀用户客户端。
- [x] PM读取实际差异和已有证据，安排一个只读审查者；不要求审查者重复跑测试。发现合同内问题交同一执行者修复，确需扩大写集则由PM明确批准。
- [x] PM维护候选说明、配置/配方/玩法文档、人工清单与Git快照；候选在同级目录启动。本批只完成到**待人工验收**：集中核查变尺寸/分区搭建、模式与保压控件、窗口/端口朝向、预热/充压/连续出汽、红石余热及堵塞保护。两台思索已另行验收并单独合入main；本锅炉人工门未过，不合入锅炉或接其他主线。

**审查报告写口：** `docs/reviews/2026-10-07/boiler-rework-01/review.md`，只读代理可写这份报告，不改核心文档/任务状态。

**精简约定：** 不为开工跑全套、不对每台独立素材再跑GameTest、不手测前后或无冲突合入重跑同一证据。真正修改公共事务/热量后只扩大相关回归；本批不写旧版本迁移、不转换/删除用户存档。

## 当前交付与人工门

- [x] PM核对[核心报告](../../reviews/2026-10-07/boiler-rework-01/implementation.md)、[素材报告](../../reviews/2026-10-07/boiler-rework-01/assets.md)及[合并审查/整改关闭](../../reviews/2026-10-07/boiler-rework-01/review.md)，保存制品快照、更新配置和候选说明。
- [x] 本卡继续适用的规则已随2026-10-08最终01F精简验收通过；被后续方案替代的旧操作不作为现行测试要求，具体范围见本页收尾说明及验收记录。

29项账本单测在生命周期整改未改算法时复用；原最终15项域测试包含原13项及2项远端区块/重叠回归，01A另增3项定向域且同步旧合法热口夹具。R1证明FULL可用性退降/恢复，不声称发生完整磁盘卸载。最新制品2,186,418字节，SHA-256见候选页；原2,179,767字节制品保留。32是工程范围封顶，未声称最大尺寸性能验收。
