# ART-SHIELDED-ASSEMBLY-15B C版正式外观

用户在15A三稿后明确“我更喜欢第三版”，选定C「模块检修舱」。按[反馈](../ART-SHIELDED-ASSEMBLY-15A-FEEDBACK.md)与此前选稿后实施安排，将C稿落成正式八格机壳、物品与三个待机partial，不更改已选比例/色板/窗或内部机构。此为现有资源流程的有限实施，不增加机械臂/夹具工序动作；用户选稿不冒充Minecraft验收。

## 工作区、分工与保护

复用 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio` / `codex/art-studio` / HEAD88b8fd289d49df90d7e3c655b6e81381ad01130f。美术负责人只维护docs/art及浏览，由原资产/预览两位执行者和一次原组合窄审接续。开工读主目录最新AGENTS、治理1.2/5.1/5.2、此卡与minecraft-resource-pack；按MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，不用新版本模型语法。

root美术范围授权覆盖已选模型/素材落地，所有手写注释中文。main/其它树、Git、世界/配置、游戏Java/注册/配方/同步/能力/碰撞全部只读。不新树、不搬档、不清理；原14B已验离心机、7d62147c…JAR、旧d12备份、用户日志和旧__pycache__保持。15A素材/脚本/契约/报告/图均冻结；唯一旧工具例外为预览作者15A server.mjs的原机对照来源重定向及README追加，详见下节。

## 资产执行者唯一写集与步骤

新 `tools/art-assets/shielded-assembly-art-15b/produce.py`、`assets/`、assets.json；新 `build/reports/art/ART-SHIELDED-ASSEMBLY-15B/assets/`；`docs/art/reports/ART-SHIELDED-ASSEMBLY-15B-ASSETS.md`。安装只允许以下29个生产资源：

- `src/main/resources/assets/create_nuclear_industry/models/block/shielded_assembly_station/parts/part_{0..7}_{off,on}.json`共16。
- 同models/block下 `shielded_assembly_station/partials/{left_arm,right_arm,fixture}.json`共3。
- 同models/block下 `shielded_assembly_station_item.json`一项。
- `src/main/resources/assets/create_nuclear_industry/textures/block/shielded_assembly_15c_{lead,steel,machined,fastener,brass,window,lamp_off,lamp_on,deck}.png`九项新PNG。

不替换旧PNG，不写blockstates/models/item入口/旧单格station_off/on：expanded=false仅旧世界兼容路径，本批不研究或重制旧形态。旧fuel_02e_assets.py/sources/fuel-02e与其它设备工具只读，不跑批量旧导出器。

步骤1：精确复制15A已选C四JSON/九SVG与批准契约原字节到本工具assets/source，绑定契约SHA b666789912cce985a7363e71d7dbb8c81cd294b490f86dc41ac3ab8bb79e41a2；冻结20个将替换的模型原字节到本批assets/backup，以完整仓库相对路径保存，不混同新生成来源。素材只有16² SVG原色/原alpha直接像素导出，没有AI图像/重绘或任意灯色变化。

步骤2：C shell整机0..32按 `part=x+2*z+4*y`、16格界拆成8块，每块局部0..16，裁面保UV采样、不造切片内面；on只将实际lamp面绑定lamp_on。三partial仍整机坐标，不做第二次16偏移。物品合成shell+3partial后坐标/旋转原点各缩为0..16，UV保持，保留现行item display配置；不能简化成方块图标。粒子用C铅材质。

底轴依旧原Renderer的Create SHAFT_HALF，不能画入静态shell或改变主控[8,0,8]。窗口保持稀疏cutout反光与内部可见，普通壳体不透明，功能黄铜锁圈静态，不混作工作灯。

步骤3：有限生成检查，至少校验九PNG与SVG逐像素/alpha相等，模型/槽/实际生产引用与边界/UV，八格还原整机与批准C外露几何一致、三partial/rig保持、物品归一化和当前四朝向blockstate引用保持。只检查本批受影响输入与明确旧资源/Java保护，不复制旧全设备测试矩阵或加镜像JUnit。

步骤4：候选稳定且预览作者ready后，由资产作者安装29资源；要求20旧模型与冻结备份一致，九新目标未被其它作者占用，安装后29源/生产SHA读回。本次必要几何/PNG检查由原作者持有，不重复生成。交给预览作者唯一增量assemble及原组合窄审；资产作者不跑Gradle/Minecraft或写预览文件。

## 预览/包执行者写集与步骤

仅新工具 `index.html`、`server.mjs`、`README.md`、`preview/`；新 `build/reports/art/ART-SHIELDED-ASSEMBLY-15B/preview/`、`client/`；`docs/art/reports/ART-SHIELDED-ASSEMBLY-15B-PREVIEW.md`、`-CLIENT.md`。29正式资源、资产工具/source、Java只读。

读取生产候选八off/on局部JSON与三partial、物品完整模型及实际导出九PNG，使用现成block-model-renderer2.20.0/Three0.180.0。整机按实际8格位置合成，三partial不加偏移，底轴复用15A已核Create原资源/变换。默认C整机、物品图标对照，统一正背侧顶底、剖视/隐藏、真实静态灯off/on；不假加工循环，不擅设机械臂动作。可显示实际底轴转动演示但须明确仅预览，相位不能冒充加工进度；本批优先保持静态选择免增行为验证。

预览服务优先4194、仅127.0.0.1固定白名单，capture仅本批machine/item两图和JSON。Windows后台Hidden，记录自有PID/exe/cwd，保留4191/4192。15A在4193的三稿持续可读；由于安装会替换其原机对照20模型，本作者只允许修改15A server.mjs将原机模型精确指向15B备份，旧PNG仍原路径；README仅追加本次来源变更。不改15A模型/SVG/scene/index/capture历史。若需重启4193，核原PID18084/exe/cwd身份后只重启自有服务、保localhost/路由及默认三稿，记录新PID；不能停止用户其它服务。

有限HTTP身份/JS语法只核实际本批输入；保留15A已有68/68等历史，不整套重跑。负责人CUA实看最终生产PNG整机/物品/窗内/剖视/底部/灯及capture，按实际结果审查。

安装29生产资源且同次组合审查无剩余必改后，预览执行者持有本批**唯一必要增量 `./gradlew.bat assemble`**：正式前备份现行2,815,098B/7d62147c84392f00062d19635cd0a5e91300d89860b1e8502cea17079e98f507包到client/backup；不启动游戏、跑全build/JUnit/GameTest。生成后核29资源与JAR和processResources一致，除预期29资源外与旧包载荷保持，Java未变。原组合审查者只补制品身份，不新审查或重复构建。Jar是该树客户端候选，不替换main或发布/Git。

当前C盘剩余0，E盘可写。可在本次构建子进程使用E盘本批临时目录作为TEMP/TMP/java.io.tmpdir，保留原Gradle缓存/已配置Java；只影响该进程，不改全局环境、不移动/清理缓存或用户数据。真实构建若因C盘写入失败，保留完整命令/exit/日志及原候选包，不绕成手工造JAR或冒称可运行；已完成模型/预览继续交付，具体阻塞回报负责人。

## 单次审查与交付

服务动作调整：原作者两条含Start-Process的后台启动/原服务重启命令被自动审批拒绝，完整证据保存在本批preview/service-rejections.md，均未执行，不能推断具体拒绝原因。不再重试停止/后台启动或换工具终止进程。采用范围更窄的候选路径：只允许在普通exec托管会话中以前台单条命令运行已完成的4194 server.mjs，保留session ID/完整stdout、localhost和工具控制生命周期；不再创建脱离工具管理的Windows后台进程。若该安全候选也被拒绝，停止服务动作并如实交回，不进一步规避。

旧4193保持当前运行进程，15A server源已重定向但尚未重载，不宣称运行生效。预览作者额外只允许修改15A index.html的原机对照控件/对应说明：暂禁用当前原机对照并说明旧模型已存档，三套稿与其它操作不变。负责人reload自己的15A Tab确认按钮已禁用，避免安装后误把新资源当旧原机。原机20模型/10旧PNG来源完整保留，重定向源等用户后续正常启动时生效；不因这条源说明执行重启或新增假对照。记录此处作为原卡15A原机服务处理方式的唯一替代。

原审查者仅 `docs/art/reports/ART-SHIELDED-ASSEMBLY-15B-REVIEW.md`。读取前两执行者检查/来源身份及负责人真实图，一次组合规格/质量窄审：C精确落地、八块UV/边界/朝向/物品实际完整图、底轴坐标/第三方来源、PNG/alpha、旧源与副作用边界；具体问题由原作者窄改并同次复核。禁止重跑旧设备/保护全矩阵和多轮打包。最终必要assemble后的补核只绑定制品。

负责人维护选稿/PLAN/README/候选入口与真实观察门。安装与自动结果不替代用户Minecraft手测，不补记本批未实现工序动画；跨逻辑改动和全局Git仍由主PM协调。

## 执行记录

2026-10-14局部收尾：原候选黑窗故障经15B-R1修复，用户明确“这次是对的了”，按[局部验收](../ART-SHIELDED-ASSEMBLY-15B-R1-ACCEPTANCE.md)关闭观察窗黑面及内部可见性门。当前602985f8…包保持；下方a92交付及待验描述是历史，其它未明确项目不补记通过，工序动画/main/Git不推进。

- [x] C精确生产资源/20模型备份与有限校验。
- [x] 同源正式PNG整机/物品预览，15A原机来源保留。
- [x] 安装29资源与负责人实际浏览/capture。
- [x] 单次组合窄审、唯一必要增量包及制品补核或真实阻塞。
- [x] 候选与本批手测入口交付。

最终唯一assemble10.619秒exit0，原同次资源/GPU审查及制品补核无剩余必改；包2,812,280B/SHA a92a0bed0f948fc000b58800cad34f84c03ffb314b167fea3a839f117b12ff7f。29资源同源且仅20JSON替换/9PNG新增、0removed，557class保持、1926非本批旧载荷保持；7d备份完整。实际构建前C盘已16.18GB，不是本批清理，仍只E进程临时目录。两作者与原审查者已停写，最终[15B候选](../ART-SHIELDED-ASSEMBLY-15B-CANDIDATE.md)和4194/Tab17保留，停本批用户Minecraft外观门，不推进工序动作或main/Git。

已实际生成/安装29资源，107shell八块还原及193完整物品、九PNG像素/alpha核对通过，29安装SHA读回/106保护零变。79/79与42HTTP为首轮；物品默认相机看背底经真实实看定位后，只改预览相机+Z并补受影响语法/HTTP，生产display及29资源保持。最终Tab17实看normal/front-on/cutaway/bottom，capture35输入production/default/off/static已保存，GPU目录保首轮失误与修后图。4194/PID16460/session53337保留；15A原机控件实读disabled/存档，旧4193未重启。原7d包备份完整，尚未assemble；一次原组合审查已进入，后续包及制品门不提前登记。
