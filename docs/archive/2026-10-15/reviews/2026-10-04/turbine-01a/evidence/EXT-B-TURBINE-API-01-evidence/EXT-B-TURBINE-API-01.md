# EXT-B-TURBINE-API-01 双轴真实 Create 探针

- 基线：`fd19c1f399127eb2b97f1d1660fe25cf662a26ef`；唯一工作树：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。
- 实际技能：`minecraft-modding`、`minecraft-testing`，并按任务计划使用 `superpowers:executing-plans`；所有示例按本仓库 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280 核对。
- 写入：新增 `turbine/TurbineShaftPowerSource.java`；新增 `gametest/turbine/` 内测试专用注册、方块实体与 3 个 GameTest；新增独立 `data/create_nuclear_industry_turbine_probe/structure/probe_empty.nbt`。未修改共享入口、正式设备、核心文档或 Git。

## 结果与接口

`TurbineShaftPowerSource` 继承 Create 的 `GeneratingKineticBlockEntity`。子类分别提供当前服务端单轴 `assignedSu()` 与带符号 `assignedRpm()`；基类按 `assignedSu / abs(assignedRpm)` 返回单位 RPM 容量，归零时不发电。每 tick 读取权威份额，只在 SU/RPM 改变时调用 `updateGeneratedRotation()`，避免稳定状态重复通知网络。正式 C 应复用该类，让唯一 owner 按总 SU 固定分配前后份额，任一轴不领取未连接端份额；owner 必须逐次验证结构、红石和所有成员 chunk 可 tick。探针的固定布局、32768SU、128RPM、注册 ID 与测试 owner 仅属 B，不是正式机组参数接口。

探针通过 `@GameTestHolder("create_nuclear_industry_turbine_probe")` 隔离测试域。`TurbineProbeContent` 仅在系统属性 `neoforge.enabledGameTestNamespaces` 包含此域时注册方块与实体；没有探针物品/创造栏项。注册方块必须在 `RegisterEvent` 内构造，静态构造会碰到冻结注册表。

最终命令：`./gradlew -PgameTestNamespace=create_nuclear_industry_turbine_probe -PgameTestDirectory=run/turbine_probe runGameTestServer --console=plain`。最终原始日志：`EXT-B-TURBINE-API-01-final-gametest.log`；退出码 **0**，**3/3 required**，`All dimensions are saved`，`Game test server shutting down`。隔离目录由本任务创建，无用户存档或客户端进程被处理。

| 真实断言 | 结果 |
| --- | --- |
| 双端未连通：前后 Create 网络各 16384SU；联轴同网合计 32768SU；拆网再各 16384SU | 通过 |
| 外部同速 Creative Motor 并网：容量仅加其自身 SU；红石停机与账本断供撤销本机 32768SU，外源容量保留 | 通过 |
| 前端/owner 与后轴跨 chunk，机身再跨两 chunk；撤销票据后实际观察前端 chunk 仍在而后轴和远段机身 `getChunkNow == null`，远侧仍加载 Create 网络只剩外源 SU | 通过 |
| 再撤远侧票据直到四个机身 chunk 与电机 chunk 均 `getChunkNow == null`；重新加载后外源 + 32768SU 恰好恢复，没有 `unloadedCapacity` 幽灵或重复登记 | 通过 |

锁定 Create 源码 `RotationPropagator.propagateNewSource()` 对异向/异速接入执行原生冲突处理；本基类未覆盖传播器或冲突规则。Create 源码 `KineticNetwork.calculateCapacity()` 会将登记的单位 RPM 容量乘生成速度，因此正式轴端绝不能返回原始 SU。GameTest 的真实票据显示，在远侧电机保持加载时，前端 chunk 可作为邻域边界继续存在，但后轴与部分机身已经实际卸载；这两种状态不能混称。正式 C 应在任一成员 chunk 不可 tick 时关闭份额，并复用本测试对远侧网络和重载缓存的断言。

## 诊断记录

首轮编译因探针 `BaseEntityBlock` 的 `codec()` 要求失败，已改为 `Block implements EntityBlock`。首轮 GameTest 的 Gradle 退出码虽为 0，但探针静态构造方块导致模组加载失败、没有执行断言；该轮不计通过。第二轮网络场景 2/2 通过。跨 chunk 首轮要求四个机身 chunk 在远侧电机保持加载时全部消失，未符合 Minecraft 邻域票据行为；远距复验确认前端两个 chunk 保留、后轴和远段机身两个 chunk 已实际卸载。断言据此修正为可达的局部卸载边界，随后 3/3 通过。最终基类减少稳定状态通知后再次定向运行 3/3 通过。没有清理旧隔离测试存档或扩大到无关测试。

`git diff --check` 只报告并发生成的 `logs/debug.log` 与 `logs/latest.log` 尾随空格；本任务新增文件未出现在该只读差异命令中。并发 A、PM 与资产任务的既有/新改动未修改、未暂存。B 无 Git 写操作。
