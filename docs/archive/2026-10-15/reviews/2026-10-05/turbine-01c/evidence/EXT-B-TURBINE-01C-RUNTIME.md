# EXT-B-TURBINE-01C 任务 A 交付记录（最终代码冻结）

## 改动与边界

- 完整双端轴和连续转子列确定 3/5/7 格直径与轴向长度，`located` 只驱动搭建外观；`formed` 仍须侧控制器的完整 Form 扫描授予。局部外观不认领机主、不开放端口库存、不发布双轴 SU。
- 机壳/观察窗独立板用保留 `piece` 编码表示六个世界面，放置、扳手转向、模型选取和碰撞共用该方向。已有壳体复用 01B 几何 piece，转子叶片随已定位档位显示；拆壳保留局部外观，拆轴清除失效定位。
- 使用 Create 6.0.10-280 的 `IPlacementHelper`、`PlacementHelpers` 和 `PlacementOffset.placeInWorld`：点转子补当排壳、点壳沿轴向补下一格、点端轴补端盖。成功仅放一件并扣一件；服务端核验触及范围与 `Level.mayInteract`。已定位但无合法落点或存在多匹配轴列时明确拒绝，避免回落成原版 BlockItem 意外摆放；潜行或无布局仍可原生放置。
- 控制器空手检查返回已定位尺寸与首个缺件，通过 Create `HighlightPacket` 仅向执行玩家标记服务端确定的错误格。缺入口/出口时标记合法中央/两端候选，不指向控制器或已有另一类接口。
- `TurbineStructure.issue` 对已定位机器先按实际档位核验。默认尺寸、256RPM、流量、容量、SU、库存 NBT、无 GUI 约束不变。

## 末次自动验证

| 检查 | 结果 | 原始记录 |
| --- | --- | --- |
| 增量 `compileJava` | 退出码 0 | `EXT-B-TURBINE-01C-RUNTIME-compile-final.log`、`.exit.txt` |
| `test --tests '*TurbineAssemblyLayoutTest'` | 2/2 通过，退出码 0；三档、四向、非默认轴长及独立板编号 | `EXT-B-TURBINE-01C-RUNTIME-junit-final.log`、`.exit.txt` |
| 隔离 `runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine -PgameTestDirectory=build/runtime-01c-gametest-final` | 8/8 required 通过，退出码 0；四项本任务场景与四项既有汽轮机生命周期场景 | `EXT-B-TURBINE-01C-RUNTIME-gametest-final.log`、`.exit.txt` |
| 增量 `assemble` | 退出码 0 | `EXT-B-TURBINE-01C-RUNTIME-assemble-final.log`、`.exit.txt` |
| 本批资源入包 | B 写集 9 个已修改 +20 个新增资源全部在 JAR 内，缺失 0 | `EXT-B-TURBINE-01C-RUNTIME-jar-resources-final.txt` |

四项新增 GameTest 覆盖：真实服务端玩家持物交互逐件包壳、阻挡失败不扣料、补齐后才 Form 及拆件停机；独立板按点击世界面放置与扳手转向碰撞；多匹配轴列拒绝辅助摆放且不扣料；控制器显示档位与缺件，并把缺入口/出口标记到合法候选。包壳用例由测试夹具先放轴列和功能接口，再用 `makeMockPlayer(GameType.SURVIVAL)` 对完整核心逐件调用方块 `useItemOn`，未用 `setBlock` 直接填满完整壳体冒充玩家搭建。首轮日志保留，末次验证以 `-final` 文件为准。

`git diff --check` 对 A 既有代码文件退出码 0；全工作树同命令发现的尾随空白仅在范围外的 `logs/debug.log`、`logs/latest.log`。共享工作树的 PM 文档和 B 资源改动均保留原样。

## 写集与人工边界

A 修改的既有文件：`content/TurbineContent.java`、`turbine/TurbineControllerBlockEntity.java`、`TurbinePartBlock.java`、`TurbineShaftBlock.java`、`TurbineStructure.java`、`client/TurbineRotorRenderer.java`。新增：`turbine/TurbineAssembly.java`、`TurbinePlacement.java`、`gametest/ExtensionTurbineAssemblyGameTests.java`、`src/test/java/.../TurbineAssemblyLayoutTest.java`。以上均位于 `src/main/java/com/iksxh/create_nuclear_industry/`，测试文件例外位于 `src/test/java/com/iksxh/create_nuclear_industry/`。A 另写本报告及 `EXT-B-TURBINE-01C-RUNTIME-interface.md`。

客户端真实渲染、Create ghost 预览、透明窗、轴端连接、世界内高亮的实际可见性以及玩家手感仍需用户人工客户端验证。服务端 GameTest 验证了诊断结果和高亮包的目标选择，不能替代客户端画面观察。未运行用户客户端、未打开其存档、未修改 run 配置、docs、B 资源、语言或生成器，未执行 Git 写操作。

实际读取并应用 `minecraft-modding`、`minecraft-testing`、`superpowers:executing-plans`；按项目治理 5.1 仅跑受影响 JUnit、汽轮机命名空间 GameTest 和增量构建。
