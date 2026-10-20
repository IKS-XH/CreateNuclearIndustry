# MIT许可与原创美术开放实施卡

> 执行方式：沿用用户已授权的PM派发与验收。唯一执行者只改下列元数据/打包写集，不做Git写操作；PM负责声明文档与提交。

**Goal:** 按用户2026-10-20要求，原创代码、文档及非Logo原创美术使用MIT，Logo和第三方许可边界明确，源码与JAR声明一致。

**Architecture:** 根LICENSE保留标准MIT全文；NOTICE界定Logo和第三方例外。沿用现有jar任务携带三份声明，只更新两个现有许可字段。

**Tech Stack:** Minecraft1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Gradle9.0.0；不改版本、依赖、功能或发布方式。

**Spec:** 用户本轮明确要求及根[LICENSE](../../../../LICENSE)/[NOTICE](../../../../NOTICE.md)；基准main `e6a21b7737ac81f183489590ee3610848aad9f1d`，开工时工作区干净。结果见[当前状态](../../../STATUS.md)及[集成记录](../../../reviews/2026-10-20/mit-license/INTEGRATION.md)，本卡保留原执行范围。

## 范围与写集

- PM：LICENSE、NOTICE.md、README及受影响治理/美术/文档入口、STATUS、最终集成记录；原始第三方许可证和历史验收不改。
- 唯一执行者：`gradle.properties`、`src/main/resources/META-INF/neoforge.mods.toml`、`build.gradle`，报告仅`build/reports/mit-license-20261020/IMPLEMENTATION.md`。
- 两个许可字段均为`MIT`；TOML紧邻中文注释提示Logo/第三方例外见NOTICE，不扩改过时简介。
- 当前`jar`任务添加根`LICENSE`、`NOTICE.md`，以及原文`tools/art-assets/industrial-fluid-visual-01/CREATE-LICENSE.md`→`META-INF/licenses/Create-LICENSE.md`；不修改其字节，不重打原依赖、不改排除名单或加入开发工具。

## 实施与核验

- [ ] 执行者读取AGENTS/治理/状态、本卡与Minecraft发布、modding/testing技能；仅完成三文件差异和静态检查，不先构建或提交。
- [ ] PM审查许可全文、Logo路径、Create来源和元数据/打包差异；保存旧正式JAR及身份，提交准确生产来源。
- [ ] 对已提交源仅运行一次增量`gradlew.bat jar --console=plain`，核进程exit0、JAR许可字段及三份声明逐字一致；与旧JAR比较非声明条目，功能class/资源须不变。无玩法改动，不跑JUnit/GameTest或新客户端手测。
- [ ] PM将核验后的正式JAR纳入Git，更新STATUS及集成记录；保留辅助工具独立树、事故候选和用户存档，不推送/发布。

## Review Focus

- MIT不能覆盖Logo，也不能重新许可Create保留权利的模型/调色派生素材；独立原创方向箭头仍MIT。
- 根标准MIT文本不附加限制条款，例外在NOTICE；JAR仅许可说明变化，原功能和素材像素不变。
- 当前README、Gradle、NeoForge声明须一致；旧归档/第三方文本不批量替换，唯一正式JAR必须来自已提交来源。
