# EXT-B-EXCHANGER-01C 独立合并规格与质量审查

日期：2026-10-04
候选：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`
基线：`3ea9e89`
环境：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82。

## 审查结论

本审查未发现必须整改的代码问题。守恒修复和用户已确认的自适应供热方案均符合本批范围；定向JUnit与GameTest断言均通过。此报告不记录任务最终验收，也不替代客户端闭环人工复测。

## 代码与范围

- `SharedFluidReceiver`、`SharedFluidFillPlan`和`FluidNetworkSharedFillMixin`在锁定Create的`FluidNetwork.tick()`填充点，用方法局部计划同时限制handler、共享库存和物理口配额；重分配会先去掉当前handler旧份额。只收窄本模组接口的SIMULATE，EXECUTE与其他handler沿用原调用。Mixin为required且只匹配一个目标调用；本机NeoForge 21.1.219配置含MixinExtras 0.5.0，未增加依赖。
- 反应堆和换热器能力只提供共享身份与当下剩余量，未改原有填充/排出账本；不同反应堆冷端保留各自128mB/t预算，同一物理口多面共用预算，换热器多面共享热罐空间。
- `HeatExchangerState`先从已付款储热确定本tick档位，再转换补热；预热、分数输入、库存容量与冷液回填均守恒。绝对断流期限在发布当前档位前处理，过期库存不能被同tick新转换复活。旧NBT首次迁移前重复保存继续保留缺期限字段；0.3HU/mB以有界向上量化触及阈值，储备仍封顶；供热状态按实际转换热量是否覆盖本tick付款分类。
- 低流量真实锅炉代表用例逐tick注入18mB并验证9级持续输出；双面换热器夹具现在仅对实际Create连接封闭开放端，未将未连接方向当作活动流向。此前夹具中心北侧空气可能形成额外OpenEndedPipe接收端，最终夹具已封闭并由双面真实输送日志确认。
- 修改集中在任务授权的共享接收计划、两个能力视图、Mixin配置、换热器账本与测试、双语现有提示，以及Loop和锅炉GameTest。未发现依赖/构建脚本升级或用户存档、客户端、`__pycache__`改动。工作区另有PM文档、`logs/*.log`和REPRO运行产物变更，保留其原状，不纳入本审查写集。

## 验证证据

四份JUnit XML无失败、错误或跳过，共30项：`SharedFluidFillPlanTest` 7项、`HeatExchangerStateTest` 12项、`ReactorCoolantPortAggregationTest` 6项、`ReactorCoolantPortFlowBudgetTest` 5项。具体文件位于`build/test-results/test/TEST-*.xml`。

REPRO最终日志`build/reports/extension/EXT-B-EXCHANGER-01C-REPRO/gametest-final-connected-faces-unified.log`报告15/15 required GameTests通过。测试记录为：

- 三冷端：源1000、冷库存12000、热库存0，总量13000mB；三个实际支路均有完整流量记录。
- 单冷端对照：源1000、冷库存12000、热库存0，总量13000mB。
- 换热器双面输入：源1000、热罐4000、冷罐0，总量5000mB；两个侧面均有完整流量记录。

GameTest日志在15/15断言通过后进入`Saving worlds`，子JVM退出阶段停滞；REPRO按治理规则仅处理本轮自有子JVM与Gradle包装器并保留daemon，没有重复运行断言。随后独立增量`assemble`在`build/reports/extension/EXT-B-EXCHANGER-01C-REPRO/assemble-final.log`报告`BUILD SUCCESSFUL`。最终JAR含Mixin JSON与NeoForge TOML；压缩包核对确认TOML恰有一个`[[mixins]]`接线且JSON注册`FluidNetworkSharedFillMixin`。因此包装/打包检查已通过，但GameTest服务端的退出停滞仍单独记录，不等同于断言失败。

## 尚待人工门

- 客户端真实反应堆→换热器→Create锅炉→冷液回路，以及断供/堵塞、拆放与卸载恢复仍按合并人工清单执行。自动测试和本审查不代替人工体验。

执行者只读审查源码、XML和原始运行日志；未运行Gradle、未执行Git写操作。本轮实际使用`minecraft-modding`与`minecraft-testing`技能，并遵循项目治理第5.1节复用定向证据。
