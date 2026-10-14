# EXT-A-FUEL-01 任务1：功能执行交付

**执行范围：** `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，`codex/ore-acquisition`。只交付未提交改动；未执行任何 Git 写操作，未触碰主工程或默认 `run/`。本报告仅陈述功能执行者实际完成与验证，不代替项目经理验收。

## 实现

- `content/FuelProcessingContent.java` 独立注册精矿、尾矿、尾矿砖、两种核粉、料浆源/流动体、密闭料浆桶、离心机、BE、菜单及 `centrifuging` 类型/序列化。`CreateNuclearIndustry.java` 仅添加注册、朝向能力与 Create 装置移动拒绝接线；创造栏、双语语言仅增加本批条目。`build.gradle` 仅加入锁定 JEI `19.27.0.340` 的 `compileOnly`，未改变运行配置或技术栈。
- `production/CentrifugeState.java` 为双罐（各 4000 mB）、双槽（各 64 件）、密闭批次及轴承积分的唯一账本。启动前和完成时均检查两粉与水全部空间；开始时扣取 1000 mB 料浆，保存配方 ID、输入/输出物品和流体快照、工作量；堵塞暂停而不丢副产。世界保存与携物取下使用同一 NBT 格式。磨损以 `1/1024 RPM` 积分及小数余量记录，上限 `18874368000`，避免小数转速截断或永不耗尽。
- `CentrifugeBlockEntity` 使用 Create 的实际转速/过载与固定 `8 SU/RPM`；正反转取绝对值、`256 RPM` 吞吐封顶、连续稳定 20 tick 后推进。只在推进时磨损，停机后重型轴承维修。端口朝向为前面 `facing`、顺时针料浆、逆时针水、背轴、底面只取粉；旋转后的旧流体能力也逐次核对实体当前朝向。
- 标准桶能力和 `CentrifugeContainerTransaction` 进行 1000 mB 模拟及提交；手工容器在相应侧交易，失败不改玩家手持物。机器普通挖掘和 Create 扳手取下共用单件携状态掉落；放置恢复库存、批次、进度和磨损，拒绝活塞和 Create contraption 搬运。
- 正面菜单、客户端面板、护目镜显示罐量、双粉、进度、转速、轴承与停机原因；菜单校验方块和 8 格距离，产物槽不可插入。JEI 类只由已安装 JEI 的客户端扫描，展示料浆、两粉、水和额定工作量；无 JEI 的 dedicated server 已加载成功。
- 本线数据文件为 `recipe/centrifuging/uranium_slurry.json` 与 `loot_table/blocks/enrichment_centrifuge.json`。数据线的四个原生配方/标签和美术线的模型/纹理未由本执行者修改，已在最终资源打包中一同纳入。

## 实际验证及原始证据

1. `./gradlew.bat test --tests '*Centrifuge*' --max-workers=1 --console=plain`：退出 0；`build/test-results/test/TEST-com.iksxh.create_nuclear_industry.production.CentrifugeStateTest.xml` 为 5/5，`...CentrifugeContainerTransactionTest.xml` 为 2/2。覆盖实际 JSON codec 解码、双粉/水堵塞守恒、已接管批次及磨损 NBT 往返、正反转稳定/小数 RPM 磨损、耗尽/零速维修；容器边界使用锁定 NeoForge 的真实 `FluidBucketWrapper` 和 `FluidTank`，覆盖满罐、空罐、错误流体和整桶成功。它不能代替游戏内 Create 储罐/盆互通体验。
2. 美术通知正式资源稳定后，`./gradlew.bat assemble --max-workers=1 --console=plain`：退出 0。桶事务代码更新后再次增量 `assemble`：退出 0，`compileJava` 与资源任务为 UP-TO-DATE、`jar` 执行。制品 `build/libs/create_nuclear_industry-0.1.0.jar` 的 SHA-256 为 `3CD134F69F92088A2DD530809DCA8A9D7FFFDD2A53BAFD21CC137FFB70E24E08`。
3. 隔离专服：`./gradlew.bat -I build/reports/extension/EXT-A-FUEL-01-runtime/isolated-server.init.gradle centrifugeRunDirectory --max-workers=1 --console=plain` 退出 0；原始目录检查见 `build/reports/extension/EXT-A-FUEL-01-runtime/directory-check.log`。NeoForge run 模型及实际 `JavaExec.gameDirectory` 均指向同一报告目录内 `isolated-server`，不指向默认 `run/`。
4. `./gradlew.bat -I build/reports/extension/EXT-A-FUEL-01-runtime/isolated-server.init.gradle runServer --max-workers=1 --console=plain` 在无 JEI 的 dedicated 模组列表下加载，隔离日志 `build/reports/extension/EXT-A-FUEL-01-runtime/isolated-server/logs/latest.log` 记录 `Loaded 2872 recipes`、`Done (5.280s)`。输入 `stop` 后完成所有维度保存，Gradle 退出 0。未运行全量 GameTest，也未修改默认服务器世界。

**测试产生的范围外日志：** 定向 JUnit 的 ModDevGradle 测试进程修改了仓库根 `logs/latest.log` 和 `logs/debug.log`。这两份文件在开工时为干净状态，执行者未重写、恢复或纳入交付；已通知项目经理按 Git 权限处理。`git diff --check` 对功能写集无空白错误，提示仅来自上述两份运行日志。

## 客户端人工门与维修加速

仍待一次真实客户端验收：按固定 21 格动力合成器生存制造；Create 水洗/制浆到料浆、离心产 1/7 粉及返还 1000 mB 水；两侧标准桶与 Create 储罐/盆满空/错流体、底面原版漏斗和 Create 漏斗；变速、反转、过载、堵塞、停机维修；菜单、护目镜、JEI 和北向外观；保存重进与普通挖掘/扳手拆放的批次延续。这些未因 JUnit/专服加载而宣称通过。

为免等待两小时，可在隔离测试世界确认目标坐标后使用：

```mcfunction
/data modify block <x> <y> <z> WearUnits set value 18874236928L
/data modify block <x> <y> <z> WearRemainder set value 0.0d
```

机器有密闭批次、产物空间足够且轴网稳定在 256 RPM 时，再有效运行一 tick 即达上限 `18874368000L`。停轴后手持一枚重型轴承在正面右键，确认消耗一枚、寿命恢复且批次/物料不变。若只验证维修交互，可先停轴，再把 `WearUnits` 直接设为 `18874368000L`；不需要新增调试命令。

**技能实际使用：** `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md` 用于 NeoForge DeferredRegister、能力/菜单、Create 动力与专用服务端边界；`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md` 用于定向 JUnit 和原生加载/客户端门区分。均按本仓库 MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 核对，未采用其他版本示例的技术升级。

## 终审两项 P2 限定整改（2026-10-02）

1. `FuelProcessingContent` 的料浆仍/流动精灵改为 `create_nuclear_industry:fluid/uranium_slurry_{still,flow}`。新增 `assets/minecraft/atlases/blocks.json` 两条 `single` 来源，将这两个 ID 纳入方块图集。格式已与锁定 Create 6.0.10-280 JAR 内的同名 atlas 文件核对；PowerShell 解析新 atlas 并逐项解析到真实 `textures/fluid/*.png`，两项均存在。未复制或修改 PNG。
2. `CentrifugeBlockEntity.tick()` 在服务端观察旧转速、旧稳定 tick 和旧停机原因；仅在转速数值/方向变化、稳定窗达到 20 tick 或停机原因变化时额外 `sendData()`。因此 0→有动力会同步“稳定中”，任意变速/反转会同步重置稳定窗，空机跨过 20 tick 会同步“等待料浆”；连续稳定等待的中间 tick 不额外发包。原加工、库存与维修路径的同步保持原样。此次未新增与实现镜像的 JUnit；既有 7/7 事务/状态证据复用，客户端护目镜结果留本批人工门。

**本次真实检查：** `./gradlew.bat assemble --max-workers=1 --console=plain` 退出 0（`compileJava`、`processResources`、`jar` 均执行）；JAR 内核对存在 `assets/minecraft/atlases/blocks.json` 及两张原有 `textures/fluid/uranium_slurry_*.png`，JAR 中 atlas 内容与 Java 返回 ID 一致。针对两处 Java 路径的 `git diff --check` 无错误。新 JAR SHA-256 为 `82DBDF2D04A0A49B2FFD213C2A4FF31ABB42F5B368C6CC00965EA7719425EEB9`。未重跑旧 7 项 JUnit、全量 GameTest、专服或客户端；无 JEI 专服旧证据与两项整改无交叉，按治理 5.1 复用。
