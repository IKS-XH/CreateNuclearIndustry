# EXT-B-EXCHANGER-01C-ADAPTIVE 执行报告

执行者 MASS；候选 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，基线 `3ea9e89`。按用户已确认的自适应方案与 PM 转授权修正 LOWFLOW 初稿。实际应用 minecraft-modding、minecraft-testing、systematic-debugging 技能；锁定 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280，无版本变更。

## 实现

- `HeatExchangerState` 按 `reserve / (huPerLevel * bufferTicks)` 下取整选档，先实付本 tick 热量后发布，再以额定最高转换量补储。默认峰值 18、36 mB/t、720 HU 和 0.5 HU/mB 均保持。
- 无转换绝对期限先于付款和新转换处理；过期旧热不能被同 tick 新热液复活。NBT 保存绝对期限，旧格式在首次 tick 前再次保存仍保留待迁移标志；卸载跳时、时间回退和配置变化按保守散热处理。
- 储热尾部不足 1 mB 空间使用向上量化并有界散失，避免 0.3 HU/mB 等自定义密度永远无法触及档位阈值；默认 0.5 无新增尾差。储备不突破上限，转换仍等体积。
- `running/residual` 根据实际补热是否覆盖本 tick 实付判断，持续 18 mB/t 的 9 级稳态显示正常供热。中英文提示改为自适应、最高转换能力与最长剩余余热窗口，不写死自定义配置的峰值。

## 验证准备与当前状态

`HeatExchangerStateTest` 共 12 项，覆盖逐 tick 18/36 mB 注入且排冷的 9/18 稳态、体积与热量守恒、先付款/查询与重复 tick 纯净性、分数档位、脉冲与中途保存、堵塞截止、卸载/携物期限、旧 NBT、无效配置、0.3 密度启动且输出不超过实付、超大密度及不足一批冷罐空间、自定义配置与回退时间。

原 `ExtensionHeatExchangerGameTests` 保留锅炉尺寸/供水、拆放、跨区块卸载、真实 FULL 不 tick 门与旧能力失效覆盖；调整逐步升温等待。新增真实 Create 锅炉逐 tick 18 mB 输入，收敛后连续 40 tick 验证 9 级及正常供热状态。未修改 REPRO 独占 Loop 类。

双语 JSON 解析及本写集 `git diff --check` 通过。全工作区检查另有运行生成的 `logs/*.log` 空白告警，未改这些范围外文件。按 PM 要求未自行运行 Gradle；编译、JUnit 与指定 GameTest 命名空间由 REPRO 统一验证，本报告不将静态检查当作通过证据。

仅改授权的状态类、其 JUnit、原锅炉 GameTest 和既有换热器语言键；未改 BE/锅炉桥接、依赖、用户客户端/存档或 Git 状态。
