# 当前状态与验收

更新：2026-10-21。开发版本0.1.0 Alpha，首个可发布版本出口尚未完成。本文件统一维护进度、制品与未闭门，原测试数字/提交链留在证据档案。

## 主分支与正式包

- 已提交生产来源`3b7a7028cb24abec72a91ea7f001119228c29ba3`，本轮仅统一MIT许可、非Logo原创美术开放范围及打包声明。功能来源`2561b1d1021d4ccf52db2eabab44e1648dd16623`保持：已移除管理员应力余量闭环及应力表绑定，保留统一调棒、恒功率、核热值班、保护和续接；此前T2→T3→T4→T5→T7→T8→T9→H1优化、锅炉主动汽口及已验机组保持，不含T6、B1或事故第二批。
- [正式JAR](../build/libs/create_nuclear_industry-0.1.0.jar)：3525495 bytes，SHA256 `17A0A41616C27B4C2FDC7EE5A2E005973AB5C1EB3D1CA1BA16DB6F2B904ACB88`，按用户要求跟踪Git。准确已提交源一次增量jar/18秒/exit0，新增三份声明、NeoForge许可字段MIT；762class与前正式包逐字一致，303文本资源仅LF/CRLF差异且逐项绑定当前源，其他非声明内容不变。见[许可与制品记录](reviews/2026-10-20/mit-license/INTEGRATION.md)。管理员协议仍为5，无新增协议或玩法变化。
- 功能来源原定向JUnit48/48、真实管理员域19/19及原一轮组合窄审证据复用；本次许可变更另做一轮组合窄审、静态与包内核验，不重复业务测试或性能采样。原[闭环移除](reviews/2026-10-20/reactor-load-follow-removal/INTEGRATION.md)、[累计优化](reviews/2026-10-20/optimization-accepted/INTEGRATION.md)、[接口外观](reviews/2026-10-18/automation-art-accepted/INTEGRATION.md)、[辐射/头罩](reviews/2026-10-18/radiation-accepted-integration/INTEGRATION.md)、[汽轮机S1/C1](reviews/2026-10-18/thermal-performance-accepted-integration/INTEGRATION.md)、[贫铀/服务器修复](reviews/2026-10-17/du-heat-accepted-integration/INTEGRATION.md)、[渲染](reviews/2026-10-17/thermal-sodium-01/INTEGRATION.md)、[停机](reviews/2026-10-16/reactor-shutdown-01/INTEGRATION.md)及[反应堆性能](reviews/2026-10-15/reactor-performance-accepted-integration/INTEGRATION.md)验收保持。

## 已合入并按相应范围验收

| 范围 | 当前结果 | 证据 |
| --- | --- | --- |
| 基础生产 | 三矿、材料加工、离心富集、烧结、组件装配及生存制造 | [燃料制造](archive/2026-10-15/reviews/2026-10-03/fuel-02e/ACCEPTANCE.md)及同阶段材料报告 |
| 反应堆 | 三边5～16、可跨区块；逐列热工/控制/损伤、枯竭快捷换料、窗口与控件 | [联合验收](archive/2026-10-15/reviews/2026-10-12/reactor-variable-accepted-integration/ACCEPTANCE.md) |
| 换热与动力 | Create锅炉供热、温压锅炉、双汽库存共容积、三档汽轮机及普通汽冷凝回水 | [锅炉](archive/2026-10-15/reviews/2026-10-08/boiler-rework-01/ACCEPTANCE.md)、[汽轮机/冷凝](archive/2026-10-15/reviews/2026-10-05/condense-01/ACCEPTANCE.md) |
| 加工核热 | 顶部盆/炉存在即持续耗热，缺热液或冷液堵塞立即停热 | [工作盆](archive/2026-10-15/reviews/2026-10-09/exchanger-basin-02r1/ACCEPTANCE.md)、[烧结炉](archive/2026-10-15/reviews/2026-10-09/exchanger-sintering-03/ACCEPTANCE.md) |
| 基础封存 | 屏蔽桶、干式架、设备/零件有序配方 | [验收](archive/2026-10-15/reviews/2026-10-06/store-01/ACCEPTANCE.md) |
| 八类基础思索 | 离心机、反应堆、锅炉、汽轮机、换热器、烧结炉、装配台、贮存架 | [装配台单页](archive/2026-10-15/reviews/2026-10-10/shielded-assembly-ponder-07/ACCEPTANCE.md)、[贮存架](archive/2026-10-15/reviews/2026-10-11/dry-storage-ponder-08/ACCEPTANCE.md)及前批播放验收 |
| 专用流体管网 | 两档管阀、一种工业泵；热液/SC专用运输，现有口2048/4096mB/t | [功能/外观合入](archive/2026-10-15/reviews/2026-10-12/turbine-fluid-accepted-integration/ACCEPTANCE.md) |
| 管理员 | 三页GUI、统一调棒、恒功率、核热值班、部分缺料不停堆及原状态续接 | [联合验收](archive/2026-10-15/reviews/2026-10-14/reactor-administrator-accepted-integration/ACCEPTANCE.md) |
| 锅炉主动汽口 | 汽口沿合法工业管主动出汽，不需补外置泵；原联合交付中的管理员闭环已按2026-10-20要求移除 | [原联合验收](archive/2026-10-15/reviews/2026-10-14/load-follow-boiler-accepted-integration/ACCEPTANCE.md)、[移除边界](reviews/2026-10-20/reactor-load-follow-removal/INTEGRATION.md) |
| 锅炉尺寸与外观 | 三边5～16、长方体及跨区块；用户确认尺寸上限与锅炉外观手测通过，尺寸净增量已接入main | [本批验收](reviews/2026-10-15/boiler-size-accepted-integration/ACCEPTANCE.md) |
| 已验美术 | 反应堆/锅炉/汽轮机、Logo、离心机、装配台黑窗与烧结炉A版；反应堆Sodium上下文窄修 | [前批美术](reviews/2026-10-14/latest-production-art-integration/INTEGRATION.md)、[烧结炉验收](reviews/2026-10-15/sintering-art-accepted-integration/ACCEPTANCE.md)、[Sodium局部验收](archive/2026-10-15/reviews/2026-10-13/reactor-sodium-surface-01/ACCEPTANCE.md) |
| 锅炉/汽轮机Sodium渲染 | 补齐两处LevelSlice入口，用户确认本批渲染手测通过并授权合main；正式包与已测包一致 | [验收与合入](reviews/2026-10-17/thermal-sodium-01/INTEGRATION.md)、[组合窄审](reviews/2026-10-17/thermal-sodium-01/REVIEW.md) |
| 反应堆性能 | 端口索引、精确反馈缓存、客户端辉光缓存和整列低面镂空全亮；用户认可效果并要求合main，液体/控制棒保留 | [验收](reviews/2026-10-15/reactor-performance-accepted-integration/ACCEPTANCE.md)、[集成/边界](reviews/2026-10-15/reactor-performance-accepted-integration/INTEGRATION.md)；C2继续后置 |
| 汽轮机性能S1/C1 | 真实成员包围盒预筛与静态截面/坐标复用；原转子/轴改用Flywheel实例。用户采纳并授权合main；原定向/真实服/性能证据复用，具体环境及一帧瑕疵见下方 | [来源、收益与合入](reviews/2026-10-18/thermal-performance-accepted-integration/INTEGRATION.md)、[一次组合窄审](reviews/2026-10-18/thermal-performance-accepted-integration/REVIEW.md)、[已提交源/class绑定](reviews/2026-10-18/thermal-performance-accepted-integration/ARTIFACT.json) |
| 停机拆除尾热 | 冷却充足时正确识别微小安全余数，不丢缓存热或工质；55项JUnit及真实安全/拒拆通过，原16³场景用户复看另列 | [根因、验证与正式包](reviews/2026-10-16/reactor-shutdown-01/INTEGRATION.md)、[组合审查](reviews/2026-10-16/reactor-shutdown-01/REVIEW.md) |
| 贫化铀材料/装备与18B外观 | 粉炼锭、锭块存取、九种钻石装备锻造升级及专属图标/块/穿戴层/饰纹；用户确认功能与对应外观手测通过并授权合main | [联合验收](reviews/2026-10-17/du-heat-accepted-integration/ACCEPTANCE.md)、[美术来源](art/ART-DEPLETED-URANIUM-18B-ACCEPTED.md) |
| 锅炉热源刷新队列 | 原子取批和锁外刷新，重入/并发新增留到下一tick；34项定向回归及一次组合窄审保持，用户确认原整合包服务器崩溃修复通过并授权合main | [原诊断](reviews/2026-10-17/heat-pending-01/DIAGNOSIS.md)、[联合验收](reviews/2026-10-17/du-heat-accepted-integration/ACCEPTANCE.md) |
| 首批辐射与护目镜头罩 | 已用燃料历史、真实物料源/屏蔽、玩家剂量/恢复、防护服与仪表；头罩额外1护目镜并提供原生信息。用户确认原清单及头罩补充手测通过，已合main与正式包 | [联合验收/来源](reviews/2026-10-18/radiation-accepted-integration/INTEGRATION.md)、[冲突并集窄审](reviews/2026-10-18/radiation-accepted-integration/REVIEW.md) |
| 自动化接口17B外观 | 用户授权C稿两资源合main与正式包，复用原像素/引用/PNG预览与组合审查；未补记未报告的独立Minecraft视觉观察 | [集成与制品](reviews/2026-10-18/automation-art-accepted/INTEGRATION.md) |
| 累计管网/汽轮机/直列优化 | 用户要求合入最新H1及其真实前置链；结构/图/路径/预算复用、同tick补投与轴本机贡献撤销、换热直列减分配；普通工业管无距离/自身流量封顶，保留阀/泵/物理口额度和节点安全上限。原测试证据复用、组合窄审及准确源一次assemble；H1仅有小幅MSPT/分配收益，不称T8/T9为等负载纯提速 | [范围/证据与边界](reviews/2026-10-20/optimization-accepted/INTEGRATION.md)、[组合审查](reviews/2026-10-20/optimization-accepted/REVIEW.md)、[制品身份](reviews/2026-10-20/optimization-accepted/ARTIFACT.json) |
| 管理员应力闭环移除 | 用户明确要求直接移除main；闭环模式/绑定/PI/相关配置、NBT和显示字段及专属测试已删除。三页GUI、两种保留模式、保护、核热和续接按定向JUnit48/48、真实服19/19验证；准确源一次assemble及删除载荷核验通过，不冒称新的客户端手测 | [集成](reviews/2026-10-20/reactor-load-follow-removal/INTEGRATION.md)、[组合审查](reviews/2026-10-20/reactor-load-follow-removal/REVIEW.md)、[正式身份](reviews/2026-10-20/reactor-load-follow-removal/ARTIFACT.json) |
| MIT与原创美术开放 | 原创代码、文档及非Logo原创美术/源文件统一MIT；Logo保留所有权利，Create派生素材等第三方许可保持。源码声明、两处元数据与正式JAR一致，无玩法变化 | [LICENSE](../LICENSE)、[NOTICE](../NOTICE.md)、[集成与制品](reviews/2026-10-20/mit-license/INTEGRATION.md) |

结论只覆盖原反馈范围，不扩大为全部配置、多人整机、所有Iris特性或发布保证。管网压力、具体事故世界效果与再处理未实现。

## 独立未闭门

| 事项 | 状态/条件 |
| --- | --- |
| 管理员退出接管 | 用户2026-10-20确认事故候选拆接口后逐棒解锁通过，未将其扩大为整批验收。[两份整改候选](reviews/2026-10-20/admin-release-01/CANDIDATE.md)来源`ca9f484`/`96a8421`；原RED、稳定高输入整改及真实域6/6、7/7/exit0已核，JUnit32/32、30/30复用，唯一组合窄审无未闭代码阻断。各一次jar/exit0，候选SHA `2257F708…DF08C1D`/`4B37F69D…FE9E3499`，来源绑定保持。未合main、正式包17A不变；用户同时报告中间反应堆拆除仍被“尚未完全停机”拦截，按下行处理 |
| 事故候选停机拆除拦截 | 用户2026-10-21确认“现在不会拦截拆除了”，原中间堆拆除手测项通过，整改人工门闭合。[候选来源与证据](reviews/2026-10-20/accident-shutdown-03/CANDIDATE.md)为`37f50b9e`/3535322 bytes/SHA `A2D7EE39…B0759DF3`；仅遗留空口重绑与既有测试变更，原热工/安全门保持。原正式RED、最终1业务/11 SKIP/正常保存exit0、唯一组合窄审及一次jar身份核验复用，不重测。未将此反馈扩大为原16³尾热病例、其它事故清单或多人验收；尚未合main，正式17A保持 |
| 高温固体与有限火点04A | 用户2026-10-21确认“这一项手动测试通过了”，[04A高温固体/有限火点](reviews/2026-10-21/accident-thermal-04a/CANDIDATE.md)集中手测通过，人工门闭合。来源`9f53a7d5`/事故树`codex/accident-thermal-04a`，3554973bytes/SHA `87D81CE0…4ED4C16`；对应残骸灼烧/点燃、自然及邻水冷却、有限火点/灭火和本批保存重进清单。原JUnit9/9、真实服2业务/11显式SKIP/exit0/正常保存、一次组合窄审及准确源jar身份核验复用，本轮不重测/重打包。原世界和候选保持，未扩大为物理卸载/真人多人或未实现熔融物验收。未合main，正式17A保持 |
| 熔融物本体与持续溶蚀04B | 用户2026-10-21确认每团20加载tick移动/40加载tick溶蚀、真实废料50%向上取整且无额外体积硬上限；无来源距离/累计溶蚀量限制，至基岩/不可破坏或冷却固化，原32格爆裂及共享64世界块写/t保持。[修订设计](proposals/2026-10-20-accident-thermal-corium.md)及[04B卡](superpowers/plans/2026-10-21-accident-thermal-04b.md)已冻结，原总计划04接线停用。必要32行只读预检已核，独立世界本体/容器事务/取液与替换保护按准确写集交唯一执行者实现；事故树同目录`codex/accident-thermal-04b`/基准`9f53a7d5`，全维度保存/无客户端/lock独占已核，原世界原位保留。当前源码/必要自动验证执行中，无新候选或人工结论；主包17A与已验04A候选保持 |
| 汽轮机实例化局部观察 | C1保留一个P3：渲染原点重定位时实例可能缺一帧，下一帧恢复；本批审查由调用链确认，未新增现场复现，后续仅针对visual初始化修正。Sodium/Iris/光影、真人多人和真实单机暂停未补记通过；S1旧堵塞帧耗时暂态不称已解决，详见[集成边界](reviews/2026-10-18/thermal-performance-accepted-integration/INTEGRATION.md) |
| 事故与辐射第二批 | [原第二批证据](reviews/2026-10-18/accident-execution-02/CANDIDATE.md)保持，当前候选续至上述`9f53a7d5`固体热/火点，原`37f50b9e`拆除候选及更早候选保留。用户已确认接口解锁、中间堆拆除及04A集中手测通过，未扩大为原第二批所有未反馈清单。原d839bc7接管/账本、有界动作、封存链和最终取消证据、原JUnit18/18及FULL等待/正常重进证据复用；原手测存档未覆盖。物理卸载/真人多人等未报告范围不补记，尚未合main；事故教学仍后置 |
| 生存整线衔接 | 用户选择优先完善现有整线；[只读核查和观察准备](reviews/2026-10-16/main-flow-01/READINESS.md)未发现可证实硬断链/自举死循环，列明副产物排出、装配工序、启动与工质回路等实际约束；小规模集中游戏观察尚未执行，不称新的整体验收。值班长构想仍后置；本地管理员应力闭环已按后续明确要求移除 |
| 超大反应堆原场景拆除复看 | 微小尾热死区已修复并接main及正式包；唯一世界原件未改。原16³、实际仪表(-8,-59,0)的用户拆除复看待反馈，不能把自动化夹具通过当作已手测。见[集成与观察边界](reviews/2026-10-16/reactor-shutdown-01/INTEGRATION.md) |
| 装配台15C动画 | 用户授权合main，自动/网页/审查通过；Minecraft制造/封装/中途停恢复尚未报告。见[观察入口](art/ART-SHIELDED-ASSEMBLY-15C-CANDIDATE.md) |
| 历史窄修观察 | 泵静止箭头/加载2048后的现场流量、冷液桶标记创造罐暂无明确独立客户端通过反馈；原管网和两端三角验收不重开。见[静止箭头/配置](archive/2026-10-15/reviews/2026-10-13/industrial-pump-idle-arrow-flow-config/CANDIDATE.md)、[桶交互](archive/2026-10-15/reviews/2026-10-12/industrial-pump-tank-interaction-fix/CANDIDATE.md) |
| 辅助工具 | 模拟器02B单文件及3D工具整体验收独立，不阻塞已验机组或自动插入主线 |
| 音效离心机首样 | 用户在“制作音效”对话正式任命音效负责人并批准克制机械风首样，PM已只读核实原文，落实[专项写集](audio/tasks/AUDIO-CENTRIFUGE-01.md)。独立树`E:/MyMC/NewMod/Create_NuclearIndustry-audio-studio`/`codex/audio-centrifuge-01`，基线`a6872db1`；main已保存世界完整携带，27文件/13337721bytes逐项SHA一致且来源未改，清单在音效树build/reports/audio-workspace-20261021/world-copy.json。可由该负责人按既定素材写集派发；尚未制作/试听/接入游戏，不涉及本批事故代码或正式包，未向音效聊天自动回发 |
| 旧GameTest断言 | 性能候选真实服务端保留一条旧控制棒2.25HU断言失败，S1基线同样失败，当前ceil结果3HU；本次未改测试/玩法，不称全GameTest通过。见[审查](reviews/2026-10-15/reactor-performance-accepted-integration/REVIEW.md) |
| 累计优化原验证边界 | T9真实服34/37、H1原直列/冷凝9/14，失败均在各自精确基线存在，未扩大修复或计为通过；H1新增真实断言10/10及干净冻结世界重载另记。物理卸载、真人多人及新客户端视觉未因合入补记验收，见[本批记录](reviews/2026-10-20/optimization-accepted/INTEGRATION.md) |
| 管理员删除后客户端复看 | 本批源及真实服已验证，GUI只剩统一深度/恒功率且仍为三页、Ctrl不再绑定应力表的实际客户端复看尚待用户反馈；未重开其余已验功能或将自动结果当视觉验收，见[本批记录](reviews/2026-10-20/reactor-load-follow-removal/INTEGRATION.md) |

管理员GUI曾有一次GL_OUT_OF_MEMORY事故，后续六分钟观察及用户反馈未复现，未定位具体源码根因，不能称已修复；仅再次发生并得到证据时恢复诊断。见[原观察](archive/2026-10-15/reviews/2026-10-14/reactor-administrator-05/OBSERVATION.md)。文档整理不关闭上述门或补写未报告操作。
