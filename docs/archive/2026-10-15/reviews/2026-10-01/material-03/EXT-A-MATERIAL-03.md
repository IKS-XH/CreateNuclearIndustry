# EXT-A-MATERIAL-03 A 执行报告

状态：A 写集已实现并交付未提交改动；专项自动验证完成，完整 GameTest 运行器仍有框架中断。客户端/用户世界人工门未执行，执行者不作验收。

## 范围与实现

候选工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，开工 HEAD `909733887bedba39bea70a9a4470d8ca4c637540`。按本批任务卡在 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280 下实施；读取并应用 AGENTS、治理、本批计划/Spec、minecraft-modding、minecraft-testing、test-driven-development。无 Git 写操作、正式 build.gradle 改动、默认 run/ 写入或客户端启动。

新增普通物品 `iron_dust`、`coal_dust`、`charcoal_dust`、`steel_dust`、`steel_ingot`，对应创造栏、英中语言、模型、c 通用标签。八配方为三路粉碎轮、一煤粉一木炭粉无热搅拌（各 4 铁粉+1 碳粉→5 钢粉）、钢粉熔炉/高炉（200/100 tick、经验 0.1）、钢锭压成既有钢板；保留原煤/木炭磨石染料链。只在 P1DataContractTest 对新压片 ID 作任务卡精确例外，未改旧链断言。五图由素材执行者独立完成，本执行者未改图或美术工具。

## TDD 与自动验证

- 隔离 `material-03-test.init.gradle` 同时指定 gameTestServer/server run model 的 gameDirectory，`material03RunDirectories` 与 JavaExec doFirst 断言任务目录均在本报告目录。首次与重试诊断、普通服诊断均退出 0，原始日志见证据目录。默认 `run/` 开始与结束均为 179 文件，逐路径、长度、SHA-256、UTC 修改时间比较差异 0。
- RED：`red-gametest.log` 的 13 个新增断言因缺注册/配方失败，旧磨石守护通过；Gradle 退出 1。已在保存阶段有限等候后仅停止本轮记录的 PID 5972，见 `red-stop.txt`。
- 实现后两次完整 `runGameTestServer` 均在 defaultBatch:2 的 `GameTestInfo.tickInternal` / fastutil `wrapped=null` 框架异常中断，分别退出 1，见 `green-gametest.log` 与 `green-retry-gametest.log`。两次都不能算完整 required 全过；未通过改生产或跳过断言掩盖中断。审查未发现本批测试内嵌套注册延迟回调。
- 隔离普通服逐条运行本批 14 个新 GameTest 方法：`normal-server-final-latest.log` 有 16 个断言完成后的成功标记，覆盖 14 个方法在默认条件下的全部路径，另两次为同一外部标签测试的启用/恢复复测。日志无本批失败标记。真实机器含粉碎轮、搅拌盆、熔炉、高炉、单件熔岩风扇、压片机，均在服务器 tick 后验证输入消耗、输出及停机/堵塞恢复；没有以配方 `apply/assemble` 代替机器行为。例：三路粉碎轮 `gameTime=6549`、欠料/错料恢复 `8174/8687`、动力恢复 `9091`、堵塞恢复 `9601`、熔岩风扇单件 `10874`、压片 `11192`。风扇仅实测单件入口，不声称大堆叠。
- 初始无外部包：20:16:26 `external=false`；在隔离普通服启用报告内外部包并执行实际 `reload`：20:17:26 `external=true`，20:17:36 替代物 `minecraft:flint` 经真实熔炉产出 1 钢锭；停用包并再次 `reload`：20:18:19 `external=false`。20:19:47 `datapack list` 显示该包仅 available、未启用；随后正常 `stop`，Gradle 退出 0。已停用的测试包实体文件保留在报告目录下的隔离世界，不在默认 `run/`；物理删除请求被自动审批以 `blocked by policy` 拒绝，未执行或重试。命令整理见 `normal-server-final-commands.txt`，该文件来自本轮 PTY 实际提交与服务器时间戳，并非原始 PTY 转录；`normal-server-final-console.log` 的 PowerShell transcript 未完整捕获子进程输出，完整服务器日志以复制的 `normal-server-final-latest.log` 为准。此前两个普通服尝试仅提交命令、无完成标记，不能算通过；其记录原位保留。
- 最终 `test build --rerun-tasks --max-workers=1` 退出 0；52 份 JUnit XML 共 265 tests、0 failures、0 errors、0 skipped。最终 JAR `create_nuclear_industry-0.1.0.jar` SHA-256 `D23732ABBFF58C2742671BF7A57A541D1EC930DEAB791815EF14FF10B895B33B`；新测试类 SHA-256 `60F738C3BDC06BAC82F4176F3436E6C73F070A2E7589273C48379419F4439FCA`。JAR 的 assets/data 264 项与源码 264 文件逐字节一致，含 60 PNG；旧 55 个已跟踪 PNG 的 Git 字节差异为 0，新增 5 图。详见 `artifact-verification.json`。

## 运行边界与交接

RED、两次 GREEN、最终普通服 Java PID 分别为 5972、17440、33624、2988；精确进程命令保存在各 `*-java-process.txt`。最终普通服的工具命令、启动/结束时刻与退出码见 `normal-server-final-command.txt`；Root 跟踪日志因自动测试变动，其测试前/后副本均在报告目录，由项目经理负责恢复，不由执行者执行 Git 写操作。保留两次完整 GameTest 运行器异常的限制；本批 14 项专项成功不等于旧全量 required 成功。仍需用户在客户端确认新粉末/钢锭图标、名称、配方书/JEI 与实际操作观感，以及用户世界保存重开；执行者未启动客户端、未接触用户存档。

原始验证日志、JUnit XML、隔离配置、外部包模板、默认 run 前后清单与最终制品核对在 `build/reports/extension/EXT-A-MATERIAL-03/evidence.zip`，压缩包 SHA 见同目录 `evidence.sha256.txt`。不含隔离世界、缓存、JAR 或旧线程 dump。
