# EXT-B-TURBINE-01B 独立规格与质量合并审查

**审查范围：** 候选工作树 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，基准 `0c25010`；按运行/资源交接接口检查当前冻结实现与资源。审查者为执行者，只读核验并写本报告。

## 结论

当前静态审查未发现未解决的阻断实现问题。审查过程中发现的端轴改向活体验证、窗口未成型透明层和向下端口状态缺项，均已在最终交接前修正。A 的定向 JUnit、GameTest 与迁移日志有通过证据；最终 `assemble` 由 PM/A 继续核验中，本文不将其记为已通过。客户端视觉与真实旧世界迁移仍需人工验收。

## 已发现并修复

| 审查发现 | 依据与影响 | 最终状态 |
| --- | --- | --- |
| 成型端轴被扳手改向后仍可输出 | 初审时 `TurbineStructure.quickLive()` 未核轴 `MACHINE_FACING/END`，`validShaft()` 也未核机组朝向；Create 轴接口可能随方块朝向变化。 | A 已在 `TurbineStructure.quickLive()` 与 `TurbineControllerBlockEntity.validShaft()` 加入朝向和端位核验；最终 GameTest 把改向后自动纠正作为预期。详见 `EXT-B-TURBINE-01B-RUNTIME.md` 与 `EXT-B-TURBINE-01B-RUNTIME-migration-gametest.log`。 |
| 端口 blockstate 缺少 `outward=down` | 初审 `turbine_inlet.json`、`turbine_exhaust.json` 各仅有320项；而 `TurbineStructure.outward()` 会为底侧端口返回 `DOWN`，接口合同要求六个 outward 方向。合法底侧端口会请求缺失模型状态。 | B 已补齐为各384项。独立解析确认两文件现均含 `down,east,north,south,up,west`。 |
| 未成型观察窗模型未指定半透明 | 初审 `turbine_window_unformed.json` 无 `render_type`，而观察窗物品模型继承该回退模型；客户端 Java 只注册转子渲染器，没有另行注册窗口层。 | B 已为未成型回退模型添加 `render_type: translucent`。复核确认12个成型窗口模型和未成型回退均声明 translucent。物品父模型仍指向该未成型窗口模型。 |
| 旧控制器邻接的 Create 动力缓存可能残留 | 旧版控制器为动力源；新 `SmartBlockEntity` 不读取旧 Create 动力字段，但相邻旧轴可能仍保存指向控制器的 `source`。 | A 增加可持久化迁移标记；逐一检查六个相邻已加载方块，仅当动力 BE 的 `source` 与该控制器位置相等时才 `detachKinetics()`/`removeSource()`，有未加载邻区块则保留标记重试。迁移定向 GameTest 4/4 通过。未扩大清理到其他 source 或整网。 |

另曾核对同 block ID 的派生壳属性命令改写。该情形不属于正常搭建设定，最终结论不将其升为玩法阻断项；壳体身份变更的正常方块替换路径会触发 owner 失效，空气核仍逐 tick 检查。

## 最终证据

- `EXT-B-TURBINE-01B-RUNTIME.md`：迁移版 `compileJava compileTestJava` 退出码0；几何/账本定向 JUnit 11/11；默认结构与迁移 GameTest 4/4；非默认配置 GameTest 1/1。各次服务端日志记录世界保存并正常退出。
- `EXT-B-TURBINE-01B-ASSETS.md`：206 个 piece 与三档截面数量、模型引用和完整状态静态检查；端口各384状态、12个窗口透明层；观察窗配方/掉落/标签/语言 JSON 检查；六条既有配方哈希保持一致。离线预览不作为客户端验收。
- 本次只读独立解析：casing/window 变体各1656，rotor 24，output shaft 16，controller 32，inlet/exhaust 各384；inlet/exhaust 六向 outward 齐全。`turbine_window_unformed.json` 与成型窗口模型均为 translucent。

## 尚待人工/最终出口

- 客户端手测确认四向搭建、端轴接入、叶片裁切与转子可见性、内外薄壳和窗口透明效果；静态模型/离线预览及 GameTest 不能证明这些画面效果。
- 未实载 01A 世界存档，未做真实跨区块卸载/重载；迁移 GameTest 只覆盖合成旧 NBT 与相邻旧 source 清理。
- 四水平朝向真实 GameTest、前后轴外接后的合网/分网组合仍以运行报告列出的未覆盖边界为准。
- 最终资源冻结后的 `assemble` 及打包引用核查尚由 PM/A 执行，本审查不预先宣称通过。

未运行 Gradle、JUnit、GameTest 或 Git 写操作；复用 A/B 报告与原始日志，不重复测试。
