# TIERED-FLUID-FLOW-01 管网候选与启动

**2026-10-12验收更新：** 功能通过及01R2外观复看已由用户确认，按[联合验收](../turbine-fluid-accepted-integration/ACCEPTANCE.md)净合main `151ceba`；本页候选启动、旧制品与待测叙述保留原时点，现行主目录结果以联合验收为准。

2026-10-12，主PM登记。功能源码候选`a5d1fe6`的自动及独立审查证据保留，用户本次明确除四项外观问题外功能手测全部通过。原Create模型/调色609914f的孤立管/自由端/T中心及箭头缺陷已按[01R2](../../../plans/2026-10-12-tiered-fluid-visual-01r2.md)在原树窄整改并交[新候选](../tiered-fluid-visual-01r2/CANDIDATE.md)；只保留这四项视觉复看，不重复功能测试，未合main。独立16³锅炉候选及其他已验收范围保持。

## 启动与制品

工作树`E:/MyMC/NewMod/Create_NuclearIndustry-fluid-network`，分支`codex/tiered-fluid-network-flow`；从已验收main的`07b2c1e`基线建立。已携带当前候选“新的世界”27文件、逐项SHA相同，来源原件保留，不链接或回写；见[开工记录](START.md)。

```powershell
cd E:\MyMC\NewMod\Create_NuclearIndustry-fluid-network
.\gradlew.bat runClient
```

当前01R2整改JAR在本树`build/libs/create_nuclear_industry-0.1.0.jar`，2,857,489 bytes，SHA256：

`0c963bbd0726c74b6d7a4ffaab7f686feabe9f67b4c2926605ec574c8e1d1f7b`

原功能包`364ec427…`保留于`build/reports/tiered-fluid-visual-01/pre-visual.jar`；首次视觉包和审查前后日志亦保留。当前世界直接留在原位，没有再次搬动、覆盖或回写。

原be28视觉包保留在`build/reports/tiered-fluid-visual-01r2/baseline-visual.jar`。01R2实际8/8烘焙/资源/成本定向、唯一4秒增量assemble和独立窄审通过，18来源/14制品一致；没有把旧11项静态检查冒称为本次GPU失败的覆盖，具体见新候选与原日志。下方首次功能及视觉证据保留原时点。

本批没有自动启动客户端或改写用户世界。只携带存档，未复制来源实例配置；本树首次启动生成自己的配置，世界内若有同名serverconfig仍优先。现有热液与SC管线需改用专用管及工业泵；水、冷冷却液及蒸汽仍允许原版输送。

## 本批参数与边界

| 部件 | 默认流量/动力 |
| --- | --- |
| 二级强化钢管/阀 | 每物理节点8192mB/t |
| 三级陶瓷内衬管/阀 | 每物理节点32768mB/t |
| 唯一工业流体泵 | 16384mB/t@256RPM，4096SU；按转速线性增长并封顶 |
| 现有反应堆冷/热口、锅炉热/冷口 | 每物理口2048mB/t |
| 锅炉给水/蒸汽口 | 每物理口4096mB/t |

数字均为SERVER配置，实际键见[配置表](../../../../../server-config.md)。管网不提高设备热功率或汽轮机耗汽上限，不新增工业口/储罐/分级泵。泵只留额定1tick周转量，管段不存液；目标短收、停转、堵塞与配置缩小保留同份余量。阀0～100%开度及红石关闭优先。热冷却液和SC只经专用管泵运输，Create普通/创造罐仍可存储；不新增管网压力、泄漏、破裂或管理员。

本树锅炉尺寸仍是已验收main默认5..11及原校验。`30020e4`的16³尺寸源码和人工门是独立批次，未借本管网候选合入。

## 已核证据及限制

- 最终标准`./gradlew.bat build`成功，99 XML/543项、失败/错误/跳过0、21秒、exit0。首次完整build的旧128默认断言失败保留；PM只扩卡改为已批准2048后第二次增量build关闭，不隐去失败或称仅构建一次。
- 正式工业域20/20自然保存退出0；十个真实口、两有效路线五tick各20480mB/t、有限总量160000守恒；实际锅炉水汽4096、冷热2048及同口主被动共享额度。
- 原代表域汽轮机12、汽种6、换热器回路4真实通过；定向JUnit83/83。审查发现的绕行、多面、红石边界和创造缓存均有对应定向真实证据；第二汽轮机入口用未满容量的同tick采样证明共享额度。
- 四项P2在同一次审查差异复看关闭，无剩余必改项；见[独立审查](REVIEW.md)。102源码/资源/工具路径SHA与冻结来源一致，其中被忽略的新IndustrialFluidConfig已单路径准确纳入Git；117个独立编译类/资源entry与JAR字节一致，临时结构域0。未再构建或重复已过组。
- 未运行的原BoilerMultiportStop、BoilerTurbineFlow、P1Coolant05Coverage、ExtensionBoiler等长工况只计受影响源码适配/编译，不冒称旧全部GameTest通过。完整过程、原日志/XML和制品清单见[执行报告](IMPLEMENTATION.md)及本树`build/reports/tiered-fluid-flow-01/`。

主目录用户/其他任务dirty的project.md、模拟器、日志与艺术稿保留；候选自动生成日志/pycache也未夹带。PM只管理计划、文档/Git与审核，生产/测试由唯一执行者完成，review未重跑测试。没有新旧存档兼容研究，也没有主目录制品替换。

## 当前需要的人工门

视觉整改最终11/11定向检查通过；首次必要增量assemble退出0，同一次审查发现阀指针被实心面/透明窗口深度遮挡，窄修三轴矩阵与准确`minecraft:cutout_mipped`后第二次必要增量退出0。154来源与143独立JAR条目一致；原543/543及热工真实组复用，不是本轮重跑。具体来源、许可证、失败历史和实际范围见[视觉执行报告](../tiered-fluid-visual-01/IMPLEMENTATION.md)与[同一次窄审](../tiered-fluid-visual-01/REVIEW.md)。

按[集中检查单](CLIENT-CHECKLIST.md)复看新增部件放置/扳手/开度、实际输送与禁运、Create罐联动、泵缓存搬移、正常重进和受影响Ponder的局部画面。只用现有小机组即可，不要求先搭16³锅炉或重新验八类教学。客户端模型、Ctrl选取按键与鼠标可读性不能由服务端/静态检查代替。

用户明确通过后再按实际范围净合main；当前保存候选及原证据，停止本批自动推进，不进入管理员或管网压力。独立美术已授权范围保持，汽轮机只读动画前置尚未交API，不以此候选关闭其视觉门。
