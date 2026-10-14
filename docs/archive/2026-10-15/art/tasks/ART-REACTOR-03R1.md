# ART-REACTOR-03R1：管束厚重感、控制棒细节与冷却液可见性整改

**状态：整改内部交付完成；用户视觉未通过。** 2026-10-10用户三张客户端截图指出燃料/控制棒过细、控制棒黑成一团、冷却液未正常显示。原03自动检查及独立审查是历史候选证据，不能代替本次整改或追认视觉通过。美术负责人主持，原执行者`reactor_ct_assets`实施，`reactor_ct_audit`只读根因核查，`reactor_asset_review`一次组合窄审规格/内部质量通过、必改项无。

## 目标与不变量

保留原03真实驱动合同：燃料列HU/t蓝辉、权威实际深度的完整控制棒升降及顶部露出、真实冷热库存的连续混合液位。仅整改几何/材质/客户端绘制，稳定ID、九管结构、可变几何消费、碰撞、服务端库存/热工/同步、其他设备与02R1外壳纹理不变。

- 燃料管八边形外径从2.5增至3.5模型单位，中心仍为X/Z=4、8、12；九管相互留隙。两块方形钢板厚度从1增至2：底Y0..2、顶Y14..16，管Y2..14。辉光逐管同轴半径仍只偏置0.0005方块，不能整体XZ缩放。
- 控制棒杆身外径从3增至5单位，保持八边截面、单位长度及轴线X/Z=8；固定端箍外径6、厚3单位。加可辨钢灰棱面、纵槽、浅色边沿和分段箍纹，黄铜仅端箍。棒体完整升降，固定端部不随行程拉伸，包围盒同步覆盖3/16端部。
- 明暗通过正确世界采光与SVG材质解决；不把整个控制棒设为蓝辉或全亮，不改变世界light。冷却液需要能在实际库存非零时透窗辨认纹样、液位和连续冷热混色，同时保留棒体细节。

## 根因核查边界

实际源码已确认杆体与冷却液复用宿主驱动/仪表BE的`light`；两者是完整不透明外壳，采光取点与实际绘制位置不一致。最终修法先核对锁定Minecraft/Catnip源码及审计证据，不从截图猜库存为0或直接以增大alpha掩盖故障。

只读核查显示最新修改存档的唯一仪表保存有8748mB冷液、126mB热液；这是磁盘历史而非截图实时值。两张正式sheet均16×128、alpha255，首次reload注册在initial reload之前，六面绕序向外；这些证据缩小排查范围，仍需核实采光、实际shader、UV与动态纹理上传。审计完成前先实施已确认的模型/SVG段，不提前改Java。若最终发现需要共享L2或服务端写集，暂停该片段由负责人协调主PM。

## 权限、技能与精确写集

唯一工作区`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，每条shell显式workdir；当前只读HEAD为`a0cf5f3e98f51f9ddff709d532a057cafda3b786`。实际读取AGENTS、美术入口、治理1.2/5.1/5.2、本卡和03设计。应用`C:/Users/IKSXH/.codex/skills/`内minecraft-modding、minecraft-testing、minecraft-resource-pack及已读systematic-debugging、TDD、验证技能；锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。中文手写注释说明坐标、单位、光照与生命周期。

1. 工具源码仅`tools/art-assets/reactor-animation/rig.json`、`palette.json`、`generate.py`、`README.md`及`sources/{fuel_rod_steel,control_rod}.svg`；保留另外17SVG原字节。允许重新导出该目录原有33个generated路径，不新增运行材质ID或直接画PNG。
2. 正式资源仅原03B列出的19路径；其中允许更新燃料/辉光/控制棒模型和两张钢/控制材质，未需变的路径保持原字节。原共享导出器`tools/art-assets/export.py`只读。
3. Java仅原03B六类：`src/main/java/com/iksxh/create_nuclear_industry/structure/client/ReactorAnimation{ClientEvents,Models,VisualState,Materials}.java`、`ReactorInternalRenderer.java`、`ReactorControlRodRenderer.java`。只改审计证实必要项；独立测试仅同包`ReactorAnimationVisualStateTest.java`、`ReactorAnimationMaterialsTest.java`。
4. 执行报告唯一`docs/art/reports/ART-REACTOR-03R1.md`；新的探针/预览/日志/XML/候选/冻结清单只写`build/reports/art/ART-REACTOR-03R1/`。审计证据只写其`audit/`子目录。

无Git写、主工程写、其他docs写、配置/依赖/构建脚本写、客户端/服务启动、用户存档写或子Agent。原03A/03A1/03B、02R1及ART01/02A冻结证据、JAR、报告和预览只读保留；整改前另存本批允许路径的原字节以核对变更边界。

## 顺序与必要验证

1. 保存基线，按冻结尺寸改rig/生成器/两份SVG；从实际生成OBJ/UV/SVG做原版与加粗版对照、正侧斜视管束、控制棒材质与0/50/100%完整升降预览。原版物品parent和GUI/手持展示仍需核对，不能只看世界示意。
2. 负责人实际看预览；审计返回可证实根因后在本卡追加精确Java修法，再交同一执行者实施。已有授权不重复向用户索取确认。
3. 仅对本次非显然渲染算法新增少量真实行为red/green；保留原16测试，验证采光位置/变换或液体提交中实际受改边界，不能用源码字符串或编译失败当red。资源几何用确定性生成、独立OBJ尺寸/法线/同轴/管间距检查，无镜像实现的无效JUnit。
4. 同一持有者唯一最终定向`gradlew.bat test --tests '*ReactorAnimation*Test' jar`；不跑GameTest、全量构建、旧功能/旧存档专项。记录命令、真实退出、XML及JAR hash。
5. 核源→generated→正式安装→JAR条目一致；冷/热两张sheet及其他17SVG不动，原共享L2 R2十二路径及02R1资源不夹带改动。冻结本批交付/证据清单。
6. 负责人核证据与预览，一次独立规格/质量窄审后交新的客户端候选。视觉门集中复看这三项：九管/棒体厚度、白天拔出杆与堆内杆细节、实际非零库存液体透窗/液位/混色/纹样。原03暂停/失效等未改自动证据可复用，客户端视觉最终仍由用户确认。

## 当前进度

- [x] 用户定向反馈与截图已登记，原03视觉保持未通过。
- [x] 已核源码几何及宿主light复用，审计继续收敛根因。
- [x] 模型/SVG与对照预览。负责人实际查看geometry、三模块三视、control、material、item五图；厚度/钢灰纵槽/分段箍纹方向通过，准许安装本批资源。离线OBJ/UV投影不是客户端采光结果。
- [x] 根因与精确客户端修法补充，见卡尾与只读audit；不增加展示补光或修改透明度。
- [x] 必要定向检查、增量候选、来源绑定与冻结。18/18、最终test/jar19秒exit0；19来源绑定一致，192/192交付/证据/依赖SHA负责人已实核，历史及共享保护保持。
- [x] 一次独立窄审与候选交付。负责人已读[独立报告](../reports/ART-REACTOR-03R1-REVIEW.md)，无必改；[候选及三项复看](../ART-REACTOR-03R1-CANDIDATE.md)已完成。
- [ ] 用户客户端三项复看。

## 审计后的精确客户端补充（本卡生效范围）

负责人已实际读取`build/reports/art/ART-REACTOR-03R1/audit/saved-chunk-and-pixels.txt`、锁定MC源码与Catnip字节码。九个合法控制空气格SKY11..13，与宿主0形成明确差异；保存库存8748+126、容量9000、液位.986仅为历史佐证。初次reload、sheet尺寸/alpha、六面绕序及默认重复采样未见该故障。不增加最低亮度、全亮液体或更大alpha，先纠正实际取光点。

1. 控制棒用已核`SuperByteBuffer.useLevelLight(BlockAndTintGetter, Matrix4f)`。该实现只将略内缩的原模型顶点乘SBB自身变换、再乘传入光照矩阵，不会自动使用BER外部Pose。shaft实际bottom平移及travel缩放、head实际top平移应放到各自SBB变换链；BER外部Pose保持原始基准，世界采光矩阵为cap坐标平移。禁止相机矩阵用于采光、重复cap/bottom/travel变换、目标深度驱动画面或整杆FULL_BRIGHT。实现者按实际锁定API组织一个生产共享变换入口，测试调用该入口；法线按原生变换保留。
2. 冷却液每个并集Face按其合法空气`Face.cell`调用原生`LevelRenderer.getLightColor(level, cellPos)`，提交该面顶点时用该值，不能继承instrument的light或把边界floor进相邻不透明壳体/燃料。原透明alpha、RGBA、UV、冷热混色、库存筛选与并集拓扑保持。
3. 在原有两个专属测试内新增必要行为：记录实际生产液面提交的VertexConsumer（同一生产取光/提交入口），给宿主0和合法空气12，确认实际顶点用空气12且alpha/UV/法线仍合约；控制棒生产变换入口核0/50/100%实际位姿、shaft/head世界采样坐标及无重复变换。编译或符号缺失不能当red；只做足以覆盖实际缺陷的少量测试。
4. 原材质prepare缺资源/尺寸错误静默返回null：允许在此catch每次失败reload记录一次包含资源/异常原因的诊断，坏资源仍撤旧纹理，无render逐帧刷日志；不为此新增自制UI或网络字段。若不需改其他客户端类应保持其字节。
5. 光照取点不强制加载区块；避免把全局float位置当当前Pose相机坐标。若常规Catnip世界平移存在极大世界坐标精度局限，明确记录实际限制，不扩展成全局渲染框架或无关大坐标专项。

本补充是对用户已授权反馈的最小客户端整改，六类写集不变，可交同一执行者接着实施；没有共享逻辑缺口，不等待额外PM授权。
