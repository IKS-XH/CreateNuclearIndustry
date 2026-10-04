# 锅炉01B与换热器01D候选交付

**2026-10-04更新：用户已确认联合手测通过，见[人工验收与新增端口优化](./MANUAL-ACCEPTANCE.md)。** 实现提交`d10e1af`，自动证据及[独立审查](./EXT-B-BOILER-01B-EXCHANGER-01D-REVIEW.md)复用。尚未合入main；先完成01C每口独立限流及多口反馈核查，不启动汽轮机。以下保留交付时的行为与证据说明，历史“待手测”由本条更新取代。执行基线`9e8690b`，候选仍在主工程同级目录`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`、分支`codex/ore-acquisition`。

## 本批行为

- 核换热器前蓝冷出、后橙热入，图案和箭头区分两端；顶部供热，侧面/底面不接液。最多16台同向首尾相连，冷热容量每台各4000mB汇总，单机储热与存档份额独立；拆分或收起不复制整组库存。
- 锅炉5×5×5、98壳位、边框全外壳；底面中央3×3允许1～9换热段，最多162HU/t和162mB/t产汽。三排各三台换热器可由各排两端接管，中央机取用整列库存。蒸汽口无需外部动力泵即可通过Create管道出汽，主动/被动共用256mB/t。
- 七种锅炉块重绘为现有金属面板风格，顶面双轴对称、观察窗透明；强化钢板为钢板→加坚固板→压片，不再用精密构件。
- 新管道连接生命周期已覆盖：先铺管后放换热器/锅炉成型，拆分/拆壳再恢复后自动重开接面。修复Create管方块状态曾在能力不可用时关闭后不重算的问题，不要求玩家重放管道来恢复。

## 证据索引

| 范围 | 结果 | 证据 |
| :--- | :--- | :--- |
| 增量构建与相关账本JUnit | 22/22（锅炉9＋换热13） | [首次构建/单测日志](./EXT-B-BOILER-01B-DEVICE-junit-assemble.log)、[锅炉XML](./junit/TEST-com.iksxh.create_nuclear_industry.boiler.BoilerStateTest.xml)、[换热XML](./junit/TEST-com.iksxh.create_nuclear_industry.heat.HeatExchangerStateTest.xml) |
| 锅炉真实GameTest | 16/16 | [日志](./EXT-B-BOILER-01B-DEVICE-boiler-gametest.log)：结构/旧库存、九段共享热、无泵出汽、分支、开放口、泵换向/拆除、读盘/归属、成型与管面恢复 |
| 换热器及材料GameTest | 14/14 | [日志](./EXT-B-BOILER-01B-DEVICE-heat-exchanger-gametest.log)：C最终接面修复后回归，含新两步强化板及旧在途件 |
| 既有闭环GameTest | 4/4 | [日志](./EXT-B-BOILER-01B-DEVICE-heat-loop-gametest.log)：C最终接面修复后回归真实Create近满守恒 |
| 新直列GameTest | 6/6 | [日志](./EXT-B-BOILER-01B-DEVICE-heat-chain-gametest.log)：真实两端管线、中央远端取热、本地满冷、方向/超限、拆放、非首成员停tick和管面恢复 |
| 最终增量打包 | 成功 | [最终assemble](./EXT-B-BOILER-01B-DEVICE-final-assemble.log)；未重复JUnit或未受影响测试 |
| 锅炉素材 | 静态通过，游戏画面待用户 | [A报告](./EXT-B-BOILER-01B-ASSETS.md)、[与反应堆对照](./EXT-B-BOILER-01B-ASSETS/boiler-reactor-style-comparison.png) |
| 换热器定向外观 | 静态通过，游戏画面待用户 | [V报告](./EXT-B-EXCHANGER-01D-ART.md)、[前后接口](./EXT-B-EXCHANGER-01D-ART/directional-interfaces-preview.png) |

实现细节分别见[锅炉D报告](./EXT-B-BOILER-01B-DEVICE.md)和[换热器C报告](./EXT-B-EXCHANGER-01D-DEVICE.md)。全批共40项required GameTest通过且测试服正常退出。失败阶段定位过两个夹具问题（重搭覆盖控制器、过早灌满本地热罐）和实际管接面恢复缺陷；保留原有守恒断言，未通过删测试换取结果。日志为最终运行记录，中间同名日志已由执行者覆盖，未冒称保留完整失败历史。

最终制品`build/libs/create_nuclear_industry-0.1.0.jar`为1,339,867字节，SHA-256：`00570863A3555ECDDCA2BBB46048429C55BD3DE719ADF366E307AFB540829816`。单元账本在后续连接修复中未变，22项证据复用；直列6项后只修改锅炉包/锅炉夹具，复用该直列结果。原生热回归在最后heat改动后新跑，不跑全P1。

## 人工门与兼容

从[联合三组清单](./CLIENT-CHECKLIST.md)启动候选：外观/新结构/减料，九段及主动出汽，拆接/存档。旧锅炉改搭五格结构，旧换热器管路改接前后端；库存由控制器或各台本地份额保留，不自动改用户世界。

旧强化板`step=2`半成品须再投入1坚固板，会按Create原生索引直接完成并跳过压片，不返还已投入精密构件；全新配方正常两步。此边界已运行验证并在手测清单列明，本批不自定义迁移。

PM未修改功能或测试代码；实现交由执行者，审查复用上述日志。用户日志、缓存、原有存档均未纳入交付；主目录`.vscode/launch.json`保持原SHA-256 `65EBB9ECB32C45F3254E2F511D3D134B17829E2FF0D73B7CB8094583EDE18C07`。仍在人工门，历史01A“基本通过”不当作本批新接口与画面的验收。
