# ART-MCMOD-COVER-13 封面执行报告

2026-10-14。执行者仅实施本批封面构图、导出与报告，不取得负责人或项目经理权限。已读当前AGENTS、本卡、治理1.2/5.1与美术README职责；使用brainstorming整理已批准的短版式，使用verification-before-completion按实物核对出口，沿用负责人既有分工，没有子委派。

## 成品与来源

左侧保留原R2厚铸钢Logo，中文“机械动力：”/“核工业”与英文“Create:”/“Nuclear Industry”有序换行；深钢灰底、暗铜细线和冷蓝英文与核芯。右侧绑定本批真实5×5×5模型渲染，保持比例与完整轮廓，没有额外压暗或裁掉顶部、边缘和窗口。

Logo直接读取`tools/art-assets/mod-logo-12-hd/source/mod_logo_hd.svg`，SHA-256 `cd763c08bf7f29ce21aef613230fd9dc37c06b826a5dbd2f7ec776e862b16c2c`。原几何、配色及绘图节点保持；构图只改变嵌套SVG视口位置/尺寸。ZIP内另保留逐字原文件。

背景由独立执行者交付`background/reactor-5x5x5.png`，负责人实际核图并绑定2048×2048 RGBA及SHA-256 `c4493e3b6953478e0361aa3afb39c92a1be9ff8da598778eff8d9c39bebca249`后才正式导出。五格外形、顶六换料口/中央三驱动器、前面3×3连窗与内部蓝色燃料棒来自该模型渲染，不是游戏截图。实际布局与模型来源的证据由背景执行者报告持有，本执行者没有重做布局或改模型。

本机实际字体`C:/Windows/Fonts/msyhbd.ttc`（微软雅黑粗体）SHA-256 `519309b7ab0479c4dc3ace5e291de5a8702175be5586e165bc810267bd4619a5`；使用已存在@napi-rs/canvas将标题转为路径，另保留文字可编辑版。ZIP不分发字体文件。成品SVG是可编辑的矢量版式与Logo、内嵌栅格反应堆背景，不能称整张纯矢量。

## 一次正式出口

`tools/art-assets/mcmod-cover-13/export-cover.cjs`在背景冻结后执行一次正式导出，退出0；此前仅导出明确draft的品牌版式，没有将空白预留图登记为最终。现有bundled Node、sharp、@napi-rs/canvas与jszip直接复用，没有安装依赖。

| 交付 | SHA-256 |
| --- | --- |
| output/mcmod-cover-1920x1200.png | `77a11300fae138b917be130d3675ecde141757784b0cbd1a9daa5020245d86bc` |
| output/mcmod-cover-960x600.png | `7d6ef584bca6334385cb9fc41cf5892c326b10990ba686a45049a20f300a965c` |
| output/mcmod-cover-240x150.png | `4ebe5fa212ba9f4a10a815180e08850b9ad7654d2662a309ea923cb8874cdc14` |
| mcmod-cover-13.zip（1,463,744字节） | `fb0336423ed09232328d91f778b5f990cbbd18289de7d9993aad347197a3f1f4` |

ZIP共11个文件：三PNG、路径版SVG、文字版SVG、manifest，原Logo、背景PNG、导出器、有限验证器、README。显示所需引用全部内嵌，路径版SVG/PNG不依赖字库；文字编辑源需要使用者具备适当字体。原始反应堆渲染场景/布局另在background交付目录保留，没有冒称ZIP内含第三方渲染库或游戏资源全集。

## 有限验证与可视观察

`verify-cover.cjs`实际执行退出0，18/18通过，证据为`build/reports/art/ART-MCMOD-COVER-13/cover/verification.json`。核对三尺寸8:5比例及SHA、准确中英名称、成品标题轮廓、背景2048方图与透明边缘、data URI来源身份、ZIP逐字输出身份及无草案/系统字体文件。原Logo已验收证据复用，不运行旧Logo整套检查、Minecraft、Gradle、JUnit/GameTest或模组打包。

执行者实际查看正式960×600及原生240×150：中文和英文没有缺字，Logo/标题不覆盖右侧设备，反应堆五格完整外缘、铜色顶件、窗口内蓝管可辨。原生240版蓝管细节因缩略减弱，但窗口与蓝色线仍可见。本次内部观察不代替负责人实看、独立窄审或用户外观选择。

报告交回时：正式图和有限证据已交负责人，等待同次独立组合窄审；没有登记用户视觉验收、上传网站或合入主分支。

## 写集与边界

只写`tools/art-assets/mcmod-cover-13/`中除background/之外的新工具、README、draft/、output/和ZIP，以及指定cover/证据与本报告。background目录只读，旧Logo11/12、其他素材、src/Java/Gradle、主目录、第三方库、配置、世界与JAR均未写入。无Git写操作。
