# EXT-B-BOILER-REWORK-01A：底层热液入口整改交付

## 实施

`BoilerStructure.inspectDetailed`现在只允许底面外围四边的非角点热液口替代外壳，并逐口验证水平朝外方向。更高层热液口不再进入热口列表；角点、竖向棱边及其他底边部件仍走原有拒绝规则。既有`Form`、控制器能力和管道刷新继续沿用，没有另建库存或改动账本算法。

两类旧锅炉GameTest夹具已把合法热口移到底边；新增`BoilerHotInletGameTests`覆盖四向多入口共享库存和逐口限流、非法层位/朝向/角点/其他棱边部件拒绝，以及真实Create泵管输送和拆装能力恢复时的冷却剂总量守恒。新增专用模板沿用项目现有`data/<namespace>/structure/boiler_empty.nbt`布局。中英文`issue.edge`诊断已描述底层入口例外。

## 技能与版本

实际读取并应用`minecraft-modding`、`minecraft-testing`、`superpowers:systematic-debugging`和`superpowers:test-driven-development`。锁定版本由仓库与运行日志确认：Minecraft 1.21.1、Java 21.0.7、NeoForge 21.1.219、Create 6.0.10、Ponder 1.0.82、Flywheel 1.0.6。按项目治理5.1只运行本整改定向域与必要的增量assemble；未重跑既有29项账本单测或15项全域证据。

## 验证证据

最终GameTest命令：

```powershell
.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_boiler_hot_inlet -PgameTestDirectory=run/verification/boiler-hot-inlet-01 --console=plain
```

最终退出码`0`，日志实际报告该专用域注册并运行3项，`All 3 required tests passed`。最终增量构建命令`.\gradlew.bat assemble --console=plain`退出码`0`。完整原始日志、逐次退出码均保存在`build/reports/extension/EXT-B-BOILER-REWORK-01A/`：`red-gametest.log`（新增测试首次编译时修正Create类导入前的编译诊断）、`gametest.log`、`gametest-retry.log`、`gametest-retry-2.log`、最终通过的`gametest-retry-3.log`、`assemble.log`、`assemble-final.log`及对应退出码文件；最终assemble退出码`0`。前三轮GameTest日志记录了非法用例恢复夹具状态、热液可能已转为冷液、泵动力与流向夹具的实际失败及修正；最终一轮三个场景全部通过。最终打包前另将旧共享入口场景的第二热口同步移到底边，未重跑不受其影响的新专用域。

制品`build/libs/create_nuclear_industry-0.1.0.jar`：2,186,418字节，SHA-256 `577CB895A33175AB18F08A65096B24E059271C94D95570622FC30638AFEC4414`。

新增模板`src/main/resources/data/create_nuclear_industry_boiler_hot_inlet/structure/boiler_empty.nbt`：94字节，SHA-256 `A46FBCA10B8C94CD0228BD493BF5F1D19787EC034047715DB6AC40B9AB211941`。

## 待办门槛

本交付没有启动用户客户端，尚未完成锅炉集中客户端手测。该整改并入REWORK-01既有人工门；是否通过及后续Git操作由项目经理处理。

## PM证据核对与集中审查

PM读取最终真实运行日志、退出码、构建输出及源码差异，核对最终制品大小/SHA，并检查两种语言与空模板在JAR中逐字节一致、结构/专用测试class存在；同一最终快照保存到主目录与同级候选的01A证据目录。功能提交`cca0dbeb5eefb82d4022938497c0e545f1f3f077`仅含7个代码/资源文件，没有夹带日志、pycache或未验收以外的新功能。

只读审查者`/root/boiler_hot_inlet_review`（gpt-6-luna/medium）核对底边条件、唯一水平外向面、Form能力与管道刷新、中文注释及三项实际回归的断言，未发现生产代码问题。PM随后发现旧共享端口场景的第二热口仍在较高层，交原执行者补齐；同一审查者对两份旧夹具的全部热口作限定复核关闭，不重跑不受影响场景。最后仅再次增量封包更新该测试class。

结论为**自动验证/审查通过，可继续现有锅炉集中人工门**。主工程仅同步本批PM文档及证据，未合入锅炉代码、未改用户世界、未把两台已验收思索的确认扩大为锅炉验收。
