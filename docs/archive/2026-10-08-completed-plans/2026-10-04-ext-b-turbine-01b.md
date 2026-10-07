# EXT-B-TURBINE-01B：三档薄壳汽轮机与256RPM实施

> PM使用writing-plans和subagent-driven-development自动派发；执行者使用executing-plans，仅实现分配写集，不做Git写操作或改核心文档。用户的自动派发、按范围验证与PM不编写功能代码规则优先于技能通用提交/审批/重复验证步骤。

**状态：2026-10-05候选交付后，搭建与外观人工反馈未通过，待整改。** [AeroEngine调查及新增交互建议](2026-10-05-turbine-assembly-experience-proposal.md)已整理；原独立板方向/扳手合同未落实，碰撞与模型也不一致，撤回该项完成标记。工作目录 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，实施基准 `0c25010`，已交付候选 `441ec68`。主目录仅同步文档，用户 `.vscode/launch.json`、已有日志/pycache不纳入本批。原自动证据保留，整体不验收、不合main。

**目标：** 三档长粗一起变化的薄八棱壳、真实叶片转子、透明窗、侧置控制器、两端独立轴；默认256RPM且仍配置化。所有加工守恒与热端已有参数不变。

**架构：** 唯一侧控制器持有库存/账本；前后两个独立动力源只读其份额；结构模型、碰撞和占格共享明确几何合同，扩大转子有正确渲染包围盒。状态为服务器权威，客户端只负责模型与护目镜。先冻结运行/模型交接接口，再并行不同写集。

**版本：** MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，无新依赖。

**规格：** [01B方案](2026-10-04-turbine-structure-revision-proposal.md)全文；本轮256RPM取代128RPM。默认3×5×3、5×8×5、7×11×7，转子3/6/9，额定流量54/108/162mB/t，双罐各4000/8000/12000mB；不因转速翻倍增加总SU。

## 全局限制与审查重点

- 必读AGENTS、治理5.1和本卡/方案。必须实际使用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`；资产另用 `minecraft-resource-pack/SKILL.md`。中文代码注释，不复制Aero素材，不用生图，不改依赖/许可证。
- 无GUI；不改锅炉/换热器/其他设备行为，不推进冷凝，不实现转子动画或事故。
- 保留蒸汽atlas修复；透明窗用可见内外表面的玻璃，不能以未添加图集的PNG宣称可显示。
- 旧前端控制器ID、类型和库存NBT继续可读；只携一份库存安全拆放。旧结构撤销SU并显示需重建，不移动世界方块。
- 中型中央两排、底部端口/控制器、四朝向、旧库存大于新容量、配置非默认长度均为必要边界。
- 空腔阻挡和外壳变更必须使机组失效。跨区块部分卸载不能遗留有效源；不能每tick遍历最大扫描体积或强制加载区块。
- 前后双轴实际256RPM合网/分网需验证，前端单接可获得自身50%份额。存在其他动力源时遵循Create原生规则，不对外部来源清零。
- 转子被窗遮挡/视锥裁剪、薄壳内部剔除、斜角与端盖共面为资产审查重点。不以模型文件存在代替游戏视觉验收。

## A. 结构、生命周期与配置（复杂实现任务）

**写集：**
- `src/main/java/com/iksxh/create_nuclear_industry/turbine/` 及其必要client子包；按职责新增几何、所有者定位、渲染辅助，避免把所有职责堆入控制器。
- `src/main/java/com/iksxh/create_nuclear_industry/config/TurbineConfig.java`。
- `src/main/java/com/iksxh/create_nuclear_industry/content/TurbineContent.java`、`ModCreativeTabs.java`，仅本批七件注册/展示；`CreateNuclearIndustry.java`仅本批注册、客户端渲染和构造搬移禁令接入。
- `src/test/java/com/iksxh/create_nuclear_industry/turbine/`、`src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionTurbineGameTests.java`、`ExtensionTurbineConfigGameTests.java` 和仅本批测试结构。旧双轴探针若被公用类型改动影响，可做最小编译兼容，不改变断言规避失败。
- 报告、接口文档、必要隔离测试配置与日志：`build/reports/extension/EXT-B-TURBINE-01B-RUNTIME*`。不得修改B资源/工具写集或核心docs。

**接口交接：** 最先向PM提交机器状态字段、局部坐标/朝向、模型名字、静态壳及转子渲染入口、窗口有效位置、物品模型入口。资源接口要有有限明确的模型集合，不因完整状态笛卡尔积生成大量重复网格；未成型与旧状态必须有合法回退模型。普通模型由B制作，任何Java客户端接入归A。PM审查并转交B后冻结；变动先报告。

- [x] 补受影响的几何/配置测试：三档shell环8/16/24格、端面轮廓9/25/45格、内部空气0/8/20格；错误直径/空腔堵塞/入口排汽错位由结构校验拒绝，完整负例矩阵不逐一自动跑。
- [x] 扩充Tier几何配置，增加三档直径键，转子有效范围3..16；默认RPM256，仍受Create上限约束。合法性进入实际成型/容量/显示，旧有效性能键不覆盖。
- [x] 控制器移到四侧中央区（中档z3/4），保持唯一库存和携带数据；两端各同一output_shaft，按所处端自动朝外。转子仍3/6/9个，其余内腔必须空气。
- [x] 保留每口独立流量、主动排汽与旧句柄世代校验；四侧含DOWN，进汽与排汽合法区不重叠。端口/轴所有者不能仅靠旧formed状态猜测。
- [ ] 七件模型/碰撞/渲染对接，包括透明窗注册、转子正确包围盒、内外薄壳、独立面板放置/扳手方向。2026-10-05复核：独立外壳无扳手方向，未成型外观与碰撞不一致，需整改；已通过的其他部分证据保留。
- [x] 旧结构失效后不删库存不残SU；控制器拆放/同tick恢复仍不能重复加工。新控制器的客户端NBT与纯服务端状态边界清楚。
- [x] 根据B交付的语言键需求输出清单，由B写语言文件，A不并发修改。
- [x] 持有唯一Gradle时段，定向JUnit与正式设备GameTest：三档北向/多口、两个独立轴网、原生排汽、红石、NBT携带及模拟旧机源迁移。四朝向外观、实际负载合分网、真实旧世界及跨区块复测交人工门；部分卸载停机路径作静态审查。纯账本原已证行为若未变复用，不重跑热端或燃料全套。
- [x] 非默认配置用最小代表场景证明直径/长度和RPM仍来自配置；新的256RPM默认另用真实网络验证。只在隔离游戏目录，不能打开或改用户存档。

## B. SVG、薄壳网格、叶片与观察窗（独立资源任务）

**写集：**
- `tools/art-assets/turbine_assets.py`、`turbine_models.py`、`turbine_data.py`及本批必要新增同前缀生成/预览辅助；`tools/art-assets/svg/block/turbine/`。不改其他设备生成器。
- `src/main/resources/assets/create_nuclear_industry/` 下仅汽轮机blockstates、models、textures；`lang/zh_cn.json`和`en_us.json`仅本批汽轮机键。不回退steam称呼。
- 新增 `data/create_nuclear_industry/recipe/crafting/turbine_window.json`、`loot_table/blocks/turbine_window.json`；对应两项Minecraft采掘标签只追加window。其他六配方不变。
- 报告及预览 `build/reports/extension/EXT-B-TURBINE-01B-ASSETS*`。不写Java或docs，不启动Gradle/客户端。

**接口：** 遵循A首先提交的模型/状态合同。可以先重绘独立SVG和窗口配方，正式模型生成必须等接口冻结。三档外/内八边形按方案法线厚度和占格裁切；不得把完整方块贴图贴在薄板每一面造成黑框和格栅箱体观感。

- [x] 原创重绘钢灰主体、有限黄铜边箍/铆钉、进/排汽箭头、侧仪表与轴承；16×16像素风格，不复制Aero的源资产。
- [x] 根据接口生成每格薄壳/斜角/前后端盖，保证端部中央轴头与两侧口清晰。内外表面不互相剔除，接缝只由一套面闭合。
- [x] 叶片模型有轴心、轮毂和多片相间叶片，叶间留空；三档半径1.1875/2.1875/3.1875格，预留动画轴心，未成型/手持适当缩放。
- [x] `turbine_window`透明玻璃及同风格金属框，物品、世界、粒子和loot齐全；无序1外壳＋1铅玻璃→1窗，英文Turbine Window，中文汽轮机观察窗。output_shaft去掉“后”字限定。
- [x] 对真实生成网格做三档外观与剖面预览（包括未成型面板、转子、窗口）；材料解码、模型/JSON引用、UV/法线/重面/包围盒检查，只能记离线证据。
- [x] 输出报告和准确资源列表给A/PM，交还写集后不得继续修改已测资源。

## 集成、配置与人工门

- [x] PM核对交接接口/方案、一次独立规格与质量合并审查；实际问题返回原执行者整改，不进行重复形式审查。
- [x] A汇总稳定版定向验证并增量assemble。PM核实原始通过/保存/退出码、最终资源与构建输出，不再重复全套。
- [x] 当前开发候选 `run/config/create_nuclear_industry-turbine.toml` 已有rpm128；默认变更不会覆盖旧文件。PM核查有效世界覆盖后，仅将本用户要求的rpm改为256，先备份，其余键原样保留。此操作不改已存在的游戏世界块/NBT，也不推广到其他实例。
- [x] PM更新配置、配方/内容清单、人工手册与阶段状态，保存候选提交。提供新截面搭法、旧机库存迁移操作和一份合并手测清单。
- [ ] 用户人工确认模型/材质、两个轴、透明窗和迁移后才合入main并开始下一主线。本批结束停在该门。

## 执行记录

- 开始前：原蒸汽图集/名称修复已在0c25010，资源验证通过，实际视觉待合并复测。
- 本轮默认转速升到256，额定耗汽与总SU不变；不把转速翻倍解释为免费翻倍功率。
- 执行者：运行A为`/root/turbine01b_runtime`（gpt-6-sol/high，结构迁移及机械网属复杂任务），资源B为`/root/turbine01b_assets`（gpt-6-luna/high）。两者文件写集分离，A独占Gradle，最终验证等B交还资源。
- 交接：有限piece编码0..206，转子分静态轴心与BE绘制叶片；窗口限四侧平直中心格、沿轴向连续。PM核对发现7格截面内斜角(±2,±2)必须属于壳，已要求A/B按真实外/内截面面积差统一，不用仅取外围一圈的错误判定。
- 开发配置：PM确认无候选客户端运行，未发现世界同名覆盖，已保存`build/reports/extension/EXT-B-TURBINE-01B-config-before-256.toml`，仅将实例rpm及对应默认注释128改为256；Create原最大转速已为256，无需改它。
- 交付：11项定向JUnit、最终默认/迁移4项和非默认1项GameTest通过，服务端均正常保存退出；资源冻结后assemble通过，本批501项资源与JAR一致。一次独立合并审查的问题已修，PM核对了原始结果及最终离线几何/纹理预览。
- 按治理5.1收敛验证：不为每个朝向和连接方式重新启动测试服。尚未自动覆盖的外接负载合网、四朝向、完整旧存档及跨区块行为明确进入同一人工清单，不写成已验证。
- PM实际使用minecraft-modding/testing评审实现，minecraft-ci-release核对候选版本与证据归档；不升级依赖、不推送、不发布，也不在人工门前合入main。原始执行报告保留写作时状态，最终证据以交付页为准。
