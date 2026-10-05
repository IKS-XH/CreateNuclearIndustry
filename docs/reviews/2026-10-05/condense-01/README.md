# 蒸汽冷凝回水01候选交付

**状态：实现、定向验证与独立审查通过，待人工闭环联调。** 功能提交`fdfba38`；用户确认顶部冷源与全部首轮参数，本批未宣称人工通过，候选功能尚未合入main。

既有换热器背面橙口输入汽轮机排出的蒸汽，正面蓝口输出水。保持同向直列、最多16台、只从两端接管，各台54mB/t、蒸汽/水各4000mB，默认1:1回收。直列库存共享，工质模式互斥，各台独立结算；冷凝不提供锅炉热或Create应力，原核热功能保留。

每台顶部必须直接接触水源、雪块、冰、浮冰或蓝冰。雪/冰累计冷凝100000mB后融水，浮冰900000mB后融水，水源100000mB后蒸发；蓝冰持续。冷源世界变化不增减机内回水，停机不消耗冷源，进度正常保存。8项数值已进入[SERVER配置](../../../server-config.md)，默认满流量水阶段约93秒。

| 验证 | 结果 | 证据 |
| :--- | :--- | :--- |
| 受影响账本JUnit | 22/22，退出0 | [核热16项XML](./evidence/HeatExchangerStateTest.xml)、[冷凝6项XML](./evidence/CondensationStateTest.xml)、[日志](./evidence/junit.log) |
| 真实机器GameTest | 8/8，正常退出0 | [日志](./evidence/gametest.log) |
| SERVER实际加载 | 默认8字段、非默认333‰与容量/缩容断言通过，临时值finally恢复 | [默认配置](./evidence/server-config-default.toml)、[实现报告](./evidence/implementation.md) |
| 增量assemble | 退出0 | [日志](./evidence/assemble.log)、[制品记录](./evidence/artifact.json) |
| 联合规格与质量审查 | 无发现，复用已存证据未重跑 | [审查报告](./evidence/review.md) |

真实Create储罐→泵管→三台冷凝→泵管→水罐逐tick总量守恒，4000mB全部回水。定向用例覆盖五种冷源、融水/蒸发阶段边界、无效位置/流水/含水方块、堵塞恢复、逐台额度、混列拆分、当前格式恢复及代表性核热18级供热。为缩短验证，冷源测试把当前格式进度设至阶段边界，默认完整寿命不在自动测试里重复等待。

[修前失败](./evidence/baseline-failure.log)确认原换热器拒收蒸汽；[首次管泵失败](./evidence/first-fixture-failure.log)及[夹具调度异常](./evidence/fixture-scheduling-failure.log)保留。将逐tick回调注册移到初始化后守恒断言保留并通过；不以隐藏失败替代定位。最终暂存检查另清理了一行测试EOF空白，无语义变化，复用同一运行证据。

重启候选目录`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`的`./gradlew.bat runClient`，按[合并联调清单](./manual-checklist.md)检查锅炉→汽轮机→冷凝→给水，以及01F新门槛/效率/周转。护目镜两处小显示问题只顺带观察，不另设测试门。新SERVER字段由NeoForge按默认补齐，本批未覆盖开发配置或编辑用户世界；完整客户端回路、画面和跨区块冷凝卸载恢复仍没有本批人工证据。
