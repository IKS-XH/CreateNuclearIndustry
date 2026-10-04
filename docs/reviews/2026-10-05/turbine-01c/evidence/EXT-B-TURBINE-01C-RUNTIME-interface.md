# EXT-B-TURBINE-01C 任务 A/B 冻结接口（2026-10-05）

本文件只约定任务 B 生成资源时需要的 Java 方块状态、模型选择和语言键。`formed` 继续表示通过完整结构扫描并获得运行许可；新增 `located` 表示完整两端轴与连续转子列已唯一定位的搭建外观。缺控制器、壳体或端口时可有 `located=true,formed=false`；任何流体、动力与机主查询只认完整运行 Form。`located=false,formed=true` 是旧存档过渡状态，服务端重核后改写，不作为新资源常态。

| 方块 | 01C 状态 | B 的模型选择 |
| --- | --- | --- |
| 机壳、观察窗 | 既有 `formed`、`machine_facing`、`piece`；新增 `located`。不增加独立的六向状态属性。 | `located=true,piece=1..206` 时沿用 01B 的 `TurbineGeometry.piece(id)` 对应 `d{D}_{front/middle/rear}_x{x}_y{y}`，`machine_facing` 仍负责水平四向旋转；`formed` 不改变选模。`located=false` 的独立板用保留 `piece` 编码表示点击面：`0=up`（兼容旧回退）、`207=down`、`208=north`、`209=south`、`210=west`、`211=east`；独立板模型、选取和碰撞同面。旧顶部未成型模型的实体范围 y=0.8125..1，碰撞也以顶部 3/16 格为基准；其他五面旋转。物品/掉落仍用单件独立板模型。 |
| 转子 | 既有 `formed`、`machine_facing`、`diameter`；新增 `located` | `located=true` 即按 `diameter=d3/d5/d7` 绘制对应叶片 partial；`formed` 不控制叶片显示。`located=false` 用既有单件 D3 轮毂/叶轮物品与世界回退。Renderer 包围盒覆盖各直径扫掠，不引入动画。 |
| 控制器、进汽口、排汽口、输出轴 | 既有状态；新增 `located` | `located=true` 时按既有 `machine_facing`、`side` / `ring_role`、`outward`、`end` 使用 01B 运行态外观；`formed` 只控制运行。`located=false` 沿用单件回退模型与自身明确朝向，端轴外端接轴方向仍由 `end` 决定。 |

`TurbineGeometry.piece(1..206)`、几何占格、三个直径、窗口合法格和四向旋转规则不改变；B 不重新编号。`TurbineGeometry.MAX_PIECE` 仍为 206，方块属性 `PIECE` 的允许上界单独扩到 211。局部已定位时对实际存在的同机件逐件写入 `located=true`，缺格不造方块；拆一件只撤销 `formed`，若两端轴和连续转子仍完整，剩余壳/转子保持 `located=true`。失去定位依据时撤销旧定位外观，从原定位 `piece` 的几何法线选择独立板面；端盖以轴向外端面为板面。单格拆出作为物品后按新点击面放置。

独立板 `piece=0/207..211` 编码的是**世界坐标朝向**，B 选六面模型时不得再次叠加 `machine_facing` 旋转；Java 选取与碰撞也读取同一个世界面。真实几何 `piece=1..206` 继续按 `machine_facing` 转。

新增语言键（中英文均需）：`gui.create_nuclear_industry.turbine.issue.unlocated_axis`（轴列尚未唯一确定，0 参数）、`gui.create_nuclear_industry.turbine.issue.blocked`（下一位置被占用，0 参数）、`gui.create_nuclear_industry.turbine.located`（已定位档位，参数 1=`直径方块数`，参数 2=`轴向长度方块数`，例如“已定位 5×8，仍需补齐结构”）。既有 `gui.create_nuclear_industry.turbine.inspect` 及 `issue.*` 保留。下一位置的世界内轮廓由 A 的客户端代码画出，不需要纹理或新物品。

任务 B 可据此生成状态 JSON 和模型。若代码端发现本接口必须变动，A 先报告 PM，再同步 B，不静默改字段。
