# 工业泵静止方向与主目录流量配置窄修实施报告

本批按 `docs/superpowers/plans/2026-10-13-industrial-pump-idle-arrow-flow-config.md` 在主目录原位实施，开工 HEAD 为 `6d1baba697079f2c88c86db550d70bc0e0dfb3a1`。执行者未执行 Git 写操作。自动定向检查与唯一增量 assemble 已通过，源码、配置与包已冻结，交 PM 进行本批一次独立规格/质量窄审；客户端局部复看尚未记录。

## 最终行为与写集

工业泵箭头始终显示预定运输方向：同步理论轴速为零或正值时沿 `FACING`，负值时反向。只在 `IndustrialVisualOrientation.displayedOutput` 读取现有 `getTheoreticalSpeed()`；运行时与原 `outputDirection()` 一致，超载/冻结仍由真实 `getSpeed()==0` 停止运输。未新增持久方向状态，原 Renderer、glyph、六面矩阵、运输规则及 R2 素材保持。

源码仅修改以下三路径，其他源码/资源与开工基线一致：

- `src/main/java/com/iksxh/create_nuclear_industry/industrial/client/IndustrialVisualOrientation.java`
- `src/test/java/com/iksxh/create_nuclear_industry/industrial/IndustrialVisualContractTest.java`
- `src/test/java/com/iksxh/create_nuclear_industry/industrial/IndustrialPipeSurfaceTest.java`

主目录实例配置 `run/config/create_nuclear_industry-server.toml` 先完整备份为本批 `config-before.toml`，再仅将 `[reactor].perPortFlowMbPerTick` 从 100 改为 2048。以原始字节替换并使用 `tomllib` 解析，确认唯一键差异、全部其他设置相等、编码/换行/注释保持。该忽略配置不进 Git，未修改任何联机实例或用户热工参数。

## 实际根因与流量消费链

原显示入口对 `getSpeed()==0` 返回 `null`，因此 Renderer 不提交箭头。锁定 Create `KineticBlockEntity.getSpeed()` 在超载或 `TickRateManager.isFrozen()` 时返回零，`getTheoreticalSpeed()` 则读取原同步轴速。定向测试实际执行这些入口与原生冻结状态；最小 `ClientLevel` 夹具只提供 `tickRateManager()`，不替代速度、方向、FaceBakery 或矩阵算法。

端口代码已为 2048：`P1ServerConfig` 的该键默认 2048；`ReactorCoolantFluidHandler.configuredFlowLimitMbPerTick()` 的主被动/专用网络共用物理端口额度与 `ReactorInstrumentPortBlockEntity.coolantInput()` 的冷热账本均读取此键。主目录原配置实际为 100，四个同向口上限合计 400 是其直接配置结果。本批恢复实例值，不改正式 handler、账本、配置注册或默认值，原已验 2048 端口证据复用。

## 验证与保留的失败

本批实际读取并应用根 `AGENTS.md`、活动卡、`minecraft-modding`、`minecraft-testing`、`systematic-debugging`、`test-driven-development`、`verification-before-completion` 与测试参考。技术栈核为 Minecraft 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280 / Ponder 1.0.82 / Flywheel 1.0.6，未按技能示例升级。

原始日志、退出码、XML 与审计均在 `build/reports/industrial-pump-idle-arrow-flow-config/`。

| 证据 | 实际结果 |
|---|---|
| `01-direction-red.log` 与同名 XML 目录 | 修改原两个方向测试后，旧生产入口 2/2 失败，19 秒、exit 1；零速预期 DOWN 得到 null，实际 glyph 预期可见得到 null。属于正确行为 RED。 |
| `02-direction-green.log` 与同名 XML 目录 | 3 项中实际 glyph 与原速度预算代表已通过；方向测试新加的 `pump.flowLimit()` 触及 JUnit 未加载配置边界，抛 `Cannot get config value before config is loaded`。保留该夹具失败，未记为生产故障。 |
| `03-direction-green.log` 与同名 XML 目录 | 去掉不必要的配置装载依赖，以真实 `getSpeed()` 接原纯预算入口，原同 3 项一次必要复跑：3/3、零失败/错误/跳过，17 秒、exit 0。 |
| `04-assemble.log` | 本批唯一 `gradlew.bat assemble --console=plain`，1 秒、exit 0，仅 jar 执行，编译/资源为 up-to-date。 |
| `05-freeze.log` | 首次包审计误设只允许外层 class，实际还含同源 switch 伴随 class，审计 exit 1；错误预期与失败日志保留。 |
| `companion-*.javap.txt` / `companion-javap.diff` | Java 21 javap 核实伴随类唯一差异为 LineNumberTable 的行 16→21；指令、异常表与局部变量表一致。 |
| `06-freeze.log`、`package-identity.json` | 修正必要制品集合为同一源码的两个 class 后冻结 exit 0，无再次构建。 |

最终定向命令为：

```powershell
.\gradlew.bat test --tests '*IndustrialVisualContractTest.displayedOutputKeepsPlannedDirectionWhenStoppedOverstressedOrFrozen' --tests '*IndustrialPipeSurfaceTest.everyActualArrowFaceShowsOpaqueTipTowardRealPumpOutput' --tests '*IndustrialFlowBudgetTest.speedScalesAndCapsOnePump' --console=plain
```

两个原方向方法均覆盖六个 `FACING` × 七个明确状态：零速、正转、负转、正/负超载、正/负原生冻结；共 42 组方向。glyph 方法对每个实际 FaceBakery 面调用真实 `ShadeSeparatingSuperByteBuffer` 变换，尖端/杆部及预定方向保持；正常运行比较正式 `outputDirection()`，停止状态核真实速度与运输预算为零。未跑 64 管道矩阵、R2 表面、桶、整机、fullbuild、素材生产或 Sodium 兼容检查。

环境入口的只读诊断：PATH 中 Python 是不可启动的 WindowsApps 别名，使用工作区依赖工具返回的 bundled Python；javap 不在 PATH，使用实际发现的 `C:/Program Files/Java/jdk-21/bin/javap.exe`。未换 Gradle 缓存、提权或请求重复授权。

## 来源、配置与制品身份

新主目录标准 JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，3,046,392 字节，SHA256：

`745e936c60c473ad2a47f83b9d683a0ae0dc227fdf07024523395edbc196c361`

同字节副本位于本批 `frozen-jar/create_nuclear_industry-0.1.0.jar`。开工时标准文件已不在 `build/libs`，因此从已验 R2 冻结原件备份为 `main-before-b0aef730.jar`，SHA256 为：

`b0aef730ef1411341d1a334985e5cc103a4d636622c02c27c833b8891bd6e6d8`

新旧 JAR 的 2150 个文件路径集合相同，仅以下两个同源编译制品变化，其余 2148 文件载荷一致，全部实际 source assets 与 JAR 一致：

- `com/iksxh/create_nuclear_industry/industrial/client/IndustrialVisualOrientation.class`
- `com/iksxh/create_nuclear_industry/industrial/client/IndustrialVisualOrientation$1.class`，仅调试行号变化。

两 class 均与 `build/classes/java/main` 对应实际编译制品逐字节一致。用户原位 `build/libs/create_nuclear_industry-0.1.0_261012.jar` 仍为原 `ecbe0e38…`，未覆盖或改名。已验 R2 原 11 来源中仅原 SurfaceTest 因本次箭头合同更新而变化，其余 10 来源零漂移；当前共 16 来源连同三处正式配置消费链以 `sources-final.json` / `frozen-sources/` 冻结。

配置身份：

- 原配置 SHA256：`7b30ec8e812bcb67e5d4d5d69fbedb081df6f4ae35e345afb384f483b1ddef10`
- 新配置 SHA256：`1c43d5bfbb736dc60c9a3f585e80161d1ffc2c4e0cab780a3c4ba6120954af1a`

`before-files.json` 记录 5887 个开工文件，最终所有任务外受保护文件零变化；仅测试框架正常更新 `logs/debug.log` / `logs/latest.log`，未清理或回退。原主目录 dirty、工具与素材保持。未建分支、搬动或写入 `run/saves`；用户继续游玩的正常保存不计作本任务写入。

## 交 PM 后的边界

只待本批一次独立窄审和两项集中客户端局部复看：停转/超载时箭头可见并保留预定方向；配置在实际服务端加载后，供液、动力和管路条件充分时核单个冷热口的 2048 mB/t 上限。四个同向口理论上限合计 8192 mB/t；冷热分流、实际产热和管路瓶颈会限制真实流量，不以 2 冷 2 热混计为保证合计流量。

自动证据不登记客户端视觉或现场流量已通过，不重新开启已验 R2、管网/汽轮机或其他独立门；Sodium/Iris、管理员、压力系统及其他主线仍按原暂停边界保持。
