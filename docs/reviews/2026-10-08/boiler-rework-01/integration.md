# EXT-B-BOILER-REWORK-ACCEPTANCE-01 主目录整合验收

- **主目录基准：** `main`，`611d3ca7ad0ba29475dd9a0a3fd4dfb3a83a892a`
- **用户人工门：** 用户于2026-10-08确认01F精简手测通过。该确认仅适用于01F列明的锅炉集中场景；不扩展为未测场景或首发发布验收。
- **技能：** 实际读取 `minecraft-modding/SKILL.md` 与 `minecraft-testing/SKILL.md`。其通用示例不改变任务卡确定的Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6、JEI 19.27.0.340与mod 0.1.0。本次仅按任务卡执行assemble和已指定静态核对，不运行测试或客户端。

## 唯一打包

按卡片要求在主目录设置 `JAVA_HOME=C:/Program Files/Java/jdk-21`，唯一一次执行：

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
.\gradlew.bat assemble --console=plain
```

结果：退出码 **0**，`BUILD SUCCESSFUL in 8s`；Gradle报告4项 actionable tasks（2 executed、1 from cache、1 up-to-date）。完整控制台日志保存于 `build/reports/extension/EXT-B-BOILER-REWORK-ACCEPTANCE-01/assemble.log`，命令、起止时间和退出码保存于同目录 `assemble-result.txt`。

## 当前JAR与24项静态核对

本次构建产物 `build/libs/create_nuclear_industry-0.1.0.jar` 为 **2,271,628 bytes**，SHA-256：`DDF93DAEF196C9669CB8A1E059B6D2C4F4A57C7175DB72E2B088BA4E454613E1`。原样快照保存为 `build/reports/extension/EXT-B-BOILER-REWORK-ACCEPTANCE-01/create_nuclear_industry-0.1.0-main-acceptance.jar`，快照大小与SHA-256相同。

依01F既有 `pm-artifact-validation.json` 的24个 `verified_entries`，逐条比较JAR条目字节与当前 `build/classes/java/main` 编译class或 `src/main/resources` 源资源，结果 **24/24完全一致**。逐项路径、来源、存在性、字节数及比较结果见本批 `artifact-validation.json`。这项检查只复核指定的24条当前产物，不重复进行历史JAR审查或素材/模板矩阵。

## 复用证据与范围

自动化证据复用01F `pm-artifact-validation.json` 记录的 `04:25/25`、`07:10/11 passing reused`、`08: corrected direct reference 1/1` 及 `09: assemble exit 0`；并保留01F `candidate-metadata.json` 对早期失败轮次的原始记录。本次没有重跑JUnit、GameTest、build、clean、runClient或生成器。既有人工确认及此次构建不代表首发发布许可，也不自动开启其他主线或设备教学。
