# ART-CENTRIFUGE-14B 客户端与正式PNG预览

2026-10-14。原网页执行者接续已选A正式实施，工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，基线88b8fd289d49df90d7e3c655b6e81381ad01130f。本批唯一Gradle/最终assemble持有人；目前已交定向GREEN与网页，**未assemble**，等待负责人实际GPU与同次组合窄审。

## 实现与权限

只改 `production/CentrifugeRenderer.java`：新增SHAFT PartialModel，新增一次与原ROTOR完全相同angle/Direction.UP/light/cutoutMipped调用。原有效lower owner早退、实际signed getSpeed、非零/非超载/非BEARING_WORN判断及时间公式保持，不super重复绘制通用轴。中文职责说明明确物理客户端、有符号RPM、两部件同相位与静态轴座不转。现 `CentrifugeClientEvents` 读取ROTOR会初始化整个Renderer类，因此SHAFT同样初始化；没有扩大该注册文件写集。

新增唯一 `CentrifugeArt14BModelTest.java`，三个有意义的正式A合同：实际Minecraft/NeoForge BlockModel反序列化后比较四运行模型的几何/旋转/各面UV和六正式材质绑定；实际346元素物品组合校验upper+16及解析origin+1格；六份实际PNG通过ImageIO解码16²。期望来自已选14A A源及独立长轴4×4×8值，不复制角度公式、不搜索源码字符串代替运行验证。

新正式预览仅本工具 `index.html`、`preview/`、`server.mjs`、`README.md`，直接读本美术树已安装五JSON/六PNG。每个实际载荷对照资产契约SHA，保留正式纹理ID/UV，完全不读SVG或使用色块。运行机含四真实模型、输入轴和转子同枢轴；完整物品为实际346元素组合JSON的静态几何近看。两视图同相机/照明/尺寸，原正背/侧/俯底/剖视/停止低速/重置及拖动/滚轮保持，剖视只裁运行机静壳，物品完整保留。六正式PNG可像素近看。24°/秒明确是网页姿态，不冒充实际Minecraft RPM或库存显示。

没改服务端、Java注册身份/状态/配方、世界、配置、主目录、Git或14A源稿/4191服务。正式11资源由原资产作者按独立写集安装。现旧d12cca…JAR未覆盖，最终唯一增量assemble需先精确备份旧包。

## 技能与实际版本

实际读 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、minecraft-testing/SKILL.md、minecraft-resource-pack/SKILL.md，并读TDD/写好测试/verification及systematic-debugging规则；只用任务卡的窄验证与无Git/无MC边界。实际版本由gradle.properties/build.gradle核MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，不跟技能新版本示例升级。

## RED、必要纠错与GREEN

- 安装前唯一该类RED：3 tests/3 failures/errors0，真实解析旧模型后lower期望167/旧60、wholeitem期望346/旧145，新machined/fastener PNG缺失。实际 `BUILD FAILED in16s`；`client/red/gradle.log`、JUnit XML/result原样保留。直接通知资产作者“可安装11”，他确认RED并精确安装，assets.json最终installed SHA `cab843377ae30aba096096cc76b0f7ea8b4d93d26b1392a6d761556f79e3e0d4`。
- 首次安装后定向运行：3 tests/1 failure/errors0，exit1、19s；四运行模型和PNG解码已过，物品Y比较expected28.015999/actual28.016差一个float ULP。不是UV或源几何漂移，原结果保留 `client/green/`。
- 仅测试Y偏移比较改为max(1e-6f,2×float ULP)，中文说明先解析float再+16与直接解析组合十进制数的单精度舍入；X/Z及UV容差和资产严格JSON/像素同源校验保持。没有改资源或Renderer来迎合测试。
- 必要纠错后只该类GREEN：**3/3通过、0失败/错误/跳过、exit0、15s**；完整日志、JUnit XML、exit-code/result在 `client/green-corrected/`。没有build/clean/rerun/full tests/GameTest或Minecraft启动；已有EventBusSubscriber弃用警告保留，不扩写注册修警告。

## 正式PNG网页证据

当前入口 [http://127.0.0.1:4192](http://127.0.0.1:4192)，PID **85932**，executable `D:/Program Files/nodejs/node.exe`，参数 `server.mjs 4192`，cwd为本美术树 `tools/art-assets/centrifuge-art-14b`；启动前核4192空闲，Windows后台Hidden，仅127.0.0.1。原4191/PID84900保持未动。身份在 `preview/runtime.json`。

server固定白名单仅本页/资产契约、五正式JSON/六PNG及原renderer2.20.0/Three0.180.0；拒绝SVG/其它路径。capture只保存本批两画布和JSON到14B preview，异步保存按钮在await前保存引用，避免旧currentTarget错误。`window.centrifugeArt14B.getState()`只读记录实际契约/11载荷SHA、运动归组/相位、元素/相机和draw-call。

本页三脚本语法实际exit0；有限HTTP **22/22通过**，包括11实际PNG/模型与安装契约/磁盘SHA一致、页面依赖与固定库、SVG/无关路由拒绝。原始 `preview/http-checks.json` 明确browserVisualVerified=false，不当作GPU或游戏验收。子线程不重试不可用CUA；负责人在同4192实际看运行机、输入轴/转子、真实PNG及物品后保存本批截图，再进行同次组合窄审。

当前只交源代码/定向GREEN/实际PNG预览，不打包、不宣称Minecraft手测通过。待负责人GPU与原独立审查无必改后，持有者备份旧d12包并执行唯一必要增量assemble，绑定11资源与Renderer class和真实输入身份；随后停美术候选等待用户集中手测，不推进main合入或其它设备。

## 负责人GPU、唯一assemble与最终制品

上文“未assemble/待GPU审查”为当时交回状态。负责人实际在4192/Tab14查看真实PNG正常机、底部、运行机剖视、静态346元素物品与低速；原同次组合窄审当前源/资源/定向/GPU无剩余必改，明确放行唯一增量assemble。运动实际捕获phase0→1.2086754135912774，shaft/rotor同phase，静壳与item均phase0；截图overview-final.png、bottom.png、cutaway.png、moving.png和motion.json保留在 `C:/Users/IKSXH/.codex/visualizations/2026/10/08/01a11c6b-f440-77c1-9275-5b2dca5cfa0f/centrifuge14b/`。最终stop/关cut/reset/capture实际成功，当前ready=true/errors=[]、12输入/19来源、默认yaw=-35/pitch16/zoom1/cut=false/slow=false/phase0；当前JSON原样冻结在 `client/assemble/preview-final.json`。这是网页GPU观察，不是Minecraft实际运行手测。

先精确核旧包SHA与卡一致，再逐字备份到 `client/backup/create_nuclear_industry-0.1.0-d12cca860f69.jar`：2,778,351字节，SHA256 `d12cca860f69f0b411a99b3c7f34bacc17895c7ac4fb7bbd149616769dc839c5`，备份后SHA一致；原身份在 `backup-identity.json`。随后只执行一次 `./gradlew.bat assemble --console=plain`：**exit0、Gradle1秒、实测1.8450371秒**，compileJava/processResources皆UP-TO-DATE，只jar执行；未build/clean/rerun/test/MC，日志与exit/elapsed在 `client/assemble/gradle.log`、result.json。

最终美术候选 `build/libs/create_nuclear_industry-0.1.0.jar`：**2,815,098字节，SHA256 `7d62147c84392f00062d19635cd0a5e91300d89860b1e8502cea17079e98f507`**。非目录载荷1946项，其中class557、assets1127。11目标资源逐项符合assets.json契约、正式安装、build/resources输出与JAR同SHA；Renderer.class与build/classes输出同SHA `5c5fcf7291e52fe0eb224c07fc606abfb7279a22d0253f07612097d90c4a6958`，已审八手写来源SHA保持。绑定与完整差异在 `client/assemble/artifact-identity.json`。

与旧d12包解压载荷逐项对比，只有**11目标资源＋1 Renderer.class共12项差异**：其中shaft/machined/fastener三资源新增，其余九项改动；无删项，其他1934载荷SHA保持，没有其它既有源码重编译引入的载荷漂移。没有新增逻辑修补。专属JUnit class、14A/14B工具/preview、assets.json均未进入JAR，toolTestLeaks为空。

最终输出字节码 `CentrifugeRenderer.javap.txt` 显示静态初始化依次PartialModel.of/putstatic ROTOR与SHAFT，两个renderInto前均fload9同角度、Direction.UP、iload5同light与cutoutMipped。`CentrifugeClientEvents.javap.txt` 的客户端注册仍getstatic ROTOR后registerRenderer，触发整类初始化足够；未改该class。上述字节码证据保留在client/assemble，不把它扩展成真实GPU或停机业务证明。

原4192/PID85932、4191/PID84900服务保持，Tab14为当前交付页。执行者已完成本批包身份交回，原同一审查者只补制品绑定；没有Git/世界/配置或正式主目录包替换。候选停在用户集中Minecraft外观/四向/真实正反转/停止/超载/磨损与重载人工门，不以自动或网页证据代替，不自动进入main或其它设备。
