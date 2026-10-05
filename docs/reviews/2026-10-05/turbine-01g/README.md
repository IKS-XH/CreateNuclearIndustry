# 汽轮机01G断汽停机整改

**状态：修复、定向验证与独立审查通过，待现场复测。** 功能提交`8273aff`；本批修复[01G合同](../../../superpowers/plans/2026-10-05-ext-b-turbine-01g.md)中的停机生命周期缺口，候选功能未合入main。

用户确认无其他动力源时，撤去蒸汽输入后外接轴或转速表仍持续有转速。Create周期校验可能先把汽轮机源轴服务端速度直接清零，随后汽轮机更新误判为0→0，跳过原生拆源与下游停速通知。定向控制校验相位后，修前真实机器发现SU/服务端RPM已零而源network仍存在；这条路径解释客户端可能长期保留旧转速，不等于已复现用户客户端画面。

最小修复只在本机原有容量归零、无上游source、仍有network且速度已被提前清零时恢复上次生成RPM作原生停机传播的前值，再由Create正常拆源/同步。没有强制清零整网；同速外源的容量与转速保持。40tick平滑、30%门槛、三档倍率、周转量与配方均不改。

| 验证 | 结果 | 证据 |
| :--- | :--- | :--- |
| 固定竞态修前真实机器 | 新增项失败，其余11通过，退出1 | [日志](./evidence/race-before-fix.log) |
| 修后真实机器＋动力探针 | 17/17，正常退出0 | [日志](./evidence/power-network-final.log)、[退出码](./evidence/power-network-final.exit-code.txt) |
| 增量assemble | 退出0，1秒增量 | [日志](./evidence/assemble.log)、[制品记录](./evidence/artifact.json) |
| 本地锁定源码分析 | 只读，未运行测试 | [诊断](./evidence/diagnosis.md) |
| 联合规格/质量审查 | 无发现，复用同一实现的运行证据 | [审查](./evidence/review.md) |

17项涵盖真实供汽停机及再次供汽、两端与外接轴归零、唯一容量重新登记、后端`sendData()`通知计数，以及同速外源保留。反射仅控制测试对象的`validationCountdown`以固定相位，不改全局配置。前三次自然断汽相位未复现的记录也保留，它们只证明对应最终服务端状态，不能证明客户端收到停速包。[实现报告](./evidence/implementation.md)记录准确写集与命令。账本未改，不重跑JUnit或无关冷凝/外观域。

从`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`重启`./gradlew.bat runClient`，在原故障装置做一次撤汽→等待残余供汽/周转与窗口衰减→再次供汽，确认外接轴动画和转速表停止后可恢复。此项已并入[联合清单](../condense-01/manual-checklist.md)，其余未确认项保持待验收；本批未启动客户端或编辑用户世界，不记录人工通过。
