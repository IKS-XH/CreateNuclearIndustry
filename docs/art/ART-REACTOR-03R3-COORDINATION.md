# ART-REACTOR-03R3：顶面控件共享窄修补充

**状态/权限：** 主PM已直接核实用户视觉反馈与锁定Create实际绘制链，批准沿用03R2同两个共享路径；本文件实际复制到美术树后生效。仅美术03R3原执行者实施，随该批液体显示形成同一组合候选及一次独立规格/质量窄审。PM和逻辑侧不重复实现、测试或构建；不转授全局PM/Git权限。

**需求来源：** 主PM只读核实美术对话`01a11c6b-f440-77c1-9275-5b2dca5cfa0f`中用户2026-10-10消息`01a125d8-0675-7fd1-aad7-a1fbe0f59d13`：冷却液过透明、燃料应有浸泡感，控制棒浮窗只在侧面，成型后无法操作。明确要求修复原顶面目的，不新增控制机制或玩法取舍，不重复询问已授权修复。

## 实物基线与根因

- 唯一美术树`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`、HEAD`af3c36b6e52f99a871bf3d8098869a3f5f7c52d8`。共享Behaviour字节SHA256 `c6c1a349ef6e78c67fd55f71ab8fb60e0bd2d8e7cd1b68101fa7f86caadd77f0`，原专属测试`64b2ce84753d180df536f30f4d6da827441d08d775c3e2fe03f506f93d93733b`。
- PM实际读本批`audit-control/scroll-handler-valuebox-bytecode.txt`、`final-vertex-depth-probe.txt`及`native-cube-and-surface-anchor.txt`，核真实驱动器cube parent。Create在生产transform后继续`scale(-2.01,-2.01,2.01)`和框z位移-1/32；文字另有原生font scale及TextValueBox位移。R2顶面scale .18配合中心y=.96875，最终框y=.98005625、文字y=.9828125，落在真实顶面1内而被遮挡。原6/6只核变换原点/命中，不能证明最终绘制出面，历史green保留、不作为本缺陷解决证据。
- 本补充只修共用法向坐标，保留切向与尺度；不单独加绘制补偿或自写鼠标公式。完成后框/文字在真实面外，命中仍从本格真实表面进入。

## 共享写集与冻结坐标

1. `src/main/java/com/iksxh/create_nuclear_industry/control/ControlRodSliderBehaviour.java`：仅原`CornerTransform.getLocalOffset`六个法向坐标及相关中文几何注释。构造器/scale/null安全、fromSide缓存、原生rotate/testHit保持。
2. `src/test/java/com/iksxh/create_nuclear_industry/control/ControlRodSliderTransformTest.java`：原位复用真实生产transform/原生Pose扩展夹具，增加最终native框顶点和文字深度回归，必要适配真表面的原命中检查。不修改依赖或另建测试框架。

坐标单位为本格0..1，显示与命中同源：

| 面 | getLocalOffset | scale |
| :--- | :--- | :--- |
| UP | `(0.10,1,0.10)` | `0.18` |
| DOWN | `(0.10,0,0.10)` | `0.18` |
| SOUTH | `(0.25,0.75,1)` | `0.40` |
| NORTH | `(0.75,0.75,0)` | `0.40` |
| EAST | `(1,0.75,0.75)` | `0.40` |
| WEST | `(0,0.75,0.25)` | `0.40` |

真实面上的顶底命中半径.09，切向范围.01..19；到端箍.175..825最近角距离约.1061，保持避让0.6方杆与0.65端箍。框/文字的法向出面距离顶底约.01130625/.0140625格，侧面约.025125/.03125格。坐标在本格面内，框与数字可读、点击手感仍由真实客户端验收。

`between(0,100)`、formatter、clientPacket条件、悬停/短交互回调、commit/response、展示值钳制及所有非几何方法保持逐字。BE、Service/Network/Protocol/ClientAdapter、L2/L1/CT、碰撞、注册、锁定/SCRAM、控制行程、设备逻辑、配方/配置/构建、正式资源/模型/SVG均只读。液体已有客户端显示写集由美术R3任务管理；本补充不给它扩大服务端库存、capacity或投影接口权限。

## 定向验证、组合交付与人工门

执行者实际读AGENTS、治理1.2/5.1/5.2、本文件及美术R3卡，应用已安装minecraft-modding/testing/resource-pack及适用排错/验证技能；版本保持MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82/Flywheel1.0.6，中文注释和Git禁令保持。

- 先让旧R2生产变换在最终出面断言真实失败，保留red，不把环境或编译错误当业务red。测试覆盖锁定原生ValueBox与TextValueBox后置矩阵，六面框四顶点/文字基线有符号出面距离严格大于0，并核正常字体深度测试；若需要记录绘制调用，用原生入口/证据，不只比较重复公式。
- 保留真实面testHit内/外、严格边界、切面缓存/构造期scale；生产共用offset应同源，顶底切向避杆/端箍；原鼠标/射线实现不改。若原生绘制入口在无客户端环境确实不可调用，保留具体障碍及锁定字节码依据，由独立窄审明确证据界限，不宣称客户端通过。
- 与美术液体段同一最终定向命令：只跑受影响VisualState和ControlRodSliderTransformTest以及一次增量jar；复用未变材料混色/L2/协议及旧模型资源检查，出现新具体失败才扩大对应范围。不另做主PM重复构建或审查。
- 原03/R1/R2制品、清单和报告完整保存；本批报告/探针/源class-JAR绑定/冻结仅写美术R3既定路径。一次独立组合窄审核新增几何及测试、非几何保持、实际源/class/JAR和液体既定边界，再交PM来源登记。
- 顶面真实鼠标操作、浸泡/液体显示及02R1材质门独立待用户复看；本补充不关闭任何视觉门，也不覆盖屏蔽装配台07-R1单页教学门。源码仍未合main，不推进其他主线。
