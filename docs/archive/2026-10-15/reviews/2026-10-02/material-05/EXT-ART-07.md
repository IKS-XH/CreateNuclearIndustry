# EXT-ART-07 四项素材交付报告

## 交付范围

- 任务：`EXT-ART-07`，基准 Git HEAD：`53f4ebdd7b2f3bd580d96d85fb39b18e02fbb98d`。
- 按任务2精确写集新增四项16×16整数 `rect` SVG：`quartz_dust`、`refractory_brick`、`heavy_bearing`、`incomplete_heavy_bearing`。石英粉采用浅色晶粒；耐火砖以厚实方砖轮廓表现；轴承用金属厚框和透明轴孔；半成品沿用同族色板并缺少右上框段。
- 新增四个色板和清单记录；管线固定白名单由65个游戏PNG扩至69个，清单由66条扩至70条，青金石粉仍保留为 `game=null` 的工具候选。
- 更新 `pipeline.py` 与 `verify.py` 的本批白名单、计数、校验和说明。固定路径/集合、尺寸与冷却剂保护仍有效；对已有路径的合法SVG编辑可通过显式 `--install` 更新PNG，输出字节相同则跳过写入。本次游戏贴图新增仅四个任务路径。
- 使用现有管线生成四个 `generated/item/*.png`、总览和分组页。另提供每项独立的原尺寸、浅底9倍、深底9倍对照图，均已实际打开检查。

## 文件与像素证据

每项均为RGBA 16×16，色板中6色，alpha只含0/255，物品最外圈透明。轴承两图的 `(8,8)` 轴孔中心透明；完整轴承 `(12,4)` 有框像素，半成品 `(12,5)` 为缺口透明像素，且二者色板一致。

| ID | PNG SHA-256 | 对照预览 |
| :--- | :--- | :--- |
| `quartz_dust` | `85297409e459e942681dea8688c703abb43afc49fbf7adfd3fc5734a53aa6dbd` | `quartz_dust-comparison.png` |
| `refractory_brick` | `9f965dac40d4e9a1696170af082202cdc870127c447b9047784cf12baeb861d3` | `refractory_brick-comparison.png` |
| `heavy_bearing` | `6e4c42b261f5864cde2714dcda72d2e53397e49cd1c3b64f4ca90db2396afb92` | `heavy_bearing-comparison.png` |
| `incomplete_heavy_bearing` | `41c811a511a43b0580a3a83dea9cb8e4a47d7045db7013181e003b6c5568e1ac` | `incomplete_heavy_bearing-comparison.png` |

初始游戏纹理目录65张PNG的逐文件哈希归档在 `docs/reviews/2026-10-02/material-05/art/preexisting-game-png-sha256.json`。`verify.py` 只从该仓库内归档JSON读取路径/PNG哈希并逐项比较；构建报告目录中的副本只是历史证据，不是验证输入。验证确认65张旧图全数不变，旧51项基线保留；EXT-ART-02A的8张冷却剂原图仍逐字节符合锁定哈希。最终游戏目录69张PNG、工具清单70条、SVG色板/源映射共66个不同来源，四项素材有JAR/客户端人工验收仍待后续流程处理。

## 验证

实际使用捆绑 Python `3.12.14` 和 Pillow `12.3.0`，设置 `PYTHONDONTWRITEBYTECODE=1`。命令：

```powershell
$py = 'C:\Users\IKSXH\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
& $py tools\art-assets\export.py
& $py tools\art-assets\verify.py
```

`verify.py` 结果为 `PASS`：默认导出两次及 `--install` 两次字节一致；逐项核对源SVG、Pillow矩形参考图与生成PNG；既有物品PNG哈希快照不变；6种CLI非法输入（非法SVG、越界新路径、错误尺寸、越界映射、未经批准的保留声明、缺失保留声明）均返回失败且失败前后状态字节不变。原有负例还覆盖非法标签、样式/变换、非整数与越界坐标、alpha和色板错误、DTD/PI、空SVG及长画布边界；青金石粉和四项已批准样稿未变。

完整验证记录见 `verification.json` 和 `commands.json`。为满足精确安装写集，验证第一次成功走到接入步骤后，后续因证据路径/统计断言修正而重跑；最终完整验证开始时四张本批PNG已存在（目录计69张），并确认先前保存的原65张哈希快照不变、再次接入幂等。早先的失败是校验脚本前置计数/路径断言，不是素材解析或旧图保护失败；最终报告仅采用最后一次通过结果。

**Important整改：** PM补充复核发现上述版本的安装预检曾错误拒绝所有已有PNG的字节变化；原独立审查当时只提出砖边Minor。按批准范围，用临时、色板内的 `quartz_dust` 像素编辑（将一个 `#FFF9E8` 像素改为 `#F2EDD9`）验证修复：原行为先RED，`export.py --install` 返回1并拒绝合法更新，目标PNG保持原哈希；移除该字节不等条件后，对同一编辑GREEN，命令返回0并更新目标PNG。随后在 `finally` 中恢复原SVG、重新安装，并确认原目标PNG、生成PNG、全部预览/生成输出均与编辑前状态相同；原65张哈希快照全程未变。GREEN编辑期间的临时PNG SHA-256为 `0d37207539e4ba7998769ed261e04d6b4db1c91cd18a185f9e81808a0c4152fa`，恢复后回到 `85297409e459e942681dea8688c703abb43afc49fbf7adfd3fc5734a53aa6dbd`。逐步记录见 `update-regression-red.json`、`update-regression.json`。修复没有改变四项交付美术。

修复后的完整 `verify.py` 再跑结果为 `PASS`：70条清单/66个不同来源/69张游戏PNG，默认导出与安装分别重复一致，六类CLI非法输入失败不写，51历史图、65张开工快照和8张冷却剂保留图均通过。

**最终快照路径整改：** PM终审发现原 `verify.py` 无条件从未跟踪的构建目录快照读取基线。先将该单个文件在本报告目录内安全临时改名，旧版脚本副本因 `FileNotFoundError` 返回1（RED）；将验证输入改为仓库归档JSON后，完整 `verify.py` 命令在构建副本缺失时返回0（GREEN）。`finally` 已原样还原该构建副本；归档和构建副本目前均为65条且内容相同，65张旧PNG及四张最终素材PNG哈希全部未变。验证脚本不锁定JSON文件原始字节/换行格式，避免Windows检出换行差异影响读取。新记录为 `snapshot-resolution-red.json` 与 `snapshot-resolution-green.json`；原 `verification.json`/`commands.json` 已先备份到 `before-snapshot-fix/`，前一版本的验证输出另存于 `before-json-layout-neutrality/`。

未运行 Gradle、Minecraft 游戏或客户端。本报告证明的是离线素材、白名单和打包源文件层面的结果，不代表游戏内显示或材料流程的人工验收。

## 实际使用技能

- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：采用Minecraft资源的16×16 RGBA规格，并核对 alpha、资源ID映射和客户端人工视觉验收边界。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/executing-plans/SKILL.md`：按实施卡精确范围执行、用交付报告记录验证证据。其通用Git步骤服从仓库执行者的Git禁令；本任务未执行Git写操作。
- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：原美术轮未读取；于2026-10-02（本次Important整改）补读。核对 `gradle.properties` 中 MC 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82 和 Flywheel 1.0.6；据其资源目录/16×16纹理边界确认本次只修美术导出工具，不触碰注册或游戏代码。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：原美术轮未读取；于2026-10-02（本次Important整改）补读。将验证限定为色板合法编辑的CLI RED/GREEN/恢复，以及既有 `verify.py` 的导出/负例检查；本任务无运行时逻辑变更，按卡约束未运行GameTest、游戏或Gradle。

## 交付文件

新增源稿：

- `tools/art-assets/sources/item/quartz_dust.svg`
- `tools/art-assets/sources/item/refractory_brick.svg`
- `tools/art-assets/sources/item/heavy_bearing.svg`
- `tools/art-assets/sources/item/incomplete_heavy_bearing.svg`

其余本任务写集为 `tools/art-assets/palette.json`、`manifest.json`、`pipeline.py`、`verify.py`，管线生成目录 `tools/art-assets/generated/`、`preview.png`、`preview.html`、`previews/`，以及 `src/main/resources/assets/create_nuclear_industry/textures/item/` 下的四张对应PNG。证据及对照图保存在本报告同名目录。
