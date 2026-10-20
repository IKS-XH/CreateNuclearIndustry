# ADMIN-RELEASE-01 管理员退出接管候选

2026-10-20，按[整改卡](../../../superpowers/plans/2026-10-20-admin-release-01.md)交付两份同源窄修。实际结果和人工门只在[STATUS](../../../STATUS.md)维护；本文记录本批来源、证据及复看方式，不代表已合入main或用户已手测。

后续用户确认事故候选的逐棒解锁通过，又报告中间堆不能安全拆除；当前同树已另含遗留事故空口整改，继续测试/安装请取[ACCIDENT-SHUTDOWN-03当前候选](../accident-shutdown-03/CANDIDATE.md)。下表4B包保留本阶段基线，主线独立窄修包不因此改变。

## 修复行为与来源

原链路在拆口时撤销运行许可，却仍保留claimed；补壳重成型后正式逐棒校验和客户端投影继续只读。本修复只在全结构已加载、扫描有效且没有自动化接口时结束接管，同一仪表恢复逐棒交互。未成型、暂时卸载、仅拆燃烧室等情况保留原保护，重装管理员不自动启动。

退出清理旧会话、绑定、观测、调节曲线和保存续接意图，不清燃料、冷热液、积热或实际棒位，不解除保存SCRAM/卡死。运行保存信封退出先设置既有安全目标，并在发布逐棒权限前按原规则采样当前红石；真实高输入仍拒绝提棒，低沿恢复沿用原规则。事故锁和托管物料不受影响。

| 用途 | 工作树 / 分支 | 准确来源 |
| --- | --- | --- |
| 当前main功能上的独立窄修 | `E:/MyMC/NewMod/Create_NuclearIndustry-admin-release-fix` / `codex/admin-release-fix` | `ca9f484a992537394ec30acc55677a8884989e6b`，基准`7ea305cad732b7bb26bc8c14504130bae059c12a` |
| 本轮事故手测整改，优先继续使用 | `E:/MyMC/NewMod/Create_NuclearIndustry-accident` / `codex/accident-execution-02` | `96a8421cc6073ea960fd331895f66eb949ea9008`，基准`fc9cbd888864566eaf53c6977b7be838ad231a67`，原事故生产来源`d839bc7e7f281a935a22a8d6121b45e3fa6027f3` |

每树仅提交3项生产文件及2项定向测试。事故树只额外清其既有闭环会话，没有夹带main闭环移除、其它优化或许可增量。主线新树已完整携带main保存世界27文件/13337721 bytes、0哈希差异；事故树保留原手测世界和进度，不回灌main世界。自动测试均用build下隔离世界。

## 候选包

| 用途 | 原始Gradle产物的同字节副本 | 大小与SHA256 |
| --- | --- | --- |
| 主线独立窄修 | [主线候选JAR](/E:/MyMC/NewMod/Create_NuclearIndustry-admin-release-fix/build/reports/admin-release-20261020/candidate-admin-release-main.jar) | 3526970 bytes；`2257F708816870A42F7AE9DA74F1A82D545E580A0DE7FBE4636B4A2A3DF08C1D` |
| 本轮事故整改 | [事故候选JAR](/E:/MyMC/NewMod/Create_NuclearIndustry-accident/build/reports/admin-release-20261020/candidate-admin-release-accident.jar) | 3533989 bytes；`4B37F69DC6997F5BA0FFFED5F722FF99302B0E458FA38FD1041758EEFE9E3499` |

每树只执行一次`./gradlew.bat jar --console=plain`，均1秒/exit0，增量编译与资源处理UP-TO-DATE；未重跑测试或重封包。[主线打包记录](BUILD-main.md)/[身份摘要](ARTIFACT-main.json)与[事故打包记录](BUILD-accident.md)/[身份摘要](ARTIFACT-accident.json)分别绑定已提交源、构建前后2128/2148输入和2453/2455个包内文件，差异为0。

PM重新计算两候选SHA，并独立比对本次3项生产class与编译输出一致。Retrofit测试类、域内模板文件均排除，ZIP仍带2个零长度空目录，不构成测试文件入包；首条PM核验将空目录计入而中止，随后按文件/目录分别核对，没有为此改构建或重打包。两树根`build/libs`均从已核备份恢复原跟踪正式包，工作树干净；安装候选应取上表报告目录中的JAR。main正式包保持3525495 bytes/SHA `17A0A41616C27B4C2FDC7EE5A2E005973AB5C1EB3D1CA1BA16DB6F2B904ACB88`。

## 验证证据

[主线执行报告](EXECUTION-main.md)、[事故执行报告](EXECUTION-accident.md)保留命令、失败和退出码；[唯一组合窄审](REVIEW.md)保留初审发现及同项整改核验，不重复另跑一轮测试或审查。

- 原真实RED确认安全拆口补壳、同一仪表有效重成型后仍claimed/FAULT，正式Slider拒绝且驱动显示只读。
- 审查另发现运行信封首次扫描漏采稳定红石高输入；新增真实RED中高输入却接受START/COMMIT，整体exit1。最小整改后该用例保持高输入SCRAM_LOCKED，实际移除红石块后可按原低沿恢复。
- 最终真实域使用各树`build/reports/admin-release-20261020/review-high-green.log`和同名exit文件：主线6/6、事故7/7，均exit0且正常保存结束。框架数量包含3项原Retrofit，新增共同业务为退出交接、当前格式保存保护、临时失联反例；事故另含真实接管/托管保持反例。
- 定向JUnit主线32/32、事故30/30；最终Service和GameTest窄改不影响State/Controller/Resume对应源码，复用已有XML，不机械重跑。中间组合命令中的真实服失败不计为整体成功。
- 当前保存验证限正式BE读写与首扫；卸载验证限BE回调与正式不可用缓存。未新证明物理ChunkEvent.Unload、完整停服再启动、客户端连续操作或真人多人。

## 集中复看

继续本轮事故手测时，在`E:/MyMC/NewMod/Create_NuclearIndustry-accident`运行`./gradlew.bat runClient`：

1. 正常安全停机后将自动化接口换回外壳，等待正式重成型；保持原仪表不拆，确认逐棒控件恢复可调。
2. 重装接口和管理员，确认保持安全停机，需要重新启动；急停/SCRAM期间仍不能提棒。
3. 原事故接管、回收封装及正常管理员的集中清单继续沿[原事故候选](../../../reviews/2026-10-18/accident-execution-02/CANDIDATE.md)核对，只反馈实际观察，不把自动证据当手测。

已批准[高温与有限熔融物设计](../../../proposals/2026-10-20-accident-thermal-corium.md)尚未编码或改默认配置，等本项复看通过后再细化并派发热危害实施。
