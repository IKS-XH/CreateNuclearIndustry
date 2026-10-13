# ART-MCMOD-COVER-13 一次组合规格与质量审查

2026-10-14。独立审查者按13卡及治理1.2/5.1，只审本批宣传封面、背景装配与交付身份；结论：**未发现本批必改项，可以交用户进行外观选择。用户视觉门尚未判定通过。**

## 审查依据与边界

已读当前AGENTS、13任务卡、美术README、背景/封面执行报告；只读检查本批scene.mjs、export-cover.cjs、verify-cover.cjs、background/verify.py、布局与来源记录、两份既有verification.json及实际成品。参考minecraft-resource-pack中模型/资源引用边界，不扩展为资源包或游戏加载审查。

复用背景执行者有限检查退出0及封面执行者18/18退出0的原证据，不重新运行生产器、验证器、浏览器、Minecraft或Gradle。另做只读SHA256、SVG XML元数据与原Logo嵌套核对、ZIP条目读取和960×600/原生240×150实际观察。唯一写入为本报告；没有资产、主目录、字体、第三方库、世界、配置、JAR或Git写操作，没有子委派。

## 规格与质量结果

- 三PNG为1920×1200、960×600、240×150，均8:5。实际文件SHA256与output/manifest.json及原验证记录一致；源SVG画布为0 0 1920 1200。
- 中文“机械动力：核工业”、英文“Create: Nuclear Industry”均准确，编辑SVG分为四行“机械动力：”“核工业”“Create:”“Nuclear Industry”。成品SVG无text节点，标题已转轮廓；实际960图与原生240图没有缺字或标题裁切，Logo、标题与右侧完整反应堆轮廓分离。240图内蓝色棒体细节减弱，但窗口和蓝色线可辨；这属于内部观察，不替用户判断外观偏好。
- 原R2 Logo当前SHA256为cd763c08bf7f29ce21aef613230fd9dc37c06b826a5dbd2f7ec776e862b16c2c。实际成品SVG包含逐字原稿，仅外层视口改为x=123、y=14、586×586，未改原节点、配色与几何。包内另留原SVG身份，旧Logo检查与验收不重开。
- 字体来自本机微软雅黑粗体msyhbd.ttc，manifest明确redistribution=false；ZIP中没有ttf/ttc/otf。成品路径SVG/PNG显示不依赖字库，文字编辑源的字库依赖在README明确说明。SVG为矢量排版和原Logo加内嵌栅格背景，报告与README没有将整图称为纯矢量。
- 背景scene实际调用主目录既有model-loader、pack-source、connected-textures接口和createConnectedTransform，装配正式模型与Partial OBJ，而非手绘替代箱体。sources.json登记68条真实来源，含模型、贴图、CT、OBJ/MTL与已有浏览器库/WASM链路；render-evidence记录block-model-renderer 2.20.0、Minecraft资源版本1.21.1、936 draw calls/9480 triangles。此处保留实际渲染证据，不将其提升为Minecraft客户端加载证明。
- 场景与layout的三轴范围0..4、125格一致。六燃料列为x=1/3与z=1..3，身体y=1..3及顶部y=4换料口；中间x=2三控制列顶部驱动，内部空气与控制棒Partial分开。正面z=0的3×3九连窗、西侧仪表及后侧冷热口均在合法侧槽，边角/底面保持壳体；与只读ReactorStructureDefinition的燃料体、顶盖和端口边界相符。原验证计数77壳体、9窗、18燃料格、9空气格、6换料口、3驱动及仪表/冷热口各1，与代码一致。
- 透明背景2048²的实际SHA256及成品SVG内嵌PNG解码SHA256均为c4493e3b6953478e0361aa3afb39c92a1be9ff8da598778eff8d9c39bebca249。原RGBA/alpha与轮廓留白证据绑定同一文件，构图使用preserveAspectRatio保持完整轮廓。固定蓝辉、无冷却液与gameScreenshot=false边界明确，不声称服务端工况或真实热工。
- ZIP为1,463,744字节，实际SHA256为fb0336423ed09232328d91f778b5f990cbbd18289de7d9993aad347197a3f1f4。只读条目核对为11个文件及两个空目录条目；六output文件、两个sources文件、导出器、验证器与README齐备，无草案或字体文件。逐字output与Logo/背景身份复用原18项结果；包内工具和说明对外部运行时、字体及另留背景场景的依赖有明确说明，没有冒称资源全集。

## 原证据与交回

背景原证据：[BACKGROUND](ART-MCMOD-COVER-13-BACKGROUND.md)、[verification.json](../../../build/reports/art/ART-MCMOD-COVER-13/background/verification.json)、[render-evidence.json](../../../tools/art-assets/mcmod-cover-13/background/render-evidence.json)。封面原证据：[COVER](ART-MCMOD-COVER-13-COVER.md)、[verification.json](../../../build/reports/art/ART-MCMOD-COVER-13/cover/verification.json)、[manifest.json](../../../tools/art-assets/mcmod-cover-13/output/manifest.json)。

未发现需整改的规格、来源身份、引用或交付质量问题。本次一次组合窄审完成，用户视觉反馈、上传网站及任何主线集成均未由本报告登记完成；其他项目人工门与治理状态保持。

## R1 同次局部复核：棒体颜色与满冷却液

2026-10-14。用户指出初版棒体发黑、漏绘冷却液后，沿同次审查只复核这两项实际变更与封面绑定；上述初版结论及SHA保留为历史，不能追认这两项视觉通过。**R1未发现剩余必改项，可交用户局部复看；用户视觉门仍未判定通过。**

已读R1 scene、diagnostic-neutral-color.json、render-evidence.json、背景verification.json、两执行报告R1节、当前manifest和cover/r1/verification.json，以及有限验证器新增的SVG/像素比较代码；实际查看中性色诊断图与R1的960/原生240成品。只读核当前背景、诊断PNG、三PNG及ZIP的SHA，未运行生成器、验证器、浏览器、Minecraft或Gradle，未重开初版全审。唯一写入为本节。

- 根因与窄修相符：只读obj-loader实际创建position/uv/normal而未写color；372个真实OBJ诊断mesh记录缺color、MeshStandardMaterial.vertexColors=true、原map存在、material color为[1,1,1]。scene仅对缺color的几何补全中性[1,1,1]，已有color直接返回，未改原纹理、几何位置或灯光。诊断PNG实看恢复钢灰棒体、铜带和燃料组件钢板，SHA为03d85389a472fcefd21724ec160e7712df6296489b6ad2355c6ced7e3250f808；背景原验证将变化限定在窗内[1030,875,1510,1478]。
- 补液直接读正式compound_coolant_still原生第0tick、16²帧，盒体2.98³居中2.5，三轴包络[1.01,1.01,1.01]至[3.99,3.99,3.99]，alpha0.36、FrontSide、depthTest=true/depthWrite=false、renderOrder=2；蓝辉order=3。实际scene与render-evidence/verification一致，完整内部静态满液姿态保留棒体、铜带与燃料蓝辉可见。R1正式图窗内青蓝液体已可见，240版仍保留窗口色块和内部线条；不宣称游戏库存、流量或热工，也不代替用户颜色与透明度选择。
- 背景有限证据保持125格、原相机/灯光、RGBA2048²、透明轮廓边界[214,211,1834,1837]，当前937 draw calls/9492 triangles及72条来源登记。背景当前实际SHA为3254b7728b5f3b085cca8600d3d1197d2567ba96a7db73a4a4005d7ed0436bb0；manifest、导出绑定和实际SVG内嵌背景解码身份一致。
- 当前三PNG实际SHA依次为：1920版b4e576abc1beee65c99dfcc0e6578c9493c15f09bbe5622a2ca9ab990d0bec0c；960版d351a1435eed78fd7756533f4861fde067840222edafb1298c4e690e0e62e561；240版0f5bdd37600875d403f7f44109c987b66cb969838398586d1613938f0163337d。与R1 manifest及报告一致。
- 复用现有R1 23/23零失败退出0证据。新增比较代码逐RGBA像素比较固定视口外与左侧x<755区域，三尺寸变化总数62162/15669/1049，视口外与品牌区变化均0；这确实约束品牌与视口之外保持，并非仅比较整图SHA。两SVG去掉内嵌PNG后正文逐字保持，路径版另外独立只读核得一致，Logo、名称、字体轮廓与版式未重画。
- 当前ZIP实际1,649,014字节、SHA为7b0967afce3cf79a835e9a8d94377b84b49a73d9483fb0ee66f5989f18acea62，与R1原证据一致；ZIP逐字输出/原Logo/背景身份复用本次23项结果。原封面及背景输入/证据已留history/pre-r1，原报告未被本节覆写。

R1证据入口：[背景报告R1](ART-MCMOD-COVER-13-BACKGROUND.md)、[封面报告R1](ART-MCMOD-COVER-13-COVER.md)、[背景verification.json](../../../build/reports/art/ART-MCMOD-COVER-13/background/verification.json)、[封面R1 verification.json](../../../build/reports/art/ART-MCMOD-COVER-13/cover/r1/verification.json)。局部复核到此完成，停写等待用户反馈，不追加全仓审查或登记网站上传/主线集成。
