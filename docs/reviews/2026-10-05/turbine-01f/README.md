# 汽轮机01F候选交付

**2026-10-05后续现场整改：** 用户反馈无外源断汽后持续零SU转动，已由[01G](../turbine-01g/README.md)修复原生校验与停机传播的时序缺口，17项定向GameTest、审查与撤汽/恢复现场复测通过。下方01F原证据保留，效率参数未改；本批效率与周转的整体通过范围正在集中核对，不因01G通过而扩大验收。

**状态：实现、定向验证及独立审查通过，等待人工复测。** 需求、数量与曲线均已获用户确认；此前三档功能手测不覆盖本批新算法。候选尚未合入main；2026-10-05用户要求护目镜显示修复后继续下一步，现进入冷凝回水准备，新效率/周转行为留待后续闭环联调，未记为人工通过。

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

从`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`重启`./gradlew.bat runClient`，按[三项精简手测](./manual-checklist.md)复测本轮新行为。没有启动或改写用户世界，未关闭当前客户端；开发配置原文件备份留在候选`build/reports/extension/EXT-B-TURBINE-01F/run-config-before-01f.toml`。01D/R1外观未确认项按原记录保留，不开展旧存档兼容研究。
