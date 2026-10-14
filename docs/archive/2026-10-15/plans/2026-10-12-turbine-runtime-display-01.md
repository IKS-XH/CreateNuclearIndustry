# TURBINE-RUNTIME-DISPLAY-01：转子动画只读运行前置

> 由主PM派一位执行者实施、一位独立审查者合并规格与质量窄审。执行者禁止Git写操作、核心文档修改和再派发；通用技能的重复全量流程由AGENTS及治理5.1覆盖。

**目标：** 为已批准ART-TURBINE-10B转子动画提供可靠的成员归属、几何及真实有符号RPM，只提供显示数据，不改变汽轮机运行。

**授权及现状：** 主PM实际read_thread核实美术负责人对话01a11c6b-f440-77c1-9275-5b2dca5cfa0f中的直接用户反馈：新的外壳、转子、输出轴模型通过测试；通入超临界蒸汽后应力正常，但没有转子动画。原10B模型/动画授权已登记于docs/art/ART-TURBINE-10B-COORDINATION.md。RotorBE目前只是静态渲染锚点，原viewRpm与signedRpm返回配置值，不能用于真实停转判断。原流量Controller写者已释放，本必要前置现在可独立实施；不借本任务推进其他主线。

**基线：** 从已验收main d63abfe建立同级E:/MyMC/NewMod/Create_NuclearIndustry-turbine-runtime，codex/turbine-runtime-display。不带入未验收流量代码、锅炉16³候选或美术dirty内容。主目录世界已保存且实际没有游戏Java进程，按既有约定携带main/run/saves完整27文件，原件保留；本卡不写世界或运行配置。

**技术栈及技能：** MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6。实际读取minecraft-modding、minecraft-testing、systematic-debugging、test-driven-development、verification-before-completion，并核本地锁定源码；新增手写注释/Javadoc中文。

## 必须保持的合同

1. 展示记录必须是同一可信样本：控制器坐标及实例世代、成型epoch、前轴/方向/直径/转子数、具体转子成员索引、有效性、采样序号或时刻、有符号实际RPM。不可由客户端邻近搜索、历史FORMED外观、配置256或旧TurbineView拼接猜测归属/运行。
2. RPM只能读取当前合法所属输出轴的Create实际getSpeed，明确其轴向与统一转子旋转坐标之间的符号换算。服务端每轮动力更新完成后读取，不能用控制器账本配置RPM或生成RPM替代实际网速；过载实际0应投影0。合法外源贯通轴旋转如实显示，不修改其传动。
3. 拆件/结构失效、控制器重建、成员替换、缺区块、卸载、世界切换或坏样本撤销可靠显示。没有完整可信成员时不发布可用样本；不强载区块、不让显示发布扫描全世界。实例身份与epoch必须挡住旧世代复活，不保留强世界/BE引用造成泄漏。
4. 客户端只读，收到过期/错误坐标、方向、几何、非有限RPM等完整坏样本时不能沿用旧旋转。同步整体不混合；初始跟踪能得到新样本。采用必要轻量刷新与有限租约（正常采样不慢于5tick，断链租约不超过40个活动客户端tick），真实单人暂停冻结租约年龄，世界/维度/实例切换立即清理。运行投影不写可携数据、不从存档恢复成可靠运行样本。
5. 现汽耗、SU、双端共享应力、生成源/残转恢复、限流/流体能力、红石、成型/辅助建造、模型与渲染器逐字保持。只有必要显示入口/生命周期钩子和只读快照同步可增加；不能为动画改ledger或Create网络。现有历史迁移代码不删除也不扩展研究。
6. 本卡不画转子、不改ART10B资源、TurbineRotorRenderer或美术中立相位辅助；实际HANDOFF交付后由原美术消费者在其授权写集接动画。已通过三件模型不重开，本前置自动证据不代替动画客户端观察。

## 唯一执行者写集

J=src/main/java/com/iksxh/create_nuclear_industry，T=src/test/java/com/iksxh/create_nuclear_industry。

| 路径 | 可写内容 |
| --- | --- |
| J/turbine/TurbineControllerBlockEntity.java | 只读身份/epoch接口及显示登记/撤销钩子；已有服务端运行和流体交易不可改 |
| J/turbine/TurbineRotorBlockEntity.java | 仅可信显示成员绑定、投影同步与只读API；不新增动力/库存 |
| 新J/turbine/display/** | 不可变样本、服务端发布、必要网络/客户端租约及client-only事件入口；不得包含正式动画渲染 |
| 新T/turbine/TurbineRuntimeDisplay*Test.java | 真实解析/世代/几何/时序/活动租约边界，避免只复述实现 |
| 新J/gametest/TurbineRuntimeDisplayGameTests.java；src/main/resources/data/turbine_runtime_display_probe/structure/empty.nbt | 一组最小真实成型/实际轴速/停转/失效探针；只复制已有空模板，不修改其他测试域 |
| docs/reviews/2026-10-12/turbine-runtime-display-01/IMPLEMENTATION.md、HANDOFF.md；build/reports/turbine-runtime-display-01/** | 实际编译API、消费边界、原始验证日志及源/JAR清单 |

其他必要路径先报告主PM，不改主目录代码、美术源码、配置或构建脚本。新树需精确require_escalated编辑/构建；不借主目录写权限绕过隔离。

## 单批执行与验证

- [ ] 核实际动力tick、getSpeed、服务端成员关系与现reactor显示租约经验，列最小结构/同步方案。不要机械复制全反应堆机制。先报具体文件及关键公共API，再实现。
- [ ] 定向RED覆盖配置256与实速0的区别、反转、世代/epoch/完整样本拒绝、停转与真实暂停租约。实现必要可信只读API，业务保护逐字核对。
- [ ] 运行新定向JUnit与必要实际GameTest组，至少证明完整合法成员对应真实输出轴、实速及停止/失效撤销；不启动用户客户端、不写真实世界。不跑原热工/汽耗全矩阵、完整build或旧存档兼容。
- [ ] 最终稳定后一次必要增量assemble，冻结生产源码/编译类/JAR、核ABI和专服client隔离；失败则保留实际记录并仅窄修相关组。美术和主PM复用此同源证据。
- [ ] 交付实际HANDOFF：每个公开类型/方法签名、字段单位/有效性、符号轴向、成员查询方式、暂停/卸载处理及示例，列原消费者可写client路径；不把未实现预案写成已交API。
- [ ] 一次独立规格/质量窄审，无必改后主PM净同步必要前置到main与美术树，保留对方dirty模型/工具及全部既有证据。流量候选独立待视觉复看，不借此合流量或锅炉16³。

**本卡自审：** 现缺口是显示同步前置，而非应力功能修复；权威成员/实速、完整样本、撤销与真实暂停均覆盖。已批准动画和已验模型不重问；没有新玩法决策。最终运行动画仍由美术消费者完成并单独等用户观察。

**精确模板扩卡：** 首次真实启动虽Gradle退出0，Minecraft实际报No test functions were given，不能算测试通过。锁定NeoForge按templateNamespace筛选，因此新探针必须使用独立turbine_runtime_display_probe命名空间及上述唯一空模板路径。执行者仅撤下本卡首次自行新建的旧任务模板，原模板/其他域/构建入口保持；失败日志保留，随后仅运行新三项。
