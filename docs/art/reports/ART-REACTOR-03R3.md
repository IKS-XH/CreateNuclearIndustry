# ART-REACTOR-03R3实施报告

液体与共享滑块定向整改已完成；负责人实际查看三张离线图通过，最终组合23/23及增量JAR通过，现冻结交一次独立窄审。用户客户端视觉与鼠标门独立保持待验，未合main。

实际读取任务卡、AGENTS/美术入口、治理1.2/5.1/5.2、液体audit末尾whole-cell简化复核；应用minecraft-modding/testing/resource-pack、TDD与writing-good-tests。维持MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，不改依赖。执行者按生效卡及主PM03R3补充仅持有两液体类、共享Behaviour、两专属测试及本批报告/证据；无Git/主树/客户端/服务/存档/子Agent操作。

开工HEAD `af3c36b6e52f99a871bf3d8098869a3f5f7c52d8`。五允许源原字节保存baseline/；111保护路径与1056正式资源指纹见baseline.json。原R2候选保存baseline/previous-03R2-candidate.jar，SHA256 `20b7223f899f7b75b56cdfb7d8374c7c7b629b26384add13202939ebc92e97ba`。旧冻结、报告、证据与候选保持只读；baseline初次JSON的WindowsPath键异常原log保留，修为字符串键后baseline-confirmed exit0，不涉及生产或基线输入修改。

先只对descriptor.coolantSpace原合法空气格分层allocate，按原高度扩展同层Fuel.body显示包络，再统一faces撤内部隔面；燃料不进入容量或库存分母。满液三列三层30外露面，半液canonical底层满、中层.5、顶层干为22面；空库存/没有合法层撤液。缓存保存原合法集合、显示→采光不可变Cell映射与网格，无descriptor/Level/BE引用。燃料采光选同Y最近合法空气格，等距按X/Y/Z字典序；每次submit重新查询当前世界光，不缓存packed-light。

实际生产提交仅沿外法向内缩1/1024格，切向格缝/UV及外法线保持；侧面alpha .46..56高度渐变、水平.45，原RGBA材质/混色/流纹不改。不新增CSG，不修改任何SVG/PNG/OBJ或容量/协议；钢板/管按原不透明深度遮挡。

| 液体定向检查 | 结果 |
| --- | --- |
| 首次red | exit1；15项/3失败，其中缺面查询为NoSuchElement；原log/XML保留 |
| liquid-red-asserted | exit1，15s；15项/3个真实AssertionFailedError：燃料外包络缺失、半层并集数量不符、生产顶点alpha过低；无编译缺符号，生产未改 |
| liquid-green | exit0，17s；15/15 failure/error/skip0，未打jar；原杆体/数值专属断言与新增液体行为同类执行 |
| 生产输入导出 | exit0；旧R2 JAR子加载器与当前编译class真实mesh/submitFace，4组位置/UV/normal/RGBA/采光坐标；原mixInto phase2.25、热比0/.5/1 |
| preview | exit0；2页真实顶点/OBJ/UV满液与跨层半液透窗对照，自己实际打开，负责人实际复看当前参数通过 |

命令实际工作目录均为美术树，JAVA_HOME为C:/Program Files/Java/jdk-21；red/green仅`gradlew.bat test --tests '*ReactorAnimationVisualStateTest'`。原Materials/L2/协议/其它专属套件不重跑。编译既有EventBusSubscriber两条removal警告及Gradle弃用提示原样保留。JUnit自动logs保留，最终范围绑定时单列。

预览生产导出MeshPreviewDump.java在本批证据目录，以真实旧JAR隔离两个生产类型及嵌套类，父加载器共用Minecraft/descriptor/记录VertexConsumer；初次遗漏renderer合成嵌套类导致IllegalAccessError，原log保留，补同族隔离后导出exit0。这是证据夹具环境失败，不当行为red、不改生产。Java证据程序只读模型/帧、写本批JSON；没有启动游戏。预览复用旧03A纯OBJ解析/Raster定义，不运行其输出段；以生产实际alpha/epsilon、原NO_CULL/LEQUAL/COLOR_DEPTH_WRITE近似同光照，非游戏光照/遮挡通过结论。

- [真实red XML](../../../build/reports/art/ART-REACTOR-03R3/red-asserted/TEST-com.iksxh.create_nuclear_industry.structure.client.ReactorAnimationVisualStateTest.xml)、[green XML](../../../build/reports/art/ART-REACTOR-03R3/liquid-green/TEST-com.iksxh.create_nuclear_industry.structure.client.ReactorAnimationVisualStateTest.xml)
- [满液图](../../../build/reports/art/ART-REACTOR-03R3/coolant-full-comparison.png)、[半液图](../../../build/reports/art/ART-REACTOR-03R3/coolant-partial-comparison.png)
- [实际mesh/顶点输入](../../../build/reports/art/ART-REACTOR-03R3/production-mesh-vertices.json)、[来源](../../../build/reports/art/ART-REACTOR-03R3/preview-provenance.json)、[生产源/class/旧JAR绑定](../../../build/reports/art/ART-REACTOR-03R3/preview-production-binding.json)

液体上述方向通过后，负责人明确主PM共享补充实际到位，原执行者接同批共享段；最终结果如下，未重复旧材料/L2/协议测试或生成任何资源。


共享补充实际读取SHA256 `49bd5c2ba8ad181ae1d53241a885ed7efcd6021f284e7e2a99fa551fbef601be`；最终HEAD `a6792e6dc55aebc4b6c856ada591aaf8d7838847`为PM文档同步，功能前置不漂移，具体文档路径见verification.json。共享基线Behaviour SHA c6c1a349…add77f0、Test64b2ce84…93733b与开工备份一致。

生产CornerTransform仅把UP/DOWN/Y、SOUTH/NORTH/Z、EAST/WEST/X法向改至真实面1/0，并更新相关中文说明；原切向、scale.18/.40、构造null安全、fromSide缓存、native rotate/testHit逐字保持。共享类除六法向与说明外逐字相同，between/formatter/持久化/权威commit/response/回调/锁定未变。

| 共享/最终检查 | 实际结果 |
| --- | --- |
| slider-red | exit1，16s；两个最终绘制深度断言真实失败（DOWN先报告），日志/XML未覆盖 |
| slider-top-red | exit1，20s；UP优先的同两回归真实失败：旧框y=.9800562、文字y=.9828125，位于顶面1内 |
| slider-green | exit0，19s；8/8，failure/error/skip0；六面最终框四角及文字基线严格出面，真表面命中/边界/切面缓存保持 |
| slider-preview | exit0；实际24个FRAME顶点与6个TEXT基线来源，负责人实际看图通过 |
| final-test-jar | exit0，18s；VisualState15＋Slider8=23/23，failure/error/skip0；增量jar执行，其余生产/测试编译UP-TO-DATE |
| verification | exit0；1056assets、111保护、19来源安装/JAR及生产class绑定通过 |

最终命令在唯一美术树：`$env:JAVA_HOME='C:/Program Files/Java/jdk-21'; .\gradlew.bat test --tests '*ReactorAnimationVisualStateTest' --tests '*ControlRodSliderTransformTest' jar`。仅这一次最终组合，旧Materials/L2/协议/全量/GameTest均未跑。

最终绘制证据界限：实际完整TextValueBox构造在JUnit失败，NPE原文为`Cannot read field "level" because the return value of "net.minecraft.client.Minecraft.getInstance()" is null`，保存于red/green/final XML。测试复用原运行时PoseStackExtension夹具，调用真实生产transform与原生旋转，再执行锁定ValueBox.render的scale(-2.01,-2.01,2.01)/translate(-.5,-.5,-1/32)、原getFontScale以及TextValueBox的后置Pose指令。四框角为6PX可见区域5/16..11/16；文字适配后续z平移0，基线深度保持。不是完整原生Font/render测试；正常Font.DisplayMode.NORMAL与深度测试依据锁定字节码/源码，GPU/文字可读与鼠标操作仍待用户。

顶/底最终框、文字出面距离约.01130625/.0140625格，侧面约.025125/.03125；实际输出的浮点差异在1e-6内。真表面命中球半径顶底.09、侧面.20，仍拒杆体/固定箍角与旧中心，严格边界和UP→SIDE→DOWN→SIDE切换通过。slider-final-depth图基于实际XML，不重绘模型或调整液体参数。

最终范围与来源：五允许源码均变化；111保护为原R2的75保护＋41交付哈希去除本批五允许输入，集合见baseline.json/verification.json；包含原19SVG、33generated、其他动画类/Materials及测试、共享控制/BE/L2/CT和旧报告。1056正式assets与旧R2候选逐字一致。19安装来源中17为源文件字节复制、两mcmeta由原mapping.animation_metadata派生；安装字节均与JAR一致，metadata内容及mapping源SHA保持。验证脚本初次把mcmeta误当mapping整个文件的字节复制，AssertionError原log保存；仅修证据绑定，不改资源/生产或重构建。共享法向/注释之外逐字保持，三个生产源码各自所有main class逐字对应候选JAR，证据程序/测试夹具不进入JAR。已批准三图的生产源/class和输出绑定仍匹配最终生产文件。手写五源尾空格检查通过，logs/debug.log与latest.log仅JUnit自动改动，最终SHA单列保留，不清理。

本批唯一候选为`build/reports/art/ART-REACTOR-03R3/candidate/create_nuclear_industry-0.1.0-ART-REACTOR-03R3.jar`，2,477,690 bytes，SHA256 `3a52c6cb61ed422ce94faa92a959ca0d272d7710388f8de099d1d02ca3bcce04`。旧R2候选20b722…e97ba与原冻结a502efc1…96ef56完整保持，旧03/R1/R2证据未重写。

- [最终log](../../../build/reports/art/ART-REACTOR-03R3/final-test-jar.log)、[两最终XML](../../../build/reports/art/ART-REACTOR-03R3/final/)
- [顶面真实red](../../../build/reports/art/ART-REACTOR-03R3/slider-top-red/TEST-com.iksxh.create_nuclear_industry.control.ControlRodSliderTransformTest.xml)、[最终出面图](../../../build/reports/art/ART-REACTOR-03R3/slider-final-depth.png)、[该图实际来源](../../../build/reports/art/ART-REACTOR-03R3/slider-preview-provenance.json)
- [范围保护/制品结果](../../../build/reports/art/ART-REACTOR-03R3/verification.json)、[十九来源绑定](../../../build/reports/art/ART-REACTOR-03R3/source-installed-jar.json)、[生产class绑定](../../../build/reports/art/ART-REACTOR-03R3/source-class-jar.json)、[命令记录](../../../build/reports/art/ART-REACTOR-03R3/command-ledger.json)
- [冻结入口](../../../build/reports/art/ART-REACTOR-03R3/frozen-manifest.json)：五源/测试、唯一报告、候选与本批证据冻结；负责人任务卡/PLAN/活候选文档不纳入交付哈希，PM03R3补充和已用只读audit另列依赖。

执行者停止写入/构建，交负责人一次组合窄审及PM来源登记；用户重启美术树观察透窗满/部分液位、冷热色、成型后顶面文字和实际调节。03R3/03R1液体与02R1外观等人工门独立保持，未宣称客户端通过。
