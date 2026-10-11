# 锅炉汽容量只读消费接口01

**任务：** `ART-BOILER-06-CAPACITY-01`。**状态：** 用户新蒸汽视觉方案已核，必要共享读数派发中；美术06容量消费等待实际ABI。主PM管理计划/Git，执行者实现单一getter，另一个执行者窄审。

原始授权在已任命美术对话`01a11c6b-f440-77c1-9275-5b2dca5cfa0f`，用户轮次`01a12b48-3930-73c3-98ae-61991fe4ceeb`要求上部汽段透窗雾效及重做两种蒸汽贴图/流动动画；用户消息`01a12b4d-0eac-78a3-b1b2-022a9588f2a8`批准乳白灰/青白、真实库存浓度与连续混色、SVG同源循环。主PM实际读原消息，不以负责人转述替代授权。

## 必要范围

现有`BoilerControllerBlockEntity.write/read(clientPacket)`已传`SteamCapacity`，客户端存私有`steamCapacityView`；两种mB在原BoilerState同步，L1闭bounds/partition可靠门已交。客户端账本Settings可能仍为DEFAULT，不能用`ledger().steamCapacity()`代替远端实际汽容量。只公开该已有读数，不增加协议、字段、首包flag、默认值改动或扫描。合同见[容量消费接口](../../art/ART-BOILER-06-CAPACITY-INTERFACE.md)。

实际基线：main `536ecb4`、复用逻辑树`E:/MyMC/NewMod/Create_NuclearIndustry-art-integration`的`dcfbb47`、美术树`9878fd8`；三树controller源为`cbc83e98cd8bca3b6d60f71e5e9348d7e1c9a48fe27818127c4a20b236622b21`。同步本计划后实际HEAD在派发消息记录。本次不新建分支，沿用已携带且完整备份27文件存档的逻辑树，不再复制或运行用户世界。日志/cache及美术05B未提交候选保持，06资产作者独立工作。

## 精确写集与技能

- 仅修改`src/main/java/com/iksxh/create_nuclear_industry/boiler/BoilerControllerBlockEntity.java`：新增`public int syncedSteamCapacityMb()`，纯返回`steamCapacityView`，中文Javadoc明确客户端显示、mB、首包前值不能当同步证明、L1门控及服务端权威边界。
- 原write/read、字段初值、ledger、L1、窗口Fill、BER包围盒、BoilerState/Config、注册/热工、测试与构建都只读。不增加便利record或其他getter。真正需要扩大写集先报告。
- 执行报告仅`docs/reviews/2026-10-11/boiler-steam-capacity-view-01/IMPLEMENTATION.md`；独立审查仅同目录`REVIEW.md`，证据仅逻辑树`build/reports/art/ART-BOILER-06-CAPACITY-01/`。禁止执行者Git写、核心文档或派发他人。
- 必读AGENTS、治理5.1/5.2、上述合同/本卡、原L1 HANDOFF；实际读取`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`与`minecraft-testing/SKILL.md`，按已锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6使用。PM已沿用此前读取的这些技能及计划/派发/审查技能，不升级栈。

## 验证与交付

本改动是可逆的单字段getter，无独立运行行为；不写镜像getter测试或重复L1矩阵。复用原29/29定向、1/1真实同步探针和窄审。仅一次`./gradlew.bat jar --console=plain`增量编译/制品，核实际`javap -public`有`public int syncedSteamCapacityMb()`、`javap -c -p`方法只读既有int字段后ireturn，无默认容量重算/写状态/扫描/客户端类型引用。核初始化值、原包getInt读取及缺失/非正值消费者安全边界，实际API无同步有效性保证；美术身份验证通过且容量>0才用作分母。

新制品编译前保存逻辑树原L1 JAR及controller源/两个class，以保留历史冻结证据；不改写旧manifest。本批冻结一源、实际ABI/字节码、两个controller class/JAR SHA及原始退出码，不建立全仓矩阵。不跑全量、原29项/旧探针、GameTest或用户客户端，不合入美术05B/06素材。

一次独立差异窄审核单一方法/注释、原网络字段未变、实物ABI/绑定和消费边界，复用已有通过证据。PM净合main并只同步一源/报告/共享合同给美术树，原主JAR备份后可更新为同一增量制品；不得以此JAR冒充含05B/06素材的美术候选。PM发布实际HANDOFF及独立06消费者准许写集，美术负责人据用户专项权限安排后续；新视觉仍待用户复看，其他工程主线暂停。
