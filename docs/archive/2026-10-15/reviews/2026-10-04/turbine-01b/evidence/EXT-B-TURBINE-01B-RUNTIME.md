# EXT-B-TURBINE-01B 运行整改交付报告

状态：A 运行代码已完成定向验证，B 资源冻结后的最终 `assemble` 与本批资源入包校验通过。本报告不代表客户端画面验收或项目经理验收。候选目录 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，基线 `0c25010`；执行者未进行任何 Git 写操作，未修改用户客户端、存档或 `run/config`。

## 实现边界

- 保持超临界蒸汽输入、普通蒸汽输出、流量、总 SU、红石停机与单份库存语义；默认真实生成转速设为 256 RPM，两端独立轴各取总 SU 的一半。短/中/长档按 3×5×3、5×8×5、7×11×7 形成，直径作为配置项并在服务端严格对应档位。
- 侧面控制器改为非动力 `SmartBlockEntity`；前后端输出轴为独立 `GeneratingKineticBlockEntity`，仅向机外一面露轴。完整 206 个端盖/薄八棱壳几何编码由同一表驱动结构判定、碰撞和资源。D7 中段包括四个斜角内格、排除四个外角。中段仅轴心为 rotor，其余内腔须为空气；窗口仅允许四个平直侧面中心格。端盖/端层侧壁、端轴 3/16 端板和格内轴身均有对应形状。
- 成型时缓存当前档位 Form，每 tick 验证实际构件、内腔和已加载区块并即时撤销旧 SU；未成型时每 10 tick 诊断一次，避免每 tick 重建三档几何列表。轴方向与 `end` 也参与活体校验，玩家旋转轴后同 tick 不向错误轴面供能，下一 tick 由机主派生正确状态。
- rotor 静态轴模型与动态叶片分离；BER 以块中心为旋转中心加载 `rotor_blades_d3/d5/d7`，三轴包围盒扩到叶片扫掠范围。对应资源命名、状态全集和方向合同见 `EXT-B-TURBINE-01B-RUNTIME-interface.md`。窗口透明模型与完整 blockstate 由 B 资源执行者交付。

## 旧版 NBT 与 Create 动力迁移

旧控制器继承 `GeneratingKineticBlockEntity`，其 `Speed`、`Source`、`Network` 由 Create `KineticBlockEntity.read` 恢复，并在 `initialize` 将缓存重新挂入 `KineticNetwork`。新控制器继承 `SmartBlockEntity`，其 `super.read` 不再读取这些旧动力标签，也不再提供 `getGeneratedSpeed`，故旧控制器自身不会重新生成 SU；服务端 `onLoad` 仍停账本并撤销当前 Form。

邻接旧轴仍可能携 `Source=旧控制器坐标`。新控制器读取旧动力标签后保存一次性 `LegacyKineticMigrationPending`；其首个服务端 tick 检查六个邻格，只对 `source` 恰等于本控制器坐标的 Create 动力 BE 先调用 `detachKinetics`、再 `removeSource`，并同步客户端；其他动力源与共享网不被主动清空。若任一邻格区块未加载，标记写回 NBT 并后续重试；六面均可核查后清除标记。唯一流体库存仍从旧 `Turbine` 标签读取，没有复制到两端轴。此机制只保证直接旧源分支被检查后的撤销，完整旧版世界存档加载及跨区块重载尚未实测。

## 定向自动验证

| 命令/范围 | 结果 | 原始日志 |
| --- | --- | --- |
| `compileJava compileTestJava`（迁移最终代码） | exit 0，3 条既有 API 弃用警告 | `EXT-B-TURBINE-01B-RUNTIME-migration-compile.log` |
| `test --tests ...TurbineGeometryTest --tests ...TurbineStateTest` | 11/11 JUnit 通过：薄壳编码/尺寸与账本 | `EXT-B-TURBINE-01B-RUNTIME-junit.log`；`build/test-results/test/TEST-*.xml` |
| `-PgameTestNamespace=create_nuclear_industry_turbine -PgameTestDirectory=build/gametest/EXT-B-TURBINE-01B-runtime-migration runGameTestServer` | 4/4 required，隔离世界正常保存关服；覆盖三档北向成型、多口、前后网络份额与 256 RPM、红石/拆放库存、Create 普通蒸汽管道、模拟旧 NBT 源撤销 | `EXT-B-TURBINE-01B-RUNTIME-migration-gametest.log` |
| `-PgameTestNamespace=create_nuclear_industry_turbine_config -PgameTestDirectory=build/gametest/EXT-B-TURBINE-01B-runtime-nondefault runGameTestServer` | 1/1 required，隔离世界正常保存关服；短档 4 rotor、D5、96 RPM 及自定义份额/流量生效 | `EXT-B-TURBINE-01B-RUNTIME-gametest-nondefault.log`，配置样本 `EXT-B-TURBINE-01B-RUNTIME-nondefault.toml` |
| B 资源冻结后 `assemble --console=plain` | exit 0，`processResources`、`jar` 实际执行 | `EXT-B-TURBINE-01B-RUNTIME-assemble.log` |
| 逐项 SHA-256 对比全部 501 个本批新增/修改的 `src/main/resources` 文件与 `build/libs/create_nuclear_industry-0.1.0.jar` 条目 | 501/501 字节一致，0 缺失、0 差异（含语言、标签及配方） | `EXT-B-TURBINE-01B-RUNTIME-jar-resources.log` |

前两次默认 GameTest 迭代日志 `EXT-B-TURBINE-01B-RUNTIME-gametest-default.log` 与 `...default-rerun.log` 保留：一次断言在短档排汽库存满后仍期待额定运行，另一次把轴改向后服务端自动纠正误判为失败；均修正测试时点/期望，最终默认 4/4 通过。最终迁移清理未改流量或几何，非默认 1/1 证据复用其先前运行。

未测边界：没有覆盖四水平朝向的真实 GameTest、两端外接轴合网/再拆网、错误端口与窗口位置的完整负例矩阵、真实旧 01A 世界文件加载、真实跨区块卸载/重载、客户端视觉/玻璃透明/叶片裁剪及手动交互。没有运行全量测试。资源冻结前的 `processResources` 为 `compileJava` 的 Gradle 依赖，最终入包证据只取资源冻结后的 `assemble` 与 SHA-256 校验。

实际使用技能：`minecraft-modding`、`minecraft-testing`、`superpowers:executing-plans`。遵循项目治理 §5.1 的增量、定向验证。
