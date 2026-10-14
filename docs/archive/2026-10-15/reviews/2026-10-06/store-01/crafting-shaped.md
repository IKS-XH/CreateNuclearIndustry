# CRAFT-SHAPED-01 执行报告

**基线：** 候选 `codex/ore-acquisition`，HEAD `3af86c028a6d465bfa6d1db8ca29d1af852a0afa`。执行前确认13条目标配方均为无序配方，材料数量与任务卡一致。保存的原始对象及豁免文件字节见 `tools/art-assets/crafting-shaped-01/baseline.json`。

## 改动

13条现有设备配方改为有序工作台配方，只改变阵列布局；原item/tag身份、重复材料数量、完整result及category均由定向核验对照基线。铅桶配方保持原字节。

| 配方 | Pattern |
| :--- | :--- |
| `crafting/reactor/reactor_window.json` | `G` / `C` |
| `crafting/reactor/reactor_cold_port.json` | `FD` / `CS` |
| `crafting/reactor/reactor_hot_port.json` | `FD` / `CS` |
| `crafting/high_pressure_boiler_window.json` | `G` / `C` |
| `crafting/high_pressure_boiler_water_port.json` | `PF` / ` C` |
| `crafting/high_pressure_boiler_steam_port.json` | `F F` / ` P ` / ` C ` |
| `crafting/boiler_safety_valve.json` | `V R` / ` I ` / ` C ` |
| `crafting/boiler_heat_exchange_section.json` | `H` / `C` |
| `crafting/turbine_window.json` | `G` / `C` |
| `turbine_inlet.json` | `P P` / ` R ` / `SC ` |
| `turbine_exhaust.json` | `P P` / ` R ` / `SC ` |
| `turbine_output_shaft.json` | `SBS` / `SAS` / `BC ` |
| `crafting/dry_storage_rack.json` | `S S` / ` C ` |

三个导出器已同步，并提供纯内存生成对象用于核对；没有运行完整资源导出器。定向静态证据在 `tools/art-assets/crafting-shaped-01/verification.json`：阵列尺寸、等宽、key完整性、材料多重集合、结果和元数据均通过；src剩余无序配方恰为三条粗矿块拆解及铅/锡锭拆粒五条豁免，五条原字节哈希与基线一致。

## 打包证据

唯一一次运行 `gradlew.bat processResources jar --console=plain`，退出码0；日志在 `build/reports/extension/CRAFT-SHAPED-01/gradle.log`。JAR中13条有序配方和5条保留无序配方均存在，18个条目逐字节等于资源源文件；条目数量为13 shaped、5 shapeless，没有缺项。

- JAR：`build/libs/create_nuclear_industry-0.1.0.jar`
- 大小：2,145,081 bytes
- SHA-256：`670f89993f555a89fde0690d8a29f76a9dc51b7f6bc63cb49c6393a34ec9a1e7`
- 条目哈希与制品信息：`build/reports/extension/CRAFT-SHAPED-01/artifact.json`

未运行JUnit、GameTest、客户端或全量导出。按任务要求读取并应用 `minecraft-modding`、`minecraft-testing` 与 `minecraft-datapack` 技能；使用本项目1.21.1单数 `recipe/` 路径与 `result.id` 格式，并按改动范围采用定向静态核验和一次资源打包。
