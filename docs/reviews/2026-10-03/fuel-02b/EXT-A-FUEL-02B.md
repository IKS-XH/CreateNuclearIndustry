# EXT-A-FUEL-02B 交付报告

- 基线：`769bcbd9d2c727f0ec4b56117ea19e8200779ca0`，候选分支 `codex/ore-acquisition`。
- 范围：烧结炉整格模型与配方从机械合成迁移至普通有序工作台配方；未改Java、测试、PNG、SVG、README、旧资产或其他文档。
- 技能实际使用：
  - `C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`：对照本项目Minecraft 1.21.1、NeoForge 21.1.219、Create 6.0.10-280 的资源路径和原生配方入口。
  - `C:/Users/IKSXH/.codex/skills/minecraft-resource-pack/SKILL.md`：按1.21.1方块模型规则检查边界、显式UV、朝向状态和三维物品显示变换。
  - `C:/Users/IKSXH/.codex/skills/minecraft-testing/SKILL.md`：按治理5.1选择资源定向检查及一次增量assemble，保留真实工作台/JEI与客户端外观手测门；不重复JUnit/GameTest。

## 修改

- 更新 `tools/art-assets/fuel_02a_assets.py`：新增 `--geometry-only` 生成选项，避免本任务触碰PNG/SVG；炉体X/Y/Z外包达到0..16，维持八棱结构、端盖、黄铜箍与顶部/底部接口。北向窗位由连续钢面拆成左右/上下钢面、黄铜框和齐平窗片，几何边缘相接且不越界。整格物品GUI缩放使用0.625，第一/第三人称手持缩放沿用原值。
- 重生成 `models/block/fuel_sintering_furnace_off.json`、`_on.json`、`_item.json`。
- 删除 `recipe/mechanical_crafting/fuel_sintering_furnace.json`；新增 `recipe/crafting/fuel_sintering_furnace.json`，使用 `minecraft:crafting_shaped` 与 `RSR/RBR/SIS`，输入为4耐火砖、3钢板标签、1原版高炉、1工业传感器，输出1台炉。

## 验证

- `python.exe -B tools/art-assets/fuel_02a_assets.py --geometry-only`：成功；检查三模型落盘几何，包围盒数值容差内为0..16³；off/on/item每份320个显式UV面，无隐式或越界UV；八棱外壳4096射线采样无缺口。北向面100×100采样未发现未覆盖点或共面重叠，窗中心没有共面钢面遮挡。
- 配方静态断言核对类型、布局、材料和产量；确认旧机械配方源文件不存在。锁定版本Create源码 `com/simibubi/create/content/kinetics/crafter/RecipeGridHandler.java` 先查询 `RecipeType.CRAFTING`；`CRecipes.allowRegularCraftingInCrafter` 默认值为true，因此不增加第二份机械专用配方。
- 对7张既有芯块/炉体PNG核对SHA-256，全部与开工前相同；没有运行素材导出器默认PNG写入模式。
- `git diff --check`：通过。
- 唯一构建：`.\gradlew.bat assemble --console=plain`，`BUILD SUCCESSFUL`，退出码0。JAR `build/libs/create_nuclear_industry-0.1.0.jar` SHA-256：`41ba1c74cd935b1e3ec05d70f695e5addfc5aec07cd76fbd082226c292787f15`；包内新配方存在，旧机械配方不存在。

## 证据与未完成验收

证据位于 `build/reports/extension/EXT-A-FUEL-02B/`：`asset-check.json`、`resource-check.json`、两张从落盘JSON模型渲染的北向窗/顶部接口与底部接口预览、PNG字节基线、`assemble.log`、`assemble.exit-code.txt`、`jar-check.json`。

本次未启动客户端或JEI。待用户/项目经理安排的手测仍为：①工作台新配方与JEI显示；②整格炉体放置、冷热窗口、物品栏/手持外观。02A既有手测只作为原基础功能证据复用，不等同于02B上述改动已通过客户端验收。
