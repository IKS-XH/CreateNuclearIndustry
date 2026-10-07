# EXT-ART-02A：恢复两种冷却剂旧外观

**状态：** 已完成；PM/独立审查和用户2026-10-01冷却剂回退测试通过，资源修订 `b4d78d5` 随 `d165d88` 合入 main。严格恢复8张旧冷却剂PNG，保留其他43张新图，不改变其他资产合同。
**执行位置 / 基线：** `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，`codex/ore-acquisition`，`c92e76282828927915dea5b5b3be399cb880eaab`。沿用现有整合候选，不建新工作树。主工程用户 `.vscode/launch.json` 不在写集。
**唯一目标：** 常温复合冷却剂和高温复合冷却剂恢复本批重绘前的原始 PNG，其他 43 个游戏 PNG 与矿物实现不变，后续批量导出不能再次安装被撤回的冷却剂重绘图。

## 必读与版本

主工程及执行工作树 `AGENTS.md`、主工程 `docs/project-governance.md`、本卡、[原美术合同](2026-09-29-ext-art-02.md)、[客户端检查点](../../reviews/2026-09-29/implementation/README.md)。实际读取 `C:/Users/IKSXH/.codex/skills/` 下 `minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md` 并记录应用。固定 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。

## 精确范围与行为

- 游戏资源只允许修改 `src/main/resources/assets/create_nuclear_industry/textures/{block,fluid}/{compound_coolant,hot_compound_coolant}_{still,flow}.png`，共 8 个现有路径。冷却剂泵、端口等结构素材不在回退范围。
- 允许修改 `tools/art-assets/` 中必要的导出/验证逻辑、清单、工具 README、对应 generated 输出和预览。已有 baseline 图、非冷却剂源稿及其生成 PNG 不得改动；未采用的冷却剂 SVG 可保留，但须明确不再安装。可把已批准旧 PNG 作为八项固定保留资源，此为用户要求恢复旧外观的例外，不能扩展成任意位图导入。保留严格路径白名单、先校验后写、非法 SVG 拒绝等保护。
- 原图来源以重绘提交 `eddd097` 的父提交及已跟踪 `tools/art-assets/baseline/` 相互核对，恢复逐字节相同的旧文件。不要用生图、重新近似绘图、重编码或改变 alpha/尺寸；block/fluid 原图若原本不同，应各自恢复，不能为追求同义路径一致而覆盖差异。
- 只读核对客户端实际纹理引用；不改 Java、模型、mcmeta、颜色乘算、透明度、流体行为、配方、构建和存档。必要手写注释使用中文。
- 报告写 `build/reports/extension/EXT-ART-02A.md` 及同名证据目录。执行者禁止所有 Git 写操作、核心文档修改、派发他人及自行验收；向 PM 交付未提交改动。

## 验证与交付

1. 记录 8 项旧图、整改前及整改后 SHA-256/尺寸，检查实际引用。其余 43 游戏 PNG、矿物代码/数据与基线保持一致。
2. 运行正式默认导出与显式安装各两次，证明旧冷却剂不会被覆盖、其余资产保持不变、输出可重复；沿用并适当调整已有严格校验，不为纯贴图恢复增加无意义的游戏逻辑测试。
3. 实际查看冷却剂对照预览，确认显示的最终候选是旧外观。报告实际技能、命令、结果、未验证项；无需启动游戏或全量 GameTest，PM 统一运行资源处理/打包验证。
4. PM 审核并保存修订候选后暂停于用户冷却剂视觉复验；三矿自然生成和采集已获用户确认，粉碎轮加工也于 2026-10-01 获用户确认。用户随后于 2026-10-01 确认粗矿 9:1 合成/拆解、保存重进及两种冷却剂回退测试通过，本轮人工门已解除。后续材料与设备仍须冻结各自参数。

**交付核对：** 八项与重绘前原图逐字节一致，另外 43 项保持整合候选原值。两次默认导出、两次安装、30 类非法 SVG 与六种 CLI 失败不写出检查通过；PM 实际查看恢复预览，并运行 `processResources jar prepareClientRun` 退出 0，JAR 内 51 项逐字节核对通过。未重跑全量 JUnit/GameTest，未启动客户端。报告与当前包见 [客户端检查点](../../reviews/2026-09-29/implementation/README.md)。
