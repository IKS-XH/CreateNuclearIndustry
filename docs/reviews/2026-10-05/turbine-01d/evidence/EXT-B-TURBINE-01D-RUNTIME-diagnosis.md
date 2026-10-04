# EXT-B-TURBINE-01D A：运行侧只读根因诊断

基线 `9ac94ae`；2026-10-05 候选客户端日志为 `run/logs/latest.log`（01:33 启动，01:42 结束）。本阶段未启动 Gradle 或客户端，未改功能、测试、资源、生成器、docs 或 Git。

| 现象 | 证据与根因 | 边界及最小修复 |
| --- | --- | --- |
| 已定位核心转子紫黑 | 客户端日志 46–73 行：`rotor_middle.json` 的 OBJ 解析报 `The material was not found in the library: rotor`。当前 `rotor_middle.obj` 使用 `usemtl rotor`，共享 `turbine.mtl` 只有 `rotor_hub` 等，没有 `rotor`。 | 这是确定的 B 材质故障。B 校正仍被客户端资源发现的旧模型和共享 MTL，并以真实 NeoForge OBJ 解析复核。A 不以改 Renderer 遮蔽该报错。 |
| 跨格叶片 partial 可能未烘焙 | 本地 Flywheel 1.0.6 `PartialModelEventHandler.onRegisterAdditional` 只枚举事件当时的 `PartialModel.ALL`；`onBakingCompleted` 才赋烘焙模型。叶片三项只在 `TurbineRotorRenderer.BLADES` 静态字段建立，而 `TurbineClientEvents.registerRenderers` 原来仅传构造器引用。项目中 Centrifuge 与 ShieldedAssembly 的对应事件会显式读取静态 partial 字段。NeoForge 21.1.219 `ClientHooks.initClientHooks` 明确注释为“首次资源加载前”，其中先发 `EntityRenderersEvent.RegisterRenderers`；因此此处显式初始化确实早于首次模型注册/烘焙。 | 这是有源码支持的生命周期风险，但现有日志没有叶片异常的直接记录。A 最小修正是在注册渲染器时显式初始化三项 partial；随后隔离客户端资源加载核实，不把风险表述为已证实的紫黑主因。 |
| 普通输出轴端面背向玩家 | `Output.getStateForPlacement` 设 `machine_facing=player.getDirection().getOpposite()`，默认 `END=FRONT`，该状态与已定位前端的外露 Create 轴方向相合；B 的未定位 blockstate 却不分 end 选 `turbine_output_shaft_unformed`，该 OBJ 由 `output_shaft_mesh("rear")` 生成，承轴板位于局部 `z=.8125..1`，与 front 外端的 `z=0..1875` 相反。 | 状态不宜简单翻转，否则前端 `hasShaftTowards` 与两端轴列语义会改变。B 将普通 front 变体选局部 z=0 的承轴板，rear 变体仍 z=1；A 增加普通放置朝向/轴端连接断言。 |
| 普通进排汽口端面不朝玩家 | `TurbinePartBlock.getStateForPlacement` 把 `OUTWARD` 设为 `context.getClickedFace()`；玩家落地点击顶面时它为 UP 而非看向玩家。B 的普通端口 `port_mesh(kind,"left")` 端面恒在局部 -X，未定位 blockstate 仅随 `machine_facing` 水平旋转，完全不使用 `OUTWARD`。 | 本地 NeoForge `BlockPlaceContext.getNearestLookingDirection()` 来自 `Direction.orderedByNearest(player)[0]`。A 用其反向记录普通放置 OUTWARD；B 六向普通端口模型按 OUTWARD 选模，含垂直面。已定位后仍由合法结构外向覆盖，不更改能力侧校验。 |
| 独立薄板碰撞 | OBJ 直接顶点范围：UP y=.8125..1、DOWN y=0...1875、NORTH z=0...1875、SOUTH z=.8125..1、WEST x=0...1875、EAST x=.8125..1。当前 Java `independentShape` 与六面边界数值一致，`piece` 代表世界方向，未叠机向。 | 这一项现有 Java 包围范围没有证据显示错位；截图的缺面优先由 B 核对 OBJ 面绕序/UV。不能为掩盖缺面恢复整格碰撞。 |
| 控制器碰撞 | 普通 `turbine_controller_unformed.obj` 是局部 z=0..0.08 全面板，加上 x/y=.12..88、z=.08..40 的中心盒；`Controller.getShape` 原按 `SIDE` 调用 3/16 的 `sidePlate`，未定位时轴和体积均不符。已定位四侧 OBJ 由这两部分旋转而来；若直接以 .4 厚全板碰撞，又会错误阻塞边缘后方。 | A 以两组件 `Shapes.or` 构造普通与四种定位侧面，随 `machine_facing` 旋转；测试同时断言边缘后方为空、中心盒有碰撞、选取与碰撞一致。B 若调整模型包围应先同步坐标合同。 |
| 成型端轴共面闪烁/端口乱纹 | B 网格独立调查已确认轴半径 .14 与轴承内壁 .14 共面、`Mesh.face` 三角片各自 UV 归一化；运行侧暂无线索证明 Java 造成纹理错误。 | B 修网格/UV；A 不跨写集改资源。 |

先前 01C 的资源入包核对只证明 9 个已修改、20 个新增资源路径位于 JAR 内，未遍历仍被客户端发现的旧 `rotor_middle.obj`，也未调用 NeoForge OBJ 解析或烘焙；服务端 GameTest 同样不加载客户端模型。因此两份通过记录均不能排除本次材质故障。

客户端资源探针可在允许的报告目录写 Gradle init 脚本，对 `runClient` 的 `JavaExec.workingDir` 临时指向 `build/runtime-01d-client`。当前生成的 `build/moddev/clientRunProgramArgs.txt` 明确 `--gameDir` 后是相对路径 `.`；`doFirst` 必须再校验该参数及隔离路径，否则禁止启动。仅在 B 冻结后执行一次，观察主菜单资源加载日志，并用唯一进程标记及 PID/后代关系确认、关闭该次自己启动的进程；不访问用户存档或修改 `build.gradle`、用户 run 配置。最终客户端可见外观与操作手感仍以用户人工门为准。
