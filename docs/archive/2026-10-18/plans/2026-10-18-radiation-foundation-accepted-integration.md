# 辐射首批已验候选合入

2026-10-18，PM。用户已报告原辐射清单全部通过，并再次报告头罩护目镜补充手测通过，要求继续系统开发。按原第一批完成定义，先合入已验基础，再接已批准设计的第二批。现行结果只在[STATUS](../../../STATUS.md)维护，本卡保留集成写集和验证依据。

## 范围与分工

main基线`020cae4ba3d919ea90b05cfc305dade27c5191bb`，正式包9DA77B19…；已验辐射来源`cc7d4fdc5918d833f40ad0a5e289ea85dfbf8f38`，候选3429789 bytes、SHA74950D69…。只合已验第一批及头罩，不提前写事故执行、废料或新数值。

复用同级干净树`Create_NuclearIndustry-du-accepted-integration`，从main建立`codex/radiation-foundation-accepted`。按约定备份该树原世界/config，再完整携带已保存退出的main世界；原辐射候选及其新手测进度保留原位，不回写或覆盖。唯一执行者处理功能资源冲突和核验；PM管理Git及文档。

预合并检测仅两语言JSON和五原版护甲标签冲突：执行者仅可在合并后的`src/main/resources/assets/create_nuclear_industry/lang/{en_us,zh_cn}.json`及`src/main/resources/data/minecraft/tags/item/{head_armor,chest_armor,leg_armor,foot_armor,trimmable_armor}.json`取两边已验增量并集，`replace=false`与既有值保持，不覆盖最新贫铀内容。其他源码/资源/Gradle自动合并结果须证明等于main叠加已验辐射净增量；若有额外冲突或语义问题先报PM，不自行改已验逻辑。

报告与核验程序只能写集成树`build/reports/radiation-accepted-20261018/`，正常Gradle输出允许；不得改main、原候选、核心文档、存档或用户实例/服务器，不做Git写或子代理派发。保留main47项无关dirty和原正式JAR，只有准确来源提交/核验后PM才可提升新正式包。

## 必要验证与交付

先解决七份JSON并核两方键/标签均保留；复用原41项JUnit、13＋2真实服、原两次组合审查及两次用户手测反馈，不重启原业务/性能/存档矩阵。只对本次资源并集跑必要装备/数据定向验证，不运行clean、全build或强制重跑。一次组合窄审只核冲突并集、最新main实现保留和制品来源，不重审整套辐射。

PM提交合并来源后，从准确已提交输入做一次增量assemble并绑定源码/全部入包class/资源；排除辐射GameTest类/模板，保留S1/C1、DU外观及已验main修复。执行者报告准确来源、命令、退出、长度/SHA及新旧载荷范围，PM核对后快进main、强制精确暂存已跟踪正式JAR并核暂存Git对象。不推送/发布/删来源树。

必读AGENTS、治理5.1、STATUS、第一批卡与交付证据；实际使用minecraft-modding/testing/ci-release及资源JSON相关规则，锁定MC1.21.1/Java21/NF21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6。技能通用全量/清理流程不覆盖项目约定。完成后归档此集成卡和已完成首批卡；第二批按独立精确计划及既有自动派发授权接续。
