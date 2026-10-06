# EXT-B-BOILER-REWORK-01C 持续流动诊断

执行者按PM根据已复现根因补入的三文件生产写集完成端点修复；本报告不改变任务状态、参数或人工门。

## 实际读取与范围

已实际读取隔离树 AGENTS.md、01C及01B任务卡、minecraft-modding、minecraft-testing、systematic-debugging、test-driven-development与verification-before-completion技能。以仓库MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82/Flywheel1.0.6为准。按治理5.1仅运行专用GameTest域；生产改动限于PM追加授权的三个入口。未Git写入、未修改核心文档、未操作用户客户端/配置/存档，保留logs与__pycache__。

## 真实夹具

新增BoilerTurbineFlowGameTests，复用ExtensionBoilerGameTests.build、ExtensionTurbineGameTests.build和01A/01B实际泵/齿轮/轴/马达布局。空模板复制自01B已有模板。

- 5³锅炉、九配对；2个底边热液进口、2个隔层冷液出口、补水口，分别经真实Create泵和管道连接真实罐。原料仅补源罐，冷液及蒸汽只从收料罐真实抽取。所有原料泵动力网与汽轮机网空间隔离。
- 6tick只使用一次合法当前账本暖炉种子：8000mB水/30400HU水侧热、14400mB汽/14400HU汽侧热；后续不灌HU、不灌汽、不修改热工参数。
- 两热泵实送256mB/t热液，实际付128HU/t并产128mB/t汽；两冷泵实收持续冷液。参考满配热上限162HU/t不等于本夹具实际128HU/t。中档108mB/t，大档216mB/t。
- 锅炉经原生汽管分别接中型、大型与空罐；汽轮机排汽再经原生管入罐。两端各有普通外接轴，无其他动力源。
- 100tick按现场将下限60→10。首轮450tick/850tick设置100暂停正常出汽、650tick恢复10；最终精简为450/650tick暂停、550tick恢复，800tick结束，仍覆盖两次停止和一次重启。

## 已复现的生产缺陷

首轮diagnosis-first.log运行1080tick，3项夹具基本链路断言通过，但遥测捕获真实停流；随后增加持续收汽断言，diagnosis-failing.log在350tick大型和空罐明确失败，3项中1通过2失败、GameTest进程exit2。另保留诊断打印访问私有压力getter导致的短编译失败日志，随后改读公开getPressure，未改生产API。

| 接收端 | 100tick下限变化后 | 稳定观测 |
| --- | --- | --- |
| 中型 | 炉压约0.769，维持SC | 108mB/t排汽，SU5308416；多余20mB/t升压后正常阀泄放 |
| 大型 | t100出128mB，t120出216mB、P0.5022 | t140起0排汽，received16644mB至t440不增长；每tick仍产128mB且SC=true，炉压转80/90循环 |
| 空罐 | t120 received16452mB | t140至t440不增长；真实接收罐每tick抽空，不能解释为满罐或背压 |

diagnosis-cache.log在t180/t350证明上述停流时第一段管道WEST连接仍有512单向压力、完整SC流、phase=IDLE；原生FluidNetwork仍为SC、targets=1、queued=0、frontier=0，但其source.getCapability()为null。中型同工况能力有效。

追加diagnosis-provider.log已直接对比：t350空罐旧network能力null，当前FlowSource能力为有效单罐15680mB SC且SIMULATE可抽1mB；大型当前端点为15168mB SC且可模拟抽取。sameProvider=false，确认不是服务端真实汽口缺失。追加每tick汽质量守恒（初始14400+累计实际产汽=炉内汽+真实接收+汽轮机周转+阀泄放）在两次失败轨迹中均未触发，断点是停流而非丢汽。

## 源码链路及最小修复建议

锁定Create源码jar已读取，相关只读副本存于本批build报告目录。

1. BoilerControllerBlockEntity.Port在EXECUTE排汽后refreshSteamKind，跨P0.5资格即递增steamEpoch、撤销汽口能力并释放自有压力；dirtyPorts由下一控制器tick刷新。
2. FluidTransportBehaviour的WAIT_FOR_PUMPS/FLIP_FLOWS相位暂跳manageFlows。连续产热使汽种迅速SC→steam→SC时，Layer II可始终保留SC流，PipeConnection.manageFlows保留旧FluidNetwork。
3. FlowSource.FluidHandler在能力撤销时放弃自身fluidHandlerCache；ICapabilityProvider.BlockCapabilityCacheProvider.invalid永久置true，旧provider今后返回null。
4. FluidNetwork仅source==null才从supplier取provider；reset只清遍历、targets和fluid，不清source。propagateChangedPipe/wipePressure/resetNetwork均不足以撤销被保留Network中的旧source。
5. 01B人工种子长期停留另一汽种，Layer II清流重建Network，未覆盖这次快速往返门槛。

建议只处理dirty蒸汽端口外向第一段连接的Layer III网络，保留Layer II流体、全部压力贡献及外部流体库存：BoilerPressureConnection增加显式“遗忘本连接原生网络”方法；已有BoilerPipePressureMixin按锁定原生字段shadow Optional<FluidNetwork> network，方法仅将其设为空；BoilerControllerBlockEntity.refreshPipes对dirty蒸汽端口正确外向连接，在正常propagateChangedPipe后调用。下一原生manageFlows重建网络并从更新的FlowSource取得新provider。操作放在控制器tick，避免在正在执行drain的网络迭代中换网。

PM随后已将三路径追加本卡并同步隔离树，实际实现与上述最小建议一致。未改第三方FluidNetwork、未添加延时/迟滞、自动降热、强制steam，未改变90/80阀线及108/216吞吐。

## 修复与正式定向复验

生产只改BoilerPressureConnection、BoilerPipePressureMixin、BoilerControllerBlockEntity。汽口首段可为普通管道或原生泵，桥接仅置空该连接的原生network Optional，保留Layer II、全部压力份额和端点库存；Controller仅在真实朝外的dirty汽口刷新期间调用。

flow-fix-first.log复用原失败三场景3/3通过：旧能力null不再出现，旧Network provider与当前FlowSource provider相同且有效。flow-loop-diagnosis.log加双端物理回接后4/4通过。

flow-final.log最终5/5通过、GameTest/Gradle exit0、24秒，未跑其他全套：

- 800tick持续空罐累计实收77636mB，自然跨汽种资格64次；大型累计75180mB，自然跨资格34次；中型累计56592mB，自然跨资格0次，维持高于启动门槛的正常108mB/t，阀泄放只消化供热超过吞吐部分。
- 三持续场景每tick严格核对汽质量：初始14400+实际产汽=炉内汽+真实罐抽取+汽轮机周转+阀泄放。每tick确认汽实际比焓为1HU/mB（Ts=2），以原初44800HU及实际转冷付款0.5HU/mB为输入，核对炉内+已输出/待排/阀泄放HU无超支；差额仅允许落在该tick累计配置自然冷却最大界内。压力门槛下声明普通汽时仍计真实1HU/mB。
- 复用01B异种背压场景，在本专用域验证普通→SC→普通管路重建、每tickmB/实际焓及外罐SC保留，直到接收方真实抽取腾空后才恢复普通汽。该回归补充守恒/外库存边界，不替代真实持续工况。
- 实际中型两端经4个Create Gearbox和普通轴从机身外回接，满额供汽→断供→重启→断供，140/310tick两次明确检查0SU及外接轴0RPM。

当前仅证实出汽停流修复；动力残转未复现、未改动力生产代码，不能声明用户所有手测问题已修复。

已核实flow-final.log直接包含五项完成、All 5 required tests passed、Stopping server、Saving worlds、三个维度All chunks are saved、Game test server shutting down与BUILD SUCCESSFUL。首轮只基本断言通过的diagnosis-first.log不作为持续恢复通过证据；diagnosis-failing/cache/provider为保留的生产停流失败证据，不改写为通过。

PM随后授权本批一次增量assemble：`./gradlew.bat assemble --console=plain`，真实exit0，Gradle显示1秒、终端测得1.863秒；日志assemble.log。最终`create_nuclear_industry-0.1.0.jar`为2,207,578字节，SHA-256 `B3BF640B296561533EA1FDBAC802DBEAC11AFCA530C58879940325DA76E6E911`，制品副本保存于本批build报告目录。未再跑JUnit或其他测试域。

## 轴残转目前证据

首轮大型在t151进入零SU时有一tick旧256RPM，t158双轴及外接轴均为0、source/network均null；中型两次停汽后也自动停止并能恢复。本轮尚未复现“持续不自停”，不能宣称该用户问题已经解决。

已读取TurbineShaftPowerSource/GeneratingKineticBlockEntity/KineticBlockEntity/RotationPropagator：01G修补覆盖父tick先清零的无source轴；原生applyNewSpeed(0)在hasSource时仅撤容量并保留转动。追加真实外部两端闭环回接中，前轴source仍为null，断供后所有网络清零，重启能恢复唯一容量；未捕获本机反向source。应保留用户现场外接拓扑/具体运行候选进一步定位的门槛，禁止直接强清全网或猜测改动力代码。

## 执行命令

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-21'
./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_boiler_turbine_flow -PgameTestDirectory=run/verification/boiler-turbine-flow-01c --console=plain
```

交付三生产文件、本批测试、模板、报告、build诊断证据及授权的一次增量assemble制品；未跑全量测试，保留动力未复现及既有人工门。停止扩大轴诊断；若同次客户端复测仍有残转，再由PM收集现场轴Source/Network/Speed。
