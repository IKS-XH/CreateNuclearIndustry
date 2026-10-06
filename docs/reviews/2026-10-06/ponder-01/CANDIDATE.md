# 离心机思索：首台教学候选

**当前状态（2026-10-07）：** 用户已明确确认离心机和反应堆思索手测通过，两台播放门关闭并单独合入main。见[联合验收](../../2026-10-07/ponder-acceptance-01/ACCEPTANCE.md)。新版锅炉重构继续保留独立手测门。

主目录已包含最终教学，启动方式：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry'
.\gradlew.bat runClient
```

## 交付前候选与播放要求（历史）

**状态（2026-10-07）：** R1提示时序、R2玩家文案和R3回水布局均已打包并经PM审查，等待同一台播放验收。回水改为独立侧直线支路，与两路粉末输出分开。反应堆02-R1四情景保留原样；两台教学的播放门未取消。工程主线暂停，不自动推进其他设备。

**当前R3功能提交：** `c4e486a`，分支`codex/ore-acquisition`；R2为`d17376e`，R1为`91941ef`，首版为`75265de`。本台教学尚未合入main；此前封存、有序配方和产热取整已合入main。

从同级候选启动：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
.\gradlew.bat runClient
```

在创造栏或JEI中悬停离心机，按提示打开思索（默认长按W）。八段依次讲解用途、两格放置、底部动力、顶部进浆、配比、双粉过滤与回水、暂停排查和轴承维修。场景里的创造马达和加工物料变化均为示意。

本候选包含Java时序与后备文案变更，需要退出并重新启动候选客户端，资源重载不能替代重启。重点回放“配比→双粉/回水→暂停排查”，确认前一提示完全退场后下一提示才出现。已完成的播放检查无需重复，尚未确认的项目仍按[播放清单](./MANUAL-CHECKLIST.md)验收；不重复已通过的离心机功能测试。

R2同步精简中英文八段字幕，去掉开发说明、重复进料说明和原生管道基础知识，保留本机的安装、动力、进浆、配比、出料、暂停与维修规则。与提示时序在同次播放确认，不新增功能验收轮次。

R3移除回水管向粉末输出侧的弯折，另侧直线连接管道、泵和回水罐，泵传动同步搬迁。轮廓高亮只覆盖回水线路及驱动，蓝色指示线通向独立水罐。两路粉末输出、八段文案、提示寿命和关键帧时序均不改；同次回放“双粉/回水”确认清晰度，不重复设备功能测试。

当前R3制品：`build/libs/create_nuclear_industry-0.1.0.jar`，2,163,989字节，SHA-256 `2510F856393D4743C96C0A5D1FF3DAA4A69BF34EA66996D6CEEBB9D55660B494`。初次打包后PM发现旋转指示落在储罐上，已修正到齿轮并一次增量复建通过（退出0）。PM只读确认105个合法NBT位置、新方向/接线、旧折线移除、8段字幕/8关键帧/14项idle不变及JAR模板与源码字节一致；复用入口与玩法证据，未跑JUnit/GameTest或客户端。[R3报告](./layout-r3.md)及候选`build/reports/extension/DEVICE-PONDER-01/layout-r3/final-assemble.log`记录最终交付。

R2当时的候选JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，2,152,557字节，SHA-256 `49B0E15C95E500933A66841D70F999FDBB13E3A94D9E5DAD6ED899687CF97DB0`。路径随后续教学打包更新，本SHA仅对应离心机R2当时的功能提交；最新大小/SHA见本页R3制品。

R2证据见[文案报告](./copy-r2.md)，当前构建日志保存在候选`build/reports/extension/DEVICE-PONDER-01/copy-r2/candidate-assemble.log`。保留[时序整改报告](./timing-r1.md)、首版[执行报告](./implementation.md)及[PM审查](./REVIEW.md)。R2没有运行JUnit/GameTest或启动客户端，当前实际播放仍待用户确认。
