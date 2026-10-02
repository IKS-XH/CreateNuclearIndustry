# EXT-A-FUEL-01F 限定审查

## 结论

限定资源审查通过，未发现超出端盖接缝修复范围的问题。最终客户端外观仍须由用户按任务卡进行手测确认；几何预览和静态 JSON 检查不代表 Minecraft 客户端的 UV、光照或渲染效果。

## 核查

- 对照任务计划及基线 `bb7ea08` 检查差异，工作树仅改了许可的四个文件：离心机生成器、lower/upper block 模型与完整 item 模型；未发现 PNG、转子或显示变换资源改动。
- 直接检查三份最终模型 JSON 中新增端盖几何。每端由四块贯通板组成，板的横截面尺寸覆盖半径 7.30；实际旋转分别为 0°、-45°、90°、+45°，侧面法线合成八个每隔 45° 的外向方向。板上下缘逐块错开 0.002，避免严格共面重叠；没有八块反向重复板。lower 端盖 Y=3.98..6.04，upper 局部端盖 Y=10.96..12.02，与相邻壳体范围相交，未见端面缝隙条件。
- 静态几何记录中的端盖外缘采样各为 4096/4096 覆盖、0 未覆盖；壳环接合亦为 4096/4096，最小搭接余量 0.016468。记录显示lower/upper/item/rotor分别为60/62/145/23个元素，UV 显式面无越界；相关证据为 `build/reports/extension/EXT-A-FUEL-01F-assets/geometry-check.json`。顶部斜视预览仍可见中央进料开口，底部与斜视预览未显示端盖周向缺口；预览仅表达 JSON 几何和纯色。
- 任务报告记录端盖预览、原有窗口/顶口/转子/显示参数保持情况；静态预览不验证实际贴图采样、游戏光照、面剔除或客户端效果。生产改动未运行测试；复用了唯一 assemble 记录：`build/reports/extension/EXT-A-FUEL-01F-assets/assemble.log`，其中 `compileJava UP-TO-DATE`、`BUILD SUCCESSFUL in 2s`。任务报告记录最终 JAR SHA-256：`36ED63B5D8A02C532CA81E234280075872FD23AF884DB0529D652BC14734A543`。

## 范围与方法

本次只读审查，没有运行测试、Gradle 或资源生成器，也没有改实现文件。已阅读活动计划、AGENTS.md、治理协议5.1，并应用 `minecraft-modding`、`minecraft-testing` 与 `minecraft-resource-pack` 技能中适用于限定资源审查的版本/证据边界。
