# EXT-B-EXCHANGER-01C REPRO

## 结论

使用真实 Create 储罐、256 rpm 机械泵和三通管网，已复现同一反应堆的三个冷端接收近满共享冷却剂库存时发生 72 mB 质量损失。单冷端对照正常守恒。三条不同支路均在传输期间观测到有效流量，因此这是有效动态复现；尚未修改生产代码。

## 夹具与结果

- 反应堆共享冷库存初始 9000 mB、热库存 0 mB，容量 12000 mB；Create 源罐装入 4000 mB，总量初始 13000 mB。没有热源或锅炉，换热器不参与该根因隔离用例。
- 单冷端对照通过：最终源/冷/热为 `1000/12000/0`，总量仍为 13000 mB，真实 Create 管网完成传输。
- 三冷端主用例失败（预期复现）：起始为 `4000/9000/0`；三个物理支路依次观测到有效流量；tick 32 时变为 `928/12000/0`，总量 12928 mB，少 72 mB。测试在每 tick 检查源罐+冷库存+热库存守恒，故在异常量到达时即失败。
- 三端普通 FLUID_PIPE 三通的中心段有 N/S/E/W 连接，左支路 N/E、右支路 N/W。日志记录了三个独立端点的支路流量状态。第一笔传输时仅中间支路完成传播；夹具改为整个传输期间累计观测三支路流量，避免把传播时序误判为管路未连通。

## 执行与证据

环境：Minecraft 1.21.1、Java 21.0.7、NeoForge 21.1.219、Create 6.0.10、Ponder 1.0.82。

命令：

```powershell
.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_heat_loop -PgameTestDirectory=build/gametest-heat-loop-01c --max-workers=1 --console=plain
```

有效运行日志：`build/reports/extension/EXT-B-EXCHANGER-01C-REPRO/gametest-console-delayed-branch-flow.log`。原始首次日志 `gametest-console.log` 与第一次三通调整日志 `gametest-console-branch-manifold.log` 也保留在同一证据目录中；它们分别因 `succeedWhen` 回调可空转成功以及过早要求三支路同时完成而无效，不能用作复现或反证。

测试夹具与空模板仅新增在任务允许路径：`ExtensionHeatExchangerLoopGameTests.java` 和 `data/create_nuclear_industry_heat_loop/structure/loop_empty.nbt`。NBT 模板直接复制既有 P1 空模板字节，没有改动其他模板。

## 根因线索与边界

本地 Create 6.0.10 源码中 `FluidNetwork` 使用基于 `IFluidHandler` 对象身份的接收模拟累计；模组的 `ReactorCoolantFluidHandler.forPort` 为每个绑定冷端创建新适配器实例，但实例操作的是同一个 owner 的共享 snapshot。该静态路径与上述三端近满动态结果一致，仍应由修复和后续兼容用例验证。

本次只证明三端汇入同一反应堆冷端库存的真实 Create 管网能导致总量漂移，并以单端管网作对照。换热器双面热入边界和原生 Create 罐分流透传尚未在此复现运行中覆盖；它们作为修复兼容边界补测，不改变本次动态结论。

## 技能

实际读取并应用 `minecraft-modding`、`minecraft-testing` 和 `systematic-debugging` 技能；按项目第 5.1 节仅运行指定 GameTest namespace，没有执行 clean、全量测试或重跑任务。

## 修复后最终验证

在同一冻结候选上，4 组定向 JUnit 共 30 项全部通过：`SharedFluidFillPlanTest` 7 项、`HeatExchangerStateTest` 12 项、`ReactorCoolantPortAggregationTest` 6 项、`ReactorCoolantPortFlowBudgetTest` 5 项。两命名空间共 15 项 required GameTest 全部通过。

- 三冷端复现与单冷端对照最终均为源/冷/热 `1000/12000/0`，总量 13000 mB；三端测试逐支观测到真实流量，并在每 tick 检查守恒。
- 双面换热器用例从热罐 1000 mB 持续输入到 4000 mB 满罐；最终源/热/冷 `1000/4000/0`，总量 5000 mB，西侧和东侧都观测到真实流量。
- 原生 Create 储罐分流实测源/左/右 `3744/64/192`，总量 4000 mB，两个独立目标均实际收到流体。
- 封口夹具只向空气邻格放置实心封口，并只对 FluidTransportBehaviour 实际存在的连接检查 OpenEndedPipe；第一次封口尝试曾覆盖右支管南侧的动力齿轮 `(3,2,6)`，夹具已修为保留所有非空气方块。最终传动和各目标流路均在真实 GameTest 中通过。

最终 GameTest 原始日志：`build/reports/extension/EXT-B-EXCHANGER-01C-REPRO/gametest-final-connected-faces-unified.log`。Gradle 在 GameTest 报告 15/15 通过、开始保存测试世界后仍挂在子 JVM 退出阶段；按PM授权，仅结束本轮 GameTest 子 JVM 和 Gradle 包装器，保留 daemon。故单独增量执行 `assemble`，结果 `BUILD SUCCESSFUL`，日志为 `build/reports/extension/EXT-B-EXCHANGER-01C-REPRO/assemble-final.log`。

最终 JAR `build/libs/create_nuclear_industry-0.1.0.jar` 包含 `create_nuclear_industry.mixins.json` 和 `META-INF/neoforge.mods.toml`；压缩包核对确认 TOML 恰有一个 `[[mixins]]`，且引用该 JSON，JSON 注册 `FluidNetworkSharedFillMixin`。
