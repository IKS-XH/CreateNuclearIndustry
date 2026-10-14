# 01E 汽轮机断汽残转源码审计

## 结论边界

只读核对了候选当前源码及锁定依赖 Create `6.0.10-280`；没有在运行态复现用户现场。以下是能按时序验证的失效路径，不是已证实根因，也不构成修复结论。未研究旧存档兼容。按任务要求读取了 `AGENTS.md`、01E任务卡、`minecraft-modding`、`minecraft-testing`、`systematic-debugging`；技能示例版本未用于升级项目的 MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280。

## 源码因果链

1. Create `KineticBlockEntity.tick()` 先运行 `needsSpeedUpdate() -> attachKinetics()`，随后在周期校验中调用 `validateKinetics()`。若 BE 没有 `source` 且 `speed != 0`、`getGeneratedSpeed() == 0`，校验直接写 `speed = 0`；该写入不调用 `onSpeedChanged`、`detachKinetics` 或 `RotationPropagator`。
2. 本模组仅前轴 `assignedSu()` 非零；两端 `assignedRpm()` 同取有效机主 RPM。`TurbineShaftPowerSource.tick()` 在 `super.tick()` 后发现账本变化，并只在 `previousSu > 0 && su == 0 && previousRpm != 0 && !hasSource() && hasNetwork() && getTheoreticalSpeed() == 0` 时回填 `previousRpm`，然后调用 `updateGeneratedRotation()`。
3. Create `GeneratingKineticBlockEntity.updateGeneratedRotation()` 只有 `prevSpeed != generatedSpeed` 才调用 `applyNewSpeed`。无上游 source 的停止分支会 `detachKinetics()`、清速度和 network；有 `source` 的分支则只把自身容量设为 0 后返回，以保留可能合法的外源转动。`RotationPropagator.handleRemoved()` 也会在被移除轴的理论速度已经为 0 时立即返回；因此若补偿未进入或 `hasSource()` 为真，不能仅凭 SU=0 断言传播器已经拆除残留网络。
4. 内部轴连接是双向且有条件的：`addPropagationLocations()` 提供另一端，`propagateRotationTo()` 仅在双方互相报告对方为链接轴时返回 1。控制器/结构失效会让链接变 null；轴 tick 随后 detach、更新 `lastLinkedShaft` 并重新 attach。Create 新源传播可通过邻轴重选 source；`KineticBlockEntity.validateKinetics()` 只在 source 区块已加载时检查，并仅以 source BE 缺失或 `sourceBE.speed == 0` 判无效，不校验其 `isSource()` / `getGeneratedSpeed()`。

## 可验证假说与最小观测

优先假说：持续残转若同时呈现 0 SU，说明“零生成容量”与“非零理论转速”分离。应首先验证断供边沿是否落入上述补偿条件之外；特别是被清零的发布轴是否当 tick 有 `hasSource()==true`，或 `lastAssignedSu` 已经是 0，或 Create 在父 tick 的 `needsSpeedUpdate/attachKinetics` 阶段先重新选源。若 `source` 指向仍有非零 `speed` 的外部源，保留它是 Create 的有意行为；只有证实该源属于汽轮机内部被动轴/已失效链接，才可讨论清除此引用。当前材料不足以证明发生过 native source 重选，也不足以把保存/加载列为原因。

在真实断供前后逐 tick 记录前轴、后轴及两端紧邻原生轴：位置、理论 `speed`、`getGeneratedSpeed()`、`source`、`network`、`isSource()`；并记录 `source` BE 的同组字段、network `sources` 键及其生成速度、`lastAssignedSu/Rpm`、`lastLinkedShaft/linkedShaft`、`needsSpeedUpdate`、Create `validationCountdown`。由此区分：父类直接清速、`attachKinetics` 选源、断开内部双向连接、合法外源持续带动。不得从“0 SU、256 RPM”单独推断来源。

## 建议的最小验证顺序

- 先按用户无额外动力源的实际连接拓扑自然运行并断汽，保留首次成功发电至零流量的逐 tick 源链记录；不要注入伪造 source/network 状态。
- 对照无双端回接、仅一端回接、双端闭环回接，检查停机时外接轴是否为同一 Create network，以及 source 是否反向选到机内另一轴。每个场景均只在确实存在原生外源时验证其转动可保留，避免“修复”误清合法外源。
- 独立验证当前版本保存/加载：自然运行存档、断汽前后各保存/加载一次，比较加载后首个 `initialize/addSilently`、第一次父类校验和第一次账本零值同步的顺序。此项只覆盖当前版本正常读写。
- 01G历史报告记载其双端回接和定向停机用例未捕获用户残转，且曾明确要求现场轴 `Source/Network/Speed` 证据；这些是既往有限证据，不能作为本次故障已复现或已解决的依据。

## 本次实际使用

只读检查 Create sources JAR 中 `GeneratingKineticBlockEntity`、`KineticBlockEntity`、`RotationPropagator`、`KineticNetwork`，以及候选 `TurbineShaftPowerSource`、`TurbineOutputShaftBlockEntity`、`TurbineControllerBlockEntity` 和既有定向报告。未运行 Gradle/GameTest，未修改生产/测试代码或其他文档；未执行任何 Git 写操作。

## 当前世界 MCA 只读路径核对（磁盘快照限定）

检查目标为 `run/saves/新的世界`，未触碰 `新的世界(1)`，未启动或停止客户端。`nbtlib` 不在指定 Python runtime，故使用内联只读 Python 解码 MCA/NBT；没有安装依赖或写解析脚本。首轮时间结论已撤回：当时读取的 region 时间与后续现场核对不一致，不能据此宣称区块早约十小时。用同一 PowerShell `Get-Item` 口径复核：`r.0.-1.mca` 为本地 17:53:44.163 (+08:00) / UTC 09:53:44.163Z，`r.0.0.mca` 为本地 17:53:51.170 (+08:00) / UTC 09:53:51.170Z，`level.dat` 为本地 17:52:00.389 (+08:00) / UTC 09:52:00.389Z。目标区域的 MCA chunk header timestamp：锅炉 chunk `(1,-1)` 为 09:53:53Z，汽轮机 chunk `(1,-2)`、罐 chunk `(2,-1)` 为 09:53:54Z；文件在解析期间仍有更新，末次读取 region mtime 到 09:54:34Z。当前磁盘 MCA 比 `level.dat` 更新，且正被客户端写入；这不是稳定、同刻的停机快照。

锅炉 controller 为 `(28,-59,-11)`。保存账本含 16 pairs、Water 32000 mB、Steam 3200 mB、SteamHu 3200.0；无可读取的 SteamTemperature、实时压力或 Produced 统计字段，不作推算。四口 `(26..29,-57,-16)` 的 facing 均为 north；存储模式 x26–28 为 SUPERCRITICAL，x29 为 NORMAL。此 3 SC/1 Normal 与后续用户口述的 4 SC 共管布局不同；当前文件时间不能判明布局变化的先后，不可据此否定后续现场描述。

中型汽轮机 controller `(26,-58,-25)`，RatedFlow 108；其入口 `(26,-56,-27)` outward=up。保存管线的 x26 口路径为 `(26,-57,-17) -> (26,-56,-17) -> (26,-55,-17) -> (26,-55,z=-18..-27) -> inlet`，13 个 Create 管节点；x27/x28 口先分别经 `(27,-57,-17)`、`(28,-57,-17)` 向西并入 x26，合计 14/15 节点。该路径未经过机械泵。分支隔断可由存储方块状态验证：`(28,-57,-17)` east=false，`(29,-57,-17)` 是 axis=z 的 glass_fluid_pipe，故两者没有横向流体连接。

x29 口是独立支路：玻璃管 `(29,-57,-17)`、普通管 z=-18/-19、encased 管 `(29,-57,-20)`，转东经 x=30/31/32 后至 `(33,-57,z=-20..-25)`，首接触普通 Create tank 边界 `(33,-57,-26)`，共 13 个管节点，无泵。这里不是 CreativeTank：普通 3×3×8 tank controller `(33,-60,-28)` 的 `TankContent` 为 18267 mB `create_nuclear_industry:steam`。周边另有 1×1×1 CreativeTank `(37,-60,-13)`，保存内容为 8000 mB `minecraft:water`；本次局部管路核对未发现它与锅炉 x29 支路相连。本次读到的 MCA 拓扑与后续口述的“4口共管→中型+创造罐”不一致；现有时间戳不能证明其因果或先后关系，也不能据此认定用户现场错误。

保存 BE 观察值来自本次读取到的 MCA chunk，但文件仍被写入：x29 支路若干管 BE 保存 steam Flow 与 256/512 压力；x26 通向汽轮机的竖/北向管 BE Flow 为空。它们不能证明当前运行流量或压力量。`run/config/create-server.toml:364` 配置 `mechanicalPumpRange=16`；目标世界 `serverconfig` 仅有 `readme.txt`，无覆盖文件。汽轮机 13/14/15 节点路径及罐支路 13 节点路径均未超过该值；路径上没有泵/方向反接证据。

本次只读现场调查确认了所读 MCA 中 3 SC/1 Normal、汽轮机与普通蒸汽罐分支，以及局部独立 CreativeTank 的状态，但它与口述的 4 SC 共管拓扑不一致。由于 region 在活动写入且文件时间与 `level.dat` 不同，无法确认是否为同一时刻布局；不能推断根因或修复结论。要确认口述拓扑需用户保存退出后提供稳定磁盘快照。
