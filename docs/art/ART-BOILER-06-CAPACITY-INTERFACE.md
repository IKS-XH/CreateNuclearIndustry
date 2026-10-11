# ART-BOILER-06 汽容量消费合同

2026-10-11。主PM已读取美术对话原始新需求及用户批准的蒸汽视觉短设计；必要共享前置为[单getter任务](../superpowers/plans/2026-10-11-boiler-steam-capacity-view-01.md)，不新增玩法或再次选稿。

已编译并净同步的唯一公开入口为`BoilerControllerBlockEntity.syncedSteamCapacityMb()`，实际签名`public int syncedSteamCapacityMb()`，单位mB；纯读取既有`steamCapacityView`，不从客户端DEFAULT Settings重算，不增网络/缓存/flag或改变原窗口水位。main `eb196c6`、美术 `a3540b2`，实际ABI、冻结源/制品与独立消费者写集见[HANDOFF](../reviews/2026-10-11/boiler-steam-capacity-view-01/HANDOFF.md)。美术06可接通该实际接口，不再等待推测签名。

该方法仅用于客户端显示，原字段初始化值18000不是已收服务器数据的证明；方法自身不声称已同步或当前成型。生产消费必须在完成原clientPacket读取之后，核同一世界/当前真实owner对象、有效surfaceDescriptor及当前L1同generation/revision/min/max/partition，再读两真实库存与该容量一次。未知/撤销/对象冲突/非正容量不绘制或不作分母；不得调用currentForm、BoilerStructure或按Fill/SteamKind猜库存。服务端容量权威仍为原ledger及配置，不用该显示读数做模拟。

两库存分别为`ledger().steam(BoilerSteamInventoryKind.NORMAL/SUPERCRITICAL)`的真实mB，合计决定浓度，相对占比决定混色；使用宽类型求和/有限归一化，超量安全限幅不改库存。真实汽区来自闭bounds及绝对partition，浓度不改汽区几何或做液位；停热但有库存仍显示。网络最新值不构成流量/年龄租约保证，材质动画不能自称真实流向。

前置原L1的29/29、1/1及窄审复用；这次单字段入口只增量编译、实物ABI/字节码与一次窄审。美术不改共享getter/read包/L1/窗口Fill或原反应堆缓冲，在实物HANDOFF后自行维护06精确客户端/资源卡及一次消费验证和最终视觉门。05B10/10和素材候选仅为其负责人交付状态，未因此合入或代替用户锅炉视觉验收。
