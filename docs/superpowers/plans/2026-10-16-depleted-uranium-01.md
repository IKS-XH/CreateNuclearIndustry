# DU-EQUIPMENT-01：贫化铀材料与钻石装备升级

2026-10-16。当前结果与未闭门统一维护于[STATUS](../../STATUS.md)，本卡用于限定本批执行和验收范围。

## 已批准合同

用户要求为现有贫化铀粉补齐锭、块及对应合成方式，并用于装备制造；随后明确改为下界合金式的钻石装备升级，并选择采用整组推荐规则：

- 一份贫化铀粉经熔炉烧炼或高炉熔炼得到一个贫化铀锭；9锭有序合成一个储存块，块无序拆回9锭。
- 新增头盔、胸甲、护腿、靴子、剑、镐、斧、锹、锄，共九件装备。每件通过原生锻造台消耗一个下界合金升级模板、对应钻石装备和一个贫化铀锭得到，不提供从锭直接制造的装备配方。
- 装备属性、耐久、附魔能力和掉落物耐火性与对应原版下界合金装备相同；修复材料改为贫化铀锭。升级保留附魔、自定义名称和已有损伤，不免费恢复耐久。
- 不增加辐射、穿甲、速度惩罚等额外效果；不改变现有燃料配方、贫化铀粉产出量、尾矿或核废料。核废料保持当前封装与干式贮存方案，必要性和处理玩法留待辐射/事故阶段另行讨论。

这是既有材料注册、资源和原版锻造配方的有限扩展。装备采用原版类型/锻造机制，不建立新的装备系统；常规熔炼时长与经验沿用现有金属粉惯例。储存块采用普通铁储存块的物理性质和采掘规则。用户追加明确要求本批不做外观，专属美术交另一个对话；本批只完整引用原版铁锭、铁块与下界合金装备模型/贴图，不绘制、改色或生成新贴图，不改已有美术资源。

## 执行边界

- 唯一执行者由主逻辑PM派发；执行者只读Git，不暂存、提交、切分支、合并或指挥其他执行者。核心文档和Git写操作归PM。
- 候选：`E:/MyMC/NewMod/Create_NuclearIndustry-depleted-uranium`，分支`codex/depleted-uranium-equipment`，基准`060f3f65fa7dc46b3784001587eca1783f0b1009`。main的无关dirty与已删除的正式JAR工作副本保持原状。
- 用户明确约定工作树位于主工程同级；桌面工具当前默认位置为C盘管理目录且不接受路径参数，因此按用户目录要求使用原生Git创建同级树。主世界最近日志确认所有维度保存完成，当前无Java游戏进程；整份`main/run/saves`已复制，27文件、13337924 bytes、SHA256逐项一致，清单在候选`build/reports/depleted-uranium-20261016/saved-world-sha256.json`。配置亦完整复制。禁止启动、修改或回写用户世界，真实自动化测试使用独立世界目录。
- 必读：根AGENTS、治理5.1、STATUS、本卡、当前注册与数据约定。
- 实际技能入口：`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`、`minecraft-resource-pack/SKILL.md`；实现前读取`C:/Users/IKSXH/.codex/plugins/cache/openai-curated-remote/superpowers/6.4.2/skills/test-driven-development/SKILL.md`，完成前读取同根`verification-before-completion/SKILL.md`。记录实际使用；用户治理的验证范围优先于通用全量测试建议。
- 锁定MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，实际核Gradle文件，不升级依赖。

## 允许写集

以下路径均相对候选根；未列文件只读，发现必要缺口先报PM，不自行扩大：

1. `src/main/java/com/iksxh/create_nuclear_industry/CreateNuclearIndustry.java`，仅接入本批注册。
2. `src/main/java/com/iksxh/create_nuclear_industry/content/ModCreativeTabs.java`，仅加入本批物品。
3. 新增`src/main/java/com/iksxh/create_nuclear_industry/content/DepletedUraniumContent.java`及`src/main/java/com/iksxh/create_nuclear_industry/equipment/DepletedUranium*.java`，集中注册/材料，不改现有贫化粉身份。
4. `src/main/resources/data/create_nuclear_industry/recipe/smelting/depleted_uranium_ingot_from_dust.json`、`blasting/depleted_uranium_ingot_from_dust.json`、`crafting/depleted_uranium_block.json`、`crafting/depleted_uranium_ingots_from_block.json`及`smithing/depleted_uranium_*.json`。
5. 新增本批命名的采掘/材料/装备标签：`data/c/tags/item/ingots/depleted_uranium.json`、`data/c/tags/item/storage_blocks/depleted_uranium.json`、`data/c/tags/block/storage_blocks/depleted_uranium.json`，及现有对应`ingots.json`/`storage_blocks.json`聚合标签仅追加本批引用；`data/minecraft/tags/block/mineable/pickaxe.json`和`needs_stone_tool.json`仅追加本批储存块；`data/minecraft/tags/item/`下原版工具、护甲、可附魔及可饰纹标签仅追加本批对应装备。所有标签位于`src/main/resources/`，不得改其他物品归属。
6. `src/main/resources/data/create_nuclear_industry/loot_table/blocks/depleted_uranium_block.json`；本批`assets/create_nuclear_industry/blockstates/depleted_uranium_block.json`、`models/block/depleted_uranium_block.json`、`models/item/depleted_uranium_*.json`；现有`lang/zh_cn.json`和`lang/en_us.json`仅追加本批名称。
7. 新增`src/test/java/com/iksxh/create_nuclear_industry/equipment/DepletedUraniumDataContractTest.java`、`src/main/java/com/iksxh/create_nuclear_industry/gametest/DepletedUraniumGameTests.java`，必要的`src/main/resources/data/cni_depleted_uranium/structure/depleted_uranium_empty.nbt`；`build.gradle`仅新增本批GameTest域启动和发布排除，不改变正式依赖、旧域、版本或任务行为。
8. `build/reports/depleted-uranium-20261016/`作为诊断、定向测试和交付报告输出；必要Gradle命令可正常生成候选`build/`编译/测试/JAR产物，但执行者不得暂存或提交它们。禁止修改已有存档清单、用户世界、main正式包、核心文档或其他报告。

## 实现与验证要求

优先复用原版Tier/ArmorMaterial数据与原生smithing_transform，不复制自定义升级事务。注册只管理身份；装备物品的客户端外观不得引入专服类加载依赖。新增公共入口和非显然标签/组件继承边界使用准确中文注释。保留普通附魔、饰纹和铁砧行为；不能因未加原版标签导致装备不能正常附魔/使用。

必要检查集中一次：

- 针对新材料数据的JUnit：13条配方（2熔炼、2存取、9锻造）数量/材料守恒、模板与对应钻石底材、资源和语言完整，无装备直接合成、无核废料/已有配方变化。只测试业务合同，不逐字锁源码写法。
- 一个独立真实GameTest域，验证注册与配方加载、九件属性/修复/耐火/适当标签，以及原生锻造对带附魔、命名、损伤底材的继承和投入限制；有实际失败才扩展或重跑。复用现有模板和启动方式，明确断言通过、进程退出、保存成功各自证据。
- 定向JUnit、compileJava/processResources及assemble可按依赖合并执行；不机械跑全部反应堆GameTest或全build。旧测试因新增合法内容发生冲突时保留失败证据并报PM，不删除/放宽断言以求通过。
- 发布JAR不带本批GameTest类/模板；此阶段构建只用于候选验证，不替换main正式包。不打开用户世界，不研究旧版本迁移。

执行者交`build/reports/depleted-uranium-20261016/REPORT.md`，列精确文件、属性/配方、使用技能、命令/实际结果、失败/修复记录、资源引用、未覆盖项和简短手测步骤。完成后由PM安排一次组合窄审，整改只复查受影响点。

人工门：在候选中检查粉炼锭、块存取、锻造台升级/材料消耗与附魔命名损伤保留、铁砧修复，以及四件穿戴/五件手持显示。静态/真实服务端通过不称客户端验收；用户通过后再安排已授权合入与正式制品身份核验。
