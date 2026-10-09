# 燃料烧结炉思索实施计划

**任务ID / 状态：** DEVICE-PONDER-06-SINTERING / 用户2026-10-10报告“烧结炉的思索也测试通过了”，两幕播放门关闭，main净教学提交`57763f8`；原候选`e76c8d9`自动与独立审查证据复用。用户此前回复“开始吧”已确认先烧结炉、后屏蔽装配台、逐台播放验收顺序。见[最终验收](../../reviews/2026-10-10/fuel-sintering-ponder-06/ACCEPTANCE.md)；其他主线保持暂缓。

**目标：** 玩家看懂生芯块的烧结、底部热源、顶部投料和水平取成品；画面紧凑，热源及接口可见，正文一次一段。沿用已验收设备规则，不新增配方、参数、GUI或服务端行为。

**架构：** 在现有Ponder插件增加烧结炉两幕和两份确定性NBT模板；独立场景类与生成器只操作客户端临时世界。已验收的离心机、反应堆、锅炉、汽轮机和换热器故事板不变。

## 前置、版本与职责

- 已实现单格烧结炉：1生芯块→1烧结芯块，400有效tick；输入/输出各64件；底部普通加热燃烧室或已验收核换热器供热；缺热、缺料或成品满暂停，断热保留进度。顶部只进生料，四个水平面只取成品，底面无物流能力。以`production/FuelSintering{Block,BlockEntity,State}.java`、现行配方及[核热验收](../../reviews/2026-10-09/exchanger-sintering-03/ACCEPTANCE.md)为实物合同。
- 复用同级候选树`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`，派发基线`f3d5cb7`。本批相关烧结炉和已验收教学源与main一致；主目录仍提供已验收版本，不提前合入新教学。
- 保留既有日志与三个__pycache__目录；不进入美术树、改存档或启动客户端。美术整改独立派发，不共享本批写集。
- PM只管理本卡、文档、审核和Git；执行者交未提交实现与报告，不做Git写操作、修改核心文档或派发子Agent。
- 必读根AGENTS、治理5.1/5.2、文档入口、本卡；实际读取`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md`，以及适用的superpowers实施/验证技能。核对Gradle及本地Ponder API，保持MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。
- 使用superpowers:subagent-driven-development执行这一独立任务。已授权教学范围的取景、分段和措辞由PM细化，不重复索取玩法批准。源码及测试由执行者编写，中文注释说明坐标、显示时序、单位与客户端演示边界。

## 两幕合同

| 故事板 | 画面与正文要点 |
| :--- | :--- |
| `fuel_sintering_operation` | 炉体下方燃烧室直接接触且有效供热，先让炉/热源都可见，再展示生芯块投入、加工和烧结芯块生成；用简短步骤展示断热暂停、恢复继续。说明“在底部供热，从顶部投入生芯块”“每个生芯块烧结为一个烧结芯块”“断热会暂停烧结，恢复加热后继续”，工时如出现只能按现有400有效tick说明，不能把教学压缩时长当工时。 |
| `fuel_sintering_automation` | 紧凑有效物流布置：从顶部输入生芯块，从可见水平面用漏斗取烧结芯块到可见承接处，明确四侧均能取成品；实际图标/物料变化与方向一致。随后简短展示核换热器可作为底部热源，冷热回路及换热器本体可见；不重复已验收换热器串联/成本/多用途课堂。燃烧室与换热器不同时占同一格，不能宣称热液缺失仍能加热。 |

允许执行者按真实API调整上述教学段落的具体时序和画面，目标与玩法不变。顶部物流选用真实可工作的Create溜槽、漏斗或漏斗容器组合，先核对当前能力和方向，不为画面创造水平输入能力。展示进度/库存只通过Ponder临时方块实体及既有API/NBT；不可复制加工引擎或新增同步。

## 视觉与文案要求

- 基座最低y=0、设备最低y≥1；炉体、热源、进料器和出料承接处全部落在有效画面中央，不使用巨大偏置平台。镜头高度/缩放覆盖顶部进料和底部热源，并留出标题、字幕、按钮与进度条空间。
- 热源讲解时移开必要遮挡物或有目的转镜头，炉体与下方设备同时可辨识；高亮必须指向实际显示的实体。热源放在地台里的方案不可用。
- 正文一段一段播放，持续时长包含淡出生命周期，后一段至少在前段完全退出10tick后出现。显示控制提示与物品图标不能挤占正文或设备核心区域。
- 文案只讲设备操作和特有规则，避免“画面仅作示意”、创造马达解释、管道/漏斗基础常识及开发说明；不加重复停机与排查页。中英文与Java fallback同序同义，不写未经实现的机制。

## 精确允许写集

- `src/main/java/com/iksxh/create_nuclear_industry/ponder/FuelSinteringPonderScenes.java`（新增，两幕及专属私有辅助方法）。
- 同目录`P1PonderPlugin.java`：只新增两幕ID及`fuel_sintering_furnace`绑定，更新相关中文注释，不改变其他入口/顺序。
- `src/main/resources/assets/create_nuclear_industry/ponder/fuel_sintering_{operation,automation}.nbt`。
- `tools/ponder/fuel_sintering_scenes.py`（新增，仅生成/校验上述模板，可只读引用现有生成工具）。
- 双语`src/main/resources/assets/create_nuclear_industry/lang/{zh_cn,en_us}.json`：仅新增`ponder.fuel_sintering_*`键，保留其他键及顺序，不全文件格式化。
- `src/test/java/com/iksxh/create_nuclear_industry/FuelSinteringPonderContractTest.java`（新增，少量有意义合同检查）。
- `docs/reviews/2026-10-10/fuel-sintering-ponder-06/IMPLEMENTATION.md`（执行者唯一交付报告）。
- 原始验证证据`build/reports/extension/DEVICE-PONDER-06-SINTERING/`，原始失败和成功均保留；审查者只写同交付目录`REVIEW.md`。

正式加工逻辑、配置、配方、能力、模型/纹理/动画、其他故事板与模板、现有测试、构建脚本、全局文档均只读。如果需要越界才能做到真实演示，先报告PM，不能自行扩大写集或缩减目标。

## 执行与必要验证

- [x] 核对真实源码、注册ID、Ponder API、NBT和能力方向，登记HEAD、技能及既有脏文件。3项实际断言失败red已记录，首次InterruptedException基础设施失败另存，不计red。
- [x] 完成两幕、确定性模板/生成器、双语与入口。最终读取NBT验证与相同输入压缩字节一致，最低热源y=1、炉体y=2、顶部输入和水平输出正确。
- [x] 最终定向`test --tests '*FuelSinteringPonderContractTest' assemble`退出0，实际JUnit3/3、0失败/错误/跳过；模板、场景class与双语进入JAR。首版green后调整窗口溜槽/上方投料容器，按实际变更必要复验；未运行全量、GameTest或旧功能复测。
- [x] 一次独立合并规格+质量窄审通过，读取本地API及已有证据，没有重跑Gradle或修改实现。
- [x] PM核对准确写集、冻结8路径SHA/最终XML/制品及报告；10路径源码/报告候选`e76c8d9`已提交，集中[播放入口](../../reviews/2026-10-10/fuel-sintering-ponder-06/CANDIDATE.md)已整理。main仅登记候选，不含新教学实现。
- [x] 用户逐幕播放确认热源、进出料、暂停/恢复及取景/文案，main净教学整合`57763f8`，8路径Git内容及冻结源SHA一致；不重复跑测。下一台进入07准备，其他主线暂缓。

**冲突扫描：** 插件、语言与两个模板交同一执行者顺序维护；美术树禁止Ponder/语言修改，本批不进入CT/素材写集。未发现新玩法取舍或前置缺失。保留已验收教学门关闭状态。

**PM交付收据：** 执行者`/root/fuel_sintering_ponder_impl`、独立审查者`/root/fuel_sintering_ponder_review`；报告见[实施](../../reviews/2026-10-10/fuel-sintering-ponder-06/IMPLEMENTATION.md)及[审查](../../reviews/2026-10-10/fuel-sintering-ponder-06/REVIEW.md)。源码候选与最终冻结证据一致，JAR SHA256 `0aead57d60b796e17c461a98bdeee2e7721d413a6cffbabcbe45af3986ab5206`；PM复制冻结JAR并复核哈希，没有重构建。用户本台播放门已按[验收](../../reviews/2026-10-10/fuel-sintering-ponder-06/ACCEPTANCE.md)关闭；既有日志、pycache及独立美术差异保留，美术视觉门不由本批覆盖。
