# 项目文档入口

管理员[GUI／连续换料／核热值班设计](superpowers/specs/2026-10-14-reactor-administrator-gui-design.md)已获用户开工批准，沿[实施卡03](superpowers/plans/2026-10-14-reactor-administrator-gui-03.md)在原候选接续三页GUI、部分燃料耗尽不停堆、默认1HU/t核热值班和一个燃料冷启动。唯一执行者与一次组合窄审；main管理员源码/包不改，01/01R并入集中复看，02仍未派发。

Logo已获用户验收并[合入main`d8efdc9`](reviews/2026-10-14/logo-accepted-integration/ACCEPTANCE.md)，创造标签改用齿轮＋蓝核芯图标，可编辑SVG和工具保留。唯一主目录增量assemble通过，正式包为34bedc81…；原物品列表、世界与其他人工门保持。

管理员01手测的“换接口后扳手成功、护目镜不可用”已交[01R局部复看候选](reviews/2026-10-13/reactor-automation-retrofit-fix/CANDIDATE.md)`2dff787`/`5e04c667…`：安全拆壳保实际尺寸，纯冷液无核状态且容量足够时原量恢复，扳手返回实际接受结果。18/18定向、真实3/3自然退出、唯一增量包及一次窄审通过；原目录/世界继续手测，未合main，01集中人工门与02待派发状态保持。

应力余量闭环[设计02](superpowers/specs/2026-10-13-reactor-load-follow-design.md)及[实施计划02](superpowers/plans/2026-10-13-reactor-load-follow-02.md)已按用户要求形成：绑定Create应力表、PI维持可设余量（默认20%）、正最低运行HU及原库存/积热保护。只准备文档，明确等01手测通过后沿唯一执行者/一次组合窄审派发；尚未实现，不新增流量测量或库存保持模式。

反应堆自动控制[首批候选](reviews/2026-10-13/reactor-automation-01/CANDIDATE.md)已交：同级`Create_NuclearIndustry-reactor-automation`的`codex/reactor-automation-basic`、源码`4757f27`。自动化接口、燃烧室四控件、统一调棒/恒定HU/t/故障保持与15参数/有序配方已实现；定向及真实代表、增量包和一次组合审查通过，最新补充夹具保存挂起与自然退出证据分列。世界27文件/6配置已携带、原件保留。当前停[集中手测](reviews/2026-10-13/reactor-automation-01/CLIENT-CHECKLIST.md)，未合main；负载跟随与其他原人工门独立保持。下方此前管理员后置、管网未合main的准备文字是历史时点。

**更新：2026-10-12。** 已接受的反应堆可变尺寸、快捷换料、窗口与顶面控件及重进存档窄修现已[净合main `6bdef37`](./reviews/2026-10-12/reactor-variable-accepted-integration/ACCEPTANCE.md)，原定向证据和冻结组合JAR复用。此前美术集成main `fba424fc`及设备教学验收保持。当前分级流量候选待集中手测；锅炉16³上限仍是独立待验候选。

## 当前执行与接续

当前[分级流量候选a5d1fe6](./reviews/2026-10-12/tiered-fluid-flow-01/CANDIDATE.md)已完成543/543标准build及一次独立组合审查，留同级`Create_NuclearIndustry-fluid-network`，按[集中检查单](./reviews/2026-10-12/tiered-fluid-flow-01/CLIENT-CHECKLIST.md)启动手测。二/三级管阀8192/32768、单泵16384mB/t@256RPM与4096SU；现有反应堆/锅炉冷热口2048、水汽口4096，全部配置。热液/SC仅专用运输且保留Create罐，当前世界已携带。源码/JAR未合main；锅炉16³、独立美术门及用户dirty源保持。停本批人工门，管理员和管网压力不自动推进；下段未决/未实现描述保留原准备时点。

用户2026-10-12明确开始多级专用管网，热冷却液与超临界蒸汽只走专用管泵，保留Create普通/创造储罐；随后明确本批只做流量，管网压力与超压损坏后置。已确认新增工业反应堆冷热口、管网优先、管理员随后；[首批流量设计](./superpowers/specs/2026-10-12-tiered-fluid-network-design.md)列出二/三级流量、配方及锅炉工业接入建议，[锁定Create禁运核查](./reviews/2026-10-12/tiered-pipe-preparation/RESTRICTION-STUDY.md)提供真实事务边界。新增数值和接口范围待集中确认后自动派发；原锅炉温压与16³独立人工门保持，旧[分阶段准备稿](./superpowers/plans/2026-10-12-large-reactor-infrastructure-preparation.md)的首批一档建议已被替代。

用户2026-10-12明确通过可变反应堆、快捷换料、窗口/顶盖控件及重进存档复看，已按[联合验收](./reviews/2026-10-12/reactor-variable-accepted-integration/ACCEPTANCE.md)净合main `6bdef37`，同源冻结包复用，未重复测试构建或回写世界。下方旧候选等待描述为历史。用户指定的[锅炉16³上限](./superpowers/plans/2026-10-12-boiler-size-limit-01.md)已交[候选 `30020e4`](./reviews/2026-10-12/boiler-size-limit-01/CANDIDATE.md)，29/29定向、真实3/3及独立窄审通过，新范围独立留候选手测；按用户答复继续当前测试存档。

候选进入原存档的换热器弱集合并发故障已交[窄修候选5d01314](./reviews/2026-10-12/exchanger-load-crash-01/CANDIDATE.md)，30/30定向、真实Basin7/7、唯一增量打包及独立窄审通过。现候选JAR仅桥class改变，快捷换料/ART09输入保持；从同一候选目录重启并进入原世界复看，本故障及其他人工门分别保持，未合main。

用户2026-10-12指定[换料端口枯竭组件快捷替换](./superpowers/plans/2026-10-12-refueling-quick-swap-01.md)，在现有可变尺寸候选树实施；仍有耐久者禁止替换，空槽装料和机械臂规则保持。已到[候选客户端门](./reviews/2026-10-12/refueling-quick-swap-01/CANDIDATE.md)，源码`d811f84`、12/12事务与最终1/1真实服务端、必要增量打包及一次独立窄审通过。ART09顶盖辨识/深度控件也净接入同包，原证据复用；在现有候选集中复看，各项人工门分别保持，未合main。

用户2026-10-12要求的[反应堆可变尺寸01](./superpowers/plans/2026-10-12-reactor-variable-size-01.md)已交[候选`0840906`](./reviews/2026-10-12/reactor-variable-size/CANDIDATE.md)：宽高深各5～16整数，允许跨区块，棒列规则及既定容量/热工保持。完整514/514、审查窄修后51/51与真实安全探针通过，独立审查已关闭必改项；主世界已备份并携带。请从同级候选目录按[集中检查单](./reviews/2026-10-12/reactor-variable-size/CLIENT-CHECKLIST.md)启动；main正式功能仍5³，源码待本批人工验收后合入，其他主线暂停。

用户2026-10-11确认锅炉美术重置验收通过并要求合入：05B连接外观/观察窗与06R1汽雾、两种蒸汽动画已净合main `81a161e`，见[验收与合入](./reviews/2026-10-11/boiler-art-integration/ACCEPTANCE.md)。唯一4秒增量assemble通过，主目录JAR已更新，原包备份；本批整体视觉门关闭，其他工程主线暂停。

用户2026-10-11报告“存储架的思索也测试通过了”，[干式贮存架思索08](./superpowers/plans/2026-10-11-device-ponder-08-storage.md)已[单页播放验收](./reviews/2026-10-11/dry-storage-ponder-08/ACCEPTANCE.md)并净合main `d2b9345`。七路径与已审候选`f87185a`一致，复用原10/10定向检查、最终增量打包及一次独立窄审；主目录JAR已更新，原制品留作备份。本页只讲存取与自动化用法，正式贮存功能不改，其他工程主线仍暂缓。

主分支加液遮挡控制棒故障已按[REACTOR-ROD-COOLANT-01验收](./reviews/2026-10-11/reactor-rod-coolant-01/ACCEPTANCE.md)关闭：修复main `79ca4e4`，29/29定向检查、增量JAR及独立差异复核通过，用户2026-10-11回复“这次对了”确认本故障视觉正确。原证据复用，未重复测试/构建，不覆盖其他独立美术视觉记录。

主分支完整build故障已按[MAIN-BUILD-01验收](./reviews/2026-10-11/main-build-01/ACCEPTANCE.md)关闭：main `8fa6363`修复测试报告目录依赖，454项全部通过，美术净同步`3e3045f`。该次制品身份与原集成打包记录保持历史；当前JAR身份以[锅炉美术集成验收](./reviews/2026-10-11/boiler-art-integration/ACCEPTANCE.md#当前制品)为准。

| 工作 | 状态与入口 |
| :--- | :--- |
| 锅炉美术05B＋06R1 | 用户整体视觉验收通过，连接外观、观察窗、加浓汽雾及两种蒸汽动画已[合入main](./reviews/2026-10-11/boiler-art-integration/ACCEPTANCE.md) `81a161e`；原测试/窄审复用，仅新增一次增量打包 |
| 锅炉汽雾容量读数前置 | [实际HANDOFF](./reviews/2026-10-11/boiler-steam-capacity-view-01/HANDOFF.md)已交：仅公开已有同步容量，唯一3秒jar、ABI/字节码及窄审通过，main `eb196c6`、美术 `a3540b2`；06可接通，不扩网络或追认视觉通过 |
| 锅炉美术共享显示前置 | [实际HANDOFF](./reviews/2026-10-11/boiler-surface-display-01/HANDOFF.md)已交：29/29、1/1真实探针及独立窄审通过，main `eb3ec43`、美术`53d699b`，05B可接独立CT消费者；玩法及最终视觉门保持 |
| 干式贮存架思索08 | [单页播放验收](./reviews/2026-10-11/dry-storage-ponder-08/ACCEPTANCE.md)通过，手动存取、漏斗自动化、占用及满架等待；main净教学提交`d2b9345`，原自动检查与独立窄审复用 |
| 燃料烧结炉思索06 | [两幕播放验收](./reviews/2026-10-10/fuel-sintering-ponder-06/ACCEPTANCE.md)通过；复用3/3定向检查、增量assemble与一次独立审查，8路径一致，main净教学提交`57763f8` |
| 屏蔽装配台思索07-R1 | [单页播放验收](./reviews/2026-10-10/shielded-assembly-ponder-07/ACCEPTANCE.md)通过：仅搭建与接口，不讲具体配方；7路径与已审候选及冻结源SHA一致，复用3/3定向合同、增量打包与独立窄审，main净教学提交`3f17657` |
| 汽轮机思索04 | 原三幕及`a94f2ea`取景/文案[播放验收](./reviews/2026-10-09/turbine-ponder-framing/ACCEPTANCE.md)通过，净教学整合main |
| 美术并行工作 | [美术负责人入口](./art/README.md)：独立工作树，专属规划/派发，跨逻辑协调与最终Git集成由本PM主持 |
| 普通冷热冷却剂材质/动画04 | 已按用户授权[合入main](./reviews/2026-10-11/art-integration/INTEGRATION.md)：八帧固定循环、alpha255；[原候选来源](./reviews/2026-10-10/coolant-fluid-art-04/HANDOFF.md)和视觉记录保留 |
| 反应堆内部运行动画03 | 已按用户授权[合入main](./reviews/2026-10-11/art-integration/INTEGRATION.md)，包含03R3液体浸泡、完整方杆和顶面控件；复用[原组合候选](./reviews/2026-10-10/reactor-animation-03/HANDOFF.md#03r3组合候选登记)的23/23定向检查及独立窄审，不补写游戏视觉/鼠标通过 |
| 反应堆运行动画共享前置 | 用户直接确认动画及按库存连续混色；[L2实物API/写集交付](./reviews/2026-10-10/reactor-runtime-display-02/HANDOFF.md)自动与独立审查通过，main `cb123c9`、美术净同步`4cb0d7f`；美术按边界接运行消费者，动画与02R1视觉门独立保留 |
| L2真暂停租约窄修 | [R2任务](./superpowers/plans/2026-10-10-reactor-runtime-display-02-pause.md)自动与独立窄审通过，main `93d0a20`、美术净同步`2bece5b`；10/10定向生命周期、一次增量assemble和12/12源核对；最新冻结/API见HANDOFF R2，暂停/恢复观察并入美术视觉门 |
| 成型反应堆连接纹理 | 02R1最终材质/窗簇消费者已[合入main](./reviews/2026-10-11/art-integration/INTEGRATION.md)，复用17/17定向检查及独立窄审；[L1接口](./reviews/2026-10-09/reactor-surface-display-01/HANDOFF.md)和原视觉记录保持 |
| 换热器工作盆供热 | `ab91356`实现及复核通过，用户手测通过，见[验收](./reviews/2026-10-09/exchanger-basin-02r1/ACCEPTANCE.md)；持续超级加热，默认4mB/t，缺液或冷满立即停热 |
| 换热器烧结炉供热 | `094a925`实现/复核及3/3真实测试通过，工时/物流不变；[用户手测验收](./reviews/2026-10-09/exchanger-sintering-03/ACCEPTANCE.md)通过，净功能整合main |
| 换热器分情景思索 | 五幕及R4剖面复看[用户手测通过](./reviews/2026-10-09/heat-exchanger-ponder-05/ACCEPTANCE.md)，复用定向合同、增量打包及独立复核；main净教学提交`b036776` |

主目录现提供已验收供热、八类设备教学及本批合入美术；可直接使用主目录`build/libs/create_nuclear_industry-0.1.0.jar`。逻辑与美术工作树保留来源及历史证据，各目录仍分别使用自己的运行配置和世界，不自动回写候选存档。

## 权威文档

| 入口 | 职责 |
| :--- | :--- |
| [实施路线图](./implementation-roadmap.md) | 当前阶段、已通过批次、未完成范围及人工门 |
| [项目策划](./project.md) | 现行玩法与明确的后续边界 |
| [反应堆局部控制](./reactor-local-control-revision-design.md) | 棒列邻接、反馈、热工、损伤与停堆合同 |
| [内容清单](./content-catalog.md) | 注册身份、资源、教学覆盖；阶段目标不等于已实现 |
| [配方表](./recipes.md) | 材料工序及现行配方规则 |
| [服务端配置指南](./server-config.md) | 锅炉、换热器、汽轮机配置键、单位和默认值 |
| [治理协议](./project-governance.md)、[根AGENTS](../AGENTS.md) | PM/执行者权限、技能、派发与精简验证 |
| [首发扩展入口](./superpowers/plans/2026-09-22-first-release-extension-preparation-plan.md) | 有效后续依赖与派发边界 |
| [彩蛋与进度](./easter-eggs-and-advancements.md) | 彩蛋、进度及其范围限制 |

## 仍有效的后置工作

- 反应堆可变尺寸已到候选人工门；辅助热、二级耐压、自动控制、具体事故与辐射、其他设备动画及发布完善仍按路线图安排，未实现行为不提前教学。
- [事故教学与发布总验收](./superpowers/plans/2026-09-12-post-accident-ponder-plan.md)继续保留；已有设备基础教学无需等待事故实现。
- [损伤倍率模拟器02B](./superpowers/plans/2026-09-08-damage-heat-burn-balance-plan.md)和[三维辅助工具](./superpowers/plans/2026-08-31-nuclear-plant-3d-html-visualization-plan.md)未完成整体验收，不阻塞近期任务，不在人工门等待期间自行接续。
- [生产成本草案](./superpowers/plans/2026-09-24-first-production-cost-draft.md)保留核算口径；旧比较值不是未批准的新配方授权。铅锡洗矿副产物议题仍暂缓。

换热器输入超临界蒸汽及降级供热路线已取消。现有核热换热和普通蒸汽冷凝保留，后续工作盆复用核热。首发前不研究旧存档兼容；当前版本保存加载仍按范围验证。

## 历史与验收

完成/被替代的计划见[归档索引](./archive/README.md)，验收报告保留在 `docs/reviews/`。历史待测和失败描述只代表原时点，不重新开启已关闭人工门。主要完成批次由[路线图](./implementation-roadmap.md#1-已验收范围)统一索引，不在此重复维护时间线。换机与P1证据见[2026-09-29交接](./handoffs/2026-09-29/README.md)。
