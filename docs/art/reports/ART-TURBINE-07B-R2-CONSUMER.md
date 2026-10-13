# ART-TURBINE-07B-R2 消费实施报告

执行者仅在美术树实施三个新客户端类与两份专属测试；HEAD起点6042fd2598865d42de88f95c2ba8110c3b7e1196。版本保持MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。实际应用minecraft-modding/testing/resource-pack及TDD，按治理1.2/5.1执行精确写集与定向验证。

机壳包装仅将既有07panel sprite替换为新64×32 atlas，d5上行周期2、d7下行周期3，世界坐标向machine_facing反向投影决定装饰相位。d3、端盖、其他sprite和无数据均保留。Catnip entry在每次使用时取当前target；独立客户端同步MOD事件幂等登记两block包装。

窗口只读轴向两邻格的已有LOCATED/PIECE/MACHINE_FACING/FORMED显示状态；合法MIDDLE四轴心窗口才连接，不寻找运行owner。整端九quad撤下，玻璃扩至共享边；一个窗30quad，三连窗21/12/21quad。getQuads只消费不可变ModelData，复制共享顶点数组后改动。实体框宽0.14、厚3/16和玻璃内缩0.04/0.08保持。连接玻璃两大面按实际四顶点恒定法向深度识别，UV裁至原PNG有色texel中心U4.5/16..11.5/16、V3.5/16..11.5/16；边面、单窗、物品/Ponder/未知上下文保持原图原quad。PNG/MTL、色alpha、光照不变。

## 真实验证记录

- 初始行为red：8项、5个AssertionFailedError、exit1、24秒；[日志](../../../build/reports/art/ART-TURBINE-07B-R2/consumer/red/gradle.log)及两XML保留。
- 首次获准定向green：8/8、exit0、21秒；[日志](../../../build/reports/art/ART-TURBINE-07B-R2/consumer/final-test/gradle.log)。在玻璃UV暂停消息到达前已完成，原UV mesh与源副本另存before-glass-uv，没有覆盖该记录。
- 玻璃UV真实red：仅窗口4项，2个AssertionFailedError、exit1、19秒；[日志](../../../build/reports/art/ART-TURBINE-07B-R2/consumer/glass-uv-red/gradle.log)。预期最小U4.5/16，旧实现仍为0。
- 玻璃UV窄green：窗口4/4、failure/error/skip0、exit0、20秒；[日志](../../../build/reports/art/ART-TURBINE-07B-R2/consumer/glass-uv-green/gradle.log)。实际大面两张、裁采样范围与边面原UV均断言；壳面未重复测试。

实际原OBJ解码、原生BlockModelRotation/BakedQuad进入生产getModelData/getQuads，最终[窗口mesh](../../../build/reports/art/ART-TURBINE-07B-R2/consumer/window-mesh-glass-uv.json)携带实际顶点/UV/色/光照/法向。预览使用该mesh及原PNG/MTL；这属于离线模型输入证据，不等同完整NeoForge OBJ bake、GPU/客户端视觉验收。

## 最终制品与保护

负责人已实际复看三档8格机壳、1/3连窗实际mesh预览并准许唯一增量assemble。`assemble --console=plain` exit0、1秒，compileJava/processResources均UP-TO-DATE；[原日志](../../../build/reports/art/ART-TURBINE-07B-R2/consumer/final-assemble/gradle.log)与exit.json保留。未重复壳面/窗口或旧套件。

唯一候选[ART-TURBINE-07B-R2.jar](../../../build/reports/art/ART-TURBINE-07B-R2/consumer/ART-TURBINE-07B-R2.jar)：2616697 bytes，SHA256 `b1c3c69c29dce947b99ccd0f0a187e9689958217a2f3727c80487d9033cd8d88`。最终HEAD仍6042fd2598865d42de88f95c2ba8110c3b7e1196。

新PNG源工具generated→安装→build/resources→JAR逐字节一致，219 bytes、SHA256 `aff06b77869abb21ba6939a2300d9ba07c7c06c7684866ddcaf5e3459be4c741`。三生产类及Appearance/Connection两个record共5新class与build/classes逐字一致；原该包3个class字节保留。精确SHA和路径见[verification.json](../../../build/reports/art/ART-TURBINE-07B-R2/consumer/verification.json)。证据脚本首次错误地将整个既有包期望为5个新class，实际遇到原TurbineClientEvents/TurbineRotorRenderer/其匿名类；纠正为与原JAR比较新增集合及旧字节后检查exit0，未改生产或重构建。

原395 Java/tests、1112正式assets、403旧07B/R1工具输入与开工清单SHA保持；原1112assets在新JAR也一致。集合来自开工时现存src/main/java及src/test/java的全部Java、src/main/resources/assets的全部文件、既有07B/R1工具目录递归文件，清单保留在baseline/baseline.json。新增源/测试严格5路径，新正式资源仅1PNG（由素材执行者安装），共享逻辑/原OBJ/MTL/JSON不写。

原R1候选备份2603740 bytes、SHA256 `4724e54c45e18b7c1917e3272fe3cf2b8e217b89f6cfc7fc0466f957955810b9`保持。当前git-status包含既有未提交候选及JUnit自动写入logs/debug.log、logs/latest.log，未清理/回退/手修日志；5手写源码定向无尾空格。冻结入口为consumer/freeze.json与freeze.sha256，含本执行者源/测试/报告/证据/候选和只读显示依赖，不纳负责人活动卡/PLAN。

交付停写，等待一次独立窄审与用户原存档中/大型连窗、疏线、日夜光照及资源重载视觉复看。内部离线预览通过和定向自动检查不替代游戏视觉门；未启动游戏/服务，未写Git、主树、存档、共享逻辑或旧资源。
