# ART-BOILER-05B：A横箍压力舱正式素材与成型连接

2026-10-11用户明确选择并要求“实施第一种设计稿吧”。设计来源为[05A](ART-BOILER-05A.md)及其05A-CT连接补充的A方案；风格已获用户确认，不重开选稿。目标是在现有锅炉上呈现深暖灰钢壳、贯穿墙面的横箍、窄耐火嵌片、外周厚钢收边、去内框连窗和同风格功能口。通过相邻关系/真实边界适配可变尺寸，不枚举整机尺寸。

负责人为用户任命的美术对话；实现交原执行者，负责人不写资产/生产或测试代码。工作树唯一为 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，起始HEAD `eef4f18`。旧05A/B/C设计稿、反应堆/冷却剂、其他设备、旧证据、无关日志保持。禁止Git写操作，主目录只读。

## 技能与技术约束

实际读取用户最新AGENTS、治理1.2/5.1、美术入口/PLAN及本卡。使用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`（`C:/Users/IKSXH/.codex/skills/<name>/SKILL.md`）；规划/执行采用writing-plans、subagent-driven-development，按本项目专项角色/禁止Git/一次合并窄审及增量验证规则裁剪通用重复流程。MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6均由当前配置核实，不升级依赖。

低分辨率内容继续以原创16×16整数rect SVG为源，有限调色板和crispEdges，PNG只能由SVG确定性导出供游戏使用，不调用生图。先候选导出/实际复看，再按明确资源映射安装。新增/修改手写代码及非显然算法使用中文注释。Python只用 `C:/Users/IKSXH/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe` 加 `-B -X utf8`，不能把WindowsApps的python/py启动失败当通过。

## 已确认表现合同

- 原稿A配色和金属明暗保持，不能因成型连接变浅或失去凹槽/压边立体感。壳体内部移除逐格厚框，保留周界/转角钢条；细横焊缝连续，顶/底及实际再加热隔层强调主横箍。顶面连成整片钢板。
- 连窗之间消除内部框线和实体框几何，外周保留厚法兰、暗内沿与蓝玻璃反光；应能透视既有内部/水位。支持单窗、2×2、长条、L形/缺角，不能仅为样稿2×2优化。保持现有碰撞、方块ID、noOcclusion和库存渲染行为。
- 控制器、水/汽/热/冷口保留各自功能图形与颜色，接口嵌入同一壳面。保持原facing、控件/管道接口和方块模型朝向；安全阀保留现有四段低矮几何；内置换热隔层材质补齐同风格，不能更改隔层玩法或碰撞。
- 成型连接只消费权威同炉归属、外平面、闭边界和绝对隔层高度。未知、撤销、局部块ID冲突、异炉邻接或Ponder缺快照时回单块。物品模型保持独立材质；不能靠客户端重扫BoilerStructure、窗口fill或运行Status判定成型。

## 前置及责任分离

只读审计已确认现有Controller仅同步Width/Height/Depth与运行遥测，Port仅SteamOutputStatus，Window仅Fill；没有可靠min/max/partitionY/同炉身份。已向主PM请求最小只读锅炉表面接口：现有服务端Form派生有效/撤销、owner/代次/修订、闭边界/partitionY/表面成员与ID、客户端不可变快照及旧/新范围刷新；规则、库存、热账本不改。接口实现/共享同步及Git净同步由主PM安排。正式CT消费须等待实际API/HANDOFF，届时负责人只在本卡追加确切消费写集与验证命令；当前不授予任何共享Java写权。素材制作可继续。

## 阶段一：正式素材候选（当前派发范围）

唯一资产作者为原 `reactor_svg_impl`，允许新增 `tools/art-assets/boiler-art-05/` 下原创SVG、色板/固定映射、确定性导出工具、generated、preview/README；短报告只写 `docs/art/reports/ART-BOILER-05B-ASSETS.md`；小型证据只写 `build/reports/art/ART-BOILER-05B/assets/`。旧05A与所有旧导出器只读。

候选基础纹理对应现有 `textures/block/high_pressure_boiler/` 的10项：`casing_side`、`casing_top`、`window_frame`、`water_port_face`、`steam_port_face`、`controller_face`、`exchanger_face`、`exchanger_top`、`valve_body`、`safety_badge`；以及block根下 `high_pressure_boiler_hot_coolant_port`、`high_pressure_boiler_cold_coolant_port`。按原模型引用精确映射，不泛化安装目录。

候选CT图集集中于新 `block/boiler_ct/`：壳体side/top和隔层强调side，以及窗口、水/汽/控制/热/冷口对应sprite。边界状态沿锁定Create原生RECTANGLE，窗口沿OMNIDIRECTIONAL原生8向/缺角索引（只读参考reactor-ct-r1，不改变它）；与消费者约定target ID后固定在mapping。每格16×16，图集为连接状态组合，不是穷举尺寸。图格SVG/索引/透明区域必须可追溯，禁止把现有PNG当新素材源。窗口中心透明、反光克制，连窗内部没有框。

作者先检查并记录需改变的模型资源列表和最终sprite映射，交负责人确认后才允许安装/改游戏JSON；本阶段初派发没有正式资源写权，也不运行Gradle。预览包含基础各面、CT墙/顶/连窗（至少单窗、2×2、L/缺角）、功能口、隔层横箍和现有四层阀；与已批准A连接稿对比。源检查只核画布/整数/色板/引用、真实Create索引与窗口透明/边界语义，必要写前拒绝例保持少量；不建立全仓冻结矩阵。

## 后续消费与验证边界

实际共享API到位后再派发客户端CT消费者，使用独立锅炉ConnectedTexture类，不改反应堆CT或既有液体批次。针对权威归属/外平面/邻接、失效回退、Create索引/共享窗面和注册重载做必要定向检查；共享同步生命周期证据复用PM前置，不重复实现或测试。

安装后只由一名执行者持有最终定向JUnit与一次增量assemble；不clean、不rerun、不跑build重复JUnit/全量GameTest，不启动用户Minecraft/改存档。一次独立审查合并规格与质量，读取已有证据。最终交用户在美术树现有存档观察成型前后同色/厚度、不同尺寸、连窗和功能口/安全阀，不把离线复看或自动通过称为游戏视觉验收。Git/主分支集成交PM或用户。

## 进度

- [x] 用户确认A及正式实施，当前版本/现有模型/共享显示缺口已只读核实。
- [x] SVG同源基础与CT素材候选、映射/实际复看。
- [x] 主PM交付只读锅炉表面API及共享精确写集。
- [x] 正式素材/模型安装与客户端CT消费。
- [x] 定向检查、唯一增量制品、一次独立窄审与用户复看清单。
- [ ] 用户游戏视觉确认，Git集成由PM处理。

### 阶段一安装补充（2026-10-11）

负责人已实际复看最终基础、9图集和修正正交/对角门控后的单窗/2×2/L预览，并读取真实bundled Python原日志：源检查退出0，12基础、128矩形及64窗图格SVG/PNG一致，原生R1索引表与少量拒绝例通过。初次生成器读取SVG错误已记录且修复；Pillow弃用提示不是失败。基础/CT保持各口中央图形、阀体/标牌分离、钢质隔层窄米色条的整改已关闭；不重跑源检查。

现只追加资产作者的21PNG逐字安装写权：`src/main/resources/assets/create_nuclear_industry/textures/block/high_pressure_boiler/`的前述10文件、block根的前述冷热口2文件，以及新 `textures/block/boiler_ct/{side,top,partition_side,water_port_face,steam_port_face,controller_face,hot_coolant_port,cold_coolant_port,window_frame}.png`。仅复制对应generated候选，不重生成，不写其他PNG/JSON/Java/注册/构建。一次核source/generated→安装21项逐字一致，在原短报告追加安装结果即可；最终打包由后续唯一客户端消费者持有。窗口模型和实际成型连接仍等待阶段二，不称本次素材安装为全批完成或游戏视觉通过。

### 窗口资源消费先行（仅模型，不涉及共享接口）

原只读审计者 `reactor_ct_audit` 接续本批消费者。当前仅授权修改 `src/main/resources/assets/create_nuclear_industry/models/block/high_pressure_boiler_window.json`，并在 `docs/art/reports/ART-BOILER-05B-CONSUMER.md`记录简短进度，必要证据在 `build/reports/art/ART-BOILER-05B/consumer/`。模型移除旧18element的框/玻璃/内沿，采用覆盖完整0..16立方体表面的六面纹理框，各面UV=[0,0,16,16]并有正确cullface；六面统一引用原 `block/high_pressure_boiler/window_frame`，particle仍为壳体，保留translucent。真实透明孔由已批准SVG/PNG提供，不能再叠原3..13玻璃或实体横条。不修改碰撞/Block类/注册/noOcclusion/水位BER/其它JSON。

只做本模型结构与已安装sprite引用的一次小检查，不跑JUnit或Gradle，不增加镜像单元测试；下一步客户端消费和最终定向验证/增量assemble仍由该执行者持有，待实际API另补确切Java写集。该资源阶段是批准设计的实现，不需要重新选择风格，内部连窗消框仍由后续权威同炉CT与共享面过滤完成。

### 资源阶段实际进度

负责人已读取两份执行报告及原始安装/窗口检查证据：21/21 PNG安装件与候选逐字节一致、退出0；窗口单element完整六面、UV/cullface及已安装引用检查通过，模型SHA为 `049cf9404d019b015c9aa624ddf1929e465af05389faaa9aa99be409cea69656`。这两项没有运行Gradle或游戏，也未改Java/共享逻辑。最终CT连接、共享窗面过滤、JUnit和增量制品仍待后续消费，不称全批完成。

主PM已接续前置并开始核实原用户授权及最小只读接口范围，保持锅炉玩法/热账本；尚未交实际API/HANDOFF，当前Java写权仍未追加。

主PM已在主目录发布 `docs/art/ART-BOILER-05-L1-INTERFACE.md` 与 `docs/superpowers/plans/2026-10-11-boiler-surface-display-01.md`。负责人已完整读取：纯投影已验证Form、完整六面外表面、绝对partitionY、schema1紧凑9角色信封、锅炉独立不可变快照及卸载/恢复，真实编译消费仍以最终HANDOFF为准。此为实现合同，不以文档中的预定签名替代实物API；共享逻辑在PM复用树实施，美术保持等待，不改共享路径。

原消费者只读核对锁定Create后建议后续三个独立客户端类为 `boiler/client/BoilerConnectedTextures.java`、`BoilerConnectedTextureBehaviour.java`、`BoilerConnectedTextureClientEvents.java`，两个专属测试为同包 `BoilerConnectedTextureBehaviourTest.java`、`BoilerConnectedTexturesTest.java`。RegisterGeometryLoaders内直接幂等登记，block注册ID作为模型键；gatherModelData一次capture/finally恢复，共享窗面mask写独立ModelData，getQuads只读mask按实际quad方向过滤。本段只记录接入方案，尚不授予上述写权或执行验证；实际API到位后追加完整消费合同。

## 阶段二消费卡（生效条件待负责人明确记录）

以下写集仅在主PM已交可消费HANDOFF、共享精确源码同步本美术树且负责人已核实实际编译ABI后生效；本段预先备齐合同，不以尚在审查的前置源码代替交接，也不允许自行复制共享文件或执行Git。

唯一实现者继续为 `reactor_ct_audit`。生效后仅新增前述三个 `boiler/client/BoilerConnected*.java` 与两个同包 `src/test/java/.../BoilerConnected*Test.java`；窗口JSON与21PNG复用本批已完成资源，不再生成/安装。实施及验证证据写原 `ART-BOILER-05B-CONSUMER.md` 与 `build/reports/art/ART-BOILER-05B/consumer/`。共享12路径、反应堆CT/L1/L2/液体、BoilerClientEvents、注册/方块类、构建、配置、游戏逻辑及独立换热器素材/模型只读。

使用实际 `BoilerSurfaceSnapshots.capture(context)`、`BoilerSurfaceSnapshot.findSurface(pos,face)` 与Member访问器；边界为 `min()/maxInclusive()`、隔层为绝对 `partitionY()`。一次gather捕获一次快照，模型构建所有面/邻接及共享面mask复用，异常/嵌套finally恢复原作用域；getQuads不得查询世界或全局最新快照。双方dimension/owner/generation/revision/min/max/partition均相同，当前两格局部ID与expectedBlockId相符，且各自属于同一朝外平面才可连接。未知/Ponder/撤销/局部冲突安全回单块；物品不登记包装。

9表面角色作为同炉邻接证据；只包装8个锅炉方块注册ID：high_pressure_boiler_casing/window/controller/water_port/steam_port/hot_coolant_port/cold_coolant_port，以及boiler_safety_valve。`nuclear_heat_exchanger`为独立设备，仅作为邻接证据，不能包装或改其素材；`boiler_heat_exchange_section`是内部再热段，亦不包装为外表面。登记在独立Dist.CLIENT/MOD的RegisterGeometryLoaders内同步、幂等执行，不enqueueWork、不提前取DeferredBlock实例，不改其他事件订阅。

shift映射按已安装mapping固定：原casing_side→side及隔层强带partition_side、原casing_top→top、原window_frame→window_frame、原四口/控制器面→各自目标。隔层强带只在可靠成员实际Y等于partitionY的水平壳面使用；未映射原sprite保持其quad，保留接口图标、facing、安全阀四段几何/标牌与其他材质。八张图集用原生RECTANGLE，窗口用原生OMNIDIRECTIONAL；不能重写Create索引或以整机尺寸选图。

共享窗面仅隐藏同炉/可靠共同外平面、两格均窗口的切向接口。六面纹理框的共享面mask写独立ModelProperty；getQuads的指定side路径可直接空，side=null混合列表按真实quad方向复制过滤，未知mask为0，不修改super共享列表或误删整窗外表面。仍保留已有水位BER/碰撞。

先用必要行为断言核实归属/局部冲突、隔层实际Y、2×2/L的原生连接及共享窗面、指定side/null过滤、一次捕获与异常/嵌套恢复、8注册ID/9图集及幂等入口；不镜像源码字符串/全部原生表或重测共享生命周期。最终仅一轮 `./gradlew.bat test --tests '*BoilerConnectedTextureBehaviourTest' --tests '*BoilerConnectedTexturesTest' assemble --console=plain`，不clean/rerun/full build/游戏。若实际失败或实现继续改动，仅针对受影响范围复验，并保留原始失败性质；缺类编译失败不能当行为RED。

交原始退出码/日志/XML、准确断言数、源/class/窗口模型/21资源与唯一JAR身份绑定及窄改动清单，不造全仓保护矩阵。前置29项及真实探针/窄审复用主PM原证据。完成后停写，负责人安排一次整批独立规格/质量窄审，不重复测试/构建；最终视觉由用户在现有存档确认。

为减少串行等待，原独立审查者 `reactor_asset_review` 可先只读预读05B素材源/映射/现有证据及窗口模型，回复具体合同问题，不运行生成/测试/构建、不改生产或工具、不写提前完成结论。待消费者与最终制品冻结后接续同一轮审查，才形成唯一整批报告 `docs/art/reports/ART-BOILER-05B-REVIEW.md`；未改的已读资产复用，不重新逐项检查。此安排按requesting-code-review执行、按治理5.1合并规格/质量，不能由预读关闭客户端或整批验收。

### 阶段二正式生效（2026-10-11）

主PM已发布实际[HANDOFF](../../reviews/2026-10-11/boiler-surface-display-01/HANDOFF.md)，main净整合 `eb3ec43`，美术源码净同步 `53d699b`。负责人已在主目录完整读取最终HANDOFF、实际实施/独立审查及逻辑树Java21编译ABI；并以冻结manifest一次只读核本美术树12/12共享源SHA一致，窗口JSON原SHA保持。实际capture/findSurface/Member签名与阶段二卡一致；原29/29、唯一1/1真实探针及窄审原证据复用，不重跑。正式HANDOFF明确准许本卡完全相同的三个客户端类、两个测试、8模型ID及9shift，阶段二现生效并派原消费者实施。

独立素材/窗口预读无必改项；Minor README旧进度已由原作者仅改文案并负责人读取，21PNG/SVG/模型/工具继续保持。同一审查在消费者/最终制品冻结后接续，不提前宣称整批或游戏视觉通过。

### 内部交付完成（2026-10-11）

原消费者完成三独立客户端类及两专属测试，21PNG/窗口模型沿批准资源保持；按实际共享成员与原生Create上下文连接8锅炉模型、9图集，并以本次ModelData消费隔层选择/共享窗面。唯一最终定向test＋assemble为10/10、26秒exit0，旧业务/共享29项与真实探针未重跑。[同轮独立规格/质量审查](../reports/ART-BOILER-05B-REVIEW.md)通过，唯一注释Minor已闭、必改项无；两作者与审查者均停写。

负责人已读原日志/XML与完整报告，核5源/当前manifest及live/frozen JAR一致，8class/22资源的JAR entry绑定复用同轮审查。JAR为2,528,938字节、SHA256 `d9745639811f57b9b5f164f46c943f6335339c83e62903ea3b40114622818a51`，当前manifest SHA256 `469e5ae0c19cbafba295a78c9e6a88b157f8bc98cea4fe4dbb7d234105ce2274`。审查后仅一行注释、行数与运行时代码保持，旧源/清单已保存，未重测或重建。

[候选与实际客户端复看](../ART-BOILER-05B-CANDIDATE.md)已交回，用户视觉门保持未通过。本批仍在美术树未提交/未合main，未启动Minecraft或更改存档，Git交PM/用户；不自动推进其他设备。
