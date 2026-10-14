# ART-SHIELDED-ASSEMBLY-15B 最终客户端候选制品

2026-10-14，原唯一预览/包执行者。负责人已转交同次组合审查无剩余必改后，本批唯一增量assemble实际退出0，构建阶段10.619秒（脚本最终输出含记录写入共10.656秒）。新客户端候选 **2,812,280B / SHA256 a92a0bed0f948fc000b58800cad34f84c03ffb314b167fea3a839f117b12ff7f**，位于美术树build/libs/create_nuclear_industry-0.1.0.jar；未替换main或发布/Git。

## 实际命令与环境

唯一调用 `preview/build-client.ps1 -Reviewed`，实际在 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio` 运行：

```text
./gradlew.bat assemble --no-daemon
-Djava.io.tmpdir=E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-SHIELDED-ASSEMBLY-15B/client/tmp
-Dorg.gradle.jvmargs=-Xmx3G -Djava.io.tmpdir=E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-SHIELDED-ASSEMBLY-15B/client/tmp
```

仅本次PowerShell/构建子进程TEMP/TMP及java.io.tmpdir使用E盘本批目录，finally恢复原TEMP/TMP；保原Gradle缓存、Java21.0.7与-Xmx3G，未改全局环境或移动/清理缓存。开工读到C盘0；本次实际启动记录Cfree=16,182,566,912B，变化未由本执行者清理触发，不能把开工快照写成构建时状态。

真实Gradle日志：createMinecraftArtifacts/compileJava UP-TO-DATE，processResources/jar执行，jarJar NO-SOURCE，assemble成功；4 actionable tasks中2 executed/2 up-to-date。没有跑全build、JUnit/GameTest或Minecraft，也没有第二次assemble或手工造JAR。

## 原包保护与唯一制品读回

原2,815,098B / 7d62147c84392f00062d19635cd0a5e91300d89860b1e8502cea17079e98f507包先精确复制本批client/backup/create_nuclear_industry-0.1.0-7d62147c.jar并核同源，构建脚本复用此备份，见old-jar-backup.json与assemble-result.json。旧d12包及其他设备源/世界/配置保持。

唯一调用 `preview/verify-jar.ps1`：passed=true/0errors，29批准候选→生产→processResources→JAR逐项SHA一致；相较7d包正好 **20模型替换＋9新PNG**，0删除。**557个class保持，1926个非本批旧文件载荷逐字节保持**，后者包含class及Manifest，不额外相加。未把源码身份或UP-TO-DATE单独当作JAR一致；直接读取真实ZIP条目完成比较。

原始证据：本批client/[assemble-attempt.json](../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15B/client/assemble-attempt.json)、[assemble.log](../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15B/client/assemble.log)、assemble.exit、[assemble-result.json](../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15B/client/assemble-result.json)、[jar-verification.json](../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15B/client/jar-verification.json)。命令/exit/日志完整，未重跑资源生成/安装/HTTP矩阵。

## 接续边界

负责人已完成修后生产PNG整机/完整物品/点灯/剖视/底部实际浏览与capture，详见GPU目录README；原首次item背面视角失败与仅相机修订分别保留。35实际模型/纹理输入、ready/errors0和54/8 drawCalls为负责人浏览证据，不冒称游戏手测。当前同次原审查者只需补核最终制品身份，不新审查或重建。

本批仍为已选C静态外观；三partial保持待机与原枢轴、底轴保Create来源/主控下方。未实现工序动画、新状态逻辑或用户Minecraft视觉/操作验收。15A旧4193不重启且原机按钮存档禁用，15B4194/PID16460/session53337托管保留供复看，详见PREVIEW报告。
