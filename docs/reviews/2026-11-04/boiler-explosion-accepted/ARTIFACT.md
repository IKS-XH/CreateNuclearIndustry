# 锅炉事故与反应堆外爆联合制品

2026-11-04。稳定生产来源 **16e14ae10735f7fb390e2f834b9b67e987586f45**，最终JAR由该提交一次增量构建；没有Git写操作、复制到main、世界操作或发布。

| 项目 | 结果 |
| --- | --- |
| 命令 | `.\gradlew.bat assemble --console=plain` |
| 退出/耗时 | exit0；Gradle13s；命令墙钟13865.8712ms |
| 增量执行 | compileJava/processResources/jar执行；createMinecraftArtifacts复用；jarJar NO-SOURCE；4任务3执行/1复用 |
| 最终路径 | `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix/build/libs/create_nuclear_industry-0.1.0.jar` |
| 大小 | 3714927bytes |
| SHA256 | C3A2C178CEDEBD05436DB3E1F3244CF361752AF7B8945C2A1AF154C82057D5C3 |
| 来源保持 | 2262生产输入构建前后路径/bytes/SHA一致；两原来源35项含测试源另核原字节未漂移 |
| 全部载荷 | **2601 = 822class + 1775资源 + manifest/三许可说明**；逐项解压SHA绑定当前编译/资源输出或清单/许可源 |
| 输出覆盖 | 2630编译/资源输出中33项按既有规则排除；0未预期遗漏、0重复载荷 |
| 相对外爆原包 | 31项新增/变化，均属锅炉来源类型及内部类、既有锅炉SteamSelection探针和两语言资源；0删除、0模型变化 |
| 新测试排除 | BoilerSafetyGameTests及ReactorExternalExplosionGameTests含内部类均无载荷；两测试域含目录条目均无载荷 |

实际载荷清单统计为**822class**。编译保留6条旧API移除警告及既有unchecked提示，未出现编译/资源/打包失败。未执行test/clean/rerun，按未变来源复用原锅炉52+外爆17定向JUnit及19+7真实服核心证据；末次1实跑/18或6显式SKIP不称全场新跑，原三条JUnit基线失败未修且不称build全绿。

原外爆包副本保存在本执行目录 `reactor-external-explosion-original.jar`，3681192bytes/SHA F8C87A27C6909EF0D225016AA59C3BEC6DA1954A6F9BA2689B12A0FA392537CA；锅炉日期包原件3710963bytes/SHA 9829AC0E3FC6E2AA8424D1591059B64B183D60EADD19422E94058A3CB10D5297，均保持。现有资源工作树与规范blob的CRLF/LF区别见INTEGRATION及资源清单，未改美术内容。

必要证据位于集成树 `build/reports/boiler-explosion-accepted-20261104/`：`assemble.log`、`assemble-command.json`、`artifact-source-before.json`/`artifact-source-after.json`、`source-consistency.json`/`source-consistency-after.json`、`artifact-summary.json`、`artifact-payloads.json`、`artifact-origins.json`、`artifact-output-coverage.json`、`artifact-vs-external-original-changed.json`/`artifact-vs-external-original-removed.json`。只读核验入口为 `verify-artifact.ps1`，完整执行说明为 `INTEGRATION.md`。PM可按任务卡转存必要证据；本报告不代替独立组合窄审或宣布合main验收。
