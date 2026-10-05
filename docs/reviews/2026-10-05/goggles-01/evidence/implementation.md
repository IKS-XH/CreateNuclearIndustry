# EXT-UI-GOGGLES-01 实施报告

## 变更

锅炉继续只由服务端账本结算。本tick的 `produced`、`vented` 通过 `View` 更新标签和区块实体数据包同步到只读镜像，tooltip 改读镜像；计数改变（包括从非零归零）时立即同步。没有把这两个瞬时计数写入锅炉持久账本。

新增共享首行排版入口：服务端直接返回原组件，客户端桥接调用 Create 6.0.10 的 `CreateLang.builder().add(component).forGoggles(list)`，保留Create原生字体测量与16像素图标避让。只包装每个设备的第一行，不改变后续层级和内容。

已格式化九个 tooltip 入口：`CentrifugeBlockEntity`、`FuelSinteringBlockEntity`、`ShieldedAssemblyBlockEntity`、`NuclearHeatExchangerBlockEntity`、`BoilerControllerBlockEntity`、`TurbineControllerBlockEntity`、`ReactorPortBlockEntity`、`ReactorInstrumentPortBlockEntity`、`ControlRodDriveBlockEntity`。

定向GameTest复用锅炉服务端真实tick；独立镜像经 `handleUpdateTag` 读回生产态；另一包镜像经 `onDataPacket` 读回生产态，随后复用该包镜像接收停机态包，断言生产态的产汽/排汽均非零且与服务端账本相等、停机态两项均与服务端归零值相等。新namespace测试结构是现有 `boiler_empty.nbt` 的逐字节副本，SHA-256：`A46FBCA10B8C94CD0228BD493BF5F1D19787EC034047715DB6AC40B9AB211941`。增量构建产物 `build/libs/create_nuclear_industry-0.1.0.jar` 大小 2,064,007 字节，SHA-256：`8A154C2388E5B38949865D42E544FC757A41D263E90B508877C6EFB21D85AEFC`。

## 技能与环境

实际读取并应用 `minecraft-modding` 与 `minecraft-testing`。按项目配置使用 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82；未升级依赖。服务端定向运行成功，也验证该代码路径在专服环境可加载；它不等于客户端真实图形验证。

## 验证

命令（PowerShell，Java 21）：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_goggle_sync -PgameTestDirectory=build/runtime-goggles-01 assemble --console=plain
```

最终退出码：`0`。日志明确记录 `All 1 required tests passed :)`、GameTest服务端正常完成 `Saving worlds` 并关闭，以及 `BUILD SUCCESSFUL`（`assemble`）。本轮没有运行JUnit或全项目测试。受影响源码的 `git diff --check` 未报告空白错误；只出现仓库的LF/CRLF提示。

执行过程中的非最终失败已保留在 `verification.log`：首次编译因测试import、运行时不存在 `DistExecutor` 及 `handleUpdateTag` 参数未对齐而失败，随后修正。GameTest随后遇到三种模板/筛选失败：新namespace缺模板；在模板名中使用冒号导致非法ResourceLocation路径；指定外部templateNamespace导致筛选后0个test functions（虽然该命令中的assemble成功，这次不作为测试通过）。按PM扩展写集复制既有模板到隔离namespace后，最终1项必需测试通过。最终成功记录位于日志末尾。

## 未覆盖

尚未由人工客户端复测护目镜图标与字体显示，也未重启候选 `runClient`。这两项仍由项目经理安排的人工显示检查确认。没有研究旧存档兼容，也没有运行锅炉热工或其他设备回归。