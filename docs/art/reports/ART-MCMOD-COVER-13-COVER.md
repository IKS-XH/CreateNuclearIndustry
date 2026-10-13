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

## R1 原稿归档与等待背景绑定

2026-10-14用户反馈棒体发黑、冷却液未充满；前述原18/18和内部观察仅是原交付时点，不能追认这两项视觉通过。按原卡R1局部接续，封面执行者没有重做Logo、字体或版式，也没有修改背景。

已将原三PNG、两SVG、manifest、ZIP、export-cover.cjs、verify-cover.cjs和README完整复制至`tools/art-assets/mcmod-cover-13/history/pre-r1/`，10项逐字SHA一致；另保留原报告与原verification.json快照。原图/ZIP身份均为上表原交付值，归档证据见`build/reports/art/ART-MCMOD-COVER-13/cover/r1/pre-r1-archive.json`。

R1有限验证入口准备了三尺寸原稿像素比较：逐像素比较固定反应堆视口之外的区域和左侧品牌区，并核对两SVG去除内嵌背景后的正文逐字一致。背景冻结前没有生成R1最终图；等待负责人绑定修后2048背景SHA后，仅更新expected-bg、说明与必要三尺寸/ZIP出口。

## R1 实际增量交付

负责人实看修后背景并独立核来源后，明确绑定2048×2048 RGBA背景SHA-256 `3254b7728b5f3b085cca8600d3d1197d2567ba96a7db73a4a4005d7ed0436bb0`。在上述等待阶段之后，封面只更新export-cover.cjs的冻结背景身份和README的R1产物说明，一次重新导出原三尺寸、两SVG和ZIP，退出0；没有再次制作品牌草案。

| R1稳定入口 | SHA-256 |
| --- | --- |
| output/mcmod-cover-1920x1200.png | `b4e576abc1beee65c99dfcc0e6578c9493c15f09bbe5622a2ca9ab990d0bec0c` |
| output/mcmod-cover-960x600.png | `d351a1435eed78fd7756533f4861fde067840222edafb1298c4e690e0e62e561` |
| output/mcmod-cover-240x150.png | `0f5bdd37600875d403f7f44109c987b66cb969838398586d1613938f0163337d` |
| mcmod-cover-13.zip（1,649,014字节） | `7b0967afce3cf79a835e9a8d94377b84b49a73d9483fb0ee66f5989f18acea62` |

`verify-cover.cjs --r1`实际运行23/23、零失败、退出0。原verification.json未覆盖；R1实测保存于`build/reports/art/ART-MCMOD-COVER-13/cover/r1/verification.json`，原18项仅保留前述原时点。两SVG去掉背景data URI后的正文逐字一致，证明原Logo节点、文字/轮廓、字体标记、位置与版式保持。

| 尺寸 | 整图变化像素 | 视口外比较像素/变化 | 左侧品牌区比较像素/变化 |
| --- | ---: | ---: | ---: |
| 1920×1200 | 62162 | 1084200 / 0 | 906000 / 0 |
| 960×600 | 15669 | 271050 / 0 | 226800 / 0 |
| 240×150 | 1049 | 16972 / 0 | 14250 / 0 |

比较区由原SVG反应堆固定视口`x=755,y=72,width=1140,height=1070`按三尺寸比例计算；视口外及左侧`x<755`的RGBA像素全部与pre-r1原图相同，而变化确实发生于反应堆区。不是仅比较整图SHA或把字体重新生成称为未改变。

执行者实际查看R1正式960和原生240：窗口由黑色内部改为满青蓝液体包络，灰钢棒体/铜带及燃料蓝辉可见，保持原五格轮廓、顶部、名称与Logo。冷却液是背景执行者创建的静态3³模型宣传姿态，不是服务器库存或流量声明；SVG仍内嵌栅格模型渲染，没有冒称全图纯矢量或游戏截图。

R1成品与实际文件身份已交负责人实看960/240，当前停写等待原同次独立窄审；未登记用户外观验收。原ZIP fb033642…与原全部输入/出口保留在history/pre-r1，不改旧报告结论或原证据时点。没有写背景目录、旧Logo、其他素材、main、游戏源码、JAR、配置、世界或Git。
