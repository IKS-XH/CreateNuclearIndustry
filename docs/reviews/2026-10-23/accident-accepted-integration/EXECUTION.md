# 已验事故链与R1集成：阶段A

2026-10-23。基准main `cbd28c420ee2715cc024d8bed74399dfa64128ed`，事故来源`c2b809154eec28efc7ca4069fa338b22815b9024`，当前树`E:/MyMC/NewMod/Create_NuclearIndustry-du-accepted-integration`、分支`codex/accident-accepted-20261023`。PM已发起标准merge；执行者未Git写、未处理文档冲突，未jar/build/assemble。现交一次并集窄审及PM源码提交。

已读主工程AGENTS、治理5.1/5.2/6、STATUS、集成卡/联合卡、04B/创造拆除及R1现成报告。实际使用本机`minecraft-modding/testing/resource-pack/ci-release`技能核锁定API、真实域、原素材与来源；MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6保持。

## 保护与存档

开工先将本树EF3C根JAR（3572469bytes）及A061根日志按原字节备份到`baseline/`，见`BASELINE.json`。JAR全程保持EF3C；JUnit生成的根日志已另存`runtime-logs/integration/`，不恢复或暂存。

源事故树21:05:29全部维度保存，复制时仅Gradle daemon。源与目标原session.lock均只读独占成功并释放；验证目标/备份绝对路径在本树且无链接，原目标27文件完整Move到`run/save-backups/accident-accepted-20261023/新的世界`并核原件一致，再完整复制事故世界29文件/13367770bytes。源前后及目标逐路径bytes/SHA一致，未混合/回写/打开世界。见`prior-world-before.json`、`world-copy.json`。复制记录最初总bytes汇总为空，随后仅从已核文件清单补正确求和，未重复搬动或复制。

## 并集与唯一手工修正

2407个生产/测试/资源/Gradle/许可输入的来源、原/并集SHA见`input-origins.json`。相对main净78路径：42生产Java、29资源、6测试及build.gradle；精确清单为`paths.json`。其中71路径沿事故来源、6路径自动并集、1路径在并集上删除失效调用；其余2202共享、127保main。完整事故包18源与候选内容保持，未重做04A/04B长矩阵。

七个特殊并集文件为build.gradle、ReactorAutomationService/State、ReactorInstrumentPortBlockEntity、双语语言及mixins JSON。保main MIT/三许可打包声明、协议5、无应力闭环、优化和护具/仪表动态指针，叠加已验事故托管/退出/守卫/排除。语言仅加事故词条及把封装桶名称改为候选“封装核废料桶”，其它main键值保持；main既有78项护具/仪表来源内容保持。

首次`compileJava processResources`真实exit1，只有仪表105/213两处找不到已删除的`setAutomationLoadFollowSession(null)`。原自动合并快照/差异和RED见`auto-merged-inputs.json`、`auto-merged-source.diff`、`01-compile.log`。按卡仅在`ReactorInstrumentPortBlockEntity.java`删除这两行，不恢复字段/API，不改其它源、测试或构建；最终手工差异见`manual-source.diff`。P1DataContractTest未失败、未修改。

R1全部17允许路径内容保持：8项原字节相同、9项仅Git LF/CRLF不同；16处理资源均与当前源逐字节绑定，两个客户端class与原已审清单原字节一致。见`r1-identity.json`。生产素材未重画/重导，原像素、动画、预览及用户联合手测反馈复用。23项美术源/工具沿来源，唯ASSETS.md由PM清理尾随双空格，非生产变化；其它文档归PM。828个开发main编译class清单在`compiled-classes.json`，这不是最终包计数。

## 实际验证及失败核界

| 证据 | 实际结果 |
| --- | --- |
| `01-compile.log` | 两个旧闭环API引用编译RED，exit1 |
| `02-retrofit-green.log` | 修正后增量编译/资源处理及既有retrofit域一次7业务全通过、0SKIP；Java/Gradle exit0，全部维度正常保存 |
| `03-junit.log`、`junit-original/` | 因真实API交接失败按PM追加授权唯一全JUnit：854项，840通过/3失败/11跳过，exit1；不称全量通过 |
| `04-main-baseline.log`、`main-baseline-xml/` | PM授权未合事故main `aa23d692`仅两类一次定向：15项/3失败、exit1；三条测试名和完整异常message均与集成结果完全相同 |

retrofit实测覆盖事故已托管且几何零接口时保事故锁/库存、真实退出逐棒权限、保存续接、稳定红石高输入及原保留功能。没有SKIP伪计业务。首次独立测试目录server.properties缺失后由原生生成，非业务失败。

三条JUnit失败保留原断言：HeatExchangerBoilerBridgeTest两条在尚未加载配置时经pauseHeat/HeatExchangerLine读取condensationSettings；ReactorFissionDifferentialTest一条在scenario0/tick4全状态严格差分不等。只证明原main同失败，未定位/修复裂变差分根因，不将其定性为纯浮点误差。精确基线输入/异常对照见`MAIN-BASELINE.json`、`main-baseline-inputs.json`；未扩大实现或重复全套。

main基线运行是本轮唯一额外树操作且经PM明确授权：仅既有两测试及普通输出，开工main根JAR/日志均不存在，原缺失状态及3D工具dirty/全部生产输入保持；JUnit新日志存`runtime-logs/main-baseline/`留PM处理。集成源未因文档卡新提交改变。

最终来源与证据摘要为`SOURCE.json`；源码差异检查通过。已反馈的联合外观/创造拆除人工门按用户原范围复用，不新开视觉门，不补记物理卸载或真人多人。阶段A执行交付，停止等待窄审、PM合并提交和阶段B唯一jar授权。
