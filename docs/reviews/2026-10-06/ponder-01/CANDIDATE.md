# 离心机思索：首台教学候选

**状态（2026-10-06）：** 本台实现、必要增量打包及PM审查完成，等待客户端播放验收。工程主线保持暂停；本台通过后才安排下一台设备。

**功能提交：** `75265de8c50aa66c7ef1c3879e989b0c977baf8f`，分支`codex/ore-acquisition`。本台教学尚未合入main；此前封存、有序配方和产热取整已合入main。

从同级候选启动：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
.\gradlew.bat runClient
```

在创造栏或JEI中悬停离心机，按提示打开思索（默认长按W）。八段依次讲解用途、两格放置、底部动力、顶部进浆、配比、双粉过滤与回水、暂停排查和轴承维修。场景里的创造马达和加工物料变化均为示意。

只按[播放清单](./MANUAL-CHECKLIST.md)检查完整播放、关键帧回放、双语和原入口；不重复已通过的离心机功能测试。

最终候选JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，2,153,059字节，SHA-256 `0BE6F77CD015F9C3BC290B2D08A9AF07C0C1C840F917BD5EAFA36E908782D862`。路径随之后打包更新，本SHA对应上述功能提交。

证据见[执行报告](./implementation.md)与[PM审查](./REVIEW.md)；构建日志保存在候选`build/reports/extension/DEVICE-PONDER-01/candidate-assemble.log`。没有运行GameTest或客户端，实际画面仍待用户确认。
