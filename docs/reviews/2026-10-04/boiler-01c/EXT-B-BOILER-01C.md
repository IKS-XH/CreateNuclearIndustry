# EXT-B-BOILER-01C 交付报告

## 根因与范围

基线为 `d10e1af`，运行栈保持 Minecraft 1.21.1、Java 21、NeoForge 21.1.219、Create 6.0.10-280、Ponder 1.0.82。原账本以控制器级单个 `fillUsed` / `drainUsed` 实施整炉256mB/t额度；主动邻罐推送也查询同一个整炉剩余额度并在耗尽后停止。`BoilerSteamPressure` 又把总512压力除以已连接汽口数，单口压力随第二端口加入而下降。这些实现与已确认的新合同冲突，故改为每个物理端口各自256mB/t，库存仍由控制器共享；每个已连接汽口提供512 Create压力，单口管网内仍按Create分支分压。

`BoilerStructure.inspect` 只接受侧面中央合法槽位：控制器所在面不可再放水口，另外三面各有一个水口槽；四个侧面各有一个汽口槽。用户反馈的手动放置场景未在用户世界或真实玩家交互流程中复现。GameTest以正确外向朝向的合法槽位复现双水口、双汽口成型及接口行为；不能据此认定真实手放失败已经复现。测试覆盖位置为水口EAST与SOUTH（控制器在NORTH），汽口WEST与EAST；未扩展现有端口槽，也未处理同一面并排加口。

## 实现

- `BoilerState` 按端口坐标分别记录并持久化同tick用量。SIMULATE不记账，重复调用和主动/被动操作使用同一端口键，tick前进后预算刷新；容量仍是水汽各16000mB的全炉共享上限。
- 旧存档只有整炉`FillUsed`/`DrainUsed`时，迁移逻辑在保存的原tick内继续保守扣除旧用量；下一tick切换到逐口额度，避免同tick保存/载入扩大可用额度。
- 每个水口在Create分流计划中使用独立的flow identity；库存 identity仍指向共享账本，空位传全炉`16000-water`，流量空位传该口剩余额度。未修改共享兼容类。
- 邻罐主动推送和能力被动抽汽都按汽口位置扣同一个预算。压力不再按汽口数均分；锅炉热量、实际产汽和共享库存上限未改。
- 更新锅炉GameTest与`BoilerStateTest`，保留真实多端口、容量与守恒断言，没有删除原有业务覆盖。

## 验证

- `gradlew.bat test --tests com.iksxh.create_nuclear_industry.boiler.BoilerStateTest assemble --console=plain`：最终定向单测12/12通过；该轮assemble成功。首次尝试中的一条新断言错误地假定第二汽口还能取满256mB，修正为按共享库存实际余量232mB后重跑通过。见`EXT-B-BOILER-01C/test-assemble.log`、`test-assemble-rerun1.log`。
- `gradlew.bat -PgameTestNamespace=create_nuclear_industry_boiler -PgameTestDirectory=build/gametest/EXT-B-BOILER-01C runGameTestServer --console=plain`：最终17/17 required通过并正常保存退出，见`EXT-B-BOILER-01C/gametest-final-verified.log`。覆盖双水口同tick各256及重复调用拒绝、同源真实Create泵分流到两个合法水口时近满炉体/源罐守恒、双汽口无泵独立管线同时出汽、每口512压力、双邻罐主动输出与同口被动预算、共享蒸汽守恒，以及先铺管/成型/加口/拆口恢复/拆壳重搭。
- 最终 `gradlew.bat assemble --console=plain` 增量打包成功，见`EXT-B-BOILER-01C/assemble-final-verified.log`；制品`build/libs/create_nuclear_industry-0.1.0.jar`含锅炉账本、控制器和GameTest类。代码写集`git diff --check`无空白错误。

先前三份GameTest失败日志原样保留，用于记录修正测试误把物理`PipeConnection`对象存在当作仍可传输，以及重搭恢复基线不充分的问题；最终断言在t47拆壳前重新获取两个有效汽口句柄，t54拆壳后分别记录两罐与炉内蒸汽，确认原始8000mB守恒后补入2000mB，t78要求两罐分别高于t54基线且两罐+炉内总量为10000mB。最终日志中的17/17通过覆盖了这些增强后的断言。没有运行换热器全套或P1全量。

## 后续人工门

合法多口结构的GameTest使用`GameTestHelper.setBlock`写入正确朝向，没有模拟玩家手持方块的放置朝向逻辑。用户仍需在客户端复测合法位置添加第二水口/汽口后的成型和实际联动；本报告不把该人工门标为通过。所有改动保持未提交状态，未执行任何Git写操作。

## 技能

实际读取并应用`minecraft-modding`、`minecraft-testing`及`systematic-debugging`。未升级任何依赖。报告和原始日志均在`build/reports/extension/EXT-B-BOILER-01C*`。
