# ART-COOLANT-04：普通冷却剂美术候选来源登记

2026-10-10。美术负责人在用户既有专项权限内，按用户“把冷热冷却剂流体的贴图和动画做成跟反应堆内部动画一样的”要求交付资源候选。**自动资源检查、唯一增量打包和独立窄审通过；游戏视觉待用户，资源尚未合main。** 本登记只写PM文档，不修改美术实现、素材、负责人任务卡或用户存档。

## 来源与制品

- 工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`；冻结HEAD为`aab06d969a56904dc341f691627c6b7ddbea49f6`。与开工基线`ca7c20d`的提交差异仅装配台教学验收AGENTS文档，不改变本批资源身份。
- [当前候选与复看说明](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/ART-COOLANT-04-CANDIDATE.md)、[任务](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/tasks/ART-COOLANT-04.md)、[实施报告](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/reports/ART-COOLANT-04.md)、[锁定渲染管线PROBE](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/reports/ART-COOLANT-04-PROBE.md)、[专用工具与来源](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/tools/art-assets/coolant-fluid-04/README.md)。
- [唯一独立规格/质量窄审](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/docs/art/reports/ART-COOLANT-04-REVIEW.md)：通过、无必改项，SHA256 `61220e612b26fb83e6e65ef7e27ecbd95c5ddb5b6c3edad11284193ad870088e`。
- [冻结清单](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-COOLANT-04/frozen-manifest.json)：29交付、22证据、24只读依赖，共75项，SHA256 `b48d3bf8968b0f92fe06ba6ed5b90d1df59dc834ce557b10482045db7dfa0580`。
- [唯一冻结JAR](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-COOLANT-04/candidate/create_nuclear_industry-0.1.0-ART-COOLANT-04.jar)：2,468,602字节，SHA256 `7f9f2da39128203700edb677ca525bf11f1d50a0811194ef24f1d3330d983b68`。登记时与美术树build/libs一致；它含既有美术候选，不是main发布包。

## 显示边界

只读复用内部冷蓝/热橙各八份SVG；still每帧16×16、条带16×128，flow每帧32×32、条带32×256，逐帧为对应16像素纹样的精确2×2平铺。四种sprite同步block/fluid两组路径，共替换8PNG、新增8mcmeta；实际ModFluids仍消费block路径。八帧、每帧2tick、interpolate=true，固定循环0.8秒，PNG alpha255。

普通atlas按全局时间循环，分别显示冷热端点；不执行内部每堆库存连续混色、转换量调速或顶点透明度。世界已有solid策略、注册、Java消费者、真实库存、温度和内部浸泡包络保持。热液目前无独立桶或可放置世界方块，不新增注册对象或宣称这些画面通过。flow平铺适配原生流动顶面旋转与侧面随液高裁切的UV；不把所有面都描述为固定左上quarter。

历史pipeline的RETAINED_ORIGINALS锁未改，本批八目标以专用producer为来源，避免旧全量export/install覆盖候选；不宣称旧工具全量check通过。

## 实际核对与复用

PM实际读取任务、报告、候选、PROBE、工具说明、完整独立审查，以及原checks/installed-check与jar退出记录。原正向/安装检查exit0，三项负例exit1且生成物保持；唯一`jar --console=plain`为4秒exit0，compileJava UP-TO-DATE，没有JUnit/GameTest。本次未重新运行producer、生成、测试或构建。

实时核75项冻结SHA与清单/审查/JAR身份，16资源生成→安装→build/resources→JAR逐字一致；24依赖包括原SVG、内部sheet、ModFluids、旧工具和原03R3JAR/冻结，均保持。其余1048正式资源、371源码/测试和1089旧素材/工具保护结论，以及对照图/GIF的独立观察，复用原verification与唯一审查，不重新扫描保护矩阵或作第二次视觉审查。PM实核留在主树`.superpowers/sdd/2026-10-10-art-coolant-04/pm-binding-check.json`。

本轮沿用已实际读取的minecraft-modding、minecraft-testing、minecraft-resource-pack及完成前验证技能，核锁定版本和证据边界；MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6未变。仅PM文档与登记Git提交，未暂存美术源码/资源/工具/负责人报告。

## 人工门

按当前美术候选说明观察冷热储罐、透明直管、冷液动态桶、冷液世界静止/流动面及F3+T重载。离线图和资源打包不代替游戏的颜色、光照、采样和循环观察。03R3浸泡/顶面操作与02R1材质连窗门继续独立；旧03R3制品保持，其历史build/libs一致记录不代表当前build/libs仍等于旧JAR。

装配台07-R1单页用法教学已按用户手测独立验收并净合main。新资源候选不重开已关闭设备功能/教学门，不自动推进其他工程主线。
