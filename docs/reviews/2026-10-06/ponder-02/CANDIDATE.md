# 反应堆三情景思索候选

**状态（2026-10-06）：** 实现、增量打包、资源检查与PM联合审查完成，等待本批实际播放。工程主线暂停；未启动下一台设备教学。

**功能提交：** `72b8ff107e525e0845dcc766ed9875d6e7af4450`，分支`codex/ore-acquisition`。反应堆原11项入口提供三个可分别选择的情景：搭建、运行与停机、装料与换料。正文为6＋7＋6段，聚焦玩家操作与设备规则，删除开发措辞和重复Create基础知识。每条故事线使用独立模板，保留关键帧与回放。

Java场景需要重启客户端。从主目录同级候选启动：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
.\gradlew.bat runClient
```

在JEI或创造栏悬停反应堆外壳、仪表端口等原有组件，按提示打开思索（默认长按W），分别选择三情景。仅按[播放清单](./MANUAL-CHECKLIST.md)检查教学；不重复已通过的反应堆功能测试。

本候选同时包含离心机R1提示时序和R2玩家文案精简。离心机Java、模板与中英文值在本次重构中保持不变，其播放门仍按[原清单](../ponder-01/MANUAL-CHECKLIST.md)确认。两批教学尚未合入main，主目录runClient仍是已验收的功能版本。

制品：候选`build/libs/create_nuclear_industry-0.1.0.jar`，2,159,125字节，SHA-256 `65816B99DB2CBE667493F0A77F086D2A0E9201275EEA8C74676857DF976BB6EE`。已确认三模板、两语言文件与JAR资源一致；相关合同测试6项通过，最终增量assemble退出0。检查结果不能替代真实播放。

证据见[执行报告](./implementation.md)与[PM审查](./REVIEW.md)。候选制品只在同级工作树，不搬移、清理用户测试世界或修改启动配置。
