# ART-REACTOR-03R2：0.6格方形控制棒与避让滑块

**状态：内部候选交付及独立窄审通过，用户客户端视觉/操作待验。** 2026-10-10用户明确要求“棒体改成方形，截面边长0.6格”“驱动器调整滑块离开方块正中心，避免棒体遮挡”。这是原有控制棒/原生Create数值控件的定向调整，保留03R1历史候选与用户视觉待验状态，不涉及新增玩法。

**Goal：** 完整控制杆改为0.6×0.6格方柱；Create原生滑块显示与命中位置一起避开中央棒体，可从原有各面调节。

**Architecture：** 原OBJ/rig生成器更改控制分件截面，沿用同一材质、单位杆长和实际深度运动；滑块只换原ValueBoxTransform几何定义，深度协议/权威提交与锁定规则保持。美术负责人主持、原执行者实施，一次最终组合窄审。

**Tech Stack：** MC1.21.1 / Java21 / NeoForge21.1.219 / Create6.0.10-280 / Ponder1.0.82 / Flywheel1.0.6。

## 已确认设计与边界

- 杆身方形横截面，方柱X/Z均0.2..0.8，边长精确0.6方块=9.6模型单位；单位Y0..1由原BER按真实body行程缩放。方柱四侧及两端封闭，外法线/绕序/UV正确，不能仅把八边管外径改大或旋转成菱形。
- 固定端箍跟随方杆采用方形，边长0.65格=10.4单位、X/Z0.175..0.825，厚3/16保持；端箍仍随完整棒顶整体移动，不拉伸、不新增独立动画。比棒体略宽是美术端部选择，不改变碰撞或注册尺寸。
- 复用原控制SVG材质与钢灰纵槽/箍纹、黄铜固定端部。全部19SVG、palette、mapping、燃料/辉光/冷却液材质帧与模型不改；继续使用SVG来源，不生图或直接PNG作画。
- 滑块需要一个生产共用的Create变换同时约束显示与命中，尤其顶面避开棒体X/Z0.2..0.8；保留六面访问，不禁用顶部或把交互目标移到相邻方块。具体位置/尺度先据锁定Create源码与命中半径审计冻结，不凭图猜值。
- 原actualDepth完整升降、正确世界采光、真实库存连续混色、暂停/失效、0..100控制范围、拖动协议、服务端重检及卡死/SCRAM锁定都不改变。

## 权限与写集

唯一工作区`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，每shell显式workdir。开工只读HEAD`d36e79134b822a8eb8c5e058b241674e7050ad94`；读取AGENTS、美术入口、治理1.2/5.1/5.2、本卡与03R1候选/报告。实际应用`C:/Users/IKSXH/.codex/skills/`的minecraft-modding、minecraft-testing、minecraft-resource-pack；使用已读设计/计划/实施/验证技能，版本与中文注释合同保持。

### 已授权模型段

1. `tools/art-assets/reactor-animation/rig.json`、`generate.py`、`README.md`；原有33个generated输出允许确定性生成，仅控制shaft/head OBJ应改变，其他31保持字节。
2. 正式资源仅`src/main/resources/assets/create_nuclear_industry/models/block/reactor_animation/mesh/control_rod_shaft.obj`、`control_rod_head.obj`，逐字节从generated安装；不改JSON/MTL/PNG及其他17个03安装路径。
3. 报告唯一`docs/art/reports/ART-REACTOR-03R2.md`；基线/预览/探针/原命令/XML/候选/冻结证据仅`build/reports/art/ART-REACTOR-03R2/`。只读审计仅其中`audit/`。

### 已生效的共享滑块段

仅`src/main/java/com/iksxh/create_nuclear_industry/control/ControlRodSliderBehaviour.java`的构造器transform选择、相关import、同文件专用几何变换及中文说明，以及新测试`src/test/java/com/iksxh/create_nuclear_industry/control/ControlRodSliderTransformTest.java`。主PM的[共享补充](../ART-REACTOR-03R2-COORDINATION.md)已实际复制到美术树，负责人已完整读取；文档同步HEAD为`6631353d4a5cfc4d635572ae37e63f3d4de2ae33`，共享源原字节SHA仍为`aef7b25252be2186b38fbffd8825b20d83a4b1b09bb60cff6aeb85e8d89afe83`。由原模型执行者持有这两个路径，按卡尾冻结几何接续实施；不另行开实现或重复构建。

动画六类、旧两测试、P1Blocks、BE、Service/Network/Protocol/ClientAdapter、L2/L1/CT、其他设备/全局流体/语言和所有既有治理文档只读。无Git写、主工程写、配置/依赖/构建脚本写、客户端/服务启动、存档写或子Agent。原03A/03A1/03B/03R1/02R1报告/证据/候选/冻结清单全部只读保留，允许作者输入漂移先另存原字节。

## 必要执行与检查

1. 保存本批精确允许路径及原03R1候选SHA；先完成方杆rig/模型与原方箍UV。以实际旧/新保存OBJ及SVG材质做单杆正侧斜视、0/50/100%实际深度对照；独立解析OBJ检查方截面0.6/端箍0.65、闭合/法线/UV和原其他31generated不变。负责人实际看图后准许安装模型。
2. 审计核原CenteredSideValueBoxTransform、CreateValueBox绘制/命中/面旋转，选定显示与命中共用的位置/尺度；共享确认到位后按卡尾实施，不用等待时间当授权。
3. 滑块只新增必要真实几何/命中行为检查，调用实际生产transform核原六面与top中心避让、目标仍在本块面、命中随显示移动及百分数控制保持；只改坐标常量不机械要求整套TDD或重复原玩法。若做red必须真实断言，不用编译失败/字符串测试。
4. 同一执行者持有唯一最终新控件定向测试与增量JAR；原18动画及L2/协议/玩法未改证据复用，不重复GameTest、全量测试/构建、旧素材矩阵或旧存档专项。核2模型source→generated→安装→JAR字节、其他资源/共享源保护。
5. 冻结本批源/资源/证据，一次独立规格/质量窄审；最后交用户重启美术树复看方形尺寸、滑块可见与实际鼠标调节。03R1冷却液/02R1外观视觉门独立保持，未合main。

## 进度

- [x] 用户明确尺寸与遮挡目的已登记。
- [x] 原源/ValueBox接口只读定位，主PM共享窄写集已协调。
- [x] 方形分件与实际对照预览。负责人实际查看single-rod、depth、top-material三图；0.6方柱、0.65端箍、钢灰材质及完整升降方向通过，允许安装两模型，离线图不代替游戏/控件交互。
- [x] 滑块审计、精确位置和共享授权补充；主PM实际复制补充，负责人读取生效。
- [x] 负责人实际读取生产transform/测试、最终14秒exit0日志及6/6 XML，并查看slider-layout.png；六面同源、角落框/热点避杆与端箍、侧面原尺度方向通过。此为原生行为和离线投影核对，不替代客户端文字/手感。
- [x] 必要检查、组合候选与来源冻结；负责人实际核151交付/证据和79保护/审计依赖一致，build/libs与冻结候选相同。
- [x] 一次独立窄审与用户候选交接；规格/内部质量通过，必改无；候选说明已集中重启与尺寸/热点复看步骤。
- [ ] 用户客户端观察。

## 冻结交回与独立窄审派发

原执行者已停止实现写入，负责人实际读最终报告、原final-confirmed日志/exit及6/6 XML。冻结`build/reports/art/ART-REACTOR-03R2/frozen-manifest.json`含41交付+110证据，SHA`a502efc1f4843b6aedf0b1f09b3ea41f694af58af2cba02be7b853519396ef56`；候选2475501字节、SHA`20b7223f899f7b75b56cdfb7d8374c7c7b629b26384add13202939ebc92e97ba`，build/libs相同。仅2OBJ改变、31generated保持，75保护及共享非几何保持证据已交回。

只派原`reactor_asset_review`一次组合规格/质量窄审。其写集仅新`docs/art/reports/ART-REACTOR-03R2-REVIEW.md`及必要新增`build/reports/art/ART-REACTOR-03R2/review/`摘要，不改任何实现/原证据/冻结报告/治理或Git，不重复生成、测试或构建。审查直接核实际共享transform与原非几何差异、真实六面Pose/testHit及原生夹具、方杆OBJ边界/UV、75保护和源/class-JAR绑定；实际看四图。若发现具体问题交负责人安排对应整改，不自行实施。审查后由负责人补候选与本卡状态，用户视觉/操作门独立待验。

一次[独立报告](../reports/ART-REACTOR-03R2-REVIEW.md)已实际交回并由负责人完整读取：规格/内部质量通过、必改项无，报告SHA`0d3eb6fdbeca571cf92a070854b1d46898f8be746c90da695dfca379c369eef9`；实际四图/151冻结及生产几何/真实检查已核，未重跑构建。负责人核候选/冻结清单SHA保持相同，[候选说明](../ART-REACTOR-03R2-CANDIDATE.md)交用户重启现有存档观察；内部本批结束，用户视觉门仍未通过。

## 滑块审计后的精确几何（共享写集已生效）

负责人已实际读`build/reports/art/ART-REACTOR-03R2/audit/`中Create字节码与`locked-icon-and-interaction-geometry.txt`。`getLocalOffset`为绘制和`testHit`共用取点，`testHit`使用受保护缓存`scale`的严格球形距离`< scale/2`。ScrollValueRenderer实际调用`wideOutline()`使用6PX框，不能用基础4PX框估算。

| 面 | 方块局部中心(x,y,z) | scale |
| --- | --- | --- |
| UP | (.10,.96875,.10) | .18 |
| DOWN | (.10,.03125,.10) | .18 |
| SOUTH | (.25,.75,.96875) | .40 |
| NORTH | (.75,.75,.03125) | .40 |
| EAST | (.96875,.75,.75) | .40 |
| WEST | (.03125,.75,.25) | .40 |

采用该类内`CenteredSideValueBoxTransform`子类：覆写实际`getLocalOffset(LevelAccessor,BlockPos,BlockState)`返回上表，保留原六面激活与继承rotate。覆写`getScale()`对构造阶段direction==null安全；必须覆写`fromSide(Direction)`先调用super，再把当前getScale结果同步到受保护`scale`，返回同一实例。基类构造器缓存尺度，原fromSide只更方向；若不刷新就会出现显示变小但命中仍旧。

顶底面显示6PX框宽.135675，中心.10时范围.03216..16784，避开杆.2..8以及端箍.175..825。沿真实块面点击圆直径约.1688（面内半径sqrt(.09²-.03125²)），范围.0156..1844；整个圆与0.65方箍角落也分离，不越过块边。侧面框宽.3015、面内点击圆直径约.3951，中心.25/.75使之留在本块面内。格式化百分数与原生数值板不改，顶底较小文字/操作手感仍留客户端观察。

定向新测试直接调用生产transform的`fromSide→getLocalOffset→testHit`，覆盖六面点/原面旋转、真实块面圆内外、顶底中心杆边界不可命中、顶底/侧面连续切换的缓存刷新与direction==null构造安全；实际PoseStack显示变换与同一offset相符。无需mock renderer、旧协议测试或启动世界。共享补充已实际生效，按此表实施共享类。写前追加共享源原字节与新测试原不存在的基线；最终核原Behaviour非几何方法逐字不变，两方只做一轮组合候选与一次独立窄审。
