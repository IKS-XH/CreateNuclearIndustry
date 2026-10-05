# 汽轮机01F候选交付

**2026-10-05后续现场整改：** 01G已修复零SU持续转动并通过自动与现场验证；用户随后明确整体所有测试项通过，01F效率/周转和早前外观范围同时完成验收，见[联合记录](../condense-01/ACCEPTANCE.md)。

**状态：实现、定向验证、独立审查及人工联调全部通过，已合入main。** 用户2026-10-05明确“所有测试项都通过了”，新门槛、效率曲线、实际排汽计量与微周转已获确认。此前01E原算法通过记录按原语境保留。

取消入口/出口大储汽罐，改为全机唯一的微量排汽周转空间，默认小/中/大54/108/216mB。按真实排汽流量计一次动力，SIMULATE不占量、不发电；堵塞时缓存满后拒收并保留残留。两端仍共享一份总容量。

默认40tick平均流量低于额定30%时只耗汽、不贡献动力；达到30%时为0.5倍，随后线性提升至三档最高1.2/1.5/1.8倍。大型额定流量216mB/t，转速保持256RPM。参数均进入[SERVER配置](../../../server-config.md)，开发实例配置已备份并定点更新。

| 验证 | 结果 | 证据 |
| :--- | :--- | :--- |
| 定向账本JUnit | 7/7，退出0 | [XML](./evidence/TurbineStateTest.xml)、[日志](./evidence/junit.log) |
| 汽轮机真实GameTest | 11/11，正常退出0 | [机器域日志](./evidence/machine.log) |
| 非默认SERVER实载 | 1/1，正常退出0 | [日志](./evidence/config.log)、[隔离配置](./evidence/nondefault-server.toml) |
| 增量assemble | 退出0 | [日志](./evidence/assemble.log)、[制品哈希](./evidence/artifact.json) |
| 联合规格与质量审查 | 无阻断项 | [审查报告](./evidence/review.md) |

真实机器域包括Create源储罐、机械泵、输入管路及原生主动排汽管路：稳定20tick目标实收2160mB，达到中型108mB/t；源扣量等于目标增加量加周转残留。低流量、堵塞恢复、护目镜状态及共享Create容量也在同域验证。非默认配置实际改变三档倍率、额定流量、周转tick数和门槛并进入机组运行。没有重跑无关全量测试。

修前[代表性失败](./evidence/baseline-failure.log)确认仅进汽未排汽即可产生SU；实施中[时序失败](./evidence/timing-failure.log)发现控制器过早推进当前tick导致稳定少算1/40，已修为只补已结束tick、真实出口再记录当前tick。[实现报告](./evidence/implementation.md)记录其它夹具调整及最终命令。只读可行性依据保存在[调查报告](./evidence/flow-feasibility.md)。

本批[三项精简手测](./manual-checklist.md)已通过，01D/R1外观待确认项同样解除，无需重复启动客户端。主目录runClient已包含现行实现；候选配置备份仍留在`build/reports/extension/EXT-B-TURBINE-01F/run-config-before-01f.toml`，本轮未编辑世界或覆盖配置，不研究旧存档兼容。
