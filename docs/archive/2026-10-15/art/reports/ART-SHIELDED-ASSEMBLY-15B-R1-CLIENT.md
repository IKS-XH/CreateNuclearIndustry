# ART-SHIELDED-ASSEMBLY-15B-R1 最终局部候选制品

2026-10-14，原唯一预览/包执行者。原同次资源审查无剩余必改后，本批唯一增量assemble实际 **exit0 / 构建阶段13.1894887秒**（脚本最终输出含记录写入13.216秒）。新局部候选 **2,812,546B / SHA256 602985f85d2522c7914d7a4cef0b1c822f8c50ce11687e60af2be697428e3cd3**，位于art-studio/build/libs/create_nuclear_industry-0.1.0.jar。用户Minecraft黑窗视觉门仍待同一窗口/内部机构复看，不称已修复。

故障原包实际 **2,812,280B / a92a0bed0f948fc000b58800cad34f84c03ffb314b167fea3a839f117b12ff7f** 已先精确备份R1/client/backup/create_nuclear_industry-0.1.0-a92a0bed.jar并读回同源，见 [old-jar-backup.json](../../../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15B-R1/client/old-jar-backup.json)。原7d/d12包、15B历史构建/检查/图及世界配置保持。

必要原15B README/verify.mjs/build-client.ps1/verify-jar.ps1修改前完整字节已存R1/client/backup/source。当前helpers匹配a92原包和R1独立日志目录，仅允许renderLayerR1.changedTargets中的8个已批准模型变化；真实生产/processResources/JAR读回及非预期载荷比较仍由本作者持有，Java/其它载荷不得改变。

两PowerShell打包/核验脚本纯语法与验证JS语法先通过，再按负责人审查后的转交唯一执行 `preview/build-client.ps1 -Reviewed`。实际命令为 `./gradlew.bat assemble --no-daemon`，附 `-Djava.io.tmpdir=<R1/client/tmp>` 和 `-Dorg.gradle.jvmargs=-Xmx3G -Djava.io.tmpdir=<R1/client/tmp>`；TEMP/TMP仅本次PowerShell/构建子进程继承E盘该目录，finally恢复，原Gradle缓存/Java与全局环境保持。完整实际参数见R1/client/assemble-attempt.json。

真实日志为createMinecraftArtifacts/compileJava UP-TO-DATE、processResources/jar各执行一次、jarJar NO-SOURCE、BUILD SUCCESSFUL；2 executed/2 up-to-date。没有全build/JUnit/GameTest、游戏或第二次assemble，没有手工拼JAR。

唯一执行 `preview/verify-jar.ps1` 读真实ZIP：passed=true/errors=[]，8批准候选→生产→processResources→JAR同源；相对a92包 **仅part0/1/4/5的off/on八模型metadata变化**，removed=[]。**557个class保持，1947个非本批旧载荷保持**（包含class/Manifest，不相加），九PNG/三partial/完整item/无window模型及其它载荷不变；离线probe class不在JAR中。

原始 [assemble-attempt.json](../../../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15B-R1/client/assemble-attempt.json)、[assemble.log](../../../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15B-R1/client/assemble.log)、assemble.exit、[assemble-result.json](../../../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15B-R1/client/assemble-result.json)、[jar-verification.json](../../../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15B-R1/client/jar-verification.json) 均新R1目录。a92/7d/d12与旧记录保持，main/Git/世界/配置不写。

原生探针/37来源检查/增量包仅证明实际绑定、metadata与制品身份，不能称用户Minecraft黑窗已解决。原同次审查者下一步只补最终制品身份，不新审查、重跑探针或构建；最终仍待同一窗/内部机构局部游戏复看。
