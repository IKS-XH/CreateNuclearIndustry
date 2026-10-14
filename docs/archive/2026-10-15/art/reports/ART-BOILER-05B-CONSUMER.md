# ART-BOILER-05B：客户端消费者进度

2026-10-11，仅完成任务卡“窗口资源消费先行”。已读取当前卡及共享接口边界，沿用已实际读取的minecraft-modding/testing/resource-pack技能和锁定版本。

唯一资源改动为 `src/main/resources/assets/create_nuclear_industry/models/block/high_pressure_boiler_window.json`：旧18个element替为完整0..16单element，六面统一原window_frame，UV0..16及逐面正确cullface，particle明确使用壳体casing_side，保留translucent。未修改PNG、其它JSON、Java、碰撞、noOcclusion或水位BER。

一次结构/已安装sprite引用小检查通过；原始证据 `build/reports/art/ART-BOILER-05B/consumer/window-model-resource-check.txt`。未创建镜像tests，未跑JUnit/Gradle/游戏或Git写操作。

当前模型提供后续CT基础，内部共享面过滤及同炉连接尚未实现。等待PM实际API/HANDOFF和负责人追加确切客户端写集；未编写推测Java，不称全批完成或视觉验收通过。

## 阶段二实际交付（2026-10-11）

主PM实际HANDOFF与美术53d699b同步生效后，按唯一写集新增三个独立锅炉CT客户端类与两个专属测试；实际应用minecraft-modding、minecraft-testing、minecraft-resource-pack技能，保持MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。窗口JSON与21PNG未再修改，12项共享源只读依赖与逻辑冻结manifest SHA逐项一致。

实现以block注册ID包装8个锅炉模型，9图集在Dist.CLIENT MOD RegisterGeometryLoaders内直接幂等登记；独立nuclear_heat_exchanger只作为第9类邻接证据，内部boiler_heat_exchange_section不作为外表面。普通side和partition_side分别保存，避免相同原sprite被覆盖。可靠实际Y==绝对partitionY的水平壳面才选隔层条；原生RECTANGLE/窗口OMNIDIRECTIONAL索引，未知sprite、接口图标和原facing/阀几何不改。

一次gather仅capture一个带context的不可变快照，面/邻接/共享窗面mask全部复用，嵌套与异常finally恢复。双方身份、闭边界、隔层及局部ID/实际外平面均可靠才连接。窗口共享切向接口通过ModelData过滤指定side与side=null实际quad方向，super列表不变。Create在getQuads才调用getShift，因此隔层选择也随gather写入ModelData，在getQuads期间通过finally恢复的短作用域提供；此阶段不访问世界或全局新快照。

两个测试类各5个行为用例，共10/10通过（零失败、错误、跳过）：6个实际外平面的跨材质邻接、身份/局部冲突与撤销、实际隔层Y、原生2×2/L凹角上下文、两路共享面过滤、一次捕获及嵌套/异常恢复、8ID/9shift幂等登记、发布撤销后仍只消费既有ModelData。测试先于生产实现编写；未另跑RED，不把缺类编译或源码字段检查称为行为RED。不重测PM同步生命周期。

唯一最终命令：`./gradlew.bat test --tests '*BoilerConnectedTextureBehaviourTest' --tests '*BoilerConnectedTexturesTest' assemble --console=plain`；退出0，BUILD SUCCESSFUL in 26s，9任务（5执行/4最新）。未clean/rerun/full build/GameTest/客户端/Git写。原始日志、退出码及两个XML保存在 `build/reports/art/ART-BOILER-05B/consumer/`。

冻结清单 `consumer/candidate-manifest.json` 绑定5源、8实际生产class、窗口JSON、21PNG、12共享依赖及唯一JAR；8class与22资源均逐字等于JAR entry，21PNG仍与作者generated逐字相同。冻结副本在 `consumer/frozen/`，候选 `create_nuclear_industry-0.1.0.jar` 大小2528938字节，SHA256 `d9745639811f57b9b5f164f46c943f6335339c83e62903ea3b40114622818a51`。窗口JSON保留SHA `049cf9404d019b015c9aa624ddf1929e465af05389faaa9aa99be409cea69656`。

至此消费者候选停写交负责人接续既定一次组合窄审；不是整批或用户视觉验收。用户需在现有存档复看不同尺寸成型/撤销、隔层横箍、跨材质口/阀、单窗/长条/2×2/L缺角及斜视共享面。主PM生命周期证据复用，Git集成与视觉门由负责人/PM处理。


### 独立窄审后注释更正

整批独立审查通过后，仅更正BoilerConnectedTextures.java第124行注释：明确不读取世界或构建快照，隔层选择来自本次ModelData，短作用域保存/恢复供Create出quad使用。行数与全部运行时代码不变；class、资源、JAR和10/10 JUnit原证据复用，未重测或重建。修改前manifest与冻结源码保存在consumer/before-comment-correction/，当前冻结源码与manifest仅更新该源SHA/bytes。

当前源SHA256：`2b7d12b6754786b0351c62fb4fe6637c04bbcbee03b61db2a22a333e13b03e72`；当前candidate-manifest.json SHA256：`469e5ae0c19cbafba295a78c9e6a88b157f8bc98cea4fe4dbb7d234105ce2274`。本次更正完成即停写。
