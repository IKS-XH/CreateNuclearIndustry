# ART-CENTRIFUGE-14A 资产交回

2026-10-14。资产执行者按14A卡交付未提交设计稿；不是负责人、未子委派。工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，实际HEAD `88b8fd289d49df90d7e3c655b6e81381ad01130f`。只写本批 `assets/`、`make-assets.py`、本报告和资产证据目录。原日志、旧 `__pycache__`、其他执行者的入口/预览保持。

已实际读取用户最新AGENTS、治理1.2/5.1、14A卡、美术入口及 `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`；应用Minecraft1.21.1的元素坐标、标准Y旋转和显式六面UV。另读 `verification-before-completion/SKILL.md`，按本卡有限实物证据收尾，不扩大到资源包安装或游戏测试。实际项目仍为Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。

## 交付与差异

[契约](../../../../../tools/art-assets/centrifuge-concepts-14/assets/concepts.json)为schemaVersion1，包含正式lower/upper/rotor路径与SHA-256、实际60/62/23元素数、四份正式PNG来源、接口语义与轴心；三项 `a/b/c` 的models/textures均是assets下相对路径。输出12份16²原创像素SVG、9份候选模型JSON，共21份源稿；另有契约和[可复现生成器](../../../../../tools/art-assets/centrifuge-concepts-14/make-assets.py)。候选仍四材质槽，JSON引用设计稿专属资源名，预览按契约取真实SVG；它们尚未成为游戏安装资源。

| 稿 | 实体与分区差异 | lower/upper元素 | rotor |
| :--- | :--- | :--- | :--- |
| A 精密钢筒 | 中亮钢灰长窗与纵向简洁骨架，细金端环；下段窗高7.2模型单位，上段窗高8.15 | 68/70 | 23 |
| B 铸钢加强壳 | 深钢灰厚底台和顶肩，八条宽加强筋；观察孔比A窄，金环较厚 | 76/78 | 23 |
| C 分段检修机壳 | 暖浅灰护板上下分段、窄暗缝后实体钢背衬、深钢端框；正交四面窗宽2.7、斜四面1.2 | 108/110 | 23 |

三稿都沿正式八棱壳/两格结构重构外观。原北向检修盖、底部轴套、顶进料孔位置保留。实际转子23元素、坐标、旋转定义及UV与正式模型保持；主要将原八根金色纵杆改为钢色，保留金色横连杆、窄端环和原功能部件。正式机械合成配方确含钢板/金板/重型轴承/传感器/whisk/精密构件，材质选择有来源依据。

原glass槽只是染色窄孔缘，正式中央观察孔没有整片覆盖面；本批沿用实体开孔与孔缘，不将整壳改成透明壳。C蓝色只在染色孔缘/深钢小压痕，赭色沿既有顶口与端环分区，未新建指定侧口或把色标绑定到不存在的独立功能。

## 实际接口与坐标

- upper采用自身格坐标0..16，预览整机时偏移 `[0,16,0]`；rotor采用lower底面原点，Y5..27，x=z=8为轴线，契约参考轴心 `[8,16,8]`。真实Renderer用 `rotateCentered(..., Direction.UP)`，参考点Y差异不改变这条竖轴。
- 上段UP现有顶孔中心 `[8,16,8]`，内部孔域x/z=7..9，为矿浆输入/回取；下段DOWN底部中心 `[8,0,8]` 是唯一动力输入。
- 上下两段全部四个水平面提供清水及两种粉末输出，并不存在只能某单独蓝/赭侧口使用的合同。
- 默认facing=north的下段北小盖中心约 `[8,3,0.5]` 是停转后的轴承维修处，随facing旋转；不是粉末/流体端口。所有行为代码只读。

## 有限验证与证据

最终实际运行：

```powershell
& 'C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' 'tools/art-assets/centrifuge-concepts-14/make-assets.py'
```

最终exit0，12份SVG尺寸16×16、viewBox0 0 16 16、crispEdges、无外部引用且各使用限定色板；9JSON元素/槽引用有效、所有显式UV在0..16。三个转子原几何23/23保持，lower/upper经真实Y旋转的包络各在本格0..16，整机两格高度保持；转子包络 `[4.22,5,4.22]..[11.78,27,11.78]`。14个实际来源文件前后SHA-256相同，包括三正式模型/四PNG/三行为源码/旧安装工具/正式item及blockstate/机械配方。

首次生成因新增斜向端环/压条默认UV略越界退出1，失败摘要[保留](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/initial-generation-summary.json)。随后仅平移这些面的纹理坐标域到0..16，保持UV跨度和像素密度，不裁剪实际几何；第二次exit0。追加必要包络/配方来源身份检查后最后一次生成也exit0，当前证据为[checks.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/checks.json)与[manifest.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/manifest.json)，后者绑定producer/14来源/22输出身份。

交回前只读核对37个来源/输出/producer身份，0项漂移；三转子的每面UV与正式源相同，报告链接均存在，实际exit0见[readback.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/readback.json)。全树 `git diff --check` 仅报告本批前既有的 `logs/debug.log` 与 `logs/latest.log` 第7行尾空格；这两日志不在资产写集，未处理。

未运行会写正式资源的旧 `centrifuge_01d_preview.py`，未生成磁盘PNG、安装src资源、Git写入、Gradle/JUnit/GameTest/MC、修改世界/配置或JAR。有限资产检查不能证明浏览器与Minecraft视觉；已向预览执行者交回契约与真实接口，负责人实际模型浏览和本批一次组合窄审接续。用户仍需在真实三稿预览中选方向，正式实施另定写集。

## R1 三稿精修交回（2026-10-14）

用户实际浏览初版后要求“能不能更精致一些”。按本卡R1，由原作者接续三稿；没有选择B、安装资源或改变玩法。上方初版数据与检查结论保留为原交付时点。

先将初版12SVG/9JSON/契约及producer原字节保存至[assets/history/pre-r1](../../../../../tools/art-assets/centrifuge-concepts-14/assets/history/pre-r1)，初版checks/manifest/readback/失败摘要及原ASSETS报告保存至[证据history/pre-r1](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/history/pre-r1)。27项源/证据SHA-256绑定在[archive-identity.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/history/pre-r1/archive-identity.json)，R1末次读回全部一致；原报告快照单独保留，没有改写初版通过为用户风格通过。

### 精修实物

已与原预览作者确认新增 `machined`、`fastener` 两槽：当前六槽、18份16²SVG，契约schemaVersion1保持，新增revision=r1；模型、网页贴图与六项缩略继续从同一契约取真实源稿。正式基线仍是四槽，新增槽只属于本批候选。

- 顶口：保留原顶孔中心与x/z=7..9孔域，以铸钢肩、后退暗槽、加工钢台阶、分层钢凸台和法兰承接；金色部件缩为嵌入式窄压环，顶面金色面积从原32降至8.25模型平方单位。顶肩仅四组垫圈/退台螺栓，整机最高仍upper本地16。
- 端座：钢边、暗退刀槽及窄金环形成三层外缘；底台增加小幅外伸的加工钢肩，不整体提高壳体亮度。A保持精密长窗、B保持厚台和八筋、C保持浅暖分段护板及暗钢背衬。
- 北检修盖：同原位置增加搭接加工钢框、后退钢片、窄金属压片与四个实体紧固点；实际维修朝向/角色保持，未画假仪表或添加运输口。
- 观察孔：八面均增加完整四边钢框及内层染色密封窄边，保持实体开孔。B孔宽1.45→1.9，加入密封后的净宽约1.44，避免厚黑边把原孔再次挤窄；A孔宽2.2、C正交2.7/斜向1.5。窗内八根钢杆切到加工钢槽，以局部材料区分加强辨识；原23元素坐标/旋转/全部UV保持。

R1元素数为A lower144/upper155、B152/163、C184/195，每稿rotor23；额外元素用于实际台阶/窄槽/闭合窗框与少量紧固件，并非新增功能件。六份材质每稿仍16²、有限色板、crispEdges，无外部引用、滤镜或提高像素分辨率。紧固件UV按其实际面跨度取样，未把整张16²图拉伸到小螺栓。

### R1受影响检查

沿原命令仅生成并检查本批SVG/JSON/UV/来源，首次R1实际exit0，无失败。当前证据写到新的[r1/checks.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/r1/checks.json)和[r1/manifest.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/r1/manifest.json)，原证据不覆盖。包络均在各自格坐标0..16/两格高度范围，标准旋转角有效；14正式来源前后保持。

末次只读核43项当前来源/28输出/producer身份、27项归档身份，均无漂移；14正式来源也与初版manifest一致。三个转子全部面UV保持，三稿顶部口中心在upper y15.99未被实体覆盖、底部两原动力套元素逐字保持，标准元素旋转角均有效，exit0见[r1/readback.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/r1/readback.json)。这些是有限资产检查，不是GPU、真实Minecraft或接口行为验收。

原作者已告知预览作者R1源稿ready。未运行原设备/全矩阵、MC或构建；未生成PNG、安装src、Git/main写入或碰世界/配置/JAR。相机/光照与网页近景由原预览作者按同卡保持，负责人浏览和原同次审查只追加精修差异，用户仍待精修三稿复看及选稿。

## R2 转子增粗交回（2026-10-14）

用户要求“转子加大一些吧”。原作者按R2卡只修改三份候选rotor、契约与producer，新增必要扫掠/UV及身份检查；三稿R1六壳JSON和18SVG共24文件逐字保持。未选稿、装游戏资源或改变真实旋转行为。

先原样保存R1三rotor/契约/producer至[assets/history/pre-r2](../../../../../tools/art-assets/centrifuge-concepts-14/assets/history/pre-r2)，R1三证据及本报告原快照保存至[证据history/pre-r2](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/history/pre-r2)。8项来源/证据SHA绑定在[archive-identity.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/history/pre-r2/archive-identity.json)；R1壳/材质原身份另存[protected-shell-textures.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/history/pre-r2/protected-shell-textures.json)。末次读回无漂移，上方初版/R1结论不改写。

### 实际尺寸与23分件

采用主体横向1.28倍，增大22移动件的XZ坐标及局部旋转originXZ；x=z=8全局竖轴、各段Y、局部旋转angle和originY保持。网页读取真实增粗后的模型，不使用group放大。细主轴element0保留原1.3宽、Y5..27及全部原坐标/UV；这是负责人确认的保守嵌座配合方案，并非全23件均匀缩放。

| 23元素分工 | 保持Y（模型单位） | 实际横向变化 |
| :--- | :--- | :--- |
| 0 单个细主轴 | 5..27 | 方形截面1.3→1.3，整根原样 |
| 1..8 八根纵杆 | 8..24 | 杆宽0.76→0.9728；杆中心半径3.4→4.352 |
| 9..14 三层横连接件 | 7..7.65 / 15..15.65 / 24..24.65 | 长4.6→5.888、宽1→1.28，各层厚0.65不动 |
| 15..18 四片正交立叶 | 8..24 | 横截面0.8→1.024，绕全局轴横向偏移同步放大 |
| 19..22 四片斜向立叶 | 8..24 | 横截面0.8→1.024，原±45度及局部Y原点保持，originXZ同步 |

三稿都是同一转子几何，保留各稿原R1材质分区。最大整圈扫掠外包络直径由约7.875增至10.080模型单位（28%）；这是现有方杆/叶片角点绕轴的圆形包络，不将23个盒状零件伪称连续圆柱或制造曲率。候选静态包络为 `[3.1616,5,3.1616]..[12.8384,27,12.8384]`，全圈半径约5.040，整机两格不超界。

### 360度扫掠、窗口/端座间隙与既有主轴接触

每个Y旋转矩形横截面求轴向最小/最大半径；整圈旋转的并集是该径向区间的圆环。以实际六壳JSON中每个固定实体的径向区间和Y区间求最小距离，并在所有静态/动态Y端点分层复核；不依赖少数相位截图或1度抽样。A/B/C分别覆盖19/26/25个有效Y区间，增大的22移动件与当前壳、窗框、密封和端座均有正净距。

| 稿 | 同Y层最小径向净距 | 增大22件最小实体净距 |
| :--- | :--- | :--- |
| A | 约1.770 | 0.960 |
| B | 约1.640 | 0.960 |
| C | 约1.730 | 0.960 |

单位均为模型单位1/16格。0.960发生在下层横连接件Y7..7.65与下端座上沿6.04之间，是轴向净距；同Y层径向最紧处是固定实体壳内缘而非网页剖视平面。完整22件逐项/Y层数据见[R2 checks](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/r2/checks.json)。

基线细主轴原本插入闭合端座：下端座与轴约Y5..6.04交叠，上端座与轴约26.96..27交叠，四交叉片的具体边界有原0.002错层。端座模型没有真实独立轴孔，因此不能声称全23元素与端座全净空。R2保留整个主轴及其接触范围，不扩大交叠、不伪造轴孔、不改壳；检查将这些既有嵌座接触逐项列在 `shaftExistingContacts`，不纳入22增大件净距通过的范围。

### R2受影响检查与交回

默认producer现仅写3rotor/契约及R2证据，不再生成R1壳/18SVG。新侧面UV只重投影横向U，纵向V逐字保持；顶/底面按新横截面跨度投影，材质仍原16²六槽。第一次R2运行exit0，追加必要originXZ/两格包络断言后末次同命令也exit0；没有失败或旧矩阵重跑。当前身份见[r2/manifest.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/r2/manifest.json)。

末次核当前43项来源/24保护/4输出/producer身份及8项R1归档身份，0漂移；14正式来源与原基线相同，三稿23元素Y/角度、侧V和主轴原样，全部新UV在0..16，实际exit0见[r2/readback.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/r2/readback.json)。

已告知原预览作者revision=r2、实际比例/尺寸及净距，按当前真实模型接续同源和负责人实看。只补本次必要资产检查，不重跑R1 SVG/30/52矩阵、设备/MC/Gradle，不写Git/main/src/PNG/世界/配置/JAR。完成后停止资产写入，待负责人同相机正常窗内/剖视/低速观察及原同次局部审查；这些实物计算不代替用户最终视觉选稿。

## R3 底部旋转轴交回（2026-10-14）

用户指出底部动力输入方块应是旋转轴。原作者先只读定位并向负责人回报：R2 lower的0/1是两层满方底台，6/7是6×6/4×4原静态接头，23是仍封中心的薄加工钢肩；2..5是原八棱端板。负责人认可只处理0/1/6/7/23、采用四角脚和上部围孔台以露出钢轴侧面，并选短轴上端Y4.0、与原端板约0.02装配接触；原2..5和检修盖不动。

R2当前28源稿及producer原字节保存至[assets/history/pre-r3](../../../../../tools/art-assets/centrifuge-concepts-14/assets/history/pre-r3)；R2三证据及原报告快照保存至[证据history/pre-r3](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/history/pre-r3)。32项源/producer/证据身份见[archive-identity.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/history/pre-r3/archive-identity.json)，24项上壳/R2rotor/18SVG保护身份单独保存[protected-upper-rotor-textures.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/history/pre-r3/protected-upper-rotor-textures.json)。本节只追加R3，上方R2数据和结论保持原时点。

### 实际几何与分件

每稿新增真实[shaft.json](../../../../../tools/art-assets/centrifuge-concepts-14/assets/a/shaft.json)：单个闭合钢短轴，截面4×4模型单位（0.25格），XZ6..10、Y0..4，竖轴x=z=8。侧面和端面都有真实面，复用现有machined槽；六面UV跨度都是4×4，保持原16²材质密度，不整张纹理拉伸。Y不低于整机底面0。

原lower 0改为四个静止角脚及Y1.25以上围孔平台；外界XZ1..15和Y0/原台顶保持。1和23仅拆条围孔，6/7移除原静态满接头，替换静止加工钢围孔轴座。孔域XZ4.8..11.2、宽6.4，轴杆下段Y0..1.25透过支脚中央可看见侧面，Y0端面可从下方观察；整个底台和轴座仍为静止件。

| 稿 | R3 lower元素 | 其余原lower对象逐项保持 | 上壳/R2转子/材质 |
| :--- | :--- | :--- | :--- |
| A | 163 | 139 | 原字节保持 |
| B | 171 | 147 | 原字节保持 |
| C | 203 | 179 | 原字节保持 |

新增轴与原R2转子是独立运动分件，契约schemaVersion1/revision=r3、`models.shaft` 为各稿真实路径，`shaft.coordinates=lower-local`、`phase=same-as-rotor`。已与原预览作者直接确认同轴/同相位映射；原机仍保持原三模型对照，不捏造原机输入轴。该候选没有修改游戏Renderer、真实转速或传动行为，正式实现另定写集。

### 实际360度净距与装配边界

短轴方截面角点扫掠半径为 `sqrt(2²+2²)`，约2.828模型单位。用矩形径向区间的整圈并集，对三份真实静止lower逐项求距离；除了明确装配接触外，最小正净距三稿均为约0.3716模型单位，即6.4方孔半宽3.2减最大扫掠半径2.8284。此值覆盖整个360度，不使用只在零相位为1.2的面间隙冒充转动净距。

原端板2..5逐项保持闭合几何，轴上端分别在Y3.98/3.982/3.984/3.986..4.0进入端板，最大约0.02模型单位。这是负责人确认的上端装配接触，检查列为 `assemblyContacts`；不声称这四处净空，也不扩孔、拆原端板或改R2主轴的既有嵌座。装配范围外无相交，完整实际数据见[r3/checks.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/r3/checks.json)。

### R3有限检查与交回

默认producer仅写三lower、三shaft、契约和新R3证据，未运行旧SVG/转子/网页30或52矩阵。首次实际同原命令exit0，修改lower/轴的元素与UV合法、lower包络在本格、其它lower对象保持、360净距及明确装配接触通过；身份绑定见[r3/manifest.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/r3/manifest.json)。

末次读回46项当前14正式源/24保护/7输出/producer及32项R2归档身份，全部0漂移；三新轴六面UV跨度均为4，14正式来源仍与R2相同，实际exit0见[r3/readback.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/r3/readback.json)。三上壳、18SVG及三份R2的23元素转子共24文件字节未写，原2..5/检修盖及其它139/147/179个lower对象的坐标/面/UV逐项一致。

已向原预览作者交回revision、shaft路径、实际尺寸、同相位说明、净距和约0.02装配接触，接负责人正常/剖视/底部/低速浏览及原同次局部审查。完成后停止资产写入；未生成PNG、安装src、MC/Gradle/设备测试、Git/main或世界/配置/JAR写入，资产几何检查不代替用户视觉验收。

## R3同次长轴窄修（首轮剖视反馈后）

负责人实际看首轮底部与剖视，确认静动分离及围孔正确，但4×4×4完整轴模型仍是正立方体，视觉上仍像钢块。上方首轮R3有限检查保留，不追认为用户所需轴形体完成。原31源稿和producer共32项保存至[assets/history/r3-first](../../../../../tools/art-assets/centrifuge-concepts-14/assets/history/r3-first)；首轮三证据与报告快照保存至[证据history/r3-first](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/history/r3-first)，35项SHA身份见[archive-identity.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/history/r3-first/archive-identity.json)。

先只读评估并报负责人，经卡中“R3首轮视觉反馈与长轴窄修”确认后，仅修改三shaft及lower四原端板孔域。当前真实钢长轴为4×4×8（0.25×0.25×0.5格），XZ6..10、Y0..8，竖轴x=z=8。侧面UV跨度4×8、端面4×4，原machined材质不变；长度明确为截面的两倍，不靠命名/旋转或网页临时几何掩盖立方体。

四原端板在首轮lower索引12..15，对应R2原2..5；每片原矩形窄宽约6.0675小于6.4孔宽，只沿自身局部长方向保留两端条，共四片→八片。每片原Y微错层、rotation.origin/angle和外界保持，原表面采用局部原全坐标UV投影取相同区域，新切面也是实际跨度。各旋转局部孔域4.8..11.2，中心始终同轴；没有改变底台、静止轴座、检修盖或其他lower部件。

当前A/B/C lower元素167/175/207；相比首轮R3，仅四端板分解，各稿其余159/167/199个lower对象逐项原样。三upper/18SVG/三R2 rotor23共24文件字节不变。契约仍schemaVersion1、revision=r3，新增 `refinement=long-shaft`，shaft实际范围Y0..8；网页原模型枚举/同轴同相位映射可直接消费，资产作者未写网页。

### 净距及同运动轮毂接合

轴最大360度扫掠半径仍2.828427，各静态孔内缘最小半径3.2。所有当前静止lower与长轴整圈净距均正，最小约0.371573模型单位；原首轮Y3.98..4静态端板装配接触已经被实际围孔消除，当前 `assemblyContacts=[]`，不再复用旧装配豁免。

长轴与不改字节的R2转子在同相位运动组连接：

- 主轴0的Y5..8位于长轴内，为同轴轮毂嵌接；不是静止摩擦。
- 横连接件9/10的Y7..7.65与长轴中心相交，为同运动组连接。
- 内斜叶19..22底面Y8与长轴顶面接合，同在运动组。

这些接合只记录候选视觉装配，不新增游戏传动规则。原上端主轴Y26.96..27与闭合上端座的既有嵌座保持，不能把整个转子宣称全净空。当前下端板真孔则同时消除原细主轴在下端板的交叠。

### 同次受影响证据与交回

本次一次受影响生成/检查exit0，写入新的[r3-long/checks.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/r3-long/checks.json)与[manifest.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/r3-long/manifest.json)，不覆盖首轮R3证据。末次身份读回核46项当前和35项首轮归档，0漂移；四原端板Y/origin/angle、真实UV密度及轴实际4×4×8/面UV通过，静态接触三稿均空，实际exit0见[readback.json](../../../../../build/reports/art/ART-CENTRIFUGE-14A/assets/r3-long/readback.json)。

已直接告知原预览作者长轴ready、当前7变化输入及全角净距，接负责人同条件GPU实看与同次局部审查。未增加材质/高分辨率、未改预览相机/灯光/控件，未运行旧矩阵、MC/Gradle/Git或正式资源安装。资产源/producer身份稳定，完成后停止写入；首轮外观失败仍保留，最终轴形体以本次实际复看为准。
