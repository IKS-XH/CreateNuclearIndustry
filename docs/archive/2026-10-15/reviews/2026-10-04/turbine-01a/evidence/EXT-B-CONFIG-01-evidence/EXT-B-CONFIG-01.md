# EXT-B-CONFIG-01 配置接口与交付报告

状态：执行者已交付，待 PM 审核；验证时段已交还 B。基线 `fd19c1f`。定向 JUnit 30/30、增量 assemble、默认管网 GameTest 4/4、非默认设备 GameTest 1/1 通过。未碰既有日志与 `__pycache__`。

## SERVER 文件与键

锅炉新文件 `create_nuclear_industry-boiler.toml`：

| 键 | 默认 | 合法范围 | 单位 |
| --- | ---: | ---: | --- |
| `waterCapacityMb` | 16000 | 1–1000000 | mB |
| `steamCapacityMb` | 16000 | 1–1000000 | mB |
| `portFlowMbPerTick` | 256 | 1–1000000 | mB/t/物理口 |
| `sectionHeatHuPerTick` | 18 | 0.000001–1000000 | HU/t/段 |
| `steamHuPerMb` | 1 | 0.000001–1000000 | HU/mB |
| `warmHuPerSection` | 3600 | 0.000001–1000000000 | HU/段 |
| `coolingHuPerSectionPerTick` | 0.9 | 0–1000000 | HU/t/段 |
| `reheatFraction` | 0.25 | 0–1 | 容量比例 |
| `valveOpenFraction` | 0.9 | 0–1 | 汽罐比例 |
| `valveCloseFraction` | 0.8 | 0–1 且严格小于开启值 | 汽罐比例 |
| `valveFlowMbPerTick` | 256 | 1–1000000 | mB/t |

换热器沿用 `create_nuclear_industry-heat-exchanger.toml` 与三旧键：

| 键 | 默认 | 合法范围 | 单位 |
| --- | ---: | ---: | --- |
| `heatLevel` | 18 | 1–18 | Create 热等级 |
| `huPerLevel` | 1 | 0.000001–1000000 | HU/级/t |
| `bufferTicks` | 40 | 1–1200 | tick |
| `hotCapacityMb` | 4000 | 1–1000000 | mB/台 |
| `coldCapacityMb` | 4000 | 1–1000000 | mB/台 |
| `maxLineLength` | 16 | 1–64 | 台 |

工质 `coolantAbsorptionHuPerMb` 唯一源仍是 P1 SERVER 配置，密度零或非有限值令换热停机。锅炉 `BoilerState.Settings` 与换热器 `HeatExchangerState.Settings` 是服务端有界快照；直列上限、端口容量与管压从快照/服务端配置派生。锅炉固定 5×5×5、换热器轴向判断与 1:1 液量守恒不是配置项。

旧默认常量 `BoilerState.CAPACITY`、`FLOW_LIMIT`、`WARM_HU_PER_SECTION` 与 `HeatExchangerState.CAPACITY` 只供默认快照、旧测试及缺少新 View 字段的客户端兼容回退；不得充当运行路径上的配置覆盖。旧 NBT 非负 Hot/Cold/Water/Steam 原量保留，超过新容量时接收余量归零，已有冷液/蒸汽仍可排出；换算密度、余热窗口或产汽耗热变更清理不可复用尾差，既存 HU 不按新系数重估或倍增。非法阈值组合停机，护目镜显示配置无效。

语言占位变化：中英文 `boiler.tanks` 显示水与汽各自容量，新增 `boiler.state.invalid`；中英文 `heat_exchanger.local_tanks` 与 `line_tanks` 分别显示热/冷容量。资产任务合并同文件时须保留这些键。

技能：实际读取 `minecraft-modding`、`minecraft-testing`、计划指定 `superpowers:executing-plans`；后者提交/清理流程服从 AGENTS 执行者 Git 禁令及治理 5.1。

## 最终验证结果（2026-10-04）

- `assemble test --tests ...BoilerStateTest --tests ...HeatExchangerStateTest`：通过；锅炉14项、换热器16项，零失败。证据：`EXT-B-CONFIG-01-junit-assemble.log` / `.exit` 与 Gradle XML。
- 默认隔离真实管网：`create_nuclear_industry_heat_loop` 4/4 required GameTest，通过并完成保存；证据 `EXT-B-CONFIG-01-default-gametest.log` / `.exit`。
- 非默认隔离真实设备：`create_nuclear_industry_config` 1/1 required GameTest，通过并完成保存；断言水/汽/热/冷四能力容量为400/500/2000/3000mB、实际额定36HU/t供热与2HU/mB产汽、128mB/t汽口对应 Create 管压256。证据 `EXT-B-CONFIG-01-nondefault-gametest-rerun.log` / `.exit`。
- 两份 SERVER TOML 实际自动生成于隔离实例的 `EXT-B-CONFIG-01-world/config/`，非默认运行所用文件已固定复制为同报告前缀证据：[`EXT-B-CONFIG-01-boiler-nondefault.toml`](EXT-B-CONFIG-01-boiler-nondefault.toml)、[`EXT-B-CONFIG-01-heat-exchanger-nondefault.toml`](EXT-B-CONFIG-01-heat-exchanger-nondefault.toml)。锁定的 NeoForge 21.1.219 / FML 4.0.42 默认使用实例 `config/`；仅当世界 `serverconfig/` 已有同名文件时才由它覆盖，故这次 GameTest 所见位置与普通实例默认一致。
- 降容保留库存与 NBT 保存恢复由 `BoilerStateTest`、`HeatExchangerStateTest` 定向 JUnit 验证；本轮没有执行真实世界重启后的旧库存恢复场景，不把账本测试说成该场景的通过证据。
- 初次非默认 GameTest 用带namespace的模板名，NeoForge再加本测试域前缀而报非法路径；改为本域空模板后通过。首次失败日志 `EXT-B-CONFIG-01-nondefault-gametest.log` 保留作诊断，不计作通过证据。
- `git diff --check` 对本任务已跟踪文件无空白错误（仓库日志不在本批差异核对内）。未清理既有日志、pycache或其他执行者文件，未执行Git写操作。

注意：新增 `BoilerConfig.java` 被仓库根 `.gitignore` 的 `config/` 规则忽略；PM提交时必须显式强制暂存该一个文件。新 GameTest 类与本域空NBT也需由PM按写集审查。执行者没有暂存、提交或改变任务状态。

本轮只做自动验证；没有开展用户客户端人工测试、普通存档热重载验证或整项目全量回归。按合同，变更配置后重新进入世界或重启服务端。
