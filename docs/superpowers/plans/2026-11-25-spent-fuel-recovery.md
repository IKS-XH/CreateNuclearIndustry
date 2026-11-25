# 0.2.0 枯竭燃料组件金属回收

用户于 2026-11-25 采用[金属回收提案](../../proposals/2026-11-25-spent-fuel-metal-recovery.md)，明确删除整件枯竭组件直接封装，使用扳手切换屏蔽装配台工序，并在护目镜显示当前工序。

- 执行分支 `codex/0.2.0-spent-fuel-recovery` 基于 `534d88a0`，位于 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-explosion-fix`。同树切分支保留原位世界，自动测试使用隔离目录。原生物辐射与铀料浆候选源码及包保留。
- 采用提案两档。默认「装配／封装」处理燃料制造、残渣和事故废料封装，「拆解」只处理未封装的枯竭铀燃料组件。已成型装配台控制器及成员均可用扳手普通右击切换，输入、输出与运行状态清空后才能切换，忙时显示原因。保留潜行扳手拆除及原有其他操作。模式由服务端判定，正常存取、拆装与观察均走现有同步和持久化入口。护目镜首行保留图标留白。
- 每件枯竭组件消耗 12800 RPM·tick 工作量，按既有转速规则产出 4 钢板、9 锡粒、3 铅粒和 1 乏燃料残渣。一次提交整批产物，各种产物可独立取出，沿用侧面过滤漏斗、置物台和机械臂的交接方式；堵塞、重载、拆台不得丢失或复制物料。残渣使用枯竭组件辐射配置和掉落实体保护。1 残渣、1 固化基材和 1 空铅桶按现有封装工作量生成记录真实残渣的封装桶，沿用干式架屏蔽、存取和已封装合法载荷。旧整件封装配方退出配方集。JEI 展示四个产物，思索只介绍扳手切换、拆解与封装的使用流程。
- 技能入口为 `C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-resource-pack,minecraft-ci-release}/SKILL.md`。锁定 MC 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。0.2.0 仍为同批候选版本。

## 执行写集

- `src/main/java/com/iksxh/create_nuclear_industry/production/` 下 `ShieldedAssembly*.java` 的配方、状态、库存、方块、机械臂和 JEI 接口。允许在此命名前缀新增模式类型。
- `content/SpentFuelStorageContent.java`、`content/ModCreativeTabs.java`、`storage/SpentFuelPayload.java`、`radiation/RadiationItemSources.java`、`radiation/RadiationSourceAdapters.java`、`radiation/client/RadiationClientEvents.java` 的注册、载荷与辐射接线。
- `ponder/ShieldedAssemblyPonderScenes.java` 仅更新用法，`assets/create_nuclear_industry/lang/{zh_cn,en_us}.json`、`models/item/spent_fuel_residue.json` 可使用现有贴图，`data/create_nuclear_industry/recipe/shielded_assembly/` 仅删除旧配方及新增两项配方。
- `gametest/ExtensionShieldedAssemblyGameTests.java`、`gametest/ExtensionSpentFuelStorageGameTests.java` 及受影响的装配台、贮存架、辐射、配方和教学 JUnit 合同允许改动。额外文件先报 PM。新改注释中文，保留用户世界、正式包、依赖、其他设备和用户工作。执行者只读检查 Git，不修改文档与 Git 状态。

## 验证与交件

运行受影响 JUnit 和 `runGameTestServer` 现有装配台、贮存架测试域，核对扳手、模式储存、四产物提交和提取、封装真实载荷、真实机械臂及拆装守恒。复用此前候选与本批无关的辐射和流体证据。验证编解码与资源后，在源码确认后执行一次增量 `assemble` 并核包内配方和摘要。原始日志与执行报告放在 `build/reports/spent-fuel-recovery-20261125/`。PM 安排一次组合审查，真实客户端观察和操作并入 0.2.0 集中手测。
