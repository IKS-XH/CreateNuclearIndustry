# ART-REACTOR-09 驱动器与换料口SVG候选

导出器只在本目录内生成四张基础纹理与四张RECTANGLE候选图集；按负责人授权，八张PNG已安装试用，客户端实机视觉尚未验收。低分辨率图案以16×16整数像素构造并保存为SVG；PNG完全从SVG读取导出。铜色仅表示换料舱盖的视觉身份，不代表配方含铜。

设计把控制棒驱动器画成中冷钢导轨、轴承座与中央方杆通道，保留外围操作台；换料口用连续铜色舱盖、粗铰链和锁扣，钢边框形成对比。四个基础纹理各16×16；对应CT图集为4×4、64×64，沿用Create RECTANGLE的index布局与孤立index12。

- sources/base/：四张基础SVG。
- sources/ct/：四类sprite各16张CT上下文SVG。
- generated/base/：四张本地16×16候选PNG。
- generated/reactor_ct/：四张本地64×64候选CT图集。
- preview/index.html：原稿/候选、12×12混排近远和控制棒插入/拔出遮挡离线对照。
- references/original/base/：四张旧基础图的只读快照；SHA-256登记在mapping.json，避免正式图后续变化污染旧稿对照。
- mapping.json 与 palette.json：源路径、尺寸、目标路径和允许色板。

从美术树根目录运行捆绑Python：

- 初始化SVG来源：C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe -B -X utf8 tools/art-assets/reactor-device-art-09/export.py --seed
- 导出本地候选：C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe -B -X utf8 tools/art-assets/reactor-device-art-09/export.py
- 只读检查：C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe -B -X utf8 tools/art-assets/reactor-device-art-09/export.py --check

检查只读核对68张SVG的整数rect、色板、尺寸、CT格数与孤立纹理一致性，并逐字节比较本批8张本地PNG。正式PNG、模型、Java、其他素材和游戏均不由此工具修改。

预览SVG把用于混排和遮挡对照的PNG以内嵌数据展示，避免浏览器将SVG作为img打开时拒绝外部图像资源。可用--preview-only仅更新预览，不重写候选PNG；对应引用检查记录在build/reports/art/ART-REACTOR-09/assets/preview-reference-check.log。
