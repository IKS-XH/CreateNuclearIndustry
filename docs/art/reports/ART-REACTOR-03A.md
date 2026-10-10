# ART-REACTOR-03A内部模型/SVG素材实施报告

未提交独立资产候选已冻结；负责人实际看四张主预览后通过方向，要求的逐管辉光偏置已烘焙。仅素材段，不追记02R1用户视觉通过，不表示运行HU/t/实际棒深/库存已接游戏。无正式资源安装、Java/同步、构建、客户端/浏览器/服务启动或Git写；不继续消费阶段。

## 范围、来源与技能

唯一工作区`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`；所有shell显式workdir，HEAD `8b83a1a97f1b539e6dcc7b6714b9ee96c13854e0`。实际读取本树与主工程AGENTS、治理1.2/5.1、美术入口、03设计与03A卡，包括偏置烘焙补充。复用已读minecraft-modding/testing/resource-pack并本批读取相关入口：NeoForge OBJ/资源路径、PartialModel分件、16px RGBA/SVG源与定向资产验证；应用verification-before-completion证据门，按治理5.1资产阶段不运行Java测试/构建。手写注释/说明中文。MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6不变，Python-B/PIL与NumPy2.3.5使用捆绑运行时，无依赖安装。

只读核对原燃料架cube_bottom_top模型、钢板＋钢格栅有序配方；控制棒配方钢杆＋吸收陶瓷＋黄铜板及原item/驱动壳模型。参考原燃料架和控制棒材质，冷青/热橙色族来自本项目compound_coolant/hot_compound_coolant原纹理；新增专用低对比流纹，不改全局液体。现有turbine NeoForge OBJ JSON、相对MTL、map_Kd资源ID及TurbineRotorRenderer PartialModel模式只读参考。全部候选原创，无外部Mod资产或图片生成。

基线`baseline.json`保护27项确切实际输入：原模型/配方/材质、OBJ/MTL/Partial参照、共享SVG renderer、02R1冻结清单/候选JAR，以及预览用原壳/9张已审窗格。最后27项hash一致。当前JAR仍SHA256 `ca591c0d64b6d184930bd42ba65a2c1647ab9ac8decdf9206ed94f788f80d533`，02R1源/图集/证据/副本均未写；复用旧保护证据，不重跑944集合或旧CT/像素/构建矩阵。

## 交付与消费约定

唯一工具目录`tools/art-assets/reactor-animation/`含19个SVG、5个rig/palette/mapping/generate/README、33个generated产物，共57路径；唯一实施报告另1路径。SVG均16×16整数rect、RGBA≤16色、alpha0/255；钢/棒/冷热作者源全不透明，辉光局部透明。所有低分辨率游戏候选PNG只来自保存SVG渲染，不重画PNG。

| 模型 | 实际结构/单位 | OBJ面数 |
|---|---|---:|
| reactor_fuel_rod | 方块范围0..1，底板Y0..1/16/顶板15/16..1，九根八边钢管轴心4/8/12模型单位，外接直径2.5单位，Y1..15单位 | 228 |
| fuel_rod_glow | 九管侧面，不含顶底板；每管半径1.25/16+0.0005方块，轴心/Y不动；偏置已烘焙，消费者不再次缩放 | 72 |
| control_rod_shaft | X/Z=.5，外接直径3单位，Y0..1规范长度，八边陶瓷/钢芯读感 | 24 |
| control_rod_head | 同轴固定Y0..2/16、外接直径3.5单位，仅端箍黄铜 | 24 |

16模型单位=1方块，Y向上，显式外法线/右手绕序/UV，flip_v=false，每面无反向副本。板/钢管/棒体/端部闭合，辉光侧面两端故意开放由静态钢板封遮。shaft只按正式body长度缩放一次、head不缩放，两者按实际深度整体平移；深度不改变棒长度，预览三格行程不是正式消费者写死3。驱动器示意壳静止，当前露出上方的完整棒体保留已审方向，不假用目标滑条数据。

静态ID仍`create_nuclear_industry:block/reactor_fuel_rod`；三个partial为`create_nuclear_industry:block/reactor_animation/{fuel_rod_glow,control_rod_shaft,control_rod_head}`；OBJ/MTL未来路径`models/block/reactor_animation/mesh/`，MTL相对文件名与map_Kd资源ID均正确。三材质目标`create_nuclear_industry:block/reactor_animation/<name>`。仅生成候选，尚未注册PartialModel或安装模型/纹理。

冷/热各8源/图块，16×128竖向帧表，每帧相位前进2px，07→00同样连续；空间边缘差不超过内部邻像素差。animation元数据仅记录于mapping（width/height16、frametime2、interpolate true、frames0..7），本段不写正式mcmeta。一个合法EMPTY/CONTROL_ROD网格按总库存定统一液位、按hot/total连续混色，不叠冷/热双透明盒、不固定上下分层、不填燃料列。总库存0/容量不可用应不画，正式网格和真实输入等待L2合同，不从outer bounds猜。

## 实际预览与方向核对

`build/reports/art/ART-REACTOR-03A/preview.py`读取实际保存OBJ的v/vt/vn/f，正交三角形深度测试并采样真实UV/候选SVG纹理；不是绘图竖线伪装几何。预览光照/alpha及驱动器/右侧围壁参照仅离线示意，使用只读原材质；窗口复用02R1已审图块，不重复旧验证。preview-provenance.json绑定真实OBJ/rig输入SHA。

已自行实际打开模型/辉光/棒深/液体四PNG，负责人也实际查看并通过：

- `models-preview.png`：单模块与3格堆叠正/侧/斜/顶，闭合接头、八边棱面及板厚。
- `glow-preview.png`：0/低/中/高（离线alpha0/.18/.42/.65），同列强度一致、钢面与板保留；正式驱动须真实列generatedFissionHeatHuPerTick。
- `control-preview.png`：0/50/100%实际深度示意，杆长和端部固定，整体升降，壳体静止。
- `coolant-preview.png`：0/50/100%液位×0/50/100%热占比透窗九组合，仅示意EMPTY/CONTROL_ROD体积，无燃料列液体。
- `animation-preview.gif`：8个保存SVG帧＋实际OBJ/rig投影短循环，冷热混比和杆深都是标注的离线输入；`animation-frames.png`是当前实际GIF解码联系页，已打开核对。GIF无服务/浏览器，非客户端运行实证。

负责人要求逐管偏置烘焙后重新生成真实glow OBJ并投影，预览不再运行偏置，已自行打开更新辉光图；其他已审方向保持。世界照明、客户端透明排序、实际HU/t/深度/卡死/库存可靠性均未验证，应由后续正式消费卡完成。

## 必要验证与真实输出

`verification.log/.exit/.json`：实际exit0。独立解析生成OBJ，核对顶点/面索引、法线长度与绕序、平面/UV0..1、无重复面；钢板/九管/棒/端部逐组焊接边计数全部闭合、正向体积>0；辉光每组8条内部双用边＋16条端口单用边符合仅侧面设计。九轴/八角环/半径/Y区间均核对，偏置烘焙且轴心不动。JSON/OBJ同名MTL与材质资源ID核对；三材质和16帧/2sheet逐字节对应SVG渲染。specific-geometry-seams补核控制杆/端部真实轴心、半径/长度和冷热X/Y周期缝，exit0，不再次生成或重复旧矩阵。

8条定向负例实际抛ValueError，原异常文本在verification.json：外部OBJ引用、重复径向放大、九轴心漂移、偏置未烘焙、破坏帧循环、钢材透明孔、非rect源，以及独立OBJ反向面。全部写前拒绝，候选SHA/mtime不变；反向面真实报`实际OBJ面绕序与法线不一致`，帧坏例真实报`流动首尾相位不连续`。不是编译失败或源码字符串测试。

唯一重复生成实际改变0文件，全部33产物字节及mtime保持。19源与33候选只读check通过，手写路径空白0错误，所有命令/原日志及exit0保存在commands.md及对应文件；部分终端中文有系统管道编码显示差异，JSON原异常为UTF-8证据。没有运行Java tests/Gradle，因为未安装资源且无行为实现。

## 冻结与未完成门

冻结清单`build/reports/art/ART-REACTOR-03A/frozen-manifest.json`绑定58交付路径和当前03A证据，源/候选逐项SHA可追踪（清单不自包含）。final-git-status.txt仅只读状态记录，保留既有候选/自动日志及负责人并行文档，本批不回退/清理或更改任务状态。

仅交付未提交素材候选和报告；停本段。未来正式资源安装、PartialModel/BER/运行投影/L2同步及真实客户端运行动画，需负责人另发正式消费卡，不由本资产段擅自接入；02R1用户视觉门仍独立保持。
