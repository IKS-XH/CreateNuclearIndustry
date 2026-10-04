# EXT-B-TURBINE-STATE-01：纯配置与账本交付

状态：执行者已交付，待 PM 审核；尚未实现正式设备、内容注册或双轴生命周期。候选基线 `fd19c1f` 加已审 A 改动；未执行 Git 写操作。

## 精确写入

- `src/main/java/com/iksxh/create_nuclear_industry/config/TurbineConfig.java`：独立 SERVER 规格，待 C 在模组入口注册。NeoForge 默认实例 `config/create_nuclear_industry-turbine.toml`，世界 `serverconfig/` 同名文件可覆盖。本批尚未注册，所以不宣称已经生成 TOML。
- `src/main/java/com/iksxh/create_nuclear_industry/turbine/TurbineState.java`：纯库存、物理口预算和 SU 滑动窗口账本，不依赖 B 的 Create 网络结论。
- `src/test/java/com/iksxh/create_nuclear_industry/turbine/TurbineStateTest.java`：定向纯账本测试。
- `src/main/java/com/iksxh/create_nuclear_industry/config/BoilerConfig.java`：仅修正 SERVER 配置默认实例路径注释，未改行为。
- `build/reports/extension/EXT-B-TURBINE-STATE-01-draft/`：B 占编译时段时准备的源码/测试草案；正式源已按 PM 授权落盘。

`TurbineConfig.java` 被根 `.gitignore` 第15行 `config/` 忽略；PM 提交时需对该文件单独精确 `git add -f`。执行者没有暂存。

## 配置键与合法范围

每个 `short` / `medium` / `long` 前缀均有四个平铺键：

| 后缀 | 三档默认 | 范围 | 单位 |
| --- | --- | --- | --- |
| `RotorCount` | 3 / 6 / 9 | 1–16，三档必须互异 | 节；轴向长度=转子数+2 |
| `RateMbPerTick` | 54 / 108 / 162 | 1–10000 | mB/t；实际加工受库存与排汽空位限制 |
| `InputCapacityMb` | 4000 / 8000 / 12000 | 1–1000000 | mB |
| `ExhaustCapacityMb` | 4000 / 8000 / 12000 | 1–1000000 | mB |

全局键：

| 键 | 默认 | 范围 | 单位/约束 |
| --- | ---: | ---: | --- |
| `rpm` | 128 | 1–65536，运行时还须不超过当前 Create 服务端实际上限 | RPM |
| `suPerMbPerTick` | 32768 | 0.000001–1000000 且有限 | SU/(mB/t) |
| `smoothingTicks` | 40 | 1–1200 | 服务端 tick |
| `inletPortFlowMbPerTick` | 256 | 1–1000000 | mB/t/进汽口 |
| `exhaustPortFlowMbPerTick` | 256 | 1–1000000 | mB/t/排汽口 |
| `frontShare` | 0.5 | 0–1 且有限 | 前轴比例，后轴取余 |

范围不额外要求档位容量大于单 tick 额定流量；小罐按真实入汽量与排汽空位处理。坏配置、重复规格、非有限数或 RPM 超过当前 Create 服务端值使 `Settings.valid(createMaxRpm)` 为假，账本停止加工与发布动力，不隐式回退默认值。

## C 的公开接口与边界

`TurbineState.Tier(rotorCount, ratedFlowMbPerTick, inputCapacityMb, exhaustCapacityMb)` 提供 `length()` / `valid()`；`TurbineState.Settings(shortTier, mediumTier, longTier, rpm, suPerMbPerTick, smoothingTicks, inletPortFlowMbPerTick, exhaustPortFlowMbPerTick, frontShare)` 提供 `DEFAULT`、`tierForRotors(int)`、`valid(int createMaxRpm)`。`TurbineConfig.settings()` 只返回 SERVER 数值快照；C 要调用 `TurbineConfig.register(container)`，取得当前 Create 实际转速上限，并传给账本。

账本公开 `applySettings(Settings,int rotorCount,int currentCreateMaxRpm)`、`canFormForNewTier(int)`、`tick(long now,boolean operational)`、`stop()`、`fillInput(long portKey,int amount,boolean simulate,long now)`、`drainExhaust(...)`、`remainingInput/remainingExhaust`、`save/load` 及库存、档位、额定流量、最近处理量、总 SU、前后 SU 视图。`portKey` 应是物理端口位置的稳定 `BlockPos.asLong()`；主动推送和被动抽取必须调用同一个 `drainExhaust`，共享该口额度。`tick` 的运行许可由 C 在结构完整、控制器和相关区块可 tick、红石未停机时给出；失效/卸载时立即 `stop()` 并撤销双轴发布。

`canFormForNewTier` **只用于新成型或换档**：库存超过候选档位容量时拒绝该次成型/换档。已成型的同一规格遇管理员降容时，C 不能逐 tick 以该结果锁机；账本仍允许按当前真实排汽空位加工，旧库存也能从端口排出。SU 历史仅按实际处理 mB 进入窗口，每 mB 在完整窗口合计贡献一次；跳过 tick 写零，红石/失效清历史。保存只含双库存与同 tick 端口预算，不保存 SU 历史，读档重新升容。

C 仍负责流体身份校验、结构/端口拓扑、唯一 owner、服务端快照向客户端同步、Create 轴容量发布及 B 已证的跨区块生命周期。后轴份额由 `totalSu-frontSu` 得出，不复制一份总 SU；具体 Create `calculateAddedStressCapacity()` 按 B 探针合同折算。此 C0 结果不替代 B/C 的真实设备验收。

## 验证

- 增量 `assemble test --tests com.iksxh.create_nuclear_industry.turbine.TurbineStateTest`：成功。随后针对小容量规则整改再运行同一范围，成功；最后新增系数变化/份额断言后定向 `test` 成功。最终 XML 显示 **8/8**、零失败/错误。
- 证据：`EXT-B-TURBINE-STATE-01-test-assemble-final.log` / `.exit`、`EXT-B-TURBINE-STATE-01-test-final.log` / `.exit`、`build/test-results/test/TEST-com.iksxh.create_nuclear_industry.turbine.TurbineStateTest.xml`。已跟踪本批差异 `git diff --check` 无空白错误。
- 未运行 GameTest 或客户端：本批只交纯配置与账本，真实Create网络、三档结构、主动排汽和跨区块门由 B/C 覆盖。

技能：实际读取 `minecraft-modding`、`minecraft-testing` 与计划指定 `superpowers:executing-plans`；测试范围按治理 5.1 精简，技能中的 Git 提交/清理步骤服从 AGENTS 执行者禁令。
