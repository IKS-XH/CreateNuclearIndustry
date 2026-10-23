# 已验事故链与R1外观联合合入

2026-10-23，用户在本对话明确确认“测试过了，合并进去吧”，沿先前“一块测试一块合并”的要求合入。验收范围是既有事故功能反馈、最终R1外观和创造拆除零掉落；不将其扩大为未报告的物理卸载、真人多人或全部光影环境。

## 来源与并集

主线生产基准`889f7e7e`、文档基准`cbd28c42`；事故最终来源`c2b809154eec28efc7ca4069fa338b22815b9024`。完整链包含`d839bc7e`局部事故接管/持久账本/封存、`96a8421c`接口退出、`37f50b9e`遗留空口安全重绑、`9f53a7d5`高温固体/有限火点、`afb16899`独立有限熔融物、`1985d1b3`创造拆残骸零掉落及本批R1美术。

复用`E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration`、分支`codex/accident-accepted-20261023`做标准merge。只有六份文档文本冲突，由PM保留当前主线状态并合入实际事故设计、配置/注册/配方；生产代码自动合并。唯一手工生产修正为仪表事故锁定/结束接管中删除两处已不存在的`setAutomationLoadFollowSession(null)`调用，不恢复主线已删除闭环。保main累计优化、协议5、统一调棒/恒功率/保护、MIT及三许可入包、已验护具与辐射计动态指针。

相对main净78生产/测试/构建路径，完整2407输入的来源由原始`input-origins.json`列明：2202共享、127保main、71沿事故、6自动并集、1删除两旧调用。42生产Java、29资源、6测试及build.gradle；其中build.gradle/管理员Service/State/仪表/双语/mixins为七个并集入口。语言仅加事故词条及将封装桶名称扩大为核废料桶，保其它main键值。18事故包源码与已验候选内容相同；17美术允许路径规范化内容与R1一致、16处理资源绑定，两个客户端class与已审R1逐字节相同。不重新生成美术。

源稿、导出器、原始/整改预览与报告随来源保留，原版父模型注明真实来源/版权，原创源与素材沿MIT。PM仅去掉两份新增ASSETS说明的尾随双空格；无生产资源变化。原[素材交付](../../../art/reports/ART-ACCIDENT-CORIUM-22-DELIVERY/README.md)、[R1限定复审](../../../art/reports/ART-ACCIDENT-CORIUM-22-R1/REVIEW.md)与已反馈游戏体验分开记录。

## 验证与既有失败

[EXECUTION](EXECUTION.md)和[SOURCE](SOURCE.json)索引真实失败/成功证据，原始日志与大清单保留在集成树`build/reports/accident-accepted-20261023/`。

- 初次compileJava真实RED仅为两条旧闭环API引用。最小修正后现有retrofit服务端域一次7/7业务、无SKIP，Java/Gradle exit0且全部维度正常保存，验证退出逐棒权限、保存/稳定红石高输入及事故托管交接。
- 由实际跨模块API交接失败触发唯一全JUnit，854项中840通过、3失败、11跳过，Gradle exit1，不能称全量通过。失败为HeatExchangerBoilerBridgeTest的两条“配置尚未加载”及ReactorFissionDifferentialTest的一条scenario0/tick4全状态差分。
- 在尚未合事故的准确main `aa23d692`只运行这两个测试类一次，15项中同名同异常三失败、exit1，完整异常与集成逐字相同。原主线输入/3D dirty/根包缺失状态保持。此证明失败是原基线已有，未修其根因、不放宽断言，也不将差分失败定性为纯浮点误差。
- 既有04A/04B/创造拆除的长时钟、物料守恒、热伤/辐射、正常保存/来源消失和预算证据按相同来源复用；本次没有重复长矩阵或要求用户重测同一外观。一次并集窄审单列[REVIEW](REVIEW.md)，制品身份见后续[BUILD](BUILD.md)和[ARTIFACT](ARTIFACT.json)。

## 世界与保护

当前已验事故世界在21:05:29正常保存退出。源/目标session.lock独占核实后，将旧目标27文件完整移入本树`run/save-backups/accident-accepted-20261023/新的世界`，再完整复制当前29文件/13367770bytes；逐路径bytes/SHA与源前后均一致，不混合或打开世界，不回写main/事故原件。

主工程3D工具dirty和日期名EF3C旧包保留；正式路径原缺失，在提升准确新JAR时才重新写入，不将删除夹入源码提交。源事故树、旧美术树、全部候选/失败证据保留；本次无推送、发布或删树。根跟踪JAR与测试临时日志的备份/恢复及最终来源/SHA由制品记录列明。
