# EXT-A-FUEL-02B 限定独立复审

**基线：** `769bcbd9d2c727f0ec4b56117ea19e8200779ca0`。仅审本批四处允许差异及现有素材、装配和JAR证据；未复审02A运行实现，也未运行Gradle、客户端或其他检查。实际读取并按项目版本应用 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack`，核对MC 1.21.1 / Java 21 / NeoForge 21.1.219 / Create 6.0.10-280。

## 结论

未发现本批可复现的 P1/P2 阻断或需返工的小问题；本批两项指定人工验收可继续。结论限于这次模型与配方差异，不代表两项客户端手测已通过。

## 本次差异核对

模型生成器只调整炉体几何、检查和预览输出；工作树差异仅为三份炉体模型JSON、该专用生成器、旧机械配方删除及新工作台配方。Java、测试、语言、PNG、SVG、旧资源和其他文档未变。七张PNG的现有SHA-256与开工基线一致。

冷态、热态和物品模型分别覆盖完整 `0..16` 三轴范围；几何计算的最小边界出现约 `-8.9e-16` 的浮点舍入尾数，源JSON坐标和实际模型边界在容差内为 `0..16`，没有实质越界。每份炉体模型有61个元素、320个显式UV面，没有隐式或范围外UV。GUI缩放为0.625，第一/第三人称缩放保留0.4/0.375。预览可见八棱壳体、顶/底接口、端盖衔接和嵌入的观察窗；两张炉体预览均直接由落盘JSON渲染。

针对此前缺面问题，生成器只移除未旋转、北端面位于 `z=0` 的中央壳板；旋转斜板不满足删除条件。北面框、窗片按相邻矩形拼接。现有 `asset-check.json` 的100×100北面采样记录未覆盖点0、同平面重复覆盖点0、可见北向面12片；窗中心也没有钢面遮挡。源码中的钢框、黄铜框和窗片边界相接。该采样和离线预览不能替代Minecraft客户端最终渲染。

新配方为 `minecraft:crafting_shaped`，布局 `RSR/RBR/SIS`，材料为4耐火砖、3个 `c:plates/steel`、1个 `minecraft:blast_furnace`、1个工业传感器，产1台炉；旧专用机械配方已删除。锁定版本Create源码 `RecipeGridHandler.tryToApplyRecipe` 在 `CRecipes.allowRegularCraftingInCrafter` 开启时先查询普通 `RecipeType.CRAFTING`，再查机械专用配方；该配置默认值为 `true`。服务器配置可关闭机械合成器对普通配方的兼容，但工作台配方自身仍可用。

## 已有证据与边界

- `asset-check.json`、`resource-check.json`：模型引用、外包范围、UV、北向覆盖及PNG字节基线检查均无报错。七张PNG字节不变。
- 唯一增量 `assemble` 退出码0，见 `assemble.log` 和 `assemble.exit-code.txt`。JAR SHA-256 为 `41ba1c74cd935b1e3ec05d70f695e5addfc5aec07cd76fbd082226c292787f15`。`jar-check.json` 与只读检查确认新配方已打包、旧 `recipe/mechanical_crafting/fuel_sintering_furnace.json` 条目不存在。
- Java与设备逻辑未变，按治理5.1复用02A已有运行证据；本批没有重跑JUnit或GameTest。
- 尚待两项客户端操作：①确认工作台配方与JEI展示；②确认整格炉体放置、冷热窗口、物品栏和手持外观。未据本次静态检查宣称客户端视觉通过。

审查报告只写入 `build/reports/extension/EXT-A-FUEL-02B-REVIEW.md`；未执行Git写操作。
