# 核换热器思索实施计划（四情景）

> **For agentic workers:** 使用`superpowers:subagent-driven-development`，由用户任命的PM派发执行者。仓库权限、精简验证和用户已确认排期优先于通用技能。执行者仅交未提交改动，不执行Git写操作。

**任务ID / 状态：** DEVICE-PONDER-05-EXCHANGER / 执行中；`/root/exchanger_ponder_impl`（gpt-6-luna，高思考）已实际启动，基线`f650aa7`。PM已核对四幕布局与玩家文案边界；静态审查和新教学播放门尚未通过。
**Goal:** 四个独立情景介绍现有核换热器的用途，玩家能看清流体方向、顶部负载与锅炉内置位置，不改变设备机制。
**Architecture:** 新建`HeatExchangerPonderScenes`、四份NBT及可复现生成工具，在现有`P1PonderPlugin`中仅为`nuclear_heat_exchanger`绑定四幕。所有显示只操作Ponder客户端临时世界；正式热账本、流体能力、配置和注册保持。
**Tech Stack:** Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，核对实际Gradle文件，不升级。
**Spec:** 用户要求汽轮机思索后补齐换热器加工热源，再分场景介绍其用途；2026-10-09明确确认工作盆、烧结炉及汽轮机思索手测通过并要求开始下一步。权威边界为[接续排期](./2026-10-08-turbine-exchanger-ponder-sequence.md)、[02R1](./2026-10-08-ext-b-exchanger-02r1-continuous.md)、[03](./2026-10-09-ext-b-exchanger-03-sintering.md)、现行源码与配置。该卡细化既有教学授权，不提出新玩法。

## 前置与权限

- 工作盆、烧结炉功能和汽轮机思索均已通过自动/独立审查及用户手测；main本次分别净整合，接续条件满足。现有离心机、反应堆、锅炉和汽轮机播放门不重开。
- 实施目录`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`；派发时实际HEAD记入报告。PM将最新任务与验收文档同步到该目录，功能基线包含已审`094a925`和`a94f2ea`。
- 既有脏文件`logs/debug.log`、`logs/latest.log`及`tools/art-assets/__pycache__/`、`tools/art-assets/store-01/__pycache__/`、`tools/ponder/__pycache__/`保留。不进入美术工作树，不写用户世界/启动配置，不覆盖其他任务。
- 必读根AGENTS、治理5.1/5.2、文档入口、本卡及上述Spec；实际读取`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md`，应用当前版本Ponder及资源路径，报告列实际使用。
- 项目经理只写计划/验收/文档和管理Git；源代码、测试、NBT生成工具由执行者实现。执行者禁止任何Git写操作、派发或改核心文档。

## 全局约束与允许写集

- `src/main/java/com/iksxh/create_nuclear_industry/ponder/HeatExchangerPonderScenes.java`（新）：四幕客户端演示；中文职责、坐标、时序和纯演示边界注释。
- 同目录`P1PonderPlugin.java`：增加换热器四故事板ID及绑定，保留其他教学的入口、顺序和行为；更新相关注释。
- `src/main/resources/assets/create_nuclear_industry/ponder/nuclear_heat_exchanger_{heating,boiler,processing,condensation}.nbt`（四个新文件），无其他模板改动。
- `tools/ponder/heat_exchanger_scenes.py`（新）：仅生成/校验上述四模板，必要时只读导入既有模板工具；禁止修改其他生成器、模型/纹理工具及构建脚本。
- `src/main/resources/assets/create_nuclear_industry/lang/{zh_cn,en_us}.json`：只增加这四幕`ponder.nuclear_heat_exchanger_*`相关标题和文本键；保留其他键、顺序及格式，不全文件重排。
- `src/test/java/com/iksxh/create_nuclear_industry/HeatExchangerPonderContractTest.java`（新）：少量有意义静态合同验证，关注四模板、注册入口、真实接口坐标/方向及正文时序。禁止变成照抄每句文案或每个实现细节的测试。
- 唯一实施报告`docs/reviews/2026-10-09/heat-exchanger-ponder-05/IMPLEMENTATION.md`；原始证据`build/reports/extension/DEVICE-PONDER-05-EXCHANGER/`。审查报告路径由PM另授审查者，不改其他核心文档。
- 不改热工、配置、配方、服务端/正式流体库存、设备碰撞、模型/贴图、动画接口或注册；不增加GUI，不实现取消的超临界蒸汽输入，不提前演示事故/辐射。首发前不研究旧存档兼容。

## 四幕与玩家文案

| 故事板 | 画面和教学要点 |
| :--- | :--- |
| `nuclear_heat_exchanger_heating` | 小型Create储罐锅炉、核换热器和清晰分开的冷热管路。橙色背面进热液、蓝色正面出冷液，顶部供热；演示同向首尾直列从两端接管、共享冷热库存而各台供热。简短说明供液稳定才能持续供热，实际供热随热液流量变化。无需重教泵/锅炉基础。 |
| `nuclear_heat_exchanger_boiler` | 复用现行合法最小锅炉尺寸思路，仅展示底部非边框换热器、上方再加热隔层和可见冷热口。分层揭开前壁看见两部件形成有效热力回路；数量由二者较少者决定。热口与换热器同层、冷口在隔层。只讲换热器内置角色，不复制锅炉完整搭建/调压教学。 |
| `nuclear_heat_exchanger_processing` | 紧凑场景依次演示顶部工作盆+搅拌器，再换成燃料烧结炉，同一热冷回路始终可见。盆持续超级加热，炉可烧结芯块；设备存在即持续耗热、空设备也耗热，热液耗尽或回液受阻立即停热，恢复液路后加工恢复。必要默认数值须明确是默认，避免硬编码文案伪称所有配置。两种顶部负载不同时占同一格。 |
| `nuclear_heat_exchanger_condensation` | 独立普通蒸汽入口与回水出口，顶部直接接触冷源。依次清楚展示水源、雪块/冰/浮冰、蓝冰：雪/冰/浮冰融为水源，水源最终蒸发，蓝冰不消耗。流水不作为冷源，回水受阻或冷源消失暂停。普通蒸汽1:1回水为默认；不演示超临界蒸汽直接输入。 |

中文和英文语义一致，Java fallback与资源一致。只讲操作和特有规则，删除“画面仅作示意”“创造马达只是动力示意”“管道不会泵送”等开发措辞和不言自明的Create基础知识。每段短句，正文过长拆成顺次两段；不要追加重复停机排查页。

所有管路使用现行合法接口，回液/回水出口和输入罐不被炉体遮挡。选取能同时看见入口出口的设备朝向/镜头，必要时用一次有目的的旋转揭示背面，禁止把主要口藏在后方。供热、冷凝模式由真实工质决定，不能演示不存在的模式开关。屏幕静态高度/缩放应覆盖整个设备和顶部负载，远离标题、底部按钮与进度条；不要复用巨大基地板/偏置坐标造成裁切。

## 审查关注点

1. 四幕NBT能加载、方块/方块实体ID与属性有效；Ponder不会调用错误类型实体或崩溃。
2. 锅炉内置布局符合现行结构，独立换热器热后进冷前出，冷凝蒸汽后进水前出；箭头/罐变化和文案一致。
3. 正文含淡出生命周期，每次一段，前段完全退出后再显示下段；缩放/位移与高亮、文字锚点一致。
4. 顶部负载互斥，连续耗热及立即停热准确；不误演示原40tick锅炉余热给盆/炉续热，不改变真实状态。
5. 仅本批文件变化，旧教学、服务端与美术素材无夹带；中英文键及fallback顺序、数量对应。

## 实施、精简验证与完成定义

- [ ] 执行者先读源码/API、配置及现有教学模板，记录技能、HEAD与既有脏文件；静态合同按上述高风险输入编写，再实现四幕和四模板，不复制运行引擎。
- [ ] 生成工具运行并验证四模板、JSON/注册/生命周期；定向`./gradlew.bat test --tests '*HeatExchangerPonderContractTest'`和一次增量`./gradlew.bat assemble`。保留命令、退出码、用例数和原始日志，不把零用例算通过。
- [ ] 冻结差异后一次独立合并审查规格+质量，复用实施证据不重复跑Gradle。实质整改只复验对应风险；本批为纯教学，不启动GameTest、全套回归或旧存档测试。
- [ ] PM核对差异及证据、提交同级候选并记录状态；用户从核换热器按W播放四幕，检查接管可见、顶部负载完整、文案不重叠、无错误方块/崩溃，各幕播完。

交付停在本台集中播放门，未播放前不合main或自动接其他设备/主线。用户视觉验收不能被静态几何或自动断言代替。自动通过与用户已验收的前置分别记录。

**冲突扫描：** 本卡为单实施任务；插件、语言和四模板由同一执行者顺序维护，注册/模板/文本消费约定一致。美术侧不同时写Ponder源码或模板；语言只增本批前缀，不改美术文本。测试验证接口和时序，不要求正式运行机制变化。未发现合同要求与写集互斥项；如实际运行需要越界，报告PM确定范围，不自行扩大。
