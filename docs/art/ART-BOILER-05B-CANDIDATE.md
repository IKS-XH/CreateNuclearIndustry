# ART-BOILER-05B：A横箍压力舱候选与客户端复看

**现行状态：2026-10-11。** 用户在主对话确认锅炉美术重置通过验收，本批与06R1整体视觉门关闭，源码、素材和资源已合入main `81a161e`。从主目录使用更新后的JAR；来源、复用证据及制品身份见[验收与集成](../reviews/2026-10-11/boiler-art-integration/ACCEPTANCE.md)。以下待验/美术目录启动说明是原候选历史，不重复要求手测。

用户已选择第一稿A及其连接外观。本批为暖钢灰锅炉外壳、贯穿横箍、窄耐火嵌片、外周钢收边、去内框连窗和保留标志的功能口；与反应堆的冷色装甲保持区别。低分辨率源仍为SVG，游戏PNG由其确定性导出。

**当前状态：** A方案内部交付完成：21PNG、窗口六面模型、三客户端CT类及两专属测试已完成，10/10定向检查、唯一增量assemble和同一轮独立规格/质量审查通过，必改项无。等待用户在实际客户端确认外观；源码与资源尚未合入main。

唯一实施/启动目录为 `E:/MyMC/NewMod/Create_NuclearIndustry-art-studio`。共享源码净同步 `53d699b`，最终交接文档同步 `9878fd8`；实际API/写集见[HANDOFF](../reviews/2026-10-11/boiler-surface-display-01/HANDOFF.md)，美术范围见[05B任务卡](tasks/ART-BOILER-05B.md)。不在主目录启动以此验证未集成的本批材质；本批未启动用户Minecraft或更改存档。

素材离线预览：[锅炉正式素材与连接图集](http://127.0.0.1:8735/tools/art-assets/boiler-art-05/preview/index.html)。它展示SVG同源素材、单窗/2×2/L形连窗，不代表游戏采光、atlas加载或鼠标体验通过。旧三稿与连接稿保留。

## 最终绑定

- [x] 三客户端类、两专属测试的必要定向检查。
- [x] 唯一增量 `build/libs/create_nuclear_industry-0.1.0.jar` 与源码/class/21资源/窗口模型绑定。
- [x] 同一轮独立规格/质量窄审完成；复用已读未改素材与共享前置证据。
- [ ] 用户实际客户端视觉确认。

原始结果见[消费报告](reports/ART-BOILER-05B-CONSUMER.md)与[同轮独立审查](reports/ART-BOILER-05B-REVIEW.md)。唯一命令为 `./gradlew.bat test --tests '*BoilerConnectedTextureBehaviourTest' --tests '*BoilerConnectedTexturesTest' assemble --console=plain`，26秒退出0；两份原始XML各5项，失败/错误/跳过均0。共享前置29/29、1/1真实探针及审查沿PM原证据复用，没有重跑。

候选[JAR](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-BOILER-05B/consumer/frozen/create_nuclear_industry-0.1.0.jar)与本树build/libs均为2,528,938字节，SHA256 `d9745639811f57b9b5f164f46c943f6335339c83e62903ea3b40114622818a51`。[清单](/E:/MyMC/NewMod/Create_NuclearIndustry-art-studio/build/reports/art/ART-BOILER-05B/consumer/candidate-manifest.json)绑定5源、8实际编译类、22资源（21PNG及窗口模型）及12共享依赖，当前SHA256 `469e5ae0c19cbafba295a78c9e6a88b157f8bc98cea4fe4dbb7d234105ce2274`；最终审查报告SHA256 `8b47a19e335f398ed16f33dc815de78ae851e1a60a323384bee5341410b23ad0`。

审查后仅第124行注释准确性更正，行数/运行时代码不变；旧源与清单保留于consumer/before-comment-correction，当前源码冻结身份已更新。class/资源/JAR/测试复用，没有为注释重测或重建。负责人已读取原日志/XML和完整审查，并一次实核5源、当前清单及两份JAR身份一致；8类/22资源的JAR绑定复用同轮独立审查，未建立全仓重复矩阵。

## 客户端复看

在本美术树按原启动方式运行 `./gradlew.bat runClient`，进入现有存档。无需为本批重建其他设备。

1. 对照独立方块与已成型锅炉：钢壳颜色、凹槽暗线和压边高光保持一致；成型后内部逐格厚框消失，外围及转角仍有厚钢边。
2. 观察墙、顶、底和实际再热隔层：细横焊缝连续，主横箍按真实边界及隔层位置出现；现有不同尺寸结构应自然延展。
3. 观察相邻窗口及单窗、长条或L形缺角：连续区域没有内部框/实体隔条，外法兰和缺角收边保留，透视内部/水位正常。
4. 观察控制器、水/汽/冷热口和安全阀：原标志、朝向、接口和四段阀体保持可辨认；停机/待液时仍保持成型外观。正常拆坏、修复或重进世界后，连接应分别撤销、恢复。

本次视觉结果只登记锅炉05B；原反应堆、普通冷却剂或设备教学的历史结果保持独立。Git与主分支集成交主PM或用户，未通过此门时不自动推进其他设备。
