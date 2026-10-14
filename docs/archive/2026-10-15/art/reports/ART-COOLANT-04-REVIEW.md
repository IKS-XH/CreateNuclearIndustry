# ART-COOLANT-04 独立规格与质量窄审

2026-10-10，唯一工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`。本次只读实现、原始证据与图像；未运行producer/check/生成/测试/构建，未启动游戏、改存档、改实现或执行Git写操作，仅写本报告。

## 结论

规格符合：通过。内部资源质量：通过。Critical/Important/Minor必改项：无。

游戏视觉门仍待用户；未合main，不追认03R3、02R1等独立人工门通过。

## 合同与实际审查

实际读取最新AGENTS、治理1.2/5.1、美术入口、04任务卡、实施报告及完整PROBE报告。复用已实际读取的以下技能，按本批应用：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`核实际ModFluids加载路径与客户端边界；同目录`minecraft-testing/SKILL.md`审查窄检查/负例原始退出和日志，复用同一候选证据；`minecraft-resource-pack/SKILL.md`审查原生atlas帧规格、mcmeta、alpha与UV语义。锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，无版本变化。

完整审读独立producer、mapping和README。四个sprite仅从16份冻结原创整数rect SVG读取；来源路径和SHA锁定，名称/16或32规格/block与fluid八路径白名单明确。所有输入先读取校验、四PNG/四mcmeta编码完成后才创建输出；没有安装入口、不调用旧pipeline。check只读实际生成物/安装物；非显然算法和说明为中文。mapping和实际ModFluids仍引用`block/*coolant_{still,flow}`，fluid别名内容与元数据同批一致。

still为16×128（八个16×16帧）；flow为32×256（八个32×32帧），每帧四象限精确重复对应16像素图案。PNG RGBA/alpha255、帧0..7、2tick、interpolate=true及2px位移含7→0由原positive/installed-check日志核实。实际读取代表SVG和元数据，检查算法比较完整RGBA，不以缩放图片或源码字符串代替来源像素验证。

PROBE及锁定LiquidBlockRenderer原始片段表明静止顶面使用still全幅，流动顶面以flow(.5,.5)旋转采样，侧面quarter随液高裁切；不能描述为所有面都取左上quarter。2×2平铺保留对应像素密度，无拉伸重画。Create流体面另有UV算法；普通atlas固定循环，不执行内部每堆真实库存冷热混合、转换量调速或Java顶点透明度。

## 证据绑定与看图

证据根：`build/reports/art/ART-COOLANT-04/`。实际读checks.json/log、installed-check.log、jar.log及jar-exit.json：正向和安装检查exit0；缺mapping、flow规格错、越权目标三项exit1，错误在写前产生，生成物前后不变证据保留。唯一增量jar为4秒exit0，compileJava UP-TO-DATE，执行processResources/jar，无test/GameTest。

一次只读冻结绑定核对：29交付、22证据、24依赖共75项SHA全部匹配；manifest SHA256为`b48d3bf8968b0f92fe06ba6ed5b90d1df59dc834ce557b10482045db7dfa0580`。16资源映射的generated→installed→build/resources→候选JAR逐项匹配，无漂移。候选2,468,602字节，SHA256为`7f9f2da39128203700edb677ca525bf11f1d50a0811194ef24f1d3330d983b68`。

复用verification.json与基线原始证据：八PNG替换、八mcmeta新增，其余1048assets、371源码/测试、1089相关旧源/工具及旧R3冻结/JAR保持；不重新扫描旧矩阵。没有Java、注册、库存、材质消费者或原SVG变更。

实际打开same-source-comparison.png：新still冷蓝/热橙与内部原帧视觉一致，flow为完整四象限重复，图案清晰且保持原内部低对比风格。实际只读解码coolant-loop.gif：八个不同关键帧、每帧100ms、loop0；并审读预览脚本，来源为实际PNG及冻结内部sheet。GIF仅示意八关键帧，不展示原生RGB中间插值，也不是游戏效果证明。

用户仍需观察储罐、透明管道、动态桶、冷液世界面的颜色、流纹、循环与资源重载。普通世界既有solid策略保持，不能承诺透明效果；热液目前无可放置世界方块，不报告热液世界显示通过。内部库存混色/调速与浸泡效果属于原消费者，未由本批普通贴图替换重新验收。
