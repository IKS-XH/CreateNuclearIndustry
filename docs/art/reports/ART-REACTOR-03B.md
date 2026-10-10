# ART-REACTOR-03B 执行报告

已完成卡内六客户端类、两专属测试与19冻结资源消费。唯一最终定向test＋jar退出0，XML16/16、0failure/error/skip；本批候选冻结并交负责人组合独立审查。客户端透窗/运行观察及02R1视觉门仍待用户，未启动客户端或服务，未新增可变反应堆玩法。

## 前置、权限与实际绑定

唯一workdir为`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，每条shell显式指定；执行者未派代理或执行Git写。开工HEAD为ad805ec；原L2 R1的12路径实核留于baseline.json。主PM随后交付R2暂停窄修，美术源码HEAD为2bece5b09bf90951d158f45f068a81b38bf774e8，实际12/12源SHA匹配source-sha256-r2；仅Events/LifecycleTest由主PM改变，公共API全文SHA保持177de2fc074b2fe9ffde92c8af1134466f9fc3d7ff3a351b2f1acb93e68a013a。复用L2已有10/10生命周期、assemble与独立窄审，不运行其测试。

最终记录HEAD为`36ea83355fab672cf1fdc7010b3e448e6396eaa6`。与R2源码同步HEAD相比，变化仅AGENTS、文档入口、接口、HANDOFF和暂停计划，见scope-check.json；主PM文档/Git操作不属于本执行者写集，最终源码12/12仍匹配R2。实际读取生效03B卡、03设计、正式HANDOFF/接口、AGENTS/治理，以及任务相关技能。

## 消费实现

| 入口 | 实际职责 |
| --- | --- |
| ReactorAnimationClientEvents / Models | Dist.CLIENT MOD同步RegisterGeometryLoaders登记三partial，既有仪表/驱动BE注册BER，prepare/apply资源重载及独立GAME生命周期；无enqueue或新geometryloader |
| ReactorInternalRenderer | 真实ClientLevel仪表owner每次绘制一次capture；按真实已加载FULL区块核对所有列/owner注册ID；绘制同列HU/t辉光与合法冷却并集 |
| ReactorControlRodRenderer | 同次快照findControlRod/findControlOwner；核对帽、主体和owner；实际深度驱动完整棒体，shaft仅Y按行程缩放、head固定2/16、AABB含上方露出段 |
| VisualState | HU/t透明度、溢出安全库存比例、真实Y层面积体积分配、无共有内面的网格、actual最多4显示tick插值、冻结暂停partial、数值身份/几何缓存 |
| Materials | 两套SVG导出8帧ABGR先时间插值再按hot/total混色；256固定ID槽与槽内像素缓冲；提交前更新、安全边界回收、release关闭原生像素 |

辉光只在可靠可用燃料且H>0时绘制，耗尽tick即使功率非零也熄辉；管身FULL_BRIGHT/disableDiffuse，逐管0.0005偏置已在mesh，不做XZ放大。控制棒target不参与位姿，卡死/未知/身份变化/超过4tick失见后直接定位；Post同次快照按自有owner/cap键撤掉未知缓存，恢复不续用旧缓动。

冷却空间只消费descriptor.coolantSpace()，EMPTY与CONTROL_ROD主体有液、燃料和帽无液。体积按Y层实际面积自下向上分配；相邻共有面剔除，不同高度仅保留暴露条带。世界int先减base再转float，UV用连续小局部整数重复，避免远坐标丢格；alpha .22～.32，水平面.28，NEW_ENTITY显式RGBA、NO_OVERLAY、pose/normal和light。稳定身份不含sample/revision；几何/填充比例不变沿用网格，混色写槽自己的256像素缓冲。

纹理draw不驱逐别堆，超过256只撤液。20显示tick未绘制在安全tick回收；未知归属即不画，GPU释放在安全边界，重载/会话切换先endBatch再release；prepare关闭InputStream/NativeImage并复制帧数据，坏帧表撤下旧液体。缓存不保存BE/NBT/强Level；异世界/Ponder BER不碰当前世界数值缓存。暂停冻结消费者tick+partial，L2租约冻结由已交R2负责，不旁路capture。

## 真实red、green与最终验证

| 命令/阶段 | 实际退出与XML |
| --- | --- |
| 首次red：`test --tests '*ReactorAnimation*Test'` | exit1，12个实际AssertionFailedError；consumer-red.log、red/两XML与两无行为编译占位源码。占位只解决不存在核心的编译，未把编译失败当red |
| 首次green：同一定向test，无jar | exit0，12/12、0failure/error/skip；consumer-green.log、green/两XML |
| 最终唯一`test --tests '*ReactorAnimation*Test' jar` | exit0，16/16，其中VisualState 9、Materials 7；0failure/error/skip；final-test-jar.log/exit.txt、final/两XML |
| 制品/范围核对 | exit0；verification.log/json、scope-check.json；源安装JAR映射、旧资源与冻结资产哈希均符合 |

最后4项补充真实列target≠actual与未知恢复、稳定网格复用、生产writer缓冲复用/暂停失效回收、256槽与会话隔离；槽测试均使用生产Consumer writer入口，无仅测试调用的生产upload重载。其他用例覆盖功率零/单调/耗尽、4tick/卡死、库存防溢出/0容量、多Y面积和去内面、ABGR端点/连续帧混色、回收/重载释放。没有源码字符串断言或测试镜像算法。

最终构建保留EventBusSubscriber bus/Bus两条既有锁定API弃用警告及Gradle未来版本警告，均不影响exit0；没有升级技术栈或扩大测试。没有全量套件、GameTest、L2/CT旧测试、素材再生成、944矩阵或存档检查。

## 资源、制品与保护

候选副本：`build/reports/art/ART-REACTOR-03B/candidate/create_nuclear_industry-0.1.0-ART-REACTOR-03B.jar`，**2,473,054 bytes**，SHA256 **`7ef99a946c3be88c5b3d6f33fbc49181024c70a4360ce365624c8039e35d441f`**。

`resource-source-installed-jar.json`逐项记录19路径：17JSON/OBJ/MTL/PNG逐字节冻结源→安装→JAR一致，含03A1静态包装parent；两mcmeta与冻结mapping动画元数据精确语义一致且安装→JAR字节一致。仅安装两份16×128sheet，未安装16单帧图块，未改全局流体。

开工旧02R1 JAR独立保存于baseline/previous-art-candidate.jar，SHA ca591c0d64b6d184930bd42ba65a2c1647ab9ac8decdf9206ed94f788f80d533。旧1038项assets中1037最终JAR字节不变，唯一允许变化为models/block/reactor_fuel_rod.json；原item包装逐字节保留。旧CT两类＋专属测试三源SHA与02R1冻结一致。03A/03A1合并后59交付源/报告哈希全部保持：原03A的两项已批准漂移按03A1冻结覆盖，旧证据链不重写；此为只读哈希保护，不重跑旧素材验证。

写集精确27路径（6源＋2测试＋19资源），另有本报告与03B证据目录。8份手写Java无行尾空白。logs/debug.log、logs/latest.log开工即已处于Git修改状态，本轮JUnit继续使用该路径；未手改、修格式、回退或清理。最终日志哈希单独记录verification.json，不把累计Git日志差异算成本批手写。其他角色同步的AGENTS/docs与共享R2代码另归主PM，既有ART01/CT候选保留。

安装预检先遇控制台实际UnicodeDecodeError（默认GBK读取UTF8），随后路径KeyError（Windows分隔符及初始mesh记录）；都发生于复制前。规范UTF8/实际generated/models/mesh后install.log exit0，以install-mapping.json为最终安装来源。失败不是行为red，记录见command-ledger.md与install-path-first.log。

## 冻结与后续观察

冻结入口：`build/reports/art/ART-REACTOR-03B/frozen-manifest.json`；列出27路径＋本报告及本批证据哈希，原始red/green/final日志、XML、旧/新JAR副本均保留。负责人接续一次组合独立规格/质量审查，不重复生成或构建；本执行者未改变任务状态或验收文档。

人工门保留：运行/停热与耗尽蓝辉、昼夜、完整实际深度/卡死/拔出、真实液位和冷热连续混色、透窗排序、真正暂停/恢复、重载及拆坏恢复。原生透明批次不能保证跨窗/跨堆全局排序；数值/CPU/打包验证不能替代客户端视觉观察，02R1独立门继续待验。

实际应用minecraft-modding/testing/resource-pack、TDD、executing-plans与verification-before-completion；以卡内MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6实际API为准，遵守用户治理的定向验证及执行者权限。
