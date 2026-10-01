# EXT-ART-02 执行交付报告

2026-09-29：51个既有PNG已重绘并按原路径接入，交付可测试候选，等待PM/独立审查及用户客户端验收。文件稳定；执行者不改变任务状态。

## 基线、技能与范围

- 工作目录：`C:/Users/IKSXH/.codex/worktrees/p1-final-verification/Create_NuclearIndustry`，分支 `codex/svg-material-pilot`，实际开工HEAD `034406387bcd97c68f6a8d9affa6b2c04231a8e2`，开工跟踪干净。
- 已读取当前AGENTS、治理协议、EXT-ART-02卡、启动计划及内容清单。实际应用本对话已完整读取的 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md`：分别用于版本/引用边界、静态与客户端验收区分、像素资源/透明与预览合同。
- 核对实际 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6；未改变技术栈或许可证。
- 仅修改 `tools/art-assets/`、原有51个游戏PNG及本卡报告/证据目录。未改Java/模型/状态/语言/mcmeta/构建/配置/核心文档，无Git写操作、外部纹理、生图或依赖安装。

## 清单与引用结论

51项=47张16×16+4张16×64，路径与尺寸完全保留；48张唯一SVG生成52候选PNG（51游戏+lapis候选）。完整映射见 `EXT-ART-02/mapping.md` 和 `tools/art-assets/manifest.json`；基线尺寸/哈希/逐项引用见 `EXT-ART-02/baseline.json`，旧图保存在工具的 `baseline/`。

`ModFluids.java:100-123` 动态引用 block 的冷/热 still/flow，fluid 四路径为兼容副本，共用SVG且PNG字节相同。无相关mcmeta；flow保留完整16×64静态布局，未裁切或新增动画。模型继续使用既有 cube_bottom_top/cube_all，未改朝向或UV。窗口中央8×8为二值透明区。

基线25路径无直接模型/流体引用：六矿石、三粗矿块、两金属块、pressure_pipe/valve、四fluid副本，以及物品 dosimeter/lead_ingot/lead_shielding_cask/raw_lead/raw_tin/raw_uranium/tin_ingot/uranium_concentrate。主泵图被P0模型复用，不等于生产泵已注册。此清单描述本分支开工状态，不将后续矿物任务或素材存在冒充注册。本批不新增lapis游戏PNG。

## 命令与自动结果

实际解释器 Python 3.12.14、Pillow 12.3.0，工作目录如上：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' build/reports/extension/EXT-ART-02/audit_baseline.py
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' build/reports/extension/EXT-ART-02/design_sources.py
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' tools/art-assets/export.py
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' tools/art-assets/export.py --install
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' tools/art-assets/verify.py
git diff --check
git diff --stat
```

audit/design 是开工记录，后续复现只需 export；勿重新采集基线或用首次设计脚本覆盖手工SVG。真实子进程命令、退出码/stdout/stderr见 `EXT-ART-02/commands.json`，详细结果和所有输出哈希见 `verification.json`。

验证PASS：51文件全部相较旧图改变且无增删；RGBA/尺寸/色板/alpha合同通过（单图最多11色）；独立Pillow矩形参考与SVG渲染/导出/游戏像素一致；30类非法SVG及长画布边界拒绝；4种真实CLI坏输入在写出前失败且全部候选/预览/游戏哈希不变；默认导出2次、显式接入2次均可复现，默认不写游戏。git diff --check无空白错误，仅出现Git现有LF→CRLF提示。

四张已批准候选PNG SHA-256保持不变（SVG与49e6c86相比仅允许Git行尾归一化，内容未改）：

| 候选 | SHA-256 |
| --- | --- |
| lapis_dust | `7011bc285c26c756d40ae7087b47022c3c9fd5002ec03f349a67e15d1475ce09` |
| lead_ingot | `8a8ed4d8cdef2683296cc51400236b7a5656ae6667ff38300d527e364bd15484` |
| steel_plate | `3f7f4714c04355fb4702122151d797cb6392d4a4c30d9f066e982c3471e79770` |
| lead_ore | `59cd7890578f2e41e9509f0cd884bce0d1306815cc88141d3d071cfb16dea1f2` |

## 实际视觉检查与停止点

执行者用view_image实际查看 `previews/minerals-1/2.png`、`reactor-1/2/3.png`、`items-1/2.png`、`coolant-1.png` 共8页，再看全量 `preview.png`。每页有before/after、原尺寸、最近邻、明暗底、2×2平铺；flow按原纵横比展示。主预览哈希 `a58eaaecda659ff461dfa6f6c7b08ff9e6fb30d6eea2b1bd343c070d0290d99b`。

首轮后图形变更涉及 minerals-1（三种粗矿块由稀疏点改为紧密咬合，去除试验过强斜向条带）、reactor-1/2（法兰内孔居中）、items-2（铀精矿颗粒堆与粗矿区分）。这些页修订后再次实际查看；items-2随后仅将lapis对照文字改成APPROVED PILOT/UNCHANGED。minerals-2、reactor-3、items-1、coolant-1图形未再变。全量总览为新增已查看产物。

静态看图：铅/锡/铀及冷蓝/热暖可区分，粗矿块紧密堆积，端口/外壳顶侧底/燃料棒束与控制棒形态分明，窗口中部透明。每图有限色块、左上光照，无标签压图。周期平铺仍可见，实际环境光/手持尺寸/流体UV需要用户客户端判断。

未运行Gradle、客户端、服务端或GameTest；HTML未做实际浏览器检查。此交付到可测试候选为止，不宣称客户端视觉验收通过，不继续扩展素材或玩法。
