# EXT-B-BOILER-REWORK-01F 实施交付

双库存生产实现已完成，可供 PM 一次合并审查。账本定向 25 项通过，真实机器 11 个不同用例以 07 日志的 10 项通过证据和 08 日志的单项通过证据合并交付；唯一一次增量 assemble 成功。本报告不代表客户端人工验收，不合入 main，不接续其他任务。

## 实现与接口

起点为候选工作区 `E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，当前文档基准 HEAD `2f9e3f741bfbed61ff6be510ae8aea6b51e82130`，生产未提交。实际读取 AGENTS、正式 01F 卡、治理 5.1/5.2、R1 根因与原始日志，应用 `minecraft-modding`、`minecraft-testing`、需求设计与 TDD 技能；故障分析继续沿用已读取的 systematic-debugging 流程。技术栈与所有数值保持不变。执行者未作 Git 写操作、未改核心文档、未派发代理、未启动或终止客户端、未读写用户世界/配置。

新增纯账本枚举 `BoilerSteamInventoryKind`，不依赖 Create UI/客户端图标或流体注册；原汽口 UI 枚举通过 `inventoryKind()` 映射。账本用两个真实 mB/HU 池，`steam()`、`steamHu()` 为合计，`steamTemperature()` 为质量加权总汽温。新增明确按种类的接口：

```
steam(kind), steamHu(kind), steamTemperature(kind), outputQualified(kind)
removableSteam(kind)
remainingDrain(kind, port, now)
drainSteam(kind, port, amount, simulate, now)
producedKind()
```

旧无种类 drain/remainingDrain 入口已删除并更新本卡调用者，不能偷偷代选某池。新库存只使用一份几何汽容量；缩容保留真实储备，拒绝新增。共同炉压为 `Σ(mk*Tk)/(C*Tsc配置)`；抽某池时保压上限按 `floor((Σ(mk*Tk)-min*C*Tsc配置)/Tk)` 重算，扣该池真实 `n*Hk/mk`，不动另一池库存或热量。

水侧预热→两池真实再热→新批次汽化的顺序不变。再热按实际欠熱比例分享真实付款；散热按显热比例分享原单一预算，不能散潜热；安全阀按库存比例分配原单一流量额度，尾量仍逐池按真实汽温封顶关阀线，一 tick 只泄放一次。新批次按加量后的共同炉压和该批实际汽温归类；已存在普通汽不升级、SC 不降级。SC 交付仍须真实比焓达到 `h(Tsc)`，但不再要求当前炉压达到生产 SC 的 50% 门槛。

控制器能力固定绑定该口所选池，动态读真实量/焓/共同余量。库存、温压变化不撤销选择句柄；结构/该口选项改变仍撤销旧能力并更新对应输出面。端口额度只限制本 tick 交易，不毁拓扑。空池/欠热池/无保压余量仍真实 SIMULATE=EMPTY，Create 原生等待、空流清除及传播范围不改。`BoilerSteamPressure` 和管道 mixin 未修改；主动压力调用者改用对应池的真实资格。

当前 NBT 明确保存 `NormalSteam/NormalSteamHu`、`SupercriticalSteam/SupercriticalSteamHu` 及原几何/付款/同 tick 预算；`Steam/SteamHu` 仅为合计观察值，恢复不从它们推测汽种。不实现旧存档迁移。客户端显示两池库存、共用总容量、实际新产汽；两池汽温由服务端 `NormalTs/SupercriticalTs` 快照传输，防止客户端用默认 Cp/Tb 重算。混合 snapshot 用服务端 `cpSteam=.4` 验证了 `1.00/1.55` 显示，客户端默认 `.2` 不会显示成 `2.10`。

生产写入限于 State、Controller、两个库存/UI 枚举、BoilerConfig 原字段语义及两种语言锅炉键。数值、流体注册、汽轮机、压力传播算法、构建脚本均未改。测试文件的本版本双池 seed 与被替代跨种断言同步，旧日志/01E 制品及 `__pycache__` 保留。

## 验证与失败记录

全部原始日志在 `build/reports/extension/EXT-B-BOILER-REWORK-01F/`，未覆盖 01E。JAVA_HOME 使用 `C:/Program Files/Java/jdk-21`，未 clean、未全工程 build、未重跑 17 项动力矩阵或全域。

| 证据 | 实际结果及解释 |
| --- | --- |
| `01-ledger-baseline.log` / `.xml` | 独立新合同 4 项均为真实断言红，旧账本尚不接受两种真实库存。 |
| `02-dual-ledger-green.log` | 编译失败：旧单测漏改一个 typed drain 参数。修正调用者，不算业务红/绿。 |
| `03-dual-ledger.log` | 新合同 9 绿、1 个夹具红：默认 18000 容量却期望 32000 示例压力；修正为显式 16 汽格，不改公式。 |
| `04-ledger-combined.log` / `04-TEST-*.xml` | 新双库存 10 项＋原热工账本 15 项，25/25 通过，退出 0。 |
| `05-inventory-game-tests.log` | 编译失败：新增测试遗漏纯账本枚举静态 import；修正后才启动服务端。 |
| `06-inventory-game-tests.log` | 11 项中的 10 项通过；直罐有实际成交，却被旧交易后“压力资格窗口”断言误判。 |
| `07-inventory-game-tests.log` | 其余 10 项通过，含自定义 Cp 的服务端温度 snapshot；直罐同 tick 出清，两池中 NORMAL 交易后库存亦可为 0，库存窗口断言仍不适用。退出 1，如实保留。 |
| `08-direct-reference.log` | 只运行失败直罐 1 项，退出 0；直接检查真实成交、后段增量、保压和逐 tick mB/HU/种类守恒。 |
| `09-assemble.log` | 唯一一次增量 assemble 成功，退出 0，编译/资源 up-to-date，仅 jar 新执行。 |

04 的 25 项覆盖共享容量、只归类新批次、SC 低压保留身份、欠热 SC 拒取→真实付款再热、异温抽 SC 精确保压、模拟纯读、多口同 tick 竞争、两种取同一物理口共用预算、比例再热/单一散热、单一阀额度与关阀线、当前双池 NBT 与剩余额度，以及既有冷却剂付款/余热/几何配置边界。

最终真实域与命令：

```
./gradlew.bat test --tests com.iksxh.create_nuclear_industry.boiler.BoilerDualSteamInventoryTest --tests com.iksxh.create_nuclear_industry.boiler.BoilerStateTest
./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_boiler_inventory -PgameTestDirectory=run/verification/boiler-inventory-01f
./gradlew.bat runGameTestServer -PgameTestNamespace=create_nuclear_industry_boiler_pressure_r1 -PgameTestDirectory=run/verification/boiler-inventory-01f
./gradlew.bat assemble
```

稳定通过证据为 07 的以下 10 个不同用例：R1 13 管主场景；01D 默认 SC 等待、独立选项保存、双支路＋直邻原生泵/冷液、每口 epoch/模拟/跨压力线真实取汽；混合当前保存/额度/护目镜 snapshot；实际热液补足冷 SC；控制器原生百分数与拆件；真实冷液动力泵；01C 连续空接收罐。08 为 R1 直罐对照，永久使用已存在的 R1 namespace/template，单独筛选 1 项，无新筛选框架。07 文件总体退出 1，不能写成整轮 11/11；其通过的 10 项与 08 的 1 项组合为 11 个不同合同的通过证据。

新增 `create_nuclear_industry_boiler_inventory/structure/inventory_empty.nbt` 是已有空模板逐字节副本，94B，SHA256 `A46FBCA10B8C94CD0228BD493BF5F1D19787EC034047715DB6AC40B9AB211941`。旧 R1 模板保留，无用户世界操作。

## R1 实际结果与供需边界

沿用真实 3SC 共管＋首段邻接 Create CreativeTank＋13 管中型，NORMAL 独立 13 管到实际 56000mB 普通罐，16 回路。热液夹具输入配置为实际能力填入 126mB/t、密度 .5HU/mB，观测真实转冷付款；不能把用户截图单 tick 的 63 产汽当作现场平均热输入。初始化仅保留暖炉快照，之后连续补水/真实热液付款，SIMULATE 不造量。

| 工况 | NORMAL 实际接收 | SC 创造罐接收 | 汽轮机排出 | 低于生产炉压线后真实 SC 抽取 |
| --- | ---: | ---: | ---: | ---: |
| 07：NORMAL 13 管 | 46633mB | 23927mB | 6574mB | 16809mB，64 个 tick |
| 08：NORMAL 直邻罐 | 49833mB | 22166mB | 5135mB | 13609mB，19 个 tick |

07 长管 NORMAL 罐在 t450 为 18283mB、t900 为 46633mB，后段真实增加 28350mB；08 直罐同样后段增加 28350mB。两者均未满 56000mB，持续段 P=10%、Ts=2，旧 NORMAL 句柄仍有效，长管 LayerII 为真实普通汽且 network pause=0。主场景固定已付热 NORMAL 库存持续存在 790 个统计 tick；直罐主动同 tick 出清可使观察到的 NORMAL 库存和可抽余量都是 0，真实累计成交仍连续增加。因此只对长管保留库存持续性检查，直罐以真实交付/后段增量为核心，不再用窗口要求伪造存量。

逐 tick 等式包含炉内两池、阀泄放、普通接收罐、真实创造罐接收及汽轮机实际排汽/滞留；HU 同时核对已转冷付款、炉内储备和真实带走焓。模拟反复读取完整 save 不变，错种计数为 0。独立实际再热用例从 `SC1000mB/900HU` 开始，真实热液转冷支付 101HU（包含自然散热补偿）后，通过原句柄交付 SC128mB/128HU，未转普通汽。

在低出汽下限、普通池持续排空时，共同炉压可以保持低于 SC 新批次门槛；此时新汽为 NORMAL，已有 SC 逐渐排出或受公共下限保留，不能保证永远产生 SC/维持汽轮机 SU。07 末期 SC 库存为 0，NORMAL 库存 3200mB；08 末期 SC 保压存量 3200mB，NORMAL 同 tick 出清为 0。这是实际新批次定种及公共下限合同，不能把不再产 SC 解读为已生成 SC 在低压被重标或锁种。

## 候选与待审查/客户端项

- 候选副本：`build/reports/extension/EXT-B-BOILER-REWORK-01F/create_nuclear_industry-0.1.0-boiler-01f.jar`。
- 大小：2266521B；SHA256：`B0CAE4FD76EA3244C1964331844DE66EBFDD9867393C10EF9A40452C27350CF8`。
- `candidate-metadata.json` 保存逐次退出码、基准 HEAD、JAR/清单/patch 哈希；`source-manifest.json` 与 `sources/` 保留 17 个写入源文件/资源的逐字节快照，`source.patch` 仅含允许源写集的相对 HEAD 差异，新文件内容另见 sources。
- 已验证候选包含双池枚举/状态/控制器 class、独立空模板及两种语言库存键，JSON 可解析；允许源写集 `git diff --check` 无空白错误。旧 01E JAR/报告/日志未覆盖。

生产代码在 PM 一次审查期间冻结。仍需 PM 核对提交与独立审查，再由用户在同级候选集中验证原 6×6×5 现场 60→10、两池/共享容量护目镜、各口对应独立管路、原生选项与冷液持续。自动证据不是手工或视觉验收；不研究历史存档兼容，不转换现存用户世界，不宣称整 chunk 真实退出重进已测。
