# EXT-B-BOILER-REWORK-01C 合并审查

**结论：** 三文件生产写集与已复现的汽口 Layer III 失效相符；当前静态审查未发现阻断项。专用 `flow-final.log` 记录 5/5 GameTests 通过、Gradle `BUILD SUCCESSFUL`。本审查不把尚未复现的 0 SU 永久残转判为已修复，也不关闭锅炉人工验收门。

## 生产改动与边界

- `BoilerControllerBlockEntity.refreshPipes()` 仅在服务端控制器 tick 消化 `dirtyPorts`，并且只对 `STEAM_PORT` 的 `FACING` 外侧连接调用桥接。它从外侧 `FluidTransportBehaviour` 取面对汽口的 `PipeConnection`，覆盖直连管道或实现该行为的相邻运输方块；无连接时不操作。
- `BoilerPressureConnection` 的桥接契约明确只用于控制器 tick，禁止在 `drain` 交易中调用。汽口 `Port.drain(EXECUTE)` 跨汽种时只撤销旧汽句柄并标记端口待刷新；实际丢弃网络在稍后的控制器 tick 完成，没有在 Create 迭代 `FluidNetwork` 时换网。
- `BoilerPipePressureMixin` 只把该 `PipeConnection.network` 设为 `Optional.empty()`。锁定 Create 6.0.10-280 源码确认 `FluidTransportBehaviour.getConnection` 返回该连接类型，`PipeConnection.network` 是未序列化的 Layer III 缓存；Layer II 的 flow、pressure 和 source、端点能力及外部库存都未由桥接写入。下一次原生 `manageFlows` 可基于现有 source 创建新网络。
- 代码和 GameTest 中的中文注释与上述服务端时序、单位和数据边界一致。

## 测试覆盖及证据

`BoilerTurbineFlowGameTests` 的三个持续场景使用真实 5³ 锅炉、原生水/热液/冷液泵管、连续补水和冷液回收，以及真实普通管道；蒸汽连接到中型、大型汽轮机或空接收罐。除一次已验证账本暖炉种子外，持续段不注入 HU 或蒸汽。中/大型机组由真实排汽与无额外源的外接轴运行。

测试每 tick 验证蒸汽质量等式（初始库存与实际产汽、炉内库存、接收罐、汽轮机周转和阀泄放）及 HU 账本；流量通过实际能力的 `EXECUTE` 成交。日志显示空罐自然跨汽种资格 64 次并实收 77,636 mB，大型跨 34 次并实收 75,180 mB，中型实收 56,592 mB；在 350 tick 对原停流工况另断言接收量超过 25,000 mB。`flow-final.log` 证明专用域 5/5 通过、GameTest 与 Gradle 正常退出。

诊断采样实际调用当前 FlowSource capability 的 `SIMULATE` drain，并记录旧 network provider、当前 provider、能力返回及 Layer II pressure/flow；最终持续成交与逐 tick 质量/HU断言验证修复后的实际运输。汽种及外部异种库存边界由复用的 `nativeSteamPipeChangesTypeWithoutLossOrExternalRewrite` 检查：普通汽→超临界汽→普通汽，逐 tick 查 mB/HU，异种外罐在接收方真实抽取前保留。既有多端口 `SIMULATE` 账本只读断言位于 `ExtensionBoilerGameTests`，不属于本次 5 项专用运行域。

需要保留的覆盖边界：持续工况中锅炉汽口直接连接的是原生管道，测试没有单独把机械泵直接放在汽口外侧。实现根据 `FluidTransportBehaviour` 取得同一种 `PipeConnection`，静态路径兼容相邻原生运输行为，但该具体相邻泵拓扑尚无本批 GameTest 证据。若将“汽口直连泵”列为本次必须验收的拓扑，应补一条定向场景后再关人工门。

350 tick 的大型机组排汽管采样曾看到一帧 `networkFluid=empty`、`queued=1`、旧 provider 为空而当前 source capability 有效；同一帧及相邻采样的实际排汽为 216 mB/t，累计接收持续增长，后续仍满足全程质量守恒。它证明诊断时允许观察到正在重建的排汽网络状态，但现有证据没有显示这导致持续停流，故不扩展到汽轮机排汽连接的重置范围。

0 SU 停转断言只证明专用场景内无外源的中/大型轴及外接轴在采样窗口内归零。用户现场报告的永久残转未复现，本改动没有触碰动力网生产代码；不得据此宣称该现场问题已解决。
