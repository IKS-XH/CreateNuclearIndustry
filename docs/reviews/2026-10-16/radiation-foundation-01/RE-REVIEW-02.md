# RADIATION-01 第二轮定向窄复核

2026-10-16，原审查者 `/root/radiation_foundation_review`。仅核RR1、本轮4个既有文件变化和直接影响；原R1/R2/R3保持上轮闭合，不重开整批审查。未跑测试、构建或游戏，未派审查者，未执行Git写操作或改核心文档。本报告不宣告PM验收、人工门通过、合入或正式包交付。

## 两项结论

- **规格符合性：RR1在本轮限定范围内闭合，无剩余代码整改项。** 远端壳区块卸载和已处理路径区块加载两方向都已使pending Job重算命中组，不再沿用旧屏蔽完成结果。既定人工门和真实区块装载体验仍保留。
- **代码质量：本轮4文件改动未发现新的功能问题。** 依赖失效继续精确到相关组，原源/目标卸载处理和不强载边界保留；可注入查询仅收窄屏蔽可见性，不开放库存/剂量写入口。测试选择和制品排除有明确边界。

## 当前来源核对

已读 `REVIEW-FIX-02.md`、更新 `REPORT.md`、`review-fix-02-source-delta.json`、`source-review-fix-02.json`、`candidate-review-fix-02.json`、`review-fix-02-freeze-verification.json`，及两项定向测试源码/原始RED、最终delivery和assemble日志。沿用已读AGENTS、治理5.1、批准设计/计划与Minecraft modding/testing技能；此次无资源变更，不扩资源审查，制品身份按已读ci-release边界只读核对。

本轮delta恰4项：RadiationFoundationGameTests、RadiationContainment、RadiationServerEvents、RadiationSourceIndex。逐项重算当前字节数/SHA256，全部与after一致；fix1与fix2的manifest均61项、无增删，freeze-verification所列全部证据文件哈希一致。旧轮实物/清单在second-review-input中，不用旧轮身份替代当前来源。

当前候选JAR实物3429128 bytes，SHA256 `AFAD58C122257112BD4BA05C275D86D30293AB5546F2D0531FFEDE5D38905817`，与当前candidate一致。独立只读逐字节哈希核对JAR内738个生产class与当前build/classes/java/main输出，0不一致；本批GameTest类/模板/独立域目录条目为0。因此assemble之后仅补被排除测试断言的来源关系成立，无需为报告收集再打包。该包仍是未提交来源候选。

## RR1两方向闭合依据

主入口：`E:/MyMC/NewMod/Create_NuclearIndustry-radiation/src/main/java/com/iksxh/create_nuclear_industry/radiation/RadiationSourceIndex.java:157`。

| 原触发 | 当前实现 | 当前定向证据 |
| --- | --- | --- |
| 源/目标区块仍加载，仅另一壳区块卸载；stage3已算containment=0、整体停在stage4 | Containment.chunkFrames通过framesByChunk返回该区块所涉已知壳身份；SourceIndex.chunkChanged合并frameDependencies命中组并调用既有dirty，使任务回stage3重算。Unload先排除源/取消目标任务，随后补同一屏蔽失效入口；ServerEvents仍dirty几何，下一次查询不授予不可见壳整壳屏蔽。 | 真实跨区块合法小堆和真实ItemEntity，用小栈预算停stage4；仅隐藏另一个壳区块的查询可见性，再向NeoForge总线投递ChunkEvent.Unload。前后明确断言源/目标实际区块仍加载，最终场1.953125E-4 D/s>0，壳块路径衰减仍可存在。 |
| 中间路径区块先未知，路径已算完、整体pending；加载该区块后存在预先放好的墙 | 每个已处理DDA格均登记pathChunkDependencies，包括当时未知的格。Load通知chunkChanged命中路径组并dirty，重读已处理路径；不依赖setBlockState通知。 | 真实已用组件、28格目标和预先存在的实际屏蔽混凝土；隐藏中间区块至stage4，恢复可见性并投递ChunkEvent.Load，最终6.377551020408162E-5 D/s，即0.05/28²。墙在事件前已存在，不能靠新放墙通知绕过待测入口。 |

同文件实际DDA查询继续先核真实LOADED_CHUNKS，再核注入visibility；默认构造器固定真实getChunkNow。Containment在读取壳之前同样要求真实已加载且查询可见；注入返回true不能越过真实加载检查。源/owner读取仍沿实际加载校验。ChunkEvent.Load只使几何/任务失效，不在可能早于FULL的事件中读取方块、等待提升或请求加载。

无关事件路径静态核对：chunkChanged只对pathChunkDependencies或chunkFrames→frameDependencies命中组调用dirty；未命中时affected为空，不清Job、不撤源游标、不重置实体/读取进度。区块失效复用原dirty的幂等队列，重复通知不会重复入队。既有40tick撤队、源失效、玩家/世界生命周期和独立显示节拍未改。此处为本轮源码推导；未把fix1的无关普通方块测试写成fix2新执行的区块事件测试。

## 证据有效性与测试入口

- `review-fix-02-red-compiled.log`明确运行2项且均失败：路径实际为未衰减0.0012755102040816326，壳体仍旧0；Java2/Gradle1并保存。此前编译失败只是夹具问题，没有被当作行为RED。
- 当前冻结日志 `review-fix-02-delivery.log`明确2 tests运行、2/2 required通过、三维度全部保存、BUILD SUCCESSFUL、GRADLE_EXIT=0。两个新断言是在实际GameTest服务端通过生产Job、真实源/方块与实际总线事件对象执行。
- 可见性受控、事件由夹具投递：区块实际一直由GameTest加载器持有，没有证明真实卸载/磁盘载入，也没有登录玩家或真人多人。反射只观察stage4，不修改Job/Group/Frame。该边界已在执行报告及原始日志明确标出，不因2/2将其升级成真实区块装载体验验收。
- 测试类内MOD总线TestRegistration默认注册原9+fix1四项共13；仅显式属性cni.radiation.reviewChunkOnly=true注册ChunkLifecycleReview两项，原方法/断言保留且templateNamespace明确。init脚本仅向runGameTestServer转发该属性，不改工程构建脚本/依赖/世界目录。默认13为静态注册核对，本轮未运行，不能报成当前13/13。
- 41项JUnit、fix1的13项及同场景配对性能按未变实现和旧轮来源复用；本轮仅2项新结果。`review-fix-02-assemble.log`为生产源固定后的增量assemble成功/exit0，最后只新增被排除测试断言；738个生产class一致性支持当前制品关系。

## 保留人工门与局限

本轮没有新增未闭合代码发现。真实区块卸载/载入、真人正常保存重进/换维、HUD中文/声音/遮挡、原皮甲外观、连续动力臂/带及封存搬运、携带源对旁人、四真人和客户端看向/背向/GUI性能，仍按原集中手测处理。受控可见性、FakePlayer、计数传输、NBT往返及服务端保存分别只证明其已记录的边界，不互相替代。

本轮未新增性能数据；此前短时GameTest配对不推导长期净成本或真实网络性能。持续相关内容变化及大规模同步引用快照等既有局限不借本轮扩成新需求。事故第二批、旧存档迁移、Git与正式包管理仍不属于本轮。最终验收/合入由PM按现有流程决定。
