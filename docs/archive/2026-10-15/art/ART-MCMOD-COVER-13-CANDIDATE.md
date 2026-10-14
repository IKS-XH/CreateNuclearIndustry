# MC百科简介封面交付

## 当前 R1 修正版：2026-10-14

用户指出封面控制棒发黑、内部没有充满冷却液后，沿原13卡只修正背景。OBJ缺少color属性但材质启用vertexColors；本批场景只对缺色几何补中性顶点色，正式钢灰棒体、铜带与燃料组件钢板细节恢复。冷却液读取正式compound_coolant_still第0帧，以alpha0.36填满三轴1.01..3.99的整个堆芯包络，仍能看清棒体和燃料蓝辉。125格布局、相机、灯光、外壳CT、Logo、名称和版式保持。

- [高清PNG 1920×1200](../../../../tools/art-assets/mcmod-cover-13/output/mcmod-cover-1920x1200.png)
- [预览PNG 960×600](../../../../tools/art-assets/mcmod-cover-13/output/mcmod-cover-960x600.png)
- [网站尺寸PNG 240×150](../../../../tools/art-assets/mcmod-cover-13/output/mcmod-cover-240x150.png)
- [标题路径版SVG](../../../../tools/art-assets/mcmod-cover-13/output/mcmod-cover.svg)与[文字可编辑SVG](../../../../tools/art-assets/mcmod-cover-13/output/mcmod-cover-editable.svg)
- [完整素材ZIP](../../../../tools/art-assets/mcmod-cover-13/mcmod-cover-13.zip)

负责人实际查看中性色诊断图、修后2048背景及960/原生240成品，并独立读回当前文件SHA。封面一次R1增量导出、23/23有限检查退出0；两SVG除内嵌背景外正文逐字一致，三尺寸反应堆视口之外和左侧品牌区均零像素变化。原同次[审查R1局部复核](reports/ART-MCMOD-COVER-13-REVIEW.md#r1-同次局部复核棒体颜色与满冷却液)无剩余必改项，用户外观反馈独立。背景72项只读来源与透明2048²来源一致。

| 当前交付 | SHA256 |
| --- | --- |
| 背景2048² | `3254b7728b5f3b085cca8600d3d1197d2567ba96a7db73a4a4005d7ed0436bb0` |
| 高清1920×1200 | `b4e576abc1beee65c99dfcc0e6578c9493c15f09bbe5622a2ca9ab990d0bec0c` |
| 预览960×600 | `d351a1435eed78fd7756533f4861fde067840222edafb1298c4e690e0e62e561` |
| 网站240×150 | `0f5bdd37600875d403f7f44109c987b66cb969838398586d1613938f0163337d` |
| ZIP（1,649,014字节） | `7b0967afce3cf79a835e9a8d94377b84b49a73d9483fb0ee66f5989f18acea62` |

原图、原ZIP与原证据完整保留history/pre-r1。本批临时渲染Tab9由负责人实际关闭；背景执行者核自有PID73564完整身份后停止，进程与4189监听均已消失，其他服务保持。只修宣传素材，不改游戏源码/贴图、主工具、第三方库、旧Logo、JAR、世界或Git；未上传MC百科。满液体为固定模型宣传姿态，SVG仍内嵌栅格背景，不表示真实服务器库存或游戏截图。详见两执行报告的R1节。

## 初版交付历史

以下为初版交付时点，SHA及68来源/18项检查只对应已归档原图。上方当前入口均已替换为R1，初版内部观察不能追认用户随后指出的两项问题通过。

2026-10-14。用户指定新厚重R2 Logo、中文“机械动力：核工业”、英文“Create: Nuclear Industry”，最小5×5构建反应堆为背景。当前成品按现有5×5×5最小结构、原模型/贴图和block-model-renderer 2.20.0实际渲染，左右分区、深钢灰底色、暗铜线及冷蓝点缀。

- [高清PNG 1920×1200](../../../../tools/art-assets/mcmod-cover-13/output/mcmod-cover-1920x1200.png)
- [预览PNG 960×600](../../../../tools/art-assets/mcmod-cover-13/output/mcmod-cover-960x600.png)
- [网站尺寸PNG 240×150](../../../../tools/art-assets/mcmod-cover-13/output/mcmod-cover-240x150.png)
- [标题路径版SVG](../../../../tools/art-assets/mcmod-cover-13/output/mcmod-cover.svg)与[文字可编辑SVG](../../../../tools/art-assets/mcmod-cover-13/output/mcmod-cover-editable.svg)
- [完整素材ZIP](../../../../tools/art-assets/mcmod-cover-13/mcmod-cover-13.zip)

8:5画布依据[MC百科官方封面尺寸说明](https://mcmod.readthedocs.io/zh/latest/mod/)。两个名称按中英各两行排版；负责人实际查看最终960与原生240，名称、Logo、完整五格反应堆轮廓、顶部接口与窗内蓝色燃料棒可辨。

背景为2048×2048透明模型渲染，布局125格覆盖三轴0..4：六燃料列顶部换料口、中央三驱动器、正面3×3连窗及合法侧面仪表/冷热口，68项只读来源身份绑定。只在本批新布局纠正旧网页样例的侧面换料口与顶盖，不改主工具或游戏。背景不是Minecraft截图，不表示真实热工工况。

成品SVG包含可编辑矢量Logo/版式及内嵌PNG背景，并非整张纯矢量。标题路径版不依赖字库；文字编辑版使用实际本机微软雅黑粗体，ZIP不分发系统字体。原R2 Logo逐字嵌套，不重绘其几何/配色。

封面一次正式出口与18/18原有限检查通过；背景布局/来源/透明边缘检查通过。负责人已读回原结果及主图/缩略图/ZIP实哈，本次[独立组合窄审](reports/ART-MCMOD-COVER-13-REVIEW.md)未发现必改项，未重复生产、设备测试或整支审查。来源和证据见[背景报告](reports/ART-MCMOD-COVER-13-BACKGROUND.md)、[封面报告](reports/ART-MCMOD-COVER-13-COVER.md)与本批build/reports/art/ART-MCMOD-COVER-13/。

主图SHA256 77a11300fae138b917be130d3675ecde141757784b0cbd1a9daa5020245d86bc；240图为4ebe5fa212ba9f4a10a815180e08850b9ad7654d2662a309ea923cb8874cdc14。ZIP1463744字节，SHA256 fb0336423ed09232328d91f778b5f990cbbd18289de7d9993aad347197a3f1f4，11条目与来源一致。背景服务已按自有进程身份停止，PNG/场景/证据保留；预览Tab所属浏览器不可用，未冒称执行关闭成功。

只交宣传素材，未上传MC百科、进行Git写入、修改旧Logo/游戏资源/源码/JAR或任何世界。内部核对不替代用户的封面外观反馈。
