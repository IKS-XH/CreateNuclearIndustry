# ART-REACTOR-08 窗口铅灰层资产候选

本批仅新增独立目录 [`reactor-window-art-08`](../../../tools/art-assets/reactor-window-art-08/)，未改写旧R1工具、R1窗口SVG/PNG、共享SVG renderer或其余13张图集。源候选含64张16×16 SVG，延续R1锁定的47个原生OMNIDIRECTIONAL上下文和17个fallback索引。原透明pane位置新增 `#66696A` 与 `fill-opacity="0.25098039215686275"`（alpha64/255）；其余框、边、角的RGBA像素经局部检查逐像素保持。

候选图集为128×128，生成64张预览tile与单张 `generated/reactor_window_lead_ct.png`。全连接mask255/index54由原先全透明变为整格alpha64；透明窗格共9280像素，原不透明框/角7104像素。其余颜色/形状与R1 tile一致。唯一观察窗CT atlas已获准安装试用；仍不是游戏客户端视觉验收，未运行Gradle、assemble或游戏。

本批离线浏览器对照入口是新目录 `index.html`：全连接旧空窗、新单层和双层alpha64合成；另展示单边index01、角窗index12、候选atlas。背景来自本批手绘矢量堆芯示意，页面明确说明它不模拟Minecraft透明排序和光照。负责人已实际复看浏览器合成；该结果仅用于颜色与内部辨识度试用，不等同游戏客户端验收。

小型检查记录为 `build/reports/art/ART-REACTOR-08/assets/check.log`：

```text
CHECK_OK svg=64 atlas=128x128 contexts=47 fallback=17 pane_alpha64=9280 frame_pixels_same=7104 generated_files=65
exit=0
```

检查锁定旧R1 64份窗SVG SHA，验证原生47/17拆分、所有窗格透明度与mask覆盖、SVG整数rect/色板、PNG完整覆盖和旧tile实体像素不变；输出只包含窗口tiles及atlas，不重导其他材质。最终实际命令与目录见同目录 `commands.md`。首次导出失败及根因保存在 `first-export-failure.log`，初始seed遗漏fallback格的中心窗格；修正seed/检查器的mask=0处理后重新导出并通过，没有删减断言或改旧R1源。

原installed mapping仍为 `reactor_ct/reactor_window.png`；既有R1验证已绑定源/安装/JAR一致，SHA-256 `cb0a9fe5dc3f106c75a8070d09ff305df99d594bd7b630b9a2796e88b8994026`。本批已按授权仅替换唯一正式CT窗口atlas；其他正式路径未触及。

技能实际读取并应用：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md`；图稿流程按 `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/brainstorming/SKILL.md`，首次导出失败后按同目录 `systematic-debugging/SKILL.md`定位并修复。
## 唯一正式atlas安装记录

安装目标仅为 `src/main/resources/assets/create_nuclear_industry/textures/block/reactor_ct/reactor_window.png`。先完整备份旧atlas至 `build/reports/art/ART-REACTOR-08/assets/baseline/reactor_window.png`，旧备份SHA-256为 `cb0a9fe5dc3f106c75a8070d09ff305df99d594bd7b630b9a2796e88b8994026`；候选generated与正式安装逐字节一致，1458字节，SHA-256 `f18296427db4c106579f778e3b0443284d5fd074ebf997889d13c495f1874723`。实际安装核对命令见 `build/reports/art/ART-REACTOR-08/assets/verify-install.ps1`，输出与exit 0见 `install-check.log`。

窄保护核对确认基础16×16 fallback纹理保持SHA `ae8f3cb1efe3d37caac43f9045854d87f53b463af66858e0623965730d61140b`，其他13张CT atlas均与ART-REACTOR-02R1冻结核验记录一致。最终mapping已记录试用安装与旧atlas备份。用户实际游戏视觉仍待复看；本执行者未运行assemble或客户端。




独立窄审说明整改：mapping与导出器中的pane覆盖范围说明已统一为64个索引（47个原生上下文及17个隔离fallback）；仅修正文案，沿用原像素与check证据，未重生成、安装或构建。

