# EXT-B-TURBINE-01G 只读源码诊断

日期：2026-10-05。目录 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，读取时 HEAD `56dd5efdae0633f3c49d00e1f198c5af021f14a2`。本报告是源码条件分析，不是根因复现或修复通过声明。没有运行构建/测试，没有修改功能、测试、配置、docs 或 Git，没有干预用户客户端。唯一写入为本报告。

## 已核对入口与证据边界

- 已读 AGENTS、治理 5.1/5.2、01G、01F、冷凝合并人工清单及 gradle.properties。
- 实际使用 `minecraft-modding`（核对平台版本和逻辑服务端/客户端边界）、`minecraft-testing`（真实 Create GameTest 与诊断探针的证据区别）、`systematic-debugging`（先查数据边界与确定时序，未复现前不猜改法）。入口分别为 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`、`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/systematic-debugging/SKILL.md`。
- 锁定 MC1.21.1、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；只读本地 sources.jar，没有查新版本或升级依赖。
- 本地源码包：`C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280/23e1219501c0debfa0bb56c30ef8e0193341aae5/create-1.21.1-6.0.10-280-sources.jar`。
- 读取 `repro-before-fix.log:98`、`repro-phase-before-fix.log:99`、`repro-phase2-before-fix.log:99` 均为 12 项通过。这些是其他执行者的修前证据，仅证明对应相位的服务端最终状态通过，不能证明客户端下游收到了停速数据包。

## 已确认的源码行为

1. `TurbineShaftPowerSource.java:57-59` 的生成 RPM 动态读取账本，SU 归零立即返回 0；`:111-128` 先 `super.tick()`，然后才比较账本变化并调用 `updateGeneratedRotation()`。纯断汽不会改变完整机组的内部邻接。
2. `TurbineOutputShaftBlockEntity.java:19-26` 仅前轴分配 SU，后轴始终分配 0；`TurbineControllerBlockEntity.java:335` 的成型机组 `signedRpm()` 仍返回额定 RPM。因此纯断汽时后轴的 `lastAssignedSu/lastAssignedRpm` 不变，后轴没有独立的末尾通知兜底。
3. Create `KineticBlockEntity.java:112-115` 的原生定期校验在该 `super.tick()` 中执行；`:149-151` 对无 `source`、实际 `speed!=0`、动态 `getGeneratedSpeed()==0` 的源轴直接执行 `speed=0`，没有 `onSpeedChanged/sendData/detachKinetics`。
4. Create `GeneratingKineticBlockEntity.java:89-105` 读取当前 `this.speed` 作为 `prevSpeed`，仅在 `prevSpeed!=getGeneratedSpeed()` 时进入 `applyNewSpeed()`。如果步骤 3 已归零，两值均为 0，`:127-129` 的无外源停机拆网分支不会执行；`:114-115` 只通知本轴，不通知下游。
5. Create `RotationPropagator.java:309-315` 在被移除轴的理论速度为 0 时直接返回；正常拆网才会走 `:339-354`，沿依赖链 `removeSource()` 并明确 `sendData()`。
6. 下游后续原生校验 `KineticBlockEntity.java:140-143` 会发现父轴 `speed==0`，先 `removeSource()` 再 `detachKinetics()`；而 `removeSource():326-335` 已先把自己的 speed 归零，只调用 `onSpeedChanged()`，没有 `sendData()`。此后的 detach 又被步骤 5 跳过，所以此路径会逐个靠校验清服务端，不能代替正常递归停机传播。
7. `KineticNetwork.updateCapacity/updateNetwork` 只更新容量/应力。`KineticBlockEntity.updateFromNetwork():155-168` 仅在过载标志变化时发送数据包。纯轴/转速表无负载时容量与应力都可以是 0，过载仍为 false，不存在容量归零自动停转或自动发停速包的不变量。
8. `SmartBlockEntity.lazyTick():88` 为空，普通轴没有周期性停速包兜底；`SyncedBlockEntity.sendData():59-62` 才将坐标加入区块变更通知。`KineticBlockEntity.tick():107-109` 客户端提前返回，不运行源链校验。`SpeedGaugeBlockEntity.onSpeedChanged():49-59` 更新服务端 `dialTarget`，却只 `setChanged()`；它的 `speed` 与 `Value` 需要实际更新包才能同步客户端。
9. 校验配置默认为 60，但 `if(validationCountdown-- <= 0)` 后重设 60，稳态实际间隔为 61 tick。以固定 60tick 推算断汽相位不可靠；各方块初始化相位及控制器/轴 tick 顺序也会影响是否重叠。

## 单一待证假设

账本从正 SU 衰减至 0 的时刻恰好遇到前源轴的原生校验，校验在本机停机更新前抢先将前轴 speed 归零，导致正常拆网/递归 `sendData()` 被跳过。双轴和外接轴最终可能在服务端靠自身校验归零，但客户端保留旧的 Speed/Value，从而持续零 SU 转动。

这条链可以解释间歇性与当前最终服务端断言通过，仍未证明用户现场就是该链。如果实测服务端持续非零，则需要另查实际 `source` 链，不能把客户端假设当成现场已确认事实。对正常无外源树状链，源码提供最终逐级清源机制；尚未发现能够在该最小树中永久保留服务端速度的合法来源。

## 建议给唯一实施者的最小实证

优先复用已注册 `TurbineProbeOwnerBlockEntity` 与 `TurbineProbeShaftBlockEntity`，无需新增模板或注册。前后轴远距 5 格，前后各接 1 格原生普通轴，无 Creative Motor、无其他动力源、无负载；保留机内连接，稳定供能后确认前轴 `source==null`，后轴 `source==front`，两端外轴 `source` 指向相应机端，速度均为 128。

在单个测试回调里按顺序执行：记录稳定网状态；`owner.setSupply(0,false)`；只通过测试反射把前轴私有 `validationCountdown` 设为 0；调用一次前轴 `tick()`，然后在后轴/外轴得到自身 tick 之前立即检查。应当断言前轴生成 0 且实际 0、后轴和外轴已经按正常停机撤源归零。若修前后轴仍为 128，就固定证明了本次拆网通知被跳过，而不是只等待 165tick 看服务端最终速度。

若要区分客户端通知风险，可在允许写的探针轴类中增加仅测试用途的 `sendData()` 计数，覆盖后仍调用 `super.sendData()`。供能稳定后记录后轴计数；执行上述相位；让正常世界 tick 经过一次后轴校验。对照记录“后轴服务端 speed 已 0，但后轴 sendData 计数没有增长”。这个断言能证明同步触发缺口，仍不是联网客户端收到包的完整证明。也可使用已存在测试连接或服务端区块变更集合观测；不要为本批扩建全局 mixin/网络框架。对普通原生外轴不便计数时，后探针轴是最小受控下游通知证人。

随后由实施者在真实机组自然供汽/停汽用例中组合原有最终断言，增加记录 SU 首次归零世界tick、前轴校验倒计时、前后/外轴 source/network/speed，以及停机广播；固定相位探针与自然机器用例必须分别标注。有外源时必须仍保留合法外源转动，只撤本机容量；不能用全网强制清零修症状。确认失败以后再确定最小实现，当前没有提出已验证的修复。

## 本次交付边界

本报告和关键源码链已直接同步 `/root` 与 `/root/turbine01g_impl`。测试/构建仍完全由原实施者持有。未研究旧存档、外观、全局流体管道或用户世界。后续审查需要另行安排。
