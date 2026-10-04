# 汽轮机准备证据

**性质：** PM整理的只读核查记录，不是功能交付、运行探针或验收。基线 `01eaa52`；下一步方案见[三档汽轮机建议](../../../superpowers/plans/2026-10-04-supercritical-steam-turbine-proposal.md)。

两名执行者分别完成 `EXT-B-TURBINE-PREP-API` 与 `EXT-B-TURBINE-PREP-PARTS`，均使用 `gpt-6-luna/high`。已读取AGENTS、治理5.1、活动计划及 `minecraft-modding`、`minecraft-testing` 技能。只返回消息，未改代码/核心文档、未执行Git写操作、构建或测试。PM随后复核材料JSON、蒸汽注册与容量源码，并核算建议规格和配方数量。

## 1. Create动力源

锁定源码：`C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280/23e1219501c0debfa0bb56c30ef8e0193341aae5/create-1.21.1-6.0.10-280-sources.jar`。

- `GeneratingKineticBlockEntity.updateGeneratedRotation()`：源码89～116行，读取生成速度、接入/解绑网络、通知容量并同步；启动、停机与外部源接管逻辑在118～168行。实际类名为Generating，不是Generated。
- `KineticBlockEntity.calculateAddedStressCapacity()`：185～189行；`KineticNetwork.calculateCapacity()`：133～147行，按各source的单位转速容量乘生成转速绝对值求和，速度处理见165～175行。实现不得把总SU直接当作单位RPM容量再次乘128。
- `PoweredShaftBlockEntity`：23～68、108～126行，是真实生成源范例；不同轴各登记固定半额时，同网相加为总量，分网各半，不自动转移未接的一端份额。
- `KineticBlockEntity.removeSource/setNetwork/remove` 和 `RotationPropagator.handleAdded/handleRemoved/propagateNewSource` 涉及断供、拆并网、卸载和外部动力竞争。静态源码不足以证明没有遗留source或unloadedCapacity。

API执行者倾向首期单轴以缩小风险；PM为保留多主轴容量分配路径，在方案中提出双轴固定均分，并把真实探针列为实现前置。该取舍待用户确认，当前没有双轴运行证据。

Create原生Steam Engine读取其储罐锅炉状态/效率，不按本模组蒸汽流体身份耗汽，因此只能参考其动力源模式，不能直接承担超临界汽轮机的流体账本。

## 2. 蒸汽与现有热源

- `src/main/java/com/iksxh/create_nuclear_industry/content/BoilerContent.java:68` 起注册且只注册 `supercritical_steam`；普通 `steam` 尚未注册。
- `src/main/java/com/iksxh/create_nuclear_industry/boiler/StorageOnlySteamFluid.java` 为无桶、无落地方块的容器/管网Fluid，但 `getFluidType()` 当前绑定超临界类型；新增普通蒸汽时不能原样实例化后仍返回超临界FluidType。
- 锅炉控制器已有独立物理口预算、能力访问和主动排汽；原生管路能力仍须按汽轮机真实结构验证。`BoilerState` 当前水/汽容量常量各16000mB，符合“尚未实现规模容量”的现状记录。
- 现行锅炉每换热段18HU/t、1HU＋1mB水→1mB超临界蒸汽，最多9段即162mB/t；核换热器默认0.5HU/mB、每台峰值18HU/t。方案三档流量据此推荐，不代表数值已获批准。

## 3. 材料可达性与建议修正

当前有注册和配方：钢板、强化钢板、密封环、钢管坯、重型轴承、工业传感器、核换热管束。相关入口为 `BasicMaterialContent`、`HeatMaterialsContent`、`ReactorCraftingContent`，配方在 `recipe/pressing`、`recipe/heat_exchanger`、`recipe/sequenced_assembly`。Create传动轴、铜板与精密构件沿用原生路线。

- 强化钢板：1钢板加入1Create坚固板并压片，现行配方已去掉精密构件。
- 钢管坯：1钢锭切成2件；密封环：1钢板压成2件。
- 耐热玻璃、专用耐压管段、钢框架/钢轴、汽轮机调速阀等旧草案内容尚不能当成可达材料；本批不引入这些深层前置。
- 执行者材料候选仅为起点：其中控制器数量合计17，不能直接称21格。PM已改为12钢板＋4强化钢板＋2精密构件＋2工业传感器＋1传动轴，并核对布局实占21格。
- PM将外壳建议改为4钢板＋4铜板＋1强化钢板产8件，控制初期搭建成本；六件最终候选配方、三档整批材料账均见方案，全部仍待批准。

PM使用短小算术核对三档结构 `9L`、外壳 `8L-2`、转子 `L-2`、配方批量余料及SU数值；这属于方案校验，不记作Minecraft自动测试。运行仍须后续定向验证和人工门。

## 4. 方案复核

两位执行者又分别只读复核最终性能合同和材料表。配方、21格数量、三档整批成本与SU算术无阻塞发现；API复核指出Create会缓存 `unloadedCapacity`，不能把双轴跨区块立即撤销容量当成静态已证事实。PM已将控制器兼前轴/后轴分区块、部分机身卸载与缓存清理明确列为必跑真实探针，并规定无法满足时停下报告，不能自行放宽守恒或增加搭建限制。
