# 汽轮机S1/C1优化集成记录

2026-10-18，主逻辑PM。用户要求把最新优化合main；随后提出锅炉优化一并合入，核查锅炉专项只有方案后，明确选择“本次先合并已完成的S1／C1”。本次不实施锅炉重复同步优化，也不重开旧反应堆C1L/C2。当前进度与独立未闭门统一见[STATUS](../../../STATUS.md)。

## 来源与范围

基线main为`a66d4342200c04d61136afedd539c67268330c01`。复用同级干净树`Create_NuclearIndustry-du-accepted-integration`，分支`codex/thermal-performance-accepted`；六文件生产来源已提交为`34e1b77c520233ff37b6f0aa51fa5ad8cb9adbd4`。

- S1：`TurbineStructure`按真实成员包围盒预筛不相交机组，仅缓存静态空气截面并复用本次检查坐标。仍逐次检查当前世界、运行资格、区块/实体/世代及实际成员重叠。
- C1：`TurbineClientEvents`及两种新增visual，使用Flywheel 1.0.6 TRANSFORMED实例绘制原转子/输出轴；原BER保留为后端关闭或不支持时的回退。动画共享状态同锁，沿用原相位、模型、材质、采光与64格距离。
- 仅四生产文件、两新增测试；未改热工、流量/效率、库存守恒、红石、NBT、网络字段、配置、依赖或资源。来源为独立诊断目录的`C1/source/src`，其中结构类及新增结构测试逐字同`S1/source/src`。

## 原验证与收益边界

复用原[S1报告](E:/MyMC/NewMod/Create_NuclearIndustry-thermal-performance-diagnosis-20261017/S1/REPORT.md)和[C1报告](E:/MyMC/NewMod/Create_NuclearIndustry-thermal-performance-diagnosis-20261017/C1/REPORT.md)及原始日志，不称本轮重跑。S1有4项JUnit、7项真实汽轮机GameTest，以及同tick失效和真实区块卸载/重载验证；C1有27项定向JUnit和原生重载、采光、后端回退、静止回退、断开/重进检查。原有效运行均有exit0记录，之前失败、票据超时及工具纠正证据保留。

同机独立场景为一座16³锅炉和16台大型汽轮机。S1额定净MSPT由两轮基线均值9.928降至4.127；净值包含配套管网、动力与诊断补给，不是设备纯独占耗时。C1以S1为对照，144转子俯视平均帧耗时18.920→2.078ms，远景35转子7.923→2.348ms。前后机组均每台216mB/t、256RPM，没有减少吞吐或可见转子获得收益。

这些数据不外推到原生产服、其它硬件或Sodium/Iris/光影。解锁帧率后C1每秒分配及GC次数反而增加，不能称总CPU/内存下降；锅炉近景基本持平，不能称锅炉渲染已优化。S1堵塞窗口的暂态帧P99异常未定位，C1不追认修复。真正单机暂停只有定向JUnit覆盖，真人多人和优化模组组合未补写现场通过。用户的本次采纳及合并授权不等同于全部场景人工验收。

一次独立[规格/质量组合窄审](REVIEW.md)未发现P0/P1/P2；保留一个P3：Flywheel渲染原点重定位当帧先删除旧实例，而新visual直到下一帧beginFrame才创建实例，转子/轴可能缺一帧。该结论来自实际依赖调用链，本批未新增现场复现；PM决定保留原候选身份，后续只针对初始化生命周期处理，不改本次范围。

## 来源与制品核验

阶段A已核准确main的2,228项既有生产/资源/构建输入；排除两项批准修改后，其余2,226项无文本差异。工作区CRLF与Git LF的1,856项差异明确归类，不声称原始字节全部等同。六文件逐字来自原冻结副本，C1最终源码SHA保持。

最终从`34e1b77`导出四生产源的准确Git blob，按原S1单文件、C1三文件的两组javac实参窄编；Java21.0.7、`-encoding UTF-8 -proc:none --release 21`及原冻结classpath保持。两组均首次exit0，六项S1及八项C1 class逐字节等于原增量输出及原候选。六文件Git文本与原来源只存在已分类的CRLF/LF差异，其余2,226项Git输入未改。[完整来源、实参、依赖及class绑定](ARTIFACT.json)保留本轮实际记录。

正式包直接提升原C1候选：3,403,459 bytes、SHA256 `9DA77B19BDF3C30EB9FE1EEAA4FF8A6CC6463E950ACC407C62DFE892E5537230`，没有重新封装、另做assemble或重跑全build/测试/性能矩阵。相对原正式9C55包，九项class修改、五项新增、零删除；另2,368个真实文件载荷保持（695class及全部1,673资源/元数据，后者含manifest）。ZIP的243个目录entry另计，不能把2,611个不变entry误报成真实文件数，也不声称容器压缩字节/时间戳不变。

main已无冲突快进到生产来源`34e1b77`；准确源提交及身份核验后，才将该同字节原候选复制到正式路径`build/libs/create_nuclear_industry-0.1.0.jar`。正式复制SHA已读回一致，继续按用户要求跟踪Git。本次只做必要窄编与身份核对，原已验功能/热工/渲染实现没有被新构建环境替换。

## 保护与原始证据

main的48项tracked状态dirty在开工时冻结，其中正式JAR本地已删除；本次只按授权提升新正式包，其余47项已逐字核验保持、零漂移，结果保存在报告`preserved-main-dirty-after.json`。复用树原世界/config先完整备份到`run/save-backups/thermal-performance-accepted-20261018/`，再完整携带已保存退出的main世界27文件、13,337,924 bytes，路径/数量/SHA零差异。未启动、混合或回写用户世界。

原始集成报告位于集成树`build/reports/thermal-performance-accepted-20261018/`，保留阶段A身份清单、原dirty、世界备份/携带核验与临时核验程序；不把这些工具、世界、诊断Agent或大体积采样文件纳入生产包。原S1/C1补丁、JFR、原生帧、截图、失败日志和独立启动入口仍保存在诊断目录，不推送、发布、安装生产服务器或删除来源树。实际技能与锁定版本记录在组合审查及执行报告中。
