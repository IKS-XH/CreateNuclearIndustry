# 当前状态与验收

更新：2026-10-15。开发版本0.1.0 Alpha，首个可发布版本出口尚未完成。本文件统一维护进度、制品与未闭门，原测试数字/提交链留在证据档案。

## 主分支与正式包

- 已提交生产来源b5a36865482930e144e535bed741d1f18155591c，接入已验烧结炉A版外观，保留5ea9c50反应堆计算/客户端优化、锅炉16³、闭环及主动出汽。
- [正式JAR](../build/libs/create_nuclear_industry-0.1.0.jar)：3333622 bytes，SHA256 `78175a55e81f3e610304a6b01586898044be5febf698777793760dbfd4fbb88e`，按用户要求跟踪Git。
- 最近准确Git来源assemble 14.949秒exit0，仅12项烧结炉资源变化；699class及全部非目标载荷与前8405包逐字相同。原静态/网页/组合审查和用户手测按范围复用，见[外观集成](reviews/2026-10-15/sintering-art-accepted-integration/INTEGRATION.md)，此前[性能记录](reviews/2026-10-15/reactor-performance-accepted-integration/INTEGRATION.md)保持；不把assemble当全build或新的Minecraft验收。

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
| 反应堆性能 | 端口索引、精确反馈缓存、客户端辉光缓存和整列低面镂空全亮；用户认可效果并要求合main，液体/控制棒保留 | [验收](reviews/2026-10-15/reactor-performance-accepted-integration/ACCEPTANCE.md)、[集成/边界](reviews/2026-10-15/reactor-performance-accepted-integration/INTEGRATION.md)；C2继续后置 |

结论只覆盖原反馈范围，不扩大为全部配置、多人整机、所有Iris特性或发布保证。管网压力、具体事故世界效果、辐射与再处理未实现。

## 独立未闭门

| 事项 | 状态/条件 |
| --- | --- |
| 装配台15C动画 | 用户授权合main，自动/网页/审查通过；Minecraft制造/封装/中途停恢复尚未报告。见[观察入口](art/ART-SHIELDED-ASSEMBLY-15C-CANDIDATE.md) |
| 历史窄修观察 | 泵静止箭头/加载2048后的现场流量、冷液桶标记创造罐暂无明确独立客户端通过反馈；原管网和两端三角验收不重开。见[静止箭头/配置](archive/2026-10-15/reviews/2026-10-13/industrial-pump-idle-arrow-flow-config/CANDIDATE.md)、[桶交互](archive/2026-10-15/reviews/2026-10-12/industrial-pump-tank-interaction-fix/CANDIDATE.md) |
| 辅助工具 | 模拟器02B单文件及3D工具整体验收独立，不阻塞已验机组或自动插入主线 |
| 旧GameTest断言 | 性能候选真实服务端保留一条旧控制棒2.25HU断言失败，S1基线同样失败，当前ceil结果3HU；本次未改测试/玩法，不称全GameTest通过。见[审查](reviews/2026-10-15/reactor-performance-accepted-integration/REVIEW.md) |

管理员GUI曾有一次GL_OUT_OF_MEMORY事故，后续六分钟观察及用户反馈未复现，未定位具体源码根因，不能称已修复；仅再次发生并得到证据时恢复诊断。见[原观察](archive/2026-10-15/reviews/2026-10-14/reactor-administrator-05/OBSERVATION.md)。文档整理不关闭上述门或补写未报告操作。
