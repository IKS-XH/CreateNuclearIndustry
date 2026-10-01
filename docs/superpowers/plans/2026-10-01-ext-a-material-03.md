# EXT-A-MATERIAL-03 / EXT-ART-05：粉末制钢实施计划

> PM 使用 writing-plans、subagent-driven-development 和完成前验证流程；按用户既有自动派发授权执行。执行者不得派发、修改核心文档、改变任务状态或进行任何 Git 写。技能中的通用提交、再审批和自主改需求步骤服从本仓库授权与人工门。

**目标：** 玩家用粉碎轮制铁/碳粉，无热搅拌成钢粉，经熔炼成钢锭并压成现有维修钢板。
**架构：** 五个普通物品身份、原生数据配方与通用标签，复用 Minecraft/Create 机器和 JEI 自动识别；不新增配方类型、机器逻辑或客户端状态。
**需求：** [已确认路线与参数](./2026-10-01-mainline-material-03-proposal.md)、[配方5.4](../../recipes.md#54-钢材陶瓷与建筑材料)、内容清单3.2。用户已答复“采用这组推荐参数”。
**工作区：** `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支 `codex/ore-acquisition`；功能基线 `27bf103`，PM同步本卡后记录开工提交。主工程既有 `.vscode/launch.json` 不动。
**版本：** MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6；保留 JEI、许可证、构建和依赖版本。
**状态：** 用户于2026-10-01确认完整客户端清单通过，并要求普通“合金钢”简称“钢”；按[03A收尾卡](./2026-10-01-ext-a-material-03a.md)处理名称及剩余回归。此前265项JUnit/build、本批14项及实际标签重载通过，完整155项GameTest因框架异常未完成，不将人工通过作为自动回归豁免。详见[本批记录](../../reviews/2026-10-01/material-03/README.md)及[客户端清单](../../reviews/2026-10-01/material-03-client.md)。开工提交为`9097338`。

## 冻结合同与评审重点

- 新增普通64堆叠物品 `iron_dust`（铁粉 / Iron Dust）、`coal_dust`（煤粉 / Coal Dust）、`charcoal_dust`（木炭粉 / Charcoal Dust）、`steel_dust`（钢粉 / Steel Dust）、`steel_ingot`（钢锭 / Steel Ingot）。复用 `ModItems.STEEL_PLATE`，不改原钢板ID、图或维修逻辑。
- 三条粉碎轮：1铁锭→1铁粉、1煤→1煤粉、1木炭→1木炭粉，`processing_time=100`。铁锭输入 `c:ingots/iron`；煤/木炭原料沿各自原版身份，不能用同时包含两者的宽标签。磨石保留原Create染料。
- 两条 `create:mixing`：4项 `c:dusts/iron` 加1项 `c:dusts/coal` 或 `c:dusts/charcoal` →本模组5钢粉，`processing_time=100`，无热要求；重复4项 Ingredient 表示四份铁粉，不能用不生效的 ingredient count。
- 两条 cooking：1份 `c:dusts/steel`→本模组1钢锭；`minecraft:smelting` 200 tick、`minecraft:blasting` 100 tick，经验均0.1。真实熔炉/高炉与 Create 熔岩风扇批量熔炼均应成功，风扇沿锁定Create规则，不新增自定义风扇配方或承诺炉子经验/时长等同风扇。
- 一条 `create:pressing`：`c:ingots/steel`→本模组1原有钢板，不自设压片时长。上述8条配方均无副产物，不新增钢粒/储存块/逆向回粉或铁粉直接烧回铁的路线。
- 四粉加入各自 `c:dusts/<material>` 与父 `c:dusts`，钢锭加入 `c:ingots/steel` 与父 `c:ingots`；`replace:false`。不将 `create:crushed_raw_iron` 加入铁粉标签，不能直接以铁锭或粉碎粗铁混钢。
- 实际服务端扣料和产物为权威；无动力/材料不足时不提前扣料，恢复条件后只产正确批次；错误碳源/金属粉不匹配；外部等价具体标签成员可加工。
- 评审重点：工作盆堆叠铁粉能消耗精确4件；输出受阻不吞料或重复生成；热级NONE是无需加热而非禁止加热；Create粉碎轮不回退染料；旧55张游戏图和旧8冷却剂原字节保留。

## A：功能与真实机器验证 EXT-A-MATERIAL-03

复用 `lead_tin_processing`，已有真实机器/运行隔离上下文，采用原标准模型处理多文件接入与GameTest。先读取最新 AGENTS、治理、本卡、已批准方案、差异预检；实际读取应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`。中文职责说明与非显然测试控制说明按 AGENTS。

**唯一允许写集：**

- `src/main/java/com/iksxh/create_nuclear_industry/content/BasicMaterialContent.java`、`content/ModCreativeTabs.java`：增加五物品与创造页，不修改旧身份。
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionSteelProcessingGameTests.java`；`src/test/java/com/iksxh/create_nuclear_industry/P1DataContractTest.java` 仅为 `pressing/steel_plate.json` 精确开例外，其他受保护P1配方继续拒绝。若需新单元契约测试，仅 `ExtensionSteelDataContractTest.java`，不得复制JSON断言充数。
- `src/main/resources/assets/create_nuclear_industry/models/item/{iron_dust,coal_dust,charcoal_dust,steel_dust,steel_ingot}.json`；`lang/{en_us,zh_cn}.json` 仅五项名称。PNG由B写。
- `src/main/resources/data/c/tags/item/dusts.json`、`dusts/{iron,coal,charcoal,steel}.json`、`ingots.json`、`ingots/steel.json`。
- `src/main/resources/data/create_nuclear_industry/recipe/crushing/{iron_dust,coal_dust,charcoal_dust}.json`；`mixing/steel_dust_from_{coal,charcoal}.json`；`smelting/steel_ingot_from_dust.json`、`blasting/steel_ingot_from_dust.json`、`pressing/steel_plate.json`。
- 报告、临时隔离配置与验证工具仅 `build/reports/extension/EXT-A-MATERIAL-03.md` 及同名目录。禁止改正式构建/JEI、旧测试、其他资源、`run/`、用户存档、旧报告和核心文档。实际需要新增写集先报PM。

**执行与验证：**

- [x] 先编写本批有意义的失败测试：运行时配方、真实机器tick、精确投入产出/错误形态与标签接入；RED中13项新路线因身份/配方缺失失败，原染料检查保留。
- [x] 在报告目录准备本轮独立RED/GREEN gameDirectory；复用02A修正的 `neoForge.runs` 模型配置方式，不仅设置JavaExec workingDir。启动前断言模型和任务实际目录均位于03报告目录。
- [x] A先准备测试及隔离配置，B结束前不运行Gradle或加入功能。PM放行独占窗口后RED→补功能→GREEN，所有Gradle串行；GREEN异常与后续专项结果分别记录。
- [x] 真实粉碎轮验证三输入；煤/木炭磨石原染料配方保持。真实无热动力搅拌盆验证两入口4+1→5、多件堆叠、少铁/缺碳、错误形态、动力中断恢复与输出受阻；输入保留断言按独立审查补齐后通过。
- [x] 真实熔炉、高炉、熔岩风扇和压片机验证通过；精确200/100、0.1与tag合约从加载配方检查。风扇证明单件实际入口，不扩为大堆叠吞吐或经验验收。
- [x] 隔离普通服以外部数据包追加等价粉末，实际 `/reload` 后false→true→false；true阶段替代物真实炉加工得到1钢锭。保存重进体验仍保留人工门。
- [ ] 运行 `test build --rerun-tasks --max-workers=1` 和带03隔离init的 `runGameTestServer --rerun-tasks --max-workers=1`，保留命令、工作目录、实际游戏目录、PID、断言/退出结果；最终资源与JAR一致，60张游戏PNG。

开工与收尾比较默认 `run/`、`run/saves` 清单/哈希，保留跟踪的 `logs/debug.log`、`logs/latest.log` 原副本供PM按范围恢复。禁止修改/迁移/删用户世界，禁止杀用户客户端或Gradle守护进程。若GameTest断言通过后保存挂起，有限等待后只结束本任务精确PID，报告通过断言与非零退出的区别，不把挂起当成功退出。

## B：五种必要素材 EXT-ART-05

复用 `lead_tin_nugget_assets` 高速模型。读取AGENTS、本卡、`tools/art-assets/README.md`，实际使用modding/testing及 `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`。采用既有16×16整数矩形像素SVG与确定性PNG导出，不使用生图模型。

**唯一允许写集：**

- `tools/art-assets/sources/item/{iron_dust,coal_dust,charcoal_dust,steel_dust,steel_ingot}.svg`；相应 `generated/item/` 五PNG和游戏 `textures/item/` 五PNG。
- `tools/art-assets/{manifest.json,palette.json,pipeline.py,verify.py,README.md}` 仅接入这五新项、计数和本批证据出口；`preview.{png,html}`、`previews/` 的必要重排输出。不得修改baseline及其51项历史身份、旧SVG、既有55游戏PNG、模型/语言/注册。
- 报告与临时工具仅 `build/reports/extension/EXT-ART-05.md` 及同名目录。正式其他工具、功能代码、核心文档与Git禁止写。

**外观及验证：**

- [x] 四粉沿既有粉堆语汇：铁粉亮灰、钢粉冷灰、煤粉近黑、木炭粉棕灰；钢锭修为短厚平顶金属锭。离线预览与审查通过，实际游戏外观未验收。
- [x] 新源稿在既有严格SVG子集内；只向新项配置需要的色板，旧色板与映射不变。16×16 RGBA、alpha仅0/255、物品外圈透明，游戏模型与名称由A接入。
- [x] 固定历史51项不变，新增白名单由4项扩至9项；游戏60PNG，manifest含青金石工具候选共61项。非法SVG/路径/映射/preserve拒绝及失败不写保护验证通过，默认输出写05不覆盖历史。
- [x] 保存开工55图哈希，两次导出安装一致，既有55图（含冷却剂）字节不变；实际查看原尺寸和放大五新图/钢板对照。工具验证退出0，交付后冻结资源。

所有Python用 `-B` 或禁用字节码。不可运行Gradle/游戏。临时坏输入试验仅在本素材独占窗口执行，finally恢复原文件，不遗留测试变更。

## PM集成与人工门

A、B资源与代码写集分离，B独占美术导出，A独占后续Gradle；先B完成，再A运行及最终打包。独立审查检查批准合同、实现差异、测试真实性、隔离和旧资源保持；执行者不以自评代替审查。

人工清单包含：三制粉入口、煤/木炭磨石染料、两种无热混粉、数量与动力恢复、熔炉/高炉/Create风扇熔炼、钢锭压板与原维修身份、JEI配方和用途、五新物品外观、保存退出重进。PM提供明确候选路径与启动命令，在用户确认前本批不合入main，不推进设备或其他材料批。

| 计划检查 | 结论 |
| :--- | :--- |
| A / B共用物品ID | 同五ID；A写模型/语言，B写对应纹理，无文件重叠 |
| A / B共用资源目录 | 导出和Gradle有先后门，B结束再打包 |
| A合同与测试 | 8配方、四粉/一锭及原钢板；真实机器/重载验证，不扩核材料 |
| B合同与测试 | 5新增图、55旧图原字节、60游戏/61清单/51历史项 |

## 执行记录

- 开工前已确认参数；技术差异预检完成，未运行游戏，原始报告保留当时待参数的语境。
- A/B于 `9097338` 启动；A已回报实际HEAD及干净候选。PM复核并纠正预检中风扇SMOKING解释：它用于同输出排除，不是优先产出；[更正预检](../../reviews/2026-10-01/material-03/EXT-A-MATERIAL-03-REVISION-PRECHECK.md)记录锁定源码依据。
- B修正总览末行裁切及钢锭轮廓后交付；PM已查看最终五物品/钢板对照并独立核对55旧图哈希，差异0，游戏60图。B的导出器验证通过，已冻结资源；A已获准运行14项新测试及实现/回归。
- B独立审查派给 `native_lead_tin_audit` 高速执行者；只读本卡B、EXT-ART-05报告、`build/reports/extension/EXT-ART-05-review-inputs.md`及最终文件。唯一写集为候选 `build/reports/extension/EXT-ART-05-REVIEW.md`；禁止运行Gradle、游戏、导出/verify、Git写或其他文件修改。须分别报告合同符合性和实现质量，读取并应用modding/testing/resource-pack技能。审查不代替客户端视觉验收。
- A及整批独立审查：真实机器扣料、堵塞恢复、测试证据与原生配方接入交由独立高级模型复核。唯一写集为候选 `build/reports/extension/EXT-A-MATERIAL-03-REVIEW.md`；读取本卡、批准方案、`EXT-A-MATERIAL-03-review-inputs.md`、A/B报告及审查证据，应用modding/testing技能。只读最终文件，不运行测试/导出、改实现、Git写或派发。自动证据尚未完成时先报告静态结论及未决验证门，证据齐备后补最终结论，不先行宣告验收。
- 全量GREEN及一次独立目录重试均在 `GameTestInfo.tickInternal` / fastutil iterator发生异常，未取得155项完成证据；未修改旧测试或生产机制绕过。普通服逐项新14项及真实重载通过，只作为本批专项证据，全量回归仍未完成。普通服无在线玩家时内置成功报告静默，执行者在新测试全部断言及 `helper.succeed()` 后添加明确标记，独立审查确认失败会抛出、不能继续输出成功。
- 首次交付时暂停在人工门；用户现已确认完整客户端清单通过。名称简称与剩余全量回归转03A收尾，在完成整合前不派发下批零件或设备。
- A/B及最终独立审查已冻结，R1输入保留断言整改闭环；PM核对最终构建/制品、默认run保持及日志恢复后，将57个实现/素材文件保存为候选提交`a1353dd`。报告与原始证据归档到上述本批记录，未改main功能或既有启动配置。
