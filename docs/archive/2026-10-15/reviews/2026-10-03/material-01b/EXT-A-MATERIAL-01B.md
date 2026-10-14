# EXT-A-MATERIAL-01B 实施报告

候选目录：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`
实施基线：`86c92cd 修复：裁除屏蔽装配台重叠面并消除端盖深度冲突`（`codex/ore-acquisition`）
环境：Minecraft 1.21.1、Java 21.0.7、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82、Flywheel 1.0.6。

## 实施内容

- 新增普通可堆叠物品 `lapis_dust`，加入主创造页及中英文名“青金石粉 / Lapis Dust”。模型使用已认可的16×16 PNG；`c:dusts/lapis` 和父 `c:dusts` 使用 `replace:false` 追加。
- 新增 Create 粉碎配方：`c:gems/lapis` → 1 青金石粉，`processing_time=100`。新增无热 Create 搅拌配方：青金石粉、红石粉、荧石粉各1份与1000mB水 → 1000mB现有冷态 `compound_coolant`，`processing_time=100`。
- 保留 Create 青金石磨石蓝色染料路线；未新增流体、桶、机器、GUI或设备算法。仅在 `P1DataContractTest` 中为精确的已批准02E组件装配配方和本批冷态配方添加路径级放行；热态、污染、净化器及其他P1禁令保持有效。
- 素材直接接入已认可的 `tools/art-assets/generated/item/lapis_dust.png`，模型引用 `create_nuclear_industry:item/lapis_dust`；本轮最终保留 lapis 的历史 `game:null` 工具候选记录，不扩展素材清单游戏路径。

## 验证证据

- 定向 GameTest：`JAVA_HOME=C:/Program Files/Java/jdk-21; .\gradlew -PgameTestNamespace=create_nuclear_industry_coolant -PgameTestDirectory=build/gametest-coolant-production runGameTestServer --console=plain`，退出码0；6/6 required tests通过并正常保存退出。覆盖加载配方/输入拒绝/标签、真实粉碎轮1:1、磨石继续产蓝色染料、无燃烧室搅拌的材料与流体守恒、输出罐堵塞后保料及恢复。
- 外部兼容探针在独立 `externalCoolantTags` GameTest batch 中运行。临时数据包仅位于 `build/gametest-coolant-production/world/datapacks/coolant-compat-pack`，`pack_format:48`，向 `c:dusts/lapis` 添加模拟成员 `minecraft:flint`。测试执行禁用并重载、启用并重载、断言青金石粉标签和真实搅拌 Ingredient 接受 flint，再禁用并重载确认标签恢复。日志标记 `COOLANT_EXTERNAL_TAG_SIMULATION`；包及其两份文件副本见 `EXT-A-MATERIAL-01B/evidence/simulated-external-datapack`，不会进入发布资源。
- `P1DataContractTest` 定向运行退出码0：5 tests，0 failures/errors/skips。首次运行因基线合同漏放已批准的02E `shielded_assembly/fresh_fuel_assembly.json` 而失败；按 PM 授权仅增加该精确路径的燃料组件ID例外后重跑通过。冷却剂例外仍只覆盖 `mixing/compound_coolant.json` 中的冷态ID。JUnit XML副本见 `EXT-A-MATERIAL-01B/evidence/p1-data-contract-results.xml`。
- 最后一次增量 `assemble` 退出码0。JAR：`build/libs/create_nuclear_industry-0.1.0.jar`，SHA-256 `67A2F0F39278DCD5B27F88ECC4A1B61FEF20CFF7BFCCCEDD3C921E272F6D1880`。ZIP核对确认新模型、贴图、标签、两条配方及现有冷却剂桶模型、静态/流动贴图均存在。
- 青金石粉PNG为16×16，SHA-256 `7011BC285C26C756D40AE7087B47022C3C9FD5002EC03F349A67E15D1475CE09`。基线中113张已跟踪PNG均无差异。实现文件定向 `git diff --check` 未报告空白错误；全候选检查仅指出本轮游戏运行改写了受跟踪根目录 `logs/debug.log`、`logs/latest.log` 的新增末行，已告知PM并留待PM归档恢复，执行者未处理这两份日志。
- 原始末轮 Gradle/GameTest 输出见 `EXT-A-MATERIAL-01B/gametest-final.log` 与 `assemble-final.log`；GameTest服务端日志见 `EXT-A-MATERIAL-01B/evidence/gametest-server-latest.log`。

开发期间曾运行临时JSON镜像JUnit探针：先以缺少资源按预期失败；资源补齐后该探针通过。按PM的验证精简要求已撤回该重复测试源码，没有纳入交付或最终验证清单。开发中还出现并修正过测试代码的API编译错误；最终GameTest与assemble均由修正后源码成功完成。

## 尚待人工验收

未启动用户客户端或存档。按任务卡保留三项人工门：JEI及名称/图标；粉碎轮制粉与磨石蓝色染料；无加热搅拌并用现有桶/管道取走冷却剂。本候选等待PM合并审查。

技能实际应用：`minecraft-modding` 核对锁定版本、注册与原生配方；`minecraft-testing` 按真实机器tick、流体/物品守恒与隔离命名空间实施定向GameTest；`minecraft-resource-pack` 核对1.21.1模型路径和16×16纹理引用。没有套用技能中的新版本示例或升级依赖。
外部测试包的可复现文件内容（只放在上述隔离运行目录，不进入`src/main/resources`）：

`world/datapacks/coolant-compat-pack/pack.mcmeta`
```json
{"pack":{"pack_format":48,"description":"Simulated external lapis dust tag member for EXT-A-MATERIAL-01B GameTest"}}
```

`world/datapacks/coolant-compat-pack/data/c/tags/item/dusts/lapis.json`
```json
{"replace":false,"values":["minecraft:flint"]}
```

开发期间曾有两次短暂的GameTest源码编译失败：错误的`FluidAction`包路径/访问`BasinBlockEntity`受保护字段，以及把返回`void`的命令API当作整数比较；分别改用嵌套`IFluidHandler.FluidAction`和公开tank pair访问、移除返回值比较后，最终6项GameTest全部通过。失败时工具输出未另存为独立日志，因此不重演生成日志。

## 交付后修正

PM审查发现，若将lapis清单的`game:null`改成贴图路径，`tools/art-assets/pipeline.py::load_manifest`会违反其固定的82个游戏路径加1个lapis候选约束，素材导出器的默认入口会拒绝该清单。按PM指示从`b61fae5`只读核对并恢复该条目的两个历史字段：`game:null`及用途“已批准候选，本批不新增注册和游戏PNG”。没有改动游戏源码、资源或JAR，也没有运行export/install/verify全套。恢复后使用捆绑Python只读调用`pipeline.load_manifest()`成功，返回83项、82个游戏路径、lapis候选`game=None`；命令输出及检查时间由本次交付记录，未另建日志。
