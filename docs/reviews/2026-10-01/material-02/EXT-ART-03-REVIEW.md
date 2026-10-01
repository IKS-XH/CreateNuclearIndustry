# EXT-ART-03（B段）只读审查报告

**结论：无阻塞，B段交付可供 PM 后续处理；Minecraft 客户端视觉门仍待用户完成。**本报告不改变项目任务状态，也不构成最终验收。

**审查基线与范围：**候选工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，基线 `7d01b86d9ac7e07fe73700052af62a904cdb7a7c`。已读取根目录 `AGENTS.md`、治理协议、`2026-10-01-ext-a-material-02.md` 的 B 段、`2026-10-01-lead-tin-material-proposal.md`，以及 `minecraft-resource-pack`、`minecraft-modding`、`minecraft-testing` 技能。审核只覆盖 EXT-ART-03。未运行 Gradle、`verify.py`、`export.py` 或其他验证/生成命令；只读检查现存差异、脚本、哈希和图像。

**发现：**

- 新增铅板、锡板 SVG 均为 16×16，使用整数矩形坐标和各自有限色板；已有导出报告记录 RGBA、二值透明度、外圈透明检查。实际查看 `items-1.png`、`items-2.png`：两板使用一致的扁平板形，铅板为较暗冷灰、锡板为较亮银灰，彼此可分，也与斜置钢板及铅/锡锭区分。两张预览都把新图明确标为 `NEW CANDIDATE / NEW`。
- 基线提交包含 51 张游戏 PNG；候选中这些路径没有已跟踪差异，纹理目录仅多出任务要求的 `item/lead_plate.png`、`item/tin_plate.png`。`baseline/` 和 `baseline.json` 仍各记录51项。8张冷却剂现存 PNG 与原始 baseline 哈希全部一致。
- `pipeline.py` 将原 51 路径与独立的两条 `NEW_GAME_FILES` 合并成精确白名单；manifest 固定53个游戏路径加1个 lapis 工具候选。验证报告维持历史 baseline 为51项，并明确两张新图无历史基线。
- 现存 `verification.json` 报告结果为 PASS：两次默认导出、两次安装导出；旧51张游戏 PNG 安装前后哈希不变；新路径无伪造 baseline；四张批准样稿未变。`commands.json` 有两次默认命令成功、六类非法输入/映射命令失败、两次 `--install` 成功的真实子进程记录。验证脚本在每个失败用例前后比较候选、预览和游戏 PNG 状态哈希，覆盖非法 SVG、额外路径、错误尺寸、错误映射及未授权/缺失 preserve 声明。
- 02A 误写修复证据在 `build/reports/extension/EXT-ART-03/pm-restored-02a-evidence.json`。记录的 `RestoredSHA256` 与现场 `EXT-ART-02A/verification.json`（`6CF93B8B…77D5593`）及 `commands.json`（`200439C9…F86B1B0`）实际哈希一致；误写副本另保存在 `EXT-ART-03/overwritten-02a/`。修复后的 `verify.py` 将证据目录固定为 `EXT-ART-03`，成功报告与命令日志也位于该目录，当前实现不再写 `EXT-ART-02A`。

**剩余人工门：**由用户在 Minecraft 客户端查看物品栏、手持和资源重载下的两种板材外观。离线图像审查不能替代该体验确认。此审查未验证 A 段 Java、配方或 GameTest 变化，也未运行候选脚本；既有命令和报告作为执行证据审阅。
