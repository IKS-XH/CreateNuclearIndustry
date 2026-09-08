# P1-BALANCE-02A 验收归档

**任务：** 游戏端可配置损伤产热与燃耗差异倍率

**基准提交：** `077b88134769b136467f033d538bee7215cacb73`

**验收日期：** 2026-09-08

**结论：** 已通过项目经理自动验收；`P1-BALANCE-02B` 可以在包含本任务提交的基线上派发。

## 最终行为

正式 P1 反应堆按损伤程度 `D = 1 - clamp(integrity, 0, 1)` 分别计算：

```text
H(D) = 1 + (Hmax - 1) × D
B(D) = 1 + (Bmax - 1) × D
```

- `reactor.fuelColumnDamageHeatMultiplier` 默认 `2.0`，表示满损伤新生裂变热倍率；
- `reactor.fuelColumnDamageBurnMultiplier` 默认 `3.0`，表示满损伤计划燃耗倍率；
- 两个值须有限并满足 `1 < Hmax < Bmax`，所以燃耗倍率随损伤增长更快；
- 缺项分别使用默认值，最终组合非法时整对回退到 `2.0/3.0`；同一非法组合不在每 tick 重复告警；
- 产热上限只截断新生热量，不返还燃耗；超频与损伤倍率分别叠乘；
- 倍率由服务端配置和已有完整度派生，不新增 NBT、网络包、注册 ID 或第二份燃料状态。

## 实现范围

- `src/main/java/com/iksxh/create_nuclear_industry/config/P1ServerConfig.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorSimulationParameters.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/ReactorFissionCalculator.java`
- `src/main/java/com/iksxh/create_nuclear_industry/reactor/FuelColumnFissionResult.java`
- `src/main/java/com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java`
- `src/test/java/com/iksxh/create_nuclear_industry/P1BalanceContractTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/P1DamageBalanceConfigurationTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/reactor/ReactorFissionCalculatorTest.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/P1DamageBalanceGameTests.java`

P0 历史原型、HTML 模拟器、NBT 格式、燃料事务、注册资源和构建配置未修改。执行者遵守只读 Git 约束；测试生成的 `logs/debug.log`、`logs/latest.log` 差异由项目经理确认后恢复，未纳入任务提交。

## 验证证据

项目经理在 Minecraft `1.21.1`、Java `21`、NeoForge `21.1.219`、Create `6.0.10-280`、Ponder `1.0.82`、Flywheel `1.0.6` 基线上独立审查并执行：

| 验证 | 结果 |
| :--- | :--- |
| `.\gradlew.bat test --rerun-tasks --max-workers=1` | 46 个测试文件，225 项测试，0 失败、0 错误、0 跳过 |
| `.\gradlew.bat runGameTestServer --rerun-tasks --max-workers=1` | `All 90 required tests passed :)`，完成停服后进程未自然退出，由项目经理终止已完成的精确会话 |
| `.\gradlew.bat build --max-workers=1` | `BUILD SUCCESSFUL` |
| `git diff --check -- src/main/java src/test/java` | 通过 |
| 生成的 `run/config/create_nuclear_industry-server.toml` | `[reactor]` 下存在两个新键及 `2.0/3.0` 默认值和中文说明 |

新增测试覆盖五个默认损伤采样点、`1.5/2.5` 与 `3.0/5.0` 自定义终点、非法与缺失配置、纯模拟参数拒绝、超频叠乘及热量截断不返还燃耗。新增 GameTest 在正式 `5×5×5` 结构和服务端 tick 中使用 `3.0/5.0`，验证半损伤时产热 `2.0` 倍、燃耗 `3.0` 倍、换料端口耐久扣除，以及端口物品和仪表运行状态的 NBT 往返。

## 验证边界

本次没有执行客户端护目镜对照，也没有在运行中的真实服务器上修改磁盘配置并观察热重载；自动验收已经覆盖配置文件生成、内存配置映射、正式服务端 tick、燃料耐久和 NBT 往返。上述两项保留为最终客户端/服务器人工总验收观察项，不阻塞只负责游戏端公式与配置接入的 02A，也不作为 HTML 模拟器 02B 已完成的证据。

## 技能与版本治理

执行者和项目经理均实际使用 `minecraft-modding` 与 `minecraft-testing`，并以仓库依赖确认 1.21.1 的既有注解式 GameTest 路径；没有采用技能中的 26.x、其他 1.21.x 或 Fabric 示例，也没有改变 `All Rights Reserved`、发布方式或版本号。
