# 原创像素 SVG 美术管线

EXT-ART-02 为历史 51 个游戏 PNG 建立可编辑 SVG 与确定性导出；EXT-ART-03 在保留该白名单和基线语义的前提下，新增铅板、锡板两条明确游戏路径。用户于 2026-09-29 在 EXT-ART-02A 中明确撤回两种冷却剂重绘：8 个冷却剂路径逐字节恢复原 PNG，其余 43 张保持重绘。另保留已批准的青金石粉工具候选，不新增该候选的游戏注册。此前批准的青金石粉、铅锭、铅矿石、合金钢板四张图形保持不变。

## 运行

在仓库根目录执行 PowerShell，实际环境为捆绑 Python 3.12.14 / Pillow 12.3.0，无安装或网络依赖：

```powershell
# 默认仅更新 generated/、previews/、preview.png 和 preview.html。
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' tools/art-assets/export.py

# 显式接入原51张既有游戏PNG，并添加白名单内的铅板、锡板两张新图。
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' tools/art-assets/export.py --install

# 自动核对并显式接入，包含短暂坏输入试验及finally恢复；不要与编辑源稿并行。
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' tools/art-assets/verify.py
```

其他机器可替换已有 Python/Pillow 解释器绝对路径。`verify.py` 的四样稿回归依赖仓库中 `49e6c86` 提交，冷却剂回归依赖重绘提交 `eddd097` 的父提交；只执行只读 `git show`。普通导出不依赖 Git、证据目录或首次绘图脚本。

## 源、清单与输出

- `sources/`：50 张唯一 SVG，其中4张冷却剂 SVG 仅存档，不再安装；其余46张用于43张历史游戏图重绘、2张新增板材和青金石粉候选。首次设计脚本仅在交付证据中记录，不作为正式导出步骤。
- `palette.json`：每张源稿有限色板，实际 4–11 色（原四样稿为 6/6/6/8）；导出允许每张最多16种实色。
- `manifest.json`：54 条显式记录，列出游戏相对路径、SVG、色板、尺寸、透明合同、分组与基线用途。53条对应游戏路径（历史51条加铅板/锡板两条），lapis 条目的 game 为 null；仅8项冷却剂必须声明 `preserve: baseline-original`，其他项禁止此字段。
- `pipeline.py`：独立硬编码原51路径白名单，并单独列出 `item/lead_plate.png` 与 `item/tin_plate.png` 两条扩展；不由 manifest 任意扩展写集。严格校验映射、尺寸后才准备输出；旧 `baseline/` 与 `baseline.json` 仍恰为51条。
- `generated/`：54 个导出候选；49个16×16游戏图、4个16×64游戏flow、1个16×16工具候选。两张新增板材在预览中标为 NEW，不生成历史对照图或基线记录。
- `baseline/` 与 `baseline.json`：51张开工旧图及其路径、尺寸、哈希、引用，保持不变。除用户批准的8项冷却剂原字节保留外，只用于 before/after；不从旧纹理采样绘制新图。
- `preview.png`：全量总览；`preview.html`：离线分组索引；`previews/`：8页前后对照，均含原尺寸、最近邻放大、明暗底与2×2平铺。文字使用 Pillow 内置字体，无外部字体文件。

block/fluid 八个冷却剂路径分别读取各自 baseline PNG，以管线中冻结的 SHA-256 验证后原样输出，不重编码、不套用新色板。这是固定八项例外，不能通过清单指定任意位图。旧 SVG 仍经过严格语法校验，但不参与最终纹理生成。flow 保持完整16×64静态画布及原路径；本仓库没有相关 mcmeta，本工具不添加动画元数据或把长画布裁成16×16。运行时透明度、UV与流体渲染仍由原游戏代码处理。

## 严格 SVG 子集

根 svg 必须采用 SVG 命名空间，且恰好包含清单对应的 `width="16"`、`height="16"` 或 `height="64"`、匹配的 `viewBox`、`shape-rendering="crispEdges"`。只允许直属 `rect` 与注释。rect 恰好包含 x/y/width/height/fill；位置和尺寸为非负十进制整数，尺寸大于零且完全位于画布内。fill 只允许该源稿色板中的 `#RRGGBB`，大小写均可。

未知元素/属性、分组、path、image、链接、命名色、描边、圆角、样式、变换、渐变、滤镜、文本、DTD/实体声明和处理指令均拒绝。按源顺序覆盖整数像素，无抗锯齿，输出RGBA、alpha仅0/255。物品外圈透明，实体/流体纹理全不透明，窗口中央8×8透明。所有源稿、清单和固定保留 PNG 哈希完整验证后才写任何输出；`--install` 另检查游戏PNG集合和尺寸。

## 验证和当前边界

EXT-ART-02A 的历史验证覆盖51条既有路径/尺寸、冷却剂原图字节保留、SVG像素一致性、四样稿回归、非法SVG与失败不写保护，以及重复导出和接入哈希。其历史证据保存在 `build/reports/extension/EXT-ART-02A/verification.json` 和 `build/reports/extension/EXT-ART-02A/commands.json`。EXT-ART-03 在此基础上增加两条板材白名单路径；验证保留原51项基线与既有游戏图，确认新板材没有伪造的旧图记录。EXT-ART-03 本批验证结果和命令分别保存在 `build/reports/extension/EXT-ART-03/verification.json` 和 `build/reports/extension/EXT-ART-03/commands.json`。板材仅完成离线像素与预览核对，Minecraft 客户端外观仍待用户验收。冷却剂预览的 BEFORE 为重绘前原图，RESTORED 为本次恢复结果；原 EXT-ART-02 验证记录保留历史语境。

已实际查看全部8页及总览，粗矿压缩块按审查意见改为密集咬合矿块，法兰内孔已对齐，铀精矿与粗矿物品采用不同颗粒轮廓。未启动浏览器核验HTML；PNG查看不是Minecraft客户端验收。重复周期、游戏环境光、物品手持表现及流体UV最终需在客户端判断。跨Pillow版本的编码/预览字体字节不保证相同。

基线25路径没有直接模型/流体纹理引用；另有主冷却剂泵图被P0模型复用，但不代表生产泵功能已注册。重绘不新增矿物获取、设备或物品功能，不能把开发素材的存在当成注册/玩法交付。
