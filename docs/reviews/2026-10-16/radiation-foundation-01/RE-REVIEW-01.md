# RADIATION-01 整改第1轮窄复核

2026-10-16，原组合窄审者 `/root/radiation_foundation_review`。仅复核delta列出的8个变化文件、R1/R2/R3及其直接引入的任务生命周期/数据顺序影响；未重读53个未变文件内容，未跑测试、构建或游戏，未派子Agent、Git写操作或修改首审输入。本报告不代替PM验收/合入或用户人工门。

## 两项结论

- **规格符合性：原R1/R2/R3逐项闭合；仍有1项与本轮跨帧缓存直接相关的P2生命周期问题，需整改。** 移动目标/无关更新、静态实体尾部与pending发包节拍均已获得同一生产算法的新证据；区块加载/卸载尚未使待完成Job已计算的外壳/路径屏蔽失效。
- **代码质量：原三项根因已修正，新增Job的区块依赖失效还需补全。** 当前可确认的修改范围内没有其他功能问题；不要求扩大成新一轮整批审查或风格整改。

## 当前来源与证据身份

读取 `REVIEW-FIX-01.md`、更新后的 `REPORT.md`、`review-fix-01-source-delta.json`、`source-review-fix-01.json` 和 `review-fix-01-freeze-verification.json`；沿用首审已读AGENTS、治理5.1、批准设计、主树计划/接入裁定及Minecraft modding/testing/resource-pack技能。本轮另核制品排除时沿用minecraft-ci-release要求，未改变版本、依赖或资源合同。

前后manifest集合均61项、无新增/删除；本轮8项实物字节数/SHA256与delta的after完全一致，freeze-verification列出的全部证据文件哈希一致。旧manifest/JAR实物在first-review-input中，REVIEW.md/review-source.diff保留原审语境，未以首审身份冒充当前来源。

当前JAR实物3427684 bytes、SHA256 `6EEFAACBA5F86686016AEC72D4313B9870A52F3F4070FFC67E09BD973057593A`，与本轮清单一致；只读ZIP核本批GameTest类/独立域目录及模板条目为0，RadiationConfig.class存在。这是未提交来源的候选。

## R1/R2/R3逐项复核

| 首审项 | 当前实现与对应新证据 | 本轮结论 |
| --- | --- | --- |
| R1 射线/移动目标永久重启 | SourceIndex按玩家UUID持有Job，固定本轮target，保留源组、待算队列、单条DDA格游标和累积阶段；小幅行走继续原任务，相关格/壳依赖使组dirty，无关块不清维度进度。当前GameTest使用实际生产类的WorkLimits(4096,4096,8)，12个真实ItemEntity、每真实tick移动及远处更新，经正式ServerEvents剂量入口在第25个续接tick得到0.11757867639469602D；单路径可大于每tick预算。 | **原触发闭合。** 与区块生命周期相关的新增缺口单列下方RR1，不以原R1重新收费或扩大范围。 |
| R2 实体前缀永远重读 | Job明确保存实体引用列表和entityCursor，读取后才推进；工作预算耗尽保留进度，校验与求和也有游标。当前同一算法WorkLimits(8,4096,4096)，20个静态实际实体、第20个放射源，在第5个续接tick完成并读到1D/s。 | **闭合。** 无需为证明相同控制流强造32769实体。初次空间查询/列表快照仍同步，证据不宣称任意实体规模纳秒上界。 |
| R3 incomplete逐tick发包 | ServerEvents分离DisplayState.lastSent、latest、unsentComplete与采样/剂量计时；正式发送和计数接收器共用offer/flush。60个逻辑在线tick仅3次计数传输，pending/complete共用20tick门；另有迟到complete优先发出，Payload.supersedes允许其接替较新pending，客户端继续核维度/时间/有限数。 | **闭合。** 这是生产发送控制流的计数证据，不是真人S2C网络性能/显示验收。 |

有效RED为 `review-fix-01-red-registered.log`：旧9项通过，新3项分别在移动70tick、实体尾部50tick与发送actual=60失败，Gradle失败；首次“无测试函数”的exit0没有被算成通过。

当前冻结运行 `review-fix-01-delivery.log` 为13/13 required、全部三维度保存、BUILD SUCCESSFUL、GRADLE_EXIT=0；新增相关变更回归覆盖已处理路径新墙、ItemEntity移除、带transport提走。原9项本轮在同次运行复核，包含65536嵌套2tick完成、incomplete不恢复而高剂量伤害继续、壳位置/持久化/配置等。`review-fix-01-assemble-final.log`为之后增量assemble成功/exit0。41项JUnit是复用纯剂量/配方/参数/旧事务未变实现的原证据，本轮没有冒称重新执行。

## RR1 [P2] 区块生命周期没有失效Job内已算出的屏蔽

主定位：`E:/MyMC/NewMod/Create_NuclearIndustry-radiation/src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationSourceIndex.java:152`。相关同文件225～227、238～252行；事件入口为 `E:/MyMC/NewMod/Create_NuclearIndustry-radiation/src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationServerEvents.java:117`，几何dirty入口为 `E:/MyMC/NewMod/Create_NuclearIndustry-radiation/src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationContainment.java:68`。

**具体触发：** 一个已知完整壳跨区块A/B，放射源及其真实owner位于仍加载的B，外部目标位于仍加载的C。Job在stage3已为该源算出containment=0并从rays移除，但因读取/验证/求和总预算尚未完成而暂停。此时仅卸载壳体另一部分所在A，再续接任务。用同一生产算法的小栈预算就能稳定把单个源暂停在stage4，实际默认预算下多源/多工作量也可出现相同阶段交错。

**控制流证据：** unload只失效chunkSources中origin落在A的源，并删除target落在A的任务；本构造两者都不满足。随后Containment.chunkChanged只把Frame.dirty置true。已结束的Group.containment/pathTransmission仍保留，任务不回到stage3调用transmission；stage4只验证真实源/owner/指纹，stage5直接使用旧containment=0。因此能交付complete=true、仍按整壳零泄漏计算的结果，而同一时刻重新调用shellComplete会因A未加载返回false。仅设置Frame.dirty无法更新Job已经复制的屏蔽结果。

相反方向也缺少通知：ChunkEvent.Load只dirty几何，没有让正在等待的Job重算已处理的路径格。如果某路径此前按“未加载未知格不授予屏蔽”算为1，区块在任务完成前加载并出现屏蔽墙，仍可交付旧的不衰减结果。普通setBlockState的依赖通知覆盖不到从磁盘载入这些格。

**影响：** 待完成任务会把区块可见性变化前的屏蔽当作当前完整结果；前述壳体情形违反“不确定完整性不能授予整壳屏蔽”，可漏计新增剂量。此问题来自本轮新增的持久Job/Group结果缓存；首审每次查询重新走containment没有这条跨帧结果留存路径。当前13项覆盖源卸除与新墙，但没有在屏蔽已算完、整体仍pending时切换相关区块生命周期，故不能关闭此项。

**建议与最小补证：** 让load/unload按区块定位并dirty受影响的路径组和壳依赖组，或在最终交付前预算化核对这些依赖的区块代次；保留源失效与目标任务取消的现有行为，不清无关任务或强载区块。只补“源/目标仍加载、仅另一壳区块卸载”及“已处理路径区块加载带墙”的定向回归/修复证据即可。本项为源码状态转换推导，审查未额外运行游戏复现。

## 其他修改相关核对与保留边界

- 相关方块更新沿已访问路径格/已知壳依赖重排射线；真实源失效重置generation、验证与求和，避免已计入旧源继续相加。现有新墙、实体移除、带transport去除的真实断言对应这些分支。
- 退出、换维、克隆撤销UUID任务及显示/在线采样时钟；死亡剂量仍清零、非死亡剂量保留；世界卸载关闭所有活动索引，40tick无请求撤队。ItemEntity.setItem和leave通知为只读失效，不改掉落运输/保护行为。未发现这些改动改变既有伤害/恢复或网络方向。
- 同场景16³/196已用组件/满架配对各40个tick，默认预算工作量835或834栈、203BE、0射线、稳定shellReads增量0，与最后日志一致。关闭/开启的是诊断查询调度；Pre/Post短样本包含背景夹具、JIT/GC和快进，不把P50/P95负差值解释成性能收益或长期净成本。没有真人多人和真实网络性能数据。
- HUD/声音/遮挡、原皮甲外观、连续动力物流、真人重登/换维、携带源对旁人、四真人及客户端看向/背向/GUI性能仍为原人工门。迟到包的纯接替谓词和计数接收器不替代实际客户端生命周期体验。
- 持续相关物料/屏蔽变化可能推迟样本、初始原生列表快照同步、多玩家公平调度未实测等局限已在报告说明；本轮没有借这些局限扩展新需求。事故第二批和旧存档迁移仍不在审查范围。最终验收/合入与正式包仍由PM管理。
