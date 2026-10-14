# ART-CENTRIFUGE-14B A版正式资源与动画接入

2026-10-14用户明确“采用第A版开始实施吧”，批准14A最终R3长轴版A的正式实施。以[14A当前设计](../ART-CENTRIFUGE-14A-CANDIDATE.md)为已选方案，不重新选稿；本卡按writing-plans/subagent-driven-development划分具体写集，治理1.2/5.1与用户授权优先于通用新树、提交、全量检查或重复许可流程。

## 目标和边界

沿用 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`、`codex/art-studio`、HEAD88b8fd289d49df90d7e3c655b6e81381ad01130f。原logs/debug.log、logs/latest.log与旧__pycache__保持；main和管理员等逻辑树只读，不新树、搬世界、Git写入或发布。本批旧美术JAR为d12cca860f69f0b411a99b3c7f34bacc17895c7ac4fb7bbd149616769dc839c5，最终唯一产包前原字节备份。

MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6保持。开工读用户最新主目录AGENTS、治理1.2/5.1/5.2、本卡及minecraft-modding/resource-pack/testing。手写代码和非显然算法注释中文。

采用14A `assets/a` 的lower167/upper155/rotor23/shaft1与六槽SVG，契约SHA6634afc2c06b183d759519a351d61d2166f78b22e04662cd882d0d1b89f16ee6。真实孔、4×4×8长轴、28%增粗转子和原UV逐项保持，不重新绘制、不改相机掩盖差异。SVG确定性导出16² PNG，此为用户选稿后的资源安装授权；14A三稿和历史原字节只读保留。

两格放置、碰撞、facing、唯一下格DOWN动力口、上格UP矿浆/四侧输出、轴承检修位置/维修规则、库存/配方/磨损/供能/暂停条件保持。只改静态模型、纹理、物品模型和客户端额外输入轴partial，禁止新增游戏逻辑、状态、同步或注册身份。

## 资产执行者写集

原资产作者负责新 `tools/art-assets/centrifuge-art-14b/assets/`、`produce.py`、`assets.json`（来源/候选/安装映射与SHA契约），`build/reports/art/ART-CENTRIFUGE-14B/assets/`及 `docs/art/reports/ART-CENTRIFUGE-14B-ASSETS.md`。

正式资源仅 `src/main/resources/assets/create_nuclear_industry/` 下：

- models/block/enrichment_centrifuge.json、enrichment_centrifuge_upper.json、enrichment_centrifuge_rotor.json：原ID，A对应几何/UV不变，仅纹理路径转正式ID。
- models/block/enrichment_centrifuge_shaft.json：新增真实4×4×8竖轴partial，A shaft原样几何/UV。
- models/block/enrichment_centrifuge_item.json：同一A整机静态组合，lower原点/upper+16/rotor与shaft下格原点；包含346元素，upper局部rotation.origin也加16，原display参数保持。
- textures/block/enrichment_centrifuge_{casing,brass,glass,dark,machined,fastener}.png：六份当前A SVG确定性导出，不改变alpha/调亮；六SVG可编辑来源逐字保存至14B assets。

blockstates/enrichment_centrifuge.json和models/item/enrichment_centrifuge.json只读，现有8个facing/half映射及物品parent仍有效，不写注册/datagen/语言或旧centrifuge_01d_preview.py。安装前备份精确旧资源；候选及安装逐字/逐像素一致，材质路径不存在黑紫或设计稿虚拟namespace。

先生成独立候选并通知客户端作者，客户端新资源合同在原正式资源上取得一次实际RED后，再按其消息安装11项。候选SVG/模型输入身份、原approvedA保护、几何映射/物品upper偏移、UV、六PNG颜色/alpha/尺寸及引用有限检查；复用14A实际360度间隙和已审几何，不重跑三稿/旧历史矩阵。

## 客户端/预览执行者写集

原预览作者负责 `src/main/java/com/iksxh/create_nuclear_industry/production/CentrifugeRenderer.java`；如确有初始化需要可改同目录CentrifugeClientEvents.java，仅客户端partial加载/中文职责说明。不得触碰其它Java、注册/服务端或base Create机制。

新增SHAFT PartialModel，使用与现有ROTOR完全相同的angle、Direction.UP/light/cutoutMipped缓冲路径。复用原有效lower owner早退、真实signed getSpeed、非零/非超载/非BEARING_WORN判断与时间公式；不调用super重复渲染通用轴，不添加全亮/假转速/新停转语义。现有注册读取ROTOR会初始化整个Renderer类，先核Java静态初始化是否已足够载入SHAFT，不无依据扩改注册。

允许一个专属 `src/test/java/com/iksxh/create_nuclear_industry/production/CentrifugeArt14BModelTest.java`：少量有意义的A正式模型合同及实际Minecraft BlockModel反序列化，覆盖四运行模型与组合物品的真实格式/坐标/纹理路径和upper偏移；不写复制角度公式或搜索字符串冒充运行验证的测试。客户端作者独占本批Gradle：安装前该合同一次RED，安装与代码稳定后只跑该类GREEN，随后唯一必要增量assemble。无clean/rerun/build全量、旧业务/旧兼容/GameTest或自动启动Minecraft；新增业务状态/生命周期疑点才扩范围并报告。

正式预览新建本工具index.html、preview/、server.mjs、README.md；只读14B已安装四模型/物品及六真实PNG，固定本地block-model-renderer2.20.0/现有Three。支持原正背/俯底/剖视/停止低速/重置，真实A与物品视图；不重新绘制或加载SVG冒充已导出PNG。server仅127.0.0.1固定资源/库白名单，capture只写14B preview报告。优先空闲4192，自有PID报告；Windows后台Hidden，不停原4191/8735或用户进程。

只写 `build/reports/art/ART-CENTRIFUGE-14B/client/`、`preview/`与原美术树build输出，及 `docs/art/reports/ART-CENTRIFUGE-14B-CLIENT.md`。所有网页/缓存/生成物不得进正式JAR；产包前备份旧d12包，最终JAR与11资源、变更客户端class及实际输入身份绑定。作者准备当前真正PNG/模型同源HTTP及语法证据，负责人通过CUA实看后同次组合窄审，无必改再产唯一包。

## 单次组合审查与交付

原独立审查者仅 `docs/art/reports/ART-CENTRIFUGE-14B-REVIEW.md`：读原source/定向结果/同源来源与负责人真实PNG预览，核双partial共用真实角度和有效owner/客户端初始化、物品+16/UV/引用、旧玩法/14A保护、写集及制品身份。不重新测试/生成/打包；同次具体整改仅窄复核。

负责人只写docs/art任务/PLAN/README/14B-CANDIDATE，实际浏览与截图，不编写资源或客户端功能。本批手测清单合并为正常上下两格与物品、四朝向检修/接口、窗内加粗转子、底部轴与内转子正/反转、停止/超载/轴承磨损保持原表现及重载；自动证据和网页姿态不代替Minecraft视觉/动画验收。交付停在美术候选待用户手测，main集成/Git及其它设备不自动推进。

## 执行记录

2026-10-14用户明确“新的离心机外观手动测试通过了”，按[外观验收反馈](../ART-CENTRIFUGE-14B-FEEDBACK.md)关闭明确的整体外观门；不补写未逐项报告的动画/操作数据，不重复原检查/构建，不Git/main同步。用户指定下一设备为屏蔽装配室，当前只进入方案准备。

- [x] 原A source/11旧资源/旧包保护与候选生成。
- [x] 专属实际模型合同原资源RED，精确安装11资源。
- [x] 客户端双partial与真实PNG预览，定向GREEN3/3。
- [x] 负责人实际浏览/物品观察、单次组合窄审无必改。
- [x] 唯一增量assemble/包身份，当前候选与集中手测入口。

实际交回：3/3纠错后定向通过、22/22正式PNG/模型HTTP同源，负责人真实看正常/底部/剖视/完整物品和低速同相位及capture；原单次窄审无必改。唯一assemble实测1.845秒exit0，最终2,815,098B/7d62147c…包、11资源及Renderer class同源，仅预期12载荷改变，其余1934保持，旧d12完整备份。首轮RED与float断言失败保留，非重复全量；两作者/审查者均停写，见[当前候选与集中手测](../ART-CENTRIFUGE-14B-CANDIDATE.md)。用户选A已确定，Minecraft本批视觉/动画仍待用户复看，未Git/main集成或推进其它任务。
