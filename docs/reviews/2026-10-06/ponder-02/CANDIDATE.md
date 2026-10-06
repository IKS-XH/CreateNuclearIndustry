# 反应堆四情景思索候选

**状态（2026-10-07）：** [02-R1](../../../superpowers/plans/2026-10-06-device-ponder-02-r1.md)已打包并通过PM审查，等待集中播放。初版发现的自由布局/多口说明缺失与背面冷热口已在源码/模板中整改，并新增棒列关系情景；真实可见性与动画仍待复验。工程主线暂停，未启动下一台设备教学。

**当前R1功能提交：** `c2bb5836a8d9dd34c2bf6677f1442bb64a8621a4`，分支`codex/ore-acquisition`；初版历史提交`72b8ff1`保留。原11项入口提供搭建、燃料棒与控制棒、运行与停机、装料与换料四情景，正文分别8/8/7/6段。补自由布局、多冷热口、前侧冷却回路以及并联超频/控制约束/无控制棒无法中止的规则。每条使用独立模板，保留关键帧与回放。

Java场景需要重启客户端。从主目录同级候选启动：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
.\gradlew.bat runClient
```

在JEI或创造栏悬停反应堆外壳、仪表端口等原有组件，按提示打开思索（默认长按W），分别选择四情景。仅按[播放清单](./MANUAL-CHECKLIST.md)检查教学；不重复已通过的反应堆功能测试。

本候选同时包含离心机R1提示时序和R2玩家文案精简。离心机Java、模板与中英文值在本次重构中保持不变，其播放门仍按[原清单](../ponder-01/MANUAL-CHECKLIST.md)确认。两批教学尚未合入main，主目录runClient仍是已验收的功能版本。

当前R1制品：候选`build/libs/create_nuclear_industry-0.1.0.jar`，2,163,923字节，SHA-256 `67FF2C85E5565FCD15CF701CC12AEDECA3B22813A23CAAA6D1D77ADC5479B1CF`。本轮6项合同通过、增量assemble退出0；四模板/两语言与JAR资源一致。检查结果不能替代真实播放，未合入main。

R1证据见[执行报告](./r1-implementation.md)和[审查记录](./R1-REVIEW.md)；保留初版[执行报告](./implementation.md)与[PM审查](./REVIEW.md)。候选制品只在同级工作树，不搬移、清理用户测试世界或修改启动配置。
