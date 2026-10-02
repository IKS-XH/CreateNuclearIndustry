# EXT-A-FUEL-01E 运行线证据

## 根因

锁定 Create 6.0.10-280 源码的管道检查先由 `FluidPropagator.hasFluidCapability` 读取目标方块实体；目标BE为空或未绑定Level时直接返回false，不会继续查询Level上的NeoForge block capability。`FluidPipeBlock.canConnectTo`和`PipeConnection.determineSource`使用该判断；`FlowSource.FluidHandler.manageSource`也需目标BE来建立能力缓存。因此，上段虽然注册了位置型 `registerBlock` capability，但无BE时Create会在调用该能力前拒绝它。Create漏斗发现路径先读BE但默认谓词接受空值，仍调用Level capability，不存在同一硬性要求。

## 修复

- 上段创建 `CentrifugeUpperProxyBlockEntity`，只有结构锚点，不存储库存/批次/磨损，不运行tick；上下段能力仍委托给下段唯一 `CentrifugeBlockEntity`。
- 上段明确返回空ticker，避免 `IBE<CentrifugeBlockEntity>` 默认Smart ticker作用于代理BE；下段保留原ticker。
- 方块类型仍识别上下两格，旧存档没有上段BE标签时，Create对方块位置调用`getBlockEntity`会依方块状态创建代理BE。放置配对时原有 `setPlacedBy` 对上下位置调用 `invalidateCapabilities`，使配对前缓存的空能力失效。
- `build.gradle`中的可选 `gameTestNamespace` 与 `gameTestDirectory` 只影响 `gameTestServer`；不传Gradle属性时仍用模组默认namespace与默认目录。

## 验证

- 修前RED：`.\gradlew.bat runGameTestServer --console=plain -PgameTestNamespace=create_nuclear_industry_01e -PgameTestDirectory=build/test-worlds/centrifuge-01e`只注册并运行1项；`createpipeconnectstouppercentrifugeandtransfersslurry`真实调用Create连接判定时失败：`Create没有识别上段 up 物料面为流体管道端点`。隔离日志为 `build/test-worlds/centrifuge-01e/logs/2026-10-02-2.log.gz`。
- 修后GREEN：同命令单测域运行1项并通过，首轮证据 `build/test-worlds/centrifuge-01e/logs/2026-10-02-1.log.gz`。后续将测试加强为先移除上段BE以模拟01D旧区块，再让Create查询端点触发BE惰性补建；最终日志 `build/test-worlds/centrifuge-01e/logs/latest.log`仍显示1项通过。
- GameTest断言上段代理无ticker、Create可识别上端和四个水平端面，且旧区块查询后代理BE已补建。实际Create玻璃管、机械泵、动力轴网将铀料浆从源罐送至上段；严格断言流体身份为 `URANIUM_SLURRY`、上段收料1000mB且源罐剩余0mB。
- 定向JUnit：`CentrifugePortsTest`，命令 `.\gradlew.bat test --tests com.iksxh.create_nuclear_industry.production.CentrifugePortsTest --console=plain`，JUnit结果XML记录3项、0失败。JUnit使用 `forgejunitdev`，其日志位于候选根 `logs/`；该目录被本轮测试更新，未清理或恢复。GameTest自己的世界和日志在上述build隔离目录。
- 最终 `.\gradlew.bat assemble --console=plain`：BUILD SUCCESSFUL。资源线冻结后仅运行一次。

工具输出摘录（来自本轮已完成调用，未为报告重跑）：

```text
定向JUnit：> Task :test
BUILD SUCCESSFUL in 15s

最终GameTest：1 tests are now running
All 1 required tests passed :)
BUILD SUCCESSFUL in 19s

assemble：> Task :assemble
BUILD SUCCESSFUL in 1s
```

## 实际使用技能

- `minecraft-modding`：核对NeoForge/Create锁定版本与方块实体注册、Create接口接入约束。
- `minecraft-testing`：实现单一隔离GameTest及定向JUnit验证。
- `superpowers:systematic-debugging`：沿Create真实管道能力发现调用链确证根因后再修复。
- `superpowers:test-driven-development`：先取得真实管网RED，再实现代理BE并验证GREEN。

GameTest并未对两种实际玩家放置时序分别进行端到端拆装模拟；本次通过端点五面判定、旧无BE惰性补建、配对时现有上下能力缓存失效逻辑以及完整真实泵管传输覆盖本缺陷。没有运行默认客户端或改动其world。
