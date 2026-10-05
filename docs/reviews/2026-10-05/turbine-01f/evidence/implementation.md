# EXT-B-TURBINE-01F 实现交付

基线：候选工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，任务卡记录 HEAD `7a36e92`、功能基线 `0477d8f`。执行者未进行 Git 写操作，未修改 docs、构建脚本、其他设备或用户世界。PM 同时修改的 docs 与原有 logs、`tools/art-assets/__pycache__` 均保留。

实际读取并应用：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`；实际 API 依据仓库锁定的 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6 及本地 Create sources.jar。依治理 5.1 只跑受影响 JUnit、汽轮机 GameTest 域、隔离 SERVER 配置域与增量 assemble；依 5.2 不研究旧存档兼容。

## 行为与写集

- `TurbineState`：移除入口/出口大罐，入口执行成交后等量进入唯一普通蒸汽周转量；默认上限为本档 1 tick 额定量。出口真实执行成交才减少周转量并计入 40 tick 窗口；模拟、压力与动画均不计量。多口共用全机入/出额定额度，且各口仍受自身 256 mB/t 默认额度约束。堵塞保留残留，缩容不截断；当前格式保存周转量及同 tick 额度，恢复不恢复凭空 SU。
- 控制器 tick 只将**已结束**的世界 tick 写零；当 tick 出口成交才进入窗口。此顺序让轴在排汽前读取前 40 个已完成样本，持续满流不会稳定少算一格；出口先于控制器 tick 也只记一次。红石/失效立即清本机动力并拒绝新交易，周转残留不丢。
- 配置新增三档最高倍率 `short/medium/longMaxEfficiencyMultiplier=1.2/1.5/1.8`、`turnoverTicks=1`、`minEfficiencyMultiplier=0.5`、`minimumOperatingFlowRatio=0.3`；大型额定流量改为 216 mB/t。移除六个 `InputCapacityMb/ExhaustCapacityMb` 配置定义。配置值域与跨字段关系非法时安全停机。
- 控制器在空周转量时仍维持原生 Create 排汽管压力，防止满供给每 tick 清空缓存后反复预热；真实排汽是唯一发电依据。堵塞状态需超过 2 tick 无真实输出，正常持续流动不会因控制器先 tick 看到满缓存而误报。
- 护目镜显示平均实际排汽、当前倍率、启动门槛与待排周转量；中英语言文件和 `tools/art-assets/turbine_data.py` 的生成源同步。两轴仍共享同一总 SU。
- 实际改动限于任务卡允许的汽轮机状态/控制器/压力 Java、`TurbineConfig.java`、汽轮机 GameTest/JUnit、中英语言键、语言生成源，以及授权的开发实例 TOML 与本报告/隔离证据。未改变结构、外观、配方、全局混入。

## 定向验证

| 命令或检查 | 结果与证据 |
| --- | --- |
| `.\gradlew.bat test --tests com.iksxh.create_nuclear_industry.turbine.TurbineStateTest --console=plain` | 修前代表性断言按预期失败：只进汽未排汽却产生 SU，见 `junit-red.log`（10 项中 1 失败）。最终 `junit-window-fixed.log` 退出码 0；7 项覆盖三档倍率/门槛下与等号/中点/满流、可配置曲线、simulate 纯查询、多口同 tick 额度、堵塞恢复、缩容、当前格式残留保存恢复及 tick 两种次序。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine -PgameTestDirectory=build/runtime-01f-machine-final --console=plain` | 11/11，退出码 0，`machine-final.log`。真实 Create 源储罐→机械泵→入口管路与排汽管路→目标储罐同时运行；源减少量＝目标实收＋周转残留，稳定 20 tick 实收 `108×20=2160 mB`。另一个原生排汽管路场景验证堵塞最多接收 108 mB、低供汽仍排汽但 0 SU、恢复后满额与两端同网唯一容量；护目镜稳定阶段为运行态。 |
| `.\gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine_config -PgameTestDirectory=build/runtime-01f-config --console=plain` | 1/1，退出码 0，`config-server.log`。隔离 SERVER TOML 实际加载三档最高倍率 1.3/1.6/1.9、大型 220 mB/t、周转 2 tick、最低倍率 0.4、门槛 0.25；真实机组短档容量 120 mB、60 mB/t、96 RPM 与共享 Create 容量按新曲线生效。配置文件在 `build/runtime-01f-config/config/create_nuclear_industry-turbine.toml`。 |
| `.\gradlew.bat assemble --console=plain` | 增量打包退出码 0，`assemble-final.log`。中英语言 JSON 可解析且新提示键齐全；任务写集 `git diff --check` 无空白错误。 |

中间失败已定位并保留原始日志：`machine-first.log` 的 39/40 窗口是**生产时序缺陷**，已由“只补已结束 tick”修复；`machine-second.log` 的几何复位检查等待不足、`machine-third.log` 与 `machine-diagnostic.log` 的测试机械泵驱动方向错误、`machine-pump-north.log` 的多口回调先后假设，均为夹具问题，已定点修正。最终机器域与配置域均正常退出；没有因此修改几何或全局管路。

## 开发实例配置与边界

已先备份原文件至 `run-config-before-01f.toml`，然后定点修改 `run/config/create_nuclear_industry-turbine.toml`：大型额定值 162→216，删除本批退役的六个大罐容量键，加入上述六个新键；转子数、直径、RPM、SU 系数、平滑窗口、端口额度、废弃 `frontShare` 的值均保持原样。只读检查候选 `run/saves` 未发现同名世界 SERVER 覆盖。开发客户端须重启或重新载入配置后才会读取这些磁盘值；未关闭或干预当前 Java 进程。

未进行客户端护目镜视觉/手动堵塞复测，亦未打开用户世界；这仍是任务卡人工门。现有 01E 动力生命周期与外观证据只按原报告复用，本批没有重跑不相关全量测试。写集已冻结，交 PM 审查。
