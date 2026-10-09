# ART-REACTOR-03-L2 实际接口与美术消费交付

2026-10-10，主逻辑PM。**L2自动验证、独立组合审查及唯一P2的R1窄复审通过；已审净源码已实际同步main及美术树，美术负责人可在本页写集内派发运行消费。** 此结论不关闭03动画或02R1视觉门，不启动装配台07教学或其他工程主线。

后续R2：美术消费期间发现真正暂停仍触发客户端Pre而令租约到期。本PM已核实本地1.21.1控制流，按[真暂停租约窄修](../../../superpowers/plans/2026-10-10-reactor-runtime-display-02-pause.md)实施；旧R1证据与公开API保持，消费者可继续独立实现，最终候选须绑定实际交付的R2。当前R2尚未通过审查/同步，不提前声称修复或客户端通过。

## 源码与证据

- 实现提交`ff3bacbd14307700e77eaf343bcb1bb9f6fc0489`，显示树`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-display`。
- main净整合`cb123c9805c85feb3a1ff945ff5edc60b0222b12`；美术树净前置同步`4cb0d7f998989d3f320569a5ff683eee09c49e82`，路径`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`。只集成12个实现/测试/模板和2份报告；未整体合并显示树历史差异，未暂存/覆盖美术CT、SVG、模型及素材候选。
- [实施及R1报告](./IMPLEMENTATION.md)、[独立审查及R1复审](./REVIEW.md)、[合同](../../../art/ART-REACTOR-03-INTERFACE.md)、[任务卡](../../../superpowers/plans/2026-10-10-reactor-runtime-display-02.md)。原始证据留在显示树`build/reports/art/ART-REACTOR-03-L2/`，最终入口为`R1/final-freeze-r1.json`，不能把初版SHA当最终候选。
- 原批24/24定向JUnit及1/1真实仪表正常退出；R1仅客户端接收顺序与生命周期测试两路径变化，6/6生命周期+增量assemble退出0，未变19项及真实服证据复用。首轮空燃料map缺席失误和测试服停滞记录、R1行为red均保留。
- 显示树R1冻结JAR为`build/reports/art/ART-REACTOR-03-L2/R1/create_nuclear_industry-0.1.0-R1.jar`，SHA256 `b4c56788146d672d7130a1231d0bce64f54afc91baba53a408d5f92ceaff83df`。实际`javap-api-r1.txt`公共签名与初版逐字节相同，API文本SHA256 `177de2fc074b2fe9ffde92c8af1134466f9fc3d7ff3a351b2f1acb93e68a013a`。美术树尚未为L2单独重打包，原02R1 JAR不能当本批动画制品。
- PM集成核对：三树12/12原始文件SHA与R1冻结清单一致，main及美术树已提交的14/14路径与实现提交一致。main增量`assemble`退出0，JAR中19个相关类/模板条目与R1冻结JAR逐字节一致；未重复定向测试或启动客户端。
- main本次`build/libs/create_nuclear_industry-0.1.0.jar`为2414930字节，SHA256 `18884068c1f97a216cde3922a1deefe95d61233991890a9ea216cd0cfd4b7764`，包含主树已验收教学。集成日志和核对清单留于main `build/reports/art/ART-REACTOR-03-L2/main-assemble.log`及`pm-integration-verification.json`；它仍只包含L2数据前置，不含03运行消费者或美术候选安装。

## 可消费公共API

类型包为`com.iksxh.create_nuclear_industry.structure`，捕获与索引位于`.client`。只通过已交付入口读取，不能直接借用正式snapshot或目标滑条冒充有效动画数据。

| 实物入口 | 含义 |
| :--- | :--- |
| `ReactorRuntimeSnapshots.capture(BlockAndTintGetter context)` → `ReactorRuntimeSnapshot` | 每次绘制只捕获一次，当前真实ClientLevel/原生RenderChunkRegion所属世界；未知、Ponder或旧世界为空 |
| `snapshot.findOwner(BlockPos ownerPos)` → `Optional<ReactorRuntimeDescriptor>` | 租约内可靠、完整同龄owner，空表示撤下 |
| `snapshot.findControlRod(BlockPos capPos)` → `Optional<ReactorRuntimeDescriptor.ControlRodColumn>` | 该cap棒列实际状态与行程 |
| `snapshot.findControlOwner(BlockPos capPos)` → `Optional<ReactorRuntimeDescriptor>` | 与棒列同份快照的owner/generation/geometryRevision，插值必须按其身份隔离 |
| `ReactorRuntimeSnapshot.empty()` | 无数据安全降级 |

Descriptor访问器：`dimension()/ownerPos()/ownerGeneration()`；独立L2 `revision()/sample()/serverGameTime()`；L1 `geometryRevision()`；`available()`；`origin()/maxInclusive()`（含端点，世界方块）；`columns()`；`coolantSpace()`；`coldCoolantMb()/hotCoolantMb()/coolantCapacityMb()`（long mB）；`convertedCoolantMbPerTick()`（double mB/t，已结算冷→热转换，非管网流速）。

`Column`为sealed interface，提供`capPos()/bodyPositions()/expectedCapBlockId()/expectedBodyBlockId()`。具体记录：

- `FuelColumn`另有`fuelUsable()`与`fissionHeatHuPerTick()`（double HU/t）。usable精确为组件可用且integrity>0；本次裂变HU独立保留，耗尽tick可false且正HU。合法从未装料列为false/0。消费者只在可靠且可用列映射蓝辉，不重算功率。
- `ControlRodColumn`另有`actualDepth()/targetDepth()/jammed()`；深度均[0,1]，0完全拔出、1完全插入。body数量决定行程；卡死保持实际位姿。用户已在美术对话确认完整棒体升降、上方露出，不能按深度缩短棒体或用target代替actual。
- `EmptyColumn`只有共享位置/ID访问器。`coolantSpace()`严格合并Empty和Control body，排除燃料/cap；0总量或不可用容量不绘制液体。

Common仪表也提供`runtimeDescriptor()`，用于桥接/诊断；正式BER应使用带上下文capture及租约保护，不直接保留BE Optional。构造器和全部JVM描述符见实施表与真实javap，不新增同步字段/正式setter。

## 消费不变量

收到新sample按客户端本地时间续20tick租约；原样本重复/旧样本不续。成功无变化会每5成功tick心跳；拆坏、失效、卸载、chunk替换、换世界、当前坏包/身份矛盾立即撤下。R1专门修正了当前token错dimension即时撤销，旧world/token仍忽略。消费者仍须按预期ID核对当次本地方块，矛盾时静态燃料保留、熄辉，可靠液体/活动棒体撤下。

一次绘制复用一个不可变快照；显示缓存身份至少包括world session、dimension、ownerPos、ownerGeneration和geometryRevision。session由真实世界实例体现，不从服务端样本伪造；换身份首次直接定位，不能跨堆/代次延续插值。仪表唯一绘制本堆辉光/液体，驱动器只绘自己的杆体。

仅燃料注册已经noOcclusion；碰撞/选框仍完整，默认阻光1，其他注册与L1/CT只读。已完成这个共享前置，美术不得再修改P1Blocks或重复适配。BER按真实bounds和完整升降范围给包围盒，避免仪表/驱动器不在视锥时内容消失。

## 准许消费写集与验收

只新建`src/main/java/com/iksxh/create_nuclear_industry/structure/client/`下六个专属类：`ReactorAnimationClientEvents.java`（仅既有instrument/drive BER注册、partial初始化及材质重载入口）、`ReactorAnimationModels.java`、`ReactorInternalRenderer.java`、`ReactorControlRodRenderer.java`、`ReactorAnimationVisualState.java`、`ReactorAnimationMaterials.java`，以及这些消费者的专属定向测试。需要其他具体辅助类先协调，不以目录授权覆盖共享类。

准许专用DynamicTexture有界缓存、CPU冷热/帧插值、显示深度插值、资源重载和安全释放；每堆复用16×16材质，原生entityTranslucent绘制合法空间并集外表面，不每帧分配、不增加着色器/依赖、不改世界流体或全局冷却剂图。NativeImage ABGR与顶点RGBA按实际API分开处理。资源重载、owner失效、卸载/会话切换及时释放；透明排序仍需客户端观察。

03A/03A1素材源及安装由美术负责人在消费卡列出精确映射：静态`models/block/reactor_fuel_rod.json`及其专属OBJ/MTL，`models/block/reactor_animation/`三组partial及专属mesh，`textures/block/reactor_animation/`冻结模型材质/冷热帧。只安装冻结清单，不获取其他模型/材质目录写权；原CT、共用框架、游戏玩法/注册/碰撞/逻辑同步/全局流体素材保持只读。

消费执行者只做必要定向red/green、一次增量JAR及源→安装→包引用核对；一轮独立组合审查，L2未改证据复用。最终人工观察集中为静态九管/接头、真实功率蓝辉、实际控制深度/卡死/完整升降、库存液位/冷热渐变/透窗及失效恢复。独立02R1门保持未通过，不由本接口或Ponder验收覆盖。不做旧存档迁移/全量，也不重复已通过功能和教学。
