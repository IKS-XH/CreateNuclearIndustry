# EXT-B-TURBINE-01G 实现交付报告

## 基线与根因

- 候选工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`
- 分支：`codex/ore-acquisition`；执行时 HEAD：`3075a68cc814f26ec603c31bbc74dabf23e0ba34`
- 技术基线：MC 1.21.1、Java 21.0.7、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。
- 只读核对 Create 锁定源码：`C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280/23e1219501c0debfa0bb56c30ef8e0193341aae5/create-1.21.1-6.0.10-280-sources.jar`。
- 该源码 JAR 的 SHA-256：376DE15CA5ACF720106A075CA4EB2EF53E63E0E5D9EC93523A1ABBDD0F9F0CB4。
- 根因在 Create 源网络生命周期与汽轮机逐 tick账本同步的先后次序。`KineticBlockEntity.tick()`先运行 `validateKinetics()`；无上游 `source` 且 `getGeneratedSpeed()==0` 时，Create 直接把服务端 `speed` 字段写成 0。汽轮机随后同步账本停机时，`GeneratingKineticBlockEntity.updateGeneratedRotation()`读到 `prevSpeed=0` 和新生成速度 0，因而跳过 `applyNewSpeed()`，没有调用 `RotationPropagator` 拆除本机作为源的网络。修前真实 GameTest 记录到账本 SU=0、轴实际服务端 RPM=0，但前轴仍持有 Create network ID。
- 普通下游在后续验证中也可能先 `removeSource()` 将自身 speed 清零，再调用 `detachKinetics()`；Create 的 `RotationPropagator.handleRemoved()`因 speed 已为 0 而提前返回。此路径缺少原生源移除传播与下游 `sendData()` 调用，可能使客户端仍保留此前收到的转速数据。测试探针统计了下游 `sendData()` 调用以验证修复走过传播器清理路径；没有启动客户端，也没有将该计数等同于客户端已收到或显示数据包。

## 最小修复

`TurbineShaftPowerSource.tick()`在同步账本变化前保存前一次 SU/RPM。仅当前次本机 SU 大于 0、当前 SU 为 0、轴无上游 source、仍有网络且 Create 已将理论速度清零时，临时恢复前次本机生成 RPM，再调用原有 `updateGeneratedRotation()`。这样现有 Create `applyNewSpeed(nonzero, 0)`与 `RotationPropagator`可以正常拆源并通知下游。存在 `hasSource()` 的轴继续使用 Create 原生外源分支，只撤销本机容量并保留外源转速。

改动不触碰 40tick 窗口、流量门槛、倍率、尺寸、配方或动力参数。新增手写说明均为中文，并解释父 tick校验、账本停机同步和 Create 网络拆源关系。

## 失败与成功证据

修前先运行了未固定校验相位的真实双端流量场景。三份日志分别记录正常通过、仍未复现，随后将校验计数固定到真实 SU 归零后的下一 tick，建立确定性失败：

- `repro-before-fix.log` / `repro-before-fix-exit-code.txt`：12/12 项通过，退出码 0。
- `repro-phase-before-fix.log` / `repro-phase-before-fix-exit-code.txt`：12/12 项通过，退出码 0。
- `repro-phase2-before-fix.log` / `repro-phase2-before-fix-exit-code.txt`：12/12 项通过，退出码 0。
- `race-before-fix.log` / `race-before-fix-exit-code.txt`：新增真实汽轮机回归按预期失败，退出码 1；日志显示 `network=3686793279367888841 source=null 本机SU=0.0 实际RPM=0.0`。同一轮其余 11 项通过。

修复后的定向命令：

`./gradlew.bat -PgameTestNamespace=create_nuclear_industry_turbine,create_nuclear_industry_turbine_probe -PgameTestDirectory=build/runtime-01g-final runGameTestServer --console=plain`

原始输出：`power-network-final.log`；退出码：`power-network-final.exit-code.txt` 中的 `0`。结果：17/17 项 GameTest 通过，其中包含真实汽轮机断汽后账本窗口/SU归零、机内双轴及两端普通轴归零、无网络源残留、恢复供汽后重新生成唯一容量；控制校验相位触发前轴无网络后，探针确认后轴 `sendData()` 计数增长；同速 Creative Motor 场景确认汽轮机容量撤销后外源容量和 128 RPM 转动保留。

增量构建命令：`./gradlew.bat assemble --console=plain`

原始输出：`assemble.log`；退出码：`assemble.exit-code.txt` 中的 `0`。`git diff --check` 未报告空白错误。

## 技能与范围

实际读取并使用 `minecraft-modding`、`minecraft-testing` 与 `superpowers:systematic-debugging`。测试使用锁定版本与真实 Create KineticNetwork/RotationPropagator，没有修改依赖、全局配置或 mixin。

实际源文件写集仅四项：

- `src/main/java/com/iksxh/create_nuclear_industry/turbine/TurbineShaftPowerSource.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionTurbineGameTests.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/turbine/TurbineKineticProbeGameTests.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/turbine/TurbineProbeShaftBlockEntity.java`

原始日志、退出码与本报告均写在 `build/reports/extension/EXT-B-TURBINE-01G/`。没有修改用户世界、测试模板、任务卡或治理文档，没有执行 Git 写操作，也没有停止用户客户端。工作树原有 `logs/debug.log`、`logs/latest.log` 与 `tools/art-assets/__pycache__/` 保留。

## 尚待确认

自动测试验证了服务端源网络拆除和下游发送更新的调用路径。客户端轴动画/转速表显示及现场无外源断汽后的行为仍需用户复测；此报告不记录人工验收通过。旧存档兼容未测试。
