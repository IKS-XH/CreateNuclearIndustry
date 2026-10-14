# BOILER-STEAM-OUTPUT-01 只读现场诊断

2026-10-14，执行者 `/root/tiered_flow_impl`。唯一现场为 `E:/MyMC/NewMod/Create_NuclearIndustry-reactor-load-follow/run/saves/新的世界`，锅炉控制器 `(-5,-59,-16)`。结论限于 19:20:19 保存现场：**SC 汽轮机管线缺工业泵；北侧 SC 邻罐连接没有合法的专用搬运路径；NORMAL 对应库存为空。现有给水泵方向正确，没有生产源码修复依据。** 玩家实际补泵后的结果仍需观察。

## 保存与只读边界

- 日志记录 19:20:19 三维度自然保存、19:20:55 客户端停止；开工及 19:37 复核均无本候选游戏进程，仅后台 Gradle/编辑器 Java。未进入游戏。
- HEAD `c82d13b31f820f8406e7f7246042a7f8595673ea`，功能基线 `8eaafd9`；本批仅写本报告及 `build/reports/boiler-steam-output-01/**`。没有生产/测试/构建代码、配置、世界或 Git 写操作，没有运行 GameTest、JUnit、模拟游戏执行或打包。
- 冻结读取的日志、level.dat、四个近区 region 和三份实际配置共 9 文件，最终 SHA256、大小、UTC mtime 与初读全部一致。`src` 与构建源码相对 HEAD 无差异。原有根目录 logs 改动及 PM 任务卡保持。
- 原 JAR 未改：3239779 字节，SHA256 `86225aea306cb69e3f96b30384ef82c519cbadb2ef65b649d86cb0404ad6c304`，mtime 17:43:56。原闭环候选及其人工门保持。

## 真实库存和资格

锅炉保存 `LastTick=FlowTick=ConversionTick=805147`，与世界 Time 一致。水汽分区各 50 格、25 对换热器；保存容量和现场 7×7×7 几何吻合。实际锅炉配置蒸汽容量 2000mB/格，冷热口 2048、水汽口 4096mB/t；世界 serverconfig 只有 readme。控制器已设置 `Minimum=.6`、`MinimumSet=1`、控件 60，不能用配置默认覆盖此值。

| 保存字段/推导 | 数值 | 对输出的含义 |
| --- | ---: | --- |
| NORMAL mB / HU | 0 / 0 | 普通汽口的首拒为本池无汽，不取 SC 替代 |
| SC mB / HU | 83150 / 83150 | 按实际配置比焓 1HU/mB，已付热合格 |
| SC 温度 / 总蒸汽容量 | 2 / 100000mB | 与现行 SC 目标相符 |
| 共同压力 / 实际出汽下限 | .8315 / .6 | 保压余量不是当前 SC 首拒 |
| SC 可取总量 | 23150mB | `floor((83150×2−.6×100000×2)/2)`，另受每口/tick 和接收额度约束 |

本 tick 保存预算三个 450mB 分别属于冷口 `(-6,-57,-10)`、水口 `(-8,-58,-11)`、热口 `(-7,-60,-10)`，没有蒸汽口已用预算。此为保存样本，不保证重新运行时库存、压力或本 tick 额度不变化。

## 八口连接和首个拒绝位置

| 汽口 | 选择/外向面 | 实际连接 | 首个边界 |
| --- | --- | --- | --- |
| `(-8,-56,-15)`、`(-8,-56,-14)`、`(-8,-55,-15)`、`(-8,-55,-14)` | SC / WEST | 15 格陶瓷管 → `(-17,-57,-11)` 汽轮机入口 | 该工业连通分量 `pumps=[]`，没有调度发起者，尚未到能力取液/许可/交付阶段 |
| `(-7,-56,-16)`、`(-7,-55,-16)` | SC / NORTH | 直邻 `(-7,-56,-17)` 起的两格普通 Create 罐 | `pushSteam` 只推 NORMAL；SC 不能邻罐直推，标准能力也拒 SC，没有工业泵路径 |
| `(-8,-56,-11)`、`(-8,-55,-11)` | NORMAL / WEST | 直邻 `(-9,-56,-11)` 起的两格普通 Create 罐 | 对应 NORMAL 库存 0；本池 `remainingDrain=0`，未到罐接收阶段 |

两只邻罐控制器 `TankContent={}`，上格 Controller 指向各自下格；北罐没有旧普通蒸汽。四个汽轮机线路口都是 SC，没有 NORMAL/SC 混管证据；该分量没有阀、泵或泵缓存。其它附近已保存工业泵均无 Cache 字段，实际分别服务给水或冷却剂路线，不能作为此汽路动力。

汽轮机入口保存 `formed=true`、`located=true`、`outward=east`；相邻管 `(-16,-57,-11)` 从 EAST 面接入，几何合法。接收器控制器 `(-23,-57,-11)` 保存 `no_steam`、Exhaust/TurnoverSteam 0、平均流量 0，额定 216mB/t、容量 216mB。源码该入口只接 SC，符合四口选择。成型绑定、实时红石及能力代次须加载后重新确认，未调用实际 capability，不能把静态几何称为运行通过。

邻近工业泵 `(-10,-58,-11)` 保存 Speed=-256、Facing=WEST，按源码实际输出 EAST：创造水罐 `(-11,-58,-11)`（water 8000mB）→泵→陶瓷管 `(-9,-58,-11)`，再直达水口 `(-8,-58,-11)` 并向下经 `(-9,-59,-11)` 到水口 `(-8,-59,-11)`。两水口均从 WEST 面接入合法。**这是给水泵，方向正确，与 SC 汽路不连通，不应为本故障反转或拆走。** 泵 Status/ActualFlow/Bottleneck 仅随客户端包写入，不在世界保存，不能伪造其保存时实时状态。

## 对应玩家操作

1. 将汽轮机线路的现有直线陶瓷管 **`(-12,-56,-14)`** 替换为本模组工业流体泵，管路轴沿东西。现有 EAST 邻管通向四个 SC 口，WEST 邻管经 `(-16,-56,-14)` 转向北，再向下到汽轮机入口。此位置没有第三支路，拆换不会把源和目标重新短接。
2. 给这只新泵提供动力，使**实际流向 WEST（朝汽轮机）**。源码对应 Facing=WEST、Speed=+256RPM；若实际动力转向为负，应翻向为 EAST 或反转动力，使最终输出仍为 WEST。按实例配置，256RPM 消耗 4096SU，泵额定 16384mB/t；当前接收机组仍受自身 216mB/t 额定约束。观察泵实际流量和汽轮机进汽/排汽，不以能力上限当实收。
3. 保留四个线路口 SC 选择。两个北侧 SC 口若要独立入罐，须给罐留出专用管泵搬运路径，不能只贴罐等待主动灌入。普通口当前是空池；切过滤项不会把已有 SC 转为 NORMAL，不应把两种库存共接一条管来绕过。

本次没有实施玩家操作，不声称故障已手测解决，不申请生产写集。定位结果支持先补现场汽路动力；若补泵后仍停流，应采该泵实际状态/流量、源口资格及汽轮机排汽现场，再按首次拒绝继续窄查。

## 证据与实际读取

证据均在本树 `build/reports/boiler-steam-output-01/`：

- `snapshot-manifest.json`、`snapshot/**`：9 份稳定原始输入；`06-final-readonly-identity.json`：最终进程、输入不变、源码无差异与 JAR 身份。
- `saved-scene.json`：邻域 region/NBT 方块与实体；`03-endpoint-states.json`：汽口、泵实体；`saved-routes.json`、`04-route-summary.txt`：实际路线与 15 节点无泵分量。
- `05-final-scene-evidence.json`：八口邻面、控制器原账本、物理预算、给水路线、汽轮机字段及建议放泵位置的离线几何。工具只分析冻结 NBT；建议几何仅在解析器内存中核 EAST 源侧四口/WEST 目标侧入口，不是游戏复现或交易测试。
- `read_saved_scene.py`、`trace_saved_routes.py`、`finish_scene.py`：标准库只读解析工具，中文注释，不安装依赖。

实际读取 AGENTS、本任务卡，并应用 `minecraft-modding`、`minecraft-testing`、`systematic-debugging`；锁定 MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280，不升级栈。技能用于核实际能力/生命周期与证据边界，阶段1未写或运行测试。核心源码核点：`IndustrialNetworkScheduler.java:69–86/130–179`（只调度已登记泵、许可和交易）；`IndustrialPumpBlockEntity.java:34–37/62–66`（正负转向及保存字段）；`BoilerControllerBlockEntity.java:245–247/262–266/280–301`（纯库存状态、只直推 NORMAL、外向面）；`BoilerState.java:102–105/132–158`（资格、口预算、共同保压）；`RestrictedFluidHandler.java:17–27`（SC 标准禁运、内部授权）；`TurbineControllerBlockEntity.java:348–379`（入口面与 SC 过滤）。
