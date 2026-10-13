# ART-TURBINE-07A：汽轮机机壳SVG贴图三稿

2026-10-11用户直接要求“先出机壳的贴图重置吧，先出几个设计稿看看”。本批交可选择的机壳贴图设计稿，承接上一轮三方向讨论；不是整机建模、转子/端口/动画或正式资源接入。锅炉按用户要求保持当前版本，原候选与证据只读。

## 职责、工作区与参考

负责人为用户直接任命的美术对话（`01a11c6b-f440-77c1-9275-5b2dca5cfa0f`）；治理1.2允许在美术范围制定任务并派发执行，子Agent没有负责人/Git权限。样稿实现交原SVG作者`reactor_svg_impl`，只在 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio` 写，当前HEAD `b8cb60a`。保留全部锅炉未提交改动、日志与存档；本批沿用现目录，不开分支/工作树，不刷新存档。

实际来源：`src/main/resources/data/create_nuclear_industry/recipe/turbine_casing.json`的钢板/铜板/强化钢板；`TurbineGeometry.java`三档3/5/7格八棱截面；`tools/art-assets/turbine_assets.py`及`tools/art-assets/svg/block/turbine/turbine_casing_panel.svg`、`turbine_casing_endcap.svg`的现有纹理；`turbine_models.py`与既有安装态模型预览。深钢与暖铜可占主体，亮边属于钢或铜，不加入无材料依据的大面积黄铜/金色。沿用Create像素材质、高反差凹槽及上亮下暗的立体感，避免大面积纯色和过密噪点。

## 三个方案

- **A 分体钢铜机壳（建议方向）**：铜质上盖、深钢下壳，连续中腰检修分缝与成组螺栓；端盖为厚钢边缘和铜内嵌片。以分体机壳和长轴方向强化机械辨识度。
- **B 分段轮舱机壳**：钢面为主，沿轴向有规律地分段，环形钢带与窄铜缝区分轮舱；环带明暗有厚度，不能退化为原锅炉纹理换色。
- **C 纵筋检修机壳**：沿长轴贯通的深钢筋条与凹入铜面，钢边高光、局部横向检修接缝；通过纵向结构与面板占比区别A/B。本批不新增观察窗设计或外露叶轮。

每稿纹理结构和材质占比实质不同，不只换色。三稿共用同一5×8×5八棱外形和投影尺度，示意只替换表面材质，不画不同几何来制造优劣。可用少量前/中/后模块表达轴向延展，不穷举尺寸；保留既有占格和模块逻辑，整机示意无需画控制器/端口/输出轴新设计。

## 精确写集

- 只新增 `tools/art-assets/turbine-casing-concepts-07/`：A/B/C独立目录，每稿侧面、上盖、端盖等3～5张16×16 SVG原稿，另加少量边界/内部连接示意模块；`overview.svg`、各稿`board.svg`、`index.html`、README及小型生成/源检查脚本与manifest。总览至少有同视角八棱机壳、单格放大、连续平铺与去内框连接对照、色板和中文说明。默认总览，可切单稿查看；清楚标“设计稿，未接入游戏”。
- 新低分辨率纹理仅整数坐标rect，16×16 canvas，`shape-rendering="crispEdges"`，有限RGB色板（目标每tile不超过14色）。展示板可用text、polygon、transform和内联symbol/use实现投影；新设计不可嵌入位图，不直接生成图片。可只读内联旧SVG作为当前机壳参考。
- 执行报告只写 `docs/art/reports/ART-TURBINE-07A.md`；检查证据只在 `build/reports/art/ART-TURBINE-07A/`。负责人管理本卡、PLAN及浏览器复看证据。

禁止修改任何旧美术工具/SVG/正式资源、Java/测试/配置/配方/语言/模型或锅炉；不运行旧export/install、Gradle/JUnit/GameTest，不启动Minecraft、写存档或Git，不再委派。设计稿的连接效果只是候选示意，不宣称已实现CT。

## 必要检查与交付

开工读取用户最新AGENTS（优先于本地旧文档）、美术README/PLAN、治理1.2/5.1及 `C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-resource-pack}/SKILL.md`。锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。手写注释中文；不采用技能中的升级/Git/清理建议。

先生成三稿与预览，用bundled Python `C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe -B -X utf8`运行一次小型源规格/引用检查，确认整数SVG、三稿差异、模块明暗保持、PNG_created=0及本批未写正式路径；不做大型保护矩阵、镜像单元测试或独立代码审查流水线。报告准确列命令、退出码、SVG数和入口。作者完成即停写；负责人实际浏览器复看总览、三个单稿及平铺，再交用户选择。风格未选不得正式替换素材或推进转子动画。

## 样稿交付记录

三稿已交[短报告](../reports/ART-TURBINE-07A.md)与[预览首页](../../../tools/art-assets/turbine-casing-concepts-07/index.html)。共12张16×16材质源、3张方案板及1张总览，PNG_created=0。负责人实际浏览器复看总览、A/B/C单稿与平铺/去内框示意；修正展示分组闭合、八棱投影厚度及A上铜下钢后，三个方向在相同外形下可比较。截图存于`build/reports/art/ART-TURBINE-07A/overview-browser.jpg`，只作预览证据。

最终小型源检查退出0，12源合法、每源5～8色，结构差异与入口检查通过。初次检查器文件后缀取键错误退出1，仅修检查器后重跑；两次原始证据保留，未重复生成SVG或启动生产构建。负责人已读取最终原始输出/退出码与实际检查范围；作者停写。当前仅完成供用户选择的设计稿，尚未选择风格或接入游戏。
