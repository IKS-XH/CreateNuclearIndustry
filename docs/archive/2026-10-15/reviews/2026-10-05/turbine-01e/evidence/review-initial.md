# EXT-B-TURBINE-01E 独立合并审查

- 审查身份：只读执行者；基线 `9159155`，对象为 `review-package.diff` 冻结的 14 个实现文件及同目录 `report.md`。仅写入本报告；未执行 Git 写操作、Gradle 或客户端测试。
- 实际使用：`minecraft-modding`、`minecraft-testing` 技能，`AGENTS.md`、任务卡、治理 5.1、本地 Create 6.0.10-280 sources.jar（`KineticBlockEntity`、`GeneratingKineticBlockEntity`、`KineticNetwork`、`RotationPropagator`）。

## 结论

**规格符合性：暂不通过，旧存档/保存恢复容量路径阻断。** 当前新建机组的单源发布、两端原生贯通、总容量不翻倍、合计过载、停机外源保留及双向真实卸载场景有定向通过证据；但已有 Create 动力网络 NBT 恢复到新单源设计时，旧容量可能被保留为 `unloadedCapacity`，再叠加新前轴总容量。此问题直接涉及本卡的旧存档恢复、不得幽灵容量和总容量不翻倍合同。需原实现者先作定向真实 NBT 复现、按结果修复或用证据排除；本轮静态审查不把推导说成已实测失败。

**质量：暂不通过，同上述持久化路径阻断。** 其余已审范围内没有发现独立的实现阻断。冻结包中 `TurbineControllerBlockEntity.java:32` 与 `TurbineKineticProbeGameTests.java:20` 曾沿用“份额”注释；审查期间工作树已改为单源/共享容量表述，复核通过。`TurbineControllerBlockEntity.java:329` 曾把“无汽时为零”写成即时效果，与 40 tick 平滑尾预算不符，PM 已交原实现者改注释；此处需在整改差异中核对文字，不要求为纯注释重跑测试。

## 阻断依据与触发

1. **旧轴 Network NBT 恢复可能留下幽灵容量（高优先级，待定向复现）。** 旧版 `9159155` 的 `TurbineOutputShaftBlockEntity` 前、后轴均按固定 `frontShare` 成为 Create 生成源。Create 的 `KineticBlockEntity.write/read` 持久化 `Speed` 和 `Network`，后者包含网络 `Capacity` 与本轴 `AddedCapacity`；`initialize()` 以保存的容量初始化网络。`KineticNetwork.addSilently()` 只有在载入实体当下 `isSource()` 为真时，才从 `unloadedCapacity` 扣除其旧 `AddedCapacity × RPM`。新代码 `TurbineOutputShaftBlockEntity.java:19-22` 使后轴永久为非生成源；`TurbineState.java:220-232` 载入时清空平滑历史，故前轴在重新处理蒸汽前也暂为非源。旧网络在这段时间初始化时可能保留旧源容量；`TurbineShaftPowerSource.java:77-84` 后续通知 Create 登记前轴完整总 SU，并不抵扣那份 `unloadedCapacity`。当保存时轴网仍连有其他成员、加载后成员维持该网络时，旧半额甚至旧两端总额可能与新总额叠加。相关原生实现为 sources.jar 内 `KineticBlockEntity.java:85-92,210-234,244-270`、`KineticNetwork.java:27-55,68-70,133-146`。应针对真实前/后轴旧 `Network` NBT、外接网络成员及控制器重新升容进行定向用例，断言加载前/加载中/重新供汽后的容量，并确认拆网/重载无残余。若引入修复，只复验该风险及受影响的现有探针。

## 已覆盖项与证据边界

- `TurbineShaftPowerSource.java:34-49,67-84` 使用 `addPropagationLocations`/`propagateRotationTo` 的双向倍率 1 连接。连接失效时保留上次远端供 Create `handleRemoved` 搜索旧 `Source` 分支；若本轴 `source` 正是旧远端，再显式 `removeSource`。这与本地 `RotationPropagator.handleRemoved` 的邻居 Source 检查相符。结构有效性由 `TurbineOutputShaftBlockEntity.linkedShaft` → owner `validShaft/live` → `TurbineStructure.quickLive` 控制；红石/断汽只使前轴生成 SU 为零，完整结构的内部传播仍在。
- 新源仅前轴读取 `owner.totalSu()`，后轴 `assignedSu()` 为零；`calculateAddedStressCapacity()` 按总 SU / 生成 RPM 返回 Create 需要的容量单位。`frontShare` 在配置中保留并标废弃，未参与状态计算。中英护目镜文字及 `turbine_data.py` 均改为“两端共用”一份容量；256 RPM、总 SU 转换、40 tick 历史与耗汽计算未见本包改动。
- 复核现有原始结果：`build/test-results/test/TEST-com.iksxh.create_nuclear_industry.turbine.TurbineStateTest.xml` 为 9/9；`build/runtime-01e-machine/logs/latest.log:83-84` 为正式机组 10/10；`build/runtime-01e-probe-reverse-far/logs/latest.log:83-84` 为真实网络探针 4/4。机器用例包含单端超过旧半额、两端合计过载和恢复；探针包含外源、回接及两种区块卸载方向。增量 `assemble` 退出码 0 由执行报告记录，本审查未重跑。探针 owner 直接持久化固定 32768 SU，不模拟生产账本加载时的 40 tick 历史归零；正式机组旧 NBT 用例 `ExtensionTurbineGameTests.java:109-145` 只模拟旧控制器邻接 Source，未覆盖旧前/后输出轴的 `Network` 容量恢复。因此这些通过结果不能排除上述阻断。
- 本轮未检查未修改的客户端模型、其他设备或用户存档，亦不把自动化通过视为任务卡规定的客户端人工门；01D/R1 外观人工复测继续保留。
