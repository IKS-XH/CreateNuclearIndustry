# STARTUP-DELIVERY-REVIEW 只读独立审查

日期：2026-09-29。审查者为执行者；此报告不验收 EXT-ART-01 / EXT-A-START-01，不批准风格、参数、客户端结果或候选合并。

## 范围与基线

- 美术候选工作树：`C:/Users/IKSXH/.codex/worktrees/p1-final-verification/Create_NuclearIndustry`，分支 `codex/svg-material-pilot`、HEAD `9669458`。只读 `git status --short --untracked-files=all` 显示仅 `tools/art-assets/` 的 13 个新文件；保留旧 P1 忽略报告。未执行 Git 写操作或启动 Minecraft。
- 主树最新依据：该工作树 `AGENTS.md`、`docs/superpowers/plans/2026-09-29-resources-production-art-start-plan.md` 第 3/5 节。实际读取 `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`，用于 1.21.1 的 16×16 RGBA、像素资源和展示边界；读取 `minecraft-testing/SKILL.md`，区分静态检查、视觉观察和游戏端验证。仓库版本优先于技能中的其他版本示例。
- 同时定点阅读主树 `build/reports/extension/EXT-A-START-01.md`，仅审查已批/候选界限、三矿建议、Create 引用和验证措辞。PM 已独立核对九条 Create JSON 的实际参数；本轮不重复研究 JAR。
- 未修改候选、源码、核心文档、配置或游戏资源；没有运行会写 PNG/预览的 `tools/art-assets/export.py`。独立解析器和像素检查使用捆绑 Python 以 `-B` / `PYTHONDONTWRITEBYTECODE=1` 在内存执行。

## EXT-ART-01 技术与图像结论

**未发现阻塞的 P1/P2 问题。** `export.py:46–89` 的受限 SVG 解析只接受固定 16×16 根属性、直属 `rect` 与注释；非法标签/属性、文本、非整数或越界矩形、非色板色值都报错。实际四张 SVG 的 XML 内容仅为这组图元及中文注释，没有位图、外链、渐变或滤镜。独立内存探针确认 `image`、`path`、嵌套 `g`、外部 `use` 与越界矩形均被拒绝，右下角合法 1×1 矩形可正确输出；没有发现“静默漏画”路径。导出器 `main():173–196` 只在四张源稿全部验证后写固定的四张 PNG、`preview.png` 和 `preview.html`，不接入 `src/`。

独立重新读取每张 PNG，并调用源 SVG 内存渲染比较逐像素字节：四张均为 **PNG、16×16、RGBA**；三件物品 alpha 集 `{0,255}` 且四周透明，铅矿石 alpha 集 `{255}`；源/PNG 像素完全一致。四张当前 SHA-256 分别为 `7011bc285c26c756d40ae7087b47022c3c9fd5002ec03f349a67e15d1475ce09`、`8a8ed4d8cdef2683296cc51400236b7a5656ae6667ff38300d527e364bd15484`、`3f7f4714c04355fb4702122151d797cb6392d4a4c30d9f066e982c3471e79770`、`59cd7890578f2e41e9509f0cd884bce0d1306815cc88141d3d071cfb16dea1f2`，与执行者 `EXT-ART-01.md` 和 `verification.json` 相同。

`EXT-ART-01/export-runs.txt` 保存两次实际子进程导出记录，四张 PNG 的两轮哈希一致；`verification.json` 还记录六个输出在每轮与运行前一致。此证据与 `verify.py` 的实际哈希比较逻辑相符；本独立审查没有再次导出，不把这些记录说成本轮重跑。当前 `preview.png` / `preview.html` SHA-256 亦与记录一致。

已用 `view_image` 实际查看最终 `tools/art-assets/preview.png`：青金石粉保持颗粒堆轮廓，铅锭为厚实冷灰，钢板为较薄的斜板，三者在明暗底色上可区分；矿石 3×3 平铺无透明缝或明显外框，重复周期仍能辨认。原尺寸、10 倍最近邻、明暗对照及平铺区域齐全，文字未压住图案。此为技术和可读性观察，最终风格是否符合用户审美仍待用户确认；HTML 浏览器效果和 Minecraft 物品栏/方块场景未实际验证，不能以静态预览替代。

## EXT-A-START-01 决策包定点审查

**未发现阻塞的 P1/P2 问题。** 报告第 1、3、4、6 节将已确认的身份/路线要求与新建议数字分开：铅/锡/铀矿的高度、尝试次数、规模、暴露概率、工具等级、掉落和 9:1 粗矿块均明确为未批准候选；D-03e 仍是候选，`EXT-A-MATERIAL-01A` 仍未合并。第 6 节只把最早门槛收为三组用户决定，并保留替代方向与玩家差异，没有要求一次批准后续整线参数。

第 5 节明确九条 Create 粉碎配方属于锁定依赖中的**静态事实**，概率产出不等于运行样本；本模组尚未注册三矿或完成真实粉碎接入，第三方洗涤不构成本模组生存链。第 7/8 节把后续写集与真实游戏、生成采样、客户端人工证据列为**建议和未执行项**，没有把当前 JAR/JSON 审查称为游戏验收或已完成首台设备。任务卡须由 PM 在用户参数门后正式冻结；本审查不派发。

## 交接界限

以上是独立静态/视觉复核，未发现需要退回的具体缺陷。候选是否符合用户风格、三组矿物/副产物建议如何取舍、真实客户端与自然生成/粉碎运行，仍按启动计划第 5 节交由 PM 组织用户决策和后续验证。美术候选当前未接入游戏，也不因此获得合并许可。
