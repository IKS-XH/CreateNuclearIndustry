# ART-SHIELDED-ASSEMBLY-15A 屏蔽装配室三套设计稿

2026-10-14用户在离心机外观手测通过后指定下一台屏蔽装配室，负责人提出保留2×2×2机身、厚铅灰板/铸钢骨架/宽观察窗及双机械臂和中央夹具，先三套SVG与真实模型设计稿、选定后正式资源与真实工序动画。用户明确“开始吧”，批准本轮设计稿及实际模型预览，未选稿/安装或授权游戏逻辑改造。

## 角色、基线与保护

负责人只维护docs/art和实际浏览；按治理1.2由具名资产/预览执行者实现，一位独立审查者组合窄审。复用 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio` / `codex/art-studio`、HEAD88b8fd289d49df90d7e3c655b6e81381ad01130f。已安装14B离心机、7d62147c…候选/旧d12备份、14A/14B源/网页/证据、logs/debug.log与logs/latest.log及旧__pycache__保持；main/其它树、Git、世界/配置/JAR只读，无新树/搬档/清理。

读用户最新AGENTS、治理1.2/5.1/5.2、本卡和minecraft-resource-pack；版本锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。用户已批准短设计，brainstorming沿用此授权；subagent-driven-development/verification/单次审查受本批写集与精简验证边界约束，不提交、重建包或做全量测试。手写代码/非显然几何说明中文。

现行名“屏蔽装配台”，注册shielded_assembly_station和shielded_assembly_part。北向part=x+2*z+4*y，x/y/z各0/1，八格占全局模型单位0..32；每个正式part自己的局部坐标加16×格偏移恢复整机。朝向绕主控格x=z=8旋转，不是绕整机x=z=16。左右臂和fixture已经用整机坐标，不能再加格偏移。

底部半轴由原Create SHAFT_HALF引用，位于主控下方x=z=8；**不能为了居中美观移到整机中心16**。原Renderer只在current/expanded时绘制底轴和三件待机机构，未按工序驱动；WORKING是现有blockstate的灯状态。当前制造/封装配方真实work25600/12800并经ShieldedAssembly NBT同步，但本批只设计静态形体，不制作假加工循环/假装料/新状态接口。

材料按实际机械动力合成配方：铅板、钢板、两个Create机械手、辐射传感器；铅与钢为主，少量黄铜只作为机械手关节/锁圈功能件。保留现有接口和主控/朝向/碰撞/八格规则，不让新图形暗示独立GUI、未知指定口或新加工行为。

## 三套方向

- A 厚壁屏蔽热室（主推荐）：中明度铅灰大面板与深钢承力框，厚退台窗框/宽横向观察区，层叠顶肩和重型底座。强调封闭屏蔽舱、整体少接缝与内部双臂可辨。
- B 铸钢装配工作站：铸钢立柱与清晰上横梁，左右分开的观察区/中央加强构件，较有机床结构感；下座与侧筋更厚，保持封闭屏蔽室，避免大面积近黑或遍布细碎铆点。
- C 模块检修舱：浅暖铅灰分段护板、四角钢梁与凹检修层次，窗与板有明确模块分区；比A更规整、比B更平缓，不能只给同一形体换色。

三稿同一尺寸/照明/相机，形体、窗口、顶肩/底座/板分区须可辨。已有三partial仅3/3/3盒体，允许细化为有真实关节/杆件/夹爪/中央台层次的候选，但保留原pivot/待机位置及rig限定走廊，不捏造新运动机构。夹具与双臂不重叠、可从观察窗看懂，不能画成黑块或遮满窗口。

## 资产执行者写集与契约

仅新 `tools/art-assets/shielded-assembly-concepts-15/assets/`、make-assets.py，`build/reports/art/ART-SHIELDED-ASSEMBLY-15A/assets/`及 `docs/art/reports/ART-SHIELDED-ASSEMBLY-15A-ASSETS.md`。旧fuel_02e_assets.py和sources/fuel-02e/全只读，不调用旧批量导出/安装器；不生成正式PNG或写src。

三稿每稿候选whole shell JSON与left_arm/right_arm/fixture三partial JSON，统一北向整机模型坐标0..32，以实际箱体/合法旋转元素及显式0..16 UV绘制。shell是八格外壳的概念整机组合；正式按八格拆分在选稿实施阶段处理，本批不谎称已经安装blockstate。原八个off正式part按格偏移合成可作为只读基线，on差异仅真实灯素材对照。

材质为16² SVG、crispEdges/有限色板/无滤镜；按真实面跨度合理取样，不把整张钢板纹理压进螺栓或拉伸窗口。window保留可见内部和玻璃线索，可用透明底/稀疏不透明反光与染色窄缘来兼容原cutout思路，不以普通钢壳透明/全亮或大面积发光提高可视度。机械加工钢/紧固件需要独立槽时与预览作者协商并记录，不能只更新缩略。

契约assets/concepts.json schemaVersion1，baseline包括实际八part/三partial/rig/配方等来源与SHA/接口/坐标规则；concepts a/b/c各含name/description/palette、models {shell,left_arm,right_arm,fixture}相对路径、textures槽到SVG映射、各候选真实枢轴/包络说明。对齐原rig的pivot/行程方向/范围；既有原生轴作为第三方只读参考由预览作者处理，不能在候选中画一只静态方块充当输入轴。

有限检查只核本批SVG/模型/引用/UV/几何包络与枢轴、窗/静臂/夹具可见性、当前来源身份及旧输入保护。复用14B原通过，不测旧设备、Minecraft/Gradle/JUnit/GameTest或打包。

## 预览执行者写集

仅新工具index.html、preview/、server.mjs、README.md，`build/reports/art/ART-SHIELDED-ASSEMBLY-15A/preview/`及 `docs/art/reports/ART-SHIELDED-ASSEMBLY-15A-PREVIEW.md`。与资产作者直接协商契约；每稿真实读取四JSON和各槽SVG，SVG只在浏览器内存转纹理，不写磁盘PNG，不手画替代3D。

优先同级block-model-renderer2.20.0与现成Three0.180.0，不安装/升级/改第三方目录。概念模型以1/16换算恢复整机0..2三轴，partial坐标不二次偏移；相机可围整机中心观察，但不能把动力轴/朝向pivot挪位。底部轴如需展示，**只读实际Create JAR的SHAFT_HALF和依赖纹理/父模型**，按原partialFacing DOWN定位；白名单素材可抽取到本工具preview/refs但须记录JAR/ID/hash/来源为Create，不作为CNI原创或正式安装资源。找不到准确资源则报负责人最小缺口，不擅造方块轴。

提供统一斜视/正背/俯底、机壳剖视或隐藏查看机构、静态状态灯off/on贴图对照与重置；三稿及窗口/内机构同照明，清晰显示候选。不要加入连续假加工动画；后续工序动画须以正式只读状态另定写集。必要只读原机对照使用真实八格/三partial/PNG，不硬编码成六面立方体。页面控制围绕视觉选择，少露调试实现字段。

服务优先空闲4193，仅127.0.0.1、固定本页/候选/实际参考/库白名单；capture仅本批固定三图/JSON路径，无任意读写。Windows后台Hidden，报告自有PID/executable/cwd，不动4191/PID84900、4192/PID85932或其它用户服务。有限HTTP/身份/语法检查只本批必要输入，负责人通过CUA实看并保存三稿/剖视/底轴等真实截图；浏览器不可用报告状态由负责人接续，不改外部自动化方案。

## 审查与交回

独立审查者仅 `docs/art/reports/ART-SHIELDED-ASSEMBLY-15A-REVIEW.md`：一次组合规格/质量窄审，读原检查/输入身份和实际截图，核三稿差异/材质来源、2×2×2与真实枢轴/主控下轴、模型面/UV/窗口/内部可视度及不假动画、不写游戏/旧任务。复用已有检查，不重复生成/HTTP矩阵/设备测试。具体问题同次窄修后复核，不开新全支审查。

负责人最终维护当前候选/PLAN/README；等待三稿复看/选择，不推定A被选，不安装正式资源或进入工序动画实现。

## 执行记录

- [x] 三稿SVG/四分件JSON与实际坐标/来源契约。
- [x] 同源block-model-renderer预览与实际Create底轴参考。
- [x] 本批有限检查与负责人正常/剖视/底部真实浏览。
- [x] 单次组合窄审、选稿入口交付。

最终交[15A选稿入口](../ART-SHIELDED-ASSEMBLY-15A-CANDIDATE.md)，4193/PID18084/Tab15保留。12JSON/27SVG与41可消费来源冻结，最终有限检查0错误/395保护零变；预览68/68、52HTTP同源、capture45实际输入一致。负责人六图保存在本批GPU目录；C盘ENOSPC仅改用E盘保存，不清理用户文件。同一次组合窄审无必改，正式资源/动画未安装，用户选稿和Minecraft验收保持独立。
