# 当前状态与验收

更新：2026-10-18。开发版本0.1.0 Alpha，首个可发布版本出口尚未完成。本文件统一维护进度、制品与未闭门，原测试数字/提交链留在证据档案。

## 主分支与正式包

- 已提交生产来源`f819fcfadf75509c7b23899daa9a7b9c59dbc56b`，合入已手测通过的首批辐射及工程师护目镜头罩。此前S1/C1、贫化铀装备与外观、队列崩溃修复、Sodium连接外观、尾热停机、管理员/闭环与其它已验机组保持。
- [正式JAR](../build/libs/create_nuclear_industry-0.1.0.jar)：3478572 bytes，SHA256 `81E7CD98D589DDC9C0AAF398C06776DC4318EA54C70CD19376F0C127E4A3DE7B`，按用户要求跟踪Git；准确已提交源一次assemble原输出，748class与1686资源全部绑定。
- 本批只处理七JSON资源并集，一次必要装备数据JUnit5/5、一次组合窄审及一次增量assemble/exit0；原辐射与头罩自动/手测证据复用，main47项无关dirty保持，见[本批验收与集成](reviews/2026-10-18/radiation-accepted-integration/INTEGRATION.md)。原[汽轮机性能](reviews/2026-10-18/thermal-performance-accepted-integration/INTEGRATION.md)、[贫铀/服务器修复](reviews/2026-10-17/du-heat-accepted-integration/INTEGRATION.md)、[渲染](reviews/2026-10-17/thermal-sodium-01/INTEGRATION.md)、[停机](reviews/2026-10-16/reactor-shutdown-01/INTEGRATION.md)及[反应堆性能](reviews/2026-10-15/reactor-performance-accepted-integration/INTEGRATION.md)验收保持。

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
| 闭环与锅炉汽口 | 应力表绑定PI控制；汽口沿合法工业管主动出汽，不需补外置泵 | [联合手测/合入](archive/2026-10-15/reviews/2026-10-14/load-follow-boiler-accepted-integration/ACCEPTANCE.md) |
| 锅炉尺寸与外观 | 三边5～16、长方体及跨区块；用户确认尺寸上限与锅炉外观手测通过，尺寸净增量已接入main | [本批验收](reviews/2026-10-15/boiler-size-accepted-integration/ACCEPTANCE.md) |
| 已验美术 | 反应堆/锅炉/汽轮机、Logo、离心机、装配台黑窗与烧结炉A版；反应堆Sodium上下文窄修 | [前批美术](reviews/2026-10-14/latest-production-art-integration/INTEGRATION.md)、[烧结炉验收](reviews/2026-10-15/sintering-art-accepted-integration/ACCEPTANCE.md)、[Sodium局部验收](archive/2026-10-15/reviews/2026-10-13/reactor-sodium-surface-01/ACCEPTANCE.md) |
| 锅炉/汽轮机Sodium渲染 | 补齐两处LevelSlice入口，用户确认本批渲染手测通过并授权合main；正式包与已测包一致 | [验收与合入](reviews/2026-10-17/thermal-sodium-01/INTEGRATION.md)、[组合窄审](reviews/2026-10-17/thermal-sodium-01/REVIEW.md) |
| 反应堆性能 | 端口索引、精确反馈缓存、客户端辉光缓存和整列低面镂空全亮；用户认可效果并要求合main，液体/控制棒保留 | [验收](reviews/2026-10-15/reactor-performance-accepted-integration/ACCEPTANCE.md)、[集成/边界](reviews/2026-10-15/reactor-performance-accepted-integration/INTEGRATION.md)；C2继续后置 |
| 汽轮机性能S1/C1 | 真实成员包围盒预筛与静态截面/坐标复用；原转子/轴改用Flywheel实例。用户采纳并授权合main；原定向/真实服/性能证据复用，具体环境及一帧瑕疵见下方 | [来源、收益与合入](reviews/2026-10-18/thermal-performance-accepted-integration/INTEGRATION.md)、[一次组合窄审](reviews/2026-10-18/thermal-performance-accepted-integration/REVIEW.md)、[已提交源/class绑定](reviews/2026-10-18/thermal-performance-accepted-integration/ARTIFACT.json) |
| 停机拆除尾热 | 冷却充足时正确识别微小安全余数，不丢缓存热或工质；55项JUnit及真实安全/拒拆通过，原16³场景用户复看另列 | [根因、验证与正式包](reviews/2026-10-16/reactor-shutdown-01/INTEGRATION.md)、[组合审查](reviews/2026-10-16/reactor-shutdown-01/REVIEW.md) |
| 贫化铀材料/装备与18B外观 | 粉炼锭、锭块存取、九种钻石装备锻造升级及专属图标/块/穿戴层/饰纹；用户确认功能与对应外观手测通过并授权合main | [联合验收](reviews/2026-10-17/du-heat-accepted-integration/ACCEPTANCE.md)、[美术来源](art/ART-DEPLETED-URANIUM-18B-ACCEPTED.md) |
| 锅炉热源刷新队列 | 原子取批和锁外刷新，重入/并发新增留到下一tick；34项定向回归及一次组合窄审保持，用户确认原整合包服务器崩溃修复通过并授权合main | [原诊断](reviews/2026-10-17/heat-pending-01/DIAGNOSIS.md)、[联合验收](reviews/2026-10-17/du-heat-accepted-integration/ACCEPTANCE.md) |
| 首批辐射与护目镜头罩 | 已用燃料历史、真实物料源/屏蔽、玩家剂量/恢复、防护服与仪表；头罩额外1护目镜并提供原生信息。用户确认原清单及头罩补充手测通过，已合main与正式包 | [联合验收/来源](reviews/2026-10-18/radiation-accepted-integration/INTEGRATION.md)、[冲突并集窄审](reviews/2026-10-18/radiation-accepted-integration/REVIEW.md) |

结论只覆盖原反馈范围，不扩大为全部配置、多人整机、所有Iris特性或发布保证。管网压力、具体事故世界效果与再处理未实现。

## 独立未闭门

| 事项 | 状态/条件 |
| --- | --- |
| 汽轮机实例化局部观察 | C1保留一个P3：渲染原点重定位时实例可能缺一帧，下一帧恢复；本批审查由调用链确认，未新增现场复现，后续仅针对visual初始化修正。Sodium/Iris/光影、真人多人和真实单机暂停未补记通过；S1旧堵塞帧耗时暂态不称已解决，详见[集成边界](reviews/2026-10-18/thermal-performance-accepted-integration/INTEGRATION.md) |
| 事故与辐射第二批 | 首批及头罩人工门已关闭并合main；按用户“继续辐射系统开发”和[第二批计划](superpowers/plans/2026-10-18-accident-execution-02.md)准备独立候选，接唯一事故事件、持久化账本、有界分批动作、残骸与废料封装出口。不得把已批准计划记成事故实现或新手测通过 |
| 生存整线衔接 | 用户选择优先完善现有整线；[只读核查和观察准备](reviews/2026-10-16/main-flow-01/READINESS.md)未发现可证实硬断链/自举死循环，列明副产物排出、装配工序、启动与工质回路等实际约束；小规模集中游戏观察尚未执行，不称新的整体验收。值班长构想后置，现行管理员应力闭环保留 |
| 超大反应堆原场景拆除复看 | 微小尾热死区已修复并接main及正式包；唯一世界原件未改。原16³、实际仪表(-8,-59,0)的用户拆除复看待反馈，不能把自动化夹具通过当作已手测。见[集成与观察边界](reviews/2026-10-16/reactor-shutdown-01/INTEGRATION.md) |
| 装配台15C动画 | 用户授权合main，自动/网页/审查通过；Minecraft制造/封装/中途停恢复尚未报告。见[观察入口](art/ART-SHIELDED-ASSEMBLY-15C-CANDIDATE.md) |
| 历史窄修观察 | 泵静止箭头/加载2048后的现场流量、冷液桶标记创造罐暂无明确独立客户端通过反馈；原管网和两端三角验收不重开。见[静止箭头/配置](archive/2026-10-15/reviews/2026-10-13/industrial-pump-idle-arrow-flow-config/CANDIDATE.md)、[桶交互](archive/2026-10-15/reviews/2026-10-12/industrial-pump-tank-interaction-fix/CANDIDATE.md) |
| 辅助工具 | 模拟器02B单文件及3D工具整体验收独立，不阻塞已验机组或自动插入主线 |
| 旧GameTest断言 | 性能候选真实服务端保留一条旧控制棒2.25HU断言失败，S1基线同样失败，当前ceil结果3HU；本次未改测试/玩法，不称全GameTest通过。见[审查](reviews/2026-10-15/reactor-performance-accepted-integration/REVIEW.md) |

管理员GUI曾有一次GL_OUT_OF_MEMORY事故，后续六分钟观察及用户反馈未复现，未定位具体源码根因，不能称已修复；仅再次发生并得到证据时恢复诊断。见[原观察](archive/2026-10-15/reviews/2026-10-14/reactor-administrator-05/OBSERVATION.md)。文档整理不关闭上述门或补写未报告操作。
