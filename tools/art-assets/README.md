# 原创像素 SVG 美术管线

## 反应堆制造01素材

本批独立入口`reactor_01_assets.py`管理`sources/reactor-01/`下12份可编辑SVG：6种材料、5种序列半成品和屏蔽混凝土纹理。当前脚本与素材仅在同级候选工作树，main先同步说明；待人工验收后再合入。仅生成本批16×16 PNG及对应物品/普通方块模型，不重绘已有反应堆模型、不修改共用manifest；运行状态与人工边界见[两批交接](../../docs/reviews/2026-10-03/reactor-01/README.md)。

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/reactor_01_assets.py
```

已有SVG保留编辑，缺失时才建立初始源稿。专用脚本按精确12条输出路径工作，对其它现有PNG做前后字节核对；114张是本次基线数量，不能变成后续增加素材后的运行限制。明暗底原尺寸/最近邻预览和本次核对写入`build/reports/extension/EXT-A-REACTOR-01-ART/`，不运行Gradle或客户端。

EXT-ART-08新增离心机六面、尾矿砖、三种粉末、料浆桶和静止/流动料浆共13张游戏纹理，并提供机器与尾矿砖JSON模型。该历史导出清单含82张游戏纹理，复用已有铀精矿，开工前69张PNG保持原字节。后续原生桶、两格模型及端盖整改已完成人工验收并合入main；见[最终验收](../../docs/reviews/2026-10-02/fuel-01/ACCEPTANCE.md)。历史清单数量不代表后续独立生成器的新增资源总数。

## 材料01B青金石粉接入

[01B候选](../../docs/reviews/2026-10-03/material-01b/README.md)直接将已认可的`generated/item/lapis_dust.png`接入游戏同名物品路径；该新增物品与配方尚待客户端验收，main尚无此运行内容。旧共用manifest及pipeline固定采用82个游戏路径加1个工具候选，保持原样，不能仅将lapis的`game:null`改为游戏路径。游戏PNG与已认可产物逐字节一致，今后若修改lapis SVG，导出后仅单独复制此PNG到游戏路径；不要为该项运行历史全量install。以下旧清单的数量和“工具候选”是其历史导出合同，不代表01B注册状态。

## 燃料02E八格装配台模型维护

`fuel_02e_assets.py`沿用已认可SVG材质，生成八格静态模型、三个待机活动件和完整物品模型。R2增加共面裁切及旋转端盖微小内缩，检查实际旋转面、完整物品和运动端点；证据默认写入`build/reports/extension/EXT-A-FUEL-02E-R2/`。113张当时已有PNG保持原字节，当前基线和客户端边界见[02E验收](../../docs/reviews/2026-10-03/fuel-02e/ACCEPTANCE.md)。后续动画继续使用原`rig.json`，本工具不实现动画时序。

## 燃料02C装配材料素材

本批新增锡铅焊料、包壳管、钢网、钢格架和格架半成品五项16×16像素SVG，已全部手测通过并合入main，见[02C最终验收](../../docs/reviews/2026-10-03/fuel-02c/ACCEPTANCE.md)。使用独立`fuel_02c_assets.py`复用严格SVG渲染器，不扩写共用manifest，不重新导出旧素材。93张已有游戏PNG保留原字节。

在包含本批实现的仓库根目录运行：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/fuel_02c_assets.py
```

默认读取`sources/fuel-02c/`下五份可编辑SVG，生成本批PNG、`item/generated`物品模型及`build/reports/extension/EXT-A-FUEL-02C-assets/`预览；不覆盖源稿。仅缺少初始源稿时使用`--initialize-sources`建立缺失文件。五项之外的素材不在该工具写集内，命令不运行Gradle。

## 燃料02A/02B专用素材

燃料02A新增两枚芯块及单格烧结炉；02B将八棱炉体扩大到整格外包范围。全部人工门已通过并合入main，见[最终验收](../../docs/reviews/2026-10-03/fuel-02b/ACCEPTANCE.md)。继续使用独立`fuel_02a_assets.py`，不扩大旧manifest或修改共用导出器。

在仓库根目录运行：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/fuel_02a_assets.py

# 仅调整炉体几何时，跳过PNG和SVG相关资源导出。
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/fuel_02a_assets.py --geometry-only
```

默认命令读取`sources/fuel-02a/`中现有7张SVG，经既有严格SVG渲染函数导出7张PNG，并生成三项物品模型、冷热炉体与8种方块状态；不会默认覆盖SVG源稿。`--geometry-only`只重建炉体模型，不重新导出PNG、芯块模型和方块状态。纹理修改编辑SVG，几何与变换修改本批生成器。当前预览及定向检查写入`build/reports/extension/EXT-A-FUEL-02B/`，只处理本批路径，不运行Gradle。JSON几何预览不代表Minecraft光照、UV或客户端验收。

EXT-ART-02 为历史51张游戏PNG建立可编辑SVG与确定性导出；EXT-ART-03新增铅板、锡板路径；EXT-ART-04新增铅粒、锡粒路径；EXT-ART-05新增铁粉、煤粉、木炭粉、钢粉和钢锭五条路径；EXT-ART-06新增锡条、工业传感器、辐射传感器及两种半成品；EXT-ART-07新增石英粉、耐火砖、重型轴承及其半成品。历史51项路径和基线规则保持不变。用户于2026-09-29在EXT-ART-02A中明确撤回两种冷却剂重绘：8个冷却剂路径逐字节恢复原PNG，其余43张保持重绘。当时另保留已批准的青金石粉工具候选；后续01B游戏接入见上方专节。此前批准的青金石粉、铅锭、铅矿石、钢板四张图形保持不变。

## 运行

在仓库根目录执行 PowerShell，实际环境为捆绑 Python 3.12.14 / Pillow 12.3.0，无安装或网络依赖：

```powershell
# 默认仅更新 generated/、previews/、preview.png 和 preview.html。
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/export.py

# 显式接入82张游戏PNG，包括本批13张离心机与铀处理素材。
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/export.py --install

# 自动核对并显式接入，包含短暂坏输入试验及finally恢复；不要与编辑源稿并行。
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/verify.py
```

其他机器可替换已有 Python/Pillow 解释器绝对路径。`verify.py` 的四样稿回归依赖仓库中 `49e6c86` 提交，冷却剂回归依赖重绘提交 `eddd097` 的父提交；只执行只读 `git show`。本批69张旧图核对使用版本管理内的`baselines/EXT-ART-08-existing-game-png-sha256.json`固定快照，验证结果输出到`build/reports/extension/EXT-ART-08/`，不需要先恢复旧build报告。快照用于本批交付验收，不禁止后续经批准修改SVG再显式安装。普通导出不依赖 Git、证据目录或首次绘图脚本。

## 源、清单与输出

- `sources/`：79张唯一SVG，其中4张冷却剂SVG仅存档、不再安装；75张供现有重绘、新增材料/机器/流体及青金石粉候选使用。首次设计脚本仅在交付证据中记录，不作为正式导出步骤。
- `palette.json`：每张源稿有限色板，实际 4–11 色；导出允许每张最多16种实色。
- `manifest.json`：83条显式记录，列出游戏相对路径、SVG、色板、尺寸、透明合同、分组与基线用途。82条对应游戏路径（历史51条、已有18条、本批13条），lapis条目的game为null；仅8项冷却剂必须声明 `preserve: baseline-original`，其他项禁止此字段。
- `pipeline.py`：独立硬编码原51路径白名单，并单独列出已有18项和本批13项素材；不由manifest任意扩展写集。严格校验映射、尺寸后才准备输出；旧 `baseline/` 与 `baseline.json` 仍恰为51条。
- `generated/`：83个导出候选；77个16×16游戏图、5个16×64游戏flow、1个16×16工具候选。无历史基线的新增路径在预览中标为NEW，不生成历史对照图或基线记录。
- `baseline/` 与 `baseline.json`：51张开工旧图及其路径、尺寸、哈希、引用，保持不变。除用户批准的8项冷却剂原字节保留外，只用于 before/after；不从旧纹理采样绘制新图。
- `preview.png`：全量总览；`preview.html`：离线分组索引；`previews/`：12页前后对照，均含原尺寸、最近邻放大、明暗底与2×2平铺。文字使用 Pillow 内置字体，无外部字体文件。

block/fluid 八个冷却剂路径分别读取各自 baseline PNG，以管线中冻结的 SHA-256 验证后原样输出，不重编码、不套用新色板。这是固定八项例外，不能通过清单指定任意位图。旧 SVG 仍经过严格语法校验，但不参与最终纹理生成。flow 保持完整16×64静态画布及原路径；本仓库没有相关 mcmeta，本工具不添加动画元数据或把长画布裁成16×16。运行时透明度、UV与流体渲染仍由原游戏代码处理。

## 严格 SVG 子集

根 svg 必须采用 SVG 命名空间，且恰好包含清单对应的 `width="16"`、`height="16"` 或 `height="64"`、匹配的 `viewBox`、`shape-rendering="crispEdges"`。只允许直属 `rect` 与注释。rect 恰好包含 x/y/width/height/fill；位置和尺寸为非负十进制整数，尺寸大于零且完全位于画布内。fill 只允许该源稿色板中的 `#RRGGBB`，大小写均可。

未知元素/属性、分组、path、image、链接、命名色、描边、圆角、样式、变换、渐变、滤镜、文本、DTD/实体声明和处理指令均拒绝。按源顺序覆盖整数像素，无抗锯齿，输出RGBA、alpha仅0/255。物品外圈透明，实体/流体纹理全不透明，窗口中央8×8透明。所有源稿、清单和固定保留 PNG 哈希完整验证后才写任何输出；`--install` 另检查游戏PNG集合和尺寸。

## 验证和当前边界

EXT-ART-02A 的历史验证覆盖51条既有路径/尺寸、冷却剂原图字节保留、SVG像素一致性、四样稿回归、非法SVG与失败不写保护，以及重复导出和接入哈希。其历史证据保存在 `build/reports/extension/EXT-ART-02A/verification.json` 和 `build/reports/extension/EXT-ART-02A/commands.json`。EXT-ART-03、04、05逐批增加板材、金属粒及制钢素材路径，均保留51项历史基线，新增纹理不伪造旧图记录。用户于2026-10-01确认铅锡和制钢完整手测通过，相应素材已验收并合入main，见 `docs/reviews/2026-10-01/material-02-acceptance.md` 和 `docs/reviews/2026-10-01/material-03b/README.md`。冷却剂预览的BEFORE为重绘前原图，RESTORED为恢复结果；旧报告保留各自历史语境。

EXT-ART-06本批证据归档于 `docs/reviews/2026-10-01/material-04/art/`：`verification.json` 和 `commands.json`记录66条清单/65张游戏图，导出与安装各两次一致、30类SVG负例拒绝和6类CLI失败不写；`baseline-60-game-png-sha256.json`记录开工60图并用于保留核对。PM实际查看五图原尺寸、明暗底放大对照，锡条调整为易与锡锭区分的细长方条。用户于2026-10-01另行确认完整客户端清单通过，包含本批五项外观，素材已验收并合入main；见 `docs/reviews/2026-10-01/material-04/ACCEPTANCE.md`。离线与客户端证据分别保留。

EXT-ART-07离线证据归档于`docs/reviews/2026-10-02/material-05/art/`：70条清单、69张游戏PNG、66个SVG来源，原65图哈希快照保留；四项原尺寸与明暗底对照图已由执行者及PM实际查看。原51项基线及8冷却剂原图保护继续有效。素材更新限制已整改并复审：合法SVG编辑可显式安装，相同字节跳过写入，清单外路径/错误尺寸/冷却剂字节保护仍保留；可恢复像素修改的RED→GREEN→还原证据单列。用户于2026-10-02确认完整客户端清单通过，包含四项外观；素材与材料加工已验收并合入main，见[最终验收](../../docs/reviews/2026-10-02/material-05/ACCEPTANCE.md)。离线与客户端证据分别保留。

EXT-ART-02A、EXT-ART-03与EXT-ART-03A的历史报告分别记录了当时查看的预览范围及审查结果；EXT-ART-04实际查看铅粒、锡粒与铅锡锭、粗矿、板材的放大对照图。EXT-ART-05实际查看`items-3.png`中的五种新物品（原尺寸、明暗底、2×平铺）和报告目录中的五物品/钢板放大对照预览；钢锭采用锡锭式平顶厚锭轮廓。PNG查看不是Minecraft客户端验收。重复周期、游戏环境光、物品手持表现及流体UV最终需在客户端判断。跨Pillow版本的编码/预览字体字节不保证相同。

基线25路径没有直接模型/流体纹理引用；另有主冷却剂泵图被P0模型复用，但不代表生产泵功能已注册。重绘不新增矿物获取、设备或物品功能，不能把开发素材的存在当成注册/玩法交付。
