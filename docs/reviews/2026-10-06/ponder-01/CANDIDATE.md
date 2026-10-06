# 离心机思索：首台教学候选

**状态（2026-10-06）：** R1提示时序整改与R2玩家文案精简均已打包并经PM审查，等待同一台播放验收。用户随后明确追加反应堆三情景重构；[当前同级候选](../ponder-02/CANDIDATE.md)已包含两台教学，本台源码与文案保持不变，播放门未取消。工程主线保持暂停，当前停止自动推进。

**当前功能提交：** `d17376e94c0f3c770386619d075fc2adc1c6f1c7`，分支`codex/ore-acquisition`；R1为`91941ef`，首版为`75265de`。本台教学尚未合入main；此前封存、有序配方和产热取整已合入main。

从同级候选启动：

```powershell
Set-Location 'E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition'
.\gradlew.bat runClient
```

在创造栏或JEI中悬停离心机，按提示打开思索（默认长按W）。八段依次讲解用途、两格放置、底部动力、顶部进浆、配比、双粉过滤与回水、暂停排查和轴承维修。场景里的创造马达和加工物料变化均为示意。

本候选包含Java时序与后备文案变更，需要退出并重新启动候选客户端，资源重载不能替代重启。重点回放“配比→双粉/回水→暂停排查”，确认前一提示完全退场后下一提示才出现。已完成的播放检查无需重复，尚未确认的项目仍按[播放清单](./MANUAL-CHECKLIST.md)验收；不重复已通过的离心机功能测试。

R2同步精简中英文八段字幕，去掉开发说明、重复进料说明和原生管道基础知识，保留本机的安装、动力、进浆、配比、出料、暂停与维修规则。与提示时序在同次播放确认，不新增功能验收轮次。

R2当时的候选JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，2,152,557字节，SHA-256 `49B0E15C95E500933A66841D70F999FDBB13E3A94D9E5DAD6ED899687CF97DB0`。路径现已随反应堆三情景打包更新，本SHA仅对应R2功能提交；当前大小/SHA见上述新候选页。

R2证据见[文案报告](./copy-r2.md)，当前构建日志保存在候选`build/reports/extension/DEVICE-PONDER-01/copy-r2/candidate-assemble.log`。保留[时序整改报告](./timing-r1.md)、首版[执行报告](./implementation.md)及[PM审查](./REVIEW.md)。R2没有运行JUnit/GameTest或启动客户端，当前实际播放仍待用户确认。
