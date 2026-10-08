# DEVICE-PONDER-05-EXCHANGER 实施记录

**日期：** 2026-10-09

**执行目录：** `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`

**分支与基线：** `codex/ore-acquisition`，`f650aa7`

**状态：** R2候选已实现且定向验证通过，等待对应差异复核与客户端播放；未提交。

## 实施范围

为 `nuclear_heat_exchanger` 增加四条独立 Ponder 故事线：独立供热与同向直列、5×5×5 最小锅炉内置、工作盆/燃料烧结炉持续供热、普通蒸汽冷凝回水。所有演示只操作 Ponder 客户端临时世界，没有改动正式换热账本、流体能力、配置或设备注册。

独立供热场景将两台朝北的换热器放在 `(7,1,5)` 与 `(7,1,6)`。北侧 `(7,1,1)` 是冷液出口罐，南侧 `(7,1,10)` 是热液入口罐；两台上方放 Create 储罐，入口/出口箭头分别指向后侧和前侧。锅炉场景采用 `x/z=5..9、y=0..4` 的合法最小壳体，换热器在底层 `(7,0,7)`，隔层 `(7,2,7)`，热液口位于底边非角点 `(7,0,5)`，冷液口位于隔层侧面 `(6,2,5)`。隐藏正面壳格前先完整加载锅炉和管路，讲解后恢复壳体。

加工场景保留同一台换热器与冷热管线，先显示顶部工作盆、搅拌器和轴，再通过 Ponder 1.0.82 的 `WorldInstructions.setBlock` 将顶部设备换成燃料烧结炉。正文说明持续耗热、默认 `4 mB/t`，以及热液耗尽或冷液回流受阻后下一 tick 停热。冷凝场景让蒸汽从北向换热器后侧进入、冷凝水从前侧排出；顶部依次显示雪块、冰、浮冰、水源和蓝冰。中英文文案说明默认 `1:1` 回水、流水无效、有限冷源融水后水源最终蒸发，以及蓝冰不会消耗。

所有正文都由同一辅助方法显示，持续时间后额外等待 20 tick，以覆盖 Ponder 1.0.82 正文额外 10 tick 的生命周期并留出淡出间隔。镜头采用居中模板和有限尺寸底座；锅炉端口与冷热管线保留在剖面可见侧。客户端实际缩放、遮挡和字幕播放仍需人工确认。

## 技能与版本

- 已阅读并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：先按 `gradle.properties` 核对 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82 与 Flywheel 1.0.6；场景职责限于客户端临时世界。
- 已阅读并应用 `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：使用一个定向 JUnit 合同测试验证四幕入口、模板布局、端口方向、双语键和场景切换，不把它扩展成复制每句文案的测试。
- 已阅读 `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：沿用模组 `assets/<namespace>/lang` 与 Ponder 资源路径，只添加双语文本和 NBT 模板；本批不制作或修改贴图、模型、声音或独立资源包，因此未运行资源包校验器。
- 从实际 Ponder 1.0.82 依赖 JAR 检查 `WorldInstructions` API，确认存在 `setBlock(BlockPos, BlockState, boolean)`、`showSection`、`hideSection` 和 `modifyBlockEntity`，并据此实现临时世界设备替换、剖面及 Create 储罐展示。

## 修改文件

- 新增 `src/main/java/com/iksxh/create_nuclear_industry/ponder/HeatExchangerPonderScenes.java`；在 `P1PonderPlugin.java` 仅新增换热器四幕绑定。
- 新增 `src/main/resources/assets/create_nuclear_industry/ponder/nuclear_heat_exchanger_{heating,boiler,processing,condensation}.nbt` 与 `tools/ponder/heat_exchanger_scenes.py`。
- `zh_cn.json`、`en_us.json` 仅增加本批四个故事线的标题和正文键。
- 新增 `src/test/java/com/iksxh/create_nuclear_industry/HeatExchangerPonderContractTest.java`。

## 验证证据

| 检查 | 结果 |
| :--- | :--- |
| NBT 生成器运行与 gzip 往返 | 4/4 模板通过；尺寸为供热 `15×9×13`、锅炉 `15×7×13`、加工 `15×9×13`、冷凝 `15×8×13`。生成输出见 `build/reports/extension/DEVICE-PONDER-05-EXCHANGER/generator.log`。 |
| `.\gradlew.bat test --tests '*HeatExchangerPonderContractTest'` | 通过，3 tests、0 failures、0 errors、0 skipped。原始输出见 `build/reports/extension/DEVICE-PONDER-05-EXCHANGER/test.log`。 |
| `.\gradlew.bat assemble` | 通过；Java 编译与资源处理为最新，打包任务成功。原始输出见 `build/reports/extension/DEVICE-PONDER-05-EXCHANGER/assemble.log`。 |
| `git diff --check` | 本批源文件无空白错误；命令同时报告既有 `logs/debug.log` 与 `logs/latest.log` 各有一条新日志行尾空白，未编辑或清理日志。 |

增量 JAR `build/libs/create_nuclear_industry-0.1.0.jar` 的 SHA-256：`6BE78E490E8A4C21C7C3DFBA2F17E2A406018835E601F0600C70073C08E3FC89`。

本批为纯教学改动，未运行 GameTest、全量测试、旧存档专项或客户端播放。Ponder 实际资源加载、四幕顺序、不同缩放下的端口可见性、文字遮挡和方块渲染仍由独立审查及用户客户端播放验收确认；静态检查不代替该人工门。

## 交付状态

改动未提交，执行者未执行 Git 写操作。开始时已存在的 `logs/debug.log`、`logs/latest.log` 和三个 `__pycache__` 目录均予以保留。工作过程中还观察到 `AGENTS.md`、`docs/art/README.md`、`docs/content-catalog.md`、两份现有验收文件及活动任务卡出现了本任务写集之外的变更；本执行者未编辑这些文件，交由项目经理按其文档权限核对。

## R1整改与复验（2026-10-09）

按独立审查与PM补充核查修正本批教学候选，未修改正式热工、流体能力、锅炉规则、配置、模型或旧教学。

- 独立供热幕现将2×2 Create储罐组放在两台直列换热器正上方：罐组为`x=6..7,z=5..6,y=2`，东侧两罐分别对应下方的两台换热器；两台`create:steam_engine`位于西侧`(5,2,5/6)`，使用Create已存在Ponder模板中的合法`face=wall,facing=west`。移除了西侧烈焰人方案，避免把热源表述混成Create燃烧室供热。画面写明下方换热器为上方锅炉供热，并分开说明锅炉热量随实际热液流量变化。
- 核对Create 6.0.10-280的原生`steam_engine.nbt`及`FluidTankBlockEntity`接口。Ponder虚拟世界不执行Create服务端组罐扫描，因此场景加载罐组后通过方块实体API将四格统一绑定到`(6,2,5)`，设置宽2、高1；液位仅写入控制罐，确保储罐组作为一个Create锅炉展示。蒸汽机仍与罐壁直接相邻。
- 锅炉幕冷液罐Java常量修正为`(6,2,1)`，与模板实体坐标一致。锅炉模板的隔层`y=2`内部九格全部补齐：中央再加热段与周围八格锅炉外壳。教学先完整加载5×5×5结构，再暂隐前壁和隔层内壳供观察，最后恢复；测试检查隔层九格、底面/外壳及对应端口。
- 加工幕展示热液从1000降到0、冷液按同量从0升到1000，分别经过空工作盆和空烧结炉阶段；之后展示生芯块投入、炉体亮起、烧结燃料芯块产出，耗尽后停止并在恢复冷热液循环后继续。所有物料和状态只由Ponder临时世界API演示。
- 冷凝幕按可读时序展示雪块、冰、浮冰、水源及水源耗尽为空气，蒸汽与回水按1:1分段变化；再单独展示蓝冰不消耗、冷凝水等量增加。各状态保持35–45 tick，不以10 tick闪切。玩家文本统一用显示名“蒸汽”，明确雪/冰/浮冰直接接触即可冷凝、融水水源会最终蒸发、蓝冰不消耗。
- Java标题/正文fallback与中文资源按场景顺序一致，英语资源语义对应，移除末尾空格；不把配置字段加入玩家正文，默认值仅保留4 mB/t。

**R1验证：**

| 检查 | 退出码与结果 | 原始证据 |
| :--- | :--- | :--- |
| 四模板生成及NBT往返 | `generator-r1b.exit=0`；四模板全部通过，锅炉229格 | `build/reports/extension/DEVICE-PONDER-05-EXCHANGER/generator-r1b.log` |
| 定向合同测试（首次R1） | `test-r1.exit=1`；3用例中2项断言失败，发现测试预期未同步新增的合法换热器边框例外及扩展剖面隐藏集合；失败输出与XML保留 | `test-r1.log`、`test-r1.exit`、`test-r1-failed.xml` |
| 定向合同测试（修正后） | `test-r1b.exit=0`；3 tests、0 failures、0 errors、0 skipped | `test-r1b.log`及`build/test-results/test/TEST-com.iksxh.create_nuclear_industry.HeatExchangerPonderContractTest.xml` |
| 增量`assemble` | `assemble-r1.exit=0`；构建成功 | `assemble-r1.log`、`assemble-r1.exit` |
| 增量JAR SHA-256 | `87D5117086B4577E66DD4E35021AD300AB3258DE0ADBA3C9E1AD6AFD7CCDAFA9` | `build/libs/create_nuclear_industry-0.1.0.jar` |

上述为静态/自动证据，不替代用户客户端逐幕播放。没有运行GameTest、全量测试或旧存档测试。全部改动保持未提交；未触碰已有`logs/debug.log`、`logs/latest.log`及三个`__pycache__`目录。交付冻结，等待独立复审和本台客户端播放验收。

## R2整改与复验（2026-10-09）

按任务卡同批R2完成最小可见性和文案整改；未改NBT、生成器或正式设备行为。

- 冷凝幕将顶部`CONDENSE_SOURCE`加入初始`showSection`集合，并用中文代码注释说明Ponder临时世界后续方块替换只在已显示区域可见。雪块、冰、浮冰、水源、空气和蓝冰继续在同一坐标替换。
- 加工幕正文现在依次为：工作盆持续供热、空炉已持续受热并展示生芯块加工、缺液立即停热、恢复液路后继续。删除原第三段重复烧结/等量变化说明，并明确生芯块投入发生在空炉受热之后、不触发开始供热。正文与中英文键从4段重新顺序对应，删除新加的`text_5`。
- 独立供热首段收束为后侧进热液、前侧出冷液、换热器为上方储罐锅炉供热；删去储罐和蒸汽机组成Create锅炉的基础描述。画面和锅炉布局保持。
- 既有三用例中，场景正文数量从5同步为4，并增加对顶部冷源属于首个可见section的断言；无新增测试套件。

**R2验证：**

| 检查 | 退出码与结果 | 原始证据 |
| :--- | :--- | :--- |
| 四模板生成 | 未重跑；R2未修改NBT/生成器，复用`generator-r1b.log`与`generator-r1b.exit`（0） | `build/reports/extension/DEVICE-PONDER-05-EXCHANGER/generator-r1b.log` |
| R1b测试记录 | XML已复制留存，3 tests、0 failures | `build/reports/extension/DEVICE-PONDER-05-EXCHANGER/test-r1b.xml` |
| 定向合同测试 | `test-r2.exit=0`；3 tests、0 failures、0 errors、0 skipped | `test-r2.log`、`test-r2.exit`、`test-r2.xml` |
| 增量`assemble` | `assemble-r2.exit=0`；构建成功 | `assemble-r2.log`、`assemble-r2.exit` |
| 增量JAR SHA-256 | `24B30A7E9B8485FBB3BEF46A70B93BBD7D78E4B902D89D0DA0764E0946D2EC47` | `build/libs/create_nuclear_industry-0.1.0.jar` |

R1首轮/R1b报告、历史哈希、日志和失败经过均保留。R2自动验证通过不代表客户端人工验收；本候选仍未提交，冻结等待原审查者复核差异及用户客户端播放。
