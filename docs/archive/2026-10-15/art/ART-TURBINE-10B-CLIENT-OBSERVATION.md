# ART-TURBINE-10B 客户端建模验收与旋转缺口

2026-10-12用户直接报告：“新的涡轮机外壳、转子、输出轴的建模都通过测试了，但是向涡轮机通入超临界蒸汽没有转子旋转动画，但是应力可以正常输出”。

按原文记录外壳、转子、输出轴的建模外观人工通过，不再重复要求这三项建模测试；用户没有报告旋转动画通过。旋转、启停、方向、暂停/恢复和拆除撤销门保持，不补写未逐项报告的观察窗邻接或动力细节。

美术负责人只读核当前codex/art-studio、HEAD 2ae83b9：TurbineRotorRenderer.render只按MACHINE_FACING进行yaw旋转并提交partial，没有读取RPM或累计角度；TurbineRotorBlockEntity仍纯静态渲染锚点。这是ART10B旋转消费此前尚未接入，不是已绑定后只在超临界模式发生的失速。原中立OutputRenderer/Clock/Phases五源和15例证据保持，原R1素材26来源/安装一致及材质审查复用。

ART-TURBINE-10B-COORDINATION已确认TIERED-FLOW写者释放，但仍明确前置未实现/HANDOFF。已将用户原消息、静态路径证据和本次建模验收告知主PM，请接续必要可靠owner/epoch/几何/有效性/实际有符号RPM只读接口。服务端、RotorBE/Controller、同步及真实运行资格不能由美术自行写入；不以裸ledger设定RPM或客户端轴链猜归属替代。

实际HANDOFF后由原唯一客户端消费者完成RotorRenderer绑定、必要受影响检查和唯一增量assemble，由原reviewer同轮追加组合审查。素材、配方、汽耗、SU、动力和管网参数不因这次动画接续改动。

本次环境仅主目录与指定visualizations目录可写；同级art-studio写入须走精确授权提权，不能将实现转写主目录规避隔离。本记录只写docs/art，不操作Git、世界或运行配置。
