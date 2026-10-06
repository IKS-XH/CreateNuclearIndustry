# CRAFT-SHAPED-01：设备工作台配方改为有序

**需求来源（2026-10-06）：** 用户确认铅屏蔽桶R1手测通过，要求检查新增无序工作台配方，避免Create搅拌机自动转换；随后明确粗矿块和金属锭拆解保持无序，只改设备与零件。

**状态：** 用户2026-10-06确认联合手测全部通过，已随STORE-01合入main，见[最终验收](../../reviews/2026-10-06/store-01/ACCEPTANCE.md)。源提交`f2cbb593ce1751bad28ce6406a878f6601c851c1`；13条设备配方与三个导出器同步，五条拆解保留无序。原静态/资源证据复用，不重跑功能测试。

**目标与合同：** 把下列13条`minecraft:crafting_shapeless`改为`minecraft:crafting_shaped`。材料标签/item身份、重复材料数量、result完整对象、配方ID、产量及其他属性保持不变；不新增原料、工序或序列装配。只改变工作台格子布局。原铅桶R1有序布局保持不变。

**基线：** 候选`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`、HEAD `3af86c028a6d465bfa6d1db8ca29d1af852a0afa`。已有`logs/debug.log`、`logs/latest.log`和`tools/art-assets/__pycache__/`不得处理或提交；main的`.vscode/launch.json`不动。MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6，版本与许可证不变。

**前置：** 已有原生配方可用；用户已明确本次类型变更和拆解豁免，无未决数值。锁定Create源码`MechanicalMixerBlockEntity.matchStaticFilters`第262～268行及`CreateJEI`第190～196行均排除`ShapedRecipe`，无需改Create全局配置或加自有匹配机制。

**必读：** 候选AGENTS.md、治理5.1、配方2.2、本卡。实际读取`C:/Users/IKSXH/.codex/skills/{minecraft-modding,minecraft-testing,minecraft-datapack}/SKILL.md`，按1.21.1的单数`recipe/`目录和result.id应用，不能沿用其他版本示例升级技术栈。

## 执行者精确写集

数据根为`src/main/resources/data/create_nuclear_industry/recipe/`，仅允许修改：

| 文件 | 有序pattern | key含义 |
| :--- | :--- | :--- |
| `crafting/reactor/reactor_window.json` | `G` / `C` | G屏蔽玻璃，C反应堆外壳 |
| `crafting/reactor/reactor_cold_port.json` | `FD` / `CS` | F耐压接头，D原蓝染料标签，C反应堆外壳，S密封环 |
| `crafting/reactor/reactor_hot_port.json` | `FD` / `CS` | 与冷口同形，D保留原红染料标签 |
| `crafting/high_pressure_boiler_window.json` | `G` / `C` | G屏蔽玻璃，C高压锅炉外壳 |
| `crafting/high_pressure_boiler_water_port.json` | `PF` / ` C` | P原Create流体管道，F耐压接头，C锅炉外壳 |
| `crafting/high_pressure_boiler_steam_port.json` | `F F` / ` P ` / ` C ` | F耐压接头两件，P强化钢板，C锅炉外壳 |
| `crafting/boiler_safety_valve.json` | `V R` / ` I ` / ` C ` | V原Create阀门，R密封环，I工业传感器，C锅炉外壳 |
| `crafting/boiler_heat_exchange_section.json` | `H` / `C` | H核换热管束，C锅炉外壳 |
| `crafting/turbine_window.json` | `G` / `C` | G屏蔽玻璃，C汽轮机外壳 |
| `turbine_inlet.json` | `P P` / ` R ` / `SC ` | P钢管坯两件，R强化钢板，S密封环，C汽轮机外壳 |
| `turbine_exhaust.json` | `P P` / ` R ` / `SC ` | 与入口同形，R保留原钢板标签 |
| `turbine_output_shaft.json` | `SBS` / `SAS` / `BC ` | S原钢板标签四件，B重型轴承两件，A原Create轴，C汽轮机外壳 |
| `crafting/dry_storage_rack.json` | `S S` / ` C ` | S原钢板标签两件，C屏蔽混凝土 |

另允许修改：

- `tools/art-assets/boiler_01a_assets.py`：只同步上述五条锅炉配方的生成，保留其他配方和素材行为。
- `tools/art-assets/turbine_data.py`：只同步汽轮机观察窗生成。
- `tools/art-assets/store-01/generate.py`：只同步贮存架配方生成及原数量检查，保留R1铅桶和既有局部导出方式。
- 新建`tools/art-assets/crafting-shaped-01/`中的定向验证脚本/证据；仅报告`docs/reviews/2026-10-06/store-01/crafting-shaped.md`。原始打包日志放`build/reports/extension/CRAFT-SHAPED-01/`。

**禁止写集：** 五条原无序拆解`raw_{lead,tin,uranium}_from_block.json`及`crafting/materials/{lead,tin}_nugget_from_ingot.json`；其余配方、Java/测试/构建脚本、语言/PNG/模型、世界/配置/日志、核心docs。禁止全量运行旧素材导出器覆写已批准配方/资产。执行者禁止任何Git写操作和再派发。

## 步骤与必要验证

- [x] 执行者记录基线13条原料多重集合（标签与item分开）、完整result及五条拆解原始字节；生成器只定向或内存核验。
- [x] 按表改13条JSON及三处导出，相关手写说明/注释用中文，不夹带其他资源。
- [x] 展开pattern与原集合比对；检查每行等宽、尺寸≤3×3、key完整且无未用key，result和其他元数据不变。扫描src与工具确认剩余无序仅为五条拆解，五条字节不变，零件无漏项；原铅桶字节不变。
- [x] 导出器对应生成对象与源JSON一致，必要时仅输出目标配方。一次`./gradlew.bat processResources jar --console=plain`退出0，核对JAR内13条及五条保留配方与源一致，记录大小/SHA。无新增机制，不新增JUnit/GameTest，不clean/强制重跑或操作客户端。
- [x] 交付简短报告、13条布局表与证据，停止写入。PM复核实际差异及已有证据，简单原生JSON不另排重复全量或多层审查。
- [x] PM记录候选和用户R1通过范围；设备代表性工作台/JEI与搅拌机不匹配确认并入现有联合人工清单，等待用户。
- [ ] 用户确认代表配方与原STORE-01联合剩余范围，PM再完成整体验收及main功能合入。

**验证复用：** STORE-01原8项JUnit、6项真实GameTest覆盖未变功能，铅桶R1已有用户手测通过，不重复。若出现JSON无法加载、不同配方冲突或生成器越界，先针对实际问题整改，不扩大为热端全回归。最终手测与main功能合入沿用原联合人工门。

## PM编排记录

| 关系 | 核查结论 |
| :--- | :--- |
| 本任务JSON↔三个导出器 | 同一执行者同步，单一原料合同；禁止全量旧导出，避免回退已验收资源 |
| 本任务↔STORE-01 | 只改变架子工作台配方，库存/封装/设备代码不变；复用行为证据，人工门合并 |
| 本任务↔拆解豁免 | 五条单原料拆解保留原字节，最新用户限定优先于上一条全改要求 |
| 执行与验证 | 一个执行者持有资源打包；PM只读审实现，核心文档和Git由PM管理 |

流程裁定：这是已存在原生配方的有限修改，用户已明确类型、目的、材料守恒及豁免，按治理2/4直接派发，不重复审批。精简验证优先于技能通用的测试/复审/提交步骤。

**实际证据：** 基线、静态核对见`tools/art-assets/crafting-shaped-01/{baseline,verification}.json`；[执行报告](../../reviews/2026-10-06/store-01/crafting-shaped.md)列明13条布局。PM读实际diff发现的空格及文件尾换行问题已由执行者在打包前修复，静态核对通过。唯一资源打包3秒exit0，compileJava保持UP-TO-DATE；最终JAR2,145,081字节、SHA-256 `670F89993F555A89FDE0690D8A29F76A9DC51B7F6BC63CB49C6393A34EC9A1E7`。PM复核19条JAR/源一致及六个保留文件基线SHA一致，不重复构建或行为测试。main只同步受影响文档，日志/缓存/launch配置保留。
