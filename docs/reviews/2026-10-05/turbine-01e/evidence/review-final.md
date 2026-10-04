# EXT-B-TURBINE-01E 最终定点复审

**规格符合性：通过当前任务代码审查。质量：通过。** 依据当前 `AGENTS.md` 与治理 5.2，首个可发布版本前的旧存档兼容、旧双源 NBT 与旧 RPM 迁移已退出本次范围；此前 `review.md`、`review-r1.md` 的历史格式阻断保留为范围变更前记录，不再作为 01E 验收门。本轮只复核共享总容量、共同过载和当前格式自身保存恢复。

最终 `TurbineShaftPowerSource.java:63-98` 在服务端保存实际本机生成容量时，把对应生成 RPM 写入同一 `Network` 的 `TurbineGeneratedRpm`。这避免用可能来自外部动力网的 `Speed` 误算本机额度。当前格式恢复且服务端账本历史已归零时，`initialize()` 临时令 Create 视该轴为保存时的生成源：原生 `KineticNetwork.addSilently` 先用旧 `AddedCapacity × TurbineGeneratedRpm` 从 `unloadedCapacity` 扣除，再按当前零 SU 计算；随后移除临时零容量源条目并更新网络。没有新字段的历史 NBT 不进入此分支。`TurbineOutputShaftBlockEntity.java:19-36` 仍只有前轴发布机主总 SU，后轴只作结构有效时的内部传播；此前审过的倍率 1 原生连接和共同负载逻辑未变。

`ExtensionTurbineGameTests.java:113-179` 使用当前正式机组实际供汽后保存的前后轴及外部 Create 电机 NBT，清空本机运行历史并重建实体交由服务器初始化。断言保存时 `AddedCapacity>0` 且生成 RPM 正确、恢复首 tick 不超过外源容量、稳定后两端同网且只保留外源。修前隔离日志 `build/runtime-01e-current-reload-red/logs/latest.log:80` 记录 4,227,072 SU 对外源 4,194,304 SU，恰多 32,768 SU；最终 `build/runtime-01e-current-reload-fixed/logs/latest.log:83-84` 为 11/11，通过该用例及两端单独超过旧半额、合计超限共同过载、减载恢复等正式机组用例。`build/runtime-01e-probe-current-final/logs/latest.log:83-84` 为 4/4，覆盖外源停机保留、回接与正反向真实区块卸载。增量 `assemble` 退出码 0 依据更新执行报告；本审查未重跑。受影响源码 `git diff --check` 无空白错误，相关中文注释已与单源及 40 tick 衰减行为一致。

证据限于当前格式实体 NBT 的隔离服务器重建与现有真实网络探针；未打开用户世界或进行客户端视觉验收。任务卡的 01E 共享容量客户端观察、停开/保存重进，以及 01D/R1 外观复测仍须人工完成；本结论不代替该人工门，也不声称旧格式迁移已完成。
