# ART-REACTOR-02R1：客户端反馈定向整改

日期2026-10-10，美术负责人。用户实际截图反馈02B成型后变浅、线条/立体感减弱，观察窗仍逐格带框；本批按已批准连接纹理目的整改。当前状态：资源/消费者已冻结，17/17定向测试、增量JAR及一次独立规格/质量窄审通过，内部候选通过；主PM[范围补充](../ART-REACTOR-02R1-COORDINATION.md)及共享接口面合同均已应用。最终用户视觉门保持未通过，停在本批客户端定向复看。

> 执行者读取本卡与已交付02B/L1合同，按superpowers:subagent-driven-development的任务边界执行；只有负责人维护本卡状态。任何Git写操作、子代理派发、客户端启动、主工程/逻辑树写入均禁止。

## 目标与实际根因

1. 成型只改变连接关系，维持原单块的深色钢材、浅色混凝土嵌板、凹槽及亮边层级；不能把原大面积深色框换成平铺浅板。只读像素统计：原casing_side平均亮度102.30，原02A全连接格172.89，浅板由内部3×8扩大为贯穿整格7×16，截图变浅有实际来源。指标用于诊断，不替代观察，不为达数字把图块机械压暗。
2. 窗口移除窗片之间的内部框，只保留窗口簇外圈和真实凹角；窗口到壳体/仪表不把它们当窗。当前16格均固定中央8×8透明，导出器还强制该范围，Java没有窗口材质域，因此旧图集无法消除内部框。
3. 保持原版Create风格及SVG作者源。所有游戏像素来自16×16整数rect SVG，再确定性导出图集；禁止生图、直接在PNG画或改色、复制外部Mod资产。原SVG/PNG与旧02A/02B报告证据完整保留；不得改存档、运行配置或其他设备。

工作区唯一`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，每个shell显式workdir，实际HEAD`8b83a1a97f1b539e6dcc7b6714b9ee96c13854e0`。原02B已冻结未提交；主PM[登记](../../reviews/2026-10-09/reactor-connected-texture-02b/HANDOFF.md)、[消费合同](../ART-REACTOR-02-INTERFACE.md)、[候选](../ART-REACTOR-02-CANDIDATE.md)均只读。美术负责人只写art计划/报告，实现交明确写集执行者。

原截图：`C:/Users/IKSXH/AppData/Local/Temp/codex-clipboard-1f5718bf-c1ed-4800-b300-0e7a0982442d.png`（成型前）、`codex-clipboard-48a3a860-4601-4970-b4b2-1818d7a6d073.png`（成型后）；文件仅作只读反馈，先保存SHA/必要证据引用，不改图。

## 技能与版本

实际读取最新主工程与本树AGENTS、治理1.2/5.1、美术入口和本卡、02B执行/审查/L1 HANDOFF。应用`C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-resource-pack}/SKILL.md`；`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/{systematic-debugging,receiving-code-review,test-driven-development,verification-before-completion}/SKILL.md`及TDD的writing-good-tests。职责、线程、坐标、非显然规则用中文注释，测试必须真实行为red/green。

MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6保持。项目5.1/5.2优先于通用技能全量suite、提交/清理/删除既有实现要求；本批复用L1/02B已审生命周期，不跑旧存档、全量设备或无关GameTest。

## 精确写集与冻结保护

新增独立`tools/art-assets/reactor-ct-r1/`，仅：

- `sources/<原sprite名>/tile_<两位索引>.svg`，13项各00–15，window00–63，共272份；窗口47有效上下文、17不可达槽明确标成隔离回退填充，不能作为额外有效状态。每格16×16，原14个sprite名字/target ID不变。
- `palette.json`、`mapping.json`、`export.py`、`README.md`，及`generated/tiles/<name>/tile_<index>.png`、`generated/<name>_ct.png`；不修改共用工具或旧reactor-ct目录。
- 新导出器只读共用严格SVGrenderer，按SVG产物粘贴atlas。13张64×64，window128×128；窗口完全连接内格允许无rect的全透明SVG，只在明确透明预期下可用，不能放宽其他源稿验证。每格RGBA≤16色、alpha0/255，所有非窗口全不透明。

消费阶段准许修改既有：

- `src/main/java/com/iksxh/create_nuclear_industry/structure/client/ReactorConnectedTextureBehaviour.java`
- 同包`ReactorConnectedTextures.java`
- `src/test/java/com/iksxh/create_nuclear_industry/structure/client/ReactorConnectedTextureTest.java`
- 14张`src/main/resources/assets/create_nuclear_industry/textures/block/reactor_ct/<name>.png`，仅从本批SVG导出的图集逐字节安装。窗口阶段须等主PM明确补充；其他13项的类型/目标名/尺寸保留。

允许交付`docs/art/reports/ART-REACTOR-02R1.md`及`build/reports/art/ART-REACTOR-02R1/`证据/一次性作者脚本、原始命令/退出码/基线JAR副本。不得修改另外一个CT事件文件、02A/02B旧源/报告/证据、ART01源/图/共享色板、原sprite/JSON、L1/API/AT、构建/注册/语言/Ponder/玩法、存档或运行设置。负责人并行art文档/PM共享补充不计执行者范围差异，自动日志保留并单独记。

开工保护旧02B三类/测试/14game图及ART01/02A相关输入；先保存旧JAR到本批baseline，之后本树JAR允许被本批必要增量构建替换。若旧JAR哈希与02B候选不同先报告，不能用旧数值绑定新候选。

## 材质整改合同

- 以当前14个原sprite及相应SVG的实际材质/功能图形为参考，保留既有材质色，不重新发明大面积浅背景。外壳内部仍有深色基底、成对嵌板或铅灰压板、1px高光/阴影与中央凹槽；不以去掉每格框边为由抹掉局部凹凸层次。
- 全连接中间格和单块的主体色域与亮暗面积接近，整面钢框只放真实外沿。重复板接缝可以保留窄深线和亮侧，避免新浅色线成为最突出的大网格。
- 端口/仪表/换料/驱动器保留真实孔、法兰与字形，冷热红横/蓝竖标不减辨识；其背景跟随同家族深色材质，保持顶/底/侧关联。
- 窗组外圈沿用原window的暗钢框和明暗色，内部窗片透明延伸到共享孔边。1×N与N×1共享边的外圈端点保留边框，不能为“整条边全透明”破坏顶部/底部连续轮廓；两边都连接且对角也有窗时角区全透明，真实缺角保留凹角轮廓。

负责人先看三联对照：原单块/旧成型/新成型，同一光照模拟及同一缩放；必须含侧面中间/边角和闭合cube、3×3窗簇中仪表打断、冷热口。预览只从SVG输出拼合，源截图用于对应反馈，不作游戏实证。

## 八向窗口已核实的接入

`reactor_ct_audit`只读锁定Create字节码，确认`AllCTTypes.OMNIDIRECTIONAL`为sheet8/ContextRequirement.all；原生buildContext角连接=对应两条边均连且对角testConnection为真。六参连接覆写继续被八参转调，不要求反向连接。

- `getDataType`：可靠window返回OMNIDIRECTIONAL，其余可靠表面仍RECTANGLE，无记录null路线不变；shift entry工厂中的window也须同类型，不能数据型和entry sheet不一致。
- 保留同dimension/owner/generation/revision/bounds、同外平面与实际局部ID验证。当前格为window：另一格必须window，允许四邻接或面内两个切向轴差各1、法向差0的对角。禁止泛化distance≤2，禁止轴向跨两格。
- 当前格其他13类表面仅四邻接，仍可连接window背景；窗口到壳体/仪表false是窗口簇绘图域，壳体到window true是结构背景绘图域。
- 原生边门控确保仅斜角相触、不通过两条合法邻边的窗不去角框。不同owner、未加载/失效/局部替换沿原可靠快照路线保守回退；不新增扫描/同步或假造有效结构。
- 只原生buildContext与getTextureIndex，不写替代UV。离线mapping256输入经原生边门控归一得到47canonical，对应47唯一Create原生索引；17槽不可达回退填充。

只读核对的关键索引：孤立0，完整内窗54，四邻有窗但右上缺窗53，仅上/右且右上缺窗17，仅上/右且右上有窗20。执行者须用真实Create `OMNIDIRECTIONAL.getTextureIndex`核对47mapping，不能依照记忆铺图或只有离线公式自证。

## 共享接口面定向整改

只读实际字节码核实：普通window为`noOcclusion`，六面cube模型仍有接口面；默认相邻剔除与背面剔除不能同时消除这对面。相邻窗正面透明延伸后，斜视仍可看见接口面上的原钢框。不得只按平面拼图判定内部框已消除，也不修改方块/模型JSON来规避。

主PM已在同一[范围补充](../ART-REACTOR-02R1-COORDINATION.md)明确允许现有消费者补齐面mask，保持原一次capture：

- `gatherModelData`在同一不可变快照作用域核对同owner/generation/revision/bounds、实际ID及共同可靠外向面F，只标记该平面切向四邻接窗口之间的接口D。不能遮蔽外向面、孤立/外圈非共享面或仅斜角接触。
- 六位mask写入本次`ModelProperty<Integer>`；未知、非窗、不同owner或无可靠快照明确为0，不能沿用上次有效值。
- 五参`getQuads`只读ModelData，不访问世界/ThreadLocal。指定side只拒绝该mask面；side=null按quad方向复制过滤，保留其他面且不改super共享列表。缺失属性按0回退，非共享背面仍保留。
- 定向行为测试涵盖两侧共享面、外向面/外圈保留、不同owner/未知回退、非窗、side=null过滤及原输入列表不变；与本批八向窗口共用一次最终test/jar与独立窄审，不重跑旧L1矩阵。

4971字节共享正文已实际读取，主树/美术树SHA256均`ccf3a3a0be2aaf1c02e97b1fa1818016fae40193bf407bfba2ccdc86f30556a7`。此前3191字节类型补充有效历史保留，当前执行以含接口面补充的完整正文为准。

## 执行与定向证据

- [x] 用户截图反馈、原像素/SVG/validator/Java根因和原生八向能力已核实；本批为既有批准目的的必要整改，不追认视觉通过。
- [x] 保存当前写集/保护输入/旧候选JAR基线；资产阶段先修13个RECTANGLE表面，保留原02A/02B。
- [x] 主PM窗口类型/规格补充实际到美术树后读取，才实现窗口8向/消费补丁。若未到，继续13项和只读原生映射准备，不以超时当批准。
- [x] 窗口接口面实际根因核实，主PM同文件追加mask授权实际同步并逐字节匹配后读取；不扩大写集或修改Block/JSON/L1。
- [x] 专属测试先补真实red：window自身/壳体定向许可、六面合法对角、轴向跨两格拒绝、不同owner/ID/非外面、47项真实索引、仪表打断/对角缺窗/仅角接触；不重复L1矩阵或源码镜像。记录非编译失败red，再完成既有两类的最小修改。
- [x] 新源/导出/mapping与窗口按8位上下文的透明区域校验；原生47索引、17填充、2×2/1×N/N×1/L形/中孔仪表/不同owner离线拼合，透明孔/外圈与凹角、成型前后色/深度三联预览真实看图。失效写前拒绝/一次重复导出保持，限本批新工具，不重做旧02A。
- [x] 负责人看预览达到反馈方向后允许安装14图；否则只改本批新源/工具，先定向复看，再进入最终增量test/jar，不机械重复包。
- [x] 一次最终`./gradlew.bat test --tests '*ReactorConnectedTextureTest' jar`，记录实际非零用例数、XML/退出码；14源/安装/JAR字节/实际sheet/type核对，旧原sprite/JSON/共享逻辑保持，新源与冻结证据绑定。
- [x] 提交未提交交付与报告，精确范围/手写空白通过；自动日志全树非零如实记，保留原样。交一次独立合并规格/质量窄审，不重跑旧L1/02B或再审整支。
- [ ] 客户端只复看用户本次失败项：同视角成型前后色/线/凹凸一致，窗口簇内部去框/仪表处凹角完整，以及针对消费变化的不同owner/失效回退；原已通过门不因本批重开。实际用户观察后再记录视觉结论。

任何真实API、共享合同或逻辑缺口先报告；负责人不自行改PM核心文档/需求或代写实现。其他设备仍不派发，不启动客户端，不做Git集成。

## 首稿实际复看

执行者开工退出0：旧JAR与02B SHA一致，保存本批baseline；944保护输入和两张反馈截图SHA留证。新13项208SVG/图块+13图集已定向导出、字节核对通过，13个孤立格与原sprite逐像素相同，未动window/Java/game或共享渲染器。

负责人实际查看`build/reports/art/ART-REACTOR-02R1/material-triptych-13.png`及`closed-cube-13.png`，看到主体亮度回到原材质附近（casing_side95.09），但大片均匀深底仍令嵌板/顶面件显得贴在平面上；要求仅本批新13面再补窄深凹缝、邻接低亮侧及嵌板亮侧/阴影侧，顶底相应压板台阶，保持内部细线/外沿粗框差别。此时不安装game或进入打包，既有保护/孤立格证据不机械重跑。

执行者第二稿仅补本批压板台阶/局部凹槽，首稿另存`*-first.png`；负责人实际复看同两张更新图，确认主体材质、嵌板嵌入关系及内部细线/外沿粗框层次符合本次反馈方向，13项素材方向通过。13孤立格与10功能面核心像素定向核对通过，旧全集合/构建未重复。完整窗口预览复看前继续不安装game。

主PM共享补充3191字节已在主树及美术树实际存在；负责人读取其正文：仅窗口OMNIDIRECTIONAL128，余13保持RECTANGLE64，14targetID不改，47有效/17回退，现有两消费者/专属测试及独立源目录可写，L1/事件/模型/注册/玩法仍只读。依此正式接续窗口段，不等待聊天回复。

## 窗口消费者实际进度

负责人实际读取本批`tdd-red.log/xml`、`entry-type-red.log/xml`及`consumer-green.log/xml`：初轮15项出现3个实际断言失败，分别捕获旧窗口材质域、RECTANGLE类型与未隐藏共享面；真实shift工厂另有1项类型断言red。两类最小补丁后17项green，failure/error/skip均0，原始日志退出0，19秒增量执行。本阶段尚未安装新14图或最终打包，不把消费者green当最终候选。

原生运行导出的`native-window-contexts.json`已实际读取，47canonical/47唯一索引及0/54/53/17/20关键值一致；`native-window-scenes.json`记录同一生产行为生成的2×2/长条/L形/仪表缺口/不同owner图集索引及实际可见quad方向，供完整预览使用，不能用任意手工藏面代替模型证据。

## 完整离线预览复看与安装许可

负责人实际打开`mixed-triptych.png`、`closed-cube.png`、`window-clusters.png`和`window-thickness.png`四图：13项保持此前第二稿，原单块/旧02B/新R1同缩放及光照三联中深色钢基底、嵌板和凹凸层次恢复；完整外壳三面闭合。窗口2×2/长条/L形/仪表缺口轮廓连续，仅真实外圈和凹角留框，3×3完整内窗透明；不同owner仍分开留框。

厚度页按本轮真实模型行为导出的方向清单进行投影，显示同组共享隔框撤下、不同owner接口保留；光照/几何模拟不代替用户客户端。内部预览方向通过，已明确允许执行者完成本批新工具定向校验后安装14张图并进入一次最终定向test/jar、冻结交付与一次独立窄审。

## 最终冻结与独立窄审入口

执行者已冻结[实施报告](../reports/ART-REACTOR-02R1.md)、580交付路径和91证据；负责人实际读取最终XML、test/jar原始日志、资源校验JSON及工具正文：17/17，0 failure/error/skip，22秒增量退出0。14组源/安装/JAR一致，窗口128/其余64；944基线仅17授权变化，927保持，旧JAR940原资源条目保持。实际HEAD仍`8b83a1a`。

负责人实际核对本树JAR2373666字节、SHA256`ca591c0d64b6d184930bd42ba65a2c1647ab9ac8decdf9206ed94f788f80d533`；新候选副本/预览/本次定向人工门集中到[02R1候选](../ART-REACTOR-02R1-CANDIDATE.md)。真实失败与自动logs尾空白保留；手写279路径空白0错误，不清理无关改动。

已向原独立执行者`reactor_asset_review`派发一次合并规格/质量窄审，仅写本批独立报告，复用未变证据，不再导出/构建/测试。审查结果到位前不宣布内部最终候选通过；用户视觉门独立保持。

独立[审查报告](../reports/ART-REACTOR-02R1-REVIEW.md)已实际读取：规格与内部质量通过，必改项无；四张新预览实际看图，671冻结哈希0缺失/不一致，未重跑构建/测试/导出。负责人再次只读核对候选副本JAR与最终哈希相同，内部最终候选通过，停用户视觉门；不自动推进其他设备或做Git集成。
