# ART-REACTOR-09：顶面辨识与深度控件共享补充

**状态：** 用户整改目的已确认；本批共享两路径临时写集已批准，实际复制到美术树后生效。尚未实现、打包或人工验收。美术负责人在既有专项权限内安排原消费者与素材作者，不取得全局PM或Git权限。

**来源与目标：** 主PM已直接读取美术对话`01a11c6b-f440-77c1-9275-5b2dca5cfa0f`中用户2026-10-12消息`01a12c82-7357-7011-a01a-a4f4d948a96d`：“控制棒驱动器和换料端口辨识度较低，大量聚集时无法一眼区分，并且深度调节控件字体过于小，操作不方便”。按现有无独立GUI目的做局部整改，不新增玩法、控制协议或设置方式，不重复确认已授权修复。顶盖素材由美术[09任务卡](/E:\MyMC\NewMod\Create_NuclearIndustry-art-studio\docs\art\tasks\ART-REACTOR-09.md)管理；本补充只批准下列控件几何。

## 实际基线与定位

- 唯一实现树`E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`，HEAD`2ae83b9c9d355c5229f973849e04c449dea0ceb5`；原ART08、07B-R2未提交来源、报告及制品完整保留。可变候选仍为`Create_NuclearIndustry-art-integration`，不把其玩法导入美术树。
- 主PM实际核三树原共享文件字节相同：Behaviour SHA256`5479f5f14418f4d6a499fc01a825735d4646123a018ce74176fc8605f652d83e`；TransformTest SHA256`cc740929108f0a555866893e6812d4ee06c4f03c09fe903745dd61cc7c342d24`。03R2/R3旧临时权限不沿用，本文件重新约定09范围。
- 当前顶/底锚点切向`.10,.10`、scale`.18`，四侧scale`.40`。顶面命中球半径`.09`；字体再乘Create默认`1/64`与TextValueBox适配，100%字面宽约`.07594`格。小角落与小热点共同影响可读性和长按稳定性。
- 主PM复用并实际读取锁定依赖原`ART-REACTOR-03R2/audit/create-valuebox-bytecode.txt`、`create-render-input-bytecode.txt`及03R3原生绘制审计：`getFontScale()`为public、非final；ScrollValueRenderer直接创建原生TextValueBox，ValueBox读取transform字体尺度。原生Sided.testHit先检查isSideActive；长按链继续使用Behaviour同一个testHit。无需客户端注册或自定义GUI。整体非等比缩放会拉扁文字，故仅使用等比font scale和局部命中几何。

## 临时共享写集与冻结几何

只由美术原消费者`reactor_ct_assets`持有：

1. `src/main/java/com/iksxh/create_nuclear_industry/control/ControlRodSliderBehaviour.java`：仅原`CornerTransform`的顶/底offset、scale、`getFontScale`、`testHit`及相关中文几何注释；保留原生rotate、fromSide方向/缓存路径及所有非几何方法。
2. `src/test/java/com/iksxh/create_nuclear_industry/control/ControlRodSliderTransformTest.java`：原位扩展实际生产transform与原生Pose夹具，不新增框架或依赖。

| 面 | localOffset（本格0..1） | scale | font scale | 命中 |
| :--- | :--- | :--- | :--- | :--- |
| UP | `(.5,1,.0875)` | `.20` | `1.8/64` | 下面的本格椭球 |
| DOWN | `(.5,0,.0875)` | `.20` | `1.8/64` | 同上 |
| 四侧 | 原坐标逐字保持 | `.40` | 原`super.getFontScale()` | 原`super.testHit(...)` |

顶底命中以同一个生产localOffset为中心；X半轴`.35`、Z半轴`.075`、法向半轴为当前scale/2（`.10`），严格采用归一化平方和`<1`。保留原isSideActive检查，不绕过面启用条件；输入仍是Create已减blockPos后的本格命中点。不改Behaviour的射线、长按、短交互或提交路径。

真实顶/底面上的操作区X范围`0.15..0.85`、Z范围`0.0125..0.1625`，面积约为原圆的3.24倍，避开方杆`0.2..0.8`及端箍`0.175..0.825`，不跨相邻方块。可见原生宽框约`.15075`格，Z范围`0.012125..0.162875`；100%字面宽约`.151875`格，为原两倍，字形等比。实际0/50/100%文字经原生旋转及全部后置矩阵后仍须在本格、出面并避端箍；这些推算不代替客户端Font与鼠标验收。

`direction`构造期可能为null，scale/font的默认仍须确定安全；fromSide继续刷新缓存。四侧显示与命中完整保持。中心杆体、端箍、相邻格、椭球边界和明确外点拒绝，内部点命中。

`between(0,100)`、formatter、clientPacket条件、悬停/短交互回调、commit/response、展示值钳制及其他非几何正文逐字保持。Service/Network/Protocol/ClientAdapter/BE/NBT、L1/L2/CT、碰撞、注册、控制行程、锁定/SCRAM、设备逻辑、配方/配置/构建、语言/Ponder均只读。素材作者只有美术卡独立SVG写集与后续明确的八PNG安装权限，不修改这两个共享文件。

## 必读、定向验证及交付

执行者实际读AGENTS、治理1.2/5.1/5.2、本文件及美术09卡；应用`C:/Users/IKSXH/.codex/skills/`下minecraft-modding、minecraft-testing、minecraft-resource-pack及适用排错/验证技能。MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6保持；手写注释中文、禁止Git写。

- [ ] 先用原实现运行新增真实几何断言取得业务RED，再实现窄改并运行`./gradlew.bat test --tests '*ControlRodSliderTransformTest'`取得GREEN；不用编译/环境错误充当RED，不通过删除有效断言取得GREEN。
- [ ] 原夹具覆盖六面框/字体完整后置矩阵、0/50/100%字面边界、椭球内外/严格边界、中心与邻格拒绝、原四侧退路及UP→SIDE→DOWN缓存；调用实际生产transform，不以源码字符串或复制命中公式替代行为验证。原长按链审计复用，不新建探针；若Font/完整客户端入口在JUnit不可用，保留确切限制，不能称实机操作已过。
- [ ] 原协议、服务端控制、库存及其他美术矩阵复用未变证据；没有新失败不跑全量、GameTest、旧存档或重复探针。PM不在其他树重复实现/测试。
- [ ] 原消费者持有最终定向命令和本批唯一美术组合`assemble`，待素材正式安装后执行一次。先保留原ART08/07B-R2制品，再冻结本批源、class、八PNG、SVG来源与JAR身份；专属原始证据写`build/reports/art/ART-REACTOR-09/consumer/`，交付报告写`docs/art/reports/ART-REACTOR-09-CONSUMER.md`。
- [ ] 美术负责人按09卡实际浏览器检查单格/混排大阵列、近远及杆体遮挡，对本批素材与控件做同一次独立规格/质量窄审；审查复用实际日志，不重跑矩阵。共享范围需要扩大时先报告具体缺口。
- [ ] 通过后交主PM精确净源与原始证据，再由PM管理同步至可变候选、必要的一次组合打包及Git；不夹带07B-R2或其他dirty。用户在现有存档复看辨识度、实际字体与顶面长按。

可变尺寸01、ART08及07B-R2原人工门独立保留；这次直接反馈不表示它们已通过，不推进其他工程主线。旧来源、SVG生产器和旧候选证据不改写，不操作用户世界。
