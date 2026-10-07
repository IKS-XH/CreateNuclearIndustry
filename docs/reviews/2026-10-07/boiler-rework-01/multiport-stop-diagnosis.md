# EXT-B-BOILER-REWORK-01E 实施与诊断

基准HEAD：`d0e9c64f06925bd25448549f058a72bd76fde374`；01D功能快照：`99a526c97697e9de134cbd090674927c2147c2bd`。工作区为同级候选 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。执行者未做Git写入、治理修改、客户端操作、用户世界/配置改写或旧存档研究。原有logs、__pycache__以及PM文档改动均保留。

## 已确认根因及修复边界

真实中型/大型机组通过实际进汽能力供汽、排汽能力排出，先运行70tick，再用其自身当前运行NBT重建输出轴并清空账本的临时流量历史。基线下账本SU归零、生成速度归零，前后轴和两端原生外接轴仍为256RPM；前轴与前外接轴自然形成互相指向的Source。160tick仍稳定残转，165tick停转断言失败。`03-natural-current-red.log` 首次捕获中型；`07-natural-fault-capture-red.log` 同时捕获中型、大型。不曾反射写坏Source、Network或转速。

锁定Create 6.0.10-280的KBE周期校验会在无Source且生成速度为0时直接把speed置0，不执行传播撤销；原生传播器对待移除轴的理论速度为0时早退。原初始化路径只核销保存的生成容量并保留保存转速，随后原生连接/校验可从仍有转速的下游反向认领Source。旧`previousSu>0`补偿不覆盖本轴刚恢复、`lastAssignedSu=NaN`的首次同步。

经PM确认，生产代码只改 `TurbineShaftPowerSource.initialize()`：沿用当前格式保存生成容量/RPM核销门槛，在初始化阶段按保存转速调用原生`detachKinetics`撤销本轴依赖分支，然后清本轴Source/speed/network并通知。没有按SU=0清整网。真实同速CreativeMotor对照仍保留外源容量和双端256RPM。

当前正常运行快照重建的红→绿已确认。这里的正常NBT实体重建不是整chunk磁盘重载或客户端退出重进，不能冒称覆盖用户现场首次触发条件。另复验共享父类原生probe的真实跨chunk卸载/加载及外源保留，覆盖原生基础机制；这同样不是完整实际汽轮机客户端存档验收。

## 已存在故障的当前快照

PM批准一次受控基线回换；仅临时撤去本批initialize十行差异，`07`在自然故障160tick保存四实体完整当前NBT。运行后恢复本批修复，原始资源封包为 `src/main/resources/data/create_nuclear_industry_boiler_multiport_stop/current_faulted_shaft.nbt`，280字节，SHA256：`250657D7259ACCA85384F61B864386CBAC0A8D7888488FF06B1719C149CCDBB2`。同一副本为制品目录 `07-natural-current-faulted-shaft.nbt`；`10-natural-current-faulted-shaft.nbt` 是第二次自然基线捕获，原点不同，不与07混用。

测试只重定位记录中的实体坐标、Source坐标和以坐标编码的Network ID，不构造异常Source。故障快照的Network没有AddedCapacity/TurbineGeneratedRpm，确实不满足新增初始化门槛。基线`10`与修复`09/11`均能在完整四实体重建后自行停止，因此该再加载路径原本就有恢复行为，不将它计为新增修复收益，也没有为假说新增有向环扫描。不能据此保证所有现场加载顺序已覆盖。

## 锅炉实际流动对照

所有锅炉测试均为6×6底面、5高、隔层y=2，水/汽各16格、16对有效回路，容量各32000mB。持续输入为真实能力交易；热液付款、生产、泄放均运行产品账本。没有改BoilerState、参数、汽种选择或外部流体。

- 原3SC/1普通，西侧四口共同短原生管，以及直罐对照均可在60→10后继续成交。
- 长管接单中型108mB/t、单大型216mB/t，按3SC/1普通保留；合法范围和大型原生机械泵对照均持续成交。
- 最新口述4SC/16对/单中型/共同分支创造储罐拓扑，合法范围无泵和原生泵两个对照持续运行900tick。创造罐使用实际原生BE与原生能力；测试子类仅记录`super.fill`的EXECUTE接收量，保留SIMULATE与无限接收行为。每tick断言mB、HU守恒和实际输出SC身份。
- `11`无泵t900：累计产汽244864mB，中型实际排出34236mB，创造罐销毁216112mB，安全阀泄放6656mB；263次SC资格跨越后管路仍自动恢复。带泵分别为244608、34344、215944、6400mB，同263次跨越。末次平均流量29.7mB/t低于中型启动门槛32.4mB/t，账本0SU属于该测试流量状态，不能从0SU单独推断残转。
- 未设创造罐时持续产汽256mB/t大于单台108/216吞吐，高炉压与泄放可由供需解释。压力下限10仍是最低放汽门槛，没有强迫炉压变10，也没有把等待SC或异种背压判为故障。

`06`中型长管0成交曾失败；实际最近匹配SC口到入口首段为17段，超过锁定Create默认pumpRange16，普通第四口不贡献SC主动压力。上升点缩短2段后自动成交。因此这个红测是合法传播范围边界，不是已证实产品缓存缺陷；没有通过扩大原生范围修改生产。本批未宣告现场锅炉停流已修复。

PM指定分析者首次对 `run/saves/新的世界` 的只读磁盘解析发现：锅炉控制器(28,-59,-11)、16对、容量32000、下限10；四北向口(26..29,-57,-16)，该时点前三SC、末口普通。中型真实顶入口(26,-56,-27)朝UP，前三SC到入口实际13/14/15管节点，无泵且均在16范围内，超范围不能解释这份记录。x28首管east=false、x29首管为axis=z玻璃管，形成实际隔断；末口普通另支经过13个pipe节点到普通Create 3×3×8储罐，不与中型线路连接。该罐控制器(33,-60,-28)存18267mB普通蒸汽；局部另有装水的创造罐，但未与本锅炉支路连接。

首次磁盘解析与口述布局不一致，取证期间客户端仍写，不能确认最新内存状态。原“region早于level.dat约10小时”时间比较经PM指出混用了时区/字段，已经撤回。分析者用一致UTC口径限定刷新后确认：2026-10-07相关MCA chunk header为09:53:53/54Z，level.dat为09:52:00Z，region在解析期间仍写至09:54:34Z；不能据此把差异归因为旧区块或用户误述。

限定刷新当前offset仍解析到前三SC、末口普通，前三到中型13/14/15管节点、末口另支到普通3×3×8蒸汽罐约13节点；局部独立创造罐为8000mB水，未证实与锅炉支路连通。上述拓扑只代表本次非静态磁盘解析所见，用户保存退出前不扩大搜索，也不视为最新内存状态确认。首次解析中的磁盘轴Speed0且无Source/Network，没有捕获原生残转；视图RPM字段不是实际轴速。锅炉生产代码保持不变；需本候选复测和保存退出后的静态故障现场证据，才能读取准确温压/流体/拓扑与源链。

用户随后明确确认所报目标就是控制器(28,-59,-11)，并说明已拆掉创造储罐。因此当前解析布置不是此前带overflow CreativeTank分支的故障原始拓扑。现阶段冻结结论：当前正常运行NBT恢复后的native Source互指残转已复现并作局部修复；锅炉旧故障中创造分支的实际位置与连线仍缺失，相近自动场景通过不等于现场修复或验收。集中客户端复测时需恢复原创造分支，复现后保存退出取证。本批停在人工门，不追加代码、测试、构建或其他任务。

## 定向验证及保留失败

唯一Gradle/服务端所有者为本实施者；目录始终 `run/verification/boiler-multiport-stop-01e`。原始日志和逐次退出码全部保留 `build/reports/extension/EXT-B-BOILER-REWORK-01E/`。

最终业务验证：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-21'
./gradlew.bat runGameTestServer '-PgameTestNamespace=create_nuclear_industry_boiler_multiport_stop,create_nuclear_industry_turbine_probe' -PgameTestDirectory=run/verification/boiler-multiport-stop-01e
```

`11-final-scoped-green.log`：01E 12项+共享动力probe 5项，共17项全部通过；Gradle退出0，正常保存与服务端关闭。没有Saving worlds停滞，没有终止其他进程。新增测试源后续仅注释调整不改变被测行为。

保留的非产品失败：`01`汽口DeferredBlock方法拼写编译失败；`02`初始能力未成型时调用过早；`03/04`大型夹具内腔残留导致未先发电，后显式清空原搭建器跳过的airSlot；`08`原生轴BE type名称编译错误；`10`跨域包装probe因专用命名空间未注册而null失败，已删除包装并在`11`正式启用原probe域。均未通过放宽产品断言获取通过。`03/04/07/10`中的持续残转属于有效产品红测；`06`属于前述超传播范围对照。

本批没有Boiler生产变更，不机械重跑01D选汽UI/能力域、无关JUnit或全量build。已审历史01C/01D证据保持各自覆盖范围。人工集中门仍待用户。

唯一最终增量 `./gradlew.bat assemble --console=plain` 已完成，`12-assemble.log` / `12-assemble.exit.txt`：退出0，Gradle4秒，正常成功。候选JAR `build/libs/create_nuclear_industry-0.1.0.jar`，2247712字节，SHA256：`080A09302E7D5E596F30CB6D75E0E2D42093227A409597AADDA9B35263CF03FB`。制品副本及 `artifact.json` 在本批制品目录；生产轴class、测试class、两份NBT均与编译/源文件逐字节SHA一致。仅做一次本批增量assemble，没有重复build或测试。独立审查由PM管理，本文不代替审查或人工验收。

## 写集与实际技能

当前写集：`BoilerMultiportStopGameTests.java`、独立`multiport_empty.nbt`、自然当前故障快照资源、`TurbineShaftPowerSource.java`及本报告。其他生产文件未修改；没有升级MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82或Flywheel1.0.6。

实际读取并应用：AGENTS、治理5.1/5.2、01E及01C/01D合同，`minecraft-modding`、`minecraft-testing`、`systematic-debugging`、`test-driven-development`及其`writing-good-tests.md`、`verification-before-completion`。按锁定源码核对FluidNetwork/FlowSource/PipeConnection和GeneratingKBE/KBE/RotationPropagator，技能新版本例子未用于升级技术栈。未自行派发、修改治理或执行Git写操作。
