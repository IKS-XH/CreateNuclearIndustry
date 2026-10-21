# ACCIDENT-THERMAL-04A 执行交付

2026-10-21；唯一工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-accident`，分支 `codex/accident-thermal-04a`，基准 `37f50b9e3425992f74beecb8ecd6e2c79514257e`。源码及必要自动验证已交付，等待 PM 安排一次组合窄审；没有 Git 写、打包、合入或人工验收结论。

## 行为与所有权

- 新事故的每件真实废料仍只交给一个 Corium BE；原 Column 随即 finish 清空可提取 payload。热状态只保存事故/位置引用、计时与收据，绝不保存物品。提前采掘沿原 onRemove→take 只掉既有一件废料，立即终止热引用；冷却不改物品或辐射源。
- 默认高温 2400 个实际加载 tick；六面相邻已加载水/含水方块每 tick 扣 4，否则扣 1。未加载不读取 BE/邻水，不用 gameTime 差追补，不持区块票据。每个对象按当前 gameTime 防止普通同 tick 重入重复扣时。
- 顶面 stepOn 与 entityInside 共用服务端接触入口，必须同时具有真实 payload 和活跃热记录。实体持久数据按同类节流 20tick，默认 2HP hotFloor 直接热伤并点燃 3秒；保留原生火抗。HOT 方块状态只作客户端冷热投影，空外观不能发热或造料。
- 原破裂区内冻结最多 8 个一次性火点候选。初始结算 complete 后，仅已加载、空气、有实心顶面支撑且不属于新机组的位置尝试一次；失败、扑灭或到期均不换位置/重新点燃。每火默认最多 600 个加载 tick，UUID 租约同时存于 SavedData 收据及火 BE，清理只认同一租约。
- AccidentFireBlock 继承 BaseFireBlock 的接触/扑灭形状和原生左键销毁路径，但覆写 onPlace 禁门户，不继承 FireBlock 的蔓延 tick；不增加粒子或声效。火点 BE 只存身份，没有计时/物料副本。逻辑到期立即无危险，预算不够时物理清理等待；不删后来方块。
- 原 complete() 仍只读原冲击/破裂/物料交接收据。SavedData 分开维护初始活跃事故和热活跃事故，热状态只遍历活跃位置，终态退出集合；保留终止收据供去重，不逐 tick 扫全部历史事故。
- 原动作/冷热投影/火点共用每维度现有预算和轮转；一项热动作至多写一格，重复 tick 入口不刷新额度。原保守收据处理继续占既有动作额度，因此真实世界写不会超过 64/t。卸载时不强载、不补做。

## 配置和资源

AccidentConfig SERVER 组新增 `solidCoolingLoadedTicks=2400`、`waterCoolingRate=4`、`firePointLimit=8`、`fireLoadedTicks=600`、`solidContactDamageHp=2.0`、`solidContactIgniteSeconds=3`，在事故创建时冻结并随热状态保存。20tick 同类节流固定。整组先校验再发布，非法整组回退默认；旧事故不跟随热重载改变规则。

火点 8/600 上界来自 04A 的“最多8/各最多600加载tick”明确安全边界；固体时长/水倍率允许合法正 int，不把默认值误当上界。HP 必须有限、非负且可转 float；点燃秒数非负且转换原版 tick 不溢出。

HOT/冷 blockstate 分别借用原版 magma/既有 crying_obsidian 模型。独立火模型借 `minecraft:block/fire_floor0` 和 `minecraft:cutout`，无需新增客户端事件类。火标签、空 loot 及双语名称齐备；没有流体、桶、软管 mixin、熔融配置、配方、GUI、音效或美术扩项。

## 实际验证

定向命令：`./gradlew.bat test --tests '*AccidentThermalStateTest' --tests '*AccidentLedgerTest' --console=plain`。

真实服命令先设 `CNI_ACCIDENT_FOCUS=thermal_solid_fire`，运行 `./gradlew.bat runGameTestServer -PgameTestNamespace=cni_accident_execution -PgameTestDirectory=build/reports/accident-thermal-04a-20261021/<对应隔离目录> --console=plain`，结束移除该环境变量。复用既有域/模板；thermal focus 同时复用 shutdown 业务，没有新框架/构建规则。

| 原始证据 | 实际结果 |
| --- | --- |
| `01-red.log`、`red-unit-results/` | 编译成功；8 项 JUnit 中 4 项业务按预期失败：2400/接水×4、不可用暂停/同tick去重、火8/600/终止、同维度预算。Gradle exit1。不是缺类/编译失败充当 RED。 |
| `02-unit.log` | 最小实现后原8项通过，exit0。 |
| `03-unit.log`、`final-unit-results/` | 补配置冻结/保存断言后 9/9，0失败/0跳过，exit0。包含完成与热活跃独立、真实payload不进入热记录、终止不复燃、重入不可重热、共享预算。 |
| `04-server.log` / `server-focused/` | 2业务失败、11 SKIP。旧shutdown误要求整个维度返回0（同时有合法热动作）；新站立夹具尚未显式产生运动碰撞。保留失败并将旧收据断言改为自身原接管终态，实体显式走原生 Entity.move 碰撞。Java exit2/Gradle exit1，所有维度保存。生产热逻辑未因这两项修改。 |
| `05-server-related.log` / `server-related-recheck/` | 仅上述相关夹具复验，2业务通过/11 SKIP；Java/Gradle exit0，全部维度保存。 |
| `06-server-final.log` / `server-final-focused/` | 将水到达火格、真实提前采掘单件掉落及邻木不燃的断言收紧后的最终证据：**2实际业务通过/11 SKIP**，Java/Gradle exit0，全部维度保存。框架的 All13 含SKIP，不称13项业务通过。 |

最终真实场景验证：两座5³实际事故产生17份真实废料+1完整全新返还；工质全损；顶面实际 Entity.move 碰撞 2HP/点燃；直接原生 stepOn 多格与20tick后续触节流；原版火抗；空HOT外观安全；每事故8租约；FakePlayer 原生 START_DESTROY_BLOCK 左键灭火；真实向下水流到达火格；600加载tick到期、不复燃、不烧邻木；预算耗尽时到期先无危险且不越额删块；后来石块不被清理；真实提前采掘恰好一件废料；真实BE当前格式与整个DataStorage读回不重置计时/载荷/火收据；水冷及2400实际加载tick自然冷却；所有热活跃集合最终清空。聚焦服以加速 GameTest tick 运行，2400 是服务端实际 tick 数，不是120秒现实等待。

同维度逐tick核全部现有事故计数差，实测峰值64。shutdown同时在热活跃期间完成正式末tick事故→幸存空口→新仪表重绑→CommonHooks维护和FULL_SHUTDOWN_RESET；旧锁/原件/废料/绑定及拒绝保护矩阵沿已有断言。Port、Lifecycle、CommonHooks最终事务、热工/停机生产源码均未改。

`RESOURCES.json`：按本机锁定 1.21.1 原版资源 jar 递归解析新/改模型父级和贴图、冷热 blockstate、fire标签、空loot、双语名称通过。`verify-resources.ps1`是本报告目录内的定向检查；未运行datagen或修改构建。`git diff --check -- src` exit0；根跟踪日志由JUnit生成的尾随空格保留交PM，不伪称全树diff空白通过。

初次隔离服自动创建缺失 server.properties 的原版 ERROR/NoSuchFileException、已有 ApiStatus/ponder refmap 警告均保留在日志，后续有明确测试结果、正常保存与退出值。资源辅助检查器首次处理原版省略命名空间和空variant键时失败，已仅修报告脚本并完成资源解析，不涉及玩法失败或扩项。

## 写集、保护与交付边界

准确修改/新增源码、资源及编译 class SHA 见 `SOURCE.json`，已跟踪差异见 `SOURCE.patch`。所有功能改动均在04A精确允许写集，含4个新事故类、6个原事故类、AccidentConfig/AccidentContent、既有真实域及2个定向测试；资源仅卡内列项。无需扩写Port/辐射/生命周期/封存/共享构建。

候选树 `docs/project.md`、`docs/server-config.md`、`docs/content-catalog.md` 的本批同步由PM维护，属于PM文档改动，未由本执行者修改，不计执行者超写集。

开工跟踪源码干净，原JAR/根logs先备份至 `pre-validation/` 并记录 `PRE-VALIDATION.json`。JUnit确实改写跟踪 `logs/debug.log`、`logs/latest.log`，执行者已报告PM，**未自行恢复**。JAR仍逐字等于原基线3478572 bytes，SHA256 `81E7CD98D589DDC9C0AAF398C06776DC4318EA54C70CD19376F0C127E4A3DE7B`。未运行jar/assemble、未覆盖main正式包；所有隔离世界和原始日志仅本报告目录。

实际读取并应用本机 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`（服务端/注册/加载边界）、`minecraft-testing/SKILL.md`（现有定向JUnit与真实GT）、`minecraft-resource-pack/SKILL.md`（1.21.1资源路径/原版模型引用）。基线已核 MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；未升级。按治理5.1复用旧爆裂/最终取消/退出矩阵，仅跑本批受影响项，无clean/build/强制重跑或旧存档兼容调查。制品阶段minecraft-ci-release尚未进入。

已验证当前格式真实BE/DataStorage序列化恢复及本次正常停服保存；**没有另外启动一次服务端读取本次热活跃世界**，不能冒称本批完整进程重启续热已观察。没有实际物理ChunkEvent.Unload、真人多人或Minecraft客户端视觉/操作验收；不可用暂停由纯状态测试和生产已加载检查保证，旧FULL不可用实证仅按原报告复用。用户原位 `run/saves/新的世界` 未启动或改写，其他客户端/工作树未碰。

停在一次组合窄审门。后续由PM核来源并提交，再安排唯一一次增量jar；集中人工看冷热外观、实际接触/火抗、灭火、水冷、采掘封装及正常保存重进。本卡不代表熔融物已实现或参数已冻结。
