# STORE-01-CODE 实施交付

状态：功能与本批自动检查完成，未提交改动交项目经理审查；客户端联合手测及最终验收待用户完成。执行目录为 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支 `codex/ore-acquisition`；派发基线 `15212d0`，报告时 HEAD 为项目经理追加文档后的 `4268d00`。执行者没有执行 Git 写操作、派发其他代理或操作用户客户端/存档。

## 实际实施

- 独立 `SpentFuelStorageContent` 注册三种普通材料、单件封装桶和单格贮存架及 BE；创造页提供有效载荷的示例桶。架子 `facing` 四向、`storage_level=0..4`，完整单格碰撞、无 GUI、无动力，Create 构造搬移禁用。
- `SpentFuelPayload` 只读检验正式单件桶和恰好一个正式单件枯竭组件；`seal` 生成完整 `DataComponents.CONTAINER` 载荷，保存自定义名等原始组件数据。新燃料、无载荷、多件和异物均被拒绝。制造配方也明确拒绝枯竭组件及封装桶作为输入。
- 装配配方改为 `operation` 和通用 `inputs` 列表。原制造仍为 8/4/2/1、25600 RPM·tick；封存为 1/1/1、12800 RPM·tick，两普通输入使用素材侧确定的专属标签。首件真正接受的原料选工序；模拟不选工序，输入/输出/进度全空后解锁。各面、玩家及机械臂共用一个账本，完成整批才原子扣料出一件。普通材料数量和工时真实读取数据，不再硬编码拒绝合法修改。
- 原 32 RPM 门槛、256 RPM 计工封顶、正反转同效及 4 SU/RPM 保持。停转、低速、过载、满输出保留有效工时；退料不足批或配方失效清工时并保留物料。保存活动工序、批次参数、完整输入/输出及进度。JEI按实际三/四料和数据工时显示64RPM参考10/20秒，护目镜显示活动工序与实际材料数量。
- 贮存架保存固定16槽，每槽一件完整桶；顶面和四侧可插/取，底面只取，所有能力实时访问同一账本。手持桶放一件，空手取最后占用槽，背包满不扣库存。容量减小时同时限制总已用数及有效槽范围，超额全部保留和可提取；占用外观只由真实数量派生。正常/创造拆除及扳手回收只散落一次桶，空架掉落不夹带库存。

## 实际读取的技能与版本

实际读取并应用 `AGENTS.md`、治理5.1/5.2、完整 STORE-01实施卡及已确认轻量规格，以及：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：采用本版本 DeferredRegister、方块实体、能力及服务端事务，未采用技能中的新版本升级示例。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：纯账本 JUnit 与真实注册/机器 GameTest 分工。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/test-driven-development/SKILL.md`：先补三料动态批次、模拟不锁工序及清空切换关键用例，记录旧实现缺少接口的编译失败，再实现；其他边界由同批定向用例覆盖。未宣称逐方法全部红绿。
- `C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/verification-before-completion/SKILL.md`：只按真实日志、用例数及退出码报告；精简验证、Git禁令及人工门遵循治理优先规则。

版本核对：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。未改构建脚本、依赖、许可证或机器尺寸/成本/动画。

## 本轮证据

原始证据根：`build/reports/extension/STORE-01/`。

1. `red-missing-api.log`：新增关键用例先在原实现编译失败，缺少动态配置和工序插入接口，非业务通过记录。实现阶段两次必要 `compileJava` 均通过，用于核对1.21.1实际API，未进行开工全量检查。
2. `junit-assemble.log`：`./gradlew.bat test --tests '*ShieldedAssemblyStateTest' --tests '*DryStorageStateTest' assemble --console=plain`，exit 0，23秒。JUnit真实匹配8项：装配5项、贮存3项，失败/错误/跳过均0；对应XML复制在同目录。唯一最终增量assemble已打包素材交付。
3. `gametest.log`：`./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_store01 -PgameTestDirectory=build/run-store01-gametest --console=plain`，exit 0，23秒。日志明确仅启用本批命名空间，6项实际发现并全部通过，服务器正常保存退出。场景分别为载荷拒收/原始记录保存、三种首料/模拟/混料、真实Create底部动力封存后切回原制造、合法配方数量/工时修改及失效恢复、六面16槽/满背包/减容保存、普通/创造/扳手唯一掉落及缓存能力失效。
4. `generated-storage.toml`：真实独立 SERVER 文件成功生成，`spentFuelStorage.rackSlots=16`，范围1～16。
5. 本批源码/测试/语言/装配配方 `git diff --check` 通过。增量编译有既有 NeoForge 弃用警告；首次隔离服务器未找到 `server.properties` 后生成并继续正常执行，未出现本批配方解析失败。没有 `clean`、`--rerun-tasks`、全域GameTest、热端全量或旧存档专项。

制品：`build/libs/create_nuclear_industry-0.1.0.jar`，2,144,665字节；SHA-256 `FD3A507934AE6F39CA6A78CE37F988ED3C884EFAA7302099679B7152C0EC3978`。本轮证据验证的是当前版本保存/加载，并非旧版本兼容。

## 写集与交接

代码执行者实际修改入口、创造页、装配台 State/Recipe/BE/Block/JEI/Arm、两语言和两装配JSON；新增独立注册、`config/SpentFuelStorageConfig.java`、`storage/{SpentFuelPayload,DryStorageState,DryStorageBlock,DryStorageBlockEntity}.java`、贮存账本JUnit、`ExtensionSpentFuelStorageGameTests`及其 `data/create_nuclear_industry_store01/structure/p0_probe_empty.nbt` 模板，修改原 `ShieldedAssemblyStateTest`，只写本报告一个docs文件。原 `ExtensionShieldedAssemblyGameTests`未改，保留业务断言；其原制造相关行为由本批真实动力场景回归。

**项目经理须显式纳入新增配置源码：** `.gitignore` 第15行的通用 `config/` 规则确实忽略 `src/main/java/com/iksxh/create_nuclear_industry/config/SpentFuelStorageConfig.java`；普通 `git status`不会显示它。本报告提示具体文件，执行者未更改忽略规则或暂存文件。

素材执行者持有模型/PNG、四条原生配方、标签/loot及素材报告，本人没有修改其文件；最终构建读取其已就绪资源。既有 `logs/debug.log`、`logs/latest.log`、`tools/art-assets/__pycache__/` 保留，不纳入交付。原始测试日志和隔离运行目录为构建产物，不作为源码提交。

剩余人工门沿用项目经理联合清单：原生磨石/热搅拌和制桶/造架实际生产、原制造/封存切换、Create/原版漏斗搬运、16桶满架拒收与取出/拆除/正常退出重进、四向空满外观/碰撞、中文护目镜与JEI。自动通过不代表这些客户端视觉和人工验收已通过。

## 统一审查后的资源整改打包

素材执行者已修复唯一P2：`dry_storage_rack_0.json`补齐 `minecraft:block/block` 父链，并同步生成器和实际MC JAR父链检查。功能代码、纹理、几何和数据未变。本执行者按PM派发只运行一次 `./gradlew.bat processResources jar --console=plain`，exit 0，4秒；`processResources`和`jar`实际执行，`compileJava`为UP-TO-DATE。未重跑JUnit或GameTest，继续复用上文8项/6项自动证据，未修改功能源码或执行Git写操作。

整改后候选制品仍为 `build/libs/create_nuclear_industry-0.1.0.jar`，现为2,144,687字节，SHA-256 `90A96EB4EE401FC56B529120A3A165A822CDAF747F2DBE3A9B49782F1B0E8FB1`；上文2,144,665字节及旧SHA保留为首次自动验证时的制品上下文。已直接打开新JAR并确认 `assets/create_nuclear_industry/models/block/dry_storage_rack_0.json` 的 `parent` 为 `minecraft:block/block`，零档父链确已打包。新增证据为 `build/reports/extension/STORE-01/resource-parent-repack.log` 与 `resource-parent-repack-evidence.json`。客户端联合手测门仍未通过，交付后停写。
