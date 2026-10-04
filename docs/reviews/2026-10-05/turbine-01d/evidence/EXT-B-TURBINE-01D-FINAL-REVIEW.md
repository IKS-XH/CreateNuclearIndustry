# EXT-B-TURBINE-01D 最终代码与资源审查

审查范围：冻结的 C 资源/生成器、A 运行改动、交付的检查器与当前已有运行记录。只读审查；未运行 Gradle、GameTest、客户端或 Git 写操作。本报告限于执行者技术审查，不改变项目状态或代替项目经理验收。

## 结论

当前 Java 与静态资源未发现新的阻断性问题。C 已落实端口实体贴图、UV 保持、端口内封面、外壳/控制器封口、轴承径向间隙与三档动态 partial 资源；A 的方向映射、控制器形状和 partial 提前初始化符合任务合同。当前可确认代码/静态资源审查通过，最终 C 对应 JAR 与隔离客户端加载证据尚待 A 交付复核；客户端加载成功也不能证明真实世界中的纹理、朝向、遮挡与碰撞视觉体验，六项人工外观门仍需在游戏内确认。

## 重点核查

- 检查器会遍历汽轮机目录中的 294 组包装 JSON 与 OBJ，要求 OBJ 材质有对应 MTL 和可达贴图；6075 个 OBJ 面逐顶点比对显式法线与几何绕序，并核对法线长度。圆端共享点 UV 逐点一致；圆周 UV 变换也由生成器级八边形扇形负例探针验证。
- 实体面 PNG 检查对每个非玻璃材质要求 alpha 全不透明；当前 MTL 中仅 `glass`/`glass_edge` 声明 `d 0.53`，其余材质无透明度覆盖。轴承检查分别读取 front/rear 的 shaft 与 inside 轴向圆柱顶点半径，并要求内壁最小半径大于轴身最大半径至少 `0.005`；背侧端口封面也有真实 OBJ 顶点/法线断言。交付 JSON 的面数、贴图面数和 partial 列表均为非空实计数。
- `rotor_blades_d3/d5/d7` 是检查器内明确的预期清单，代码字符串及对应包装 JSON 均需存在，不再依赖之前无法匹配字符串拼接表达式的正则扫描。报告附的 9ac94ae 基线负例有五项实际失败：缺 `rotor` 材质、圆端扇形 UV 断裂、独立外壳端盖缺失、控制器背面缺失、轴承与轴身共半径/重叠。
- 唯一检查器覆盖缺口：审计器读取 MTL 的 `d/Tr` 后只验证数值范围 `[0,1]`，未对非玻璃材质强制透明度为 `1`。当前 `turbine.mtl` 的实际数据没有该问题，且所有实体 PNG 透明度检查已通过；建议后续将此项补入检查器/反例，但不构成当前资源的静态阻断。
- C 离线预览确实使用安装后 OBJ 的逐面 UV、MTL PNG 并进行背面剔除，覆盖端口正反/侧下、独立外壳和控制器的背面/底面、前后轴承。图像作为网格与纹理核对有效，不是 NeoForge 游戏画面。
- A 源差异将普通进/排汽口 `OUTWARD` 设为玩家最近视向的反向，保持成型扫描后的合法外向；普通输出轴端向状态合同未改。控制器选取/碰撞按薄面板和中央盒体并集建形，四侧定位外形使用同等两层结构。`RegisterRenderers` 显式触发三档 `PartialModel` 静态初始化，符合本地 NeoForge/Flywheel 生命周期诊断。名称改为“汽轮机动力输出轴”/“Turbine Power Output Shaft”，注册 ID 保持不变。
- A 已有记录的定向 GameTest 9/9 通过，本审查未重跑。A 较早的资源打包/客户端探针在 C 冻结前完成，不能作为最终冻结资源证据；待 A 提供新打包及隔离客户端探针后，只需核验日志路径、制品哈希/内容关联和汽轮机 OBJ/partial 加载错误，不重跑 Java GameTest。

## 依据与边界

已读取并应用 `AGENTS.md`、`docs/project-governance.md` 第 5.1 节、01D 任务卡、既有初审/最终差异审查，以及本机 `minecraft-modding`、`minecraft-testing`、`minecraft-resource-pack` 技能。技术基线遵循任务卡所载 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280；未按技能示例改变项目依赖。

关键证据：`build/reports/extension/EXT-B-TURBINE-01D-FINAL-ART/EXT-B-TURBINE-01D-FINAL-ART-REPORT.md`、`EXT-B-TURBINE-01D-FINAL-ART-checks.json`、`EXT-B-TURBINE-01D-FINAL-ART-preview.png`；旧基线负例 `build/reports/extension/EXT-B-TURBINE-01D-ASSETS/EXT-B-TURBINE-01D-ASSETS-baseline-negative-checks.json`；A 记录 `build/reports/extension/EXT-B-TURBINE-01D-RUNTIME.md`。

## A 最终冻结资源证据补核（2026-10-05）

A 的最终候选证据与本次审查对象一致：`assemble-FINAL-C.log` 显示 `processResources` 和 `jar` 执行、`BUILD SUCCESSFUL`，退出码 0。JAR 资源清单为 126 项（含 8 张新增 PNG），126/126 已打包且与源字节一致，缺失 0、字节差异 0；JAR SHA-256 为 `657AC1D35CE532DA93D49FB79E038F90C5310159DA3F5C0A761890C2267E14D0`。打包前后资源哈希 CSV 各 126 行，逐路径/长度/SHA 比较差异 0。

最终客户端探针日志身份为 `CNI01DProbe`，Gradle 输出的 `CNI01D_PROBE_DIR` 指向 `build/runtime-01d-client`；该隔离目录的 `logs/latest.log` 在 02:26:39 完成方块贴图图集创建，02:27:40 正常进入 `Stopping`，探针构建退出 0。日志未见汽轮机 OBJ、材质、blockstate 或 `PartialModel` 加载异常。日志仍有复合冷却剂/铀浆液体方块缺少 level 变体的既有警告，属于本任务以外的问题。

因此最终 JAR/资源一致性及 NeoForge 客户端资源加载证据现已闭合。探针未进入世界，也没有提供真实汽轮机布置画面；它只证实该冻结制品能启动并完成资源/图集加载，不替代六项缺陷的客户端人工视觉确认。

本次只读取上述证据并追加本报告；未重跑 Gradle、GameTest、客户端或资源生成。
