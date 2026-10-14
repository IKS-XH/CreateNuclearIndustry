# ART-REACTOR-08：双面观察窗与库存更新显示试用

当前状态（2026-10-12）：用户在主PM对话先报告“手动测试通过了”，随后对“可变反应堆、快捷换料、观察窗和顶面控件复看”明确回答“上述项目都通过了”。主PM已按 [组合验收记录](</docs/archive/2026-10-15/reviews/2026-10-12/reactor-variable-accepted-integration/ACCEPTANCE.md>)关闭当前组合范围及最近加载故障门；ART08对应窗口/库存显示组合的待复看状态结束，不补写所有光照、面或网络的逐项结果，其他设备美术门保持。

本组合274项src/tools已净合main `6bdef37e96a2d708be95d786e09f59adcd3a72b6`，与接受候选 `edb5b653c51867df26a2f16030f5ed52cf1923f3` 内容一致；主目录复用同源冻结JAR2,678,313字节/SHA256 `15c0ab893d080c6aa93c11f05e5e09ef55931979bd4fc8693ad502802db14ddd`，负责人已读验收并核当前主包身份。原美术冻结和失败/检查证据不改、不重复测试构建。本美术生产树尚未净同步新变量玩法，Git及跨逻辑同步仍由主PM管理。

以下为验收前的候选来源、当时包身份和原复看步骤，保留作历史，不再要求重复本次已通过的组合门。美术树本轮组合打包HEAD为 `f787aa40a323d8fdd09a0f95e5798a9a2c8e3818`，之后主PM仅净同步共享交接文档。

## 本批行为

- 观察窗真实外面优先；朝堆芯内面引用同一格已证明的反向外面，原生Create按实际渲染面处理八向连接。结构归属、代次、版本、边角及本地材质校验保持；不同反应堆不因此连接。
- 原全透明窗格新增低饱和铅灰 `#66696A`、alpha64/255；金属框、边、角保持原RGBA像素。64份16×16 SVG同源导出唯一128×128窗口图集，不改基础fallback或其他13张CT图集。
- 正常冷液输入/热液输出引起的整液闪断已按主PM[共享HANDOFF](../reviews/2026-10-12/reactor-runtime-coolant-03/HANDOFF.md)接入，净提交 `f787aa4`。纯库存服务端入口保留上次完整样本原租约，下一正式结算更新真实显示；真正失效仍撤销，不在客户端拼接假样本。原真实1/1、相关17/17与共享窄审复用。

## 来源与已核证据

[SVG及透明合成预览](../../../../tools/art-assets/reactor-window-art-08/index.html)是离线单层/双层对照，背景为矢量堆芯示意，不代表游戏GPU排序或实际光照验收。负责人已实际复看全开口及边角；新源通过64窗格/47有效上下文＋17隔离fallback检查，9280个pane像素为alpha64，7104个原框角像素保持。生成与唯一正式安装图集均为1458字节、SHA-256 `f18296427db4c106579f778e3b0443284d5fd074ebf997889d13c495f1874723`。

客户端专属检查先21项中3项真实断言RED，修复后21/21 GREEN、零失败/错误/跳过、22秒exit0；六向内外和边角运行生产Behaviour及锁定Create上下文。原始RED/GREEN分开保留，02R1旧输出不覆盖。

[消费报告](reports/ART-REACTOR-08-CONSUMER.md)、[素材报告](reports/ART-REACTOR-08-ASSETS.md)、[同一次独立窄审](reports/ART-REACTOR-08-REVIEW.md)和[活动卡](tasks/ART-REACTOR-08.md)记录精确写集及证据。旧07B-R2、反应堆L1/L2/02R1/03R3和已验收液体批次证据保持。

## 最终组合身份与交接

美术树唯一 `gradlew.bat assemble --console=plain` 自然exit0、7秒，没有重跑测试。当前 `build/libs/create_nuclear_industry-0.1.0.jar` 与[冻结组合JAR](../../../../build/reports/art/ART-REACTOR-08/consumer/final-ART-REACTOR-08.jar)一致：2,624,312字节，SHA-256 `a4b08dd0a5bebe72555dc44969553ede120080ef66ff2ab38d27974648d353c7`。CT class及窗口图集与build/JAR一致；共享5个class和新旧NBT与主PM冻结制品一致。三Java跨树仅Git行尾差异，规范化文本一致，原raw身份分别记录，不改写行尾。

[最终绑定](../../../../build/reports/art/ART-REACTOR-08/consumer/final-binding.json)、[精确美术净来源141项](../../../../build/reports/art/ART-REACTOR-08/consumer/final-net-art-sources.json)和[最终冻结](../../../../build/reports/art/ART-REACTOR-08/consumer/final-freeze.json)用于交接，最终冻结SHA-256 `1222868a903111c4a419adb0a8b416e9abf47532a10a74c34a0abd19fa2e775b`。负责人实读原始日志/exit、消费者最终绑定及原reviewer最终段；说明Minor已关闭，没有生产必改项。旧07B-R2 JAR及5源/5class/稀疏PNG保持，原1113资产只有本批观察窗图集改变，旧RED/GREEN/freeze不覆盖。

## 客户端复看入口

用户截图中的大反应堆应从 `E:/MyMC/NewMod/Create_NuclearIndustry-art-integration` 重启客户端，沿用该候选现有存档，在该目录执行 `./gradlew.bat runClient`。主PM已将08两源、唯一atlas及136项新工具来源精确复制至该可变候选，不带入07B-R2；必要唯一组合assemble为exit0、8秒。负责人实读 `build/reports/reactor-variable-size/shared-window-08-integration/assemble.log` 和exit0并核当前JAR：2,667,047字节，SHA-256 `0940bddc9a8c6d991a3d3bbd7b42e0dd3382c6d9ca3fa0f0d482682aaea8958b`。

主PM净来源提交 `fe30e83727fe6125690ee84a0a4f46855355740d`，共享源仍 `209cd816ae018f734c05f08a99f309de77d30525`；原候选manifest和JAR保留。5个工具文件仅删EOF多余空行，134来源保持原SHA，5份新旧SHA分别登记候选补充identity，不改美术freeze/Java/素材/JAR，不重生成或构建。负责人已实读候选 `docs/reviews/2026-10-12/reactor-variable-size/ART08-INTEGRATION.md`、`CLIENT-CHECKLIST.md` 和最终文档HEAD `31554094f6b8c2ae3726d4c60fd83c0872eb80a7`，核当前包SHA保持。组合来源登记完成；这不是新一轮业务测试或视觉验收。

仅复看固定尺寸反应堆及原汽轮机试用时，也可从美术树启动其组合制品。Java变更需要重启，F3+T不能代替。观察：近侧和对侧窗口内外面都按相邻窗口连续，边框仅在应有外周/缺角出现；铅灰玻璃仍能辨认燃料棒及冷却液；正常管道持续进出液时整片冷却液不再消失后重现，库存液位和冷热连续混色仍按真实值变化。

本页不处理Git合入；自动检查、浏览器合成和源码审查不关闭用户游戏视觉门，也不关闭独立可变尺寸候选的人工门。
