# DU-EQUIPMENT-01 执行报告

2026-10-16，唯一执行者。候选 `E:/MyMC/NewMod/Create_NuclearIndustry-depleted-uranium`，分支 `codex/depleted-uranium-equipment`，只读核对基准 `060f3f65fa7dc46b3784001587eca1783f0b1009`。未进行Git写操作，未改治理、STATUS或任务卡，未启动或修改用户世界/用户配置，未触碰main。此报告是执行证据，不宣布客户端验收或发布。

## 实现

- 保留现有 `FuelProcessingContent.DEPLETED_URANIUM_DUST` 身份；新增 `depleted_uranium_ingot`、`depleted_uranium_block` 及对应方块物品、四护甲和剑/镐/斧/锹/锄。
- 熔炉1粉→1锭，200 tick、0.1 XP；高炉1粉→1锭，100 tick、0.1 XP，沿已有钢粉炼锭惯例。3×3九锭有序合块，单块无序拆9锭。
- 九装备只有 `minecraft:smithing_transform` 配方：原版下界合金升级模板+对应钻石装备+1本批锭→1装备。原生锻造保留名称、附魔、损伤、铁砧惩罚、护甲饰纹，没有自定义事务或免费修复。
- 工具Tier委托 `Tiers.NETHERITE`，只替换修复Ingredient；护甲直接使用 `ArmorMaterials.NETHERITE`，薄 `ArmorItem` 子类只接受本批锭修复，绝不回退super而接受下界合金锭。原版攻击属性构造参数保持对应装备一致，护甲耐久读取对应原版物品组件。装备均 `fireResistant`。
- 块完整复制普通铁块物理属性，加入镐采掘与石镐等级标签；补锭/储存块common标签及聚合入口。
- 九件加入原版工具/护甲槽位基础标签，原版可附魔聚合标签自然继承。四护甲加入 `trimmable_armor`；镐追加 `cluster_max_harvestables`，保持紫水晶簇采收规则，已获PM确认属于卡内原版工具标签。
- 新注册加入模组公共注册入口和创造页。未改核废料、尾矿、燃料配方、粉产出、旧配方、热工/反应堆逻辑，无辐射/特殊战斗效果。

## 原版占位外观与API证据

- 锭模型父级 `minecraft:item/iron_ingot`；块父级 `minecraft:block/iron_block`；九装备父级为对应 `minecraft:item/netherite_*`。
- 本机1.21.1实际 `BlockModel.getOverrides()` 不继承parent的overrides，故四护甲JSON直接复制原版10个 `trim_type` override，全部仍引用原版饰纹模型。已与本机原版client JAR逐项核对四组各10项且所有引用存在，无PNG新增/改色/生成。
- 护甲保留原版下界合金ArmorMaterial的穿戴layer和trim材质身份，不引入任何客户端类或自定义渲染器。
- 实际读取 `C:/Users/IKSXH/.gradle/caches/neoformruntime/intermediate_results/sourcesWithNeoForge_f5a61d362941bcd4b74a86c95e0d21126a1245aa_output.zip` 中 Tier/Tiers、ArmorMaterial/ArmorMaterials/ArmorItem、Items、SmithingTransformRecipe、SmithingMenu、RecipeCraftingHolder、GameTestHelper、BlockModel、ItemProperties 等源码；原版资源来自 `C:/Users/IKSXH/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar`。
- `gradle.properties` 与实际运行日志确认：MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，依赖未变。

## 实际使用技能

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：核平台/版本，采用DeferredRegister公共注册和原版物品类型。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：JUnit数据合同与独立真实GameTest域、原生菜单和加工行为验证。
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：使用1.21.1物品/方块模型、blockstate、loot与语言资源约定，直接核原版资源引用。
- `C:/Users/IKSXH/.codex/skills/minecraft-ci-release/SKILL.md`：候选JAR来源/摘要与测试内容排除核对；不改版本、依赖、发布方式，不部署发布。
- `.../superpowers/6.4.2/skills/test-driven-development/SKILL.md` 及 `writing-good-tests.md`：先写数据合同并观察缺失内容导致4项红灯，再实现。
- `.../superpowers/6.4.2/skills/systematic-debugging/SKILL.md`：对首轮GameTest夹具登录异常先读完整调用链、比较项目既有FakePlayer用法，再只改测试夹具。
- `.../superpowers/6.4.2/skills/verification-before-completion/SKILL.md`：核实际命令退出、断言和保存记录后汇报。用户治理5.1和活动卡的定向验证优先于技能全量示例，未跑全build/全部反应堆测试。

## 命令、失败与验证结果

所有命令工作目录均为候选根，无clean、强制重跑、用户存档启动。

1. `./gradlew.bat test --tests '*DepletedUraniumDataContractTest' --console=plain`：预期红灯，4项因新配方/模型缺失而失败，日志 `junit-red.log`。没有其他测试失败被删改或放宽。
2. `./gradlew.bat test --tests '*DepletedUraniumDataContractTest' assemble --console=plain`：exit0，22秒，4/4 JUnit、0失败0错误，日志 `verification.log` 和保存副本 `junit-green.xml`。覆盖13条配方输入/输出数量、9模板/对应钻石底材、无直接工作台装备制造、双语、占位模型、loot入口。编译保留现有FluidType弃用提示，初版GT mock玩家API弃用提示随后在夹具修正中消除，未改生产依赖。
3. `./gradlew.bat runGameTestServer -PgameTestNamespace=cni_depleted_uranium -PgameTestDirectory=build/reports/depleted-uranium-20261016/gametest-run --console=plain`：exit1，首轮未完成断言。`GameTestHelper.makeMockServerPlayerInLevel()` 内部调用 `PlayerList.placeNewPlayer` 模拟登录；Create登录同步向未握手连接发 `create:sync_edge_group`，随后tick广播 `create:clientbound_chain_conveyor` 抛异常。保留 `gametest-initial-failure.log`、`gametest-initial-failure-exit.txt`、原 `gametest.log` 及 `gametest-run/crash-reports/`。这是测试夹具失败，不以此宣称生产功能失败或断言通过。
4. 仅把测试玩家改成项目已有 `FakePlayerFactory.getMinecraft(level)`，它提供原生菜单所需ServerPlayer上下文，不执行模拟登录；不改生产代码或Create。然后 `./gradlew.bat runGameTestServer -PgameTestNamespace=cni_depleted_uranium -PgameTestDirectory=build/reports/depleted-uranium-20261016/gametest-run-r1 --console=plain`：exit0，22秒，3/3 required测试通过，见 `gametest-r1.log` / `gametest-r1-exit.txt`。
   - 九件与对应下界合金逐项对比耐久、有效属性修饰、附魔能力、可附魔性、1堆叠、火伤免疫/普通伤害、全部实际原版item tags及代表性方块采掘速度/掉落等级；DU锭修复有效，下界合金锭/铁锭/DU粉无效；九件原生铁砧实际产生修复结果。护甲材质引用身份与可饰纹标签通过，块硬度/采掘标签和common材料标签通过。
   - 九条原生SmithingTransformRecipe和SmithingMenu：带Unbreaking III、中文名称、123既有损伤、7铁砧惩罚，护甲额外带Sentry/Redstone饰纹；升级全部保留，输出一件。空/饰纹模板、铁底材、重复DU底材、粉/下界合金锭/空添加物均拒绝；实际结果槽onTake恰好消耗模板、底材、锭各1件。
   - 13条配方加载；真实熔炉及高炉各投入2粉、1煤并运行410 tick，分别恰好产2锭、原料为空；原生crafting解析并组装9锭→1块→9锭。
   - 断言边界：`All 3 required tests passed`。进程边界：Gradle正常exit0。保存边界：三个维度分别 `All chunks are saved`，随后 `All dimensions are saved` 和 `Game test server shutting down`，没有Saving worlds停滞或手动终止。
5. `./gradlew.bat assemble --console=plain`：夹具修正后增量确认up-to-date；静态ZIP核对曾发现仅排除模板会残留两个空测试域目录，遂把发布排除收窄完整目标为 `data/cni_depleted_uranium/**`（无测试/生产行为变化）。最终再执行assemble：exit0，1秒，1个jar任务执行，其余3项up-to-date，见 `assemble-final.log`。不因打包过滤改动重复业务JUnit/GameTest。
6. `& build/reports/depleted-uranium-20261016/verify-artifact.ps1`：exit0，13新配方、3生产类型、本批GameTest类/整个测试资源域均完全排除；九个父模型、四套各十种原版饰纹引用逐项核对。结果 `static-and-artifact.json`。
7. `git diff --check -- . ':!logs/debug.log' ':!logs/latest.log'`：exit0。未排除自动日志的全diff检查仅报告测试框架日志中的尾空格，未手动编辑/清理它们。

## 制品与范围
- 候选JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，3363957 bytes，SHA256 `53b0b554ad68f69f8bab384b1012599aacc3627469d592147ed970b4421c7189`。这是候选验证包，不替换main正式包或作为已发布版本。
- 精确生产/测试写集共57个文件，见下列清单及 `changed-source-files.txt`。已有配方tracked差异为空；核心文档、用户世界和配置不在本执行者写集中。PM已另行更新候选核心文档；这些PM写入与下列57项执行写集分开归属。
- Gradle会自动改变候选tracked `build/libs/create_nuclear_industry-0.1.0.jar`、`logs/debug.log`、`logs/latest.log`；已向PM报告，未自行Git恢复、暂存或提交。任务卡副本是PM提供的untracked只读文件，不是本执行者写入。
- 既有 `saved-world-sha256.json` 未修改，未重新扫描/写入用户存档；真实测试只使用本报告目录下两个独立测试世界。

### 精确源码/资源/测试写集

- `build.gradle`
- `src/main/java/com/iksxh/create_nuclear_industry/content/DepletedUraniumContent.java`
- `src/main/java/com/iksxh/create_nuclear_industry/content/ModCreativeTabs.java`
- `src/main/java/com/iksxh/create_nuclear_industry/CreateNuclearIndustry.java`
- `src/main/java/com/iksxh/create_nuclear_industry/equipment/DepletedUraniumArmorItem.java`
- `src/main/java/com/iksxh/create_nuclear_industry/equipment/DepletedUraniumTier.java`
- `src/main/java/com/iksxh/create_nuclear_industry/gametest/DepletedUraniumGameTests.java`
- `src/main/resources/assets/create_nuclear_industry/blockstates/depleted_uranium_block.json`
- `src/main/resources/assets/create_nuclear_industry/lang/en_us.json`
- `src/main/resources/assets/create_nuclear_industry/lang/zh_cn.json`
- `src/main/resources/assets/create_nuclear_industry/models/block/depleted_uranium_block.json`
- `src/main/resources/assets/create_nuclear_industry/models/item/depleted_uranium_axe.json`
- `src/main/resources/assets/create_nuclear_industry/models/item/depleted_uranium_block.json`
- `src/main/resources/assets/create_nuclear_industry/models/item/depleted_uranium_boots.json`
- `src/main/resources/assets/create_nuclear_industry/models/item/depleted_uranium_chestplate.json`
- `src/main/resources/assets/create_nuclear_industry/models/item/depleted_uranium_helmet.json`
- `src/main/resources/assets/create_nuclear_industry/models/item/depleted_uranium_hoe.json`
- `src/main/resources/assets/create_nuclear_industry/models/item/depleted_uranium_ingot.json`
- `src/main/resources/assets/create_nuclear_industry/models/item/depleted_uranium_leggings.json`
- `src/main/resources/assets/create_nuclear_industry/models/item/depleted_uranium_pickaxe.json`
- `src/main/resources/assets/create_nuclear_industry/models/item/depleted_uranium_shovel.json`
- `src/main/resources/assets/create_nuclear_industry/models/item/depleted_uranium_sword.json`
- `src/main/resources/data/c/tags/block/storage_blocks.json`
- `src/main/resources/data/c/tags/block/storage_blocks/depleted_uranium.json`
- `src/main/resources/data/c/tags/item/ingots.json`
- `src/main/resources/data/c/tags/item/ingots/depleted_uranium.json`
- `src/main/resources/data/c/tags/item/storage_blocks.json`
- `src/main/resources/data/c/tags/item/storage_blocks/depleted_uranium.json`
- `src/main/resources/data/cni_depleted_uranium/structure/depleted_uranium_empty.nbt`
- `src/main/resources/data/create_nuclear_industry/loot_table/blocks/depleted_uranium_block.json`
- `src/main/resources/data/create_nuclear_industry/recipe/blasting/depleted_uranium_ingot_from_dust.json`
- `src/main/resources/data/create_nuclear_industry/recipe/crafting/depleted_uranium_block.json`
- `src/main/resources/data/create_nuclear_industry/recipe/crafting/depleted_uranium_ingots_from_block.json`
- `src/main/resources/data/create_nuclear_industry/recipe/smelting/depleted_uranium_ingot_from_dust.json`
- `src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_axe.json`
- `src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_boots.json`
- `src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_chestplate.json`
- `src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_helmet.json`
- `src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_hoe.json`
- `src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_leggings.json`
- `src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_pickaxe.json`
- `src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_shovel.json`
- `src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_sword.json`
- `src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json`
- `src/main/resources/data/minecraft/tags/block/needs_stone_tool.json`
- `src/main/resources/data/minecraft/tags/item/axes.json`
- `src/main/resources/data/minecraft/tags/item/chest_armor.json`
- `src/main/resources/data/minecraft/tags/item/cluster_max_harvestables.json`
- `src/main/resources/data/minecraft/tags/item/foot_armor.json`
- `src/main/resources/data/minecraft/tags/item/head_armor.json`
- `src/main/resources/data/minecraft/tags/item/hoes.json`
- `src/main/resources/data/minecraft/tags/item/leg_armor.json`
- `src/main/resources/data/minecraft/tags/item/pickaxes.json`
- `src/main/resources/data/minecraft/tags/item/shovels.json`
- `src/main/resources/data/minecraft/tags/item/swords.json`
- `src/main/resources/data/minecraft/tags/item/trimmable_armor.json`
- `src/test/java/com/iksxh/create_nuclear_industry/equipment/DepletedUraniumDataContractTest.java`
## 未覆盖与人工门

- 尚未启动候选客户端，无Minecraft视觉或玩家操作验收；静态模型引用和服务端ArmorMaterial/trim组件通过不替代四护甲穿戴、五工具手持、背包饰纹、附魔光效与资源实际加载观察。
- 未跑完整JUnit/全部反应堆GameTest、多人/整机测试、旧存档迁移；本批仅原生材料配方和装备注册增量，不触发这些扩大范围条件。
- 正常存档证据来自独立测试世界的保存退出；用户世界按卡原样保留，未以自动化夹具替代用户当前世界验收。

建议候选客户端在另建临时世界一次组合手测：

1. JEI/创造页查看贫化粉、锭、块和九件装备；确认锭/块为原版铁占位、装备为下界合金占位，无缺失贴图。
2. 熔炉/高炉分别粉炼锭，工作台九锭有序合块并无序拆回，观察数量与经验。
3. 用带名称/附魔/既有损伤的钻石装备配下界合金升级模板和1锭，锻造取出并核三格各消耗1件、输出一件、名称/附魔/损伤保留；对护甲先饰纹再升级并查看保留。
4. 用贫化铀锭在铁砧修复，确认下界合金锭和粉不作为材料；检查附魔台、饰纹锻造。
5. 穿戴四件、手持五件，观察外观/饰纹/附魔光效；试采铁储存块和紫水晶簇，确认原版工具行为；临时世界正常保存退出。

执行交付到此，等待PM组合窄审及用户人工门。未申请卡外写集、未更改玩法合同。