# BOILER-STEAM-OUTPUT-01 主动出汽接入研究

2026-10-14，执行者 `/root/tiered_flow_impl`。本报告形成于只读阶段；用户已明确汽口自身主动输出，无需外置泵。原保存中的15管节点/四SC口和库存事实保留，补泵建议不作为修复结论。北侧SC直贴罐不在本次接入范围。

## 结论与最小写集

可在现有工业事务中接入锅炉汽口主动源，不需要新节点、持久缓存或新动力机制。当前缺口是：`IndustrialNetworkScheduler.tick`仅遍历已登记泵；`IndustrialTopology.search`固定以泵为根；`BoilerControllerBlockEntity.pushSteam`跳过SC，并只向邻格标准能力推送。只删除SC过滤无法接入零库存的工业管。

四个生产路径均在 `src/main/java/com/iksxh/create_nuclear_industry/` 下：

| 路径 | 最小变化 |
| --- | --- |
| `boiler/BoilerControllerBlockEntity.java` | 有效成型且成员可用时，为汽口外向邻工业节点提交仅当tick有效的主动请求；保留NORMAL原生管/邻罐分支；提供带端口身份/过滤代次检查的内部收据入口 |
| `boiler/BoilerState.java` | 一次性蒸汽收据，绑定原账本、kind、物理port、tick和实际扣除mB/HU；只退款该收据未交付部分，不提供任意填汽/加热入口 |
| `industrial/IndustrialTopology.java` | 让原BFS接受汽口位置/外向面根入口；共用原面去重、方向验证、距离/节点上限和非强载读法；泵原入口保持 |
| `industrial/IndustrialNetworkScheduler.java` | Post tick统一消费汽口请求及真实泵；共用同一个World.budget/media、目标绑定、受限目标白名单和许可；没有泵时仍调度汽口 |

现有 `IndustrialTransferPermit` 可作目标侧授权，标准SC能力仍禁运。`IndustrialTransfer`保持原泵缓存语义，不把短收缓存当作临时数字丢弃；`IndustrialFlowBudget`原按节点实收收费足够，不需新增第二本账。没有Mixin、配方、注册、配置或其他设备无泵运输改动。

## 同步收据事务

1. 检查源控制器实例、当前成型/成员、源面、选定池/流体及源代次，读现有 `remainingDrain`（库存资格、共同保压、物理口余额）；检查目标当前绑定、合法介质、全路径加载/阀/实际泵方向与动力，模拟目标接收。SIMULATE不创建扣账收据、不修改mB/HU/口或节点预算。
2. 从原选定池预扣不超过源/路径/接收模拟额的N mB。收据记准确扣除HU和N，并暂占该物理口N额度。受限目标只通过当前有效工业许可执行fill。
3. 收到实际A（0≤A≤N）后一次结算：未交付N−A及其同批真实HU退原池，退原物理口未交付额度。既不换种，也不按默认比焓重新造HU。全拒恢复全部，完整交付不返还；重复结算拒绝。使用try/finally保证同步零接收/许可撤销也退款。
4. 节点只按实际穿过路径的A收费并记介质；未交付部分在源侧同步退回，未进入管段，不应消费节点预算。已有泵短收仍按原输入段抽取量/输出段实收量计费，二者语义分别保留。

收据必须在同一服务端同步调用结束前闭合，不能跨tick持有、持久化或成为新隐藏库存。退款属于原账本撤销，不调用会因换滤器/端口失效而拒收的公开fill；真实交付后源扣A及原池真实HU。生命周期在执行前重核；SIMULATE期间源/目标/路径失效不提交。若执行许可已撤销但源预扣已完成，退款仍返回同一原账本；不能写到新BE、新滤池或下一tick预算。现行受控目标按IFluidHandler同步合同交付，本研究未承诺任意第三方在fill内部重入破坏源控制器的非正常行为。

## 共管、泵与失效

- 汽口请求由原Controller合法tick提交，调度时要求请求tick=level.gameTime且实例/代次仍有效；未提交口不沿用前tick请求。世界卸载清请求，处理后移除，不新增持续强引用源登记。
- 汽口不作为工业管节点；其共享4096额度由原BoilerState按物理口扣账，与标准NORMAL抽取、邻罐主动输出和现有工业泵被动抽取共享。路径额度仍由原节点BlockPos身份计费，多个主动口及泵没有独立的共边预算。
- 多口请求稳定排序并轮转；与原泵调度交替先后，避免一种来源长期占尽共边额度。两组都在同一Post tick、共用原介质记录中运行。
- BFS端点不作桥，排除本根口，visited/面去重阻止环无限搜索。实际工业泵节点仍必须有动力、方向与入口合法，管路跨停泵或反向泵时拒绝；主动源不能把它当普通管绕过。其缓存也纳入混介质检查。
- 沿路端点当前库存、泵缓存和World.media使用与原泵一致的单介质判定；NORMAL/SC有真实存量时共管拒混，不自动降级。源口纯过滤，只取被选池；源为空不能借另一个池供给。
- 保留原getChunkNow/缺实体拒绝、Create多方块罐成员和真实controller绑定、旧句柄永久失效。断管、关阀、卸载、换滤器或锅炉成型失效终止当前请求；恢复后靠新的合法请求/路线重新交易，不能用缓存句柄继续取液。

## 建议测试范围

精确新增/扩展：`src/main/java/com/iksxh/create_nuclear_industry/gametest/BoilerIndustrialActiveOutputGameTests.java`、`src/test/java/com/iksxh/create_nuclear_industry/boiler/BoilerDualSteamInventoryTest.java`。新真实域可用 `templateNamespace=create_nuclear_industry_industrial`复用industrial_empty，不新增模板或注册玩法对象；打包脚本只排除新GameTest class及其内部类。

锁定NF后续核实：筛选实际按templateNamespace，且BeforeBatch晚于模板放置；因此按PM批准后备，实际复用原industrial筛选域组合新例与原23代表，未建立别名、复制模板或增加测试框架。后续同次审查又发现组间先后与组内首位共用cursor会饥饿，最终按每组实际先行机会推进组内首位；已用十二帧真实实收关闭该项，详见实施报告。

- 先RED：有效成型、合格SC存量、保压有余量、合法入口、15节点四口并管→真实汽轮机，无工业泵时原生产不交付。不能用空NORMAL池或不成型夹具假装复现。
- 收据JUnit：两种池的准确mB/HU、短收/全拒与口额度返还、重复结算、模拟零写、原kind/port/tick绑定；改变过滤不能把退款写另一池。
- 真实域：NORMAL和SC无泵入有限Create罐；15节点四SC口至汽轮机代表；多主动口共8192节点、主动+泵同物理口4096、混介质零交付、关阀/断管/恢复、停泵/反向泵不可绕、换滤器/拆口/卸载不续取、SC普通Create管禁运与NORMAL原生路线保留。
- 真实短收可仅在新测试类内给真实Create罐的tankInventory装执行限量包装，重新发布能力；锁定字节码已核该字段为FluidTank及handlerForCapability同库选择。测试世界恢复原库存对象，不能改生产罐或给SC未知端点授权。这样可证明真实调度短收退款，不能只断言返回数字。

仅必要账本JUnit、新真实域及受影响原工业代表；不扩大管理员/美术全量。自然保存exit与断言结果分别记录。最后一次必要assemble需保原包并排除新测试类载荷；本只读研究没有执行这些检查。

## 证据和技能

复用原 `snapshot/**`、`snapshot-manifest.json`、`05-final-scene-evidence.json` 和原JAR身份，不重新运行世界。新增 `build/reports/boiler-steam-output-01/07-active-study-create-tank-bytecode.txt`：对锁定Create6.0.10-280 slim.jar执行只读javap，核SmartFluidTank同步回调、tankInventory及私有能力选择入口。实际重新读取AGENTS和最新窄修卡，使用minecraft-modding/testing、systematic-debugging与brainstorming；栈保持MC1.21.1/Java21/NF21.1.219/Create6.0.10-280。没有生产/测试/构建/配置/世界/Git写或测试打包。
