# ART-REACTOR-03R2实施报告

本批方形控制棒与避让滑块已组合交付、冻结待独立窄审；最终专属JUnit 6/6，failure/error/skip均0，增量JAR成功。负责人实际复看三张模型图与控件布局页通过；用户客户端文字清晰度、手感和实际调节仍待观察，03R1冷却液及02R1外观视觉门独立保持，未合main。

实际读取任务卡、共享COORDINATION、AGENTS、美术入口、治理1.2/5.1/5.2及必要原报告；应用minecraft-modding/testing/resource-pack和实施/排错/验证技能。保持MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。执行者无Git写、子Agent、主树/客户端/服务/存档操作，不改构建依赖；原动画18项及L2/协议证据复用。

开工HEAD `d36e79134b822a8eb8c5e058b241674e7050ad94`；共享授权文档同步及最终HEAD `6631353d4a5cfc4d635572ae37e63f3d4de2ae33`。原38模型写集字节/mtime与共享Java原字节分别保存baseline/、baseline.json、shared-baseline.json；新测试原不存在。旧03R1候选保存为baseline/previous-03R1-candidate.jar，SHA256 `ec9901a46d95707efdb2c4bc2b63f52fa76a3223f6e115950062eba2f8a44ed1`（2,474,232 bytes）。原03R1冻结清单SHA `4ee58009f19310693e6909208b7ffa8347470b07c2959db6fa0d2c905d18ef7a`保持；所有旧报告/证据/候选未重写。

控制shaft方柱X/Z0.2..0.8，边长0.6格（9.6单位）、单位Y0..1；fixed head方箍X/Z0.175..0.825，边长0.65格（10.4单位）、Y厚3/16。四側与两端闭合，轴线仍(.5,.5)，沿用原control SVG钢灰纵槽/箍纹和黄铜UV。完整actualDepth、shaft仅Y行程缩放、固定head及原采光不变。rig/generate/README三源更新，33生成候选仅两控制OBJ改变；其他31字节和mtime均保持。两正式OBJ经负责人看图后逐字节安装。

共享类仅新增四个几何import、构造器选择CornerTransform与同文件嵌套几何。UP/DOWN中心(.10,.96875/.03125,.10)、scale.18；SOUTH/NORTH/EAST/WEST位置按卡表、scale.40。构造direction==null安全，fromSide先super再同步缓存scale；沿用原六面rotate/testHit，不改百分数、范围、网络、提交、持久化或锁定。最终源SHA `c6c1a349ef6e78c67fd55f71ab8fb60e0bd2d8e7cd1b68101fa7f86caadd77f0`，原非几何代码逐字保持。

| 保存检查 | 实际结果 |
| --- | --- |
| baseline / shared-baseline | exit0；38原路径、共享源原字节与测试不存在基线 |
| generate / model-check / preview / install-model | exit0；仅2OBJ改变，真实顶点截面/闭合/外法线/绕序/UV通过；负责人复看后安装 |
| 首次专属test+jar | exit1，6项中3个Pose显示组ClassCastException；3个命中组通过。JUnit未应用PoseStackExtension客户端mixin，非行为red |
| 编译期夹具尝试 | exit1；Flywheel扩展仅runtime可见、7个符号编译失败，非行为red；未改依赖 |
| runtime ASM夹具首次 | exit1，6项/3失败；反射包内FieldWriter访问异常，非行为red |
| final-success | exit0，6/6，15s；仅经公共Visitor声明调用反射，JAR UP-TO-DATE |
| final-confirmed | exit0，6/6，14s；移除测试固定证据目录写入后窄复测，JAR UP-TO-DATE |
| verify-final | exit0；两模型source-generated-installed-JAR与共享3个class-JAR一致，75保护保持 |

原始失败log/exit/XML/当时测试源保存在first-run/，首次成功存prior-success/，均未覆盖。最终命令为`$env:JAVA_HOME='C:/Program Files/Java/jdk-21'; .\gradlew.bat test --tests '*ControlRodSliderTransformTest' jar`，实际从美术树运行，未跑旧18项、GameTest或全量。首次任务已生成生产JAR，此后仅改测试夹具，因此两次成功构建JAR均UP-TO-DATE。最终XML对应移除IO后的最终测试源。

夹具只在测试运行时用ASM补原生PoseStackExtension挂点：构造锁定Flywheel PoseTransformStack(this)并返回该字段；实际Create.rotate、MC PoseStack和原testHit继续执行，无自写旋转/命中、无mock行为。native-pose-fixture.class及javap文本保留首次成功时的字节码证据；最终测试仅内存defineClass，不写固定路径、不依赖证据父目录。测试覆盖六面实际Pose位置/朝向、真实面内外、严格球边界、旧中心和杆/端箍角拒绝、UP→SIDE→DOWN→SIDE缓存刷新与初始UP。

模型三联为保存旧/新OBJ及UV同尺度投影，不重生成旧证据。控件布局页从最终XML LAYOUT/HIT实际输出生成：原生6PX宽框×2.01×实际Pose尺度、真实块面球形点击截面与实际命中点，展示顶面/南侧及0/50/100%杆足印。侧面控件Z=.96875在方杆Z≤.8前方；图内足印重叠不表示共面。全部为离线示意，不代替客户端文字/遮挡/操作验收。

保护集合为模型baseline的62路径（原03R1允许集合扣除本批38、19SVG、palette/mapping/shared exporter、CT三源及L2十二源）加共享baseline的13控制/BE路径，共75唯一项。所有19SVG、PNG、其他17正式资源、动画六类/旧两测试、L2/CT及控制非几何路径未变；JAR相对旧03R1共1056 assets仅两OBJ变化，1054保持。40精确源码/资源路径中9变化、31保持，另唯一报告；手写路径尾空格检查通过。logs/debug.log、logs/latest.log为JUnit自动更新，单列最终SHA，保留不清理。无额外探针或旧矩阵重跑。

本批候选：`build/reports/art/ART-REACTOR-03R2/candidate/create_nuclear_industry-0.1.0-ART-REACTOR-03R2.jar`，2,475,501 bytes，SHA256 `20b7223f899f7b75b56cdfb7d8374c7c7b629b26384add13202939ebc92e97ba`。

- [最终日志](../../../build/reports/art/ART-REACTOR-03R2/final-confirmed.log)、[最终XML](../../../build/reports/art/ART-REACTOR-03R2/final/TEST-com.iksxh.create_nuclear_industry.control.ControlRodSliderTransformTest.xml)
- [来源/保护结果](../../../build/reports/art/ART-REACTOR-03R2/verification.json)、[两模型映射](../../../build/reports/art/ART-REACTOR-03R2/source-generated-installed-jar.json)、[共享源/class映射](../../../build/reports/art/ART-REACTOR-03R2/source-class-jar.json)
- [布局页](../../../build/reports/art/ART-REACTOR-03R2/slider-layout.png)、[实际来源](../../../build/reports/art/ART-REACTOR-03R2/slider-layout-provenance.json)
- [冻结入口](../../../build/reports/art/ART-REACTOR-03R2/frozen-manifest.json)。所有路径完整SHA以清单为准；根负责一次组合窄审和用户视觉交接。
