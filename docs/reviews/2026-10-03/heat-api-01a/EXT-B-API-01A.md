# EXT-B-API-01A：Create 换热接入口源码探查

- 候选：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支 `codex/ore-acquisition`，基线 `e608989`。检查时另有任务卡和 `tools/art-assets/__pycache__/` 未跟踪项；均未触碰。
- 锁定：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82（`gradle.properties`）。本地 Create source JAR：`C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280/23e1219501c0debfa0bb56c30ef8e0193341aae5/create-1.21.1-6.0.10-280-sources.jar`，SHA-256 `376DE15CA5ACF720106A075CA4EB2EF53E63E0E5D9EC93523A1ABBDD0F9F0CB4`。
- 实际读取技能：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 与 `minecraft-testing/SKILL.md`。本轮以本地锁定版本源码为准；测试建议采用真实 Create GameTest 场景，不从通用技能样例推断 API。
- 唯一写入：本报告与同名证据目录。未改核心文档、源码、测试、构建文件或 Git；未运行构建、GameTest、客户端。

## 发现

Create 有两个不同的热接入合同。流体储罐锅炉查询公开 `BoilerHeater.REGISTRY`：每个底部方块返回数值热等级，由 `BoilerData.updateTemperature` 对锅炉底面 `width × width` 个方块累加。返回正值表示主动热，`0` 是有限被动热，负值表示无热。接口签名返回 `float`，但 Create 将每次值加到 `int activeHeat`；使用整数等级，避免小数累加被截断。

18/9 的文档用语存在量纲差异。`project.md` §4.2、§10验收把核热/蒸汽热写成“相当于18个超级燃烧室/9个普通燃烧室”；§4.2/§4.3及§4.4又分别写默认 `return 18`/`return 9`。在 Create 6.0.10-280 中，一个 `SEETHING` 超级烈焰人燃烧室返回2级，一个 `FADING` 或更高的普通加热燃烧室返回1级。因此：

- 18个超级燃烧室原始聚合是36级，但 Create 有效锅炉热等级上限18；达到上限只需9个 `SEETHING` 源。
- 返回18的含义是“提供18个Create锅炉热等级”，不是“等效18个超级燃烧室”；返回9同理是9级，数值上相当于9个普通 `HEATED` 源。
- 锅炉实际等级还受锅炉尺寸与供水限值约束：`min(activeHeat, min(18, boilerSize/4), min(18, ceil(waterSupply)/10))`。完整得到18级要求至少 `boilerSize=72` 且 `waterSupply=180 mB/t`；9级对应至少36与90 mB/t。它们不是 `HU/t`。
- `BoilerData.isActive()` 只判断已连接蒸汽引擎或汽笛数量大于0；不判断引擎当前RPM、是否有有效应力负载或用户是否实际消耗动力。Create 热值 callback 也不是 HU 扣账/传输事务。

动力搅拌盆不读 `BoilerHeater.REGISTRY`。`BasinBlockEntity.getHeatLevel()` 每 tick 清空缓存后，直接读盆正下方方块状态；若它带公开 `BlazeBurnerBlock.HEAT_LEVEL` 属性则取其值，否则仅 `PASSIVE_BOILER_HEATERS` 标签可给 `SMOULDERING`。Create 配方把 `SUPERHEATED` 映射为必须 `SEETHING`，把 `HEATED` 映射为 `FADING/KINDLED/SEETHING`（不接受 `SMOULDERING`）。

现有 Basin 配方匹配本身会检查热级：`BasinOperatingBlockEntity.getMatchingRecipes` → `BasinRecipe.match` → `BasinRecipe.apply(..., test=true)`；`apply` 先以 `getRequiredHeat().testBlazeBurner` 拒绝错误热级。因此，“先从当前匹配配方/currentRecipe 判断有需求，再供热”形成循环：缺热时目标热配方不会成为匹配结果。Create 未在这些入口提供可跳过热条件的匹配方法。

要称为真实盆负载，至少要有满足配方的实际物品/流体输入、允许产物输出、正转速的机械搅拌器，以及实际完成其处理时间；仅放置盆和搅拌器不构成有效加工。锅炉的“活跃”也只有已连接引擎或汽笛，不等同于正在消耗热量。

## 无 mixin 的最小接入

1. 在适当的模组注册生命周期为换热器方块直接执行 `BoilerHeater.REGISTRY.register(block, callback)`；由 callback 从服务端权威换热账本读当前可供核热等级，空/断供返回 `NO_HEAT`，核热/蒸汽模式输出冻结为18/9级配置值。该 registry 是 Create 明确公开的线程安全 `SimpleRegistry`，直接注册优先于 provider，不需要 mixin。
2. 在同一方块状态定义中增加并同步 `BlazeBurnerBlock.HEAT_LEVEL` 属性，使正上方 Basin 按 Create 原生读法看到 `SEETHING`、`KINDLED` 或 `NONE`。`BoilerHeater` 注册不会自动令 Basin 变热；必须显式提供 Basin 认识的状态属性。
3. 如果政策只要求热源按可用HU供热，则由换热器账本驱动上述热状态，Create 自己负责匹配/执行配方。若政策要求“只在存在可执行的某个加热配方时才公开热”，不能依赖 `currentRecipe`；须另实现不含热条件的输入/流体/输出可行性查询，避免循环，并把该查询作为实现边界单独冻结。

FluidTank 在正下方方块发生邻居更新时调用 `updateBoilerTemperature`；后者只有锅炉 `isActive` 才置脏标志，随后由控制器 tick 重扫热源。Create 的这条 API 没有热源卸载回调；`BoilerData` 会持久化 active heat 和更新标志。故断供/温级切换应同步更新可读状态并触发更新；源端权威HU仍由换热器自己守恒。持久化恢复、源/锅炉卸载重载后的刷新顺序，以及断供后是否有一 tick 缓存窗口，均不能仅凭接口文档判为通过，须在真实用例覆盖。

PM当前建议首批只接入真实Create储罐锅炉、暂不注册蒸汽，最终三模式规划保留；该产品范围仍待用户确认。盆热级入口另立后续任务，不改写Create配方匹配，也不把盆测试冒充首批验收。

未来应力源只定位到 `BlockStressValues.IMPACTS`、`CAPACITIES`、`getImpact`、`getCapacity`，未研究多轴网络或输出分配。

## 下一步最多3个定向真实用例

1. Create 原生流体储罐锅炉：配置18/9级来源值及不同锅炉体积、供水量，核对有效等级受尺寸和供水限制。
2. 真实换热负载下供热、断供及输出堵塞：确认热量/冷却剂守恒、无负载时不重复结算，解堵后可恢复。
3. 来源持续供热后断供、拆除及重载：检查锅炉刷新、保存恢复和旧热级清除。

盆上运行实际加热配方的用例留待后续盆热级任务，不属于建议首批用例。
这是基于锁定 JAR 的静态源码核查；没有运行探针、构建或实机结果，不表示接口行为已测试通过。源码定位和必要摘录见同名目录 `build/reports/extension/EXT-B-API-01A/`。
