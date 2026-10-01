任务 ID：EXT-A-NEXT-MATERIAL-GATE-01
核对日期：2026-10-01
开工时读取 HEAD：6eac7184cb53c9ceece47ec2c464cd52ae3ef00d；交付前只读复核时 HEAD 已变为 3a365dc9e6188582d40886f9f6c1d88975566ea3（期间项目经理推进了 Git 状态，本执行者未作 Git 写操作）
只读候选：39db2b4ab47f5f1519f06c667e3351406b3a3f14（修复开发客户端 JEI 模组发现路径）

## 结论

下一最小材料批建议为铅、锡通用粗矿标签直熔为锭，再由 Create 压片机把锭压为本模组对应金属板。推荐新数值仍未获用户批准，配方实现前须先决定这组参数。

本批输入仅为 `c:raw_materials/lead` 与 `c:raw_materials/tin` 当前成员，支持其他模组通过通用标签提供等价粗矿；不在本批把 `create:crushed_raw_lead/tin` 加入这些标签，也不为它们新增熔炼/高炉配方。粉碎粗矿仍沿已有关系进入鼓风机洗矿，所得铅粒/锡粒如何进入锭另留后续批次。这样不新增洗矿之外的旁路，也不产生一粒换一锭的推断。

## 合同状态依据

- 用户已报告三矿批人工门中的粗矿 9:1、重进存档和旧冷却剂外观全部通过；此前生成的采集粉碎、其他新素材及 JEI 界面通过。本报告据此核对下一材料批，不代替项目经理登记候选验收、合并或任务状态。
- `docs/recipes.md:151-181` 已确认粗矿与粉碎粗矿的工序关系：粉碎粗矿走鼓风机洗涤成为粒，关系表另写“粒或粗矿→熔炼→锭”、锭压板；但三矿已确认合同明确排除熔炼新数值，粒到锭数量及工序也仍待澄清。
- `docs/recipes.md:61-67` 与 `docs/content-catalog.md:53-65` 定义粗矿、锭与板材对应的 `c:*` 标签，预列 `raw_lead/tin`、`lead_ingot/tin_ingot` 和 `lead_plate/tin_plate`。这些内容关系没有批准新数值。
- `EXT-A-START-01.md:118-127` 将后续小批建议为铅锡粗矿直熔和压板，曾给出每粗矿 1 锭、每锭 1 板作为待讨论起点，并明确未批准及排除粒；该报告并未把 Create 粉碎粗矿另开直熔路线。
- `docs/superpowers/plans/2026-09-22-first-release-extension-preparation-plan.md:24-34, 106-108` 规定加工参数分批冻结、每个副产物须有去向；D-03a 已确认首套投入适中、流程与产线建设为主要挑战，并允许手动转运。
- `docs/superpowers/plans/2026-09-24-first-production-cost-draft.md` 没有铅锡熔炼/压板的新批准数值。D-03e 冷却剂候选与本批无关，不能外推。

## 一组首批参数建议（尚未批准）

| 输入 | 工序 | 建议产出 | 建议时长与经验 | 边界 |
|---|---|---|---|---|
| 1 件 `c:raw_materials/lead` 成员 | 熔炉熔炼 | 1 `lead_ingot` | 200 ticks；0.7 XP | 铅锡同构；标签接入当前和未来等价粗矿，不自行加入 Create 粉碎粗矿。 |
| 1 件 `c:raw_materials/lead` 成员 | 高炉烧炼 | 1 `lead_ingot` | 100 ticks；0.7 XP | 与对应熔炉配方同 XP；仅加快处理，不增产。 |
| 1 件 `c:raw_materials/tin` 成员 | 熔炉熔炼 | 1 `tin_ingot` | 200 ticks；0.7 XP | 与铅同构。 |
| 1 件 `c:raw_materials/tin` 成员 | 高炉烧炼 | 1 `tin_ingot` | 100 ticks；0.7 XP | 与对应熔炉配方同 XP。 |
| 1 件 `c:ingots/lead` 成员 | Create 压片机 | 1 `lead_plate` | 无副产物；不在配方写 `processing_time` | 输入标签兼容，输出固定为本模组铅板。 |
| 1 件 `c:ingots/tin` 成员 | Create 压片机 | 1 `tin_plate` | 无副产物；不在配方写 `processing_time` | 输入标签兼容，输出固定为本模组锡板。 |

推荐 1 粗矿→1 锭，便于按一件原料一件基础金属追踪；200/100 ticks 和 0.7 XP 与 Minecraft 1.21.1 raw iron、raw copper 熔炉/高炉配方完全相同。Create 原生 iron/copper/gold 压片均为一个 `c:ingots/*` 输入、一个板输出，配方 JSON 不声明 `processing_time`；所以 1 锭→1 板、无副产物并沿用原生压片配方形态是低范围候选，不代表已测固定现实耗时。

锁定 Create 中 crushed raw iron/copper/gold 的熔炼/高炉配方经验为 0.1 XP。本批不采纳该路线，也不把该参数套到 `c:raw_materials/lead/tin`。参考数据保存在证据文件，说明该方案是新材料标签仿原版粗矿路线，而非对 Create 粉碎配方的复制。

## 唯一最早用户决策

请用户一次决定是否批准上表整组试验参数：“`c:raw_materials/lead/tin` 每件熔炉 200 ticks、高炉 100 ticks，均产 1 对应锭并给 0.7 XP；`c:ingots/lead/tin` 每锭压 1 对应板，无副产物、不另写压片时长。”如不同意，可直接指出需修改的数值。此决定不含 Create 粉碎粗矿直熔、铅锡粒换锭、煤粉/木炭粉、铁碳配比、制钢、设备或下游配方。未获决定前，只暂停本铅锡熔炼/压板配方实现。

## 锁定版本、技能与方法

项目基线：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。Minecraft 1.21.1 客户端 JAR SHA-256：`499f6897d1837516680f3114072d8106e11c9adcd933fe5cf051b551089b0c99`。Create slim JAR SHA-256：`f0652bee27460f2d26a748f537cb5d687981441378d3a526d17ffaab5a1072bb`。

- 实际完整读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：按锁定版本识别 NeoForge、物品注册/标签输入和数据配方的边界；未将技能的 1.21.11 示例当成本项目接口。
- 实际完整读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：区分 JAR 静态配方事实与真实熔炉、高炉、压片机测试；本次未实现、未运行测试。
- 用 PowerShell 只读读取项目文件，`rg`、`git status --short`、`git rev-parse`、`git show 39db2b4:<path>`，以及 .NET ZipArchive 读取锁定 Minecraft/Create JAR 配方条目；用 SHA-256 校验 JAR。未运行 Gradle、JUnit、GameTest、游戏或联网安装。
- 当前观察到 `.vscode/launch.json` 有既有改动，未触碰。仅写本报告与同名证据目录。

## 执行者交付边界

本报告交付一个下一材料批建议和一组待决参数，不构成参数批准、任务派发、验收或核心文档修改；由项目经理按授权接续。
