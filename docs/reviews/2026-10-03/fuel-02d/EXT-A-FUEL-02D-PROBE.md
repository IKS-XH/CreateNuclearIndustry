# EXT-A-FUEL-02D-PROBE 只读核对

日期：2026-10-03；基线：`5afc572`；核对目录：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`。候选工作树无未提交差异；主目录 `.vscode/launch.json` 保持原样（主目录只读状态检查见 `M .vscode/launch.json`）。未运行测试/Gradle/客户端，未读写 run 存档及进程。

技能：已读取并应用 `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`；按仓库锁定版本核对，不采用技能中新版范例。版本：Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6（`gradle.properties:6-13`）。

## 已确认事实

1. `fresh_fuel_assembly` 是已注册正式物品，单件堆叠，耐久上限 `3*60*60*20`（`src/main/java/com/iksxh/create_nuclear_industry/content/ModItems.java:17-29`；ID见 `content/P1ContentIds.java:26,54`）。新建 `new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get())` 初始损伤为 0，故可作为满耐久新产物。`FuelAssemblyItemCodec.isFreshFuel` 只接受该正式 ID；`isValidStoredFuel` 还要求 count=1，并对新组件核验耐久范围（`reactor/FuelAssemblyItemCodec.java:17-48,63-72`）。`writeFreshFuel(state)` 是从既有列耐久状态重建的适配器，会按传入 `damage` 写损伤并拒绝缺失/耗尽状态，不宜作为“全新组件”工厂（同文件 `:75-85`）。`copyWithDamage` 则复制原栈仅改耐久（`:114-122`）。
2. 反应堆换料端口只保存一个完整 `ItemStack`，服务端校验正式物品、数量及耐久，NBT 通过 `ItemStack.saveOptional/parseOptional` 保留完整栈（`blockentity/ReactorPortBlockEntity.java:81-104,487-514`）。列状态只投影燃料耐久，不由生产机写温度/完整度或反应堆第二份组件；其唯一状态所有权见 `content-catalog.md:121`、`reactor/FuelAssemblyItemCodec.java:88-102`。装配机产物应为新单件栈，不能从旧/受损组件复制，也不能把旧组件作为输入或用输出槽回灌。
3. 文档与实现存在一处待PM判定的不一致：`docs/content-catalog.md:121` 写有自定义 `fuel_assembly` 数据组件字段；本次对候选 `src/main/java` 全文检索只发现正式物品 ID、原版 ItemStack 耐久和整栈 NBT 持久化，未发现该自定义组件的类型/codec/字段实现。当前可证的装配合同是正式 ID + 原版耐久；若自定义组件仍是有效合同，应先由PM澄清，不要由执行者臆造字段。
4. 新设备沿用单格 BE、服务端独占库存/进度、客户端只接收展示、护目镜显示、扳手拆放及原生物品能力是现有可行样式。`FuelSinteringBlockEntity` 示例为库存持有者、`IHaveGoggleInformation`、按面返回 `IItemHandler`，输入/输出有严格过滤（`production/FuelSinteringBlockEntity.java:22-23,72-98,139-179`）；方块支持手持物品右键输入与空手取料（`production/FuelSinteringBlock.java:132-176`）。Centrifuge 示例有扳手操作与空手/物流输出处理（`production/CentrifugeBlock.java:219-222,260-300`）。这支持无独立GUI、物品本身手工插入、空手取出、护目镜读状态的方案；Create 值框由离心机明确未用，过滤留给外置黄铜漏斗（`CentrifugeBlockEntity.java:37-40`）。
5. Create 6.0.10-280 锁定源码 `com/simibubi/create/content/kinetics/mechanicalArm/ArmInteractionPoint.java:97-137` 的默认交互点通过 `Capabilities.ItemHandler.BLOCK`、`Direction.UP` 取得普通能力，默认 `insert` 使用 `ItemHandlerHelper` 并传递 simulate，`extract` 委托指定槽位；因此普通能力目标可供 Create 原生机械臂取放。仓库也有实际 Create 机械臂方块实体和 `ArmInteractionPoint.create` 的测试适配（`gametest/P1Refuel03GameTests.java:170-207`、`p0probe/gametest/P0ProbeGameTests.java:131-168`）。测试配适证明 API/事务调用可行，不等同于本装配台已有真实自动化验收。
6. 换料端口的 `FuelRefuelingArmInteractionPoint` 特意不暴露 ItemHandler，而是直连带模拟/提交语义的换料事务（`reactor/FuelRefuelingArmInteractionPoint.java:18-25,77-123`）；`recipes.md:380` 将普通漏斗/通用物流明确排除在**反应堆在线换料端口**之外。没有找到把这一限制施加到燃料制造产物/屏蔽装配台的合同。故新装配台采用通用 ItemHandler + 普通 Create 机械臂/物流在技术上与该端口特例不冲突；新鲜组件成品作为普通货物搬运，不因此开放反应堆端口的普通能力。机械臂方向固定向上取目标能力，建议其上面能力提供固定输入槽和输出槽；水平侧另供漏斗/玩家交互。
7. 4铅板、3钢板、1 `create:deployer`、1辐射传感器恰好占满工作台 3×3，可表达为原生 shaped 配方，但具体布局不是已批准合同。铅板/钢板已有 Create 压片配方（`src/main/resources/data/create_nuclear_industry/recipe/pressing/lead_plate.json:1-12`、`.../steel_plate.json:1-9`）；辐射传感器现有序列装配由铅板半成品、工业传感器、Create电子管经两次部署和压制产出（`recipe/sequenced_assembly/radiation_sensor.json:1-12`），前置工业传感器也有序列装配（同目录 `industrial_sensor.json:1-13`）；部署器为 Create 原生物品。因而这些材料与加工类型在现有路线中可达；本次未运行生存客户端重新核验每个供应链步骤。现配方表也有燃料烧结炉用工作台 3×3 的同类制造先例（`recipes.md:356`）。
8. 底部垂直动力轴/旋转外观可沿用 `CentrifugeBlock` 的 `HorizontalKineticBlock`、`hasShaftTowards(...DOWN)`，以及 `CentrifugeBlockEntity extends KineticBlockEntity` 和 `calculateStressApplied`（`production/CentrifugeBlock.java:47-70`、`production/CentrifugeBlockEntity.java:24-58`）。这证明接线形态可行；离心机每 RPM 8 SU 是它自己的合同，不授权搬用或确认新设备应力。护目镜可用离心机 tooltip 模式参考（`CentrifugeBlockEntity.java:236-246`）。

## 建议与待验证假设（不构成参数批准）

- 可把内部库存建模为四个固定材料输入槽＋一个新组件输出槽；每个输入槽只接纳其对应正式物品，输出只允许抽取，拒绝外部插入。侧向 ItemHandler 与方块手工交互共用同一服务端库存。手持四种合法输入右键进入对应槽；空手对输出侧取走产物；护目镜逐槽显示数量、进度和停机原因。Create 手臂经顶部 ItemHandler 自动送入/取出；外置漏斗仍可使用各面 capability。不要暴露任意 5 槽无过滤 handler，防止错物料、输出回灌和未定义槽索引行为。64件输入缓存、单件输出容量，以及四种缓存的顶部/侧面分配方式是待用户确认或实现测试的接口细节。
- 数量 `8芯块＋4包壳＋2焊料＋1格架`、转速下限/上限、加工时间、正反转、SU/RPM、断电/满产物如何处理、欠料是否清空进度、四种缓存面向、是否实际交付机械臂自动化、Ponder/JEI展现细节均未批准；`docs/recipes.md:294,296` 明确数量及运行参数另卡确认。上面的接口形态只代表工程上可行的方向。
- 至少定向覆盖：正确/错误四种输入槽与模拟插入只读；批次完成时四项扣料与单件正式新组件原子提交；成品满时不丢输入/进度；断动力/恢复及存档重载保持合同指定状态；输出禁止插入、错误 ID/耐久/count 被拒；Create Arm 真方块实体对顶部能力的模拟、提交/抽取和拒绝回滚。复用 `p0_probe_empty` 空结构、单格库存能力和 Create Arm fixture（`ExtensionFuelSinteringGameTests.java:22-30`、`P0ProbeGameTests.java:131-168`、`P1Refuel03GameTests.java:170-207`）；客户端手工插取、侧面与护目镜显示仍由必要人工门确认。
- 已核实可按 namespace 定向：`build.gradle:50-55` 的 `-PgameTestNamespace=...` 传入 NeoForge `enabledGameTestNamespaces`；未来可给新类单独 `@GameTestHolder("create_nuclear_industry_02d")`，再用 `-PgameTestNamespace=create_nuclear_industry_02d runGameTestServer`，避免扫其他域。当前没有 02D 类，因此本次未运行。

## 对方案草案的补充核对

方案草案 `docs/superpowers/plans/2026-10-03-shielded-assembly-station-proposal.md:50-60` 提出顶面输入与四侧输出都可被 Create 机械臂直接选取。锁定 Create 6.0.10-280 的默认 `ArmInteractionPoint.getHandler` 将能力查询方向固定为 `Direction.UP`（来源 JAR `com/simibubi/create/content/kinetics/mechanicalArm/ArmInteractionPoint.java:97-112`）；`insert/extract` 虽可映射能力槽，但没有按操作者点中的侧面切换能力方向。因此，普通默认点不能兑现“四侧各一个机械臂目标点”。

推荐草案保持唯一顶面 ItemHandler：Create 默认机械臂只承诺把料送入顶面能力；侧面产物由漏斗/管道取出并送到外部置物台，再由另一条已支持的机械臂路线转运到反应堆换料端口。反应堆端口继续走既有专用 ArmInteractionPoint。若要把装配机侧面直接选为机械臂输出端，需单独设计注册的自定义点和选点/方向合同，并用锁定版本真实机械臂验证；本次没有验证“同一方块位置按四个面重复注册/去重”的 UI 语义，不应承诺该行为。
