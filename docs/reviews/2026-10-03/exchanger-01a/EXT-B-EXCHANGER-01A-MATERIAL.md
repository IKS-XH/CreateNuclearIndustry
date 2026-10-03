# EXT-B-EXCHANGER-01A MATERIAL 交付记录

- 候选：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，开工 HEAD `3f64678263151f418112a5a7cb6d0c6d03a74743`。
- 日期：2026-10-04。
- 本报告只覆盖 MATERIAL 写集。设备、艺术和项目经理同期改动同处该候选，不纳入本交付文件清单。
- 保留开工时已有的 `tools/art-assets/__pycache__/`；未运行 Git 写操作、Gradle、JUnit、GameTest 或客户端。

## 交付内容

- 新增 `HeatMaterialsContent`，定义 `STEEL_PIPE_BLANK`、`REINFORCED_STEEL_PLATE` 和 `NUCLEAR_HEAT_EXCHANGE_BUNDLE` 三个延迟注册物品，供设备入口调用 `register(IEventBus)`。
- 新增四条原生配方：钢锭切石产2钢管坯；坚固钢板、精密构件与钢板竖排合成1强化钢板；4管坯、2铜片和1强化钢板工作台合成1管束；21格 Create 机械合成产1核换热器，禁止镜像。
- 新增通用管坯标签 `c:tubes/steel` 及父标签 `c:tubes`；新增 `c:plates/reinforced_steel` 并追加到既有 `c:plates`，不加入 `c:plates/steel`。整机沿用专用核换热管束标签；按PM确认的扩展写集，为既有耐压接头和工业传感器建立单成员专用标签。
- 新增三项物品模型、16×16透明PNG、严格SVG源稿与独立生成脚本。强化钢板沿用原钢铁色板；钢管坯呈现中空管口和延伸管身，核换热管束呈现三条并列管线与两组连接箍。预览为离线像素检查，不代表客户端验收。
- 中英文语言文件只加入本批三材料名及设备执行者交来的换热器名称/护目镜键。依据 `build/reports/extension/EXT-B-EXCHANGER-01A-DEVICE/language-keys.json`，两份文件的设备键均无缺漏。
- 新增 `ExtensionHeatExchangerCraftingGameTests`。覆盖物品注册和标签、切石结果及真实切石菜单取料、机械锯原生切石回退、两条工作台配方匹配/产量，以及21格材料分布/匹配/组装产量。

## 技术依据和检查

实际读取并应用的技能：

- `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：沿用项目锁定的 NeoForge 延迟注册与原生数据配方方式，不引入新依赖。
- `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：沿用 NeoForge GameTest holder/template 约定；本批代表测试针对已加载配方和真实原生输入路径编写。
- `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：采用对应1.21.1的16×16 RGBA物品纹理和 `minecraft:item/generated` 模型，使用仓库严格SVG渲染器。

实现基线为 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280。使用锁定 Create source JAR（SHA-256 `376DE15CA5ACF720106A075CA4EB2EF53E63E0E5D9EC93523A1ABBDD0F9F0CB4`）核对：`SawBlockEntity.getRecipes` 仅在 `allowStonecuttingOnSaw` 开启时纳入 `RecipeType.STONECUTTING`（源码约395–403行）；`applyRecipe` 对每个输入件读取切石结果，并放入锯库存（约339–385行）。因此测试以一条原版切石配方作为Create机械锯回退，不添加同输入的竞争 `create:cutting` 配方。另从同一锁定JAR确认 `c:plates/copper` 含 `create:copper_sheet`，且没有 `create:sturdy_sheets` 标签，坚固钢板配方据此使用实际物品 `create:sturdy_sheet`。

21格代表测试使用锁定版 `RecipeGridHandler.GroupedItems.read` 与 `MechanicalCraftingInput.of` 构造Create输入，再调用 `MechanicalCraftingRecipe.matches` 和 `assemble`。机械锯场景先堵住Depot出料以核对锯内两根管坯，再清空Depot并核对两根实际转交；配置关闭时断言钢锭保留。真实服务器用例尚未由本执行者运行；依任务合同由设备执行者持有唯一GameTest运行。

本地静态检查结果：本批配方、标签、模型和语言JSON均可解析；21格行列为5×5、去四角共21格，计数为钢板12、铜片4、耐压接头2、管束2、传感器1；设备语言键两份均无缺漏；图像生成器完成三项16×16 RGBA纹理和模型，并生成预览及哈希清单；`git diff --check` 未报告空白错误。未用资源存在性替代任何配方匹配或真实加工声明。

证据：`build/reports/extension/EXT-B-EXCHANGER-01A-MATERIAL/assets.json`、`heat-exchanger-materials-preview.png`。项目其余客户端模型外观、JEI与实际制造/机械锯运行仍待整批客户端验收；本报告不宣告任务最终验收通过。

## 首轮机械合成 GameTest 整改

设备执行者首轮隔离服务端组中，材料相关用例除21格机械合成匹配外均通过。失败原因为测试输入构造漏掉Create原生要求的尺寸统计：锁定版 `RecipeGridHandler.tryToApplyRecipe` 在 `MechanicalCraftingInput.of` 前调用 `GroupedItems.calcStats()`；而 `GroupedItems.read` 只还原NBT格子、不计算宽高。首版测试直接把尚未统计的零尺寸分组交给输入适配器，因此传入 `MechanicalCraftingRecipe.matches` 的实际网格为空。

现已在代表测试中按原生顺序显式调用 `grouped.calcStats()`，再创建 `MechanicalCraftingInput`；NBT `Grid` / `x` / `y` / `item` 键与 `GroupedItems.write/read` 一致，纵向行序按原生 `maxY - y` 归一化。`matches`、`assemble` 与精确数量断言均保留。未运行Gradle或GameTest；修复后的通过状态等待设备执行者统一复测确认。
