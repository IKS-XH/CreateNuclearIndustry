# 离心机思索：首台教学候选

**状态（2026-10-06）：** 用户首轮播放发现提示重叠；R1时序整改、增量打包及PM审查完成，等待同一台播放复验。工程主线保持暂停；本台通过后才安排下一台设备。

**当前功能提交：** `91941efd02356098daa40c41a88259d1a5e3198a`，分支`codex/ore-acquisition`；首版为`75265de8c50aa66c7ef1c3879e989b0c977baf8f`。本台教学尚未合入main；此前封存、有序配方和产热取整已合入main。

从同级候选启动：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
.\gradlew.bat runClient
```

在创造栏或JEI中悬停离心机，按提示打开思索（默认长按W）。八段依次讲解用途、两格放置、底部动力、顶部进浆、配比、双粉过滤与回水、暂停排查和轴承维修。场景里的创造马达和加工物料变化均为示意。

本次修改Java时序，需要退出并重新启动候选客户端，资源重载不能替代重启。重点回放“配比→双粉/回水→暂停排查”，确认前一提示完全退场后下一提示才出现。已完成的播放检查无需重复，尚未确认的项目仍按[播放清单](./MANUAL-CHECKLIST.md)验收；不重复已通过的离心机功能测试。

当前候选JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，2,153,062字节，SHA-256 `C201D34EC50C15CAE346955F3C84B2621CEB0955078278F0BDCD5FE2B704AEEB`。路径随之后打包更新，本SHA对应R1功能提交。

R1证据见[时序整改报告](./timing-r1.md)与[PM审查](./REVIEW.md)，构建日志保存在候选`build/reports/extension/DEVICE-PONDER-01/timing-r1/candidate-assemble.log`。首版证据保留在[执行报告](./implementation.md)。R1没有运行GameTest或启动客户端，整改后实际画面仍待用户确认。
