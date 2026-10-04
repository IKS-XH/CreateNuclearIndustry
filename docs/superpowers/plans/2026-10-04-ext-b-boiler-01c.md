# EXT-B-BOILER-01C：多端口独立流量与联动整改

**状态：实现与独立审查通过，待本轮人工验收，执行者`/root/boiler01c_impl`、审查者`/root/boiler01c_review`（均高速模型）。** 用户已确认提交`d10e1af`的锅炉01B及换热器01D手测通过；反馈加第二个端口后不成型，并明确“去掉整炉的给水和出气上限，改为每个端口单独限制流量”。本卡按故障定位与新流量合同实施，不扩展端口位置。用户追问现有每面数量，PM已说明固定槽位；合法多口成型与同面非法多口应区分，未复现不宣称结构缺陷已修复。

## 合同与范围

- 给水口每个256mB/t、蒸汽口每个256mB/t；取消整炉总流量额度。沿用现有单口值，不重新询问。同一端口多次执行及主动/被动混用共用该端口每tick额度；SIMULATE不消耗，下一tick刷新。无可用库存或容量时以实际可交易量为限。
- 水汽仍各16000mB整炉共享，1～9段实际产能和热量成本不变；更多端口增加输送能力，不增加产汽量。输入只接水，输出只出超临界蒸汽；方向、红石、失效结构、旧句柄、存档恢复的保护不变。
- 保留5×5×5及现有端口槽：1～3个水口位于第2层四侧中央且排除控制器，1～4个汽口位于第4层四侧中央。控制器/安全阀仍唯一；未批准侧面任意位置扩展。
- 原生主动出汽每个已连接汽口具备独立额度对应压力，不能再将512总压力先除汽口数。每个汽口内部的管道分支仍按Create规则分流；最终真实取液受该源端口额度和共享库存约束。真实多口分离管线、多个直接容器及主动/被动共存都应工作，不能仅删除上限断言。
- 先给出可复现证据和根因：区别结构不成型、capability/接面关闭、压力被均分、按固定顺序耗尽总额度等原因；无法复现用户具体症状时如实标明，不虚构现场根因。

## 执行、写集与验证

- 工作目录：`E:/MyMC/NewMod/Create_NuclearIndustry-ore-acquisition`，分支`codex/ore-acquisition`，功能基线`d10e1af`。当前PM正在修改docs验收记录；用户日志和`tools/art-assets/__pycache__`不属于本卡。不得改存档、删除日志、终止用户客户端。
- 执行者读取`AGENTS.md`、`docs/project-governance.md`第5.1节、本卡及实际`gradle.properties`；应用`C:/Users/IKSXH/.codex/skills/minecraft-modding/SKILL.md`、`minecraft-testing/SKILL.md`及systematic-debugging。基线MC1.21.1/Java21/NeoForge21.1.219/Create6.0.10-280/Ponder1.0.82，不升级依赖。
- 允许写：`src/main/java/com/iksxh/create_nuclear_industry/boiler/*.java`、`src/main/java/com/iksxh/create_nuclear_industry/gametest/ExtensionBoilerGameTests.java`、`src/test/java/com/iksxh/create_nuclear_industry/boiler/*.java`；必要时修改锅炉中英说明键，但不得重排整份语言文件。其他共享兼容类、Mixin、构建、资源及核心docs先报告必要性，未授权不得改。
- 所有新增/修改手写注释为中文；非显然端口预算、事务、生命周期说明单位、服务端边界和不变量。禁止Git写、转派或自行验收。交付报告仅`build/reports/extension/EXT-B-BOILER-01C.md`和同前缀原始日志。
- 一名执行者独占构建：增量assemble、受影响锅炉JUnit、锅炉namespace GameTest最终定向回归；新增/调整少量代表用例覆盖同tick两口分别256且合计512、同口不能绕过额度、模拟不扣、下一tick恢复、库存守恒、真实多汽口无泵并行及先铺管成型/加口/拆接恢复。水口复用现有双口同源泵管近满守恒场景，汽口用一条综合生命周期场景，避免重复搭测试。旧整炉限流是已审旧需求证据，新合同不人为回退基线补RED；真实失败另存日志，修正后仅重跑受影响部分。不要重跑换热器全套或P1全量。
- 若需修改共享Create兼容机制，报告后由PM扩大针对性范围。不得通过删测试断言或改变fixture掩盖失败。
- 完成后一轮独立审查复用日志；PM记录交付并交用户仅复测多口联动。本轮人工门前不合main、不推进汽轮机。

## 交付

[候选、证据与两项人工清单](../../reviews/2026-10-04/boiler-01c/README.md)：12JUnit、最终增强后的17GameTest和增量assemble通过；独立审查无待整改项。PM已核对原始日志及主目录配置保护，未重复全量。用户手放“不成型”现场未复现；只确认测试内合法多面端口及本卡流量合同，不宣称位置规则已扩展。
