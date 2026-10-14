# ART-BOILER-06 素材候选报告

已在唯一候选目录 tools/art-assets/boiler-steam-06 完成阶段一素材。普通蒸汽采用乳白灰色板（#A4A9A8、#D5D7D3、#F7F4EC），超临界蒸汽采用青白色板（#76B7BD、#B1E1E3、#F0FFFA）。still为团雾形，flow为斜向上翻卷云纹；帧内有明暗层次和大小云团。每种材质、每类图案各8帧，共32张16×16整数rect SVG。对应still/flow每帧的两种材质共享同一像素遮罩和亮度等级，只有色板不同，便于逐像素连续混色。源帧保留alpha 0/255，不烘入内部半透明。

四个atlas候选和注册路径如下；每个atlas是16×128纵向八帧，mcmeta为2tick、8帧、interpolate=true：

- sources/normal/still → generated/atlas/steam_still.png → create_nuclear_industry:fluid/steam_still
- sources/normal/flow → generated/atlas/steam_flow.png → create_nuclear_industry:fluid/steam_flow
- sources/supercritical/still → generated/atlas/supercritical_steam_still.png → create_nuclear_industry:fluid/supercritical_steam_still
- sources/supercritical/flow → generated/atlas/supercritical_steam_flow.png → create_nuclear_industry:fluid/supercritical_steam_flow

这些候选对应原 textures/fluid/ 四张PNG及原注册sprite。独立内部帧共32张16×16，输出在 generated/internal/textures/boiler_steam/{normal,supercritical}/{still,flow}_0..7.png，消费目标相对路径为 textures/boiler_steam/{normal,supercritical}/{still,flow}_0..7.png。内部alpha仍由顶点控制；消费端可依固定材质权重合成单个16×16动态纹理，再按实际超临界库存占比连续混色。该固定权重仅属材质表现，不表示库存或流向规则。

preview/index.html展示四组循环及透窗低/高浓度、连续混色滑条；preview/contact-sheet.png并列显示32帧。负责人复看之前，本批不安装正式PNG或mcmeta，不修改Java、模型、注册或旧SVG/工具。

最终素材命令使用bundled Python：
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/boiler-steam-06/pipeline.py author
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/boiler-steam-06/pipeline.py build

author与build最终均exit 0；实际命令和输出见build/reports/art/ART-BOILER-06/assets/generate-command.txt、generate-output.txt。

唯一源/帧检查命令：
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -B -X utf8 tools/art-assets/boiler-steam-06/pipeline.py check

实际exit 0。检查确认32/32 SVG有效、对应两色板的遮罩/明暗16/16一致且各组八帧互异；4/4 atlas为16×128并与源SVG逐帧逐像素一致；4/4 mcmeta符合8帧/2tick/interpolate；32/32内部帧为16×16且逐像素同源，alpha仅0/255；mapping中的四个注册引用和内部帧消费路径正确，预览入口存在。Pillow输出Image.getdata弃用提示，不影响结果。真实命令、stdout和exit码保存在build/reports/art/ART-BOILER-06/assets/check-command.txt及check-output.txt。

素材候选生成与检查阶段未安装正式资源；阶段一安装补充记录随后按新增授权完成的安装。整个素材批次未运行Gradle、游戏或Git操作；游戏内透窗、浓度与材质显示仍待用户视觉复看。

## 阶段一安装补充

按负责人追加的阶段一安装授权，将四张流体atlas PNG及四个对应`.png.mcmeta`，以及普通/超临界still/flow各八张内部帧，从既有`generated/`逐项复制到任务卡指定的正式纹理路径。未重生成素材、未重跑源检查，也未改其他资源、模型或代码。

安装及唯一逐字节核对命令：
`& 'build\reports\art\ART-BOILER-06\assets\install-and-compare.ps1'`

实际退出码为0；逐项使用.NET字节序列比较，结果`installed_and_byte_identical=40/40`（36 PNG、4 mcmeta）。真实命令、逐项stdout、汇总和退出码分别保存在`build/reports/art/ART-BOILER-06/assets/install-command.txt`、`install-output.txt`、`install-exit.txt`；执行脚本为`install-and-compare.ps1`。素材检查/构建日志沿用前述已审记录，本次未重跑。消费者接续及真实游戏视觉复看仍待完成。
