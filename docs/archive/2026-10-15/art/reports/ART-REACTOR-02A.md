# ART-REACTOR-02A 执行报告

日期：2026-10-09。执行者仅实现本卡资产准备，未取得美术负责人或PM权限。工作区为`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，HEAD基线`6bcdbdd45d22029f7b60208bd14172e5c815ecad`。资源和工具已准备完成，交负责人一次规格/质量独立审查；用户视觉门与游戏成型接入门保持独立。

## 本批交付

- `tools/art-assets/reactor-ct/sources/`：14名各16份可编辑16×16整数rect SVG，共224份。
- 同目录`palette.json`：14套色板，每套7–12种实色；`mapping.json`：schema 1、Create RECTANGLE、16种上下左右连接布尔上下文及14项完整sprite接口。
- `export.py`与`README.md`：只消费SVG的定向导出器与实际运行说明，没有游戏安装入口或输出路径扩展参数。
- `generated/`：14张64×64图集、224张16×16图块。
- `build/reports/art/ART-REACTOR-02/`：14张开工游戏参考图、输入哈希、四张预览、实际检查命令/stdout/stderr、定向检查脚本及制作展开记录。

14名严格为任务卡列出的外壳side/top/bottom、冷热口side/top、窗口、仪表口side/top、换料口side/top及控制棒驱动器side/top。未新增其他sprite。

图集列0/1/2/3为左右不连/仅右/双侧/仅左，行0/1/2/3为仅下/上下/仅上/都不连，`index=column+4*row`，孤立格12。完整ID和相对路径在mapping：original=`create_nuclear_industry:block/<name>`，target=`create_nuclear_industry:block/reactor_ct/<name>`；target仅供后续消费，本批没有相应game文件。

负责人在首轮检查后同步主PM接入合同，最终target撤下建议的`_ct`后缀；本卡已同步，工具图集名仍为`generated/<name>_ct.png`，未来game名为`textures/block/reactor_ct/<name>.png`。仅调整mapping、导出器校验字面值及说明，SVG/PNG像素没有改动。只读接口与拒绝负例定向复验记录为`interface-check.log`、`interface-commands.json`及`interface-verification.json`；复用本轮已审像素、重复导出及铺设证据，不重复全套验证。

## 制作与来源

只读参考同工程14份既有SVG及14张当前游戏PNG。外壳、冷热口保留ART-REACTOR-01候选的暖灰钢、铅灰压板、浅暖灰混凝土、红横标与蓝竖标。仅结构外沿保留两像素钢框，真角才有螺栓；内部撤下每格箱体粗框，保留克制的板材接缝。顶底为封闭盖板与加固面。

仪表、换料及驱动器从既有SVG矩形裁取可辨识的局部图案，背景钢灰统一为本批暖灰；没有新增旋钮、动态读数或运动状态。窗口孔仍为x/y均4至11的8×8透明区域，通过SVG没有覆盖该区域实现。其余13类全不透明，alpha仅0/255。没有复制第三方资产，没有生图或用Pillow直接绘制游戏图块。

一次机械展开记录为`build/reports/art/ART-REACTOR-02/seed-sources.py`。正式`export.py`只读取交付SVG、palette和mapping，复用未改动的`tools/art-assets/export.py`中的`render_svg(text, allowed_colors, (16,16))`，图集只粘贴其渲染结果。共享渲染器16×16/16×64尺寸合同保持不变。

## 实际技能和环境

已读取主工程最新AGENTS、本工作树AGENTS、美术入口、治理1.2/5.1、唯一任务卡、已批准设计及ART-REACTOR-01报告，按治理5.1精简验证。实际读取并应用：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：读取gradle.properties核对MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；核对14项原sprite确实被7份现行模型引用，不照搬新版本模板。
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：保持现行模型和命名空间，检查16×16 RGBA、有限色板、alpha和图集引用尺寸，不将图集误当动画条或OptiFine CTM。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：按资产/工具范围执行纯离线断言和负例，区分离线资产结果与Minecraft行为，不增加GameTest或构建。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/verification-before-completion/SKILL.md`：依据本轮实际命令与输出交付，任务卡与治理优先于通用重复全量检查。

使用现有捆绑Python/Pillow，所有Python调用加`-B`，未安装依赖、改版本或写Git。

## 命令与实际检查结果

所有shell命令显式cwd=`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`。初次默认导出命令为：

```powershell
$env:PYTHONIOENCODING = 'utf-8'
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B tools/art-assets/reactor-ct/export.py
```

实际stdout：`导出通过：224图块、14图集、4预览；实际更新242文件；未安装游戏资源`，见`first-export.log`。

一次完整必要验证命令为：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B build/reports/art/ART-REACTOR-02/verify-candidate.py
```

脚本顺序执行锁定索引断言、`export.py --check`、一次重复默认导出、两个内存负例、像素/选格/几何断言、范围保持和Git只读检查。真实子进程参数、cwd、退出码、stdout/stderr保存在`commands.json`；当次完整日志为`checks.log`，逐项结果与242产物SHA-256为`verification.json`。

| 本次检查 | 实际结果 |
| :--- | :--- |
| 16种上下左右上下文 | 16/16唯一且完整，与锁定行列相符；孤立格12 |
| `export.py --check` | 退出0；224图块、14图集与SVG预期PNG字节一致，无额外生成物 |
| 一次重复默认导出 | 退出0、更新0文件；242份图块/图集/预览SHA-256及mtime完全相同 |
| 非法SVG | 最后一项`control_rod_drive_top/tile_15.svg`的内存副本追加circle；写前抛ValueError |
| 非法mapping | 内存副本将index6的up改为false；写前抛ValueError |
| 两个负例范围保持 | 242份既有产物内容及mtime均不变；正式源稿未改 |
| 图块/图集 | 224/224格位与RGBA字节正确；每格7–12色，窗口计入透明仍≤16 |
| alpha/钢框/螺栓 | 窗口孔精确8×8，其余全不透明；四向未连接边有钢框，仅真角有螺栓 |
| 选格 | 角/边/中间、2×2、一行/一列、6类长方形、跨owner及未成型断言通过 |
| 三面几何 | 3/3不同长宽高投影面共享角/底部垂边端点一致 |
| 无字体的只读检查 | 替换字体入口为抛错函数后check仍通过；check不构造字体/预览 |
| 开工相关输入 | 47/47旧输入SHA-256保持；14项现行模型sprite齐全 |
| `git diff --check` | 退出0；已有PLAN.md仅提示Git后续LF转CRLF，非错误 |
| 新增文本空白 | 228份源稿/工具/接口/说明直接检查通过，覆盖Git未跟踪文件 |

两个负例当次实际捕获并写入日志的异常文本为：

```text
非法SVG实际拒绝：ValueError: 只支持直属 rect 及 x/y/width/height/fill 五个属性
非法mapping实际拒绝：ValueError: mapping必须完整匹配锁定的14项sprite、RECTANGLE索引与相对路径
```

没有把推断的错误分支文案当作stdout。初次源稿展开的中文控制台编码显示不佳；正式导出和检查通过`PYTHONIOENCODING=utf-8`记录真实中文输出，不覆盖这条历史情况。

## 预览与实际复看

执行者已实际打开四张最终预览。负责人随后实际看过墙面、几何及归属页，确认首稿方向内部通过、无需改图；这只是方向检查，不替代一次最终独立审查或用户视觉门。

- `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-REACTOR-02/indices.png`：14类16格，原16像素与最近邻4倍并列。
- `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-REACTOR-02/wall-preview.png`：完整5×5混合正面墙、中间外壳格、冷热口/窗口/仪表/换料/驱动器，顶底6×3及外壳2×2铺面。
- `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-REACTOR-02/geometry-preview.png`：闭合5×5×5、6×5×8、9×7×5等距三面与正交正面；后两者标注尺寸适配假设、非合法玩法尺寸。
- `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-REACTOR-02/owner-preview.png`：不同owner接壤各自封边、原游戏独立方块/连接候选前后对照、透明窗后方双色对照、缩小/灰度/降亮55%模拟。

几何使用统一坐标投影`P(x,z,y)=(ox+(x-z)a,oy+(x+z)b+yc)`，顶/正/右面共用真实端点，确认没有V形展开片冒充cube。等距预览可辨认功能件，但离线倍率不能证明Minecraft昼夜、过滤或管路遮挡效果。

## 范围保持与交付限制

`baseline.json`在开工保存14张当前游戏参考图、14份现有SVG、ART-REACTOR-01七张generated、共享palette/export/manifest/pipeline、首批工具及7份相关模型的47项哈希。本批验证全部不变，ART-REACTOR-01七SVG/游戏PNG/色板及既有候选完整保留。

执行者新增仅在卡片指定`tools/art-assets/reactor-ct/`、本报告及本批build证据目录。负责人并行写入的`docs/art/ART-REACTOR-02-INTERFACE.md`等不属于本执行者改动。未写game PNG、共享SVG/palette/export/manifest/pipeline、Java、JSON模型、语言、Ponder、配方、构建脚本或Git，未构建、运行GameTest、客户端或历史全量导出。

这份交付只准备图集和后续消费接口。服务端成型身份、客户端bounds/owner/刷新同步及真实渲染接入仍由逻辑侧协调；本报告不能据此宣布游戏成型连接、可变反应堆玩法或用户视觉验收完成。资源/工具冻结摘要见`build/reports/art/ART-REACTOR-02/frozen-manifest.json`，后续整改须由负责人明确指派范围。
