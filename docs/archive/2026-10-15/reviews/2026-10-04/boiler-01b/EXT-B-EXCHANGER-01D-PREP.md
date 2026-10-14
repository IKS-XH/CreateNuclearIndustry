# EXT-B-EXCHANGER-01D：列式核换热器只读准备

只读静态核查，HEAD `9e8690b`。已对照`AGENTS.md`、治理5.1、换热器方案/01A实现合同，以及此前已实际读取的`minecraft-modding`和`minecraft-testing`技能；本轮未运行Gradle、测试服或客户端，未写源码/docs、未执行Git写操作。

## 关键结论

每台`HeatExchangerState.CAPACITY`为4,000mB，热罐和冷罐各自上限4,000mB；链式容量无需放大本地常量，N台自然合计每种流体`4,000×N`。因此5×5锅炉底下3×3的9台若都接成列，总热液/冷液容量各36,000mB；16台是64,000mB，但16台最大值仍是待确认参数。现有每台仍独立发布/消耗热量，默认18HU/t（峰值流体转换36mB/t）。

合并/拆分可保留每个BE自己的ledger和NBT，不搬库存或热储备：每台仍存热液、冷液、HU储备、流量小数、最后tick和绝对余热期限。列端聚合`IFluidHandler`可把外部填充/抽取分配到这些原账本，但这单独不够支持列内换热：本地`HeatExchangerState.tick()`按本机`min(hot, 4000-cold)`限制转换，因此中央机本地热罐空时看不到别机热液，本地冷罐满时也会错误停机。必须另加列级同步换热事务：工作成员仍只用自己的已付reserve/档位预算，在全列热库存与冷罐总余量内预检，从任意成员扣热液并向任意成员加同量冷液；只有提交成功量才补入当前处理机的reserve。这样库存成员分布与耗热机器可以不同，库存不搬迁，老NBT仍有效。拓扑变化只重算成员视图，拆列后本地账本自然留在原机，不复制也不丢失。

## 现状与限制

- `NuclearHeatExchangerBlockEntity.fluidPort(side)`目前仅拒绝`UP`，其余五面返回相同两罐handler；水平`FACING`只作视觉朝向。`Hot`只可输入，`Cold`可抽取，但输入/输出方向未限制，不能满足前/后单向合同。
- 流体能力在`HeatExchangeContent.capabilities`注册，每次调用传入`side`，可在现有注册点实施方向门控。Create共享填充接口已存在：`SharedFluidReceiver.Limits`用对象identity区分库存和流量scope；`SharedFluidFillPlan`按共享库存累计并支持重新分配；Mixin只在`FluidNetwork.tick()`单次方法调用期间为模拟填充建立临时计划，执行调用原样透传。
- 聚合入口实现`SharedFluidReceiver`并给整列热罐一个稳定库存identity、汇总剩余空间，可复用已有mixin处理Create同一次管网分流模拟。原每成员本地ledger仍在Execute时兜底容量。该计划不跨两个独立`FluidNetwork.tick()`调用预约空间；若同tick能有多个不同管网/泵同时灌同一入口，来源先抽后填可能出现执行期容量变化，应限制单入口拓扑或补这一边界验证。新line identity须在一次Create调用期间稳定，并随拓扑世代变化失效。
- 现有BE`canTick()`只检查本实体所在区块/实体tick门；`pauseHeat()`会撤销发布并失效本机能力，账本以保存的世界tick/绝对deadline在恢复时保守散热。列式合同要求处理端点聚合前确认所有成员已加载且可tick；扫描不可强加载，邻接位置跨未加载区块时须保守暂停，不能把未知区块误判成列端。活动状态检查否则会让活跃成员单独继续工作。
- 方块扳手旋转通过`setBlock`更新同一个方块state；`onRemove`只在方块ID改变时使本机能力失效。新朝向决定管口、成员关系和列端，旋转及相邻成员放拆都必须刷新整列成员和外侧入口/出口的缓存能力，旧epoch句柄应拒绝读写。

## 库存与搬迁合同证据

`savePortableData()`返回该机自己的ledger；普通BE保存也写同一ledger。`getDrops()`只把当前BE快照复制进物品`CustomData`，不清空实体；`setPlacedBy()`在新BE加载该份快照。潜行扳手沿用BreakEvent后生成单件携物掉落，随后销毁方块；一般`onRemove`不再额外生成另一份。故目前单台拆放可保留已存流体和已付余热，不通过迁移改写NBT。

针对列实现，建议仍一机一件、一机一份本地状态：抽走中间成员后，两侧各自重算为独立列，被抽走机器物品只带它的4,000mB级本地两罐和本地reserve；其余机器状态不动。禁止把全列快照写入每个成员，否则每件携物会复制库存/热。旋转造成的拆列/并列同理只重算成员视图。

## 推荐的最小实现写集与验证边界（供PM冻结后派发）

可能写集限于：`NuclearHeatExchangerBlockEntity.java`（侧向门控、列端聚合handler、成员活动检查）、`NuclearHeatExchangerBlock.java`（旋转/邻接拓扑变化时整列能力失效）、新增窄范围`heat/HeatExchangerLine.java`（同向轴向发现、唯一入口/出口、长度限制、无加载扫描及共享换热事务）、`HeatExchangerState.java`（将本地源/冷余量运算改为接受列事务预算/回调，保留单机ledger与每机reserve；单机作为一成员线）。测试至少改`HeatExchangerStateTest.java`并在`ExtensionHeatExchangerGameTests.java`覆盖真实列内跨成员取热、跨成员排冷。聚合Handler仍实现现有`SharedFluidReceiver`；单列一次Create网络的共享容量可复用现有计划/Mixin，但不跨独立调用预约。旧NBT不必迁移；需确保成员热液/冷液各自分散保存，拆分以后不用搬账。

必要边界：①正面只抽冷、背面只填热、上下和两侧拒绝；②同向连续3台聚合容量12,000mB，热冷可分散在成员间且总量守恒；③特意令中央工作成员本地热量为0、只在其他成员放热液仍能获得全列热输入，并令中央本地冷罐满、只在其他成员留空间仍能提交换热；④各处理机只消耗自己的reserve，已处理质量与已付HU一致，列空间/来源不足时不部分扣账；⑤同一行相邻同向合并、侧邻不合并，反向/拆除/旋转立即更新端点；⑥单机携物旧NBT round-trip，拆列不搬账；⑦跨区块一成员不活动/未加载时全列拒绝管输/换热、不加载区块，恢复不复制或免费增加热；⑧现有共享填充同一`FluidNetwork`多分支守恒，并界定多个独立网络执行期边界。

数量上限建议由PM与用户确认后再冻结；9台是5×5底部3×3铺设直接需要，16台会允许未来更长列但当前缺少明确玩法依据。以上不把16写成已批数值。
