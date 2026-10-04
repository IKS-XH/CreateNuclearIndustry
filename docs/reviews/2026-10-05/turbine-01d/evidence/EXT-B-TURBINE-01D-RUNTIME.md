# EXT-B-TURBINE-01D A：方向、碰撞与客户端资源探针记录

## 代码冻结范围

基线 `9ac94ae`。A 仅改动五个 Java 文件：`turbine/TurbinePartBlock.java`、`TurbineShaftBlock.java`、`client/TurbineClientEvents.java`、`client/TurbineRotorRenderer.java`，以及 `gametest/ExtensionTurbineAssemblyGameTests.java`。未改资源、生成器、语言、docs、Git、用户世界或玩法配置。诊断详情见同目录 `EXT-B-TURBINE-01D-RUNTIME-diagnosis.md`。

- 普通进/排汽口 `OUTWARD` 由玩家最近视向的反面决定；轴列定位后原结构扫描仍写回合法外向，流体能力查询条件不变。普通输出轴的 `machine_facing` 与 `END` 未翻转，避免破坏前后端连接；其未定位模型 front/rear 修复由 B 资源承担。
- 控制器选取/碰撞与两层模型一致：普通态局部 z=0..0.08 全面板 + x/y=.12..88、z=.08..40 中央盒体；定位四侧按同一模型变换和机组朝向旋转。边缘后方保持空隙。
- 在 NeoForge 首次资源加载前的 `EntityRenderersEvent.RegisterRenderers` 显式初始化三档叶片 `PartialModel`。本地 NeoForge `ClientHooks.initClientHooks` 与 Flywheel `PartialModelEventHandler` 源码证明该时点早于首次 RegisterAdditional/BakingCompleted；当前日志并不能单独证明叶片在世界里已正确可见。

## 自动证据与失败记录

| 检查 | 原始结果与记录 |
| --- | --- |
| `./gradlew compileJava --console=plain` | 首次退出 1，只因新增 GameTest 将 AABB 误传 VoxelShape，见 `EXT-B-TURBINE-01D-RUNTIME-compile.log`；测试变量修正后 `compile-fix.log` 退出 0。 |
| `./gradlew runGameTestServer -PgameTestNamespace=create_nuclear_industry_turbine -PgameTestDirectory=build/runtime-01d-gametest-final --console=plain` | 9/9 required 通过、退出 0，见 `gametest-final.log` 和 `.exit.txt`。首次 8/9 与诊断重跑 8/9 的失败均是 MockPlayer 只设身体 yaw 未设头 yaw；本地 `LivingEntity.getViewYRot` 使用 `yHeadRot`，测试改为同步头身角度。保留 `gametest.log` 和 `gametest-diagnostic.log`，不改写失败原因。 |
| `./gradlew assemble --console=plain` | 02:06 后一次增量封包退出 0，`processResources`、`jar` 确认执行，见 `assemble-frozen.log`、`.exit.txt`。本批当时 83 个已修改 +26 个新增资源，与 JAR 逐字节比对 109/109，缺失和不一致均为 0，见 `jar-resources-frozen.txt`。该资源快照时间及关键 SHA-256 见 `resource-snapshot.txt`。**B 随后又发现并准备修复端口透明纹理缺面，因此此包不能视为 01D 最终资源。** |
| `git diff --check`（仅 A 五文件） | 退出 0；仅 Git 提示未来 LF/CRLF 转换。 |

新增 GameTest 一项使用 MockPlayer 与真实 `BlockPlaceContext` 核对进/排汽六向普通端面、四向前轴外露 Create 轴、控制器普通/定位四侧的形状包围和空隙占据。其余八项是本命名空间既有汽轮机搭建与运行生命周期覆盖；未跑无关模块或全量测试。

## 客户端资源探针与隔离事故

首次 `runClient` 探针的 init 脚本只修改 JavaExec `workingDir`，但本地 ModDevGradle 2.0.143 `RunGameTask.exec()` 后续从独立 `gameDirectory` 属性重新设置 workingDir，实际进入旧 `run`。确认唯一 JVM 标记和用户名后只结束自启 PID 30844，未打开任何世界。按全树文件时间元数据，本次误触仅 `run/config/fml.toml`、`run/logs/latest.log`、`debug.log` 及两份压缩轮转日志；`options.txt`、存档没有本次写入。不读取用户配置/存档内容，也未还原或清理。首次探针还早于 B 最后 OBJ 写入，看到旧 `rotor_middle` 缺材质错误；该结果不能评价修正后资源。原日志 `client-probe.log` 保留，强制关闭自己进程导致 Gradle 退出 1。

随后在 PM 授权下把 init 脚本改为同时设置 `RunGameTask.gameDirectory` 与 `workingDir`，并在启动前核验参数文件 `--gameDir .`、两个路径均指向 `build/runtime-01d-client`。第二次客户端在独立目录生成 `build/runtime-01d-client/logs/latest.log`，原 `run/logs` 与 `run/config/fml.toml` 时间保持首次误触时刻。资源重载到 02:08:43，日志中不再出现 `rotor_middle` 缺材质或其他汽轮机 OBJ 加载错误；旧的非本任务流体 blockstate level 警告仍存在。收到 B 发现端口透明纹理缺面反馈后，只结束双重标记的自启 PID 30188，未进入世界。原始输出为 `client-probe-final.log`、隔离目录日志，主动停止使 Gradle 退出 1。此探针只证明当时资源已加载且无汽轮机加载异常，不能证明面、UV、纹理及三档叶片在世界中正确可见；它同样早于 B 后续资产整改，不作为最终视觉验收。

实际读取并应用 `minecraft-modding`、`minecraft-testing` 与 `systematic-debugging`；按治理 5.1 仅运行汽轮机定向检查及增量构建。A 代码与九项服务端用例已冻结，B 后续资源修复只需要重新确认对应资源/JAR，不重复 Java GameTest，客户端人工视觉门仍待通过。

## FINAL-C 冻结资源集成（2026-10-05 02:26–02:27）

PM 确认 C 最终资源冻结后，先把上次隔离 `latest.log` 原样保存为 `EXT-B-TURBINE-01D-RUNTIME-client-probe-previous-latest.log`（SHA-256 `0D7DCB98F43FB5AE5E6FE93C2CBAFE1D34F00831F45AD41102B18E0CD73880F7`），再对全部本轮已修改/新增 `src/main/resources` 文件保存 `resource-hashes-FINAL-C-before.csv`。使用下列命令完成本轮唯一增量封包与客户端加载：

```powershell
.\gradlew processResources assemble --console=plain
.\gradlew runClient --init-script build/reports/extension/EXT-B-TURBINE-01D-RUNTIME-client-probe.init.gradle --console=plain
```

`processResources` 与 `jar` 实际执行，`assemble` 退出 0，原始输出及退出码为 `assemble-FINAL-C.log` / `.exit.txt`。随后 `verify-resource-FINAL-C.ps1` 逐项比对冻结时源 SHA-256、客户端结束后源 SHA-256 和 JAR 条目 SHA-256：126 个已修改/新增资源全部字节相同，其中新 PNG 8 个；源漂移 0、JAR 缺失 0、字节不一致 0。变更/新增路径集合前后也是 126/126、差异 0，见 `path-set-FINAL-C.txt`。哈希明细分别见 `resource-hashes-FINAL-C-before.csv`、`resource-hashes-FINAL-C-after.csv`、`jar-resources-FINAL-C.csv`、`jar-resources-FINAL-C.txt`，校验脚本退出 0。最终 JAR SHA-256：`657AC1D35CE532DA93D49FB79E038F90C5310159DA3F5C0A761890C2267E14D0`。本轮无 Java 改动，复用此前 9/9 GameTest 通过记录，未重跑 JUnit/GT。

八张新增纹理均在 `assets/create_nuclear_industry/textures/block/turbine/`：`turbine_bearing_support_surface.png`、`turbine_brass_surface.png`、`turbine_controller_surface.png`、`turbine_exhaust_face.png`、`turbine_exhaust_side.png`、`turbine_inlet_face.png`、`turbine_inlet_side.png`、`turbine_output_shaft_surface.png`；八张均包含在上述 126 项逐字节校验中。

隔离 init 在 `runClient` 启动前同时核验 `task.gameDirectory`、`workingDir` 均为绝对路径 `E:\MyMC\NewMod\Create_NuclearIndustry-ore-acquisition\build\runtime-01d-client`，且启动参数为相对此工作目录的 `--gameDir .`。Gradle 输出 `CNI01D_PROBE_DIR` 与实际新增日志路径 `build/runtime-01d-client/logs/latest.log` 一致，原始日志另存 `client-probe-FINAL-C-latest.log`。探针 JVM 为带专用 `cni.01d.probe=EXT-B-TURBINE-01D` 标记的 PID 20252，启动 02:26:20；确认窗口标题 `Minecraft NeoForge* 1.21.1` 且资源图集建立后，对**该 PID**发送 `CloseMainWindow`，02:27:40 日志出现 `Stopping!`，Gradle `BUILD SUCCESSFUL`、退出 0。没有加载世界；无自有客户端进程残留。

最终隔离日志 104 行，一次 `Reloading ResourceManager`，区块、流体以外的图集等资源加载到 02:26:39；汽轮机/OBJ/MTL/叶片相关警告或错误 0。日志中一条关于第三方 `ApiStatus$ScheduledForRemoval` 的 WARN 包含文字 `Error loading class`，不是 ERROR 级别且与汽轮机无关；既有铀浆流体 level blockstate WARN 仍在。日志只能证明本次未报告汽轮机资源加载错误，不能证明三档叶片已实际 baked 或进世界后几何/UV/遮挡都正确；人工客户端外观门仍待用户执行。

对旧 `run` 目录只查文件时间/大小元数据，本次探针开始（02:26）以后修改文件数为 0；`run/options.txt` 最后写入 10/1 20:34，`run/logs/latest.log` 与 `run/config/fml.toml` 仍为首次隔离失误的 02:03 时间。该结论仅是文件时间元数据检查，非字节级鉴别，也不抵消前述首次误触记录。没有读取、恢复或清理用户配置及存档。
