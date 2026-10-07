# 分区温压锅炉：候选证据与验收

**当前状态（2026-10-08）：用户已确认01F精简手测通过，锅炉重构已验收合入main。** 整合提交`611d3ca`，65个源码/资源/美术源路径与最终候选`e721797`一致；复用25项账本和11个不同真实用例的证据，07整轮exit1及早期失败记录仍保留。新增一次main增量assemble及24项静态封包核对通过，见[验收与主目录制品](../../2026-10-08/boiler-rework-01/ACCEPTANCE.md)。本页旧待验收描述保留历史含义，不再作为人工门；下方四项清单已获用户确认，不扩大为未列场景或首发验收。

**上一轮01E候选：** 汽轮机当前实体恢复后的断汽残转路径局部修复，功能快照`5ced309a46b85b3c0de7e98069bb1bf90f67e53f`，17项定向GameTest、一次增量assemble与独立审查通过。此轮用户报告残转没有复现；不扩写为全部动力场景验收。当时锅炉原始共管停流未定位、创造罐已拆除，旧非稳定快照不能代替故障现场；最新稳定拓扑和短窗复现以本页R1为准，旧恢复支路清单不继续作为当前操作要求。[01E计划与边界](../../../superpowers/plans/2026-10-07-boiler-multiport-turbine-stop-01e.md)。下方01D自动通过与暂缓说明为历史证据，不表示本次人工通过；主线/教学继续停止。

**历史状态（01D）：[蒸汽口独立汽种过滤](../../../superpowers/plans/2026-10-07-boiler-steam-port-selection.md)已实现；最终4项定向GameTest、唯一一次增量assemble及一次合并审查通过，功能快照`99a526c97697e9de134cbd090674927c2147c2bd`。当时用户未能复现持续残转，排查暂缓；随后反馈及现阶段局部修复以01E为准。锅炉集中人工门仍保留，未合入main。** [01C来源缓存修复](../../../superpowers/plans/2026-10-07-boiler-turbine-continuous-flow-fix.md)快照`050afe3e4eb70789804697b2880b7c76281ca8d7`继续包含；原[01B](../../../superpowers/plans/2026-10-07-boiler-controls-automatic-output-fix.md)、[REWORK-01](../../../superpowers/plans/2026-10-07-ext-b-boiler-rework-01.md)和[01A](../../../superpowers/plans/2026-10-07-boiler-hot-inlet-layer-fix.md)的未变部分继续适用。01B自动任意汽种输出由01D逐口纯过滤替代，历史证据不改写。主目录仅同步文档与候选制品，新源码须从同级候选启动。

新版锅炉现已合入主目录`E:/MyMC/NewMod/Create_NuclearIndustry`，可从主目录启动runClient；原同级候选和测试世界保留。两套运行目录与配置各自独立，不搬移或改写存档。

## 启动与制品

**当前启动：** 新版锅炉已合入主目录，可在`E:/MyMC/NewMod/Create_NuclearIndustry`运行`.\gradlew.bat runClient`。[本轮主目录打包制品及验收](../../2026-10-08/boiler-rework-01/ACCEPTANCE.md)以2026-10-08记录为准；下方候选制品和同级启动命令保留历史与原测试世界使用场景，不再表示必须从候选启动。

[01F候选JAR](../../../../build/reports/extension/EXT-B-BOILER-REWORK-01F/create_nuclear_industry-0.1.0-boiler-01f.jar)：2,266,521字节，内部版本`0.1.0`；SHA-256 `B0CAE4FD76EA3244C1964331844DE66EBFDD9867393C10EF9A40452C27350CF8`。PM已核对24项相关class、语言及模板与实际编译输出一致，两目录保留同一制品；详见制品目录`pm-artifact-validation.json`。重启同级候选runClient加载01F源码。

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
.\gradlew.bat runClient
```

请按下方新搭法搭建锅炉；原底部外置热源结构不属于本候选搭法。默认尺寸范围为`[5,11]`，热工、容量、逐口流量与压力阈值均在`create_nuclear_industry-boiler.toml`中配置，准确字段和实际生效路径见[配置指南](../../../server-config.md#锅炉)。显式机器保压设置由各机器保存。两类新口及再加热段成本见[配方表](../../../recipes.md#14-专用高压锅炉部件)。

[历史01E候选JAR](../../../../build/reports/extension/EXT-B-BOILER-REWORK-01E/client-candidate/create_nuclear_industry-0.1.0-boiler-multiport-stop.jar)：2,247,712字节，内部版本仍为`0.1.0`；SHA-256 `080A09302E7D5E596F30CB6D75E0E2D42093227A409597AADDA9B35263CF03FB`。两目录保存相同制品；PM独立核对本批生产class、测试class及两NBT与实际编译输出/资源一致，记录见同目录`pm-artifact-validation.json`。开发启动读取源码和资源，须重启客户端加载新代码。依赖保持MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。

原[01D候选JAR](../../../../build/reports/extension/EXT-B-BOILER-REWORK-01D/client-candidate/create_nuclear_industry-0.1.0-boiler-steam-selection.jar)：2,227,784字节，SHA-256 `C699C2A97E84D318CFBDA33A79A4409AD3ED0D88C038DD5E33678620A72A5412`，封包核对记录继续保留。

原[01C候选JAR](../../../../build/reports/extension/EXT-B-BOILER-REWORK-01C/client-candidate/create_nuclear_industry-0.1.0-boiler-flow.jar)（2,207,578字节，SHA-256 `B3BF640B296561533EA1FDBAC802DBEAC11AFCA530C58879940325DA76E6E911`）继续保留。

原[01B候选JAR](../../../../build/reports/extension/EXT-B-BOILER-REWORK-01B/client-candidate/create_nuclear_industry-0.1.0-boiler-controls.jar)（2,193,605字节，SHA-256 `94F5B6481AB62BDB4B8594AAD1044B2FB693DCAB009A6FA644A77CEBE818AA2F`）、01A快照（2,186,418字节，SHA-256 `577CB895A33175AB18F08A65096B24E059271C94D95570622FC30638AFEC4414`）及REWORK-01快照（2,179,767字节，SHA-256 `A93ED326EAB0F9BF471DBD9A6A9497D968D226028509AB30D0E3B09856B9640D`）保留历史核对。

## 自动证据与审查

| 证据 | 结果 |
| :--- | :--- |
| 01F双库存 | 25项账本通过，11个不同真实用例以10＋1定向证据通过；唯一assemble及独立审查通过。共享容量、已付热、逐种保压、长管实际成交、冷液与选项、当前双池快照通过；客户端仍待集中验收。 |
| 01E残转与多口诊断 | 最终17/17、GameTest exit0、assemble exit0，独立审查允许局部汽轮机候选进入人工测试。正常本版本实体恢复后中/大型自然残转红测由局部初始化修复关闭，合法外源对照保留；原生范围内四SC/16回路/中型/创造罐共同支路通过并守恒。锅炉原现场故障仍未定位，完整客户端保存重进也不由实体重建证据代替。 |
| 01D逐口汽种过滤 | 最终4/4、GameTest exit0/实际19.79秒，服务端正常保存退出；唯一assemble exit0/实际5.42秒，一次独立合并审查无阻断。原生选项/当前NBT/客户端快照、拒绝降级、SIMULATE、逐口额度与HU、真实双路管/直接相邻泵切换、冷液持续及外部异种背压通过。最后仅遥测分支和槽位布局由assemble核对，实际客户端点击/显示仍待人工。 |
| 01C持续供热及联动 | 专用5/5、GameTest/Gradle exit0/24秒，增量assemble exit0/1秒，一次独立合并审查无阻断；持续大型/中型/空罐、自然快速切种、实际汽量/HU边界、异种背压及同机双端四齿轮箱回接停开均通过。持续残转没有复现，不据此关闭现场项。 |
| 01B单控件与自动汽种定向验证 | 新BoilerStateTest 15/15，专用真实GameTest 3/3，增量assemble exit0。原生数值包17/43/70/0/100提交、冷液真实泵跨调压/汽种保持原句柄、蒸汽原管自动换种且质量/HU守恒均通过。首轮夹具快照错误及修复证据保留；一次合并审查通过，未发现确定性阻断缺陷。 |
| 两套账本单测 | BoilerStateTest 13项＋HeatExchangerStateTest 16项，共29项，0失败/错误/跳过；生命周期整改未改账本，复用此证据。 |
| 最终锅炉域GameTest | 15/15通过，包含真实Create普通/超临界管路、独立机冒烟及新增远端FULL可用性恢复/重叠拒绝回归；服务端正常保存退出。 |
| 底层热入口定向GameTest | 3/3通过：四向多入口共享库存/逐口额度，非法层位/朝向/角点/其他棱边部件拒绝，真实Create泵送及拆装能力失效/恢复守恒。既有完整域夹具已同步下降，不重跑无关账本/全域。 |
| 增量构建与制品 | assemble成功；PM独立核对当前JAR大小与SHA-256，并保存同一制品快照。 |
| 合并审查 | 原两项Important均已整改并复核关闭，无新增已证实Critical/Important；可进入集中人工候选。 |

[实现及整改记录](./implementation.md)、[SVG素材记录](./assets.md)、[原审查及关闭结论](./review.md)、[01A实施及审查记录](./hot-inlet-layer.md)。原日志与XML保留在`build/reports/extension/EXT-B-BOILER-REWORK-01/`，01A定向日志在`build/reports/extension/EXT-B-BOILER-REWORK-01A/`，失败轮不改写为成功。远端用例证明FULL可用性退降/恢复，不声称发生完整区块磁盘卸载；32格是可配置工程封顶，不代表最大规模客户端性能已验收。

01B见[实现及定向证据](./controls-automatic-output.md)、[规格/质量合并审查](./controls-review.md)；原始日志/XML及PM封包核对在同级候选`build/reports/extension/EXT-B-BOILER-REWORK-01B/`。原29/15项为当时合同证据，涉及旧手动模式的行为由01B新的15项账本及3项真实管路证据替代，未声称原全域按新规则重新运行。

01C见[连续流动排错与原始证据索引](./continuous-flow-diagnosis.md)、[本批合并审查](./continuous-flow-review.md)。失败轮、最终运行及原始日志保留在同级候选`build/reports/extension/EXT-B-BOILER-REWORK-01C/`；未改账本公式与动力生产代码，不重复原15项账本单测或全域。汽口首段是管道的工况已实测，直接邻接机械泵仅有静态同接口依据，本批未专测。

## 新搭法

01D交付与验证边界见[实施报告](./steam-port-selection.md)、[规格/质量合并审查](./steam-port-selection-review.md)。原始红测、编译失败和最终4项日志保留在同级候选`build/reports/extension/EXT-B-BOILER-REWORK-01D/`；三类旧夹具已按显式选择适配并编译，本轮未运行旧域，不将旧自动汽种行为称为现版本通过。

默认外尺寸每边可取5～11格，包括偶数和长方体。底面外围四边的非角点格可放热液入口；底层角点和其他棱边只放外壳。底面非边框区域放至少一台核换热器，其余位置补外壳。内部用再加热段和外壳铺满一个水平隔层，至少一个再加热段，隔层上下各留至少一层空气炉腔。下部是水区，上部是汽区。

控制器位于水区侧面的非边框格且全炉仅一个；顶部非边框格放一个安全阀，阀上方留空。热液口必须在底层四边非角点位置，与换热器同层；给水口在水区非边框侧面，冷液口在隔层高度的非边框侧面，蒸汽口在汽区非边框侧面，每类至少一个，可放多个。所有端口水平朝壳外，炉内换热器由这组端口统一供回冷却剂，不再在锅炉下方另放热源层。

参考5×5×5：以底层为第1层，第2层水区、第3层完整隔层、第4层汽区、第5层顶盖；上下各9格，水汽各18000mB。最高9台换热器与9段配对，162HU/t。只比较数量取最小值，不要求上下逐格对齐。

## 01F集中手测清单

**本清单已于2026-10-08获用户确认通过。** 下表作为验收依据保留，不要求再重复操作；未变搭建、外观、材料及汽轮机功能不扩写为本轮全部复测。

| 项目 | 操作与预期 |
| :--- | :--- |
| 双库存与共同容量 | 接热冷却剂、给水及冷液回流，观察护目镜分列蒸汽/超临界蒸汽库存。两种合计共用同一个汽区总容量；升压或再热不会把已有蒸汽改名为超临界蒸汽，降压也不会把已有超临界汽改成蒸汽。 |
| 原现场长管输出 | 在原6×6×5/16回路锅炉上，三个SC口接原共管机组/接收端，NORMAL口保持独立蒸汽管路。压力下限60→10后，已有SC在已付热合格时可继续排至公共下限；普通库存也能经独立长管排出，无需反复拆泵/管。接收端需有余量。 |
| 选项与冷液 | 保持热输入，切换单个汽口选项并切回，各口只抽对应库存，冷液回路持续。管路/储罐已有异种时允许真实背压等待，锅炉不转换或清掉外部流体。冷却不足的SC仍占库存并等待真实再热，不自动降级。 |
| 当前保存恢复 | 使用本版本生成的双库存保存退出并重进，核对两种量/公共容量和各口独立选择保留，管路可继续工作；不安排旧版本迁移测试。 |

出汽压力下限是最低输出门槛，不是目标炉压。持续热源、下游吞吐和安全阀决定实际稳定工况，不要求始终保持10%；本次要消除的是已有汽种随50%门槛反复失去来源资格。不同汽种保持独立管路，共用锅炉容量不等于原生管网支持混装。

01E的局部汽轮机修复及用户“残转没有复现”反馈保留，不据此扩写为所有动力工况通过，也不要求再重复17项动力矩阵。完成本表后停止在锅炉集中人工门，不接续其他主线或教学。
