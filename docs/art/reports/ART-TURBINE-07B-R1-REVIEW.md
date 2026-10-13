# ART-TURBINE-07B-R1 独立差异窄审

结论：未发现本轮生产候选必改项，可交负责人安排原存档游戏复看；用户游戏视觉门保持待验。审查以HEAD6042fd2及保留的07B generated OBJ/PNG为差异基线，读取R1卡、作者报告、AGENTS、美术入口、治理1.2/5.1与Minecraft modding/testing/resource-pack技能；技术栈保持MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。没有生成、安装、构建、测试、游戏或Git写操作。

只读实际比较确认164个endcap消费者OBJ改变；所有非vt记录及非endcap使用的vt逐字保持，48个middle没有变化。专用MTL和侧壳panel与07B保留副本字节相同。未发现本批扩大到侧壳、其他部件、模型JSON或Java的接入需求，几何和原包装保持。

全部实际endcap vt与区域换算吻合：d7原点(0,0)、112×112；d5(0,112)、80×80；d3(80,112)、48×48；未成型(112,0)、16×3。成型由完整截面原UV乘d×16，邻格延续同坐标；未成型从原up截面V=.8125..1映射3像素厚边，原六向预旋转UV关系保持。当前包装flip_v=false；锁定neoforge-21.1.219-sources.jar中ObjLoader.java:51读取该值，ObjModel.java:375–376执行getU(u)、getV(flipV ? 1-v : v)，因此此处V不需额外翻转，atlas区与实际消费相符。

复用candidate-check.log：整数SVG/PNG同源、atlas128×256、48铜色像素、两处短筋、164 OBJ/1744 endcap vt及其余记录保持，exit0；install-byte-check.log记录165项一致；assemble.log记录唯一增量assemble成功5秒、exit0；jar-check.log记录165条目一致、exit0。本轮只读核当前JAR为2603740字节，SHA256=4724e54c45e18b7c1917e3272fe3cf2b8e217b89f6cfc7fc0466f957955810b9，与作者绑定一致。以上均位于build/reports/art/ART-TURBINE-07B-R1/，未重跑原检查。

已独立查看负责人保存的final-model-preview.jpg，包含d3/d5/d7和未成型最终旧新对照。大铜环已移除，钢板拼缝、窄法兰和轴座小铜片可辨，侧壳B束带保持；不同尺寸细节没有再次整面放大。build_candidate.py:301–316及375–381明确旧预览取07B副本OBJ/PNG，新预览取R1候选，middle复用基线，未将新UV配旧PNG。CUA直接打开失败后使用该现存截图查看，未另建预览流水线。

这只是静态OBJ材质与资源差异审查；真实照明、实际轴件遮盖、两端观看距离和整体审美仍须用户客户端确认。只新增本报告，之后停止写入。
