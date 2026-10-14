# 最新离心机与屏蔽装配台美术合入

用户在明确锅炉主动出汽与应力余量闭环手测均通过、要求合入之后，追加要求“美术分支最新的变更也合并进去”。本卡只接当前已交付美术来源并生成包含新逻辑的正式主包。

## 冻结范围

- main当前双亲合并提交`4149a9ff43a4063c2516c2306acf02ad6f475c75`，已含闭环02与锅炉修复，正式JAR为3248823B／`f23b5cf49a3bc6ec464f14efb722af1c65067658a51b378e336379be0bab0f57`。
- 来源为同级`Create_NuclearIndustry-art-studio`／`codex/art-studio`，HEAD`88b8fd2`上的未提交14A/14B及15A/15B/R1/15C已交来源。离心机外观与屏蔽装配台黑窗已由美术对话用户反馈验收；15C动画按本次用户指令合入，但不补记尚未报告的Minecraft动画手测通过。
- 接入离心机五模型、六贴图及输入轴渲染；屏蔽装配台静态八格机壳/物品/窗材质、八活动分件、客户端动作数据及纯算法；同源SVG、设计/预览/报告和定向测试一并保留。
- 不以旧美术JAR或整份Java目录覆盖main，不改反应堆/锅炉/管网/管理员服务端逻辑、配方、协议或运行配置。原main dirty、日志、世界、配置、既有预览服务保持。

## 唯一执行者

执行者先只读核来源身份，给出精确非日志/缓存写集及main与美术最终内容的重叠表。可在main的`build/reports/latest-production-art-integration/`创建本批清单、校验/干净Git导出/Gradle调用辅助脚本和报告；生产来源仅在PM确认存在实际冲突并扩卡时才能修改。禁止任何Git写操作及核心文档写入，不派其他执行者。

PM负责净提交美术已交来源、保护dirty、合并Git对象与主目录更新。合并完成并通知准确源提交后，执行者只从该已提交Git树导出完整源码，核完导出再执行唯一必要`assemble --no-daemon --console=plain`；允许本进程E盘Gradle/tmp覆盖并恢复，禁止全局配置变更、停止其他服务、清盘或改build脚本。若失败先保原始证据并报告，不自行修代码或机械重试。

验证导出与Git源一致、新包全部class/资源与本次输出一致、测试载荷不泄漏，并对比原f23包确保新增美术写集外的生产资源与服务端class保持。保留原f23包、旧美术dd6包和阶段失败，不覆盖用户存档。最后PM备份/替换/跟踪正式JAR并独立核Git blob及原dirty字节。

本次实际制品核发现旧f23工作树包的1195条UTF-8文本资源使用CRLF，而干净Git源及当前输出为LF；`eol-delta-classification.json`逐项证明1195/1195只含该换行表示变化、1195条新字节等于准确源497f64b的Git资源blob，非换行差异为空。PM已独立读回并接受这一精确封包特例：不改源或手工改包、不再次构建，严格逐字比较失败保留，最终结论必须区分325条原字节资源及1195条仅换行等价资源；677条非美术class仍要求原字节保持。任何其它差异仍不允许。

## 证据复用与审查

复用14B的3/3及22/22、15B-R1原生16项及37/37、15C最终17/17及两个矩阵导出单项、原一次组合窄审。仅做本次组合来源及制品补核；无代码冲突/新实现时不重跑旧套件、业务GameTest或全build。若发现新的行为风险，再精确扩定向检查，不冒称assemble为build或Minecraft视觉验收。

实际应用技能：minecraft-modding、minecraft-testing、minecraft-resource-pack、minecraft-ci-release、finishing-a-development-branch、verification-before-completion；版本沿用仓库锁定MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280。
