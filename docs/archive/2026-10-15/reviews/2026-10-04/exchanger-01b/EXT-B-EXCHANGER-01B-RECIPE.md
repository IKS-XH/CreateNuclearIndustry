# EXT-B-EXCHANGER-01B RECIPE

## 范围

基线 HEAD：`72b453fe522e2c39337ce869d46ad58b06c3e850`。实现只修改任务卡允许的整机配方 JSON 与 `ExtensionHeatExchangerCraftingGameTests.java`；本次追加执行报告及同名证据目录。未执行任何 Git 写操作。

已实际读取并应用 Minecraft Modding 与 Minecraft Testing 技能，以及 `AGENTS.md`、治理协议第1/4/5.1节和活动任务卡。技术栈仍为 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82。

## 实现

`heat_exchanger/nuclear_heat_exchanger.json` 保留原 ID `create_nuclear_industry:heat_exchanger/nuclear_heat_exchanger`，配方类型改为 `minecraft:crafting_shaped`，布局为 `CCC / SHS / SSS`；C 使用 `c:plates/copper`，H 使用 `create_nuclear_industry:nuclear_heat_exchange_bundles`，S 使用 `c:plates/steel`，结果仍为一台 `create_nuclear_industry:nuclear_heat_exchanger`。旧21格入口已移除。钢管坯、强化钢板和管束配方未改。

对应 GameTest 从旧21格机械合成测试改为 `nuclearHeatExchangerMatchesThreeByThreeWorkbenchRecipe`。它检查已加载配方为原生 `CraftingRecipe`、类型是 `RecipeType.CRAFTING` 且不是 `MechanicalCraftingRecipe`，再复用已有帮助方法构造真实3×3 `CraftingInput`，调用原生 `matches` 和 `assemble`，断言确切物品与数量1。其他材料用例未改。

## 验证

Java 版本为 21.0.7。ART 定稿后，按通知只运行一次既有 Gradle 验证：

```text
./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_heat_exchanger -PgameTestDirectory=build/gametest-heat-exchanger-01b assemble --console=plain
```

结果：GameTest 启用了 `create_nuclear_industry_heat_exchanger` 命名空间，运行10项（`heat_liveness` 批次1项，默认批次9项），10项必需测试全部通过；随后的 `assemble` 成功。此命名空间批次包含本次新增的工作台匹配测试以及已存在的设备/材料测试。没有单类/单方法现成筛选器，因此按任务卡获准运行完整的10项命名空间批次；没有运行 JUnit、全量回归或第二次构建。

JAR `build/libs/create_nuclear_industry-0.1.0.jar` 的资源核对通过：整机配方、blockstate、5个模型文件（含物品模型）共7项关键 JSON 均与工作区字节一致；10张换热器纹理 PNG 均与工作区字节一致；5个模型和 blockstate 声明的模型引用都存在。JAR 中的配方确认仍为 `CCC / SHS / SSS` 并产1台。SHA-256：`961F5005D60CBDD4BD3DEF119DA5AB79D84B8B7837FADCA7A46666548C794F65`。

完整 Gradle/GameTest 输出保存在本目录 `validation.log`；资源字节与引用检查结果见 `static-check.txt`。启动日志有 `server.properties` 不存在的初始读取错误和第三方 Mixin 警告，但服务端随后正常启动，10项必需 GameTest 全通过且构建成功。此次复用了未改动的其余命名空间测试以及01A已有的运行逻辑/半成品配方证据；没有重跑JUnit或旧全量测试。人工客户端检查仍按任务卡保留，尚未在本任务中验收。
