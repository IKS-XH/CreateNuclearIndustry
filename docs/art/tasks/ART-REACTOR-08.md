# ART-REACTOR-08：双面连窗、库存液面稳定与铅灰玻璃

2026-10-12用户在既有反应堆美术上反馈三项：透过已成型观察窗看到的对侧内面没有连接纹理；库存冷却剂数量变化时液面闪烁；成型玻璃全透明似空气，需要金属铅的颜色。这是用户直接要求的显示整改，保留原风格、SVG来源与真实库存/容量/混色规则，不重开其他设备或原已验收遮棒故障。

唯一写工作树 E:/MyMC/NewMod/Create_NuclearIndustry-art-studio；主目录/逻辑树只读，禁止Git、游戏、存档写或子委派。当前基线6042fd2及未提交07B-R2试用；保留所有既有改动与制品。负责人按用户美术专项授权管理与审查，实现由原执行者完成。新增共享逻辑/网络/规则若确实必要先报告协调，不自行扩大；中文注释，锁定现行MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。

## 阶段一只读定位

原消费者 reactor_ct_assets 核结构/client内现有CTBehaviour/窗口Model、L1外向面/同owner描述，以及动画事件、VisualState、实际冷却液Renderer/批次顺序与L2更新路径。分别指出能复现的根因、最小客户端修复写集、是否有共享缺口；此阶段不修改生产/测试/资源或运行Gradle。主树同路径可只读比对，防止用美术旧源解释不同客户端问题；不猜截图尺寸/运行成型。

原SVG作者 reactor_svg_impl 核reactor-ct-r1的窗SVG/PNG/atlas、反应堆window模型与实际source→installed映射，说明为何成型玻璃为空，以及铅灰透明层的最小同源素材方案/写集。现有工具与素材只读，此阶段仅简短消息，不生成或安装，不画未经实际接口支持的展示。

两者实际读取AGENTS、美术README/PLAN和本卡，以及Minecraft modding/testing/resource-pack技能；系统排查先定位、再定写集，旧反应堆L1/L2/遮棒/02R1/03R3及汽轮机证据复用。

## 后续交付原则

负责人核实根因后补精确生产/测试/新工具与资源写集。连窗须内外一致，仍校验真实同owner/generation/bounds/窗口邻接，不扩大实体机壳CT或伪造外向面同步。液面按真实库存显示；客户端无法安全消除共享撤销引起的闪断时，按跨逻辑协调处理，不保留已撤销信封或伪造新样本。暂停/卸载/未知失效与原缓冲排序保持。玻璃用低饱和铅灰半透明层，仍能看清内部，来源只用SVG，先负责人实际预览再安装。

新增行为仅做必要定向回归，原始失败和窄修证据分开；唯一最终增量assemble、07B-R2 JAR先备份、source/资源/class→JAR绑定与一次独立窄审。不clean/全量/旧存档兼容或重跑已审矩阵。真实双面透窗、液面连续变化与玻璃质感仍由用户在原存档复看；本卡不关闭其他人工门或处理Git集成。

## 阶段二：已定位的客户端与SVG整改

负责人已核现行调用与SVG。用户补充闪烁为“整片液体短暂消失再出现”：外部普通fill/drain进入Instrument.setSnapshot，在正式结算作用域外发布L2不可用，BER撤下整液，下一成功结算再恢复。此共享缺口已按用户专项协调授权交主PM；共享Instrument/FluidHandler/Descriptor/State及已验收InternalRenderer/RenderBuffers/Materials/VisualState均只读，不安排客户端续租或缓动掩盖。

原消费者reactor_ct_assets的生产写集仅：

- `src/main/java/com/iksxh/create_nuclear_industry/structure/client/ReactorConnectedTextureBehaviour.java`
- `src/test/java/com/iksxh/create_nuclear_industry/structure/client/ReactorConnectedTextureTest.java`
- 本卡指定报告 `docs/art/reports/ART-REACTOR-08-CONSUMER.md` 与证据 `build/reports/art/ART-REACTOR-08/consumer/`。

真实外向面查找优先。仅实际WINDOW内面可用该面反向的已可靠外向Member，沿其真实外平面验证localID/pos/bounds/owner/generation/revision和邻接；Create.buildContext仍接实际渲染面，由原生处理内外八向朝向。实体壳面与sharedWindowFaces切向遮罩不扩大，L1公开合同不改。先在专属测试中复现内面null/未连接，再最小修复；测试实际生产Behaviour与Create上下文，不以字符串或单纯同公式自测。覆盖六面内外、边角/缺对角、独立owner或代次、本地替换/缺成员和壳面原退路；同一专属class定向一次即可，失败证据保留，不运行assemble。

原SVG作者reactor_svg_impl的素材写集仅新目录 `tools/art-assets/reactor-window-art-08/` 与证据 `build/reports/art/ART-REACTOR-08/assets/`，报告 `docs/art/reports/ART-REACTOR-08-ASSETS.md`。旧reactor-ct-r1及其他既有工具全部只读；从其64张窗口SVG复制保留形状、原生47上下文和17隔离fallback索引，仅原透明窗格区域新增铅灰 `#66696A`、alpha64/255。金属框/角/边所有RGBA逐像素保持，新增透明度明确写于SVG，不从PNG后期修色。局部新导出器严格检查整数矩形、色板、唯一pane透明度、mask窗格覆盖与实际Create索引，不改共享SVG工具或R1导出规则，不重导其他13图集。

新目录允许64份窗口SVG、色板/精确来源映射、窗口专用export/check、generated窗口tiles及128×128图集、README和HTML预览。预览比较全连接与边角、单/双玻璃层的铅灰颜色和内部辨识度，标明离线透明混合示意；不要宣称游戏透明排序通过。先交负责人实际浏览器复看，未复看前禁止安装。

负责人确认候选后，作者可仅替换 `src/main/resources/assets/create_nuclear_industry/textures/block/reactor_ct/reactor_window.png`；旧正式窗atlas先备份到本批assets证据。基础16×16 fallback、block模型/JSON、其他13图集不改。安装后源SVG→导出/安装图集绑定一次即可。最终assemble及class/JAR核对另待负责人在共享协调结果明确后授权。

## 阶段三：共享交付后的唯一组合打包

主PM已实际净同步共享修复至美术HEAD `f787aa40a323d8fdd09a0f95e5798a9a2c8e3818`，负责人已读本树 `docs/reviews/2026-10-12/reactor-runtime-coolant-03/HANDOFF.md`。仅两共享生产源、新独立GameTest/旧NBT逐字副本及报告；公开服务端 `setCoolantInventories(long,long)`，原客户端合同/批次保持。1/1真实GREEN、17/17相关回归、唯一共享assemble和同一次独立窄审复用，不在美术重跑。主PM负责可变候选的08净同步，本树不引入可变玩法。

原消费者reactor_ct_assets现在可仅在已有consumer证据目录做最终绑定/更新本批消费报告，不修改生产/测试/素材。先核自身两源与GREEN冻结身份、新窗atlas/generated/安装SHA一致、主PM同步精确四源/资源及API；原consumer/scope中两共享源的旧身份保留历史，仅这两个净修复路径按HANDOFF改记最终身份，其他原保护保持。旧07B-R2的5源/5class/1PNG及其冻结JAR、其他旧反应堆资源不被夹带改变。

先确认当前JAR仍为 `b1c3c69c29dce947b99ccd0f0a187e9689958217a2f3727c80487d9033cd8d88`，备份到本批consumer/baseline或已有freeze身份一致备份，不覆盖旧记录。唯一执行 `gradlew.bat assemble --console=plain`，增量无clean/test/build/rerun。保存原命令/cwd/exit/日志，核本批CT class/atlas及主PM同步的5class/新NBT与build/ZIP一致；记录最终JAR字节/SHA和source/API，复制本批冻结JAR与最终组合manifest，不覆盖旧consumer/red/green/freeze或共享证据。

来源清单须精确区分本批艺术净写集（Behaviour、既有专属Test、唯一atlas、新 `tools/art-assets/reactor-window-art-08/` 来源及少量报告）与主PM共享输入；不要纳入07B-R2或整个dirty状态。主PM据此将08单独同步到可变候选。本批新的manifest/报告绑定使用最终文件，不去重跑旧source检查/客户端测试/共享探针。完毕停写，原reviewer仅续同一次最终身份核查。

## 唯一独立窄审的预检查与最终绑定

窗口专属21/21 GREEN、SVG局部检查和负责人实际单/双层、边角预览已核；原作者按唯一atlas安装并停写。原reviewer reactor_ct_audit 可先审已完成的两源及新SVG/producer/安装像素，不运行生成/测试/构建，也不重审旧L1/L2/液体矩阵。只写 `docs/art/reports/ART-REACTOR-08-REVIEW.md` 和 `build/reports/art/ART-REACTOR-08/review/`；先发发现，最终报告等待共享HANDOFF及唯一组合JAR后补身份核对。这是同一次窄审的后续绑定，不重复测试或第二份审查。主PM专用纯库存入口的规格/质量审查复用实际HANDOFF，美术只核明确同步源/ABI和客户端消费边界。

## 内部交付收尾

2026-10-12消费者唯一组合assemble自然exit0、7秒，原reviewer完成同一次最终绑定，无必改项、说明Minor关闭，两者停写。负责人实读最终原日志/exit和完整报告，当前及冻结JAR均2,624,312字节/SHA `a4b08dd0a5bebe72555dc44969553ede120080ef66ff2ab38d27974648d353c7`。141项艺术来源与四项主PM共享输入分开登记，CT/atlas、共享class/NBT和07B-R2保护身份已核；原测试与共享证据复用，没有重跑。见[完整候选](../ART-REACTOR-08-CANDIDATE.md)。

主PM已同步08精确两源/唯一atlas/136工具至可变候选，唯一必要组合assemble exit0、8秒；其来源提交和交接文档由主PM管理。美术树不导入可变玩法、不写Git或启动用户客户端，真实双面透窗、铅灰质感和持续库存变化稳定显示留给用户现有存档复看。
