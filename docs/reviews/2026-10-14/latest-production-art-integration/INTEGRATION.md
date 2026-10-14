# 最新离心机与屏蔽装配台美术集成

2026-10-14，用户在确认锅炉修复与应力余量闭环均手测通过、要求合入之后，追加要求最新美术也合入。功能联合验收与main`4149a9f`见[前批记录](../../../archive/2026-10-15/reviews/2026-10-14/load-follow-boiler-accepted-integration/ACCEPTANCE.md)。本次按[活动卡](../../../archive/2026-10-15/plans/2026-10-14-latest-production-art-integration.md)只接已交美术来源及组合正式包。

## 来源与Git合并

美术树`Create_NuclearIndustry-art-studio`的479条非日志/缓存来源由PM净提交为`794d16a0dee67bb4ec611355535ea408eaac5374`：84文档、5生产Java、49资源、3定向测试及338可编辑素材/工具。仅排两日志及两Python缓存，原路径保持；设计历史、失败截图和原素材备份按报告引用保留。来源基线为`88b8fd2`。

main在纯文档提交`41dce14`之后，通过双亲合并`497f64b0a8e053f1e6048cb8a2e1bb4cbd170ada`接入，树为`53f6860300d6be2c7a645fff5396eee56c9045d9`。54生产路径中31与原美术基线共路径、23新增，没有代码冲突或main dirty重叠。仅PLAN/README两文档由PM组合美术新增46行和现行main正文，再附当前集成状态；原主线整理保持。PM独立核479写集精确、全部57项src生产/测试与art提交一致、暂存区无夹带。

离心机五模型/六贴图/底部旋转轴、屏蔽装配台八格机壳/物品/黑窗修正及八分件工序动画已接入。服务器热工、库存、结构、协议与新闭环/锅炉出汽源码保持；未把仅含旧逻辑的dd6美术整包替换main。

## 验收与证据范围

离心机外观及屏蔽装配台黑窗的美术对话用户验收保持。15C动画按本次直接合并指令接入，尚未报告Minecraft两工序/中途停恢复的实际手测，不因合入补记通过。

复用原14B 3/3、22/22，15B-R1原生16模型与37/37，15C最终17/17及两个矩阵导出各单项1/1和原组合窄审，不机械重跑。唯一执行者本次只读补核五身份记录128条绑定、49资源及14相关class与dd6一致；辅助脚本初次class路径解析错误保留，修核入口后读同一包通过，没有改生产或重打旧美术包。

原main105条dirty按原字节冻结，f23主包和dd6美术包精确副本保存在`build/reports/latest-production-art-integration/preserved/`。世界、运行配置、已有预览服务和其它候选原位，没有新分支/存档复制或候选进度回写。

## 组合制品

准确源`497f64b`完整导出6592个Git文件、1968项src/main生产来源；全部输入核完后，唯一`assemble --no-daemon --console=plain`实测27.9266017秒、exit0。36条既有API弃用警告、0编译错误，没有全build、GameTest或Minecraft手测。

正式JAR为`build/libs/create_nuclear_industry-0.1.0.jar`，**3304595 bytes**，SHA256 **`33da90021f938d2349e9d88c4522df72bfe587a4f1ca388d3158d34d9e8e9e8b`**。691class／1569资源和生成Manifest共2261载荷与全部应打包输出逐字节一致；已审排除规则排掉9测试class及7模板，美术JUnit/离线探针/工具无泄漏。54原交付生产来源及干净Git输入无漂移。

677个非美术class和325个非美术资源与旧f23包保持原字节；另外1195条UTF-8文本资源只有旧工作树包CRLF到干净Git源LF的差异，全部新字节等于准确Git资源blob，非换行内容差异为零。严格字节门最初failed保留，PM独立逐项读回only_eol、Git字节匹配及CRLF计数后在卡中接受该精确表示特例，不改源码/拼包或重复构建。不能将此描述为所有无关资源原字节相同。

PM核`built-33da9002.jar`的SHA后将其复制到正式路径；旧f23仍完整保存在preserved/jars/main-f23.jar，dd6美术包及其原目录不改。执行者最终补核和PM制品/Git读回按本报告及本批FINAL记录保存。源与新包纳入Git，后续文档状态提交不改包对应的生产来源。

原始证据位于`build/reports/latest-production-art-integration/`：STAGE1/STAGE2、来源清单/冻结、Git导出身份、assemble.log/result、严格制品报告及EOL分类。暂存不夹带原dirty、未删除分支或候选树、没有推送或发布；其它工程主线不自动接续。
