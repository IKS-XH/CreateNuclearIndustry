# EXT-B-TURBINE-01E 定点复审 R1

- 对象：首次 `review.md` 的旧动力网 NBT 疑点、中文注释；核对 `review-r1.diff`、更新后的 `report.md`、`build/runtime-01e-legacy-first-tick/logs/latest.log`、现行测试源码及本地 Create 6.0.10-280 sources.jar。未重跑测试或执行 Git 写操作。

## 结论

**规格符合性：仍暂不通过。** 新增 `ExtensionTurbineLegacyGameTests` 确实覆盖了旧前轴与电机同网、后轴另网的 `Network.Capacity/AddedCapacity`，以及生产账本历史归零后的首次/二次初始化、稳定同网和重新供汽；最终日志为 11/11 required GameTests 通过。但合成器在 `ExtensionTurbineLegacyGameTests.java:170-180` 对三份旧 NBT 一律删除 `Source`。旧同速电机驱动前轴时，前轴可能保存 `Source=电机坐标`；这条不同的原生清理分支尚未覆盖，故通过结果不能排除首次审查所指的持久容量残留。

**质量：仍暂不通过，原因是上述同一具体风险和一处待修注释。** 冻结包的控制器类注释、探针类总说明，以及控制器 `totalSu()` 的 40 tick 衰减注释现已改正。`TurbineKineticProbeGameTests.java:54` 仍写红石“仅撤销本机份额”，应改为“本机生成容量”以避免延续已废弃的轴份额语义；这只需文字复核。

## 仍未排除的精确路径

旧版 9159155 前后轴均为 Create 生成源。前轴接入已运行的同速 Creative Motor 网络时，Create `RotationPropagator.propagateNewSource` 的异网同速分支可调用 `setSource`，使旧前轴保存指向电机的 `Source`，且两者使用同一个 `Network.Id`。新设计载入后，生产 `TurbineState.load` 清空 40 tick 历史，前轴暂时 `getGeneratedSpeed()==0`。Create `KineticBlockEntity.initialize` 从 NBT 的 `Network.Capacity` 初始化网络；`KineticNetwork.addSilently` 仅在当下 `isSource()` 时扣除旧 `AddedCapacity`。若旧前轴带 `Source`，`GeneratingKineticBlockEntity.applyNewSpeed(speed==0)` 走 `hasSource()` 分支，只通知当前容量为零并返回，不执行无源分支的 `detachKinetics()/setNetwork(null)`。电机作为原生源继续维持网络，旧前轴份额可能留在 `unloadedCapacity`；重新供汽时新整机额度又登记进同网。相关代码是新 `TurbineOutputShaftBlockEntity.java:19-22`、`TurbineShaftPowerSource.java:77-84`，Create sources.jar 内 `KineticBlockEntity.java:85-92,210-270`、`KineticNetwork.java:27-55,68-70`、`GeneratingKineticBlockEntity.java:118-130`、`RotationPropagator.java:265-284`。

本次用例在 `ExtensionTurbineLegacyGameTests.java:55-63` 构造了前轴与电机同网，却在 `:172` 清掉两者的 `Source`，因此没有进入上述 `hasSource()` 清理分支。最小后续证据是在现有同一用例中保存真实旧拓扑可产生的 `Source=电机`，保留现有首 tick、稳定两端/电机同网及重新供汽精确容量断言；只需该定向用例及若有修复的受影响探针。若它通过，应以 Create 网络实际清理顺序解释为何缓存被抵扣或网络被销毁；若失败，则按失败路径修复，不用扩充其他设备矩阵。

## 已证实范围与人工门

最终 `latest.log:83-84` 确认正式域 11/11 通过；执行报告称该次退出码 0，且新增测试及注释后增量 `assemble` 退出码 0。本复审仅读取，未重复运行。此用例合成旧版两轴 Network NBT、替换正式机组的新 BE，并让服务器原生 `initialize`；**未打开真实旧世界，也未执行完整区块磁盘卸载**。既有 9/9 JUnit、4/4 网络探针及原 10/10 正式机组证据沿用首次审查，不视为本轮新运行。01E 的客户端共享容量观察及 01D/R1 外观复测仍是任务卡人工门，自动化通过不能代替。
