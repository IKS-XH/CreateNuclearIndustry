# 01E R1 保存现场只读分析

## 范围与快照稳定性

只读目标为 `run/saves/新的世界`；没有查看 `新的世界(1)`，没有启动/停止客户端、运行 Gradle、修改世界/配置/生产/测试代码或执行 Git 写操作。`nbtlib` 不在指定 Python runtime，因此用内联只读 Python 解压 MCA/NBT，无安装依赖、无落盘解析脚本。已阅读 `AGENTS.md`、01E计划R1、`minecraft-modding` 与 `minecraft-testing` 技能；按项目版本 Create 6.0.10-280 检查流体源规则，没有照搬技能中较新版本示例。

解析前后文件稳定：`level.dat` UTC `2026-10-07T10:26:33.688Z`/5017 bytes，`region/r.0.-1.mca` UTC `10:26:33.651Z`/3956736 bytes；MCA 文件的mtime和长度前后不变。目标锅炉 `(1,-1)`、汽轮机 `(1,-2)`、普通储罐 `(2,-2)`、另一创造储罐 `(2,-1)` 的 MCA chunk header timestamp 均为 `10:26:33Z`。这是保存退出后的稳定磁盘数据，不是实时内存采样。

## 锅炉库存、汽种和温压

目标 controller `(28,-59,-11)`；本次结构/截图说明为 6×6×5。保存 ledger：16 pairs、16 steam cells、Water `32000/32000 mB`、Steam `16188/32000 mB`、SteamHu `16188.0 HU`、WaterHu `57587.4 HU`、ProcessHu `0.1803643 HU`、CoolantHu `0`。Steam 数量比用户截图 `16311 mB` 少 123 mB，故截图和保存 NBT 并非完全同一 tick。正常区块 NBT 不含实时 client telemetry 字段 `Paid`、`Tw`、`Ts`、`Pressure`、`Produced`；`Paid`只写进 clientPacket，且端口`SteamOutputStatus`也只在clientPacket同步，均不能从普通保存NBT还原；不能从 ledger 推出“当tick PaidHU”或`Produced=63`。

四个北向汽口为 `(26..29,-57,-16)`。保存 `ScrollValue`：x26–28=1（SUPERCRITICAL），x29=0（NORMAL）。控制器 `Minimum=0.1` 且 `MinimumSet=true`，因此已保存出汽下限为10%；它是最低出汽门槛，不是炉压目标。用户截图时炉压50.97%，高于下限。

配置加载来源有边界：`BoilerConfig.register` 明确注册 `ModConfig.Type.SERVER`；当前世界 `serverconfig` 只有 `readme.txt`，没有 `create_nuclear_industry-boiler.toml`。`run/config/create_nuclear_industry-boiler.toml` 文件存在且稳定，但它不能单独证明本世界运行时采用了哪些 SERVER 设置。该文件/代码默认 `boilingTemperature=1`、`supercriticalTemperature=2`、`wallHeatCapacity=1600`、`waterSpecificHeat=.1`、`steamSpecificHeat=.2`、潜热 `.7`、steam cell `2000 mB`；若这些默认设置对当前世界有效，则按存档账本公式计算 `Tw=57587.4/(16×1600+32000×.1)=1.99956`、`Ts=1+((16188/16188)-.8)/.2=2.0`、`pressure=(16188/32000)×(2/2)=50.5875%`，符合超临界门槛(温度2、炉压≥50%)。截图的水温和汽温均为2.00；对截图 Steam=16311 mB 用同一默认公式，炉压为50.971875%，与截图50.97%吻合。因此截图与当前NBT/默认参数条件计算没有温度矛盾。但缺少本世界 SERVER 配置文件，仍不能将模板默认值称为已证实的实际运行设置，也不能由压力吻合反推出唯一配置。

## 管路、接收端与原生流体规则

保存方块状态显示 x26–28 的三个超临界口汇入同一主支路。首段 `(26,-57,-17)` 西侧一格即 Create CreativeTank `(25,-57,-17)`，到 sink 仅1个管节点；该 CreativeTank BE `TankContent={}`、1×1×1。Create 6.0.10-280 的 `CreativeSmartFluidTank.fill()` 直接返回输入量而不写入库存，因此它是实际可接收并销毁输出的原生 sink，当前为空内容不代表流体过滤条件。主支路同时由 `(26,-56,-17)`、`(26,-55,-17)` 上行，并沿 `(26,-55,z=-18..-27)` 接中型108汽轮机入口 `(26,-56,-27)`，共13个 Create 管节点；x27/x28先横向并入主干，到汽轮机分别为14/15个节点。按Create `mechanicalPumpRange=16`（`run/config/create-server.toml:364`；世界无覆盖）均在范围内；这些汽路上没有机械泵。

x29 普通蒸汽口没有加入前三口主网：`(28,-57,-17)` 状态 `east=false`，而 `(29,-57,-17)` 是 `axis=z` 的玻璃管，不能横向接x28。它沿独立支路 `(29,-57,-17/-18/-19/-20)`、经 `(30..33,-57,-18)` 再沿 `(33,-57,z=-19..-25)` 到普通Create tank `(33,-60,-28)` 的边界，最短13个管节点、无泵；普通罐为3×3×8且保存 `TankContent={}`。另一 1×1 CreativeTank `(37,-60,-13)` 保存8000 mB `minecraft:water`，属于独立水路，不在目标蒸汽支路上。

MCA 中主支路 `(26..28,-57,-17)`保存的Flow fluid为 `create_nuclear_industry:supercritical_steam`，CNI pressure字段约341.33/512；例如 `(26,-57,-17)` 西向/上向Flow为`In=0`且有341.33压力贡献，南向与东向Flow为`In=1`。`In`是Create `PipeConnection.Flow.inbound`状态位，不是计量流量；NBT的Flow `amount=1`也不是mB/t。x29普通蒸汽支路对应的管BE Flow/Pressure字段为空。保存流状态支持前三SC口当前已建立原生流，不能视为精确流量测量或当前游戏tick观测。

Create原生规则与混汽边界：锁定源码 `FluidTransportBehaviour` 检测同一 pipe transport 上不同 `FluidStack` 时调用 `FluidReactions.handlePipeFlowCollision`；`FluidNetwork` 以 `FluidStack.isSameFluidSameComponents` 检查网络流体并剔除不匹配flow。不能把两种汽种当作可在同一原生流体网络中并存的不同出口。当前已保存拓扑将前三SC与第四NORMAL物理分开，因此当前Nbt不存在四口异种流碰撞；它不同于用户最初口述的四口共同汇流拓扑。

以用户截图当前实际产汽为SC：x26–28选择匹配并有通往汽轮机及创造sink的已保存主支路；x29选择NORMAL且物理连接到普通空罐。在截图所示SC阶段，x29按纯过滤规则应等待，不降级输出。该支路本次保存NBT中Flow/Pressure为空，仅说明保存时没有记录到对应流状态；不能据此认定管路断开或永远没有NORMAL输出，也可能未采到汽种短暂切换/资格窗口。若炉内变为NORMAL，则x29到普通罐有受支持去向，前三SC端口会等待汽种匹配。此行为符合已确认的纯过滤合同；不要把端口等待或旧空罐状态推断成永久停流缺陷。

## 结论边界

稳定保存快照显示当前三个SC出口的主网有流体Flow记录、炉压/下限组合允许出汽，并有两个可接收端（创造sink与汽轮机）；第四个NORMAL端口独立连接普通空罐，保存时没有对应Flow/Pressure记录。截图温度2.00与账本按模板默认参数的条件计算相符，但本世界有效SERVER设置未从现存文件证实。用户表示本轮汽轮机真实外接轴残转未复现；该观察不扩大为全部动力/重载场景通过。该快照不复现原始“四口共管异种流”问题，不能宣称锅炉缺陷已修复，也不对其根因作结论。
