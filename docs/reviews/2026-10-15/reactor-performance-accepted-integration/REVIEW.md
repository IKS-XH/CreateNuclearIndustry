# 反应堆已验性能优化接入：组合窄审

日期：2026-10-15。角色：独立审查执行者；本报告是规格与质量审查，不代替PM集成验收或用户验收。

## 结论

S1、S3、C1、C1L的生产实现未发现阻断性的算法、库存、冷却守恒、管理员、协议或玩法回退。可按精确净清单接入；正式制品交付前有一个必须解决的打包阻断：标准JAR会携入新增的`ReactorPortLookupGameTests.class`。完成下述窄排除并由唯一集成执行者核实最终包后，无需因本轮审查重复性能采样或整套游戏测试。

本轮只写本文件，没有修改代码、测试、核心文档、Git、运行配置或世界，没有构建、运行测试、启动Minecraft或派发其他执行者。

## 基准、范围和技能

- 执行卡：[已验性能优化接入](../../../archive/2026-10-15/plans/2026-10-15-reactor-performance-accepted-integration.md)；已读AGENTS、治理5.1和STATUS，现已由PM归档。
- main基准`7e214b979f1d3bb5528278bb117a2867914f88d7`；完整审查源为`E:/MyMC/NewMod/Create_NuclearIndustry-reactor-performance-diagnosis-20261015/C1L/source`，结合S1/S3/C1/C1L补丁、报告和原始结果核对。
- 复用集成执行者的[完整净差异](../../../../build/reports/reactor-performance-accepted-integration/candidate-full-source-diff.json)、[补丁链](../../../../build/reports/reactor-performance-accepted-integration/patch-chain.json)和[16路径清单](../../../../build/reports/reactor-performance-accepted-integration/import-paths.txt)，不重复全树哈希/备份。清单为7个生产Java、1个开发GameTest、2个资源和6个JUnit/参考源；未授权净增量为空。换行差异不视作额外功能改动。
- 实际Gradle锁定值为Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；无依赖/加载器升级。
- 实际读取并应用`C:/Users/IKSXH/.codex/skills/`下`minecraft-modding/SKILL.md`（BE/客户端边界）、`minecraft-testing/SKILL.md`（冻结参考/原生日志/跳过边界）、`minecraft-resource-pack/SKILL.md`（模型和材质引用）、`minecraft-ci-release/SKILL.md`（标准构建和正式包载荷边界）。遵循治理5.1及本卡只读审查约束。

## 必须关闭的阻断

**[P1] 新开发GameTest会进入标准正式JAR。** 新类位于`src/main/java/.../gametest/ReactorPortLookupGameTests.java:27`，带`@GameTestHolder("cni_reactor_port_lookup")`，使用反射调用私有入口与线程分配计数，属于开发验证夹具。当前`build.gradle:138`的JAR排除表不含该类。只读打开既有`C1L/source/build/libs/create_nuclear_industry-0.1.0.jar`确认实际存在`com/iksxh/create_nuclear_industry/gametest/ReactorPortLookupGameTests.class`，19534 bytes。这不是仅凭路径推测。

由PM另派`build.gradle`窄适配，沿用现有排除机制覆盖`com/iksxh/create_nuclear_industry/gametest/ReactorPortLookupGameTests*.class`，再让集成执行者核实标准assemble最终包不存在该类及其内部类。该夹具复用既有`create_nuclear_industry:p0_probe_empty`模板，没有本批新模板需要删减；不扩大为清理旧测试域。`artifacts/c1l-performance.jar`是基线包上的受限增量制品，不能替代标准assemble的排除校验。本审查者未代写适配。

## 实现核查

**S1端口索引。** `ReactorInstrumentPortBlockEntity`按燃料列保存每仪表私有索引，有效重扫先清后建，失效/解绑清理；`detachPort`按实体身份移除，旧BE迟到解绑不会撤掉同坐标替代者。查询仍检查正式职责、列和owner绑定、结构有效/移除状态及世界当前BE身份；原完整物品捕获、提交前整批验证、有序结算没有删除。索引不持久化、不进入网络，数量受本结构端口数限制；缺失/错列返回拒绝结算，不伪造燃料或绕过库存事务。

**S3精确反馈缓存。** `ReactorSnapshot`保证坐标TreeMap顺序，拓扑数组沿此顺序，邻接保留北、南、西、东顺序和DoubleStream补偿平均/求和。原初值、同步双缓冲迭代、热/燃耗两项`1e-9`收敛判定、256轮上限一致；未收敛仍返回最后一轮但不保存复用。缓存键完整涵盖截面/燃料与棒坐标、燃料可用性、实际棒深及五项反馈参数，并按double位匹配。高度、损伤、当前燃料和全部结算参数在每次结果计算重新读取；全堆限幅、一次ceil、最后正产热列尾差和燃耗保留。冻结`ReactorFissionReference`主体与基准生产类在仅更名后文本完全一致；差分不是把新计算器当自己的参考。

缓存按计算线程隔离，每线程最多8拓扑、每拓扑4反馈组；只持有局部坐标/纯数值数组，没有Level、BE、快照、库存或配置对象引用，不写NBT/网络。返回结果只读缓存数组，不暴露可变数组给调用方。管理员预测与权威结算只有真实反馈输入一致才命中，未改101档曲线缓存、安全评估、控制棒推进或tick编排。

**C1/C1L客户端。** `ReactorGlowMeshCache`仅由客户端事件和BER使用；身份含会话/维度/owner/generation/geometry，持有纯值，不保存运行描述符或世界。完整快照边界、owner/本地成员失配、区块/世界切换、20显示tick未用和模型烘焙完成均撤销相应缓存；重载同时撤UV模板。8 owner/16MiB是展开位置的上限，小量列/法线/最多256档颜色另计；超预算按相同变换流式提交，不丢面。静止姿态精确匹配后才复用相机位置，移动即重算。

C1L按扫描器自底向上的连续列契约，将首格和格数映射到`1/8..高度-1/8`；每列九根开口四侧细管，共36 QUADS，只有水平法线，竖直拉伸不需新增法线变换。热量只调RGB，alpha255/FULL_BRIGHT；无料/零热不提交黑色遮挡。原液体网格/材质/库存与控制棒代码、行程和资源未改；辉光改用原生`cutoutMipped`，液体仍在棒体/辉光之后结束，未引入自定义shader或同步变更。整列连续、取消分格间隙与半透明柔边是已批准的可见取舍，不声称与原辉光像素等同。

`GLOW_LOW`在既有客户端模型初始化登记，JSON经`neoforge:obj`引用新增OBJ，OBJ复用原`fuel_rod_glow.mtl`，MTL与particle均指向既有PNG；新文件路径、材质名和命名空间匹配。原72面辉光资源保留，不改液体和棒体资源。当前依赖的`ModelEvent.BakingCompleted`本地源码确认发生于模型注册表应用后；已有原生重载记录进一步覆盖新模板恢复。

## 可复用证据及必须保留的边界

以下是候选原记录，本轮没有重跑：

| 范围 | 已读证据和可复用结果 | 边界 |
| --- | --- | --- |
| S1 | `S1/s1-build-targeted.log`、`green-final-results.txt`和`green-final-summary.json`：定向JUnit24项通过，真实服务端10项通过；重扫、替换/迟到解绑、错列、物品变化整批拒绝、当前版本保存续接及既有机械臂事务有覆盖 | RED/模板准备失败等原记录保留，不合计为通过 |
| S3 | `S3/targeted-tests.log`和`junit-summary.json`：127项通过；冻结参考1400次裂变严格equals及600步完整tick阶段严格equals；缓存命中、精确失效、有界淘汰和不收敛不复用有覆盖 | 随机/定向覆盖支持同语义判断，不扩称全部输入数学证明 |
| S3真实服务端 | `gametest.log`、`s3-green-summary.json`为13通过/1失败，`gametest-extra.log`和`s3-extra-summary.json`补2通过；服务端保存退出0与业务结果分开 | 旧`controlRodTickAppliesTargetToFissionAndPreservesScramAndJammedState`在S1基线也失败：旧断言2.25HU与既有全堆ceil后3HU不符。合计15通过、1旧失败；不能写全部通过，不在本批删断言/改热工 |
| C1 | `C1/junit-summary.json`、`targeted-final.log`、原生停止/恢复、区块往返和在线重载记录 | **原REPORT的“57/57”过度表述：原summary实际57总数、54通过、3 Sodium跳过。** 原报告不由本审查者改写，集成结论须使用更正口径 |
| C1L | `C1L/validation.log`及`source/build/test-results/test/`原XML：58总数、55通过、3 Sodium条件跳过、0失败/错误；`native-check-2/result.txt`确认144模板顶点、149整列、257472位置字节，在线重载换模板并恢复owner，全插棒零热后owner缓存归零 | 无Sodium/Iris/光影；3跳过不能计通过，亦不能外推多人或其他硬件 |
| 性能/视觉 | 四批REPORT与矩阵、原始JFR/逐帧/实际镜头守卫；C1L六角度补拍索引、原生重载/全插棒记录；用户后续视觉/性能反馈范围由活动卡确认 | S3首轮菜单状态失控数据仅辅助，主结论用无菜单补采；C1L是本硬件一轮配对，不把背景/JIT波动当算法收益 |

性能收益可引用原场景：S1运行MSPT21.149→7.625；S3无菜单补采9.140→5.997、管理员15.090→7.061；C1L相对C1正看平均帧36.596→3.700ms。它们分别来自不同阶段，不能相乘成一轮整机收益。C1L服务端字节不变，其MSPT基本持平；正常服务端P99为14.156→18.379ms，管理员客户端GC总时70→104ms，均保留，不能声称全部高分位或GC改善。约6MB/s是单观察者压缩前编码量，C2/协议频率未优化，继续后置。

## 最小集成验证建议

1. PM依据16路径净清单精确导入，保留所有既有dirty与世界；另记录窄JAR排除适配。核最终源与已验候选一致，只有该打包适配为新增差异。
2. 来源提交后，由唯一集成执行者从准确Git树导出、核输入，按卡只运行一次`assemble --no-daemon --console=plain`。不追加`build`、`clean`、`--rerun-tasks`或重复性能/全套JUnit/GameTest。
3. 对标准输出核全部class/资源与构建输入及相对旧正式JAR的载荷差异：新增生产缓存内部类齐全，2个低面资源与原MTL/PNG可解析，新GameTest/JUnit参考/诊断agent没有新增进入正式包，旧有非本批载荷不顺手清理。禁止直接把受限增量性能包当正式新包。
4. 若出现实际语义冲突、非清单变化或资源缺失，再扩大对应定向验证；否则复用上述原证据和已确认用户反馈，最终源码提交、正式JAR身份及边界由PM记录。当前审查不新增旧存档迁移、Sodium/Iris/多人或C2验收门。

## 整改及最终制品补核（2026-10-15）

按PM追加授权，仅复核原P1整改和唯一构建的实际制品；未重做算法审查、构建、性能采样或测试。已读提交`5ea9c509187a3656a08248843e0142d3d7ddca67`的实际`build.gradle`差异：只在现有JAR排除块新增`ReactorPortLookupGameTests*.class`一行，未扩大清理旧域。该整改关闭源码层面的排除缺口。

已读[构建输入绑定](../../../../build/reports/reactor-performance-accepted-integration/build-input-binding.json)、[assemble原日志](../../../../build/reports/reactor-performance-accepted-integration/assemble.log)、[运行结果](../../../../build/reports/reactor-performance-accepted-integration/assemble-result.json)和[制品核验](../../../../build/reports/reactor-performance-accepted-integration/artifact-verification.json)：构建来自上述准确来源提交，树`bef7f5203e5870eaaf8d61006b9e4ddab22e566c`，唯一`assemble --no-daemon --console=plain`由集成执行者运行，exit0、18.749秒。日志为assemble成功，未冒称全build或新游戏验收；`compileJava FROM-CACHE`与原同实现证据复用一致。

审查者另行只读打开实际交付包`build/reports/reactor-performance-accepted-integration/built-8405e4cb6ad010f4b3f4e7a0e475a39d5e09918605f488c8c7466fc822981d5f.jar`，核得3323054 bytes、SHA256 `8405e4cb6ad010f4b3f4e7a0e475a39d5e09918605f488c8c7466fc822981d5f`。实际ZIP不存在新增端口GameTest、四个S3测试/参考类型或Glow/Visual诊断agent。结构化核验确认699 class、1571资源及Manifest与准确编译/资源输出逐项相同、无额外文件；开发GameTest编译输出存在而正式JAR中缺席，旧124项测试/探针载荷保留。由此原P1打包阻断也已关闭，结论仅为“无本批新增测试/探针泄漏”，不是声称历史包完全没有开发类。

对实际新包与实际`fe446ef51c63a07430bf4daed40d8f246a11612fc5fdd022c29fc7ee77e1b0dc`手测包逐ZIP条目独立比较，只有`ReactorInstrumentPortBlockEntity$1.class`和`ReactorInstrumentPortBlockEntity$PortBindingSpec.class`两项不同，条目集合相同，无其他资源或类差异。两类的新包哈希与候选完整编译输出绑定一致；[内部类绑定](../../../../build/reports/reactor-performance-accepted-integration/instrument-inner-binding.json)说明手测性能包当时只替换仪表外层、沿用旧正式包内部类。已复读两组`javap -c -p`现成反汇编，字段与方法体文本逐字一致；现有证据支持这两项仅为重编译调试位置差异，不是运行算法差异，不能把整包不同SHA误称为新玩法实现。

本轮已确认的唯一阻断关闭，未发现新增整改项。原S3旧GameTest失败、C1/C1L的Sodium跳过、性能/客户端/多人边界和C2后置仍按上文保留。正式路径替换、Git记录与最终项目验收由PM处理。
