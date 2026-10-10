# ART-REACTOR-03R2：共享滑块避让实施补充

> **执行：** 本文件由主PM冻结，只将下列两个共享文件在本批临时派给美术03R2已有实现执行者；美术负责人可在此准确边界内安排同批实现与一次组合窄审。不转授全局PM、Git或其他交互逻辑权限。

**任务 / 状态：** ART-REACTOR-03R2-SLIDER；窄写集已批准，实际复制到美术树后生效。尚未实现或验收，不合main。

**需求来源：** PM已直接只读核实美术对话`01a11c6b-f440-77c1-9275-5b2dca5cfa0f`的用户消息`01a12377-95bf-7782-a43a-508a1049fdf0`（2026-10-10）：“控制棒的棒体改成方形的吧，让它的截面边长为0.6格。控制棒驱动器的调整滑块需要让它离开方块正中心，避免被控制棒棒体遮挡”。用户已确认目的与尺寸，不重复请求同一授权。原03R1候选材料已收到，尚未完成主PM候选登记，其内部证据与未通过视觉门保留。

**目标：** 原Create深度控件显示和鼠标命中共同移开0.6格方杆，保留六面操作、现有百分比范围与权威提交；随美术方杆同一候选交付。

**架构：** 只替换`ControlRodSliderBehaviour`构造器中的ValueBoxTransform几何定义，专用嵌套或包内可测试入口仍位于同一文件。显示与命中消费同一个生产transform，继承锁定Create的面旋转与命中机制，不另写一套鼠标判断、不修改正式状态。

**技术栈 / Spec：** MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；上述用户原话、本文件及美术树`docs/art/tasks/ART-REACTOR-03R2.md`模型段。模型/SVG写集由美术卡管理，本补充不扩大它。

## 基线、实际API与技能

- 唯一实现目录`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`、分支`codex/art-studio`，核对HEAD`d36e79134b822a8eb8c5e058b241674e7050ad94`。主目录当前`ebfba6580057ad397bcbbede9de65eb5c4f059b9`；本次仅文档协调，不创建第二实现或构建。
- 共享源当前两树除行尾外逐字一致，主树Git blob `f7e8113ac559de8a956a545dab96eae1c223520d`。原字节SHA：main `074f6cc6268b6df4226cc75d570a761e012a512d3f54e343f8b59eb790d84893`；美术 `aef7b25252be2186b38fbffd8825b20d83a4b1b09bb60cff6aeb85e8d89afe83`。保存美术原字节，不将行尾区别误报为玩法差异。
- PM已实际读原Behaviour、驱动器完整方块模型及美术审计`build/reports/art/ART-REACTOR-03R2/audit/create-valuebox-bytecode.txt`、`create-render-input-bytecode.txt`。原CenteredSide使用south `(8,8,15.5)/16`，全部六面启用；Sided从同一south位置旋转。原生transform和testHit共同读取getLocalOffset，命中半径为构造时scale/2；ScrollValueBehaviour先减blockPos，再交给这一transform。渲染与交互同源，不可只移动文字。
- 基类构造时虚调用getScale并缓存scale；专用尺度须在此时就有效，不能读尚未初始化的实例字段。用实际生产transform的testHit验证这个边界。
- 执行者实际读AGENTS、治理1.2/5.1/5.2、本文件、美术03R2卡；应用`C:/Users/IKSXH/.codex/skills/`下minecraft-modding、minecraft-testing、minecraft-resource-pack及适用实施/验证技能。版本、中文手写注释、精简验证和Git禁令保持。

## 精确共享写集

1. `src/main/java/com/iksxh/create_nuclear_industry/control/ControlRodSliderBehaviour.java`：仅构造器选择transform、相关import及同文件的专用transform定义/中文几何说明；位置、尺度和既有六面旋转适配。允许包内测试入口，禁止新增对外状态接口。
2. 新`src/test/java/com/iksxh/create_nuclear_industry/control/ControlRodSliderTransformTest.java`：调用实际生产transform，覆盖面位置、显示变换与原生命中一致性；不以自写公式或源码字符串代替行为验证。

报告并入既定`docs/art/reports/ART-REACTOR-03R2.md`，原始审计、坐标表、检查、候选与冻结证据仅在本批`build/reports/art/ART-REACTOR-03R2/`。这两个共享文件在同批只由一个实现者持有，主PM及逻辑侧不另行修改或重复构建。

`between(0,100)`、百分比formatter、读写clientPacket条件、短交互/悬停回调、服务端commit/response以及展示值钳制全部保持。Service/Network/Protocol/ClientAdapter/BE/NBT、L2/L1/CT、碰撞、注册、控制行程/速度、锁定/SCRAM和其他设备/语言/Ponder/配置/构建脚本仍只读。仅获准的几何改动不扩大为交互语义改造。

## 实现与审查合同

- [ ] 按下面已经核实并冻结的六面坐标/尺度实现，记录到美术03R2卡或本批审计；继承CenteredSideValueBoxTransform的rotate和testHit，覆写共用getLocalOffset/getScale及fromSide中的几何缓存刷新，不另写鼠标公式。具体偏移与尺度是已批准避让目的的实现细节，不再询问用户。
- [ ] 滑块目标仍在原驱动器本格面内，原法向内缩和六面访问保持。顶面避开棒体X/Z `0.2..0.8`，并核方形端箍`0.175..0.825`在0/50/100%深度下的遮挡；侧面同样离开面中心。控件及0/50/100数值应可读、可命中，不能为避让把热点缩成近乎不可用或移到相邻格。
- [ ] 新测试读取真实生产transform并遍历六面：localOffset有限且在本格、面对齐和避中心；显示变换的位置对应同一个offset；实际面上目标点testHit为真、旧中心及明确区域外点拒绝；scale从构造起有效。正常/倒转侧面与上下旋转都覆盖，不用仅自算坐标互证。
- [ ] 同一执行者做一次新`ControlRodSliderTransformTest`定向检查及本批增量JAR，保留原始XML/exit/log。未修改的18项动画、L2与协议/控制证据复用，不再跑旧矩阵、全量、GameTest或旧存档。若有新的有名有据失败，只扩大对应范围。
- [ ] 与方杆两模型一起冻结组合候选，一次独立规格＋内部质量窄审实际看共享两文件差异、六面坐标/命中证据、OBJ边界及源/class—JAR绑定；核非几何旧方法逐字保持，原03R1历史制品不改。此处不产生第二轮重复构建或另一个相同终审。
- [ ] 用户最终在美术树观察方杆与滑块显示并实际鼠标调节。尺寸/滑块、03R1透窗/液体及02R1材质门各自记录，不因内部检查或装配台07教学关闭。

本补充只解除共享两文件的范围阻塞。若必须触及只读协议、服务端或客户端状态接口，停止受影响片段并报告实际缺口；不会因等待时间或本文件把未决玩法视为批准。

## 锁定Create审计后的坐标与缓存冻结

主PM实际核对美术`create-valuebox-bytecode`、`create-render-input-bytecode`、`create-allicons-bytecode`及`locked-icon-and-interaction-geometry.txt`。ScrollValueRenderer用wideOutline的6PX帧，而不是默认4PX，ValueBox绘制倍率2.01。以下坐标单位为本格0..1；normal仍距实际表面1/32格。

| 面 | getLocalOffset `(x,y,z)` | getScale |
| :--- | :--- | :--- |
| UP | `(0.10,0.96875,0.10)` | `0.18` |
| DOWN | `(0.10,0.03125,0.10)` | `0.18` |
| SOUTH | `(0.25,0.75,0.96875)` | `0.40` |
| NORTH | `(0.75,0.75,0.03125)` | `0.40` |
| EAST | `(0.96875,0.75,0.75)` | `0.40` |
| WEST | `(0.03125,0.75,0.25)` | `0.40` |

顶底命中球半径0.09，实际方块面可点击圆半径`sqrt(0.09²−0.03125²)=0.0844004591`，切向范围约0.015600..0.184400，离开杆体0.2..0.8。绘制6PX边框宽0.135675，范围0.0321625..0.1678375，也避开端箍0.175..0.825。侧面命中半径0.20、可见宽0.3015，目标偏上角且仍在本格。这些是锁定资源/代码下的几何依据，数字可读及鼠标体验仍由用户观察，不能据此关闭视觉门。

`fromSide(Direction)`必须先调用super更新direction，再把继承受保护scale刷新为当前getScale，返回本transform；Create原fromSide仅更方向，而transform/testHit使用缓存scale。可允许构造器内在super完成后校正初始UP缓存；getScale在direction尚为null时须安全且确定。检查初始UP、TOP→SIDE→DOWN→SIDE连续切换，不得让画面变小但命中仍停在上一个面尺度。只刷新该几何缓存，不接触Behaviour百分数value或正式控制状态。

生产实例的原生testHit检查圆内、圆外及严格边界，不能只比较getScale返回值。原生旋转和面朝向仍继承，不把鼠标交互规则复制到测试里自证。
