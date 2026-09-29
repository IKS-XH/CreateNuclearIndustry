# EXT-ART-01 执行交付报告

2026-09-29：四张候选及验证证据已交付，等待 PM 审查与用户风格确认；未改变任务状态。

## 基线与范围

- 工作目录：`C:/Users/IKSXH/.codex/worktrees/p1-final-verification/Create_NuclearIndustry`。
- 分支 `codex/svg-material-pilot`，HEAD `966945808860e7b0ee1d40a5241805f78ef7ca2d`；开工干净，结束跟踪文件差异为空。
- 完整读取 AGENTS、治理协议、2026-09-29 启动计划，核对内容清单 3.1/3.2/4.1/6 节。
- 实际读取技能入口均为 `C:/Users/IKSXH/.codex/skills/<name>/SKILL.md`：`minecraft-modding` 用于核对实际版本与资源边界，`minecraft-testing` 用于区分静态/视觉/客户端验证，`minecraft-resource-pack` 用于 16×16 RGBA 与像素要求。
- `gradle.properties`/`build.gradle` 确认 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6，许可证不变。
- 定点检查现有铅锭/钢板/铅矿石 PNG 均为 16×16 RGBA、钢板模型使用 item/generated；尚无 lapis_dust 资源。没有复制原纹理。

交付恰好为允许的 13 个生产文件：4 张 `sources/` SVG、4 张 `generated/` PNG、`export.py`、`palette.json`、`README.md`、`preview.png`、`preview.html`，均位于 `tools/art-assets/`。另有本报告及 `EXT-ART-01/` 证据目录。未修改 src、配置、构建、核心文档、其他工具或 .git；无 Git 写操作、依赖安装或生图调用，旧 build 证据保留。

## 实现与命令

四张原创 SVG 使用整数 rect、左上光照、6/6/6/8 色有限色板；物品四周透明，矿石不透明。导出器仅接受固定 SVG/rect/注释子集，严格拒绝未知属性/元素、外链、位图、样式、非整数、越界和色板外值；全部源稿通过后才写固定 PNG/预览。PNG 不抗锯齿，预览采用最近邻。README 详述语法与局限。

实际运行时为已捆绑 Python 3.12.14 / Pillow 12.3.0，工作目录如上：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -c "import sys,PIL; print(sys.version); print('Pillow',PIL.__version__)"
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' build/reports/extension/EXT-ART-01/make_sources.py
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' tools/art-assets/export.py
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' build/reports/extension/EXT-ART-01/verify.py
git diff --check
git status --short --untracked-files=all
git diff --stat
git branch --show-current
git rev-parse HEAD
```

`make_sources.py` 是首次原创字符草图记录，正式导出只读 SVG/色板，不依赖该脚本；后续编辑 SVG 后不应重跑它覆盖修改。

## 自动验证与证据

- `verify.py` 返回 PASS：四 PNG 16×16 RGBA，色板一致，alpha 仅 0/255，透明边界符合合同。
- 使用独立 Pillow 矩形 API 对照 SVG 与 PNG 像素全一致，并检查覆盖顺序和矩形右下边界。
- 28 类非法 SVG 输入全部拒绝；连续两次导出，四 PNG 与两预览共六文件 SHA-256 全一致。
- `git diff --check` 无输出，`git diff --stat` 无跟踪文件变化；新文件未暂存，以上实际校验覆盖新资源。
- 详细数据和全部哈希：`EXT-ART-01/verification.json`；真实导出 stdout：`EXT-ART-01/export-runs.txt`；复验脚本：`EXT-ART-01/verify.py`。
- 最终预览哈希 `ec777949ab98daeb7766c9550cd7e9ebd06ddd4aa8e92463d1d410433823af58`。依赖版本固定时字节可复现，跨 Pillow 版本的预览字体/编码字节未保证。

## 实际看图与边界

执行者两次通过 view_image 查看实际 `tools/art-assets/preview.png`。首版矿点偏圆整、钢板下缘偏厚；修订为不规则嵌岩碎簇和薄板边后再次查看。粉堆/厚锭/薄板可以区分，明暗背景仍可辨，矿石 3×3 平铺无透明缝或整圈外框，文字不压图。首版与终版分别保留为 `EXT-ART-01/preview-first-pass.png`、`preview-final.png`。

静态预览包含原尺寸、10 倍最近邻、明暗底 5 倍、矿石 3×3 平铺 6 倍。单张 16×16 周期仍可见，自然度和风格待用户判断；原尺寸是画布像素，查看器/系统缩放可改变实际屏幕大小。HTML 已生成但未做实际浏览器渲染检查。

未接入游戏，未运行客户端/服务端/GameTest/Gradle；没有把自动检查或执行者看图当成人工验收。文件稳定，等待 PM/用户评审，不扩大素材范围。
