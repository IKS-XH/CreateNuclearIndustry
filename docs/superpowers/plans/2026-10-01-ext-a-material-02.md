# EXT-A-MATERIAL-02：铅锡粗矿熔炼与压板实施计划

> 执行方式：PM 按 `superpowers:writing-plans` / `subagent-driven-development` 拆卡并审查；执行者禁止 Git 写、派发、核心文档修改和自行验收。用户已授权自动派发，已确认的参数不重复询问。

**状态：** 已由02A完成整改、人工验收并合入main，见 [收尾记录](../../reviews/2026-10-01/material-02-acceptance.md)。用户确认完整复测清单通过，水洗副产物暂缓；具体整改合同见 [02A/03A/04卡](./2026-10-01-material-02a-feedback.md)。下文为首轮实现合同与证据，原“粉碎料不得直熔”限制已被取代，不可继续据此拒绝补齐路线。
**目标：** 铅锡粗矿经熔炉/高炉产锭，Create 压片机产板；四个物品在同一候选中可见、可获取、支持通用标签。
**架构：** 只增加注册、原生数据配方和资源；使用 Minecraft 与 Create 既有机器，不新增设备逻辑、JEI 插件或自定义配方类型。
**位置：** 复用 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`、`codex/ore-acquisition`；PM 将本卡同步后记录精确基线。用户主工程 `.vscode/launch.json`、存档和现有手测世界不得改动。
**技术栈：** Minecraft 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82 / Flywheel 1.0.6；保留 JEI 开发专用配置和 All Rights Reserved。

## 首轮历史合同（当前执行已由02A扩展）

本节只解释首轮六配方、四物品和53图的历史证据，不再是当前派发边界。当前已批准补矿石/粉碎料直熔、水洗9粒、9粒↔锭及两粒素材，详见 [02A卡](./2026-10-01-material-02a-feedback.md)；当前目标为六物品、55张游戏图。下列“粉碎料/粒不接收”只对原有粗矿直熔及压板那六条配方成立，不能解释为全局禁用这些输入。

- 四个既定 ID：`lead_ingot`（铅锭）、`tin_ingot`（锡锭）、`lead_plate`（铅板）、`tin_plate`（锡板），各普通可堆叠物品。现有钢板、矿物、燃料和流体注册不改。
- 铅、锡各两条原生 cooking 配方：输入各自 `c:raw_materials/<metal>` 标签 1 件，产本模组对应锭 1 件；熔炉 200 ticks、高炉 100 ticks，经验均 0.7，无其他产物。
- 两条 Create `pressing`：各自 `c:ingots/<metal>` 标签 1 件产本模组板 1 件，无副产物，沿原生压片 JSON 不声明固定 `processing_time`。配方 ID 使用 `smelting/<metal>_ingot_from_raw_<metal>`、`blasting/<metal>_ingot_from_raw_<metal>`、`pressing/<metal>_plate`。
- 物品加入 `c:ingots/lead,tin`、`c:plates/lead,tin`，汇总标签通过嵌套引用接入 `c:ingots` / `c:plates`，不覆盖其他提供者。输入接受对应等价标签成员，输出固定身份；默认标签下错误材料、粗矿块、粉碎粗矿、粒、铀不被本批新配方接收；不另建绕过通用标签的硬编码黑名单。
- 首轮未包含粉碎料直熔、洗矿、锭粒压缩；这三项已在02A扩展，原排除约束失效。钢材、设备、反应堆配方仍后置；不修改 Create 原生配方或已批准矿物生成/粉碎/掉落。
- 现有 51 PNG（含8旧冷却剂）原字节保持；复用铅锡锭已批准 PNG，只新增两张板材 SVG 与导出 PNG，游戏总数增至53。不得调用生图模型。

## 必读、技能和写集

两位执行者先读主工程最新 AGENTS、治理、参数页、本卡，完整读取 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`；资源执行者另读 `minecraft-resource-pack/SKILL.md`。实现者按 `test-driven-development` 合理安排可执行的失败用例，不能用无意义编译错误或镜像 JSON 的测试冒充行为覆盖。版本与数据路径以锁定依赖为准。手写代码职责和非显然算法注释用中文。

### A. 注册、配方与机器验证

**允许修改/新增仅：**

- `src/main/java/com/iksxh/create_nuclear_industry/content/BasicMaterialContent.java`：四个物品的注册层，提供对应 DeferredItem 及 `register(IEventBus)`。
- `src/main/java/com/iksxh/create_nuclear_industry/CreateNuclearIndustry.java`：只接入上述注册；`content/ModCreativeTabs.java`：只追加四种物品。
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionBasicMaterialGameTests.java`：真实配方管理器与机器验证；可复用已有空结构，不扩改 P1 测试。
- `src/main/resources/assets/create_nuclear_industry/models/item/{lead_ingot,tin_ingot,lead_plate,tin_plate}.json` 与 `lang/{en_us,zh_cn}.json`，只补对应四种身份。
- `src/main/resources/data/c/tags/item/{ingots,plates}.json`，`ingots/{lead,tin}.json`，`plates/{lead,tin}.json`。
- `src/main/resources/data/create_nuclear_industry/recipe/smelting/{lead_ingot_from_raw_lead,tin_ingot_from_raw_tin}.json`、`blasting/` 下相同两文件、`pressing/{lead_plate,tin_plate}.json`。
- 报告与临时验证入口仅 `build/reports/extension/EXT-A-MATERIAL-02.md` 及同名目录；可在此放隔离 GameTest init 脚本/测试数据包/日志，不改正式运行目录或构建配置。

**步骤与审查重点：**

- [x] 核对实际注册和锁定配方 schema，先写能发现配方缺失/错输入/错输出的真实 GameTest；记录失败原因，再实现最小注册与六配方。
- [x] 正常熔炉与高炉均通过实际方块实体 tick 加工铅/锡，核对消费、输出、两种时长、经验声明与阻塞后不增殖/丢料；不只调用 Recipe.assemble。
- [x] 真实有动力的 Create 压片机与置物台接收两种锭并产各自板。覆盖无动力不加工、恢复动力继续；使用实际库存/加工路径，不以手动调用处理辅助方法替代设备运行。
- [x] 通过隔离测试标签追加（只在报告目录）验证等价粗矿/锭匹配、错误形态拒绝及重载后仍生效；不能把测试标签模拟称为真实第三方模组联调。
- [x] 标签替代测试若重载涉及共享全服状态，使用隔离运行避免污染其他 GameTest；默认运行不得加入测试标签。现有玩法 Java/数据保持不变。
- [x] 美术交付后由 PM 放行串行执行 JUnit/build、GameTest 和制品检查。GameTest 若断言已全过但保存挂起，保留汇总/退出码与日志；只允许结束本任务创建且 PID/命令明确的测试进程。若断言失败先排查，不无限重试或升级依赖。
- [x] 报告记录基线、命令/退出码、实际技能、正常/拒绝/阻塞结果、未完成项；不进入用户世界，不自行宣布客户端通过。

### B. EXT-ART-03：两张板材 SVG

**允许写集仅：**

- `tools/art-assets/sources/item/{lead_plate,tin_plate}.svg`、`generated/item/{lead_plate,tin_plate}.png`。
- `tools/art-assets/{manifest.json,palette.json,pipeline.py,verify.py,README.md}`，只支持这两个明确新路径；保留既有白名单、非法输入拒绝、8项冷却剂保留和旧基线语义。`baseline/`、`baseline.json`、现有源稿/游戏图不得改。
- 必要更新 `tools/art-assets/preview.{png,html}`、`previews/`，允许增加板材对照，不改已有导出图本身。
- `src/main/resources/assets/create_nuclear_industry/textures/item/{lead_plate,tin_plate}.png`。
- 报告及临时检查脚本仅 `build/reports/extension/EXT-ART-03.md` 与同名目录。

- [x] 沿已批准16×16整数像素SVG、有限色板、左上光照、0/255透明度，绘制铅板/锡板；可辨别两种金属，也能与钢板、对应锭区分。
- [x] 扩展清单/独立白名单只增加2项，不能把历史基线伪造为53张。导出器新路径未注册基线时应明确显示新增，不取消旧51项完整性核对。
- [x] 两次导出/安装哈希相同，旧51游戏PNG保持不变；非法路径、preserve声明、映射和SVG仍拒绝且失败不写出。四张既有样稿保持不变。
- [x] 实际查看对照预览后交付，PM再看图；自动预览不代替用户游戏视觉验收。不运行 Gradle，不改 A 写集。

## 交付、集成与人工门

两写集独立，先并行实现，Gradle 在美术写入结束后串行；A/B 执行者不得相互指挥。PM 分别核对后安排独立整批代码/合同与资源审查，整改仍在原写集。新增计划或状态只由PM维护。

整合后已有 `test build --rerun-tasks --max-workers=1` 必须通过；真实加工与负例有 GameTest/隔离服务端证据，PNG与制品对应。保存候选提交和明确启动目录后，暂停请用户在熔炉、高炉与真实压片机检查产出、JEI配方/用途、四物品外观、保存重进。未获人工确认不合入该新材料批，也不推进依赖它的钢材/设备。

## 执行记录

- 本卡派发 A `lead_tin_processing`（`gpt-6-sol/high`，真实机器和重载集成）、B `lead_tin_plate_art`（`gpt-6-luna/medium`）；B独立审查使用高速模型，整批复杂集成审查使用高级模型。均不转授PM权限。
- 开工基线 `7d01b86`。A/B写集无交叉，唯一共享接口是四个物品模型使用两张既有锭PNG与两张新增板PNG；A的Gradle在B停止写入后串行执行。默认标签负例与外部标签模拟分开运行，输入仍遵守通用标签合同。
- B已交付并停止写入：两新图、53路径白名单、两次导出/安装与拒绝检查完成；PM已看两页预览，安排高速模型 `plate_art_review` 只读独立复核。A已获独占Gradle/GameTest窗口，先RED再实现并GREEN。
- B首次验证时旧脚本误写 `EXT-ART-02A/verification.json` 和 `commands.json`。PM已将误写版本存入03报告的 `overwritten-02a/`，并从已提交 `coolant-restore-evidence.zip` 精确恢复两旧文件；恢复哈希记录在 `EXT-ART-03/pm-restored-02a-evidence.json`。脚本当前只向03证据目录写入，既有游戏素材未改变。该副作用不隐瞒为全程无越界。
- A首次隔离init设置了会被MDG覆盖的 `workingDir`，RED/GREEN及普通服首启实际写入既有候选 `run/`。PM停止后续启动、备份现场后，核实执行者修正的模型与任务 `gameDirectory` 均位于报告目录，再放行复验。原配置无旧备份，未虚称还原；详见 [目录偏差记录](../../reviews/2026-10-01/material-02-isolation-incident.md)。
- 最终265项JUnit与build退出0；修正后125项GameTest断言全过，保存挂起后退出1。独立普通服完成实际标签重载三态并全维保存，退出0由A进程结果记录。独立审查无生产实现阻塞；两板离线检查通过、旧51图原字节保留。候选实现 `3a57a88` 已保存，证据见 [索引](../../reviews/2026-10-01/material-02/README.md)。
- 所有执行者已停止写入。新材料批仅进入待验收，须用户检查熔炼/高炉、压片和动力恢复、JEI、四物品外观与保存重进；不重复旧三矿批验收，也不越过此门推进钢材或设备。
